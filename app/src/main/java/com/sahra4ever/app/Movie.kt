package com.sahra4ever.app

import com.google.gson.annotations.SerializedName

data class Movie(
    val id: Int,
    val title: String?,
    val name: String?, 
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("media_type") val mediaType: String?,
    val seasons: List<Season>? // أضفنا المواسم
)

data class MovieResponse(
    val results: List<Movie>
)

data class Season(
    @SerializedName("season_number") val seasonNumber: Int,
    val name: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    val overview: String?
)

data class Episode(
    @SerializedName("episode_number") val episodeNumber: Int,
    val name: String?,
    val overview: String?
)

data class SeasonResponse(
    val episodes: List<Episode>
)