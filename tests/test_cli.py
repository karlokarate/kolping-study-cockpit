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


@patch("kolping_cockpit.graphql_client.KolpingGraphQLClient")
def test_export_graphql_command_success(mock_client_class, tmp_path):
    """Test successful export graphql command."""
    # Setup mock client
    mock_client = MagicMock()
    mock_client.__enter__ = MagicMock(return_value=mock_client)
    mock_client.__exit__ = MagicMock(return_value=False)
    mock_client.is_authenticated = True
    mock_client.test_connection.return_value = (True, "Connected")
    mock_client.export_all.return_value = {"data": {"test": "data"}}
    mock_client_class.return_value = mock_client

    output_file = tmp_path / "test.json"
    result = runner.invoke(app, ["export", "graphql", "--output", str(output_file)])

    assert result.exit_code == 0
    assert output_file.exists()


@patch("kolping_cockpit.graphql_client.KolpingGraphQLClient")
def test_export_graphql_command_no_auth(mock_client_class):
    """Test export graphql command without authentication."""
    mock_client = MagicMock()
    mock_client.__enter__ = MagicMock(return_value=mock_client)
    mock_client.__exit__ = MagicMock(return_value=False)
    mock_client.is_authenticated = False
    mock_client.test_connection.return_value = (False, "Not authenticated")
    mock_client_class.return_value = mock_client

    result = runner.invoke(app, ["export", "graphql"])

    # Should exit with error when no auth and connection fails
    assert result.exit_code == 1


@patch("kolping_cockpit.moodle_client.KolpingMoodleClient")
def test_export_moodle_command_no_session(mock_client_class):
    """Test export moodle command without session."""
    mock_client = MagicMock()
    mock_client.__enter__ = MagicMock(return_value=mock_client)
    mock_client.__exit__ = MagicMock(return_value=False)
    mock_client.is_authenticated = False
    mock_client_class.return_value = mock_client

    result = runner.invoke(app, ["export", "moodle"])

    # Should exit with error when no session
    assert result.exit_code == 1
