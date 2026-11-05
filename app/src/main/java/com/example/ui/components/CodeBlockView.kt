package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Input
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CodeBlockView(
    code: String,
    language: String = "code",
    onInsertToEditor: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val highlightedText = remember(code, language) {
        highlightSyntax(code.trim(), language)
    }

    val lines = remember(code) { code.trim().lines() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
            .background(CodeBackground, RoundedCornerShape(8.dp))
    ) {
        // Code header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(CyanPrimary, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = language.uppercase(),
                    color = CyanPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${lines.size} lines",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onInsertToEditor != null) {
                    IconButton(
                        onClick = { onInsertToEditor(code.trim()) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("insert_to_editor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Input,
                            contentDescription = "Insert into Editor",
                            tint = NeonPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("code", code.trim())
                        clipboard.setPrimaryClip(clip)
                        copied = true
                        scope.launch {
                            delay(2000)
                            copied = false
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("copy_code_button")
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (copied) NeonGreen else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Code content with line numbers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            // Line numbers
            Column(
                modifier = Modifier.padding(end = 12.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lines.size) {
                    Text(
                        text = "$i",
                        color = Color(0xFF475569),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Syntax styled code
            Text(
                text = highlightedText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

private fun highlightSyntax(code: String, language: String): AnnotatedString {
    return buildAnnotatedString {
        val keywords = setOf(
            "fun", "val", "var", "class", "interface", "object", "import", "package",
            "return", "if", "else", "when", "for", "while", "do", "in", "is", "as",
            "def", "import", "from", "print", "class", "self", "async", "await",
            "function", "const", "let", "var", "export", "default", "new", "this",
            "public", "private", "protected", "static", "void", "override",
            "suspend", "data", "sealed", "enum", "typealias", "try", "catch", "throw"
        )

        val words = code.split(Regex("(?<=[^a-zA-Z0-9_])|(?=[^a-zA-Z0-9_])"))
        for (w in words) {
            when {
                w in keywords -> {
                    pushStyle(SpanStyle(color = CodeKeyword))
                    append(w)
                    pop()
                }
                w.startsWith("\"") || w.endsWith("\"") || w.startsWith("'") || w.endsWith("'") -> {
                    pushStyle(SpanStyle(color = CodeString))
                    append(w)
                    pop()
                }
                w.startsWith("//") || w.startsWith("#") -> {
                    pushStyle(SpanStyle(color = CodeComment))
                    append(w)
                    pop()
                }
                w.toIntOrNull() != null -> {
                    pushStyle(SpanStyle(color = CodeNumber))
                    append(w)
                    pop()
                }
                w.startsWith("@") -> {
                    pushStyle(SpanStyle(color = NeonPurple))
                    append(w)
                    pop()
                }
                else -> {
                    pushStyle(SpanStyle(color = TextPrimary))
                    append(w)
                    pop()
                }
            }
        }
    }
}
