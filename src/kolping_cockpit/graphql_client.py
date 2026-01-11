"""GraphQL client for Kolping Study Cockpit.

Based on the introspection schema from:
https://app-kolping-prod-gateway.azurewebsites.net/graphql

Available queries discovered:
- myStudentGradeOverview: Student grade overview
- myStudentData: Student personal data
- myTranscriptOfRecords: Academic transcript
- myCertificateOfStudy: Certificate of study
- moduls: List of all modules
- modul(id): Single module by ID
- pruefungs: List of all exams
- pruefungsergebnis(id): Single exam result by ID
- semesters: List of all semesters
- matchModulStudent: Student-module mappings
- students: List of students
"""

from dataclasses import dataclass
from typing import Any

import httpx

from kolping_cockpit.settings import get_settings


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

    Handles authentication and provides typed query methods.
    NOTE: This API works WITHOUT authentication for public queries!
    Personal data (myStudentData etc.) requires auth.
    """

    # GraphQL queries based on discovered schema (introspection 2026-01-11)
    # Field names from __type introspection queries
    QUERIES = {
        "myStudentData": """
            query MyStudentData {
                myStudentData {
                    studentId
                    vorname
                    nachname
                    emailKh
                    emailPrivat
                    geburtsdatum
                    geburtsort
                    strasse
                    hausnummer
                    plz
                    wohnort
                    telefonnummer
                    hochschulsemester
                    studienstartSemesterId
                    studienstartStudiengangKuerzel
                    anrede
                    titel
                    benutzername
                }
            }
        """,
        "myStudentGradeOverview": """
            query MyStudentGradeOverview {
                myStudentGradeOverview {
                    studentId
                    note
                    gesamtEcts
                    offeneEcts
                    bestandeneEcts
                }
            }
        """,
        "myTranscriptOfRecords": """
            query MyTranscriptOfRecords {
                myTranscriptOfRecords {
                    studentId
                }
            }
        """,
        "myCertificateOfStudy": """
            query MyCertificateOfStudy {
                myCertificateOfStudy {
                    studentId
                }
            }
        """,
        "moduls": """
            query Moduls {
                moduls {
                    id
                    modulName
                    modulNameIntern
                    modulkuerzel
                    ectspunkte
                    workload
                    qualifikationsziele
                    inhalte
                    dauer
                }
            }
        """,
        "semesters": """
            query Semesters {
                semesters {
                    id
                    semesterName
                    semesterType
                    semesterPeriode
                    sortierung
                }
            }
        """,
        "pruefungs": """
            query Pruefungs {
                pruefungs {
                    id
                    modulId
                    datum
                    pruefungsformId
                    pruefungsortId
                }
            }
        """,
        "matchModulStudents": """
            query MatchModulStudents {
                matchModulStudents {
                    id
                    studentId
                    modulId
                    note
                    ects
                    modulStudentStatusId
                }
            }
        """,
        "studiengangs": """
            query Studiengangs {
                studiengangs {
                    id
                    kuerzel
                    bezeichnung
                    ects
                }
            }
        """,
    }

    # Simplified queries for initial testing (less fields = less likely to fail)
    QUERIES_SIMPLE = {
        "myStudentData": """
            query { myStudentData { studentId vorname nachname emailKh } }
        """,
        "myStudentGradeOverview": """
            query { myStudentGradeOverview { studentId note gesamtEcts } }
        """,
        "moduls": """
            query { moduls { id modulName modulkuerzel ectspunkte } }
        """,
        "semesters": """
            query { semesters { id semesterName semesterPeriode } }
        """,
        "pruefungs": """
            query { pruefungs { id modulId datum } }
        """,
        "studiengangs": """
            query { studiengangs { id kuerzel bezeichnung } }
        """,
    }

    def __init__(self, bearer_token: str | None = None):
        """Initialize the GraphQL client.

        Args:
            bearer_token: Optional bearer token (NOT required for this API).
        """
        self.settings = get_settings()
        self.endpoint = self.settings.graphql_endpoint

        # Note: This API does NOT require authentication
        # Bearer token causes "Unexpected Execution Error"
        self._bearer_token = None  # Disabled - API works without auth

        self._client: httpx.Client | None = None

    @property
    def client(self) -> httpx.Client:
        """Get or create HTTP client."""
        if self._client is None:
            headers = {
                "Content-Type": "application/json",
                "Accept": "application/json",
            }
            # Note: Do NOT send Bearer token - causes errors

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

    # Convenience methods for each query type
    def get_my_student_data(self, simple: bool = False) -> GraphQLResponse:
        """Get current student's personal data."""
        return self.execute_named_query("myStudentData", simple=simple)

    def get_my_grade_overview(self, simple: bool = False) -> GraphQLResponse:
        """Get current student's grade overview."""
        return self.execute_named_query("myStudentGradeOverview", simple=simple)

    def get_my_transcript(self) -> GraphQLResponse:
        """Get current student's transcript of records."""
        return self.execute_named_query("myTranscriptOfRecords")

    def get_my_certificate(self) -> GraphQLResponse:
        """Get current student's certificate of study."""
        return self.execute_named_query("myCertificateOfStudy")

    def get_modules(self, simple: bool = False) -> GraphQLResponse:
        """Get all available modules."""
        return self.execute_named_query("moduls", simple=simple)

    def get_module(self, module_id: str) -> GraphQLResponse:
        """Get a specific module by ID."""
        query = """
            query GetModul($id: ID!) {
                modul(id: $id) {
                    id name code beschreibung ects semester
                    pruefungsform studiengang verantwortlicher
                }
            }
        """
        return self.execute(query, variables={"id": module_id})

    def get_semesters(self, simple: bool = False) -> GraphQLResponse:
        """Get all semesters."""
        return self.execute_named_query("semesters", simple=simple)

    def get_exams(self, simple: bool = False) -> GraphQLResponse:
        """Get all exam appointments (Prüfungen)."""
        return self.execute_named_query("pruefungs", simple=simple)

    def get_exam_result(self, exam_id: str) -> GraphQLResponse:
        """Get a specific exam result."""
        query = """
            query GetPruefungsergebnis($id: ID!) {
                pruefungsergebnis(id: $id) {
                    id modulId modulName note ects datum versuch status
                }
            }
        """
        return self.execute(query, variables={"id": exam_id})

    def get_student_modules(self) -> GraphQLResponse:
        """Get student-module mappings."""
        return self.execute_named_query("matchModulStudents")

    def get_studiengangs(self, simple: bool = False) -> GraphQLResponse:
        """Get all study programs."""
        return self.execute_named_query("studiengangs", simple=simple)

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

        # Public queries (no auth required)
        public_queries = [
            ("modules", self.get_modules),
            ("semesters", self.get_semesters),
            ("exams", self.get_exams),
            ("studiengangs", self.get_studiengangs),
        ]

        # Personal queries (require auth)
        personal_queries = [
            ("student_data", self.get_my_student_data),
            ("grade_overview", self.get_my_grade_overview),
            ("student_modules", self.get_student_modules),
        ]

        # Execute public queries first
        for name, query_func in public_queries:
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

        # Execute personal queries (may fail without auth)
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

        # Also try full queries if not using simple mode
        if not simple:
            try:
                transcript = self.get_my_transcript()
                if transcript.data:
                    results["data"]["transcript"] = transcript.data
                if transcript.has_errors:
                    results["errors"]["transcript"] = [e.message for e in transcript.errors or []]
            except Exception as e:
                results["errors"]["transcript"] = str(e)

            try:
                certificate = self.get_my_certificate()
                if certificate.data:
                    results["data"]["certificate"] = certificate.data
                if certificate.has_errors:
                    results["errors"]["certificate"] = [e.message for e in certificate.errors or []]
            except Exception as e:
                results["errors"]["certificate"] = str(e)

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
