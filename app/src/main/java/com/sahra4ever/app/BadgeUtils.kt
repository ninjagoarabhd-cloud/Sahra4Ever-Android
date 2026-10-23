package com.sahra4ever.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ContentBadge { COMING_SOON, LOW_QUALITY }

private val LowQualityKeywords = listOf("cam", "hdts", "hd-ts", "ts-rip", "tsrip", "camrip", "hdcam")

/**
 * يحدد الشارة المناسبة اعتماداً على تاريخ الإصدار ونص الوصف.
 * أولوية "يعرض قريباً" أعلى من شارة الجودة لأن الفيلم لم يُطرح أصلاً.
 */
fun computeBadge(dateStr: String?, overview: String?): ContentBadge? {
    if (isFutureDate(dateStr)) return ContentBadge.COMING_SOON
    val text = overview?.lowercase(Locale.US).orEmpty()
    if (text.isNotEmpty() && LowQualityKeywords.any { text.contains(it) }) return ContentBadge.LOW_QUALITY
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
        ContentBadge.COMING_SOON -> "يعرض قريباً" to Color(0xFF7B2CFF)
        ContentBadge.LOW_QUALITY -> "انتظر النسخة عالية الجودة" to Color(0xFFE65100)
    }
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}