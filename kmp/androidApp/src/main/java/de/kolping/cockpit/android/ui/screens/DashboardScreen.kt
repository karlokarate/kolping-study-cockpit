package de.kolping.cockpit.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kolping.cockpit.android.viewmodel.DashboardViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToGrades: () -> Unit,
    onNavigateToCourses: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
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
                is DashboardViewModel.DashboardUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is DashboardViewModel.DashboardUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Grade Overview Card
                        state.gradeOverview?.let { gradeOverview ->
                            item {
                                Card(
                                    onClick = onNavigateToGrades,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Notendurchschnitt",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        gradeOverview.grade?.let { grade ->
                                            Text(
                                                text = grade,
                                                style = MaterialTheme.typography.headlineLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "ECTS",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    text = "${gradeOverview.eCTS}",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Semester",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    text = gradeOverview.currentSemester,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Upcoming Deadlines
                        if (state.upcomingDeadlines.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Anstehende Termine",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            
                            items(state.upcomingDeadlines.take(5)) { event ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = event.title,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        event.startTime?.let { time ->
                                            Text(
                                                text = time,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Courses Card
                        state.moodleDashboard?.let { dashboard ->
                            if (dashboard.courses.isNotEmpty()) {
                                item {
                                    Card(
                                        onClick = onNavigateToCourses,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Meine Kurse",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "${dashboard.courses.size} Kurse",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                is DashboardViewModel.DashboardUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Fehler beim Laden",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { viewModel.loadDashboard() }) {
                            Text("Erneut versuchen")
                        }
                    }
                }
            }
        }
    }
}
