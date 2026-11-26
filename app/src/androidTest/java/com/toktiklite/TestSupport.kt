package com.toktiklite

import android.content.Context
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.View
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matcher
import org.hamcrest.Matchers.equalTo
import org.junit.rules.TestWatcher
import org.junit.runner.Description

object TestEnv {
    val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    val ctx: Context get() = instrumentation.targetContext

    /** Runs a shell command as the `shell` user and returns its output. */
    fun shell(cmd: String): String {
        val pfd = instrumentation.uiAutomation.executeShellCommand(cmd)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes().toString(Charsets.UTF_8) }
    }

    /** Screen on, unlocked, no system dialog in front. */
    fun readyDevice() {
        shell("input keyevent KEYCODE_WAKEUP")
        shell("wm dismiss-keyguard")
        shell("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS")
    }

    /** Retries [block] until it stops throwing, or [timeoutMs] passes. */
    fun eventually(timeoutMs: Long = 5_000, block: () -> Unit) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (true) {
            try {
                block()
                return
            } catch (e: Throwable) {
                if (SystemClock.uptimeMillis() > deadline) throw e
                SystemClock.sleep(200)
            }
        }
    }
}

fun tagged(tag: String): Matcher<View> = withTagValue(equalTo<Any>(tag))

/** On failure, saves a screenshot to /data/local/tmp/toktiklite-test-failures/ (CI pulls it). */
class FailureScreenshot : TestWatcher() {
    override fun failed(e: Throwable?, description: Description) {
        val name = "${description.testClass.simpleName}-${description.methodName}"
        try {
            TestEnv.shell("mkdir -p /data/local/tmp/toktiklite-test-failures")
            TestEnv.shell("screencap -p /data/local/tmp/toktiklite-test-failures/$name.png")
        } catch (ignored: Exception) {
        }
    }
}
