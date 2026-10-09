package com.innogen.aipro.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.innogen.aipro.domain.model.Project
import com.innogen.aipro.domain.repository.AuthRepository
import com.innogen.aipro.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    private var projectsJob: Job? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    init {
        authListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            _uiState.update { it.copy(
                userName  = user?.displayName?.ifBlank { null } ?: user?.email?.substringBefore("@") ?: "User",
                userEmail = user?.email ?: ""
            )}
            startObservingProjects(user?.uid ?: "")
        }
        FirebaseAuth.getInstance().addAuthStateListener(authListener!!)
        refreshProjects()
    }

    override fun onCleared() {
        super.onCleared()
        authListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
    }

    fun refreshProjects() {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: authRepository.getCurrentUser()?.uid ?: ""
        _uiState.update { it.copy(
            userName  = user?.displayName?.ifBlank { null } ?: user?.email?.substringBefore("@") ?: "User",
            userEmail = user?.email ?: ""
        )}
        startObservingProjects(userId)
    }

    private fun startObservingProjects(userId: String) {
        if (userId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, projects = emptyList()) }
            return
        }

        projectsJob?.cancel()
        projectsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            launch {
                try {
                    projectRepository.syncWithFirestore(userId)
                } catch (e: Exception) {
                    // Non-fatal background initial sync
                }
            }
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
}
