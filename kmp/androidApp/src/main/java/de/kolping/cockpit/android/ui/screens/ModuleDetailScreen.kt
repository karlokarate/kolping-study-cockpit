package de.kolping.cockpit.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.kolping.cockpit.android.database.entities.FileEntity
import de.kolping.cockpit.android.database.entities.ModuleEntity
import de.kolping.cockpit.android.util.FileUtils
import de.kolping.cockpit.android.viewmodel.ModuleDetailViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * ModuleDetailScreen - Displays individual module with all files
 * Based on Issue #6 PR 5 requirements
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailScreen(
    moduleId: String,
    onNavigateBack: () -> Unit = {},
    onOpenFile: (FileEntity) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ModuleDetailViewModel = koinViewModel { parametersOf(moduleId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    when (val state = uiState) {
                        is ModuleDetailViewModel.ModuleDetailUiState.Success -> {
                            Text(state.module.modulbezeichnung)
                        }
                        else -> {
                            Text("Modul Details")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
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
                is ModuleDetailViewModel.ModuleDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is ModuleDetailViewModel.ModuleDetailUiState.Success -> {
                    ModuleDetailContent(
                        module = state.module,
                        files = state.files,
                        onOpenFile = onOpenFile,
                        formatFileSize = viewModel::formatFileSize,
                        getFileTypeCategory = viewModel::getFileTypeCategory
                    )
                }
                
                is ModuleDetailViewModel.ModuleDetailUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleDetailContent(
    module: ModuleEntity,
    files: List<FileEntity>,
    onOpenFile: (FileEntity) -> Unit,
    formatFileSize: (Long) -> String,
    getFileTypeCategory: (String) -> FileUtils.FileTypeCategory
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Module information card
        item {
            ModuleInfoCard(module = module)
        }
        
        item {
            HorizontalDivider()
        }
        
        // Files section header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📁 Dateien",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${files.size} Datei${if (files.size != 1) "en" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Files list
        if (files.isEmpty()) {
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
                            text = "Keine Dateien vorhanden",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(files) { file ->
                FileCard(
                    file = file,
                    onClick = { onOpenFile(file) },
                    formatFileSize = formatFileSize,
                    getFileTypeIcon = { fileType ->
                        getFileTypeIcon(getFileTypeCategory(fileType))
                    }
                )
            }
        }
    }
}

@Composable
private fun ModuleInfoCard(module: ModuleEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Module title
            Text(
                text = module.modulbezeichnung,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // Module info grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Semester
                InfoItem(
                    label = "Semester",
                    value = module.semester.toString(),
                    modifier = Modifier.weight(1f)
                )
                
                // ECTS
                InfoItem(
                    label = "ECTS",
                    value = module.eCTS.toString(),
                    modifier = Modifier.weight(1f)
                )
                
                // Grade
                InfoItem(
                    label = "Note",
                    value = module.grade ?: module.note ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Additional info
            if (module.pruefungsform != null || module.examStatus != null) {
                HorizontalDivider()
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    module.pruefungsform?.let { form ->
                        Text(
                            text = "Prüfungsform: $form",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    module.examStatus?.let { status ->
                        Text(
                            text = "Status: $status",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    module.points?.let { points ->
                        Text(
                            text = "Punkte: $points",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun FileCard(
    file: FileEntity,
    onClick: () -> Unit,
    formatFileSize: (Long) -> String,
    getFileTypeIcon: (String) -> ImageVector
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File type icon
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = getFileTypeIcon(file.fileType),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // File info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = file.fileType.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = formatFileSize(file.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Open indicator
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Öffnen",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getFileTypeIcon(category: FileUtils.FileTypeCategory): ImageVector {
    return when (category) {
        FileUtils.FileTypeCategory.PDF -> Icons.Default.PictureAsPdf
        FileUtils.FileTypeCategory.DOCUMENT -> Icons.Default.Description
        FileUtils.FileTypeCategory.SPREADSHEET -> Icons.Default.TableChart
        FileUtils.FileTypeCategory.PRESENTATION -> Icons.Default.Slideshow
        FileUtils.FileTypeCategory.IMAGE -> Icons.Default.Image
        FileUtils.FileTypeCategory.VIDEO -> Icons.Default.VideoFile
        FileUtils.FileTypeCategory.AUDIO -> Icons.Default.AudioFile
        FileUtils.FileTypeCategory.ARCHIVE -> Icons.Default.FolderZip
        FileUtils.FileTypeCategory.OTHER -> Icons.Default.InsertDriveFile
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
            text = "Fehler",
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
