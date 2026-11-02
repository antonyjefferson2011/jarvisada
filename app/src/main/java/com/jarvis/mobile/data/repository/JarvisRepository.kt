package com.jarvis.mobile.data.repository

import android.content.Context
import com.jarvis.mobile.data.local.ChatMessage
import com.jarvis.mobile.data.local.JarvisDatabase
import com.jarvis.mobile.data.local.MemoryEntry
import com.jarvis.mobile.data.remote.SupabaseApi
import com.jarvis.mobile.data.remote.SupabaseSession
import com.jarvis.mobile.data.settings.PreferencesStore
import com.jarvis.mobile.security.SecureTokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JarvisRepository(context: Context) {
    private val database = JarvisDatabase(context.applicationContext)
    val preferences = PreferencesStore(context.applicationContext)
    private val tokenStore = SecureTokenStore(context.applicationContext)

    fun currentSession(): SupabaseSession? = tokenStore.load()

    suspend fun messages(): List<ChatMessage> = withContext(Dispatchers.IO) {
        database.listMessages()
    }

    suspend fun memories(): List<MemoryEntry> = withContext(Dispatchers.IO) {
        database.listMemories()
    }

    suspend fun saveMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        if (preferences.historyEnabled) database.upsertMessage(message)
    }

    suspend fun saveMemory(memory: MemoryEntry) = withContext(Dispatchers.IO) {
        database.upsertMemory(memory)
    }

    suspend fun removeMemory(id: String) = withContext(Dispatchers.IO) {
        database.removeMemory(id)
    }

    suspend fun deleteMemory(id: String) {
        if (currentSession() != null) api().deleteMemory(id)
    }

    suspend fun signUp(email: String, password: String): String =
        api().signUp(email, password)

    suspend fun signIn(email: String, password: String): String {
        api().signIn(email, password)
        syncCloud()
        return "Conectado como ${tokenStore.load()?.email.orEmpty()}."
    }

    suspend fun signOut() {
        if (preferences.backendIsConfigured()) api().signOut() else tokenStore.clear()
    }

    suspend fun ask(messages: List<ChatMessage>, memories: List<MemoryEntry>): String =
        api().askAssistant(messages, memories)

    suspend fun syncCloud() {
        val backend = api()
        val remoteMessages = backend.loadMessages()
        val allMessages = withContext(Dispatchers.IO) {
            database.mergeMessages(remoteMessages)
            database.listMessages()
        }
        backend.syncMessages(allMessages)

        val remoteMemories = backend.loadMemories()
        val allMemories = withContext(Dispatchers.IO) {
            database.mergeMemories(remoteMemories)
            database.listMemories()
        }
        backend.syncMemories(allMemories)
    }

    suspend fun syncMessages() {
        if (currentSession() == null) return
        api().syncMessages(messages())
    }

    suspend fun syncMemories() {
        if (currentSession() == null) return
        api().syncMemories(memories())
    }

    suspend fun deleteAllMessages() {
        withContext(Dispatchers.IO) { database.clearMessages() }
        if (currentSession() != null) api().deleteMessages()
    }

    fun saveBackendConfig(url: String, anonymousKey: String) {
        val changed = preferences.backendUrl != url.trim().trimEnd('/') ||
            preferences.anonymousKey != anonymousKey.trim()
        preferences.backendUrl = url
        preferences.anonymousKey = anonymousKey
        if (changed) tokenStore.clear()
    }

    private fun api(): SupabaseApi {
        check(preferences.backendIsConfigured()) {
            "Configure a URL HTTPS e a chave anon do Supabase antes de continuar."
        }
        return SupabaseApi(
            baseUrl = preferences.backendUrl,
            anonymousKey = preferences.anonymousKey,
            sessionProvider = tokenStore::load,
            saveSession = tokenStore::save,
            clearSession = tokenStore::clear,
        )
    }
}
