package com.example.booksopdsapp

import java.net.URI
import java.net.URLEncoder

class OpdsNavigator {
    private var currentFeedUrl: String? = null
    private var currentFeedLinks: List<OpdsLink> = emptyList()

    fun reset() {
        currentFeedUrl = null
        currentFeedLinks = emptyList()
    }

    fun updateFeedContext(url: String, links: List<OpdsLink>) {
        currentFeedUrl = url
        currentFeedLinks = links
    }

    fun resolveFromCurrentOrFallback(fallbackUrl: String, href: String): String {
        val baseUrl = currentFeedUrl ?: fallbackUrl
        return resolveUrl(baseUrl, href)
    }

    fun pickNavigationLink(book: OpdsBook): OpdsLink? {
        return book.links.firstOrNull {
            it.type.contains("application/atom+xml", ignoreCase = true) ||
                it.rel.contains("subsection", ignoreCase = true) ||
                it.rel.contains("collection", ignoreCase = true)
        }
    }

    fun pickReadableLinks(book: OpdsBook): List<OpdsLink> {
        return book.links.filter {
            // Prefer OPDS acquisition semantics, but also allow common downloadable MIME links.
            it.rel.contains("acquisition", ignoreCase = true) ||
                isDownloadableMime(it.type)
        }
    }

    private fun isDownloadableMime(type: String): Boolean {
        val t = type.lowercase()
        if (t.isBlank()) return false
        if (t.contains("atom+xml") || t.contains("opds") || t.contains("opensearchdescription+xml")) return false
        if (t.startsWith("image/") || t.startsWith("text/html")) return false
        return t.startsWith("application/") || t.startsWith("text/")
    }

    fun pickCoverLink(book: OpdsBook): OpdsLink? {
        return book.links.firstOrNull {
            it.rel.contains("thumbnail", ignoreCase = true) ||
                it.rel.contains("image", ignoreCase = true)
        } ?: book.links.firstOrNull {
            it.type.startsWith("image/", ignoreCase = true)
        }
    }

    fun hasFeedRel(rel: String): Boolean {
        return currentFeedLinks.any { it.rel.contains(rel, ignoreCase = true) }
    }

    fun hasFeedSearch(): Boolean {
        return currentFeedLinks.any { link ->
            link.rel.contains("search", ignoreCase = true) ||
                link.type.contains("opensearchdescription+xml", ignoreCase = true)
        }
    }

    fun resolveFeedLinkUrl(rel: String, fallbackUrl: String): String? {
        val link = currentFeedLinks.firstOrNull { it.rel.contains(rel, ignoreCase = true) } ?: return null
        val baseUrl = currentFeedUrl ?: fallbackUrl
        return resolveUrl(baseUrl, link.href).ifBlank { null }
    }

    suspend fun resolveSearchUrl(
        query: String,
        fallbackUrl: String,
        username: String,
        password: String,
        fetchText: suspend (String, String, String) -> String
    ): String? {
        val link = currentFeedLinks.firstOrNull { it.rel.contains("search", ignoreCase = true) } ?: return null
        val baseUrl = currentFeedUrl ?: fallbackUrl
        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
        var href = link.href
        if (link.type.contains("opensearchdescription+xml", ignoreCase = true) ||
            href.contains("feed=osd", ignoreCase = true)
        ) {
            val osdUrl = resolveUrl(baseUrl, href)
            val osdXml = fetchText(osdUrl, username, password)
            val template = Regex("template\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
                .find(osdXml)
                ?.groupValues
                ?.getOrNull(1)
            if (!template.isNullOrBlank()) {
                href = decodeXmlEntities(template)
            }
        }
        href = href
            .replace("{searchTerms}", encodedQuery)
            .replace("{searchTerms?}", encodedQuery)
            .replace("{count}", "30")
            .replace("{startIndex}", "0")
            .replace("{startPage}", "1")
        return resolveUrl(baseUrl, href).ifBlank { null }
    }

    private fun resolveUrl(baseUrl: String, href: String): String {
        return runCatching { URI(baseUrl).resolve(href).toString() }.getOrElse { href }
    }

    private fun decodeXmlEntities(value: String): String {
        return value
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}
