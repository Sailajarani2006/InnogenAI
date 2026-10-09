package com.innogen.aipro.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

// ── GitHub OAuth token exchange models ───────────────────────────────────────

data class GitHubTokenRequest(
    @SerializedName("client_id")     val clientId    : String,
    @SerializedName("client_secret") val clientSecret: String,
    val code                                         : String
)

data class GitHubTokenResponse(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("token_type")   val tokenType  : String?,
    val error                                       : String?,
    @SerializedName("error_description")
    val errorDescription                            : String?
)

// ── GitHub API models ─────────────────────────────────────────────────────────

data class GitHubUser(
    val id        : Long,
    val login     : String,
    val name      : String?,
    val email     : String?,
    @SerializedName("avatar_url")   val avatarUrl  : String,
    @SerializedName("public_repos") val publicRepos: Int,
    val bio       : String?
)

data class GitHubRepo(
    val id         : Long,
    val name       : String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("html_url") val htmlUrl  : String,
    val private    : Boolean,
    val description: String?
)

data class CreateRepoRequest(
    val name       : String,
    val description: String,
    val private    : Boolean = false,
    @SerializedName("auto_init") val autoInit: Boolean = true
)

data class CreateFileRequest(
    val message: String,
    val content: String,   // base64-encoded
    val sha    : String? = null
)

data class CreateFileResponse(
    val content: FileContent?,
    val commit : CommitInfo?
)

data class FileContent(
    val name: String,
    @SerializedName("html_url") val htmlUrl: String
)

data class CommitInfo(val sha: String)

// ── Device Flow Models ───────────────────────────────────────────────────────

data class GitHubDeviceCodeRequest(
    @SerializedName("client_id") val clientId: String,
    val scope: String = "repo,user"
)

data class GitHubDeviceCodeResponse(
    @SerializedName("device_code") val deviceCode: String,
    @SerializedName("user_code") val userCode: String,
    @SerializedName("verification_uri") val verificationUri: String,
    @SerializedName("expires_in") val expiresIn: Int,
    val interval: Int = 5
)

data class GitHubDeviceTokenRequest(
    @SerializedName("client_id") val clientId: String,
    @SerializedName("device_code") val deviceCode: String,
    @SerializedName("grant_type") val grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
)

// ── OAuth Service (github.com) ────────────────────────────────────────────────

interface GitHubOAuthService {
    @POST("login/oauth/access_token")
    @Headers("Accept: application/json")
    suspend fun exchangeCodeForToken(
        @Body request: GitHubTokenRequest
    ): Response<GitHubTokenResponse>

    @POST("login/device/code")
    @Headers("Accept: application/json")
    suspend fun requestDeviceCode(
        @Body request: GitHubDeviceCodeRequest
    ): Response<GitHubDeviceCodeResponse>

    @POST("login/oauth/access_token")
    @Headers("Accept: application/json")
    suspend fun pollDeviceToken(
        @Body request: GitHubDeviceTokenRequest
    ): Response<GitHubTokenResponse>
}

// ── GitHub API Service (api.github.com) ──────────────────────────────────────

interface GitHubService {

    @GET("user")
    suspend fun getUser(
        @Header("Authorization") token: String
    ): Response<GitHubUser>

    @GET("user/repos")
    suspend fun getUserRepos(
        @Header("Authorization") token  : String,
        @Query("per_page")       perPage: Int    = 30,
        @Query("sort")           sort   : String = "updated"
    ): Response<List<GitHubRepo>>

    @POST("user/repos")
    suspend fun createRepo(
        @Header("Authorization") token  : String,
        @Body                    request: CreateRepoRequest
    ): Response<GitHubRepo>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Header("Authorization")                  token  : String,
        @Path("owner")                            owner  : String,
        @Path("repo")                             repo   : String,
        @Path("path", encoded = false)            path   : String,
        @Body                                     request: CreateFileRequest
    ): Response<CreateFileResponse>
}
