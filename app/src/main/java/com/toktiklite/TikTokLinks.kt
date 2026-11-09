package com.toktiklite

import java.net.URI

/**
 * Pure URL logic (no Android), so it's covered by fast JVM unit tests.
 *
 * - [toEmbedUrl] turns a TikTok video link into TikTok's single-video embed player.
 * - [isTikTok] decides which pages may load inside the app: TikTok and its CDNs only.
 *   Anything else (a link in a video description, an ad) opens in the browser instead,
 *   so the app's JavaScript-enabled WebView only ever runs TikTok's own pages.
 */
object TikTokLinks {
    // Matches: www.tiktok.com/@user/video/1234567890
    //          tiktok.com/@user/video/1234567890
    //          m.tiktok.com/v/1234567890.html
    private val VIDEO_URL_REGEX = Regex("""tiktok\.com/(?:@[^/]+/video|v)/(\d+)""")

    /** Domains whose pages (and redirects) may load in the app, including subdomains. */
    private val ALLOWED_DOMAINS = listOf(
        "tiktok.com",        // www., m., vm., vt. (short links redirect within these)
        "tiktokv.com",
        "tiktokcdn.com",
        "tiktokcdn-us.com",
        "ttwstatic.com",
        "byteoversea.com",
        "ibytedtos.com",
        "ibyteimg.com",
    )

    /**
     * The embed player URL for a TikTok video link, or null if [url] isn't a video link
     * (or is already an embed URL) so it can load as-is (e.g. a short link that redirects).
     */
    fun toEmbedUrl(url: String): String? {
        if (!isTikTok(url)) return null
        if (url.contains("tiktok.com/embed/")) return null
        val videoId = VIDEO_URL_REGEX.find(url)?.groupValues?.get(1) ?: return null
        return "https://www.tiktok.com/embed/v2/$videoId"
    }

    /** True for https URLs on TikTok's own domains (and only those). */
    fun isTikTok(url: String): Boolean {
        val uri = try {
            URI(url.trim())
        } catch (e: Exception) {
            return false
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (uri.rawUserInfo != null) return false // https://www.tiktok.com@evil.example/
        val host = uri.host?.lowercase()?.trimEnd('.') ?: return false
        return ALLOWED_DOMAINS.any { host == it || host.endsWith(".$it") }
    }

    /** The URL to show for an incoming link: the embed player, the link itself, or null to refuse it. */
    fun forIncoming(url: String?): String? {
        if (url == null || !isTikTok(url)) return null
        return toEmbedUrl(url) ?: url
    }

    /** Finds the first TikTok link in pasted text (e.g. "Check this out https://vm.tiktok.com/ZM…/"). */
    fun findIn(text: String): String? =
        Regex("""https://[^\s"'<>]+""").findAll(text).map { it.value.trimEnd('.', ',', ')', '!') }
            .firstOrNull { isTikTok(it) }
}
