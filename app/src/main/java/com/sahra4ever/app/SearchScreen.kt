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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val API_KEY = "26097366152d7131fde0f53b80ee37de"
private val Pink = Color(0xFFFF2FA6)
private val Ink = Color(0xFF0F0E14)
private val Muted = Color(0xFFD0CFDA)

private fun img(path: String?, size: String = "w342"): String? = path?.let { "https://image.tmdb.org/t/p/$size$it" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialCategory: String,
    onBack: () -> Unit,
    onMovieClick: (Int, String, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var results by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    var page by remember { mutableIntStateOf(1) }
    var isLastPage by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }

    val categories = listOf("الكل", "أفلام", "مسلسلات", "أكشن", "دراما", "خيال علمي", "كوميدي", "رعب")

    suspend fun fetchData(isLoadMore: Boolean = false) {
        if (isLoadMore) isLoadingMore = true else { isLoading = true; page = 1; isLastPage = false }
        
        try {
            val api = TMDBApi.create()
            val response = if (query.isNotBlank()) {
                api.searchMulti(API_KEY, query, page, "en-US")
            } else {
                when (selectedCategory) {
                    "أفلام" -> api.getPopularMovies(API_KEY, page, "en-US")
                    "مسلسلات" -> api.getPopularTvShows(API_KEY, page, "en-US")
                    "أكشن" -> api.getTopRatedMoviesByGenre(API_KEY, "28", page = page, language = "en-US")
                    "دراما" -> api.getTopRatedMoviesByGenre(API_KEY, "18", page = page, language = "en-US")
                    "خيال علمي" -> api.getTopRatedMoviesByGenre(API_KEY, "878", page = page, language = "en-US")
                    "كوميدي" -> api.getTopRatedMoviesByGenre(API_KEY, "35", page = page, language = "en-US")
                    "رعب" -> api.getTopRatedMoviesByGenre(API_KEY, "27", page = page, language = "en-US")
                    else -> api.getTrendingToday(API_KEY, "en-US") 
                }
            }
            
            val newResults = response.results.filter { it.posterPath != null }
            if (isLoadMore) results = results + newResults else results = newResults
            isLastPage = response.results.isEmpty() || page >= 10
            
        } catch (e: Exception) {
            if (!isLoadMore) results = emptyList()
        } finally {
            isLoading = false
            isLoadingMore = false
        }
    }

    LaunchedEffect(query, selectedCategory) {
        // تأخير بسيط فقط عند الكتابة في البحث
        if (query.isNotBlank()) {
            delay(400)
        }
        fetchData(false)
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        Column(modifier = Modifier.fillMaxSize()) {
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

            TextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("بحث", color = Muted) }, // ✅ تم تغيير النص هنا
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
                    focusedContainerColor = Color(0xFF1E1E1E), unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Pink, focusedTextColor = Color.White, unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(60.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                    color = Muted, fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(results, key = { "${it.id}-${it.mediaType}" }) { movie ->
                        SearchMovieCard(movie) {
                            onMovieClick(movie.id, movie.mediaType ?: "movie", movie.title ?: movie.name ?: "")
                        }
                    }
                    
                    if (!isLastPage) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(color = Pink, modifier = Modifier.size(24.dp))
                                } else {
                                    Button(
                                        onClick = { 
                                            page++
                                            scope.launch { fetchData(isLoadMore = true) }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                    ) {
                                        Text("تحميل المزيد")
                                    }
                                }
                            }
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
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.9f))
            )
        )
        
        Row(
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("★", color = Color.Yellow, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(String.format(java.util.Locale.US, "%.1f", movie.voteAverage ?: 0.0), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            text = if (movie.mediaType == "tv") "مسلسل" else "فيلم",
            color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                .background(Pink, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
        )

        Text(
            text = movie.title ?: movie.name ?: "",
            color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
        )
    }
}