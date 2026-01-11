"""CLI interface for Kolping Study Cockpit using Typer and Rich."""

import typer
from rich.console import Console
from rich.panel import Panel
from rich.table import Table

# Constants for display formatting
MODULE_NAME_MAX_LENGTH = 50
COURSE_NAME_MATCH_LENGTH = 20

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


@app.command("deadlines")
def show_deadlines(
    include_past: bool = typer.Option(
        False, "--include-past", "-p", help="Include past/completed modules"
    ),
    semester: int = typer.Option(
        None, "--semester", "-s", help="Filter by specific semester number"
    ),
) -> None:
    """
    Show upcoming exams, assignments and deadlines.

    Combines data from:
    - GraphQL API (exam registrations, module status)
    - Moodle Calendar (upcoming events, deadlines)

    Example:
        kolping deadlines
        kolping deadlines --semester 3
    """

    from rich.panel import Panel

    console.print("[bold cyan]📚 Kolping Study Cockpit - Prüfungen & Deadlines[/bold cyan]")
    console.print("=" * 60)

    # Data containers
    grade_data = None
    calendar_events = []
    errors = []

    # 1. Fetch GraphQL data (exam status)
    console.print("\n[dim]Lade Prüfungsstatus...[/dim]")
    try:
        from kolping_cockpit.graphql_client import KolpingGraphQLClient

        with KolpingGraphQLClient() as client:
            if client.is_authenticated:
                success, _ = client.test_connection()
                if success:
                    response = client.execute_named_query("myStudentGradeOverview")
                    if response.data and "myStudentGradeOverview" in response.data:
                        grade_data = response.data["myStudentGradeOverview"]
                        console.print("[green]✓ Prüfungsdaten geladen[/green]")
                else:
                    errors.append("GraphQL: Verbindung fehlgeschlagen")
            else:
                errors.append("GraphQL: Kein Bearer Token konfiguriert")
    except Exception as e:
        errors.append(f"GraphQL: {e}")

    # 2. Fetch Moodle calendar events
    console.print("[dim]Lade Kalender-Events...[/dim]")
    try:
        from kolping_cockpit.moodle_client import KolpingMoodleClient

        with KolpingMoodleClient() as client:
            if client.is_authenticated:
                is_valid, _ = client.test_session()
                if is_valid:
                    calendar_events = client.get_upcoming_deadlines()
                    console.print(
                        f"[green]✓ {len(calendar_events)} Kalender-Events geladen[/green]"
                    )
                else:
                    errors.append("Moodle: Session abgelaufen")
            else:
                errors.append("Moodle: Keine Session konfiguriert")
    except Exception as e:
        errors.append(f"Moodle: {e}")

    # Show errors if any
    if errors:
        console.print("\n[yellow]⚠ Einige Datenquellen nicht verfügbar:[/yellow]")
        for err in errors:
            console.print(f"  [dim]{err}[/dim]")

    # 3. Display exam overview
    if grade_data:
        current_sem = grade_data.get("currentSemester", "Unbekannt")
        total_grade = grade_data.get("grade", "-")
        total_ects = grade_data.get("eCTS", 0)

        console.print(f"\n[bold]Aktuelles Semester:[/bold] {current_sem}")
        console.print(f"[bold]Notendurchschnitt:[/bold] {total_grade}")
        console.print(f"[bold]Erreichte ECTS:[/bold] {total_ects}")

        modules = grade_data.get("modules", [])

        # Filter by semester if specified
        if semester:
            modules = [m for m in modules if m.get("semester") == semester]

        # Categorize modules
        angemeldet = [m for m in modules if m.get("examStatus") == "angemeldet"]
        nicht_bestanden = [m for m in modules if m.get("examStatus") == "nicht bestanden"]
        offen = [
            m
            for m in modules
            if m.get("examStatus") is None and m.get("pruefungsform") != "Anerkennung"
        ]
        abgemeldet = [m for m in modules if m.get("examStatus") == "abgemeldet"]

        # Show registered exams (urgent!)
        if angemeldet:
            console.print("\n")
            table = Table(
                title="🔴 ANGEMELDETE PRÜFUNGEN",
                title_style="bold red",
                border_style="red",
            )
            table.add_column("Modul", style="bold")
            table.add_column("Sem.", justify="center")
            table.add_column("Prüfungsform", style="cyan")
            table.add_column("ECTS", justify="right")

            for m in angemeldet:
                table.add_row(
                    m.get("modulbezeichnung", "?")[:50],
                    str(m.get("semester", "?")),
                    m.get("pruefungsform", "?"),
                    str(m.get("eCTS", 0)),
                )
            console.print(table)

        # Show failed exams (need retry)
        if nicht_bestanden:
            console.print("\n")
            table = Table(
                title="⚠️ NICHT BESTANDEN (Wiederholung nötig)",
                title_style="bold yellow",
                border_style="yellow",
            )
            table.add_column("Modul", style="bold")
            table.add_column("Sem.", justify="center")
            table.add_column("Prüfungsform", style="cyan")
            table.add_column("ECTS", justify="right")

            for m in nicht_bestanden:
                table.add_row(
                    m.get("modulbezeichnung", "?")[:50],
                    str(m.get("semester", "?")),
                    m.get("pruefungsform", "?"),
                    str(m.get("eCTS", 0)),
                )
            console.print(table)

        # Show deregistered exams
        if abgemeldet:
            console.print("\n")
            table = Table(
                title="📋 ABGEMELDET (neu anmelden)",
                title_style="bold blue",
                border_style="blue",
            )
            table.add_column("Modul", style="bold")
            table.add_column("Sem.", justify="center")
            table.add_column("Prüfungsform", style="cyan")
            table.add_column("ECTS", justify="right")

            for m in abgemeldet:
                table.add_row(
                    m.get("modulbezeichnung", "?")[:50],
                    str(m.get("semester", "?")),
                    m.get("pruefungsform", "?"),
                    str(m.get("eCTS", 0)),
                )
            console.print(table)

        # Show open modules (not yet registered)
        if offen and not include_past:
            # Filter to current semester range (show semesters 1-5 for WiSe 2025-2026 = 5th sem)
            current_sem_num = 5  # Could be parsed from currentSemester
            offen_relevant = [m for m in offen if m.get("semester", 0) <= current_sem_num]
        else:
            offen_relevant = offen

        if offen_relevant:
            console.print("\n")
            table = Table(
                title="📝 OFFENE MODULE (noch nicht angemeldet)",
                title_style="bold",
            )
            table.add_column("Modul", style="bold")
            table.add_column("Sem.", justify="center")
            table.add_column("Prüfungsform", style="cyan")
            table.add_column("ECTS", justify="right")

            for m in sorted(offen_relevant, key=lambda x: x.get("semester", 99)):
                table.add_row(
                    m.get("modulbezeichnung", "?")[:50],
                    str(m.get("semester", "?")),
                    m.get("pruefungsform", "?"),
                    str(m.get("eCTS", 0)),
                )
            console.print(table)

        # Summary panel
        bestanden = [m for m in modules if m.get("examStatus") == "bestanden"]
        anerkannt = [m for m in modules if m.get("examStatus") == "anerkannt"]

        summary = f"""
[green]✓ Bestanden:[/green] {len(bestanden)} Module
[green]✓ Anerkannt:[/green] {len(anerkannt)} Module
[red]✗ Nicht bestanden:[/red] {len(nicht_bestanden)} Module
[blue]○ Angemeldet:[/blue] {len(angemeldet)} Module
[yellow]○ Abgemeldet:[/yellow] {len(abgemeldet)} Module
[dim]○ Offen:[/dim] {len(offen)} Module
        """
        console.print(Panel(summary.strip(), title="Zusammenfassung", border_style="cyan"))

    # 4. Display calendar events
    if calendar_events:
        console.print("\n")
        table = Table(title="📅 KOMMENDE TERMINE (Moodle Kalender)")
        table.add_column("Event", style="bold")
        table.add_column("Datum/Zeit", style="cyan")
        table.add_column("Kurs", style="dim")

        for event in calendar_events[:10]:  # Limit to 10
            table.add_row(
                event.title[:40] if event.title else "?",
                event.start_time or "?",
                event.course_name or "",
            )
        console.print(table)

    # Final hint
    console.print("\n[dim]Tipp: Prüfungstermine im Moodle-Portal unter Kalender prüfen![/dim]")
    console.print("[dim]      kolping export all - für vollständigen Datenexport[/dim]")


@app.command("analyze")
def analyze_captures(
    docs_dir: str = typer.Option("docs", "--docs", "-d", help="Directory with HTTP captures"),
    show_all: bool = typer.Option(
        False, "--all", "-a", help="Show all modules, not just open ones"
    ),
) -> None:
    """
    Analyze captured HTTP data for exam dates and deadlines.

    Reads from local capture files (docs/ folder) to extract:
    - GraphQL grade overview with exam status
    - Moodle calendar events with dates
    - Klausur (exam) dates

    This works offline using previously captured data.
    """
    import json
    from datetime import datetime
    from pathlib import Path

    from rich.panel import Panel

    console.print("[bold cyan]📊 Kolping Study Cockpit - Offline Analyse[/bold cyan]")
    console.print("=" * 60)

    docs_path = Path(docs_dir)
    if not docs_path.exists():
        console.print(f"[red]✗ Verzeichnis nicht gefunden: {docs_path}[/red]")
        raise typer.Exit(code=1)

    # Data containers
    grade_data = None
    calendar_events = []
    student_data = None

    # 1. Find and load GraphQL grade overview
    console.print("\n[dim]Suche GraphQL Prüfungsdaten...[/dim]")
    for subdir in sorted(docs_path.iterdir()):
        if not subdir.is_dir():
            continue
        response_file = subdir / "response_body.json"
        if not response_file.exists():
            continue
        try:
            with response_file.open("r", encoding="utf-8") as f:
                data = json.load(f)
            if isinstance(data, dict) and "data" in data:
                if "myStudentGradeOverview" in data["data"]:
                    grade_data = data["data"]["myStudentGradeOverview"]
                    console.print(f"[green]✓ Prüfungsdaten gefunden in {subdir.name}/[/green]")
                if "myStudentData" in data["data"]:
                    student_data = data["data"]["myStudentData"]
                    console.print(f"[green]✓ Studentendaten gefunden in {subdir.name}/[/green]")
        except (json.JSONDecodeError, KeyError):
            continue

    # 2. Find and parse Moodle calendar HTML
    console.print("[dim]Suche Moodle Kalender-Daten...[/dim]")
    for subdir in sorted(docs_path.iterdir()):
        if not subdir.is_dir():
            continue
        for html_file in subdir.glob("*.html"):
            try:
                from bs4 import BeautifulSoup

                with html_file.open("r", encoding="utf-8") as f:
                    soup = BeautifulSoup(f.read(), "html.parser")

                # Look for calendar events
                event_divs = soup.find_all(
                    "div", class_="event", attrs={"data-region": "event-item"}
                )
                for elem in event_divs:
                    link = elem.find("a", attrs={"data-event-id": True})
                    date_div = elem.find("div", class_="date")

                    if link and date_div:
                        title = link.get_text(strip=True)
                        date_text = date_div.get_text(strip=True)
                        # Extract timestamp from link href
                        href = str(link.get("href", ""))
                        timestamp = None
                        if "time=" in href:
                            import re

                            match = re.search(r"time=(\d+)", href)
                            if match:
                                timestamp = int(match.group(1))

                        calendar_events.append(
                            {
                                "title": title,
                                "date_text": date_text.replace("»", "→"),
                                "timestamp": timestamp,
                                "url": href,
                            }
                        )

                if event_divs:
                    console.print(
                        f"[green]✓ {len(event_divs)} Events gefunden in {html_file.name}[/green]"
                    )
            except Exception:
                continue

    # Remove duplicates based on timestamp
    seen_timestamps = set()
    unique_events = []
    for event in calendar_events:
        ts = event.get("timestamp")
        if ts and ts not in seen_timestamps:
            seen_timestamps.add(ts)
            unique_events.append(event)
    calendar_events = unique_events

    # 3. Display student info
    if student_data:
        name = f"{student_data.get('vorname', '')} {student_data.get('nachname', '')}"
        console.print(f"\n[bold]Student:[/bold] {name}")

    # 4. Display exam overview from GraphQL
    if grade_data:
        current_sem = grade_data.get("currentSemester", "Unbekannt")
        total_grade = grade_data.get("grade", "-")
        total_ects = grade_data.get("eCTS", 0)

        console.print(f"[bold]Aktuelles Semester:[/bold] {current_sem}")
        console.print(f"[bold]Notendurchschnitt:[/bold] {total_grade}")
        console.print(f"[bold]Erreichte ECTS:[/bold] {total_ects}")

        modules = grade_data.get("modules", [])

        # Find all Klausuren (exams)
        klausuren = [m for m in modules if m.get("pruefungsform") == "Klausur"]

        if klausuren:
            console.print("\n")
            table = Table(
                title="📝 ALLE KLAUSUREN",
                title_style="bold magenta",
                border_style="magenta",
            )
            table.add_column("Modul", style="bold")
            table.add_column("Sem.", justify="center")
            table.add_column("Status", style="cyan")
            table.add_column("Note", justify="right")
            table.add_column("ECTS", justify="right")

            for m in sorted(
                klausuren, key=lambda x: (x.get("semester", 99), x.get("modulbezeichnung", ""))
            ):
                status = m.get("examStatus") or "offen"
                note = m.get("note") or "-"
                status_style = {
                    "bestanden": "[green]bestanden[/green]",
                    "nicht bestanden": "[red]nicht bestanden[/red]",
                    "angemeldet": "[blue]ANGEMELDET[/blue]",
                    "abgemeldet": "[yellow]abgemeldet[/yellow]",
                }.get(status, f"[dim]{status}[/dim]")

                table.add_row(
                    m.get("modulbezeichnung", "?")[:45].strip(),
                    str(m.get("semester", "?")),
                    status_style,
                    str(note),
                    str(m.get("eCTS", 0)),
                )
            console.print(table)

        # Categorize all modules
        angemeldet = [m for m in modules if m.get("examStatus") == "angemeldet"]
        nicht_bestanden = [m for m in modules if m.get("examStatus") == "nicht bestanden"]
        abgemeldet = [m for m in modules if m.get("examStatus") == "abgemeldet"]
        offen = [
            m
            for m in modules
            if m.get("examStatus") is None and m.get("pruefungsform") != "Anerkennung"
        ]
        bestanden = [m for m in modules if m.get("examStatus") == "bestanden"]
        anerkannt = [m for m in modules if m.get("examStatus") == "anerkannt"]

        # Show registered exams (urgent!)
        if angemeldet:
            console.print("\n")
            table = Table(
                title="🔴 ANGEMELDETE PRÜFUNGEN (Termine beachten!)",
                title_style="bold red",
                border_style="red",
            )
            table.add_column("Modul", style="bold")
            table.add_column("Sem.", justify="center")
            table.add_column("Prüfungsform", style="cyan")
            table.add_column("ECTS", justify="right")

            for m in angemeldet:
                table.add_row(
                    m.get("modulbezeichnung", "?")[:50].strip(),
                    str(m.get("semester", "?")),
                    m.get("pruefungsform", "?"),
                    str(m.get("eCTS", 0)),
                )
            console.print(table)

        # Show failed exams
        if nicht_bestanden:
            console.print("\n")
            table = Table(
                title="⚠️ NICHT BESTANDEN (Wiederholung nötig)",
                title_style="bold yellow",
                border_style="yellow",
            )
            table.add_column("Modul", style="bold")
            table.add_column("Sem.", justify="center")
            table.add_column("Prüfungsform", style="cyan")

            for m in nicht_bestanden:
                table.add_row(
                    m.get("modulbezeichnung", "?")[:50].strip(),
                    str(m.get("semester", "?")),
                    m.get("pruefungsform", "?"),
                )
            console.print(table)

        # Summary
        summary = f"""
[green]✓ Bestanden:[/green] {len(bestanden)} Module ({sum(m.get("eCTS", 0) for m in bestanden):.0f} ECTS)
[green]✓ Anerkannt:[/green] {len(anerkannt)} Module ({sum(m.get("eCTS", 0) for m in anerkannt):.0f} ECTS)
[red]✗ Nicht bestanden:[/red] {len(nicht_bestanden)} Module
[blue]○ Angemeldet:[/blue] {len(angemeldet)} Module
[yellow]○ Abgemeldet:[/yellow] {len(abgemeldet)} Module
[dim]○ Offen:[/dim] {len(offen)} Module ({sum(m.get("eCTS", 0) for m in offen):.0f} ECTS)
        """
        console.print(Panel(summary.strip(), title="Zusammenfassung", border_style="cyan"))

    # 5. Display calendar events with proper dates
    if calendar_events:
        console.print("\n")
        table = Table(title="📅 KOMMENDE TERMINE (aus Moodle Kalender)")
        table.add_column("Modul/Event", style="bold", max_width=40)
        table.add_column("Datum & Zeit", style="cyan", max_width=35)

        # Sort by timestamp
        sorted_events = sorted(
            [e for e in calendar_events if e.get("timestamp")], key=lambda x: x["timestamp"]
        )

        for event in sorted_events[:15]:
            # Format timestamp to readable date
            ts = event.get("timestamp")
            if ts:
                dt = datetime.fromtimestamp(ts)
                date_formatted = dt.strftime("%a, %d.%m.%Y %H:%M")
            else:
                date_formatted = event.get("date_text", "?")

            table.add_row(
                event.get("title", "?")[:40],
                date_formatted,
            )
        console.print(table)

    # Final summary
    if not grade_data and not calendar_events:
        console.print("\n[yellow]Keine Daten gefunden. Stelle sicher, dass:[/yellow]")
        console.print("  1. HTTP-Captures im docs/ Ordner liegen")
        console.print("  2. ZIP-Dateien entpackt wurden")
        console.print("  3. response_body.json oder .html Dateien vorhanden sind")
    else:
        console.print("\n[dim]Datenquelle: Offline-Analyse von HTTP-Captures[/dim]")


@app.command("fetch")
def fetch_all_online(
    output: str = typer.Option(None, "--output", "-o", help="Output JSON file path for export"),
    limit: int = typer.Option(0, "--limit", "-l", help="Limit number of events (0 = unlimited)"),
) -> None:
    """
    Full online fetch of all study data.

    Fetches LIVE data from:
    - GraphQL API: All modules, grades, student data
    - Moodle Portal: All calendar events, courses, assignments

    Requires valid tokens (use 'kolping set-graphql' and 'kolping set-moodle').

    Example:
        kolping fetch
        kolping fetch --output study_data.json
    """
    import json
    from datetime import UTC, datetime
    from pathlib import Path

    from rich.panel import Panel

    console.print("[bold cyan]🌐 Kolping Study Cockpit - Online Vollfetch[/bold cyan]")
    console.print("=" * 60)

    all_data: dict = {
        "fetch_timestamp": datetime.now(UTC).isoformat(),
        "graphql": {},
        "moodle": {},
        "errors": [],
    }

    # 1. GraphQL Full Fetch
    console.print("\n[bold]1. GraphQL API Fetch[/bold]")
    try:
        from kolping_cockpit.graphql_client import KolpingGraphQLClient

        with KolpingGraphQLClient() as client:
            if not client.is_authenticated:
                console.print("[red]✗ Kein Bearer Token konfiguriert[/red]")
                console.print("[dim]  Setze Token mit: kolping set-graphql <TOKEN>[/dim]")
                all_data["errors"].append("GraphQL: Kein Bearer Token")
            else:
                console.print("[dim]  Teste Verbindung...[/dim]")
                success, msg = client.test_connection()
                if not success:
                    console.print(f"[red]✗ Verbindung fehlgeschlagen: {msg}[/red]")
                    all_data["errors"].append(f"GraphQL: {msg}")
                else:
                    console.print("[green]✓ Verbunden[/green]")

                    # Fetch student data
                    console.print("[dim]  Lade Studentendaten...[/dim]")
                    response = client.execute_named_query("myStudentData")
                    if response.data and "myStudentData" in response.data:
                        all_data["graphql"]["student"] = response.data["myStudentData"]
                        name = f"{response.data['myStudentData'].get('vorname', '')} {response.data['myStudentData'].get('nachname', '')}"
                        console.print(f"[green]✓ Student: {name}[/green]")
                    elif response.has_errors:
                        console.print(f"[yellow]⚠ Studentendaten: {response.errors}[/yellow]")

                    # Fetch grade overview (all modules)
                    console.print("[dim]  Lade Prüfungsübersicht...[/dim]")
                    response = client.execute_named_query("myStudentGradeOverview")
                    if response.data and "myStudentGradeOverview" in response.data:
                        overview = response.data["myStudentGradeOverview"]
                        all_data["graphql"]["gradeOverview"] = overview
                        modules = overview.get("modules", [])
                        console.print(f"[green]✓ {len(modules)} Module geladen[/green]")
                        console.print(
                            f"[dim]  Durchschnitt: {overview.get('grade', '-')} | "
                            f"ECTS: {overview.get('eCTS', 0)} | "
                            f"Semester: {overview.get('currentSemester', '?')}[/dim]"
                        )
                    elif response.has_errors:
                        console.print(f"[yellow]⚠ Prüfungsdaten: {response.errors}[/yellow]")

    except Exception as e:
        console.print(f"[red]✗ GraphQL Fehler: {e}[/red]")
        all_data["errors"].append(f"GraphQL: {e}")

    # 2. Moodle Full Fetch
    console.print("\n[bold]2. Moodle Portal Fetch[/bold]")
    try:
        from kolping_cockpit.moodle_client import KolpingMoodleClient

        with KolpingMoodleClient() as client:
            if not client.is_authenticated:
                console.print("[red]✗ Keine Moodle Session konfiguriert[/red]")
                console.print("[dim]  Setze Session mit: kolping set-moodle <SESSION>[/dim]")
                all_data["errors"].append("Moodle: Keine Session")
            else:
                console.print("[dim]  Teste Session...[/dim]")
                is_valid, msg = client.test_session()
                if not is_valid:
                    console.print(f"[red]✗ Session ungültig: {msg}[/red]")
                    all_data["errors"].append(f"Moodle: {msg}")
                else:
                    console.print("[green]✓ Session gültig[/green]")

                    # Fetch dashboard
                    console.print("[dim]  Lade Dashboard...[/dim]")
                    dashboard = client.get_dashboard()
                    all_data["moodle"]["user"] = dashboard.user_name
                    console.print(f"[green]✓ User: {dashboard.user_name}[/green]")

                    # Fetch courses
                    console.print("[dim]  Lade Kurse...[/dim]")
                    courses = client.get_courses()
                    all_data["moodle"]["courses"] = [
                        {"id": c.id, "name": c.name, "url": c.url} for c in courses
                    ]
                    console.print(f"[green]✓ {len(courses)} Kurse geladen[/green]")

                    # Fetch calendar events (all upcoming)
                    console.print("[dim]  Lade Kalender-Events...[/dim]")
                    events = client.get_upcoming_deadlines()
                    all_data["moodle"]["events"] = [
                        {
                            "id": e.id,
                            "title": e.title,
                            "start_time": e.start_time,
                            "course_name": e.course_name,
                            "url": e.url,
                        }
                        for e in events
                    ]
                    console.print(f"[green]✓ {len(events)} Events geladen[/green]")

                    # Fetch assignments
                    console.print("[dim]  Lade Aufgaben...[/dim]")
                    assignments = client.get_assignments()
                    all_data["moodle"]["assignments"] = [
                        {
                            "id": a.id,
                            "name": a.name,
                            "due_date": a.due_date,
                            "course_name": a.course_name,
                        }
                        for a in assignments
                    ]
                    console.print(f"[green]✓ {len(assignments)} Aufgaben geladen[/green]")

                    # Fetch grades
                    console.print("[dim]  Lade Noten...[/dim]")
                    grades = client.get_grades()
                    all_data["moodle"]["grades"] = [
                        {"item": g.item_name, "grade": g.grade} for g in grades
                    ]
                    console.print(f"[green]✓ {len(grades)} Noteneinträge[/green]")

    except Exception as e:
        console.print(f"[red]✗ Moodle Fehler: {e}[/red]")
        all_data["errors"].append(f"Moodle: {e}")

    # 3. Display Results
    console.print("\n" + "=" * 60)
    console.print("[bold]📊 ERGEBNISSE[/bold]\n")

    # Student Info
    student = all_data["graphql"].get("student", {})
    if student:
        console.print(
            f"[bold]Student:[/bold] {student.get('vorname', '')} {student.get('nachname', '')}"
        )
        console.print(f"[dim]Email: {student.get('emailKh', '')}[/dim]")

    # Grade Overview
    overview = all_data["graphql"].get("gradeOverview", {})
    if overview:
        console.print(f"\n[bold]Semester:[/bold] {overview.get('currentSemester', '?')}")
        console.print(f"[bold]Notendurchschnitt:[/bold] {overview.get('grade', '-')}")
        console.print(f"[bold]ECTS:[/bold] {overview.get('eCTS', 0)}")

        modules = overview.get("modules", [])

        # Klausuren
        klausuren = [m for m in modules if m.get("pruefungsform") == "Klausur"]
        if klausuren:
            console.print("\n")
            table = Table(title="📝 KLAUSUREN", title_style="bold magenta")
            table.add_column("Modul", style="bold", max_width=45)
            table.add_column("Sem.", justify="center")
            table.add_column("Status")
            table.add_column("Note", justify="right")

            for m in sorted(klausuren, key=lambda x: x.get("semester", 99)):
                status = m.get("examStatus") or "offen"
                status_fmt = {
                    "bestanden": "[green]✓[/green]",
                    "nicht bestanden": "[red]✗[/red]",
                    "angemeldet": "[blue]●[/blue]",
                    "abgemeldet": "[yellow]○[/yellow]",
                }.get(status, "[dim]○[/dim]")
                table.add_row(
                    m.get("modulbezeichnung", "?")[:45].strip(),
                    str(m.get("semester", "?")),
                    status_fmt,
                    str(m.get("note") or "-"),
                )
            console.print(table)

        # Alle Module (ungecapped)
        console.print("\n")
        table = Table(title="📚 ALLE MODULE", title_style="bold")
        table.add_column("Modul", style="bold", max_width=40)
        table.add_column("Sem.", justify="center")
        table.add_column("Prüfungsform", max_width=15)
        table.add_column("Status")
        table.add_column("ECTS", justify="right")

        display_modules = modules if limit == 0 else modules[:limit]
        for m in sorted(
            display_modules, key=lambda x: (x.get("semester", 99), x.get("modulbezeichnung", ""))
        ):
            status = m.get("examStatus") or "-"
            status_fmt = {
                "bestanden": "[green]bestanden[/green]",
                "nicht bestanden": "[red]nicht best.[/red]",
                "angemeldet": "[blue]ANGEMELDET[/blue]",
                "abgemeldet": "[yellow]abgemeldet[/yellow]",
                "anerkannt": "[cyan]anerkannt[/cyan]",
            }.get(status, f"[dim]{status}[/dim]")
            table.add_row(
                m.get("modulbezeichnung", "?")[:40].strip(),
                str(m.get("semester", "?")),
                (m.get("pruefungsform") or "?")[:15],
                status_fmt,
                str(m.get("eCTS", 0)),
            )
        console.print(table)

        # Summary
        bestanden = [m for m in modules if m.get("examStatus") == "bestanden"]
        anerkannt = [m for m in modules if m.get("examStatus") == "anerkannt"]
        nicht_bestanden = [m for m in modules if m.get("examStatus") == "nicht bestanden"]
        angemeldet = [m for m in modules if m.get("examStatus") == "angemeldet"]
        abgemeldet = [m for m in modules if m.get("examStatus") == "abgemeldet"]
        offen = [
            m
            for m in modules
            if m.get("examStatus") is None and m.get("pruefungsform") != "Anerkennung"
        ]

        summary = f"""
[green]✓ Bestanden:[/green] {len(bestanden)} ({sum(m.get("eCTS", 0) for m in bestanden):.0f} ECTS)
[cyan]✓ Anerkannt:[/cyan] {len(anerkannt)} ({sum(m.get("eCTS", 0) for m in anerkannt):.0f} ECTS)
[red]✗ Nicht bestanden:[/red] {len(nicht_bestanden)}
[blue]● Angemeldet:[/blue] {len(angemeldet)}
[yellow]○ Abgemeldet:[/yellow] {len(abgemeldet)}
[dim]○ Offen:[/dim] {len(offen)} ({sum(m.get("eCTS", 0) for m in offen):.0f} ECTS)
        """
        console.print(Panel(summary.strip(), title="Zusammenfassung", border_style="cyan"))

    # Moodle Events (all, ungecapped)
    moodle_events = all_data["moodle"].get("events", [])
    if moodle_events:
        console.print("\n")
        table = Table(title="📅 ALLE TERMINE (Moodle Kalender)", title_style="bold green")
        table.add_column("Event", style="bold", max_width=45)
        table.add_column("Datum/Zeit", style="cyan")
        table.add_column("Kurs", style="dim", max_width=20)

        display_events = moodle_events if limit == 0 else moodle_events[:limit]
        for event in display_events:
            table.add_row(
                (event.get("title") or "?")[:45],
                event.get("start_time") or "?",
                (event.get("course_name") or "")[:20],
            )
        console.print(table)

    # Courses
    courses = all_data["moodle"].get("courses", [])
    if courses:
        console.print("\n")
        table = Table(title="📖 EINGESCHRIEBENE KURSE", title_style="bold blue")
        table.add_column("Kurs", style="bold")
        table.add_column("ID", style="dim")

        for course in courses[:20]:  # Show first 20
            table.add_row(course.get("name", "?")[:60], course.get("id", "?"))
        if len(courses) > 20:
            console.print(f"[dim]  ... und {len(courses) - 20} weitere Kurse[/dim]")
        console.print(table)

    # Errors
    if all_data["errors"]:
        console.print("\n[yellow]⚠ Fehler während des Fetchs:[/yellow]")
        for err in all_data["errors"]:
            console.print(f"  [red]• {err}[/red]")

    # Save to file if requested
    if output:
        output_path = Path(output)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        with output_path.open("w", encoding="utf-8") as f:
            json.dump(all_data, f, indent=2, ensure_ascii=False, default=str)
        console.print(f"\n[green]✓ Daten exportiert nach: {output_path}[/green]")

    console.print("\n[dim]Datenquelle: Live Online-Abfrage[/dim]")


@app.command("get-token")
def get_graphql_token_auto(
    headless: bool = typer.Option(
        False,
        "--headless",
        help="Run browser in headless mode (may not work with MFA)",
    ),
    timeout: int = typer.Option(
        120,
        "--timeout",
        "-t",
        help="Timeout in seconds for login completion",
    ),
) -> None:
    """
    Automatically extract GraphQL Bearer token via browser.

    Opens a browser, logs into cms.kolping-hochschule.de, and captures
    the Bearer token from network requests.

    The token is required for accessing the "Mein Studium" GraphQL API.
    """
    from kolping_cockpit.settings import store_secret

    console.print("[bold cyan]🔑 GraphQL Token Extraktion[/bold cyan]")
    console.print("=" * 50)
    console.print()
    console.print("Dieser Befehl öffnet einen Browser und loggt automatisch ein.")
    console.print("Nach erfolgreicher Anmeldung wird der GraphQL Token extrahiert.")
    console.print()

    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        console.print("[red]✗ Playwright nicht installiert![/red]")
        console.print("  Installiere mit: pip install playwright && playwright install chromium")
        raise typer.Exit(code=1)

    captured_token: str | None = None
    target_audience = "api://b3d6dbac-7f13-4032-9e12-c0aae5910e20"

    def handle_request(request):
        """Capture Authorization headers from GraphQL requests."""
        nonlocal captured_token
        url = request.url

        # Look for GraphQL requests or any request with Bearer token
        auth_header = request.headers.get("authorization", "")
        if auth_header.startswith("Bearer ") and "graphql" in url.lower():
            token = auth_header[7:]  # Remove "Bearer " prefix
            # Verify it's the correct token by checking audience
            try:
                import base64
                import json as json_mod

                # Decode JWT payload (middle part)
                parts = token.split(".")
                if len(parts) >= 2:
                    # Add padding if needed
                    payload_b64 = parts[1]
                    padding = 4 - len(payload_b64) % 4
                    if padding != 4:
                        payload_b64 += "=" * padding
                    payload = json_mod.loads(base64.urlsafe_b64decode(payload_b64))
                    aud = payload.get("aud", "")
                    if aud == target_audience:
                        captured_token = token
                        console.print("[green]✓ Token mit korrekter Audience gefunden![/green]")
                        console.print(f"  [dim]aud: {aud}[/dim]")
            except Exception:
                pass  # Ignore decode errors

    console.print("[yellow]Starte Browser...[/yellow]")

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=headless)
        context = browser.new_context(
            viewport={"width": 1280, "height": 800},
        )
        page = context.new_page()

        # Intercept all requests to capture Authorization headers
        page.on("request", handle_request)

        try:
            # Navigate to CMS which hosts the "Mein Studium" app
            cms_url = "https://cms.kolping-hochschule.de/"
            console.print(f"[cyan]Navigiere zu: {cms_url}[/cyan]")
            page.goto(cms_url, timeout=30000)

            console.print()
            console.print("[bold yellow]⚡ AKTION ERFORDERLICH:[/bold yellow]")
            console.print("1. Logge dich im Browser ein (Microsoft SSO)")
            console.print("2. Navigiere zu 'Mein Studium' wenn nötig")
            console.print("3. Warte bis die Seite vollständig geladen ist")
            console.print()
            console.print(f"[dim]Warte max. {timeout} Sekunden auf Token...[/dim]")

            # Wait for token to be captured or timeout
            import time

            start_time = time.time()
            while not captured_token and (time.time() - start_time) < timeout:
                # Check if we're on a page that might have GraphQL
                current_url = page.url
                if "khs-meinstudium" in current_url or "mein-studium" in current_url.lower():
                    console.print(f"[green]✓ Auf Mein Studium Seite: {current_url}[/green]")

                    # Try to trigger a GraphQL request by navigating/refreshing
                    page.reload()
                    time.sleep(3)

                time.sleep(1)

            if captured_token:
                # Store the token
                success = store_secret("graphql_bearer_token", captured_token)
                if success:
                    console.print()
                    console.print(
                        "[bold green]✓ Token erfolgreich extrahiert und gespeichert![/bold green]"
                    )
                    console.print()
                    # Show token preview
                    console.print(
                        f"[dim]Token (gekürzt): {captured_token[:50]}...{captured_token[-20:]}[/dim]"
                    )
                else:
                    console.print("[red]✗ Konnte Token nicht speichern[/red]")
                    raise typer.Exit(code=1)
            else:
                console.print()
                console.print("[red]✗ Kein Token gefunden![/red]")
                console.print()
                console.print("[yellow]Mögliche Ursachen:[/yellow]")
                console.print("  • Login nicht abgeschlossen")
                console.print("  • Nicht zu 'Mein Studium' navigiert")
                console.print("  • Timeout zu kurz (--timeout erhöhen)")
                console.print()
                console.print("[cyan]Alternative: Manuell Token setzen[/cyan]")
                console.print("  kolping set-graphql")
                raise typer.Exit(code=1)

        except Exception as e:
            error_msg = str(e)
            if "Timeout" in error_msg:
                console.print("[red]✗ Timeout - Seite hat zu lange gebraucht[/red]")
            else:
                console.print(f"[red]✗ Fehler: {error_msg}[/red]")
            raise typer.Exit(code=1)

        finally:
            context.close()
            browser.close()


@app.command("exams")
def show_comprehensive_exams(
    semester: int = typer.Option(
        None, "--semester", "-s", help="Filter by specific semester number (default: current semester)"
    ),
    include_completed: bool = typer.Option(
        False, "--completed", "-c", help="Include completed modules"
    ),
    analyze_endpoints: bool = typer.Option(
        False, "--analyze", "-a", help="First analyze all available GraphQL endpoints"
    ),
) -> None:
    """
    Comprehensive exam dates and requirements overview.

    Fetches and displays:
    - All exam dates for registered modules this semester
    - Required assessment types for each module (Klausur, Lerntagebuch, etc.)
    - Links to Moodle courses and materials
    - Upcoming deadlines from Moodle calendar
    - What you need to do for each module

    If --analyze is set, first analyzes all available GraphQL endpoints.

    Example:
        kolping exams
        kolping exams --semester 3
        kolping exams --analyze
    """

    console.print("[bold cyan]📚 Kolping Study Cockpit - Prüfungstermine & Leistungsübersicht[/bold cyan]")
    console.print("=" * 70)

    # Step 1: Analyze endpoints if requested
    if analyze_endpoints:
        console.print("\n[bold yellow]🔍 SCHRITT 1: Analyse aller verfügbaren Endpunkte[/bold yellow]")
        console.print("[dim]Teste alle bekannten GraphQL Queries...[/dim]\n")

        try:
            from kolping_cockpit.graphql_client import KolpingGraphQLClient

            with KolpingGraphQLClient() as client:
                if not client.is_authenticated:
                    console.print("[red]✗ Kein Bearer Token konfiguriert[/red]")
                    console.print("[dim]  Setze Token mit: kolping set-graphql[/dim]")
                else:
                    # Test each available query
                    test_queries = [
                        "myStudentData",
                        "myStudentGradeOverview",
                        "moduls",
                        "semesters",
                        "pruefungs",
                        "studiengangs",
                        "matchModulStudent",
                    ]

                    table = Table(title="GraphQL Endpoint Analyse")
                    table.add_column("Query", style="cyan")
                    table.add_column("Status", style="magenta")
                    table.add_column("Ergebnis")

                    for query_name in test_queries:
                        try:
                            response = client.execute_named_query(query_name, simple=True)
                            if response.has_errors:
                                status = "[yellow]⚠[/yellow]"
                                result = f"Fehler: {response.errors[0].message if response.errors else 'Unknown'}"
                            elif response.data:
                                # Count results
                                data_key = list(response.data.keys())[0] if response.data else None
                                if data_key:
                                    data_val = response.data[data_key]
                                    if isinstance(data_val, list):
                                        result = f"{len(data_val)} Einträge"
                                    elif isinstance(data_val, dict):
                                        result = f"{len(data_val)} Felder"
                                    else:
                                        result = "Daten vorhanden"
                                    status = "[green]✓[/green]"
                                else:
                                    status = "[yellow]○[/yellow]"
                                    result = "Keine Daten"
                            else:
                                status = "[yellow]○[/yellow]"
                                result = "Leer"
                            table.add_row(query_name, status, result)
                        except Exception as e:
                            table.add_row(query_name, "[red]✗[/red]", str(e)[:50])

                    console.print(table)
                    console.print()
        except Exception as e:
            console.print(f"[red]✗ Analyse fehlgeschlagen: {e}[/red]\n")

    # Step 2: Fetch comprehensive exam data
    console.print("[bold yellow]🎓 SCHRITT 2: Lade Prüfungsdaten und Modulübersicht[/bold yellow]\n")

    grade_data = None
    exam_dates = []
    all_modules = []
    enrolled_modules = []
    calendar_events = []
    moodle_courses = []
    errors = []

    # Fetch from GraphQL
    console.print("[dim]Lade GraphQL Daten...[/dim]")
    try:
        from kolping_cockpit.graphql_client import KolpingGraphQLClient

        with KolpingGraphQLClient() as client:
            if not client.is_authenticated:
                errors.append("GraphQL: Kein Bearer Token konfiguriert")
            else:
                success, _ = client.test_connection()
                if not success:
                    errors.append("GraphQL: Verbindung fehlgeschlagen")
                else:
                    # Get grade overview (includes all modules with status)
                    response = client.execute_named_query("myStudentGradeOverview")
                    if response.data and "myStudentGradeOverview" in response.data:
                        grade_data = response.data["myStudentGradeOverview"]
                        console.print("[green]✓ Prüfungsübersicht geladen[/green]")

                    # Get exam dates
                    response = client.execute_named_query("pruefungs", simple=True)
                    if response.data and "pruefungs" in response.data:
                        exam_dates = response.data["pruefungs"]
                        console.print(f"[green]✓ {len(exam_dates)} Prüfungstermine gefunden[/green]")

                    # Get all modules
                    response = client.execute_named_query("moduls", simple=True)
                    if response.data and "moduls" in response.data:
                        all_modules = response.data["moduls"]
                        console.print(f"[green]✓ {len(all_modules)} Module geladen[/green]")

                    # Get enrolled modules
                    response = client.execute_named_query("matchModulStudent", simple=True)
                    if response.data and "matchModulStudent" in response.data:
                        enrolled_modules = response.data["matchModulStudent"]
                        console.print(f"[green]✓ {len(enrolled_modules)} Einschreibungen gefunden[/green]")
    except Exception as e:
        errors.append(f"GraphQL: {e}")
        console.print(f"[red]✗ GraphQL Fehler: {e}[/red]")

    # Fetch from Moodle
    console.print("[dim]Lade Moodle Daten...[/dim]")
    try:
        from kolping_cockpit.moodle_client import KolpingMoodleClient

        with KolpingMoodleClient() as client:
            if not client.is_authenticated:
                errors.append("Moodle: Keine Session konfiguriert")
            else:
                is_valid, _ = client.test_session()
                if not is_valid:
                    errors.append("Moodle: Session abgelaufen")
                else:
                    # Get calendar events
                    calendar_events = client.get_upcoming_deadlines()
                    console.print(f"[green]✓ {len(calendar_events)} Kalender-Events geladen[/green]")

                    # Get courses
                    moodle_courses = client.get_courses()
                    console.print(f"[green]✓ {len(moodle_courses)} Kurse geladen[/green]")
    except Exception as e:
        errors.append(f"Moodle: {e}")
        console.print(f"[red]✗ Moodle Fehler: {e}[/red]")

    if errors:
        console.print("\n[yellow]⚠ Einige Datenquellen nicht verfügbar:[/yellow]")
        for err in errors:
            console.print(f"  [dim]{err}[/dim]")

    console.print()

    # Step 3: Display comprehensive overview
    if grade_data:
        current_sem = grade_data.get("currentSemester", "Unbekannt")
        current_sem_num = int(current_sem.split()[0]) if current_sem and current_sem[0].isdigit() else None

        console.print(f"[bold]📊 Aktuelles Semester:[/bold] {current_sem}")
        console.print(f"[bold]Notendurchschnitt:[/bold] {grade_data.get('grade', '-')}")
        console.print(f"[bold]Erreichte ECTS:[/bold] {grade_data.get('eCTS', 0)}")

        modules = grade_data.get("modules", [])

        # Filter by semester if specified
        if semester:
            modules = [m for m in modules if m.get("semester") == semester]
            display_semester = semester
        elif current_sem_num:
            display_semester = current_sem_num
        else:
            display_semester = None

        # Categorize modules
        angemeldet = [m for m in modules if m.get("examStatus") == "angemeldet"]
        offen = [
            m for m in modules
            if m.get("examStatus") is None and m.get("pruefungsform") != "Anerkennung"
        ]

        # Filter open modules to current semester range if not explicitly set
        if not semester and display_semester:
            offen = [m for m in offen if m.get("semester", 0) <= display_semester]

        # Show registered exams with dates
        if angemeldet:
            console.print("\n")
            table = Table(
                title="🔴 ANGEMELDETE PRÜFUNGEN MIT TERMINEN",
                title_style="bold red",
                border_style="red",
            )
            table.add_column("Modul", style="bold", max_width=40)
            table.add_column("Sem.", justify="center", width=5)
            table.add_column("Prüfungsform", style="cyan", max_width=15)
            table.add_column("ECTS", justify="right", width=5)
            table.add_column("Termin", style="yellow", max_width=25)

            for m in sorted(angemeldet, key=lambda x: x.get("semester", 99)):
                modul_id = m.get("modulId")
                modul_name = m.get("modulbezeichnung", "?")[:40]

                # Find exam date for this module
                exam_date = "Siehe Kalender"
                if modul_id and exam_dates:
                    matching_exam = next((e for e in exam_dates if str(e.get("modulId")) == str(modul_id)), None)
                    if matching_exam:
                        datum = matching_exam.get("datum", "")
                        uhrzeit = matching_exam.get("uhrzeit", "")
                        raum = matching_exam.get("raum", "")
                        if datum:
                            exam_date = f"{datum}"
                            if uhrzeit:
                                exam_date += f" {uhrzeit}"
                            if raum:
                                exam_date += f" ({raum})"

                table.add_row(
                    modul_name,
                    str(m.get("semester", "?")),
                    m.get("pruefungsform", "?")[:15],
                    str(m.get("eCTS", 0)),
                    exam_date[:25],
                )
            console.print(table)

            # Show what's needed for each exam
            console.print("\n[bold]📋 Was du für die angemeldeten Prüfungen brauchst:[/bold]\n")
            for m in angemeldet:
                pruefungsform = m.get("pruefungsform", "Unbekannt")
                modul_name = m.get("modulbezeichnung", "Unbekannt")

                requirements = _get_requirements_for_pruefungsform(pruefungsform)

                console.print(Panel(
                    f"[bold]{modul_name}[/bold]\n"
                    f"[cyan]Prüfungsform:[/cyan] {pruefungsform}\n"
                    f"[yellow]Erforderlich:[/yellow]\n{requirements}",
                    border_style="blue"
                ))

        # Show open modules with requirements
        if offen:
            console.print("\n")
            table = Table(
                title=f"📝 OFFENE MODULE{f' (Semester {display_semester})' if display_semester else ''}",
                title_style="bold",
            )
            table.add_column("Modul", style="bold", max_width=40)
            table.add_column("Sem.", justify="center", width=5)
            table.add_column("Prüfungsform", style="cyan", max_width=20)
            table.add_column("ECTS", justify="right", width=5)
            table.add_column("Moodle Kurs", style="dim", max_width=15)

            for m in sorted(offen, key=lambda x: x.get("semester", 99)):
                modul_name = m.get("modulbezeichnung", "?")

                # Try to find matching Moodle course
                moodle_link = "–"
                if moodle_courses:
                    # Simple fuzzy match on course name
                    matching_course = next(
                        (
                            c
                            for c in moodle_courses
                            if modul_name[:COURSE_NAME_MATCH_LENGTH].lower() in c.name.lower()
                        ),
                        None,
                    )
                    if matching_course:
                        moodle_link = "✓ Verfügbar"

                table.add_row(
                    modul_name[:40],
                    str(m.get("semester", "?")),
                    m.get("pruefungsform", "?")[:20],
                    str(m.get("eCTS", 0)),
                    moodle_link[:15],
                )
            console.print(table)

            # Group by assessment type
            console.print("\n[bold]📚 Offene Module nach Prüfungsform gruppiert:[/bold]\n")

            pruefungsformen: dict[str, list] = {}
            for m in offen:
                pform = m.get("pruefungsform", "Unbekannt")
                if pform not in pruefungsformen:
                    pruefungsformen[pform] = []
                pruefungsformen[pform].append(m)

            for pform, modules_list in sorted(pruefungsformen.items()):
                count = len(modules_list)
                ects_sum = sum(m.get("eCTS", 0) for m in modules_list)
                requirements = _get_requirements_for_pruefungsform(pform)

                console.print(f"[bold cyan]{pform}[/bold cyan] ({count} Module, {ects_sum} ECTS)")
                console.print(f"[dim]{requirements}[/dim]")
                for mod in modules_list:
                    console.print(
                        f"  • {mod.get('modulbezeichnung', '?')[:MODULE_NAME_MAX_LENGTH]} "
                        f"(Sem. {mod.get('semester', '?')})"
                    )
                console.print()

    # Show calendar events with course links
    if calendar_events:
        console.print("\n")
        table = Table(title="📅 KOMMENDE TERMINE & DEADLINES (Moodle)")
        table.add_column("Event", style="bold", max_width=45)
        table.add_column("Datum/Zeit", style="cyan", max_width=25)
        table.add_column("Link", style="dim", max_width=10)

        for event in calendar_events[:15]:
            has_link = "✓" if event.url else "–"
            table.add_row(
                (event.title or "?")[:45],
                event.start_time or "?",
                has_link,
            )
        console.print(table)

    # Show Moodle courses with links
    if moodle_courses and len(moodle_courses) > 0:
        console.print("\n")
        table = Table(title="🔗 MOODLE KURSE & MATERIALIEN")
        table.add_column("Kurs", style="bold", max_width=50)
        table.add_column("Link", style="cyan", max_width=30)

        for course in moodle_courses[:20]:
            short_url = course.url[:30] + "..." if course.url and len(course.url) > 30 else (course.url or "")
            table.add_row(
                course.name[:50],
                short_url,
            )
        if len(moodle_courses) > 20:
            console.print(f"[dim]... und {len(moodle_courses) - 20} weitere Kurse[/dim]")
        console.print(table)

    # Final tips
    console.print("\n[bold cyan]💡 Nützliche Links:[/bold cyan]")
    console.print("  • Moodle Portal: https://portal.kolping-hochschule.de")
    console.print("  • Mein Studium: https://cms.kolping-hochschule.de")
    console.print("  • Kalender: https://portal.kolping-hochschule.de/calendar/view.php")
    console.print("\n[dim]Tipp: Verwende 'kolping export all' für vollständigen JSON-Export[/dim]")


def _get_requirements_for_pruefungsform(pruefungsform: str) -> str:
    """Get description of requirements for a given assessment type."""
    requirements_map = {
        "Klausur": "  • Schriftliche Prüfung im Prüfungszeitraum\n  • Anmeldung erforderlich\n  • Prüfungsvorbereitung empfohlen",
        "Lerntagebuch": "  • Regelmäßige Reflexion über Lernprozess\n  • Dokumentation in vorgegebenem Format\n  • Abgabe über Moodle",
        "Präsentation": "  • Vorbereitung einer Präsentation (10-20 Min.)\n  • Handout oder Folien\n  • Präsentation vor Kurs/Dozent",
        "Seminararbeit": "  • Schriftliche Ausarbeitung (10-15 Seiten)\n  • Wissenschaftliche Zitierweise\n  • Abgabe als PDF über Moodle",
        "E-Portfolio ": "  • Digitale Sammlung von Lernartefakten\n  • Reflexion über Lernfortschritt\n  • Online-Präsentation",
        "Mündliche Prüfung": "  • Terminvereinbarung mit Prüfer\n  • Vorbereitung auf Prüfungsgespräch\n  • Ca. 20-30 Minuten",
        "Anerkennung": "  • Nachweis über Praxisphase\n  • Bestätigung vom Arbeitgeber\n  • Einreichung über Studierendensekretariat",
        "Exposé": "  • Forschungsplan für Abschlussarbeit\n  • 3-5 Seiten\n  • Einreichung beim Betreuer",
        "Bachelorthesis & Kolloquium": "  • Wissenschaftliche Arbeit (40-60 Seiten)\n  • Kolloquium (30 Min. Verteidigung)\n  • Anmeldung und Themenfindung",
        "Praxistransferbericht": "  • Bericht über Praxisphase (10-15 Seiten)\n  • Reflexion der praktischen Tätigkeit\n  • Abgabe über Moodle",
    }

    return requirements_map.get(pruefungsform, "  • Details siehe Modulhandbuch\n  • Informationen auf Moodle")


@app.command("extract-token")
def extract_token_from_captures() -> None:
    """
    Extract GraphQL token from existing HTTP captures in docs/ folder.

    Use this if you have recent HAR/HTTP captures with a valid token.
    """
    import base64
    import json
    import re
    from pathlib import Path

    from kolping_cockpit.settings import store_secret

    console.print("[bold cyan]🔍 Token aus HTTP Captures extrahieren[/bold cyan]")
    console.print("=" * 50)

    docs_path = Path("/workspaces/kolping-study-cockpit/docs")
    target_audience = "api://b3d6dbac-7f13-4032-9e12-c0aae5910e20"

    found_tokens: list[tuple[str, str, str]] = []  # (token, aud, source)

    # Search all numbered directories
    for i in range(1, 100):
        dir_path = docs_path / str(i)
        if not dir_path.exists():
            continue

        # Check request.json for authorization header
        request_json = dir_path / "request.json"
        if request_json.exists():
            try:
                with request_json.open() as f:
                    data = json.load(f)
                    if isinstance(data, dict):
                        auth_header = data.get("authorization", "")
                        if auth_header.startswith("Bearer "):
                            token = auth_header[7:]  # Remove "Bearer " prefix
                            try:
                                parts = token.split(".")
                                if len(parts) >= 2:
                                    payload_b64 = parts[1]
                                    padding = 4 - len(payload_b64) % 4
                                    if padding != 4:
                                        payload_b64 += "=" * padding
                                    payload = json.loads(base64.urlsafe_b64decode(payload_b64))
                                    aud = payload.get("aud", "unknown")
                                    if not any(t[0] == token for t in found_tokens):
                                        found_tokens.append((token, aud, str(request_json)))
                            except Exception:
                                pass
            except Exception:
                pass

        # Also check request.txt and request.hcy files
        for filename in ["request.txt", "request.hcy"]:
            request_file = dir_path / filename
            if request_file.exists():
                content = request_file.read_text(errors="ignore")
                # Look for Authorization: Bearer xxx
                match = re.search(
                    r"[Aa]uthorization:\s*Bearer\s+([A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+)",
                    content,
                )
                if match:
                    token = match.group(1)
                    try:
                        parts = token.split(".")
                        if len(parts) >= 2:
                            payload_b64 = parts[1]
                            padding = 4 - len(payload_b64) % 4
                            if padding != 4:
                                payload_b64 += "=" * padding
                            payload = json.loads(base64.urlsafe_b64decode(payload_b64))
                            aud = payload.get("aud", "unknown")
                            if not any(t[0] == token for t in found_tokens):
                                found_tokens.append((token, aud, str(request_file)))
                    except Exception:
                        pass

    if not found_tokens:
        console.print("[red]✗ Keine Token in HTTP Captures gefunden[/red]")
        raise typer.Exit(code=1)

    console.print(f"\n[green]Gefundene Tokens: {len(found_tokens)}[/green]\n")

    # Find token with correct audience
    correct_token = None
    for token, aud, source in found_tokens:
        is_correct = aud == target_audience
        marker = "[bold green]✓ KORREKT[/bold green]" if is_correct else "[dim]falsche aud[/dim]"
        console.print(f"  {marker}")
        console.print(f"    [dim]Quelle: {source}[/dim]")
        console.print(f"    [dim]Audience: {aud}[/dim]")

        if is_correct:
            correct_token = token

    if correct_token:
        console.print()
        if typer.confirm("Token mit korrekter Audience gefunden. Speichern?"):
            success = store_secret("graphql_bearer_token", correct_token)
            if success:
                console.print("[bold green]✓ Token gespeichert![/bold green]")
            else:
                console.print("[red]✗ Speichern fehlgeschlagen[/red]")
    else:
        console.print()
        console.print("[yellow]⚠ Kein Token mit korrekter Audience gefunden.[/yellow]")
        console.print(f"  Benötigte Audience: {target_audience}")
        console.print()
        console.print("[cyan]Lösung: Neue HTTP Capture erstellen oder Browser-Login nutzen[/cyan]")
        console.print("  kolping get-token")


if __name__ == "__main__":
    app()
