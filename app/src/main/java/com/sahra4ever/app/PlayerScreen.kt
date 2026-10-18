package com.sahra4ever.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(movieId: Int, mediaType: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    val servers = listOf(
        "Server 1" to "https://embedmaster.link/v2hggflri06a65jvvp/",
        "Server 2" to "https://vsembed.ru/embed/",
        "Server 3" to "https://vidlink.pro/",
        "Server 4" to "https://vidsrc.wiki/embed/"
    )

    var selectedServer by remember { mutableStateOf(servers[1].second) } // جعلنا VsEmbed هو الافتراضي لأنك تريد التركيز عليه
    var currentUrl by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(true) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    LaunchedEffect(selectedServer) {
        isLoading = true
        currentUrl = if (mediaType == "tv") {
            "${selectedServer}tv/$movieId/1/1" 
        } else {
            "${selectedServer}movie/$movieId"
        }
    }

    // التعامل مع وضع الشاشة الكاملة (إخفاء شريط الحالة)
    DisposableEffect(customView) {
        val windowInsetsController = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        if (customView != null) {
            // تفعيل وضع Immersive (شاشة كاملة)
            windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            // العودة للوضع العادي
            windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
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
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("المشاهدة", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
            },
            containerColor = Color.Black
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f) 
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
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
                                    settings.mediaPlaybackRequiresUserGesture = false // تشغيل تلقائي
                                    settings.javaScriptCanOpenWindowsAutomatically = false
                                    settings.setSupportMultipleWindows(false)
                                    
                                    // كود JavaScript هجومي لإخفاء إعلانات VsEmbed
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            isLoading = true
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isLoading = false
                                            
                                            // سكربت متقدم لإخفاء الإعلانات
                                            val jsCode = """
                                                javascript:(function() {
                                                    // إخفاء العناصر التي تحتوي على كلمات دالة على الإعلانات
                                                    var elements = document.querySelectorAll('*');
                                                    for (var i = 0; i < elements.length; i++) {
                                                        var el = elements[i];
                                                        if (el.id && el.id.toLowerCase().includes('ad')) el.style.display = 'none';
                                                        if (el.className && typeof el.className === 'string' && el.className.toLowerCase().includes('ad')) el.style.display = 'none';
                                                        
                                                        // إخفاء الأزرار التي تفتح روابط خارجية (مثل علي إكسبريس)
                                                        if (el.tagName === 'A' && el.href && (el.href.includes('aliexpress') || el.href.includes('play.google'))) {
                                                            el.style.display = 'none';
                                                        }
                                                    }
                                                    // محاولة إغلاق أي نوافذ منبثقة
                                                    window.open = function(){};
                                                })()
                                            """.trimIndent()
                                            view?.evaluateJavascript(jsCode, null)
                                        }

                                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                            if (url?.startsWith("intent://") == true || 
                                                url?.startsWith("market://") == true || 
                                                url?.startsWith("https://play.google.com") == true ||
                                                url?.contains("aliexpress") == true) {
                                                return true // منع فتح التطبيقات الخارجية
                                            }
                                            return false
                                        }
                                    }

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                            super.onShowCustomView(view, callback)
                                            customView = view
                                            customViewCallback = callback
                                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        }

                                        override fun onHideCustomView() {
                                            super.onHideCustomView()
                                            customView = null
                                            customViewCallback = null
                                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        }
                                    }
                                    loadUrl(currentUrl)
                                }
                            },
                            update = { webView ->
                                webView.loadUrl(currentUrl)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
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

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(servers) { server ->
                        Button(
                            onClick = { selectedServer = server.second },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedServer == server.second) Color.Red else Color.DarkGray
                            )
                        ) {
                            Text(server.first, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}