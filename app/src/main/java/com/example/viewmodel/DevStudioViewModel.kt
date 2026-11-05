package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ConversationRepository
import com.example.data.LogRepository
import com.example.data.SettingsRepository
import com.example.data.WorkspaceRepository
import com.example.model.AICapability
import com.example.model.AIProviderId
import com.example.model.AIRequest
import com.example.model.AIResponse
import com.example.model.ApiLogEntry
import com.example.model.ChatMessage
import com.example.model.CompareSession
import com.example.model.ConnectionTestResult
import com.example.model.MessageSender
import com.example.model.ProviderConfig
import com.example.provider.ProviderRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DevStudioViewModel(application: Application) : AndroidViewModel(application) {
    val logRepository = LogRepository()
    val settingsRepository = SettingsRepository(application, logRepository)
    val workspaceRepository = WorkspaceRepository()
    val conversationRepository = ConversationRepository()

    val providerConfigs: StateFlow<Map<AIProviderId, ProviderConfig>> = settingsRepository.configs
    val defaultProviderId: StateFlow<AIProviderId> = settingsRepository.defaultProviderId
    val apiLogs = logRepository.logs
    val projectFiles = workspaceRepository.files
    val activeFilePath = workspaceRepository.activeFilePath
    val conversations = conversationRepository.conversations
    val activeConversationId = conversationRepository.activeConversationId

    // Chat UI state
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _attachedFileContext = MutableStateFlow<String?>(null)
    val attachedFileContext: StateFlow<String?> = _attachedFileContext.asStateFlow()

    // Compare UI state
    private val _compareSession = MutableStateFlow<CompareSession?>(null)
    val compareSession: StateFlow<CompareSession?> = _compareSession.asStateFlow()

    private val _isComparing = MutableStateFlow(false)
    val isComparing: StateFlow<Boolean> = _isComparing.asStateFlow()

    // Testing connection UI state
    private val _testingProviderId = MutableStateFlow<AIProviderId?>(null)
    val testingProviderId: StateFlow<AIProviderId?> = _testingProviderId.asStateFlow()

    private val _connectionTestResults = MutableStateFlow<Map<AIProviderId, ConnectionTestResult>>(emptyMap())
    val connectionTestResults: StateFlow<Map<AIProviderId, ConnectionTestResult>> = _connectionTestResults.asStateFlow()

    // Status message / toast banner
    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    fun dismissBanner() {
        _statusBanner.value = null
    }

    fun showBanner(message: String) {
        _statusBanner.value = message
    }

    fun toggleAttachActiveFile() {
        if (_attachedFileContext.value != null) {
            _attachedFileContext.value = null
        } else {
            val active = workspaceRepository.getActiveFile()
            if (active != null) {
                _attachedFileContext.value = "File: ${active.path}\n\n${active.content}"
                showBanner("Attached '${active.name}' as AI context")
            }
        }
    }

    fun clearAttachedContext() {
        _attachedFileContext.value = null
    }

    fun switchActiveProvider(providerId: AIProviderId, modelId: String? = null) {
        val config = providerConfigs.value[providerId]
        val targetModel = modelId ?: config?.selectedModelId ?: ProviderRegistry.getDefaultModel(providerId)
        conversationRepository.updateActiveProviderAndModel(providerId, targetModel)
        showBanner("Switched to ${providerId.displayName} ($targetModel)")
    }

    fun sendMessage(promptText: String, capabilityOverride: String? = null) {
        if (promptText.isBlank() || _isGenerating.value) return

        val activeConv = conversationRepository.getActiveConversation()
        val providerId = activeConv.activeProviderId
        val modelId = activeConv.activeModelId
        val config = providerConfigs.value[providerId] ?: return

        val activeFileName = if (_attachedFileContext.value != null) workspaceRepository.getActiveFile()?.name else null

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = promptText,
            providerId = providerId,
            modelId = modelId,
            attachedFileName = activeFileName
        )
        conversationRepository.addMessage(userMessage)

        val fileCtx = _attachedFileContext.value
        _attachedFileContext.value = null

        _isGenerating.value = true

        viewModelScope.launch {
            val provider = ProviderRegistry.getProvider(providerId)
            val request = AIRequest(
                prompt = promptText,
                systemPrompt = capabilityOverride,
                history = activeConv.messages,
                fileContext = fileCtx
            )

            val response = provider.generateResponse(request, config)

            logRepository.addLog(
                ApiLogEntry(
                    providerId = providerId,
                    modelId = modelId,
                    endpoint = config.endpoint,
                    statusCode = if (response.isError) 400 else 200,
                    latencyMs = response.latencyMs,
                    success = !response.isError,
                    promptSnippet = promptText.take(60),
                    errorMessage = response.errorMessage
                )
            )

            val assistantMsg = ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = if (response.isError) "⚠️ ${response.errorMessage}" else response.text,
                providerId = providerId,
                modelId = modelId,
                latencyMs = response.latencyMs,
                isError = response.isError
            )
            conversationRepository.addMessage(assistantMsg)
            _isGenerating.value = false
        }
    }

    fun regenerateLastResponse() {
        val activeConv = conversationRepository.getActiveConversation()
        if (activeConv.messages.isEmpty() || _isGenerating.value) return

        val lastAssistant = conversationRepository.removeLastAssistantMessage() ?: return
        val lastUserMessage = activeConv.messages.lastOrNull { it.sender == MessageSender.USER } ?: return

        sendMessage(lastUserMessage.text)
    }

    fun executeCodingAction(action: AICapability) {
        val activeFile = workspaceRepository.getActiveFile()
        if (activeFile == null) {
            showBanner("No file currently open in editor")
            return
        }

        val prompt = when (action) {
            AICapability.CODE_EXPLANATION -> "Please explain the architecture and functionality of '${activeFile.name}'."
            AICapability.DEBUGGING -> "Analyze '${activeFile.name}' for potential bugs, security issues, null-pointer exceptions, or concurrency errors. Provide fixes."
            AICapability.REFACTORING -> "Refactor '${activeFile.name}' for cleaner architecture, idiomatic patterns, better modularity, and enhanced performance."
            AICapability.DOCUMENTATION -> "Generate comprehensive technical documentation, inline KDoc/JSDoc comments, and a README section for '${activeFile.name}'."
            AICapability.KOTLIN_ANDROID -> "Convert or upgrade '${activeFile.name}' following modern Android Jetpack Compose & Clean Architecture best practices."
            AICapability.WEB_DEV -> "Optimize and modernize this web file '${activeFile.name}' with responsive UI and clean logic."
            AICapability.PYTHON_JAVA -> "Optimize and convert this code into clean, typed Python/Java production standards."
            AICapability.CODE_GENERATION -> "Generate comprehensive unit tests and edge-case validations for '${activeFile.name}'."
            AICapability.FILE_ANALYSIS -> "Perform a complete static analysis of '${activeFile.name}'."
        }

        _attachedFileContext.value = "File: ${activeFile.path}\n\n${activeFile.content}"
        sendMessage(prompt, capabilityOverride = "Specialized Developer Tool: ${action.title}")
    }

    fun startComparison(prompt: String, selectedProviders: List<AIProviderId>) {
        if (prompt.isBlank() || selectedProviders.isEmpty() || _isComparing.value) return

        val session = CompareSession(
            prompt = prompt,
            targetProviders = selectedProviders,
            inProgress = selectedProviders.associateWith { true }
        )
        _compareSession.value = session
        _isComparing.value = true

        viewModelScope.launch {
            val tasks = selectedProviders.map { pId ->
                async {
                    val config = providerConfigs.value[pId]
                    val provider = ProviderRegistry.getProvider(pId)
                    val resp = if (config != null) {
                        provider.generateResponse(AIRequest(prompt = prompt), config)
                    } else {
                        AIResponse(
                            text = "",
                            providerId = pId,
                            modelId = "unknown",
                            latencyMs = 0L,
                            isError = true,
                            errorMessage = "Provider configuration not found"
                        )
                    }

                    // Update live session progress
                    val currentSession = _compareSession.value
                    if (currentSession != null) {
                        val newResults = currentSession.results.toMutableMap()
                        val newInProgress = currentSession.inProgress.toMutableMap()
                        newResults[pId] = resp
                        newInProgress[pId] = false
                        _compareSession.value = currentSession.copy(
                            results = newResults,
                            inProgress = newInProgress
                        )
                    }

                    logRepository.addLog(
                        ApiLogEntry(
                            providerId = pId,
                            modelId = config?.selectedModelId ?: "default",
                            endpoint = config?.endpoint ?: provider.defaultEndpoint,
                            statusCode = if (resp.isError) 400 else 200,
                            latencyMs = resp.latencyMs,
                            success = !resp.isError,
                            promptSnippet = "[Compare] ${prompt.take(50)}",
                            errorMessage = resp.errorMessage
                        )
                    )
                    Pair(pId, resp)
                }
            }

            tasks.awaitAll()
            _isComparing.value = false
        }
    }

    fun testConnection(providerId: AIProviderId) {
        if (_testingProviderId.value != null) return
        _testingProviderId.value = providerId

        viewModelScope.launch {
            val result = settingsRepository.testProviderConnection(providerId)
            val currentMap = _connectionTestResults.value.toMutableMap()
            currentMap[providerId] = result
            _connectionTestResults.value = currentMap
            _testingProviderId.value = null

            if (result.success) {
                showBanner("✅ ${providerId.displayName}: ${result.message}")
            } else {
                showBanner("❌ ${providerId.displayName}: ${result.message}")
            }
        }
    }

    fun insertSnippetToEditor(codeSnippet: String) {
        workspaceRepository.insertIntoActiveFile(codeSnippet)
        showBanner("Inserted code snippet into '${workspaceRepository.getActiveFile()?.name}'")
    }
}
