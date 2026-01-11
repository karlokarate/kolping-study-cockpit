"""Environment and configuration management for Kolping Study Cockpit."""

import os
from functools import lru_cache
from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class KolpingSettings(BaseSettings):
    """
    Application settings loaded from environment variables and .env file.

    Priority order (highest to lowest):
    1. Environment variables
    2. .env file
    3. Default values
    """

    model_config = SettingsConfigDict(
        env_prefix="KOLPING_",
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # Authentication
    username: str | None = Field(default=None, description="Kolping-Hochschule login email")
    password: str | None = Field(default=None, description="Kolping-Hochschule password (prefer keyring)")
    client_id: str = Field(
        default="e27e57f4-abba-4475-b507-389e1e48e282",
        description="Microsoft Entra client ID",
    )

    # Endpoints
    moodle_base_url: str = Field(
        default="https://portal.kolping-hochschule.de",
        description="Moodle portal base URL",
    )
    graphql_endpoint: str = Field(
        default="https://app-kolping-prod-gateway.azurewebsites.net/graphql",
        description="GraphQL API endpoint",
    )

    # Microsoft Entra (Azure AD) - client_id now from env KOLPING_CLIENT_ID
    entra_client_id: str = Field(
        default="e27e57f4-abba-4475-b507-389e1e48e282",
        alias="client_id",
        description="Microsoft Entra client ID (alias for client_id)",
    )
    entra_tenant: str = Field(
        default="kolpinghochschule.onmicrosoft.com",
        description="Microsoft Entra tenant",
    )

    # Runtime configuration
    export_dir: Path = Field(
        default=Path("exports"),
        description="Directory for exported data",
    )
    headless: bool = Field(
        default=False,
        description="Run browser in headless mode (False for interactive login)",
    )
    log_level: str = Field(
        default="INFO",
        description="Logging level",
    )

    # Token storage (set after login, not in .env)
    moodle_session: str | None = Field(default=None, description="Moodle session cookie")
    graphql_bearer_token: str | None = Field(default=None, description="GraphQL bearer token")

    @property
    def entra_authorize_url(self) -> str:
        """Get the Microsoft Entra authorization URL."""
        return f"https://login.microsoftonline.com/{self.entra_tenant}/oauth2/authorize"

    @property
    def moodle_login_url(self) -> str:
        """Get the Moodle login entry point."""
        return f"{self.moodle_base_url}/my/"

    @property
    def moodle_oidc_callback(self) -> str:
        """Get the Moodle OIDC callback URL."""
        return f"{self.moodle_base_url}/auth/oidc/"

    def get_export_path(self, filename: str) -> Path:
        """
        Get the full export path with date-based subdirectory.

        Args:
            filename: Name of the export file

        Returns:
            Full path like exports/2026-01-11/filename.json
        """
        from datetime import UTC, datetime

        date_str = datetime.now(UTC).strftime("%Y-%m-%d")
        export_path = self.export_dir / date_str
        export_path.mkdir(parents=True, exist_ok=True)
        return export_path / filename


@lru_cache
def get_settings() -> KolpingSettings:
    """
    Get cached application settings.

    Returns:
        KolpingSettings instance (cached for performance)
    """
    return KolpingSettings()


def get_secret_from_env_or_keyring(key: str, service: str = "kolping-cockpit") -> str | None:
    """
    Get a secret from environment variable or keyring.

    Priority:
    1. Environment variable (for Codespaces/CI)
    2. System keyring (for local development)

    Args:
        key: The key name (e.g., "username", "moodle_session")
        service: Keyring service name

    Returns:
        The secret value or None if not found
    """
    # Check environment first (Codespaces, CI)
    env_key = f"KOLPING_{key.upper()}"
    env_value = os.environ.get(env_key)
    if env_value:
        return env_value

    # Fall back to keyring
    try:
        import keyring

        return keyring.get_password(service, key)
    except Exception:
        return None


def store_secret(key: str, value: str, service: str = "kolping-cockpit") -> bool:
    """
    Store a secret in the system keyring.

    Args:
        key: The key name
        value: The secret value
        service: Keyring service name

    Returns:
        True if successful, False otherwise
    """
    try:
        import keyring

        keyring.set_password(service, key, value)
        return True
    except Exception:
        return False


def delete_secret(key: str, service: str = "kolping-cockpit") -> bool:
    """
    Delete a secret from the system keyring.

    Args:
        key: The key name
        service: Keyring service name

    Returns:
        True if successful, False otherwise
    """
    try:
        import keyring

        keyring.delete_password(service, key)
        return True
    except Exception:
        return False
