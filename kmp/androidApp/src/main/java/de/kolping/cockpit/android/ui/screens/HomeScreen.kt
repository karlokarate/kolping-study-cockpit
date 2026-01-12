package de.kolping.cockpit.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.kolping.cockpit.android.ui.components.*
import de.kolping.cockpit.android.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * HomeScreen - Main screen after login
 * Displays current semester modules, upcoming events, and sync button
 * Based on Issue #6 PR 4 requirements
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToModuleDetail: (String) -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kolping Study Cockpit") },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Open navigation drawer */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menü")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is HomeViewModel.HomeUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is HomeViewModel.HomeUiState.Success -> {
                    HomeContent(
                        studentProfile = state.studentProfile,
                        modules = state.currentSemesterModules,
                        events = state.upcomingEvents,
                        lastSyncTimestamp = state.lastSyncTimestamp,
                        isSyncing = syncState is HomeViewModel.SyncState.Syncing,
                        onSyncClick = { viewModel.startSync() },
                        onModuleClick = onNavigateToModuleDetail,
                        onEventClick = { onNavigateToCalendar() },
                        formatLastSync = viewModel::formatLastSync
                    )
                }
                
                is HomeViewModel.HomeUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadOfflineData() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            
            // Sync dialogs
            when (val sync = syncState) {
                is HomeViewModel.SyncState.Syncing -> {
                    SyncProgressDialog(
                        phase = sync.phase,
                        progress = sync.progress,
                        filesDownloaded = sync.filesDownloaded,
                        totalFiles = sync.totalFiles
                    )
                }
                is HomeViewModel.SyncState.Success -> {
                    SyncSuccessDialog(
                        modulesCount = sync.modulesCount,
                        coursesCount = sync.coursesCount,
                        filesDownloaded = sync.filesDownloaded,
                        eventsCount = sync.eventsCount,
                        durationMs = sync.durationMs,
                        onDismiss = { viewModel.resetSyncState() }
                    )
                }
                is HomeViewModel.SyncState.Error -> {
                    SyncErrorDialog(
                        errorMessage = sync.message,
                        onDismiss = { viewModel.resetSyncState() },
                        onRetry = { 
                            viewModel.resetSyncState()
                            viewModel.startSync()
                        }
                    )
                }
                HomeViewModel.SyncState.Idle -> {
                    // No dialog
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    studentProfile: de.kolping.cockpit.android.database.entities.StudentProfileEntity?,
    modules: List<de.kolping.cockpit.android.database.entities.ModuleEntity>,
    events: List<de.kolping.cockpit.android.database.entities.CalendarEventEntity>,
    lastSyncTimestamp: Long,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onModuleClick: (String) -> Unit,
    onEventClick: () -> Unit,
    formatLastSync: (Long) -> String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting header
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = buildString {
                        append("👋 Hallo")
                        studentProfile?.let {
                            append(", ${it.vorname} ${it.nachname}")
                        }
                        append("!")
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                studentProfile?.currentSemester?.let { semester ->
                    Text(
                        text = "Semester $semester",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        item {
            HorizontalDivider()
        }
        
        // Sync button
        item {
            SyncButton(
                lastSyncText = formatLastSync(lastSyncTimestamp),
                onSyncClick = onSyncClick,
                enabled = !isSyncing
            )
        }
        
        // Current semester modules
        if (modules.isNotEmpty()) {
            item {
                Text(
                    text = "📊 Meine Module (aktuelles Semester)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(modules) { module ->
                ModuleCard(
                    module = module,
                    onClick = { onModuleClick(module.modulId) }
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keine Module vorhanden.\nBitte synchronisieren Sie Ihre Daten.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Upcoming events
        if (events.isNotEmpty()) {
            item {
                Text(
                    text = "📅 Nächste Termine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(events) { event ->
                EventCard(
                    event = event,
                    onClick = onEventClick
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Fehler beim Laden",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onRetry) {
            Text("ERNEUT VERSUCHEN")
        }
    }
}
