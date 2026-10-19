package com.sahra4ever.app

import android.content.Context
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
import androidx.compose.material.icons.filled.Close
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w780"
private const val TMDB_POSTER_BASE = "https://image.tmdb.org/t/p/w342"
private val BRAND_RED = Color(0xFFE50914)
private val BG_DARK = Color(0xFF0B0B12)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMovieClick: (Int, String, String) -> Unit,
    onContinueWatchingClick: (WatchHistory) -> Unit
) {
    val apiKey = "26097366152d7131fde0f53b80ee37de"
    val context = LocalContext.current
    
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    
    val historyList = remember { HistoryManager.getHistory(context) }

    LaunchedEffect(Unit) {
        try {
            val api = TMDBApi.create()
            val response = api.getTrendingToday(apiKey)
            movies = response.results
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            isSearching = false
        } else {
            isSearching = true
            delay(500)
            try {
                val api = TMDBApi.create()
                val response = api.searchMulti(apiKey, searchQuery)
                searchResults = response.results.filter { it.mediaType == "movie" || it.mediaType == "tv" }
            } catch (e: Exception) {
                Log.e("Search", "Error", e)
            }
            isSearching = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(BG_DARK), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BRAND_RED)
        }
    } else {
        val featuredMovies = movies.take(5)
        val otherMovies = movies.drop(5)

        LazyColumn(modifier = Modifier.fillMaxSize().background(BG_DARK)) {
            item {
                Row(
                    modifier = Modifier.padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
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
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.DarkGray,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }

            // نتائج البحث أو الواجهة الرئيسية
            if (searchQuery.isNotBlank()) {
                if (isSearching) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BRAND_RED)
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    item {
                        Text("لا توجد نتائج مطابقة.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    item {
                        Text("نتائج البحث", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                    }
                    items(searchResults.chunked(3)) { chunk ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            chunk.forEach { movie ->
                                Box(modifier = Modifier.weight(1f)) {
                                    MovieCard(movie) {
                                        onMovieClick(movie.id, if(movie.mediaType=="tv") "tv" else "movie", movie.title ?: movie.name ?: "")
                                    }
                                }
                            }
                            repeat(3 - chunk.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            } else {
                // قسم متابعة المشاهدة
                if (historyList.isNotEmpty()) {
                    item {
                        Text("متابعة المشاهدة", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                            items(historyList) { historyItem ->
                                ContinueWatchingCard(historyItem) {
                                    onContinueWatchingClick(historyItem)
                                }
                            }
                        }
                    }
                }

                // السلايدر الرئيسي
                item {
                    if (featuredMovies.isNotEmpty()) {
                        HeroSlider(movies = featuredMovies, onPlayClick = { movie ->
                            onMovieClick(movie.id, if(movie.mediaType=="tv") "tv" else "movie", movie.title ?: movie.name ?: "")
                        })
                    }
                }

                // الأقسام السفلية
                item { SectionRow("الترند الآن", otherMovies, onMovieClick) }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// بطاقة متابعة المشاهدة
@Composable
fun ContinueWatchingCard(item: WatchHistory, onClick: () -> Unit) {
    val imageUrl = item.posterPath?.let { "$TMDB_POSTER_BASE$it" } ?: ""
    val episodeText = if (item.type == "tv") "الحلقة ${item.episode}" else "فيلم"
    
    Column(modifier = Modifier.width(140.dp).clickable { onClick() }) {
        Box(modifier = Modifier.aspectRatio(16f/9f).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {
            AsyncImage(
                model = imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(0.7f).height(3.dp).background(BRAND_RED))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(episodeText, color = Color.Gray, fontSize = 11.sp)
    }
}

// صفوف الأقسام
@Composable
fun SectionRow(title: String, movies: List<Movie>, onMovieClick: (Int, String, String) -> Unit) {
    if (movies.isEmpty()) return
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
            items(movies) { movie ->
                MovieCard(movie) {
                    onMovieClick(movie.id, if (movie.mediaType == "tv" || movie.name != null) "tv" else "movie", movie.title ?: movie.name ?: "")
                }
            }
        }
    }
}

// ------------------- السلايدر (Hero Slider) -------------------
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

    Box(modifier = Modifier.fillMaxWidth().height(500.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            HeroSlide(movie = movies[page], onPlayClick = onPlayClick)
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
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
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, animatedColor.copy(alpha = 0.55f), BG_DARK)
                )
            )
        )

        Column(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Text(
                text = title.uppercase(),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "دراما  •  الأكثر مشاهدة", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
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

// ------------------- بطاقة الفيلم العادية -------------------
@Composable
fun MovieCard(movie: Movie, onClick: () -> Unit) {
    val imageUrl = movie.posterPath?.let { "$TMDB_POSTER_BASE$it" } ?: ""
    Column(
        modifier = Modifier.width(110.dp).clickable { onClick() }
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