package com.innogen.aipro.data.remote

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.innogen.aipro.data.remote.api.*
import com.innogen.aipro.domain.model.Project
import com.innogen.aipro.domain.repository.GitHubRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GitHubRepositoryImpl @Inject constructor(
    private val gitHubService: GitHubService,
    private val dataStore    : DataStore<Preferences>
) : GitHubRepository {

    companion object {
        private val GITHUB_TOKEN_KEY = stringPreferencesKey("github_token")
    }

    override suspend fun authenticateWithToken(token: String): Result<GitHubUser> =
        try {
            val response = gitHubService.getUser("token $token")
            if (response.isSuccessful) {
                saveToken(token)
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("GitHub auth failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun createRepository(name: String, description: String, private: Boolean): Result<GitHubRepo> {
        val token = getSavedTokenAsync()
            ?: return Result.failure(Exception("No GitHub token found. Please authenticate first."))
        return try {
            val response = gitHubService.createRepo(
                "token $token",
                CreateRepoRequest(name, description, private)
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else if (response.code() == 403) {
                Result.failure(Exception("Your GitHub token lacks the 'repo' permission. Please generate a new token with 'repo' scope."))
            } else {
                Result.failure(Exception("Failed to create repo: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pushProjectFiles(owner: String, repo: String, project: Project): Result<Boolean> {
        val token = getSavedTokenAsync()
            ?: return Result.failure(Exception("No GitHub token"))

        return try {
            val files = buildFileMap(project)
            var allSuccess = true

            files.forEach { (path, content) ->
                val encoded = Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP)
                val response = gitHubService.createOrUpdateFile(
                    token     = "token $token",
                    owner     = owner,
                    repo      = repo,
                    path      = path,
                    request   = CreateFileRequest(
                        message = "Add $path via InnoGen AI Pro",
                        content = encoded
                    )
                )
                if (!response.isSuccessful) allSuccess = false
            }

            Result.success(allSuccess)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSavedToken(): String? = null // synchronous stub; use flow below

    /** Async token retrieval */
    suspend fun getSavedTokenAsync(): String? =
        dataStore.data.map { it[GITHUB_TOKEN_KEY] }.first()

    override suspend fun saveToken(token: String) {
        dataStore.edit { prefs -> prefs[GITHUB_TOKEN_KEY] = token }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildFileMap(project: Project): Map<String, String> {
        val code = project.generatedCode ?: return emptyMap()
        return buildMap {
            if (code.readme.isNotBlank())         put("README.md",                 code.readme)
            if (code.frontendCode.isNotBlank())   put("frontend/main.js",          code.frontendCode)
            if (code.backendCode.isNotBlank())    put("backend/server.js",         code.backendCode)
            if (code.databaseSchema.isNotBlank()) put("database/schema.sql",       code.databaseSchema)
            if (code.dockerConfig.isNotBlank())   put("Dockerfile",                code.dockerConfig)
            if (code.apiDocs.isNotBlank())        put("docs/API.md",               code.apiDocs)
            if (code.testCases.isNotBlank())      put("tests/app.test.js",         code.testCases)
        }
    }
}
