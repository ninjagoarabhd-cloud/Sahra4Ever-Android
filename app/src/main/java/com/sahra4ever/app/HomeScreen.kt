package com.sahra4ever.app

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w780"
private const val TMDB_POSTER_BASE = "https://image.tmdb.org/t/p/w342"
private val BRAND_RED = Color(0xFFE50914)
private val BG_DARK = Color(0xFF0B0B12)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onMovieClick: (Int, String, String) -> Unit) {
    val apiKey = "26097366152d7131fde0f53b80ee37de"
    
    // حالات البيانات لكل قسم
    var heroMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var trendingNow by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var nowPlaying by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var airingToday by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var animeShows by remember { mutableStateOf<List<Movie>>(emptyList()) }
    
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val api = TMDBApi.create()
            
            // جلب كل الأقسام بالتوازي لتسريع التحميل
            val heroDeferred = async { api.getTrendingWeek(apiKey) }
            val trendingDeferred = async { api.getTrendingToday(apiKey) }
            val nowPlayingDeferred = async { api.getNowPlayingMovies(apiKey) }
            val airingDeferred = async { api.getAiringTodayTVShows(apiKey) }
            val animeDeferred = async { api.getAnimeShows(apiKey) }

            heroMovies = heroDeferred.await().results.filter { it.backdropPath != null }.take(5)
            trendingNow = trendingDeferred.await().results
            nowPlaying = nowPlayingDeferred.await().results
            airingToday = airingDeferred.await().results
            animeShows = animeDeferred.await().results
            
            isLoading = false
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error fetching movies", e)
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(BG_DARK), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BRAND_RED)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BG_DARK)
        ) {
            // الشريط العلوي
            item {
                Row(
                    modifier = Modifier.padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sahra", color = BRAND_RED, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("4Ever", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
            }

            // شريط البحث
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن فيلم أو مسلسل...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BRAND_RED,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // 1. البانر الرئيسي (Hero Slider)
            item {
                if (heroMovies.isNotEmpty()) {
                    HeroSlider(
                        movies = heroMovies,
                        onPlayClick = { movie ->
                            onMovieClick(movie.id, if(movie.mediaType=="tv") "tv" else "movie", movie.title ?: movie.name ?: "")
                        }
                    )
                }
            }

            // 2. متابعة المشاهدة (هيكل مؤقت - سنفعله لاحقاً)
            item {
                SectionRow(
                    title = "متابعة المشاهدة",
                    movies = trendingNow.take(3), // نستخدم تريند كمثال مؤقت
                    onMovieClick = onMovieClick
                )
            }

            // 3. التريند الآن
            item {
                SectionRow(
                    title = "التريند الآن",
                    movies = trendingNow,
                    onMovieClick = onMovieClick
                )
            }

            // 4. وصل حديثاً للسينما
            item {
                SectionRow(
                    title = "وصل حديثاً للسينما",
                    movies = nowPlaying,
                    onMovieClick = onMovieClick
                )
            }

            // 5. جديد المسلسلات
            item {
                SectionRow(
                    title = "جديد المسلسلات",
                    movies = airingToday,
                    onMovieClick = onMovieClick
                )
            }

            // 6. عالم الأنمي
            item {
                SectionRow(
                    title = "عالم الأنمي",
                    movies = animeShows,
                    onMovieClick = onMovieClick
                )
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// ---------------- مكون السلايدر (Hero Slider) ----------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroSlider(
    movies: List<Movie>,
    onPlayClick: (Movie) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { movies.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage, movies.size) {
        delay(6000L)
        if (movies.size > 1) {
            val nextPage = (pagerState.currentPage + 1) % movies.size
            scope.launch { pagerState.animateScrollToPage(nextPage) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            HeroSlide(
                movie = movies[page],
                onPlayClick = onPlayClick
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(movies.size) { index ->
                val isActive = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (isActive) 20.dp else 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isActive) BRAND_RED else Color.White.copy(alpha = 0.35f))
                )
            }
        }
    }
}

@Composable
private fun HeroSlide(
    movie: Movie,
    onPlayClick: (Movie) -> Unit,
) {
    val context = LocalContext.current
    var dominantColor by remember(movie.id) { mutableStateOf(Color(0xFF1A1024)) }

    LaunchedEffect(movie.posterPath) {
        val imageUrl = movie.posterPath?.let { TMDB_POSTER_BASE + it } ?: return@LaunchedEffect
        val request = ImageRequest.Builder(context).data(imageUrl).allowHardware(false).build()
        val result = context.imageLoader.execute(request)
        if (result is SuccessResult) {
            val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            bitmap?.let { bmp ->
                Palette.from(bmp).generate { palette ->
                    val swatch = palette?.darkVibrantSwatch ?: palette?.dominantSwatch
                    swatch?.let { dominantColor = Color(it.rgb) }
                }
            }
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 700),
        label = "heroGradientColor"
    )

    val title = movie.title ?: movie.name ?: "بدون عنوان"

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = (movie.backdropPath ?: movie.posterPath)?.let { TMDB_IMAGE_BASE + it },
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, animatedColor.copy(alpha = 0.55f), BG_DARK)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Text(
                text = title.uppercase(),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black, // خط عريض جداً يحاكي نيتفليكس
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "دراما  •  الأكثر مشاهدة",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onPlayClick(movie) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("شاهد الآن", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------- مكون الأقسام (صفوف أفقية) ----------------
@Composable
fun SectionRow(
    title: String,
    movies: List<Movie>,
    onMovieClick: (Int, String, String) -> Unit
) {
    if (movies.isEmpty()) return

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(movies) { movie ->
                MovieCard(movie) {
                    val type = if (movie.mediaType == "tv" || movie.name != null) "tv" else "movie"
                    onMovieClick(movie.id, type, movie.title ?: movie.name ?: "بدون عنوان")
                }
            }
        }
    }
}

@Composable
fun MovieCard(movie: Movie, onClick: () -> Unit) {
    val imageUrl = movie.posterPath?.let { "$TMDB_POSTER_BASE$it" } ?: ""
    Column(
        modifier = Modifier
            .width(110.dp) // تصغير حجم البطاقة قليلاً لتناسب الصفوف
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        )
    }
}