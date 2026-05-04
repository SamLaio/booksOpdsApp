package com.example.booksopdsapp

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

class OpdsParser {
    fun parse(feedXml: String): OpdsFeed {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true

        val parser = factory.newPullParser()
        parser.setInput(StringReader(feedXml))

        var feedTitle = "OPDS Feed"
        val books = mutableListOf<OpdsBook>()
        val feedLinks = mutableListOf<OpdsLink>()

        var eventType = parser.eventType
        var currentBook: MutableBook? = null
        var text = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "entry" -> currentBook = MutableBook()
                        "link" -> {
                            val link = OpdsLink(
                                href = parser.getAttributeValue(null, "href") ?: "",
                                rel = parser.getAttributeValue(null, "rel") ?: "",
                                type = parser.getAttributeValue(null, "type") ?: "",
                                title = parser.getAttributeValue(null, "title") ?: ""
                            )
                            if (currentBook == null) {
                                feedLinks.add(link)
                            } else {
                                currentBook.links.add(link)
                            }
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    text = parser.text ?: ""
                }

                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "title" -> {
                            if (currentBook == null) {
                                if (text.isNotBlank()) feedTitle = text.trim()
                            } else {
                                currentBook.title = text.trim()
                            }
                        }
                        "id" -> currentBook?.id = text.trim()
                        "name" -> {
                            if (currentBook != null && text.isNotBlank()) {
                                currentBook.author = text.trim()
                            }
                        }
                        "summary", "content" -> {
                            if (currentBook != null && currentBook.summary.isBlank()) {
                                currentBook.summary = text.trim()
                            }
                        }
                        "entry" -> {
                            currentBook?.let {
                                books += OpdsBook(
                                    title = if (it.title.isBlank()) "(Untitled)" else it.title,
                                    author = if (it.author.isBlank()) "Unknown" else it.author,
                                    id = it.id,
                                    summary = it.summary,
                                    links = it.links.toList()
                                )
                            }
                            currentBook = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return OpdsFeed(title = feedTitle, books = books, links = feedLinks)
    }

    private data class MutableBook(
        var title: String = "",
        var author: String = "",
        var id: String = "",
        var summary: String = "",
        val links: MutableList<OpdsLink> = mutableListOf()
    )
}
