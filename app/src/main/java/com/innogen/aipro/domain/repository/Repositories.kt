package com.innogen.aipro.domain.repository

import com.innogen.aipro.data.remote.api.GitHubRepo
import com.innogen.aipro.data.remote.api.GitHubUser
import com.innogen.aipro.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getProjects(userId: String): Flow<List<Project>>
    suspend fun getProjectById(id: String): Project?
    suspend fun saveProject(project: Project)
    suspend fun deleteProject(id: String)
    suspend fun syncWithFirestore(userId: String)
}

interface AIRepository {
    suspend fun generateApp(prompt: String): Result<Project>
    suspend fun regenerateSection(projectId: String, instruction: String): Result<Project>
    suspend fun detectBugs(code: String): Result<List<BugReport>>
    suspend fun analyzeSecurity(code: String): Result<List<SecurityIssue>>
    suspend fun generateTests(code: String): Result<List<TestCase>>
}

interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(email: String, password: String, name: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signOut()
    fun getCurrentUser(): User?
    val isLoggedIn: Boolean
}

interface GitHubRepository {
    suspend fun authenticateWithToken(token: String): Result<GitHubUser>
    suspend fun createRepository(name: String, description: String, private: Boolean): Result<GitHubRepo>
    suspend fun pushProjectFiles(owner: String, repo: String, project: Project): Result<Boolean>
    fun getSavedToken(): String?
    suspend fun saveToken(token: String)
}