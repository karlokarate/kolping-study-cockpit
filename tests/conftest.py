"""Pytest configuration and shared fixtures."""

import pytest


@pytest.fixture
def mock_credentials():
    """Provide mock credentials for testing."""
    return ("testuser", "testpass")


@pytest.fixture
def sample_export_data():
    """Provide sample export data for testing."""
    return {
        "version": "0.1.0",
        "exported_at": "2024-01-01T00:00:00Z",
        "user": "testuser",
        "records": [
            {
                "id": 1,
                "title": "Test Record",
                "description": "Test description",
            }
        ],
    }
