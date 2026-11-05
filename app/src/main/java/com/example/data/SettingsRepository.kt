package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.core.security.SecureKeyStorage
import com.example.model.AIProviderId
import com.example.model.ApiLogEntry
import com.example.model.ConnectionTestResult
import com.example.model.ProviderConfig
import com.example.provider.ProviderRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(
    context: Context,
    private val logRepository: LogRepository
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nawa_settings", Context.MODE_PRIVATE)
    private val secureStorage = SecureKeyStorage(context)

    private val _configs = MutableStateFlow<Map<AIProviderId, ProviderConfig>>(emptyMap())
    val configs: StateFlow<Map<AIProviderId, ProviderConfig>> = _configs.asStateFlow()

    private val _defaultProviderId = MutableStateFlow(AIProviderId.GEMINI)
    val defaultProviderId: StateFlow<AIProviderId> = _defaultProviderId.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val defProvName = prefs.getString("default_provider", AIProviderId.GEMINI.name) ?: AIProviderId.GEMINI.name
        val defProvider = AIProviderId.fromId(defProvName)
        _defaultProviderId.value = defProvider

        val map = mutableMapOf<AIProviderId, ProviderConfig>()
        AIProviderId.entries.forEach { pid ->
            val provider = ProviderRegistry.getProvider(pid)
            val apiKey = secureStorage.getApiKey(pid.name)
            val endpoint = prefs.getString("endpoint_${pid.name}", provider.defaultEndpoint) ?: provider.defaultEndpoint
            val selectedModel = prefs.getString("model_${pid.name}", provider.models.first().id) ?: provider.models.first().id
            val isEnabled = prefs.getBoolean("enabled_${pid.name}", true)
            val lastTestedSuccess = if (prefs.contains("tested_ok_${pid.name}")) prefs.getBoolean("tested_ok_${pid.name}", false) else null
            val lastTestedTimestamp = prefs.getLong("tested_ts_${pid.name}", 0L)
            val lastLatency = prefs.getLong("tested_lat_${pid.name}", 0L)

            map[pid] = ProviderConfig(
                providerId = pid,
                apiKey = apiKey,
                endpoint = endpoint,
                selectedModelId = selectedModel,
                isEnabled = isEnabled,
                lastTestedSuccess = lastTestedSuccess,
                lastTestedTimestamp = lastTestedTimestamp,
                lastLatencyMs = lastLatency
            )
        }
        _configs.value = map
    }

    fun updateApiKey(providerId: AIProviderId, key: String) {
        secureStorage.saveApiKey(providerId.name, key.trim())
        val current = _configs.value[providerId] ?: return
        val updated = current.copy(
            apiKey = key.trim(),
            lastTestedSuccess = null // Reset status on key change until tested
        )
        updateConfigState(providerId, updated)
    }

    fun removeApiKey(providerId: AIProviderId) {
        secureStorage.removeApiKey(providerId.name)
        val current = _configs.value[providerId] ?: return
        val updated = current.copy(
            apiKey = "",
            lastTestedSuccess = null
        )
        updateConfigState(providerId, updated)
    }

    fun updateEndpoint(providerId: AIProviderId, endpoint: String) {
        val cleanEndpoint = endpoint.trim()
        prefs.edit().putString("endpoint_${providerId.name}", cleanEndpoint).apply()
        val current = _configs.value[providerId] ?: return
        val updated = current.copy(endpoint = cleanEndpoint)
        updateConfigState(providerId, updated)
    }

    fun updateSelectedModel(providerId: AIProviderId, modelId: String) {
        prefs.edit().putString("model_${providerId.name}", modelId).apply()
        val current = _configs.value[providerId] ?: return
        val updated = current.copy(selectedModelId = modelId)
        updateConfigState(providerId, updated)
    }

    fun setProviderEnabled(providerId: AIProviderId, enabled: Boolean) {
        prefs.edit().putBoolean("enabled_${providerId.name}", enabled).apply()
        val current = _configs.value[providerId] ?: return
        val updated = current.copy(isEnabled = enabled)
        updateConfigState(providerId, updated)
    }

    fun setDefaultProvider(providerId: AIProviderId) {
        prefs.edit().putString("default_provider", providerId.name).apply()
        _defaultProviderId.value = providerId
    }

    suspend fun testProviderConnection(providerId: AIProviderId): ConnectionTestResult {
        val config = _configs.value[providerId] ?: return ConnectionTestResult(false, message = "Provider not found")
        val provider = ProviderRegistry.getProvider(providerId)

        val result = provider.testConnection(config)

        // Persist test outcome
        prefs.edit()
            .putBoolean("tested_ok_${providerId.name}", result.success)
            .putLong("tested_ts_${providerId.name}", System.currentTimeMillis())
            .putLong("tested_lat_${providerId.name}", result.latencyMs)
            .apply()

        val updated = config.copy(
            lastTestedSuccess = result.success,
            lastTestedTimestamp = System.currentTimeMillis(),
            lastLatencyMs = result.latencyMs
        )
        updateConfigState(providerId, updated)

        // Log result
        logRepository.addLog(
            ApiLogEntry(
                providerId = providerId,
                modelId = config.selectedModelId,
                endpoint = config.endpoint,
                statusCode = result.statusCode,
                latencyMs = result.latencyMs,
                success = result.success,
                promptSnippet = "[Connection Test]",
                errorMessage = if (!result.success) result.message else null
            )
        )

        return result
    }

    private fun updateConfigState(providerId: AIProviderId, config: ProviderConfig) {
        val mutable = _configs.value.toMutableMap()
        mutable[providerId] = config
        _configs.value = mutable
    }
}
