package com.innogen.aipro.presentation.github

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innogen.aipro.BuildConfig
import com.innogen.aipro.data.remote.GitHubRepositoryImpl
import com.innogen.aipro.data.remote.api.GitHubRepo
import com.innogen.aipro.data.remote.api.GitHubUser
import com.innogen.aipro.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.google.firebase.firestore.FirebaseFirestore
import com.innogen.aipro.data.remote.api.GitHubDeviceCodeResponse
import com.innogen.aipro.domain.model.Project
import kotlinx.coroutines.Job

data class GitHubUiState(
    val isLoading       : Boolean     = false,
    val isConnected     : Boolean     = false,
    val user            : GitHubUser? = null,
    val deviceCodeData  : GitHubDeviceCodeResponse? = null,
    val isWaitingForAuth: Boolean     = false,
    val repoName        : String      = "",
    val repoDescription : String      = "",
    val isPrivate       : Boolean     = false,
    val createdRepo     : GitHubRepo? = null,
    val isPushing       : Boolean     = false,
    val isPushSuccess   : Boolean     = false,
    val errorMessage    : String?     = null,
    val successMessage  : String?     = null
)

@HiltViewModel
class GitHubViewModel @Inject constructor(
    private val gitHubRepository : GitHubRepositoryImpl,
    private val projectRepository: ProjectRepository,
    private val firestore        : FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(GitHubUiState())
    val uiState: StateFlow<GitHubUiState> = _uiState.asStateFlow()
    private var pollJob: Job? = null

    init { checkExistingConnection() }

    // ── Check if already connected ────────────────────────────────────────────

    private fun checkExistingConnection() {
        viewModelScope.launch {
            val cachedUser = gitHubRepository.getCachedUser()
            if (cachedUser != null) {
                _uiState.update { it.copy(isConnected = true, user = cachedUser) }
            }
        }
    }

    // ── GitHub Device Flow ───────────────────────────────────────────────────

    fun startDeviceAuth() {
        pollJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, deviceCodeData = null) }
            gitHubRepository.requestDeviceCode().fold(
                onSuccess = { deviceCodeResponse ->
                    _uiState.update { it.copy(
                        isLoading        = false,
                        deviceCodeData   = deviceCodeResponse,
                        isWaitingForAuth = true
                    )}

                    // Start background polling
                    pollJob = launch {
                        gitHubRepository.pollDeviceToken(
                            deviceCodeResponse.deviceCode,
                            deviceCodeResponse.interval
                        ).fold(
                            onSuccess = { user ->
                                _uiState.update { it.copy(
                                    isConnected      = true,
                                    user             = user,
                                    isWaitingForAuth = false,
                                    deviceCodeData   = null,
                                    successMessage   = "Connected as ${user.login}!"
                                )}
                            },
                            onFailure = { e ->
                                _uiState.update { it.copy(
                                    isWaitingForAuth = false,
                                    deviceCodeData   = null,
                                    errorMessage     = e.message ?: "Authorization failed"
                                )}
                            }
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(
                        isLoading    = false,
                        errorMessage = "Failed to start GitHub authorization: ${e.message}"
                    )}
                }
            )
        }
    }

    // ── Build GitHub OAuth URL and open in browser ────────────────────────────

    fun getOAuthUrl(): String {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        val redirectUri = "innogenai://callback"
        val scope = "repo,user"
        return "https://github.com/login/oauth/authorize" +
               "?client_id=$clientId" +
               "&redirect_uri=${Uri.encode(redirectUri)}" +
               "&scope=${Uri.encode(scope)}" +
               "&state=innogen_secure_state"
    }

    // ── Handle OAuth callback with code ───────────────────────────────────────

    fun handleOAuthCallback(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            gitHubRepository.exchangeCodeForToken(code).fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(
                        isLoading      = false,
                        isConnected    = true,
                        user           = user,
                        successMessage = "Connected as ${user.login}!"
                    )}
                },
                onFailure = { e ->
                    _uiState.update { it.copy(
                        isLoading    = false,
                        errorMessage = "Connection failed: ${e.message}"
                    )}
                }
            )
        }
    }

    // ── Disconnect GitHub account ─────────────────────────────────────────────

    fun disconnect() {
        pollJob?.cancel()
        viewModelScope.launch {
            gitHubRepository.disconnectGitHub()
            _uiState.value = GitHubUiState()
        }
    }

    // ── Create repository ─────────────────────────────────────────────────────

    fun createRepo() {
        val state = _uiState.value
        if (state.repoName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Repository name cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            gitHubRepository.createRepository(
                state.repoName, state.repoDescription, state.isPrivate
            ).fold(
                onSuccess = { repo ->
                    _uiState.update { it.copy(
                        isLoading      = false,
                        createdRepo    = repo,
                        successMessage = "Repository '${repo.name}' created!"
                    )}
                },
                onFailure = { e ->
                    _uiState.update { it.copy(
                        isLoading    = false,
                        errorMessage = e.message
                    )}
                }
            )
        }
    }

    // ── Push code to GitHub ───────────────────────────────────────────────────

    fun pushCode(projectId: String) {
        val user = _uiState.value.user ?: return
        val repo = _uiState.value.createdRepo ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPushing = true, errorMessage = null) }
            val project = projectRepository.getProjectById(projectId)
            if (project == null) {
                _uiState.update { it.copy(isPushing = false, errorMessage = "Project not found") }
                return@launch
            }
            gitHubRepository.pushProjectFiles(user.login, repo.name, project).fold(
                onSuccess = {
                    // Update project in Firestore and local repo
                    try {
                        firestore.collection("projects")
                            .document(projectId)
                            .update("githubRepo", repo.htmlUrl)
                        projectRepository.saveProject(project.copy(githubRepo = repo.htmlUrl))
                    } catch (_: Exception) {}

                    _uiState.update { it.copy(
                        isPushing      = false,
                        isPushSuccess  = true,
                        successMessage = "All files pushed to ${repo.fullName}!"
                    )}
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isPushing = false, errorMessage = e.message) }
                }
            )
        }
    }

    fun updateRepoName(name: String)        = _uiState.update { it.copy(repoName = name) }
    fun updateRepoDescription(desc: String) = _uiState.update { it.copy(repoDescription = desc) }
    fun updateIsPrivate(value: Boolean)     = _uiState.update { it.copy(isPrivate = value) }
    fun clearMessages()                     = _uiState.update { it.copy(errorMessage = null, successMessage = null) }
}
