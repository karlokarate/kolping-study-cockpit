package de.kolping.cockpit.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.kolping.cockpit.android.database.entities.CalendarEventEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card component for displaying a calendar event
 * Shows event name, date/time, and course name
 * Based on Issue #6 HomeScreen mockup
 */
@Composable
fun EventCard(
    event: CalendarEventEntity,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Calendar icon
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            // Event details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Event name
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                // Date and time
                Text(
                    text = formatEventDateTime(event.timestart, event.timeduration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Course name if available
                event.courseName?.let { courseName ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = courseName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Format event date and time
 */
private fun formatEventDateTime(timestart: Long, timeduration: Long?): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMAN)
    val startTime = Date(timestart * 1000) // Convert from seconds to milliseconds
    
    val formatted = sdf.format(startTime)
    
    // Add duration if available
    return if (timeduration != null && timeduration > 0) {
        val durationMinutes = timeduration / 60
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        
        when {
            hours > 0 && minutes > 0 -> "$formatted ($hours h $minutes min)"
            hours > 0 -> "$formatted ($hours h)"
            minutes > 0 -> "$formatted ($minutes min)"
            else -> formatted
        }
    } else {
        formatted
    }
}
