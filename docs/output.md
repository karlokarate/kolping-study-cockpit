👋 Welcome to Codespaces! You are on a custom image defined in your devcontainer.json file.

🔍 To explore VS Code to its fullest, search using the Command Palette (Cmd/Ctrl + Shift + P)

📝 Edit away, then run your build command to see your code running in the browser.
@karlokarate ➜ /workspaces/kolping-study-cockpit (main) $ uv run kolping diagnose^C
source /workspaces/kolping-study-cockpit/.venv/bin/activate
@karlokarate ➜ /workspaces/kolping-study-cockpit (main) $ source /workspaces/kolping-study-cockpit/.venv/bin/activate
(kolping-cockpit) @karlokarate ➜ /workspaces/kolping-study-cockpit (main) $ uv run kolping diagnose
bash: uv: command not found
(kolping-cockpit) @karlokarate ➜ /workspaces/kolping-study-cockpit (main) $ curl -LsSf https://astral.sh/uv/install.sh | sh
source ~/.local/bin/env
uv sync
uv run playwright install chromium
uv run kolping diagnose
downloading uv 0.9.24 x86_64-unknown-linux-gnu
no checksums to verify
installing to /home/vscode/.local/bin
  uv
  uvx
everything's installed!
bash: /home/vscode/.local/bin/env: No such file or directory
Resolved 52 packages in 0.99ms
Audited 34 packages in 15ms
Kolping Study Cockpit - Diagnostics
==================================================
Checking secrets...
Checking keyring...
Checking Moodle portal...
Checking GraphQL API...

                                         Diagnostic Results                                          
┏━━━━━━━━━━━━━━━┳━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ Check         ┃ Status ┃ Details                                                                  ┃
┡━━━━━━━━━━━━━━━╇━━━━━━━━╇━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┩
│ Secrets       │ ✓ OK   │ All secrets configured                                                   │
│ Keyring       │ ✓ OK   │ Backend: Keyring                                                         │
│ Moodle Portal │ ✓ OK   │ Redirect chain OK → Microsoft Entra (2 hops)                             │
│ GraphQL API   │ ✓ OK   │ Endpoint reachable                                                       │
│               │        │ (https://app-kolping-prod-gateway.azurewebsites.net/graphql)             │
└───────────────┴────────┴──────────────────────────────────────────────────────────────────────────┘

✓ All checks passed
(kolping-cockpit) @karlokarate ➜ /workspaces/kolping-study-cockpit (main) $ git add -A && git commit -m "feat: Add diagnose, login, logout commands" && git push
On branch main
Your branch is ahead of 'origin/main' by 1 commit.
  (use "git push" to publish your local commits)

nothing to commit, working tree clean
(kolping-cockpit) @karlokarate ➜ /workspaces/kolping-study-cockpit (main) $ uv run kolping login
Kolping Study Cockpit - Interactive Login
==================================================
Opening browser for authentication...
Complete the login in the browser window (MFA may be required)
✗ Login error: BrowserType.launch: Target page, context or browser has been closed
Browser logs:

╔════════════════════════════════════════════════════════════════════════════════════════
════════╗
║ Looks like you launched a headed browser without having a XServer running.             
║
║ Set either 'headless: true' or use 'xvfb-run <your-playwright-app>' before running 
Playwright. ║
║                                                                                        
║
║ <3 Playwright Team                                                                     
║
╚════════════════════════════════════════════════════════════════════════════════════════
════════╝
Call log:
  - <launching> /home/vscode/.cache/ms-playwright/chromium-1200/chrome-linux64/chrome 
--disable-field-trial-config --disable-background-networking 
--disable-background-timer-throttling --disable-backgrounding-occluded-windows 
--disable-back-forward-cache --disable-breakpad --disable-client-side-phishing-detection 
--disable-component-extensions-with-background-pages --disable-component-update 
--no-default-browser-check --disable-default-apps --disable-dev-shm-usage 
--disable-extensions 
--disable-features=AcceptCHFrame,AvoidUnnecessaryBeforeUnloadCheckSync,DestroyProfileOnBr
owserClose,DialMediaRouteProvider,GlobalMediaControls,HttpsUpgrades,LensOverlay,MediaRout
er,PaintHolding,ThirdPartyStoragePartitioning,Translate,AutoDeElevate,RenderDocument,Opti
mizationHints --enable-features=CDPScreenshotNewSurface --allow-pre-commit-input 
--disable-hang-monitor --disable-ipc-flooding-protection --disable-popup-blocking 
--disable-prompt-on-repost --disable-renderer-backgrounding --force-color-profile=srgb 
--metrics-recording-only --no-first-run --password-store=basic --use-mock-keychain 
--no-service-autorun --export-tagged-pdf --disable-search-engine-choice-screen 
--unsafely-disable-devtools-self-xss-warnings --edge-skip-compat-layer-relaunch 
--enable-automation --disable-infobars --disable-search-engine-choice-screen 
--disable-sync --no-sandbox --user-data-dir=/tmp/playwright_chromiumdev_profile-nhs4Wh 
--remote-debugging-pipe --no-startup-window
  - <launched> pid=6268
  -  [6268:6306:0111/140648.229994:ERROR:dbus/bus.cc:406] Failed to connect to the bus: 
Failed to connect to socket /run/dbus/system_bus_socket: No such file or directory
  -  [6268:6268:0111/140648.246702:ERROR:ui/ozone/platform/x11/ozone_platform_x11.cc:259]
Missing X server or $DISPLAY
  -  [6268:6268:0111/140648.246730:ERROR:ui/aura/env.cc:257] The platform failed to 
initialize.  Exiting.
  -  <gracefully close start>
  -  <kill>
  -  <will force kill>
  -  <process did exit: exitCode=1, signal=null>
  -  starting temporary directories cleanup
  -  finished temporary directories cleanup
  -  <gracefully close end>

(kolping-cockpit) @karlokarate ➜ /workspaces/kolping-study-cockpit (main) $ uv run kolping login-manual
Kolping Study Cockpit - Manual Login
==================================================

Instructions:
1. Open in your local browser:
   https://portal.kolping-hochschule.de/my/

2. Login with your Microsoft account (complete MFA if needed)

3. After successful login, open Developer Tools (F12)
   → Application tab → Cookies → portal.kolping-hochschule.de
   → Copy the value of 'MoodleSession'

Paste MoodleSession cookie v53iti3bap8pai-g7gjj1ljj8ve3
✓ Session stored successfully!
(kolping-cockpit) @karlokarate ➜ /workspaces/kolping-study-cockpit (main) $ kolping export moodle     # Kurse, Prüfungen, Deadlines
Usage: kolping export [OPTIONS]
Try 'kolping export --help' for help.
╭─ Error ───────────────────────────────────────────────────────────────────────────────╮
│ Got unexpected extra argument (moodle)                                                │
╰───────────────────────────────────────────────────────────────────────────────────────╯
(kolping-cockpit) @karlokarate ➜ /workspaces/kolping-study-cockpit (main) $ # Führe aus und teile den Output:
curl -s -b "MoodleSession=$(python -c "from kolping_cockpit.settings import get_secret_from_env_or_keyring; print(get_secret_from_env_or_keyring('moodle_session'))")" \
  "https://portal.kolping-hochschule.de/my/" | head -200
<!DOCTYPE html>
<html  lang="de" xml:lang="de">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
                <title>Weiterleiten</title>
        <style>
body {
    margin: 0;
    font-family: -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"Helvetica Neue",Arial,"Noto Sans",sans-serif,"Apple Color Emoji","Segoe UI Emoji","Segoe UI Symbol","Noto Color Emoji";
    font-size: .9375rem;
    font-weight: 400;
    line-height: 1.5;
    color: #343a40;
    text-align: left;
    background-color: #f2f2f2;
}
#page {
    margin-top: 15px;
    background: white;
    max-width: 600px;
    margin: 0 auto;
    padding: 15px;
}
#region-main {
    margin: 0 auto;
    border: 1px solid rgba(0,0,0,.125);
    padding: 1rem 1.25rem 1.25rem;
    background-color: #fff;
}
h1 {
    font-size: 2.34rem;
    margin: 0 0 .5rem;
    font-weight: 300;
    line-height: 1.2;
}
.alert-danger {
    color: #6e211e;
    background-color: #f6d9d8;
    border-color: #f3c9c8;
    padding: .75rem 1.25rem;
}
    </style>
    </head>
    <body>
        <div id="page">
            <div id="region-main">
                <h1>Weiterleiten</h1>
                <div style="margin-top: 3em; margin-left:auto; margin-right:auto; text-align:center;">Die Weiterleitung sollte automatisch funktionieren - falls nichts passiert, klicken Sie bitte auf den nachfolgenden Link.<br /><a href="https://login.microsoftonline.com/kolpinghochschule.onmicrosoft.com/oauth2/authorize?response_type=code&amp;client_id=e27e57f4-abba-4475-b507-389e1e48e282&amp;scope=openid%20profile%20email&amp;nonce=N6963b2545c6fb&amp;response_mode=form_post&amp;state=SaDoZpj3OGah6qD&amp;redirect_uri=https%3A%2F%2Fportal.kolping-hochschule.de%2Fauth%2Foidc%2F&amp;resource=https%3A%2F%2Fgraph.microsoft.com">Weiter</a></div>                            </div>
        </div>
    </body>
</html>

(kolping-cockpit) @karlokarate ➜ /workspaces/kolping-study-cockpit (main) $ curl -X POST "https://app-kolping-prod-gateway.azurewebsites.net/graphql" \
  -H "Content-Type: application/json" \
  -d '{"query": "{ __schema { queryType { fields { name description } } } }"}'
{"data":{"__schema":{"queryType":{"fields":[{"name":"query_statistics","description":null},{"name":"anmeldungs","description":null},{"name":"anmeldung","description":null},{"name":"bewerbers","description":null},{"name":"bewerber","description":null},{"name":"geburtslands","description":null},{"name":"geburtsland","description":null},{"name":"geschlechts","description":null},{"name":"geschlecht","description":null},{"name":"interessents","description":null},{"name":"interessent","description":null},{"name":"semesters","description":null},{"name":"semester","description":null},{"name":"staatsangehoerigkeits","description":null},{"name":"staatsangehoerigkeit","description":null},{"name":"studiengangOnBews","description":null},{"name":"studiengangOnBew","description":null},{"name":"studiengangSemesters","description":null},{"name":"studiengangSemester","description":null},{"name":"studienmodells","description":null},{"name":"studienmodell","description":null},{"name":"uploadedFiles","description":null},{"name":"uploadedFile","description":null},{"name":"uploadFileTypes","description":null},{"name":"uploadFileType","description":null},{"name":"veranstaltungs","description":null},{"name":"veranstaltung","description":null},{"name":"veranstaltungsTyps","description":null},{"name":"veranstaltungsTyp","description":null},{"name":"wohnlands","description":null},{"name":"wohnland","description":null},{"name":"zahlungsmodells","description":null},{"name":"zahlungsmodell","description":null},{"name":"cmsSchema_query_statistics","description":null},{"name":"akademischerGradMas","description":null},{"name":"akademischerGradMa","description":null},{"name":"akademischerGradSgs","description":null},{"name":"akademischerGradSg","description":null},{"name":"akademischerGradTns","description":null},{"name":"akademischerGradTn","description":null},{"name":"ausbildungsArts","description":null},{"name":"ausbildungsArt","description":null},{"name":"bankverbindungMas","description":null},{"name":"bankverbindungMa","description":null},{"name":"beVerhaeltnis","description":null},{"name":"beVerhaeltni","description":null},{"name":"geburtslandMas","description":null},{"name":"geburtslandMa","description":null},{"name":"geburtslandTns","description":null},{"name":"geburtslandTn","description":null},{"name":"geschlechtMas","description":null},{"name":"geschlechtMa","description":null},{"name":"geschlechtTns","description":null},{"name":"geschlechtTn","description":null},{"name":"haeufigkeitDesAngebots","description":null},{"name":"haeufigkeitDesAngebot","description":null},{"name":"lerneinheits","description":null},{"name":"lerneinheit","description":null},{"name":"matchMitarbeiterTypMitarbeiters","description":null},{"name":"matchMitarbeiterTypMitarbeiter","description":null},{"name":"matchModulPruefungTyps","description":null},{"name":"matchModulStudents","description":null},{"name":"matchModulStudent","description":null},{"name":"matchMoodleModulStudents","description":null},{"name":"matchMoodleModulStudent","description":null},{"name":"matchPruefungModuls","description":null},{"name":"matchPruefungModul","description":null},{"name":"matchStudentSemesters","description":null},{"name":"matchStudentSemester","description":null},{"name":"matchStudiengangModuls","description":null},{"name":"matchStudiengangModul","description":null},{"name":"matchStudiengangStudents","description":null},{"name":"matchStudiengangStudent","description":null},{"name":"matchStudiumStatusStudiumArts","description":null},{"name":"matchStudiumStatusStudiumArt","description":null},{"name":"mitarbeiterTyps","description":null},{"name":"mitarbeiterTyp","description":null},{"name":"moduls","description":null},{"name":"modul","description":null},{"name":"modulStudentStatuses","description":null},{"name":"modulStudentStatus","description":null},{"name":"modultermins","description":null},{"name":"modultermin","description":null},{"name":"modulterminTyps","description":null},{"name":"modulterminTyp","description":null},{"name":"moodleModuls","description":null},{"name":"moodleModul","description":null},{"name":"pruefungs","description":null},{"name":"pruefung","description":null},{"name":"pruefungsergebnis","description":null},{"name":"pruefungsergebni","description":null},{"name":"pruefungsforms","description":null},{"name":"pruefungsform","description":null},{"name":"pruefungsorts","description":null},{"name":"pruefungsort","description":null},{"name":"pruefungStatuses","description":null},{"name":"pruefungStatus","description":null},{"name":"pruefungTyps","description":null},{"name":"pruefungTyp","description":null},{"name":"rahmenpruefungsordnungs","description":null},{"name":"rahmenpruefungsordnung","description":null},{"name":"staatsangehoerigkeitMas","description":null},{"name":"staatsangehoerigkeitMa","description":null},{"name":"staatsangehoerigkeitTns","description":null},{"name":"staatsangehoerigkeitTn","description":null},{"name":"stammdatenMas","description":null},{"name":"stammdatenMa","description":null},{"name":"students","description":null},{"name":"student","description":null},{"name":"studentSemesterStatuses","description":null},{"name":"studentSemesterStatus","description":null},{"name":"studiengangs","description":null},{"name":"studiengang","description":null},{"name":"studiengangTyps","description":null},{"name":"studiengangTyp","description":null},{"name":"studiumArts","description":null},{"name":"studiumArt","description":null},{"name":"studiumStatuses","description":null},{"name":"studiumStatus","description":null},{"name":"verantwortlichMitarbeiterStudiengangs","description":null},{"name":"verantwortlichMitarbeiterStudiengang","description":null},{"name":"verlaufMas","description":null},{"name":"verlaufMa","description":null},{"name":"verlaufTns","description":null},{"name":"verlaufTn","description":null},{"name":"verlaufTypMas","description":null},{"name":"verlaufTypMa","description":null},{"name":"verlaufTypTns","description":null},{"name":"verlaufTypTn","description":null},{"name":"wohnlandMas","description":null},{"name":"wohnlandMa","description":null},{"name":"wohnlandTns","description":null},{"name":"wohnlandTn","description":null},{"name":"zahlungsmodellTns","description":null},{"name":"zahlungsmodellTn","description":null},{"name":"zeitraums","description":null},{"name":"zeitraum","description":null},{"name":"zeitraumTyps","description":null},{"name":"zeitraumTyp","description":null},{"name":"checkIfMoodleIdExistsInOtherModules","description":null},{"name":"certificateOfStudy","description":null},{"name":"myCertificateOfStudy","description":null},{"name":"allCertificatesOfStudy","description":null},{"name":"studentIdCard","description":null},{"name":"myStudentIdCard","description":null},{"name":"allStudentIdCards","description":null},{"name":"modulSelection","description":null},{"name":"myStudentData","description":null},{"name":"canDeleteSemesterFromStudent","description":null},{"name":"checkIfMoodleModuleExists","description":null},{"name":"getCourseStudent","description":null},{"name":"getCalendarEvent","description":null},{"name":"studentGradeOverview","description":null},{"name":"myStudentGradeOverview","description":null},{"name":"transcriptOfRecords","description":null},{"name":"myTranscriptOfRecords","description":null},{"name":"allTranscriptsOfRecord","description":null},{"name":"matchModulPruefungTyp","description":null},{"name":"userSchema_query_statistics","description":null},{"name":"roles","description":null},{"name":"role","description":null},{"name":"roleClaims","description":null},{"name":"roleClaim","description":null},{"name":"users","description":null},{"name":"user","description":null},{"name":"userClaims","description":null},{"name":"userClaim","description":null},{"name":"userLogins","description":null},{"name":"userLogin","description":null},{"name":"userRoles","description":null},{"name":"userRole","description":null},{"name":"generateToken","description":null},{"name":"generateCiandoJWTToken","description":null},{"name":"validateCiandoJWTToken","description":null}]}}}}(kolping-cockpit) @karlokarate ➜ /workspaces/kolping-study-cockpit (main) $ curl -s -b "MoodleSession=$(python -c "from kolping_cockpit.settings import get_secret_from_env_or_keyring; print(get_secret_from_env_or_keyring('moodle_session'))")"   "https://portal.kolping-hochschule.de/my/" | head -200
<!DOCTYPE html>
<html  lang="de" xml:lang="de">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
                <title>Weiterleiten</title>
        <style>
body {
    margin: 0;
    font-family: -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"Helvetica Neue",Arial,"Noto Sans",sans-serif,"Apple Color Emoji","Segoe UI Emoji","Segoe UI Symbol","Noto Color Emoji";
    font-size: .9375rem;
    font-weight: 400;
    line-height: 1.5;
    color: #343a40;
    text-align: left;
    background-color: #f2f2f2;
}
#page {
    margin-top: 15px;
    background: white;
    max-width: 600px;
    margin: 0 auto;
    padding: 15px;
}
#region-main {
    margin: 0 auto;
    border: 1px solid rgba(0,0,0,.125);
    padding: 1rem 1.25rem 1.25rem;
    background-color: #fff;
}
h1 {
    font-size: 2.34rem;
    margin: 0 0 .5rem;
    font-weight: 300;
    line-height: 1.2;
}
.alert-danger {
    color: #6e211e;
    background-color: #f6d9d8;
    border-color: #f3c9c8;
    padding: .75rem 1.25rem;
}
    </style>
    </head>
    <body>
        <div id="page">
            <div id="region-main">
                <h1>Weiterleiten</h1>
                <div style="margin-top: 3em; margin-left:auto; margin-right:auto; text-align:center;">Die Weiterleitung sollte automatisch funktionieren - falls nichts passiert, klicken Sie bitte auf den nachfolgenden Link.<br /><a href="https://login.microsoftonline.com/kolpinghochschule.onmicrosoft.com/oauth2/authorize?response_type=code&amp;client_id=e27e57f4-abba-4475-b507-389e1e48e282&amp;scope=openid%20profile%20email&amp;nonce=N6963b26aa387a&amp;response_mode=form_post&amp;state=LH7ycTsqCbIdi8E&amp;redirect_uri=https%3A%2F%2Fportal.kolping-hochschule.de%2Fauth%2Foidc%2F&amp;resource=https%3A%2F%2Fgraph.microsoft.com">Weiter</a></div>                            </div>
        </div>
    </body>
</html>

(kolping-cockpit) @karlokarate ➜ /workspaces/kolping-study-cockpit (main) $ 