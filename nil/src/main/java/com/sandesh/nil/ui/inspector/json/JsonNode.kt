/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.ui.inspector.json

sealed class JsonNode {
    data class ObjectNode(
        val key: String? = null,
        val children: Map<String, JsonNode>,
        val rawJson: String? = null
    ) : JsonNode()

    data class ArrayNode(
        val key: String? = null,
        val items: List<JsonNode>,
        val rawJson: String? = null
    ) : JsonNode()

    data class ValueNode(
        val key: String? = null,
        val value: String
    ) : JsonNode()
}
