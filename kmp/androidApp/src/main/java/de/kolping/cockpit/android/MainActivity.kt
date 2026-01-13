package de.kolping.cockpit.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import de.kolping.cockpit.android.auth.TokenManager
import de.kolping.cockpit.android.ui.screens.*
import de.kolping.cockpit.android.ui.theme.KolpingCockpitTheme
import org.koin.android.ext.android.inject

import de.kolping.cockpit.mapping.android.ui.RecorderScreen
class MainActivity : ComponentActivity() {
    
    private val tokenManager: TokenManager by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            KolpingCockpitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KolpingCockpitApp(tokenManager)
                }
            }
        }
    }
}

@Composable
fun KolpingCockpitApp(tokenManager: TokenManager) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Loading) }
    var previousScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var selectedModuleId by remember { mutableStateOf<String?>(null) }
    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    val isAuthenticated by tokenManager.isAuthenticatedFlow.collectAsState(initial = false)
    
    LaunchedEffect(isAuthenticated) {
        currentScreen = if (isAuthenticated) {
            Screen.Home
        } else {
            Screen.Login
        }
    }
    
    when (currentScreen) {
        Screen.Loading -> {
            // Show loading screen while checking auth
        }
        
        Screen.Login -> {
            LoginScreen(
                onLoginSuccess = {
                    currentScreen = Screen.Home
                }
            )
        }
        
        Screen.Home -> {
            HomeScreen(
                onNavigateToRecorder = { currentScreen = Screen.Recorder },
                onNavigateToModuleDetail = { moduleId ->
                    selectedModuleId = moduleId
                    previousScreen = Screen.Home
                    currentScreen = Screen.ModuleDetail
                },
                onNavigateToCalendar = {
                    previousScreen = Screen.Home
                    currentScreen = Screen.Calendar
                },
                onNavigateToOfflineLibrary = {
                    previousScreen = Screen.Home
                    currentScreen = Screen.OfflineLibrary
                }
            )
        }
        
        Screen.Dashboard -> {
            DashboardScreen(
                onNavigateToGrades = {
                    currentScreen = Screen.Grades
                },
                onNavigateToCourses = {
                    currentScreen = Screen.Courses
                }
            )
        }
        
        Screen.Calendar -> {
            CalendarScreen(
                onNavigateBack = {
                    currentScreen = previousScreen
                }
            )
        }
        
        Screen.ModuleDetail -> {
            selectedModuleId?.let { moduleId ->
                ModuleDetailScreen(
                    moduleId = moduleId,
                    onNavigateBack = {
                        currentScreen = previousScreen
                    },
                    onOpenFile = { file ->
                        if (file.fileType.lowercase() == "pdf") {
                            selectedFilePath = file.filePath
                            selectedFileName = file.fileName
                            previousScreen = Screen.ModuleDetail
                            currentScreen = Screen.PdfViewer
                        } else {
                            // TODO: Handle other file types (open with system app)
                        }
                    }
                )
            } ?: run {
                // Handle null moduleId - navigate back to Home
                currentScreen = Screen.Home
            }
        }
        
        Screen.OfflineLibrary -> {
            OfflineLibraryScreen(
                onNavigateBack = {
                    currentScreen = previousScreen
                },
                onOpenFile = { file ->
                    if (file.fileType.lowercase() == "pdf") {
                        selectedFilePath = file.filePath
                        selectedFileName = file.fileName
                        previousScreen = Screen.OfflineLibrary
                        currentScreen = Screen.PdfViewer
                    } else {
                        // TODO: Handle other file types
                    }
                }
            )
        }
        
        Screen.Recorder -> {
            RecorderScreen(onBack = { currentScreen = Screen.Home })
        }

        Screen.PdfViewer -> {
            selectedFilePath?.let { filePath ->
                PdfViewerScreen(
                    filePath = filePath,
                    fileName = selectedFileName ?: "PDF Dokument",
                    onNavigateBack = {
                        currentScreen = previousScreen
                    }
                )
            } ?: run {
                // Handle null filePath - navigate back to previous screen
                currentScreen = previousScreen
            }
        }
        
        Screen.Grades -> {
            GradesScreen(
                onNavigateBack = {
                    currentScreen = Screen.Dashboard
                }
            )
        }
        
        Screen.Courses -> {
            CoursesScreen(
                onNavigateBack = {
                    currentScreen = Screen.Dashboard
                }
            )
        }
    }
}

sealed class Screen {
    object Loading : Screen()
    object Login : Screen()
    object Dashboard : Screen()
    object Home : Screen()
    object Grades : Screen()
    object Courses : Screen()
    object Calendar : Screen()
    object ModuleDetail : Screen()
    object OfflineLibrary : Screen()
    object PdfViewer : Screen()
    object Recorder : Screen()
}
