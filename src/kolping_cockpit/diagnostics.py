"""Diagnostics module for checking connectivity and configuration."""

import os
from dataclasses import dataclass

import httpx
from rich.console import Console
from rich.table import Table


@dataclass
class DiagnosticResult:
    """Result of a diagnostic check."""

    name: str
    status: str  # "ok", "warning", "error"
    message: str
    details: dict | None = None


def redact_value(value: str | None, show_chars: int = 3) -> str:
    """Redact a sensitive value, showing only first/last chars."""
    if not value:
        return "[not set]"
    if len(value) <= show_chars * 2:
        return "***"
    return f"{value[:show_chars]}***{value[-show_chars:]}"


def redact_url(url: str) -> str:
    """Redact sensitive parameters from URL."""
    import re

    # Redact common sensitive params
    patterns = [
        (r"(state=)[^&]+", r"\1[REDACTED]"),
        (r"(nonce=)[^&]+", r"\1[REDACTED]"),
        (r"(code=)[^&]+", r"\1[REDACTED]"),
        (r"(token=)[^&]+", r"\1[REDACTED]"),
        (r"(session=)[^&]+", r"\1[REDACTED]"),
    ]
    for pattern, replacement in patterns:
        url = re.sub(pattern, replacement, url, flags=re.IGNORECASE)
    return url


async def check_secrets() -> DiagnosticResult:
    """Check if required secrets are configured."""
    secrets = {
        "KOLPING_USERNAME": os.environ.get("KOLPING_USERNAME"),
        "KOLPING_PASSWORD": os.environ.get("KOLPING_PASSWORD"),
        "KOLPING_CLIENT_ID": os.environ.get("KOLPING_CLIENT_ID"),
    }

    missing = [k for k, v in secrets.items() if not v]
    configured = [k for k, v in secrets.items() if v]

    if not missing:
        return DiagnosticResult(
            name="Secrets",
            status="ok",
            message="All secrets configured",
            details={k: redact_value(v) for k, v in secrets.items()},
        )
    elif configured:
        return DiagnosticResult(
            name="Secrets",
            status="warning",
            message=f"Missing: {', '.join(missing)}",
            details={k: redact_value(v) for k, v in secrets.items()},
        )
    else:
        return DiagnosticResult(
            name="Secrets",
            status="error",
            message="No secrets configured",
            details=dict.fromkeys(secrets, "[not set]"),
        )


async def check_moodle_redirect(verbose: bool = False) -> DiagnosticResult:
    """Check Moodle portal redirect chain."""
    from kolping_cockpit.settings import get_settings

    settings = get_settings()
    url = settings.moodle_login_url

    redirects: list[dict] = []

    try:
        async with httpx.AsyncClient(follow_redirects=False, timeout=10.0) as client:
            current_url = url
            max_redirects = 10

            for i in range(max_redirects):
                response = await client.get(current_url)
                redirect_info = {
                    "step": i + 1,
                    "url": redact_url(current_url),
                    "status": response.status_code,
                }

                # Check for redirect headers
                if "location" in response.headers:
                    redirect_info["redirect_to"] = redact_url(response.headers["location"])

                # Check for Moodle header
                if "x-redirect-by" in response.headers:
                    redirect_info["x-redirect-by"] = response.headers["x-redirect-by"]

                redirects.append(redirect_info)

                # Follow redirect
                if response.status_code in (301, 302, 303, 307, 308):
                    current_url = response.headers.get("location", "")
                    if not current_url:
                        break
                    # Handle relative URLs
                    if current_url.startswith("/"):
                        current_url = f"{settings.moodle_base_url}{current_url}"
                else:
                    break

        # Check if we hit Microsoft login
        hit_microsoft = any(
            "login.microsoftonline.com" in r.get("redirect_to", "")
            or "login.microsoftonline.com" in r.get("url", "")
            for r in redirects
        )

        if hit_microsoft:
            return DiagnosticResult(
                name="Moodle Portal",
                status="ok",
                message=f"Redirect chain OK → Microsoft Entra ({len(redirects)} hops)",
                details={"redirects": redirects} if verbose else None,
            )
        else:
            return DiagnosticResult(
                name="Moodle Portal",
                status="warning",
                message=f"Unexpected redirect chain ({len(redirects)} hops)",
                details={"redirects": redirects},
            )

    except httpx.TimeoutException:
        return DiagnosticResult(
            name="Moodle Portal",
            status="error",
            message="Connection timeout",
        )
    except Exception as e:
        return DiagnosticResult(
            name="Moodle Portal",
            status="error",
            message=f"Connection failed: {e!s}",
        )


async def check_graphql_endpoint(verbose: bool = False) -> DiagnosticResult:
    """Check GraphQL endpoint availability."""
    from kolping_cockpit.settings import get_settings

    settings = get_settings()
    url = settings.graphql_endpoint

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            # Try introspection query (may fail without auth, but shows endpoint is up)
            response = await client.post(
                url,
                json={"query": "{ __typename }"},
                headers={"Content-Type": "application/json"},
            )

            if response.status_code == 200:
                return DiagnosticResult(
                    name="GraphQL API",
                    status="ok",
                    message=f"Endpoint reachable ({url})",
                    details={"response": response.json()} if verbose else None,
                )
            elif response.status_code == 401:
                return DiagnosticResult(
                    name="GraphQL API",
                    status="warning",
                    message="Endpoint reachable (auth required)",
                    details={"status": 401, "url": url},
                )
            else:
                return DiagnosticResult(
                    name="GraphQL API",
                    status="warning",
                    message=f"Unexpected status: {response.status_code}",
                    details={"status": response.status_code, "url": url},
                )

    except httpx.TimeoutException:
        return DiagnosticResult(
            name="GraphQL API",
            status="error",
            message="Connection timeout",
        )
    except Exception as e:
        return DiagnosticResult(
            name="GraphQL API",
            status="error",
            message=f"Connection failed: {e!s}",
        )


async def check_keyring() -> DiagnosticResult:
    """Check if keyring backend is available."""
    try:
        import keyring

        backend = keyring.get_keyring()
        backend_name = type(backend).__name__

        # Check if it's a usable backend
        if "Fail" in backend_name or "Null" in backend_name:
            return DiagnosticResult(
                name="Keyring",
                status="warning",
                message=f"No secure backend ({backend_name})",
                details={"backend": backend_name},
            )

        return DiagnosticResult(
            name="Keyring",
            status="ok",
            message=f"Backend: {backend_name}",
            details={"backend": backend_name},
        )

    except Exception as e:
        return DiagnosticResult(
            name="Keyring",
            status="error",
            message=f"Not available: {e!s}",
        )


async def run_diagnostics(console: Console, verbose: bool = False) -> None:
    """Run all diagnostic checks and display results."""
    checks = [
        ("Checking secrets...", check_secrets()),
        ("Checking keyring...", check_keyring()),
        ("Checking Moodle portal...", check_moodle_redirect(verbose)),
        ("Checking GraphQL API...", check_graphql_endpoint(verbose)),
    ]

    results: list[DiagnosticResult] = []

    for description, coro in checks:
        console.print(f"[dim]{description}[/dim]")
        result = await coro
        results.append(result)

    console.print()

    # Display results table
    table = Table(title="Diagnostic Results")
    table.add_column("Check", style="cyan")
    table.add_column("Status")
    table.add_column("Details", style="dim")

    status_icons = {
        "ok": "[green]✓ OK[/green]",
        "warning": "[yellow]⚠ Warning[/yellow]",
        "error": "[red]✗ Error[/red]",
    }

    for result in results:
        table.add_row(
            result.name,
            status_icons.get(result.status, result.status),
            result.message,
        )

    console.print(table)

    # Show verbose details if requested
    if verbose:
        console.print("\n[bold]Detailed Results:[/bold]")
        for result in results:
            if result.details:
                console.print(f"\n[cyan]{result.name}:[/cyan]")
                for key, value in result.details.items():
                    if isinstance(value, list):
                        console.print(f"  {key}:")
                        for item in value:
                            console.print(f"    {item}")
                    else:
                        console.print(f"  {key}: {value}")

    # Summary
    errors = sum(1 for r in results if r.status == "error")
    warnings = sum(1 for r in results if r.status == "warning")

    console.print()
    if errors:
        console.print(f"[red]✗ {errors} error(s) found[/red]")
    elif warnings:
        console.print(f"[yellow]⚠ {warnings} warning(s) found[/yellow]")
    else:
        console.print("[green]✓ All checks passed[/green]")
