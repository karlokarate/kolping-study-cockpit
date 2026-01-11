# Kolping Study Cockpit - Kotlin Multiplatform

Kotlin Multiplatform (KMP) Android App für die Kolping-Hochschule.

## Überblick

Dieses Projekt portiert die Python-basierte Logik in ein natives Android-App mit Kotlin Multiplatform.

### Architektur

```
kmp/
├── shared/                    # Gemeinsame Business-Logik
│   ├── api/                   # GraphQL & Moodle Clients
│   ├── models/                # Datenmodelle
│   └── repository/            # Repository-Schicht
├── androidApp/                # Android UI mit Jetpack Compose
│   ├── ui/screens/           # Bildschirme
│   ├── ui/theme/             # Material Design 3 Theme
│   ├── auth/                 # Authentifizierung
│   └── viewmodel/            # ViewModels
```

## Features

### Shared Module
- **GraphQLClient**: Portiert von `graphql_client.py`
  - Bearer Token Authentication
  - `myStudentData` Query
  - `myStudentGradeOverview` Query
  - Endpoint: `https://app-kolping-prod-gateway.azurewebsites.net/graphql`

- **MoodleClient**: Portiert von `moodle_client.py`
  - Session Cookie Authentication
  - Dashboard, Kurse, Assignments, Deadlines
  - HTML Parsing mit Ksoup
  - Base URL: `https://portal.kolping-hochschule.de`

- **Datenmodelle**:
  - Student, Module, GradeOverview (aus GraphQL)
  - MoodleCourse, MoodleEvent, MoodleAssignment, MoodleDashboard (aus Moodle)

### Android App
- **Material Design 3** mit dynamischen Farben
- **Jetpack Compose** UI
- **Screens**:
  - LoginScreen: WebView für Microsoft Entra SSO
  - DashboardScreen: Übersicht mit Noten und Terminen
  - GradesScreen: Detaillierte Notenübersicht
  - CoursesScreen: Moodle-Kursliste
- **Dependency Injection** mit Koin
- **Secure Storage** mit DataStore für Tokens

## Technologie-Stack

### Shared Module
- Kotlin 1.9.22
- Ktor Client 2.3.8 (HTTP & GraphQL)
- Kotlinx Serialization
- Ksoup (HTML Parsing)

### Android App
- Android SDK 26+ (Android 8.0+)
- Jetpack Compose BOM 2024.02.00
- Material Design 3
- Koin 3.5.3 (DI)
- AndroidX DataStore
- AndroidX Security Crypto

## Build & Run

### Voraussetzungen
- JDK 17 oder höher
- Android SDK (für Android Development)

### Build
```bash
cd kmp
./gradlew :shared:build
./gradlew :androidApp:build
```

### Android App installieren
```bash
./gradlew :androidApp:installDebug
```

## Authentifizierung

Die App verwendet ein WebView für die Microsoft Entra SSO-Authentifizierung:

1. WebView lädt `https://cms.kolping-hochschule.de`
2. User meldet sich mit Microsoft-Konto an (inkl. MFA)
3. Nach erfolgreicher Anmeldung:
   - Bearer Token wird aus dem JavaScript-Context extrahiert
   - Session Cookie wird von Moodle gespeichert
4. Tokens werden sicher in DataStore gespeichert

## Unterschiede zum Python-Projekt

| Aspekt | Python | Kotlin Multiplatform |
|--------|--------|---------------------|
| Browser Automation | Playwright | WebView (Microsoft Entra) |
| HTTP Client | httpx | Ktor Client |
| HTML Parsing | BeautifulSoup | Ksoup |
| Token Storage | Keyring / File | DataStore |
| Platform | Desktop (CLI) | Android (Mobile App) |

## Zukünftige Erweiterungen

- iOS Support (Shared Module ist bereits KMP-ready)
- Desktop Support (Compose Multiplatform)
- Offline-Modus mit lokaler Datenbank
- Push-Benachrichtigungen für neue Noten/Termine
- Widget für Homescreen

## Lizenz

Siehe Haupt-README im Repository-Root.
