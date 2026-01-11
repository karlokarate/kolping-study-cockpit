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


if __name__ == "__main__":
    app()
