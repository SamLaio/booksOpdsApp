package com.example.booksopdsapp

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.net.toUri
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap

data class EnqueuedDownload(
    val id: Long,
    val fileName: String,
    val isNew: Boolean
)

private const val DOWNLOAD_LOG_TAG = "BooksOpdsDownload"

fun enqueueBookDownload(
    context: Context,
    action: OpdsViewModel.BookActionLink,
    username: String,
    password: String
): EnqueuedDownload {
    val normalizedUrl = normalizeDownloadUrl(action.url)
    val fileName = resolveServerFileName(
        url = normalizedUrl,
        username = username,
        password = password
    ) ?: buildFallbackFileName(action, normalizedUrl)
    val dedupeKey = buildDedupeKey(normalizedUrl, fileName)
    Log.d(
        DOWNLOAD_LOG_TAG,
        "request key=$dedupeKey mime=${action.mimeType} extHint=${action.extensionHint}"
    )
    if (!acquireEnqueueLock(dedupeKey)) {
        Log.d(DOWNLOAD_LOG_TAG, "blocked_by_lock key=$dedupeKey")
        Toast.makeText(context, "此檔案下載請求處理中：$fileName", Toast.LENGTH_SHORT).show()
        return EnqueuedDownload(
            id = -1L,
            fileName = fileName,
            isNew = false
        )
    }

    val request = DownloadManager.Request(normalizedUrl.toUri())
        .setTitle(fileName)
        .setDescription("下載中")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    val finalMimeType = resolveDownloadMimeType(action.mimeType, action.extensionHint)
    if (finalMimeType.isNotBlank()) {
        request.setMimeType(finalMimeType)
    }

    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

    if (username.isNotBlank()) {
        val token = Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)
        request.addRequestHeader("Authorization", "Basic $token")
    }

    val dm = context.getSystemService(DownloadManager::class.java)
    val removed = removeDuplicateActiveDownloads(dm, normalizedUrl, fileName)
    if (removed.isNotEmpty()) {
        Log.d(DOWNLOAD_LOG_TAG, "removed_active_duplicates ids=${removed.joinToString(",")} file=$fileName")
    }
    val downloadId = dm.enqueue(request)
    Log.d(DOWNLOAD_LOG_TAG, "enqueued id=$downloadId file=$fileName")
    Toast.makeText(context, "已加入下載：$fileName", Toast.LENGTH_SHORT).show()
    return EnqueuedDownload(
        id = downloadId,
        fileName = fileName,
        isNew = true
    )
}

private fun removeDuplicateActiveDownloads(
    dm: DownloadManager,
    normalizedUrl: String,
    fileName: String
): List<Long> {
    val query = DownloadManager.Query().setFilterByStatus(
        DownloadManager.STATUS_PENDING or
            DownloadManager.STATUS_PAUSED or
            DownloadManager.STATUS_RUNNING
    )
    val duplicateIds = mutableListOf<Long>()
    dm.query(query).use { cursor ->
        val idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
        val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI)
        val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
        if (idIndex == -1 || uriIndex == -1 || titleIndex == -1) return emptyList()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idIndex)
            val uri = cursor.getString(uriIndex).orEmpty()
            val title = cursor.getString(titleIndex).orEmpty()
            if (uri == normalizedUrl || title == fileName) {
                duplicateIds += id
            }
        }
    }
    if (duplicateIds.isNotEmpty()) {
        dm.remove(*duplicateIds.toLongArray())
    }
    return duplicateIds
}

private fun normalizeDownloadUrl(rawUrl: String): String {
    // Keep existing percent-encoding and only normalize raw square brackets.
    return rawUrl
        .replace("[", "%5B")
        .replace("]", "%5D")
}

private fun resolveDownloadMimeType(rawMimeType: String, extensionHint: String): String {
    val explicit = rawMimeType.trim()
    if (explicit.isNotBlank()) return explicit

    val ext = extensionHint.lowercase()
    if (ext == "epub") return "application/epub+zip"
    if (ext == "cbz") return "application/vnd.comicbook+zip"
    return ""
}

private fun buildFallbackFileName(
    action: OpdsViewModel.BookActionLink,
    normalizedUrl: String
): String {
    val guessedName = URLUtil.guessFileName(
        normalizedUrl,
        null,
        action.mimeType.ifBlank { null }
    ).ifBlank { "download" }

    val desiredExt = action.extensionHint.trim().lowercase()
    if (desiredExt.isBlank()) return guessedName

    val base = guessedName.substringBeforeLast('.').ifBlank { guessedName }
    return "$base.$desiredExt"
}

private fun resolveServerFileName(
    url: String,
    username: String,
    password: String
): String? {
    val fromHead = fetchContentDisposition(url, username, password, "HEAD")
        ?.let(::extractFileNameFromContentDisposition)
        ?.let(::sanitizeFileName)
        ?.ifBlank { null }
    if (!fromHead.isNullOrBlank()) return fromHead

    val fromGet = fetchContentDisposition(url, username, password, "GET")
        ?.let(::extractFileNameFromContentDisposition)
        ?.let(::sanitizeFileName)
        ?.ifBlank { null }
    if (fromGet.isNullOrBlank()) {
        Log.d(DOWNLOAD_LOG_TAG, "server_filename_missing url=$url")
    }
    return fromGet
}

private fun fetchContentDisposition(
    url: String,
    username: String,
    password: String,
    method: String
): String? {
    val connection = (URL(url).openConnection() as? HttpURLConnection) ?: return null
    return runCatching {
        connection.requestMethod = method
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        if (username.isNotBlank()) {
            val token = Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)
            connection.setRequestProperty("Authorization", "Basic $token")
        }
        if (method == "GET") {
            connection.setRequestProperty("Range", "bytes=0-0")
        }
        connection.connect()
        connection.getHeaderField("Content-Disposition")
    }.getOrNull().also { connection.disconnect() }
}

private fun extractFileNameFromContentDisposition(disposition: String): String? {
    if (disposition.isBlank()) return null

    val filenameStar = Regex("""filename\*\s*=\s*([^;]+)""", RegexOption.IGNORE_CASE)
        .find(disposition)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.trim('"')
    if (!filenameStar.isNullOrBlank()) {
        val encodedPart = filenameStar.substringAfter("''", filenameStar)
        val decoded = runCatching { URLDecoder.decode(encodedPart, Charsets.UTF_8.name()) }.getOrNull()
        if (!decoded.isNullOrBlank()) return decoded
    }

    val filename = Regex("""filename\s*=\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
        .find(disposition)
        ?.groupValues
        ?.getOrNull(1)
        ?: Regex("""filename\s*=\s*([^;]+)""", RegexOption.IGNORE_CASE)
            .find(disposition)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()

    return filename?.trim('"')?.takeIf { it.isNotBlank() }
}

private fun sanitizeFileName(name: String): String {
    return name
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .trim()
}

private val enqueueLocks = ConcurrentHashMap<String, Long>()
private const val ENQUEUE_LOCK_WINDOW_MS = 10000L

private fun buildDedupeKey(url: String, fileName: String): String = "$url|$fileName"

private fun acquireEnqueueLock(key: String): Boolean {
    val now = System.currentTimeMillis()
    val previous = enqueueLocks[key]
    if (previous != null && now - previous <= ENQUEUE_LOCK_WINDOW_MS) {
        return false
    }
    enqueueLocks[key] = now
    if (enqueueLocks.size > 256) {
        val expireBefore = now - ENQUEUE_LOCK_WINDOW_MS
        enqueueLocks.entries.removeIf { it.value < expireBefore }
    }
    return true
}
