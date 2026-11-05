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

class OllamaProvider : AIProvider {
    override val id = AIProviderId.OLLAMA
    override val displayName = "Ollama (Local AI)"
    override val description = "Run local open-source models completely on-device or via local LAN / workstation without API keys."
    override val defaultEndpoint = "http://10.0.2.2:11434"
    override val requiresApiKey = false
    override val documentationUrl = "https://ollama.com"

    override val models = listOf(
        AIModelInfo(
            id = "deepseek-coder:6.7b",
            name = "DeepSeek Coder (6.7B)",
            description = "High performance local coding model specialized in multi-language programming",
            contextWindow = "16k tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "qwen2.5-coder",
            name = "Qwen 2.5 Coder",
            description = "State-of-the-art open coding model for code repair, test generation and documentation",
            contextWindow = "32k tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "llama3.2",
            name = "Llama 3.2",
            description = "Fast, efficient general local assistant and code analyzer",
            contextWindow = "128k tokens",
            capabilities = listOf(
                AICapability.CODE_GENERATION,
                AICapability.CODE_EXPLANATION,
                AICapability.DEBUGGING,
                AICapability.DOCUMENTATION
            )
        ),
        AIModelInfo(
            id = "codellama",
            name = "Code Llama",
            description = "Meta's specialized code LLM for Python, Java, Kotlin and C++",
            contextWindow = "16k tokens",
            capabilities = listOf(
                AICapability.CODE_GENERATION,
                AICapability.CODE_EXPLANATION,
                AICapability.DEBUGGING,
                AICapability.PYTHON_JAVA
            )
        ),
        AIModelInfo(
            id = "mistral",
            name = "Mistral 7B Local",
            description = "Solid local general purpose foundation model with code fluency",
            contextWindow = "8k tokens",
            capabilities = listOf(
                AICapability.CODE_GENERATION,
                AICapability.CODE_EXPLANATION,
                AICapability.DEBUGGING
            )
        ),
        AIModelInfo(
            id = "phi3",
            name = "Microsoft Phi-3",
            description = "Ultra-compact local model suitable for edge reasoning and quick refactors",
            contextWindow = "4k tokens",
            capabilities = listOf(
                AICapability.CODE_GENERATION,
                AICapability.CODE_EXPLANATION
            )
        )
    )

    override suspend fun testConnection(config: ProviderConfig): ConnectionTestResult = withContext(Dispatchers.IO) {
        val endpoint = (if (config.endpoint.isNotBlank()) config.endpoint else defaultEndpoint).trimEnd('/')
        val startTime = System.currentTimeMillis()

        try {
            val tagsUrl = "$endpoint/api/tags"
            val request = Request.Builder()
                .url(tagsUrl)
                .get()
                .build()

            val response = OkHttpClientFactory.client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                // Parse installed models if any
                val installedModels = mutableListOf<String>()
                try {
                    val json = JSONObject(body)
                    val modelsArr = json.optJSONArray("models")
                    if (modelsArr != null) {
                        for (i in 0 until modelsArr.length()) {
                            val m = modelsArr.getJSONObject(i).optString("name")
                            if (m.isNotBlank()) installedModels.add(m)
                        }
                    }
                } catch (_: Exception) {}

                val detailMsg = if (installedModels.isNotEmpty()) {
                    "Connected ($latency ms). ${installedModels.size} local models found: ${installedModels.take(3).joinToString()}"
                } else {
                    "Connected ($latency ms). Ollama daemon is running."
                }

                ConnectionTestResult(
                    success = true,
                    statusCode = response.code,
                    message = detailMsg,
                    latencyMs = latency,
                    rawDetails = body
                )
            } else {
                ConnectionTestResult(
                    success = false,
                    statusCode = response.code,
                    message = "Ollama returned HTTP ${response.code}: $body",
                    latencyMs = latency,
                    rawDetails = body
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            ConnectionTestResult(
                success = false,
                statusCode = 0,
                message = "Could not reach Ollama at $endpoint. Ensure Ollama is running (`ollama serve`) and accessible from device/emulator (use 10.0.2.2 or your machine IP).",
                latencyMs = latency
            )
        }
    }

    override suspend fun generateResponse(request: AIRequest, config: ProviderConfig): AIResponse = withContext(Dispatchers.IO) {
        val endpoint = (if (config.endpoint.isNotBlank()) config.endpoint else defaultEndpoint).trimEnd('/')
        val startTime = System.currentTimeMillis()

        try {
            val chatUrl = "$endpoint/api/chat"
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
                put("stream", false)
            }

            val httpRequest = Request.Builder()
                .url(chatUrl)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = OkHttpClientFactory.client.newCall(httpRequest).execute()
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val messageObj = json.optJSONObject("message")
                val text = messageObj?.optString("content") ?: ""
                val promptTokens = json.optInt("prompt_eval_count", 0)
                val completionTokens = json.optInt("eval_count", 0)

                AIResponse(
                    text = text,
                    providerId = id,
                    modelId = config.selectedModelId,
                    latencyMs = latency,
                    promptTokens = if (promptTokens > 0) promptTokens else null,
                    completionTokens = if (completionTokens > 0) completionTokens else null,
                    totalTokens = if (promptTokens + completionTokens > 0) promptTokens + completionTokens else null
                )
            } else {
                AIResponse(
                    text = "",
                    providerId = id,
                    modelId = config.selectedModelId,
                    latencyMs = latency,
                    isError = true,
                    errorMessage = "Ollama HTTP ${response.code}: $body"
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
                errorMessage = "Ollama connection failed: ${e.localizedMessage ?: "Daemon not responding at $endpoint"}"
            )
        }
    }
}
