package com.cipher.studio.domain.repository

import android.content.Context
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ውጤቱን ለመግለፅ የምንጠቀምበት (Success or Error message)
sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val db: FirebaseFirestore,
    @ApplicationContext private val context: Context // Context ያስፈልገናል ለ Device ID
) {

    /**
     * Translating the logic from EliteAuth.tsx:
     * 1. Get Device ID
     * 2. Check Firestore User
     * 3. Validate Key & Payment
     * 4. Device Lock Check (First time vs Registered)
     */
    suspend fun verifyAccess(email: String, key: String): AuthResult {
        return try {
            val cleanEmail = email.lowercase().trim()
            if (cleanEmail.isEmpty() || key.isEmpty()) {
                return AuthResult.Error("እባክዎ መረጃዎን በትክክል ያስገቡ!")
            }

            // 1. የስልኩን ልዩ መለያ (Device ID) ማግኘት
            // Equiv to: Device.getId() -> uuid/identifier
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

                // 2. የቁልፍ እና የክፍያ ማረጋገጫ
                if (accessKey == key && isPaid) {
                    
                    // --- DEVICE LOCKING LOGIC ---
                    if (registeredDeviceId.isNullOrEmpty()) {
                        // ስልኩ ለመጀመሪያ ጊዜ ከሆነ፣ ID-ውን ይመዘግባል
                        // updateDoc(userRef, { deviceId: currentDeviceId })
                        userRef.update("deviceId", currentDeviceId).await()
                        AuthResult.Success
                    } else if (registeredDeviceId == currentDeviceId) {
                        // ስልኩ ቀድሞ ከተመዘገበው ጋር አንድ ከሆነ ያስገባል
                        AuthResult.Success
                    } else {
                        // ስልኩ የተለያየ ከሆነ ይከለክላል
                        AuthResult.Error("❌ Access Violation: ይህ ቁልፍ ቀድሞ በሌላ ስልክ ላይ ተይዟል! እባክዎ ባለቤቱን ያነጋግሩ።")
                    }

                } else {
                    AuthResult.Error("❌ Access Denied: የተሳሳተ ቁልፍ ወይም ያልተከፈለበት አካውንት።")
                }
            } else {
                AuthResult.Error("🔍 ተጠቃሚው አልተገኘም፡ እባክዎ መጀመሪያ ይመዝገቡ።")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AuthResult.Error("📡 የግንኙነት ችግር፡ እባክዎ ኢንተርኔትዎን ያረጋግጡ ወይም ደግመው ይሞክሩ።")
        }
    }
}