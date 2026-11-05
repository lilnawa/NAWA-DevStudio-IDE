package com.example

import com.example.data.ConversationRepository
import com.example.data.WorkspaceRepository
import com.example.model.AICapability
import com.example.model.AIProviderId
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.provider.ProviderRegistry
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `all 8 providers are registered and have models and documentation`() {
        val providers = ProviderRegistry.allProviders
        assertEquals(8, providers.size)

        val expectedIds = setOf(
            AIProviderId.OPENAI,
            AIProviderId.GEMINI,
            AIProviderId.CLAUDE,
            AIProviderId.DEEPSEEK,
            AIProviderId.MISTRAL,
            AIProviderId.GROQ,
            AIProviderId.OPENROUTER,
            AIProviderId.OLLAMA
        )

        val registeredIds = providers.map { it.id }.toSet()
        assertEquals(expectedIds, registeredIds)

        providers.forEach { provider ->
            assertTrue("Provider ${provider.id} should have models", provider.models.isNotEmpty())
            assertTrue("Provider ${provider.id} default endpoint should not be blank", provider.defaultEndpoint.isNotBlank())
            assertTrue("Provider ${provider.id} docs url should start with http", provider.documentationUrl.startsWith("http"))
        }
    }

    @Test
    fun `ollama does not require api key while openai does`() {
        val ollama = ProviderRegistry.getProvider(AIProviderId.OLLAMA)
        val openai = ProviderRegistry.getProvider(AIProviderId.OPENAI)

        assertFalse(ollama.requiresApiKey)
        assertTrue(openai.requiresApiKey)
    }

    @Test
    fun `conversation repository retains messages across provider switches`() {
        val repo = ConversationRepository()
        val initialConv = repo.getActiveConversation()
        assertEquals(1, initialConv.messages.size)

        repo.addMessage(
            ChatMessage(
                sender = MessageSender.USER,
                text = "Write a debounce function in Kotlin"
            )
        )

        assertEquals(2, repo.getActiveConversation().messages.size)

        // Switch to Claude
        repo.updateActiveProviderAndModel(AIProviderId.CLAUDE, "claude-3-5-sonnet-20241022")
        val switchedConv = repo.getActiveConversation()

        // Conversation history MUST be retained
        assertEquals(2, switchedConv.messages.size)
        assertEquals(AIProviderId.CLAUDE, switchedConv.activeProviderId)
        assertEquals("claude-3-5-sonnet-20241022", switchedConv.activeModelId)
    }

    @Test
    fun `workspace repository can create, edit and insert code snippets`() {
        val workspace = WorkspaceRepository()
        val initialCount = workspace.files.value.size
        assertTrue(initialCount >= 5)

        workspace.createFile("Utils.kt", "fun calculate() = 42")
        assertEquals(initialCount + 1, workspace.files.value.size)

        val active = workspace.getActiveFile()
        assertNotNull(active)
        assertEquals("Utils.kt", active?.name)
        assertTrue(active?.content?.contains("calculate") == true)

        workspace.insertIntoActiveFile("// Inserted AI Code")
        val updated = workspace.getActiveFile()
        assertTrue(updated?.content?.contains("// Inserted AI Code") == true)
    }
}
