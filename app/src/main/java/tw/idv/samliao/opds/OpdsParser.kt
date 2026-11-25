package tw.idv.samliao.opds

import androidx.core.text.HtmlCompat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

class OpdsParser {
    fun parse(feedXml: String): OpdsFeed {
        val parser = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }.newPullParser()
        parser.setInput(StringReader(feedXml))

        var feedTitle = "OPDS Feed"
        val books = mutableListOf<OpdsBook>()
        val feedLinks = mutableListOf<OpdsLink>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG || parser.depth != 2 || !parser.isAtom()) continue
            when (parser.name) {
                "title" -> readText(parser).takeIf { it.isNotBlank() }?.let { feedTitle = it }
                "link" -> feedLinks.add(readLink(parser))
                "entry" -> books.add(readBook(parser))
            }
        }
        return OpdsFeed(title = feedTitle, books = books, links = feedLinks)
    }

    private fun readBook(parser: XmlPullParser): OpdsBook {
        val depth = parser.depth
        var title = ""
        var id = ""
        var summary = ""
        var content = ""
        val authors = mutableListOf<String>()
        val links = mutableListOf<OpdsLink>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.END_TAG && parser.depth == depth) break
            if (parser.eventType != XmlPullParser.START_TAG || parser.depth != depth + 1 || !parser.isAtom()) continue
            when (parser.name) {
                "title" -> title = readText(parser)
                "id" -> id = readText(parser)
                "author" -> readAuthor(parser).takeIf { it.isNotBlank() }?.let { authors.add(it) }
                "summary" -> summary = readText(parser)
                "content" -> content = readText(parser)
                "link" -> links.add(readLink(parser))
            }
        }
        return OpdsBook(
            title = title.ifBlank { "(Untitled)" },
            author = authors.joinToString("、").ifBlank { "Unknown" },
            id = id,
            summary = summary.ifBlank { content },
            links = links
        )
    }

    private fun readAuthor(parser: XmlPullParser): String {
        val depth = parser.depth
        var name = ""
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.END_TAG && parser.depth == depth) break
            if (parser.eventType == XmlPullParser.START_TAG && parser.depth == depth + 1 &&
                parser.isAtom() && parser.name == "name"
            ) {
                name = readText(parser)
            }
        }
        return name
    }

    private fun readText(parser: XmlPullParser): String {
        val depth = parser.depth
        val type = parser.getAttributeValue(null, "type")
        val text = StringBuilder()
        val blockTags = setOf("br", "p", "div", "li", "tr", "h1", "h2", "h3")
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.END_TAG && parser.depth == depth) break
            when (parser.eventType) {
                XmlPullParser.TEXT -> text.append(parser.text)
                XmlPullParser.START_TAG, XmlPullParser.END_TAG -> {
                    if (parser.name in blockTags) text.append('\n')
                }
            }
        }
        val value = if (type == "html") {
            HtmlCompat.fromHtml(text.toString(), HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
        } else {
            text.toString()
        }
        return value.replace(Regex("[\\t\\r ]+"), " ")
            .replace(Regex(" *\\n\\s*"), "\n")
            .trim()
    }

    private fun readLink(parser: XmlPullParser): OpdsLink {
        return OpdsLink(
            href = parser.getAttributeValue(null, "href") ?: "",
            rel = parser.getAttributeValue(null, "rel") ?: "",
            type = parser.getAttributeValue(null, "type") ?: "",
            title = parser.getAttributeValue(null, "title") ?: ""
        )
    }

    private fun XmlPullParser.isAtom(): Boolean {
        return namespace.isNullOrEmpty() || namespace == "http://www.w3.org/2005/Atom"
    }
}
