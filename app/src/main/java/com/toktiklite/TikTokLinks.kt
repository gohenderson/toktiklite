package com.toktiklite

import java.net.URI

/**
 * Pure URL logic (no Android), so it's covered by fast JVM unit tests.
 *
 * - [toEmbedUrl] turns a TikTok post link (video or photos) into TikTok's embed player,
 *   which fits the post to the screen.
 * - [isTikTok] decides which pages may load inside the app: TikTok and its CDNs only.
 *   Anything else (a link in a video description, an ad) opens in the browser instead,
 *   so the app's JavaScript-enabled WebView only ever runs TikTok's own pages.
 */
object TikTokLinks {
    // Matches: www.tiktok.com/@user/video/1234567890
    //          www.tiktok.com/@user/photo/1234567890  (a photo post: "swipe for more")
    //          tiktok.com/@user/video/1234567890
    //          m.tiktok.com/v/1234567890.html
    private val VIDEO_URL_REGEX = Regex("""tiktok\.com/(?:@[^/]*/(?:video|photo)|v)/(\d+)""")

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
     * The embed player URL for a TikTok post link, or null if [url] isn't a post link (or is
     * already the player) so it can load as-is (e.g. a short link that redirects).
     *
     * TikTok's player (player/v1) scales the post to fit the screen, for videos and photo
     * posts alike; it plays on its own, and at the end shows more from the same creator
     * rather than recommendations.
     */
    fun toEmbedUrl(url: String): String? {
        if (!isTikTok(url)) return null
        if (url.contains("tiktok.com/embed/") || url.contains("tiktok.com/player/")) return null
        val videoId = VIDEO_URL_REGEX.find(url)?.groupValues?.get(1) ?: return null
        return playerUrl(videoId)
    }

    fun playerUrl(postId: String) = "https://www.tiktok.com/player/v1/$postId?autoplay=1&rel=0"

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

    /** What the player does when a page in it goes to a new address. */
    sealed interface Navigation {
        /** Show this post in the embed player instead. */
        data class Embed(val url: String) : Navigation
        /** A TikTok page that isn't a post (a short link on its way to one, say): let it load. */
        data object Load : Navigation
        /** Not TikTok, or not https: not in the app (the browser, if it was a tap). */
        data object Leave : Navigation
    }

    /** Where a navigation to [url] goes. */
    fun navigate(url: String): Navigation = when {
        !isTikTok(url) -> Navigation.Leave
        else -> toEmbedUrl(url)?.let { Navigation.Embed(it) } ?: Navigation.Load
    }

    /** Finds the first TikTok link in pasted text (e.g. "Check this out https://vm.tiktok.com/ZM…/"). */
    fun findIn(text: String): String? =
        Regex("""https://[^\s"'<>]+""").findAll(text).map { it.value.trimEnd('.', ',', ')', '!') }
            .firstOrNull { isTikTok(it) }
}
