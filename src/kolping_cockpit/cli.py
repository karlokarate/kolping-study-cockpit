"""CLI interface for Kolping Study Cockpit using Typer and Rich."""

import typer
from rich.console import Console
from rich.table import Table

app = typer.Typer(
    name="kolping",
    help="Kolping Study Cockpit - Local connector for secure data export",
    add_completion=False,
)
console = Console()


# Export subcommand group
export_app = typer.Typer(
    name="export",
    help="Export study data from various sources",
)
app.add_typer(export_app, name="export")


@export_app.command("graphql")
def export_graphql(
    output: str = typer.Option(
        None,
        "--output",
        "-o",
        help="Output JSON file path (default: exports/YYYY-MM-DD/graphql.json)",
    ),
    simple: bool = typer.Option(
        True,
        "--simple/--full",
        help="Use simplified queries (less fields, more reliable)",
    ),
    query: str = typer.Option(
        None,
        "--query",
        "-q",
        help="Execute a specific query (myStudentData, moduls, semesters, etc.)",
    ),
) -> None:
    """
    Export data from the GraphQL API ("Mein Studium").

    Available queries:
    - myStudentData: Personal student data
    - myStudentGradeOverview: Grades and ECTS
    - moduls: All available modules
    - semesters: Semester information
    - pruefungs: Exam appointments
    - matchModulStudent: Your enrolled modules

    Examples:
        kolping export graphql
        kolping export graphql --query myStudentData
        kolping export graphql --full -o study_data.json
    """
    import json
    from datetime import UTC, datetime
    from pathlib import Path

    from kolping_cockpit.graphql_client import KolpingGraphQLClient

    console.print("[bold cyan]Kolping Study Cockpit - GraphQL Export[/bold cyan]")
    console.print("=" * 50)

    # Determine output path
    if output:
        output_path = Path(output)
    else:
        date_str = datetime.now(UTC).strftime("%Y-%m-%d")
        output_path = Path(f"exports/{date_str}/graphql.json")

    output_path.parent.mkdir(parents=True, exist_ok=True)

    try:
        with KolpingGraphQLClient() as client:
            console.print(f"[dim]Endpoint: {client.endpoint}[/dim]")
            console.print(f"[dim]Authenticated: {'Yes' if client.is_authenticated else 'No'}[/dim]")

            # Test connection
            success, message = client.test_connection()
            if not success:
                console.print(f"[red]✗ Connection failed: {message}[/red]")
                raise typer.Exit(code=1)

            console.print(f"[green]✓ Connected: {message}[/green]")

            if query:
                # Execute specific query
                console.print(f"[yellow]Executing query: {query}...[/yellow]")
                response = client.execute_named_query(query, simple=simple)

                if response.has_errors:
                    for error in response.errors or []:
                        console.print(f"[red]✗ Error: {error.message}[/red]")

                data = {
                    "query": query,
                    "timestamp": datetime.now(UTC).isoformat(),
                    "data": response.data,
                    "errors": [e.message for e in response.errors or []],
                }
            else:
                # Export all
                console.print(f"[yellow]Exporting all data (simple={simple})...[/yellow]")
                data = client.export_all(simple=simple)

            # Save to file
            with output_path.open("w", encoding="utf-8") as f:
                json.dump(data, f, indent=2, ensure_ascii=False, default=str)

            console.print(f"[green]✓ Data exported to: {output_path}[/green]")

            # Display summary
            if "data" in data and data["data"]:
                table = Table(title="Export Summary")
                table.add_column("Data Type", style="cyan")
                table.add_column("Records", style="magenta")

                for key, value in data["data"].items():
                    if isinstance(value, dict):
                        count = "1 object"
                    elif isinstance(value, list):
                        count = f"{len(value)} items"
                    else:
                        count = "present"
                    table.add_row(key, count)

                console.print(table)

            if "errors" in data and data["errors"]:
                console.print("[yellow]⚠ Some queries had errors (see export file)[/yellow]")

    except Exception as e:
        console.print(f"[red]✗ Export error: {e}[/red]")
        raise typer.Exit(code=1) from e


@export_app.command("moodle")
def export_moodle(
    output: str = typer.Option(
        None,
        "--output",
        "-o",
        help="Output JSON file path (default: exports/YYYY-MM-DD/moodle.json)",
    ),
) -> None:
    """
    Export data from Moodle Portal.

    Exports:
    - Dashboard overview
    - Enrolled courses
    - Assignments and deadlines
    - Grades
    - Upcoming calendar events

    Requires valid MoodleSession cookie (use 'kolping login-manual' first).
    """
    import json
    from datetime import UTC, datetime
    from pathlib import Path

    from kolping_cockpit.moodle_client import KolpingMoodleClient

    console.print("[bold cyan]Kolping Study Cockpit - Moodle Export[/bold cyan]")
    console.print("=" * 50)

    # Determine output path
    if output:
        output_path = Path(output)
    else:
        date_str = datetime.now(UTC).strftime("%Y-%m-%d")
        output_path = Path(f"exports/{date_str}/moodle.json")

    output_path.parent.mkdir(parents=True, exist_ok=True)

    try:
        with KolpingMoodleClient() as client:
            console.print(f"[dim]Portal: {client.base_url}[/dim]")
            console.print(
                f"[dim]Session: {'Configured' if client.is_authenticated else 'Not set'}[/dim]"
            )

            if not client.is_authenticated:
                console.print("[red]✗ No MoodleSession cookie found[/red]")
                console.print("[yellow]Run 'kolping login-manual' to set session[/yellow]")
                raise typer.Exit(code=1)

            # Test session
            is_valid, message = client.test_session()
            if not is_valid:
                console.print(f"[red]✗ Session invalid: {message}[/red]")
                console.print("[yellow]Run 'kolping login-manual' to refresh session[/yellow]")
                raise typer.Exit(code=1)

            console.print(f"[green]✓ Session valid: {message}[/green]")
            console.print("[yellow]Exporting Moodle data...[/yellow]")

            # Export all data
            data = client.export_all()

            # Save to file
            with output_path.open("w", encoding="utf-8") as f:
                json.dump(data, f, indent=2, ensure_ascii=False, default=str)

            console.print(f"[green]✓ Data exported to: {output_path}[/green]")

            # Display summary
            if "data" in data and data["data"]:
                table = Table(title="Export Summary")
                table.add_column("Data Type", style="cyan")
                table.add_column("Records", style="magenta")

                for key, value in data["data"].items():
                    if isinstance(value, dict):
                        if "courses" in value:
                            count = f"{len(value.get('courses', []))} courses"
                        else:
                            count = "1 object"
                    elif isinstance(value, list):
                        count = f"{len(value)} items"
                    else:
                        count = "present"
                    table.add_row(key, count)

                console.print(table)

            if "errors" in data and data["errors"]:
                console.print("[yellow]⚠ Some exports had errors:[/yellow]")
                for key, error in data["errors"].items():
                    console.print(f"  [red]{key}: {error}[/red]")

    except Exception as e:
        console.print(f"[red]✗ Export error: {e}[/red]")
        raise typer.Exit(code=1) from e


@export_app.command("all")
def export_all(
    output_dir: str = typer.Option(
        None,
        "--output-dir",
        "-d",
        help="Output directory (default: exports/YYYY-MM-DD/)",
    ),
) -> None:
    """
    Export all available data (GraphQL + Moodle).

    Creates separate files for each data source.
    """
    import json
    from datetime import UTC, datetime
    from pathlib import Path

    console.print("[bold cyan]Kolping Study Cockpit - Full Export[/bold cyan]")
    console.print("=" * 50)

    # Determine output directory
    if output_dir:
        base_path = Path(output_dir)
    else:
        date_str = datetime.now(UTC).strftime("%Y-%m-%d")
        base_path = Path(f"exports/{date_str}")

    base_path.mkdir(parents=True, exist_ok=True)

    results = {"graphql": None, "moodle": None}

    # Export GraphQL
    console.print("\n[bold]1. GraphQL API Export[/bold]")
    try:
        from kolping_cockpit.graphql_client import KolpingGraphQLClient

        with KolpingGraphQLClient() as client:
            success, _ = client.test_connection()
            if success:
                results["graphql"] = client.export_all(simple=True)
                graphql_path = base_path / "graphql.json"
                with graphql_path.open("w", encoding="utf-8") as f:
                    json.dump(results["graphql"], f, indent=2, ensure_ascii=False, default=str)
                console.print(f"[green]✓ GraphQL: {graphql_path}[/green]")
            else:
                console.print("[yellow]⚠ GraphQL: Connection failed[/yellow]")
    except Exception as e:
        console.print(f"[red]✗ GraphQL error: {e}[/red]")

    # Export Moodle
    console.print("\n[bold]2. Moodle Portal Export[/bold]")
    try:
        from kolping_cockpit.moodle_client import KolpingMoodleClient

        with KolpingMoodleClient() as client:
            if client.is_authenticated:
                is_valid, _ = client.test_session()
                if is_valid:
                    results["moodle"] = client.export_all()
                    moodle_path = base_path / "moodle.json"
                    with moodle_path.open("w", encoding="utf-8") as f:
                        json.dump(results["moodle"], f, indent=2, ensure_ascii=False, default=str)
                    console.print(f"[green]✓ Moodle: {moodle_path}[/green]")
                else:
                    console.print("[yellow]⚠ Moodle: Session expired[/yellow]")
            else:
                console.print("[yellow]⚠ Moodle: No session configured[/yellow]")
    except Exception as e:
        console.print(f"[red]✗ Moodle error: {e}[/red]")

    # Summary
    console.print("\n" + "=" * 50)
    console.print(f"[bold]Export complete: {base_path}[/bold]")

    exported = sum(1 for v in results.values() if v is not None)
    console.print(f"[dim]Successfully exported: {exported}/2 sources[/dim]")


@app.command()
def configure() -> None:
    """
    Configure credentials for the connector.

    Credentials are securely stored in the system keyring.
    """
    from kolping_cockpit.connector import LocalConnector

    console.print("[bold cyan]Configure Kolping Cockpit Credentials[/bold cyan]")
    console.print("=" * 50)

    username = typer.prompt("Username")
    password = typer.prompt("Password", hide_input=True)

    try:
        LocalConnector.store_credentials(username, password)
        console.print("[green]✓ Credentials stored securely[/green]")
    except Exception as e:
        console.print(f"[red]✗ Error storing credentials: {e}[/red]")
        raise typer.Exit(code=1) from e


@app.command()
def version() -> None:
    """Show version information."""
    from kolping_cockpit import __version__

    console.print(f"Kolping Study Cockpit v{__version__}")


@app.command()
def diagnose(
    verbose: bool = typer.Option(
        False,
        "--verbose",
        "-v",
        help="Show detailed output including headers",
    ),
) -> None:
    """
    Diagnose connectivity and configuration.

    Checks:
    - Environment variables and secrets
    - Moodle portal redirect chain (redacted)
    - GraphQL endpoint availability
    """
    import asyncio

    from kolping_cockpit.diagnostics import run_diagnostics

    console.print("[bold cyan]Kolping Study Cockpit - Diagnostics[/bold cyan]")
    console.print("=" * 50)

    try:
        asyncio.run(run_diagnostics(console, verbose=verbose))
    except Exception as e:
        console.print(f"[red]✗ Diagnostic error: {e}[/red]")
        raise typer.Exit(code=1) from e


@app.command()
def login(
    headless: bool = typer.Option(
        False,
        "--headless/--headed",
        help="Run browser in headless mode (default: headed for MFA)",
    ),
) -> None:
    """
    Interactive login via browser.

    Opens a browser window for Microsoft Entra authentication.
    Supports MFA. Stores session tokens securely after login.

    NOTE: Requires a display (X server). In Codespaces without GUI,
    use 'kolping login-manual' instead.
    """
    import os
    import sys

    from kolping_cockpit.auth import interactive_login

    console.print("[bold cyan]Kolping Study Cockpit - Interactive Login[/bold cyan]")
    console.print("=" * 50)

    # Check for display availability
    if not headless and not os.environ.get("DISPLAY") and sys.platform == "linux":
        console.print("[yellow]⚠ No display detected (Codespace/SSH environment)[/yellow]")
        console.print()
        console.print("[bold]Options:[/bold]")
        console.print("  1. Run locally with GUI: [cyan]kolping login[/cyan]")
        console.print("  2. Manual token entry:   [cyan]kolping login-manual[/cyan]")
        console.print("  3. Force headless:       [cyan]kolping login --headless[/cyan]")
        console.print()
        console.print("[dim]Headless login may not work with MFA.[/dim]")
        raise typer.Exit(code=1)

    console.print("[yellow]Opening browser for authentication...[/yellow]")
    console.print("[dim]Complete the login in the browser window (MFA may be required)[/dim]")

    try:
        result = interactive_login(headless=headless)
        if result.success:
            console.print("[green]✓ Login successful![/green]")
            console.print(f"[dim]Session stored for user: {result.username}[/dim]")
        else:
            console.print(f"[red]✗ Login failed: {result.error}[/red]")
            raise typer.Exit(code=1)
    except Exception as e:
        console.print(f"[red]✗ Login error: {e}[/red]")
        raise typer.Exit(code=1) from e


@app.command()
def logout() -> None:
    """
    Clear stored session tokens.

    Removes all stored authentication tokens from keyring.
    """
    from kolping_cockpit.settings import delete_secret

    console.print("[bold cyan]Kolping Study Cockpit - Logout[/bold cyan]")
    console.print("=" * 50)

    secrets_to_clear = ["moodle_session", "graphql_bearer_token", "access_token"]
    cleared = 0

    for secret in secrets_to_clear:
        if delete_secret(secret):
            cleared += 1
            console.print(f"[green]✓ Cleared: {secret}[/green]")

    if cleared > 0:
        console.print(f"\n[green]✓ Logged out successfully ({cleared} tokens cleared)[/green]")
    else:
        console.print("[yellow]No stored tokens found[/yellow]")


@app.command("login-manual")
def login_manual() -> None:
    """
    Manual token entry for headless environments.

    Use this in Codespaces or SSH sessions where no GUI is available.
    You'll need to login via browser elsewhere and paste the session cookie.
    """
    from kolping_cockpit.settings import get_settings, store_secret

    settings = get_settings()

    console.print("[bold cyan]Kolping Study Cockpit - Manual Login[/bold cyan]")
    console.print("=" * 50)
    console.print()
    console.print("[yellow]Instructions:[/yellow]")
    console.print("1. Open in your local browser:")
    console.print(f"   [cyan]{settings.moodle_login_url}[/cyan]")
    console.print()
    console.print("2. Login with your Microsoft account (complete MFA if needed)")
    console.print()
    console.print("3. After successful login, open Developer Tools (F12)")
    console.print("   → Application tab → Cookies → portal.kolping-hochschule.de")
    console.print("   → Copy the value of 'MoodleSession'")
    console.print()

    moodle_session = typer.prompt("Paste MoodleSession cookie value")

    if moodle_session:
        store_secret("moodle_session", moodle_session.strip())
        console.print("[green]✓ Session stored successfully![/green]")
    else:
        console.print("[red]✗ No session provided[/red]")
        raise typer.Exit(code=1)

    # Optional: Also ask for GraphQL bearer token
    console.print()
    console.print("[yellow]Optional: GraphQL Bearer Token[/yellow]")
    console.print("If you also want to access 'Mein Studium' data:")
    console.print("1. Open: [cyan]https://khs-meinstudium.de[/cyan]")
    console.print("2. Login and open Developer Tools (F12) → Network tab")
    console.print("3. Look for GraphQL requests and copy the Authorization header")
    console.print()

    bearer_token = typer.prompt(
        "Paste Bearer token (or press Enter to skip)", default="", show_default=False
    )

    if bearer_token:
        # Remove "Bearer " prefix if included
        token = bearer_token.strip()
        if token.lower().startswith("bearer "):
            token = token[7:]
        success = store_secret("graphql_bearer_token", token)
        if success:
            console.print("[green]✓ GraphQL token stored![/green]")
        else:
            console.print("[red]✗ Failed to store GraphQL token![/red]")


@app.command("set-moodle")
def set_moodle_token() -> None:
    """
    Set only the Moodle session cookie.

    Use this to update just the Moodle token without affecting GraphQL.
    """
    from kolping_cockpit.settings import store_secret

    console.print("[bold cyan]Set Moodle Session Cookie[/bold cyan]")
    console.print("=" * 50)

    moodle_session = typer.prompt("Paste MoodleSession cookie value")

    if moodle_session:
        success = store_secret("moodle_session", moodle_session.strip())
        if success:
            console.print("[green]✓ Moodle session stored![/green]")
        else:
            console.print("[red]✗ Failed to store session![/red]")
            raise typer.Exit(code=1)
    else:
        console.print("[red]✗ No session provided[/red]")
        raise typer.Exit(code=1)


@app.command("set-graphql")
def set_graphql_token() -> None:
    """
    Set only the GraphQL bearer token.

    Use this to update just the GraphQL token without affecting Moodle.
    """
    from kolping_cockpit.settings import store_secret

    console.print("[bold cyan]Set GraphQL Bearer Token[/bold cyan]")
    console.print("=" * 50)

    bearer_token = typer.prompt("Paste Bearer token")

    if bearer_token:
        # Remove "Bearer " prefix if included
        token = bearer_token.strip()
        if token.lower().startswith("bearer "):
            token = token[7:]
        success = store_secret("graphql_bearer_token", token)
        if success:
            console.print("[green]✓ GraphQL token stored![/green]")
        else:
            console.print("[red]✗ Failed to store token![/red]")
            raise typer.Exit(code=1)
    else:
        console.print("[red]✗ No token provided[/red]")
        raise typer.Exit(code=1)


@app.command()
def status() -> None:
    """
    Show current authentication and export status.
    """
    from kolping_cockpit.settings import get_secret_from_env_or_keyring, get_settings

    settings = get_settings()

    console.print("[bold cyan]Kolping Study Cockpit - Status[/bold cyan]")
    console.print("=" * 50)

    # Check stored tokens
    table = Table(title="Authentication Status")
    table.add_column("Token", style="cyan")
    table.add_column("Status", style="magenta")
    table.add_column("Action")

    moodle = get_secret_from_env_or_keyring("moodle_session")
    graphql = get_secret_from_env_or_keyring("graphql_bearer_token")

    table.add_row(
        "Moodle Session",
        "[green]✓ Set[/green]" if moodle else "[red]✗ Not set[/red]",
        "" if moodle else "kolping login-manual",
    )
    table.add_row(
        "GraphQL Token",
        "[green]✓ Set[/green]" if graphql else "[yellow]○ Optional[/yellow]",
        "" if graphql else "(set via login-manual)",
    )

    console.print(table)

    # Show endpoints
    console.print()
    console.print("[bold]Endpoints:[/bold]")
    console.print(f"  Moodle: {settings.moodle_base_url}")
    console.print(f"  GraphQL: {settings.graphql_endpoint}")

    # Test connections if tokens are available
    if moodle:
        console.print()
        console.print("[yellow]Testing Moodle session...[/yellow]")
        try:
            from kolping_cockpit.moodle_client import KolpingMoodleClient

            with KolpingMoodleClient() as client:
                is_valid, message = client.test_session()
                if is_valid:
                    console.print(f"[green]✓ Moodle: {message}[/green]")
                else:
                    console.print(f"[red]✗ Moodle: {message}[/red]")
        except Exception as e:
            console.print(f"[red]✗ Moodle test failed: {e}[/red]")


if __name__ == "__main__":
    app()
