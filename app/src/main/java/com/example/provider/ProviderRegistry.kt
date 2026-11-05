package com.example.provider

import com.example.model.AIProviderId

object ProviderRegistry {
    private val openAI = OpenAIProvider()
    private val gemini = GeminiProvider()
    private val claude = ClaudeProvider()
    private val deepSeek = DeepSeekProvider()
    private val mistral = MistralProvider()
    private val groq = GroqProvider()
    private val openRouter = OpenRouterProvider()
    private val ollama = OllamaProvider()

    val allProviders: List<AIProvider> = listOf(
        gemini,
        openAI,
        claude,
        deepSeek,
        mistral,
        groq,
        openRouter,
        ollama
    )

    fun getProvider(id: AIProviderId): AIProvider {
        return allProviders.firstOrNull { it.id == id } ?: gemini
    }

    fun getDefaultModel(id: AIProviderId): String {
        return getProvider(id).models.first().id
    }
}
