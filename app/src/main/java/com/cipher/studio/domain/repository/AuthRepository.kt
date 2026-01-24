package com.cipher.studio.domain.repository

import android.content.Context
import android.provider.Settings
import com.cipher.studio.data.local.LocalStorageManager // Added Import
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// AuthResult class remains the same...
sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val storageManager: LocalStorageManager, // Injected Storage
    @ApplicationContext private val context: Context
) {

    suspend fun verifyAccess(email: String, key: String): AuthResult {
        // 1. FAST PATH: Check Local Storage first (Offline Support)
        if (storageManager.isAuthorized()) {
            return AuthResult.Success
        }

        // 2. SLOW PATH: Check Firebase (Online)
        return try {
            val cleanEmail = email.lowercase().trim()
            if (cleanEmail.isEmpty() || key.isEmpty()) {
                return AuthResult.Error("እባክዎ መረጃዎን በትክክል ያስገቡ!")
            }

            val currentDeviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            val userRef = db.collection("authorized_users").document(cleanEmail)
            val userSnap = userRef.get().await()

            if (userSnap.exists()) {
                val accessKey = userSnap.getString("accessKey")
                val isPaid = userSnap.getBoolean("isPaid") ?: false
                val registeredDeviceId = userSnap.getString("deviceId")

                if (accessKey == key && isPaid) {
                    // Device Logic
                    if (registeredDeviceId.isNullOrEmpty()) {
                        userRef.update("deviceId", currentDeviceId).await()
                        // SUCCESS: Save to local storage
                        storageManager.setAuthorized(true) 
                        AuthResult.Success
                    } else if (registeredDeviceId == currentDeviceId) {
                        // SUCCESS: Save to local storage
                        storageManager.setAuthorized(true)
                        AuthResult.Success
                    } else {
                        AuthResult.Error("❌ Access Violation: ይህ ቁልፍ በሌላ ስልክ ተይዟል!")
                    }
                } else {
                    AuthResult.Error("❌ Access Denied: የተሳሳተ ቁልፍ!")
                }
            } else {
                AuthResult.Error("🔍 ተጠቃሚው አልተገኘም።")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AuthResult.Error("📡 የግንኙነት ችግር።")
        }
    }
    
    // Manual Logout
    fun logout() {
        storageManager.setAuthorized(false)
    }
}