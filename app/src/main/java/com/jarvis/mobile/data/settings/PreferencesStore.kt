package com.jarvis.mobile.data.settings

import android.content.Context

class PreferencesStore(context: Context) {
    private val preferences =
        context.getSharedPreferences("jarvis_preferences", Context.MODE_PRIVATE)

    var backendUrl: String
        get() = preferences.getString(KEY_BACKEND_URL, "").orEmpty()
        set(value) = preferences.edit().putString(KEY_BACKEND_URL, value.trim().trimEnd('/')).apply()

    var anonymousKey: String
        get() = preferences.getString(KEY_ANON_KEY, "").orEmpty()
        set(value) = preferences.edit().putString(KEY_ANON_KEY, value.trim()).apply()

    var voiceEnabled: Boolean
        get() = preferences.getBoolean(KEY_VOICE_ENABLED, true)
        set(value) = preferences.edit().putBoolean(KEY_VOICE_ENABLED, value).apply()

    var voiceSpeed: Float
        get() = preferences.getFloat(KEY_VOICE_SPEED, 1f)
        set(value) = preferences.edit().putFloat(KEY_VOICE_SPEED, value.coerceIn(0.7f, 1.4f)).apply()

    var voiceVolume: Float
        get() = preferences.getFloat(KEY_VOICE_VOLUME, 1f)
        set(value) = preferences.edit().putFloat(KEY_VOICE_VOLUME, value.coerceIn(0f, 1f)).apply()

    var wakeWordPrepared: Boolean
        get() = preferences.getBoolean(KEY_WAKE_WORD_PREPARED, false)
        set(value) = preferences.edit().putBoolean(KEY_WAKE_WORD_PREPARED, value).apply()

    var historyEnabled: Boolean
        get() = preferences.getBoolean(KEY_HISTORY_ENABLED, true)
        set(value) = preferences.edit().putBoolean(KEY_HISTORY_ENABLED, value).apply()

    fun backendIsConfigured(): Boolean =
        backendUrl.startsWith("https://") && anonymousKey.isNotBlank()

    companion object {
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_ANON_KEY = "anon_key"
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_VOICE_SPEED = "voice_speed"
        private const val KEY_VOICE_VOLUME = "voice_volume"
        private const val KEY_WAKE_WORD_PREPARED = "wake_word_prepared"
        private const val KEY_HISTORY_ENABLED = "history_enabled"
    }
}
