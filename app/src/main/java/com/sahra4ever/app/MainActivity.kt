package com.sahra4ever.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

// ✅ قيمة سنتينل تُستخدم لتمثيل "لا توجد قيمة" داخل مسار التنقل (مسارات Compose Navigation
// لا تدعم null مباشرة لـ StringType بدون تعقيد زائد). posterPath/backdropPath الحقيقية
// من TMDB تبدأ دائماً بـ "/" ولا يمكن أبداً أن تساوي "null" حرفياً، لذا السنتينل آمن تماماً.
private const val NULL_SENTINEL = "null"

/**
 * ✅ (تمرير البيانات الأولية / Optimistic UI) دالة مركزية واحدة لبناء رابط شاشة التفاصيل،
 * تُستخدم من كل نقاط الدخول (الرئيسية، متابعة المشاهدة، قائمتي، البحث) لضمان شكل موحّد للمسار.
 * عند غياب poster/backdrop/vote (كما في المسارات القادمة من MyListScreen/SearchScreen التي
 * لا تملك هذه البيانات حالياً) تُستخدم القيم الافتراضية بأمان دون أي تأثير على عملها الحالي.
 */
private fun buildDetailsRoute(
    movieId: Int,
    mediaType: String,
    title: String,
    season: Int = 1,
    episode: Int = 1,
    autoPlay: Boolean = false,
    posterPath: String? = null,
    backdropPath: String? = null,
    voteAverage: Double? = null
): String {
    val posterSeg = Uri.encode(posterPath ?: NULL_SENTINEL)
    val backdropSeg = Uri.encode(backdropPath ?: NULL_SENTINEL)
    val voteSeg = (voteAverage ?: 0.0).toString()
    return "details/$movieId/$mediaType/${Uri.encode(title)}/$season/$episode/$autoPlay/$posterSeg/$backdropSeg/$voteSeg"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // rememberSaveable: لا يعيد السبلاش عند تدوير الشاشة
                var showSplash by rememberSaveable { mutableStateOf(true) }
                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(500),
                    label = "splash"
                ) { splashVisible ->
                    if (splashVisible) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        AppRoot()
                    }
                }
            }
        }
    }

    // ✅ تنظيف أي حالة إعلان معلّقة (Timeout أو isShowing) عند إغلاق النشاط نهائياً
    override fun onDestroy() {
        super.onDestroy()
        UnityAdsManager.reset()
    }
}

/** ✅ المسارات التي يجب أن يختفي فيها الشريط السفلي تماماً. */
private fun shouldHideBottomBar(route: String?): Boolean {
    if (route == null) return false
    return route.startsWith("details") || route.startsWith("player") || route == "mylist"
}

/**
 * ✅ الحاوية الجذرية: Scaffold يحتوي NavHost + الشريط السفلي.
 * تم وضع CompositionLocalProvider هنا لضمان أن الشريط السفلي يترتب من اليمين لليسار.
 */
@Composable
private fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // ✅ هذا هو التعديل: إجبار الاتجاه على RTL
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
                // ✅ (Optimistic UI) الآن يصل poster/backdrop/voteAverage مباشرة من كائن Movie
                // في الشاشة الرئيسية، دون أي طلب شبكة إضافي، لتُستخدم كعرض أولي فوري في DetailsScreen
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
                    navController.navigate(
                        buildDetailsRoute(
                            movieId = historyItem.id,
                            mediaType = historyItem.type,
                            title = historyItem.title,
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
                onMovieClick = { id, type, title ->
                    navController.navigate(buildDetailsRoute(movieId = id, mediaType = type, title = title))
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
            val category = Uri.decode(backStackEntry.arguments?.getString("category") ?: "الكل")
            SearchScreen(
                initialCategory = category,
                onBack = { navController.popBackStack() },
                onMovieClick = { id, type, title ->
                    navController.navigate(buildDetailsRoute(movieId = id, mediaType = type, title = title))
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
            val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
            val title = Uri.decode(backStackEntry.arguments?.getString("title") ?: "بدون عنوان")
            val season = backStackEntry.arguments?.getInt("season") ?: 1
            val episode = backStackEntry.arguments?.getInt("episode") ?: 1
            val autoPlay = backStackEntry.arguments?.getBoolean("autoPlay") ?: false
            val posterRaw = Uri.decode(backStackEntry.arguments?.getString("poster") ?: NULL_SENTINEL)
            val backdropRaw = Uri.decode(backStackEntry.arguments?.getString("backdrop") ?: NULL_SENTINEL)
            val voteRaw = backStackEntry.arguments?.getString("vote")?.toDoubleOrNull() ?: 0.0

            DetailsScreen(
                movieId = movieId,
                mediaType = mediaType,
                initialTitle = title,
                initialPoster = posterRaw.takeIf { it != NULL_SENTINEL },
                initialBackdrop = backdropRaw.takeIf { it != NULL_SENTINEL },
                initialVoteAverage = voteRaw.takeIf { it > 0.0 },
                initialSeason = season,
                initialEpisode = episode,
                autoShowServers = autoPlay,
                onBack = { navController.popBackStack() },
                onPlayClick = { id, type, t, s, e, serverIndex ->
                    navController.navigate("player/$id/$type/${Uri.encode(t)}/$s/$e/$serverIndex")
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
            val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
            val title = Uri.decode(backStackEntry.arguments?.getString("title") ?: "بدون عنوان")
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