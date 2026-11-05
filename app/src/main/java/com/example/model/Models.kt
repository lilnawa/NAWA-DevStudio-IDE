package com.example.model

enum class AIProviderId(val displayName: String, val shortName: String, val defaultEndpoint: String, val requiresKey: Boolean) {
    OPENAI("OpenAI", "ChatGPT", "https://api.openai.com/v1/chat/completions", true),
    GEMINI("Google Gemini", "Gemini", "https://generativelanguage.googleapis.com/v1beta", true),
    CLAUDE("Anthropic Claude", "Claude", "https://api.anthropic.com/v1/messages", true),
    DEEPSEEK("DeepSeek", "DeepSeek", "https://api.deepseek.com/chat/completions", true),
    MISTRAL("Mistral AI", "Mistral", "https://api.mistral.ai/v1/chat/completions", true),
    GROQ("Groq Cloud", "Groq", "https://api.groq.com/openai/v1/chat/completions", true),
    OPENROUTER("OpenRouter", "OpenRouter", "https://openrouter.ai/api/v1/chat/completions", true),
    OLLAMA("Ollama (Local)", "Ollama", "http://10.0.2.2:11434", false);

    companion object {
        fun fromId(id: String): AIProviderId = entries.firstOrNull { it.name.equals(id, ignoreCase = true) } ?: OPENAI
    }
}

data class AIModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val contextWindow: String,
    val capabilities: List<AICapability> = listOf(
        AICapability.CODE_GENERATION,
        AICapability.CODE_EXPLANATION,
        AICapability.DEBUGGING,
        AICapability.REFACTORING
    )
)

enum class AICapability(val title: String) {
    CODE_GENERATION("Code Generation"),
    CODE_EXPLANATION("Code Explanation"),
    DEBUGGING("Bug Fixing & Debugging"),
    REFACTORING("Code Refactoring"),
    WEB_DEV("HTML/CSS/JS Development"),
    KOTLIN_ANDROID("Kotlin & Android Dev"),
    PYTHON_JAVA("Python & Java Dev"),
    DOCUMENTATION("Documentation & README"),
    FILE_ANALYSIS("File Context Analysis")
}

data class ProviderConfig(
    val providerId: AIProviderId,
    val apiKey: String = "",
    val endpoint: String = providerId.defaultEndpoint,
    val selectedModelId: String,
    val isEnabled: Boolean = true,
    val lastTestedSuccess: Boolean? = null,
    val lastTestedTimestamp: Long = 0L,
    val lastLatencyMs: Long = 0L
)

data class ConnectionTestResult(
    val success: Boolean,
    val statusCode: Int = 0,
    val message: String,
    val latencyMs: Long = 0L,
    val rawDetails: String = ""
)

data class AIRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val history: List<ChatMessage> = emptyList(),
    val fileContext: String? = null,
    val maxTokens: Int = 2048,
    val temperature: Double = 0.7
)

data class AIResponse(
    val text: String,
    val providerId: AIProviderId,
    val modelId: String,
    val latencyMs: Long,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val providerId: AIProviderId? = null,
    val modelId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val attachedFileName: String? = null,
    val latencyMs: Long? = null,
    val isError: Boolean = false
)

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

data class Conversation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val activeProviderId: AIProviderId = AIProviderId.GEMINI,
    val activeModelId: String = "gemini-2.0-flash"
)

data class CompareSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val prompt: String,
    val targetProviders: List<AIProviderId>,
    val results: Map<AIProviderId, AIResponse> = emptyMap(),
    val inProgress: Map<AIProviderId, Boolean> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

data class ApiLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val providerId: AIProviderId,
    val modelId: String,
    val endpoint: String,
    val statusCode: Int,
    val latencyMs: Long,
    val success: Boolean,
    val promptSnippet: String,
    val errorMessage: String? = null
)

data class ProjectFile(
    val name: String,
    val extension: String,
    val path: String,
    var content: String,
    val isReadOnly: Boolean = false
)
