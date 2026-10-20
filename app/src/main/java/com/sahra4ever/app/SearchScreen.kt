package com.sahra4ever.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

private const val API_KEY = "26097366152d7131fde0f53b80ee37de"
private val Pink = Color(0xFFFF2FA6)
private val Purple = Color(0xFF7B2CFF)
private val Ink = Color(0xFF0F0E14)
private val GlassFill = Color.White.copy(alpha = 0.12f)
private val GlassStroke = Color.White.copy(alpha = 0.22f)
private val Muted = Color(0xFFD0CFDA)

private fun img(path: String?, size: String = "w342"): String? = path?.let { "https://image.tmdb.org/t/p/$size$it" }

@Composable
fun SearchScreen(
    initialCategory: String,
    onBack: () -> Unit,
    onMovieClick: (Int, String, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var results by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val categories = listOf("الكل", "أفلام", "مسلسلات", "أكشن", "دراما", "خيال علمي", "كوميدي", "رعب")

    // جلب البيانات بناءً على التصنيف أو البحث
    LaunchedEffect(query, selectedCategory) {
        isLoading = true
        delay(300) // Debounce
        try {
            val api = TMDBApi.create()
            if (query.isNotBlank()) {
                results = api.searchMulti(API_KEY, query).results.filter { it.mediaType == "movie" || it.mediaType == "tv" }
            } else {
                results = when (selectedCategory) {
                    "أفلام" -> api.getPopularMovies(API_KEY).results.map { it.copy(mediaType = "movie") }
                    "مسلسلات" -> api.getPopularTvShows(API_KEY).results.map { it.copy(mediaType = "tv") }
                    else -> {
                        // للتبسيط، "الكل" أو التصنيفات الفرعية ستجلب التريند حالياً
                        api.getTrendingToday(API_KEY).results
                    }
                }
            }
        } catch (e: Exception) {
            results = emptyList()
        }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // الشريط العلوي
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("البحث", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            // شريط البحث
            TextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("ابحث بالاسم، الممثل، التصنيف، أو السنة...", color = Muted) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Pink) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Outlined.Close, "مسح", tint = Muted,
                            modifier = Modifier.clip(CircleShape).clickable { query = "" }.padding(4.dp)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Pink,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(60.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // التصنيفات
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory && query.isBlank()
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Pink else Color.Transparent)
                            .border(1.dp, if (isSelected) Pink else Color.DarkGray, RoundedCornerShape(20.dp))
                            .clickable { 
                                query = "" 
                                selectedCategory = cat 
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(cat, color = if (isSelected) Color.White else Muted, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // النتائج
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Pink)
                }
            } else if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد نتائج مطابقة", color = Muted, fontSize = 16.sp)
                }
            } else {
                Text(
                    "جميع النتائج (${results.size})",
                    color = Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(results, key = { it.id }) { movie ->
                        SearchMovieCard(movie) {
                            onMovieClick(movie.id, movie.mediaType ?: "movie", movie.title ?: movie.name ?: "")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchMovieCard(movie: Movie, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = img(movie.posterPath),
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        
        // تدرج لوني في الأسفل لقراءة النص
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.6f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.9f)
                    )
                )
        )
        
        // التقييم (نجمة)
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("★", color = Color.Yellow, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = String.format(java.util.Locale.US, "%.1f", movie.voteAverage ?: 0.0),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // النوع (فيلم / مسلسل)
        Text(
            text = if (movie.mediaType == "tv") "مسلسل" else "فيلم",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                .background(Pink, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )

        // العنوان
        Text(
            text = movie.title ?: movie.name ?: "",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
        )
    }
}