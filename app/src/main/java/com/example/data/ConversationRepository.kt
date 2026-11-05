package com.example.data

import com.example.model.AIProviderId
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.MessageSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConversationRepository {
    private val defaultConversation = Conversation(
        title = "AI Coding Session",
        messages = listOf(
            ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "Welcome to NAWA DevStudio Multi-AI System! You have access to 8 frontier AI providers (OpenAI, Gemini, Claude, DeepSeek, Mistral, Groq, OpenRouter, and Ollama). Ask any programming question, analyze your project files, or switch providers anytime at the top bar.",
                providerId = AIProviderId.GEMINI,
                modelId = "gemini-2.0-flash"
            )
        )
    )

    private val _conversations = MutableStateFlow<List<Conversation>>(listOf(defaultConversation))
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _activeConversationId = MutableStateFlow(defaultConversation.id)
    val activeConversationId: StateFlow<String> = _activeConversationId.asStateFlow()

    fun getActiveConversation(): Conversation {
        return _conversations.value.firstOrNull { it.id == _activeConversationId.value }
            ?: _conversations.value.first()
    }

    fun addMessage(message: ChatMessage) {
        val currentId = _activeConversationId.value
        val list = _conversations.value.map { conv ->
            if (conv.id == currentId) {
                val newMessages = conv.messages + message
                val newTitle = if (conv.messages.size <= 1 && message.sender == MessageSender.USER) {
                    message.text.take(30).trim() + "..."
                } else {
                    conv.title
                }
                conv.copy(
                    messages = newMessages,
                    title = newTitle,
                    lastModifiedAt = System.currentTimeMillis()
                )
            } else {
                conv
            }
        }
        _conversations.value = list
    }

    fun updateActiveProviderAndModel(providerId: AIProviderId, modelId: String) {
        val currentId = _activeConversationId.value
        val list = _conversations.value.map { conv ->
            if (conv.id == currentId) {
                conv.copy(activeProviderId = providerId, activeModelId = modelId)
            } else {
                conv
            }
        }
        _conversations.value = list
    }

    fun removeLastAssistantMessage(): ChatMessage? {
        val currentId = _activeConversationId.value
        var removed: ChatMessage? = null
        val list = _conversations.value.map { conv ->
            if (conv.id == currentId && conv.messages.isNotEmpty()) {
                val last = conv.messages.last()
                if (last.sender == MessageSender.ASSISTANT) {
                    removed = last
                    conv.copy(messages = conv.messages.dropLast(1))
                } else {
                    conv
                }
            } else {
                conv
            }
        }
        _conversations.value = list
        return removed
    }

    fun createNewConversation(title: String = "New Coding Session", providerId: AIProviderId, modelId: String): String {
        val newConv = Conversation(
            title = title,
            activeProviderId = providerId,
            activeModelId = modelId,
            messages = listOf(
                ChatMessage(
                    sender = MessageSender.ASSISTANT,
                    text = "New session started with ${providerId.displayName} ($modelId). What are we building today?",
                    providerId = providerId,
                    modelId = modelId
                )
            )
        )
        _conversations.value = listOf(newConv) + _conversations.value
        _activeConversationId.value = newConv.id
        return newConv.id
    }

    fun setActiveConversation(id: String) {
        if (_conversations.value.any { it.id == id }) {
            _activeConversationId.value = id
        }
    }

    fun clearConversation(id: String) {
        val list = _conversations.value.map { conv ->
            if (conv.id == id) {
                conv.copy(messages = emptyList())
            } else {
                conv
            }
        }
        _conversations.value = list
    }
}
