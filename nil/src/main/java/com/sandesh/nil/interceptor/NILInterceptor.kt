/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.interceptor

import com.sandesh.nil.utils.RequestBodyReader
import com.sandesh.nil.utils.ResponseBodyReader
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection

class NILInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return captureNetworkEvent(
            url = request.url.toString(),
            method = request.method,
            requestHeaders = request.headers.toFlatString(),
            requestBody = RequestBodyReader.read(request),
            execute = { chain.proceed(request) },
            onSuccess = { response ->
                CaptureResult(
                    responseHeaders = response.headers.toFlatString(),
                    responseBody = ResponseBodyReader.read(response),
                    statusCode = response.code
                )
            },
            onFailure = { throwable ->
                CaptureResult(
                    responseHeaders = null,
                    responseBody = throwable.message,
                    statusCode = null
                )
            }
        )
    }

    fun <T> intercept(
        connection: HttpURLConnection,
        requestBody: String? = null,
        execute: (HttpURLConnection) -> T,
        responseBodyExtractor: (T) -> String? = { null }
    ): T {
        return captureNetworkEvent(
            url = connection.url.toString(),
            method = connection.requestMethod.orEmpty(),
            requestHeaders = connection.requestProperties.toFlatString(),
            requestBody = requestBody,
            execute = { execute(connection) },
            onSuccess = { result ->
                CaptureResult(
                    responseHeaders = connection.headerFields.toFlatString(),
                    responseBody = responseBodyExtractor(result),
                    statusCode = connection.safeResponseCode()
                )
            },
            onFailure = { throwable ->
                CaptureResult(
                    responseHeaders = connection.headerFields.toFlatString(),
                    responseBody = throwable.message,
                    statusCode = connection.safeResponseCode()
                )
            }
        )
    }
}

private fun okhttp3.Headers.toFlatString(): String = buildString {
    for (index in 0 until size) {
        append(name(index))
        append(": ")
        append(value(index))
        append('\n')
    }
}.trimEnd()

private fun Map<String?, List<String>?>.toFlatString(): String? {
    if (isEmpty()) return null
    val content = buildString {
        entries.forEach { entry ->
            val name = entry.key
            val values = entry.value
            if (name.isNullOrBlank()) return@forEach
            values.orEmpty().forEach { value ->
                append(name)
                append(": ")
                append(value)
                append('\n')
            }
        }
    }.trimEnd()
    return content.ifBlank { null }
}

private fun HttpURLConnection.safeResponseCode(): Int? = runCatching { responseCode }.getOrNull()
