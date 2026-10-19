package com.sahra4ever.app

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TMDBApi {
    @GET("trending/all/day")
    suspend fun getTrendingToday(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "ar-SA"
    ): MovieResponse

    @GET("movie/{id}")
    suspend fun getMovieDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "ar-SA"
    ): Movie

    @GET("tv/{id}")
    suspend fun getTvDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "ar-SA"
    ): Movie

    // ✅ الدالة الجديدة لجلب حلقات المسلسل
    @GET("tv/{id}/season/{season_number}")
    suspend fun getTvSeason(
        @Path("id") id: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "ar-SA"
    ): SeasonResponse

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/3/"

        fun create(): TMDBApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TMDBApi::class.java)
        }
    }
}