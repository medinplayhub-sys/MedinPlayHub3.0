package com.example.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TmdbMovieResult(
    val title: String,
    val synopsis: String,
    val duration: String,
    val genre: String,
    val year: Int,
    val rating: String,
    val posterUrl: String
)

object TmdbService {
    private const val API_KEY = "1e90d846573d8e1a756513a9546e3a19"
    private const val BASE_URL = "https://api.themoviedb.org/3"
    private const val IMAGE_BASE_URL = "https://image.tmdb.org/t5/p/w500"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val genreMap = mapOf(
        28 to "Acción",
        12 to "Aventura",
        16 to "Animación",
        35 to "Comedia",
        80 to "Crimen",
        99 to "Documental",
        18 to "Drama",
        10751 to "Familia",
        14 to "Fantasía",
        36 to "Historia",
        27 to "Terror",
        10402 to "Música",
        9648 to "Misterio",
        10749 to "Romance",
        878 to "Ciencia Ficción",
        10770 to "Película de TV",
        53 to "Suspense",
        10752 to "Bélica",
        37 to "Oeste"
    )

    suspend fun searchMovie(query: String): TmdbMovieResult? = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext null
        try {
            val encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8")
            val searchUrl = "$BASE_URL/search/movie?api_key=$API_KEY&language=es-ES&query=$encodedQuery"
            val request = Request.Builder().url(searchUrl).build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val json = JSONObject(responseBody)
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val first = results.getJSONObject(0)
                    val movieId = first.optInt("id", 0)
                    val title = first.optString("title", query)
                    val synopsis = first.optString("overview", "")
                    val releaseDate = first.optString("release_date", "")
                    val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4).toIntOrNull() ?: 2026 else 2026
                    val voteAvg = first.optDouble("vote_average", 0.0)
                    val ratingStr = if (voteAvg > 0) String.format(java.util.Locale.US, "%.1f", voteAvg) else ""
                    val posterPath = first.optString("poster_path", "")
                    val posterUrl = if (posterPath.isNotBlank() && posterPath != "null") "$IMAGE_BASE_URL$posterPath" else ""

                    var durationStr = ""
                    var genreStr = ""

                    val genreIds = first.optJSONArray("genre_ids")
                    if (genreIds != null && genreIds.length() > 0) {
                        val genresList = mutableListOf<String>()
                        for (i in 0 until genreIds.length()) {
                            val gid = genreIds.optInt(i)
                            genreMap[gid]?.let { genresList.add(it) }
                        }
                        if (genresList.isNotEmpty()) {
                            genreStr = genresList.joinToString(", ")
                        }
                    }

                    if (movieId > 0) {
                        try {
                            val detailUrl = "$BASE_URL/movie/$movieId?api_key=$API_KEY&language=es-ES"
                            val detailReq = Request.Builder().url(detailUrl).build()
                            val detailResp = client.newCall(detailReq).execute()
                            val detailBody = detailResp.body?.string() ?: ""
                            if (detailResp.isSuccessful && detailBody.isNotBlank()) {
                                val detailJson = JSONObject(detailBody)
                                val runtime = detailJson.optInt("runtime", 0)
                                if (runtime > 0) {
                                    val hours = runtime / 60
                                    val mins = runtime % 60
                                    durationStr = if (hours > 0) "${hours}h ${mins}m" else "${mins} min"
                                }
                                val detailGenres = detailJson.optJSONArray("genres")
                                if (detailGenres != null && detailGenres.length() > 0) {
                                    val gList = mutableListOf<String>()
                                    for (i in 0 until detailGenres.length()) {
                                        val gObj = detailGenres.getJSONObject(i)
                                        val gName = gObj.optString("name", "")
                                        if (gName.isNotBlank()) gList.add(gName)
                                    }
                                    if (gList.isNotEmpty()) {
                                        genreStr = gList.joinToString(", ")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    return@withContext TmdbMovieResult(
                        title = title,
                        synopsis = synopsis,
                        duration = durationStr.ifBlank { "120 min" },
                        genre = genreStr.ifBlank { "Cine" },
                        year = year,
                        rating = ratingStr.ifBlank { "1080p" },
                        posterUrl = posterUrl
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
