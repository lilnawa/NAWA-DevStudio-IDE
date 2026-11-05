package com.example.provider

import com.example.model.AICapability
import com.example.model.AIModelInfo
import com.example.model.AIProviderId
import com.example.model.AIRequest
import com.example.model.AIResponse
import com.example.model.ConnectionTestResult
import com.example.model.ProviderConfig

interface AIProvider {
    val id: AIProviderId
    val displayName: String
    val description: String
    val defaultEndpoint: String
    val requiresApiKey: Boolean
    val documentationUrl: String
    val models: List<AIModelInfo>

    fun getCapabilities(modelId: String): List<AICapability> {
        return models.firstOrNull { it.id == modelId }?.capabilities ?: listOf(
            AICapability.CODE_GENERATION,
            AICapability.CODE_EXPLANATION,
            AICapability.DEBUGGING,
            AICapability.REFACTORING,
            AICapability.DOCUMENTATION
        )
    }

    suspend fun testConnection(config: ProviderConfig): ConnectionTestResult

    suspend fun generateResponse(request: AIRequest, config: ProviderConfig): AIResponse
}
