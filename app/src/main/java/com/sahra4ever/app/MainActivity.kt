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
                    // 1. شاشة الرئيسية
                    composable("home") {
                        HomeScreen(
                            onMovieClick = { id, type ->
                                navController.navigate("details/$id/$type")
                            }
                        )
                    }
                    // 2. شاشة التفاصيل
                    composable("details/{movieId}/{mediaType}") { backStackEntry ->
                        val movieId = backStackEntry.arguments?.getString("movieId")?.toIntOrNull() ?: 0
                        val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                        
                        DetailsScreen(
                            movieId = movieId,
                            mediaType = mediaType,
                            onBack = { navController.popBackStack() },
                            // هذا السطر يربط زر المشاهدة بشاشة المشغل
                            onPlayClick = { id, type ->
                                navController.navigate("player/$id/$type")
                            }
                        )
                    }
                    // 3. شاشة المشاهدة (المشغل)
                    composable("player/{movieId}/{mediaType}") { backStackEntry ->
                        val movieId = backStackEntry.arguments?.getString("movieId")?.toIntOrNull() ?: 0
                        val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                        
                        PlayerScreen(
                            movieId = movieId,
                            mediaType = mediaType,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}