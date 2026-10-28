package com.example.service

import com.example.BuildConfig
import com.example.models.IptvItem
import com.example.models.MovieItem
import com.example.models.RadioItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiRecommendationResult(
    val reason: String,
    val recommendedMovieIds: List<String>,
    val recommendedIptvIds: List<String>,
    val recommendedRadioIds: List<String>
)

object GeminiRecommendationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchRecommendations(
        profileName: String,
        radioHistory: List<RadioItem>,
        iptvHistory: List<IptvItem>,
        movieHistory: List<MovieItem>,
        allRadios: List<RadioItem>,
        allIptv: List<IptvItem>,
        allMovies: List<MovieItem>
    ): GeminiRecommendationResult = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateFallbackRecommendations(
                profileName, radioHistory, iptvHistory, movieHistory, allRadios, allIptv, allMovies
            )
        }

        try {
            val radioHistNames = radioHistory.take(5).joinToString(", ") { "${it.name} (${it.genre})" }
            val iptvHistNames = iptvHistory.take(5).joinToString(", ") { "${it.name} (${it.category})" }
            val movieHistNames = movieHistory.take(5).joinToString(", ") { "${it.title} (${it.genre})" }

            val radioCatalog = allRadios.take(15).joinToString("; ") { "ID:${it.id}-Name:${it.name}-Genre:${it.genre}" }
            val iptvCatalog = allIptv.filter { !it.isAdult }.take(20).joinToString("; ") { "ID:${it.id}-Name:${it.name}-Category:${it.category}" }
            val movieCatalog = allMovies.filter { !it.isAdult }.take(15).joinToString("; ") { "ID:${it.id}-Title:${it.title}-Genre:${it.genre}" }

            val promptText = """
                Eres el motor de recomendaciones IA de MedinPlay.
                Perfil de usuario: "$profileName".
                Historial reciente del usuario:
                - Radios: ${radioHistNames.ifBlank { "Ninguna" }}
                - Canales TV: ${iptvHistNames.ifBlank { "Ninguno" }}
                - Películas: ${movieHistNames.ifBlank { "Ninguna" }}

                Catálogo disponible:
                - Películas: $movieCatalog
                - Canales TV: $iptvCatalog
                - Radios: $radioCatalog

                Genera una recomendación personalizada. Devuelve ÚNICAMENTE un objeto JSON válido con este formato exacto:
                {
                  "reason": "Explicación breve de 1 o 2 oraciones en español del porqué de las recomendaciones.",
                  "recommendedMovieIds": ["id1", "id2"],
                  "recommendedIptvIds": ["id1", "id2"],
                  "recommendedRadioIds": ["id1", "id2"]
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArr = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArr = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", promptText)
                            }
                            put(partObj)
                        }
                        put("parts", partsArr)
                    }
                    put(contentObj)
                }
                put("contents", contentsArr)
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        var text = parts.getJSONObject(0).optString("text", "")
                        text = text.replace("```json", "").replace("```", "").trim()
                        
                        val recObj = JSONObject(text)
                        val reason = recObj.optString("reason", "Recomendaciones basadas en tu historial de reproducción.")
                        
                        val recMovies = parseJsonArray(recObj.optJSONArray("recommendedMovieIds"))
                        val recIptv = parseJsonArray(recObj.optJSONArray("recommendedIptvIds"))
                        val recRadios = parseJsonArray(recObj.optJSONArray("recommendedRadioIds"))

                        return@withContext GeminiRecommendationResult(
                            reason = reason,
                            recommendedMovieIds = recMovies,
                            recommendedIptvIds = recIptv,
                            recommendedRadioIds = recRadios
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext generateFallbackRecommendations(
            profileName, radioHistory, iptvHistory, movieHistory, allRadios, allIptv, allMovies
        )
    }

    private fun parseJsonArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.optString(i))
        }
        return list
    }

    private fun generateFallbackRecommendations(
        profileName: String,
        radioHistory: List<RadioItem>,
        iptvHistory: List<IptvItem>,
        movieHistory: List<MovieItem>,
        allRadios: List<RadioItem>,
        allIptv: List<IptvItem>,
        allMovies: List<MovieItem>
    ): GeminiRecommendationResult {
        val nonAdultMovies = allMovies.filter { !it.isAdult }
        val nonAdultIptv = allIptv.filter { !it.isAdult }

        val recMovies = nonAdultMovies.take(3).map { it.id }
        val recIptv = nonAdultIptv.take(3).map { it.id }
        val recRadios = allRadios.take(3).map { it.id }

        val reason = if (movieHistory.isNotEmpty() || iptvHistory.isNotEmpty() || radioHistory.isNotEmpty()) {
            "Basado en tus selecciones recientes en MedinPlay para $profileName, hemos seleccionado el contenido más popular para ti."
        } else {
            "¡Bienvenido $profileName! Descubre los títulos y canales destacados recomendados por la IA de MedinPlay."
        }

        return GeminiRecommendationResult(
            reason = reason,
            recommendedMovieIds = recMovies,
            recommendedIptvIds = recIptv,
            recommendedRadioIds = recRadios
        )
    }
}
