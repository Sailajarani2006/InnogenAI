package com.innogen.aipro.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.innogen.aipro.data.remote.api.*
import com.innogen.aipro.domain.model.Project
import com.innogen.aipro.domain.repository.GitHubRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * FIX-03 (CRIT-002): GitHub PAT is now stored in EncryptedSharedPreferences
 * (AES-256-GCM via Android Keystore) instead of plaintext DataStore.
 *
 * Previous implementation stored the token in unencrypted DataStore which was:
 *  - Readable via ADB backup (now also blocked by FIX-02: allowBackup=false)
 *  - Readable by rooted devices with plain file access
 *
 * EncryptedSharedPreferences encrypts both keys and values using AES-256-GCM,
 * with the master key stored in Android Keystore (hardware-backed where available).
 */
class GitHubRepositoryImpl @Inject constructor(
    private val gitHubService: GitHubService,
    @ApplicationContext private val context: Context
) : GitHubRepository {

    companion object {
        private const val PREFS_FILE  = "innogen_secure_github"
        private const val TOKEN_KEY   = "github_pat"
    }

    // Lazily constructed — EncryptedSharedPreferences init is blocking I/O
    private val encryptedPrefs: SharedPreferences by lazy { buildEncryptedPrefs() }

    // ── Public API ────────────────────────────────────────────────────────────

    override suspend fun authenticateWithToken(token: String): Result<GitHubUser> =
        withContext(Dispatchers.IO) {
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
        }

    override suspend fun createRepository(
        name: String,
        description: String,
        private: Boolean
    ): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        val token = getSavedTokenAsync()
            ?: return@withContext Result.failure(
                Exception("No GitHub token found. Please authenticate first.")
            )
        try {
            val response = gitHubService.createRepo(
                "token $token",
                CreateRepoRequest(name, description, private)
            )
            when {
                response.isSuccessful -> Result.success(response.body()!!)
                response.code() == 403 ->
                    Result.failure(Exception("Token lacks 'repo' permission. Please generate a new token with 'repo' scope."))
                else ->
                    Result.failure(Exception("Failed to create repo (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pushProjectFiles(
        owner: String,
        repo: String,
        project: Project
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = getSavedTokenAsync()
            ?: return@withContext Result.failure(Exception("No GitHub token"))

        try {
            val files = buildFileMap(project)
            var allSuccess = true
            files.forEach { (path, content) ->
                val encoded = Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP)
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
            }
            Result.success(allSuccess)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSavedToken(): String? =
        encryptedPrefs.getString(TOKEN_KEY, null)

    /** Async token retrieval (runs on IO dispatcher) */
    suspend fun getSavedTokenAsync(): String? =
        withContext(Dispatchers.IO) {
            encryptedPrefs.getString(TOKEN_KEY, null)
        }

    override suspend fun saveToken(token: String) =
        withContext(Dispatchers.IO) {
            encryptedPrefs.edit().putString(TOKEN_KEY, token).apply()
        }

    suspend fun clearToken() =
        withContext(Dispatchers.IO) {
            encryptedPrefs.edit().remove(TOKEN_KEY).apply()
        }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun buildFileMap(project: Project): Map<String, String> {
        val code = project.generatedCode ?: return emptyMap()
        return buildMap {
            if (code.readme.isNotBlank())         put("README.md",          code.readme)
            if (code.frontendCode.isNotBlank())   put("frontend/main.js",   code.frontendCode)
            if (code.backendCode.isNotBlank()) {
                put("backend/server.js",   code.backendCode)
                put("backend/package.json", """
                    {
                      "name": "backend",
                      "version": "1.0.0",
                      "main": "server.js",
                      "scripts": { "start": "node server.js" },
                      "dependencies": {
                        "express": "^4.18.2",
                        "cors": "^2.8.5",
                        "dotenv": "^16.3.1",
                        "pg": "^8.11.3"
                      }
                    }
                """.trimIndent())
            }
            if (code.databaseSchema.isNotBlank()) put("database/schema.sql", code.databaseSchema)
            if (code.dockerConfig.isNotBlank())   put("Dockerfile",          code.dockerConfig)
            if (code.apiDocs.isNotBlank())        put("docs/API.md",         code.apiDocs)
            if (code.testCases.isNotBlank())      put("tests/app.test.js",   code.testCases)
        }
    }
}
