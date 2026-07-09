package com.bastion.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Host::class, AppSettings::class, SshKey::class, ApiKey::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hostDao(): HostDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun sshKeyDao(): SshKeyDao
    abstract fun apiKeyDao(): ApiKeyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_settings (
                        id INTEGER NOT NULL PRIMARY KEY,
                        serverName TEXT NOT NULL DEFAULT 'BASTION-PRIME-01',
                        timezone TEXT NOT NULL DEFAULT 'EST (Eastern Standard Time)',
                        language TEXT NOT NULL DEFAULT 'English (United States)',
                        fontSize REAL NOT NULL DEFAULT 14.0,
                        twoFactorEnabled INTEGER NOT NULL DEFAULT 1,
                        sessionTimeout TEXT NOT NULL DEFAULT '30 Minutes',
                        webhookUrl TEXT NOT NULL DEFAULT '',
                        emailAlerts INTEGER NOT NULL DEFAULT 0,
                        colorMode TEXT NOT NULL DEFAULT 'DARK'
                    )
                """)
                db.execSQL("INSERT OR IGNORE INTO app_settings (id) VALUES (1)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ssh_keys (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        fingerprint TEXT NOT NULL,
                        servers TEXT NOT NULL DEFAULT '',
                        created INTEGER NOT NULL,
                        lastUsed INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS api_keys (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL,
                        keyValue TEXT NOT NULL,
                        created INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE app_settings ADD COLUMN skippedUpdateVersion TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bastion_vault.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
