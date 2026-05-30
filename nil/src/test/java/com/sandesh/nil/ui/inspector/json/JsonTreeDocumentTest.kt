package com.sandesh.nil.ui.inspector.json

import com.sandesh.nil.utils.json.JsonTreeBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonTreeDocumentTest {
    @Test
    fun expandNodeAtPath_supportsQuotedKeysInLazyMode() {
        val root = JsonTreeBuilder.buildLazy(
            """
            {
              "simple": {
                "key.with.dot": {
                  "items[0]": [1, 2, 3]
                }
              }
            }
            """.trimIndent()
        )!!

        val expandedSimple = JsonTreeDocument.expandNodeAtPath(root, "root.simple")
        val expandedNested = JsonTreeDocument.expandNodeAtPath(
            expandedSimple,
            "root.simple[\"key.with.dot\"]"
        )
        val expandedArrayContainer = JsonTreeDocument.expandNodeAtPath(
            expandedNested,
            "root.simple[\"key.with.dot\"][\"items[0]\"]"
        )

        val targetNode = JsonTreeDocument.findNodeAtPath(
            expandedArrayContainer,
            "root.simple[\"key.with.dot\"][\"items[0]\"]"
        )

        assertTrue("targetNode=$targetNode", targetNode is JsonNode.ArrayNode)
        assertEquals(3, (targetNode as JsonNode.ArrayNode).items.size)
    }

    @Test
    fun buildSearchEntries_andFindMatches_includePathTokensForComplexKeys() {
        val root = JsonTreeBuilder.build(
            """
            {
              "payload.meta": {
                "status": "ok"
              }
            }
            """.trimIndent()
        )!!

        val entries = JsonTreeDocument.buildSearchEntries(root)
        val matches = JsonTreeDocument.findMatches(
            entries = entries,
            query = "path:payload.meta status",
            pathSearchMode = false
        )

        assertEquals("matches=$matches", 1, matches.size)
        assertTrue("match=${matches.firstOrNull()}", matches.first().normalizedPath.contains("[\"payload.meta\"].status"))
    }

    @Test
    fun buildVisibleRows_usesStableQuotedPathsForComplexKeys() {
        val root = JsonTreeBuilder.build(
            """
            {
              "user.name": {
                "age": 30
              }
            }
            """.trimIndent()
        )!!

        val rows = JsonTreeDocument.buildVisibleRows(
            root = root,
            expandedPaths = setOf(ROOT_PATH, "root[\"user.name\"]"),
            loadingPaths = emptySet()
        )

        assertTrue("rows=$rows", rows.any { it.path == "root[\"user.name\"]" && it.type == JsonRowType.ObjectOpen })
        assertTrue("rows=$rows", rows.any { it.path == "root[\"user.name\"].age" && it.type == JsonRowType.Value })
    }

    @Test
    fun build_returnsValueNodeForInvalidJson() {
        val node = JsonTreeBuilder.build("{invalid")

        assertNotNull(node)
        assertTrue(node is JsonNode.ValueNode)
        assertEquals("{invalid", (node as JsonNode.ValueNode).value)
    }
}
