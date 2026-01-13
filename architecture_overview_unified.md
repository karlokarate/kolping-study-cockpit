# Kolping App — Unified Architecture Overview

This document merges:
1) The **latest full-repo Architecture Overview** (Python project + KMP project), created using the strict “Full Linear File Walk” workflow.
2) The **previous KMP-focused Architecture Overview** (AndroidApp + shared) to preserve earlier structure and terminology.

> Scope note: The repo currently contains **two codebases living side-by-side**:
> - a Python CLI/connector project (`kolping-study-cockpit-main`)
> - a Kotlin Multiplatform project (`kmp/`) containing `androidApp` and `shared`

---

## 1) Executive Summary

- **KMP (kmp/)** is the *actual app codebase*: Android UI (Compose), offline cache (Room), sync pipeline (SyncManager + DownloadManager), and a shared KMP engine (`shared`) with Ktor clients for GraphQL/Moodle/Moodle AJAX plus a `StudyRepository` facade.
- **Python (kolping-study-cockpit-main)** is a *separate automation/connector tool*: CLI + connector (Playwright/keyring), exports and CI/quality tooling. There is **no explicit code-level coupling** between Python and KMP at the moment.
- Planned next major feature: **Browser Recorder + Record-Chains + Map Graph + LLM-friendly exports**, to be integrated primarily into the KMP app (platform hooks in Android, core logic reusable).

---

## 2) Mandatory Analysis Method (for traceability)

This merged overview is based on:
- extracting **all provided archives**
- building a **complete tree listing**
- generating a **full file list** (all files), sorted alphabetically
- reading **each file fully top-to-bottom** internally in that order
- producing architecture only **after** full context was ingested

(Implementation details and per-file cards can be generated on demand.)

---

## 3) Repository Topology (Two Codebases)

### 3.1 Python Project (kolping-study-cockpit-main)
Purpose: local automation toolchain (CLI + connector) to log in, fetch data, and write exports.

High-level responsibilities:
- CLI UX (Typer/Rich)
- connector: auth + navigation via Playwright
- credential storage via keyring
- exports to `exports/YYYY-MM-DD/*.json`
- CI + secret hygiene (pre-commit, gitleaks, workflow)

### 3.2 KMP Project (kmp/)
Purpose: the cross-platform app foundation (Android now; web later).

Sub-structure:
- `androidApp`: Android UI + platform storage + offline + sync
- `shared`: KMP commonMain engine (clients, models, repository facade)

---

## 4) Whole-Repo Architecture Map (Python + KMP)

```mermaid
flowchart TB
  subgraph PY[Python Project: kolping-study-cockpit-main]
    PYCLI[CLI (Typer/Rich)] --> PYCONN[Local Connector (Playwright+Keyring)]
    PYCONN --> PYEXPORT[exports/YYYY-MM-DD/*.json]
    PYCONN --> PYSEC[Secret hygiene (gitleaks/pre-commit/CI)]
  end

  subgraph KMP[KMP Project: kmp]
    subgraph ANDR[androidApp (Android UI + Offline + Sync)]
      AUI[Compose Screens/Components] --> AVM[ViewModels]
      AVM --> AOFF[OfflineRepository (Room)]
      AVM --> ASYNC[SyncManager + DownloadManager]
      ASYNC --> AFS[FileStorageManager]
      AAUTH[EntraAuthWebView] --> ATOK[TokenManager (DataStore)]
    end

    subgraph SHARED[shared (KMP commonMain)]
      SREP[StudyRepository Facade] --> SGQL[GraphQLClient (Ktor)]
      SREP --> SMOOD[MoodleClient (Ktor)]
      SREP --> SAJAX[MoodleAjaxClient (Ktor)]
      SGQL --> SMODEL1[Student Models]
      SMOOD --> SMODEL2[Moodle Models]
      SAJAX --> SMODEL3[MoodleAjax Models]
    end

    ATOK --> SREP
    ASYNC --> SREP
    ASYNC --> ADB[(Room DB: entities/daos)]
  end
```

---

## 5) KMP Architecture (Detailed)

### 5.1 Dataflow (runtime)

1) **Login**
- UI uses `EntraAuthWebView` (WebView-based Entra SSO)
- `TokenManager` persists **bearer token** + **Moodle session cookie** via DataStore

2) **Online fetch (live)**
- ViewModels call `StudyRepository` (shared)
- `StudyRepository` calls:
  - GraphQL gateway via `GraphQLClient`
  - Moodle endpoints via `MoodleClient`
  - Moodle AJAX via `MoodleAjaxClient`

3) **Offline / cache**
- `SyncManager` orchestrates periodic sync
- results stored in Room (`KolpingDatabase`, Entities + DAOs)
- UI reads cached data via `OfflineRepository` (Flow-based)
- files downloaded via `DownloadManager` + stored via `FileStorageManager`
- Offline Library shows files; PDF viewer renders PDFs

### 5.2 Feature Inventory (KMP)

**Auth**
- `EntraAuthWebView` (SSO)
- `TokenManager` (DataStore)

**Online Screens**
- Dashboard / Grades / Courses

**Offline Screens**
- Home (offline summary)
- Calendar (offline events)
- Module detail (offline)
- Offline library + PDF viewer

**Sync**
- phase-based orchestrator (`SyncManager`)
- downloader (`DownloadManager`)
- progress/result types (`SyncPhase`, `SyncProgress`, `SyncResult`)

**Storage**
- local filesystem management (`FileStorageManager`)
- room cache entities + DAOs

### 5.3 Package Map (KMP)

#### androidApp
- `de.kolping.cockpit.android`
  - `KolpingCockpitApp.kt`: Application entry, Koin init
  - `MainActivity.kt`: Compose root + screen routing
- `de.kolping.cockpit.android.auth`
  - `EntraAuthWebView.kt`: WebView login flow
  - `TokenManager.kt`: DataStore secrets store (bearer + session cookie)
- `de.kolping.cockpit.android.di`
  - `AppModule.kt`: Koin wiring
- `de.kolping.cockpit.android.database`
  - `KolpingDatabase.kt`: Room DB
- `de.kolping.cockpit.android.database.entities`
  - `ModuleEntity`, `CourseEntity`, `CalendarEventEntity`, `StudentProfileEntity`, `FileEntity`
- `de.kolping.cockpit.android.database.dao`
  - DAOs for entities (Flow queries, inserts)
- `de.kolping.cockpit.android.repository`
  - `OfflineRepository.kt`: unified cache access
- `de.kolping.cockpit.android.storage`
  - `FileStorageManager.kt`: local file layout & ops
- `de.kolping.cockpit.android.sync`
  - `SyncManager`, `DownloadManager`, `SyncPhase`, `SyncProgress`, `SyncResult`
- `de.kolping.cockpit.android.ui.*`
  - screens + components + theme
- `de.kolping.cockpit.android.viewmodel`
  - viewmodels per screen

#### shared (commonMain)
- `de.kolping.cockpit.api`
  - `GraphQLClient.kt`
  - `MoodleClient.kt`
  - `MoodleAjaxClient.kt`
- `de.kolping.cockpit.models`
  - `Student.kt`, `Moodle.kt`, `MoodleAjax.kt`
- `de.kolping.cockpit.repository`
  - `StudyRepository.kt`

---

## 6) Python Project Architecture (Detailed)

### 6.1 Purpose
A separate CLI/connector codebase to automate:
- authentication and navigation (Playwright)
- export normalized JSON for later analysis
- provide quality tooling (ruff, pyright, pytest, pre-commit)
- prevent secret leakage (gitleaks, gitignore)

### 6.2 Package Map
- `src/kolping_cockpit/__init__.py`: package API + version
- `src/kolping_cockpit/cli.py`: CLI commands scaffold (Typer/Rich)
- `src/kolping_cockpit/connector.py`: connector scaffold (Playwright + keyring)
- `tests/`: CLI + connector tests scaffolding
- `.github/workflows/ci.yml`: CI automation

> Note: Some files in the Python codebase contain scaffolding/placeholders; the structure is present but parts may not be executable yet.

---

## 7) Current Gaps vs. Desired Vision

### 7.1 Not yet implemented in KMP
- Record-Chains (runtime-created)
- Chain points & branching UI
- automatic mapping heuristics (hub/root detection, sidebar switch detection)
- Recorder session capture (UI actions + requests/responses correlation)
- LLM-friendly export bundles (map.json + sessions + derived schemas)

### 7.2 No coupling Python ↔ KMP
Currently, Python and KMP operate as separate projects. If both remain, define a shared export contract; otherwise consolidate around KMP.

---

## 8) Recommended Next Integration Points (KMP)

Best places to attach the Recorder/Mapping system (no code changes here; just placement):
- Android WebView layer (existing auth WebView patterns)
- `shared`: data models for recorder events, map graph, export schema derivation
- Android storage: reuse `FileStorageManager` for export bundles
- Offline DB: optional Room tables for map graph (nodes/edges/sessions)

---

## 9) Polish later
- Hardening of secret handling (especially around exports)
- Stronger redaction policy with configurable profiles
- CI gates expansion (Detekt/ktlint/Android lint on KMP side)
- Full schema inference + storage hinting for all endpoints


---

## Appendix A — Previous `architecture_overview.md` (verbatim)

Architecture_Overview
https://chatgpt.com/s/t_69660fca44cc8191ad2296d4b98823af

kmp.zip entpackt und alle .kt-Dateien analysiert (55 Kotlin-Files insgesamt: 48 in androidApp, 7 in shared).
Ich ignoriere bewusst build.gradle/settings.gradle und leite den Ist-Zustand nur aus dem Codeinhalt ab.


---

Architektur-Übersicht (Ist-Zustand)

1) Top-Level Map der “Module” (aus Ordnerstruktur + Codezweck)

flowchart LR
  subgraph KMP[kmp/]
    A[androidApp\nAndroid UI + Storage + Sync] --> S[shared\nKMP commonMain: API + Models + Repository]
  end

  A -->|DataStore Tokens| TM[TokenManager]
  A -->|Room Cache| DB[KolpingDatabase + DAOs + Entities]
  A -->|Sync Pipeline| SY[SyncManager + DownloadManager]
  S -->|Ktor HTTP| API[GraphQLClient + MoodleClient + MoodleAjaxClient]

Kurz gesagt:

androidApp ist die Android-UI + Offline-Cache + Sync-Orchestrierung.

shared ist die KMP-Engine für API-Clients + Models + StudyRepository (Ktor + Serialization).



---

2) Zentraler Datenfluss (so arbeitet die App aktuell)

1. Login



UI zeigt EntraAuthWebView (WebView)

TokenManager speichert Bearer Token + MoodleSession Cookie in DataStore


2. Online-Daten (live)



ViewModels (z.B. Dashboard/Grades/Courses) rufen StudyRepository (shared) auf

StudyRepository nutzt:

GraphQLClient (GraphQL Gateway)

MoodleClient (Moodle HTML/Endpunkte)

MoodleAjaxClient (Moodle AJAX service)



3. Offline/Cache



SyncManager zieht alles und schreibt in Room DB (Entities + DAOs)

OfflineRepository liefert Flows aus Room an UI

FileStorageManager verwaltet lokale Downloads

OfflineLibraryScreen browsed lokale Dateien, PdfViewerScreen rendert PDFs



---

3) File-Map nach Paketen (alle .kt-Files + Zweck)

A) androidApp (48 Dateien)

de.kolping.cockpit.android (2)

1. KolpingCockpitApp.kt
Zweck: Application-Entry — startet Koin (DI) mit appModule.


2. MainActivity.kt
Zweck: Compose Root + Navigation-State. Enthält sealed class Screen (Loading/Login/Dashboard/Home/Grades/Courses/Calendar/ModuleDetail/OfflineLibrary/PdfViewer).
Funktion: App-Screen-Routing ohne externes Nav-Framework.




---

de.kolping.cockpit.android.auth (2)

3. EntraAuthWebView.kt
Zweck: WebView-basierter Microsoft Entra SSO Login. Versucht Session-Cookie + (Fallback) Token aus Page-Kontext zu extrahieren.
Rolle: Auth-UI & Session-Aufbau.


4. TokenManager.kt
Zweck: Persistiert bearer_token + session_cookie in DataStore; bietet Sync- und Clear-Funktionen.
Rolle: Single Source of Truth für Auth-Material auf Android.




---

de.kolping.cockpit.android.di (1)

5. AppModule.kt
Zweck: Koin-Definitionen: DataStore, TokenManager, Room DB + DAOs, Storage, DownloadManager, OfflineRepository, API Clients, StudyRepository, ViewModels.
Rolle: Objektgraph / Wiring.




---

de.kolping.cockpit.android.database (1)

6. KolpingDatabase.kt
Zweck: Room Database Setup inkl. Entities + DAOs; nutzt fallbackToDestructiveMigration (bewusst „Cache“-Charakter).
Rolle: Offline Cache Storage.




---

de.kolping.cockpit.android.database.entities (5)

7. ModuleEntity.kt
Zweck: Offline-Modulinfo (GraphQL Module) inkl. examStatus, pruefungsform, etc.


8. CourseEntity.kt
Zweck: Offline-Kursdaten (Moodle Kursliste).


9. FileEntity.kt
Zweck: Metadaten zu Downloads (lokaler Pfad, URL, Typ, Größe); moduleId oder courseId Pflicht.


10. CalendarEventEntity.kt
Zweck: Offline Kalender-Events (Moodle Events).


11. StudentProfileEntity.kt
Zweck: Offline Student Profile Snapshot (GraphQL Profil + derived summary wie overall grade).




---

de.kolping.cockpit.android.database.dao (5)

12. ModuleDao.kt – CRUD + Flow-Queries für Module


13. CourseDao.kt – CRUD + Flow-Queries für Kurse


14. FileDao.kt – CRUD + Queries für Files (by module/course, search etc.)


15. CalendarEventDao.kt – CRUD + Queries für Events (range/day)


16. StudentProfileDao.kt – CRUD + Queries für Profil




---

de.kolping.cockpit.android.repository (1)

17. OfflineRepository.kt
Zweck: Unified Offline-First Zugriff: Alles lesen über Room Flow, schreiben/clear über DAO Methoden.
Rolle: Lokaler Read-API für UI.




---

de.kolping.cockpit.android.storage (1)

18. FileStorageManager.kt
Zweck: Dateiablage-Struktur (sync/…), sicheren Zugriff, Copy/Save/Delete, Pfadauflösung.
Rolle: Lokale Artefakte/Downloads.




---

de.kolping.cockpit.android.sync (5)

19. SyncManager.kt
Zweck: Orchestriert Gesamt-Sync (Profile/Grades/Courses/Calendar/Content/Files). Schreibt in Room, nutzt DownloadManager. Liefert Flow mit Progress/Result.


20. DownloadManager.kt
Zweck: Datei-Downloads via Ktor HttpClient, Concurrency-Limits, HEAD für Größe, speichert via FileStorageManager.


21. SyncPhase.kt
Zweck: Enum der Sync-Phasen (GRADES/PROFILE/COURSES/CALENDAR/CONTENT/FILES/COMPLETED/FAILED)


22. SyncProgress.kt
Zweck: Progress-Datenklasse (Phase, Item, Progress, Counters; berechnet file/byte progress)


23. SyncResult.kt
Zweck: sealed result (Success/Failure/Cancelled)




---

de.kolping.cockpit.android.ui.components (5)

24. EventCard.kt
Zweck: Compose Card für Kalender-Event Darstellung.


25. ModuleCard.kt
Zweck: Compose Card für Module (Semester/ECTS/Status etc).


26. SharedComponents.kt
Zweck: Wiederverwendbare UI-Bausteine (Header/Loading/Error patterns).


27. SyncButton.kt
Zweck: Prominenter „Alles synchronisieren“-Button + last sync text.


28. SyncProgressDialog.kt
Zweck: Dialog/Progress UI inkl. Fehler/Retry.




---

de.kolping.cockpit.android.ui.screens (9)

29. LoginScreen.kt
Zweck: Screen wrapper um EntraAuthWebView + LoginViewModel.


30. HomeScreen.kt
Zweck: Offline-Home: Profile/Module/Events + Sync Trigger UI.


31. DashboardScreen.kt
Zweck: Online-Dashboard (GradeOverview, MoodleDashboard, Deadlines) über DashboardViewModel.


32. GradesScreen.kt
Zweck: Online Grades Übersicht.


33. CoursesScreen.kt
Zweck: Online Kurse Übersicht (MoodleCourse).


34. CalendarScreen.kt
Zweck: Offline Kalender UI (Room events).


35. ModuleDetailScreen.kt
Zweck: Offline Moduldetails + zugehörige Files.


36. OfflineLibraryScreen.kt
Zweck: Offline File Browser (Suche/Sortierung) über OfflineLibraryViewModel.


37. PdfViewerScreen.kt
Zweck: PDF-Rendering über com.alamin5g.pdf.PDFView.




---

de.kolping.cockpit.android.ui.theme (2)

38. Theme.kt – Compose Theme Setup


39. Type.kt – Typography/Fonts Setup




---

de.kolping.cockpit.android.util (1)

40. FileUtils.kt
Zweck: Dateityp-Kategorisierung (PDF/DOC/XLS/PPT/…) + Zeitformatierung („vor x Min“ etc).




---

de.kolping.cockpit.android.viewmodel (8)

41. LoginViewModel.kt
Zweck: Persistiert Tokens via TokenManager; UI-Loginstate.


42. HomeViewModel.kt
Zweck: Orchestriert Sync (SyncManager) + liest OfflineRepository (Profile/Module/Events); SyncState state machine.


43. DashboardViewModel.kt
Zweck: Online-Aggregat: grade overview + moodle dashboard + upcoming deadlines (StudyRepository).


44. GradesViewModel.kt
Zweck: Online Grades (StudyRepository).


45. CoursesViewModel.kt
Zweck: Online Courses (StudyRepository).


46. CalendarViewModel.kt
Zweck: Offline Calendar (Room events), Auswahl nach Tag/Monat.


47. ModuleDetailViewModel.kt
Zweck: Offline Moduldetail + Files, state via flows.


48. OfflineLibraryViewModel.kt
Zweck: Offline Downloads Browser: Search + Sort + Open via FileStorageManager.




---

B) shared (7 Dateien, commonMain)

de.kolping.cockpit.api (3)

49. GraphQLClient.kt
Zweck: Ktor GraphQL Client, Methoden getMyGradeOverview() und getMyStudentData(); Endpoint ist im Code:
https://app-kolping-prod-gateway.azurewebsites.net/graphql


50. MoodleClient.kt
Zweck: Moodle HTTP Client (Ktor) für Dashboard/Courses/Assignments; Base URL:
https://portal.kolping-hochschule.de


51. MoodleAjaxClient.kt
Zweck: Moodle AJAX Client für lib/ajax/service.php Methoden (z.B. enrolled courses, calendar month view/events) — benötigt sessionCookie + sesskey.




---

de.kolping.cockpit.models (3)

52. Student.kt
Zweck: Serialisierbare Models für Student/Profile/Module/GradeOverview (GraphQL).


53. Moodle.kt
Zweck: Serialisierbare Models für Moodle Dashboard, Course, Assignment, Event etc.


54. MoodleAjax.kt
Zweck: Serialisierbare Models für Moodle AJAX Request/Response-Formate (Call/Result, calendar structures).




---

de.kolping.cockpit.repository (1)

55. StudyRepository.kt
Zweck: Facade/UseCase-Layer über GraphQL + Moodle:



getStudentData, getGradeOverview

getMoodleDashboard, getMoodleCourses, getAssignments, getUpcomingDeadlines

testConnectivity (+ ConnectivityStatus)



---

4) Was diese Codebasis jetzt schon kann

SSO Login via WebView + Tokens/Cookies speichern (DataStore)

Online:

GraphQL Studienstand (Module/ECTS/Noten)

Moodle Dashboard/Courses/Assignments/Deadlines


Offline:

Room Cache für Module/Kurse/Events/Profile/Files

Sync Pipeline (Phasen + Progress + Downloads)

Offline Library + PDF Viewer



5) Was noch nicht enthalten ist (im Code nicht vorhanden)

Record-Chains / Browser-Recorder / Mapping-Graph / Export-Bundles (das ist eure geplante nächste Ausbaustufe)

Automatische „Website Map“ Generierung/Visualisierung (Graph UI)
