package com.sahra4ever.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val SplashBackground = Color(0xFF0D0F14)
private val DefaultGlowColor = Color(0xFFE5A93C) // غيّره ليناسب ألوان شعارك

@Composable
fun SplashScreen(
    glowColor: Color = DefaultGlowColor,
    onFinished: () -> Unit
) {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.8f) }
    val glow = remember { Animatable(0f) }
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        // 1) Fade-In + Scale-Up خلال 1.5 ثانية
        coroutineScope {
            launch { logoAlpha.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }
            launch { logoScale.animateTo(1.05f, tween(1500, easing = FastOutSlowInEasing)) }
        }
        // 2) Pulse Glow (نبضة واحدة ≈ 0.7 ثانية) — اجعلها 0 لو أردت الانتقال فور 1.5 ثانية
        glow.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
        glow.animateTo(0.3f, tween(350, easing = FastOutSlowInEasing))
        // 3) الانتقال
        currentOnFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground),
        contentAlignment = Alignment.Center
    ) {
        // هالة الإضاءة خلف الشعار
        Canvas(modifier = Modifier.size(360.dp)) {
            val radius = (110.dp.toPx()) + (40.dp.toPx() * glow.value)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.40f * glow.value),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = radius
                ),
                radius = radius,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = null,
            modifier = Modifier
                .size(160.dp)
                .scale(logoScale.value)
                .alpha(logoAlpha.value)
        )
    }
}