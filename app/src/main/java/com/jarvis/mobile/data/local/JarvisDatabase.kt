package com.jarvis.mobile.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class JarvisDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE messages (
                id TEXT PRIMARY KEY NOT NULL,
                role TEXT NOT NULL CHECK(role IN ('user', 'assistant')),
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX messages_created_at_idx ON messages(created_at)")
        db.execSQL(
            """
            CREATE TABLE memories (
                id TEXT PRIMARY KEY NOT NULL,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS memories (
                    id TEXT PRIMARY KEY NOT NULL,
                    content TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

    fun listMessages(limit: Int = 500): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()
        readableDatabase.query(
            "messages",
            arrayOf("id", "role", "content", "created_at"),
            null,
            null,
            null,
            null,
            "created_at ASC",
            limit.toString(),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += ChatMessage(
                    id = cursor.getString(0),
                    role = cursor.getString(1),
                    content = cursor.getString(2),
                    createdAt = cursor.getLong(3),
                )
            }
        }
        return result
    }

    fun upsertMessage(message: ChatMessage) {
        val values = ContentValues().apply {
            put("id", message.id)
            put("role", message.role)
            put("content", message.content)
            put("created_at", message.createdAt)
        }
        writableDatabase.insertWithOnConflict(
            "messages",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun clearMessages() {
        writableDatabase.delete("messages", null, null)
    }

    fun mergeMessages(messages: List<ChatMessage>) {
        writableDatabase.beginTransaction()
        try {
            messages.forEach(::upsertMessage)
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun listMemories(): List<MemoryEntry> {
        val result = mutableListOf<MemoryEntry>()
        readableDatabase.query(
            "memories",
            arrayOf("id", "content", "created_at"),
            null,
            null,
            null,
            null,
            "created_at DESC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += MemoryEntry(
                    id = cursor.getString(0),
                    content = cursor.getString(1),
                    createdAt = cursor.getLong(2),
                )
            }
        }
        return result
    }

    fun upsertMemory(memory: MemoryEntry) {
        val values = ContentValues().apply {
            put("id", memory.id)
            put("content", memory.content)
            put("created_at", memory.createdAt)
        }
        writableDatabase.insertWithOnConflict(
            "memories",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    fun removeMemory(id: String) {
        writableDatabase.delete("memories", "id = ?", arrayOf(id))
    }

    fun mergeMemories(memories: List<MemoryEntry>) {
        writableDatabase.beginTransaction()
        try {
            memories.forEach(::upsertMemory)
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    companion object {
        private const val DATABASE_NAME = "jarvis.db"
        private const val DATABASE_VERSION = 2
    }
}
