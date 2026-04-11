package com.unesell.comfysorter

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SavedServer(val url: String, val name: String, val addedAt: Long)

class ServerRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("servers_prefs", Context.MODE_PRIVATE)

    private val _servers = MutableStateFlow<List<SavedServer>>(emptyList())
    val servers: StateFlow<List<SavedServer>> = _servers.asStateFlow()

    init {
        loadServers()
    }

    private fun loadServers() {
        val serversString = prefs.getString("saved_servers", "") ?: ""
        if (serversString.isNotBlank()) {
            val list = serversString.split("|").mapNotNull {
                val parts = it.split(",")
                if (parts.size == 3) SavedServer(parts[0], parts[1], parts[2].toLong()) else null
            }
            _servers.value = list.sortedByDescending { it.addedAt }
        }
    }

    fun addServer(url: String) {
        val currentList = _servers.value.toMutableList()
        // Избегаем дубликатов
        if (currentList.none { it.url == url }) {
            // Формируем имя из URL, например убираем https://
            val name = url.replace("https://", "").replace("http://", "").substringBefore("/")
            currentList.add(SavedServer(url, name, System.currentTimeMillis()))
            saveServers(currentList)
        }
    }

    fun removeServer(url: String) {
        val currentList = _servers.value.filterNot { it.url == url }
        saveServers(currentList)
    }

    fun renameServer(url: String, newName: String) {
        val currentList = _servers.value.map {
            if (it.url == url) it.copy(name = newName) else it
        }
        saveServers(currentList)
    }

    private fun saveServers(list: List<SavedServer>) {
        val serialized = list.joinToString("|") { "${it.url},${it.name},${it.addedAt}" }
        prefs.edit().putString("saved_servers", serialized).apply()
        _servers.value = list.sortedByDescending { it.addedAt }
    }
}