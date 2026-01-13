package de.kolping.cockpit.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kolping.cockpit.android.database.entities.CalendarEventEntity
import de.kolping.cockpit.android.repository.OfflineRepository
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for CalendarScreen
 * Manages calendar events display with month view and filtering
 * Based on Issue #6 PR 5 requirements
 */
class CalendarViewModel(
    private val offlineRepository: OfflineRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "CalendarViewModel"
    }
    
    private val _selectedDate = MutableStateFlow<Date>(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()
    
    private val _filterCourseId = MutableStateFlow<Int?>(null)
    val filterCourseId: StateFlow<Int?> = _filterCourseId.asStateFlow()
    
    private val _filterEventType = MutableStateFlow<String?>(null)
    val filterEventType: StateFlow<String?> = _filterEventType.asStateFlow()
    
    /**
     * UI state using combine to properly manage Flow lifecycle and apply filters
     */
    val uiState: StateFlow<CalendarUiState> = combine(
        offlineRepository.getAllEvents(),
        _selectedDate,
        _filterCourseId,
        _filterEventType
    ) { events, selectedDate, courseId, eventType ->
        try {
            // Apply filters
            var filteredEvents = events
            
            courseId?.let { id ->
                filteredEvents = filteredEvents.filter { it.courseId == id }
            }
            
            eventType?.let { type ->
                filteredEvents = filteredEvents.filter { it.eventtype == type }
            }
            
            CalendarUiState.Success(
                allEvents = filteredEvents,
                selectedDayEvents = getEventsForSelectedDay(filteredEvents, selectedDate)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process calendar events", e)
            CalendarUiState.Error("Fehler beim Laden der Kalenderereignisse: ${e.message}")
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState.Loading
    )
    
    /**
     * Select a date to view its events
     */
    fun selectDate(date: Date) {
        _selectedDate.value = date
    }
    
    /**
     * Filter events by course ID
     */
    fun filterByCourse(courseId: Int?) {
        _filterCourseId.value = courseId
    }
    
    /**
     * Filter events by event type
     */
    fun filterByEventType(eventType: String?) {
        _filterEventType.value = eventType
    }
    
    /**
     * Clear all filters
     */
    fun clearFilters() {
        _filterCourseId.value = null
        _filterEventType.value = null
    }
    
    /**
     * Get events for a specific day
     */
    private fun getEventsForSelectedDay(events: List<CalendarEventEntity>, selectedDate: Date): List<CalendarEventEntity> {
        val startOfDayCalendar = Calendar.getInstance().apply {
            time = selectedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = startOfDayCalendar.timeInMillis / 1000

        val endOfDayCalendar = Calendar.getInstance().apply {
            time = selectedDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfDay = endOfDayCalendar.timeInMillis / 1000
        
        return events.filter { event ->
            event.timestart in startOfDay..endOfDay
        }.sortedBy { it.timestart }
    }
    
    /**
     * Get events for a specific month
     */
    fun getEventsForMonth(year: Int, month: Int): List<CalendarEventEntity> {
        val currentState = uiState.value
        if (currentState !is CalendarUiState.Success) return emptyList()
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val startOfMonth = calendar.timeInMillis / 1000
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        
        val endOfMonth = calendar.timeInMillis / 1000
        
        return currentState.allEvents.filter { event ->
            event.timestart in startOfMonth..endOfMonth
        }
    }
    
    /**
     * Get all unique event types for filtering
     */
    fun getAvailableEventTypes(): List<String> {
        val currentState = uiState.value
        if (currentState !is CalendarUiState.Success) return emptyList()
        
        return currentState.allEvents
            .mapNotNull { it.eventtype }
            .distinct()
            .sorted()
    }
    
    /**
     * Get all unique courses for filtering
     */
    fun getAvailableCourses(): List<Pair<Int, String>> {
        val currentState = uiState.value
        if (currentState !is CalendarUiState.Success) return emptyList()
        
        return currentState.allEvents
            .filter { it.courseId != null && it.courseName != null }
            .map { it.courseId!! to it.courseName!! }
            .distinct()
            .sortedBy { it.second }
    }
    
    /**
     * Format date to display string
     */
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
        return sdf.format(Date(timestamp * 1000))
    }
    
    /**
     * Format time to display string
     */
    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.GERMAN)
        return sdf.format(Date(timestamp * 1000))
    }
    
    /**
     * Refresh calendar data
     */
    fun refresh() {
        // Trigger a refresh by re-collecting the flow
        // The flow will automatically re-emit when the database changes
        // This is a no-op since we're using reactive flows
    }
    
    /**
     * UI State for CalendarScreen
     */
    sealed class CalendarUiState {
        object Loading : CalendarUiState()
        data class Success(
            val allEvents: List<CalendarEventEntity>,
            val selectedDayEvents: List<CalendarEventEntity>
        ) : CalendarUiState()
        data class Error(val message: String) : CalendarUiState()
    }
}
