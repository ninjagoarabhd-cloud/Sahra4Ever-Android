package com.sahra4ever.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
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
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = "home") {
                    // 1. الشاشة الرئيسية
                    composable("home") {
                        HomeScreen(
                            onMovieClick = { id, type, title ->
                                navController.navigate("details/$id/$type/$title/1/1/false")
                            },
                            onContinueWatchingClick = { historyItem ->
                                navController.navigate("details/${historyItem.id}/${historyItem.type}/${historyItem.title}/${historyItem.season}/${historyItem.episode}/true")
                            },
                            onCategoryClick = { category ->
                                // الانتقال لصفحة البحث مع التصنيف المختار
                                navController.navigate("search/$category")
                            },
                            onSearchClick = {
                                navController.navigate("search/الكل")
                            }
                        )
                    }

                    // 2. صفحة البحث الجديدة
                    composable(
                        route = "search/{category}",
                        arguments = listOf(navArgument("category") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val category = backStackEntry.arguments?.getString("category") ?: "الكل"
                        SearchScreen(
                            initialCategory = category,
                            onBack = { navController.popBackStack() },
                            onMovieClick = { id, type, title ->
                                navController.navigate("details/$id/$type/$title/1/1/false")
                            }
                        )
                    }
                    
                    // 3. شاشة التفاصيل
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
                        val title = backStackEntry.arguments?.getString("title") ?: "بدون عنوان"
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
                                navController.navigate("player/$id/$type/$t/$s/$e/$serverIndex")
                            }
                        )
                    }
                    
                    // 4. شاشة المشغل (التي برمجناها بالكامل سابقاً)
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
                        val title = backStackEntry.arguments?.getString("title") ?: "بدون عنوان"
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
        }
    }
}