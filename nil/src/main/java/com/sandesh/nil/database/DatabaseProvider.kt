/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.database

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE network_events ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Volatile
    private var database: NILDatabase? = null

    fun getDatabase(context: Context): NILDatabase {
        return database?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                NILDatabase::class.java,
                "nil_database"
            )
                .addMigrations(migration1To2)
                .build()

            database = instance

            instance
        }
    }
}
