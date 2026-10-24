package com.sahra4ever.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ContentBadge {
    COMING_SOON
}

fun computeBadge(dateStr: String?, overview: String?): ContentBadge? {
    if (isFutureDate(dateStr)) return ContentBadge.COMING_SOON
    return null
}

private fun isFutureDate(dateStr: String?): Boolean {
    if (dateStr.isNullOrBlank()) return false
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val date = sdf.parse(dateStr) ?: return false
        date.after(Date())
    } catch (e: Exception) {
        false
    }
}

@Composable
fun BadgeLabel(badge: ContentBadge, modifier: Modifier = Modifier) {
    val (text, color) = when (badge) {
        ContentBadge.COMING_SOON -> "يعرض قريباً" to Color(0xFFE53935) // أحمر
    }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.6f),
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            .clip(shape)
            .background(color)
            .border(1.5.dp, Color.White.copy(alpha = 0.95f), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}