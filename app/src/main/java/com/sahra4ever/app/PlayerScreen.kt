package com.sahra4ever.app

import android.annotation.SuppressLint
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(movieId: Int, mediaType: String, onBack: () -> Unit) {
    // الترتيب الجديد للسيرفرات بناءً على طلبك
    val servers = listOf(
        "Server 1" to "https://embedmaster.link/v2hggflri06a65jvvp/",
        "Server 2" to "https://vsembed.ru/embed/",
        "Server 3" to "https://vidlink.pro/",
        "Server 4" to "https://vidsrc.wiki/embed/"
    )

    var selectedServer by remember { mutableStateOf(servers[0].second) }
    var currentUrl by remember { mutableStateOf("") }

    // بناء الرابط
    LaunchedEffect(selectedServer) {
        currentUrl = if (mediaType == "tv") {
            "${selectedServer}tv/$movieId/1/1" 
        } else {
            "${selectedServer}movie/$movieId"
        }
    }

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
            // شاشة العرض (في الأعلى بنسبة 16:9)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f) 
                    .background(Color.Black)
            ) {
                if (currentUrl.isNotEmpty()) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                
                                webChromeClient = WebChromeClient() 
                                webViewClient = WebViewClient()
                                loadUrl(currentUrl)
                            }
                        },
                        update = { webView ->
                            webView.loadUrl(currentUrl)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // أزرار السيرفرات (تحت شاشة العرض)
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