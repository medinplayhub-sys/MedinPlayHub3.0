package com.example.data

import com.example.models.IptvItem
import com.example.models.MovieItem
import com.example.models.RadioItem
import com.example.models.UserProfile
import com.example.models.UserPaymentRequest
import com.example.models.PaymentMethod
import com.example.models.AppUpdateInfo
import com.example.models.ProfileHistoryData
import com.example.models.ErrorReport
import com.example.models.AdBanner
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    // Real-time Flow for Radios
    fun getRadiosFlow(): Flow<List<RadioItem>> = callbackFlow {
        val listener = db.collection("radios")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreRepository", "Error getting radios", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toRadioItemSafe()
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreRepository", "Error parsing radio document", e)
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // Real-time Flow for IPTV channels
    fun getIptvFlow(): Flow<List<IptvItem>> = callbackFlow {
        val listener = db.collection("iptv")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreRepository", "Error getting iptv", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toIptvItemSafe()
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreRepository", "Error parsing iptv document", e)
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // Real-time Flow for Movies
    fun getMoviesFlow(): Flow<List<MovieItem>> = callbackFlow {
        val listener = db.collection("movies")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreRepository", "Error getting movies", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toMovieItemSafe()
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreRepository", "Error parsing movie document", e)
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // Real-time Flow for User Profiles
    fun getProfilesFlow(): Flow<List<UserProfile>> = getUserProfilesFlow("")

    // Real-time Flow for User Profiles bound to email primary key in Users collection
    fun getUserProfilesFlow(userEmail: String): Flow<List<UserProfile>> = callbackFlow {
        val cleanEmail = userEmail.trim().lowercase()
        val query = if (cleanEmail.isNotBlank()) {
            db.collection("Users").document(cleanEmail).collection("profiles")
        } else {
            db.collection("profiles")
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirestoreRepository", "Error getting user profiles for $cleanEmail", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(UserProfile::class.java)?.let { item ->
                            val docId = if (doc.id.isNotBlank()) doc.id else item.id
                            item.copy(id = docId, email = if (item.email.isBlank()) cleanEmail else item.email)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreRepository", "Error parsing profile document", e)
                        null
                    }
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    // --- CRUD OPERATIONS ---

    // Radios
    fun saveRadio(radio: RadioItem, onComplete: ((Boolean) -> Unit)? = null) {
        val docId = if (radio.id.isNotBlank()) radio.id else "radio_${java.util.UUID.randomUUID().toString().take(6)}"
        val itemToSave = radio.copy(id = docId)
        db.collection("radios").document(docId).set(itemToSave)
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
    }

    fun deleteRadio(radioId: String, onComplete: ((Boolean) -> Unit)? = null) {
        db.collection("radios").document(radioId).delete()
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
    }

    // IPTV
    fun saveIptv(iptv: IptvItem, onComplete: ((Boolean) -> Unit)? = null) {
        val docId = if (iptv.id.isNotBlank()) iptv.id else "iptv_${java.util.UUID.randomUUID().toString().take(6)}"
        val itemToSave = iptv.copy(id = docId)
        db.collection("iptv").document(docId).set(itemToSave)
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
    }

    fun deleteIptv(iptvId: String, onComplete: ((Boolean) -> Unit)? = null) {
        db.collection("iptv").document(iptvId).delete()
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
    }

    // Movies
    fun saveMovie(movie: MovieItem, onComplete: ((Boolean) -> Unit)? = null) {
        val docId = if (movie.id.isNotBlank()) movie.id else "movie_${java.util.UUID.randomUUID().toString().take(6)}"
        val itemToSave = movie.copy(id = docId)
        db.collection("movies").document(docId).set(itemToSave)
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
    }

    fun deleteMovie(movieId: String, onComplete: ((Boolean) -> Unit)? = null) {
        db.collection("movies").document(movieId).delete()
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
    }

    // Profiles
    fun saveProfile(profile: UserProfile, onComplete: ((Boolean) -> Unit)? = null) {
        val docId = if (profile.id.isNotBlank()) profile.id else "profile_${java.util.UUID.randomUUID().toString().take(6)}"
        val itemToSave = profile.copy(id = docId)
        val cleanEmail = profile.email.trim().lowercase()
        if (cleanEmail.isNotBlank()) {
            db.collection("Users").document(cleanEmail).collection("profiles").document(docId).set(itemToSave)
        }
        db.collection("profiles").document(docId).set(itemToSave)
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
    }

    fun deleteProfile(profileId: String, userEmail: String = "", onComplete: ((Boolean) -> Unit)? = null) {
        val cleanEmail = userEmail.trim().lowercase()
        if (cleanEmail.isNotBlank()) {
            db.collection("Users").document(cleanEmail).collection("profiles").document(profileId).delete()
        }
        db.collection("profiles").document(profileId).delete()
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
    }

    fun saveAllProfiles(profiles: List<UserProfile>) {
        val batch = db.batch()
        profiles.forEach { item ->
            val docId = if (item.id.isNotBlank()) item.id else "profile_${java.util.UUID.randomUUID().toString().take(6)}"
            val docRef = db.collection("profiles").document(docId)
            batch.set(docRef, item.copy(id = docId))
        }
        batch.commit()
    }

    fun replaceAllProfiles(profiles: List<UserProfile>) {
        db.collection("profiles").get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            profiles.forEach { item ->
                val docId = if (item.id.isNotBlank()) item.id else "profile_${java.util.UUID.randomUUID().toString().take(6)}"
                val docRef = db.collection("profiles").document(docId)
                batch.set(docRef, item.copy(id = docId))
            }
            batch.commit()
        }
    }

    // Batch operations
    fun replaceAllRadios(radios: List<RadioItem>) {
        db.collection("radios").get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            val deduplicated = radios
                .distinctBy { if (it.id.isNotBlank()) it.id else "${it.name}_${it.url}" }
            deduplicated.forEach { item ->
                val docId = if (item.id.isNotBlank()) item.id else "radio_${java.util.UUID.randomUUID().toString().take(6)}"
                val docRef = db.collection("radios").document(docId)
                batch.set(docRef, item.copy(id = docId))
            }
            batch.commit()
        }
    }

    fun saveAllRadios(radios: List<RadioItem>) {
        replaceAllRadios(radios)
    }

    fun replaceAllIptv(iptvList: List<IptvItem>) {
        db.collection("iptv").get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            val deduplicated = iptvList
                .distinctBy { if (it.id.isNotBlank()) it.id else "${it.name}_${it.url}" }
            deduplicated.forEach { item ->
                val docId = if (item.id.isNotBlank()) item.id else "iptv_${java.util.UUID.randomUUID().toString().take(6)}"
                val docRef = db.collection("iptv").document(docId)
                batch.set(docRef, item.copy(id = docId))
            }
            batch.commit()
        }
    }

    fun saveAllIptv(iptvList: List<IptvItem>) {
        replaceAllIptv(iptvList)
    }

    fun replaceAllMovies(movies: List<MovieItem>) {
        db.collection("movies").get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            val deduplicated = movies
                .distinctBy { if (it.id.isNotBlank()) it.id else "${it.title}_${it.url}" }
            deduplicated.forEach { item ->
                val docId = if (item.id.isNotBlank()) item.id else "movie_${java.util.UUID.randomUUID().toString().take(6)}"
                val docRef = db.collection("movies").document(docId)
                batch.set(docRef, item.copy(id = docId))
            }
            batch.commit()
        }
    }

    fun saveAllMovies(movies: List<MovieItem>) {
        replaceAllMovies(movies)
    }

    // Real-time Flow for Banners
    fun getBannersFlow(): Flow<List<AdBanner>> = callbackFlow {
        val listener = db.collection("banners")
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreRepository", "Error getting banners", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: emptyMap<String, Any>()
                            @Suppress("UNCHECKED_CAST")
                            val locationsMap = (data["locations"] as? Map<String, Boolean>) ?: emptyMap()
                            val id = doc.id
                            val title = doc.getString("title") ?: doc.getString("name") ?: ""
                            val type = doc.getString("type") ?: "image"
                            val imageUrl = doc.getString("imageUrl")
                                ?: doc.getString("image_url")
                                ?: doc.getString("image")
                                ?: doc.getString("url")
                                ?: ""
                            val clickUrl = doc.getString("clickUrl")
                                ?: doc.getString("click_url")
                                ?: doc.getString("actionUrl")
                                ?: doc.getString("action_url")
                                ?: doc.getString("targetUrl")
                                ?: doc.getString("target_url")
                                ?: doc.getString("link")
                                ?: ""
                            val htmlCode = doc.getString("htmlCode")
                                ?: doc.getString("html_code")
                                ?: doc.getString("html")
                                ?: ""
                            val interval = (data["interval"] as? Number)?.toInt() ?: 6
                            val active = doc.getBoolean("active") ?: true
                            val priority = (data["priority"] as? Number)?.toInt() ?: 1

                            AdBanner(
                                id = id,
                                title = title,
                                type = type,
                                imageUrl = imageUrl,
                                clickUrl = clickUrl,
                                actionUrl = clickUrl,
                                htmlCode = htmlCode,
                                interval = interval,
                                active = active,
                                priority = priority,
                                locations = locationsMap
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreRepository", "Error parsing banner document", e)
                            null
                        }
                    }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    fun saveBanner(banner: AdBanner, onComplete: ((Boolean) -> Unit)? = null) {
        val docId = if (banner.id.isNotBlank()) banner.id else db.collection("banners").document().id
        val itemToSave = banner.copy(id = docId)
        db.collection("banners").document(docId).set(itemToSave)
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
    }

    fun deleteBanner(bannerId: String, onComplete: ((Boolean) -> Unit)? = null) {
        db.collection("banners").document(bannerId).delete()
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
    }

    // Real-time Flow for Payment Requests / App Nuevo Usuario Clients
    fun getPaymentRequestsFlow(): Flow<List<UserPaymentRequest>> = callbackFlow {
        val listenerConfig = db.collection("ConfiguracionGlobal").document("payments").collection("payment_requests")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(UserPaymentRequest::class.java)?.let { item ->
                                val docId = if (doc.id.isNotBlank()) doc.id else item.id
                                item.copy(id = docId)
                            }
                        } catch (e: Exception) { null }
                    }
                    trySend(list)
                } else {
                    // Check root collection fallback
                    db.collection("payment_requests").get().addOnSuccessListener { rootSnap ->
                        if (rootSnap != null) {
                            val list = rootSnap.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(UserPaymentRequest::class.java)?.let { item ->
                                        val docId = if (doc.id.isNotBlank()) doc.id else item.id
                                        item.copy(id = docId)
                                    }
                                } catch (e: Exception) { null }
                            }
                            trySend(list)
                        }
                    }
                }
            }
        awaitClose { listenerConfig.remove() }
    }

    fun savePaymentRequest(request: UserPaymentRequest, onComplete: ((Boolean) -> Unit)? = null) {
        val docId = if (request.id.isNotBlank()) request.id else "req_${java.util.UUID.randomUUID().toString().take(6)}"
        val itemToSave = request.copy(id = docId)
        db.collection("ConfiguracionGlobal").document("payments").collection("payment_requests").document(docId).set(itemToSave)
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
        // Keep root collection in sync
        db.collection("payment_requests").document(docId).set(itemToSave)
    }

    fun deletePaymentRequest(requestId: String, onComplete: ((Boolean) -> Unit)? = null) {
        db.collection("ConfiguracionGlobal").document("payments").collection("payment_requests").document(requestId).delete()
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
        db.collection("payment_requests").document(requestId).delete()
    }

    // Real-time Flow for Payment Methods
    fun getPaymentMethodsFlow(): Flow<List<PaymentMethod>> = callbackFlow {
        val listenerConfig = db.collection("ConfiguracionGlobal").document("payments").collection("payment_methods")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(PaymentMethod::class.java)?.let { item ->
                                val docId = if (doc.id.isNotBlank()) doc.id else item.id
                                item.copy(id = docId)
                            }
                        } catch (e: Exception) { null }
                    }
                    trySend(list)
                } else {
                    db.collection("payment_methods").get().addOnSuccessListener { rootSnap ->
                        if (rootSnap != null) {
                            val list = rootSnap.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(PaymentMethod::class.java)?.let { item ->
                                        val docId = if (doc.id.isNotBlank()) doc.id else item.id
                                        item.copy(id = docId)
                                    }
                                } catch (e: Exception) { null }
                            }
                            trySend(list)
                        }
                    }
                }
            }
        awaitClose { listenerConfig.remove() }
    }

    fun savePaymentMethod(method: PaymentMethod, onComplete: ((Boolean) -> Unit)? = null) {
        val docId = if (method.id.isNotBlank()) method.id else "pm_${java.util.UUID.randomUUID().toString().take(6)}"
        val itemToSave = method.copy(id = docId)
        db.collection("ConfiguracionGlobal").document("payments").collection("payment_methods").document(docId).set(itemToSave)
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
        db.collection("payment_methods").document(docId).set(itemToSave)
    }

    fun deletePaymentMethod(methodId: String, onComplete: ((Boolean) -> Unit)? = null) {
        db.collection("ConfiguracionGlobal").document("payments").collection("payment_methods").document(methodId).delete()
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { onComplete?.invoke(false) }
        db.collection("payment_methods").document(methodId).delete()
    }

    // Real-time Flow for Subscription License Price
    fun getSubscriptionPriceFlow(): Flow<String> = callbackFlow {
        val listener = db.collection("ConfiguracionGlobal").document("pricing")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val price = snapshot.getString("subscription_price") ?: "10.00$"
                    trySend(price)
                } else {
                    db.collection("app_config").document("pricing").get().addOnSuccessListener { legSnap ->
                        val price = legSnap.getString("subscription_price") ?: "10.00$"
                        trySend(price)
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    fun saveSubscriptionPrice(price: String) {
        val data = mapOf("subscription_price" to price)
        db.collection("ConfiguracionGlobal").document("pricing").set(data)
        db.collection("app_config").document("pricing").set(data)
    }

    // Real-time Flow for Disable Share Dialog setting
    fun getDisableShareDialogFlow(): Flow<Boolean> = callbackFlow {
        val listener = db.collection("ConfiguracionGlobal").document("settings")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val disabled = snapshot.getBoolean("disable_share_dialog") ?: false
                    trySend(disabled)
                } else {
                    db.collection("app_config").document("settings").get().addOnSuccessListener { legSnap ->
                        val disabled = legSnap.getBoolean("disable_share_dialog") ?: false
                        trySend(disabled)
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    fun saveDisableShareDialog(disabled: Boolean) {
        val data = mapOf("disable_share_dialog" to disabled)
        db.collection("ConfiguracionGlobal").document("settings").set(data)
        db.collection("app_config").document("settings").set(data)
    }

    // Real-time Flow for App Update Info (Checking both app_updates collection and ConfiguracionGlobal -> app_version)
    fun getAppUpdateFlow(): Flow<AppUpdateInfo?> = callbackFlow {
        var updateFromCollection: AppUpdateInfo? = null
        var updateFromConfig: AppUpdateInfo? = null

        fun emitLatest() {
            val candidate1 = updateFromCollection
            val candidate2 = updateFromConfig
            val chosen = if (candidate1 != null && candidate2 != null) {
                if (candidate1.versionCode >= candidate2.versionCode) candidate1 else candidate2
            } else {
                candidate1 ?: candidate2
            }
            trySend(chosen)
        }

        val appUpdatesListener = db.collection("app_updates")
            .addSnapshotListener { appUpdatesSnap, _ ->
                if (appUpdatesSnap != null && !appUpdatesSnap.isEmpty) {
                    val latestDoc = appUpdatesSnap.documents.maxByOrNull { doc ->
                        doc.getLong("versionCode") ?: doc.getLong("version_code") ?: 0L
                    }
                    if (latestDoc != null) {
                        try {
                            val vCode = latestDoc.getLong("versionCode") ?: latestDoc.getLong("version_code") ?: 1L
                            val vName = latestDoc.getString("versionName") ?: latestDoc.getString("version_name") ?: "1.0.0"
                            val apk = latestDoc.getString("apkUrl") ?: latestDoc.getString("downloadUrl") ?: latestDoc.getString("url") ?: ""
                            val notes = latestDoc.getString("releaseNotes") ?: latestDoc.getString("notes") ?: latestDoc.getString("description") ?: ""
                            val force = latestDoc.getBoolean("isForceUpdate") ?: latestDoc.getBoolean("mandatory") ?: latestDoc.getBoolean("force") ?: false
                            val isMand = latestDoc.getBoolean("mandatory") ?: force
                            val linkType = latestDoc.getString("linkType") ?: latestDoc.getString("link_type") ?: "direct"
                            val showBell = latestDoc.getBoolean("showOnBell") ?: true
                            val updateTitle = latestDoc.getString("title") ?: "Nueva Actualización Disponible"
                            val updateDesc = latestDoc.getString("description") ?: notes

                            updateFromCollection = AppUpdateInfo(
                                versionCode = vCode,
                                versionName = vName,
                                apkUrl = apk,
                                releaseNotes = notes,
                                isForceUpdate = force,
                                mandatory = isMand,
                                linkType = linkType,
                                showOnBell = showBell,
                                title = updateTitle,
                                description = updateDesc
                            )
                        } catch (e: Exception) { }
                    }
                } else {
                    updateFromCollection = null
                }
                emitLatest()
            }

        val configListener = db.collection("ConfiguracionGlobal").document("app_version")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    try {
                        updateFromConfig = snapshot.toObject(AppUpdateInfo::class.java)
                    } catch (e: Exception) {
                        updateFromConfig = null
                    }
                } else {
                    updateFromConfig = null
                }
                emitLatest()
            }

        awaitClose {
            appUpdatesListener.remove()
            configListener.remove()
        }
    }

    fun saveAppUpdateInfo(info: AppUpdateInfo) {
        // Save to mandatory ConfiguracionGlobal -> app_version & app_updates collection
        db.collection("ConfiguracionGlobal").document("app_version").set(info)
        db.collection("app_config").document("update").set(info)
        val docId = "v_${info.versionCode}"
        db.collection("app_updates").document(docId).set(info)
    }

    // Real-time Flow for Profile History Sync (Strictly bound to Users/{userEmail}/profiles/{profileId})
    fun getUserProfileHistoryFlow(userEmail: String, profileId: String): Flow<ProfileHistoryData?> = callbackFlow {
        if (profileId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val cleanEmail = userEmail.trim().lowercase()
        val docRef = if (cleanEmail.isNotBlank()) {
            db.collection("Users").document(cleanEmail).collection("profiles").document(profileId).collection("history").document("data")
        } else {
            db.collection("profile_history").document(profileId)
        }

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null && snapshot.exists()) {
                try {
                    val history = snapshot.toObject(ProfileHistoryData::class.java)
                    trySend(history)
                } catch (e: Exception) {
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    fun getProfileHistoryFlow(profileId: String): Flow<ProfileHistoryData?> = getUserProfileHistoryFlow("", profileId)

    fun saveUserProfileHistory(userEmail: String, profileId: String, radios: List<String>, iptv: List<String>, movies: List<String>, adult: List<String>) {
        if (profileId.isBlank()) return
        val data = mapOf(
            "profileId" to profileId,
            "radioHistory" to radios,
            "iptvHistory" to iptv,
            "movieHistory" to movies,
            "adultHistory" to adult
        )
        val cleanEmail = userEmail.trim().lowercase()
        if (cleanEmail.isNotBlank()) {
            db.collection("Users").document(cleanEmail).collection("profiles").document(profileId).collection("history").document("data").set(data)
        }
        db.collection("profile_history").document(profileId).set(data)
    }

    fun saveProfileHistory(profileId: String, radios: List<String>, iptv: List<String>, movies: List<String>, adult: List<String>) {
        saveUserProfileHistory("", profileId, radios, iptv, movies, adult)
    }

    fun deleteAllContent() {
        db.collection("radios").get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit()
        }
        db.collection("iptv").get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit()
        }
        db.collection("movies").get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit()
        }
    }

    private fun parseErrorReport(doc: com.google.firebase.firestore.DocumentSnapshot): ErrorReport? {
        if (!doc.exists()) return null
        return try {
            val data = doc.data ?: return null
            val docId = doc.id
            val parsedItemId = (data["itemId"] as? String)
                ?: (data["contentId"] as? String)
                ?: (data["mediaId"] as? String)
                ?: ""
            val parsedItemName = (data["itemName"] as? String)
                ?: (data["name"] as? String)
                ?: (data["title"] as? String)
                ?: ""
            val parsed = ErrorReport(
                id = docId,
                itemType = (data["itemType"] as? String) ?: (data["type"] as? String) ?: "",
                itemId = parsedItemId,
                itemName = parsedItemName,
                issueType = (data["issueType"] as? String) ?: "",
                comments = (data["comments"] as? String) ?: "",
                userEmail = (data["userEmail"] as? String) ?: "",
                profileName = (data["profileName"] as? String) ?: "",
                timestamp = (data["timestamp"] as? String) ?: "",
                status = (data["status"] as? String) ?: "PENDIENTE",
                resolvedAt = (data["resolvedAt"] as? String) ?: ""
            )
            android.util.Log.d("ErrorReport", "📡 Parsed report from Firestore: DocID=$docId, itemId=${parsed.itemId}, itemName=${parsed.itemName}, status=${parsed.status}")
            parsed
        } catch (e: Exception) {
            android.util.Log.e("ErrorReport", "❌ Error parsing document ${doc.id}: ${e.message}")
            try {
                doc.toObject(ErrorReport::class.java)?.let { item ->
                    val docId = if (doc.id.isNotBlank()) doc.id else item.id
                    item.copy(id = docId)
                }
            } catch (ex: Exception) { null }
        }
    }

    // Real-time Flow for Error Reports
    fun getErrorReportsFlow(): Flow<List<ErrorReport>> = callbackFlow {
        var reportsConfig = emptyList<ErrorReport>()
        var reportsRoot = emptyList<ErrorReport>()

        fun emitCombined() {
            val combined = (reportsConfig + reportsRoot)
                .associateBy { if (it.id.isNotBlank()) it.id else "${it.itemId}_${it.timestamp}" }
                .values
                .toList()
            android.util.Log.d("ErrorReport", "📡 Stream de reportes actualizado: ${combined.size} reportes totales")
            trySend(combined)
        }

        val listener1 = db.collection("ConfiguracionGlobal").document("reports").collection("error_reports")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("ErrorReport", "❌ Error en listener1 de reportes: ${e.message}")
                }
                if (snapshot != null) {
                    reportsConfig = snapshot.documents.mapNotNull { doc -> parseErrorReport(doc) }
                    emitCombined()
                }
            }

        val listener2 = db.collection("error_reports")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("ErrorReport", "❌ Error en listener2 de reportes: ${e.message}")
                }
                if (snapshot != null) {
                    reportsRoot = snapshot.documents.mapNotNull { doc -> parseErrorReport(doc) }
                    emitCombined()
                }
            }

        awaitClose {
            listener1.remove()
            listener2.remove()
        }
    }

    fun saveErrorReport(report: ErrorReport, onComplete: ((Boolean) -> Unit)? = null) {
        val docId = if (report.id.isNotBlank()) report.id else "report_${java.util.UUID.randomUUID().toString().take(6)}"
        val itemToSave = report.copy(id = docId)
        val dataMap = mapOf(
            "id" to docId,
            "itemId" to itemToSave.itemId,
            "itemName" to itemToSave.itemName,
            "itemType" to itemToSave.itemType,
            "issueType" to itemToSave.issueType,
            "comments" to itemToSave.comments,
            "userEmail" to itemToSave.userEmail,
            "profileName" to itemToSave.profileName,
            "timestamp" to itemToSave.timestamp,
            "status" to itemToSave.status,
            "resolvedAt" to itemToSave.resolvedAt
        )
        android.util.Log.d("ErrorReport", "🔴 Enviando reporte: itemId=${itemToSave.itemId}, userEmail=${itemToSave.userEmail}")
        android.util.Log.d("ErrorReport", "📋 itemName: ${itemToSave.itemName}, itemType: ${itemToSave.itemType}, issueType: ${itemToSave.issueType}, comments: ${itemToSave.comments}")

        // Save directly into root collection "error_reports"
        db.collection("error_reports").document(docId).set(dataMap)
            .addOnSuccessListener {
                android.util.Log.d("ErrorReport", "✅ Reporte enviado, ID: $docId")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ErrorReport", "❌ Error al enviar: ${e.message}")
            }

        // Also save into subcollection "ConfiguracionGlobal/reports/error_reports"
        db.collection("ConfiguracionGlobal").document("reports").collection("error_reports").document(docId).set(dataMap)
            .addOnSuccessListener {
                val notif = mapOf(
                    "id" to "notif_$docId",
                    "title" to "⚠️ Reporte de Error (${itemToSave.itemType}): ${itemToSave.itemName}",
                    "message" to "Problema: ${itemToSave.issueType}. Usuario: ${itemToSave.profileName} (${itemToSave.userEmail}). Comentarios: ${itemToSave.comments}",
                    "timestamp" to itemToSave.timestamp,
                    "isRead" to false
                )
                db.collection("ConfiguracionGlobal").document("reports").collection("AdminNotifications").document("notif_$docId").set(notif)
                db.collection("AdminNotifications").document("notif_$docId").set(notif)
                android.util.Log.d("ErrorReport", "✅ REPORTE ENVIADO EXITOSAMENTE a 'ConfiguracionGlobal/reports/error_reports'")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ErrorReport", "❌ ERROR AL ENVIAR REPORTE A 'ConfiguracionGlobal/reports/error_reports': ${e.message}")
                onComplete?.invoke(false)
            }
    }

    fun updateErrorReportStatus(reportId: String, status: String, resolvedAt: String = "") {
        if (reportId.isBlank()) return
        val updates = mutableMapOf<String, Any>("status" to status)
        if (resolvedAt.isNotBlank()) {
            updates["resolvedAt"] = resolvedAt
        }
        db.collection("ConfiguracionGlobal").document("reports").collection("error_reports").document(reportId).update(updates)
        db.collection("error_reports").document(reportId).update(updates)
    }

    // Real-time Firestore User Document Management (collection "users")
    fun syncUserDocumentToFirestore(userDoc: com.example.models.UserFirestoreDocument) {
        val cleanEmail = userDoc.email.trim().lowercase()
        val uid = if (userDoc.uid.isNotBlank()) userDoc.uid else if (cleanEmail.isNotBlank()) cleanEmail else "user_${java.util.UUID.randomUUID().toString().take(6)}"

        val historyListMaps = userDoc.history.map {
            mapOf("itemId" to it.itemId, "itemType" to it.itemType, "watchedAt" to it.watchedAt)
        }
        val aiSuggestionsMaps = userDoc.aiSuggestions.map {
            mapOf("itemId" to it.itemId, "reason" to it.reason, "score" to it.score)
        }

        val userMap = mapOf(
            "uid" to uid,
            "email" to if (userDoc.email.isBlank()) cleanEmail else userDoc.email,
            "displayName" to if (userDoc.displayName.isBlank()) "Sin nombre" else userDoc.displayName,
            "photoURL" to userDoc.photoURL,
            "favorites" to userDoc.favorites,
            "history" to historyListMaps,
            "aiSuggestions" to aiSuggestionsMaps,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "lastLogin" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        android.util.Log.d("UserDoc", "👤 Creando/Actualizando documento de usuario en colección 'users/$uid'")
        db.collection("users").document(uid).set(userMap, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                android.util.Log.d("UserDoc", "✅ Documento de usuario guardado exitosamente en 'users/$uid'")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("UserDoc", "❌ Error al guardar documento de usuario 'users/$uid': ${e.message}")
            }

        if (cleanEmail.isNotBlank() && cleanEmail != uid) {
            db.collection("users").document(cleanEmail).set(userMap, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    fun deleteErrorReport(reportId: String) {
        if (reportId.isBlank()) return
        db.collection("ConfiguracionGlobal").document("reports").collection("error_reports").document(reportId).delete()
        db.collection("error_reports").document(reportId).delete()
    }

    // OTP Admin Authentication methods
    fun requestAdminOtp(email: String, context: android.content.Context? = null, onResult: (Boolean, String) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail != "medinplayhub@gmail.com") {
            onResult(false, "Acceso denegado. Solo el correo de administración tiene permisos.")
            return
        }
        val code = String.format("%06d", (100000..999999).random())
        val now = System.currentTimeMillis()
        val expiresAt = now + 60000L // 1 minute (60 seconds)
        val otpData = mapOf(
            "email" to cleanEmail,
            "otp" to code,
            "createdAt" to now,
            "expiresAt" to expiresAt,
            "used" to false
        )

        db.collection("ConfiguracionGlobal").document("admin_otp").set(otpData)
            .addOnSuccessListener {
                if (context != null) {
                    try {
                        val channelId = "admin_otp_channel"
                        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            val channel = android.app.NotificationChannel(
                                channelId,
                                "Código OTP Administrador",
                                android.app.NotificationManager.IMPORTANCE_HIGH
                            ).apply {
                                description = "Notificaciones nativas para acceso de administración"
                                enableVibration(true)
                            }
                            notificationManager.createNotificationChannel(channel)
                        }

                        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle("🔑 Código OTP Acceso Admin: $code")
                            .setContentText("Tu código de verificación OTP es $code. Vence en 60 segundos.")
                            .setStyle(
                                androidx.core.app.NotificationCompat.BigTextStyle()
                                    .bigText("Notificación Nativa MedinPlay:\nTu código OTP para ingresar al Panel Administrativo es: $code\n\nEste código vence en 60 segundos.")
                            )
                            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                            .setAutoCancel(true)
                            .build()

                        notificationManager.notify(8888, notification)
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreRepository", "Notification error: ${e.message}")
                    }
                }

                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    // Send backup email in background
                    com.example.service.EmailSender.sendEmail(
                        recipientEmail = cleanEmail,
                        subject = "🔑 Notificación Nativa / Código OTP Admin - MedinPlay",
                        bodyText = "Hola Admin,\n\nTu código OTP de acceso es: $code\n\nNotificado nativamente en tu dispositivo."
                    )
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(true, "Código OTP ($code) enviado como notificación nativa al dispositivo. Válido por 60s.")
                    }
                }
            }
            .addOnFailureListener { e ->
                onResult(false, "Error al solicitar OTP: ${e.message}")
            }
    }

    fun verifyAdminOtp(email: String, otpInput: String, onResult: (Boolean, String) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        val cleanOtp = otpInput.trim()
        if (cleanEmail != "medinplayhub@gmail.com") {
            onResult(false, "Acceso denegado. Correo no autorizado.")
            return
        }
        if (cleanOtp.length != 6) {
            onResult(false, "El código OTP debe tener 6 dígitos.")
            return
        }

        db.collection("ConfiguracionGlobal").document("admin_otp").get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val storedOtp = doc.getString("otp") ?: ""
                    val expiresAt = doc.getLong("expiresAt") ?: 0L
                    val used = doc.getBoolean("used") ?: false
                    val now = System.currentTimeMillis()

                    if (used) {
                        onResult(false, "Este código OTP ya ha sido utilizado. Solicita un nuevo código.")
                        return@addOnSuccessListener
                    }
                    if (now > expiresAt) {
                        onResult(false, "El código OTP ha expirado (máximo 60 segundos). Solicita un nuevo código.")
                        return@addOnSuccessListener
                    }
                    if (storedOtp == cleanOtp) {
                        db.collection("ConfiguracionGlobal").document("admin_otp").update("used", true)
                        onResult(true, "Acceso concedido. Bienvenido al Panel Administrativo.")
                    } else {
                        onResult(false, "Código OTP incorrecto. Por favor verifica el número enviado a tu correo.")
                    }
                } else {
                    onResult(false, "No hay ningún código OTP activo. Presiona 'Solicitar Código' primero.")
                }
            }
            .addOnFailureListener { e ->
                onResult(false, "Error al verificar OTP: ${e.message}")
            }
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toIptvItemSafe(): IptvItem {
    try {
        toObject(IptvItem::class.java)?.let { item ->
            val docId = if (id.isNotBlank()) id else item.id
            return item.copy(id = docId)
        }
    } catch (e: Exception) {
        // Fallback for mismatched fields (e.g. category as ArrayList in Firestore)
    }

    val rawCategory = get("category")
    val rawCategories = get("categories")

    var categoryStr = ""
    val categoriesList = mutableListOf<String>()

    when (rawCategory) {
        is String -> categoryStr = rawCategory
        is List<*> -> {
            val list = rawCategory.mapNotNull { it?.toString() }
            categoriesList.addAll(list)
            categoryStr = list.joinToString(", ")
        }
    }

    when (rawCategories) {
        is List<*> -> {
            val list = rawCategories.mapNotNull { it?.toString() }
            categoriesList.addAll(list)
        }
        is String -> if (rawCategories.isNotBlank()) categoriesList.add(rawCategories)
    }

    val name = getString("name") ?: ""
    val url = getString("url") ?: ""
    val logoUrl = getString("logoUrl") ?: getString("logo") ?: ""
    val country = getString("country") ?: "General"
    val isAdult = getBoolean("isAdult") ?: (get("isAdult")?.toString()?.lowercase() == "true")

    val docId = if (id.isNotBlank()) id else (getString("id") ?: "")

    return IptvItem(
        id = docId,
        name = name,
        category = categoryStr,
        categories = categoriesList.distinct(),
        url = url,
        logoUrl = logoUrl,
        country = country,
        isAdult = isAdult
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toRadioItemSafe(): RadioItem {
    try {
        toObject(RadioItem::class.java)?.let { item ->
            val docId = if (id.isNotBlank()) id else item.id
            return item.copy(id = docId)
        }
    } catch (e: Exception) {
        // Fallback for mismatched fields
    }

    val rawGenre = get("genre")
    val rawGenres = get("genres")

    var genreStr = ""
    val genresList = mutableListOf<String>()

    when (rawGenre) {
        is String -> genreStr = rawGenre
        is List<*> -> {
            val list = rawGenre.mapNotNull { it?.toString() }
            genresList.addAll(list)
            genreStr = list.joinToString(", ")
        }
    }

    when (rawGenres) {
        is List<*> -> {
            val list = rawGenres.mapNotNull { it?.toString() }
            genresList.addAll(list)
        }
        is String -> if (rawGenres.isNotBlank()) genresList.add(rawGenres)
    }

    val name = getString("name") ?: ""
    val url = getString("url") ?: ""
    val imageUrl = getString("imageUrl") ?: getString("logo") ?: ""
    val description = getString("description") ?: ""

    val docId = if (id.isNotBlank()) id else (getString("id") ?: "")

    return RadioItem(
        id = docId,
        name = name,
        genre = genreStr,
        genres = genresList.distinct(),
        url = url,
        imageUrl = imageUrl,
        description = description
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toMovieItemSafe(): MovieItem {
    try {
        toObject(MovieItem::class.java)?.let { item ->
            val docId = if (id.isNotBlank()) id else item.id
            return item.copy(id = docId)
        }
    } catch (e: Exception) {
        // Fallback for mismatched fields
    }

    val rawGenre = get("genre")
    val rawGenres = get("genres")

    var genreStr = ""
    val genresList = mutableListOf<String>()

    when (rawGenre) {
        is String -> genreStr = rawGenre
        is List<*> -> {
            val list = rawGenre.mapNotNull { it?.toString() }
            genresList.addAll(list)
            genreStr = list.joinToString(", ")
        }
    }

    when (rawGenres) {
        is List<*> -> {
            val list = rawGenres.mapNotNull { it?.toString() }
            genresList.addAll(list)
        }
        is String -> if (rawGenres.isNotBlank()) genresList.add(rawGenres)
    }

    val title = getString("title") ?: getString("name") ?: ""
    val url = getString("url") ?: ""
    val posterUrl = getString("posterUrl") ?: getString("poster") ?: ""
    val description = getString("description") ?: getString("synopsis") ?: ""
    val rawYear = get("year")
    val yearInt = when (rawYear) {
        is Long -> rawYear.toInt()
        is Int -> rawYear
        is String -> rawYear.toIntOrNull() ?: 0
        else -> 0
    }
    val duration = getString("duration") ?: ""
    val isAdult = getBoolean("isAdult") ?: (get("isAdult")?.toString()?.lowercase() == "true")
    val rating = get("rating")?.toString() ?: ""

    val docId = if (id.isNotBlank()) id else (getString("id") ?: "")

    return MovieItem(
        id = docId,
        title = title,
        genre = genreStr,
        genres = genresList.distinct(),
        url = url,
        posterUrl = posterUrl,
        description = description,
        year = yearInt,
        duration = duration,
        isAdult = isAdult,
        rating = rating
    )
}

