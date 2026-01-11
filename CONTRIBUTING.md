# Contributing to Kolping Study Cockpit

Thank you for considering contributing to Kolping Study Cockpit! This document provides guidelines and instructions for contributing.

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Help others learn and grow
- Prioritize security and privacy

## Getting Started

### Prerequisites

- Python 3.12 or higher
- uv package manager
- Git

### Setting Up Development Environment

1. Fork the repository on GitHub

2. Clone your fork:
```bash
git clone https://github.com/YOUR-USERNAME/kolping-study-cockpit.git
cd kolping-study-cockpit
```

3. Create a virtual environment with uv:
```bash
uv venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
```

4. Install development dependencies:
```bash
uv pip install -e ".[dev]"
```

5. Install Playwright browsers:
```bash
playwright install chromium
```

6. Install pre-commit hooks:
```bash
pre-commit install
```

## Development Workflow

### Branching Strategy

- `main` - Stable release branch
- `develop` - Development branch (if used)
- Feature branches: `feature/your-feature-name`
- Bug fixes: `fix/issue-description`

### Making Changes

1. Create a new branch:
```bash
git checkout -b feature/your-feature-name
```

2. Make your changes following the coding standards below

3. Run tests and checks:
```bash
# Run tests
pytest

# Run linter
ruff check src/ tests/

# Run type checker
pyright

# Format code
ruff format src/ tests/
```

4. Commit your changes:
```bash
git add .
git commit -m "feat: add your feature description"
```

Use conventional commit messages:
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `test:` Test additions or changes
- `refactor:` Code refactoring
- `chore:` Maintenance tasks

5. Push to your fork:
```bash
git push origin feature/your-feature-name
```

6. Open a Pull Request on GitHub

## Coding Standards

### Python Style

- Follow PEP 8 style guide
- Use type hints for all function signatures
- Maximum line length: 100 characters
- Use docstrings for all public functions and classes

Example:
```python
def export_data(self, output_path: Path) -> dict[str, Any]:
    """
    Export data to JSON file.

    Args:
        output_path: Path where JSON will be written

    Returns:
        Dictionary containing exported data

    Raises:
        ValueError: If credentials are not configured
    """
    pass
```

### Code Quality

All code must pass:
- **Ruff**: Linting and formatting
- **Pyright**: Type checking
- **Pytest**: All tests
- **Pre-commit hooks**: Including gitleaks

### Testing

- Write tests for all new features
- Maintain or improve code coverage
- Use descriptive test names
- Follow the Arrange-Act-Assert pattern

Example:
```python
def test_connector_stores_credentials():
    """Test that connector can store credentials in keyring."""
    # Arrange
    username = "testuser"
    password = "testpass"  # noqa: S105
    
    # Act
    LocalConnector.store_credentials(username, password)
    
    # Assert
    connector = LocalConnector()
    stored_username, stored_password = connector.get_credentials()
    assert stored_username == username
    assert stored_password == password
```

### Documentation

- Update README.md for user-facing changes
- Add docstrings for new functions/classes
- Update SECURITY.md for security-related changes
- Include inline comments for complex logic

## Security Guidelines

### Never Commit Secrets

- No API keys, passwords, or tokens
- No real credentials in tests (use fixtures/mocks)
- No hardcoded URLs to production systems
- Pre-commit hooks will catch most issues

### Secure Coding Practices

- Always use parameterized queries
- Validate and sanitize user input
- Use secure defaults (e.g., `headless=True`)
- Handle sensitive data carefully
- Follow OWASP guidelines

### Dependency Management

- Only add necessary dependencies
- Prefer well-maintained packages
- Check for known vulnerabilities
- Document why each dependency is needed

## Testing Guidelines

### Unit Tests

Located in `tests/`, organized by module:
```
tests/
├── test_connector.py
├── test_cli.py
└── conftest.py  # Shared fixtures
```

Run specific tests:
```bash
pytest tests/test_connector.py
pytest -k test_export_data
```

### Integration Tests

Test real browser automation (when appropriate):
```python
@pytest.mark.integration
def test_playwright_browser_launch():
    """Test that Playwright can launch browser."""
    # Test implementation
```

Run integration tests:
```bash
pytest -m integration
```

### Test Fixtures

Use fixtures for common setup:
```python
@pytest.fixture
def mock_connector():
    """Provide a connector with mocked credentials."""
    with patch("kolping_cockpit.connector.keyring"):
        connector = LocalConnector()
        yield connector
```

## Pull Request Process

### Before Submitting

- [ ] All tests pass locally
- [ ] Code is formatted with ruff
- [ ] Type checking passes with pyright
- [ ] Pre-commit hooks pass
- [ ] Documentation is updated
- [ ] CHANGELOG is updated (if applicable)

### PR Description Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
Describe testing performed

## Checklist
- [ ] Code follows style guidelines
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] No secrets committed
```

### Review Process

1. Automated CI checks must pass
2. At least one maintainer review required
3. Address review feedback
4. Maintain a clean commit history
5. Squash commits if requested

## Common Tasks

### Adding a New Dependency

1. Add to `pyproject.toml`:
```toml
[project]
dependencies = [
    "new-package>=1.0.0",
]
```

2. Install and test:
```bash
uv pip install -e ".[dev]"
pytest
```

3. Document why it's needed in PR

### Adding a New CLI Command

1. Add to `cli.py`:
```python
@app.command()
def new_command() -> None:
    """Command description."""
    pass
```

2. Add tests in `tests/test_cli.py`
3. Update README with usage example

### Debugging Tips

Run with verbose output:
```bash
kolping export --headed  # See browser
pytest -vv  # Verbose tests
ruff check --verbose  # Detailed lint output
```

## Getting Help

- Open an issue for bugs or feature requests
- Use discussions for questions
- Tag maintainers for urgent issues

## Recognition

Contributors will be recognized in:
- GitHub contributors page
- Release notes
- README (for significant contributions)

Thank you for contributing! 🎉
