package com.example.provider

import com.example.model.AICapability
import com.example.model.AIModelInfo
import com.example.model.AIProviderId

class GroqProvider : OpenAiCompatibleProvider() {
    override val id = AIProviderId.GROQ
    override val displayName = "Groq"
    override val description = "Ultra-fast LPU inference engine powering open-weights models at 500+ tokens/sec."
    override val defaultEndpoint = "https://api.groq.com/openai/v1/chat/completions"
    override val requiresApiKey = true
    override val documentationUrl = "https://console.groq.com/keys"

    override val models = listOf(
        AIModelInfo(
            id = "llama-3.3-70b-versatile",
            name = "Llama 3.3 70B Versatile",
            description = "State-of-the-art open weights with remarkable software engineering speed",
            contextWindow = "128k tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "deepseek-r1-distill-llama-70b",
            name = "DeepSeek R1 Distill (70B)",
            description = "High-speed reasoning and deep step-by-step logic powered by Groq LPU",
            contextWindow = "128k tokens",
            capabilities = listOf(
                AICapability.CODE_GENERATION,
                AICapability.DEBUGGING,
                AICapability.REFACTORING,
                AICapability.FILE_ANALYSIS
            )
        ),
        AIModelInfo(
            id = "llama-3.1-8b-instant",
            name = "Llama 3.1 8B Instant",
            description = "Sub-100ms ultra-low latency model for instant code snippets and fixes",
            contextWindow = "128k tokens",
            capabilities = listOf(
                AICapability.CODE_GENERATION,
                AICapability.CODE_EXPLANATION,
                AICapability.DEBUGGING
            )
        ),
        AIModelInfo(
            id = "mixtral-8x7b-32768",
            name = "Mixtral 8x7B MoE",
            description = "High performance sparse Mixture-of-Experts architecture",
            contextWindow = "32k tokens",
            capabilities = AICapability.entries
        )
    )
}
