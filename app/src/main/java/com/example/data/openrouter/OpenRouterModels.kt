package com.example.data.openrouter

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class MovieAiAnalysis(
    @Json(name = "titulo") val title: String = "",
    @Json(name = "anio") val year: Any? = null,
    @Json(name = "duracion") val duration: String = "",
    @Json(name = "genero") val genres: Any? = null,
    @Json(name = "sinopsis") val synopsis: String = "",
    @Json(name = "franquicia") val franchise: String = "",
    @Json(name = "secuelas_sagas") val franchiseList: List<String>? = null,
    @Json(name = "recomendaciones") val recommendations: List<MovieAiRecommendation> = emptyList(),
    @Json(name = "curiosidades") val curiosities: List<String>? = null
) {
    val displayYear: String
        get() = when (val y = year) {
            is Number -> y.toInt().toString()
            is String -> y.filter { it.isDigit() }.take(4)
            else -> ""
        }

    val displayGenres: String
        get() = when (val g = genres) {
            is List<*> -> g.filterNotNull().joinToString(", ")
            is String -> g
            else -> ""
        }

    val displayFranchise: String
        get() = if (franchiseList != null && franchiseList.isNotEmpty()) {
            franchiseList.joinToString(" • ")
        } else if (franchise.isNotBlank()) {
            franchise
        } else {
            "Película independiente / No tiene saga registrada"
        }
}

@JsonClass(generateAdapter = false)
data class MovieAiRecommendation(
    @Json(name = "titulo") val title: String = "",
    @Json(name = "anio") val year: Any? = null,
    @Json(name = "motivo") val reason: String = ""
) {
    val displayYear: String
        get() = when (val y = year) {
            is Number -> y.toInt().toString()
            is String -> y.filter { it.isDigit() }.take(4)
            else -> ""
        }
}

// OpenRouter API payload & response classes
@JsonClass(generateAdapter = false)
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = false)
data class OpenRouterRequest(
    val model: String = "openrouter/auto",
    val messages: List<OpenRouterMessage>,
    @Json(name = "response_format") val responseFormat: Map<String, String>? = mapOf("type" to "json_object")
)

@JsonClass(generateAdapter = false)
data class OpenRouterChoice(
    val message: OpenRouterMessage
)

@JsonClass(generateAdapter = false)
data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>? = null,
    val error: Map<String, Any>? = null
)
