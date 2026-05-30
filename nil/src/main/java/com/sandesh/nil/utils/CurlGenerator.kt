/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.utils

import com.sandesh.nil.model.NetworkEvent
import okhttp3.Request

object CurlGenerator {
    fun fromRequest(request: Request): String {
        val sb = StringBuilder("curl")
            .append(" -X ")
            .append(request.method)

        val headers = request.headers
        for (index in 0 until headers.size) {
            sb.append(" -H ")
                .append(escape("'${headers.name(index)}: ${headers.value(index)}'"))
        }

        val body = RequestBodyReader.read(request)
        if (!body.isNullOrBlank()) {
            sb.append(" --data-raw ")
                .append(escape("'$body'"))
        }

        sb.append(" ")
            .append(escape("'${request.url}'"))

        return sb.toString()
    }

    fun fromEvent(event: NetworkEvent): String {
        val sb = StringBuilder("curl")
            .append(" -X ")
            .append(event.method)

        event.requestHeaders
            ?.split('\n')
            ?.filter { it.contains(':') }
            ?.forEach { header ->
                sb.append(" -H ")
                    .append(escape("'$header'"))
            }

        if (!event.requestBody.isNullOrBlank()) {
            sb.append(" --data-raw ")
                .append(escape("'${event.requestBody}'"))
        }

        sb.append(" ")
            .append(escape("'${event.url}'"))

        return sb.toString()
    }

    private fun escape(value: String): String = value.replace("'", "'\"'\"'")
}
