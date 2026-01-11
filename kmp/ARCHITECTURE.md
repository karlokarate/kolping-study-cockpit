# Kolping Study Cockpit - Architecture Diagram

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Android Application                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                        UI Layer (Compose)                     │  │
│  ├───────────────────────────────────────────────────────────────┤  │
│  │  LoginScreen  │  DashboardScreen  │  GradesScreen  │  Courses │  │
│  │     (Auth)    │    (Overview)     │   (Details)    │  (List)  │  │
│  └───────┬───────────────┬───────────────┬──────────────────┬────┘  │
│          │               │               │                  │        │
│  ┌───────▼───────────────▼───────────────▼──────────────────▼────┐  │
│  │                   ViewModel Layer                            │  │
│  ├──────────────────────────────────────────────────────────────┤  │
│  │ LoginVM │ DashboardVM │ GradesVM │ CoursesVM                 │  │
│  │         │             │          │                           │  │
│  │         └─────────────┴──────────┴───────────┬───────────────┘  │
│  │                                               │                   │
│  │  ┌────────────────────────────────────────────▼────────────────┐ │
│  │  │              Dependency Injection (Koin)                    │ │
│  │  └────────────────────────────────────────────┬────────────────┘ │
│  │                                               │                   │
│  └───────────────────────────────────────────────┼───────────────────┘
│                                                  │
│  ┌───────────────────────────────────────────────▼────────────────┐  │
│  │                     Data Layer                               │  │
│  ├──────────────────────────────────────────────────────────────┤  │
│  │  TokenManager         │      StudyRepository                 │  │
│  │  (DataStore)          │      (Coordinator)                   │  │
│  └──────┬────────────────┴───────────┬──────────────────────────┘  │
│         │                            │                              │
└─────────┼────────────────────────────┼──────────────────────────────┘
          │                            │
          │                            │
┌─────────▼────────────────────────────▼──────────────────────────────┐
│                    Shared Module (KMP)                               │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                      API Clients                               │ │
│  ├────────────────────────────────────────────────────────────────┤ │
│  │                                                                │ │
│  │  ┌─────────────────────────┐    ┌─────────────────────────┐  │ │
│  │  │    GraphQLClient        │    │     MoodleClient        │  │ │
│  │  ├─────────────────────────┤    ├─────────────────────────┤  │ │
│  │  │ • Bearer Token Auth     │    │ • Session Cookie Auth   │  │ │
│  │  │ • myStudentData         │    │ • getDashboard()        │  │
│  │  │ • myGradeOverview       │    │ • getCourses()          │  │
│  │  │ • Ktor HTTP Client      │    │ • getAssignments()      │  │
│  │  │ • JSON Parsing          │    │ • HTML Parsing (Ksoup)  │  │
│  │  └──────────┬──────────────┘    └──────────┬──────────────┘  │ │
│  │             │                               │                 │ │
│  └─────────────┼───────────────────────────────┼─────────────────┘ │
│                │                               │                   │
│  ┌─────────────▼───────────────────────────────▼─────────────────┐ │
│  │                     Data Models                               │ │
│  ├───────────────────────────────────────────────────────────────┤ │
│  │ Student, Module, GradeOverview                               │ │
│  │ MoodleCourse, MoodleEvent, MoodleAssignment, MoodleDashboard │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
                                │
                ┌───────────────┴───────────────┐
                │                               │
        ┌───────▼────────┐              ┌──────▼──────┐
        │   GraphQL API  │              │   Moodle    │
        ├────────────────┤              ├─────────────┤
        │ app-kolping-   │              │ portal.     │
        │ prod-gateway.  │              │ kolping-    │
        │ azurewebsites  │              │ hochschule  │
        │ .net/graphql   │              │ .de         │
        └────────────────┘              └─────────────┘
```

## Authentication Flow

```
┌──────────────┐
│   User       │
│   Opens App  │
└──────┬───────┘
       │
       │ Check stored tokens
       ▼
┌──────────────┐      No       ┌─────────────────────┐
│  Token       ├──────────────>│   LoginScreen       │
│  Manager     │               │   (WebView)         │
└──────┬───────┘               └──────────┬──────────┘
       │                                  │
       │ Yes                              │ Load CMS URL
       │                                  ▼
       │                       ┌─────────────────────┐
       │                       │ Microsoft Entra SSO │
       │                       │ (User logs in)      │
       │                       └──────────┬──────────┘
       │                                  │
       │                                  │ Auth Success
       │                                  ▼
       │                       ┌─────────────────────┐
       │                       │ Extract Tokens:     │
       │              ┌────────┤ • Bearer Token      │
       │              │        │ • Session Cookie    │
       │              │        └─────────────────────┘
       │              │
       │              │ Save tokens
       │              ▼
       │        ┌──────────────┐
       └───────>│  Dashboard   │
                │  Screen      │
                └──────────────┘
```

## Data Flow Example: Loading Dashboard

```
1. User opens Dashboard
        │
        ▼
2. DashboardViewModel.loadDashboard()
        │
        ├─> viewModelScope.launch { ... }
        │
        ▼
3. StudyRepository methods called in parallel:
        │
        ├─> getGradeOverview()
        │       │
        │       └─> GraphQLClient.getMyGradeOverview()
        │               │
        │               └─> POST with Bearer token
        │
        ├─> getMoodleDashboard()
        │       │
        │       └─> MoodleClient.getDashboard()
        │               │
        │               └─> GET with Session cookie
        │
        └─> getUpcomingDeadlines()
                │
                └─> MoodleClient.getUpcomingDeadlines()
                        │
                        └─> GET /calendar/view.php
        │
        ▼
4. Results combined into DashboardUiState
        │
        ▼
5. StateFlow emits to UI
        │
        ▼
6. Compose recomposes with new data
        │
        ▼
7. User sees updated dashboard
```

## Technology Stack Layers

```
┌─────────────────────────────────────────────────────┐
│              UI Framework                           │
│  Jetpack Compose + Material Design 3                │
└─────────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────────┐
│         Presentation Layer                          │
│  ViewModels + StateFlow + Lifecycle                 │
└─────────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────────┐
│         Dependency Injection                        │
│  Koin (Service Locator)                             │
└─────────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────────┐
│         Domain Layer                                │
│  Repository Pattern + Use Cases                     │
└─────────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────────┐
│         Data Layer (Kotlin Multiplatform)           │
│  API Clients + Models + Serialization               │
└─────────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────────┐
│         Network Layer                               │
│  Ktor Client + Ksoup HTML Parser                    │
└─────────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────────┐
│         Persistence                                 │
│  DataStore (Encrypted Token Storage)                │
└─────────────────────────────────────────────────────┘
```

## Module Dependencies

```
androidApp
    │
    ├─> shared (KMP module)
    │       │
    │       ├─> ktor-client-core
    │       ├─> kotlinx-serialization
    │       ├─> kotlinx-coroutines
    │       └─> ksoup
    │
    ├─> compose-bom (UI)
    ├─> koin (DI)
    ├─> datastore (Storage)
    └─> security-crypto (Encryption)
```

## File Structure Tree

```
kmp/
│
├── shared/                           # Multiplatform Business Logic
│   ├── build.gradle.kts              # KMP configuration
│   └── src/
│       ├── commonMain/kotlin/        # Platform-independent code
│       │   └── de/kolping/cockpit/
│       │       ├── api/              # API clients
│       │       │   ├── GraphQLClient.kt
│       │       │   └── MoodleClient.kt
│       │       ├── models/           # Data models
│       │       │   ├── Student.kt
│       │       │   └── Moodle.kt
│       │       └── repository/       # Repository pattern
│       │           └── StudyRepository.kt
│       └── androidMain/kotlin/       # Android-specific code (if needed)
│
├── androidApp/                       # Android Application
│   ├── build.gradle.kts              # Android app configuration
│   └── src/main/
│       ├── AndroidManifest.xml       # App manifest
│       ├── java/de/kolping/cockpit/android/
│       │   ├── MainActivity.kt       # App entry point
│       │   ├── KolpingCockpitApp.kt  # Application class
│       │   │
│       │   ├── auth/                 # Authentication
│       │   │   ├── EntraAuthWebView.kt
│       │   │   └── TokenManager.kt
│       │   │
│       │   ├── di/                   # Dependency Injection
│       │   │   └── AppModule.kt
│       │   │
│       │   ├── ui/                   # User Interface
│       │   │   ├── screens/
│       │   │   │   ├── LoginScreen.kt
│       │   │   │   ├── DashboardScreen.kt
│       │   │   │   ├── GradesScreen.kt
│       │   │   │   └── CoursesScreen.kt
│       │   │   └── theme/
│       │   │       ├── Theme.kt
│       │   │       └── Type.kt
│       │   │
│       │   └── viewmodel/            # ViewModels
│       │       ├── LoginViewModel.kt
│       │       ├── DashboardViewModel.kt
│       │       ├── GradesViewModel.kt
│       │       └── CoursesViewModel.kt
│       │
│       └── res/                      # Android resources
│           ├── values/
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── mipmap-*/
│               └── ic_launcher.xml
│
├── gradle/                           # Gradle configuration
│   ├── libs.versions.toml            # Version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties
│
├── build.gradle.kts                  # Root build file
├── settings.gradle.kts               # Project settings
├── gradle.properties                 # Gradle properties
├── gradlew                           # Gradle wrapper
├── .gitignore                        # Git ignore rules
│
└── Documentation/
    ├── README.md                     # Project overview
    ├── IMPLEMENTATION_SUMMARY.md     # Complete feature list
    └── PYTHON_TO_KOTLIN_MAPPING.md   # Porting reference
```

## Key Design Patterns Used

1. **Repository Pattern**: `StudyRepository` coordinates data access
2. **MVVM**: ViewModels manage UI state, Views observe state
3. **Dependency Injection**: Koin provides loose coupling
4. **State Management**: StateFlow for reactive UI updates
5. **Clean Architecture**: Clear separation of layers
6. **Result Type**: Kotlin Result<T> for error handling
7. **Coroutines**: Structured concurrency for async operations

## Security Model

```
User Data Flow:

Login Credentials
      │
      │ (Never stored)
      ▼
Microsoft Entra SSO
      │
      ├─> Bearer Token ──────┐
      │                      │
      └─> Session Cookie ────┤
                             │
                             ▼
                    ┌────────────────┐
                    │  DataStore     │
                    │  (Encrypted)   │
                    └────────────────┘
                             │
                             │ (Read on app start)
                             ▼
                    ┌────────────────┐
                    │  API Clients   │
                    └────────────────┘
                             │
                             │ (Added to headers)
                             ▼
                    ┌────────────────┐
                    │  Backend APIs  │
                    └────────────────┘
```

All sensitive data:
- ✅ Encrypted at rest (DataStore)
- ✅ Transmitted over HTTPS
- ✅ Never logged or printed
- ✅ Cleared on logout
- ✅ Protected by Android OS permissions
