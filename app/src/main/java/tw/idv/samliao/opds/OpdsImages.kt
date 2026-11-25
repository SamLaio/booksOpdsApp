package tw.idv.samliao.opds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Composable
fun rememberCoverRequest(
    url: String?,
    feedUrl: String,
    username: String,
    password: String
): ImageRequest? {
    val context = LocalContext.current
    return remember(context, url, feedUrl, username, password) {
        url?.let {
            val cover = it.toHttpUrlOrNull()
            val feed = feedUrl.toHttpUrlOrNull()
            ImageRequest.Builder(context)
                .data(it)
                .apply {
                    // Credentials belong only to the configured catalog's origin.
                    if (username.isNotBlank() && cover != null && feed != null &&
                        cover.scheme == feed.scheme && cover.host == feed.host && cover.port == feed.port
                    ) {
                        addHeader("Authorization", Credentials.basic(username, password))
                        memoryCachePolicy(CachePolicy.DISABLED)
                        diskCachePolicy(CachePolicy.DISABLED)
                    }
                }
                .build()
        }
    }
}
