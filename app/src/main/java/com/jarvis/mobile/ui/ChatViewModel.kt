package com.jarvis.mobile.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.mobile.data.local.ChatMessage
import com.jarvis.mobile.data.local.MemoryEntry
import com.jarvis.mobile.data.repository.JarvisRepository
import com.jarvis.mobile.domain.LocalCommandRegistry
import com.jarvis.mobile.domain.VoicePreferenceTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AssistantStatus(val label: String) {
    ONLINE("ONLINE"),
    OFFLINE("OFFLINE"),
    LISTENING("OUVINDO"),
    PROCESSING("PROCESSANDO"),
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val memories: List<MemoryEntry> = emptyList(),
    val status: AssistantStatus = AssistantStatus.OFFLINE,
    val isProcessing: Boolean = false,
    val isConnected: Boolean = false,
    val connectedEmail: String = "",
    val backendUrl: String = "",
    val anonymousKey: String = "",
    val voiceEnabled: Boolean = true,
    val voiceSpeed: Float = 1f,
    val voiceVolume: Float = 1f,
    val wakeWordPrepared: Boolean = false,
    val historyEnabled: Boolean = true,
    val error: String? = null,
    val notice: String? = null,
    val deviceInfo: String = "",
    val speechText: String = "",
    val speechSequence: Int = 0,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = JarvisRepository(application)
    private val localCommands = LocalCommandRegistry(
        application.applicationContext,
        VoicePreferenceTool(
            currentValue = { repository.preferences.voiceEnabled },
            onChange = { enabled ->
                repository.preferences.voiceEnabled = enabled
                mutableState.value = mutableState.value.copy(voiceEnabled = enabled)
            },
        ),
    )
    private val mutableState = MutableStateFlow(readPreferences())
    val state: StateFlow<ChatUiState> = mutableState.asStateFlow()

    init {
        refreshLocalData()
        updateStatus()
    }

    fun sendMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank() || mutableState.value.isProcessing) return

        val userMessage = ChatMessage(role = "user", content = text)
        mutableState.value = mutableState.value.copy(
            messages = mutableState.value.messages + userMessage,
            error = null,
            notice = null,
            isProcessing = true,
            status = AssistantStatus.PROCESSING,
        )
        viewModelScope.launch {
            try {
                repository.saveMessage(userMessage)
                val localReply = withContext(Dispatchers.IO) {
                    localCommands.execute(text)
                }
                val reply = when {
                    localReply != null -> localReply
                    !repository.preferences.backendIsConfigured() ->
                        throw IllegalStateException(
                            "Comandos locais estão disponíveis. Para conversar com a IA, configure o Supabase em Configurações.",
                        )
                    repository.currentSession() == null ->
                        throw IllegalStateException(
                            "Entre com sua conta Supabase em Configurações para usar a IA.",
                        )
                    else -> repository.ask(
                        messages = mutableState.value.messages,
                        memories = mutableState.value.memories,
                    )
                }

                val assistantMessage = ChatMessage(role = "assistant", content = reply)
                repository.saveMessage(assistantMessage)
                mutableState.value = mutableState.value.copy(
                    messages = mutableState.value.messages + assistantMessage,
                    notice = if (localReply != null) "Comando executado no aparelho." else null,
                    speechText = reply,
                    speechSequence = mutableState.value.speechSequence + 1,
                )
                runCatching { repository.syncMessages() }
                    .onFailure { error ->
                        mutableState.value = mutableState.value.copy(
                            notice = "Resposta recebida. A sincronização falhou: ${error.message.orEmpty()}",
                        )
                    }
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    error = error.message ?: "Não foi possível processar a mensagem.",
                )
            } finally {
                mutableState.value = mutableState.value.copy(isProcessing = false)
                updateStatus()
            }
        }
    }

    fun setListening(listening: Boolean) {
        if (mutableState.value.isProcessing) return
        mutableState.value = mutableState.value.copy(
            status = if (listening) AssistantStatus.LISTENING else currentReadyStatus(),
        )
    }

    fun setError(message: String) {
        mutableState.value = mutableState.value.copy(error = message)
        updateStatus()
    }

    fun dismissMessages() {
        mutableState.value = mutableState.value.copy(error = null, notice = null)
    }

    fun saveBackend(url: String, anonymousKey: String) {
        try {
            repository.saveBackendConfig(url.trim(), anonymousKey.trim())
            val session = repository.currentSession()
            mutableState.value = mutableState.value.copy(
                backendUrl = repository.preferences.backendUrl,
                anonymousKey = repository.preferences.anonymousKey,
                isConnected = session != null,
                connectedEmail = session?.email.orEmpty(),
                notice = "Configuração do backend salva neste aparelho.",
                error = null,
            )
            updateStatus()
        } catch (error: Exception) {
            setError(error.message ?: "Não foi possível salvar o backend.")
        }
    }

    fun createAccount(email: String, password: String) {
        runBackendAction {
            repository.signUp(email.trim(), password)
        }
    }

    fun signIn(email: String, password: String) {
        runBackendAction {
            repository.signIn(email.trim(), password)
            refreshData()
            "Sessão iniciada e dados sincronizados."
        }
    }

    fun signOut() {
        runBackendAction {
            repository.signOut()
            refreshData()
            "Sessão encerrada."
        }
    }

    fun saveVoiceSettings(
        enabled: Boolean,
        speed: Float,
        volume: Float,
        wakeWordPrepared: Boolean,
        historyEnabled: Boolean,
    ) {
        repository.preferences.apply {
            voiceEnabled = enabled
            voiceSpeed = speed
            voiceVolume = volume
            this.wakeWordPrepared = wakeWordPrepared
            this.historyEnabled = historyEnabled
        }
        mutableState.value = mutableState.value.copy(
            voiceEnabled = enabled,
            voiceSpeed = speed,
            voiceVolume = volume,
            wakeWordPrepared = wakeWordPrepared,
            historyEnabled = historyEnabled,
            notice = "Preferências salvas.",
        )
    }

    fun addMemory(text: String) {
        val content = text.trim().take(400)
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                val memory = MemoryEntry(content = content)
                repository.saveMemory(memory)
                mutableState.value = mutableState.value.copy(
                    memories = listOf(memory) + mutableState.value.memories,
                    notice = "Memória salva neste aparelho.",
                    error = null,
                )
                if (repository.currentSession() != null) {
                    repository.syncMemories()
                    mutableState.value = mutableState.value.copy(notice = "Memória sincronizada.")
                }
            } catch (error: Exception) {
                setError(error.message ?: "Não foi possível salvar a memória.")
            }
        }
    }

    fun removeMemory(memory: MemoryEntry) {
        viewModelScope.launch {
            try {
                repository.removeMemory(memory.id)
                mutableState.value = mutableState.value.copy(
                    memories = mutableState.value.memories.filterNot { it.id == memory.id },
                    notice = "Memória removida deste aparelho.",
                )
                if (repository.currentSession() != null) repository.deleteMemory(memory.id)
            } catch (error: Exception) {
                setError(error.message ?: "Não foi possível remover a memória.")
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                repository.deleteAllMessages()
                mutableState.value = mutableState.value.copy(
                    messages = emptyList(),
                    notice = "Histórico removido.",
                    error = null,
                )
            } catch (error: Exception) {
                refreshLocalData()
                setError(
                    "O histórico local foi removido, mas não foi possível limpar a cópia na nuvem: ${error.message.orEmpty()}",
                )
            }
        }
    }

    private fun runBackendAction(action: suspend () -> String) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                error = null,
                notice = null,
                isProcessing = true,
                status = AssistantStatus.PROCESSING,
            )
            try {
                val notice = action()
                refreshData()
                mutableState.value = mutableState.value.copy(notice = notice)
            } catch (error: Exception) {
                runCatching { refreshData() }
                mutableState.value = mutableState.value.copy(
                    error = error.message ?: "Não foi possível concluir a operação.",
                )
            } finally {
                mutableState.value = mutableState.value.copy(isProcessing = false)
                updateStatus()
            }
        }
    }

    private fun refreshLocalData() {
        viewModelScope.launch {
            runCatching {
                val messages = repository.messages()
                val memories = repository.memories()
                mutableState.value = mutableState.value.copy(messages = messages, memories = memories)
            }.onFailure {
                mutableState.value = mutableState.value.copy(
                    error = "Não foi possível carregar os dados locais: ${it.message.orEmpty()}",
                )
            }
        }
    }

    private suspend fun refreshData() {
        val messages = repository.messages()
        val memories = repository.memories()
        val session = repository.currentSession()
        mutableState.value = mutableState.value.copy(
            messages = messages,
            memories = memories,
            isConnected = session != null,
            connectedEmail = session?.email.orEmpty(),
            backendUrl = repository.preferences.backendUrl,
            anonymousKey = repository.preferences.anonymousKey,
        )
        updateStatus()
    }

    private fun readPreferences(): ChatUiState {
        val preferences = repository.preferences
        val session = runCatching { repository.currentSession() }.getOrNull()
        return ChatUiState(
            isConnected = session != null,
            connectedEmail = session?.email.orEmpty(),
            backendUrl = preferences.backendUrl,
            anonymousKey = preferences.anonymousKey,
            voiceEnabled = preferences.voiceEnabled,
            voiceSpeed = preferences.voiceSpeed,
            voiceVolume = preferences.voiceVolume,
            wakeWordPrepared = preferences.wakeWordPrepared,
            historyEnabled = preferences.historyEnabled,
            deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}",
        )
    }

    private fun currentReadyStatus(): AssistantStatus =
        if (runCatching { repository.currentSession() }.getOrNull() != null &&
            repository.preferences.backendIsConfigured()
        ) {
            AssistantStatus.ONLINE
        } else {
            AssistantStatus.OFFLINE
        }

    private fun updateStatus() {
        val current = mutableState.value
        if (!current.isProcessing && current.status != AssistantStatus.LISTENING) {
            mutableState.value = current.copy(status = currentReadyStatus())
        }
    }
}
