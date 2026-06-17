package com.innogen.aipro.presentation.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innogen.aipro.domain.model.*
import com.innogen.aipro.domain.repository.AIRepository
import com.innogen.aipro.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectUiState(
    val isLoading    : Boolean          = true,
    val project      : Project?         = null,
    val bugs         : List<BugReport>  = emptyList(),
    val securityIssues: List<SecurityIssue> = emptyList(),
    val testCases    : List<TestCase>   = emptyList(),
    val isAnalyzing  : Boolean          = false,
    val editPrompt   : String           = "",
    val isRegenerating: Boolean         = false,
    val errorMessage : String?          = null
)

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val aiRepository     : AIRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val project = projectRepository.getProjectById(projectId)
            _uiState.update { it.copy(isLoading = false, project = project) }
        }
    }

    fun detectBugs() {
        val code = _uiState.value.project?.generatedCode?.frontendCode ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            aiRepository.detectBugs(code).fold(
                onSuccess = { bugs -> _uiState.update { it.copy(isAnalyzing = false, bugs = bugs) } },
                onFailure = { e  -> _uiState.update { it.copy(isAnalyzing = false, errorMessage = e.message) } }
            )
        }
    }

    fun analyzeSecurity() {
        val code = _uiState.value.project?.generatedCode?.backendCode ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            aiRepository.analyzeSecurity(code).fold(
                onSuccess = { issues -> _uiState.update { it.copy(isAnalyzing = false, securityIssues = issues) } },
                onFailure = { e     -> _uiState.update { it.copy(isAnalyzing = false, errorMessage = e.message) } }
            )
        }
    }

    fun generateTests() {
        val code = _uiState.value.project?.generatedCode?.backendCode ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            aiRepository.generateTests(code).fold(
                onSuccess = { tests -> _uiState.update { it.copy(isAnalyzing = false, testCases = tests) } },
                onFailure = { e    -> _uiState.update { it.copy(isAnalyzing = false, errorMessage = e.message) } }
            )
        }
    }

    fun regenerate(instruction: String) {
        val projectId = _uiState.value.project?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRegenerating = true) }
            aiRepository.regenerateSection(projectId, instruction).fold(
                onSuccess = { updated ->
                    projectRepository.saveProject(updated)
                    _uiState.update { it.copy(isRegenerating = false, project = updated) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isRegenerating = false, errorMessage = e.message) }
                }
            )
        }
    }
}
