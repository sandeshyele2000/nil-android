/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.interceptor

import com.sandesh.nil.core.NIL
import com.sandesh.nil.model.NetworkEvent
import com.sandesh.nil.storage.NILRepository
import java.util.UUID
import java.util.Locale

internal data class CaptureResult(
    val responseHeaders: String?,
    val responseBody: String?,
    val statusCode: Int?
)

internal inline fun <T> captureNetworkEvent(
    url: String,
    method: String,
    requestHeaders: String?,
    requestBody: String?,
    execute: () -> T,
    onSuccess: (T) -> CaptureResult,
    onFailure: (Throwable) -> CaptureResult
): T {
    if (!NIL.shouldLogEvents()) {
        return execute()
    }

    val requestStartedAt = System.currentTimeMillis()
    val result = try {
        execute()
    } catch (throwable: Throwable) {
        val failure = onFailure(throwable)
        val failedEvent = NetworkEvent(
            id = UUID.randomUUID().toString(),
            url = url,
            method = method,
            requestHeaders = requestHeaders.limitForStorage(),
            requestBody = requestBody.limitForStorage(),
            responseHeaders = failure.responseHeaders.limitForStorage(),
            responseBody = failure.responseBody.limitForStorage(),
            statusCode = failure.statusCode,
            durationMs = System.currentTimeMillis() - requestStartedAt,
            timestamp = requestStartedAt
        )
        NILRepository.addEvent(failedEvent)
        throw throwable
    }

    val success = onSuccess(result)
    val completedEvent = NetworkEvent(
        id = UUID.randomUUID().toString(),
        url = url,
        method = method,
        requestHeaders = requestHeaders.limitForStorage(),
        requestBody = requestBody.limitForStorage(),
        responseHeaders = success.responseHeaders.limitForStorage(),
        responseBody = success.responseBody.limitForStorage(),
        statusCode = success.statusCode,
        durationMs = System.currentTimeMillis() - requestStartedAt,
        timestamp = requestStartedAt
    )
    NILRepository.addEvent(completedEvent)
    return result
}

private fun String?.limitForStorage(): String? {
    val value = this ?: return null
    val maxPersistedTextChars = NIL.inspectorPayloadCharLimit()
    if (value.length <= maxPersistedTextChars) return value

    val omittedChars = value.length - maxPersistedTextChars
    return buildString(maxPersistedTextChars + 96) {
        append(value, 0, maxPersistedTextChars)
        append("\n\n[truncated by NIL to keep the database row readable; ")
        append(formatCharCount(omittedChars))
        append(" chars omitted]")
    }
}

private fun formatCharCount(value: Int): String = String.format(Locale.US, "%,d", value)
