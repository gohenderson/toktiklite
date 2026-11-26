package com.toktiklite

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

/**
 * Shows one TikTok video in TikTok's embed player: no feed, no recommendations, no account.
 * Opened by tapping a TikTok link anywhere on the phone (or from the home screen).
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView

    /** The last URL this screen asked the WebView to load (for tests and diagnostics). */
    var lastLoadedUrl: String? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        // Android 15 draws apps edge to edge: keep the player clear of the status and
        // navigation bars (earlier versions already do).
        if (android.os.Build.VERSION.SDK_INT >= 35) {
            findViewById<android.view.View>(R.id.root).setOnApplyWindowInsetsListener { v, insets ->
                val bars = insets.getInsets(
                    android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout(),
                )
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }
        webView.setBackgroundColor(android.graphics.Color.BLACK)
        configureWebView()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val incoming = intent?.data?.toString()
        // Only TikTok links: this screen is exported, so any app could send it any URL.
        val url = TikTokLinks.forIncoming(incoming)
        if (url == null) {
            if (incoming != null) Toast.makeText(this, "Not a TikTok link", Toast.LENGTH_SHORT).show()
            if (lastLoadedUrl == null) finish()
            return
        }
        load(url)
    }

    private fun load(url: String) {
        lastLoadedUrl = url
        webView.loadUrl(url)
    }

    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true // TikTok's embed player needs it; only TikTok pages load here
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val url = uri.toString()

                return when (val to = TikTokLinks.navigate(url)) {
                    // Not TikTok, or not https (fail closed in this JavaScript-enabled WebView):
                    // a link someone tapped opens in the browser; anything else is dropped.
                    TikTokLinks.Navigation.Leave -> {
                        if (request.isForMainFrame && request.hasGesture()) openExternally(uri)
                        true
                    }
                    // A post (a short link's redirect, say): the embed player.
                    is TikTokLinks.Navigation.Embed -> {
                        lastLoadedUrl = to.url
                        view.loadUrl(to.url)
                        true
                    }
                    // Another TikTok page (a short link still redirecting): let it load.
                    TikTokLinks.Navigation.Load -> false
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                // Black behind the player, whatever the page's own background.
                if (TikTokLinks.isTikTok(url)) {
                    view.evaluateJavascript(
                        "document.documentElement.style.background='#000';" +
                            "if(document.body)document.body.style.background='#000';", null,
                    )
                }
            }
        }
    }

    private fun openExternally(uri: Uri) {
        if (uri.scheme != "https" && uri.scheme != "http") return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (e: ActivityNotFoundException) {
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
