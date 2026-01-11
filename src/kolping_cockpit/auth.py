"""Authentication module for interactive browser login."""

from dataclasses import dataclass
from pathlib import Path

from kolping_cockpit.settings import get_settings, store_secret


@dataclass
class LoginResult:
    """Result of a login attempt."""

    success: bool
    username: str | None = None
    error: str | None = None
    moodle_session: str | None = None
    access_token: str | None = None


def get_auth_storage_path() -> Path:
    """Get the path for storing Playwright auth state."""
    # Use platformdirs if available, otherwise fallback
    try:
        from platformdirs import user_data_dir

        base = Path(user_data_dir("kolping-cockpit", "kolping"))
    except ImportError:
        base = Path.home() / ".kolping-cockpit"

    auth_dir = base / "playwright" / ".auth"
    auth_dir.mkdir(parents=True, exist_ok=True)
    return auth_dir


def interactive_login(headless: bool = False) -> LoginResult:
    """
    Perform interactive login via Playwright browser.

    Opens a browser window for Microsoft Entra authentication.
    Supports MFA. Stores session tokens securely after successful login.

    Args:
        headless: Run browser in headless mode (default False for MFA support)

    Returns:
        LoginResult with success status and extracted tokens
    """
    from playwright.sync_api import sync_playwright

    settings = get_settings()
    auth_storage = get_auth_storage_path() / "state.json"

    with sync_playwright() as p:
        # Use persistent context to maintain login state
        browser = p.chromium.launch(headless=headless)

        # Create context with storage state if exists
        context_options = {
            "viewport": {"width": 1280, "height": 720},
        }
        if auth_storage.exists():
            context_options["storage_state"] = str(auth_storage)

        context = browser.new_context(**context_options)
        page = context.new_page()

        try:
            # Navigate to Moodle portal (will redirect to Microsoft login)
            page.goto(settings.moodle_login_url, timeout=60000)

            # Pre-fill username if available from environment
            if settings.username:
                try:
                    # Microsoft login form
                    email_input = page.locator('input[type="email"], input[name="loginfmt"]')
                    if email_input.is_visible(timeout=3000):
                        email_input.fill(settings.username)
                except Exception:
                    pass  # Input not found or not visible yet

            # Wait for user to complete login
            # We detect successful login by checking for Moodle dashboard URL
            page.wait_for_url(
                f"{settings.moodle_base_url}/**",
                timeout=300000,  # 5 minutes for MFA
            )

            # Extract session cookies
            cookies = context.cookies()
            moodle_session = None
            for cookie in cookies:
                if cookie["name"] == "MoodleSession":
                    moodle_session = cookie["value"]
                    break

            # Try to extract username from page
            username = settings.username
            try:
                # Moodle often shows username in user menu
                user_element = page.locator(".usertext, .usermenu .userbutton")
                if user_element.is_visible(timeout=2000):
                    username = user_element.inner_text()
            except Exception:
                pass

            # Save browser state for future sessions
            context.storage_state(path=str(auth_storage))

            # Store session in keyring
            if moodle_session:
                store_secret("moodle_session", moodle_session)

            return LoginResult(
                success=True,
                username=username,
                moodle_session=moodle_session,
            )

        except Exception as e:
            error_msg = str(e)
            if "Timeout" in error_msg:
                error_msg = "Login timeout - did you complete the login in the browser?"
            return LoginResult(
                success=False,
                error=error_msg,
            )

        finally:
            context.close()
            browser.close()


def get_stored_session() -> tuple[str | None, str | None]:
    """
    Get stored session tokens.

    Returns:
        Tuple of (moodle_session, access_token)
    """
    from kolping_cockpit.settings import get_secret_from_env_or_keyring

    moodle_session = get_secret_from_env_or_keyring("moodle_session")
    access_token = get_secret_from_env_or_keyring("access_token")

    return moodle_session, access_token


def is_logged_in() -> bool:
    """Check if user has stored session tokens."""
    moodle_session, access_token = get_stored_session()
    return bool(moodle_session or access_token)
