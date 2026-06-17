package com.innogen.aipro.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

// ── OpenAI Request/Response models ───────────────────────────────────────────

data class OpenAIRequest(
    val model       : String                = "gpt-4o",
    val messages    : List<ChatMessage>,
    @SerializedName("max_tokens")
    val maxTokens   : Int                   = 4000,
    val temperature : Double                = 0.7,
    val stream      : Boolean               = false
)

data class ChatMessage(
    val role   : String,  // "system" | "user" | "assistant"
    val content: String
)

data class OpenAIResponse(
    val id      : String,
    val `object`: String,
    val created : Long,
    val model   : String,
    val choices : List<Choice>,
    val usage   : Usage?
)

data class Choice(
    val index        : Int,
    val message      : ChatMessage,
    @SerializedName("finish_reason")
    val finishReason : String
)

data class Usage(
    @SerializedName("prompt_tokens")     val promptTokens    : Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens")      val totalTokens     : Int
)

// ── Retrofit Service ──────────────────────────────────────────────────────────

interface OpenAIService {
    @POST("v1/chat/completions")
    suspend fun generateCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIRequest
    ): Response<OpenAIResponse>
}
