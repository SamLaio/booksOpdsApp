package com.example.booksopdsapp

data class OpdsBook(
    val title: String,
    val author: String,
    val id: String,
    val summary: String,
    val links: List<OpdsLink>
)

data class OpdsLink(
    val href: String,
    val rel: String,
    val type: String,
    val title: String
)

data class OpdsFeed(
    val title: String,
    val books: List<OpdsBook>,
    val links: List<OpdsLink>
)
