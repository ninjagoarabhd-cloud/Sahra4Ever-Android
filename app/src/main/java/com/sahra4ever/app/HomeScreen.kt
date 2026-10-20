package com.sahra4ever.app

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image // ✅ إضافة Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource // ✅ إضافة painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.math.absoluteValue

private const val API_KEY = "26097366152d7131fde0f53b80ee37de"
private const val IMG = "https://image.tmdb.org/t/p/"

private val Pink = Color(0xFFFF2FA6)
private val Purple = Color(0xFF7B2CFF)
private val Ink = Color(0xFF0F0E14)
private val DefaultTop = Color(0xFF3A3650)
private val GlassFill = Color.White.copy(alpha = 0.12f)
private val GlassStroke = Color.White.copy(alpha = 0.22f)
private val Muted = Color(0xFFD0CFDA)

private val CHIPS = listOf("أفلام", "مسلسلات", "أكشن وحركة", "دراما", "خيال علمي", "كوميدي", "مغامرة", "رعب")

private fun Movie.displayTitle(): String = title ?: name ?: ""
private fun img(path: String?, size: String = "w500"): String? = path?.let { "$IMG$size$it" }

private data class HomeData(
    val trending: List<Movie> = emptyList(),
    val nowPlaying: List<Movie> = emptyList(),
    val airing: List<Movie> = emptyList(),
    val anime: List<Movie> = emptyList()
)

private suspend fun fetchHome(): HomeData = coroutineScope {
    val api = TMDBApi.create()
    // نستخدم en-US لضمان البوسترات الإنجليزية
    suspend fun safe(type: String?, block: suspend () -> List<Movie>): List<Movie> = try {
        block().map { if (type != null) it.copy(mediaType = type) else it }
    } catch (e: CancellationException) { throw e } catch (e: Exception) { emptyList() }

    val trending = async { safe(null) { api.getTrendingToday(API_KEY, "en-US").results } }
    val nowPlaying = async { safe("movie") { api.getNowPlayingMovies(API_KEY, "en-US").results } }
    val airing = async { safe("tv") { api.getAiringTodayTVShows(API_KEY, "en-US").results } }
    val anime = async { safe("tv") { api.getAnimeShows(API_KEY, "en-US").results } }
    HomeData(trending.await(), nowPlaying.await(), airing.await(), anime.await())
}

private fun progressOf(item: WatchHistory): Float = 0.4f

private suspend fun extractColor(context: Context, url: String?): Color? {
    if (url == null) return null
    return withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(200).build()
            val bitmap = (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap() ?: return@withContext null
            val p = Palette.from(bitmap).maximumColorCount(16).generate()
            val swatch = p.darkVibrantSwatch ?: p.vibrantSwatch ?: p.darkMutedSwatch ?: p.dominantSwatch
            swatch?.let { Color(it.rgb) }
        } catch (e: Exception) { null }
    }
}

@Composable
fun HomeScreen(
    onMovieClick: (Int, String, String) -> Unit,
    onContinueWatchingClick: (WatchHistory) -> Unit,
    onCategoryClick: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        HomeContent(onMovieClick, onContinueWatchingClick, onCategoryClick, onSearchClick)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(
    onMovieClick: (Int, String, String) -> Unit,
    onContinueWatchingClick: (WatchHistory) -> Unit,
    onCategoryClick: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var home by remember { mutableStateOf(HomeData()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val history = HistoryManager.getHistory(context)
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var chip by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    val colors = remember { mutableStateMapOf<Int, Color>() }

    LaunchedEffect(reloadKey) {
        loading = true; error = false
        try { home = fetchHome() } catch (e: Exception) { error = true }
        loading = false
    }

    LaunchedEffect(query) {
        if (query.isBlank()) { results = emptyList(); searching = false; return@LaunchedEffect }
        searching = true; delay(400)
        try { results = TMDBApi.create().searchMulti(API_KEY, query.trim(), 1, "en-US").results.filter { it.mediaType != "person" && it.posterPath != null } } catch (e: Exception) { results = emptyList() }
        searching = false
    }

    val filtered = remember(home, chip) {
        when (chip) {
            "أفلام" -> home.trending.filter { it.mediaType == "movie" }
            "مسلسلات" -> home.trending.filter { it.mediaType == "tv" }
            else -> home.trending
        }.filter { it.posterPath != null }
    }
    val heroes = filtered.take(5)
    val rest = filtered.drop(5)
    val pagerState = rememberPagerState { heroes.size }

    LaunchedEffect(heroes) {
        heroes.forEach { m -> if (!colors.containsKey(m.id)) extractColor(context, img(m.posterPath, "w185"))?.let { colors[m.id] = it } }
    }
    val base = heroes.getOrNull(pagerState.currentPage)?.let { colors[it.id] }
    val top by animateColorAsState(base?.let { lerpColor(it, Color.Black, 0.3f) } ?: DefaultTop, tween(700), label = "top")
    val bottom by animateColorAsState(base?.let { lerpColor(it, Ink, 0.88f) } ?: Ink, tween(700), label = "bottom")

    val movieClick: (Movie) -> Unit = { m -> onMovieClick(m.id, m.mediaType ?: "movie", m.displayTitle()) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, bottom)))) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Header()
            SearchBar(query, { query = it }, focusRequester)
            Spacer(Modifier.height(8.dp))

            when {
                query.isNotBlank() -> SearchResults(results, searching, movieClick)
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Pink) }
                error -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("تعذّر تحميل البيانات", color = Color.White, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { reloadKey++ }, colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White)) { Text("إعادة المحاولة") }
                }
                else -> HomeFeed(
                    heroes = heroes, rest = rest, nowPlaying = home.nowPlaying, airing = home.airing, anime = home.anime, history = history, chip = chip, pagerState = pagerState, 
                    onChip = { c -> onCategoryClick(c) }, 
                    onMovieClick = movieClick, onContinue = onContinueWatchingClick
                )
            }
        }

        BottomNav(
            selected = tab,
            onSelect = { i ->
                tab = i
                if (i == 1) onSearchClick() 
                if (i == 0) focusRequester.requestFocus()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun Header() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        // ✅ تم استبدال النص بصورة الشعار
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Sahra4Ever Logo",
            modifier = Modifier.height(40.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Icon(Icons.Outlined.Cast, "بث", tint = Color.White, modifier = Modifier.height(24.dp))
            Icon(Icons.Outlined.Download, "تحميل", tint = Color.White, modifier = Modifier.height(24.dp))
            Icon(Icons.Outlined.Notifications, "تنبيهات", tint = Color.White, modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SearchBar(query: String, onChange: (String) -> Unit, focusRequester: FocusRequester) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = query, onValueChange = onChange, singleLine = true,
        placeholder = { Text("ابحث عن فيلم أو مسلسل...", color = Muted) },
        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Muted) },
        trailingIcon = { if (query.isNotEmpty()) { Icon(Icons.Outlined.Close, "مسح", tint = Muted, modifier = Modifier.clip(CircleShape).clickable { onChange("") }.padding(4.dp)) } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(focusedContainerColor = GlassFill, unfocusedContainerColor = GlassFill, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, cursorColor = Pink, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).border(1.dp, GlassStroke, RoundedCornerShape(24.dp)).focusRequester(focusRequester)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeFeed(
    heroes: List<Movie>, rest: List<Movie>, nowPlaying: List<Movie>, airing: List<Movie>, anime: List<Movie>, history: List<WatchHistory>, chip: String?, pagerState: PagerState, onChip: (String) -> Unit, onMovieClick: (Movie) -> Unit, onContinue: (WatchHistory) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {
        item { Chips(chip, onChip) }
        if (heroes.isNotEmpty()) {
            item {
                HorizontalPager(state = pagerState, contentPadding = PaddingValues(horizontal = 28.dp), pageSpacing = 12.dp, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) { page ->
                    val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                    HeroCard(movie = heroes[page], onClick = { onMovieClick(heroes[page]) }, modifier = Modifier.graphicsLayer {
                        val s = lerp(1f, 0.92f, offset); scaleX = s; scaleY = s; alpha = lerp(1f, 0.6f, offset)
                    })
                }
            }
            item { Dots(heroes.size, pagerState.currentPage) }
        }
        if (history.isNotEmpty()) {
            item { SectionTitle("متابعة المشاهدة") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(history, key = { "${it.type}-${it.id}" }) { h -> ContinueCard(h) { onContinue(h) } }
                }
            }
        }
        movieRow("الترند الآن", rest, onMovieClick)
        movieRow("وصل حديثاً للسينما", nowPlaying, onMovieClick)
        movieRow("جديد المسلسلات", airing, onMovieClick)
        movieRow("عالم الأنمي", anime, onMovieClick)
    }
}

private fun LazyListScope.movieRow(title: String, list: List<Movie>, onClick: (Movie) -> Unit) {
    val visible = list.filter { it.posterPath != null }
    if (visible.isEmpty()) return
    item { SectionTitle(title) }
    item {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visible, key = { "${it.mediaType}-${it.id}" }) { m -> PosterCard(m, Modifier.width(120.dp)) { onClick(m) } }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp))
}

@Composable
private fun Chips(selected: String?, onSelect: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CHIPS) { label ->
            val isSelected = label == selected
            val shape = RoundedCornerShape(22.dp)
            Box(Modifier.clip(shape).background(if (isSelected) Color.White.copy(alpha = 0.3f) else GlassFill).border(1.dp, GlassStroke, shape).clickable { onSelect(label) }.padding(horizontal = 20.dp, vertical = 11.dp)) {
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun HeroCard(movie: Movie, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    val tags = listOfNotNull(if (movie.mediaType == "tv") "مسلسل" else "فيلم", movie.voteAverage?.takeIf { it > 0 }?.let { String.format(Locale.US, "★ %.1f", it) }).joinToString("  •  ")
    Box(modifier.fillMaxWidth().aspectRatio(3f / 4f).shadow(12.dp, shape).clip(shape).clickable(onClick = onClick)) {
        AsyncImage(model = img(movie.posterPath, "w780") ?: img(movie.backdropPath, "w780"), contentDescription = movie.displayTitle(), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.9f))))
        Column(Modifier.align(Alignment.BottomCenter).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // النص الإنجليزي كشعار
            Text(movie.displayTitle(), style = TextStyle(brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFFFD9EF), Color(0xFFD7B6FF))), fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.SansSerif, textAlign = TextAlign.Center, shadow = Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 6f), 18f)), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Box(Modifier.padding(top = 10.dp).width(44.dp).height(3.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Purple, Pink))))
            Text(tags, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onClick, shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), modifier = Modifier.weight(1f).height(48.dp)) { Icon(Icons.Outlined.PlayArrow, null); Text(" شاهد الآن", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Button(onClick = { }, shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White), modifier = Modifier.weight(1f).height(48.dp)) { Icon(Icons.Outlined.Add, null); Text(" قائمتي", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }
    }
}

@Composable
private fun Dots(count: Int, current: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center) {
        repeat(count) { i ->
            val selected = i == current
            val width by animateDpAsState(if (selected) 18.dp else 6.dp, label = "dot")
            Box(Modifier.padding(horizontal = 3.dp).height(6.dp).width(width).clip(CircleShape).background(if (selected) Pink else Color.White.copy(alpha = 0.35f)))
        }
    }
}

@Composable
private fun ContinueCard(item: WatchHistory, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val badge = if (item.type == "tv") { if (item.season > 1) "الموسم ${item.season} • الحلقة ${item.episode}" else "الحلقة ${item.episode}" } else "فيلم"
    Box(Modifier.width(250.dp).aspectRatio(16f / 9f).clip(shape).background(Color.White.copy(alpha = 0.08f)).clickable(onClick = onClick)) {
        AsyncImage(model = img(item.posterPath, "w500"), contentDescription = item.title, contentScale = ContentScale.Crop, alignment = BiasAlignment(0f, -0.5f), modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
        Text(badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(Pink).padding(horizontal = 7.dp, vertical = 3.dp))
        Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, end = 10.dp, bottom = 12.dp))
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.25f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progressOf(item).coerceIn(0f, 1f)).background(Brush.horizontalGradient(listOf(Purple, Pink))))
        }
    }
}

@Composable
private fun PosterCard(movie: Movie, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick)) {
        AsyncImage(model = img(movie.posterPath, "w342"), contentDescription = movie.displayTitle(), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.08f)))
        Text(movie.displayTitle(), color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun SearchResults(results: List<Movie>, searching: Boolean, onMovieClick: (Movie) -> Unit) {
    when {
        searching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Pink) }
        results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("لا توجد نتائج", color = Muted, fontSize = 16.sp) }
        else -> LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            gridItems(results, key = { "${it.mediaType}-${it.id}" }) { m -> PosterCard(m) { onMovieClick(m) } }
        }
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        "الرئيسية" to Icons.Outlined.Home,
        "بحث" to Icons.Outlined.Search,
        "ادعمنا" to Icons.Outlined.FavoriteBorder,
        "حسابي" to Icons.Outlined.Person
    )
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Row(modifier.fillMaxWidth().clip(shape).background(Ink.copy(alpha = 0.92f)).border(1.dp, Color.White.copy(alpha = 0.08f), shape).navigationBarsPadding().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceAround) {
        items.forEachIndexed { i, (label, icon) ->
            val tint = if (i == selected) Color.White else Color(0xFFA9A8B4)
            Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable { onSelect(i) }.padding(horizontal = 14.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, label, tint = tint)
                Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}