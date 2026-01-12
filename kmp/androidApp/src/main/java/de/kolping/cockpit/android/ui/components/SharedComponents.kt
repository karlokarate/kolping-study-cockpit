package de.kolping.cockpit.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.kolping.cockpit.android.util.FileTypeCategory

/**
 * Get Material icon for file type category
 */
@Composable
fun getFileTypeIcon(category: FileTypeCategory): ImageVector {
    return when (category) {
        FileTypeCategory.PDF -> Icons.Default.PictureAsPdf
        FileTypeCategory.DOCUMENT -> Icons.Default.Description
        FileTypeCategory.SPREADSHEET -> Icons.Default.TableChart
        FileTypeCategory.PRESENTATION -> Icons.Default.Slideshow
        FileTypeCategory.IMAGE -> Icons.Default.Image
        FileTypeCategory.VIDEO -> Icons.Default.VideoFile
        FileTypeCategory.AUDIO -> Icons.Default.AudioFile
        FileTypeCategory.ARCHIVE -> Icons.Default.FolderZip
        FileTypeCategory.OTHER -> Icons.Default.InsertDriveFile
    }
}

/**
 * Shared error content component for screens
 */
@Composable
fun ErrorContent(
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
