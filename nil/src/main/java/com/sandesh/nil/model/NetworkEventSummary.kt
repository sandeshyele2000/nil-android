package com.sandesh.nil.model

data class NetworkEventSummary(
    val id: String,
    val url: String,
    val method: String,
    val statusCode: Int?,
    val durationMs: Long,
    val timestamp: Long,
    val pinned: Boolean
)
