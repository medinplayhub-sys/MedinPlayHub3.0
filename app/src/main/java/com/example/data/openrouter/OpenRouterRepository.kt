package com.example.data.openrouter

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenRouterRepository(private val context: Context) {

    // La clave se carga localmente desde .env mediante BuildConfig.
    private val apiKey = BuildConfig.OPENROUTER_API_KEY

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(OpenRouterRequest::class.java)
    private val responseAdapter = moshi.adapter(OpenRouterResponse::class.java)
    private val analysisAdapter = moshi.adapter(MovieAiAnalysis::class.java)

    private val fallbackModels = listOf(
        "openrouter/auto",
        "nvidia/nemotron-3.5-lightning:free",
        "liquid/lfm-2.5-2.6b:free"
    )

    /**
     * Queries OpenRouter AI to analyze a movie, returning franchise/saga details,
     * Spanish synopsis, and at least 3 similar recommended movies.
     */
    suspend fun analyzeMovie(
        movieTitle: String,
        year: String? = null,
        originalPlot: String? = null
    ): Result<MovieAiAnalysis> = withContext(Dispatchers.IO) {
        if (movieTitle.isBlank()) {
            return@withContext Result.failure(Exception("Título de película vacío"))
        }

        val systemPrompt = """
            Eres un crítico y experto en cine de clase mundial para la aplicación MedinPlay Hub.
            Debes analizar la película y responder ÚNICAMENTE con un objeto JSON válido con la siguiente estructura exacta:
            {
              "titulo": "Título oficial de la película",
              "anio": 2010,
              "duracion": "148 min",
              "genero": "Acción, Ciencia Ficción, Suspense",
              "sinopsis": "Sinopsis atractiva, completa y redactada en español neutro sin spoilers graves.",
              "franquicia": "Explicación breve de la saga o franquicia a la que pertenece o si es autoconclusiva.",
              "secuelas_sagas": ["Película 1 (Año)", "Película 2 (Año)"],
              "recomendaciones": [
                {
                  "titulo": "Título de película recomendada 1",
                  "anio": 1999,
                  "motivo": "Explicación de por qué le gustará al espectador."
                },
                {
                  "titulo": "Título de película recomendada 2",
                  "anio": 2000,
                  "motivo": "Explicación clara."
                },
                {
                  "titulo": "Título de película recomendada 3",
                  "anio": 2014,
                  "motivo": "Explicación clara."
                }
              ],
              "curiosidades": [
                "Dato curioso 1 del rodaje o los actores.",
                "Dato curioso 2."
              ]
            }
        """.trimIndent()

        val userMessage = buildString {
            append("Película: ").append(movieTitle)
            if (!year.isNullOrBlank()) append(" (Año: ").append(year).append(")")
            if (!originalPlot.isNullOrBlank()) append(". Argumento base: ").append(originalPlot)
        }

        for (model in fallbackModels) {
            try {
                val reqBodyJson = requestAdapter.toJson(
                    OpenRouterRequest(
                        model = model,
                        messages = listOf(
                            OpenRouterMessage(role = "system", content = systemPrompt),
                            OpenRouterMessage(role = "user", content = userMessage)
                        ),
                        responseFormat = mapOf("type" to "json_object")
                    )
                )

                val request = Request.Builder()
                    .url("https://openrouter.ai/api/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("HTTP-Referer", "https://medinplayhub.app")
                    .addHeader("X-Title", "MedinPlay Hub")
                    .post(reqBodyJson.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val bodyStr = response.body?.string()

                if (response.isSuccessful && !bodyStr.isNullOrBlank()) {
                    val openRouterResp = responseAdapter.fromJson(bodyStr)
                    val content = openRouterResp?.choices?.firstOrNull()?.message?.content

                    if (!content.isNullOrBlank()) {
                        // Extract JSON substring if wrapped in markdown code blocks
                        val cleanedJson = cleanJsonString(content)
                        val analysis = parseAnalysisJson(cleanedJson)
                        if (analysis != null) {
                            return@withContext Result.success(analysis)
                        }
                    }
                } else {
                    Log.w("OpenRouter", "Error HTTP ${response.code} con modelo $model: $bodyStr")
                }
            } catch (e: Exception) {
                Log.e("OpenRouter", "Fallo llamando a OpenRouter con modelo $model", e)
            }
        }

        Result.failure(Exception("No se pudo obtener el análisis de la IA para $movieTitle"))
    }

    /**
     * Natural language movie finder using OpenRouter
     */
    suspend fun discoverMoviesByPrompt(prompt: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val systemPrompt = """
            Eres un recomendador de cine. El usuario te describirá gustos, géneros o tramas.
            Responde ÚNICAMENTE con un JSON que contenga un array "peliculas" con 6 a 8 títulos de películas exactos con su año entre paréntesis, ej:
            {"peliculas": ["Inception (2010)", "Interstellar (2014)", "The Prestige (2006)"]}
        """.trimIndent()

        try {
            val reqBodyJson = requestAdapter.toJson(
                OpenRouterRequest(
                    model = "openrouter/auto",
                    messages = listOf(
                        OpenRouterMessage(role = "system", content = systemPrompt),
                        OpenRouterMessage(role = "user", content = prompt)
                    ),
                    responseFormat = mapOf("type" to "json_object")
                )
            )

            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(reqBodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string()

            if (response.isSuccessful && !bodyStr.isNullOrBlank()) {
                val openRouterResp = responseAdapter.fromJson(bodyStr)
                val content = openRouterResp?.choices?.firstOrNull()?.message?.content
                if (!content.isNullOrBlank()) {
                    val cleaned = cleanJsonString(content)
                    val json = JSONObject(cleaned)
                    val arr = json.optJSONArray("peliculas")
                    val list = mutableListOf<String>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.optString(i)
                            if (item.isNotBlank()) list.add(item)
                        }
                    }
                    return@withContext Result.success(list)
                }
            }
        } catch (e: Exception) {
            Log.e("OpenRouter", "Fallo en discoverMoviesByPrompt", e)
        }

        Result.failure(Exception("No se pudieron descubrir películas con la IA"))
    }

    private fun cleanJsonString(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.removePrefix("```json")
        } else if (str.startsWith("```")) {
            str = str.removePrefix("```")
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```")
        }
        val firstBrace = str.indexOf('{')
        val lastBrace = str.lastIndexOf('}')
        return if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            str.substring(firstBrace, lastBrace + 1).trim()
        } else {
            str.trim()
        }
    }

    private fun parseAnalysisJson(jsonStr: String): MovieAiAnalysis? {
        return try {
            analysisAdapter.fromJson(jsonStr)
        } catch (e: Exception) {
            try {
                val jsonObj = JSONObject(jsonStr)
                val recommendationsList = mutableListOf<MovieAiRecommendation>()
                val recsArr = jsonObj.optJSONArray("recomendaciones")
                if (recsArr != null) {
                    for (i in 0 until recsArr.length()) {
                        val obj = recsArr.optJSONObject(i)
                        if (obj != null) {
                            recommendationsList.add(
                                MovieAiRecommendation(
                                    title = obj.optString("titulo"),
                                    year = obj.opt("anio"),
                                    reason = obj.optString("motivo")
                                )
                            )
                        }
                    }
                }

                val franchiseList = mutableListOf<String>()
                val sagasArr = jsonObj.optJSONArray("secuelas_sagas")
                if (sagasArr != null) {
                    for (i in 0 until sagasArr.length()) {
                        val s = sagasArr.optString(i)
                        if (s.isNotBlank()) franchiseList.add(s)
                    }
                }

                val curiosities = mutableListOf<String>()
                val curArr = jsonObj.optJSONArray("curiosidades")
                if (curArr != null) {
                    for (i in 0 until curArr.length()) {
                        val s = curArr.optString(i)
                        if (s.isNotBlank()) curiosities.add(s)
                    }
                }

                MovieAiAnalysis(
                    title = jsonObj.optString("titulo"),
                    year = jsonObj.opt("anio"),
                    duration = jsonObj.optString("duracion"),
                    genres = jsonObj.opt("genero"),
                    synopsis = jsonObj.optString("sinopsis"),
                    franchise = jsonObj.optString("franquicia"),
                    franchiseList = franchiseList,
                    recommendations = recommendationsList,
                    curiosities = curiosities
                )
            } catch (ex: Exception) {
                Log.e("OpenRouter", "Error al parsear JSON manualmente", ex)
                null
            }
        }
    }
}
