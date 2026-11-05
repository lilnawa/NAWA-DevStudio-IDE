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

abstract class OpenAiCompatibleProvider : AIProvider {
    open val extraHeaders: Map<String, String> = emptyMap()

    override suspend fun testConnection(config: ProviderConfig): ConnectionTestResult = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        if (requiresApiKey && apiKey.isBlank()) {
            return@withContext ConnectionTestResult(
                success = false,
                statusCode = 400,
                message = "$displayName API Key is required."
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

            val requestBuilder = Request.Builder()
                .url(endpoint)
                .addHeader("Content-Type", "application/json")

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
            extraHeaders.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val request = requestBuilder
                .post(testPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = OkHttpClientFactory.client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                ConnectionTestResult(
                    success = true,
                    statusCode = response.code,
                    message = "Connected to $displayName successfully ($latency ms)",
                    latencyMs = latency
                )
            } else {
                val errorMsg = parseError(body, response.code)
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
                message = "Network error: ${e.localizedMessage ?: "Failed to connect to $displayName"}",
                latencyMs = latency
            )
        }
    }

    override suspend fun generateResponse(request: AIRequest, config: ProviderConfig): AIResponse = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        if (requiresApiKey && apiKey.isBlank()) {
            return@withContext AIResponse(
                text = "",
                providerId = id,
                modelId = config.selectedModelId,
                latencyMs = 0L,
                isError = true,
                errorMessage = "$displayName API Key is missing. Configure it in Settings."
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val endpoint = if (config.endpoint.isNotBlank()) config.endpoint else defaultEndpoint
            val messagesArray = JSONArray()

            val sysPrompt = buildString {
                append("You are an expert developer inside NAWA DevStudio IDE. ")
                if (!request.systemPrompt.isNullOrBlank()) {
                    append(request.systemPrompt).append(" ")
                }
                if (!request.fileContext.isNullOrBlank()) {
                    append("\nProject File Context:\n```\n").append(request.fileContext).append("\n```\n")
                }
            }
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", sysPrompt)
            })

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
                put("messages", messagesArray)
                put("max_tokens", request.maxTokens)
                put("temperature", request.temperature)
            }

            val requestBuilder = Request.Builder()
                .url(endpoint)
                .addHeader("Content-Type", "application/json")

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
            extraHeaders.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val httpRequest = requestBuilder
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = OkHttpClientFactory.client.newCall(httpRequest).execute()
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val choices = json.optJSONArray("choices")
                val text = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
                val usage = json.optJSONObject("usage")
                val promptTokens = usage?.optInt("prompt_tokens")
                val completionTokens = usage?.optInt("completion_tokens")
                val totalTokens = usage?.optInt("total_tokens")

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
                val errorMsg = parseError(body, response.code)
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
                errorMessage = "$displayName error: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    protected open fun parseError(body: String, code: Int): String {
        return try {
            val json = JSONObject(body)
            val errObj = json.optJSONObject("error")
            val msg = errObj?.optString("message") ?: json.optString("message", "")
            when (code) {
                401 -> "Authentication failed (401): Invalid $displayName API key. $msg"
                429 -> "Rate limit reached or credits exhausted (429): $msg"
                404 -> "Model not found (404): $msg"
                500, 502, 503 -> "$displayName server error ($code): $msg"
                else -> "HTTP $code: $msg"
            }
        } catch (_: Exception) {
            "HTTP $code: $body"
        }
    }
}
