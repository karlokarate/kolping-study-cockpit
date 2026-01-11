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


@app.command()
def export(
    output: str = typer.Option(
        "exports/data.json",
        "--output",
        "-o",
        help="Output JSON file path",
    ),
    headless: bool = typer.Option(
        True,
        "--headless/--headed",
        help="Run browser in headless mode",
    ),
) -> None:
    """
    Export study data to JSON using Playwright browser automation.

    Credentials are securely stored and retrieved using the system keyring.
    """
    from pathlib import Path

    from kolping_cockpit.connector import LocalConnector

    console.print("[bold cyan]Kolping Study Cockpit Exporter[/bold cyan]")
    console.print("=" * 50)

    try:
        connector = LocalConnector(headless=headless)
        console.print("[yellow]Connecting to source...[/yellow]")

        data = connector.export_data()

        output_path = Path(output)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        import json

        with output_path.open("w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)

        console.print(f"[green]✓ Data exported to: {output_path}[/green]")

        # Display summary
        table = Table(title="Export Summary")
        table.add_column("Metric", style="cyan")
        table.add_column("Value", style="magenta")
        table.add_row("Records", str(len(data.get("records", []))))
        table.add_row("Output File", str(output_path))
        console.print(table)

    except Exception as e:
        console.print(f"[red]✗ Error: {e}[/red]")
        raise typer.Exit(code=1) from e


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
    """
    from kolping_cockpit.auth import interactive_login

    console.print("[bold cyan]Kolping Study Cockpit - Interactive Login[/bold cyan]")
    console.print("=" * 50)
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


if __name__ == "__main__":
    app()
