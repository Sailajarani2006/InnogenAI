package com.innogen.aipro.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innogen.aipro.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading   : Boolean = false,
    val isSuccess   : Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (!validateInputs(email, password)) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.signInWithEmail(email, password).fold(
                onSuccess = { _uiState.value = AuthUiState(isSuccess = true) },
                onFailure = { e -> _uiState.value = AuthUiState(errorMessage = e.message) }
            )
        }
    }

    fun signUp(email: String, password: String, name: String) {
        if (name.isBlank()) {
            _uiState.value = AuthUiState(errorMessage = "Name cannot be empty")
            return
        }
        if (!validateInputs(email, password)) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.signUpWithEmail(email, password, name).fold(
                onSuccess = { _uiState.value = AuthUiState(isSuccess = true) },
                onFailure = { e -> _uiState.value = AuthUiState(errorMessage = e.message) }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.signInWithGoogle(idToken).fold(
                onSuccess = { _uiState.value = AuthUiState(isSuccess = true) },
                onFailure = { e -> _uiState.value = AuthUiState(errorMessage = e.message) }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isBlank() || !email.contains("@")) {
            _uiState.value = AuthUiState(errorMessage = "Please enter a valid email")
            return false
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState(errorMessage = "Password must be at least 6 characters")
            return false
        }
        return true
    }
}
