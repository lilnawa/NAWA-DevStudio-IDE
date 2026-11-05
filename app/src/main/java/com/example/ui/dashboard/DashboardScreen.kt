package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AIProviderId
import com.example.provider.ProviderRegistry
import com.example.ui.components.ConnectionStatusDot
import com.example.ui.components.ProviderBadge
import com.example.ui.components.getProviderColor
import com.example.ui.theme.*
import com.example.viewmodel.DevStudioViewModel

@Composable
fun DashboardScreen(
    viewModel: DevStudioViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToCompare: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val configs by viewModel.providerConfigs.collectAsState()
    val defaultProvider by viewModel.defaultProviderId.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val testingProvider by viewModel.testingProviderId.collectAsState()

    val connectedCount = configs.values.count { it.lastTestedSuccess == true }
    val readyCount = configs.values.count { it.isEnabled && (!it.providerId.requiresKey || it.apiKey.isNotBlank()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Hero Header Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF0F1A30),
                                    Color(0xFF142442),
                                    Color(0xFF1D1B3A)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(CyanPrimary, RoundedCornerShape(5.dp))
                            )
                            Text(
                                text = "NAWA DEVSTUDIO IDE",
                                color = CyanPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Multi-AI Provider Engine",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Unified orchestration across 8 premier AI labs. Real APIs, side-by-side comparison & code intelligence.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Metric pills row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricPill(
                                label = "Verified",
                                value = "$connectedCount / 8",
                                color = NeonGreen
                            )
                            MetricPill(
                                label = "Configured",
                                value = "$readyCount / 8",
                                color = CyanPrimary
                            )
                            MetricPill(
                                label = "Default",
                                value = defaultProvider.shortName,
                                color = NeonPurple
                            )
                        }
                    }
                }
            }
        }

        // Quick Launch Actions
        item {
            Text(
                text = "WORKBENCH QUICK LAUNCH",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "AI Chat",
                    subtitle = "8 Providers",
                    icon = Icons.Default.Chat,
                    accentColor = CyanPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToChat
                )
                QuickActionCard(
                    title = "AI Compare",
                    subtitle = "Multi-Model",
                    icon = Icons.Default.CompareArrows,
                    accentColor = NeonPurple,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCompare
                )
                QuickActionCard(
                    title = "Code Editor",
                    subtitle = "Live Workspace",
                    icon = Icons.Default.Code,
                    accentColor = NeonGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToEditor
                )
            }
        }

        // Provider Status Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI PROVIDERS STATUS & MODELS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Configure",
                    color = CyanPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToSettings() }
                )
            }
        }

        // List of 8 providers
        items(AIProviderId.entries) { providerId ->
            val provider = ProviderRegistry.getProvider(providerId)
            val config = configs[providerId]
            val isDefault = providerId == defaultProvider
            val isTesting = testingProvider == providerId

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isDefault) CyanPrimary.copy(alpha = 0.5f) else CyberBorder,
                        RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProviderBadge(providerId = providerId)
                            if (isDefault) {
                                Surface(
                                    color = CyanPrimary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "DEFAULT",
                                        color = CyanPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        ConnectionStatusDot(
                            testedSuccess = config?.lastTestedSuccess,
                            isEnabled = config?.isEnabled ?: true,
                            hasApiKey = config?.apiKey?.isNotBlank() == true,
                            requiresKey = provider.requiresApiKey
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = provider.description,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Model and latency info
                        Column {
                            Text(
                                text = "Model: ${config?.selectedModelId ?: provider.models.first().id}",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (config?.lastLatencyMs != null && config.lastLatencyMs > 0L) {
                                Text(
                                    text = "Latency: ${config.lastLatencyMs} ms",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Test button
                        FilledTonalButton(
                            onClick = { viewModel.testConnection(providerId) },
                            enabled = !isTesting,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = CyberCard,
                                contentColor = CyanPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("test_btn_${providerId.name.lowercase()}")
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = CyanPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Test Connection",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Recent Conversations summary
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "RECENT CONVERSATIONS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        items(conversations.take(3)) { conv ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.conversationRepository.setActiveConversation(conv.id)
                        onNavigateToChat()
                    }
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = conv.title,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${conv.messages.size} messages • ${conv.activeProviderId.displayName}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, color: Color) {
    Surface(
        color = CyberSurface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = label, color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberSurface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(text = title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = TextMuted, fontSize = 10.sp)
        }
    }
}
