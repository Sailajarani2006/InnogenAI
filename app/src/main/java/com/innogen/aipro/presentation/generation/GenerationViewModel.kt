package com.innogen.aipro.presentation.generation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innogen.aipro.domain.model.*
import com.innogen.aipro.domain.repository.AIRepository
import com.innogen.aipro.domain.repository.AuthRepository
import com.innogen.aipro.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GenerationUiState(
    val steps          : List<GenerationStep> = defaultSteps(),
    val isComplete     : Boolean              = false,
    val isError        : Boolean              = false,
    val errorMessage   : String               = "",
    val generatedProjectId: String            = ""
)

private fun defaultSteps() = listOf(
    GenerationStep(1, "Understanding Idea",        "Analyzing your concept with AI…"),
    GenerationStep(2, "Designing Architecture",    "Creating system design and tech stack…"),
    GenerationStep(3, "Generating Frontend Code",  "Building UI components and screens…"),
    GenerationStep(4, "Generating Backend Code",   "Creating APIs, routes and controllers…"),
    GenerationStep(5, "Creating Database Schema",  "Designing tables and relationships…"),
    GenerationStep(6, "Writing Documentation",     "Generating README, API docs, tests…"),
    GenerationStep(7, "Finalizing Project",        "Saving to cloud and local storage…")
)

@HiltViewModel
class GenerationViewModel @Inject constructor(
    private val aiRepository     : AIRepository,
    private val projectRepository: ProjectRepository,
    private val authRepository   : AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerationUiState())
    val uiState: StateFlow<GenerationUiState> = _uiState.asStateFlow()

    fun startGeneration(prompt: String) {
        viewModelScope.launch {
            try {
                // Step 1 & 2: Show as active
                activateStep(1)
                delay(800)
                completeStep(1); activateStep(2)
                delay(600)

                // Step 3: Actually call OpenAI
                completeStep(2); activateStep(3)
                val result = aiRepository.generateApp(prompt)

                if (result.isFailure) {
                    errorStep(3, result.exceptionOrNull()?.message ?: "AI generation failed")
                    return@launch
                }

                val project = result.getOrThrow()

                // Simulate steps 4-6 while OpenAI response is already done
                delay(500); completeStep(3); activateStep(4); delay(400)
                completeStep(4); activateStep(5); delay(400)
                completeStep(5); activateStep(6); delay(400)
                completeStep(6); activateStep(7)

                // Step 7: Save project
                val userId  = authRepository.getCurrentUser()?.uid?.ifBlank { null }
                    ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    ?: ""
                val toSave  = project.copy(userId = userId)
                projectRepository.saveProject(toSave)

                delay(300)
                completeStep(7)

                _uiState.update { it.copy(isComplete = true, generatedProjectId = toSave.id) }

            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isError      = true,
                    errorMessage = e.message ?: "Unknown error"
                )}
            }
        }
    }

    private fun activateStep(id: Int) = _uiState.update { state ->
        state.copy(steps = state.steps.map { s ->
            if (s.id == id) s.copy(status = StepStatus.IN_PROGRESS) else s
        })
    }

    private fun completeStep(id: Int) = _uiState.update { state ->
        state.copy(steps = state.steps.map { s ->
            if (s.id == id) s.copy(status = StepStatus.COMPLETE) else s
        })
    }

    private fun errorStep(id: Int, message: String) = _uiState.update { state ->
        state.copy(
            isError      = true,
            errorMessage = message,
            steps        = state.steps.map { s ->
                if (s.id == id) s.copy(status = StepStatus.ERROR) else s
            }
        )
    }
}
