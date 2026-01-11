"""Moodle client for Kolping Study Cockpit.

Handles session-based authentication and data extraction from:
https://portal.kolping-hochschule.de (Moodle Portal)

Authentication flow:
1. User logs in via Microsoft Entra OIDC SSO
2. MoodleSession cookie is obtained
3. Cookie is stored in keyring for subsequent requests
"""

import re
from dataclasses import dataclass, field
from typing import Any

import httpx
from bs4 import BeautifulSoup

from kolping_cockpit.settings import get_secret_from_env_or_keyring, get_settings


@dataclass
class MoodleCourse:
    """Moodle course information."""

    id: str
    name: str
    shortname: str | None = None
    category: str | None = None
    url: str | None = None
    progress: float | None = None
    visible: bool = True


@dataclass
class MoodleEvent:
    """Moodle calendar event (deadlines, exams, etc.)."""

    id: str
    title: str
    description: str | None = None
    course_id: str | None = None
    course_name: str | None = None
    event_type: str | None = None  # assignment, quiz, etc.
    start_time: str | None = None
    end_time: str | None = None
    url: str | None = None


@dataclass
class MoodleAssignment:
    """Moodle assignment information."""

    id: str
    name: str
    course_id: str | None = None
    course_name: str | None = None
    due_date: str | None = None
    cutoff_date: str | None = None
    description: str | None = None
    submission_status: str | None = None
    grading_status: str | None = None
    grade: str | None = None
    url: str | None = None


@dataclass
class MoodleGrade:
    """Moodle grade item."""

    id: str | None = None
    item_name: str = ""
    grade: str | None = None
    range_min: str | None = None
    range_max: str | None = None
    percentage: str | None = None
    feedback: str | None = None
    course_id: str | None = None
    course_name: str | None = None


@dataclass
class MoodleDashboard:
    """Complete Moodle dashboard data."""

    user_name: str | None = None
    courses: list[MoodleCourse] = field(default_factory=list)
    events: list[MoodleEvent] = field(default_factory=list)
    assignments: list[MoodleAssignment] = field(default_factory=list)
    notifications: list[dict[str, Any]] = field(default_factory=list)
    raw_html: str | None = None


class KolpingMoodleClient:
    """Moodle client for Kolping portal.

    Uses session cookie authentication.
    """

    def __init__(self, session_cookie: str | None = None):
        """Initialize the Moodle client.

        Args:
            session_cookie: Optional MoodleSession cookie value.
                            If not provided, tries to load from keyring.
        """
        self.settings = get_settings()
        self.base_url = self.settings.moodle_base_url

        # Try to get session cookie from various sources
        self._session_cookie = session_cookie
        if not self._session_cookie:
            self._session_cookie = get_secret_from_env_or_keyring("moodle_session")

        self._client: httpx.Client | None = None

    @property
    def client(self) -> httpx.Client:
        """Get or create HTTP client with session cookie."""
        if self._client is None:
            cookies = {}
            if self._session_cookie:
                cookies["MoodleSession"] = self._session_cookie

            self._client = httpx.Client(
                cookies=cookies,
                headers={
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                    "Accept-Language": "de-DE,de;q=0.9,en;q=0.8",
                },
                follow_redirects=True,
                timeout=30.0,
            )
        return self._client

    @property
    def is_authenticated(self) -> bool:
        """Check if client has session cookie."""
        return self._session_cookie is not None

    def test_session(self) -> tuple[bool, str]:
        """Test if the current session is valid.

        Returns:
            Tuple of (is_valid, message)
        """
        try:
            response = self.client.get(f"{self.base_url}/my/")

            # Check if we got redirected to login
            if "login" in str(response.url) or "Weiterleiten" in response.text:
                return False, "Session expired - redirected to login"

            # Check for user menu (indicates logged in)
            if "user-menu" in response.text or "usermenu" in response.text:
                return True, "Session valid"

            # Check for dashboard content
            if "Dashboard" in response.text or "Meine Kurse" in response.text:
                return True, "Session valid"

            return False, "Unable to verify session status"

        except httpx.HTTPStatusError as e:
            return False, f"HTTP error: {e.response.status_code}"
        except Exception as e:
            return False, f"Connection error: {e}"

    def get_dashboard(self) -> MoodleDashboard:
        """Fetch and parse the Moodle dashboard.

        Returns:
            MoodleDashboard with courses, events, and other data
        """
        response = self.client.get(f"{self.base_url}/my/")
        response.raise_for_status()

        soup = BeautifulSoup(response.text, "html.parser")
        dashboard = MoodleDashboard(raw_html=response.text)

        # Extract user name
        user_menu = soup.find(class_=re.compile(r"user.*menu|usermenu"))
        if user_menu:
            dashboard.user_name = user_menu.get_text(strip=True)[:50]

        # Extract courses
        dashboard.courses = self._extract_courses(soup)

        # Extract upcoming events
        dashboard.events = self._extract_events(soup)

        return dashboard

    def _extract_courses(self, soup: BeautifulSoup) -> list[MoodleCourse]:
        """Extract course list from dashboard HTML."""
        courses = []

        # Look for course cards/links
        course_elements = soup.find_all("a", href=re.compile(r"/course/view\.php\?id=\d+"))

        seen_ids: set[str] = set()
        for elem in course_elements:
            href = elem.get("href", "")
            match = re.search(r"id=(\d+)", href)
            if not match:
                continue

            course_id = match.group(1)
            if course_id in seen_ids:
                continue
            seen_ids.add(course_id)

            # Get course name from various possible locations
            name = elem.get_text(strip=True)
            if not name or len(name) < 3:
                # Try parent elements
                parent = elem.parent
                if parent:
                    name = parent.get_text(strip=True)[:100]

            if name:
                courses.append(
                    MoodleCourse(
                        id=course_id,
                        name=name[:200],
                        url=href if href.startswith("http") else f"{self.base_url}{href}",
                    )
                )

        return courses

    def _extract_events(self, soup: BeautifulSoup) -> list[MoodleEvent]:
        """Extract calendar events from dashboard HTML."""
        events = []

        # Look for event blocks (upcoming deadlines, etc.)
        event_elements = soup.find_all(class_=re.compile(r"event|deadline|assignment"))

        for elem in event_elements:
            # Try to extract event ID from links
            link = elem.find("a", href=re.compile(r"event|calendar|mod"))
            event_id = "unknown"
            url = None

            if link:
                href = link.get("href", "")
                match = re.search(r"id=(\d+)", href)
                if match:
                    event_id = match.group(1)
                url = href if href.startswith("http") else f"{self.base_url}{href}"

            title = elem.get_text(strip=True)[:200]
            if title:
                events.append(
                    MoodleEvent(
                        id=event_id,
                        title=title,
                        url=url,
                    )
                )

        return events

    def get_courses(self) -> list[MoodleCourse]:
        """Fetch list of enrolled courses.

        Returns:
            List of MoodleCourse objects
        """
        response = self.client.get(f"{self.base_url}/my/courses.php")
        response.raise_for_status()

        soup = BeautifulSoup(response.text, "html.parser")
        return self._extract_courses(soup)

    def get_course_details(self, course_id: str) -> dict[str, Any]:
        """Fetch details for a specific course.

        Args:
            course_id: Moodle course ID

        Returns:
            Dictionary with course details
        """
        response = self.client.get(f"{self.base_url}/course/view.php?id={course_id}")
        response.raise_for_status()

        soup = BeautifulSoup(response.text, "html.parser")

        # Extract course title
        title_elem = soup.find("h1") or soup.find(class_="page-header-headings")
        title = title_elem.get_text(strip=True) if title_elem else f"Course {course_id}"

        # Extract sections/modules
        sections = []
        section_elems = soup.find_all(class_=re.compile(r"section|activity"))
        for section in section_elems[:20]:  # Limit to avoid too much data
            section_text = section.get_text(strip=True)[:500]
            if section_text:
                sections.append(section_text)

        return {
            "id": course_id,
            "title": title,
            "url": f"{self.base_url}/course/view.php?id={course_id}",
            "sections": sections,
        }

    def get_assignments(self) -> list[MoodleAssignment]:
        """Fetch all assignments from the calendar/upcoming view.

        Returns:
            List of MoodleAssignment objects
        """
        # Try the assignments overview page
        response = self.client.get(f"{self.base_url}/mod/assign/index.php")

        if response.status_code == 200:
            soup = BeautifulSoup(response.text, "html.parser")
            return self._extract_assignments_from_page(soup)

        return []

    def _extract_assignments_from_page(self, soup: BeautifulSoup) -> list[MoodleAssignment]:
        """Extract assignments from an assignments page."""
        assignments = []

        # Look for assignment links
        assign_links = soup.find_all("a", href=re.compile(r"/mod/assign/view\.php\?id=\d+"))

        for link in assign_links:
            href = link.get("href", "")
            match = re.search(r"id=(\d+)", href)
            if not match:
                continue

            assign_id = match.group(1)
            name = link.get_text(strip=True)

            if name:
                assignments.append(
                    MoodleAssignment(
                        id=assign_id,
                        name=name,
                        url=href if href.startswith("http") else f"{self.base_url}{href}",
                    )
                )

        return assignments

    def get_grades(self) -> list[MoodleGrade]:
        """Fetch user grades overview.

        Returns:
            List of MoodleGrade objects
        """
        response = self.client.get(f"{self.base_url}/grade/report/overview/index.php")

        if response.status_code != 200:
            return []

        soup = BeautifulSoup(response.text, "html.parser")
        grades = []

        # Look for grade table rows
        grade_rows = soup.find_all("tr", class_=re.compile(r"grade|item"))

        for row in grade_rows:
            cells = row.find_all(["td", "th"])
            if len(cells) >= 2:
                item_name = cells[0].get_text(strip=True)
                grade_value = cells[1].get_text(strip=True) if len(cells) > 1 else None

                if item_name:
                    grades.append(
                        MoodleGrade(
                            item_name=item_name,
                            grade=grade_value,
                        )
                    )

        return grades

    def get_calendar_events(
        self, month: int | None = None, year: int | None = None
    ) -> list[MoodleEvent]:
        """Fetch calendar events for a specific month.

        Args:
            month: Month number (1-12), defaults to current
            year: Year, defaults to current

        Returns:
            List of MoodleEvent objects
        """
        from datetime import UTC, datetime

        now = datetime.now(UTC)
        month = month or now.month
        year = year or now.year

        response = self.client.get(
            f"{self.base_url}/calendar/view.php",
            params={"view": "month", "time": f"{year}{month:02d}01"},
        )

        if response.status_code != 200:
            return []

        soup = BeautifulSoup(response.text, "html.parser")
        return self._extract_events(soup)

    def get_upcoming_deadlines(self) -> list[MoodleEvent]:
        """Fetch upcoming deadlines from the calendar block.

        Returns:
            List of MoodleEvent objects representing deadlines
        """
        response = self.client.get(f"{self.base_url}/calendar/view.php?view=upcoming")

        if response.status_code != 200:
            return []

        soup = BeautifulSoup(response.text, "html.parser")
        return self._extract_events(soup)

    def export_all(self) -> dict[str, Any]:
        """Export all available Moodle data.

        Returns:
            Dictionary with all exported data and metadata
        """
        from datetime import UTC, datetime

        results: dict[str, Any] = {
            "export_timestamp": datetime.now(UTC).isoformat(),
            "authenticated": self.is_authenticated,
            "base_url": self.base_url,
            "session_valid": False,
            "data": {},
            "errors": {},
        }

        # Test session first
        is_valid, message = self.test_session()
        results["session_valid"] = is_valid
        results["session_message"] = message

        if not is_valid:
            results["errors"]["session"] = message
            return results

        # Export each data type
        exporters = [
            ("dashboard", self.get_dashboard),
            ("courses", self.get_courses),
            ("assignments", self.get_assignments),
            ("grades", self.get_grades),
            ("upcoming_deadlines", self.get_upcoming_deadlines),
        ]

        for name, exporter_func in exporters:
            try:
                data = exporter_func()
                # Convert dataclasses to dicts
                if hasattr(data, "__dataclass_fields__"):
                    from dataclasses import asdict

                    results["data"][name] = asdict(data)
                elif isinstance(data, list):
                    from dataclasses import asdict

                    results["data"][name] = [
                        asdict(item) if hasattr(item, "__dataclass_fields__") else item
                        for item in data
                    ]
                else:
                    results["data"][name] = data
            except Exception as e:
                results["errors"][name] = str(e)

        return results

    def close(self) -> None:
        """Close the HTTP client."""
        if self._client:
            self._client.close()
            self._client = None

    def __enter__(self) -> "KolpingMoodleClient":
        """Context manager entry."""
        return self

    def __exit__(self, *args: Any) -> None:
        """Context manager exit."""
        self.close()
