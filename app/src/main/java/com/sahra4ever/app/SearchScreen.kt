package com.sahra4ever.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val API_KEY = "26097366152d7131fde0f53b80ee37de"
private val Pink = Color(0xFFFF2FA6)
private val Ink = Color(0xFF0F0E14)
private val Muted = Color(0xFFD0CFDA)

private fun img(path: String?, size: String = "w342"): String? = path?.let { "https://image.tmdb.org/t/p/$size$it" }

// ✅ إصلاح: تحديد mediaType الافتراضي الصحيح حسب التصنيف بدل افتراض "movie" دائماً،
// لأن ردود TMDB لتصنيف "مسلسلات" (getPopularTvShows) لا تحتوي أبداً على حقل media_type.
private fun defaultMediaTypeFor(category: String): String =
    if (category == "مسلسلات") "tv" else "movie"

/**
 * ✅ (إصلاح فقدان حالة البحث عند العودة من التفاصيل)
 * Saver مخصص لحفظ/استعادة قائمة Movie كاملة عبر rememberSaveable، بتحويلها
 * إلى JSON (بنفس آلية Gson المستخدمة أصلاً في HistoryManager.kt) وإعادة
 * بنائها عند استعادة الحالة. بهذا تنجو نتائج البحث فعلياً من تدمير/إعادة بناء
 * الـ Composition الذي يقوم به NavHost عند الذهاب لصفحة التفاصيل والعودة منها،
 * بدل الاعتماد على remember العادي الذي يُفرَّغ بالكامل عند ذلك.
 */
private val MovieListSaver: Saver<List<Movie>, String> = Saver(
    save = { list ->
        try { Gson().toJson(list) } catch (e: Exception) { "[]" }
    },
    restore = { json ->
        try {
            val type = object : TypeToken<List<Movie>>() {}.type
            Gson().fromJson<List<Movie>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialCategory: String,
    onBack: () -> Unit,
    // ✅ إصلاح الـ Crash الجذري: توقيع موحّد بـ 6 متغيرات مطابق تماماً لـ HomeScreen،
    // يمرر posterPath/backdropPath/voteAverage الحقيقية بدل null دائماً، لتفعيل
    // Optimistic UI في DetailsScreen بنفس الطريقة تماماً القادمة من الرئيسية.
    onMovieClick: (Int, String, String, String?, String?, Double?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // ✅ (إصلاح فقدان الحالة) كل الحالات الأساسية أصبحت rememberSaveable بدل
    // remember العادي، فتنجو تماماً من تدمير/إعادة بناء الـ Composition التي
    // يقوم بها NavHost عند الذهاب لصفحة التفاصيل ثم العودة منها.
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(initialCategory) }
    var results by rememberSaveable(stateSaver = MovieListSaver) { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    var page by rememberSaveable { mutableIntStateOf(1) }
    var isLastPage by rememberSaveable { mutableStateOf(false) }
    var isLoadingMore by rememberSaveable { mutableStateOf(false) }

    // ✅ "بصمة" آخر طلب تم تنفيذه فعلياً بنجاح (تصنيف + نص بحث). تُستخدم لمنع
    // LaunchedEffect من إعادة الجلب من الصفر إذا كانت الحالة الحالية مطابقة
    // تماماً لآخر حالة تم جلبها ولا تزال نتائجها محفوظة — أي أننا فقط عدنا من
    // صفحة التفاصيل ولم يغيّر المستخدم شيئاً فعلياً.
    var fetchedKey by rememberSaveable { mutableStateOf<String?>(null) }

    // ✅ موضع تمرير الشبكة صريح الآن (بدل الاعتماد على الافتراضي الضمني داخل
    // LazyVerticalGrid)، فيبقى محفوظاً بشكل مضمون عبر rememberSaveable الداخلي
    // الخاص به بنفس الطريقة بالضبط.
    val gridState = rememberLazyGridState()

    val categories = listOf("الكل", "أفلام", "مسلسلات", "أكشن", "دراما", "خيال علمي", "كوميدي", "رعب")

    suspend fun fetchData(isLoadMore: Boolean = false) {
        if (isLoadMore) isLoadingMore = true else { isLoading = true; page = 1; isLastPage = false }

        try {
            // ✅ استخدام العميل المشترك TmdbClient.api بدل TMDBApi.create() الباردة
            val api = TmdbClient.api
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
            results = if (isLoadMore) results + newResults else newResults
            isLastPage = response.results.isEmpty() || page >= 10

        } catch (e: Exception) {
            if (!isLoadMore) results = emptyList()
        } finally {
            isLoading = false
            isLoadingMore = false
        }
    }

    // ✅ الإصلاح الجذري: لا نُعيد الجلب إذا كانت الحالة الحالية (تصنيف + نص بحث)
    // مطابقة تماماً لآخر طلب تم تنفيذه بنجاح ولا تزال نتائجه محفوظة فعلياً
    // (أي أننا فقط عدنا من صفحة التفاصيل). أي تغيير حقيقي من المستخدم في
    // query أو selectedCategory يُنتج currentKey مختلفاً فيُعاد الجلب كالمعتاد.
    LaunchedEffect(query, selectedCategory) {
        val currentKey = "$selectedCategory|$query"
        if (fetchedKey == currentKey && results.isNotEmpty()) {
            return@LaunchedEffect
        }
        if (query.isNotBlank()) {
            delay(400)
        }
        fetchData(false)
        fetchedKey = currentKey
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ✅ ثابت في الأعلى كما هو تماماً: زر الرجوع + عنوان "البحث"
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

            // ✅ (Collapsible Header) شريط البحث + التصنيفات داخل LazyVerticalGrid
            // كأول عنصرين يمتدان على كامل عرض الشبكة (span = maxLineSpan).
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // --- عنصر 1: شريط البحث (span كامل) ---
                item(span = { GridItemSpan(maxLineSpan) }) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("بحث", color = Muted) },
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
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                    )
                }

                // --- عنصر 2: التصنيفات (span كامل) ---
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
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
                    }
                }

                // --- حالة: جاري التحميل ---
                if (isLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Pink)
                        }
                    }
                }
                // --- حالة: لا توجد نتائج ---
                else if (results.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد نتائج مطابقة", color = Muted, fontSize = 16.sp)
                        }
                    }
                }
                // --- حالة: نتائج موجودة ---
                else {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "جميع النتائج (${results.size})",
                            color = Muted, fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(results, key = { "${it.id}-${it.mediaType}" }) { movie ->
                        SearchMovieCard(movie) {
                            // ✅ قيم آمنة قبل التمرير لمنع أي segment فارغ في route أو mediaType خاطئ
                            val safeTitle = (movie.title ?: movie.name)
                                ?.takeIf { it.isNotBlank() } ?: "بدون عنوان"
                            val safeMediaType = movie.mediaType ?: defaultMediaTypeFor(selectedCategory)
                            onMovieClick(
                                movie.id,
                                safeMediaType,
                                safeTitle,
                                movie.posterPath,
                                movie.backdropPath,
                                movie.voteAverage
                            )
                        }
                    }

                    if (!isLastPage) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
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

        // ✅ إضافة الشارة الحمراء "يعرض قريباً" فوق شارة التقييم مباشرة (TopStart)
        // دون أي مساس بتصميم شارة التقييم الأصلية.
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val badge = computeBadge(movie.releaseDate ?: movie.firstAirDate, movie.overview)
            if (badge != null) {
                BadgeLabel(badge)
            }
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("★", color = Color.Yellow, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(String.format(java.util.Locale.US, "%.1f", movie.voteAverage ?: 0.0), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
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