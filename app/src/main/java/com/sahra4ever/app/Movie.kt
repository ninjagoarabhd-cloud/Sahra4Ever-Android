package com.sahra4ever.app

import com.google.gson.annotations.SerializedName

data class Movie(
    val id: Int,
    val title: String?,
    val name: String?, // للمسلسلات
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("media_type") val mediaType: String?
)

// هذا القالب يمثل استجابة TMDB التي تحتوي على قائمة الأفلام
data class MovieResponse(
    val results: List<Movie>
)