package com.sahra4ever.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// 1. تعديل الدالة لتمرير الاسم
fun DetailsScreen(movieId: Int, mediaType: String, onBack: () -> Unit, onPlayClick: (Int, String, String) -> Unit) {
    
    val apiKey = "26097366152d7131fde0f53b80ee37de" 
    
    var movie by remember { mutableStateOf<Movie?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(movieId) {
        try {
            val api = TMDBApi.create()
            movie = if (mediaType == "tv") {
                api.getTvDetails(movieId, apiKey)
            } else {
                api.getMovieDetails(movieId, apiKey)
            }
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.Red)
            }
        } else if (movie != null) {
            val backdropUrl = "https://image.tmdb.org/t/p/w780${movie?.backdropPath ?: movie?.posterPath}"
            val title = movie?.title ?: movie?.name ?: "بدون عنوان"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212))
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
            ) {
                Box(modifier = Modifier.height(300.dp)) {
                    AsyncImage(
                        model = backdropUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xFF121212)),
                                    startY = 400f
                                )
                            )
                    )
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("IMDb ${movie?.voteAverage?.toString()?.take(3) ?: "0.0"}", color = Color.Yellow, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(movie?.releaseDate ?: movie?.firstAirDate ?: "", color = Color.Gray, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = movie?.overview ?: "لا يتوفر وصف.",
                        color = Color.LightGray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. تعديل زر المشاهدة ليمرر الاسم (title)
                    Button(
                        onClick = { onPlayClick(movieId, mediaType, title) }, 
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("مشاهدة الآن", fontSize = 18.sp, color = Color.White)
                    }
                }
            }
        }
    }
}