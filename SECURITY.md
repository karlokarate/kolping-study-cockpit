# Security Policy

## Reporting Security Vulnerabilities

If you discover a security vulnerability in Kolping Study Cockpit, please report it by emailing the maintainers. Do not open a public issue.

## Credential Security

### Secure Storage

Kolping Study Cockpit uses the system keyring to securely store credentials:

- **macOS**: Keychain
- **Windows**: Credential Manager
- **Linux**: Secret Service (GNOME Keyring, KWallet, etc.)

Credentials are never stored in:
- Plain text files
- Environment variables
- Source code
- Git repository
- Log files

### Credential Revocation Steps

If you believe your credentials have been compromised, follow these steps immediately:

#### 1. Change Your Password

Change your password on the target system immediately:
1. Log in to the web interface
2. Navigate to account settings
3. Change your password
4. Log out from all sessions if available

#### 2. Remove Stored Credentials

Remove credentials from the system keyring:

**Using Python:**
```bash
python -c "import keyring; keyring.delete_password('kolping-cockpit', 'username')"
python -c "import keyring; keyring.delete_password('kolping-cockpit', '<your-username>')"
```

**Using Command Line Tools:**

On macOS:
```bash
security delete-generic-password -s "kolping-cockpit" -a "username"
security delete-generic-password -s "kolping-cockpit" -a "<your-username>"
```

On Windows (PowerShell):
```powershell
cmdkey /delete:kolping-cockpit
```

On Linux (GNOME Keyring):
```bash
secret-tool clear service kolping-cockpit account username
secret-tool clear service kolping-cockpit account <your-username>
```

#### 3. Reconfigure with New Credentials

After changing your password and clearing old credentials:
```bash
kolping configure
```

#### 4. Audit Export Files

Review any exported JSON files in the `exports/` directory for sensitive data:
```bash
ls -la exports/
```

If they contain sensitive information, delete them securely:
```bash
rm -P exports/*.json  # macOS
shred -u exports/*.json  # Linux
```

#### 5. Check for Unauthorized Access

- Review recent login activity on the target system
- Check for any unauthorized data exports
- Look for unusual account activity

## Best Practices

### Development

1. **Never commit credentials** to version control
   - Pre-commit hooks with gitleaks are enabled by default
   - `.gitignore` excludes `.env*`, `cookies*`, and auth directories

2. **Never log credentials**
   - The connector is designed to never log passwords
   - Be careful when adding debug logging

3. **Use headless mode in production**
   - Run `kolping export --headless` (default)
   - Only use `--headed` for debugging

4. **Secure export files**
   - Keep `exports/` directory private
   - Do not commit export files
   - Delete exports after processing

### CI/CD Security

- GitHub Actions workflows include secret scanning with gitleaks
- No credentials are stored in GitHub Actions secrets
- All CI checks run on pull requests before merging

## Dependency Security

### Regular Updates

Keep dependencies updated to receive security patches:
```bash
uv pip install --upgrade -e ".[dev]"
```

### Vulnerability Scanning

Dependencies are scanned for known vulnerabilities:
- GitHub Dependabot alerts
- Pre-commit hooks with safety checks (optional)

### Minimal Dependencies

The project uses only essential, well-maintained dependencies:
- playwright (Microsoft)
- keyring (Jason R. Coombs)
- pydantic (Samuel Colvin)
- typer (Sebastián Ramírez)
- rich (Will McGugan)
- httpx (Tom Christie)

## Data Privacy

### Local Processing

- All data processing happens locally
- No data is sent to third-party services
- Exports are stored only on your machine

### Browser Isolation

- Each Playwright session is isolated
- Browser contexts are cleaned up after use
- No persistent browser profiles are created

### Export Safety

Exported JSON files may contain:
- Personal study data
- Academic records
- Login usernames (not passwords)

Handle exports according to your institution's data privacy policies.

## Compliance

This tool is designed for personal use and should comply with:
- Your institution's acceptable use policies
- Terms of service of the target system
- Data protection regulations (GDPR, FERPA, etc.)

Always verify you have permission to automate access to any system.

## Contact

For security concerns, contact the repository maintainers through GitHub.
