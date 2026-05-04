package com.example.booksopdsapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request

class OpdsRepository(
    private val client: OkHttpClient = OkHttpClient(),
    private val parser: OpdsParser = OpdsParser()
) {
    suspend fun fetchText(url: String, username: String, password: String): String = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder().url(url)
        if (username.isNotBlank()) {
            requestBuilder.header("Authorization", Credentials.basic(username, password))
        }
        val request = requestBuilder.build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                if (response.code == 401) {
                    throw IllegalStateException("認證失敗（401），請檢查帳號密碼")
                }
                throw IllegalStateException("HTTP ${response.code}")
            }
            response.body?.string() ?: throw IllegalStateException("Empty response")
        }
    }

    suspend fun loadFeed(
        url: String,
        username: String,
        password: String
    ): OpdsFeed = withContext(Dispatchers.IO) {
        val body = fetchText(url, username, password)
        val head = body.trimStart().take(200).lowercase()
        if (head.startsWith("<!doctype html") || head.startsWith("<html")) {
            throw IllegalStateException("回傳的是 HTML 頁面，請輸入 OPDS Feed 連結（不是網站首頁）")
        }
        parser.parse(body)
    }
}
