package com.example.ui.compare

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.model.AIProviderId
import com.example.model.AIResponse
import com.example.ui.components.CodeBlockView
import com.example.ui.components.ProviderBadge
import com.example.ui.theme.*
import com.example.viewmodel.DevStudioViewModel

@Composable
fun CompareScreen(
    viewModel: DevStudioViewModel,
    onNavigateToSettings: () -> Unit
) {
    val session by viewModel.compareSession.collectAsState()
    val isComparing by viewModel.isComparing.collectAsState()
    val configs by viewModel.providerConfigs.collectAsState()

    var promptInput by remember { mutableStateOf("Create a Jetpack Compose glassmorphic login card with email validation") }
    val selectedProviders = remember {
        mutableStateListOf(
            AIProviderId.GEMINI,
            AIProviderId.OPENAI,
            AIProviderId.CLAUDE,
            AIProviderId.DEEPSEEK
        )
    }

    var selectedTabProvider by remember { mutableStateOf<AIProviderId?>(null) }

    val presetPrompts = listOf(
        "Create a Jetpack Compose glassmorphic login card with email validation",
        "Implement a thread-safe LRU Cache in Kotlin with O(1) operations",
        "Write a robust TypeScript debounce and throttle function",
        "Design a responsive CSS Grid product card layout with dark mode"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Compare Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(NeonPurple.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompareArrows,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Multi-AI Benchmark & Comparison",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Execute one prompt simultaneously across multiple AI labs.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "SELECT TARGET PROVIDERS (${selectedProviders.size} SELECTED)",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Multi-select provider chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AIProviderId.entries.forEach { pId ->
                            val isSelected = selectedProviders.contains(pId)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        if (selectedProviders.size > 1) selectedProviders.remove(pId)
                                    } else {
                                        selectedProviders.add(pId)
                                    }
                                },
                                label = { Text(pId.shortName, fontSize = 11.sp) },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonPurple.copy(alpha = 0.25f),
                                    selectedLabelColor = TextPrimary,
                                    containerColor = CyberCard,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prompt Input
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        label = { Text("Comparison Prompt / Question", color = TextSecondary, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compare_prompt_input"),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberCard,
                            unfocusedContainerColor = CyberCard
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset prompts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetPrompts.forEach { preset ->
                            SuggestionChip(
                                onClick = { promptInput = preset },
                                label = { Text(preset.take(24) + "...", fontSize = 10.sp, color = TextSecondary) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = CyberCard),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = CyberBorder
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Run Comparison Button
                    Button(
                        onClick = {
                            viewModel.startComparison(promptInput, selectedProviders.toList())
                        },
                        enabled = promptInput.isNotBlank() && selectedProviders.isNotEmpty() && !isComparing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("run_compare_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isComparing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Querying ${selectedProviders.size} Providers Concurrently...", fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Run",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Submit to ${selectedProviders.size} AI Providers",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Live Execution / Results Section
        if (session != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COMPARISON RESULTS",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${session?.results?.size ?: 0} / ${session?.targetProviders?.size ?: 0} Finished",
                        color = CyanPrimary,
                        fontSize = 11.sp
                    )
                }
            }

            // Tabs for quick switching between results
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // "All" tab
                    FilterChip(
                        selected = selectedTabProvider == null,
                        onClick = { selectedTabProvider = null },
                        label = { Text("View All", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyanPrimary
                        )
                    )

                    session?.targetProviders?.forEach { pId ->
                        val resp = session?.results?.get(pId)
                        val inProg = session?.inProgress?.get(pId) == true
                        val isSelected = selectedTabProvider == pId

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTabProvider = pId },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(pId.shortName, fontSize = 11.sp)
                                    if (inProg) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(10.dp),
                                            strokeWidth = 2.dp,
                                            color = CyanPrimary
                                        )
                                    } else if (resp?.isError == true) {
                                        Text("⚠️", fontSize = 9.sp)
                                    } else if (resp != null) {
                                        Text("${resp.latencyMs}ms", fontSize = 9.sp, color = NeonGreen)
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = CyanPrimary
                            )
                        )
                    }
                }
            }

            // Results Cards
            val displayProviders = if (selectedTabProvider != null) {
                listOf(selectedTabProvider!!)
            } else {
                session?.targetProviders ?: emptyList()
            }

            items(displayProviders) { pId ->
                val response = session?.results?.get(pId)
                val inProgress = session?.inProgress?.get(pId) == true

                CompareResultCard(
                    providerId = pId,
                    response = response,
                    inProgress = inProgress,
                    onInsertToEditor = { code -> viewModel.insertSnippetToEditor(code) },
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
private fun CompareResultCard(
    providerId: AIProviderId,
    response: AIResponse?,
    inProgress: Boolean,
    onInsertToEditor: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Provider + Model + Latency + Tokens
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderBadge(providerId = providerId, modelName = response?.modelId)
                }

                if (response != null && !response.isError) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${response.latencyMs} ms",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (response.totalTokens != null) {
                            Text(
                                text = "• ${response.totalTokens} tokens",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                inProgress -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CyanPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Querying ${providerId.displayName}...",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                response?.isError == true -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeonRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .border(1.dp, NeonRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = NeonRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Provider Error",
                                color = NeonRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = response.errorMessage ?: "Unknown error occurred.",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = onNavigateToSettings,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Configure API Key in Settings →", color = CyanPrimary, fontSize = 11.sp)
                        }
                    }
                }

                response != null -> {
                    // Extract code blocks and text
                    val codeBlocks = Regex("```([a-zA-Z0-9_]*)\\n?([\\s\\S]*?)```").findAll(response.text).toList()

                    if (codeBlocks.isEmpty()) {
                        Text(
                            text = response.text,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    } else {
                        var lastIndex = 0
                        for (match in codeBlocks) {
                            if (match.range.first > lastIndex) {
                                val preText = response.text.substring(lastIndex, match.range.first).trim()
                                if (preText.isNotEmpty()) {
                                    Text(
                                        text = preText,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                            val lang = match.groupValues[1].ifBlank { "code" }
                            val code = match.groupValues[2]
                            CodeBlockView(
                                code = code,
                                language = lang,
                                onInsertToEditor = onInsertToEditor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            lastIndex = match.range.last + 1
                        }

                        if (lastIndex < response.text.length) {
                            val postText = response.text.substring(lastIndex).trim()
                            if (postText.isNotEmpty()) {
                                Text(
                                    text = postText,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                else -> {
                    Text(
                        text = "Awaiting execution...",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
