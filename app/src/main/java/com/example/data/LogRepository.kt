package com.example.data

import com.example.model.ApiLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LogRepository {
    private val _logs = MutableStateFlow<List<ApiLogEntry>>(emptyList())
    val logs: StateFlow<List<ApiLogEntry>> = _logs.asStateFlow()

    fun addLog(entry: ApiLogEntry) {
        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 100) {
            _logs.value = current.take(100)
        } else {
            _logs.value = current
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
