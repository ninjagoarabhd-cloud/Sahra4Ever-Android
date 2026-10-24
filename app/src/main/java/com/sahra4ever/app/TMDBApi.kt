package com.sahra4ever.app

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface TMDBApi {
    @GET("trending/all/week")
    suspend fun getTrendingWeek(@Query("api_key") apiKey: String, @Query("language") language: String = "en-US"): MovieResponse

    @GET("trending/all/day")
    suspend fun getTrendingToday(@Query("api_key") apiKey: String, @Query("language") language: String = "en-US"): MovieResponse

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(@Query("api_key") apiKey: String, @Query("language") language: String = "en-US"): MovieResponse

    @GET("tv/airing_today")
    suspend fun getAiringTodayTVShows(@Query("api_key") apiKey: String, @Query("language") language: String = "en-US"): MovieResponse

    @GET("discover/tv?with_genres=16&with_original_language=ja&sort_by=popularity.desc&vote_count.gte=300")
    suspend fun getAnimeShows(@Query("api_key") apiKey: String, @Query("language") language: String = "en-US"): MovieResponse

    // البحث مع ترقيم الصفحات
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): MovieResponse

    // الأفلام الأعلى تقييماً حسب التصنيف
    @GET("discover/movie")
    suspend fun getTopRatedMoviesByGenre(
        @Query("api_key") apiKey: String,
        @Query("with_genres") genreId: String,
        @Query("sort_by") sortBy: String = "vote_average.desc",
        @Query("vote_count.gte") voteCount: Int = 200,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): MovieResponse

    // المسلسلات الأعلى تقييماً حسب التصنيف
    @GET("discover/tv")
    suspend fun getTopRatedTvByGenre(
        @Query("api_key") apiKey: String,
        @Query("with_genres") genreId: String,
        @Query("sort_by") sortBy: String = "vote_average.desc",
        @Query("vote_count.gte") voteCount: Int = 200,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): MovieResponse

    // الأفلام الأعلى تقييماً بشكل عام (بدون قيد تصنيف) — لقسم "الأعمال الأعلى تقييماً" في الرئيسية
    @GET("discover/movie")
    suspend fun getTopRatedMoviesDiscover(
        @Query("api_key") apiKey: String,
        @Query("sort_by") sortBy: String = "vote_average.desc",
        @Query("vote_count.gte") voteCount: Int = 500,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): MovieResponse

    // المسلسلات الأعلى تقييماً بشكل عام (بدون قيد تصنيف) — لقسم "الأعمال الأعلى تقييماً" في الرئيسية
    @GET("discover/tv")
    suspend fun getTopRatedTvDiscover(
        @Query("api_key") apiKey: String,
        @Query("sort_by") sortBy: String = "vote_average.desc",
        @Query("vote_count.gte") voteCount: Int = 500,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "en-US"
    ): MovieResponse

    // الأفلام الشائعة (لزر "أفلام" في البحث)
    @GET("movie/popular")
    suspend fun getPopularMovies(@Query("api_key") apiKey: String, @Query("page") page: Int = 1, @Query("language") language: String = "en-US"): MovieResponse

    // المسلسلات الشائعة (لزر "مسلسلات" في البحث)
    @GET("tv/popular")
    suspend fun getPopularTvShows(@Query("api_key") apiKey: String, @Query("page") page: Int = 1, @Query("language") language: String = "en-US"): MovieResponse

    // التفاصيل: نقبل اللغة كمتغير لكي نجلب العربي أولاً ثم الإنجليزي إذا لزم
    @GET("movie/{id}")
    suspend fun getMovieDetails(@Path("id") id: Int, @Query("api_key") apiKey: String, @Query("language") language: String = "ar-SA"): Movie

    @GET("tv/{id}")
    suspend fun getTvDetails(@Path("id") id: Int, @Query("api_key") apiKey: String, @Query("language") language: String = "ar-SA"): Movie

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

/**
 * ✅ (إصلاح الاتصال الشبكي) عميل Retrofit/OkHttp واحد مشترك (Singleton) بين كل الشاشات.
 * يُهيَّأ مرة واحدة فقط (by lazy) ويُعاد استخدامه في كل الاستدعاءات، مما يسمح
 * بإعادة استخدام اتصالات HTTP/2 الدافئة (Connection Pool/Keep-Alive) بدل بدء
 * DNS + TCP + TLS handshake من الصفر في كل مرة يُفتح فيها فيلم جديد.
 */
object TmdbClient {
    private const val BASE_URL = "https://api.themoviedb.org/3/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val api: TMDBApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TMDBApi::class.java)
    }
}