package com.sahra4ever.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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
                                navController.navigate("details/$id/$type/$title")
                            }
                        )
                    }
                    // 2. شاشة التفاصيل
                    composable("details/{movieId}/{mediaType}/{title}") { backStackEntry ->
                        val movieId = backStackEntry.arguments?.getString("movieId")?.toIntOrNull() ?: 0
                        val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                        val title = backStackEntry.arguments?.getString("title") ?: "بدون عنوان"
                        
                        DetailsScreen(
                            movieId = movieId,
                            mediaType = mediaType,
                            onBack = { navController.popBackStack() },
                            onPlayClick = { id, type, t, season, episode ->
                                // ✅ التوجيه لشاشة المشغل مع الموسم والحلقة
                                navController.navigate("player/$id/$type/$t/$season/$episode")
                            }
                        )
                    }
                    // 3. شاشة المشغل (المشاهدة)
                    composable("player/{movieId}/{mediaType}/{title}/{season}/{episode}") { backStackEntry ->
                        val movieId = backStackEntry.arguments?.getString("movieId")?.toIntOrNull() ?: 0
                        val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                        val title = backStackEntry.arguments?.getString("title") ?: "بدون عنوان"
                        val season = backStackEntry.arguments?.getString("season")?.toIntOrNull() ?: 1
                        val episode = backStackEntry.arguments?.getString("episode")?.toIntOrNull() ?: 1
                        
                        PlayerScreen(
                            movieId = movieId,
                            mediaType = mediaType,
                            title = title,
                            season = season,
                            episode = episode,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}