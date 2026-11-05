package com.example.data

import com.example.model.ProjectFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkspaceRepository {
    private val initialFiles = listOf(
        ProjectFile(
            name = "MainActivity.kt",
            extension = "kt",
            path = "app/src/main/java/com/nawa/MainActivity.kt",
            content = """package com.nawa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DevStudioGreeting("NAWA DevStudio")
                }
            }
        }
    }
}

@Composable
fun DevStudioGreeting(appName: String) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Welcome to " + appName, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Multi-AI Provider IDE ready for rapid development.")
    }
}
"""
        ),
        ProjectFile(
            name = "index.html",
            extension = "html",
            path = "web/index.html",
            content = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>NAWA Studio Web Preview</title>
  <link rel="stylesheet" href="styles.css">
</head>
<body>
  <div class="card">
    <div class="badge">NAWA DevStudio</div>
    <h1>Multi-AI Engine</h1>
    <p>Connected to 8 Frontier AI Providers with live side-by-side comparison.</p>
    <button id="actionBtn" onclick="onExplore()">Explore Features</button>
    <div id="output"></div>
  </div>
  <script src="script.js"></script>
</body>
</html>
"""
        ),
        ProjectFile(
            name = "styles.css",
            extension = "css",
            path = "web/styles.css",
            content = """body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  background: #0b0f19;
  color: #f1f5f9;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  margin: 0;
}
.card {
  background: #111827;
  border: 1px solid #1e293b;
  border-radius: 16px;
  padding: 32px;
  max-width: 420px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.5);
  text-align: center;
}
.badge {
  display: inline-block;
  background: #00e5ff22;
  color: #00e5ff;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 12px;
  border-radius: 999px;
  margin-bottom: 12px;
}
button {
  background: linear-gradient(135deg, #00e5ff, #8b5cf6);
  color: #0b0f19;
  font-weight: bold;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  cursor: pointer;
  margin-top: 16px;
}
"""
        ),
        ProjectFile(
            name = "script.js",
            extension = "js",
            path = "web/script.js",
            content = """function onExplore() {
  const out = document.getElementById('output');
  out.innerHTML = '<p style="color: #00e5ff; margin-top: 12px;">Providers: OpenAI, Gemini, Claude, DeepSeek, Mistral, Groq, OpenRouter, Ollama!</p>';
}
"""
        ),
        ProjectFile(
            name = "main.py",
            extension = "py",
            path = "scripts/main.py",
            content = """# NAWA DevStudio AI Orchestrator
import time

PROVIDERS = [
    "OpenAI", "Google Gemini", "Anthropic Claude",
    "DeepSeek", "Mistral AI", "Groq", "OpenRouter", "Ollama"
]

def main():
    print("Initiating Multi-AI System Benchmark...")
    for p in PROVIDERS:
        print(f"Provider: {p} -> Standby")
    print(f"Loaded {len(PROVIDERS)} AI Providers successfully.")

if __name__ == "__main__":
    main()
"""
        ),
        ProjectFile(
            name = "README.md",
            extension = "md",
            path = "README.md",
            content = """# NAWA DevStudio IDE

Next-generation mobile developer IDE with a unified **Multi-AI Provider System**.

## Supported AI Providers
1. **OpenAI** (ChatGPT: gpt-4o, gpt-4o-mini, o1, o3-mini)
2. **Google Gemini** (gemini-2.0-flash, gemini-1.5-pro, gemini-1.5-flash)
3. **Anthropic Claude** (claude-3-7-sonnet, claude-3-5-sonnet, claude-3-5-haiku)
4. **DeepSeek** (deepseek-chat V3, deepseek-reasoner R1)
5. **Mistral AI** (codestral, mistral-large, mistral-small)
6. **Groq** (llama-3.3-70b-versatile, deepseek-r1-distill, llama-3.1-8b)
7. **OpenRouter** (unified routing gateway to 100+ models)
8. **Ollama** (Local models: llama3.2, codellama, deepseek-coder)

## Features
- **Multi-AI Dashboard**: Live status, latency metrics, connected providers.
- **Side-by-Side Comparison**: Submit one prompt to multiple AI models concurrently.
- **Code Editor**: Syntax highlighting, line numbers, live web preview, file manager.
- **Secure Android Keystore**: Encrypted API credentials storage.
"""
        )
    )

    private val _files = MutableStateFlow<List<ProjectFile>>(initialFiles)
    val files: StateFlow<List<ProjectFile>> = _files.asStateFlow()

    private val _activeFilePath = MutableStateFlow<String>(initialFiles[0].path)
    val activeFilePath: StateFlow<String> = _activeFilePath.asStateFlow()

    fun getActiveFile(): ProjectFile? {
        return _files.value.firstOrNull { it.path == _activeFilePath.value } ?: _files.value.firstOrNull()
    }

    fun setActiveFile(path: String) {
        _activeFilePath.value = path
    }

    fun updateActiveFileContent(newContent: String) {
        val path = _activeFilePath.value
        val list = _files.value.map { file ->
            if (file.path == path) {
                file.copy(content = newContent)
            } else {
                file
            }
        }
        _files.value = list
    }

    fun insertIntoActiveFile(codeSnippet: String) {
        val current = getActiveFile() ?: return
        val updated = current.content + "\n\n" + codeSnippet
        updateActiveFileContent(updated)
    }

    fun createFile(name: String, content: String = "") {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val ext = cleanName.substringAfterLast('.', "txt")
        val path = "workspace/$cleanName"
        val newFile = ProjectFile(
            name = cleanName,
            extension = ext,
            path = path,
            content = content
        )
        val list = _files.value.toMutableList()
        list.add(newFile)
        _files.value = list
        _activeFilePath.value = path
    }

    fun deleteFile(path: String) {
        val list = _files.value.filter { it.path != path }
        if (list.isNotEmpty()) {
            _files.value = list
            if (_activeFilePath.value == path) {
                _activeFilePath.value = list.first().path
            }
        }
    }
}
