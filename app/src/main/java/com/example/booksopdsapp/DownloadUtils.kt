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
    val normalizedUrl = normalizeDownloadUrl(action.url)
    val guessedName = URLUtil.guessFileName(
        normalizedUrl,
        null,
        action.mimeType.ifBlank { null }
    )
    val rawExtension = guessedName.substringAfterLast('.', "")
    val extension = when {
        action.extensionHint.isNotBlank() -> action.extensionHint
        rawExtension.equals("bin", ignoreCase = true) -> ""
        else -> rawExtension
    }
    val hasTitle = book.title.isNotBlank()
    val hasAuthor = book.author.isNotBlank()
    val safeTitle = sanitizeFilePart(book.title)
    val safeAuthor = sanitizeFilePart(book.author)
    val safeGuessedName = sanitizeFilePart(guessedName)
    val guessedBase = sanitizeFilePart(guessedName.substringBeforeLast('.', guessedName))
    val serverFileName = when {
        safeGuessedName.isBlank() -> ""
        rawExtension.equals("bin", ignoreCase = true) && extension.isNotBlank() ->
            joinNameAndExtension(guessedBase, extension)
        else -> safeGuessedName
    }
    val hasUsableServerFileName = serverFileName.isNotBlank() &&
        !safeGuessedName.equals("downloadfile.bin", ignoreCase = true) &&
        !safeGuessedName.equals("download.bin", ignoreCase = true)
    val requestedFileName = when {
        hasUsableServerFileName -> serverFileName
        hasTitle && hasAuthor -> joinNameAndExtension("$safeTitle - $safeAuthor", extension)
        hasTitle -> joinNameAndExtension(safeTitle, extension)
        else -> {
            if (serverFileName.isNotBlank()) serverFileName else joinNameAndExtension(guessedBase, extension)
        }
    }
    val fileName = requestedFileName

    val request = DownloadManager.Request(Uri.parse(normalizedUrl))
        .setTitle(fileName)
        .setDescription(normalizedUrl)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    if (action.mimeType.isNotBlank()) {
        request.setMimeType(action.mimeType)
    }

    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

    if (username.isNotBlank()) {
        val token = Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)
        request.addRequestHeader("Authorization", "Basic $token")
    }

    val dm = context.getSystemService(DownloadManager::class.java)
    dm.enqueue(request)
    Toast.makeText(context, "已加入下載：$fileName", Toast.LENGTH_SHORT).show()
}

private fun normalizeDownloadUrl(rawUrl: String): String {
    // Keep existing percent-encoding and only normalize raw square brackets.
    return rawUrl
        .replace("[", "%5B")
        .replace("]", "%5D")
}

private fun sanitizeFilePart(value: String): String {
    return value
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun joinNameAndExtension(base: String, extension: String): String {
    if (base.isBlank()) return "download"
    return if (extension.isBlank()) base else "$base.$extension"
}
