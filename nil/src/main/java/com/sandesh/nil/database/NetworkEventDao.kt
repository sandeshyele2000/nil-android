/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sandesh.nil.model.NetworkEvent
import com.sandesh.nil.model.NetworkEventSummary
import kotlinx.coroutines.flow.Flow


@Dao
interface NetworkEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(networkEvent: NetworkEvent)

    @Query("""
        SELECT
            id,
            url,
            method,
            statusCode,
            durationMs,
            timestamp,
            pinned
        FROM network_events
        ORDER BY timestamp DESC
    """)
    fun observeAll(): Flow<List<NetworkEventSummary>>

    @Query(
        """
        SELECT
            id,
            url,
            method,
            statusCode,
            durationMs,
            timestamp,
            pinned
        FROM network_events
        WHERE url LIKE '%' || :query || '%'
           OR method LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
        """
    )
    fun observeByQuery(query: String): Flow<List<NetworkEventSummary>>

    @Query("DELETE FROM network_events WHERE pinned = 0")
    suspend fun clear()

    @Query("UPDATE network_events SET pinned = :pinned WHERE id = :eventId")
    suspend fun setPinned(eventId: String, pinned: Boolean)

    @Query(
        """
        DELETE FROM network_events
        WHERE id IN (
            SELECT id
            FROM network_events
            ORDER BY timestamp DESC
            LIMIT -1 OFFSET :maxRows
        )
        """
    )
    suspend fun trimToLatest(maxRows: Int)

    @Query(
        """
        SELECT * FROM network_events
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getEventById(id: String): NetworkEvent?
}
