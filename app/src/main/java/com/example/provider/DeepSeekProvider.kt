package com.example.provider

import com.example.model.AICapability
import com.example.model.AIModelInfo
import com.example.model.AIProviderId

class DeepSeekProvider : OpenAiCompatibleProvider() {
    override val id = AIProviderId.DEEPSEEK
    override val displayName = "DeepSeek"
    override val description = "DeepSeek AI models including V3 and R1 chain-of-thought reasoning."
    override val defaultEndpoint = "https://api.deepseek.com/chat/completions"
    override val requiresApiKey = true
    override val documentationUrl = "https://platform.deepseek.com/api_keys"

    override val models = listOf(
        AIModelInfo(
            id = "deepseek-chat",
            name = "DeepSeek V3",
            description = "General coding, multi-language synthesis, refactoring, and agentic workflows",
            contextWindow = "64k tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "deepseek-reasoner",
            name = "DeepSeek R1",
            description = "Reinforcement-learning driven chain-of-thought for deep algorithmic problem solving",
            contextWindow = "64k tokens",
            capabilities = listOf(
                AICapability.CODE_GENERATION,
                AICapability.DEBUGGING,
                AICapability.REFACTORING,
                AICapability.FILE_ANALYSIS
            )
        )
    )
}
