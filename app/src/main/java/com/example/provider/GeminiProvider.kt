package com.example.provider

import com.example.core.network.OkHttpClientFactory
import com.example.model.AICapability
import com.example.model.AIModelInfo
import com.example.model.AIProviderId
import com.example.model.AIRequest
import com.example.model.AIResponse
import com.example.model.ConnectionTestResult
import com.example.model.MessageSender
import com.example.model.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiProvider : AIProvider {
    override val id = AIProviderId.GEMINI
    override val displayName = "Google Gemini"
    override val description = "Google's multimodal AI with ultra-fast latency & 2M token context windows."
    override val defaultEndpoint = "https://generativelanguage.googleapis.com/v1beta"
    override val requiresApiKey = true
    override val documentationUrl = "https://aistudio.google.com/app/apikey"

    override val models = listOf(
        AIModelInfo(
            id = "gemini-2.0-flash",
            name = "Gemini 2.0 Flash",
            description = "Next-generation multimodal model with ultra-fast execution & top coding speed",
            contextWindow = "1M tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "gemini-1.5-pro",
            name = "Gemini 1.5 Pro",
            description = "Deep reasoning across complex codebases with up to 2 million tokens",
            contextWindow = "2M tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "gemini-1.5-flash",
            name = "Gemini 1.5 Flash",
            description = "High-throughput and lightweight for rapid development and explanations",
            contextWindow = "1M tokens",
            capabilities = listOf(
                AICapability.CODE_GENERATION,
                AICapability.CODE_EXPLANATION,
                AICapability.DEBUGGING,
                AICapability.WEB_DEV,
                AICapability.PYTHON_JAVA
            )
        )
    )

    override suspend fun testConnection(config: ProviderConfig): ConnectionTestResult = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank()) {
            return@withContext ConnectionTestResult(
                success = false,
                statusCode = 400,
                message = "Gemini API Key is required. Please obtain one at aistudio.google.com."
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val base = if (config.endpoint.isNotBlank()) config.endpoint.trimEnd('/') else defaultEndpoint
            val testUrl = "$base/models?key=$apiKey"

            val request = Request.Builder()
                .url(testUrl)
                .get()
                .build()

            val response = OkHttpClientFactory.client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                ConnectionTestResult(
                    success = true,
                    statusCode = response.code,
                    message = "Connected to Google Gemini successfully ($latency ms)",
                    latencyMs = latency
                )
            } else {
                val errorMsg = parseErrorMessage(body, response.code)
                ConnectionTestResult(
                    success = false,
                    statusCode = response.code,
                    message = errorMsg,
                    latencyMs = latency,
                    rawDetails = body
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            ConnectionTestResult(
                success = false,
                statusCode = 0,
                message = "Network error: ${e.localizedMessage ?: "Failed to reach Gemini servers"}",
                latencyMs = latency
            )
        }
    }

    override suspend fun generateResponse(request: AIRequest, config: ProviderConfig): AIResponse = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank()) {
            return@withContext AIResponse(
                text = "",
                providerId = id,
                modelId = config.selectedModelId,
                latencyMs = 0L,
                isError = true,
                errorMessage = "Gemini API Key is missing. Configure it in Settings."
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val base = if (config.endpoint.isNotBlank()) config.endpoint.trimEnd('/') else defaultEndpoint
            val url = "$base/models/${config.selectedModelId}:generateContent?key=$apiKey"

            val contents = JSONArray()

            // System instruction or file context
            var systemText = "You are an expert software developer in NAWA DevStudio IDE. Provide clean, production-grade code and insightful explanations."
            if (!request.systemPrompt.isNullOrBlank()) {
                systemText += " " + request.systemPrompt
            }
            if (!request.fileContext.isNullOrBlank()) {
                systemText += "\nProject File Context:\n```\n${request.fileContext}\n```"
            }

            // Prior messages
            request.history.takeLast(8).forEach { msg ->
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                contents.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.text) })
                    })
                })
            }

            // Current user turn
            val currentPromptText = if (request.history.isEmpty()) {
                "$systemText\n\n${request.prompt}"
            } else {
                request.prompt
            }

            contents.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", currentPromptText) })
                })
            })

            val payload = JSONObject().apply {
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", request.temperature)
                    put("maxOutputTokens", request.maxTokens)
                })
            }

            val httpRequest = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = OkHttpClientFactory.client.newCall(httpRequest).execute()
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val candidates = json.optJSONArray("candidates")
                val text = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""
                val usage = json.optJSONObject("usageMetadata")
                val promptTokens = usage?.optInt("promptTokenCount")
                val completionTokens = usage?.optInt("candidatesTokenCount")
                val totalTokens = usage?.optInt("totalTokenCount")

                AIResponse(
                    text = text,
                    providerId = id,
                    modelId = config.selectedModelId,
                    latencyMs = latency,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    totalTokens = totalTokens
                )
            } else {
                val errorMsg = parseErrorMessage(body, response.code)
                AIResponse(
                    text = "",
                    providerId = id,
                    modelId = config.selectedModelId,
                    latencyMs = latency,
                    isError = true,
                    errorMessage = errorMsg
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            AIResponse(
                text = "",
                providerId = id,
                modelId = config.selectedModelId,
                latencyMs = latency,
                isError = true,
                errorMessage = "Gemini request failed: ${e.localizedMessage ?: "Unknown network error"}"
            )
        }
    }

    private fun parseErrorMessage(responseBody: String, statusCode: Int): String {
        return try {
            val json = JSONObject(responseBody)
            val errObj = json.optJSONObject("error")
            val msg = errObj?.optString("message") ?: ""
            when (statusCode) {
                400 -> "Invalid request (400): $msg"
                403 -> "Forbidden (403): Invalid API key or API not enabled. $msg"
                429 -> "Rate limit or quota exceeded (429): $msg"
                500, 503 -> "Google Gemini server error ($statusCode): $msg"
                else -> "HTTP $statusCode: $msg"
            }
        } catch (_: Exception) {
            "HTTP $statusCode: $responseBody"
        }
    }
}
