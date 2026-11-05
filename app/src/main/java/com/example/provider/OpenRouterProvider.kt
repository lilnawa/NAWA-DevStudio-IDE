package com.example.provider

import com.example.model.AICapability
import com.example.model.AIModelInfo
import com.example.model.AIProviderId

class OpenRouterProvider : OpenAiCompatibleProvider() {
    override val id = AIProviderId.OPENROUTER
    override val displayName = "OpenRouter"
    override val description = "Unified gateway routing to 100+ AI models across multiple labs with single API key."
    override val defaultEndpoint = "https://openrouter.ai/api/v1/chat/completions"
    override val requiresApiKey = true
    override val documentationUrl = "https://openrouter.ai/keys"

    override val extraHeaders: Map<String, String> = mapOf(
        "HTTP-Referer" to "https://nawadevstudio.app",
        "X-Title" to "NAWA DevStudio"
    )

    override val models = listOf(
        AIModelInfo(
            id = "openrouter/auto",
            name = "Auto Router",
            description = "Intelligently routes requests to the best price-to-performance provider",
            contextWindow = "128k tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "anthropic/claude-3.5-sonnet",
            name = "Claude 3.5 Sonnet (via OpenRouter)",
            description = "Anthropic's flagship coding model accessible via your OpenRouter credits",
            contextWindow = "200k tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "openai/gpt-4o",
            name = "GPT-4o (via OpenRouter)",
            description = "OpenAI's multimodal flagship served through unified OpenRouter infrastructure",
            contextWindow = "128k tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "deepseek/deepseek-r1",
            name = "DeepSeek R1 (via OpenRouter)",
            description = "Frontier open-weights reasoning model with complete thinking traces",
            contextWindow = "64k tokens",
            capabilities = listOf(
                AICapability.CODE_GENERATION,
                AICapability.DEBUGGING,
                AICapability.REFACTORING,
                AICapability.FILE_ANALYSIS
            )
        ),
        AIModelInfo(
            id = "google/gemini-2.0-flash-exp:free",
            name = "Gemini 2.0 Flash (Free Tier)",
            description = "Free experimental endpoint provided on OpenRouter for quick testing",
            contextWindow = "1M tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "meta-llama/llama-3.3-70b-instruct",
            name = "Llama 3.3 70B Instruct",
            description = "High-performing Meta open model hosted across low-latency clusters",
            contextWindow = "128k tokens",
            capabilities = AICapability.entries
        )
    )
}
