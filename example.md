Alles klar — ich gebe dir jetzt (1) Pydantic-v2 Models (raw + normalized) und (2) eine Redaction Map + Utilities, so dass Copilot das direkt in euer Repo legen kann, ohne Tokens/PII zu leaken.

Ich packe es als konkrete Files mit sauberer Struktur. (Pydantic v2 ist hier die richtige Wahl; wenn ihr noch pydantic<2 irgendwo habt: upgraden. Für Speed später gern orjson + msgspec, aber erst Stabilität.)


---

Zielstruktur im Repo

src/kolping_cockpit/models/
  __init__.py
  raw_capture.py
  moodle_ajax.py
  graphql.py
  oidc.py
  normalized.py
src/kolping_cockpit/redaction.py


---

1) src/kolping_cockpit/models/raw_capture.py

from __future__ import annotations

from typing import Any
from pydantic import BaseModel, ConfigDict, Field


class CaptureRequest(BaseModel):
    """
    Generic capture wrapper for a single HTTP request.
    Matches your dumps: request.json top-level keys.
    """
    model_config = ConfigDict(extra="ignore")

    app: str | None = None
    duration: int | None = None
    headers: dict[str, str] = Field(default_factory=dict)
    method: str | None = None
    protocol: str | None = None
    remoteIp: str | None = None
    remotePort: int | None = None
    sessionId: str | None = None
    time: str | None = None
    url: str


class CaptureResponse(BaseModel):
    """
    Generic capture wrapper for a single HTTP response.
    Matches your dumps: response.json top-level keys.
    """
    model_config = ConfigDict(extra="ignore")

    app: str | None = None
    code: int | None = None
    duration: int | None = None
    headers: dict[str, str] = Field(default_factory=dict)
    message: str | None = None
    protocol: str | None = None
    remoteIp: str | None = None
    remotePort: int | None = None
    sessionId: str | None = None
    time: str | None = None


class CapturePair(BaseModel):
    """
    Optional helper if you store request+response together.
    """
    model_config = ConfigDict(extra="ignore")

    request: CaptureRequest
    response: CaptureResponse
    body: Any | None = None  # raw body (HTML/JSON); keep optional


---

2) src/kolping_cockpit/models/moodle_ajax.py

from __future__ import annotations

from typing import Any
from pydantic import BaseModel, ConfigDict, Field


class MoodleAjaxCall(BaseModel):
    """One element in the JSON array posted to /lib/ajax/service.php"""
    model_config = ConfigDict(extra="ignore")

    index: int
    methodname: str
    args: dict[str, Any] = Field(default_factory=dict)


class MoodleAjaxException(BaseModel):
    model_config = ConfigDict(extra="ignore")

    errorcode: str | None = None
    message: str | None = None
    link: str | None = None
    moreinfourl: str | None = None


class MoodleAjaxResult(BaseModel):
    """One element in the JSON array returned by /lib/ajax/service.php"""
    model_config = ConfigDict(extra="ignore")

    error: bool | None = None
    data: Any | None = None
    exception: MoodleAjaxException | None = None


# --- Course list: core_course_get_enrolled_courses_by_timeline_classification ---


class MoodleCourse(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: int
    idnumber: str | None = None
    shortname: str | None = None
    fullname: str | None = None
    fullnamedisplay: str | None = None

    coursecategory: str | None = None
    courseimage: str | None = None

    summary: str | None = None
    summaryformat: int | None = None

    startdate: int | None = None
    enddate: int | None = None

    visible: bool | None = None
    hidden: bool | None = None

    viewurl: str | None = None
    showshortname: bool | None = None

    hasprogress: bool | None = None
    progress: float | int | None = None

    isfavourite: bool | None = None
    showactivitydates: bool | None = None
    showcompletionconditions: bool | None = None


class MoodleEnrolledCoursesData(BaseModel):
    model_config = ConfigDict(extra="ignore")

    courses: list[MoodleCourse] = Field(default_factory=list)
    nextoffset: int | None = None


# --- Calendar: core_calendar_get_calendar_monthly_view ---


class MoodleCalendarDate(BaseModel):
    model_config = ConfigDict(extra="ignore")

    year: int | None = None
    month: int | None = None
    mon: int | None = None
    mday: int | None = None
    wday: int | None = None
    weekday: str | None = None
    yday: int | None = None
    hours: int | None = None
    minutes: int | None = None
    seconds: int | None = None
    timestamp: int | None = None


class MoodleCalendarEventCourse(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: int | None = None
    idnumber: str | None = None
    shortname: str | None = None
    fullname: str | None = None
    fullnamedisplay: str | None = None
    coursecategory: str | None = None
    courseimage: str | None = None
    summary: str | None = None
    summaryformat: int | None = None
    startdate: int | None = None
    enddate: int | None = None
    visible: bool | None = None
    hidden: bool | None = None
    viewurl: str | None = None
    showshortname: bool | None = None
    hasprogress: bool | None = None
    progress: float | int | None = None
    isfavourite: bool | None = None
    showactivitydates: bool | None = None
    showcompletionconditions: bool | None = None


class MoodleCalendarEvent(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: int | None = None
    name: str | None = None
    description: str | None = None
    descriptionformat: int | None = None

    eventtype: str | None = None
    normalisedeventtype: str | None = None
    normalisedeventtypetext: str | None = None

    formattedtime: str | None = None
    url: str | None = None
    viewurl: str | None = None
    icon: str | None = None

    component: str | None = None
    modulename: str | None = None

    categoryid: int | None = None
    groupid: int | None = None
    groupname: str | None = None
    userid: int | None = None
    repeatid: int | None = None
    eventcount: int | None = None

    timestart: int | None = None
    timeduration: int | None = None
    timesort: int | None = None
    timemodified: int | None = None
    timeusermidnight: int | None = None

    visible: bool | None = None
    draggable: bool | None = None
    candelete: bool | None = None
    canedit: bool | None = None

    deleteurl: str | None = None
    editurl: str | None = None
    popupname: str | None = None

    subscription: Any | None = None
    course: MoodleCalendarEventCourse | None = None


class MoodleCalendarDay(BaseModel):
    model_config = ConfigDict(extra="ignore")

    year: int | None = None
    mday: int | None = None
    wday: int | None = None
    yday: int | None = None
    hours: int | None = None
    minutes: int | None = None
    seconds: int | None = None
    timestamp: int | None = None

    daytitle: str | None = None
    popovertitle: str | None = None
    istoday: bool | None = None
    isweekend: bool | None = None
    hasevents: bool | None = None
    haslastdayofevent: bool | None = None

    neweventtimestamp: int | None = None
    viewdaylink: str | None = None
    viewdaylinktitle: str | None = None

    calendareventtypes: Any | None = None
    nextperiod: Any | None = None
    previousperiod: Any | None = None
    navigation: Any | None = None

    events: list[MoodleCalendarEvent] = Field(default_factory=list)


class MoodleCalendarWeek(BaseModel):
    model_config = ConfigDict(extra="ignore")
    days: list[MoodleCalendarDay] = Field(default_factory=list)


class MoodleCalendarMonthlyViewData(BaseModel):
    model_config = ConfigDict(extra="ignore")

    categoryid: int | None = None
    courseid: int | None = None
    date: MoodleCalendarDate | None = None
    daynames: list[str] = Field(default_factory=list)

    defaulteventcontext: Any | None = None
    includenavigation: bool | None = None
    initialeventsloaded: bool | None = None

    larrow: str | None = None
    rarrow: str | None = None

    nextperiod: MoodleCalendarDate | None = None
    previousperiod: MoodleCalendarDate | None = None
    nextperiodlink: str | None = None
    nextperiodname: str | None = None
    previousperiodlink: str | None = None
    previousperiodname: str | None = None

    periodname: str | None = None
    url: str | None = None
    view: str | None = None

    weeks: list[MoodleCalendarWeek] = Field(default_factory=list)


---

3) src/kolping_cockpit/models/graphql.py

from __future__ import annotations

from typing import Any
from pydantic import BaseModel, ConfigDict, Field


class GraphQLRequest(BaseModel):
    model_config = ConfigDict(extra="ignore")
    operationName: str | None = None
    variables: dict[str, Any] = Field(default_factory=dict)
    query: str


class GraphQLError(BaseModel):
    model_config = ConfigDict(extra="ignore")
    message: str | None = None
    locations: list[dict[str, Any]] = Field(default_factory=list)
    path: list[Any] = Field(default_factory=list)
    extensions: dict[str, Any] = Field(default_factory=dict)


class GraphQLResponse(BaseModel):
    model_config = ConfigDict(extra="ignore")
    data: Any | None = None
    errors: list[GraphQLError] = Field(default_factory=list)
    extensions: dict[str, Any] = Field(default_factory=dict)


# --- Specific operation: getMyStudentGradeOverview ---


class MyStudentModule(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="ignore")

    modulId: int | str | None = None
    semester: int | None = None
    modulbezeichnung: str | None = None

    eCTS: int | float | None = Field(default=None, alias="eCTS")
    eCTSString: str | None = None

    grade: str | float | None = None
    note: str | float | None = None
    points: float | int | None = None

    pruefungsId: int | str | None = None
    pruefungsform: str | None = None
    examStatus: str | None = None
    color: str | None = None

    __typename: str | None = None


class MyStudentProfile(BaseModel):
    """
    Contains PII keys seen in captures.
    We keep them for parsing but will redact by default.
    """
    model_config = ConfigDict(extra="ignore")

    id: int | str | None = None
    benutzername: str | None = None
    vorname: str | None = None
    nachname: str | None = None
    titel: str | None = None
    akademischerGradTnid: int | str | None = None
    geschlechtTnid: int | str | None = None
    geburtsdatum: str | None = None
    geburtsort: str | None = None
    geburtslandTnid: int | str | None = None
    staatsangehoerigkeitTnid: int | str | None = None
    wohnort: str | None = None
    wohnlandTnid: int | str | None = None
    strasse: str | None = None
    hausnummer: str | None = None
    plz: str | None = None
    telefonnummer: str | None = None
    emailKh: str | None = None
    emailPrivat: str | None = None
    notizen: str | None = None
    bemerkung: str | None = None
    createdAt: str | None = None
    __typename: str | None = None


class MyStudentGradeOverview(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="ignore")

    modules: list[MyStudentModule] = Field(default_factory=list)
    grade: str | float | None = None
    eCTS: int | float | None = Field(default=None, alias="eCTS")
    currentSemester: int | None = None
    student: MyStudentProfile | None = None
    __typename: str | None = None


class GetMyStudentGradeOverviewData(BaseModel):
    model_config = ConfigDict(extra="ignore")
    myStudentGradeOverview: MyStudentGradeOverview | None = None


---

4) src/kolping_cockpit/models/oidc.py

from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field


class OidcAuthorizeParams(BaseModel):
    """
    Parsed from authorize URL query params (keys only here; values handled elsewhere).
    """
    model_config = ConfigDict(extra="ignore")

    response_type: str | None = None
    client_id: str | None = None
    scope: str | None = None
    nonce: str | None = None
    response_mode: str | None = None
    state: str | None = None
    redirect_uri: str | None = None
    resource: str | None = None


class OidcCallbackForm(BaseModel):
    """Form POST back to redirect_uri endpoints."""
    model_config = ConfigDict(extra="ignore")

    code: str | None = None
    id_token: str | None = None
    state: str | None = None
    session_state: str | None = None


class TokenResponse(BaseModel):
    """OAuth token endpoint JSON response (keys seen in captures)."""
    model_config = ConfigDict(extra="ignore")

    token_type: str | None = None
    scope: str | None = None
    expires_in: int | None = None
    ext_expires_in: int | None = None

    access_token: str | None = None
    refresh_token: str | None = None
    id_token: str | None = None

    client_info: str | None = None


class OpenIdConfiguration(BaseModel):
    """Subset of .well-known/openid-configuration keys seen."""
    model_config = ConfigDict(extra="ignore")

    authorization_endpoint: str | None = None
    token_endpoint: str | None = None
    jwks_uri: str | None = None
    issuer: str | None = None
    userinfo_endpoint: str | None = None
    end_session_endpoint: str | None = None
    device_authorization_endpoint: str | None = None

    scopes_supported: list[str] = Field(default_factory=list)
    claims_supported: list[str] = Field(default_factory=list)
    response_types_supported: list[str] = Field(default_factory=list)
    response_modes_supported: list[str] = Field(default_factory=list)
    subject_types_supported: list[str] = Field(default_factory=list)
    id_token_signing_alg_values_supported: list[str] = Field(default_factory=list)
    token_endpoint_auth_methods_supported: list[str] = Field(default_factory=list)

    request_uri_parameter_supported: bool | None = None
    frontchannel_logout_supported: bool | None = None
    http_logout_supported: bool | None = None


---

5) Normalform: src/kolping_cockpit/models/normalized.py

Diese Modelle sind die Exports, die ihr langfristig stabil halten wollt (und die KI konsumiert).

from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field


class NormalizedCourse(BaseModel):
    model_config = ConfigDict(extra="ignore")

    source: str = "moodle"
    course_id: int
    name: str | None = None
    url: str | None = None
    progress: float | int | None = None
    is_favourite: bool | None = None


class NormalizedExamInfo(BaseModel):
    model_config = ConfigDict(extra="ignore")

    source: str = "moodle"
    course_id: int
    course_name: str | None = None

    assessment_type: str | None = None  # Lerntagebuch / E-Portfolio / Klausur / Präsentation
    exam_period_start: str | None = None  # ISO date if parsed
    exam_period_end: str | None = None    # ISO date if parsed

    submission_system: str | None = None  # Mahara/Moodle/Unknown
    mahara_group_id: int | None = None
    mahara_url: str | None = None

    template_required: bool | None = None
    immutable_after_submit: bool | None = None

    raw_source_url: str | None = None  # for traceability (no secrets)


class NormalizedModuleResult(BaseModel):
    model_config = ConfigDict(extra="ignore")

    source: str = "graphql"
    module_id: int | str | None = None
    semester: int | None = None
    name: str | None = None
    ects: float | int | None = None

    exam_status: str | None = None
    exam_type: str | None = None  # pruefungsform
    grade: str | float | None = None
    note: str | float | None = None
    points: float | int | None = None


class NormalizedGradeOverview(BaseModel):
    model_config = ConfigDict(extra="ignore")

    ects_total: int | None = None  # optional if you compute 180 elsewhere
    ects_earned: float | int | None = None
    current_semester: int | None = None
    overall_grade: str | float | None = None

    modules: list[NormalizedModuleResult] = Field(default_factory=list)


class ExportMetadata(BaseModel):
    model_config = ConfigDict(extra="ignore")

    tool_version: str | None = None
    generated_at: str | None = None  # ISO datetime
    sources: list[str] = Field(default_factory=list)


class ExportBundle(BaseModel):
    """Convenience bundle if you want one file later."""
    model_config = ConfigDict(extra="ignore")

    meta: ExportMetadata
    courses: list[NormalizedCourse] = Field(default_factory=list)
    exams: list[NormalizedExamInfo] = Field(default_factory=list)
    grades: NormalizedGradeOverview | None = None


---

6) Redaction Map + Utilities: src/kolping_cockpit/redaction.py

Prinzip:

Redact by key name (robust) + by path (präzise)

Redact URL Query Params (state/nonce/code etc.)

Redact HTTP headers (Authorization, Cookie, Set-Cookie)


from __future__ import annotations

from dataclasses import dataclass
from typing import Any
from urllib.parse import urlsplit, urlunsplit, parse_qsl, urlencode


# --- SENSITIVE KEYS (value must never be printed/stored in logs/exports) ---

SENSITIVE_KEYS = {
    # OAuth / OIDC
    "access_token",
    "refresh_token",
    "id_token",
    "code",
    "code_verifier",
    "code_challenge",
    "nonce",
    "state",
    "session_state",

    # headers/cookies
    "authorization",
    "cookie",
    "set-cookie",
    "moodlesession",
}

# Keys that are PII-ish in GraphQL student profile:
PII_KEYS = {
    "benutzername",
    "vorname",
    "nachname",
    "titel",
    "geburtsdatum",
    "geburtsort",
    "telefonnummer",
    "emailkh",
    "emailprivat",
    "strasse",
    "hausnummer",
    "plz",
    "wohnort",
    "notizen",
    "bemerkung",
}

# Redact these query params in URLs:
SENSITIVE_QUERY_PARAMS = {
    "code",
    "id_token",
    "access_token",
    "refresh_token",
    "state",
    "nonce",
    "session_state",
    "client-request-id",
}


REDACTED = "<redacted>"


@dataclass(frozen=True)
class RedactionConfig:
    redact_pii: bool = True
    mask: str = REDACTED
    # if True, keep key but replace value; if False, remove key from dict
    keep_shape: bool = True
    max_depth: int = 50


def redact_url(url: str, cfg: RedactionConfig = RedactionConfig()) -> str:
    try:
        parts = urlsplit(url)
        q = parse_qsl(parts.query, keep_blank_values=True)
        redacted_q = []
        for k, v in q:
            if k.lower() in {p.lower() for p in SENSITIVE_QUERY_PARAMS}:
                redacted_q.append((k, cfg.mask))
            else:
                redacted_q.append((k, v))
        new_query = urlencode(redacted_q, doseq=True)
        return urlunsplit((parts.scheme, parts.netloc, parts.path, new_query, parts.fragment))
    except Exception:
        return url


def redact_headers(headers: dict[str, str], cfg: RedactionConfig = RedactionConfig()) -> dict[str, str]:
    out: dict[str, str] = {}
    for k, v in headers.items():
        kl = k.lower()
        if kl in {"authorization", "cookie", "set-cookie"}:
            out[k] = cfg.mask
            continue
        # also catch MoodleSession cookie name if present in weird maps
        if "moodlesession" in kl:
            out[k] = cfg.mask
            continue
        out[k] = v
    return out


def _should_redact_key(key: str, cfg: RedactionConfig) -> bool:
    kl = key.lower()
    if kl in {s.lower() for s in SENSITIVE_KEYS}:
        return True
    if cfg.redact_pii and kl in {p.lower() for p in PII_KEYS}:
        return True
    return False


def redact_obj(obj: Any, cfg: RedactionConfig = RedactionConfig(), _depth: int = 0) -> Any:
    if _depth > cfg.max_depth:
        return obj

    if isinstance(obj, dict):
        new: dict[str, Any] = {}
        for k, v in obj.items():
            if _should_redact_key(str(k), cfg):
                if cfg.keep_shape:
                    new[k] = cfg.mask
                # else drop key
                continue
            # Special-case URLs (common leak vector: query params)
            if str(k).lower() == "url" and isinstance(v, str):
                new[k] = redact_url(v, cfg)
                continue
            # Special-case headers
            if str(k).lower() == "headers" and isinstance(v, dict):
                new[k] = redact_headers({str(hk): str(hv) for hk, hv in v.items()}, cfg)
                continue

            new[k] = redact_obj(v, cfg, _depth=_depth + 1)
        return new

    if isinstance(obj, list):
        return [redact_obj(x, cfg, _depth=_depth + 1) for x in obj]

    # primitives
    return obj


---

7) Copilot Task (English, precise) — damit er das sauber einbaut

Add Pydantic v2 models and redaction utilities.

Create files:
- src/kolping_cockpit/models/raw_capture.py
- src/kolping_cockpit/models/moodle_ajax.py
- src/kolping_cockpit/models/graphql.py
- src/kolping_cockpit/models/oidc.py
- src/kolping_cockpit/models/normalized.py
- src/kolping_cockpit/redaction.py

Requirements:
- Pydantic v2 BaseModel + ConfigDict(extra="ignore")
- Models must match captured field names (including eCTS alias).
- Redaction:
  - redact tokens/cookies/auth headers + OIDC params (state, nonce, code, session_state)
  - redact GraphQL student profile PII keys by default
  - redact URL query parameters
  - provide config: keep_shape vs drop keys
- Add a small unit test suite for redaction and model parsing with fixture dicts (no real secrets).


---

Externe Tools / bessere Varianten (für später, aber sinnvoll)

HTML Parsing: selectolax ist schneller und stabiler als BeautifulSoup (BS4 ok zum Start).

JSON Speed: orjson ist schneller als stdlib json (wenn ihr große Exports habt).

Schema/Type Safety: msgspec kann später für ultra-schnelle typed decoding interessant sein, aber Pydantic v2 reicht erstmal locker.

Security Gates: zusätzlich zu gitleaks gern trufflehog (stärker bei Entropy/History) in CI.



