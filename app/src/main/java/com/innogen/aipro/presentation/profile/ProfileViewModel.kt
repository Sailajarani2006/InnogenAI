package com.innogen.aipro.presentation.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innogen.aipro.domain.repository.AuthRepository
import com.innogen.aipro.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository   : AuthRepository,
    private val projectRepository: ProjectRepository,
    private val dataStore        : DataStore<Preferences>
) : ViewModel() {

    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    }

    val isDarkMode: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[DARK_MODE_KEY] ?: false }

    private val _projectCount = MutableStateFlow(0)
    val projectCount: StateFlow<Int> = _projectCount.asStateFlow()

    init { loadProjectCount() }

    private fun loadProjectCount() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            projectRepository.getProjects(uid).collect { projects ->
                _projectCount.value = projects.size
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[DARK_MODE_KEY] = !(prefs[DARK_MODE_KEY] ?: false)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun getCurrentUser() = authRepository.getCurrentUser()
}
