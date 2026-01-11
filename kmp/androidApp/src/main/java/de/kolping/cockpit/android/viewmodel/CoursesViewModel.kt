package de.kolping.cockpit.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kolping.cockpit.models.MoodleCourse
import de.kolping.cockpit.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CoursesViewModel(
    private val repository: StudyRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<CoursesUiState>(CoursesUiState.Loading)
    val uiState: StateFlow<CoursesUiState> = _uiState.asStateFlow()
    
    init {
        loadCourses()
    }
    
    fun loadCourses() {
        viewModelScope.launch {
            _uiState.value = CoursesUiState.Loading
            
            val result = repository.getMoodleCourses()
            result.fold(
                onSuccess = { courses ->
                    _uiState.value = CoursesUiState.Success(courses)
                },
                onFailure = { exception ->
                    _uiState.value = CoursesUiState.Error(exception.message ?: "Unknown error")
                }
            )
        }
    }
    
    sealed class CoursesUiState {
        object Loading : CoursesUiState()
        data class Success(val courses: List<MoodleCourse>) : CoursesUiState()
        data class Error(val message: String) : CoursesUiState()
    }
}
