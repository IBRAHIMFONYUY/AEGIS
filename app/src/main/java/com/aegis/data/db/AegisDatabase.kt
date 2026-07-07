package com.aegis.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aegis.data.db.dao.*
import com.aegis.data.db.entity.*
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        ThreatEvent::class,
        SafetyScore::class,
        LearningProgress::class,
        AppSettings::class,
        MemoryEntry::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AegisDatabase : RoomDatabase() {

    abstract fun threatDao(): ThreatDao
    abstract fun safetyScoreDao(): SafetyScoreDao
    abstract fun learningDao(): LearningDao
    abstract fun settingsDao(): SettingsDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        init {
            System.loadLibrary("sqlcipher")
        }

        private const val DB_NAME = "aegis_secure.db"

        @Volatile
        private var INSTANCE: AegisDatabase? = null

        fun getInstance(context: Context, passphrase: ByteArray): AegisDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, passphrase).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context, passphrase: ByteArray): AegisDatabase {
            val factory = SupportOpenHelperFactory(passphrase, object : net.zetetic.database.sqlcipher.SQLiteDatabaseHook {
                override fun preKey(connection: net.zetetic.database.sqlcipher.SQLiteConnection?) {}
                override fun postKey(connection: net.zetetic.database.sqlcipher.SQLiteConnection?) {
                    connection?.executeRaw("PRAGMA cipher_compatibility = 3;", null, null)
                }
            }, true)
            return Room.databaseBuilder(
                context.applicationContext,
                AegisDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
