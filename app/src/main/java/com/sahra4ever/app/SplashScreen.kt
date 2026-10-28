package com.sahra4ever.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// الخلفية الداكنة للشاشة
private val SplashBackground = Color(0xFF0D0F14)

@Composable
fun SplashScreen(
    // تم حذف glowColor لأنه لم يعد هناك ضوء
    awaitReady: suspend () -> Unit = {},
    onFinished: () -> Unit
) {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.8f) }
    
    val currentOnFinished by rememberUpdatedState(onFinished)
    val currentAwaitReady by rememberUpdatedState(awaitReady)

    LaunchedEffect(Unit) {
        // 1) حركة ظهور اللوجو وتكبيره خلال 1.5 ثانية
        coroutineScope {
            launch { logoAlpha.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }
            launch { logoScale.animateTo(1.05f, tween(1500, easing = FastOutSlowInEasing)) }
        }
        
        // 2) انتظار جاهزية البيانات (بدون حجب الشاشة)
        currentAwaitReady()
        
        // 3) الانتقال للواجهة الرئيسية
        currentOnFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground),
        contentAlignment = Alignment.Center
    ) {
        // عرض اللوجو فقط بدون أي هالة أو ضوء
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Sahra4Ever Logo",
            modifier = Modifier
                .size(160.dp) // حجم اللوجو
                .scale(logoScale.value)
                .alpha(logoAlpha.value)
        )
    }
}