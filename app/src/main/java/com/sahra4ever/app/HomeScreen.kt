package com.sahra4ever.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult

private const val POSTER_BASE_URL = "https://image.tmdb.org/t/p/w342"
private const val BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w780"
private val DEFAULT_BG_COLOR = Color(0xFF121212)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(onMovieClick: (Int, String, String) -> Unit) {
    val apiKey = "26097366152d7131fde0f53b80ee37de"
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

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

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(DEFAULT_BG_COLOR), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.Red)
        }
    } else {
        val featuredMovies = movies.take(5) // 5 أفلام للسلايدر

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DEFAULT_BG_COLOR)
        ) {
            // الشريط العلوي (اللوجو)
            Row(
                modifier = Modifier.padding(top = 40.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sahra", color = Color.Red, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("4Ever", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            
            // التصنيفات
            LazyRow(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf("أفلام", "مسلسلات", "كوري", "تركي", "أنمي")
                items(categories) { cat ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.DarkGray,
                        modifier = Modifier.clickable { /* فلترة لاحقاً */ }
                    ) {
                        Text(
                            text = cat,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // السلايدر الرئيسي مع الألوان الديناميكية (كود كلود المدمج)
            if (featuredMovies.isNotEmpty()) {
                HeroSlider(movies = featuredMovies, onMovieClick = onMovieClick)
            }

            // قائمة الأفلام السفلية
            Text(
                text = "الأكثر رواجاً الآن",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(movies.drop(5)) { movie ->
                    MovieCard(movie) {
                        val type = if (movie.mediaType == "tv" || movie.name != null) "tv" else "movie"
                        onMovieClick(movie.id, type, movie.title ?: movie.name ?: "بدون عنوان")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroSlider(
    movies: List<Movie>,
    onMovieClick: (Int, String, String) -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { movies.size })
    val extractedColors = remember { mutableStateMapOf<Int, Color>() }

    // استخراج اللون السائد (مكتبة Palette)
    LaunchedEffect(pagerState.currentPage, movies) {
        val currentMovie = movies.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (extractedColors.containsKey(currentMovie.id)) return@LaunchedEffect

        val posterUrl = currentMovie.posterPath?.let { "$POSTER_BASE_URL$it" } ?: return@LaunchedEffect
        val bitmap = loadBitmapFromUrl(context, posterUrl)

        bitmap?.let { bmp ->
            Palette.from(bmp).generate { palette ->
                val dominantArgb = palette?.getDarkVibrantColor(DEFAULT_BG_COLOR.toArgb())
                    ?: palette?.getDarkMutedColor(DEFAULT_BG_COLOR.toArgb())
                    ?: DEFAULT_BG_COLOR.toArgb()
                extractedColors[currentMovie.id] = Color(dominantArgb)
            }
        }
    }

    val currentMovie = movies.getOrNull(pagerState.currentPage)
    val targetColor = currentMovie?.let { extractedColors[it.id] } ?: DEFAULT_BG_COLOR

    // تحريك اللون بسلاسة
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 800),
        label = "heroGradientColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(animatedColor.copy(alpha = 0.7f), DEFAULT_BG_COLOR),
                    startY = 300f
                )
            )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val movie = movies[page]
            val imageUrl = movie.backdropPath?.let { "$BACKDROP_BASE_URL$it" }
                ?: movie.posterPath?.let { "$POSTER_BASE_URL$it" }

            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f), DEFAULT_BG_COLOR),
                                startY = 200f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = (movie.title ?: movie.name ?: "").uppercase(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onMovieClick(movie.id, if(movie.mediaType=="tv") "tv" else "movie", movie.title ?: movie.name ?: "") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("شاهد الآن", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { /* My List */ },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("قائمتي", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        
        // نقاط التنقل
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(movies.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(width = if (isSelected) 20.dp else 6.dp, height = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.4f))
                )
            }
        }
    }
}

@Composable
fun MovieCard(movie: Movie, onClick: () -> Unit) {
    val imageUrl = movie.posterPath?.let { "$POSTER_BASE_URL$it" } ?: ""
    Column(modifier = Modifier.width(120.dp).clickable { onClick() }) {
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

// دالة استخراج الصورة
private suspend fun loadBitmapFromUrl(context: Context, url: String): Bitmap? {
    return try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
    } catch (e: Exception) {
        null
    }
}