/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

internal object ShareFileUtil {
    fun shareTextFile(
        context: Context,
        fileName: String,
        content: String,
        mimeType: String = "text/plain"
    ) {
        val sharedDir = File(context.cacheDir, "nil-shared").apply { mkdirs() }
        val file = File(sharedDir, fileName)
        file.writeText(content)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.nil.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share file"))
    }
}
