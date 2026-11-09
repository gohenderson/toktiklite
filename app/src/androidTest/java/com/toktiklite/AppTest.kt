package com.toktiklite

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The player screen: which links it opens, and how. (Checks what it loads, not TikTok's servers.) */
@RunWith(AndroidJUnit4::class)
class PlayerTest {
    @get:Rule
    val failureScreenshot = FailureScreenshot()

    @Before
    fun setUp() = TestEnv.readyDevice()

    private fun open(url: String) = ActivityScenario.launch<MainActivity>(
        Intent(TestEnv.ctx, MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse(url)),
    )

    private fun ActivityScenario<MainActivity>.loaded(): String? {
        var url: String? = null
        onActivity { url = it.lastLoadedUrl }
        return url
    }

    @Test
    fun aVideoLinkOpensTheEmbedPlayer() {
        open("https://www.tiktok.com/@some.user/video/7301234567890123456?lang=en").use {
            assertEquals("https://www.tiktok.com/embed/v2/7301234567890123456", it.loaded())
        }
    }

    @Test
    fun aShortLinkLoadsSoItCanRedirect() {
        open("https://vm.tiktok.com/ZMabc123/").use {
            assertEquals("https://vm.tiktok.com/ZMabc123/", it.loaded())
        }
    }

    @Test
    fun aNewLinkReplacesTheVideoInTheSameScreen() {
        open("https://www.tiktok.com/@a/video/1111111111").use { s ->
            // singleTask: a second link goes to the open screen (onNewIntent).
            TestEnv.ctx.startActivity(
                Intent(TestEnv.ctx, MainActivity::class.java).setAction(Intent.ACTION_VIEW)
                    .setData(Uri.parse("https://www.tiktok.com/@b/video/2222222222"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            TestEnv.eventually { assertEquals("https://www.tiktok.com/embed/v2/2222222222", s.loaded()) }
        }
    }

    @Test
    fun aNonTikTokLinkIsRefused() {
        // The screen is exported, so another app could send it anything.
        open("https://example.com/@a/video/7301234567890123456").use {
            TestEnv.eventually { assertEquals(Lifecycle.State.DESTROYED, it.state) }
        }
    }
}

/** The launcher screen. */
@RunWith(AndroidJUnit4::class)
class HomeTest {
    @get:Rule
    val failureScreenshot = FailureScreenshot()

    @Before
    fun setUp() = TestEnv.readyDevice()

    @Test
    fun showsLinkStatusAndSettings() {
        ActivityScenario.launch<HomeActivity>(HomeActivity.intent(TestEnv.ctx)).use {
            onView(withText("TokTikLite")).check(matches(isDisplayed()))
            onView(tagged("link-status")).check(matches(isDisplayed()))
            onView(tagged("link-settings")).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun aPastedShareTextOpensThePlayer() {
        ActivityScenario.launch<HomeActivity>(HomeActivity.intent(TestEnv.ctx)).use {
            onView(tagged("link-input")).perform(scrollTo(),
                replaceText("Look at this https://www.tiktok.com/@a/video/7301234567890123456 lol"), closeSoftKeyboard())
            onView(tagged("watch")).perform(scrollTo(), click())
            TestEnv.eventually {
                val focus = TestEnv.shell("dumpsys activity activities")
                check(focus.contains("com.toktiklite/.MainActivity")) { "player didn't open" }
            }
        }
    }
}
