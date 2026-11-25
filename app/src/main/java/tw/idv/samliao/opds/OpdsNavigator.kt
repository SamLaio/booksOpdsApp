package tw.idv.samliao.opds

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
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
        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name()).replace("+", "%20")
        var href = link.href
        var templateBaseUrl = baseUrl
        if (link.type.contains("opensearchdescription+xml", ignoreCase = true) ||
            href.contains("feed=osd", ignoreCase = true)
        ) {
            val osdUrl = resolveUrl(baseUrl, href)
            val osdXml = fetchText(osdUrl, username, password)
            href = findSearchTemplate(osdXml) ?: return null
            templateBaseUrl = osdUrl
        }
        for ((name, value) in mapOf("searchTerms" to encodedQuery, "count" to "30", "startIndex" to "0", "startPage" to "1")) {
            href = href.replace("{$name}", value).replace("{$name?}", value)
        }
        href = href.replace(Regex("\\{[^{}]+\\?}"), "")
        if (href.contains('{') || href.contains('}')) return null
        return resolveUrl(templateBaseUrl, href).ifBlank { null }
    }

    private fun resolveUrl(baseUrl: String, href: String): String {
        return runCatching { URI(baseUrl).resolve(href).toString() }.getOrElse { href }
    }

    private fun findSearchTemplate(xml: String): String? {
        val parser = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }.newPullParser()
        parser.setInput(StringReader(xml))
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG || parser.name != "Url") continue
            val type = parser.getAttributeValue(null, "type").orEmpty().substringBefore(';').trim()
            val template = parser.getAttributeValue(null, "template")
            if (type.equals("application/atom+xml", ignoreCase = true) && !template.isNullOrBlank()) {
                return template
            }
        }
        return null
    }
}
