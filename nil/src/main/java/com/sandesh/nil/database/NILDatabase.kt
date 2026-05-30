/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sandesh.nil.model.NetworkEvent

@Database(
    entities = [
        NetworkEvent::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NILDatabase: RoomDatabase() {
    abstract fun networkEventDao(): NetworkEventDao
}
