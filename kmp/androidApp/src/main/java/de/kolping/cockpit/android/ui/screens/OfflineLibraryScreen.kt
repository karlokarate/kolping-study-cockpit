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
import de.kolping.cockpit.android.viewmodel.ModuleDetailViewModel
import de.kolping.cockpit.android.viewmodel.OfflineLibraryViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * OfflineLibraryScreen - Browse all downloaded files with search
 * Based on Issue #6 PR 5 requirements
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineLibraryScreen(
    onNavigateBack: () -> Unit = {},
    onOpenFile: (FileEntity) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: OfflineLibraryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    
    var showSortDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Offline-Bibliothek") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showStorageDialog = true }) {
                            Icon(Icons.Default.Storage, contentDescription = "Speicherplatz")
                        }
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sortieren")
                        }
                    }
                )
                
                // Search bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.search(it) },
                    onClearSearch = { viewModel.clearSearch() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is OfflineLibraryViewModel.OfflineLibraryUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is OfflineLibraryViewModel.OfflineLibraryUiState.Success -> {
                    OfflineLibraryContent(
                        files = state.allFiles,
                        storageInfo = state.storageInfo,
                        onOpenFile = onOpenFile,
                        formatFileSize = viewModel::formatFileSize,
                        formatDownloadDate = viewModel::formatDownloadDate,
                        getFileTypeCategory = viewModel::getFileTypeCategory
                    )
                }
                
                is OfflineLibraryViewModel.OfflineLibraryUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
    
    // Sort dialog
    if (showSortDialog) {
        SortDialog(
            currentSortOrder = sortOrder,
            onSortOrderSelected = { 
                viewModel.setSortOrder(it)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }
    
    // Storage dialog
    if (showStorageDialog) {
        val state = uiState
        if (state is OfflineLibraryViewModel.OfflineLibraryUiState.Success) {
            StorageInfoDialog(
                storageInfo = state.storageInfo,
                formatFileSize = viewModel::formatFileSize,
                onDismiss = { showStorageDialog = false }
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Dateien durchsuchen...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearSearch) {
                    Icon(Icons.Default.Clear, contentDescription = "Suche löschen")
                }
            }
        },
        singleLine = true
    )
}

@Composable
private fun OfflineLibraryContent(
    files: List<FileEntity>,
    storageInfo: OfflineLibraryViewModel.StorageInfo,
    onOpenFile: (FileEntity) -> Unit,
    formatFileSize: (Long) -> String,
    formatDownloadDate: (Long) -> String,
    getFileTypeCategory: (String) -> ModuleDetailViewModel.FileTypeCategory
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary card
        item {
            StorageSummaryCard(
                storageInfo = storageInfo,
                formatFileSize = formatFileSize
            )
        }
        
        item {
            HorizontalDivider()
        }
        
        // Files header
        item {
            Text(
                text = "📁 ${files.size} Datei${if (files.size != 1) "en" else ""}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
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
                            text = "Keine Dateien gefunden",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(files) { file ->
                OfflineFileCard(
                    file = file,
                    onClick = { onOpenFile(file) },
                    formatFileSize = formatFileSize,
                    formatDownloadDate = formatDownloadDate,
                    getFileTypeIcon = { fileType ->
                        getFileTypeIcon(getFileTypeCategory(fileType))
                    }
                )
            }
        }
    }
}

@Composable
private fun StorageSummaryCard(
    storageInfo: OfflineLibraryViewModel.StorageInfo,
    formatFileSize: (Long) -> String
) {
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
            Text(
                text = "💾 Speicherplatz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Gesamtgröße",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatFileSize(storageInfo.totalSizeBytes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Anzahl Dateien",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = storageInfo.fileCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineFileCard(
    file: FileEntity,
    onClick: () -> Unit,
    formatFileSize: (Long) -> String,
    formatDownloadDate: (Long) -> String,
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
                    
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = formatDownloadDate(file.downloadedAt),
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

@Composable
private fun getFileTypeIcon(category: ModuleDetailViewModel.FileTypeCategory): ImageVector {
    return when (category) {
        ModuleDetailViewModel.FileTypeCategory.PDF -> Icons.Default.PictureAsPdf
        ModuleDetailViewModel.FileTypeCategory.DOCUMENT -> Icons.Default.Description
        ModuleDetailViewModel.FileTypeCategory.SPREADSHEET -> Icons.Default.TableChart
        ModuleDetailViewModel.FileTypeCategory.PRESENTATION -> Icons.Default.Slideshow
        ModuleDetailViewModel.FileTypeCategory.IMAGE -> Icons.Default.Image
        ModuleDetailViewModel.FileTypeCategory.VIDEO -> Icons.Default.VideoFile
        ModuleDetailViewModel.FileTypeCategory.AUDIO -> Icons.Default.AudioFile
        ModuleDetailViewModel.FileTypeCategory.ARCHIVE -> Icons.Default.FolderZip
        ModuleDetailViewModel.FileTypeCategory.OTHER -> Icons.Default.InsertDriveFile
    }
}

@Composable
private fun SortDialog(
    currentSortOrder: OfflineLibraryViewModel.SortOrder,
    onSortOrderSelected: (OfflineLibraryViewModel.SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sortieren nach") },
        text = {
            LazyColumn {
                items(OfflineLibraryViewModel.SortOrder.values()) { sortOrder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortOrderSelected(sortOrder) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortOrder == sortOrder,
                            onClick = { onSortOrderSelected(sortOrder) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = sortOrder.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("SCHLIESSEN")
            }
        }
    )
}

@Composable
private fun StorageInfoDialog(
    storageInfo: OfflineLibraryViewModel.StorageInfo,
    formatFileSize: (Long) -> String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Speicherplatz Details") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow(
                            label = "Gesamter Speicher:",
                            value = formatFileSize(storageInfo.totalSizeBytes)
                        )
                        InfoRow(
                            label = "Dateien gesamt:",
                            value = formatFileSize(storageInfo.totalFileSize)
                        )
                        InfoRow(
                            label = "Anzahl Dateien:",
                            value = storageInfo.fileCount.toString()
                        )
                    }
                }
                
                item {
                    HorizontalDivider()
                    Text(
                        text = "Nach Dateityp",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(storageInfo.fileTypeBreakdown) { (fileType, size) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = fileType.uppercase(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatFileSize(size),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("SCHLIESSEN")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
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
