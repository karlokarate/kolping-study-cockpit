Ich hab alle 3 ZIPs ausgewertet (module_kolping 2.zip, portal_auth_kolping 2.zip, portal_kolping_mein_studium 2.zip). Wichtig: Ich liste ausschließlich Feldnamen/Keys/Parameter – keine Werte (keine Tokens, keine Cookies, keine PII-Inhalte).

0) Capture-Format (wie eure Dumps strukturiert sind)

request.json – Top-Level Felder

app

duration

headers (Map)

method

protocol

remoteIp

remotePort

sessionId

time

url


response.json – Top-Level Felder

app

code

duration

headers (Map)

message

protocol

remoteIp

remotePort

sessionId

time



---

1) Systeme & Haupt-Endpunkte (aus den Captures)

Moodle (Portal)

Host: portal.kolping-hochschule.de
Hauptpfade (gesehen):

/my/

/login/index.php

/auth/oidc/

/lib/ajax/service.php

/lib/ajax/setuserpref.php

/course/view.php

/mod/resource/view.php

diverse /pluginfile.php/... (Assets)


“Mein Studium” Web-App

Host: cms.kolping-hochschule.de
Pfade (gesehen):

/myStudent

/.auth/login/aad/callback

/service-worker.js, /manifest.json, /static/... (Assets)


GraphQL Gateway

Host: app-kolping-prod-gateway.azurewebsites.net

/graphql


Microsoft Entra / OIDC

Hosts (gesehen):

login.microsoftonline.com

login.windows.net



---

2) Auth/OIDC – Felder aus Requests/Responses

2.1 OIDC authorize (Query-Parameter, nur Keys)

Authorize-URLs (mehrere Varianten gesehen):

/oauth2/authorize (v1-style)

/oauth2/v2.0/authorize (v2-style)


Query-Parameter Keys (v1 authorize):

response_type

client_id

scope

nonce

response_mode

state

redirect_uri

resource


Query-Parameter Keys (v2 authorize):

client_id

scope

response_type

response_mode

redirect_uri

state

nonce

code_challenge

code_challenge_method

client_info

x-client-SKU

x-client-VER

client-request-id

api-version

authorization_endpoint


2.2 OIDC Callback (Form POST Felder)

Moodle OIDC Callback

Endpoint: portal.kolping-hochschule.de/auth/oidc/

Form fields:

code

state

session_state



Azure App Service Auth Callback (CMS)

Endpoint: cms.kolping-hochschule.de/.auth/login/aad/callback

Form fields:

code

id_token

state

session_state



2.3 Token Exchange (v2.0/token) – Request Body (x-www-form-urlencoded Keys)

Endpoint: login.microsoftonline.com/.../oauth2/v2.0/token

Form fields (gesehen):

client_id

scope

grant_type

client_info

refresh_token

code (kommt in manchen Flows vor)

code_verifier (PKCE)

redirect_uri

client-request-id

x-client-SKU

x-client-VER

x-client-OS

x-client-CPU

x-ms-lib-capability

x-client-current-telemetry

x-client-last-telemetry


2.4 Token Exchange – Response JSON Felder (Keys)

token_type

scope

expires_in

ext_expires_in

access_token

refresh_token

id_token

client_info


2.5 OIDC Discovery – OpenID Configuration (Response JSON Keys)

Endpoint: .../.well-known/openid-configuration

Keys (gesehen):

authorization_endpoint

token_endpoint

jwks_uri

issuer

userinfo_endpoint

end_session_endpoint

device_authorization_endpoint

scopes_supported

claims_supported

response_types_supported

response_modes_supported

subject_types_supported

id_token_signing_alg_values_supported

token_endpoint_auth_methods_supported

request_uri_parameter_supported

frontchannel_logout_supported

http_logout_supported

cloud_instance_name

cloud_graph_host_name

msgraph_host

rbac_url

kerberos_endpoint

tenant_region_scope

tls_client_certificate_bound_access_tokens

mtls_endpoint_aliases


2.6 Instance Discovery (Response JSON Keys)

Endpoint: /common/discovery/instance

Top keys:

tenant_discovery_endpoint

api-version

metadata (Array)


metadata[] keys:

aliases (Array)

preferred_cache

preferred_network



---

3) Moodle JSON API – /lib/ajax/service.php

Allgemein: Query-Parameter Keys

sesskey

info


Allgemein: Request Body Schema (JSON)

Request body ist Array von Calls, jedes Element:

index

methodname

args (Objekt)


Response body ist Array von Results, jedes Element typischerweise:

error

data

optional exception (Objekt)


exception keys (gesehen):

errorcode

message

link

moreinfourl



---

3.1 core_course_get_enrolled_courses_by_timeline_classification

Request args keys

offset

limit

classification

sort

customfieldname

customfieldvalue


Response data keys

courses (Array)

nextoffset


Response courses[] keys

id

idnumber

shortname

fullname

fullnamedisplay

coursecategory

courseimage

summary

summaryformat

startdate

enddate

visible

hidden

viewurl

showshortname

hasprogress

progress

isfavourite

showactivitydates

showcompletionconditions



---

3.2 core_calendar_get_calendar_monthly_view

Request args keys

year

month

day

courseid

categoryid

includenavigation

mini


Response data top keys

categoryid

courseid

date (Objekt)

daynames (Array)

defaulteventcontext

includenavigation

initialeventsloaded

larrow

rarrow

nextperiod (Objekt)

previousperiod (Objekt)

nextperiodlink

nextperiodname

previousperiodlink

previousperiodname

periodname

url

view

weeks (Array)


date / nextperiod / previousperiod (Date-Objekte) keys

year

month

mon

mday

wday

weekday

yday

hours

minutes

seconds

timestamp


weeks[] keys

days (Array)


weeks[].days[] keys

year

mday

wday

yday

hours

minutes

seconds

timestamp

daytitle

popovertitle

istoday

isweekend

hasevents

haslastdayofevent

neweventtimestamp

viewdaylink

viewdaylinktitle

calendareventtypes

nextperiod

previousperiod

navigation

events (Array)


weeks[].days[].events[] keys

id

name

description

descriptionformat

eventtype

normalisedeventtype

normalisedeventtypetext

formattedtime

url

viewurl

icon

component

modulename

categoryid

groupid

groupname

userid

repeatid

eventcount

timestart

timeduration

timesort

timemodified

timeusermidnight

visible

draggable

candelete

canedit

deleteurl

editurl

popupname

subscription

course (Objekt)


weeks[].days[].events[].course keys

id

idnumber

shortname

fullname

fullnamedisplay

coursecategory

courseimage

summary

summaryformat

startdate

enddate

visible

hidden

viewurl

showshortname

hasprogress

progress

isfavourite

showactivitydates

showcompletionconditions



---

4) GraphQL – /graphql

Request Body (JSON Keys)

operationName

variables

query


Operations gesehen

getMyStudentGradeOverview

gtMyStudentData

RaftgetmyStudentData



---

4.1 getMyStudentGradeOverview – Response Felder

Top:

data

myStudentGradeOverview



myStudentGradeOverview keys:

modules (Array)

grade

eCTS

currentSemester

student (Objekt)

__typename


modules[] keys:

modulId

semester

modulbezeichnung

eCTS

eCTSString

grade

note

points

pruefungsId

pruefungsform

examStatus

color

__typename


student keys (PII-feldig, aber nur Keys):

id

benutzername

vorname

nachname

titel

akademischerGradTnid

geschlechtTnid

geburtsdatum

geburtsort

geburtslandTnid

staatsangehoerigkeitTnid

wohnort

wohnlandTnid

strasse

hausnummer

plz

telefonnummer

emailKh

emailPrivat

notizen

bemerkung

createdAt

__typename



---

4.2 gtMyStudentData – Response Felder

Top:

data

myStudentData



myStudentData keys:

studentId

anrede (nicht hier gesehen, nur bei Raft)

titel

vorname

nachname

akademischerGrad

akademischerGradTnid

geschlechtTnid

geburtsdatum

geburtsort

geburtsland

geburtslandTnid

staatsangehoerigkeit

staatsangehoerigkeitTnid

wohnort

wohnland

wohnlandTnid

strasse

hausnummer

plz

telefonnummer

emailKh

emailPrivat

benutzername

notizen

bemerkung

createdAt

__typename



---

4.3 RaftgetmyStudentData – Response Felder

Top:

data

result



result keys:

studentId

anrede

titel

vorname

nachname

akademischerGrad

geburtsdatum

geburtsort

geburtsland

staatsangehoerigkeit

wohnort

wohnland

strasse

hausnummer

plz

telefonnummer

emailPrivat

__typename



---

5) “Kleinkram”, den du fürs Extrahieren kennen solltest

Moodle UI Pref Endpoint – Query Keys

/lib/ajax/setuserpref.php

sesskey

pref

value


Kurs/Section Routing – Query Keys

/course/view.php

id

section


/mod/resource/view.php

id



---

Praktische Konsequenz für Copilot (kurz, knallhart)

Kurse bekommst du am stabilsten über Moodle AJAX:

core_course_get_enrolled_courses_by_timeline_classification → data.courses[]


Kalender/Deadlines über:

core_calendar_get_calendar_monthly_view → data.weeks[].days[].events[]


Studienstand/ECTS/Noten über GraphQL:

getMyStudentGradeOverview → modules[], grade, eCTS, currentSemester


PII-Felder sind in GraphQL enthalten (student.*) → im Export default redact.



---

Copilot Kontext-Block (English, precise)

Field inventory from captured traffic (values redacted):
- Moodle AJAX endpoint: https://portal.kolping-hochschule.de/lib/ajax/service.php?sesskey=...&info=...
  Request body: [{ index, methodname, args:{...} }]
  Methods:
  1) core_course_get_enrolled_courses_by_timeline_classification
     args: offset, limit, classification, sort, customfieldname, customfieldvalue
     resp: [{ error, data:{ nextoffset, courses:[{id,idnumber,shortname,fullname,fullnamedisplay,coursecategory,courseimage,summary,summaryformat,startdate,enddate,visible,hidden,viewurl,showshortname,hasprogress,progress,isfavourite,showactivitydates,showcompletionconditions}] }, exception? }]
  2) core_calendar_get_calendar_monthly_view
     args: year, month, day, courseid, categoryid, includenavigation, mini
     resp data: categoryid, courseid, date{year,month,mon,mday,wday,weekday,yday,hours,minutes,seconds,timestamp}, daynames[], nextperiod{...}, previousperiod{...}, periodname, weeks[{days[{... events:[{id,name,description,descriptionformat,eventtype,normalisedeventtype,normalisedeventtypetext,formattedtime,url,viewurl,icon,component,modulename,categoryid,groupid,groupname,userid,repeatid,eventcount,timestart,timeduration,timesort,timemodified,timeusermidnight,visible,draggable,candelete,canedit,deleteurl,editurl,popupname,subscription,course{...course fields...}}]}]}]
- GraphQL: https://app-kolping-prod-gateway.azurewebsites.net/graphql
  Request keys: operationName, variables, query
  Ops:
  getMyStudentGradeOverview -> data.myStudentGradeOverview{ modules[{modulId,semester,modulbezeichnung,eCTS,eCTSString,grade,note,points,pruefungsId,pruefungsform,examStatus,color,__typename}], grade, eCTS, currentSemester, student{...PII keys...}, __typename }
  gtMyStudentData -> data.myStudentData{...student profile keys...}
  RaftgetmyStudentData -> data.result{...student profile keys...}
- OIDC callbacks:
  portal /auth/oidc/ form fields: code, state, session_state
  cms /.auth/login/aad/callback form fields: code, id_token, state, session_state
- Token endpoint (v2.0/token) form keys: client_id, scope, grant_type, client_info, refresh_token, code, code_verifier, redirect_uri, client-request-id, x-client-*, x-ms-lib-capability, x-client-*-telemetry
  token response keys: token_type, scope, expires_in, ext_expires_in, access_token, refresh_token, id_token, client_info

