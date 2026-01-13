# Copilot Coding Agent Instructions

## Project Overview
Kolping Study Cockpit is a secure local connector for study data management at Kolping-Hochschule. It interfaces with:
- Moodle Portal (SSO via Microsoft Entra OIDC)
- GraphQL API for "Mein Studium" dashboard
- Android KMP App (Kotlin Multiplatform)

## Copilot Permissions & Capabilities
Copilot has **full access** to:
- All files in this repository (read/write)
- Terminal execution (bash, gradle, python)
- Git operations (commit, push, branch, merge)
- GitHub API (issues, PRs, actions)
- File creation and modification
- Package installation (pip, gradle, apt)

## Environment Variables Available
The following secrets are configured and available as environment variables:
- `KOLPING_USERNAME`: User's Kolping-Hochschule email
- `KOLPING_PASSWORD`: User's password (use carefully, prefer interactive login)
- `KOLPING_CLIENT_ID`: Microsoft Entra OAuth client ID
- `JAVA_HOME`: /usr/lib/jvm/java-17-openjdk-amd64
- `ANDROID_HOME`: ~/Android/Sdk

## Project Structure
```
/workspaces/kolping-study-cockpit/
├── src/kolping_cockpit/     # Python CLI & connectors
├── kmp/                      # Kotlin Multiplatform
│   ├── androidApp/          # Android app
│   └── shared/              # Shared Kotlin code
├── .devcontainer/           # Codespace configuration
└── .vscode/                 # VS Code settings
```

## Build Commands
```bash
# Python
pip install -e ".[dev]"
pytest
ruff check src/

# Android/Kotlin
cd kmp && ./gradlew :androidApp:assembleDebug
cd kmp && ./gradlew :shared:build

# With detailed output
./gradlew :androidApp:assembleDebug --warning-mode all --console=plain
```

## Security Requirements
1. **Never log or print credentials** - Always redact sensitive data
2. **Never commit tokens/cookies** - Use keyring or environment variables
3. **Use headless=false for login** - MFA may be required
4. **Respect gitleaks rules** - Pre-commit hooks will catch secrets

## Key Files
- `src/kolping_cockpit/settings.py` - Configuration management
- `src/kolping_cockpit/connector.py` - Browser automation
- `src/kolping_cockpit/cli.py` - CLI commands
- `kmp/shared/` - Shared Kotlin models and API clients
- `kmp/androidApp/` - Android UI and ViewModels

## Testing
- Run `pytest` for Python tests
- Run `ruff check src/` for linting
- Run `pyright` for type checking
- Run `./kmp/gradlew :androidApp:assembleDebug` for Kotlin compilation

## CLI Commands
- `kolping configure` - Store credentials in keyring
- `kolping export` - Export study data
- `kolping diagnose` - Check connectivity
- `kolping login` - Interactive browser login

## Copilot Workflow Preferences
1. **Always use tools** - Prefer file edits over code blocks
2. **Auto-commit** - Commit and push changes when requested
3. **Proactive fixes** - Fix related issues when found
4. **German locale** - Respond in German when appropriate
