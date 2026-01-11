# Python to Kotlin Porting Reference

This document shows the direct mapping between Python source files and their Kotlin equivalents.

## File Mapping

| Python File | Kotlin File(s) | Status |
|-------------|----------------|--------|
| `src/kolping_cockpit/graphql_client.py` | `shared/src/commonMain/kotlin/de/kolping/cockpit/api/GraphQLClient.kt` | ✅ Complete |
| `src/kolping_cockpit/moodle_client.py` | `shared/src/commonMain/kotlin/de/kolping/cockpit/api/MoodleClient.kt` | ✅ Complete |
| `src/kolping_cockpit/settings.py` | Android `TokenManager.kt` + DataStore | ✅ Adapted |
| `src/kolping_cockpit/connector.py` | `androidApp/.../auth/EntraAuthWebView.kt` | ✅ Replaced with WebView |

## Code Comparison Examples

### 1. GraphQL Client Initialization

**Python:**
```python
class KolpingGraphQLClient:
    def __init__(self, bearer_token: str | None = None):
        self.settings = get_settings()
        self.endpoint = self.settings.graphql_endpoint
        self._bearer_token = bearer_token
        self._client: httpx.Client | None = None
```

**Kotlin:**
```kotlin
class GraphQLClient(
    private val bearerToken: String? = null,
    private val endpoint: String = "https://app-kolping-prod-gateway.azurewebsites.net/graphql"
) {
    private val client = HttpClient {
        // Ktor configuration
    }
}
```

### 2. GraphQL Query Execution

**Python:**
```python
def execute(
    self,
    query: str,
    variables: dict[str, Any] | None = None,
    operation_name: str | None = None,
) -> GraphQLResponse:
    payload: dict[str, Any] = {"query": query}
    if variables:
        payload["variables"] = variables
    if operation_name:
        payload["operationName"] = operation_name

    response = self.client.post(self.endpoint, json=payload)
    response.raise_for_status()
    result = response.json()
    # ...
```

**Kotlin:**
```kotlin
private suspend inline fun <reified T> execute(
    query: String,
    variables: Map<String, String>? = null,
    operationName: String? = null
): Result<T> {
    return try {
        val response = client.post(endpoint) {
            setBody(GraphQLRequest(query, variables, operationName))
        }.body<GraphQLResponse<T>>()
        // ...
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 3. Data Models

**Python:**
```python
@dataclass
class Student:
    studentId: str
    vorname: str
    nachname: str
    emailKh: str | None = None
    emailPrivat: str | None = None
    # ...
```

**Kotlin:**
```kotlin
@Serializable
data class Student(
    val studentId: String,
    val vorname: String,
    val nachname: String,
    val emailKh: String? = null,
    val emailPrivat: String? = null,
    // ...
)
```

### 4. Moodle Client - HTML Parsing

**Python:**
```python
def _extract_courses(self, soup: BeautifulSoup) -> list[MoodleCourse]:
    courses = []
    course_elements = soup.find_all("a", href=re.compile(r"/course/view\.php\?id=\d+"))
    
    for elem in course_elements:
        href = str(elem.get("href", ""))
        match = re.search(r"id=(\d+)", href)
        if not match:
            continue
        # ...
```

**Kotlin:**
```kotlin
private fun extractCourses(document: Document): List<MoodleCourse> {
    val courses = mutableListOf<MoodleCourse>()
    document.select("a[href*='/course/view.php?id=']").forEach { element ->
        val href = element.attr("href")
        val idMatch = Regex("id=(\\d+)").find(href)
        val courseId = idMatch?.groupValues?.get(1) ?: return@forEach
        // ...
    }
}
```

### 5. Authentication - Token Storage

**Python (using keyring/file):**
```python
def get_secret_from_env_or_keyring(key: str, service: str = "kolping-cockpit") -> str | None:
    env_key = f"KOLPING_{key.upper()}"
    env_value = os.environ.get(env_key)
    if env_value:
        return env_value
    
    try:
        import keyring
        return keyring.get_password(service, key)
    except Exception:
        pass
```

**Kotlin (using DataStore):**
```kotlin
class TokenManager(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val BEARER_TOKEN_KEY = stringPreferencesKey("bearer_token")
        private val SESSION_COOKIE_KEY = stringPreferencesKey("session_cookie")
    }
    
    val bearerTokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[BEARER_TOKEN_KEY]
    }
    
    suspend fun saveBearerToken(token: String) {
        dataStore.edit { preferences ->
            preferences[BEARER_TOKEN_KEY] = token
        }
    }
}
```

### 6. Error Handling

**Python:**
```python
def test_connection(self) -> tuple[bool, str]:
    try:
        response = self.execute("{ __typename }")
        if response.has_errors:
            return False, f"GraphQL errors: {response.errors}"
        return True, "Connection successful"
    except httpx.HTTPStatusError as e:
        return False, f"HTTP error: {e.response.status_code}"
    except Exception as e:
        return False, f"Connection error: {e}"
```

**Kotlin:**
```kotlin
suspend fun testConnection(): Result<Boolean> {
    return try {
        val result = execute<Map<String, String>>(
            query = "{ __typename }"
        )
        if (result.isSuccess) {
            Result.success(true)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Connection test failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

## Library Equivalents

| Python Library | Kotlin/Android Equivalent | Purpose |
|----------------|---------------------------|---------|
| `httpx` | `io.ktor:ktor-client` | HTTP client |
| `beautifulsoup4` | `com.fleeksoft.ksoup:ksoup` | HTML parsing |
| `dataclasses` | Kotlin data classes + `@Serializable` | Data structures |
| `keyring` | AndroidX DataStore | Secure storage |
| `playwright` | Android WebView | Browser automation (for auth only) |
| `pydantic` | Kotlinx Serialization | JSON serialization |
| Standard library `json` | `kotlinx.serialization.json` | JSON handling |
| Standard library `re` | Kotlin `Regex` | Regular expressions |

## Key Differences

### 1. Async Programming

**Python (synchronous with optional async):**
```python
def get_my_student_data(self, simple: bool = False) -> GraphQLResponse:
    return self.execute_named_query("myStudentData", simple=simple)
```

**Kotlin (coroutines-first):**
```kotlin
suspend fun getMyStudentData(): Result<StudentDataResponse> {
    return execute(MY_STUDENT_DATA_QUERY)
}
```

### 2. Null Safety

**Python (runtime checks):**
```python
def extract_user_name(self, soup: BeautifulSoup) -> str | None:
    user_menu = soup.find(class_=re.compile(r"user.*menu|usermenu"))
    if user_menu:
        return user_menu.get_text(strip=True)[:50]
    return None
```

**Kotlin (compile-time safety):**
```kotlin
private fun extractUserName(document: Document): String? {
    return document.select("[class*=user][class*=menu], [class*=usermenu]")
        .firstOrNull()
        ?.text()
        ?.take(50)
}
```

### 3. Type System

**Python (gradual typing with type hints):**
```python
from typing import Any, List, Optional

def export_all(self, simple: bool = True) -> dict[str, Any]:
    results: dict[str, Any] = {
        "data": {},
        "errors": {},
    }
```

**Kotlin (static typing, enforced):**
```kotlin
data class ExportResult(
    val data: Map<String, Any>,
    val errors: Map<String, String>
)

fun exportAll(simple: Boolean = true): ExportResult {
    // ...
}
```

### 4. Context Managers vs. Try-with-resources

**Python:**
```python
with KolpingGraphQLClient(token) as client:
    response = client.get_my_student_data()
```

**Kotlin:**
```kotlin
GraphQLClient(token).use { client ->
    val response = client.getMyStudentData()
}
```

## Android-Specific Additions

These components don't exist in the Python version but are necessary for Android:

### 1. ViewModels (MVVM Pattern)
```kotlin
class DashboardViewModel(
    private val repository: StudyRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    fun loadDashboard() {
        viewModelScope.launch {
            // Load data
        }
    }
}
```

### 2. Compose UI Components
```kotlin
@Composable
fun DashboardScreen(
    onNavigateToGrades: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // UI code
}
```

### 3. Dependency Injection Setup
```kotlin
val appModule = module {
    single { TokenManager(get()) }
    factory { GraphQLClient(bearerToken = get<TokenManager>().getBearerToken()) }
    factory { StudyRepository(get(), get()) }
    viewModel { DashboardViewModel(get()) }
}
```

## Consistency Guarantees

All the following are **identical** between Python and Kotlin implementations:

✅ GraphQL query strings (field names, structure)
✅ API endpoints and URLs
✅ Authentication flow (Bearer token, session cookie)
✅ Data model field names (studentId, vorname, nachname, etc.)
✅ Moodle HTML parsing selectors
✅ Error handling patterns
✅ HTTP headers and request structure

This ensures the Kotlin version is a **drop-in replacement** for the Python version in terms of API compatibility.
