package com.sahra4ever.app

import com.google.gson.annotations.SerializedName

data class Movie(
    val id: Int,
    val title: String?,
    val name: String?, 
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?, // صورة الخلفية الكبيرة
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("media_type") val mediaType: String?
)

data class MovieResponse(
    val results: List<Movie>
)