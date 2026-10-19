package com.sahra4ever.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

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
    val apiKey = "26097366152d7131fde0f53b80ee37de"
    val context = LocalContext.current
    
    var movie by remember { mutableStateOf<Movie?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // نوافذ منبثقة
    var showSeasonSheet by remember { mutableStateOf(false) }
    var showServerSheet by remember { mutableStateOf(false) }
    
    // حالة المسلسلات
    var selectedSeason by remember { mutableStateOf<Season?>(null) }
    var selectedEpisodeNumber by remember { mutableIntStateOf(initialEpisode) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var isLoadingEpisodes by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()

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

    // دالة جلب الحلقات
    fun fetchEpisodes(season: Season) {
        selectedSeason = season
        isLoadingEpisodes = true
        // توليد الحلقات بناءً على العدد
        val count = season.episodeCount ?: 1
        episodes = List(count) { index -> Episode(index + 1, "الحلقة ${index + 1}", "") }
        isLoadingEpisodes = false
    }

    // التحقق من التشغيل التلقائي (متابعة المشاهدة)
    LaunchedEffect(autoShowServers, movie) {
        if (autoShowServers && movie != null) {
            if (mediaType == "tv") {
                // نجد الموسم المطلوب أو نأخذ الأول
                val targetSeason = movie?.seasons?.find { it.seasonNumber == initialSeason } 
                    ?: movie?.seasons?.firstOrNull { it.seasonNumber > 0 }
                
                if (targetSeason != null) {
                    fetchEpisodes(targetSeason)
                }
            }
            // فتح السيرفرات
            showServerSheet = true 
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
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.Red)
            }
        } else if (movie != null) {
            val backdropUrl = "https://image.tmdb.org/t/p/w780${movie?.backdropPath ?: movie?.posterPath}"
            val title = movie?.title ?: movie?.name ?: "بدون عنوان"

            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                    Text(text = title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("IMDb ${movie?.voteAverage?.toString()?.take(3) ?: "0.0"}", color = Color.Yellow, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(movie?.releaseDate ?: movie?.firstAirDate ?: "", color = Color.Gray, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = movie?.overview ?: "لا يتوفر وصف.", color = Color.LightGray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (mediaType == "tv") {
                                // إذا كان مسلسل، نفتح المواسم أولاً (إلا إذا كنا قادمين من متابعة المشاهدة)
                                if (autoShowServers) {
                                    showServerSheet = true
                                } else {
                                    showSeasonSheet = true
                                }
                            } else {
                                // إذا كان فيلماً، نفتح السيرفرات مباشرة
                                showServerSheet = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (autoShowServers) "متابعة المشاهدة" else "مشاهدة الآن", fontSize = 18.sp, color = Color.White)
                    }
                }
            }

            // نافذة المواسم والحلقات
            if (showSeasonSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSeasonSheet = false },
                    sheetState = sheetState,
                    containerColor = Color(0xFF1E1E1E)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("اختر الموسم / الجزء:", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(movie?.seasons?.filter { it.seasonNumber > 0 } ?: emptyList()) { season ->
                                Button(
                                    onClick = { fetchEpisodes(season) },
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
                        Text("قائمة الحلقات (الموسم ${selectedSeason?.seasonNumber ?: 1}):", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (isLoadingEpisodes) {
                            CircularProgressIndicator(color = Color.Red, modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                items(episodes) { episode ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                selectedEpisodeNumber = episode.episodeNumber
                                                showSeasonSheet = false
                                                showServerSheet = true
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("الحلقة ${episode.episodeNumber}: ${episode.name}", color = Color.White, fontSize = 16.sp)
                                    }
                                    Divider(color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }

            // نافذة السيرفرات
            if (showServerSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showServerSheet = false },
                    sheetState = sheetState,
                    containerColor = Color(0xFF1E1E1E)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        val headerText = if (mediaType == "tv") "الحلقة $selectedEpisodeNumber" else title
                        Text(headerText, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("اختر السيرفر، إذا لم يعمل سيرفر جرب سيرفراً آخر.", color = Color.LightGray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(24.dp))

                        // دالة مساعدة للحفظ والتشغيل
                        val onServerClick = { serverIndex: Int ->
                            // 1. حفظ في السجل
                            HistoryManager.addToHistory(
                                context = context,
                                item = WatchHistory(
                                    id = movieId,
                                    title = title,
                                    type = mediaType,
                                    posterPath = movie?.posterPath,
                                    season = selectedSeason?.seasonNumber ?: 1,
                                    episode = selectedEpisodeNumber
                                )
                            )
                            // 2. الانتقال للمشغل
                            onPlayClick(movieId, mediaType, title, selectedSeason?.seasonNumber ?: 1, selectedEpisodeNumber, serverIndex)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { onServerClick(0) },
                                modifier = Modifier.weight(1f).height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(30.dp)
                            ) { Text("Server 1", color = Color.Black, fontWeight = FontWeight.Bold) }
                            
                            Button(
                                onClick = { onServerClick(1) },
                                modifier = Modifier.weight(1f).height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(30.dp)
                            ) { Text("Server 2", color = Color.Black, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { onServerClick(2) },
                                modifier = Modifier.weight(1f).height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(30.dp)
                            ) { Text("Server 3", color = Color.Black, fontWeight = FontWeight.Bold) }
                            
                            Button(
                                onClick = { onServerClick(3) },
                                modifier = Modifier.weight(1f).height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(30.dp)
                            ) { Text("Server 4", color = Color.Black, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}