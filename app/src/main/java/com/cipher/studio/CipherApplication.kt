package com.cipher.studio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CipherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase initialization is handled automatically via google-services.json
    }
}