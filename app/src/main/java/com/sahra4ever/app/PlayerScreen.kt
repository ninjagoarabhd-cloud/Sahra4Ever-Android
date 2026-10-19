package com.sahra4ever.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
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
    
    // متغيرات للتحكم في الشاشة الكاملة الخاصة بالمشغل
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

    // إذا تم تفعيل الشاشة الكاملة من داخل المشغل، اعرض الفيديو فقط
    if (customView != null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { customView!! },
                modifier = Modifier.fillMaxSize()
            )
            // زر إغلاق الشاشة الكاملة (يمكنك إخفاؤه أو تعديله)
            IconButton(
                onClick = {
                    customViewCallback?.onCustomViewHidden()
                    customView = null
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                // أيقونة إغلاق بسيطة
                Text("X", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        // الشاشة العادية
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
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.setSupportMultipleWindows(false)

                            webViewClient = object : WebViewClient() {
                                // حظر الشبكة للإعلانات
                                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                    val url = request?.url.toString().lowercase()
                                    if (url.contains("aliexpress") || url.contains("shein") || url.contains("doubleclick") || url.contains("popads")) {
                                        return WebResourceResponse("text/plain", "utf-8", null)
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
                                    
                                    // كود حقن CSS قوي جداً لإخفاء الإعلانات
                                    val jsCode = """
                                        javascript:(function() {
                                            // إخفاء أي عنصر يحتوي على كلمة ad أو sponsor
                                            var style = document.createElement('style');
                                            style.innerHTML = '.ad, .ads, .adsbygoogle, [id*="ad-"], [id*="banner"], iframe[src*="ad"] { display: none !important; }';
                                            document.head.appendChild(style);
                                            
                                            // إخفاء SHEIN و AliExpress
                                            var elements = document.querySelectorAll('*');
                                            for (var i = 0; i < elements.length; i++) {
                                                var el = elements[i];
                                                var text = (el.innerText || "").toLowerCase();
                                                if (text.includes('shein') || text.includes('aliexpress')) {
                                                    el.style.display = 'none';
                                                }
                                            }
                                            
                                            // الضغط على زر التشغيل
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

                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    if (url?.startsWith("intent://") == true || 
                                        url?.startsWith("market://") == true || 
                                        url?.contains("aliexpress") == true ||
                                        url?.contains("shein") == true) {
                                        return true 
                                    }
                                    return false
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                // إعطاء المتصفح نافذة ليضع فيها الفيديو عند التوسيع
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