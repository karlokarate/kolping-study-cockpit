"""Local connector using Playwright and keyring for secure credential management."""

from datetime import UTC
from typing import Any

import keyring
from playwright.sync_api import sync_playwright
from pydantic import BaseModel, Field


class ConnectorConfig(BaseModel):
    """Configuration for the local connector."""

    service_name: str = Field(default="kolping-cockpit", description="Keyring service name")
    username_key: str = Field(default="username", description="Username key in keyring")
    base_url: str = Field(
        default="https://example.com",
        description="Base URL for the target system",
    )
    timeout: int = Field(default=30000, description="Page timeout in milliseconds")


class LocalConnector:
    """
    Local connector that uses Playwright for browser automation.

    Credentials are securely stored and retrieved using the system keyring.
    """

    def __init__(
        self,
        config: ConnectorConfig | None = None,
        headless: bool = True,
    ) -> None:
        """
        Initialize the local connector.

        Args:
            config: Configuration object, uses defaults if not provided
            headless: Whether to run browser in headless mode
        """
        self.config = config or ConnectorConfig()
        self.headless = headless

    @classmethod
    def store_credentials(cls, username: str, password: str) -> None:
        """
        Store credentials securely in the system keyring.

        Args:
            username: Username to store
            password: Password to store
        """
        config = ConnectorConfig()
        # Store username
        keyring.set_password(config.service_name, config.username_key, username)
        # Store password
        keyring.set_password(config.service_name, username, password)

    def get_credentials(self) -> tuple[str, str]:
        """
        Retrieve credentials from the system keyring.

        Returns:
            Tuple of (username, password)

        Raises:
            ValueError: If credentials are not found
        """
        username = keyring.get_password(self.config.service_name, self.config.username_key)
        if not username:
            msg = "Username not found in keyring. Run 'kolping configure' first."
            raise ValueError(msg)

        password = keyring.get_password(self.config.service_name, username)
        if not password:
            msg = "Password not found in keyring. Run 'kolping configure' first."
            raise ValueError(msg)

        return username, password

    def export_data(self) -> dict[str, Any]:
        """
        Export data using Playwright browser automation.

        Returns:
            Dictionary containing exported data
        """
        username, password = self.get_credentials()

        with sync_playwright() as p:
            browser = p.chromium.launch(headless=self.headless)
            context = browser.new_context()
            page = context.new_page()

            try:
                # This is a placeholder implementation
                # In a real scenario, this would navigate to the actual system
                # and extract data

                page.set_default_timeout(self.config.timeout)

                # Example: Navigate to login page
                # page.goto(f"{self.config.base_url}/login")

                # Example: Login
                # page.fill("#username", username)
                # page.fill("#password", password)
                # page.click("#login-button")

                # Example: Navigate to data page
                # page.goto(f"{self.config.base_url}/data")

                # Example: Extract data
                # data = page.evaluate("() => window.dataObject")

                # For now, return mock data
                data = {
                    "version": "0.1.0",
                    "exported_at": self._get_timestamp(),
                    "user": username,
                    "records": [
                        {
                            "id": 1,
                            "title": "Sample Record",
                            "description": "This is a sample record",
                        }
                    ],
                }

                return data

            finally:
                context.close()
                browser.close()

    @staticmethod
    def _get_timestamp() -> str:
        """Get current timestamp in ISO format."""
        from datetime import datetime

        return datetime.now(UTC).isoformat()
