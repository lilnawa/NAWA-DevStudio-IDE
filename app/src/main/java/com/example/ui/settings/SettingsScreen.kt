package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AIProviderId
import com.example.model.ApiLogEntry
import com.example.model.ProviderConfig
import com.example.provider.ProviderRegistry
import com.example.ui.components.ConnectionStatusDot
import com.example.ui.components.ProviderBadge
import com.example.ui.theme.*
import com.example.viewmodel.DevStudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: DevStudioViewModel) {
    val context = LocalContext.current
    val configs by viewModel.providerConfigs.collectAsState()
    val defaultProvider by viewModel.defaultProviderId.collectAsState()
    val testingProvider by viewModel.testingProviderId.collectAsState()
    val testResults by viewModel.connectionTestResults.collectAsState()
    val logs by viewModel.apiLogs.collectAsState()

    var expandedProviderId by remember { mutableStateOf<AIProviderId?>(null) }
    var showLogsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Settings Header
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(CyanPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AI API & Keystore Security",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Credentials encrypted via Android Keystore (AES-256 GCM).",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Default AI Provider",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = defaultProvider.displayName,
                                color = CyanPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // View Logs Button
                        OutlinedButton(
                            onClick = { showLogsDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("API Logs (${logs.size})", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Section title
        item {
            Text(
                text = "CONFIGURED PROVIDERS (${AIProviderId.entries.size})",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // 8 Provider Cards
        items(AIProviderId.entries) { providerId ->
            val provider = ProviderRegistry.getProvider(providerId)
            val config = configs[providerId] ?: ProviderConfig(providerId = providerId, selectedModelId = provider.models.first().id)
            val isExpanded = expandedProviderId == providerId
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
                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedProviderId = if (isExpanded) null else providerId
                            },
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ConnectionStatusDot(
                                testedSuccess = config.lastTestedSuccess,
                                isEnabled = config.isEnabled,
                                hasApiKey = config.apiKey.isNotBlank(),
                                requiresKey = provider.requiresApiKey
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Expanded Details & Editing
                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            HorizontalDivider(color = CyberBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Provider description & docs link
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = provider.description,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(provider.documentationUrl))
                                        context.startActivity(intent)
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("API Docs ↗", color = CyanPrimary, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Enable / Disable switch & Set Default row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Switch(
                                        checked = config.isEnabled,
                                        onCheckedChange = { viewModel.settingsRepository.setProviderEnabled(providerId, it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = CyanPrimary,
                                            checkedTrackColor = CyanPrimary.copy(alpha = 0.3f)
                                        )
                                    )
                                    Text(
                                        text = if (config.isEnabled) "Enabled" else "Disabled",
                                        color = if (config.isEnabled) TextPrimary else TextMuted,
                                        fontSize = 12.sp
                                    )
                                }

                                if (!isDefault && config.isEnabled) {
                                    TextButton(
                                        onClick = { viewModel.settingsRepository.setDefaultProvider(providerId) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Set as Default", color = CyanPrimary, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // API Key Field (if required)
                            if (provider.requiresApiKey) {
                                var keyInput by remember(config.apiKey) { mutableStateOf(config.apiKey) }
                                var showPassword by remember { mutableStateOf(false) }

                                Text(
                                    text = "API Key (Stored in Android Keystore)",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = keyInput,
                                        onValueChange = { keyInput = it },
                                        placeholder = { Text("Enter ${provider.displayName} API Key", fontSize = 12.sp, color = TextMuted) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("key_input_${providerId.name.lowercase()}"),
                                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { showPassword = !showPassword }) {
                                                Icon(
                                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = "Toggle key visibility",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyanPrimary,
                                            unfocusedBorderColor = CyberBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedContainerColor = CyberCard,
                                            unfocusedContainerColor = CyberCard
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.settingsRepository.updateApiKey(providerId, keyInput)
                                            viewModel.showBanner("Securely saved ${provider.id.shortName} API Key")
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(50.dp)
                                    ) {
                                        Text("Save", color = Color(0xFF041824), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (config.apiKey.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                viewModel.settingsRepository.removeApiKey(providerId)
                                                keyInput = ""
                                                viewModel.showBanner("Removed ${provider.id.shortName} key")
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Remove Key",
                                                tint = NeonRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Custom API Endpoint field
                            var endpointInput by remember(config.endpoint) { mutableStateOf(config.endpoint) }
                            Text(
                                text = "API Endpoint (Default or Custom Proxy / Ollama Host)",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = endpointInput,
                                    onValueChange = { endpointInput = it },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyanPrimary,
                                        unfocusedBorderColor = CyberBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedContainerColor = CyberCard,
                                        unfocusedContainerColor = CyberCard
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Button(
                                    onClick = {
                                        viewModel.settingsRepository.updateEndpoint(providerId, endpointInput)
                                        viewModel.showBanner("Updated ${provider.id.shortName} endpoint")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCard),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Text("Apply", color = CyanPrimary, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Model selection dropdown
                            Text(
                                text = "Active Model",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            var modelDropdownExpanded by remember { mutableStateOf(false) }

                            Box {
                                OutlinedButton(
                                    onClick = { modelDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CyberCard),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(config.selectedModelId, color = TextPrimary, fontSize = 12.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                                    }
                                }

                                DropdownMenu(
                                    expanded = modelDropdownExpanded,
                                    onDismissRequest = { modelDropdownExpanded = false },
                                    modifier = Modifier.background(CyberSurface)
                                ) {
                                    provider.models.forEach { m ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(m.name, color = CyanPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("${m.id} • ${m.contextWindow}", color = TextSecondary, fontSize = 10.sp)
                                                }
                                            },
                                            onClick = {
                                                viewModel.settingsRepository.updateSelectedModel(providerId, m.id)
                                                modelDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Real "Test Connection" button
                            Button(
                                onClick = { viewModel.testConnection(providerId) },
                                enabled = !isTesting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("verify_conn_${providerId.name.lowercase()}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = CyanPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = CyanPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Testing connection...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Connection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Show recent test result details if any
                            val testRes = testResults[providerId]
                            if (testRes != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = if (testRes.success) NeonGreen.copy(alpha = 0.1f) else NeonRed.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (testRes.success) NeonGreen.copy(alpha = 0.3f) else NeonRed.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(
                                        text = testRes.message,
                                        color = if (testRes.success) NeonGreen else NeonRed,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // API Logs & Errors Dialog
    if (showLogsDialog) {
        val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("API Request & Error Logs", color = TextPrimary, fontSize = 16.sp)
                    IconButton(onClick = { viewModel.logRepository.clearLogs() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = TextSecondary)
                    }
                }
            },
            text = {
                if (logs.isEmpty()) {
                    Text("No requests logged yet. Connection tests and AI completions will appear here.", color = TextMuted, fontSize = 12.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs) { log ->
                            Surface(
                                color = CyberCard,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (log.success) NeonGreen.copy(alpha = 0.3f) else NeonRed.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${log.providerId.shortName} • ${log.modelId}",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "${dateFormat.format(Date(log.timestamp))} (${log.latencyMs}ms)",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Status: ${if (log.statusCode > 0) log.statusCode else "Network"}",
                                        color = if (log.success) NeonGreen else NeonRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!log.errorMessage.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = log.errorMessage,
                                            color = NeonRed,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogsDialog = false }) {
                    Text("Close", color = CyanPrimary)
                }
            },
            containerColor = CyberSurface
        )
    }
}
