/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.ui.inspector.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import com.sandesh.nil.core.NIL
import com.sandesh.nil.model.NetworkEventSummary
import com.sandesh.nil.ui.components.NILSearchBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val EVENT_LIST_FILTER_DEBOUNCE_MS = 250L

private enum class StatusFilter(val label: String) {
    ALL("All"),
    STATUS_2XX("2xx"),
    STATUS_3XX("3xx"),
    STATUS_4XX("4xx"),
    STATUS_5XX("5xx"),
    ERROR("Errors")
}

@Composable
fun EventListScreen(
    events: List<NetworkEventSummary>,
    onClick: (NetworkEventSummary) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf(StatusFilter.ALL) }
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    val paused by NIL.isLoggingPaused.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        delay(EVENT_LIST_FILTER_DEBOUNCE_MS)
        NIL.setFilter(query)
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter by status code", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusFilter.entries.forEach { filter ->
                        Text(
                            text = if (statusFilter == filter) "• ${filter.label}" else filter.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (statusFilter == filter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { statusFilter = filter }
                                .padding(vertical = 2.dp)
                                .background(
                                    color = if (statusFilter == filter) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) { Text("Done") }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all events?", style = MaterialTheme.typography.titleMedium) },
            text = { Text("This will delete all unpinned network events. Pinned events are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    scope.launch { NIL.clearEvents() }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }

    val filteredByStatus = remember(events, statusFilter) {
        events.filter { event ->
            when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.STATUS_2XX -> (event.statusCode ?: -1) in 200..299
                StatusFilter.STATUS_3XX -> (event.statusCode ?: -1) in 300..399
                StatusFilter.STATUS_4XX -> (event.statusCode ?: -1) in 400..499
                StatusFilter.STATUS_5XX -> (event.statusCode ?: -1) in 500..599
                StatusFilter.ERROR -> event.statusCode == null || event.statusCode >= 400
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "Network Events",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Row {
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(
                        Icons.Filled.FilterAlt,
                        contentDescription = "Filter",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    if (paused) NIL.resumeLogging() else NIL.pauseLogging()
                }) {
                    Icon(
                        imageVector = if (paused) Icons.Filled.PlayCircle else Icons.Filled.PauseCircle,
                        contentDescription = if (paused) "Resume" else "Pause",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(
                        Icons.Filled.DeleteSweep,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            NILSearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search URL / method",
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Filter: ${statusFilter.label}" + if (paused) " • Paused" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredByStatus, key = { it.id }) { event ->
                    EventListRow(
                        event = event,
                        onClick = onClick,
                        onPinToggle = { selected ->
                            NIL.setEventPinned(selected.id, !selected.pinned)
                        }
                    )
                }
            }
        }
    }
}
