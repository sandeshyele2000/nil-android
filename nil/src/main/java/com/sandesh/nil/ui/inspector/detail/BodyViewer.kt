/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.ui.inspector.detail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sandesh.nil.core.NIL
import com.sandesh.nil.utils.BodyPrettyPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val BODY_PREVIEW_MAX_CHARS = 1_200

@Composable
fun BodyViewer(
    body: String?,
    headers: String?
) {
    val payloadCharLimit = NIL.inspectorPayloadCharLimit()
    val prepared by produceState<PreparedBody?>(
        initialValue = null,
        key1 = body,
        key2 = headers,
        key3 = payloadCharLimit
    ) {
        value = withContext(Dispatchers.Default) {
            val sourceBody = body.orEmpty()
            val searchDisabled = sourceBody.length > payloadCharLimit
            val computedBody = if (searchDisabled) sourceBody else BodyPrettyPrinter.prettyPrint(body, headers).orEmpty()
            PreparedBody(
                body = computedBody,
                searchDisabled = searchDisabled,
                isTruncated = computedBody.length > BODY_PREVIEW_MAX_CHARS
            )
        }
    }
    val preparedBody = prepared ?: run {
        DetailLoadingState("Preparing body preview...")
        return
    }
    val prettyBody = preparedBody.body
    val searchDisabled = preparedBody.searchDisabled
    val isTruncated = preparedBody.isTruncated

    if (prettyBody.isBlank()) {
        DetailEmptyState(label = "No body available")
        return
    }

    val preview = prettyBody.take(BODY_PREVIEW_MAX_CHARS)
    Text(
        text = preview,
        style = MaterialTheme.typography.bodySmall
    )

    if (isTruncated) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (searchDisabled) {
                "Preview truncated. Search is unavailable for large payloads. Use Share to export the full payload."
            } else {
                "Preview truncated. Use Search to inspect the full payload."
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class PreparedBody(
    val body: String,
    val searchDisabled: Boolean,
    val isTruncated: Boolean
)
