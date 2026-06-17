package com.innogen.aipro.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

// ── GitHub API models ─────────────────────────────────────────────────────────

data class GitHubRepo(
    val id          : Long,
    val name        : String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("html_url") val htmlUrl  : String,
    val private     : Boolean,
    val description : String?
)

data class CreateRepoRequest(
    val name       : String,
    val description: String,
    val private    : Boolean = false,
    @SerializedName("auto_init") val autoInit: Boolean = true
)

data class CreateFileRequest(
    val message: String,
    val content: String,  // base64-encoded content
    val sha    : String?  = null  // required for updates
)

data class CreateFileResponse(
    val content: FileContent?,
    val commit : CommitInfo?
)

data class FileContent(
    val name    : String,
    @SerializedName("html_url") val htmlUrl: String
)

data class CommitInfo(val sha: String)

data class GitHubUser(
    val login     : String,
    val name      : String?,
    @SerializedName("avatar_url") val avatarUrl: String,
    @SerializedName("public_repos") val publicRepos: Int
)

// ── Retrofit Service ──────────────────────────────────────────────────────────

interface GitHubService {

    @GET("user")
    suspend fun getUser(
        @Header("Authorization") token: String
    ): Response<GitHubUser>

    @GET("user/repos")
    suspend fun getUserRepos(
        @Header("Authorization") token: String,
        @Query("per_page") perPage: Int = 30,
        @Query("sort") sort: String = "updated"
    ): Response<List<GitHubRepo>>

    @POST("user/repos")
    suspend fun createRepo(
        @Header("Authorization") token: String,
        @Body request: CreateRepoRequest
    ): Response<GitHubRepo>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Header("Authorization") token: String,
        @Path("owner") owner : String,
        @Path("repo")  repo  : String,
        @Path("path", encoded = false) path: String,
        @Body request: CreateFileRequest
    ): Response<CreateFileResponse>
}
