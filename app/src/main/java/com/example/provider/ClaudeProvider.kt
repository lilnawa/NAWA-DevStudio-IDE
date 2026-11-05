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

class ClaudeProvider : AIProvider {
    override val id = AIProviderId.CLAUDE
    override val displayName = "Anthropic Claude"
    override val description = "Industry-standard coding intelligence, nuanced reasoning, and precision refactoring."
    override val defaultEndpoint = "https://api.anthropic.com/v1/messages"
    override val requiresApiKey = true
    override val documentationUrl = "https://console.anthropic.com/settings/keys"

    override val models = listOf(
        AIModelInfo(
            id = "claude-3-7-sonnet-20250219",
            name = "Claude 3.7 Sonnet",
            description = "State-of-the-art hybrid reasoning & software engineering model",
            contextWindow = "200k tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "claude-3-5-sonnet-20241022",
            name = "Claude 3.5 Sonnet",
            description = "Unrivaled software engineering, code generation, and debugging accuracy",
            contextWindow = "200k tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "claude-3-5-haiku-20241022",
            name = "Claude 3.5 Haiku",
            description = "Lightning-fast responses matching prior flagship reasoning levels",
            contextWindow = "200k tokens",
            capabilities = listOf(
                AICapability.CODE_GENERATION,
                AICapability.CODE_EXPLANATION,
                AICapability.DEBUGGING,
                AICapability.WEB_DEV,
                AICapability.DOCUMENTATION
            )
        ),
        AIModelInfo(
            id = "claude-3-opus-20240229",
            name = "Claude 3 Opus",
            description = "Deepest contextual analysis for large multi-file architectures",
            contextWindow = "200k tokens",
            capabilities = AICapability.entries
        )
    )

    override suspend fun testConnection(config: ProviderConfig): ConnectionTestResult = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank()) {
            return@withContext ConnectionTestResult(
                success = false,
                statusCode = 400,
                message = "Anthropic API Key is required."
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val endpoint = if (config.endpoint.isNotBlank()) config.endpoint else defaultEndpoint
            val payload = JSONObject().apply {
                put("model", config.selectedModelId)
                put("max_tokens", 2)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    })
                })
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = OkHttpClientFactory.client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                ConnectionTestResult(
                    success = true,
                    statusCode = response.code,
                    message = "Connected to Anthropic Claude successfully ($latency ms)",
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
                message = "Network error connecting to Claude: ${e.localizedMessage ?: "Unknown error"}",
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
                errorMessage = "Anthropic API Key is missing. Configure it in Settings."
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val endpoint = if (config.endpoint.isNotBlank()) config.endpoint else defaultEndpoint

            val sysPrompt = buildString {
                append("You are an expert software developer and technical assistant inside NAWA DevStudio IDE. ")
                if (!request.systemPrompt.isNullOrBlank()) {
                    append(request.systemPrompt).append(" ")
                }
                if (!request.fileContext.isNullOrBlank()) {
                    append("\nCurrent project file context:\n```\n").append(request.fileContext).append("\n```\n")
                }
            }

            val messagesArray = JSONArray()
            request.history.takeLast(10).forEach { msg ->
                messagesArray.put(JSONObject().apply {
                    put("role", if (msg.sender == MessageSender.USER) "user" else "assistant")
                    put("content", msg.text)
                })
            }
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", request.prompt)
            })

            val payload = JSONObject().apply {
                put("model", config.selectedModelId)
                put("system", sysPrompt)
                put("max_tokens", request.maxTokens)
                put("messages", messagesArray)
            }

            val httpRequest = Request.Builder()
                .url(endpoint)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = OkHttpClientFactory.client.newCall(httpRequest).execute()
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val contentArray = json.optJSONArray("content")
                val text = contentArray?.optJSONObject(0)?.optString("text") ?: ""
                val usage = json.optJSONObject("usage")
                val promptTokens = usage?.optInt("input_tokens")
                val completionTokens = usage?.optInt("output_tokens")
                val totalTokens = if (promptTokens != null && completionTokens != null) promptTokens + completionTokens else null

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
                errorMessage = "Anthropic Claude request failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun parseErrorMessage(responseBody: String, statusCode: Int): String {
        return try {
            val json = JSONObject(responseBody)
            val errObj = json.optJSONObject("error")
            val msg = errObj?.optString("message") ?: ""
            when (statusCode) {
                401 -> "Authentication failed (401): Invalid Anthropic API key. $msg"
                403 -> "Forbidden (403): Account suspended or permission denied. $msg"
                429 -> "Rate limit reached (429): $msg"
                529 -> "Anthropic servers overloaded (529): Please retry in a few moments."
                500, 503 -> "Anthropic service error ($statusCode): $msg"
                else -> "HTTP $statusCode: $msg"
            }
        } catch (_: Exception) {
            "HTTP $statusCode: $responseBody"
        }
    }
}
