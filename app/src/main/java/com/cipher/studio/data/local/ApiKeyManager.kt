package com.cipher.studio.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "cipher_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
    }

    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit().putString(KEY_GEMINI_API, apiKey).apply()
    }

    fun getApiKey(): String? {
        return sharedPreferences.getString(KEY_GEMINI_API, null)
    }

    fun clearApiKey() {
        sharedPreferences.edit().remove(KEY_GEMINI_API).apply()
    }
    
    fun hasKey(): Boolean = !getApiKey().isNullOrBlank()
}