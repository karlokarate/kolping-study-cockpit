package de.kolping.cockpit.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.kolping.cockpit.android.database.entities.CalendarEventEntity
import de.kolping.cockpit.android.viewmodel.CalendarViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * CalendarScreen - Displays calendar with month view and event filtering
 * Based on Issue #6 PR 5 requirements
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val filterCourseId by viewModel.filterCourseId.collectAsState()
    val filterEventType by viewModel.filterEventType.collectAsState()
    
    var showFilterDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kalender") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    // Filter button with indicator
                    BadgedBox(
                        badge = {
                            if (filterCourseId != null || filterEventType != null) {
                                Badge()
                            }
                        }
                    ) {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
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
                is CalendarViewModel.CalendarUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is CalendarViewModel.CalendarUiState.Success -> {
                    CalendarContent(
                        allEvents = state.allEvents,
                        selectedDayEvents = state.selectedDayEvents,
                        selectedDate = selectedDate,
                        onDateSelected = { viewModel.selectDate(it) },
                        formatDate = viewModel::formatDate,
                        formatTime = viewModel::formatTime
                    )
                }
                
                is CalendarViewModel.CalendarUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
    
    // Filter dialog
    if (showFilterDialog) {
        FilterDialog(
            availableCourses = viewModel.getAvailableCourses(),
            availableEventTypes = viewModel.getAvailableEventTypes(),
            selectedCourseId = filterCourseId,
            selectedEventType = filterEventType,
            onCourseSelected = { viewModel.filterByCourse(it) },
            onEventTypeSelected = { viewModel.filterByEventType(it) },
            onClearFilters = { viewModel.clearFilters() },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
private fun CalendarContent(
    allEvents: List<CalendarEventEntity>,
    selectedDayEvents: List<CalendarEventEntity>,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    formatDate: (Long) -> String,
    formatTime: (Long) -> String
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Month view
        CalendarMonthView(
            selectedDate = selectedDate,
            events = allEvents,
            onDateSelected = onDateSelected,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        HorizontalDivider()
        
        // Selected day events
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "📅 ${SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMAN).format(selectedDate)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (selectedDayEvents.isEmpty()) {
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
                                text = "Keine Termine an diesem Tag",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(selectedDayEvents) { event ->
                    CalendarEventCard(
                        event = event,
                        formatTime = formatTime
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthView(
    selectedDate: Date,
    events: List<CalendarEventEntity>,
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    val calendar = remember(selectedDate) {
        Calendar.getInstance().apply { time = selectedDate }
    }
    
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    
    // Get days in month
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
    
    // Create event lookup map
    val eventsByDay = remember(events, month, year) {
        events.groupBy { event ->
            val eventCal = Calendar.getInstance().apply {
                timeInMillis = event.timestart * 1000
            }
            if (eventCal.get(Calendar.YEAR) == year && eventCal.get(Calendar.MONTH) == month) {
                eventCal.get(Calendar.DAY_OF_MONTH)
            } else {
                -1
            }
        }
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Month/Year header
        Text(
            text = SimpleDateFormat("MMMM yyyy", Locale.GERMAN).format(selectedDate),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        // Weekday headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("So", "Mo", "Di", "Mi", "Do", "Fr", "Sa").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Empty cells before first day
            items(firstDayOfWeek) {
                Box(modifier = Modifier.aspectRatio(1f))
            }
            
            // Days of month
            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val isSelected = calendar.get(Calendar.DAY_OF_MONTH) == day
                val hasEvents = eventsByDay[day]?.isNotEmpty() == true
                
                CalendarDayCell(
                    day = day,
                    isSelected = isSelected,
                    hasEvents = hasEvents,
                    onClick = {
                        val newDate = Calendar.getInstance().apply {
                            set(year, month, day)
                        }.time
                        onDateSelected(newDate)
                    }
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isSelected: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.aspectRatio(1f),
        shape = MaterialTheme.shapes.small,
        color = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        }
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                if (hasEvents) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                else 
                                    MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.extraSmall
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarEventCard(
    event: CalendarEventEntity,
    formatTime: (Long) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Time and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = formatTime(event.timestart),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Course name
                    event.courseName?.let { courseName ->
                        Text(
                            text = courseName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Description if available
            event.description?.takeIf { it.isNotBlank() }?.let { description ->
                HorizontalDivider()
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Event type badge
            event.eventtype?.let { type ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDialog(
    availableCourses: List<Pair<Int, String>>,
    availableEventTypes: List<String>,
    selectedCourseId: Int?,
    selectedEventType: String?,
    onCourseSelected: (Int?) -> Unit,
    onEventTypeSelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Course filter
                item {
                    Text(
                        text = "Kurs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(availableCourses) { (courseId, courseName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCourseSelected(courseId) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCourseId == courseId,
                            onClick = { onCourseSelected(courseId) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = courseName)
                    }
                }
                
                // Event type filter
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Termintyp",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(availableEventTypes) { eventType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEventTypeSelected(eventType) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedEventType == eventType,
                            onClick = { onEventTypeSelected(eventType) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = eventType)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("SCHLIESSEN")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onClearFilters()
                    onDismiss()
                }
            ) {
                Text("FILTER ZURÜCKSETZEN")
            }
        }
    )
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
