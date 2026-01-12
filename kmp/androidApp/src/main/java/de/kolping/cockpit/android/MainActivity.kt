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
                onNavigateToModuleDetail = { moduleId ->
                    selectedModuleId = moduleId
                    currentScreen = Screen.ModuleDetail
                },
                onNavigateToCalendar = {
                    currentScreen = Screen.Calendar
                },
                onNavigateToOfflineLibrary = {
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
                    currentScreen = Screen.Home
                }
            )
        }
        
        Screen.ModuleDetail -> {
            selectedModuleId?.let { moduleId ->
                ModuleDetailScreen(
                    moduleId = moduleId,
                    onNavigateBack = {
                        currentScreen = Screen.Home
                    },
                    onOpenFile = { file ->
                        if (file.fileType.lowercase() == "pdf") {
                            selectedFilePath = file.filePath
                            selectedFileName = file.fileName
                            currentScreen = Screen.PdfViewer
                        } else {
                            // TODO: Handle other file types (open with system app)
                        }
                    }
                )
            }
        }
        
        Screen.OfflineLibrary -> {
            OfflineLibraryScreen(
                onNavigateBack = {
                    currentScreen = Screen.Home
                },
                onOpenFile = { file ->
                    if (file.fileType.lowercase() == "pdf") {
                        selectedFilePath = file.filePath
                        selectedFileName = file.fileName
                        currentScreen = Screen.PdfViewer
                    } else {
                        // TODO: Handle other file types
                    }
                }
            )
        }
        
        Screen.PdfViewer -> {
            selectedFilePath?.let { filePath ->
                PdfViewerScreen(
                    filePath = filePath,
                    fileName = selectedFileName ?: "PDF Dokument",
                    onNavigateBack = {
                        currentScreen = Screen.Home
                    }
                )
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
}
