# Kolping Study Cockpit - Codespaces/Devcontainer Secrets

This document describes how to configure secrets for different environments.

## 🔐 GitHub Codespaces Secrets (BEREITS KONFIGURIERT ✅)

Die folgenden Secrets sind bereits unter https://github.com/settings/codespaces konfiguriert:

| Secret Name | Beschreibung | Status |
|-------------|--------------|--------|
| `KOLPING_USERNAME` | Kolping-Hochschule E-Mail | ✅ Konfiguriert |
| `KOLPING_PASSWORD` | Passwort | ✅ Konfiguriert |
| `KOLPING_CLIENT_ID` | Microsoft Entra OAuth Client ID | ✅ Konfiguriert |

Diese Secrets sind automatisch als **Umgebungsvariablen** im Codespace verfügbar.

## 🤖 Repository Secrets (für Actions & Cloud Agents)

Für GitHub Actions und Copilot Coding Agent müssen die Secrets **zusätzlich** als Repository Secrets konfiguriert werden:

**URL:** https://github.com/karlokarate/kolping-study-cockpit/settings/secrets/actions

| Secret Name | Beschreibung | Für Agent Tasks |
|-------------|--------------|-----------------|
| `KOLPING_USERNAME` | Kolping-Hochschule E-Mail | ✅ Hinzufügen |
| `KOLPING_PASSWORD` | Passwort | ✅ Hinzufügen |
| `KOLPING_CLIENT_ID` | Microsoft Entra OAuth Client ID | ✅ Hinzufügen |

### Repository Secrets hinzufügen:

1. Gehe zu https://github.com/karlokarate/kolping-study-cockpit/settings/secrets/actions
2. Klicke "New repository secret"
3. Füge die drei Secrets hinzu (gleiche Werte wie Codespaces Secrets)

## 💻 Local Development

For local development, use one of these methods:

### Option 1: System Keyring (Recommended)

```bash
kolping configure
# Enter your credentials when prompted
```

Credentials are stored securely in:
- **macOS**: Keychain
- **Windows**: Credential Manager
- **Linux**: Secret Service (GNOME Keyring, KWallet)

### Option 2: Environment File

```bash
cp .env.example .env
# Edit .env with your values
```

### Option 3: Environment Variables

```bash
export KOLPING_USERNAME="your.email@kolping-hochschule.de"
```

## 🚫 Security Rules

1. **Never commit** `.env` files or credentials
2. **Never log** tokens or session cookies without redaction
3. **Rotate tokens** if accidentally exposed
4. **Use headless=false** for initial login (MFA may be required)

## 🔍 Verifying Secret Access

In Codespace or terminal:

```bash
# Check if secrets are available
echo "Username configured: ${KOLPING_USERNAME:+yes}"

# Run diagnostics
kolping diagnose
```
