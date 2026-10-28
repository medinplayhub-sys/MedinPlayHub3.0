package com.example.data.omdb

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class OmdbRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("omdb_prefs", Context.MODE_PRIVATE)

    // The 3 API keys provided by the user (1,000 requests/day each = 3,000 total daily)
    val apiKeys = listOf(
        "130e8e7c", // Key 1
        "2b9df909", // Key 2
        "130c973d"  // Key 3
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val searchAdapter = moshi.adapter(OmdbSearchResponse::class.java)
    private val detailAdapter = moshi.adapter(OmdbMovieDetail::class.java)

    var currentKeyIndex: Int
        get() = prefs.getInt("omdb_active_key_index", 0) % apiKeys.size
        private set(value) {
            prefs.edit().putInt("omdb_active_key_index", value % apiKeys.size).apply()
        }

    fun getCurrentKey(): String = apiKeys[currentKeyIndex]

    fun getActiveKeyLabel(): String = "Clave #${currentKeyIndex + 1} (${getCurrentKey()})"

    private fun rotateKey() {
        val next = (currentKeyIndex + 1) % apiKeys.size
        Log.w("OmdbRepository", "⚠️ Límite de OMDb alcanzado o clave inválida. Rotando de clave #${currentKeyIndex + 1} a clave #${next + 1}")
        currentKeyIndex = next
    }

    private fun isLimitReachedError(errorMessage: String?): Boolean {
        if (errorMessage.isNullOrBlank()) return false
        val lower = errorMessage.lowercase()
        return lower.contains("limit") || lower.contains("request limit") || lower.contains("daily") || lower.contains("key")
    }

    /**
     * Searches movies in OMDb by keyword, rotating keys automatically if daily limit is reached.
     */
    suspend fun searchMovies(query: String, page: Int = 1): Result<List<OmdbSearchItem>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext Result.success(emptyList())

        val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
        var attempts = 0
        var lastError = "Error desconocido"

        while (attempts < apiKeys.size) {
            val key = getCurrentKey()
            val url = "https://www.omdbapi.com/?s=$encodedQuery&type=movie&page=$page&apikey=$key"

            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val bodyString = response.body?.string()

                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val parsed = searchAdapter.fromJson(bodyString)
                    if (parsed?.response?.equals("True", ignoreCase = true) == true) {
                        return@withContext Result.success(parsed.search ?: emptyList())
                    } else {
                        val errMsg = parsed?.error ?: "Película no encontrada"
                        if (isLimitReachedError(errMsg)) {
                            rotateKey()
                            attempts++
                            lastError = errMsg
                            continue
                        } else {
                            return@withContext Result.failure(Exception(errMsg))
                        }
                    }
                } else if (response.code == 401 || response.code == 429) {
                    rotateKey()
                    attempts++
                    lastError = "HTTP ${response.code} límite de solicitudes"
                    continue
                } else {
                    return@withContext Result.failure(Exception("Error de red: código ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e("OmdbRepository", "Excepción buscando en OMDb con clave $key", e)
                lastError = e.message ?: "Fallo de conexión"
                attempts++
                rotateKey()
            }
        }

        Result.failure(Exception("Se alcanzó el límite diario en las 3 claves OMDb (3,000 consultas/día): $lastError"))
    }

    /**
     * Gets movie details by IMDb ID (e.g., tt1375666) or Title
     */
    suspend fun getMovieDetails(imdbId: String): Result<OmdbMovieDetail> = withContext(Dispatchers.IO) {
        val cleanId = imdbId.trim()
        if (cleanId.isBlank()) return@withContext Result.failure(Exception("ID inválido"))

        var attempts = 0
        var lastError = "Error desconocido"

        while (attempts < apiKeys.size) {
            val key = getCurrentKey()
            val url = "https://www.omdbapi.com/?i=$cleanId&plot=full&apikey=$key"

            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val bodyString = response.body?.string()

                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val parsed = detailAdapter.fromJson(bodyString)
                    if (parsed?.response?.equals("True", ignoreCase = true) == true) {
                        return@withContext Result.success(parsed)
                    } else {
                        val errMsg = parsed?.error ?: "Detalles no encontrados"
                        if (isLimitReachedError(errMsg)) {
                            rotateKey()
                            attempts++
                            lastError = errMsg
                            continue
                        } else {
                            return@withContext Result.failure(Exception(errMsg))
                        }
                    }
                } else if (response.code == 401 || response.code == 429) {
                    rotateKey()
                    attempts++
                    lastError = "HTTP ${response.code} límite de solicitudes"
                    continue
                } else {
                    return@withContext Result.failure(Exception("Error de red: código ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e("OmdbRepository", "Excepción obteniendo detalles OMDb con clave $key", e)
                lastError = e.message ?: "Fallo de conexión"
                attempts++
                rotateKey()
            }
        }

        Result.failure(Exception("Se alcanzó el límite en las 3 claves OMDb: $lastError"))
    }

    /**
     * Gets movie details by exact Title and optional Year
     */
    suspend fun getMovieByTitle(title: String, year: Int? = null): Result<OmdbMovieDetail> = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return@withContext Result.failure(Exception("Título inválido"))

        val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
        var attempts = 0
        var lastError = "Error desconocido"

        while (attempts < apiKeys.size) {
            val key = getCurrentKey()
            val yearParam = if (year != null && year > 1900) "&y=$year" else ""
            val url = "https://www.omdbapi.com/?t=$encodedTitle$yearParam&plot=full&apikey=$key"

            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val bodyString = response.body?.string()

                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val parsed = detailAdapter.fromJson(bodyString)
                    if (parsed?.response?.equals("True", ignoreCase = true) == true) {
                        return@withContext Result.success(parsed)
                    } else {
                        val errMsg = parsed?.error ?: "Película no encontrada"
                        if (isLimitReachedError(errMsg)) {
                            rotateKey()
                            attempts++
                            lastError = errMsg
                            continue
                        } else {
                            return@withContext Result.failure(Exception(errMsg))
                        }
                    }
                } else if (response.code == 401 || response.code == 429) {
                    rotateKey()
                    attempts++
                    lastError = "HTTP ${response.code} límite de solicitudes"
                    continue
                } else {
                    return@withContext Result.failure(Exception("Error de red: código ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e("OmdbRepository", "Excepción en getMovieByTitle OMDb con clave $key", e)
                lastError = e.message ?: "Fallo de conexión"
                attempts++
                rotateKey()
            }
        }

        Result.failure(Exception("Se alcanzó el límite en las 3 claves OMDb: $lastError"))
    }
}
