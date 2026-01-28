package com.cipher.studio.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SessionEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(CipherTypeConverters::class)
abstract class CipherDatabase : RoomDatabase() {
    abstract fun cipherDao(): CipherDao
}