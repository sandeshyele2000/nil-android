/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.utils.json

import com.sandesh.nil.ui.inspector.json.JsonNode

object JsonTreeBuilder {
    fun build(input: String?): JsonNode? {
        val root = buildLazy(input) ?: return null
        return expandRecursively(root)
    }

    fun buildLazy(input: String?): JsonNode? {
        if (input.isNullOrBlank()) return null
        val trimmed = input.trim()
        return runCatching {
            when {
                trimmed.startsWith("{") -> parseLazyObject(trimmed, key = null)
                trimmed.startsWith("[") -> parseLazyArray(trimmed, key = null)
                else -> JsonNode.ValueNode(value = trimmed)
            }
        }.getOrElse {
            JsonNode.ValueNode(value = input)
        }
    }

    fun expand(node: JsonNode): JsonNode = when (node) {
        is JsonNode.ObjectNode -> {
            val rawJson = node.rawJson ?: return node
            parseLazyObject(rawJson, node.key, expanded = true)
        }

        is JsonNode.ArrayNode -> {
            val rawJson = node.rawJson ?: return node
            parseLazyArray(rawJson, node.key, expanded = true)
        }

        is JsonNode.ValueNode -> node
    }

    private fun parseLazyObject(rawJson: String, key: String?, expanded: Boolean = false): JsonNode.ObjectNode {
        val entries = parseTopLevelEntries(rawJson)
        val children = LinkedHashMap<String, JsonNode>(entries.size).apply {
            entries.forEach { entry ->
                put(entry.key.orEmpty(), createLazyNode(entry.key, entry.rawValue))
            }
        }
        return JsonNode.ObjectNode(
            key = key,
            children = children,
            rawJson = if (expanded) null else rawJson
        )
    }

    private fun parseLazyArray(rawJson: String, key: String?, expanded: Boolean = false): JsonNode.ArrayNode {
        val entries = parseTopLevelEntries(rawJson)
        val items = entries.mapIndexed { index, entry ->
            createLazyNode(entry.key ?: "[$index]", entry.rawValue)
        }
        return JsonNode.ArrayNode(
            key = key,
            items = items,
            rawJson = if (expanded) null else rawJson
        )
    }

    private fun createLazyNode(key: String?, rawValue: String): JsonNode {
        val trimmed = rawValue.trim()
        return when {
            trimmed.startsWith("{") -> JsonNode.ObjectNode(
                key = key,
                children = emptyMap(),
                rawJson = trimmed
            )

            trimmed.startsWith("[") -> JsonNode.ArrayNode(
                key = key,
                items = emptyList(),
                rawJson = trimmed
            )

            else -> JsonNode.ValueNode(key = key, value = normalizePrimitive(trimmed))
        }
    }

    private fun normalizePrimitive(rawValue: String): String = when {
        rawValue == "null" -> "null"
        rawValue == "true" || rawValue == "false" -> rawValue
        rawValue.startsWith("\"") && rawValue.endsWith("\"") && rawValue.length >= 2 -> decodeJsonStringLiteral(rawValue)

        else -> rawValue
    }

    private fun parseTopLevelEntries(rawJson: String): List<TopLevelEntry> {
        val trimmed = rawJson.trim()
        if (trimmed.length < 2) return emptyList()
        return when (trimmed.first()) {
            '{' -> parseObjectEntries(trimmed)
            '[' -> parseArrayEntries(trimmed)
            else -> emptyList()
        }
    }

    private fun parseObjectEntries(rawJson: String): List<TopLevelEntry> {
        val entries = mutableListOf<TopLevelEntry>()
        var cursor = skipWhitespace(rawJson, 1)
        while (cursor < rawJson.lastIndex) {
            if (rawJson[cursor] == '}') break
            require(rawJson[cursor] == '"')
            val keyEnd = findStringEnd(rawJson, cursor)
            val key = decodeJsonStringLiteral(rawJson.substring(cursor, keyEnd + 1))
            cursor = skipWhitespace(rawJson, keyEnd + 1)
            require(rawJson[cursor] == ':')
            cursor = skipWhitespace(rawJson, cursor + 1)
            val valueEndExclusive = findValueEndExclusive(rawJson, cursor)
            entries += TopLevelEntry(
                key = key,
                rawValue = rawJson.substring(cursor, valueEndExclusive)
            )
            cursor = skipWhitespace(rawJson, valueEndExclusive)
            if (cursor < rawJson.lastIndex && rawJson[cursor] == ',') {
                cursor = skipWhitespace(rawJson, cursor + 1)
            }
        }
        return entries
    }

    private fun parseArrayEntries(rawJson: String): List<TopLevelEntry> {
        val entries = mutableListOf<TopLevelEntry>()
        var cursor = skipWhitespace(rawJson, 1)
        var index = 0
        while (cursor < rawJson.lastIndex) {
            if (rawJson[cursor] == ']') break
            val valueEndExclusive = findValueEndExclusive(rawJson, cursor)
            entries += TopLevelEntry(
                key = "[$index]",
                rawValue = rawJson.substring(cursor, valueEndExclusive)
            )
            index += 1
            cursor = skipWhitespace(rawJson, valueEndExclusive)
            if (cursor < rawJson.lastIndex && rawJson[cursor] == ',') {
                cursor = skipWhitespace(rawJson, cursor + 1)
            }
        }
        return entries
    }

    private fun findValueEndExclusive(rawJson: String, start: Int): Int {
        return when (rawJson[start]) {
            '{' -> findClosingBracket(rawJson, start, '{', '}') + 1
            '[' -> findClosingBracket(rawJson, start, '[', ']') + 1
            '"' -> findStringEnd(rawJson, start) + 1
            else -> findPrimitiveEndExclusive(rawJson, start)
        }
    }

    private fun findClosingBracket(rawJson: String, start: Int, open: Char, close: Char): Int {
        var cursor = start
        var depth = 0
        while (cursor < rawJson.length) {
            when (rawJson[cursor]) {
                '"' -> cursor = findStringEnd(rawJson, cursor)
                open -> depth += 1
                close -> {
                    depth -= 1
                    if (depth == 0) return cursor
                }
            }
            cursor += 1
        }
        throw IllegalArgumentException("Malformed JSON container")
    }

    private fun findStringEnd(rawJson: String, start: Int): Int {
        var cursor = start + 1
        while (cursor < rawJson.length) {
            if (rawJson[cursor] == '"' && !isEscaped(rawJson, cursor)) {
                return cursor
            }
            cursor += 1
        }
        throw IllegalArgumentException("Malformed JSON string")
    }

    private fun isEscaped(rawJson: String, index: Int): Boolean {
        var slashCount = 0
        var cursor = index - 1
        while (cursor >= 0 && rawJson[cursor] == '\\') {
            slashCount += 1
            cursor -= 1
        }
        return slashCount % 2 == 1
    }

    private fun findPrimitiveEndExclusive(rawJson: String, start: Int): Int {
        var cursor = start
        while (cursor < rawJson.length && rawJson[cursor] !in charArrayOf(',', '}', ']')) {
            cursor += 1
        }
        return cursor
    }

    private fun skipWhitespace(rawJson: String, start: Int): Int {
        var cursor = start
        while (cursor < rawJson.length && rawJson[cursor].isWhitespace()) {
            cursor += 1
        }
        return cursor
    }

    private fun expandRecursively(node: JsonNode): JsonNode {
        val expandedNode = expand(node)
        return when (expandedNode) {
            is JsonNode.ObjectNode -> expandedNode.copy(
                children = expandedNode.children.mapValues { (_, child) -> expandRecursively(child) }
            )

            is JsonNode.ArrayNode -> expandedNode.copy(
                items = expandedNode.items.map(::expandRecursively)
            )

            is JsonNode.ValueNode -> expandedNode
        }
    }

    private fun decodeJsonStringLiteral(literal: String): String {
        require(literal.length >= 2 && literal.first() == '"' && literal.last() == '"')
        val builder = StringBuilder(literal.length - 2)
        var cursor = 1
        while (cursor < literal.lastIndex) {
            val current = literal[cursor]
            if (current != '\\') {
                builder.append(current)
                cursor += 1
                continue
            }

            require(cursor + 1 < literal.lastIndex) { "Malformed JSON string literal: $literal" }
            when (val escaped = literal[cursor + 1]) {
                '"', '\\', '/' -> builder.append(escaped)
                'b' -> builder.append('\b')
                'f' -> builder.append('\u000C')
                'n' -> builder.append('\n')
                'r' -> builder.append('\r')
                't' -> builder.append('\t')
                'u' -> {
                    require(cursor + 5 < literal.length) { "Malformed unicode escape in JSON string literal: $literal" }
                    val hex = literal.substring(cursor + 2, cursor + 6)
                    builder.append(hex.toInt(16).toChar())
                    cursor += 4
                }

                else -> builder.append(escaped)
            }
            cursor += 2
        }
        return builder.toString()
    }

    private data class TopLevelEntry(
        val key: String?,
        val rawValue: String
    )
}
