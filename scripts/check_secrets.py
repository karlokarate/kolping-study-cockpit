#!/usr/bin/env python3
"""Check if required secrets/environment variables are configured."""

import os
import sys


def check_secrets() -> bool:
    """Check if required secrets are available as environment variables."""
    secrets = {
        "KOLPING_USERNAME": os.environ.get("KOLPING_USERNAME"),
        "KOLPING_PASSWORD": os.environ.get("KOLPING_PASSWORD"),
        "KOLPING_CLIENT_ID": os.environ.get("KOLPING_CLIENT_ID"),
    }

    print("🔐 Kolping Study Cockpit - Secret Check\n")
    print("=" * 50)

    all_set = True
    for name, value in secrets.items():
        if value:
            # Redact the actual value
            redacted = value[:3] + "***" + value[-3:] if len(value) > 6 else "***"
            print(f"✅ {name}: {redacted}")
        else:
            print(f"❌ {name}: NOT SET")
            all_set = False

    print("=" * 50)

    if all_set:
        print("\n✅ All secrets are configured!")
        return True
    else:
        print("\n⚠️  Some secrets are missing.")
        print("\nTo configure secrets:")
        print("  1. Codespaces: https://github.com/settings/codespaces")
        print("  2. Repository: https://github.com/karlokarate/kolping-study-cockpit/settings/secrets/actions")
        print("  3. Local: cp .env.example .env && edit .env")
        return False


if __name__ == "__main__":
    success = check_secrets()
    sys.exit(0 if success else 1)
