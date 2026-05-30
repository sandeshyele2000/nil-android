package com.sandesh.nil.ui.inspector.json

import com.sandesh.nil.utils.json.JsonTreeBuilder

internal const val ROOT_PATH = "root"

internal object JsonTreeDocument {
    fun buildVisibleRows(
        root: JsonNode,
        expandedPaths: Set<String>,
        loadingPaths: Set<String>
    ): List<JsonRenderRow> {
        val rows = mutableListOf<JsonRenderRow>()
        appendVisibleRows(
            node = root,
            path = ROOT_PATH,
            depth = 0,
            expandedPaths = expandedPaths,
            rows = rows,
            loadingPaths = loadingPaths
        )
        return rows
    }

    fun buildSearchEntries(root: JsonNode): List<JsonSearchEntry> {
        val entries = mutableListOf<JsonSearchEntry>()
        appendSearchEntries(
            node = root,
            path = ROOT_PATH,
            depth = 0,
            ancestorPaths = emptySet(),
            entries = entries
        )
        return entries
    }

    fun findMatches(
        entries: List<JsonSearchEntry>,
        query: String,
        pathSearchMode: Boolean
    ): List<JsonSearchEntry> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return emptyList()

        return entries.filter { entry ->
            if (pathSearchMode) {
                entry.normalizedPath.contains(trimmedQuery, ignoreCase = true)
            } else {
                val tokens = trimmedQuery
                    .split(Regex("\\s+"))
                    .map(String::trim)
                    .filter(String::isNotEmpty)

                tokens.isNotEmpty() && tokens.all { token ->
                    val normalizedToken = token.removePrefix("path:").removePrefix("p:")
                    if (token.startsWith("path:", ignoreCase = true) || token.startsWith("p:", ignoreCase = true)) {
                        entry.normalizedPath.contains(normalizedToken, ignoreCase = true)
                    } else {
                        entry.searchText.contains(token, ignoreCase = true) ||
                            entry.normalizedPath.contains(token, ignoreCase = true)
                    }
                }
            }
        }
    }

    fun findNodeAtPath(root: JsonNode, path: String): JsonNode? {
        if (path == ROOT_PATH) return root
        val segments = parsePathSegments(path)
        var current: JsonNode = root
        segments.forEach { segment ->
            current = when {
                current is JsonNode.ObjectNode && segment is PathSegment.Key -> current.children[segment.value] ?: return null
                current is JsonNode.ArrayNode && segment is PathSegment.Index && segment.value in current.items.indices -> current.items[segment.value]
                else -> return null
            }
        }
        return current
    }

    fun expandNodeAtPath(root: JsonNode, path: String): JsonNode {
        if (path == ROOT_PATH) return JsonTreeBuilder.expand(root)
        return replaceNodeAtPath(root, parsePathSegments(path), 0)
    }

    fun isLazyContainer(node: JsonNode): Boolean = when (node) {
        is JsonNode.ObjectNode -> node.rawJson != null
        is JsonNode.ArrayNode -> node.rawJson != null
        is JsonNode.ValueNode -> false
    }

    private fun appendVisibleRows(
        node: JsonNode,
        path: String,
        depth: Int,
        expandedPaths: Set<String>,
        rows: MutableList<JsonRenderRow>,
        loadingPaths: Set<String>,
        fallbackKey: String? = null
    ) {
        when (node) {
            is JsonNode.ObjectNode -> {
                rows += JsonRenderRow(
                    path = path,
                    depth = depth,
                    type = JsonRowType.ObjectOpen,
                    key = displayKey(node.key ?: fallbackKey, depth),
                    value = null
                )
                if (path !in expandedPaths) return
                if (path in loadingPaths) {
                    rows += JsonRenderRow(
                        path = "$path#loading",
                        depth = depth + 1,
                        type = JsonRowType.Loading,
                        key = null,
                        value = "Loading..."
                    )
                    return
                }
                node.children.forEach { (childKey, child) ->
                    appendVisibleRows(
                        node = child,
                        path = appendKeyPath(path, childKey),
                        depth = depth + 1,
                        expandedPaths = expandedPaths,
                        rows = rows,
                        loadingPaths = loadingPaths,
                        fallbackKey = childKey
                    )
                }
                rows += JsonRenderRow(
                    path = "$path#close",
                    depth = depth,
                    type = JsonRowType.ObjectClose,
                    key = null,
                    value = "}"
                )
            }

            is JsonNode.ArrayNode -> {
                rows += JsonRenderRow(
                    path = path,
                    depth = depth,
                    type = JsonRowType.ArrayOpen,
                    key = displayKey(node.key ?: fallbackKey, depth),
                    value = null
                )
                if (path !in expandedPaths) return
                if (path in loadingPaths) {
                    rows += JsonRenderRow(
                        path = "$path#loading",
                        depth = depth + 1,
                        type = JsonRowType.Loading,
                        key = null,
                        value = "Loading..."
                    )
                    return
                }
                node.items.forEachIndexed { index, child ->
                    appendVisibleRows(
                        node = child,
                        path = appendIndexPath(path, index),
                        depth = depth + 1,
                        expandedPaths = expandedPaths,
                        rows = rows,
                        loadingPaths = loadingPaths,
                        fallbackKey = "[$index]"
                    )
                }
                rows += JsonRenderRow(
                    path = "$path#close",
                    depth = depth,
                    type = JsonRowType.ArrayClose,
                    key = null,
                    value = "]"
                )
            }

            is JsonNode.ValueNode -> {
                rows += JsonRenderRow(
                    path = path,
                    depth = depth,
                    type = JsonRowType.Value,
                    key = displayKey(node.key ?: fallbackKey, depth),
                    value = node.value
                )
            }
        }
    }

    private fun appendSearchEntries(
        node: JsonNode,
        path: String,
        depth: Int,
        ancestorPaths: Set<String>,
        entries: MutableList<JsonSearchEntry>,
        fallbackKey: String? = null
    ) {
        when (node) {
            is JsonNode.ObjectNode -> {
                val key = displayKey(node.key ?: fallbackKey, depth)
                entries += JsonSearchEntry(path, normalizePath(path), "$key {", ancestorPaths + path)
                node.children.forEach { (childKey, child) ->
                    appendSearchEntries(
                        node = child,
                        path = appendKeyPath(path, childKey),
                        depth = depth + 1,
                        ancestorPaths = ancestorPaths + path,
                        entries = entries,
                        fallbackKey = childKey
                    )
                }
            }

            is JsonNode.ArrayNode -> {
                val key = displayKey(node.key ?: fallbackKey, depth)
                entries += JsonSearchEntry(path, normalizePath(path), "$key [", ancestorPaths + path)
                node.items.forEachIndexed { index, child ->
                    appendSearchEntries(
                        node = child,
                        path = appendIndexPath(path, index),
                        depth = depth + 1,
                        ancestorPaths = ancestorPaths + path,
                        entries = entries,
                        fallbackKey = "[$index]"
                    )
                }
            }

            is JsonNode.ValueNode -> {
                val key = displayKey(node.key ?: fallbackKey, depth)
                entries += JsonSearchEntry(path, normalizePath(path), "$key ${node.value}", ancestorPaths)
            }
        }
    }

    private fun replaceNodeAtPath(
        node: JsonNode,
        segments: List<PathSegment>,
        depth: Int
    ): JsonNode {
        if (depth >= segments.size) {
            return JsonTreeBuilder.expand(node)
        }

        return when (node) {
            is JsonNode.ObjectNode -> {
                val segment = segments[depth] as? PathSegment.Key ?: return node
                val child = node.children[segment.value] ?: return node
                node.copy(
                    children = node.children.toMutableMap().apply {
                        put(segment.value, replaceNodeAtPath(child, segments, depth + 1))
                    }
                )
            }

            is JsonNode.ArrayNode -> {
                val segment = segments[depth] as? PathSegment.Index ?: return node
                if (segment.value !in node.items.indices) return node
                node.copy(
                    items = node.items.toMutableList().apply {
                        this[segment.value] = replaceNodeAtPath(node.items[segment.value], segments, depth + 1)
                    }
                )
            }

            is JsonNode.ValueNode -> node
        }
    }

    private fun parsePathSegments(path: String): List<PathSegment> {
        val normalized = path.removePrefix(ROOT_PATH)
        if (normalized.isBlank()) return emptyList()

        val segments = mutableListOf<PathSegment>()
        var cursor = 0
        while (cursor < normalized.length) {
            when {
                normalized[cursor] == '.' -> {
                    cursor += 1
                    val end = normalized.indexOfAny(charArrayOf('.', '['), startIndex = cursor)
                        .takeIf { it >= 0 } ?: normalized.length
                    segments += PathSegment.Key(normalized.substring(cursor, end))
                    cursor = end
                }

                normalized[cursor] == '[' && cursor + 1 < normalized.length && normalized[cursor + 1] == '"' -> {
                    val parsed = parseQuotedKeySegment(normalized, cursor)
                    segments += PathSegment.Key(parsed.value)
                    cursor = parsed.nextCursor
                }

                normalized[cursor] == '[' -> {
                    val end = normalized.indexOf(']', startIndex = cursor)
                    if (end <= cursor) break
                    val index = normalized.substring(cursor + 1, end).toIntOrNull() ?: break
                    segments += PathSegment.Index(index)
                    cursor = end + 1
                }

                else -> break
            }
        }
        return segments
    }

    private fun parseQuotedKeySegment(path: String, start: Int): ParsedKeySegment {
        val builder = StringBuilder()
        var cursor = start + 2
        while (cursor < path.length) {
            val current = path[cursor]
            when {
                current == '\\' && cursor + 1 < path.length -> {
                    builder.append(path[cursor + 1])
                    cursor += 2
                }

                current == '"' && cursor + 1 < path.length && path[cursor + 1] == ']' -> {
                    return ParsedKeySegment(
                        value = builder.toString(),
                        nextCursor = cursor + 2
                    )
                }

                else -> {
                    builder.append(current)
                    cursor += 1
                }
            }
        }
        throw IllegalArgumentException("Malformed quoted JSON path segment: $path")
    }

    private fun appendKeyPath(parentPath: String, key: String): String {
        return if (SIMPLE_KEY_REGEX.matches(key)) {
            "$parentPath.$key"
        } else {
            "$parentPath[\"${escapeKey(key)}\"]"
        }
    }

    private fun appendIndexPath(parentPath: String, index: Int): String = "$parentPath[$index]"

    private fun escapeKey(key: String): String = buildString(key.length) {
        key.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                else -> append(ch)
            }
        }
    }

    private fun normalizePath(path: String): String = path.removePrefix(ROOT_PATH)

    private fun displayKey(key: String?, depth: Int): String? {
        if (depth == 0 && key.isNullOrBlank()) return null
        return key ?: ""
    }

    private data class ParsedKeySegment(
        val value: String,
        val nextCursor: Int
    )

    private sealed interface PathSegment {
        data class Key(val value: String) : PathSegment
        data class Index(val value: Int) : PathSegment
    }

    private val SIMPLE_KEY_REGEX = Regex("[A-Za-z_][A-Za-z0-9_]*")
}

internal data class JsonSearchEntry(
    val path: String,
    val normalizedPath: String,
    val searchText: String,
    val expansionPaths: Set<String>
)

internal data class JsonRenderRow(
    val path: String,
    val depth: Int,
    val type: JsonRowType,
    val key: String?,
    val value: String?
) {
    val isExpandable: Boolean = type == JsonRowType.ObjectOpen || type == JsonRowType.ArrayOpen
}

internal enum class JsonRowType {
    ObjectOpen,
    ObjectClose,
    ArrayOpen,
    ArrayClose,
    Value,
    Loading
}
