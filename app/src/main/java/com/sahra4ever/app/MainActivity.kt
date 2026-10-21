package com.sahra4ever.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

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
                        AppNavHost()
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onMovieClick = { id, type, title ->
                    navController.navigate("details/$id/$type/${Uri.encode(title)}/1/1/false")
                },
                onContinueWatchingClick = { historyItem ->
                    navController.navigate("details/${historyItem.id}/${historyItem.type}/${Uri.encode(historyItem.title)}/${historyItem.season}/${historyItem.episode}/true")
                },
                onCategoryClick = { category ->
                    navController.navigate("search/${Uri.encode(category)}")
                },
                onSearchClick = {
                    navController.navigate("search/${Uri.encode("الكل")}")
                },
                onSupportClick = {
                    navController.navigate("support")
                },
                onAccountClick = {
                    navController.navigate("account")
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
                    Toast.makeText(context, "قائمتي قادمة قريباً", Toast.LENGTH_SHORT).show()
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
                    navController.navigate("details/$id/$type/${Uri.encode(title)}/1/1/false")
                }
            )
        }
        composable(
            route = "details/{movieId}/{mediaType}/{title}/{season}/{episode}/{autoPlay}",
            arguments = listOf(
                navArgument("movieId") { type = NavType.IntType },
                navArgument("mediaType") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("season") { type = NavType.IntType; defaultValue = 1 },
                navArgument("episode") { type = NavType.IntType; defaultValue = 1 },
                navArgument("autoPlay") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
            val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
            val title = Uri.decode(backStackEntry.arguments?.getString("title") ?: "بدون عنوان")
            val season = backStackEntry.arguments?.getInt("season") ?: 1
            val episode = backStackEntry.arguments?.getInt("episode") ?: 1
            val autoPlay = backStackEntry.arguments?.getBoolean("autoPlay") ?: false
            
            DetailsScreen(
                movieId = movieId,
                mediaType = mediaType,
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
