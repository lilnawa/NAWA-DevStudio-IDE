package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AICapability
import com.example.model.AIProviderId
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.provider.ProviderRegistry
import com.example.ui.components.CodeBlockView
import com.example.ui.components.ProviderBadge
import com.example.ui.components.getProviderColor
import com.example.ui.theme.*
import com.example.viewmodel.DevStudioViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: DevStudioViewModel,
    onNavigateToCompare: () -> Unit,
    onNavigateToEditor: () -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()
    val activeConvId by viewModel.activeConversationId.collectAsState()
    val activeConversation = conversations.firstOrNull { it.id == activeConvId } ?: conversations.first()

    val configs by viewModel.providerConfigs.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val attachedContext by viewModel.attachedFileContext.collectAsState()
    val activeFile = viewModel.workspaceRepository.getActiveFile()

    var inputText by remember { mutableStateOf("") }
    var showProviderMenu by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }
    var showSessionsDrawer by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val currentProvider = ProviderRegistry.getProvider(activeConversation.activeProviderId)
    val currentCapabilities = currentProvider.getCapabilities(activeConversation.activeModelId)

    // Scroll to bottom on new message
    LaunchedEffect(activeConversation.messages.size) {
        if (activeConversation.messages.isNotEmpty()) {
            listState.animateScrollToItem(activeConversation.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(bottom = 80.dp)
    ) {
        // Top AI Provider & Model Bar
        Surface(
            color = CyberSurface,
            tonalElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Provider selector dropdown trigger
                    Box {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showProviderMenu = true }
                                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp)),
                            color = CyberCard
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ProviderBadge(providerId = activeConversation.activeProviderId)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Provider",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showProviderMenu,
                            onDismissRequest = { showProviderMenu = false },
                            modifier = Modifier.background(CyberSurface)
                        ) {
                            AIProviderId.entries.forEach { pId ->
                                val prov = ProviderRegistry.getProvider(pId)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            ProviderBadge(providerId = pId)
                                            Text(
                                                text = prov.displayName,
                                                color = if (pId == activeConversation.activeProviderId) CyanPrimary else TextPrimary,
                                                fontSize = 13.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.switchActiveProvider(pId)
                                        showProviderMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Model selector trigger
                    Box {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showModelMenu = true }
                                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp)),
                            color = CyberCard
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = activeConversation.activeModelId,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Model",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false },
                            modifier = Modifier.background(CyberSurface)
                        ) {
                            currentProvider.models.forEach { modelInfo ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = modelInfo.name,
                                                color = if (modelInfo.id == activeConversation.activeModelId) CyanPrimary else TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = modelInfo.description,
                                                color = TextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.switchActiveProvider(activeConversation.activeProviderId, modelInfo.id)
                                        showModelMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Conversation manager / new chat button
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                viewModel.conversationRepository.createNewConversation(
                                    providerId = activeConversation.activeProviderId,
                                    modelId = activeConversation.activeModelId
                                )
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = "New Session",
                                tint = CyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { onNavigateToCompare() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompareArrows,
                                contentDescription = "Compare Mode",
                                tint = NeonPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Capabilities & Quick Actions Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // File context attachment button
                    AssistChip(
                        onClick = { viewModel.toggleAttachActiveFile() },
                        label = {
                            Text(
                                text = if (attachedContext != null) "Attached: ${activeFile?.name}" else "+ Attach File",
                                fontSize = 11.sp,
                                color = if (attachedContext != null) NeonGreen else TextSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (attachedContext != null) Icons.Default.Check else Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = if (attachedContext != null) NeonGreen else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (attachedContext != null) NeonGreen.copy(alpha = 0.15f) else CyberCard
                        )
                    )

                    // Quick AI Developer action chips
                    if (currentCapabilities.contains(AICapability.CODE_EXPLANATION)) {
                        DevToolChip(label = "Explain File") {
                            viewModel.executeCodingAction(AICapability.CODE_EXPLANATION)
                        }
                    }
                    if (currentCapabilities.contains(AICapability.DEBUGGING)) {
                        DevToolChip(label = "Fix Bugs") {
                            viewModel.executeCodingAction(AICapability.DEBUGGING)
                        }
                    }
                    if (currentCapabilities.contains(AICapability.REFACTORING)) {
                        DevToolChip(label = "Refactor") {
                            viewModel.executeCodingAction(AICapability.REFACTORING)
                        }
                    }
                    if (currentCapabilities.contains(AICapability.DOCUMENTATION)) {
                        DevToolChip(label = "Create Docs") {
                            viewModel.executeCodingAction(AICapability.DOCUMENTATION)
                        }
                    }
                    if (currentCapabilities.contains(AICapability.KOTLIN_ANDROID)) {
                        DevToolChip(label = "Android / Kotlin") {
                            viewModel.executeCodingAction(AICapability.KOTLIN_ANDROID)
                        }
                    }
                }
            }
        }

        // Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
        ) {
            items(activeConversation.messages) { message ->
                MessageBubble(
                    message = message,
                    onInsertToEditor = { snippet ->
                        viewModel.insertSnippetToEditor(snippet)
                    }
                )
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = CyanPrimary
                        )
                        Text(
                            text = "${activeConversation.activeProviderId.displayName} is thinking...",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Regenerate button row (if last was assistant)
        val lastMessage = activeConversation.messages.lastOrNull()
        if (lastMessage?.sender == MessageSender.ASSISTANT && !isGenerating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { viewModel.regenerateLastResponse() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        tint = CyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Regenerate", color = CyanPrimary, fontSize = 11.sp)
                }
            }
        }

        // Input Area
        Surface(
            color = CyberSurface,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Ask ${activeConversation.activeProviderId.shortName} to write, debug, or refactor code...",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberCard,
                            unfocusedContainerColor = CyberCard
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isGenerating) {
                                val text = inputText
                                inputText = ""
                                viewModel.sendMessage(text)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                if (inputText.isNotBlank() && !isGenerating) CyanPrimary else CyberCard,
                                CircleShape
                            )
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && !isGenerating) Color(0xFF041824) else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DevToolChip(label: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp, color = TextPrimary) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = CyberCard),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = CyberBorder
        )
    )
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onInsertToEditor: (String) -> Unit
) {
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender header badge
        if (!isUser && message.providerId != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            ) {
                ProviderBadge(providerId = message.providerId, modelName = message.modelId)
                if (message.latencyMs != null && message.latencyMs > 0) {
                    Text(
                        text = "${message.latencyMs}ms",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Bubble Content
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            color = if (isUser) Color(0xFF003848) else CyberSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) CyanPrimary.copy(alpha = 0.5f) else CyberBorder
            ),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.attachedFileName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Context: ${message.attachedFileName}",
                            color = NeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Render text with code blocks extraction
                FormattedMessageContent(
                    text = message.text,
                    isUser = isUser,
                    onInsertToEditor = onInsertToEditor
                )
            }
        }
    }
}

@Composable
private fun FormattedMessageContent(
    text: String,
    isUser: Boolean,
    onInsertToEditor: (String) -> Unit
) {
    val codeBlockRegex = Regex("```([a-zA-Z0-9_]*)\\n?([\\s\\S]*?)```")
    var lastIndex = 0

    val matches = codeBlockRegex.findAll(text).toList()

    if (matches.isEmpty()) {
        Text(
            text = text,
            color = if (isUser) Color(0xFFE0F7FA) else TextPrimary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (match in matches) {
                // Text before code block
                if (match.range.first > lastIndex) {
                    val plain = text.substring(lastIndex, match.range.first).trim()
                    if (plain.isNotEmpty()) {
                        Text(
                            text = plain,
                            color = if (isUser) Color(0xFFE0F7FA) else TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }

                val lang = match.groupValues[1].ifBlank { "code" }
                val code = match.groupValues[2]
                CodeBlockView(
                    code = code,
                    language = lang,
                    onInsertToEditor = onInsertToEditor
                )

                lastIndex = match.range.last + 1
            }

            // Remaining text after last code block
            if (lastIndex < text.length) {
                val remaining = text.substring(lastIndex).trim()
                if (remaining.isNotEmpty()) {
                    Text(
                        text = remaining,
                        color = if (isUser) Color(0xFFE0F7FA) else TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}
