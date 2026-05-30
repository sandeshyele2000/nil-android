/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.utils

import com.sandesh.nil.model.NetworkEvent

object XhrGenerator {
    fun fromEvent(event: NetworkEvent): String {
        val requestHeaders = event.requestHeaders
            .orEmpty()
            .split('\n')
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }

        val builder = StringBuilder()
        builder.append("const xhr = new XMLHttpRequest();\n")
        builder.append("xhr.open(\"${escapeJs(event.method)}\", \"${escapeJs(event.url)}\", true);\n")
        requestHeaders.forEach { (name, value) ->
            builder.append("xhr.setRequestHeader(\"${escapeJs(name)}\", \"${escapeJs(value)}\");\n")
        }
        builder.append("xhr.onreadystatechange = function () {\n")
        builder.append("  if (xhr.readyState === 4) {\n")
        builder.append("    console.log(xhr.status, xhr.responseText);\n")
        builder.append("  }\n")
        builder.append("};\n")
        val body = event.requestBody.orEmpty()
        if (body.isBlank()) {
            builder.append("xhr.send();\n")
        } else {
            builder.append("xhr.send(\"${escapeJs(body)}\");\n")
        }
        return builder.toString()
    }

    private fun escapeJs(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}
