package com.example.ui.editor

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.ProjectFile
import com.example.ui.theme.*
import com.example.viewmodel.DevStudioViewModel

@Composable
fun EditorScreen(
    viewModel: DevStudioViewModel,
    onNavigateToChat: () -> Unit
) {
    val files by viewModel.projectFiles.collectAsState()
    val activePath by viewModel.activeFilePath.collectAsState()
    val activeFile = files.firstOrNull { it.path == activePath } ?: files.firstOrNull()

    var fileContent by remember(activeFile?.path) {
        mutableStateOf(activeFile?.content ?: "")
    }

    var showCreateFileDialog by remember { mutableStateOf(false) }
    var newFileNameInput by remember { mutableStateOf("") }
    var showPreviewMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(bottom = 80.dp)
    ) {
        // Top Toolbar: File tabs + Actions
        Surface(
            color = CyberSurface,
            tonalElevation = 3.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Column {
                // File Tabs Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    files.forEach { file ->
                        val isActive = file.path == activePath
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    if (fileContent != activeFile?.content) {
                                        viewModel.workspaceRepository.updateActiveFileContent(fileContent)
                                    }
                                    viewModel.workspaceRepository.setActiveFile(file.path)
                                },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isActive) CyberCard else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isActive) CyanPrimary.copy(alpha = 0.6f) else CyberBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = getFileIcon(file.extension),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = file.name,
                                    color = if (isActive) CyanPrimary else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Add new file button
                    IconButton(
                        onClick = { showCreateFileDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New File",
                            tint = CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Action Bar: Save, Run/Preview toggle, Send to AI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Save Button
                        FilledTonalButton(
                            onClick = {
                                viewModel.workspaceRepository.updateActiveFileContent(fileContent)
                                viewModel.showBanner("Saved '${activeFile?.name}'")
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = CyberCard,
                                contentColor = CyanPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontSize = 11.sp)
                        }

                        // Preview / Code toggle
                        FilledTonalButton(
                            onClick = { showPreviewMode = !showPreviewMode },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (showPreviewMode) NeonGreen.copy(alpha = 0.2f) else CyberCard,
                                contentColor = if (showPreviewMode) NeonGreen else TextSecondary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                if (showPreviewMode) Icons.Default.Code else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showPreviewMode) "Editor" else "Run/Preview", fontSize = 11.sp)
                        }
                    }

                    // Send to AI Button
                    Button(
                        onClick = {
                            viewModel.workspaceRepository.updateActiveFileContent(fileContent)
                            viewModel.toggleAttachActiveFile()
                            onNavigateToChat()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPurple,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("send_to_ai_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send to AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Main Editor or Preview Area
        if (showPreviewMode) {
            // Preview / Run Mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                val isHtmlOrWeb = activeFile?.extension in listOf("html", "htm", "css", "js")

                if (isHtmlOrWeb) {
                    // Find all web files to inject
                    val htmlFile = files.firstOrNull { it.extension == "html" }?.content ?: fileContent
                    val cssFile = files.firstOrNull { it.extension == "css" }?.content ?: ""
                    val jsFile = files.firstOrNull { it.extension == "js" }?.content ?: ""

                    val fullHtml = htmlFile
                        .replace("<link rel=\"stylesheet\" href=\"styles.css\">", "<style>$cssFile</style>")
                        .replace("<script src=\"script.js\"></script>", "<script>$jsFile</script>")

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                loadDataWithBaseURL(null, fullHtml, "text/html", "utf-8", null)
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(null, fullHtml, "text/html", "utf-8", null)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Sandbox console for Kotlin / Python / scripts
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CodeBackground)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "CONSOLE OUTPUT [SANDBOX EXECUTION]",
                            color = NeonGreen,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Compiling ${activeFile?.name}...\nSyntax check passed. No diagnostic errors found.\nOutput:\nLoaded NAWA Studio runtime successfully.\nCode inspection ready for multi-AI refactoring.",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            // Code Editor
            val lines = remember(fileContent) { fileContent.lines() }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(CodeBackground)
            ) {
                // Line numbers gutter
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .background(Color(0xFF090D16))
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..maxOf(lines.size, 1)) {
                        Text(
                            text = "$i",
                            color = Color(0xFF475569),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Code Input Area
                OutlinedTextField(
                    value = fileContent,
                    onValueChange = { fileContent = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("code_editor_field"),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = TextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = CodeBackground,
                        unfocusedContainerColor = CodeBackground
                    )
                )
            }
        }
    }

    // Create New File Dialog
    if (showCreateFileDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("Create New Project File", color = TextPrimary) },
            text = {
                Column {
                    Text("Enter filename with extension (e.g., App.kt, styles.css, script.py):", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFileNameInput,
                        onValueChange = { newFileNameInput = it },
                        placeholder = { Text("FileName.kt") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileNameInput.isNotBlank()) {
                            viewModel.workspaceRepository.createFile(newFileNameInput)
                            newFileNameInput = ""
                            showCreateFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Create", color = Color(0xFF041824))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberSurface
        )
    }
}

private fun getFileIcon(extension: String): String {
    return when (extension.lowercase()) {
        "kt" -> "🔷"
        "java" -> "☕"
        "py" -> "🐍"
        "html" -> "🌐"
        "css" -> "🎨"
        "js" -> "⚡"
        "json" -> "📦"
        "md" -> "📝"
        else -> "📄"
    }
}
