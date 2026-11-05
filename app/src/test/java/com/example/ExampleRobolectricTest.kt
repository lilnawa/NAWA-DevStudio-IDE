package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.security.SecureKeyStorage
import com.example.data.LogRepository
import com.example.data.SettingsRepository
import com.example.model.AIProviderId
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app_name is NAWA DevStudio`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NAWA DevStudio", appName)
    }

    @Test
    fun `secure storage can save and retrieve provider keys`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = SecureKeyStorage(context)

        storage.saveApiKey("OPENAI", "sk-test-secret-key-12345")
        assertTrue(storage.hasApiKey("OPENAI"))
        assertEquals("sk-test-secret-key-12345", storage.getApiKey("OPENAI"))

        storage.removeApiKey("OPENAI")
        assertFalse(storage.hasApiKey("OPENAI"))
        assertEquals("", storage.getApiKey("OPENAI"))
    }

    @Test
    fun `settings repository manages default provider and endpoints`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val logRepo = LogRepository()
        val settingsRepo = SettingsRepository(context, logRepo)

        assertEquals(AIProviderId.GEMINI, settingsRepo.defaultProviderId.value)

        settingsRepo.setDefaultProvider(AIProviderId.CLAUDE)
        assertEquals(AIProviderId.CLAUDE, settingsRepo.defaultProviderId.value)

        settingsRepo.updateEndpoint(AIProviderId.OLLAMA, "http://192.168.1.50:11434")
        val ollamaConfig = settingsRepo.configs.value[AIProviderId.OLLAMA]
        assertNotNull(ollamaConfig)
        assertEquals("http://192.168.1.50:11434", ollamaConfig?.endpoint)
    }
}
