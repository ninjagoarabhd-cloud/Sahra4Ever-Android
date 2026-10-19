package com.sahra4ever.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
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
    
    // متغيرات التحكم في الواجهة والملء
    var showOverlay by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }

    // إخفاء الواجهة العلوية تلقائياً بعد 3 ثواني
    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            delay(3000)
            showOverlay = false
        }
    }

    // إجبار الوضع الأفقي وإخفاء أشرطة النظام
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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // مشغل الفيديو
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
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                
                                // كود مسح الإعلانات والتشغيل التلقائي
                                val jsCode = """
                                    javascript:(function() {
                                        var elements = document.querySelectorAll('*');
                                        for (var i = 0; i < elements.length; i++) {
                                            var el = elements[i];
                                            var text = (el.innerText || "").toLowerCase();
                                            var src = (el.src || "").toLowerCase();
                                            if (text.includes('shein') || text.includes('aliexpress') || src.includes('aliexpress')) {
                                                el.style.display = 'none';
                                            }
                                        }
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

                            // حظر روابط الإعلانات
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
                            // عندما يضغط المستخدم على زر التوسيع [ ]
                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                super.onShowCustomView(view, callback)
                                isFullscreen = true
                                showOverlay = false // إخفاء الواجهة العربية
                                val windowInsetsController = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
                                windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
                            }

                            // عندما يخرج المستخدم من وضع التوسيع
                            override fun onHideCustomView() {
                                super.onHideCustomView()
                                isFullscreen = false
                                showOverlay = true
                                val windowInsetsController = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
                                windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
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
                        indication = null // إزالة تأثير الضغط
                    ) {
                        // عند لمس الشاشة، أظهر الواجهة العلوية
                        showOverlay = !showOverlay 
                    }
            )
        }

        // الواجهة العربية العلوية (تظهر وتختفي)
        AnimatedVisibility(
            visible = showOverlay && !isFullscreen, // لا تظهر أبداً في وضع التوسيع الكامل
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
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // شاشة التحميل
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("جاري تهيئة مشغل البث...", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}