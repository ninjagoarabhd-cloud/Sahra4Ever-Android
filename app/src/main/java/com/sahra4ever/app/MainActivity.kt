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
                    composable("home") {
                        HomeScreen(
                            onMovieClick = { id, type, title ->
                                navController.navigate("details/$id/$type/$title")
                            }
                        )
                    }
                    composable("details/{movieId}/{mediaType}/{title}") { backStackEntry ->
                        val movieId = backStackEntry.arguments?.getString("movieId")?.toIntOrNull() ?: 0
                        val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                        val title = backStackEntry.arguments?.getString("title") ?: "بدون عنوان"
                        
                        DetailsScreen(
                            movieId = movieId,
                            mediaType = mediaType,
                            onBack = { navController.popBackStack() },
                            onPlayClick = { id, type, t ->
                                navController.navigate("player/$id/$type/$t")
                            }
                        )
                    }
                    composable("player/{movieId}/{mediaType}/{title}") { backStackEntry ->
                        val movieId = backStackEntry.arguments?.getString("movieId")?.toIntOrNull() ?: 0
                        val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                        val title = backStackEntry.arguments?.getString("title") ?: "بدون عنوان"
                        
                        PlayerScreen(
                            movieId = movieId,
                            mediaType = mediaType,
                            title = title,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}