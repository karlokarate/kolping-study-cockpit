package de.kolping.cockpit.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.kolping.cockpit.android.database.entities.ModuleEntity

// Constants for styling
private const val STATUS_BADGE_ALPHA = 0.5f

/**
 * Card component for displaying a module
 * Shows module name, grade, ECTS, and status
 * Based on Issue #6 HomeScreen mockup
 */
@Composable
fun ModuleCard(
    module: ModuleEntity,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Module name and semester
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = module.modulbezeichnung,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                // Semester badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Sem. ${module.semester}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // Grade and ECTS info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Grade
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Note:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = module.grade ?: module.note ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            module.grade != null || module.note != null -> 
                                MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                // ECTS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${module.eCTS} ECTS",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Exam status if available
            module.examStatus?.let { status ->
                val statusColors = getExamStatusColors(status.lowercase())
                Surface(
                    color = statusColors.backgroundColor,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColors.textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Get appropriate colors for exam status
 */
@Composable
private fun getExamStatusColors(status: String): StatusColors {
    return when (status) {
        "bestanden", "passed" -> StatusColors(
            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = STATUS_BADGE_ALPHA),
            textColor = MaterialTheme.colorScheme.primary
        )
        "nicht bestanden", "failed" -> StatusColors(
            backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = STATUS_BADGE_ALPHA),
            textColor = MaterialTheme.colorScheme.error
        )
        else -> StatusColors(
            backgroundColor = MaterialTheme.colorScheme.surface,
            textColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

private data class StatusColors(
    val backgroundColor: Color,
    val textColor: Color
)
