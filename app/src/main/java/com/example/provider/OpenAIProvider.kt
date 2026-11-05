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

class OpenAIProvider : AIProvider {
    override val id = AIProviderId.OPENAI
    override val displayName = "OpenAI"
    override val description = "Creator of ChatGPT, GPT-4o, and reasoning model o1/o3-mini."
    override val defaultEndpoint = "https://api.openai.com/v1/chat/completions"
    override val requiresApiKey = true
    override val documentationUrl = "https://platform.openai.com/api-keys"

    override val models = listOf(
        AIModelInfo(
            id = "gpt-4o",
            name = "GPT-4o",
            description = "High-intelligence flagship omni model for complex coding & reasoning",
            contextWindow = "128k tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "gpt-4o-mini",
            name = "GPT-4o mini",
            description = "Affordable, fast, intelligent model for everyday coding tasks",
            contextWindow = "128k tokens",
            capabilities = listOf(
                AICapability.CODE_GENERATION,
                AICapability.CODE_EXPLANATION,
                AICapability.DEBUGGING,
                AICapability.REFACTORING,
                AICapability.WEB_DEV,
                AICapability.PYTHON_JAVA
            )
        ),
        AIModelInfo(
            id = "o1",
            name = "o1 Reasoning",
            description = "Advanced reasoning model designed for deeply complex math & algorithms",
            contextWindow = "200k tokens",
            capabilities = listOf(
                AICapability.CODE_GENERATION,
                AICapability.DEBUGGING,
                AICapability.REFACTORING,
                AICapability.FILE_ANALYSIS
            )
        ),
        AIModelInfo(
            id = "o3-mini",
            name = "o3-mini",
            description = "High-speed reasoning model specialized in STEM, coding, and architecture",
            contextWindow = "200k tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "gpt-4-turbo",
            name = "GPT-4 Turbo",
            description = "High-capability legacy flagship with vision and deep tool knowledge",
            contextWindow = "128k tokens",
            capabilities = AICapability.entries
        )
    )

    override suspend fun testConnection(config: ProviderConfig): ConnectionTestResult = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank()) {
            return@withContext ConnectionTestResult(
                success = false,
                statusCode = 400,
                message = "API Key is required. Please enter an OpenAI key."
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val endpoint = if (config.endpoint.isNotBlank()) config.endpoint else defaultEndpoint
            val testPayload = JSONObject().apply {
                put("model", config.selectedModelId)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    })
                })
                put("max_tokens", 3)
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(testPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = OkHttpClientFactory.client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                ConnectionTestResult(
                    success = true,
                    statusCode = response.code,
                    message = "Connected to OpenAI successfully ($latency ms)",
                    latencyMs = latency
                )
            } else {
                val errorMsg = parseErrorMessage(responseBody, response.code)
                ConnectionTestResult(
                    success = false,
                    statusCode = response.code,
                    message = errorMsg,
                    latencyMs = latency,
                    rawDetails = responseBody
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            ConnectionTestResult(
                success = false,
                statusCode = 0,
                message = "Network error: ${e.localizedMessage ?: "Unable to reach OpenAI endpoint"}",
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
                errorMessage = "OpenAI API Key is missing. Configure it in Settings."
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val endpoint = if (config.endpoint.isNotBlank()) config.endpoint else defaultEndpoint
            val messagesArray = JSONArray()

            // System prompt
            val sysPrompt = buildString {
                append("You are an expert software engineer and coding assistant inside NAWA DevStudio IDE. ")
                if (!request.systemPrompt.isNullOrBlank()) {
                    append(request.systemPrompt).append(" ")
                }
                if (!request.fileContext.isNullOrBlank()) {
                    append("\nCurrent project file context:\n```\n").append(request.fileContext).append("\n```\n")
                }
            }
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", sysPrompt)
            })

            // Conversation history
            request.history.takeLast(10).forEach { msg ->
                messagesArray.put(JSONObject().apply {
                    put("role", if (msg.sender == MessageSender.USER) "user" else "assistant")
                    put("content", msg.text)
                })
            }

            // Current prompt
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", request.prompt)
            })

            val payload = JSONObject().apply {
                put("model", config.selectedModelId)
                put("messages", messagesArray)
                if (!config.selectedModelId.startsWith("o1") && !config.selectedModelId.startsWith("o3")) {
                    put("temperature", request.temperature)
                    put("max_tokens", request.maxTokens)
                }
            }

            val httpRequest = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = OkHttpClientFactory.client.newCall(httpRequest).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val choices = json.optJSONArray("choices")
                val answer = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                val usage = json.optJSONObject("usage")
                val promptTokens = usage?.optInt("prompt_tokens")
                val completionTokens = usage?.optInt("completion_tokens")
                val totalTokens = usage?.optInt("total_tokens")

                AIResponse(
                    text = answer,
                    providerId = id,
                    modelId = config.selectedModelId,
                    latencyMs = latency,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    totalTokens = totalTokens
                )
            } else {
                val errorMsg = parseErrorMessage(responseBody, response.code)
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
                errorMessage = "Connection error: ${e.localizedMessage ?: "Request failed"}"
            )
        }
    }

    private fun parseErrorMessage(responseBody: String, statusCode: Int): String {
        return try {
            val json = JSONObject(responseBody)
            val errObj = json.optJSONObject("error")
            val msg = errObj?.optString("message") ?: ""
            when (statusCode) {
                401 -> "Authentication failed (401): Invalid OpenAI API key. $msg"
                429 -> "Rate limit reached or quota exceeded (429): $msg"
                404 -> "Model or endpoint not found (404): $msg"
                500, 503 -> "OpenAI service error ($statusCode): $msg"
                else -> "HTTP $statusCode: $msg"
            }
        } catch (_: Exception) {
            "HTTP $statusCode: $responseBody"
        }
    }
}
