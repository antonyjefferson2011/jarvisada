package com.jarvis.mobile.data.remote

import com.jarvis.mobile.data.local.ChatMessage
import com.jarvis.mobile.data.local.MemoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.TimeUnit

class SupabaseApi(
    private val baseUrl: String,
    private val anonymousKey: String,
    private val sessionProvider: () -> SupabaseSession?,
    private val saveSession: (SupabaseSession) -> Unit,
    private val clearSession: () -> Unit,
) {
    private val root = baseUrl.trim().trimEnd('/')
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(65, TimeUnit.SECONDS)
        .build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    init {
        require(root.startsWith("https://")) { "Informe a URL HTTPS do projeto Supabase." }
        require(anonymousKey.isNotBlank()) { "Informe a chave pública anon do Supabase." }
    }

    suspend fun signUp(email: String, password: String): String = withContext(Dispatchers.IO) {
        val result = rawRequest(
            path = "/auth/v1/signup",
            method = "POST",
            body = JSONObject().put("email", email.trim()).put("password", password),
        )
        val access = result.optString("access_token")
        if (access.isNotBlank()) {
            saveSession(parseSession(result))
            "Conta criada e conectada."
        } else {
            "Conta criada. Confirme o e-mail e depois entre no aplicativo."
        }
    }

    suspend fun signIn(email: String, password: String): SupabaseSession = withContext(Dispatchers.IO) {
        val result = rawRequest(
            path = "/auth/v1/token?grant_type=password",
            method = "POST",
            body = JSONObject().put("email", email.trim()).put("password", password),
        )
        parseSession(result).also(saveSession)
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val session = sessionProvider()
        if (session != null) {
            runCatching {
                rawRequest(
                    path = "/auth/v1/logout",
                    method = "POST",
                    session = session,
                    allowEmpty = true,
                )
            }
        }
        clearSession()
    }

    suspend fun loadMessages(): List<ChatMessage> = withUserSession { session ->
        val query = "select=id,role,content,created_at&user_id=eq.${session.userId}" +
            "&order=created_at.asc&limit=500"
        val rows = requestArray("/rest/v1/jarvis_messages?$query", session)
        buildList {
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                add(
                    ChatMessage(
                        id = row.getString("id"),
                        role = row.getString("role"),
                        content = row.getString("content"),
                        createdAt = parseTimestamp(row.getString("created_at")),
                    ),
                )
            }
        }
    }

    suspend fun syncMessages(messages: List<ChatMessage>) = withUserSession { session ->
        if (messages.isNotEmpty()) {
            val rows = JSONArray()
            messages.takeLast(500).forEach { message ->
                rows.put(
                    JSONObject()
                        .put("id", message.id)
                        .put("user_id", session.userId)
                        .put("conversation_id", PRIMARY_CONVERSATION_ID)
                        .put("role", message.role)
                        .put("content", message.content)
                        .put("created_at", Instant.ofEpochMilli(message.createdAt).toString()),
                )
            }
            rawRequest(
                path = "/rest/v1/jarvis_messages?on_conflict=id",
                method = "POST",
                body = rows,
                session = session,
                prefer = "resolution=merge-duplicates,return=minimal",
                allowEmpty = true,
            )
        }
    }

    suspend fun deleteMessages() = withUserSession { session ->
        rawRequest(
            path = "/rest/v1/jarvis_messages?user_id=eq.${session.userId}",
            method = "DELETE",
            session = session,
            allowEmpty = true,
        )
    }

    suspend fun loadMemories(): List<MemoryEntry> = withUserSession { session ->
        val rows = requestArray(
            "/rest/v1/jarvis_memories?select=id,content,created_at" +
                "&user_id=eq.${session.userId}&order=created_at.desc&limit=100",
            session,
        )
        buildList {
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                add(
                    MemoryEntry(
                        id = row.getString("id"),
                        content = row.getString("content"),
                        createdAt = parseTimestamp(row.getString("created_at")),
                    ),
                )
            }
        }
    }

    suspend fun syncMemories(memories: List<MemoryEntry>) = withUserSession { session ->
        if (memories.isNotEmpty()) {
            val rows = JSONArray()
            memories.take(100).forEach { memory ->
                rows.put(
                    JSONObject()
                        .put("id", memory.id)
                        .put("user_id", session.userId)
                        .put("content", memory.content)
                        .put("created_at", Instant.ofEpochMilli(memory.createdAt).toString()),
                )
            }
            rawRequest(
                path = "/rest/v1/jarvis_memories?on_conflict=id",
                method = "POST",
                body = rows,
                session = session,
                prefer = "resolution=merge-duplicates,return=minimal",
                allowEmpty = true,
            )
        }
    }

    suspend fun deleteMemory(id: String) = withUserSession { session ->
        rawRequest(
            path = "/rest/v1/jarvis_memories?id=eq.$id&user_id=eq.${session.userId}",
            method = "DELETE",
            session = session,
            allowEmpty = true,
        )
    }

    suspend fun askAssistant(
        messages: List<ChatMessage>,
        memories: List<MemoryEntry>,
    ): String = withUserSession { session ->
        val payload = JSONObject()
            .put(
                "messages",
                JSONArray().apply {
                    messages.takeLast(20).forEach { message ->
                        put(
                            JSONObject()
                                .put("role", message.role)
                                .put("content", message.content.take(MAX_MESSAGE_LENGTH)),
                        )
                    }
                },
            )
            .put(
                "memories",
                JSONArray().apply {
                    memories.take(20).forEach { put(it.content.take(400)) }
                },
            )
        val response = rawRequest(
            path = "/functions/v1/jarvis-chat",
            method = "POST",
            body = payload,
            session = session,
        )
        response.optString("reply").takeIf(String::isNotBlank)
            ?: throw BackendException("O backend retornou uma resposta vazia.")
    }

    private suspend fun <T> withUserSession(block: suspend (SupabaseSession) -> T): T {
        var session = sessionProvider()
            ?: throw BackendException("Entre com sua conta Supabase em Configurações.")
        return try {
            block(session)
        } catch (error: HttpStatusException) {
            if (error.status != 401) throw error
            session = refreshSession(session)
            block(session)
        }
    }

    private suspend fun refreshSession(oldSession: SupabaseSession): SupabaseSession {
        val json = rawRequest(
            path = "/auth/v1/token?grant_type=refresh_token",
            method = "POST",
            body = JSONObject().put("refresh_token", oldSession.refreshToken),
        )
        return parseSession(json).also(saveSession)
    }

    private suspend fun requestArray(path: String, session: SupabaseSession): JSONArray =
        withContext(Dispatchers.IO) {
            val response = execute(path, "GET", null, session)
            try {
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = errorMessage(text)
                    throw HttpStatusException(
                        response.code,
                        message.ifBlank { "Falha HTTP ${response.code} ao acessar o Supabase." },
                    )
                }
                JSONArray(text)
            } finally {
                response.close()
            }
        }

    private suspend fun rawRequest(
        path: String,
        method: String = "GET",
        body: Any? = null,
        session: SupabaseSession? = null,
        prefer: String? = null,
        allowEmpty: Boolean = false,
    ): JSONObject = withContext(Dispatchers.IO) {
        val response = execute(path, method, body, session, prefer)
        try {
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = errorMessage(text)
                throw HttpStatusException(
                    response.code,
                    message.ifBlank { "Falha HTTP ${response.code} ao acessar o Supabase." },
                )
            }
            if (allowEmpty && text.isBlank()) JSONObject() else JSONObject(text.ifBlank { "{}" })
        } finally {
            response.close()
        }
    }

    private fun errorMessage(text: String): String = runCatching {
        val json = JSONObject(text)
        sequenceOf("msg", "message", "error_description", "hint", "error")
            .map(json::optString)
            .firstOrNull(String::isNotBlank)
            .orEmpty()
    }.getOrDefault("")

    private suspend fun execute(
        path: String,
        method: String,
        body: Any?,
        session: SupabaseSession?,
        prefer: String? = null,
    ): okhttp3.Response = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url("$root$path")
            .header("apikey", anonymousKey)
            .header("Accept", "application/json")
        if (session != null) {
            builder.header("Authorization", "Bearer ${session.accessToken}")
        }
        if (prefer != null) builder.header("Prefer", prefer)

        val requestBody = body?.toString()?.toRequestBody(jsonMediaType)
        when (method) {
            "POST" -> builder.post(requestBody ?: ByteArray(0).toRequestBody(jsonMediaType))
            "DELETE" -> builder.delete(requestBody)
            else -> builder.get()
        }
        client.newCall(builder.build()).execute()
    }

    private fun parseSession(json: JSONObject): SupabaseSession {
        val user = json.optJSONObject("user")
            ?: throw BackendException("A resposta de autenticação não contém usuário.")
        val accessToken = json.optString("access_token")
            .takeIf(String::isNotBlank)
            ?: throw BackendException("O Supabase não retornou uma sessão. Confirme o e-mail e tente entrar.")
        return SupabaseSession(
            accessToken = accessToken,
            refreshToken = json.optString("refresh_token"),
            userId = user.optString("id").takeIf(String::isNotBlank)
                ?: throw BackendException("O Supabase não retornou o identificador da conta."),
            email = user.optString("email"),
        )
    }

    private fun parseTimestamp(value: String): Long =
        runCatching { Instant.parse(value).toEpochMilli() }.getOrElse { System.currentTimeMillis() }

    private class HttpStatusException(val status: Int, message: String) : BackendException(message)

    companion object {
        private const val PRIMARY_CONVERSATION_ID = "00000000-0000-0000-0000-000000000001"
        private const val MAX_MESSAGE_LENGTH = 6000
    }
}
