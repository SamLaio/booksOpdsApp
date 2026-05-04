package com.example.booksopdsapp

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.webkit.URLUtil
import android.widget.Toast

fun enqueueBookDownload(
    context: Context,
    action: OpdsViewModel.BookActionLink,
    book: OpdsBook,
    username: String,
    password: String
) {
    val guessedName = URLUtil.guessFileName(action.url, null, null)
    val rawExtension = guessedName.substringAfterLast('.', "")
    val extension = when {
        action.extensionHint.isNotBlank() -> action.extensionHint
        rawExtension.equals("bin", ignoreCase = true) -> ""
        else -> rawExtension
    }
    val safeTitle = sanitizeFilePart(book.title.ifBlank { "Untitled" })
    val safeAuthor = sanitizeFilePart(book.author.ifBlank { "Unknown" })
    val fileName = if (extension.isBlank()) {
        "$safeTitle - $safeAuthor"
    } else {
        "$safeTitle - $safeAuthor.$extension"
    }

    val request = DownloadManager.Request(Uri.parse(action.url))
        .setTitle(fileName)
        .setDescription(action.url)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

    if (username.isNotBlank()) {
        val token = Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)
        request.addRequestHeader("Authorization", "Basic $token")
    }

    val dm = context.getSystemService(DownloadManager::class.java)
    dm.enqueue(request)
    Toast.makeText(context, "已加入下載：$fileName", Toast.LENGTH_SHORT).show()
}

private fun sanitizeFilePart(value: String): String {
    return value
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .replace(Regex("\\s+"), " ")
        .trim()
}
