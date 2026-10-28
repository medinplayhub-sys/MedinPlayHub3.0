package com.example.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val avatarName: String = "avatar_1",
    val accentColorHex: String = "#1E88E5",
    val favoriteRadioIds: List<String> = emptyList(),
    val favoriteIptvIds: List<String> = emptyList(),
    val favoriteMovieIds: List<String> = emptyList(),
    val pin: String = "",
    val adultPin: String = "",
    val isPrincipal: Boolean = false,
    val gender: String = "Masculino", // "Masculino" or "Femenino"
    val email: String = ""
)

@JsonClass(generateAdapter = false)
data class RadioItem(
    val id: String = "",
    val name: String = "",
    val genre: String = "",
    val genres: List<String> = emptyList(),
    val url: String = "",
    val imageUrl: String = "",
    val description: String = ""
) {
    val displayGenre: String
        get() = if (genres.isNotEmpty()) genres.joinToString(", ") else genre

    val allGenres: List<String>
        get() = if (genres.isNotEmpty()) genres else if (genre.isNotBlank()) genre.split(Regex("[,;/]")).map { it.trim() }.filter { it.isNotBlank() } else emptyList()
}

@JsonClass(generateAdapter = false)
data class IptvItem(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val categories: List<String> = emptyList(),
    val url: String = "",
    val logoUrl: String = "",
    val country: String = "General",
    val isAdult: Boolean = false
) {
    val displayCategory: String
        get() = if (categories.isNotEmpty()) categories.joinToString(", ") else category

    val allCategories: List<String>
        get() = if (categories.isNotEmpty()) categories else if (category.isNotBlank()) category.split(Regex("[,;/]")).map { it.trim() }.filter { it.isNotBlank() } else emptyList()

    val isAdultChannel: Boolean
        get() = isAdult || category.contains("+18", ignoreCase = true) || category.contains("Adult", ignoreCase = true) || name.contains("+18", ignoreCase = true) || categories.any { it.contains("+18", ignoreCase = true) || it.contains("Adult", ignoreCase = true) }
}

@JsonClass(generateAdapter = false)
data class MovieItem(
    val id: String = "",
    val title: String = "",
    val genre: String = "",
    val genres: List<String> = emptyList(),
    val url: String = "",
    val posterUrl: String = "",
    val description: String = "",
    val year: Int = 0,
    val duration: String = "",
    val isAdult: Boolean = false,
    val rating: String = ""
) {
    val displayGenre: String
        get() = if (genres.isNotEmpty()) genres.joinToString(", ") else genre

    val allGenres: List<String>
        get() = if (genres.isNotEmpty()) genres else if (genre.isNotBlank()) genre.split(Regex("[,;/]")).map { it.trim() }.filter { it.isNotBlank() } else emptyList()

    val isAdultMovie: Boolean
        get() = isAdult || genre.contains("+18", ignoreCase = true) || genre.contains("Adult", ignoreCase = true) || title.contains("+18", ignoreCase = true) || genres.any { it.contains("+18", ignoreCase = true) || it.contains("Adult", ignoreCase = true) }
}

@JsonClass(generateAdapter = false)
data class PaymentMethod(
    val id: String,
    val title: String,
    val details: String,
    val instructions: String = "",
    val isActive: Boolean = true
)

@JsonClass(generateAdapter = false)
data class UserPaymentRequest(
    val id: String = "",
    val userEmail: String = "",
    val paymentMethodName: String = "",
    val referenceCode: String = "",
    val amount: String = "",
    val timestamp: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val activationCode: String = "",
    val codeSent: Boolean = false,
    val codeSentTimestamp: String = "",
    val subscriptionStartDate: String = "",
    val subscriptionEndDate: String = "",
    val clientProfiles: List<UserProfile> = emptyList(),
    val sharedByProfile: String = "",
    val notes: String = ""
)

@JsonClass(generateAdapter = false)
data class AdminNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false
)

@JsonClass(generateAdapter = false)
data class AppUpdateInfo(
    val versionCode: Long = 1L,
    val versionName: String = "1.0.0",
    val apkUrl: String = "",
    val releaseNotes: String = "",
    val isForceUpdate: Boolean = false,
    val mandatory: Boolean = false,
    val linkType: String = "direct", // "direct" or "interstitial"
    val showOnBell: Boolean = true,
    val title: String = "",
    val description: String = ""
) {
    val isMandatoryUpdate: Boolean
        get() = mandatory || isForceUpdate
}

@JsonClass(generateAdapter = false)
data class UserHistoryEntry(
    val itemId: String = "",
    val itemType: String = "", // "radio", "iptv", "movie"
    val watchedAt: String = "",
    val progress: Long = 0L
)

@JsonClass(generateAdapter = false)
data class UserAiSuggestion(
    val itemId: String = "",
    val reason: String = "",
    val score: Double = 0.0
)

@JsonClass(generateAdapter = false)
data class UserFirestoreDocument(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoURL: String = "",
    val favorites: List<String> = emptyList(),
    val history: List<UserHistoryEntry> = emptyList(),
    val aiSuggestions: List<UserAiSuggestion> = emptyList(),
    val createdAt: String = "",
    val lastLogin: String = ""
)

@JsonClass(generateAdapter = false)
data class ProfileHistoryData(
    val profileId: String = "",
    val radioHistory: List<String> = emptyList(),
    val iptvHistory: List<String> = emptyList(),
    val movieHistory: List<String> = emptyList(),
    val adultHistory: List<String> = emptyList()
)

@JsonClass(generateAdapter = false)
data class ErrorReport(
    val id: String = "",
    val itemType: String = "", // RADIO, IPTV, MOVIE
    val itemId: String = "",
    val itemName: String = "",
    val issueType: String = "",
    val comments: String = "",
    val userEmail: String = "",
    val profileName: String = "",
    val timestamp: String = "",
    val status: String = "PENDIENTE", // PENDIENTE, RESUELTO
    val resolvedAt: String = ""
)

@JsonClass(generateAdapter = false)
data class AdBanner(
    val id: String = "",
    val title: String = "",
    val type: String = "image", // "image" or "html"
    val imageUrl: String = "",
    val clickUrl: String = "",
    val actionUrl: String = "",
    val htmlCode: String = "",
    val interval: Int = 6,
    val active: Boolean = true,
    val priority: Int = 1,
    val locations: Map<String, Boolean> = emptyMap()
) {
    val effectiveClickUrl: String
        get() = clickUrl.ifBlank { actionUrl }
}


