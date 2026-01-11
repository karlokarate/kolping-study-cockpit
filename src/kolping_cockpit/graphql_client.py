"""GraphQL client for Kolping Study Cockpit.

Based on captured API responses from:
https://app-kolping-prod-gateway.azurewebsites.net/graphql
via https://cms.kolping-hochschule.de/ ("Mein Studium")

IMPORTANT: This API REQUIRES Bearer token authentication!
Token is obtained from Microsoft Entra login on cms.kolping-hochschule.de

Available queries (verified from real responses):
- myStudentData: Student personal data (name, address, email, etc.)
- myStudentGradeOverview: Complete grade overview with modules and student data
"""

from dataclasses import dataclass
from typing import Any

import httpx

from kolping_cockpit.settings import get_secret_from_env_or_keyring, get_settings


@dataclass
class GraphQLError:
    """GraphQL error response."""

    message: str
    locations: list[dict] | None = None
    path: list[str] | None = None
    extensions: dict | None = None


@dataclass
class GraphQLResponse:
    """GraphQL response container."""

    data: dict[str, Any] | None
    errors: list[GraphQLError] | None

    @property
    def has_errors(self) -> bool:
        """Check if response has errors."""
        return self.errors is not None and len(self.errors) > 0


class KolpingGraphQLClient:
    """GraphQL client for Kolping Study API.

    Handles Bearer token authentication and provides typed query methods.
    REQUIRES authentication - token from cms.kolping-hochschule.de login.
    """

    # GraphQL queries verified from captured API responses (2026-01-11)
    # Field names extracted from real response_body.json files
    QUERIES = {
        # Full student data query (from docs/6/request_body.json)
        "myStudentData": """
            query gtMyStudentData {
                myStudentData {
                    studentId
                    geschlechtTnid
                    titel
                    akademischerGradTnid
                    vorname
                    nachname
                    geburtsdatum
                    geburtsort
                    geburtslandTnid
                    staatsangehoerigkeitTnid
                    createdAt
                    wohnlandTnid
                    telefonnummer
                    emailPrivat
                    strasse
                    hausnummer
                    plz
                    wohnort
                    benutzername
                    emailKh
                    notizen
                    bemerkung
                    akademischerGrad
                    geburtsland
                    staatsangehoerigkeit
                    wohnland
                }
            }
        """,
        # Complete grade overview with modules (from docs/7/request_body.json)
        "myStudentGradeOverview": """
            query getMyStudentGradeOverview {
                myStudentGradeOverview {
                    modules {
                        modulId
                        semester
                        modulbezeichnung
                        eCTS
                        pruefungsId
                        pruefungsform
                        grade
                        points
                        note
                        color
                        examStatus
                        eCTSString
                    }
                    grade
                    eCTS
                    student {
                        id
                        geschlechtTnid
                        titel
                        akademischerGradTnid
                        vorname
                        nachname
                        geburtsdatum
                        geburtsort
                        geburtslandTnid
                        staatsangehoerigkeitTnid
                        createdAt
                        wohnlandTnid
                        telefonnummer
                        emailPrivat
                        strasse
                        hausnummer
                        plz
                        wohnort
                        benutzername
                        emailKh
                        notizen
                        bemerkung
                    }
                    currentSemester
                }
            }
        """,
        # Simplified student data (from docs/5/request_body.json)
        "myStudentDataSimple": """
            query RaftgetmyStudentData {
                result: myStudentData {
                    studentId
                    anrede
                    titel
                    akademischerGrad
                    vorname
                    nachname
                    geburtsdatum
                    geburtsort
                    geburtsland
                    staatsangehoerigkeit
                    wohnland
                    telefonnummer
                    emailPrivat
                    strasse
                    hausnummer
                    plz
                    wohnort
                }
            }
        """,
    }

    # Simplified queries with fewer fields for quick checks
    QUERIES_SIMPLE = {
        "myStudentData": """
            query { myStudentData { studentId vorname nachname emailKh emailPrivat } }
        """,
        "myStudentGradeOverview": """
            query { myStudentGradeOverview { grade eCTS currentSemester } }
        """,
        "moduls": """
            query { moduls { id modulName modulkuerzel ectspunkte semester pruefungsform beschreibung } }
        """,
        "semesters": """
            query { semesters { id semesterName semesterPeriode } }
        """,
        "pruefungs": """
            query { pruefungs { id modulId datum uhrzeit raum pruefungsform anmerkung } }
        """,
        "studiengangs": """
            query { studiengangs { id kuerzel bezeichnung } }
        """,
        "matchModulStudent": """
            query { matchModulStudent { modulId studentId status anmeldedatum } }
        """,
    }

    def __init__(self, bearer_token: str | None = None):
        """Initialize the GraphQL client.

        Args:
            bearer_token: Optional bearer token. If not provided, tries to load
                          from keyring/env (KOLPING_GRAPHQL_BEARER_TOKEN).
        """
        self.settings = get_settings()
        self.endpoint = self.settings.graphql_endpoint

        # Try to get bearer token from various sources
        self._bearer_token = bearer_token
        if not self._bearer_token:
            self._bearer_token = get_secret_from_env_or_keyring("graphql_bearer_token")

        self._client: httpx.Client | None = None

    @property
    def client(self) -> httpx.Client:
        """Get or create HTTP client with Bearer auth if available."""
        if self._client is None:
            headers = {
                "Content-Type": "application/json",
                "Accept": "*/*",
                "Origin": "https://cms.kolping-hochschule.de",
                "Referer": "https://cms.kolping-hochschule.de/",
            }
            # Add Bearer token if available (required for personal data)
            if self._bearer_token:
                headers["Authorization"] = f"Bearer {self._bearer_token}"

            self._client = httpx.Client(
                headers=headers,
                timeout=30.0,
            )
        return self._client

    @property
    def is_authenticated(self) -> bool:
        """Check if client has authentication token."""
        return self._bearer_token is not None

    def execute(
        self,
        query: str,
        variables: dict[str, Any] | None = None,
        operation_name: str | None = None,
    ) -> GraphQLResponse:
        """Execute a GraphQL query.

        Args:
            query: GraphQL query string
            variables: Optional query variables
            operation_name: Optional operation name

        Returns:
            GraphQLResponse with data and/or errors
        """
        payload: dict[str, Any] = {"query": query}
        if variables:
            payload["variables"] = variables
        if operation_name:
            payload["operationName"] = operation_name

        response = self.client.post(self.endpoint, json=payload)
        response.raise_for_status()

        result = response.json()

        errors = None
        if "errors" in result:
            errors = [
                GraphQLError(
                    message=e.get("message", "Unknown error"),
                    locations=e.get("locations"),
                    path=e.get("path"),
                    extensions=e.get("extensions"),
                )
                for e in result["errors"]
            ]

        return GraphQLResponse(
            data=result.get("data"),
            errors=errors,
        )

    def execute_named_query(
        self,
        query_name: str,
        simple: bool = False,
    ) -> GraphQLResponse:
        """Execute a predefined named query.

        Args:
            query_name: Name of the query from QUERIES dict
            simple: Use simplified query with fewer fields

        Returns:
            GraphQLResponse with data and/or errors
        """
        queries = self.QUERIES_SIMPLE if simple else self.QUERIES
        if query_name not in queries:
            return GraphQLResponse(
                data=None,
                errors=[GraphQLError(message=f"Unknown query: {query_name}")],
            )
        return self.execute(queries[query_name])

    # Convenience methods for verified working queries
    def get_my_student_data(self, simple: bool = False) -> GraphQLResponse:
        """Get current student's personal data.

        Returns fields like: studentId, vorname, nachname, emailKh, emailPrivat,
        geburtsdatum, geburtsort, strasse, plz, wohnort, telefonnummer, etc.
        """
        return self.execute_named_query("myStudentData", simple=simple)

    def get_my_grade_overview(self, simple: bool = False) -> GraphQLResponse:
        """Get current student's complete grade overview.

        Returns:
        - modules: List of all modules with grades, ECTS, exam status
        - grade: Overall grade (Durchschnittsnote)
        - eCTS: Total ECTS earned
        - currentSemester: Current semester name
        - student: Full student data
        """
        return self.execute_named_query("myStudentGradeOverview", simple=simple)

    def export_all(self, simple: bool = True) -> dict[str, Any]:
        """Export all available data.

        Args:
            simple: Use simplified queries (recommended for initial export)

        Returns:
            Dictionary with all exported data and metadata
        """
        from datetime import UTC, datetime

        results: dict[str, Any] = {
            "export_timestamp": datetime.now(UTC).isoformat(),
            "authenticated": self.is_authenticated,
            "endpoint": self.endpoint,
            "data": {},
            "errors": {},
        }

        # Personal queries (require Bearer token auth)
        # These are the only verified working queries from captured responses
        personal_queries = [
            ("student_data", self.get_my_student_data),
            ("grade_overview", self.get_my_grade_overview),
        ]

        if not self.is_authenticated:
            results["errors"]["auth"] = "No Bearer token - personal data queries will fail"

        # Execute personal queries
        for name, query_func in personal_queries:
            try:
                response = (
                    query_func(simple=simple)
                    if "simple" in query_func.__code__.co_varnames
                    else query_func()
                )
                if response.has_errors:
                    results["errors"][name] = [e.message for e in response.errors or []]
                if response.data:
                    results["data"][name] = response.data
            except Exception as e:
                results["errors"][name] = str(e)

        return results

    def test_connection(self) -> tuple[bool, str]:
        """Test connection to GraphQL endpoint.

        Returns:
            Tuple of (success, message)
        """
        try:
            # Simple introspection query to test connection
            response = self.execute("{ __typename }")
            if response.has_errors:
                return False, f"GraphQL errors: {response.errors}"
            return True, "Connection successful"
        except httpx.HTTPStatusError as e:
            return False, f"HTTP error: {e.response.status_code}"
        except Exception as e:
            return False, f"Connection error: {e}"

    def close(self) -> None:
        """Close the HTTP client."""
        if self._client:
            self._client.close()
            self._client = None

    def __enter__(self) -> "KolpingGraphQLClient":
        """Context manager entry."""
        return self

    def __exit__(self, *args: Any) -> None:
        """Context manager exit."""
        self.close()
