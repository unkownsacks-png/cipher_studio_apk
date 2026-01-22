package com.cipher.studio.di

import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Provides the Firestore instance with ADVANCED Offline Persistence enabled.
     * This matches your TypeScript logic: 'persistentLocalCache'.
     *
     * It allows the app to load data instantly from disk before network calls finish.
     */
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val db = Firebase.firestore

        // Translating: initializeFirestore(app, { localCache: persistentLocalCache(...) })
        // We explicitly enable persistent cache settings for robust offline support.
        val settings = firestoreSettings {
            setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
        }
        db.firestoreSettings = settings
        
        return db
    }

    /**
     * Provides the FirebaseAuth instance.
     * Maps to: export const auth = getAuth(app);
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return Firebase.auth
    }

    /**
     * Provides FirebaseAnalytics.
     * Maps to: export const analytics = ... getAnalytics(app) ...
     * Note: Android automatically handles the context safely.
     */
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(): FirebaseAnalytics {
        return Firebase.analytics
    }
}