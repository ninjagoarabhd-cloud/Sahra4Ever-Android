package com.sahra4ever.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream

private const val TAG = "PlayerWeb"

private data class ServerProfile(
    val name: String,
    val baseUrl: String,
    val strict: Boolean,
    val allowedHosts: List<String> = emptyList(),
    val lockNavigation: Boolean = false
) {
    val autoClickPlay: Boolean get() = strict
}

// ✅ الترتيب المطلوب: VsEmbed هو السيرفر 1 (VIP)، EmbedMaster هو السيرفر 2
private val SERVERS = listOf(
    ServerProfile("Server 1", "https://vsembed.ru/embed/", strict = true), // VsEmbed
    ServerProfile("Server 2", "https://embedmaster.link/v2hggflri06a65jvvp/", strict = false, allowedHosts = listOf("embedmaster.link")), // EmbedMaster
    ServerProfile("Server 3", "https://vidlink.pro/", strict = true),
    ServerProfile("Server 4", "https://vidsrc.wiki/embed/", strict = true)
)

private val AD_KEYWORDS = listOf(
    "aliexpress", "shein", "popads", "2.1trk", "doubleclick",
    "adservice", "googleads", "adsystem", "popcash", "propellerads"
)
private val STRICT_NAV_KEYWORDS = listOf("vsembed", "embedmaster", "vidlink", "vidsrc")
private val OK_SCHEMES = setOf("http", "https", "about", "blob")

private const val AUTO_PLAY_JS = """
javascript:(function() {
    window.open = function() { return null; };
    setTimeout(function() {
        var playButtons = document.querySelectorAll('button, div[class*="play"], .vjs-big-play-button');
        for(var j=0; j<playButtons.length; j++) {
            playButtons[j].click();
        }
    }, 1000);
})()
"""

private fun emptyResponse() =
    WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))

private fun hostMatches(host: String, domains: List<String>) =
    domains.any { host == it || host.endsWith(".$it") }

private class PlayerWebViewClient(
    private val profile: ServerProfile,
    private val debug: Boolean,
    private val onStarted: () -> Unit,
    private val onFinished: () -> Unit
) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val uri = request?.url ?: return null
        val blocked = if (profile.strict) {
            val full = uri.toString().lowercase()
            AD_KEYWORDS.any { full.contains(it) }
        } else {
            val host = uri.host?.lowercase().orEmpty()
            AD_KEYWORDS.any { host.contains(it) }
        }
        if (blocked) {
            if (debug) Log.d(TAG, "BLOCK request  $uri")
            return emptyResponse()
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val req = request ?: return false
        val allow = if (profile.strict) strictAllows(req) else relaxedAllows(req)
        if (debug) {
            Log.d(TAG, "${if (allow) "ALLOW" else "BLOCK"} nav  main=${req.isForMainFrame} gesture=${req.hasGesture()}  ${req.url}")
        }
        return !allow
    }

    private fun strictAllows(req: WebResourceRequest): Boolean {
        val url = req.url.toString().lowercase()
        return STRICT_NAV_KEYWORDS.any { url.contains(it) }
    }

    private fun relaxedAllows(req: WebResourceRequest): Boolean {
        val scheme = req.url.scheme?.lowercase()
        if (scheme !in OK_SCHEMES) return false
        val host = req.url.host?.lowercase().orEmpty()
        if (AD_KEYWORDS.any { host.contains(it) }) return false
        if (hostMatches(host, profile.allowedHosts)) return true
        if (!req.isForMainFrame) return true 
        return !profile.lockNavigation && !req.hasGesture()
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onStarted()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onFinished()
        if (profile.autoClickPlay) view?.evaluateJavascript(AUTO_PLAY_JS.trimIndent(), null)
    }
}

private class PlayerChromeClient(
    private val onLoaded: () -> Unit,
    private val onShowCustom: (View, CustomViewCallback?) -> Unit,
    private val onHideCustom: () -> Unit
) : WebChromeClient() {

    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean = false

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        if (newProgress >= 75) onLoaded()
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view != null) onShowCustom(view, callback)
    }

    override fun onHideCustomView() = onHideCustom()
}

@SuppressLint("SetJavaScriptEnabled")
private fun createPlayerWebView(
    ctx: Context,
    profile: ServerProfile,
    debug: Boolean,
    onStarted: () -> Unit,
    onLoaded: () -> Unit,
    onShowCustom: (View, WebChromeClient.CustomViewCallback?) -> Unit,
    onHideCustom: () -> Unit
): WebView {
    if (debug) WebView.setWebContentsDebuggingEnabled(true)
    return WebView(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(!profile.strict)
        }
        if (!profile.strict) CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

        webViewClient = PlayerWebViewClient(profile, debug, onStarted, onLoaded)
        webChromeClient = PlayerChromeClient(onLoaded, onShowCustom, onHideCustom)
    }
}

@Composable
fun PlayerScreen(
    movieId: Int,
    mediaType: String,
    title: String,
    season: Int,
    episode: Int,
    initialServerIndex: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val debug = remember { (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }

    val profile = SERVERS[initialServerIndex.coerceIn(SERVERS.indices)]
    val url = remember(profile, mediaType, movieId, season, episode) {
        if (mediaType == "tv") "${profile.baseUrl}tv/$movieId/$season/$episode"
        else "${profile.baseUrl}movie/$movieId"
    }

    var isLoading by remember(url) { mutableStateOf(true) }
    var showOverlay by remember { mutableStateOf(true) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    LaunchedEffect(url) {
        delay(10_000)
        isLoading = false
    }

    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            delay(3000)
            showOverlay = false
        }
    }

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        key(url) {
            AndroidView(
                factory = { ctx ->
                    createPlayerWebView(
                        ctx = ctx,
                        profile = profile,
                        debug = debug,
                        onStarted = { isLoading = true },
                        onLoaded = { isLoading = false },
                        onShowCustom = { v, cb -> customView = v; customViewCallback = cb },
                        onHideCustom = { customView = null; customViewCallback = null }
                    ).also { it.loadUrl(url) } 
                },
                onRelease = { it.stopLoading(); it.destroy() },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showOverlay = !showOverlay }
            )
        }

        val cv = customView

        AnimatedVisibility(
            visible = showOverlay && cv == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text(text = title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (isLoading && cv == null) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("جاري تهيئة مشغل البث...", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        if (cv != null) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    factory = { (cv.parent as? ViewGroup)?.removeView(cv); cv },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = {
                        customViewCallback?.onCustomViewHidden()
                        customView = null
                        customViewCallback = null
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Text("X", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}