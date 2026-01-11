# Quick Start Guide - Kotlin Multiplatform Android App

## 📊 Project Stats

- **24 Kotlin files** (`.kt` + `.kts`)
- **2,257 lines of code**
- **4 documentation files**
- **2 modules** (shared + androidApp)
- **20+ dependencies** managed via version catalog

## 🚀 Getting Started in 5 Minutes

### Prerequisites
```bash
# Required
✅ JDK 17 or higher
✅ Android Studio Hedgehog (2023.1.1) or later
✅ Android SDK API 26+ (Android 8.0+)

# Optional (for command line builds)
✅ Gradle 8.5+ (included via wrapper)
```

### Step 1: Clone and Open
```bash
git clone https://github.com/karlokarate/kolping-study-cockpit.git
cd kolping-study-cockpit/kmp
```

Open the `kmp` directory in Android Studio:
- File → Open → Select `kmp` folder
- Wait for Gradle sync to complete

### Step 2: Build
```bash
# Option A: Using Gradle wrapper
./gradlew build

# Option B: In Android Studio
# Build → Make Project (Ctrl+F9 / Cmd+F9)
```

### Step 3: Run
```bash
# Option A: Command line
./gradlew :androidApp:installDebug

# Option B: Android Studio
# Click green "Run" button (Shift+F10)
```

### Step 4: Test Authentication
1. App opens with WebView login screen
2. Navigate to `https://cms.kolping-hochschule.de`
3. Log in with your Kolping credentials
4. Complete Microsoft Entra MFA
5. App extracts tokens and navigates to Dashboard

## 📱 App Screens

### 1. Login Screen
- WebView for Microsoft Entra SSO
- Automatic token extraction
- Loading indicators

### 2. Dashboard Screen
```
┌─────────────────────────┐
│ Notendurchschnitt      │
│      1.8               │
│ ECTS: 120  Semester: 6 │
└─────────────────────────┘

┌─────────────────────────┐
│ Anstehende Termine     │
├─────────────────────────┤
│ Prüfung Mathematik II  │
│ Mo, 15.01, 10:00       │
└─────────────────────────┘

┌─────────────────────────┐
│ Meine Kurse (12)       │
└─────────────────────────┘
```

### 3. Grades Screen
- Complete module list
- Sorted by semester
- ECTS and exam status
- Color-coded grade badges

### 4. Courses Screen
- All enrolled Moodle courses
- Course metadata
- Progress indicators

## 🔧 Build Variants

### Debug Build (Default)
```bash
./gradlew :androidApp:assembleDebug
# Output: androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### Release Build (for production)
```bash
./gradlew :androidApp:assembleRelease
# Requires signing configuration
```

## 🧪 Development

### Project Structure
```
kmp/
├── shared/                    # Business logic (KMP)
│   ├── api/                   # GraphQL + Moodle clients
│   ├── models/                # Data models
│   └── repository/            # Repository layer
│
└── androidApp/                # Android UI
    ├── ui/screens/            # Compose screens
    ├── viewmodel/             # ViewModels
    ├── auth/                  # Auth components
    └── di/                    # Dependency injection
```

### Key Files to Customize

**Branding:**
- `androidApp/src/main/res/values/strings.xml` - App name
- `androidApp/src/main/res/mipmap-*/` - Launcher icons
- `androidApp/.../ui/theme/Theme.kt` - Colors

**Endpoints:**
- `shared/.../api/GraphQLClient.kt` - GraphQL URL
- `shared/.../api/MoodleClient.kt` - Moodle base URL

**Features:**
- `shared/.../repository/StudyRepository.kt` - Add new data sources
- `androidApp/.../ui/screens/` - Add new screens

### Adding Dependencies

Edit `gradle/libs.versions.toml`:
```toml
[versions]
my-library = "1.0.0"

[libraries]
my-library = { module = "com.example:library", version.ref = "my-library" }
```

Then use in `build.gradle.kts`:
```kotlin
implementation(libs.my.library)
```

## 🐛 Troubleshooting

### Build Fails: "SDK not found"
```bash
# Set ANDROID_HOME environment variable
export ANDROID_HOME=/path/to/Android/Sdk

# Or create local.properties
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
```

### Gradle Sync Issues
```bash
# Clean build
./gradlew clean

# Refresh dependencies
./gradlew --refresh-dependencies
```

### WebView Not Loading
- Check internet connection
- Ensure `INTERNET` permission in AndroidManifest.xml
- Check Android system WebView is updated

### Token Extraction Fails
- Open Chrome DevTools on desktop
- Check JavaScript localStorage/sessionStorage
- Adjust token extraction logic in `EntraAuthWebView.kt`

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| [README.md](./README.md) | Overview and features |
| [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) | Complete technical details |
| [PYTHON_TO_KOTLIN_MAPPING.md](./PYTHON_TO_KOTLIN_MAPPING.md) | Code comparison with Python version |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | System architecture and diagrams |

## 🔐 Security Notes

- Tokens stored in encrypted DataStore
- Never log or print credentials
- HTTPS for all network calls
- WebView sandboxed by Android
- Credentials cleared on logout

## 🎯 Next Steps

### For Development:
1. ✅ Run the app on emulator/device
2. ✅ Test authentication flow
3. ✅ Verify data loading from APIs
4. ⬜ Add unit tests for ViewModels
5. ⬜ Add UI tests with Compose Testing
6. ⬜ Create proper launcher icons
7. ⬜ Set up CI/CD pipeline

### For Production:
1. ⬜ Configure release signing
2. ⬜ Add ProGuard rules
3. ⬜ Test on multiple Android versions
4. ⬜ Optimize APK size
5. ⬜ Set up crash reporting
6. ⬜ Implement analytics
7. ⬜ Prepare Play Store listing

### For Platform Expansion:
1. ⬜ Add iOS target to shared module
2. ⬜ Create SwiftUI iOS app
3. ⬜ Add Desktop target (Compose Multiplatform)
4. ⬜ Implement offline mode with SQLDelight
5. ⬜ Add push notifications

## 💡 Tips

1. **Use Android Studio's Compose Preview** for UI development
2. **Test on real device** for WebView authentication (MFA might not work on emulator)
3. **Check Logcat** for detailed error messages
4. **Use Layout Inspector** to debug Compose UI
5. **Profile with Android Profiler** to optimize performance

## 🤝 Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) in the repository root.

## 📄 License

See [LICENSE](../LICENSE) in the repository root.

## 🆘 Getting Help

1. Check [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) for technical details
2. Review [ARCHITECTURE.md](./ARCHITECTURE.md) for system design
3. Compare with Python code using [PYTHON_TO_KOTLIN_MAPPING.md](./PYTHON_TO_KOTLIN_MAPPING.md)
4. Open an issue on GitHub

---

**Happy Coding! 🚀**
