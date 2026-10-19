package com.sahra4ever.app

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TMDBApi {
    // 1. البانر الرئيسي (تريند الأسبوع)
    @GET("trending/all/week")
    suspend fun getTrendingWeek(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "ar-SA"
    ): MovieResponse

    // 2. التريند الآن (تريند اليوم)
    @GET("trending/all/day")
    suspend fun getTrendingToday(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "ar-SA"
    ): MovieResponse

    // 3. وصل حديثاً للسينما
    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "ar-SA"
    ): MovieResponse

    // 4. جديد المسلسلات (تعرض اليوم)
    @GET("tv/airing_today")
    suspend fun getAiringTodayTVShows(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "ar-SA"
    ): MovieResponse

    // 5. عالم الأنمي
    @GET("discover/tv?with_genres=16&with_original_language=ja&sort_by=popularity.desc&vote_count.gte=300")
    suspend fun getAnimeShows(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "ar-SA"
    ): MovieResponse

    // 6. البحث الشامل
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "ar-SA"
    ): MovieResponse

    // 7. تفاصيل فيلم
    @GET("movie/{id}")
    suspend fun getMovieDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "ar-SA"
    ): Movie

    // 8. تفاصيل مسلسل
    @GET("tv/{id}")
    suspend fun getTvDetails(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "ar-SA"
    ): Movie

    // 9. حلقات الموسم
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