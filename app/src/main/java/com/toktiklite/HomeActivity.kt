package com.toktiklite

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * What you see when you open TokTikLite from the launcher: what it does, whether it's set
 * to open TikTok links, a shortcut to that setting, and a box to paste a link.
 */
class HomeActivity : Activity() {

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun text(s: String, size: Float, bold: Boolean = false, color: Int = WHITE) = TextView(this).apply {
        text = s
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun LinearLayout.add(v: View, top: Int = 0) =
        addView(v, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = top
        })

    private fun render() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(48), dp(24), dp(32))
        }
        col.add(text("TokTikLite", 34f, bold = true))
        col.add(text("One TikTok video at a time. No feed, no algorithm, no scrolling, no account.", 17f, color = SOFT), dp(8))

        val handles = handlesLinks()
        val status = when (handles) {
            true -> "✓ TikTok links open here."
            false -> "TikTok links don't open here yet."
            null -> "Choose TokTikLite (and \"Always\") the next time you tap a TikTok link."
        }
        col.add(text(status, 17f, bold = true, color = if (handles == true) ACCENT else WHITE).apply { tag = "link-status" }, dp(28))
        if (handles != true) {
            col.add(text("Turn on \"Open supported links\" and tick the TikTok addresses. If the TikTok app is " +
                "installed, turn its link handling off in its own App info.", 15f, color = SOFT), dp(6))
        }
        col.add(Button(this).apply {
            text = "Link settings"
            tag = "link-settings"
            setOnClickListener { openLinkSettings() }
        }, dp(12))

        col.add(text("Or paste a link", 17f, bold = true), dp(32))
        val input = EditText(this).apply {
            hint = "https://www.tiktok.com/@user/video/…"
            setTextColor(WHITE)
            setHintTextColor(0x66FFFFFF)
            tag = "link-input"
            setSingleLine()
        }
        col.add(input, dp(6))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        actions.addView(Button(this).apply {
            text = "Paste"
            setOnClickListener {
                val clip = getSystemService(ClipboardManager::class.java).primaryClip
                val s = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this@HomeActivity)?.toString()
                if (s != null) input.setText(s)
            }
        })
        actions.addView(Button(this).apply {
            text = "Watch"
            tag = "watch"
            setOnClickListener { watch(input.text.toString()) }
        })
        col.add(actions, dp(4))

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(col)
        })
    }

    private fun watch(pasted: String) {
        val link = TikTokLinks.findIn(pasted)
        if (link == null) {
            Toast.makeText(this, "That isn't a TikTok link.", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse(link)))
    }

    /** true / false on Android 12+ (where it can be checked), null before that. */
    private fun handlesLinks(): Boolean? {
        if (Build.VERSION.SDK_INT < 31) return null
        val state = getSystemService(DomainVerificationManager::class.java)
            ?.getDomainVerificationUserState(packageName) ?: return null
        if (!state.isLinkHandlingAllowed) return false
        return state.hostToStateMap.values.any {
            it == DomainVerificationUserState.DOMAIN_STATE_SELECTED || it == DomainVerificationUserState.DOMAIN_STATE_VERIFIED
        }
    }

    private fun openLinkSettings() {
        val uri = Uri.parse("package:$packageName")
        val intent = if (Build.VERSION.SDK_INT >= 31) {
            Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, uri)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
        }
    }

    companion object {
        private const val WHITE = Color.WHITE
        private const val SOFT = 0xB3FFFFFF.toInt()
        private const val ACCENT = 0xFF25F4EE.toInt()

        fun intent(context: Context) = Intent(context, HomeActivity::class.java)
    }
}
