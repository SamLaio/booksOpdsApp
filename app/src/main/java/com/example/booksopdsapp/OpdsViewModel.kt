package com.example.booksopdsapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OpdsUiState(
    val profileName: String = "",
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val feedTitle: String = "",
    val books: List<OpdsBook> = emptyList(),
    val error: String? = null,
    val canGoBack: Boolean = false,
    val canGoNextPage: Boolean = false,
    val canGoPreviousPage: Boolean = false,
    val canSearch: Boolean = false,
    val contextLabel: String = "",
    val debugUrlMessage: String? = null
)

class OpdsViewModel(
    private val repository: OpdsRepository = OpdsRepository(),
    private val navigator: OpdsNavigator = OpdsNavigator()
) : ViewModel() {
    companion object {
        private const val DEBUG_POPUP_ENABLED = false
    }
    data class BookActionLink(
        val label: String,
        val url: String,
        val extensionHint: String = "",
        val mimeType: String = ""
    )

    private val _uiState = MutableStateFlow(OpdsUiState())
    val uiState: StateFlow<OpdsUiState> = _uiState.asStateFlow()
    private data class HistoryEntry(val url: String, val contextLabel: String)
    private val urlHistory = ArrayDeque<HistoryEntry>()
    private var lastRootUrl: String? = null

    fun updateUrl(newUrl: String) {
        _uiState.value = _uiState.value.copy(url = newUrl)
    }

    fun updateProfileName(newName: String) {
        _uiState.value = _uiState.value.copy(profileName = newName)
    }

    fun updateUsername(newUsername: String) {
        _uiState.value = _uiState.value.copy(username = newUsername)
    }

    fun updatePassword(newPassword: String) {
        _uiState.value = _uiState.value.copy(password = newPassword)
    }

    fun loadFeed() {
        val state = _uiState.value
        val targetUrl = state.url.trim()
        if (targetUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "請輸入 OPDS URL")
            return
        }
        if (!targetUrl.equals(lastRootUrl, ignoreCase = true)) {
            lastRootUrl = targetUrl
        }
        urlHistory.clear()
        navigator.reset()
        loadFeedInternal(
            targetUrl = targetUrl,
            username = state.username,
            password = state.password,
            contextLabel = "",
            updateInputUrl = true
        )
    }

    fun showDebugMessage(message: String) {
        if (DEBUG_POPUP_ENABLED) {
            _uiState.value = _uiState.value.copy(debugUrlMessage = message)
        }
    }

    fun clearDebugUrlMessage() {
        _uiState.value = _uiState.value.copy(debugUrlMessage = null)
    }

    fun isDebugPopupEnabled(): Boolean = DEBUG_POPUP_ENABLED

    fun openBook(book: OpdsBook) {
        val navLink = navigator.pickNavigationLink(book) ?: return
        val targetUrl = navigator.resolveFromCurrentOrFallback(_uiState.value.url.trim(), navLink.href)
        if (targetUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "無法解析子節點 URL")
            return
        }
        urlHistory.addLast(HistoryEntry(_uiState.value.url.trim(), _uiState.value.contextLabel))
        loadFeedInternal(
            targetUrl = targetUrl,
            username = _uiState.value.username,
            password = _uiState.value.password,
            contextLabel = book.title,
            updateInputUrl = false
        )
    }

    fun goBack() {
        if (urlHistory.isEmpty()) return
        val previous = urlHistory.removeLast()
        loadFeedInternal(
            targetUrl = previous.url,
            username = _uiState.value.username,
            password = _uiState.value.password,
            contextLabel = previous.contextLabel,
            updateInputUrl = false
        )
    }

    fun goNextPage() {
        val next = navigator.resolveFeedLinkUrl("next", _uiState.value.url.trim()) ?: return
        loadFeedInternal(
            targetUrl = next,
            username = _uiState.value.username,
            password = _uiState.value.password,
            contextLabel = _uiState.value.contextLabel,
            updateInputUrl = false
        )
    }

    fun goPreviousPage() {
        val prev = navigator.resolveFeedLinkUrl("previous", _uiState.value.url.trim())
            ?: navigator.resolveFeedLinkUrl("prev", _uiState.value.url.trim())
            ?: return
        loadFeedInternal(
            targetUrl = prev,
            username = _uiState.value.username,
            password = _uiState.value.password,
            contextLabel = _uiState.value.contextLabel,
            updateInputUrl = false
        )
    }

    fun searchInOpds(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "請輸入搜尋關鍵字")
            return
        }
        viewModelScope.launch {
            val username = _uiState.value.username
            val password = _uiState.value.password
            runCatching {
                navigator.resolveSearchUrl(
                    query = trimmed,
                    fallbackUrl = _uiState.value.url.trim(),
                    username = username,
                    password = password,
                    fetchText = repository::fetchText
                )
            }.onSuccess { searchUrl ->
                if (searchUrl.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(error = "此 OPDS 不支援搜尋")
                    return@onSuccess
                }
                urlHistory.addLast(HistoryEntry(_uiState.value.url.trim(), _uiState.value.contextLabel))
                loadFeedInternal(
                    targetUrl = searchUrl,
                    username = username,
                    password = password,
                    contextLabel = "搜尋: $trimmed",
                    updateInputUrl = false
                )
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(error = ex.message ?: "搜尋失敗")
            }
        }
    }

    fun returnToHomeAfterError() {
        urlHistory.clear()
        navigator.reset()
        _uiState.value = _uiState.value.copy(
            feedTitle = "",
            books = emptyList(),
            error = null,
            canGoBack = false,
            canGoNextPage = false,
            canGoPreviousPage = false,
            canSearch = false,
            contextLabel = ""
        )
    }

    private fun loadFeedInternal(
        targetUrl: String,
        username: String,
        password: String,
        contextLabel: String,
        updateInputUrl: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            runCatching {
                repository.loadFeed(targetUrl, username, password)
            }.onSuccess { feed ->
                navigator.updateFeedContext(targetUrl, feed.links)
                _uiState.value = _uiState.value.copy(
                    url = if (updateInputUrl) targetUrl else _uiState.value.url,
                    loading = false,
                    feedTitle = feed.title,
                    books = feed.books,
                    error = null,
                    canGoBack = urlHistory.isNotEmpty(),
                    canGoNextPage = navigator.hasFeedRel("next"),
                    canGoPreviousPage = navigator.hasFeedRel("previous") || navigator.hasFeedRel("prev"),
                    canSearch = navigator.hasFeedSearch(),
                    contextLabel = contextLabel
                )
            }.onFailure { ex ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = ex.message ?: "讀取失敗"
                )
            }
        }
    }

    fun hasNavigableLink(book: OpdsBook): Boolean = navigator.pickNavigationLink(book) != null
    fun hasReadableLink(book: OpdsBook): Boolean = navigator.pickReadableLinks(book).isNotEmpty()
    fun resolveReadableLinks(book: OpdsBook): List<BookActionLink> {
        return navigator.pickReadableLinks(book).mapNotNull { link ->
            val url = navigator.resolveFromCurrentOrFallback(_uiState.value.url.trim(), link.href).ifBlank { return@mapNotNull null }
            val extensionHint = bestExtensionHint(link.type, url)
            val label = when {
                extensionHint.isNotBlank() -> "下載 ${extensionHint.uppercase()}"
                link.rel.contains("acquisition", ignoreCase = true) -> "下載書籍"
                else -> "開啟連結"
            }
            BookActionLink(label = label, url = url, extensionHint = extensionHint, mimeType = link.type)
        }
    }

    fun buildDownloadDebugMessage(action: BookActionLink): String {
        val regexGet = """/get/([a-z0-9]+)(/|\?|$)"""
        val regexDownload = """/download/\d+/([a-z0-9]+)(/|\?|$)"""
        val lowerUrl = action.url.lowercase()
        val urlExt = Regex(regexGet).find(lowerUrl)?.groupValues?.getOrNull(1)
            ?: Regex(regexDownload).find(lowerUrl)?.groupValues?.getOrNull(1)
            ?: ""
        val mimeExt = mimeToExtension(action.mimeType)
        return buildString {
            appendLine("下載 URL：${action.url}")
            appendLine("目前 regex(get)：$regexGet")
            appendLine("目前 regex(download)：$regexDownload")
            appendLine("URL 解析結果：${if (urlExt.isBlank()) "(未命中)" else urlExt}")
            appendLine("MIME：${action.mimeType.ifBlank { "(空)" }}")
            appendLine("MIME 解析結果：${if (mimeExt.isBlank()) "(空)" else mimeExt}")
            append("最終 extensionHint：${if (action.extensionHint.isBlank()) "(空)" else action.extensionHint}")
        }
    }

    private fun bestExtensionHint(type: String, url: String): String {
        val fromUrl = urlToExtension(url)
        if (fromUrl.isNotBlank()) return fromUrl
        return mimeToExtension(type)
    }

    private fun mimeToExtension(type: String): String {
        val normalized = type.lowercase().substringBefore(";").trim()
        return when (normalized) {
            "application/epub+zip" -> "epub"
            "application/pdf" -> "pdf"
            "application/x-mobipocket-ebook" -> "mobi"
            "application/x-mobi8-ebook" -> "azw3"
            "application/vnd.amazon.ebook" -> "azw"
            "application/x-cbz", "application/vnd.comicbook+zip" -> "cbz"
            "application/x-cbr", "application/vnd.comicbook-rar" -> "cbr"
            "application/zip" -> "zip"
            "application/x-rar-compressed" -> "rar"
            "text/plain" -> "txt"
            "text/markdown" -> "md"
            else -> {
                val subtype = normalized.substringAfter("/", "").substringBefore("+").ifBlank { "" }
                when (subtype) {
                    "x-mobi8-ebook" -> "azw3"
                    "x-mobipocket-ebook" -> "mobi"
                    else -> ""
                }
            }
        }
    }

    private fun urlToExtension(url: String): String {
        val lower = url.lowercase()
        val known = listOf("epub", "pdf", "mobi", "azw3", "azw", "fb2", "txt", "rtf", "docx", "cbz", "cbr", "zip", "rar")
        val getMatch = Regex("/get/([a-z0-9]+)(/|\\?|$)").find(lower)?.groupValues?.getOrNull(1).orEmpty()
        if (getMatch in known) return getMatch

        val downloadMatch = Regex("/download/\\d+/([a-z0-9]+)(/|\\?|$)").find(lower)?.groupValues?.getOrNull(1).orEmpty()
        if (downloadMatch in known) return downloadMatch

        val formatMatch = Regex("[?&](format|type)=([a-z0-9]+)(&|$)").find(lower)?.groupValues?.getOrNull(2).orEmpty()
        if (formatMatch in known) return formatMatch

        val pathExt = lower.substringBefore('?').substringAfterLast('.', "")
        return if (pathExt in known) pathExt else ""
    }

    fun resolveCoverUrl(book: OpdsBook): String? {
        val coverLink = navigator.pickCoverLink(book) ?: return null
        return navigator.resolveFromCurrentOrFallback(_uiState.value.url.trim(), coverLink.href).ifBlank { null }
    }
}
