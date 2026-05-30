/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.utils

import okhttp3.Request
import okio.Buffer
import java.nio.charset.Charset

internal object RequestBodyReader {
    private const val MAX_BODY_BYTES = 1024L * 1024L

    fun read(request: Request): String? {
        val body = request.body ?: return null
        return runCatching {
            val buffer = Buffer()
            body.writeTo(buffer)
            if (buffer.size > MAX_BODY_BYTES) {
                return "Body omitted: request body exceeds $MAX_BODY_BYTES bytes"
            }

            val charset: Charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            buffer.readString(charset)
        }.getOrNull()
    }
}
