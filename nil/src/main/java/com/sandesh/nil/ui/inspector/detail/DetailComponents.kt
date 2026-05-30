/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.ui.inspector.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SectionActionButtons(
    onSearch: (() -> Unit)?,
    searchEnabled: Boolean = true,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (onSearch == null && onShare == null) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (onSearch != null) {
            OutlinedButton(
                onClick = onSearch,
                enabled = searchEnabled
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ManageSearch,
                    contentDescription = "Search",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Search")
            }
        }
        if (onShare != null) {
            OutlinedButton(onClick = onShare) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "Share",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share")
            }
        }
    }
}

@Composable
internal fun KeyValueList(
    pairs: List<DetailPair>,
    emptyLabel: String
) {
    if (pairs.isEmpty()) {
        DetailEmptyState(label = emptyLabel)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        pairs.forEach { pair ->
            Surface(
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = pair.key,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(0.38f, fill = false)
                    )
                    Text(
                        text = pair.value.ifBlank { " " },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.62f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun DetailEmptyState(label: String) {
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
        )
    }
}

@Composable
internal fun DetailLoadingState(label: String = "Loading...") {
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal data class DetailPair(
    val key: String,
    val value: String
)

internal fun toKeyValueText(pairs: List<DetailPair>): String {
    if (pairs.isEmpty()) return ""
    return pairs.joinToString(separator = "\n") { pair ->
        "${pair.key}: ${pair.value}"
    }
}

@Composable
internal fun SectionContent(
    onSearch: (() -> Unit)?,
    searchEnabled: Boolean = true,
    onShare: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    SectionActionButtons(
        onSearch = onSearch,
        searchEnabled = searchEnabled,
        onShare = onShare
    )
    if (onSearch != null || onShare != null) {
        Spacer(modifier = Modifier.height(8.dp))
    }
    content()
}
