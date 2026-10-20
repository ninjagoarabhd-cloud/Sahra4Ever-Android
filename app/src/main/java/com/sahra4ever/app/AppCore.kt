package com.sahra4ever.app

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

// ✅ تأكد من وجود مفتاح الـ API هنا
internal const val TMDB_API_KEY = "26097366152d7131fde0f53b80ee37de"
internal const val TMDB_IMG = "https://image.tmdb.org/t/p/"

internal fun tmdbImg(path: String?, size: String): String? = path?.let { "$TMDB_IMG$size$it" }

internal fun Movie.displayTitle(): String = title ?: name ?: ""

/** نسخة واحدة من Retrofit/OkHttp لكل التطبيق (بدل إنشاء واحدة جديدة مع كل طلب). */
internal object TmdbClient {
    val api by lazy { TMDBApi.create() }
}

/** Scope للأعمال القصيرة التي يجب ألا تُلغى عند مغادرة الشاشة. */
internal val AppScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

internal inline fun <T> tryOrNull(block: () -> T): T? =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

/** كاش للألوان المستخرجة: نفس الصورة لا تُحلَّل مرتين. */
internal object PaletteCache {
    private val cache = LruCache<String, Int>(200)

    suspend fun colorFor(context: Context, url: String?): Color? {
        if (url == null) return null
        cache.get(url)?.let { return Color(it) }

        val app = context.applicationContext
        val rgb: Int? = withContext(Dispatchers.Default) {
            try {
                val request = ImageRequest.Builder(app)
                    .data(url)
                    .allowHardware(false)
                    .size(64) // صورة مصغّرة تكفي لاستخراج اللون
                    .build()
                val bitmap = (app.imageLoader.execute(request) as? SuccessResult)
                    ?.drawable?.toBitmap() ?: return@withContext null
                val p = Palette.from(bitmap).maximumColorCount(8).generate()
                val swatch = p.darkVibrantSwatch ?: p.vibrantSwatch
                    ?: p.darkMutedSwatch ?: p.dominantSwatch
                swatch?.rgb
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
        if (rgb != null) cache.put(url, rgb)
        return rgb?.let { Color(it) }
    }
}