package com.sahra4ever.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.*
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

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(movieId: Int, mediaType: String, title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    val servers = listOf(
        "Server 1" to "https://embedmaster.link/v2hggflri06a65jvvp/",
        "Server 2" to "https://vsembed.ru/embed/",
        "Server 3" to "https://vidlink.pro/",
        "Server 4" to "https://vidsrc.wiki/embed/"
    )

    var selectedServer by remember { mutableStateOf(servers[1].second) } 
    var currentUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    
    var showOverlay by remember { mutableStateOf(true) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            delay(3000)
            showOverlay = false
        }
    }

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val windowInsetsController = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(selectedServer) {
        isLoading = true
        currentUrl = if (mediaType == "tv") {
            "${selectedServer}tv/$movieId/1/1"
        } else {
            "${selectedServer}movie/$movieId"
        }
    }

    if (customView != null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { customView!! },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = {
                    customViewCallback?.onCustomViewHidden()
                    customView = null
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Text("X", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (currentUrl.isNotEmpty()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false 
                            
                            // منع فتح النوافذ المنبثقة
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.setSupportMultipleWindows(false)

                            // قائمة النطاقات الإعلانية (بناءً على اقتراح Claude)
                            val adDomains = listOf(
                                "aliexpress", "shein", "popads", "2.1trk", "doubleclick", 
                                "adservice", "googleads", "adsystem", "popcash", "propellerads"
                            )

                            webViewClient = object : WebViewClient() {
                                // 1. حظر طلبات الشبكة
                                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                    val url = request?.url.toString().lowercase()
                                    for (domain in adDomains) {
                                        if (url.contains(domain)) {
                                            return WebResourceResponse("text/plain", "utf-8", null) // قتل الطلب
                                        }
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    
                                    // حقن JS لمسح الطبقات والتشغيل التلقائي
                                    val jsCode = """
                                        javascript:(function() {
                                            window.open = function() { return null; };
                                            setTimeout(function() {
                                                var playButtons = document.querySelectorAll('button, div[class*="play"], .vjs-big-play-button');
                                                for(var j=0; j<playButtons.length; j++) {
                                                    playButtons[j].click();
                                                }
                                            }, 1000);
                                        })()
                                    """.trimIndent()
                                    view?.evaluateJavascript(jsCode, null)
                                }

                                // 2. سلاح Claude الأقوى: منع أي تنقل خارج السيرفر الأساسي
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    if (url == null) return false
                                    val lowerUrl = url.lowercase()
                                    
                                    // السماح فقط بالبقاء داخل السيرفر المختار
                                    if (lowerUrl.contains("vsembed") || lowerUrl.contains("embedmaster") || 
                                        lowerUrl.contains("vidlink") || lowerUrl.contains("vidsrc")) {
                                        return false // اسمح بالتحميل
                                    }
                                    
                                    // منع أي رابط آخر (إعلانات، متاجر، إلخ)
                                    return true 
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                // منع النوافذ الجديدة نهائياً
                                override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                                    return false 
                                }

                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    super.onShowCustomView(view, callback)
                                    customView = view
                                    customViewCallback = callback
                                }
                                override fun onHideCustomView() {
                                    super.onHideCustomView()
                                    customView = null
                                    customViewCallback = null
                                }
                            }
                            loadUrl(currentUrl)
                        }
                    },
                    update = { webView ->
                        webView.loadUrl(currentUrl)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showOverlay = !showOverlay 
                        }
                )
            }

            AnimatedVisibility(
                visible = showOverlay,
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("جاري تهيئة مشغل البث...", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}