package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        UserEntity::class,
        TrustRelationshipEntity::class,
        ChatMessageEntity::class,
        LocalServiceEntity::class,
        ReportEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GeoConnectDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun trustDao(): TrustDao
    abstract fun chatDao(): ChatDao
    abstract fun serviceDao(): ServiceDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: GeoConnectDatabase? = null

        fun getDatabase(context: Context): GeoConnectDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GeoConnectDatabase::class.java,
                    "geoconnect_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
