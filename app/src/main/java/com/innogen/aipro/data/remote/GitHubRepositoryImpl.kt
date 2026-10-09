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
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Named

class GitHubRepositoryImpl @Inject constructor(
    private val gitHubService    : GitHubService,
    private val gitHubOAuthService: GitHubOAuthService,
    private val dataStore        : DataStore<Preferences>,
    @Named("github_client_id")     private val clientId    : String,
    @Named("github_client_secret") private val clientSecret: String
) : GitHubRepository {

    companion object {
        private val GITHUB_TOKEN_KEY    = stringPreferencesKey("github_token")
        private val GITHUB_LOGIN_KEY    = stringPreferencesKey("github_login")
        private val GITHUB_NAME_KEY     = stringPreferencesKey("github_name")
        private val GITHUB_AVATAR_KEY   = stringPreferencesKey("github_avatar")
        private val GITHUB_REPOS_KEY    = stringPreferencesKey("github_repos")
    }

    // ── Exchange OAuth code for access token ──────────────────────────────────

    suspend fun exchangeCodeForToken(code: String): Result<GitHubUser> {
        return try {
            // Step 1: Exchange code for token
            val tokenResponse = gitHubOAuthService.exchangeCodeForToken(
                GitHubTokenRequest(
                    clientId     = clientId,
                    clientSecret = clientSecret,
                    code         = code
                )
            )

            if (!tokenResponse.isSuccessful) {
                return Result.failure(Exception("Token exchange failed: ${tokenResponse.code()}"))
            }

            val tokenBody = tokenResponse.body()
            if (tokenBody?.error != null) {
                return Result.failure(Exception("OAuth error: ${tokenBody.errorDescription}"))
            }

            val accessToken = tokenBody?.accessToken
                ?: return Result.failure(Exception("No access token received"))

            // Step 2: Save token
            saveToken(accessToken)

            // Step 3: Fetch user info
            val userResponse = gitHubService.getUser("token $accessToken")
            if (!userResponse.isSuccessful) {
                return Result.failure(Exception("Failed to fetch user: ${userResponse.code()}"))
            }

            val user = userResponse.body()!!

            // Step 4: Cache user info in DataStore
            dataStore.edit { prefs ->
                prefs[GITHUB_LOGIN_KEY]  = user.login
                prefs[GITHUB_NAME_KEY]   = user.name ?: user.login
                prefs[GITHUB_AVATAR_KEY] = user.avatarUrl
                prefs[GITHUB_REPOS_KEY]  = user.publicRepos.toString()
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Device Flow ──────────────────────────────────────────────────────────

    suspend fun requestDeviceCode(): Result<GitHubDeviceCodeResponse> =
        try {
            val response = gitHubOAuthService.requestDeviceCode(
                GitHubDeviceCodeRequest(clientId = clientId)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to request device code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun pollDeviceToken(deviceCode: String, intervalSeconds: Int = 5): Result<GitHubUser> {
        val intervalMs = (if (intervalSeconds < 5) 5 else intervalSeconds) * 1000L
        val maxAttempts = 60
        var attempts = 0

        while (attempts < maxAttempts) {
            kotlinx.coroutines.delay(intervalMs)
            attempts++

            try {
                val tokenResponse = gitHubOAuthService.pollDeviceToken(
                    GitHubDeviceTokenRequest(clientId = clientId, deviceCode = deviceCode)
                )
                val body = tokenResponse.body()
                val accessToken = body?.accessToken

                if (accessToken != null) {
                    saveToken(accessToken)
                    val userResponse = gitHubService.getUser("token $accessToken")
                    if (userResponse.isSuccessful && userResponse.body() != null) {
                        val user = userResponse.body()!!
                        dataStore.edit { prefs ->
                            prefs[GITHUB_LOGIN_KEY]  = user.login
                            prefs[GITHUB_NAME_KEY]   = user.name ?: user.login
                            prefs[GITHUB_AVATAR_KEY] = user.avatarUrl
                            prefs[GITHUB_REPOS_KEY]  = user.publicRepos.toString()
                        }
                        return Result.success(user)
                    } else {
                        return Result.failure(Exception("Failed to load user profile"))
                    }
                } else if (body?.error == "authorization_pending") {
                    continue
                } else if (body?.error == "slow_down") {
                    kotlinx.coroutines.delay(5000L)
                    continue
                } else if (body?.error != null) {
                    return Result.failure(Exception(body.errorDescription ?: body.error))
                }
            } catch (e: Exception) {
                // Keep polling on temporary glitches
            }
        }
        return Result.failure(Exception("Authorization timed out. Please try again."))
    }

    // ── Authenticate with manually entered token (fallback) ───────────────────

    override suspend fun authenticateWithToken(token: String): Result<GitHubUser> =
        try {
            val response = gitHubService.getUser("token $token")
            if (response.isSuccessful) {
                val user = response.body()!!
                saveToken(token)
                dataStore.edit { prefs ->
                    prefs[GITHUB_LOGIN_KEY]  = user.login
                    prefs[GITHUB_NAME_KEY]   = user.name ?: user.login
                    prefs[GITHUB_AVATAR_KEY] = user.avatarUrl
                    prefs[GITHUB_REPOS_KEY]  = user.publicRepos.toString()
                }
                Result.success(user)
            } else {
                Result.failure(Exception("GitHub auth failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    // ── Create repository ─────────────────────────────────────────────────────

    override suspend fun createRepository(
        name: String, description: String, private: Boolean
    ): Result<GitHubRepo> {
        val token = getSavedTokenAsync()
            ?: return Result.failure(Exception("Not connected to GitHub. Please sign in first."))
        return try {
            val response = gitHubService.createRepo(
                "token $token",
                CreateRepoRequest(name, description, private)
            )
            if (response.isSuccessful) Result.success(response.body()!!)
            else Result.failure(Exception("Failed to create repo: ${response.errorBody()?.string()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Push project files ────────────────────────────────────────────────────

    override suspend fun pushProjectFiles(
        owner: String, repo: String, project: Project
    ): Result<Boolean> {
        val token = getSavedTokenAsync()
            ?: return Result.failure(Exception("Not connected to GitHub"))
        return try {
            val files = buildFileMap(project)
            var allSuccess = true
            files.forEach { (path, content) ->
                try {
                    val encoded = Base64.encodeToString(
                        content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP
                    )
                    val response = gitHubService.createOrUpdateFile(
                        token   = "token $token",
                        owner   = owner,
                        repo    = repo,
                        path    = path,
                        request = CreateFileRequest(
                            message = "Add $path via InnoGen AI Pro",
                            content = encoded
                        )
                    )
                    if (!response.isSuccessful) allSuccess = false
                } catch (e: Exception) { allSuccess = false }
            }
            Result.success(allSuccess)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Token helpers ─────────────────────────────────────────────────────────

    override fun getSavedToken(): String? = runBlocking {
        try { dataStore.data.map { it[GITHUB_TOKEN_KEY] }.first() }
        catch (e: Exception) { null }
    }

    suspend fun getSavedTokenAsync(): String? =
        try { dataStore.data.map { it[GITHUB_TOKEN_KEY] }.first() }
        catch (e: Exception) { null }

    override suspend fun saveToken(token: String) {
        dataStore.edit { prefs -> prefs[GITHUB_TOKEN_KEY] = token }
    }

    // ── Get cached user info ──────────────────────────────────────────────────

    suspend fun getCachedUser(): GitHubUser? {
        return try {
            val prefs = dataStore.data.first()
            val login = prefs[GITHUB_LOGIN_KEY] ?: return null
            GitHubUser(
                id         = 0,
                login      = login,
                name       = prefs[GITHUB_NAME_KEY],
                email      = null,
                avatarUrl  = prefs[GITHUB_AVATAR_KEY] ?: "",
                publicRepos= prefs[GITHUB_REPOS_KEY]?.toIntOrNull() ?: 0,
                bio        = null
            )
        } catch (e: Exception) { null }
    }

    suspend fun disconnectGitHub() {
        dataStore.edit { prefs ->
            prefs.remove(GITHUB_TOKEN_KEY)
            prefs.remove(GITHUB_LOGIN_KEY)
            prefs.remove(GITHUB_NAME_KEY)
            prefs.remove(GITHUB_AVATAR_KEY)
            prefs.remove(GITHUB_REPOS_KEY)
        }
    }

    private fun buildFileMap(project: Project): Map<String, String> {
        val code = project.generatedCode ?: return emptyMap()
        return buildMap {
            if (code.readme.isNotBlank())         put("README.md",           code.readme)
            if (code.frontendCode.isNotBlank())   put("frontend/main.js",    code.frontendCode)
            if (code.backendCode.isNotBlank())    put("backend/server.js",   code.backendCode)
            if (code.databaseSchema.isNotBlank()) put("database/schema.sql", code.databaseSchema)
            if (code.dockerConfig.isNotBlank())   put("Dockerfile",          code.dockerConfig)
            if (code.apiDocs.isNotBlank())        put("docs/API.md",         code.apiDocs)
            if (code.testCases.isNotBlank())      put("tests/app.test.js",   code.testCases)
        }
    }
}
