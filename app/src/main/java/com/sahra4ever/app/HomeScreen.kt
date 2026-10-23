@file:OptIn(ExperimentalFoundationApi::class, FlowPreview::class)

package com.sahra4ever.app

import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.res.painterResource
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
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.math.absoluteValue

private val Pink = Color(0xFFFF2FA6)
private val Purple = Color(0xFF7B2CFF)
private val Ink = Color(0xFF0F0E14)
private val DefaultTop = Color(0xFF3A3650)
private val GlassFill = Color.White.copy(alpha = 0.12f)
private val GlassStroke = Color.White.copy(alpha = 0.22f)
private val Muted = Color(0xFFD0CFDA)

// ✅ عتبة اعتبار الشاشة "جهاز لوحي" (Medium/Expanded) — لا تؤثر إطلاقاً على عرض الجوال العادي
private val TABLET_BREAKPOINT = 600.dp

private val CHIPS = listOf("أفلام", "مسلسلات", "أكشن وحركة", "دراما", "خيال علمي", "كوميدي", "مغامرة", "رعب")

private val HeroTitleStyle = TextStyle(
    brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFFFD9EF), Color(0xFFD7B6FF))),
    fontSize = 36.sp,
    lineHeight = 42.sp,
    fontWeight = FontWeight.Black,
    fontFamily = FontFamily.SansSerif,
    textAlign = TextAlign.Center,
    shadow = Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 6f), 18f)
)
private val BrandBrush = Brush.horizontalGradient(listOf(Purple, Pink))
private val HeroScrim = Brush.verticalGradient(0f to Color.Transparent, 0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.9f))
private val CardScrim = Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))

@Immutable
private data class HomeData(
    val trending: List<Movie> = emptyList(),
    val nowPlaying: List<Movie> = emptyList(),
    val airing: List<Movie> = emptyList(),
    val anime: List<Movie> = emptyList()
)

private object HomeCache {
    private const val TTL_MS = 10 * 60 * 1000L
    private var data: HomeData? = null
    private var savedAt = 0L
    fun latest(): HomeData? = data
    fun fresh(): HomeData? = data?.takeIf { System.currentTimeMillis() - savedAt < TTL_MS }
    fun put(d: HomeData) { data = d; savedAt = System.currentTimeMillis() }
}

private val searchCache = LruCache<String, List<Movie>>(30)

private fun List<Movie>.clean(type: String?): List<Movie> =
    asSequence()
        .filter { it.posterPath != null }
        .map { if (type != null) it.copy(mediaType = type) else it }
        .distinctBy { "${it.mediaType}-${it.id}" }
        .toList()

private suspend fun fetchHome(): HomeData = coroutineScope {
    val api = TmdbClient.api
    suspend fun safe(type: String?, block: suspend () -> List<Movie>): List<Movie> =
        tryOrNull { block() }?.clean(type) ?: emptyList()

    val trending = async { safe(null) { api.getTrendingToday(TMDB_API_KEY, "en-US").results } }
    val nowPlaying = async { safe("movie") { api.getNowPlayingMovies(TMDB_API_KEY, "en-US").results } }
    val airing = async { safe("tv") { api.getAiringTodayTVShows(TMDB_API_KEY, "en-US").results } }
    val anime = async { safe("tv") { api.getAnimeShows(TMDB_API_KEY, "en-US").results } }

    val data = HomeData(trending.await(), nowPlaying.await(), airing.await(), anime.await())
    if (data.trending.isEmpty() && data.nowPlaying.isEmpty() && data.airing.isEmpty() && data.anime.isEmpty()) {
        throw IOException("no data")
    }
    data
}

private fun progressOf(item: WatchHistory): Float = 0.4f
private fun PagerState.offsetFor(page: Int): Float = ((currentPage - page) + currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)

private fun Movie.contentBadge(): ContentBadge? = computeBadge(releaseDate ?: firstAirDate, overview)

@Composable
fun HomeScreen(
    onMovieClick: (Int, String, String) -> Unit,
    onContinueWatchingClick: (WatchHistory) -> Unit,
    onCategoryClick: (String) -> Unit,
    onMyListClick: () -> Unit = {}
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        HomeContent(onMovieClick, onContinueWatchingClick, onCategoryClick, onMyListClick)
    }
}

@Composable
private fun HomeContent(
    onMovieClick: (Int, String, String) -> Unit,
    onContinueWatchingClick: (WatchHistory) -> Unit,
    onCategoryClick: (String) -> Unit,
    onMyListClick: () -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    val cached = remember { HomeCache.latest() }
    var home by remember { mutableStateOf(cached ?: HomeData()) }
    var loading by remember { mutableStateOf(cached == null) }
    var error by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    var history by remember { mutableStateOf<List<WatchHistory>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val colors = remember { mutableStateMapOf<Int, Color>() }

    // ✅ تهيئة نظام المفضلة مبكراً (آمنة للاستدعاء المتكرر)
    LaunchedEffect(Unit) {
        FavoritesManager.init(context)
    }

    LaunchedEffect(Unit) {
        history = withContext(Dispatchers.IO) {
            HistoryManager.getHistory(context).distinctBy { "${it.type}-${it.id}" }
        }
    }

    LaunchedEffect(reloadKey) {
        if (reloadKey == 0 && HomeCache.fresh() != null) return@LaunchedEffect
        val hadData = home.trending.isNotEmpty()
        if (!hadData) loading = true
        error = false
        try {
            val fresh = fetchHome()
            HomeCache.put(fresh)
            home = fresh
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!hadData) error = true
        }
        loading = false
    }

    LaunchedEffect(Unit) {
        snapshotFlow { query.trim() }
            .distinctUntilChanged()
            .onEach { searching = it.isNotEmpty() }
            .debounce { q -> if (q.isEmpty()) 0L else 350L }
            .collectLatest { q ->
                if (q.isEmpty()) {
                    results = emptyList(); searching = false
                    return@collectLatest
                }
                searchCache.get(q)?.let {
                    results = it; searching = false
                    return@collectLatest
                }
                try {
                    val r = TmdbClient.api.searchMulti(TMDB_API_KEY, q, 1, "en-US").results
                        .filter { it.mediaType != "person" && it.posterPath != null }
                        .distinctBy { "${it.mediaType}-${it.id}" }
                    searchCache.put(q, r)
                    results = r
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    results = emptyList()
                }
                searching = false
            }
    }

    val filtered = remember(home) { home.trending.filter { it.posterPath != null } }
    val heroes = remember(filtered) { filtered.take(5) }
    val rest = remember(filtered) { filtered.drop(5) }
    val pagerState = rememberPagerState { heroes.size }

    LaunchedEffect(heroes) {
        heroes.forEach { m ->
            if (!colors.containsKey(m.id)) {
                launch {
                    PaletteCache.colorFor(context, tmdbImg(m.posterPath, "w185"))?.let { colors[m.id] = it }
                }
            }
        }
    }

    val movieClick = remember(onMovieClick) {
        { m: Movie -> onMovieClick(m.id, m.mediaType ?: "movie", m.displayTitle()) }
    }

    // ✅ (دعم الأجهزة اللوحية) BoxWithConstraints يسمح بمعرفة عرض الشاشة الفعلي
    // دون أي تأثير على تصميم الجوال (isTablet = false يحافظ على نفس القيم الأصلية تماماً)
    BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
        val isTablet = maxWidth >= TABLET_BREAKPOINT

        AmbientBackground(heroes, pagerState, colors, Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Header(onMyListClick = onMyListClick)
            SearchBar(query, { query = it }, focusRequester)
            Spacer(Modifier.height(8.dp))

            when {
                query.isNotBlank() -> SearchResults(results, searching, movieClick, isTablet)
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Pink) }
                error -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("تعذّر تحميل البيانات", color = Color.White, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { reloadKey++ }, colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.White)) { Text("إعادة المحاولة") }
                }
                else -> HomeFeed(
                    heroes = heroes, rest = rest, nowPlaying = home.nowPlaying, airing = home.airing, anime = home.anime,
                    history = history, pagerState = pagerState, listState = listState,
                    onChip = onCategoryClick, onMovieClick = movieClick, onContinue = onContinueWatchingClick,
                    isTablet = isTablet
                )
            }
        }
    }
}

@Composable
private fun AmbientBackground(heroes: List<Movie>, pagerState: PagerState, colors: Map<Int, Color>, modifier: Modifier) {
    val base = heroes.getOrNull(pagerState.currentPage)?.let { colors[it.id] }
    val top by animateColorAsState(base?.let { lerpColor(it, Color.Black, 0.3f) } ?: DefaultTop, tween(700), label = "top")
    val bottom by animateColorAsState(base?.let { lerpColor(it, Ink, 0.88f) } ?: Ink, tween(700), label = "bottom")
    Box(modifier.drawBehind { drawRect(Brush.verticalGradient(listOf(top, bottom))) })
}

/** ✅ (نقطة 3) الشريط العلوي الجديد: اللوغو + (صورة الحساب واسمه) + زر قائمتي — بدون جرس/تحميل/Cast */
@Composable
private fun Header(onMyListClick: () -> Unit) {
    val user by AuthManager.user.collectAsState()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Sahra4Ever Logo",
            modifier = Modifier.height(34.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    name = user?.name ?: GUEST_NAME,
                    photoUrl = user?.photoUrl,
                    size = 34.dp,
                    showInitial = user != null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    user?.name ?: GUEST_NAME,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 90.dp)
                )
            }

            Box(
                Modifier
                    .clip(CircleShape)
                    .background(GlassFill)
                    .border(1.dp, GlassStroke, CircleShape)
                    .clickable(onClick = onMyListClick)
                    .padding(8.dp)
            ) {
                Icon(Icons.Outlined.FavoriteBorder, "قائمتي", tint = Color.White, modifier = Modifier.size(20.dp))
            }
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

@Composable
private fun HomeFeed(
    heroes: List<Movie>, rest: List<Movie>, nowPlaying: List<Movie>, airing: List<Movie>, anime: List<Movie>,
    history: List<WatchHistory>, pagerState: PagerState, listState: LazyListState,
    onChip: (String) -> Unit, onMovieClick: (Movie) -> Unit, onContinue: (WatchHistory) -> Unit,
    isTablet: Boolean
) {
    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "chips", contentType = "chips") { Chips(onChip) }
        if (heroes.isNotEmpty()) {
            item(key = "hero", contentType = "hero") {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 28.dp),
                    pageSpacing = 12.dp,
                    key = { "${heroes[it].mediaType}-${heroes[it].id}" },
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                ) { page ->
                    val movie = heroes[page]
                    HeroCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) },
                        dim = { pagerState.offsetFor(page) * 0.4f },
                        modifier = Modifier.graphicsLayer {
                            val s = lerp(1f, 0.92f, pagerState.offsetFor(page))
                            scaleX = s; scaleY = s
                        }
                    )
                }
            }
            item(key = "dots", contentType = "dots") { Dots(heroes.size, pagerState) }
        }
        if (history.isNotEmpty()) {
            item(key = "history-title", contentType = "title") { SectionTitle("متابعة المشاهدة") }
            item(key = "history-row", contentType = "row") {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(history, key = { "${it.type}-${it.id}" }, contentType = { "continue" }) { h -> ContinueCard(h) { onContinue(h) } }
                }
            }
        }
        movieRow("trending", "الترند الآن", rest, onMovieClick, isTablet)
        movieRow("now", "وصل حديثاً للسينما", nowPlaying, onMovieClick, isTablet)
        movieRow("airing", "جديد المسلسلات", airing, onMovieClick, isTablet)
        movieRow("anime", "عالم الأنمي", anime, onMovieClick, isTablet)
    }
}

/** ✅ (دعم الأجهزة اللوحية) عرض كرت أكبر قليلاً + تباعد أوسع فقط عندما isTablet=true؛
 * على الجوال (isTablet=false) القيم مطابقة تماماً للأصل (120.dp / 8.dp). */
private fun LazyListScope.movieRow(id: String, title: String, list: List<Movie>, onClick: (Movie) -> Unit, isTablet: Boolean) {
    if (list.isEmpty()) return
    item(key = "$id-title", contentType = "title") { SectionTitle(title) }
    item(key = "$id-row", contentType = "row") {
        val cardWidth = if (isTablet) 168.dp else 120.dp
        val spacing = if (isTablet) 14.dp else 8.dp
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(spacing)) {
            items(list, key = { "${it.mediaType}-${it.id}" }, contentType = { "poster" }) { m ->
                PosterCard(m, Modifier.width(cardWidth)) { onClick(m) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp))
}

@Composable
private fun Chips(onSelect: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(CHIPS, key = { it }, contentType = { "chip" }) { label ->
            val shape = RoundedCornerShape(22.dp)
            Box(Modifier.clip(shape).background(GlassFill).border(1.dp, GlassStroke, shape).clickable { onSelect(label) }.padding(horizontal = 20.dp, vertical = 11.dp)) {
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

/** ✅ (نقطة 2) زر "قائمتي" أصبح فعّالاً بالكامل: يبدّل بين + و ✓ ويضيف/يحذف من المفضلة فوراً */
@Composable
private fun HeroCard(movie: Movie, onClick: () -> Unit, dim: () -> Float, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(16.dp)
    val tags = remember(movie) {
        listOfNotNull(
            if (movie.mediaType == "tv") "مسلسل" else "فيلم",
            movie.voteAverage?.takeIf { it > 0 }?.let { String.format(Locale.US, "★ %.1f", it) }
        ).joinToString("  •  ")
    }
    val badge = remember(movie) { movie.contentBadge() }

    val favoritesList by FavoritesManager.favorites.collectAsState()
    val mediaType = movie.mediaType ?: "movie"
    val isFavorite = remember(favoritesList, movie.id, mediaType) {
        favoritesList.any { it.id == movie.id && it.type == mediaType }
    }

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(shape)
            .clickable(onClick = onClick)
            .drawWithContent {
                drawContent()
                val a = dim()
                if (a > 0f) drawRect(Color.Black.copy(alpha = a))
            }
    ) {
        AsyncImage(model = tmdbImg(movie.posterPath, "w780") ?: tmdbImg(movie.backdropPath, "w780"), contentDescription = movie.displayTitle(), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(HeroScrim))

        badge?.let {
            BadgeLabel(it, Modifier.align(Alignment.TopStart).padding(12.dp))
        }

        Column(Modifier.align(Alignment.BottomCenter).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(movie.displayTitle(), style = HeroTitleStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Box(Modifier.padding(top = 10.dp).width(44.dp).height(3.dp).clip(CircleShape).background(BrandBrush))
            Text(tags, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onClick, shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black), modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Outlined.PlayArrow, null)
                    Text(" شاهد الآن", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Button(
                    onClick = {
                        FavoritesManager.toggle(
                            context,
                            FavoriteItem(movie.id, movie.displayTitle(), mediaType, movie.posterPath)
                        )
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFavorite) Pink.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(if (isFavorite) Icons.Outlined.Check else Icons.Outlined.Add, null)
                    Text(" قائمتي", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun Dots(count: Int, pagerState: PagerState) {
    val current = pagerState.currentPage
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
    val badge = remember(item) {
        if (item.type == "tv") { if (item.season > 1) "الموسم ${item.season} • الحلقة ${item.episode}" else "الحلقة ${item.episode}" } else "فيلم"
    }
    Box(Modifier.width(250.dp).aspectRatio(16f / 9f).clip(shape).background(Color.White.copy(alpha = 0.08f)).clickable(onClick = onClick)) {
        AsyncImage(model = tmdbImg(item.posterPath, "w500"), contentDescription = item.title, contentScale = ContentScale.Crop, alignment = BiasAlignment(0f, -0.5f), modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(CardScrim))
        Text(badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(Pink).padding(horizontal = 7.dp, vertical = 3.dp))
        Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, end = 10.dp, bottom = 12.dp))
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.25f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progressOf(item).coerceIn(0f, 1f)).background(BrandBrush))
        }
    }
}

@Composable
private fun PosterCard(movie: Movie, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val badge = remember(movie) { movie.contentBadge() }

    Column(modifier.clickable(onClick = onClick)) {
        Box {
            AsyncImage(
                model = tmdbImg(movie.posterPath, "w342"),
                contentDescription = movie.displayTitle(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.08f))
            )
            badge?.let {
                BadgeLabel(it, Modifier.align(Alignment.TopStart).padding(6.dp))
            }
        }
        Text(movie.displayTitle(), color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
    }
}

/** ✅ (دعم الأجهزة اللوحية) عدد أعمدة الشبكة يزداد على الشاشات الكبيرة فقط؛
 * على الجوال يبقى 3 أعمدة تماماً كما كان. */
@Composable
private fun SearchResults(results: List<Movie>, searching: Boolean, onMovieClick: (Movie) -> Unit, isTablet: Boolean) {
    Box(Modifier.fillMaxSize()) {
        when {
            results.isNotEmpty() -> LazyVerticalGrid(
                columns = GridCells.Fixed(if (isTablet) 5 else 3),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                gridItems(results, key = { "${it.mediaType}-${it.id}" }, contentType = { "poster" }) { m -> PosterCard(m) { onMovieClick(m) } }
            }
            searching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Pink) }
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("لا توجد نتائج", color = Muted, fontSize = 16.sp) }
        }
        if (searching && results.isNotEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter), color = Pink, trackColor = Color.Transparent)
        }
    }
}