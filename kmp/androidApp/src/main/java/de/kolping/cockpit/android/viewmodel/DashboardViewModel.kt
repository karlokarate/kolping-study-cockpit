package de.kolping.cockpit.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kolping.cockpit.models.*
import de.kolping.cockpit.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: StudyRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDashboard()
    }
    
    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            
            try {
                val gradeResult = repository.getGradeOverview()
                val moodleResult = repository.getMoodleDashboard()
                val deadlinesResult = repository.getUpcomingDeadlines()
                
                _uiState.value = DashboardUiState.Success(
                    gradeOverview = gradeResult.getOrNull(),
                    moodleDashboard = moodleResult.getOrNull(),
                    upcomingDeadlines = deadlinesResult.getOrNull() ?: emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    sealed class DashboardUiState {
        object Loading : DashboardUiState()
        data class Success(
            val gradeOverview: GradeOverview?,
            val moodleDashboard: MoodleDashboard?,
            val upcomingDeadlines: List<MoodleEvent>
        ) : DashboardUiState()
        data class Error(val message: String) : DashboardUiState()
    }
}
