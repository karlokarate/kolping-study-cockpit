package de.kolping.cockpit.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kolping.cockpit.models.GradeOverview
import de.kolping.cockpit.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GradesViewModel(
    private val repository: StudyRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<GradesUiState>(GradesUiState.Loading)
    val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()
    
    init {
        loadGrades()
    }
    
    fun loadGrades() {
        viewModelScope.launch {
            _uiState.value = GradesUiState.Loading
            
            val result = repository.getGradeOverview()
            result.fold(
                onSuccess = { gradeOverview ->
                    _uiState.value = GradesUiState.Success(gradeOverview)
                },
                onFailure = { exception ->
                    _uiState.value = GradesUiState.Error(exception.message ?: "Unknown error")
                }
            )
        }
    }
    
    sealed class GradesUiState {
        object Loading : GradesUiState()
        data class Success(val gradeOverview: GradeOverview) : GradesUiState()
        data class Error(val message: String) : GradesUiState()
    }
}
