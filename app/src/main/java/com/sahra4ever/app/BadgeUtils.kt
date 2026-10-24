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
import java.util.Calendar
import java.util.Locale

enum class ContentBadge {
    COMING_SOON
}

fun computeBadge(dateStr: String?, overview: String?): ContentBadge? {
    if (isFutureDate(dateStr)) return ContentBadge.COMING_SOON
    return null
}

/**
 * ✅ (نسخة دفاعية أكثر متانة)
 * - تُنظّف السلسلة (trim) وتأخذ أول 10 أحرف فقط (yyyy-MM-dd) حتى لو جاء التاريخ
 *   بصيغة أطول (مثل وجود جزء وقت/منطقة زمنية إضافية في بعض استجابات TMDB النادرة)
 *   بدل فشل الـ parse بصمت بسبب isLenient = false.
 * - المقارنة تتم مع "بداية اليوم الحالي" (منتصف الليل) بدل اللحظة الحالية الدقيقة،
 *   لضمان: فيلم يصدر اليوم بالضبط => غير قادم (متاح)، فيلم يصدر غدًا أو بعده => قادم،
 *   بغض النظر عن الساعة الحالية وقت تشغيل التطبيق.
 */
private fun isFutureDate(dateStr: String?): Boolean {
    val cleaned = dateStr?.trim()
    if (cleaned.isNullOrBlank() || cleaned.length < 10) return false

    return try {
        val datePart = cleaned.take(10) // "yyyy-MM-dd"
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.isLenient = false
        val releaseDate = sdf.parse(datePart) ?: return false

        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        releaseDate.after(startOfToday)
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