package com.innogen.aipro.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innogen.aipro.domain.model.Project
import com.innogen.aipro.domain.repository.AuthRepository
import com.innogen.aipro.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading   : Boolean      = false,
    val projects    : List<Project> = emptyList(),
    val userName    : String        = "",
    val userEmail   : String        = "",
    val errorMessage: String?       = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val authRepository   : AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadUser()
        loadProjects()
    }

    private fun loadUser() {
        val user = authRepository.getCurrentUser()
        _uiState.update { it.copy(
            userName  = user?.name  ?: user?.email?.substringBefore("@") ?: "User",
            userEmail = user?.email ?: ""
        )}
    }

    private fun loadProjects() {
        val userId = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            projectRepository.getProjects(userId)
                .catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
                .collect { projects ->
                    _uiState.update { it.copy(isLoading = false, projects = projects) }
                }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
        }
    }

    fun refreshProjects() {
        loadProjects()
    }
}
