package com.jarvis.mobile.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.jarvis.mobile.data.remote.SupabaseSession
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureTokenStore(context: Context) {
    private val preferences =
        context.getSharedPreferences("jarvis_secure_session", Context.MODE_PRIVATE)

    fun save(session: SupabaseSession) {
        val plainText = JSONObject()
            .put("accessToken", session.accessToken)
            .put("refreshToken", session.refreshToken)
            .put("userId", session.userId)
            .put("email", session.email)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plainText)
        val packed = cipher.iv + encrypted
        check(preferences.edit().putString(SESSION_KEY, Base64.encodeToString(packed, Base64.NO_WRAP)).commit()) {
            "Não foi possível guardar a sessão protegida."
        }
    }

    fun load(): SupabaseSession? {
        val encoded = preferences.getString(SESSION_KEY, null) ?: return null
        return try {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            require(packed.size > IV_LENGTH) { "Sessão protegida inválida." }
            val iv = packed.copyOfRange(0, IV_LENGTH)
            val encrypted = packed.copyOfRange(IV_LENGTH, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val json = JSONObject(String(cipher.doFinal(encrypted), Charsets.UTF_8))
            SupabaseSession(
                accessToken = json.getString("accessToken"),
                refreshToken = json.getString("refreshToken"),
                userId = json.getString("userId"),
                email = json.optString("email"),
            )
        } catch (error: Exception) {
            throw IllegalStateException(
                "A sessão local não pôde ser descriptografada. Saia e entre novamente.",
                error,
            )
        }
    }

    fun clear() {
        preferences.edit().remove(SESSION_KEY).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "jarvis_session_aes"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val SESSION_KEY = "encrypted_session"
        private const val IV_LENGTH = 12
    }
}
