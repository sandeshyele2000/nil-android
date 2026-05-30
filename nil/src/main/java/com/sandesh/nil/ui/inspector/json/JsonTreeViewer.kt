/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.ui.inspector.json

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sandesh.nil.ui.theme.NILColors
import com.sandesh.nil.utils.json.JsonTreeBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val JSON_SEARCH_DEBOUNCE_MS = 180L

@Composable
fun JsonTreeViewer(
    json: String?,
    query: String = "",
    pathSearchMode: Boolean = false,
    activeMatchIndex: Int = 0,
    onMatchCountChanged: ((Int) -> Unit)? = null,
    enableInternalScroll: Boolean = false,
    forceLazyMode: Boolean = false
) {
    if (json.isNullOrBlank()) {
        Text("No JSON payload", style = MaterialTheme.typography.bodySmall)
        return
    }

    val documentState by produceState<JsonDocumentState>(
        initialValue = JsonDocumentState.Loading,
        key1 = json,
        key2 = forceLazyMode
    ) {
        value = JsonDocumentState.Loading
        value = withContext(Dispatchers.Default) {
            val root = if (forceLazyMode) {
                JsonTreeBuilder.buildLazy(json)
            } else {
                JsonTreeBuilder.build(json)
            }
            if (root == null) {
                JsonDocumentState.Error("Unable to parse JSON payload")
            } else {
                JsonDocumentState.Ready(
                    root = root,
                    searchEntries = if (forceLazyMode) emptyList() else JsonTreeDocument.buildSearchEntries(root)
                )
            }
        }
    }

    when (val state = documentState) {
        JsonDocumentState.Loading -> LoadingBanner("Preparing JSON tree...")
        is JsonDocumentState.Error -> Text(state.message, style = MaterialTheme.typography.bodySmall)
        is JsonDocumentState.Ready -> JsonTreeContent(
            initialRoot = state.root,
            searchEntries = state.searchEntries,
            query = query,
            pathSearchMode = pathSearchMode,
            activeMatchIndex = activeMatchIndex,
            onMatchCountChanged = onMatchCountChanged,
            enableInternalScroll = enableInternalScroll,
            json = json,
            forceLazyMode = forceLazyMode
        )
    }
}

@Composable
private fun JsonTreeContent(
    initialRoot: JsonNode,
    searchEntries: List<JsonSearchEntry>,
    query: String,
    pathSearchMode: Boolean,
    activeMatchIndex: Int,
    onMatchCountChanged: ((Int) -> Unit)?,
    enableInternalScroll: Boolean,
    json: String,
    forceLazyMode: Boolean
) {
    val scope = rememberCoroutineScope()
    val expanded = remember(json) {
        mutableStateMapOf<String, Boolean>().apply { put(ROOT_PATH, true) }
    }
    val loadingPaths = remember(json) { mutableStateMapOf<String, Boolean>() }
    var rootNode by remember(json, initialRoot) { mutableStateOf(initialRoot) }

    val manualExpandedPaths = remember(expanded.toMap()) {
        expanded.filterValues { it }.keys
    }

    val searchState by produceState<JsonSearchState>(
        initialValue = JsonSearchState.Idle,
        searchEntries,
        query,
        pathSearchMode,
        forceLazyMode
    ) {
        if (forceLazyMode || query.isBlank()) {
            value = JsonSearchState.Idle
            return@produceState
        }

        value = JsonSearchState.Loading
        delay(JSON_SEARCH_DEBOUNCE_MS)
        value = withContext(Dispatchers.Default) {
            JsonSearchState.Ready(
                JsonTreeDocument.findMatches(
                    entries = searchEntries,
                    query = query,
                    pathSearchMode = pathSearchMode
                )
            )
        }
    }

    val activeMatch = (searchState as? JsonSearchState.Ready)
        ?.matches
        ?.getOrNull(activeMatchIndex.coerceAtLeast(0))
    val autoExpandedPaths = activeMatch?.expansionPaths.orEmpty()
    val effectiveExpandedPaths = remember(manualExpandedPaths, autoExpandedPaths) {
        (manualExpandedPaths + autoExpandedPaths).toSet()
    }

    val visibleRows by produceState<List<JsonRenderRow>>(
        initialValue = emptyList(),
        key1 = rootNode,
        key2 = effectiveExpandedPaths,
        key3 = loadingPaths.keys.toSet()
    ) {
        value = withContext(Dispatchers.Default) {
            JsonTreeDocument.buildVisibleRows(
                root = rootNode,
                expandedPaths = effectiveExpandedPaths,
                loadingPaths = loadingPaths.keys
            )
        }
    }

    val matchedPaths = ((searchState as? JsonSearchState.Ready)?.matches ?: emptyList())
        .map(JsonSearchEntry::path)
        .toSet()
    LaunchedEffect(searchState) {
        onMatchCountChanged?.invoke((searchState as? JsonSearchState.Ready)?.matches?.size ?: 0)
    }

    val activePath = activeMatch?.path
    val activeItemIndex = remember(visibleRows, activePath) {
        visibleRows.indexOfFirst { it.path == activePath }.takeIf { it >= 0 }
    }
    val listState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()

    LaunchedEffect(activeItemIndex, enableInternalScroll) {
        if (enableInternalScroll && activeItemIndex != null) {
            listState.animateScrollToItem(activeItemIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        when (searchState) {
            JsonSearchState.Loading -> LoadingBanner("Searching JSON...")
            JsonSearchState.Idle,
            is JsonSearchState.Ready -> Unit
        }
        if (loadingPaths.isNotEmpty()) {
            LoadingBanner("Loading JSON branch...")
        }

        Box(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
            if (enableInternalScroll) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth()
                ) {
                    itemsIndexed(visibleRows, key = { _, item -> item.path }) { index, row ->
                        JsonRow(
                            row = row,
                            isMatch = row.path in matchedPaths,
                            isActive = activeItemIndex == index,
                            isExpanded = row.path in effectiveExpandedPaths,
                            onToggle = { path ->
                                handleToggle(
                                    path = path,
                                    expanded = expanded,
                                    loadingPaths = loadingPaths,
                                    forceLazyMode = forceLazyMode,
                                    rootNode = rootNode,
                                    updateRoot = { rootNode = it },
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth()
                ) {
                    visibleRows.forEachIndexed { index, row ->
                        JsonRow(
                            row = row,
                            isMatch = row.path in matchedPaths,
                            isActive = activeItemIndex == index,
                            isExpanded = row.path in effectiveExpandedPaths,
                            onToggle = { path ->
                                handleToggle(
                                    path = path,
                                    expanded = expanded,
                                    loadingPaths = loadingPaths,
                                    forceLazyMode = forceLazyMode,
                                    rootNode = rootNode,
                                    updateRoot = { rootNode = it },
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun handleToggle(
    path: String,
    expanded: MutableMap<String, Boolean>,
    loadingPaths: MutableMap<String, Boolean>,
    forceLazyMode: Boolean,
    rootNode: JsonNode,
    updateRoot: (JsonNode) -> Unit,
    scope: CoroutineScope
) {
    if (expanded[path] == true) {
        expanded[path] = false
        return
    }

    if (!forceLazyMode) {
        expanded[path] = true
        return
    }

    val targetNode = JsonTreeDocument.findNodeAtPath(rootNode, path)
    if (targetNode == null || !JsonTreeDocument.isLazyContainer(targetNode)) {
        expanded[path] = true
        return
    }

    loadingPaths[path] = true
    scope.launch {
        val updatedRoot = withContext(Dispatchers.Default) {
            JsonTreeDocument.expandNodeAtPath(rootNode, path)
        }
        updateRoot(updatedRoot)
        expanded[path] = true
        loadingPaths.remove(path)
    }
}

@Composable
private fun LoadingBanner(label: String) {
    Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun JsonRow(
    row: JsonRenderRow,
    isMatch: Boolean,
    isActive: Boolean,
    isExpanded: Boolean,
    onToggle: (String) -> Unit
) {
    val background = when {
        isActive -> NILColors.jsonActiveMatch()
        isMatch -> NILColors.jsonMatch()
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(6.dp))
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .animateContentSize()
            .clickable(enabled = row.isExpandable) { onToggle(row.path) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width((row.depth * 12).dp))
        when (row.type) {
            JsonRowType.ObjectOpen -> ExpandableLabel(
                isExpanded = isExpanded,
                text = objectOrArrayLabel(row.key, "{", isExpanded)
            )

            JsonRowType.ArrayOpen -> ExpandableLabel(
                isExpanded = isExpanded,
                text = objectOrArrayLabel(row.key, "[", isExpanded)
            )

            JsonRowType.ObjectClose,
            JsonRowType.ArrayClose,
            JsonRowType.Loading,
            JsonRowType.Value -> {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = when (row.type) {
                        JsonRowType.Value -> valueLabel(row.key, row.value.orEmpty())
                        else -> buildAnnotatedString { append(row.value.orEmpty()) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpandableLabel(isExpanded: Boolean, text: AnnotatedString) {
    Icon(
        imageVector = if (isExpanded) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(text = text, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun objectOrArrayLabel(key: String?, openingBrace: String, isExpanded: Boolean) = buildAnnotatedString {
    if (!key.isNullOrBlank()) {
        withStyle(SpanStyle(color = NILColors.jsonKey())) { append("\"$key\"") }
        append(": ")
    }
    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
        append(openingBrace)
        if (!isExpanded) {
            append(" ... ")
            append(if (openingBrace == "{") "}" else "]")
        }
    }
}

@Composable
private fun valueLabel(key: String?, value: String) = buildAnnotatedString {
    if (!key.isNullOrBlank()) {
        withStyle(SpanStyle(color = NILColors.jsonKey())) { append("\"$key\"") }
        append(": ")
    }
    withStyle(SpanStyle(color = valueColor(value))) {
        append(value)
    }
}

@Composable
private fun valueColor(value: String) = when {
    value == "true" || value == "false" || value == "null" -> NILColors.jsonBoolNull()
    value.toDoubleOrNull() != null -> NILColors.jsonNumber()
    else -> NILColors.jsonString()
}

private sealed interface JsonDocumentState {
    data object Loading : JsonDocumentState
    data class Ready(
        val root: JsonNode,
        val searchEntries: List<JsonSearchEntry>
    ) : JsonDocumentState
    data class Error(val message: String) : JsonDocumentState
}

private sealed interface JsonSearchState {
    data object Idle : JsonSearchState
    data object Loading : JsonSearchState
    data class Ready(val matches: List<JsonSearchEntry>) : JsonSearchState
}
