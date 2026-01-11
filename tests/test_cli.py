"""Tests for the CLI module."""

from unittest.mock import MagicMock, patch

from typer.testing import CliRunner

from kolping_cockpit.cli import app

runner = CliRunner()


def test_version_command():
    """Test that version command shows version."""
    result = runner.invoke(app, ["version"])
    assert result.exit_code == 0
    assert "0.1.0" in result.stdout


@patch("kolping_cockpit.connector.LocalConnector.store_credentials")
def test_configure_command(mock_store):
    """Test that configure command stores credentials."""
    result = runner.invoke(
        app,
        ["configure"],
        input="testuser\ntestpass\n",
    )

    assert result.exit_code == 0
    mock_store.assert_called_once_with("testuser", "testpass")
    assert "stored securely" in result.stdout.lower()


@patch("kolping_cockpit.connector.LocalConnector.store_credentials")
def test_configure_command_error(mock_store):
    """Test that configure command handles errors."""
    mock_store.side_effect = Exception("Keyring error")

    result = runner.invoke(
        app,
        ["configure"],
        input="testuser\ntestpass\n",
    )

    assert result.exit_code == 1
    assert "error" in result.stdout.lower()


@patch("kolping_cockpit.connector.LocalConnector")
def test_export_command_success(mock_connector_class, tmp_path):
    """Test successful export command."""
    # Setup mock connector
    mock_connector = MagicMock()
    mock_connector.export_data.return_value = {"records": [{"id": 1, "title": "Test"}]}
    mock_connector_class.return_value = mock_connector

    output_file = tmp_path / "test.json"
    result = runner.invoke(app, ["export", "--output", str(output_file)])

    assert result.exit_code == 0
    mock_connector.export_data.assert_called_once()


@patch("kolping_cockpit.connector.LocalConnector")
def test_export_command_error(mock_connector_class):
    """Test export command handles errors."""
    mock_connector = MagicMock()
    mock_connector.export_data.side_effect = ValueError("No credentials")
    mock_connector_class.return_value = mock_connector

    result = runner.invoke(app, ["export"])

    assert result.exit_code == 1
    assert "error" in result.stdout.lower()


@patch("kolping_cockpit.connector.LocalConnector")
def test_export_command_headless_flag(mock_connector_class):
    """Test that headless flag is passed to connector."""
    mock_connector = MagicMock()
    mock_connector.export_data.return_value = {"records": []}
    mock_connector_class.return_value = mock_connector

    # Test headless (default)
    runner.invoke(app, ["export"])
    mock_connector_class.assert_called_with(headless=True)

    # Test headed
    runner.invoke(app, ["export", "--headed"])
    mock_connector_class.assert_called_with(headless=False)
