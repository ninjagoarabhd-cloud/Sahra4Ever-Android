package com.sahra4ever.app

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset // ✅ تم إضافة الاستيراد
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow // ✅ تم إضافة الاستيراد
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private const val API_KEY = "26097366152d7131fde0f53b80ee37de"
private const val IMG = "https://image.tmdb.org/t/p/"

private val Pink = Color(0xFFFF2FA6)
private val Purple = Color(0xFF7B2CFF)
private val Gold = Color(0xFFFFC83D)
private val Ink = Color(0xFF0F0E14)
private val DefaultTop = Color(0xFF3A3650)
private val GlassFill = Color.White.copy(alpha = 0.12f)
private val GlassStroke = Color.White.copy(alpha = 0.22f)
private val Muted = Color(0xFFD0CFDA)

private fun img(path: String?, size: String = "w500"): String? = path?.let { "$IMG$size$it" }

private data class DetailsUi(
    val title: String,
    val overview: String,
    val backdropPath: String?,
    val posterPath: String?,
    val rating: Double?,
    val date: String?
)

private suspend fun fetchDetails(id: Int, mediaType: String): DetailsUi {
    val api = TMDBApi.create()
    return if (mediaType == "tv") {
        val d = api.getTvDetails(id, API_KEY)
        DetailsUi(
            title = d.name ?: "",
            overview = d.overview ?: "",
            backdropPath = d.backdropPath,
            posterPath = d.posterPath,
            rating = d.voteAverage,
            date = d.firstAirDate
        )
    } else {
        val d = api.getMovieDetails(id, API_KEY)
        DetailsUi(
            title = d.title ?: "",
            overview = d.overview ?: "",
            backdropPath = d.backdropPath,
            posterPath = d.posterPath,
            rating = d.voteAverage,
            date = d.releaseDate
        )
    }
}

private suspend fun extractColor(context: Context, url: String?): Color? {
    if (url == null) return null
    return withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(200).build()
            val bitmap = (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
                ?: return@withContext null
            val p = Palette.from(bitmap).maximumColorCount(16).generate()
            val swatch = p.darkVibrantSwatch ?: p.vibrantSwatch ?: p.darkMutedSwatch ?: p.dominantSwatch
            swatch?.let { Color(it.rgb) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
// ✅ تم إضافة المتغيرات المطلوبة هنا
fun DetailsScreen(
    movieId: Int,
    mediaType: String,
    initialSeason: Int = 1,
    initialEpisode: Int = 1,
    autoShowServers: Boolean = false,
    onBack: () -> Unit,
    onPlayClick: (Int, String, String, Int, Int, Int) -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        DetailsContent(movieId, mediaType, initialSeason, initialEpisode, autoShowServers, onBack, onPlayClick)
    }
}

@Composable
private fun DetailsContent(
    movieId: Int,
    mediaType: String,
    initialSeason: Int,
    initialEpisode: Int,
    autoShowServers: Boolean,
    onBack: () -> Unit,
    onPlayClick: (Int, String, String, Int, Int, Int) -> Unit
) {
    val context = LocalContext.current
    var ui by remember(movieId, mediaType) { mutableStateOf<DetailsUi?>(null) }
    var accent by remember(movieId, mediaType) { mutableStateOf<Color?>(null) }
    var error by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(movieId, mediaType, reloadKey) {
        error = false
        try {
            val d = fetchDetails(movieId, mediaType)
            ui = d
            accent = extractColor(context, img(d.backdropPath ?: d.posterPath, "w300"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = true
        }
    }

    val top by animateColorAsState(
        accent?.let { lerpColor(it, Color.Black, 0.3f) } ?: DefaultTop, tween(700), label = "top"
    )
    val bottom by animateColorAsState(
        accent?.let { lerpColor(it, Ink, 0.88f) } ?: Ink, tween(700), label = "bottom"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, bottom)))
    ) {
        val d = ui
        when {
            d != null -> DetailsBody(d) {
                // حفظ في السجل
                HistoryManager.addToHistory(
                    context = context,
                    item = WatchHistory(
                        id = movieId,
                        title = d.title,
                        type = mediaType,
                        posterPath = d.posterPath,
                        season = initialSeason,
                        episode = initialEpisode
                    )
                )
                // التشغيل بالبيانات الصحيحة
                onPlayClick(movieId, mediaType, d.title, initialSeason, initialEpisode, 0)
            }
            error -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("تعذّر تحميل التفاصيل", color = Color.White, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { reloadKey++ },
                    colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White)
                ) { Text("إعادة المحاولة") }
            }
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Pink)
            }
        }

        BackButton(onBack, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp))
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, GlassStroke, CircleShape)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "رجوع", tint = Color.White)
    }
}

@Composable
private fun DetailsBody(d: DetailsUi, onPlay: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val year = d.date?.take(4)?.takeIf { it.length == 4 }
    val rating = d.rating?.takeIf { it > 0 }?.let { String.format(Locale.US, "★ %.1f", it) }
    val overview = d.overview.ifBlank { "لا توجد قصة متاحة حالياً." }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth()) {
            AsyncImage(
                model = img(d.backdropPath ?: d.posterPath, "w780"),
                contentDescription = d.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(0.5f to Color.Black, 1f to Color.Transparent),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )
            Box(
                Modifier.fillMaxWidth().height(120.dp).background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))
                )
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 290.dp, bottom = 32.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    d.title,
                    style = TextStyle(
                        brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFFFD9EF), Color(0xFFD7B6FF))),
                        fontSize = 40.sp,
                        lineHeight = 46.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        shadow = Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 6f), 18f)
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    Modifier
                        .padding(top = 12.dp)
                        .width(48.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Purple, Pink)))
                )

                Row(
                    Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rating?.let { MetaChip(it, Gold) }
                    year?.let { MetaChip(it, Color.White) }
                }

                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    modifier = Modifier.padding(top = 20.dp).fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Outlined.PlayArrow, null)
                    Text(" شاهد الآن", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }

                val shape = RoundedCornerShape(16.dp)
                Column(
                    Modifier
                        .padding(top = 22.dp)
                        .fillMaxWidth()
                        .clip(shape)
                        .background(GlassFill)
                        .border(1.dp, GlassStroke, shape)
                        .clickable { expanded = !expanded }
                        .padding(16.dp)
                ) {
                    Text("القصة", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        overview,
                        color = Muted,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (overview.length > 180) {
                        Text(
                            if (expanded) "عرض أقل" else "المزيد",
                            color = Pink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String, color: Color) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .clip(shape)
            .background(GlassFill)
            .border(1.dp, GlassStroke, shape)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}