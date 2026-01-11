# Copilot Coding Agent Instructions

## Project Overview
Kolping Study Cockpit is a secure local connector for study data management at Kolping-Hochschule. It interfaces with:
- Moodle Portal (SSO via Microsoft Entra OIDC)
- GraphQL API for "Mein Studium" dashboard

## Environment Variables Available
The following secrets are configured and available as environment variables:
- `KOLPING_USERNAME`: User's Kolping-Hochschule email
- `KOLPING_PASSWORD`: User's password (use carefully, prefer interactive login)
- `KOLPING_CLIENT_ID`: Microsoft Entra OAuth client ID

## Security Requirements
1. **Never log or print credentials** - Always redact sensitive data
2. **Never commit tokens/cookies** - Use keyring or environment variables
3. **Use headless=false for login** - MFA may be required
4. **Respect gitleaks rules** - Pre-commit hooks will catch secrets

## Key Files
- `src/kolping_cockpit/settings.py` - Configuration management
- `src/kolping_cockpit/connector.py` - Browser automation
- `src/kolping_cockpit/cli.py` - CLI commands

## Testing
- Run `pytest` for tests
- Run `ruff check src/` for linting
- Run `pyright` for type checking

## Commands
- `kolping configure` - Store credentials in keyring
- `kolping export` - Export study data
- `kolping diagnose` - Check connectivity (to be implemented)
- `kolping login` - Interactive browser login (to be implemented)
