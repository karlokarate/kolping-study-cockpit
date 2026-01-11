"""Tests for the connector module."""

from unittest.mock import MagicMock, patch

import pytest

from kolping_cockpit.connector import ConnectorConfig, LocalConnector


def test_connector_config_defaults():
    """Test that ConnectorConfig has correct defaults."""
    config = ConnectorConfig()
    assert config.service_name == "kolping-cockpit"
    assert config.username_key == "username"
    assert config.timeout == 30000


def test_connector_initialization():
    """Test that LocalConnector initializes correctly."""
    connector = LocalConnector(headless=True)
    assert connector.headless is True
    assert connector.config.service_name == "kolping-cockpit"


def test_connector_initialization_with_config():
    """Test that LocalConnector accepts custom config."""
    config = ConnectorConfig(service_name="test-service")
    connector = LocalConnector(config=config)
    assert connector.config.service_name == "test-service"


@patch("kolping_cockpit.connector.keyring")
def test_store_credentials(mock_keyring):
    """Test that credentials are stored in keyring."""
    username = "testuser"
    password = "testpass"  # noqa: S105

    LocalConnector.store_credentials(username, password)

    assert mock_keyring.set_password.call_count == 2
    mock_keyring.set_password.assert_any_call("kolping-cockpit", "username", username)
    mock_keyring.set_password.assert_any_call("kolping-cockpit", username, password)


@patch("kolping_cockpit.connector.keyring")
def test_get_credentials_success(mock_keyring):
    """Test that credentials are retrieved from keyring."""
    mock_keyring.get_password.side_effect = ["testuser", "testpass"]

    connector = LocalConnector()
    username, password = connector.get_credentials()

    assert username == "testuser"
    assert password == "testpass"
    assert mock_keyring.get_password.call_count == 2


@patch("kolping_cockpit.connector.keyring")
def test_get_credentials_no_username(mock_keyring):
    """Test that ValueError is raised when username not found."""
    mock_keyring.get_password.return_value = None

    connector = LocalConnector()
    with pytest.raises(ValueError, match="Username not found in keyring"):
        connector.get_credentials()


@patch("kolping_cockpit.connector.keyring")
def test_get_credentials_no_password(mock_keyring):
    """Test that ValueError is raised when password not found."""
    mock_keyring.get_password.side_effect = ["testuser", None]

    connector = LocalConnector()
    with pytest.raises(ValueError, match="Password not found in keyring"):
        connector.get_credentials()


@patch("kolping_cockpit.connector.keyring")
@patch("kolping_cockpit.connector.sync_playwright")
def test_export_data(mock_playwright, mock_keyring):
    """Test that export_data returns expected structure."""
    # Mock credentials
    mock_keyring.get_password.side_effect = ["testuser", "testpass"]

    # Mock Playwright
    mock_browser = MagicMock()
    mock_context = MagicMock()
    mock_page = MagicMock()

    mock_playwright_instance = MagicMock()
    mock_playwright_instance.__enter__.return_value.chromium.launch.return_value = mock_browser
    mock_browser.new_context.return_value = mock_context
    mock_context.new_page.return_value = mock_page

    mock_playwright.return_value = mock_playwright_instance

    # Test export
    connector = LocalConnector()
    data = connector.export_data()

    assert "version" in data
    assert "exported_at" in data
    assert "user" in data
    assert data["user"] == "testuser"
    assert "records" in data
    assert isinstance(data["records"], list)


def test_get_timestamp():
    """Test that timestamp is in correct format."""
    timestamp = LocalConnector._get_timestamp()
    assert isinstance(timestamp, str)
    # Should be ISO format with timezone
    assert "T" in timestamp
    assert timestamp.endswith("+00:00") or timestamp.endswith("Z")
