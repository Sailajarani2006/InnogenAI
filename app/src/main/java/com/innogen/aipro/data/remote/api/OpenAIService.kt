package com.innogen.aipro.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class GeminiRequest(
    val model                  : String              = "openai/gpt-oss-120b",
    val messages               : List<GeminiContent>,
    @SerializedName("max_tokens")
    val maxTokens              : Int                 = 8192,
    @SerializedName("max_completion_tokens")
    val maxCompletionTokens    : Int                 = 8192,
    val temperature            : Double              = 0.7
)

data class GeminiContent(
    val role   : String,
    val content: String
)

data class GeminiPart(val text: String)

data class GeminiConfig(
    val temperature    : Double = 0.7,
    val maxOutputTokens: Int    = 4096
)

data class GeminiResponse(
    val choices: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val message: GeminiMessage?
)

data class GeminiMessage(
    val content: String?
)

interface OpenAIService {
    @POST("openai/v1/chat/completions")
    suspend fun generateCompletion(
        @Header("Authorization") apiKey  : String,
        @Body                    request : GeminiRequest
    ): Response<GeminiResponse>
}