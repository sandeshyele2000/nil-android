/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.utils

import org.json.JSONArray
import org.json.JSONObject
import java.io.StringReader
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

object BodyPrettyPrinter {
    fun prettyPrint(
        body: String?,
        headers: String?
    ): String? {
        if (body.isNullOrBlank()) return body
        val contentType = extractContentType(headers)
        return when {
            contentType.contains("application/json") || contentType.endsWith("+json") -> prettyJson(body)
            contentType.contains("xml") || contentType.endsWith("+xml") -> prettyXml(body)
            contentType.contains("application/x-www-form-urlencoded") -> prettyFormUrlEncoded(body)
            else -> body
        }
    }

    private fun extractContentType(headers: String?): String {
        if (headers.isNullOrBlank()) return ""
        return headers.lineSequence()
            .firstOrNull { it.startsWith("Content-Type:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.lowercase()
            ?: ""
    }

    private fun prettyJson(body: String): String {
        return runCatching {
            val trimmed = body.trim()
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                else -> body
            }
        }.getOrDefault(body)
    }

    private fun prettyXml(body: String): String {
        return runCatching {
            val transformer = TransformerFactory.newInstance().newTransformer().apply {
                setOutputProperty(OutputKeys.INDENT, "yes")
                setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            }
            val writer = java.io.StringWriter()
            transformer.transform(
                StreamSource(StringReader(body)),
                StreamResult(writer)
            )
            writer.toString().trim()
        }.getOrDefault(body)
    }

    private fun prettyFormUrlEncoded(body: String): String {
        return runCatching {
            body.split("&")
                .filter { it.isNotBlank() }
                .joinToString(separator = "\n") { pair ->
                    val key = pair.substringBefore("=")
                    val value = pair.substringAfter("=", "")
                    "${decode(key)} = ${decode(value)}"
                }
        }.getOrDefault(body)
    }

    private fun decode(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }
}
