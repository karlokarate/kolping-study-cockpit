package de.kolping.cockpit.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kolping.cockpit.android.viewmodel.GradesViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GradesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Noten") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadGrades() }) {
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
                is GradesViewModel.GradesUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is GradesViewModel.GradesUiState.Success -> {
                    val gradeOverview = state.gradeOverview
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Summary Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    gradeOverview.grade?.let { grade ->
                                        Text(
                                            text = "Durchschnittsnote",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = grade,
                                            style = MaterialTheme.typography.displayMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "ECTS gesamt",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "${gradeOverview.eCTS}",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Semester",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = gradeOverview.currentSemester,
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Modules Header
                        item {
                            Text(
                                text = "Module (${gradeOverview.modules.size})",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        
                        // Module Cards
                        items(gradeOverview.modules.sortedByDescending { it.semester }) { module ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = module.modulbezeichnung,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "Semester ${module.semester}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        
                                        module.grade?.let { grade ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Text(
                                                    text = grade,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${module.eCTS} ECTS",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        module.examStatus?.let { status ->
                                            Text(
                                                text = status,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                is GradesViewModel.GradesUiState.Error -> {
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
                        Button(onClick = { viewModel.loadGrades() }) {
                            Text("Erneut versuchen")
                        }
                    }
                }
            }
        }
    }
}
