package com.toktiklite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TikTokLinksTest {
    private val embed = "https://www.tiktok.com/embed/v2/7301234567890123456"

    @Test
    fun videoLinksBecomeTheEmbedPlayer() {
        assertEquals(embed, TikTokLinks.toEmbedUrl("https://www.tiktok.com/@some.user/video/7301234567890123456"))
        assertEquals(embed, TikTokLinks.toEmbedUrl("https://tiktok.com/@some.user/video/7301234567890123456?is_from_webapp=1&sender_device=pc"))
        assertEquals(embed, TikTokLinks.toEmbedUrl("https://m.tiktok.com/v/7301234567890123456.html"))
        assertEquals(embed, TikTokLinks.toEmbedUrl("https://www.tiktok.com/@u_s-er/video/7301234567890123456/"))
    }

    @Test
    fun otherTikTokLinksLoadAsIs() {
        assertNull("short links redirect first", TikTokLinks.toEmbedUrl("https://vm.tiktok.com/ZMabc123/"))
        assertNull("already embedded", TikTokLinks.toEmbedUrl(embed))
        assertNull("a profile isn't a video", TikTokLinks.toEmbedUrl("https://www.tiktok.com/@some.user"))
        assertEquals("https://vt.tiktok.com/ZSxyz/", TikTokLinks.forIncoming("https://vt.tiktok.com/ZSxyz/"))
        assertEquals(embed, TikTokLinks.forIncoming("https://www.tiktok.com/@a/video/7301234567890123456"))
    }

    @Test
    fun onlyTikTokOverHttpsLoadsInTheApp() {
        assertTrue(TikTokLinks.isTikTok("https://www.tiktok.com/@a/video/1"))
        assertTrue(TikTokLinks.isTikTok("https://vm.tiktok.com/ZM1/"))
        assertTrue(TikTokLinks.isTikTok("https://p16-sign.tiktokcdn.com/obj/x.jpg"))
        assertTrue(TikTokLinks.isTikTok("https://WWW.TIKTOK.COM/@a/video/1"))
        assertFalse("http", TikTokLinks.isTikTok("http://www.tiktok.com/@a/video/1"))
        assertFalse("look-alike domain", TikTokLinks.isTikTok("https://www.tiktok.com.evil.example/@a/video/1"))
        assertFalse("look-alike suffix", TikTokLinks.isTikTok("https://nottiktok.com/@a/video/1"))
        assertFalse("user info trick", TikTokLinks.isTikTok("https://www.tiktok.com@evil.example/@a/video/1"))
        assertFalse("other site", TikTokLinks.isTikTok("https://example.com/?u=tiktok.com/@a/video/1"))
        assertFalse("javascript", TikTokLinks.isTikTok("javascript:alert(1)"))
        assertFalse("file", TikTokLinks.isTikTok("file:///data/data/com.toktiklite/x"))
        assertFalse("garbage", TikTokLinks.isTikTok("not a url"))
        assertNull("refused", TikTokLinks.forIncoming("https://example.com/@a/video/7301234567890123456"))
        assertNull(TikTokLinks.toEmbedUrl("https://example.com/tiktok.com/@a/video/7301234567890123456"))
        assertNull(TikTokLinks.forIncoming(null))
    }

    @Test
    fun findsTheLinkInSharedText() {
        assertEquals("https://vm.tiktok.com/ZMabc123/",
            TikTokLinks.findIn("Check this out! https://vm.tiktok.com/ZMabc123/ lol"))
        assertEquals("https://www.tiktok.com/@a/video/1",
            TikTokLinks.findIn("(https://example.com/x) then https://www.tiktok.com/@a/video/1."))
        assertNull(TikTokLinks.findIn("https://example.com/only"))
        assertNull(TikTokLinks.findIn("nothing here"))
    }
}
