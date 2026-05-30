/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "network_events")
data class NetworkEvent(

    @PrimaryKey
    var id: String,

    val url: String,

    val method: String,

    val requestHeaders: String?,

    val requestBody: String?,

    val responseHeaders: String?,

    val responseBody: String?,

    val statusCode: Int?,

    val durationMs: Long,

    val timestamp: Long,

    val pinned: Boolean = false
)
