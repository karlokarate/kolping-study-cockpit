package de.kolping.cockpit.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kolping.cockpit.android.database.entities.CalendarEventEntity
import de.kolping.cockpit.android.repository.OfflineRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    
    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    
    private val _selectedDate = MutableStateFlow<Date>(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()
    
    private val _filterCourseId = MutableStateFlow<Int?>(null)
    val filterCourseId: StateFlow<Int?> = _filterCourseId.asStateFlow()
    
    private val _filterEventType = MutableStateFlow<String?>(null)
    val filterEventType: StateFlow<String?> = _filterEventType.asStateFlow()
    
    init {
        loadEvents()
    }
    
    /**
     * Load calendar events from offline database
     */
    private fun loadEvents() {
        viewModelScope.launch {
            try {
                _uiState.value = CalendarUiState.Loading
                
                offlineRepository.getAllEvents().collect { events ->
                    val filteredEvents = applyFilters(events)
                    
                    _uiState.value = CalendarUiState.Success(
                        allEvents = filteredEvents,
                        selectedDayEvents = getEventsForSelectedDay(filteredEvents)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load calendar events", e)
                _uiState.value = CalendarUiState.Error("Fehler beim Laden: ${e.message}")
            }
        }
    }
    
    /**
     * Select a date to view its events
     */
    fun selectDate(date: Date) {
        _selectedDate.value = date
        
        val currentState = _uiState.value
        if (currentState is CalendarUiState.Success) {
            _uiState.value = currentState.copy(
                selectedDayEvents = getEventsForSelectedDay(currentState.allEvents)
            )
        }
    }
    
    /**
     * Filter events by course ID
     */
    fun filterByCourse(courseId: Int?) {
        _filterCourseId.value = courseId
        loadEvents()
    }
    
    /**
     * Filter events by event type
     */
    fun filterByEventType(eventType: String?) {
        _filterEventType.value = eventType
        loadEvents()
    }
    
    /**
     * Clear all filters
     */
    fun clearFilters() {
        _filterCourseId.value = null
        _filterEventType.value = null
        loadEvents()
    }
    
    /**
     * Refresh calendar data
     */
    fun refresh() {
        loadEvents()
    }
    
    /**
     * Apply active filters to events list
     */
    private fun applyFilters(events: List<CalendarEventEntity>): List<CalendarEventEntity> {
        var filtered = events
        
        _filterCourseId.value?.let { courseId ->
            filtered = filtered.filter { it.courseId == courseId }
        }
        
        _filterEventType.value?.let { eventType ->
            filtered = filtered.filter { it.eventtype == eventType }
        }
        
        return filtered
    }
    
    /**
     * Get events for the currently selected day
     */
    private fun getEventsForSelectedDay(events: List<CalendarEventEntity>): List<CalendarEventEntity> {
        val selectedCalendar = Calendar.getInstance().apply {
            time = _selectedDate.value
        }
        
        val startOfDay = selectedCalendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis / 1000
        
        val endOfDay = selectedCalendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis / 1000
        
        return events.filter { event ->
            event.timestart in startOfDay..endOfDay
        }.sortedBy { it.timestart }
    }
    
    /**
     * Get events for a specific month
     */
    fun getEventsForMonth(year: Int, month: Int): List<CalendarEventEntity> {
        val currentState = _uiState.value
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
        val currentState = _uiState.value
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
        val currentState = _uiState.value
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
