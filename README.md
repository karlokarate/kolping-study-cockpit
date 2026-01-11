# Kolping Study Cockpit

**Lokaler, sicherer Connector für Studiumsdaten der Kolping-Hochschule**

> 📱 **NEU**: Kotlin Multiplatform Android App verfügbar! Siehe [`kmp/`](./kmp/) Verzeichnis.

A secure local connector for study data management using Playwright browser automation and system keyring for credential management.

## Goal

Kolping Study Cockpit provides a secure, automated way to export study-related data from web-based systems. It uses:
- **Playwright** for reliable browser automation
- **Keyring** for secure credential storage in your system's native credential manager
- **JSON export** for structured data output

## Architecture

### Projects

This repository contains two implementations:

1. **Python CLI Tool** (`src/` directory) - Desktop automation tool
   - Uses Playwright for browser automation
   - Command-line interface with Typer
   - Secure credential storage with Keyring
   - JSON export functionality

2. **📱 Kotlin Multiplatform Android App** ([`kmp/` directory](./kmp/)) - Native mobile app
   - Ports Python business logic to Kotlin
   - Native Android UI with Jetpack Compose
   - WebView-based Microsoft Entra authentication
   - Material Design 3 interface
   - See [kmp/README.md](./kmp/README.md) for details

### Core Components (Python CLI)

```
kolping-cockpit/
├── src/kolping_cockpit/
│   ├── __init__.py          # Package initialization
│   ├── cli.py               # CLI interface (Typer + Rich)
│   └── connector.py         # Local connector (Playwright + Keyring)
├── tests/                   # Test suite
├── docs/                    # Documentation
├── scripts/                 # Helper scripts
├── config/                  # Configuration files
└── graphql/                 # GraphQL schemas (if needed)
```

### Technology Stack

- **Python 3.12+**: Modern Python with latest features
- **uv**: Fast Python package installer and resolver
- **Playwright**: Browser automation for web scraping
- **Keyring**: Secure credential storage
- **Pydantic v2**: Data validation
- **Typer**: CLI framework
- **Rich**: Beautiful terminal output
- **httpx**: Modern HTTP client

### Development Tools

- **Ruff**: Fast Python linter and formatter
- **Pyright**: Static type checker
- **Pytest**: Testing framework
- **Pre-commit**: Git hooks for code quality
- **Gitleaks**: Secret scanning

## Quick Start

### Prerequisites

- Python 3.12 or higher
- uv package manager

### Installation

1. Clone the repository:
```bash
git clone https://github.com/karlokarate/kolping-study-cockpit.git
cd kolping-study-cockpit
```

2. Install dependencies using uv:
```bash
uv venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
uv pip install -e ".[dev]"
```

3. Install Playwright browsers:
```bash
playwright install chromium
```

4. Install pre-commit hooks:
```bash
pre-commit install
```

### Configuration

Before first use, configure your credentials securely:

```bash
kolping configure
```

You'll be prompted to enter your username and password. These are stored securely in your system's keyring (Keychain on macOS, Credential Manager on Windows, Secret Service on Linux).

### Usage

#### Exam Dates and Requirements Overview

View comprehensive exam dates, assessment requirements, and module information:

```bash
# Show all exam dates and requirements
kolping exams

# Filter by specific semester
kolping exams --semester 3

# Include endpoint analysis
kolping exams --analyze
```

This command provides:
- All exam dates for registered modules with time and location
- Required assessment types for each module (Klausur, Lerntagebuch, Präsentation, etc.)
- Detailed requirements for each assessment type
- Links to Moodle courses and materials
- Upcoming calendar events and deadlines
- Integration of GraphQL and Moodle data

#### Export Data to JSON

```bash
# Export GraphQL data
kolping export graphql

# Export Moodle data
kolping export moodle

# Export all data
kolping export all

# Export to a specific directory
kolping export all --output-dir ./my-exports
```

#### Other Commands

Show version:

```bash
kolping version
```

Show authentication status:

```bash
kolping status
```

Configure credentials:

```bash
kolping configure
```

Show upcoming deadlines:

```bash
kolping deadlines
```

### Development

Run tests:
```bash
pytest
```

Run linter:
```bash
ruff check src/ tests/
```

Run type checker:
```bash
pyright
```

Format code:
```bash
ruff format src/ tests/
```

## Safety & Security

### Credential Storage

- **Never** store credentials in plain text files or environment variables
- Credentials are stored in your system's native credential manager via `keyring`
- The connector never logs or exposes credentials
- See [SECURITY.md](SECURITY.md) for credential revocation procedures

### Data Handling

- Exported data is stored locally in the `exports/` directory (gitignored by default)
- No data is transmitted to third-party services
- Browser automation traces and cookies are excluded from version control

### Secret Scanning

- Pre-commit hooks include `gitleaks` to prevent accidental credential commits
- CI pipeline scans for secrets before merging

### Browser Security

- Playwright sessions are isolated per run
- Authentication state is stored in `playwright/.auth/` (gitignored)
- HAR files and traces are gitignored to prevent data leaks

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines.

## License

MIT

