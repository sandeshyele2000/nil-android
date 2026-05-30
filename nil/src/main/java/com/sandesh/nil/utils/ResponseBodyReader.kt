/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.utils

import okhttp3.Response

internal object ResponseBodyReader {
    private const val MAX_PEEK_BYTES = 1024L * 1024L

    fun read(response: Response): String? {
        val body = response.body ?: return null
        return runCatching {
            val raw = response.peekBody(MAX_PEEK_BYTES).string()
            if (raw.isBlank()) null else raw
        }.getOrNull()
    }
}
