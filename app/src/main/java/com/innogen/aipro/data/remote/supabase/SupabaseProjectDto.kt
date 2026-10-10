package com.innogen.aipro.data.remote.supabase

import com.google.gson.annotations.SerializedName

data class SupabaseProjectDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("user_email") val userEmail: String? = null,
    @SerializedName("title") val title: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("prompt") val prompt: String? = null,
    @SerializedName("status") val status: String = "COMPLETE",
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long,
    @SerializedName("features") val features: List<String> = emptyList(),
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("generated_code") val generatedCode: Map<String, Any?>? = null,
    @SerializedName("tech_stack") val techStack: Map<String, Any?>? = null,
    @SerializedName("github_repo") val githubRepo: String? = "",
    @SerializedName("has_code") val hasCode: Boolean = true
)
