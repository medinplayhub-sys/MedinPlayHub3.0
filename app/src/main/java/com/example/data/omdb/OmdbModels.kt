package com.example.data.omdb

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class OmdbSearchResponse(
    @Json(name = "Search") val search: List<OmdbSearchItem>? = null,
    val totalResults: String? = null,
    @Json(name = "Response") val response: String = "False",
    @Json(name = "Error") val error: String? = null
)

@JsonClass(generateAdapter = false)
data class OmdbSearchItem(
    @Json(name = "Title") val title: String = "",
    @Json(name = "Year") val year: String = "",
    val imdbID: String = "",
    @Json(name = "Type") val type: String = "movie",
    @Json(name = "Poster") val poster: String = ""
) {
    val cleanPoster: String
        get() = if (poster.isNotBlank() && !poster.equals("N/A", ignoreCase = true)) poster else ""

    val cleanYear: Int
        get() = year.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0
}

@JsonClass(generateAdapter = false)
data class OmdbRating(
    @Json(name = "Source") val source: String = "",
    @Json(name = "Value") val value: String = ""
)

@JsonClass(generateAdapter = false)
data class OmdbMovieDetail(
    @Json(name = "Title") val title: String = "",
    @Json(name = "Year") val year: String = "",
    @Json(name = "Rated") val rated: String = "",
    @Json(name = "Released") val released: String = "",
    @Json(name = "Runtime") val runtime: String = "",
    @Json(name = "Genre") val genre: String = "",
    @Json(name = "Director") val director: String = "",
    @Json(name = "Writer") val writer: String = "",
    @Json(name = "Actors") val actors: String = "",
    @Json(name = "Plot") val plot: String = "",
    @Json(name = "Language") val language: String = "",
    @Json(name = "Country") val country: String = "",
    @Json(name = "Awards") val awards: String = "",
    @Json(name = "Poster") val poster: String = "",
    @Json(name = "Ratings") val ratings: List<OmdbRating>? = null,
    val metascore: String? = null,
    val imdbRating: String? = null,
    val imdbVotes: String? = null,
    val imdbID: String = "",
    @Json(name = "Type") val type: String = "movie",
    @Json(name = "Response") val response: String = "False",
    @Json(name = "Error") val error: String? = null
) {
    val cleanPoster: String
        get() = if (poster.isNotBlank() && !poster.equals("N/A", ignoreCase = true)) poster else ""

    val cleanPlot: String
        get() = if (plot.isNotBlank() && !plot.equals("N/A", ignoreCase = true)) plot else ""

    val cleanYear: Int
        get() = year.filter { it.isDigit() }.take(4).toIntOrNull() ?: 0

    val cleanRating: String
        get() = if (!imdbRating.isNullOrBlank() && !imdbRating.equals("N/A", ignoreCase = true)) imdbRating else ""
}
