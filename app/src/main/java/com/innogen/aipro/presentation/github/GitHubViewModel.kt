package com.innogen.aipro.presentation.github

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innogen.aipro.data.remote.GitHubRepositoryImpl
import com.innogen.aipro.data.remote.api.GitHubRepo
import com.innogen.aipro.data.remote.api.GitHubUser
import com.innogen.aipro.domain.repository.GitHubRepository
import com.innogen.aipro.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GitHubUiState(
    val isLoading      : Boolean     = false,
    val user           : GitHubUser? = null,
    val token          : String      = "",
    val repoName       : String      = "",
    val repoDescription: String      = "",
    val isPrivate      : Boolean     = false,
    val createdRepo    : GitHubRepo? = null,
    val isPushing      : Boolean     = false,
    val isPushSuccess  : Boolean     = false,
    val errorMessage   : String?     = null
)

@HiltViewModel
class GitHubViewModel @Inject constructor(
    private val gitHubRepository : GitHubRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GitHubUiState())
    val uiState: StateFlow<GitHubUiState> = _uiState.asStateFlow()

    fun authenticate(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            gitHubRepository.authenticateWithToken(token).fold(
                onSuccess = { user -> _uiState.update { it.copy(isLoading = false, user = user, token = token) } },
                onFailure = { e   -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
            )
        }
    }

    fun createRepo() {
        val state = _uiState.value
        if (state.repoName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Repository name cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            gitHubRepository.createRepository(state.repoName, state.repoDescription, state.isPrivate).fold(
                onSuccess = { repo -> _uiState.update { it.copy(isLoading = false, createdRepo = repo) } },
                onFailure = { e   -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
            )
        }
    }

    fun pushCode(projectId: String) {
        val state = _uiState.value
        val user  = state.user ?: return
        val repo  = state.createdRepo ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPushing = true) }
            val project = projectRepository.getProjectById(projectId)
            if (project != null) {
                gitHubRepository.pushProjectFiles(user.login, repo.name, project).fold(
                    onSuccess = { _uiState.update { it.copy(isPushing = false, isPushSuccess = true) } },
                    onFailure = { e -> _uiState.update { it.copy(isPushing = false, errorMessage = e.message) } }
                )
            }
        }
    }

    fun updateRepoName(name: String)        = _uiState.update { it.copy(repoName = name) }
    fun updateRepoDescription(desc: String) = _uiState.update { it.copy(repoDescription = desc) }
    fun updateIsPrivate(private: Boolean)   = _uiState.update { it.copy(isPrivate = private) }
}
