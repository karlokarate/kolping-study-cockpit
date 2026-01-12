package de.kolping.cockpit.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Dialog component showing sync progress
 * Displays current phase, progress bar, and file counter
 * Based on Issue #6 requirements
 */
@Composable
fun SyncProgressDialog(
    phase: String,
    progress: Float,
    filesDownloaded: Int,
    totalFiles: Int,
    onDismiss: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = { /* Prevent dismissal during sync to avoid interrupting the process */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Synchronisierung läuft...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Current phase
                Text(
                    text = phase,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Progress bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    
                    // Progress percentage
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
                
                // File counter if files are being downloaded
                if (totalFiles > 0) {
                    HorizontalDivider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dateien:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$filesDownloaded / $totalFiles",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Note about not closing
                Text(
                    text = "Bitte warten Sie, bis die Synchronisierung abgeschlossen ist.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Dialog showing sync success result
 */
@Composable
fun SyncSuccessDialog(
    modulesCount: Int,
    coursesCount: Int,
    filesDownloaded: Int,
    eventsCount: Int,
    durationMs: Long,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "✓ Synchronisierung erfolgreich",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Die Daten wurden erfolgreich synchronisiert:")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SyncResultRow("Module:", "$modulesCount")
                SyncResultRow("Kurse:", "$coursesCount")
                SyncResultRow("Events:", "$eventsCount")
                if (filesDownloaded > 0) {
                    SyncResultRow("Dateien:", "$filesDownloaded")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val durationSeconds = durationMs / 1000
                Text(
                    text = "Dauer: ${durationSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun SyncResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Dialog showing sync error
 */
@Composable
fun SyncErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Synchronisierung fehlgeschlagen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(errorMessage)
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text("ERNEUT VERSUCHEN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ABBRECHEN")
            }
        }
    )
}
