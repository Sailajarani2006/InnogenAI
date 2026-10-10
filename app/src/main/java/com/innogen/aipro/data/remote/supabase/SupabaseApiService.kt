package com.innogen.aipro.data.remote.supabase

import retrofit2.Response
import retrofit2.http.*

interface SupabaseApiService {

    @GET("rest/v1/projects")
    suspend fun getProjects(
        @Header("apikey") apiKey: String = SupabaseConfig.ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.ANON_KEY}",
        @Query("user_id") userFilter: String,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<SupabaseProjectDto>>

    @POST("rest/v1/projects")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertProject(
        @Header("apikey") apiKey: String = SupabaseConfig.ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.ANON_KEY}",
        @Body project: SupabaseProjectDto
    ): Response<Unit>

    @DELETE("rest/v1/projects")
    suspend fun deleteProject(
        @Header("apikey") apiKey: String = SupabaseConfig.ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${SupabaseConfig.ANON_KEY}",
        @Query("id") idFilter: String
    ): Response<Unit>
}
