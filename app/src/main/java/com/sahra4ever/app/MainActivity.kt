package com.sahra4ever.app

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private val Ink = Color(0xFF0F0E14)

private val BottomNavItems: List<Triple<String, ImageVector, String>> = listOf(
    Triple("الرئيسية", Icons.Outlined.Home, "home"),
    Triple("بحث", Icons.Outlined.Search, "search"),
    Triple("ادعمنا", Icons.Outlined.FavoriteBorder, "support"),
    Triple("حسابي", Icons.Outlined.Person, "account")
)

private const val NULL_SENTINEL = "null"
private const val DEFAULT_TITLE = "بدون عنوان"
private const val DEFAULT_MEDIA_TYPE = "movie"

/**
 * فك تشفير آمن لمعاملات بسيطة (مثل category) لا تحتاج Base64.
 */
private fun safeDecode(value: String?, fallback: String = ""): String {
    if (value == null) return fallback
    return try {
        Uri.decode(value)
    } catch (e: Exception) {
        fallback
    }
}

/**
 * ترميز آمن تماماً بـ Base64 (URL-safe, بدون padding) — يُنتج فقط
 * [A-Za-z0-9_-]، أي لا يحتوي أبداً على "/" أو "%" التي كانت تسبب
 * تعارضاً في مطابقة أنماط Navigation Compose.
 */
private fun encodeSegment(value: String?): String {
    if (value.isNullOrBlank()) return NULL_SENTINEL
    return Base64.encodeToString(
        value.toByteArray(Charsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    )
}

private fun decodeSegment(value: String?): String? {
    if (value.isNullOrBlank() || value == NULL_SENTINEL) return null
    return try {
        String(
            Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
            Charsets.UTF_8
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * يمتص كل أحداث اللمس ولا تسمح لها بالمرور لأي عنصر أسفلها.
 * تُستخدم فوق AppRoot أثناء ظهور السبلاش.
 */
private fun Modifier.blockAllTouches(): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
        }
    }
}

/**
 * ✅ (إصلاح كراش "متابعة المشاهدة") هذه الدالة تقبل الآن title وmediaType
 * كـ Nullable (String?) بدلاً من افتراض أنهما دائماً غير فارغين.
 *
 * السبب: Gson يتجاوز نظام null-safety الخاص بـ Kotlin عند التحويل من JSON.
 * فحتى لو كان WatchHistory.title مُعرَّفاً كـ String (غير قابل للـ null)
 * في الكود، فإن سجلات قديمة/تالفة في SharedPreferences (محفوظة بإصدار
 * سابق لا يحتوي هذا الحقل، أو JSON معطوب) تجعل Gson يضع null فعلياً في
 * وقت التشغيل رغم تعريف النوع. أي استخدام مباشر مثل title.ifBlank{...}
 * كان يرمي NullPointerException فوراً ويُسقط التطبيق.
 *
 * الحل: معاملة title وmediaType كـ Nullable دائماً هنا (بغض النظر عمّا
 * يقوله الـ compiler عن النوع المصدر)، مع قيمة افتراضية آمنة لكل منهما.
 * هذا يحمي جميع نقاط الاستدعاء (home, mylist, search, continue watching)
 * من نقطة واحدة مركزية.
 */
private fun buildDetailsRoute(
    movieId: Int,
    mediaType: String?,
    title: String?,
    season: Int = 1,
    episode: Int = 1,
    autoPlay: Boolean = false,
    posterPath: String? = null,
    backdropPath: String? = null,
    voteAverage: Double? = null
): String {
    val safeTitle = title?.takeIf { it.isNotBlank() } ?: DEFAULT_TITLE
    val safeMediaType = mediaType?.takeIf { it.isNotBlank() } ?: DEFAULT_MEDIA_TYPE
    val safeSeason = if (season > 0) season else 1
    val safeEpisode = if (episode > 0) episode else 1

    val titleSeg = encodeSegment(safeTitle)
    val posterSeg = encodeSegment(posterPath)
    val backdropSeg = encodeSegment(backdropPath)
    val voteSeg = (voteAverage ?: 0.0).toString()
    return "details/$movieId/$safeMediaType/$titleSeg/$safeSeason/$safeEpisode/$autoPlay/$posterSeg/$backdropSeg/$voteSeg"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        HomeDataInitializer.start(applicationContext)

        setContent {
            MaterialTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }

                Box(modifier = Modifier.fillMaxSize()) {

                    AppRoot()

                    AnimatedVisibility(
                        visible = showSplash,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(500))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .blockAllTouches()
                        ) {
                            SplashScreen(
                                awaitReady = { HomeDataInitializer.awaitReady(timeoutMs = 4000L) },
                                onFinished = {
                                    showSplash = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        UnityAdsManager.reset()
    }
}

private fun shouldHideBottomBar(route: String?): Boolean {
    if (route == null) return false
    return route.startsWith("details") || route.startsWith("player") || route == "mylist"
}

@Composable
private fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                if (!shouldHideBottomBar(currentRoute)) {
                    AppBottomNav(currentRoute = currentRoute, navController = navController)
                }
            }
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun AppBottomNav(currentRoute: String?, navController: NavHostController) {
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Ink.copy(alpha = 0.92f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .navigationBarsPadding()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        BottomNavItems.forEach { (label, icon, route) ->
            val selected = when (route) {
                "home" -> currentRoute == "home"
                "search" -> currentRoute?.startsWith("search") == true
                else -> currentRoute == route
            }
            val tint = if (selected) Color.White else Color(0xFFA9A8B4)
            Column(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        when (route) {
                            "home" -> if (currentRoute != "home") {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                            "search" -> navController.navigate("search/${Uri.encode("الكل")}")
                            "support" -> navController.navigate("support")
                            "account" -> navController.navigate("account")
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, label, tint = tint)
                Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "home", modifier = modifier) {
        composable("home") {
            HomeScreen(
                onMovieClick = { id, type, title, poster, backdrop, vote ->
                    navController.navigate(
                        buildDetailsRoute(
                            movieId = id,
                            mediaType = type,
                            title = title,
                            posterPath = poster,
                            backdropPath = backdrop,
                            voteAverage = vote
                        )
                    )
                },
                onContinueWatchingClick = { historyItem ->
                    // ✅ تحصين إضافي عند نقطة الاستدعاء (دفاع من طبقتين):
                    // حتى لو كانت البيانات المخزَّنة قديمة/تالفة ويحمل
                    // الكائن title أو type قيمة null فعلياً وقت التشغيل
                    // (رغم أن Kotlin "يعتقد" أنها String غير قابلة للـ null)،
                    // فإن buildDetailsRoute أصبحت الآن تتعامل معها كـ Nullable
                    // داخلياً، وهنا نضيف طبقة حماية إضافية صريحة أيضاً.
                    navController.navigate(
                        buildDetailsRoute(
                            movieId = historyItem.id,
                            mediaType = (historyItem.type as String?) ?: DEFAULT_MEDIA_TYPE,
                            title = (historyItem.title as String?) ?: DEFAULT_TITLE,
                            season = historyItem.season,
                            episode = historyItem.episode,
                            autoPlay = true,
                            posterPath = historyItem.posterPath
                        )
                    )
                },
                onCategoryClick = { category ->
                    navController.navigate("search/${Uri.encode(category)}")
                },
                onMyListClick = {
                    navController.navigate("mylist")
                }
            )
        }
        composable("mylist") {
            MyListScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { id, type, title, poster, backdrop, vote ->
                    navController.navigate(
                        buildDetailsRoute(
                            movieId = id,
                            mediaType = type,
                            title = title,
                            posterPath = poster,
                            backdropPath = backdrop,
                            voteAverage = vote
                        )
                    )
                }
            )
        }
        composable("support") {
            SupportUsScreen(onBack = { navController.popBackStack() })
        }
        composable("account") {
            AccountScreen(
                onBack = { navController.popBackStack() },
                onFavoritesClick = {
                    navController.navigate("mylist")
                }
            )
        }
        composable(
            route = "search/{category}",
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val category = safeDecode(backStackEntry.arguments?.getString("category"), "الكل")
            SearchScreen(
                initialCategory = category,
                onBack = { navController.popBackStack() },
                onMovieClick = { id, type, title, poster, backdrop, vote ->
                    navController.navigate(
                        buildDetailsRoute(
                            movieId = id,
                            mediaType = type,
                            title = title,
                            posterPath = poster,
                            backdropPath = backdrop,
                            voteAverage = vote
                        )
                    )
                }
            )
        }
        composable(
            route = "details/{movieId}/{mediaType}/{title}/{season}/{episode}/{autoPlay}/{poster}/{backdrop}/{vote}",
            arguments = listOf(
                navArgument("movieId") { type = NavType.IntType },
                navArgument("mediaType") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("season") { type = NavType.IntType; defaultValue = 1 },
                navArgument("episode") { type = NavType.IntType; defaultValue = 1 },
                navArgument("autoPlay") { type = NavType.BoolType; defaultValue = false },
                navArgument("poster") { type = NavType.StringType; defaultValue = NULL_SENTINEL },
                navArgument("backdrop") { type = NavType.StringType; defaultValue = NULL_SENTINEL },
                navArgument("vote") { type = NavType.StringType; defaultValue = "0.0" }
            )
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
            val mediaType = backStackEntry.arguments?.getString("mediaType") ?: DEFAULT_MEDIA_TYPE
            val title = decodeSegment(backStackEntry.arguments?.getString("title")) ?: DEFAULT_TITLE
            val season = backStackEntry.arguments?.getInt("season") ?: 1
            val episode = backStackEntry.arguments?.getInt("episode") ?: 1
            val autoPlay = backStackEntry.arguments?.getBoolean("autoPlay") ?: false
            val posterRaw = decodeSegment(backStackEntry.arguments?.getString("poster"))
            val backdropRaw = decodeSegment(backStackEntry.arguments?.getString("backdrop"))
            val voteRaw = backStackEntry.arguments?.getString("vote")?.toDoubleOrNull() ?: 0.0

            DetailsScreen(
                movieId = movieId,
                mediaType = mediaType,
                initialTitle = title,
                initialPoster = posterRaw,
                initialBackdrop = backdropRaw,
                initialVoteAverage = voteRaw.takeIf { it > 0.0 },
                initialSeason = season,
                initialEpisode = episode,
                autoShowServers = autoPlay,
                onBack = { navController.popBackStack() },
                onPlayClick = { id, type, t, s, e, serverIndex ->
                    val titleSeg = encodeSegment(t.takeIf { it.isNotBlank() } ?: DEFAULT_TITLE)
                    navController.navigate("player/$id/$type/$titleSeg/$s/$e/$serverIndex")
                }
            )
        }
        composable(
            route = "player/{movieId}/{mediaType}/{title}/{season}/{episode}/{serverIndex}",
            arguments = listOf(
                navArgument("movieId") { type = NavType.IntType },
                navArgument("mediaType") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("season") { type = NavType.IntType },
                navArgument("episode") { type = NavType.IntType },
                navArgument("serverIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
            val mediaType = backStackEntry.arguments?.getString("mediaType") ?: DEFAULT_MEDIA_TYPE
            val title = decodeSegment(backStackEntry.arguments?.getString("title")) ?: DEFAULT_TITLE
            val season = backStackEntry.arguments?.getInt("season") ?: 1
            val episode = backStackEntry.arguments?.getInt("episode") ?: 1
            val serverIndex = backStackEntry.arguments?.getInt("serverIndex") ?: 0

            PlayerScreen(
                movieId = movieId,
                mediaType = mediaType,
                title = title,
                season = season,
                episode = episode,
                initialServerIndex = serverIndex,
                onBack = { navController.popBackStack() }
            )
        }
    }
}