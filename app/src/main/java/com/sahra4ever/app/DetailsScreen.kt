package com.sahra4ever.app

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
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
import kotlinx.coroutines.launch
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

val CairoBold = FontFamily(Font(R.font.cairo_bold, FontWeight.Bold))
val CairoLight = FontFamily(Font(R.font.cairo_light, FontWeight.Light))

private fun img(path: String?, size: String = "w500"): String? = path?.let { "$IMG$size$it" }

private data class DetailsUi(
    val title: String,
    val overview: String,
    val backdropPath: String?,
    val posterPath: String?,
    val rating: Double?,
    val date: String?,
    val seasons: List<Season>? = null
)

private suspend fun fetchDetails(id: Int, mediaType: String): DetailsUi {
    val api = TMDBApi.create()
    var title = ""
    var overview = ""
    var backdrop: String? = null
    var poster: String? = null
    var rating: Double? = null
    var date: String? = null
    var seasons: List<Season>? = null

    try {
        if (mediaType == "tv") {
            val arD = api.getTvDetails(id, API_KEY, "ar-SA")
            title = arD.name ?: ""
            overview = arD.overview ?: ""
            backdrop = arD.backdropPath
            poster = arD.posterPath
            rating = arD.voteAverage
            date = arD.firstAirDate
            seasons = arD.seasons

            if (overview.isBlank()) {
                val enD = api.getTvDetails(id, API_KEY, "en-US")
                overview = enD.overview ?: ""
            }
        } else {
            val arD = api.getMovieDetails(id, API_KEY, "ar-SA")
            title = arD.title ?: ""
            overview = arD.overview ?: ""
            backdrop = arD.backdropPath
            poster = arD.posterPath
            rating = arD.voteAverage
            date = arD.releaseDate

            if (overview.isBlank()) {
                val enD = api.getMovieDetails(id, API_KEY, "en-US")
                overview = enD.overview ?: ""
            }
        }
    } catch (e: Exception) { }

    if (overview.isBlank()) overview = "لا توجد قصة متاحة حالياً."

    return DetailsUi(title, overview, backdrop, poster, rating, date, seasons)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
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
        val context = LocalContext.current
        var ui by remember(movieId, mediaType) { mutableStateOf<DetailsUi?>(null) }
        var accent by remember(movieId, mediaType) { mutableStateOf<Color?>(null) }
        var error by remember { mutableStateOf(false) }
        var reloadKey by remember { mutableIntStateOf(0) }

        var showSeasonSheet by remember { mutableStateOf(false) }
        var showServerSheet by remember { mutableStateOf(false) }
        var selectedSeason by remember { mutableStateOf<Season?>(null) }
        var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
        var selectedEpisodeNumber by remember { mutableIntStateOf(initialEpisode) }
        val sheetState = rememberModalBottomSheetState()

        LaunchedEffect(movieId, mediaType, reloadKey) {
            error = false
            try {
                val d = fetchDetails(movieId, mediaType)
                ui = d
                launch(Dispatchers.IO) {
                    accent = extractColor(context, img(d.backdropPath ?: d.posterPath, "w300"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = true
            }
        }

        LaunchedEffect(autoShowServers, ui) {
            if (autoShowServers && ui != null) {
                if (mediaType == "tv") {
                    val targetSeason = ui?.seasons?.find { it.seasonNumber == initialSeason }
                        ?: ui?.seasons?.firstOrNull { it.seasonNumber > 0 }
                    if (targetSeason != null) {
                        selectedSeason = targetSeason
                        episodes = List(targetSeason.episodeCount ?: 1) { Episode(it + 1, "الحلقة ${it + 1}", "") }
                    }
                }
                showServerSheet = true
            }
        }

        fun loadSeason(season: Season) {
            selectedSeason = season
            episodes = List(season.episodeCount ?: 1) { Episode(it + 1, "الحلقة ${it + 1}", "") }
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
                // ✅ جديد: commentsContent يضيف قسم التعليقات أسفل الصفحة (لا تغيير في باقي المنطق)
                d != null -> DetailsBody(d, commentsContent = { CommentSection(movieId, mediaType) }) {
                    if (mediaType == "tv") {
                        if (autoShowServers) {
                            showServerSheet = true
                        } else {
                            showSeasonSheet = true
                        }
                    } else {
                        showServerSheet = true
                    }
                }
                error -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("تعذّر تحميل التفاصيل", color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
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

            if (showSeasonSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSeasonSheet = false },
                    sheetState = sheetState,
                    containerColor = Color(0xFF1E1E1E)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("اختر الموسم:", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ui?.seasons?.filter { it.seasonNumber > 0 } ?: emptyList()) { season ->
                                Button(
                                    onClick = { loadSeason(season) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedSeason?.seasonNumber == season.seasonNumber) Color.White else Color.DarkGray,
                                        contentColor = if (selectedSeason?.seasonNumber == season.seasonNumber) Color.Black else Color.White
                                    )
                                ) {
                                    Text(season.name ?: "الموسم ${season.seasonNumber}")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("الحلقات:", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(episodes) { ep ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedEpisodeNumber = ep.episodeNumber
                                            showSeasonSheet = false
                                            showServerSheet = true
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.PlayArrow, null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("الحلقة ${ep.episodeNumber}", color = Color.White)
                                }
                                Divider(color = Color.DarkGray)
                            }
                        }
                    }
                }
            }

            if (showServerSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showServerSheet = false },
                    sheetState = sheetState,
                    containerColor = Color(0xFF1E1E1E)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            if (mediaType == "tv") "الحلقة $selectedEpisodeNumber" else ui?.title ?: "",
                            color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val servers = listOf(
                            "Server 1" to 0, "Server 2" to 1, "Server 3" to 2, "Server 4" to 3
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            servers.take(2).forEach { (name, index) ->
                                Button(
                                    onClick = {
                                        HistoryManager.addToHistory(
                                            context,
                                            WatchHistory(movieId, ui?.title ?: "", mediaType, ui?.posterPath, selectedSeason?.seasonNumber ?: 1, selectedEpisodeNumber)
                                        )
                                        onPlayClick(movieId, mediaType, ui?.title ?: "", selectedSeason?.seasonNumber ?: 1, selectedEpisodeNumber, index)
                                    },
                                    modifier = Modifier.weight(1f).height(60.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(30.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, color = Color.Black, fontWeight = FontWeight.Bold)
                                        if (index == 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            // ✅ شعار VIP الذهبي
                                            Text(
                                                "👑 VIP",
                                                color = Color(0xFFFFD700),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                modifier = Modifier
                                                    .background(Color.Black, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            servers.drop(2).forEach { (name, index) ->
                                Button(
                                    onClick = {
                                        HistoryManager.addToHistory(
                                            context,
                                            WatchHistory(movieId, ui?.title ?: "", mediaType, ui?.posterPath, selectedSeason?.seasonNumber ?: 1, selectedEpisodeNumber)
                                        )
                                        onPlayClick(movieId, mediaType, ui?.title ?: "", selectedSeason?.seasonNumber ?: 1, selectedEpisodeNumber, index)
                                    },
                                    modifier = Modifier.weight(1f).height(60.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(30.dp)
                                ) { Text(name, color = Color.Black, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
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
private fun DetailsBody(d: DetailsUi, commentsContent: @Composable () -> Unit, onPlay: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val year = d.date?.take(4)?.takeIf { it.length == 4 }
    val rating = d.rating?.takeIf { it > 0 }?.let { String.format(Locale.US, "★ %.1f", it) }
    val overview = d.overview.ifBlank { "لا توجد قصة متاحة حالياً." }

    // imePadding: يرفع المحتوى فوق الكيبورد عند كتابة تعليق (لا أثر بصري بدون الكيبورد)
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
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
                        fontWeight = FontWeight.Bold,
                        fontFamily = CairoBold,
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        overview,
                        style = TextStyle(
                            color = Muted,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            fontFamily = CairoLight
                        ),
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

                // ✅ جديد: قسم التعليقات أسفل صفحة التفاصيل
                Spacer(modifier = Modifier.height(28.dp))
                commentsContent()
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
