package com.example.data

import android.content.Context
import com.example.models.IptvItem
import com.example.models.MovieItem
import com.example.models.RadioItem
import com.example.models.UserProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.JsonClass

class DataRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("medinplay_hub_prefs", Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Default Seed Data
    private val defaultProfiles = listOf(
        UserProfile(
            id = "p_principal",
            name = "Perfil Principal",
            avatarName = "avatar_1",
            accentColorHex = "#C67C65",
            isPrincipal = true
        )
    )

    fun isAppActivated(): Boolean {
        return sharedPrefs.getBoolean("is_app_activated", true)
    }

    fun setAppActivated(activated: Boolean) {
        sharedPrefs.edit().putBoolean("is_app_activated", activated).apply()
    }

    fun getSubscriptionPrice(): String {
        return sharedPrefs.getString("subscription_price", "10.00$") ?: "10.00$"
    }

    fun setSubscriptionPrice(price: String) {
        sharedPrefs.edit().putString("subscription_price", price).apply()
    }

    fun getRegisteredEmail(): String {
        return sharedPrefs.getString("registered_email", "") ?: ""
    }

    fun setRegisteredEmail(email: String) {
        sharedPrefs.edit().putString("registered_email", email).apply()
    }

    fun isDisableShareDialog(): Boolean {
        return sharedPrefs.getBoolean("disable_share_dialog", false)
    }

    fun setDisableShareDialog(disabled: Boolean) {
        sharedPrefs.edit().putBoolean("disable_share_dialog", disabled).apply()
    }

    fun getSkippedVersion(): Long {
        return sharedPrefs.getLong("skipped_version", 0L)
    }

    fun saveSkippedVersion(versionCode: Long) {
        sharedPrefs.edit().putLong("skipped_version", versionCode).apply()
    }

    fun isBaseAppResetDone(): Boolean {
        return sharedPrefs.getBoolean("base_app_reset_done", false)
    }

    fun setBaseAppResetDone(done: Boolean) {
        sharedPrefs.edit().putBoolean("base_app_reset_done", done).apply()
    }

    private val defaultRadios = emptyList<RadioItem>()
    private val defaultIptv = emptyList<IptvItem>()
    private val defaultMovies = emptyList<MovieItem>()

    // Helper functions to read/write SharedPreferences

    fun loadProfiles(): List<UserProfile> {
        val json = sharedPrefs.getString("profiles_list", null)
        return if (json == null) {
            saveProfiles(defaultProfiles)
            defaultProfiles
        } else {
            try {
                val type = Types.newParameterizedType(List::class.java, UserProfile::class.java)
                val adapter = moshi.adapter<List<UserProfile>>(type)
                adapter.fromJson(json) ?: defaultProfiles
            } catch (e: Exception) {
                defaultProfiles
            }
        }
    }

    fun saveProfiles(profiles: List<UserProfile>) {
        val type = Types.newParameterizedType(List::class.java, UserProfile::class.java)
        val adapter = moshi.adapter<List<UserProfile>>(type)
        val json = adapter.toJson(profiles)
        sharedPrefs.edit().putString("profiles_list", json).apply()
    }

    fun loadRadios(): List<RadioItem> {
        val json = sharedPrefs.getString("radios_list", null)
        return if (json == null) {
            saveRadios(defaultRadios)
            defaultRadios
        } else {
            try {
                val type = Types.newParameterizedType(List::class.java, RadioItem::class.java)
                val adapter = moshi.adapter<List<RadioItem>>(type)
                adapter.fromJson(json) ?: defaultRadios
            } catch (e: Exception) {
                defaultRadios
            }
        }
    }

    fun saveRadios(radios: List<RadioItem>) {
        val type = Types.newParameterizedType(List::class.java, RadioItem::class.java)
        val adapter = moshi.adapter<List<RadioItem>>(type)
        val json = adapter.toJson(radios)
        sharedPrefs.edit().putString("radios_list", json).apply()
    }

    fun saveLastActiveRadio(radio: RadioItem?) {
        if (radio == null) {
            sharedPrefs.edit().remove("last_active_radio").apply()
        } else {
            val adapter = moshi.adapter(RadioItem::class.java)
            sharedPrefs.edit().putString("last_active_radio", adapter.toJson(radio)).apply()
        }
    }

    fun getLastActiveRadio(): RadioItem? {
        val json = sharedPrefs.getString("last_active_radio", null) ?: return null
        return try {
            val adapter = moshi.adapter(RadioItem::class.java)
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun loadIptv(): List<IptvItem> {
        val json = sharedPrefs.getString("iptv_list", null)
        return if (json == null) {
            saveIptv(defaultIptv)
            defaultIptv
        } else {
            try {
                val type = Types.newParameterizedType(List::class.java, IptvItem::class.java)
                val adapter = moshi.adapter<List<IptvItem>>(type)
                adapter.fromJson(json) ?: defaultIptv
            } catch (e: Exception) {
                defaultIptv
            }
        }
    }

    fun saveIptv(iptv: List<IptvItem>) {
        val type = Types.newParameterizedType(List::class.java, IptvItem::class.java)
        val adapter = moshi.adapter<List<IptvItem>>(type)
        val json = adapter.toJson(iptv)
        sharedPrefs.edit().putString("iptv_list", json).apply()
    }

    fun loadMovies(): List<MovieItem> {
        val json = sharedPrefs.getString("movies_list", null)
        return if (json == null) {
            saveMovies(defaultMovies)
            defaultMovies
        } else {
            try {
                val type = Types.newParameterizedType(List::class.java, MovieItem::class.java)
                val adapter = moshi.adapter<List<MovieItem>>(type)
                adapter.fromJson(json) ?: defaultMovies
            } catch (e: Exception) {
                defaultMovies
            }
        }
    }

    fun saveMovies(movies: List<MovieItem>) {
        val type = Types.newParameterizedType(List::class.java, MovieItem::class.java)
        val adapter = moshi.adapter<List<MovieItem>>(type)
        val json = adapter.toJson(movies)
        sharedPrefs.edit().putString("movies_list", json).apply()
    }

    fun getUnifiedConfigJson(): String {
        val config = UnifiedConfig(
            radios = loadRadios(),
            iptv = loadIptv(),
            movies = loadMovies(),
            profiles = loadProfiles()
        )
        return try {
            val adapter = moshi.adapter(UnifiedConfig::class.java)
            adapter.indent("  ").toJson(config)
        } catch (e: Exception) {
            ""
        }
    }

    fun importUnifiedConfigJson(json: String): Boolean {
        return try {
            val adapter = moshi.adapter(UnifiedConfig::class.java)
            val config = adapter.fromJson(json) ?: return false
            
            // Save each item
            saveRadios(config.radios)
            saveIptv(config.iptv)
            saveMovies(config.movies)
            saveProfiles(config.profiles)
            
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getMovieProgress(profileId: String, movieId: String): Long {
        return sharedPrefs.getLong("movie_progress_${profileId}_${movieId}", 0L)
    }

    fun saveMovieProgress(profileId: String, movieId: String, progressMillis: Long) {
        sharedPrefs.edit().putLong("movie_progress_${profileId}_${movieId}", progressMillis).apply()
    }

    // --- PLAYBACK HISTORY IMPLEMENTATION ---
    fun getRadioHistory(profileId: String): List<String> {
        val json = sharedPrefs.getString("history_radios_$profileId", null) ?: return emptyList()
        return try {
            val type = Types.newParameterizedType(List::class.java, String::class.java)
            val adapter = moshi.adapter<List<String>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRadioHistory(profileId: String, history: List<String>) {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        val json = adapter.toJson(history)
        sharedPrefs.edit().putString("history_radios_$profileId", json).apply()
    }

    fun addRadioToHistory(profileId: String, radioId: String) {
        val current = getRadioHistory(profileId).toMutableList()
        current.remove(radioId)
        current.add(0, radioId)
        if (current.size > 20) {
            current.removeAt(current.size - 1)
        }
        saveRadioHistory(profileId, current)
    }

    fun getIptvHistory(profileId: String): List<String> {
        val json = sharedPrefs.getString("history_iptv_$profileId", null) ?: return emptyList()
        return try {
            val type = Types.newParameterizedType(List::class.java, String::class.java)
            val adapter = moshi.adapter<List<String>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveIptvHistory(profileId: String, history: List<String>) {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        val json = adapter.toJson(history)
        sharedPrefs.edit().putString("history_iptv_$profileId", json).apply()
    }

    fun addIptvToHistory(profileId: String, iptvId: String) {
        val current = getIptvHistory(profileId).toMutableList()
        current.remove(iptvId)
        current.add(0, iptvId)
        if (current.size > 20) {
            current.removeAt(current.size - 1)
        }
        saveIptvHistory(profileId, current)
    }

    fun getMovieHistory(profileId: String): List<String> {
        val json = sharedPrefs.getString("history_movies_$profileId", null) ?: return emptyList()
        return try {
            val type = Types.newParameterizedType(List::class.java, String::class.java)
            val adapter = moshi.adapter<List<String>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMovieHistory(profileId: String, history: List<String>) {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        val json = adapter.toJson(history)
        sharedPrefs.edit().putString("history_movies_$profileId", json).apply()
    }

    fun addMovieToHistory(profileId: String, movieId: String) {
        val current = getMovieHistory(profileId).toMutableList()
        current.remove(movieId)
        current.add(0, movieId)
        if (current.size > 20) {
            current.removeAt(current.size - 1)
        }
        saveMovieHistory(profileId, current)
    }

    fun clearAllHistory(profileId: String) {
        sharedPrefs.edit()
            .remove("history_radios_$profileId")
            .remove("history_iptv_$profileId")
            .remove("history_movies_$profileId")
            .apply()
    }

    fun clearRadioHistory(profileId: String) {
        sharedPrefs.edit().remove("history_radios_$profileId").apply()
    }

    fun clearIptvHistory(profileId: String) {
        sharedPrefs.edit().remove("history_iptv_$profileId").apply()
    }

    fun clearMovieHistory(profileId: String) {
        sharedPrefs.edit().remove("history_movies_$profileId").apply()
    }

    // --- ADULT HISTORY IMPLEMENTATION ---
    fun getAdultHistory(profileId: String): List<String> {
        val json = sharedPrefs.getString("history_adult_$profileId", null) ?: return emptyList()
        return try {
            val type = Types.newParameterizedType(List::class.java, String::class.java)
            val adapter = moshi.adapter<List<String>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAdultHistory(profileId: String, history: List<String>) {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        val json = adapter.toJson(history)
        sharedPrefs.edit().putString("history_adult_$profileId", json).apply()
    }

    fun addAdultToHistory(profileId: String, adultItemId: String) {
        val current = getAdultHistory(profileId).toMutableList()
        current.remove(adultItemId)
        current.add(0, adultItemId)
        if (current.size > 20) {
            current.removeAt(current.size - 1)
        }
        saveAdultHistory(profileId, current)
    }

    fun clearAdultHistory(profileId: String) {
        sharedPrefs.edit().remove("history_adult_$profileId").apply()
    }

    fun removeRadioHistoryItems(profileId: String, idsToRemove: List<String>) {
        val current = getRadioHistory(profileId).filterNot { idsToRemove.contains(it) }
        saveRadioHistory(profileId, current)
    }

    fun removeIptvHistoryItems(profileId: String, idsToRemove: List<String>) {
        val current = getIptvHistory(profileId).filterNot { idsToRemove.contains(it) }
        saveIptvHistory(profileId, current)
    }

    fun removeMovieHistoryItems(profileId: String, idsToRemove: List<String>) {
        val current = getMovieHistory(profileId).filterNot { idsToRemove.contains(it) }
        saveMovieHistory(profileId, current)
    }

    fun removeAdultHistoryItems(profileId: String, idsToRemove: List<String>) {
        val current = getAdultHistory(profileId).filterNot { idsToRemove.contains(it) }
        saveAdultHistory(profileId, current)
    }

    // --- PAYMENT METHODS & SEED APP MANAGEMENT ---
    private val defaultPaymentMethods = listOf(
        com.example.models.PaymentMethod("pm_pago_movil", "Pago Móvil (Venezuela)", "Banco: Banesco (0134)\nCI/RIF: V-24123456\nTeléfono: 0412-5550199", "Monto: $5.00 USD (o Bs. a tasa oficial). Ingresa el número de referencia de la transferencia."),
        com.example.models.PaymentMethod("pm_zelle", "Zelle (USD)", "Correo: medinplayhub@gmail.com\nTitular: MedinPlay Corp", "Envía $5.00 USD. Adjunta el correo de la cuenta Zelle y código de confirmación."),
        com.example.models.PaymentMethod("pm_binance", "Binance Pay (USDT)", "Binance Pay ID: 849301294\nCorreo: medinplayhub@gmail.com", "Red recomendada: Binance Pay. Monto: 5.00 USDT."),
        com.example.models.PaymentMethod("pm_transferencia", "Transferencia Bancaria", "Banco Mercantil\nCuenta Corriente: 0105-0012-3456-7890-1234\nRIF: J-50123456-7", "Realiza la transferencia y reporta tu número de referencia.")
    )

    fun loadPaymentMethods(): List<com.example.models.PaymentMethod> {
        val json = sharedPrefs.getString("payment_methods_list", null) ?: return defaultPaymentMethods
        return try {
            val type = Types.newParameterizedType(List::class.java, com.example.models.PaymentMethod::class.java)
            moshi.adapter<List<com.example.models.PaymentMethod>>(type).fromJson(json) ?: defaultPaymentMethods
        } catch (e: Exception) {
            defaultPaymentMethods
        }
    }

    fun savePaymentMethods(methods: List<com.example.models.PaymentMethod>) {
        val type = Types.newParameterizedType(List::class.java, com.example.models.PaymentMethod::class.java)
        val json = moshi.adapter<List<com.example.models.PaymentMethod>>(type).toJson(methods)
        sharedPrefs.edit().putString("payment_methods_list", json).apply()
    }

    fun loadPaymentRequests(): List<com.example.models.UserPaymentRequest> {
        val json = sharedPrefs.getString("payment_requests_list", null) ?: return emptyList()
        return try {
            val type = Types.newParameterizedType(List::class.java, com.example.models.UserPaymentRequest::class.java)
            moshi.adapter<List<com.example.models.UserPaymentRequest>>(type).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePaymentRequests(requests: List<com.example.models.UserPaymentRequest>) {
        val type = Types.newParameterizedType(List::class.java, com.example.models.UserPaymentRequest::class.java)
        val json = moshi.adapter<List<com.example.models.UserPaymentRequest>>(type).toJson(requests)
        sharedPrefs.edit().putString("payment_requests_list", json).apply()
    }

    fun addPaymentRequest(request: com.example.models.UserPaymentRequest) {
        val current = loadPaymentRequests().toMutableList()
        current.add(0, request)
        savePaymentRequests(current)
    }

    fun loadAdminNotifications(): List<com.example.models.AdminNotification> {
        val json = sharedPrefs.getString("admin_notifications_list", null) ?: return emptyList()
        return try {
            val type = Types.newParameterizedType(List::class.java, com.example.models.AdminNotification::class.java)
            moshi.adapter<List<com.example.models.AdminNotification>>(type).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveAdminNotifications(notes: List<com.example.models.AdminNotification>) {
        val type = Types.newParameterizedType(List::class.java, com.example.models.AdminNotification::class.java)
        val json = moshi.adapter<List<com.example.models.AdminNotification>>(type).toJson(notes)
        sharedPrefs.edit().putString("admin_notifications_list", json).apply()
    }

    fun addAdminNotification(title: String, message: String) {
        val current = loadAdminNotifications().toMutableList()
        val note = com.example.models.AdminNotification(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            message = message,
            timestamp = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        )
        current.add(0, note)
        saveAdminNotifications(current)
    }

    fun resetToDefaults() {
        saveProfiles(defaultProfiles)
        saveRadios(defaultRadios)
        saveIptv(defaultIptv)
        saveMovies(defaultMovies)
    }
}

@JsonClass(generateAdapter = false)
data class UnifiedConfig(
    val radios: List<RadioItem> = emptyList(),
    val iptv: List<IptvItem> = emptyList(),
    val movies: List<MovieItem> = emptyList(),
    val profiles: List<UserProfile> = emptyList()
)

