# Kolping Exams Command

The `kolping exams` command provides a comprehensive overview of exam dates, assessment requirements, and module information for your study program.

## Features

### 1. **Endpoint Analysis** (`--analyze`)

Analyzes all available GraphQL endpoints to show which data sources are accessible:

```bash
kolping exams --analyze
```

Tests queries for:
- `myStudentData` - Personal student information
- `myStudentGradeOverview` - Complete grade overview with modules
- `moduls` - All available modules
- `semesters` - Semester information
- `pruefungs` - Exam appointments
- `studiengangs` - Study program data
- `matchModulStudent` - Your enrolled modules

### 2. **Exam Dates with Details**

Fetches exam appointments from the server with:
- Exact date and time
- Room/location information
- Module assignment
- Assessment type

### 3. **Assessment Requirements**

For each assessment type, provides detailed requirements:

#### Klausur (Written Exam)
- Written exam during examination period
- Registration required
- Exam preparation recommended

#### Lerntagebuch (Learning Journal)
- Regular reflection on learning process
- Documentation in specified format
- Submission via Moodle

#### Präsentation (Presentation)
- Preparation of 10-20 minute presentation
- Handout or slides required
- Presentation to course/instructor

#### Seminararbeit (Seminar Paper)
- Written paper (10-15 pages)
- Academic citation style
- PDF submission via Moodle

#### E-Portfolio
- Digital collection of learning artifacts
- Reflection on learning progress
- Online presentation

#### Mündliche Prüfung (Oral Exam)
- Appointment scheduling with examiner
- Preparation for exam discussion
- Approximately 20-30 minutes

#### Anerkennung (Recognition)
- Proof of practical phase
- Employer confirmation
- Submission via student office

#### Exposé
- Research plan for thesis
- 3-5 pages
- Submission to supervisor

#### Bachelorthesis & Kolloquium
- Academic thesis (40-60 pages)
- Colloquium (30 min defense)
- Registration and topic selection

#### Praxistransferbericht (Practice Transfer Report)
- Report on practical phase (10-15 pages)
- Reflection on practical activity
- Submission via Moodle

### 4. **Module Overview**

Shows modules categorized by status:
- **Registered Exams** (🔴) - Urgent! These are coming up
- **Open Modules** (📝) - Not yet registered
- **Completed Modules** (✓) - Already passed
- **Failed Modules** (⚠️) - Need to be retaken

### 5. **Moodle Integration**

- Links to relevant Moodle courses
- Calendar events and deadlines
- Assignment information
- Resource URLs

## Usage Examples

### Basic Usage

```bash
kolping exams
```

Shows all exam information for the current semester.

### Filter by Semester

```bash
kolping exams --semester 3
```

Shows only modules and exams from semester 3.

### With Endpoint Analysis

```bash
kolping exams --analyze
```

First analyzes all GraphQL endpoints, then displays exam information.

### Include Completed Modules

```bash
kolping exams --completed
```

Also shows modules you've already passed (default: hidden).

## Output Sections

### 1. Student Information
- Current semester
- Grade average
- Total ECTS earned

### 2. Registered Exams
Table showing:
- Module name
- Semester
- Assessment type
- ECTS credits
- Exam date/time/location

### 3. Requirements for Registered Exams
Detailed panel for each registered exam explaining:
- What assessment type is required
- What you need to prepare
- How to submit/complete it

### 4. Open Modules
Table of modules not yet registered with:
- Module name
- Semester
- Assessment type
- ECTS credits
- Moodle course availability

### 5. Open Modules Grouped by Assessment Type
Lists open modules organized by their assessment type with:
- Number of modules
- Total ECTS
- Requirement description
- List of specific modules

### 6. Calendar Events
Upcoming deadlines from Moodle calendar:
- Event name
- Date and time
- Link availability

### 7. Moodle Courses
List of enrolled courses with:
- Course name
- Direct link to course

### 8. Useful Links
Quick access to:
- Moodle Portal
- Mein Studium (CMS)
- Calendar

## Data Sources

The command combines data from two sources:

### GraphQL API (Mein Studium)
- Student data
- Grade overview
- Module information
- Exam appointments
- Enrollment status

### Moodle Portal
- Calendar events
- Course list
- Assignment deadlines
- Course materials

## Requirements

### Authentication

You need valid authentication for both data sources:

```bash
# Set GraphQL token
kolping set-graphql

# Set Moodle session
kolping set-moodle
```

Or use the interactive login:

```bash
kolping login-manual
```

### Tokens

The command requires:
1. **GraphQL Bearer Token** - From cms.kolping-hochschule.de
2. **Moodle Session Cookie** - From portal.kolping-hochschule.de

## Error Handling

If data sources are unavailable, the command:
- Shows which sources failed
- Continues with available data
- Displays partial results

Example:
```
⚠ Einige Datenquellen nicht verfügbar:
  GraphQL: Kein Bearer Token konfiguriert
  Moodle: Session abgelaufen
```

## Tips

1. **Run regularly** to stay updated on exam dates
2. **Use `--analyze`** to troubleshoot connection issues
3. **Filter by semester** to focus on current modules
4. **Export data** for offline reference: `kolping export all`
5. **Check calendar** in Moodle for detailed event information

## Related Commands

- `kolping deadlines` - Quick view of upcoming deadlines
- `kolping fetch` - Full online data fetch with detailed output
- `kolping export all` - Export all data to JSON
- `kolping analyze` - Offline analysis of captured data
- `kolping status` - Check authentication status

## Example Output

```
📚 Kolping Study Cockpit - Prüfungstermine & Leistungsübersicht
======================================================================

📊 Aktuelles Semester: 5. Semester WiSe 2025/2026
Notendurchschnitt: 1.14
Erreichte ECTS: 25

🔴 ANGEMELDETE PRÜFUNGEN MIT TERMINEN
┏━━━━━━━━━━━━━━━━━━━━━━┳━━━━━┳━━━━━━━━━━━━━━━┳━━━━━┳━━━━━━━━━━━━━┓
┃ Modul                 ┃ Sem.┃ Prüfungsform   ┃ ECTS┃ Termin       ┃
┡━━━━━━━━━━━━━━━━━━━━━━╇━━━━━╇━━━━━━━━━━━━━━━╇━━━━━╇━━━━━━━━━━━━━┩
│ Qualitätsmanagement…  │  3  │ Klausur        │  5  │ 15.02.2026…  │
│ Embodiment            │  3  │ Präsentation   │  5  │ Siehe Kal…   │
└───────────────────────┴─────┴────────────────┴─────┴──────────────┘

📋 Was du für die angemeldeten Prüfungen brauchst:

╭─ Qualitätsmanagement im Gesundheits- und Sozialsystem ────────────╮
│ Prüfungsform: Klausur                                             │
│ Erforderlich:                                                     │
│   • Schriftliche Prüfung im Prüfungszeitraum                     │
│   • Anmeldung erforderlich                                       │
│   • Prüfungsvorbereitung empfohlen                              │
╰───────────────────────────────────────────────────────────────────╯

📝 OFFENE MODULE (Semester 5)
[Table with open modules...]

📅 KOMMENDE TERMINE & DEADLINES (Moodle)
[Calendar events...]

🔗 MOODLE KURSE & MATERIALIEN
[Course list with links...]

💡 Nützliche Links:
  • Moodle Portal: https://portal.kolping-hochschule.de
  • Mein Studium: https://cms.kolping-hochschule.de
  • Kalender: https://portal.kolping-hochschule.de/calendar/view.php
```
