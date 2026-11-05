package com.example.provider

import com.example.model.AICapability
import com.example.model.AIModelInfo
import com.example.model.AIProviderId

class MistralProvider : OpenAiCompatibleProvider() {
    override val id = AIProviderId.MISTRAL
    override val displayName = "Mistral AI"
    override val description = "Frontier European AI with specialized Codestral coding engine."
    override val defaultEndpoint = "https://api.mistral.ai/v1/chat/completions"
    override val requiresApiKey = true
    override val documentationUrl = "https://console.mistral.ai/api-keys"

    override val models = listOf(
        AIModelInfo(
            id = "codestral-latest",
            name = "Codestral",
            description = "Specialized generative AI model explicitly trained for code completion and synthesis",
            contextWindow = "32k tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "mistral-large-latest",
            name = "Mistral Large 2",
            description = "Flagship reasoning, multilingual fluency, and high-level software architecture",
            contextWindow = "128k tokens",
            capabilities = AICapability.entries
        ),
        AIModelInfo(
            id = "mistral-small-latest",
            name = "Mistral Small",
            description = "Cost-effective, responsive model for rapid debugging and script generation",
            contextWindow = "128k tokens",
            capabilities = listOf(
                AICapability.CODE_GENERATION,
                AICapability.CODE_EXPLANATION,
                AICapability.DEBUGGING
            )
        )
    )
}
