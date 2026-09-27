package com.example.calorietracker.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Groq exposes a free-tier, OpenAI-compatible chat completions API
 * (https://console.groq.com). The user supplies their own free API key in
 * Settings; we send it as a Bearer token per-request.
 */
data class ChatMessage(val role: String, val content: String)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.2
)

data class ChatChoice(val message: ChatMessage)
data class ChatCompletionResponse(val choices: List<ChatChoice>)

data class GroqModel(val id: String)
data class GroqModelsResponse(val data: List<GroqModel>)

interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") bearerToken: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    @GET("openai/v1/models")
    suspend fun listModels(@Header("Authorization") bearerToken: String): GroqModelsResponse
}

/** Parsed nutrition estimate for one free-text food description. */
data class AiNutritionEstimate(
    val name: String,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double
)
