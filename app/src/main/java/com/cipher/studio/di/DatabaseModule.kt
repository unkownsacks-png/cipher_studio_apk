package com.cipher.studio.di

import android.content.Context
import androidx.room.Room
import com.cipher.studio.data.local.room.CipherDao
import com.cipher.studio.data.local.room.CipherDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CipherDatabase {
        return Room.databaseBuilder(
            context,
            CipherDatabase::class.java,
            "cipher_elite_db" // The actual file name on disk
        )
        .fallbackToDestructiveMigration() // If schema changes, recreate db (Safe for dev)
        .build()
    }

    @Provides
    fun provideDao(database: CipherDatabase): CipherDao {
        return database.cipherDao()
    }
}