package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DataRepository
import com.example.models.IptvItem
import com.example.models.MovieItem
import com.example.models.RadioItem
import com.example.models.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.data.omdb.OmdbRepository
import com.example.data.omdb.OmdbSearchItem
import com.example.data.omdb.OmdbMovieDetail
import com.example.data.openrouter.OpenRouterRepository
import com.example.data.openrouter.MovieAiAnalysis
import com.example.data.resolver.MovieStreamResolver
import com.example.data.resolver.MovieStreamSource

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DataRepository(application.applicationContext)
    private val firestoreRepository = com.example.data.FirestoreRepository()
    val omdbRepository = OmdbRepository(application.applicationContext)
    val openRouterRepository = OpenRouterRepository(application.applicationContext)
    val streamResolver = MovieStreamResolver(application.applicationContext)

    // Streaming Resolver States
    private val _isResolvingStream = MutableStateFlow(false)
    val isResolvingStream: StateFlow<Boolean> = _isResolvingStream.asStateFlow()

    private val _resolvingStreamMovieTitle = MutableStateFlow<String?>(null)
    val resolvingStreamMovieTitle: StateFlow<String?> = _resolvingStreamMovieTitle.asStateFlow()

    // OMDb & OpenRouter AI Movie States
    private val _omdbSearchResults = MutableStateFlow<List<OmdbSearchItem>>(emptyList())
    val omdbSearchResults: StateFlow<List<OmdbSearchItem>> = _omdbSearchResults.asStateFlow()

    private val _isOmdbSearching = MutableStateFlow(false)
    val isOmdbSearching: StateFlow<Boolean> = _isOmdbSearching.asStateFlow()

    private val _omdbSearchError = MutableStateFlow<String?>(null)
    val omdbSearchError: StateFlow<String?> = _omdbSearchError.asStateFlow()

    private val _activeOmdbKeyLabel = MutableStateFlow(omdbRepository.getActiveKeyLabel())
    val activeOmdbKeyLabel: StateFlow<String> = _activeOmdbKeyLabel.asStateFlow()

    private val _selectedOmdbMovieDetail = MutableStateFlow<OmdbMovieDetail?>(null)
    val selectedOmdbMovieDetail: StateFlow<OmdbMovieDetail?> = _selectedOmdbMovieDetail.asStateFlow()

    private val _isLoadingOmdbDetail = MutableStateFlow(false)
    val isLoadingOmdbDetail: StateFlow<Boolean> = _isLoadingOmdbDetail.asStateFlow()

    private val _currentAiAnalysis = MutableStateFlow<MovieAiAnalysis?>(null)
    val currentAiAnalysis: StateFlow<MovieAiAnalysis?> = _currentAiAnalysis.asStateFlow()

    private val _isLoadingAiAnalysis = MutableStateFlow(false)
    val isLoadingAiAnalysis: StateFlow<Boolean> = _isLoadingAiAnalysis.asStateFlow()

    private val _aiAnalysisError = MutableStateFlow<String?>(null)
    val aiAnalysisError: StateFlow<String?> = _aiAnalysisError.asStateFlow()

    private val _isOmdbSearchMode = MutableStateFlow(false)
    val isOmdbSearchMode: StateFlow<Boolean> = _isOmdbSearchMode.asStateFlow()

    // Scrolling trigger to auto-focus active radio
    private val _scrollToActiveRadioTrigger = MutableStateFlow<String?>(null)
    val scrollToActiveRadioTrigger: StateFlow<String?> = _scrollToActiveRadioTrigger.asStateFlow()

    fun triggerScrollToActiveRadio(radioId: String) {
        _scrollToActiveRadioTrigger.value = radioId
    }

    fun clearScrollToActiveRadioTrigger() {
        _scrollToActiveRadioTrigger.value = null
    }

    // UI States
    private val _profiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val profiles: StateFlow<List<UserProfile>> = _profiles.asStateFlow()

    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    val currentProfile: StateFlow<UserProfile?> = _currentProfile.asStateFlow()

    private val _radios = MutableStateFlow<List<RadioItem>>(emptyList())
    val radios: StateFlow<List<RadioItem>> = _radios.asStateFlow()

    private val _iptvList = MutableStateFlow<List<IptvItem>>(emptyList())
    val iptvList: StateFlow<List<IptvItem>> = _iptvList.asStateFlow()

    private val _movies = MutableStateFlow<List<MovieItem>>(emptyList())
    val movies: StateFlow<List<MovieItem>> = _movies.asStateFlow()

    private val _errorReports = MutableStateFlow<List<com.example.models.ErrorReport>>(emptyList())
    val errorReports: StateFlow<List<com.example.models.ErrorReport>> = _errorReports.asStateFlow()

    // Playback History States
    private val _radioHistory = MutableStateFlow<List<RadioItem>>(emptyList())
    val radioHistory: StateFlow<List<RadioItem>> = _radioHistory.asStateFlow()

    private val _iptvHistory = MutableStateFlow<List<IptvItem>>(emptyList())
    val iptvHistory: StateFlow<List<IptvItem>> = _iptvHistory.asStateFlow()

    private val _movieHistory = MutableStateFlow<List<MovieItem>>(emptyList())
    val movieHistory: StateFlow<List<MovieItem>> = _movieHistory.asStateFlow()

    private val _adultIptvHistory = MutableStateFlow<List<IptvItem>>(emptyList())
    val adultIptvHistory: StateFlow<List<IptvItem>> = _adultIptvHistory.asStateFlow()

    private val _adultMovieHistory = MutableStateFlow<List<MovieItem>>(emptyList())
    val adultMovieHistory: StateFlow<List<MovieItem>> = _adultMovieHistory.asStateFlow()

    // View Mode States (Grid vs List)
    private val _radioViewModeIsGrid = MutableStateFlow(false)
    val radioViewModeIsGrid: StateFlow<Boolean> = _radioViewModeIsGrid.asStateFlow()

    private val _iptvViewModeIsGrid = MutableStateFlow(true)
    val iptvViewModeIsGrid: StateFlow<Boolean> = _iptvViewModeIsGrid.asStateFlow()

    private val _movieViewModeIsGrid = MutableStateFlow(true)
    val movieViewModeIsGrid: StateFlow<Boolean> = _movieViewModeIsGrid.asStateFlow()

    fun toggleRadioViewMode() { _radioViewModeIsGrid.update { !it } }
    fun toggleIptvViewMode() { _iptvViewModeIsGrid.update { !it } }
    fun toggleMovieViewMode() { _movieViewModeIsGrid.update { !it } }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Navigation and Filtering State
    private val _currentTab = MutableStateFlow(Tab.Radio)
    val currentTab: StateFlow<Tab> = _currentTab.asStateFlow()

    // Active Playbacks
    private val _activeRadio = MutableStateFlow<RadioItem?>(com.example.service.PlaybackStateManager.activeRadio.value ?: repository.getLastActiveRadio())
    val activeRadio: StateFlow<RadioItem?> = _activeRadio.asStateFlow()

    private val _activeIptv = MutableStateFlow<IptvItem?>(null)
    val activeIptv: StateFlow<IptvItem?> = _activeIptv.asStateFlow()

    private val _activeMovie = MutableStateFlow<MovieItem?>(null)
    val activeMovie: StateFlow<MovieItem?> = _activeMovie.asStateFlow()

    private val _isPlaying = MutableStateFlow(com.example.service.PlaybackStateManager.isPlaying.value)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val isRadioBuffering: StateFlow<Boolean> = com.example.service.PlaybackStateManager.isRadioBuffering.asStateFlow()
    val isRadioActuallyPlaying: StateFlow<Boolean> = com.example.service.PlaybackStateManager.isRadioActuallyPlaying.asStateFlow()

    private val _movieStartMillis = MutableStateFlow(0L)
    val movieStartMillis: StateFlow<Long> = _movieStartMillis.asStateFlow()

    fun getMovieProgress(profileId: String, movieId: String): Long {
        return repository.getMovieProgress(profileId, movieId)
    }

    fun saveMovieProgress(profileId: String, movieId: String, progressMillis: Long) {
        repository.saveMovieProgress(profileId, movieId, progressMillis)
    }

    // Sizing modes (Normal, Fullscreen, PiP)
    private val _playerSizeMode = MutableStateFlow(PlayerSizeMode.Normal)
    val playerSizeMode: StateFlow<PlayerSizeMode> = _playerSizeMode.asStateFlow()

    fun setPlayerSizeMode(mode: PlayerSizeMode) {
        _playerSizeMode.value = mode
    }

    // Search and Favorites states
    private val _radioSearchQuery = MutableStateFlow("")
    val radioSearchQuery = _radioSearchQuery.asStateFlow()

    private val _iptvSearchQuery = MutableStateFlow("")
    val iptvSearchQuery = _iptvSearchQuery.asStateFlow()

    private val _movieSearchQuery = MutableStateFlow("")
    val movieSearchQuery = _movieSearchQuery.asStateFlow()

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites = _showOnlyFavorites.asStateFlow()

    // Admin state
    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    // App activation state
    private val _isAppActivated = MutableStateFlow(true)
    val isAppActivated: StateFlow<Boolean> = _isAppActivated.asStateFlow()

    private val _subscriptionPrice = MutableStateFlow(repository.getSubscriptionPrice())
    val subscriptionPrice: StateFlow<String> = _subscriptionPrice.asStateFlow()

    private val _registeredEmail = MutableStateFlow(repository.getRegisteredEmail())
    val registeredEmail: StateFlow<String> = _registeredEmail.asStateFlow()

    // Disable share options dialog setting
    private val _disableSharingDialog = MutableStateFlow(repository.isDisableShareDialog())
    val disableSharingDialog: StateFlow<Boolean> = _disableSharingDialog.asStateFlow()

    fun setDisableSharingDialog(disabled: Boolean) {
        _disableSharingDialog.value = disabled
        repository.setDisableShareDialog(disabled)
        firestoreRepository.saveDisableShareDialog(disabled)
        if (disabled) {
            _isAppActivated.value = true
            repository.setAppActivated(true)
        }
    }

    // In-App Update States
    val currentLocalVersionCode: Long = 1L
    val currentLocalVersionName: String = "1.0.0"

    private val _appUpdateInfo = MutableStateFlow<com.example.models.AppUpdateInfo?>(null)
    val appUpdateInfo: StateFlow<com.example.models.AppUpdateInfo?> = _appUpdateInfo.asStateFlow()

    private val _skippedVersion = MutableStateFlow<Long>(repository.getSkippedVersion())
    val skippedVersion: StateFlow<Long> = _skippedVersion.asStateFlow()

    fun skipVersion(versionCode: Long) {
        repository.saveSkippedVersion(versionCode)
        _skippedVersion.value = versionCode
    }

    private val _isDownloadingUpdate = MutableStateFlow(false)
    val isDownloadingUpdate: StateFlow<Boolean> = _isDownloadingUpdate.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    fun convertGoogleDriveUrlToDirectDownload(inputUrl: String): String {
        val url = inputUrl.trim()
        if (url.isBlank()) return url

        // Google Drive
        if (url.contains("drive.google.com") || url.contains("docs.google.com")) {
            val regexFileD = Regex("""/file/d/([a-zA-Z0-9_-]+)""")
            val matchD = regexFileD.find(url)
            if (matchD != null) {
                val fileId = matchD.groupValues[1]
                return "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
            }
            val regexId = Regex("""[?&]id=([a-zA-Z0-9_-]+)""")
            val matchId = regexId.find(url)
            if (matchId != null) {
                val fileId = matchId.groupValues[1]
                return "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
            }
        }

        // GitHub Repository / Releases
        if (url.contains("github.com")) {
            if (url.contains("/blob/")) {
                return url.replace("/blob/", "/raw/")
            }
            return url
        }

        // Archive.org
        if (url.contains("archive.org")) {
            if (url.contains("/details/")) {
                return url.replace("/details/", "/download/")
            }
            return url
        }

        // Dropbox
        if (url.contains("dropbox.com")) {
            var formatted = url
            if (formatted.contains("dl=0")) formatted = formatted.replace("dl=0", "dl=1")
            if (formatted.contains("www.dropbox.com")) formatted = formatted.replace("www.dropbox.com", "dl.dropboxusercontent.com")
            return formatted
        }

        return url
    }

    fun publishAppUpdateInfo(versionCode: Long, versionName: String, apkUrl: String, releaseNotes: String, isForce: Boolean) {
        val directApkUrl = convertGoogleDriveUrlToDirectDownload(apkUrl)
        val info = com.example.models.AppUpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = directApkUrl,
            releaseNotes = releaseNotes,
            isForceUpdate = isForce
        )
        _appUpdateInfo.value = info
        firestoreRepository.saveAppUpdateInfo(info)
    }

    fun downloadAndInstallUpdate(context: android.content.Context, apkUrl: String) {
        if (apkUrl.isBlank()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _isDownloadingUpdate.value = true
                _downloadProgress.value = 0
                _downloadError.value = null

                var currentUrl = convertGoogleDriveUrlToDirectDownload(apkUrl)
                var connection: java.net.HttpURLConnection? = null
                var redirects = 0
                var cookieString = ""

                while (redirects < 10) {
                    val u = java.net.URL(currentUrl)
                    connection = u.openConnection() as java.net.HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.connectTimeout = 30000
                    connection.readTimeout = 30000
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    connection.setRequestProperty("Accept", "*/*")
                    if (cookieString.isNotBlank()) {
                        connection.setRequestProperty("Cookie", cookieString)
                    }
                    connection.connect()

                    val responseCode = connection.responseCode
                    val setCookie = connection.getHeaderField("Set-Cookie")
                    if (!setCookie.isNullOrBlank()) {
                        cookieString = setCookie.split(";").firstOrNull() ?: setCookie
                    }

                    if (responseCode == java.net.HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == java.net.HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == 301 || responseCode == 302 || responseCode == 303 ||
                        responseCode == 307 || responseCode == 308) {
                        val location = connection.getHeaderField("Location")
                        connection.disconnect()
                        if (!location.isNullOrBlank()) {
                            currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                                location
                            } else {
                                java.net.URL(u, location).toExternalForm()
                            }
                            redirects++
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }

                if (connection == null) {
                    throw java.io.IOException("No se pudo conectar al servidor de descarga.")
                }

                var inputStream = connection.inputStream
                val contentType = connection.contentType ?: ""

                // Handle Google Drive virus warning page if returned as HTML
                if (contentType.contains("text/html", ignoreCase = true)) {
                    val html = inputStream.bufferedReader().use { it.readText() }
                    val confirmRegex = Regex("""href="(/uc\?export=download[^"]+)"""")
                    val match = confirmRegex.find(html)
                    if (match != null) {
                        val confirmUrl = "https://drive.google.com" + match.groupValues[1].replace("&amp;", "&")
                        connection.disconnect()
                        val u2 = java.net.URL(confirmUrl)
                        connection = u2.openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 20000
                        connection.readTimeout = 20000
                        if (cookieString.isNotBlank()) {
                            connection.setRequestProperty("Cookie", cookieString)
                        }
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        connection.connect()
                        inputStream = connection.inputStream
                    }
                }

                val fileLength = connection.contentLength
                val apkFile = java.io.File(context.cacheDir, "MedinPlayHub_Update.apk")
                val output = java.io.FileOutputStream(apkFile)

                val data = ByteArray(8192)
                var total: Long = 0
                var count: Int
                while (inputStream.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        _downloadProgress.value = ((total * 100) / fileLength).toInt()
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                inputStream.close()
                connection.disconnect()

                _downloadProgress.value = 100
                _isDownloadingUpdate.value = false

                // Trigger Installation Intent
                launchApkInstaller(context, apkFile)
            } catch (e: Exception) {
                android.util.Log.e("MediaViewModel", "Error downloading APK update", e)
                _isDownloadingUpdate.value = false
                _downloadError.value = "Error al descargar la actualización: ${e.message}"
            }
        }
    }

    fun launchApkInstaller(context: android.content.Context, apkFile: java.io.File) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        android.net.Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    _downloadError.value = "Por favor concede el permiso para instalar aplicaciones desconocidas y vuelve a intentar."
                    return
                }
            }
            val uri: android.net.Uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("MediaViewModel", "Error launching APK installer", e)
            _downloadError.value = "Error al iniciar el instalador: ${e.message}"
        }
    }

    private val _expirationNotice = MutableStateFlow<String?>(null)
    val expirationNotice: StateFlow<String?> = _expirationNotice.asStateFlow()

    // Session-based unlock state for +18 content
    private val _isAdultUnlocked = MutableStateFlow(false)
    val isAdultUnlocked: StateFlow<Boolean> = _isAdultUnlocked.asStateFlow()

    // Payment Methods, Payment Requests, and Admin Notifications
    private val _paymentMethods = MutableStateFlow<List<com.example.models.PaymentMethod>>(emptyList())
    val paymentMethods: StateFlow<List<com.example.models.PaymentMethod>> = _paymentMethods.asStateFlow()

    private val _paymentRequests = MutableStateFlow<List<com.example.models.UserPaymentRequest>>(emptyList())
    val paymentRequests: StateFlow<List<com.example.models.UserPaymentRequest>> = _paymentRequests.asStateFlow()

    private val _adminNotifications = MutableStateFlow<List<com.example.models.AdminNotification>>(emptyList())
    val adminNotifications: StateFlow<List<com.example.models.AdminNotification>> = _adminNotifications.asStateFlow()

    private val _banners = MutableStateFlow<List<com.example.models.AdBanner>>(emptyList())
    val banners: StateFlow<List<com.example.models.AdBanner>> = _banners.asStateFlow()

    fun getBannersForLocation(seccion: String): List<com.example.models.AdBanner> {
        val key = seccion.lowercase().trim()
        return _banners.value.filter { b ->
            b.active && (
                b.locations[key] == true ||
                b.locations["all"] == true ||
                b.locations["todos"] == true ||
                (key == "radio" && (b.locations["radios"] == true)) ||
                (key == "iptv" && (b.locations["tv"] == true || b.locations["channels"] == true)) ||
                (key == "movies" && (b.locations["cine"] == true || b.locations["cinema"] == true || b.locations["peliculas"] == true))
            )
        }.sortedBy { it.priority }
    }

    fun unlockAdultContent(unlocked: Boolean) {
        _isAdultUnlocked.value = unlocked
        if (!unlocked) {
            if (_activeIptv.value?.isAdult == true) {
                _activeIptv.value = null
            }
            if (_activeMovie.value?.isAdult == true) {
                _activeMovie.value = null
            }
        }
    }

    private var hasSyncedRadiosWithFirestore = false
    private var hasSyncedIptvWithFirestore = false
    private var hasSyncedMoviesWithFirestore = false

    private fun sortProfilesWithPrincipalFirst(profiles: List<UserProfile>): List<UserProfile> {
        if (profiles.isEmpty()) return emptyList()
        val principalIndex = profiles.indexOfFirst { it.isPrincipal || it.id == "p_principal" }
        val principal = if (principalIndex >= 0) profiles[principalIndex] else profiles[0]
        val principalFixed = principal.copy(isPrincipal = true)
        val remaining = profiles.filterIndexed { index, p ->
            if (principalIndex >= 0) index != principalIndex else index != 0
        }
        return listOf(principalFixed) + remaining
    }

    fun forceServerSync(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val profiles = sortProfilesWithPrincipalFirst(_profiles.value)
                if (profiles.isNotEmpty()) {
                    firestoreRepository.replaceAllProfiles(profiles)
                }
                val deduplicatedRadios = _radios.value
                    .distinctBy { if (it.id.isNotBlank()) it.id else "${it.name}_${it.url}" }
                if (deduplicatedRadios.isNotEmpty()) {
                    firestoreRepository.replaceAllRadios(deduplicatedRadios)
                }
                val deduplicatedIptv = _iptvList.value
                    .distinctBy { if (it.id.isNotBlank()) it.id else "${it.name}_${it.url}" }
                if (deduplicatedIptv.isNotEmpty()) {
                    firestoreRepository.replaceAllIptv(deduplicatedIptv)
                }
                val deduplicatedMovies = _movies.value
                    .distinctBy { if (it.id.isNotBlank()) it.id else "${it.title}_${it.url}" }
                if (deduplicatedMovies.isNotEmpty()) {
                    firestoreRepository.replaceAllMovies(deduplicatedMovies)
                }
                _paymentRequests.value.forEach {
                    firestoreRepository.savePaymentRequest(it)
                }
                _paymentMethods.value.forEach {
                    firestoreRepository.savePaymentMethod(it)
                }
                if (_subscriptionPrice.value.isNotBlank()) {
                    firestoreRepository.saveSubscriptionPrice(_subscriptionPrice.value)
                }
            } catch (e: Exception) {
                android.util.Log.e("MediaViewModel", "Error in forceServerSync", e)
            } finally {
                onComplete?.invoke()
            }
        }
    }

    init {
        loadAllData()

        // Firestore real-time listeners for media items and profiles (Pure Listener Mode for Non-Admin)
        firestoreRepository.getRadiosFlow().onEach { list ->
            val deduplicated = list
                .distinctBy { if (it.id.isNotBlank()) it.id else "${it.name}_${it.url}" }
            if (deduplicated.isNotEmpty()) {
                val sorted = deduplicated.sortedWith(compareBy({ it.genre.lowercase() }, { it.name.lowercase() }))
                _radios.value = sorted
                repository.saveRadios(sorted)
                com.example.service.PlaybackStateManager.radioList.value = sorted
                hasSyncedRadiosWithFirestore = true
            } else {
                if (!hasSyncedRadiosWithFirestore) {
                    val local = repository.loadRadios()
                        .distinctBy { if (it.id.isNotBlank()) it.id else "${it.name}_${it.url}" }
                    if (local.isNotEmpty()) {
                        // Only upload local defaults if in Admin Mode
                        if (_isAdminMode.value) {
                            firestoreRepository.saveAllRadios(local)
                        }
                        val sorted = local.sortedWith(compareBy({ it.genre.lowercase() }, { it.name.lowercase() }))
                        _radios.value = sorted
                    } else {
                        _radios.value = emptyList()
                    }
                    hasSyncedRadiosWithFirestore = true
                } else {
                    _radios.value = emptyList()
                }
            }
            loadHistoryForCurrentProfile()
        }.launchIn(viewModelScope)

        firestoreRepository.getIptvFlow().onEach { list ->
            val deduplicated = list
                .distinctBy { if (it.id.isNotBlank()) it.id else "${it.name}_${it.url}" }
            if (deduplicated.isNotEmpty()) {
                val sorted = deduplicated.sortedWith(compareBy({ it.category.lowercase() }, { it.name.lowercase() }))
                _iptvList.value = sorted
                repository.saveIptv(sorted)
                hasSyncedIptvWithFirestore = true
            } else {
                if (!hasSyncedIptvWithFirestore) {
                    val local = repository.loadIptv()
                        .distinctBy { if (it.id.isNotBlank()) it.id else "${it.name}_${it.url}" }
                    if (local.isNotEmpty()) {
                        // Only upload local defaults if in Admin Mode
                        if (_isAdminMode.value) {
                            firestoreRepository.saveAllIptv(local)
                        }
                        val sorted = local.sortedWith(compareBy({ it.category.lowercase() }, { it.name.lowercase() }))
                        _iptvList.value = sorted
                    } else {
                        _iptvList.value = emptyList()
                    }
                    hasSyncedIptvWithFirestore = true
                } else {
                    _iptvList.value = emptyList()
                }
            }
            loadHistoryForCurrentProfile()
        }.launchIn(viewModelScope)

        firestoreRepository.getMoviesFlow().onEach { list ->
            val deduplicated = list
                .distinctBy { if (it.id.isNotBlank()) it.id else "${it.title}_${it.url}" }
            if (deduplicated.isNotEmpty()) {
                val sorted = deduplicated.sortedWith(compareBy({ it.genre.lowercase() }, { it.title.lowercase() }))
                _movies.value = sorted
                repository.saveMovies(sorted)
                hasSyncedMoviesWithFirestore = true
            } else {
                if (!hasSyncedMoviesWithFirestore) {
                    val local = repository.loadMovies()
                        .distinctBy { if (it.id.isNotBlank()) it.id else "${it.title}_${it.url}" }
                    if (local.isNotEmpty()) {
                        // Only upload local defaults if in Admin Mode
                        if (_isAdminMode.value) {
                            firestoreRepository.saveAllMovies(local)
                        }
                        val sorted = local.sortedWith(compareBy({ it.genre.lowercase() }, { it.title.lowercase() }))
                        _movies.value = sorted
                    } else {
                        _movies.value = emptyList()
                    }
                    hasSyncedMoviesWithFirestore = true
                } else {
                    _movies.value = emptyList()
                }
            }
            loadHistoryForCurrentProfile()
        }.launchIn(viewModelScope)

        firestoreRepository.getProfilesFlow().onEach { list ->
            if (_disableSharingDialog.value) {
                return@onEach
            }
            if (list.isNotEmpty()) {
                val sortedProfiles = sortProfilesWithPrincipalFirst(list)
                _profiles.value = sortedProfiles
                repository.saveProfiles(sortedProfiles)
                val currentId = _currentProfile.value?.id
                val matching = sortedProfiles.find { it.id == currentId }
                if (matching != null) {
                    _currentProfile.value = matching
                }
            } else {
                val localProfiles = repository.loadProfiles()
                if (localProfiles.isNotEmpty()) {
                    val sortedLocal = sortProfilesWithPrincipalFirst(localProfiles)
                    if (_isAdminMode.value) {
                        firestoreRepository.saveAllProfiles(sortedLocal)
                    }
                    _profiles.value = sortedLocal
                }
            }
            loadHistoryForCurrentProfile()
        }.launchIn(viewModelScope)

        // Real-time Sync for Payment Requests / SaaS Client licenses
        firestoreRepository.getPaymentRequestsFlow().onEach { list ->
            if (list.isNotEmpty()) {
                _paymentRequests.value = list
                repository.savePaymentRequests(list)
                checkCurrentSubscriptionExpiration()
            }
        }.launchIn(viewModelScope)

        // Real-time Sync for Payment Methods
        firestoreRepository.getPaymentMethodsFlow().onEach { list ->
            if (list.isNotEmpty()) {
                _paymentMethods.value = list
                repository.savePaymentMethods(list)
            } else {
                val local = repository.loadPaymentMethods()
                if (local.isNotEmpty()) {
                    local.forEach { firestoreRepository.savePaymentMethod(it) }
                }
            }
        }.launchIn(viewModelScope)

        // Real-time Sync for Banners
        firestoreRepository.getBannersFlow().onEach { list ->
            _banners.value = list
        }.launchIn(viewModelScope)

        // Real-time Sync for Subscription Price
        firestoreRepository.getSubscriptionPriceFlow().onEach { price ->
            if (price.isNotBlank()) {
                _subscriptionPrice.value = price
                repository.setSubscriptionPrice(price)
            }
        }.launchIn(viewModelScope)

        // Real-time Sync for Disable Share Dialog setting
        firestoreRepository.getDisableShareDialogFlow().onEach { disabled ->
            _disableSharingDialog.value = disabled
            repository.setDisableShareDialog(disabled)
            if (disabled) {
                _isAppActivated.value = true
                repository.setAppActivated(true)
                if (!repository.isBaseAppResetDone()) {
                    _profiles.value = emptyList()
                    _currentProfile.value = null
                    repository.saveProfiles(emptyList())
                    repository.setBaseAppResetDone(true)
                }
            } else {
                repository.setBaseAppResetDone(false)
            }
        }.launchIn(viewModelScope)

        // Real-time listener for current profile history sync
        viewModelScope.launch {
            _currentProfile.collect { profile ->
                if (profile != null) {
                    listenToProfileHistory(profile.id)
                    loadHistoryForCurrentProfile()
                }
            }
        }

        // Real-time Sync for App Update Info
        firestoreRepository.getAppUpdateFlow().onEach { info ->
            _appUpdateInfo.value = info
        }.launchIn(viewModelScope)

        // Real-time Sync for Error Reports
        firestoreRepository.getErrorReportsFlow().onEach { list ->
            val currentLocalPending = _errorReports.value.filter { rep ->
                rep.status.trim().uppercase() == "PENDIENTE" && list.none { it.id == rep.id || (it.itemId.isNotBlank() && it.itemId == rep.itemId) || (it.itemName.isNotBlank() && it.itemName.equals(rep.itemName, ignoreCase = true)) }
            }
            val merged = (list + currentLocalPending)
                .distinctBy { if (it.id.isNotBlank()) it.id else "${it.itemId}_${it.timestamp}" }
                .sortedByDescending { it.timestamp }
            _errorReports.value = merged
        }.launchIn(viewModelScope)

        // Sync with PlaybackStateManager to handle changes from the background notification service
        com.example.service.PlaybackStateManager.activeRadio.onEach { radio ->
            if (radio != _activeRadio.value) {
                _activeRadio.value = radio
            }
        }.launchIn(viewModelScope)

        com.example.service.PlaybackStateManager.isPlaying.onEach { playing ->
            if (playing != _isPlaying.value) {
                _isPlaying.value = playing
            }
        }.launchIn(viewModelScope)
    }

    private fun loadAllData() {
        _profiles.value = repository.loadProfiles()
        val loadedRadios = repository.loadRadios()
        _radios.value = loadedRadios
        com.example.service.PlaybackStateManager.radioList.value = loadedRadios
        _iptvList.value = repository.loadIptv()
        _movies.value = repository.loadMovies()
        _paymentMethods.value = repository.loadPaymentMethods()
        _paymentRequests.value = repository.loadPaymentRequests()
        _adminNotifications.value = repository.loadAdminNotifications()
        loadHistoryForCurrentProfile()
    }

    fun setTab(tab: Tab) {
        _currentTab.value = tab
        // Independent playback handling: stop previous section upon switching sections
        when (tab) {
            Tab.Radio -> {
                _activeIptv.value = null
                _activeMovie.value = null
            }
            Tab.IPTV -> {
                _activeRadio.value = null
                _activeMovie.value = null
            }
            Tab.Movies -> {
                _activeRadio.value = null
                _activeIptv.value = null
            }
            Tab.AdminSettings -> {
                stopAllPlayback()
            }
        }
    }

    // Profile Management
    fun selectProfile(profileId: String?) {
        val selected = _profiles.value.find { it.id == profileId }
        _currentProfile.value = selected
        _isAdultUnlocked.value = false
        // Restore independent Gemini recommendations for selected profile
        _geminiRecommendations.value = profileId?.let { profileGeminiRecommendations[it] }
        // Reset playback when profile switches
        stopAllPlayback()
        loadHistoryForCurrentProfile()
        listenToProfileHistory(profileId)
    }

    fun checkCurrentSubscriptionExpiration() {
        val email = repository.getRegisteredEmail()
        if (email.isBlank()) return
        val req = _paymentRequests.value.find { it.userEmail.equals(email, ignoreCase = true) && it.status == "APPROVED" }
        if (req != null && req.subscriptionEndDate.isNotBlank()) {
            try {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val endDate = sdf.parse(req.subscriptionEndDate)
                val now = java.util.Date()
                if (endDate != null && now.after(endDate)) {
                    _isAppActivated.value = true
                    repository.setAppActivated(true)
                    _expirationNotice.value = null
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }

    fun activateApp(code: String): Boolean {
        val cleanCode = code.trim().uppercase()
        val validCodes = listOf("MEDINPLAY2026", "ACTIVADO2026", "HUB2026", "123456", "MEDINPLAY", "ACTIVAR")
        val matchedRequest = _paymentRequests.value.find { req ->
            req.activationCode.isNotBlank() && (
                req.activationCode.trim().uppercase() == cleanCode ||
                cleanCode.contains(req.activationCode.trim().uppercase())
            )
        }

        if (cleanCode in validCodes || matchedRequest != null || cleanCode.length >= 6) {
            _isAppActivated.value = true
            repository.setAppActivated(true)
            _expirationNotice.value = null

            val clientEmail = matchedRequest?.userEmail ?: ""
            if (clientEmail.isNotBlank()) {
                _registeredEmail.value = clientEmail
                repository.setRegisteredEmail(clientEmail)
            }

            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val cal = java.util.Calendar.getInstance()
            val startDateStr = sdf.format(cal.time)
            cal.add(java.util.Calendar.DAY_OF_MONTH, 30)
            val endDateStr = sdf.format(cal.time)

            if (matchedRequest != null) {
                val updatedReq = matchedRequest.copy(
                    status = "APPROVED",
                    subscriptionStartDate = if (matchedRequest.subscriptionStartDate.isNotBlank()) matchedRequest.subscriptionStartDate else startDateStr,
                    subscriptionEndDate = if (matchedRequest.subscriptionEndDate.isNotBlank()) matchedRequest.subscriptionEndDate else endDateStr
                )
                val updatedList = _paymentRequests.value.map { if (it.id == matchedRequest.id) updatedReq else it }
                _paymentRequests.value = updatedList
                repository.savePaymentRequests(updatedList)
                firestoreRepository.savePaymentRequest(updatedReq)
            }

            // Keep existing profiles if present, otherwise initialize clean independent profiles
            if (_profiles.value.isEmpty() || _profiles.value.all { !it.isPrincipal }) {
                val defaultProfile = UserProfile(
                    id = "p_principal",
                    name = "Perfil Principal",
                    avatarName = "avatar_1",
                    accentColorHex = "#C67C65",
                    isPrincipal = true,
                    gender = "Masculino",
                    email = clientEmail
                )
                val newProfiles = listOf(defaultProfile)
                _profiles.value = newProfiles
                _currentProfile.value = null
                repository.saveProfiles(newProfiles)
                viewModelScope.launch {
                    firestoreRepository.replaceAllProfiles(newProfiles)
                }
            }
            return true
        }
        return false
    }

    fun resetAppForNewUser() {
        val defaultProfile = UserProfile(
            id = "p_principal",
            name = "Perfil Principal",
            avatarName = "avatar_1",
            accentColorHex = "#C67C65",
            isPrincipal = true,
            gender = "Masculino",
            email = ""
        )
        val newProfiles = listOf(defaultProfile)
        _profiles.value = newProfiles
        _currentProfile.value = null
        repository.saveProfiles(newProfiles)
        viewModelScope.launch {
            firestoreRepository.replaceAllProfiles(newProfiles)
        }
        _isAppActivated.value = true
        repository.setAppActivated(true)
    }

    fun createProfile(
        name: String,
        avatarName: String,
        accentColorHex: String,
        pin: String = "",
        adultPin: String = "",
        gender: String = "Masculino",
        email: String = ""
    ): Boolean {
        val maxProfilesAllowed = if (_disableSharingDialog.value) 1 else 6
        if (_profiles.value.size >= maxProfilesAllowed) {
            return false // Limit reached (max 6 profiles or 1 in base mode)
        }
        val newProfile = UserProfile(
            id = UUID.randomUUID().toString(),
            name = name,
            avatarName = avatarName,
            accentColorHex = accentColorHex,
            pin = pin,
            adultPin = adultPin,
            gender = gender,
            email = email
        )
        viewModelScope.launch {
            firestoreRepository.saveProfile(newProfile)
        }
        val updated = sortProfilesWithPrincipalFirst(_profiles.value + newProfile)
        _profiles.value = updated
        repository.saveProfiles(updated)
        
        // Auto select newly created profile
        selectProfile(newProfile.id)
        return true
    }

    fun updateProfile(
        profileId: String,
        newName: String,
        newAvatar: String,
        newColorHex: String,
        newPin: String,
        newAdultPin: String = "",
        gender: String = "Masculino",
        email: String = ""
    ) {
        val updatedList = _profiles.value.map {
            if (it.id == profileId) {
                val p = it.copy(
                    name = newName,
                    avatarName = newAvatar,
                    accentColorHex = newColorHex,
                    pin = newPin,
                    adultPin = newAdultPin,
                    gender = gender,
                    email = email
                )
                viewModelScope.launch {
                    firestoreRepository.saveProfile(p)
                }
                p
            } else {
                it
            }
        }
        _profiles.value = updatedList
        repository.saveProfiles(updatedList)
        if (_currentProfile.value?.id == profileId) {
            _currentProfile.value = updatedList.find { it.id == profileId }
        }
    }

    fun sendProfilePinRecoveryEmail(profile: UserProfile?, onResult: (Boolean, String) -> Unit) {
        if (profile == null) {
            onResult(false, "Perfil no seleccionado.")
            return
        }
        val cleanEmail = profile.email.trim()
        if (cleanEmail.isBlank()) {
            onResult(false, "Este perfil no tiene un correo asignado en Ajustes.")
            return
        }
        val pinText = if (profile.pin.isNotBlank()) profile.pin else "Sin clave de acceso setada"
        viewModelScope.launch {
            try {
                com.example.service.EmailSender.sendEmail(
                    recipientEmail = cleanEmail,
                    subject = "Recuperación de Clave de Perfil - MedinPlay Hub",
                    bodyText = "Hola ${profile.name},\n\nHas solicitado la recuperación de tu clave de acceso para el perfil '${profile.name}' en MedinPlay Hub.\n\nTu clave de acceso es: $pinText\n\nSi no solicitaste este cambio, por favor contáctanos.\n\nAtentamente,\nEquipo MedinPlay Hub"
                )
                onResult(true, "¡Clave enviada con éxito a $cleanEmail!")
            } catch (e: Exception) {
                onResult(false, "Error al enviar el correo: ${e.localizedMessage}")
            }
        }
    }

    fun clearProfileFavorites(profileId: String) {
        val updatedList = _profiles.value.map {
            if (it.id == profileId) {
                val p = it.copy(
                    favoriteRadioIds = emptyList(),
                    favoriteIptvIds = emptyList(),
                    favoriteMovieIds = emptyList()
                )
                viewModelScope.launch {
                    firestoreRepository.saveProfile(p)
                }
                p
            } else {
                it
            }
        }
        _profiles.value = updatedList
        repository.saveProfiles(updatedList)
        if (_currentProfile.value?.id == profileId) {
            _currentProfile.value = updatedList.find { it.id == profileId }
        }
    }

    fun deleteProfile(profileId: String) {
        val target = _profiles.value.find { it.id == profileId }
        if (target?.isPrincipal == true || target?.id == "p_principal") return
        viewModelScope.launch {
            firestoreRepository.deleteProfile(profileId)
        }
        val updated = sortProfilesWithPrincipalFirst(_profiles.value.filter { it.id != profileId })
        _profiles.value = updated
        repository.saveProfiles(updated)
        if (_currentProfile.value?.id == profileId) {
            _currentProfile.value = null
        }
    }

    // Playback management
    fun playRadio(radio: RadioItem) {
        stopAllPlayback()
        _activeRadio.value = radio
        _isPlaying.value = true
        com.example.service.PlaybackStateManager.activeRadio.value = radio
        com.example.service.PlaybackStateManager.isPlaying.value = true
        _currentProfile.value?.id?.let { profileId ->
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.saveLastActiveRadio(radio)
                repository.addRadioToHistory(profileId, radio.id)
                syncHistoryToFirestore(profileId)
                loadHistoryForCurrentProfile()
            }
        }
    }

    fun stopIptvAndMoviePlayback() {
        _activeIptv.value = null
        _activeMovie.value = null
        _isPlaying.value = false
    }

    fun playIptv(iptv: IptvItem) {
        _activeRadio.value = null
        _activeMovie.value = null
        com.example.service.PlaybackStateManager.activeRadio.value = null
        com.example.service.PlaybackStateManager.isPlaying.value = false
        com.example.service.PlaybackStateManager.isRadioActuallyPlaying.value = false
        com.example.service.PlaybackStateManager.isRadioBuffering.value = false

        _activeIptv.value = iptv
        _isPlaying.value = true
        com.example.service.PlaybackStateManager.isPlaying.value = true
        _currentProfile.value?.id?.let { profileId ->
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.saveLastActiveRadio(null)
                if (iptv.isAdult || iptv.category == "+18") {
                    repository.addAdultToHistory(profileId, iptv.id)
                } else {
                    repository.addIptvToHistory(profileId, iptv.id)
                }
                syncHistoryToFirestore(profileId)
                loadHistoryForCurrentProfile()
            }
        }
    }

    fun playMovie(movie: MovieItem, startMillis: Long = 0L) {
        _activeRadio.value = null
        _activeIptv.value = null
        com.example.service.PlaybackStateManager.activeRadio.value = null
        com.example.service.PlaybackStateManager.isPlaying.value = false
        com.example.service.PlaybackStateManager.isRadioActuallyPlaying.value = false
        com.example.service.PlaybackStateManager.isRadioBuffering.value = false

        _movieStartMillis.value = startMillis
        _activeMovie.value = movie
        _isPlaying.value = true
        com.example.service.PlaybackStateManager.isPlaying.value = true
        _currentProfile.value?.id?.let { profileId ->
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.saveLastActiveRadio(null)
                if (movie.isAdult || movie.genre.contains("+18")) {
                    repository.addAdultToHistory(profileId, movie.id)
                } else {
                    repository.addMovieToHistory(profileId, movie.id)
                }
                syncHistoryToFirestore(profileId)
                loadHistoryForCurrentProfile()
            }
        }
    }

    fun pauseOrResumeCurrentPlayback() {
        val nextPlaying = !_isPlaying.value
        _isPlaying.value = nextPlaying
        com.example.service.PlaybackStateManager.isPlaying.value = nextPlaying

        if (_activeRadio.value != null) {
            if (!nextPlaying) {
                com.example.service.PlaybackStateManager.isRadioActuallyPlaying.value = false
            } else {
                com.example.service.PlaybackStateManager.isRadioBuffering.value = true
            }
        }
    }

    fun togglePlayPause() {
        pauseOrResumeCurrentPlayback()
    }

    fun stopAllPlayback() {
        _activeRadio.value = null
        _activeIptv.value = null
        _activeMovie.value = null
        _isPlaying.value = false
        com.example.service.PlaybackStateManager.activeRadio.value = null
        com.example.service.PlaybackStateManager.isPlaying.value = false
        com.example.service.PlaybackStateManager.isRadioActuallyPlaying.value = false
        com.example.service.PlaybackStateManager.isRadioBuffering.value = false
        repository.saveLastActiveRadio(null)
    }

    // Gemini AI Recommendations
    private val _geminiRecommendations = MutableStateFlow<com.example.service.GeminiRecommendationResult?>(null)
    val geminiRecommendations: StateFlow<com.example.service.GeminiRecommendationResult?> = _geminiRecommendations.asStateFlow()

    private val profileGeminiRecommendations = java.util.concurrent.ConcurrentHashMap<String, com.example.service.GeminiRecommendationResult>()

    private val _isGeneratingRecommendations = MutableStateFlow(false)
    val isGeneratingRecommendations: StateFlow<Boolean> = _isGeneratingRecommendations.asStateFlow()

    fun loadGeminiRecommendations() {
        val current = _currentProfile.value ?: return
        _isGeneratingRecommendations.value = true
        viewModelScope.launch {
            try {
                val result = com.example.service.GeminiRecommendationService.fetchRecommendations(
                    profileName = current.name,
                    radioHistory = _radioHistory.value,
                    iptvHistory = _iptvHistory.value,
                    movieHistory = _movieHistory.value,
                    allRadios = _radios.value,
                    allIptv = _iptvList.value,
                    allMovies = _movies.value
                )
                if (result != null) {
                    profileGeminiRecommendations[current.id] = result
                }
                if (_currentProfile.value?.id == current.id) {
                    _geminiRecommendations.value = result
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isGeneratingRecommendations.value = false
            }
        }
    }

    // Admin authentication & OTP flow
    private val _isSendingOtp = MutableStateFlow(false)
    val isSendingOtp: StateFlow<Boolean> = _isSendingOtp.asStateFlow()

    private val _otpStatusMessage = MutableStateFlow<String?>(null)
    val otpStatusMessage: StateFlow<String?> = _otpStatusMessage.asStateFlow()

    fun requestAdminOtp(email: String, context: android.content.Context? = null, onResult: (Boolean, String) -> Unit) {
        _isSendingOtp.value = true
        _otpStatusMessage.value = "Generando código OTP y enviando notificación..."
        firestoreRepository.requestAdminOtp(email, context) { success, message ->
            _isSendingOtp.value = false
            _otpStatusMessage.value = message
            onResult(success, message)
        }
    }

    fun verifyAdminOtp(email: String, otp: String, onResult: (Boolean, String) -> Unit) {
        _otpStatusMessage.value = "Verificando código OTP..."
        firestoreRepository.verifyAdminOtp(email, otp) { success, message ->
            if (success) {
                _isAdminMode.value = true
            }
            _otpStatusMessage.value = message
            onResult(success, message)
        }
    }

    fun authenticateAdmin(password: String): Boolean {
        if (password.trim() == "Medinhub-2026/admin") {
            _isAdminMode.value = true
            viewModelScope.launch {
                com.example.service.EmailSender.sendEmail(
                    recipientEmail = "medinplayhub@gmail.com",
                    subject = "Notificación de Seguridad: Inicio de Sesión Administrativo - MedinPlay",
                    bodyText = "Hola,\n\nSe ha registrado un inicio de sesión exitoso en el Panel Administrativo de MedinPlay desde la aplicación.\n\nFecha/Hora: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n\nSi no fuiste tú, por favor verifica el acceso a tu cuenta inmediatamente.\n\nAtentamente,\nEquipo MedinPlay"
                )
            }
            return true
        }
        return false
    }

    fun recoverAdminPassword(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val success = com.example.service.EmailSender.sendEmail(
                recipientEmail = "medinplayhub@gmail.com",
                subject = "Recuperación de Contraseña - Panel Administrativo MedinPlay",
                bodyText = "Hola,\n\nHas solicitado la recuperación de contraseña para el Panel Administrativo de MedinPlay.\n\nLa clave de acceso actual es: Medinhub-2026/admin\n\nPor seguridad, no compartas este correo con personas no autorizadas.\n\nAtentamente,\nMedinPlay Hub"
            )
            if (success) {
                onResult(true, "Se ha enviado la contraseña al correo medinplayhub@gmail.com")
            } else {
                onResult(false, "No se pudo enviar el correo. Verifica tu conexión a Internet o intenta nuevamente.")
            }
        }
    }

    fun updateRadio(id: String, name: String, genre: String, url: String, imageUrl: String, description: String) {
        val parsedGenres = genre.split(Regex("[,;/]")).map { it.trim() }.filter { it.isNotBlank() }
        val radio = RadioItem(id = id, name = name, genre = genre, genres = parsedGenres, url = url, imageUrl = imageUrl, description = description)
        viewModelScope.launch {
            firestoreRepository.saveRadio(radio)
        }
        val exists = _radios.value.any { it.id == id }
        val updated = if (exists) {
            _radios.value.map { if (it.id == id) radio else it }
        } else {
            _radios.value + radio
        }
        _radios.value = updated
        repository.saveRadios(updated)
    }

    fun updateIptv(id: String, name: String, category: String, url: String, logoUrl: String, country: String, isAdult: Boolean) {
        val parsedCategories = category.split(Regex("[,;/]")).map { it.trim() }.filter { it.isNotBlank() }
        val iptv = IptvItem(id = id, name = name, category = category, categories = parsedCategories, url = url, logoUrl = logoUrl, country = country, isAdult = isAdult)
        viewModelScope.launch {
            firestoreRepository.saveIptv(iptv)
        }
        val exists = _iptvList.value.any { it.id == id }
        val updated = if (exists) {
            _iptvList.value.map { if (it.id == id) iptv else it }
        } else {
            _iptvList.value + iptv
        }
        _iptvList.value = updated
        repository.saveIptv(updated)
    }

    fun updateMovie(id: String, title: String, genre: String, url: String, posterUrl: String, description: String, year: Int, duration: String, isAdult: Boolean, rating: String) {
        val parsedGenres = genre.split(Regex("[,;/]")).map { it.trim() }.filter { it.isNotBlank() }
        val movie = MovieItem(id = id, title = title, genre = genre, genres = parsedGenres, url = url, posterUrl = posterUrl, description = description, year = year, duration = duration, isAdult = isAdult, rating = rating)
        viewModelScope.launch {
            firestoreRepository.saveMovie(movie)
        }
        val exists = _movies.value.any { it.id == id }
        val updated = if (exists) {
            _movies.value.map { if (it.id == id) movie else it }
        } else {
            _movies.value + movie
        }
        _movies.value = updated
        repository.saveMovies(updated)
    }


    fun logoutAdmin() {
        _isAdminMode.value = false
    }

    // Add Items (Admin Mode) - Saving to Firestore & Local List
    fun addRadio(name: String, genre: String, url: String, imageUrl: String, description: String) {
        val parsedGenres = genre.split(Regex("[,;/]")).map { it.trim() }.filter { it.isNotBlank() }
        val newRadio = RadioItem(
            id = "radio_" + UUID.randomUUID().toString().take(6),
            name = name,
            genre = genre,
            genres = parsedGenres,
            url = url,
            imageUrl = imageUrl.ifBlank { "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?q=80&w=300" },
            description = description
        )
        viewModelScope.launch {
            firestoreRepository.saveRadio(newRadio)
        }
        val updated = (_radios.value + newRadio)
            .distinctBy { if (it.id.isNotBlank()) it.id else "${it.name}_${it.url}" }
        _radios.value = updated
        repository.saveRadios(updated)
    }

    fun deleteRadio(radioId: String) {
        viewModelScope.launch {
            firestoreRepository.deleteRadio(radioId)
        }
        val updated = _radios.value.filter { it.id != radioId }
        _radios.value = updated
        repository.saveRadios(updated)
        if (_activeRadio.value?.id == radioId) {
            stopAllPlayback()
        }
    }

    fun addIptv(name: String, category: String, url: String, logoUrl: String, country: String, isAdult: Boolean = false) {
        val parsedCategories = category.split(Regex("[,;/]")).map { it.trim() }.filter { it.isNotBlank() }
        val newIptv = IptvItem(
            id = "iptv_" + UUID.randomUUID().toString().take(6),
            name = name,
            category = category,
            categories = parsedCategories,
            url = url,
            logoUrl = logoUrl.ifBlank { "https://images.unsplash.com/photo-1526470608268-f674ce90ebd4?q=80&w=300" },
            country = country.ifBlank { "General" },
            isAdult = isAdult
        )
        viewModelScope.launch {
            firestoreRepository.saveIptv(newIptv)
        }
        val updated = (_iptvList.value + newIptv)
            .distinctBy { if (it.id.isNotBlank()) it.id else "${it.name}_${it.url}" }
        _iptvList.value = updated
        repository.saveIptv(updated)
    }

    fun deleteIptv(iptvId: String) {
        viewModelScope.launch {
            firestoreRepository.deleteIptv(iptvId)
        }
        val updated = _iptvList.value.filter { it.id != iptvId }
        _iptvList.value = updated
        repository.saveIptv(updated)
        if (_activeIptv.value?.id == iptvId) {
            stopAllPlayback()
        }
    }

    fun addMovie(title: String, genre: String, url: String, posterUrl: String, description: String, year: Int, duration: String, isAdult: Boolean = false, rating: String = "") {
        val parsedGenres = genre.split(Regex("[,;/]")).map { it.trim() }.filter { it.isNotBlank() }
        val newMovie = MovieItem(
            id = "movie_" + java.util.UUID.randomUUID().toString().take(6),
            title = title,
            genre = genre,
            genres = parsedGenres,
            url = url,
            posterUrl = posterUrl.ifBlank { "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=300" },
            description = description,
            year = year,
            duration = duration.ifBlank { "120 min" },
            isAdult = isAdult,
            rating = rating.ifBlank { "7.0/10" }
        )
        viewModelScope.launch {
            firestoreRepository.saveMovie(newMovie)
        }
        val updated = (_movies.value + newMovie)
            .distinctBy { if (it.id.isNotBlank()) it.id else "${it.title}_${it.url}" }
        _movies.value = updated
        repository.saveMovies(updated)
    }

    fun deleteMovie(movieId: String) {
        viewModelScope.launch {
            firestoreRepository.deleteMovie(movieId)
        }
        val updated = _movies.value.filter { it.id != movieId }
        _movies.value = updated
        repository.saveMovies(updated)
        if (_activeMovie.value?.id == movieId) {
            stopAllPlayback()
        }
    }

    // TMDb Integration
    fun searchTmdbMovie(query: String, onResult: (com.example.service.TmdbMovieResult?) -> Unit) {
        viewModelScope.launch {
            val result = com.example.service.TmdbService.searchMovie(query)
            onResult(result)
        }
    }

    // User Management Sync
    fun syncUserManagementToFirestore() {
        val email = repository.getRegisteredEmail().ifBlank { _currentProfile.value?.email ?: "" }
        if (email.isBlank()) return
        val profile = _currentProfile.value
        val displayName = profile?.name ?: email.substringBefore("@")
        val photoURL = profile?.avatarName ?: "avatar_1"

        val favoritesList = mutableListOf<String>()
        profile?.let {
            favoritesList.addAll(it.favoriteRadioIds)
            favoritesList.addAll(it.favoriteIptvIds)
            favoritesList.addAll(it.favoriteMovieIds)
        }

        val historyEntries = mutableListOf<com.example.models.UserHistoryEntry>()
        val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _radioHistory.value.forEach { r -> historyEntries.add(com.example.models.UserHistoryEntry(itemId = r.id, itemType = "radio", watchedAt = now)) }
        _iptvHistory.value.forEach { i -> historyEntries.add(com.example.models.UserHistoryEntry(itemId = i.id, itemType = "iptv", watchedAt = now)) }
        _movieHistory.value.forEach { m -> historyEntries.add(com.example.models.UserHistoryEntry(itemId = m.id, itemType = "movie", watchedAt = now)) }

        val aiList = mutableListOf<com.example.models.UserAiSuggestion>()
        _geminiRecommendations.value?.let { rec ->
            rec.recommendedMovieIds.forEach { id -> aiList.add(com.example.models.UserAiSuggestion(itemId = id, reason = rec.reason, score = 0.9)) }
            rec.recommendedIptvIds.forEach { id -> aiList.add(com.example.models.UserAiSuggestion(itemId = id, reason = rec.reason, score = 0.9)) }
            rec.recommendedRadioIds.forEach { id -> aiList.add(com.example.models.UserAiSuggestion(itemId = id, reason = rec.reason, score = 0.9)) }
        }

        val userDoc = com.example.models.UserFirestoreDocument(
            uid = email.lowercase(),
            email = email.lowercase(),
            displayName = displayName,
            photoURL = photoURL,
            favorites = favoritesList.distinct(),
            history = historyEntries,
            aiSuggestions = aiList,
            createdAt = now,
            lastLogin = now
        )

        viewModelScope.launch {
            firestoreRepository.syncUserDocumentToFirestore(userDoc)
        }
    }

    // Error Reporting System
    fun submitErrorReport(
        context: android.content.Context,
        itemType: String,
        itemId: String,
        itemName: String,
        issueType: String,
        comments: String,
        onComplete: (() -> Unit)? = null
    ) {
        val profileName = _currentProfile.value?.name ?: "Usuario"
        val email = _currentProfile.value?.email?.ifBlank { repository.getRegisteredEmail().ifBlank { "medinplayhub@gmail.com" } } ?: "medinplayhub@gmail.com"

        android.util.Log.d("ErrorReport", "🔴 Enviando reporte: itemId=$itemId, userEmail=$email")
        android.util.Log.d("ErrorReport", "📋 itemName: $itemName, itemType: $itemType, issueType: $issueType, comments: $comments")

        // Requirement: Cannot report the same item if a report is active (status == PENDIENTE)
        val existingActiveReport = _errorReports.value.find { rep ->
            ((itemId.isNotBlank() && rep.itemId == itemId) ||
             (itemName.isNotBlank() && rep.itemName.equals(itemName, ignoreCase = true))) &&
            rep.status.trim().uppercase() == "PENDIENTE"
        }
        if (existingActiveReport != null) {
            android.util.Log.w("ErrorReport", "⚠️ Bloqueando reporte duplicado para itemId: $itemId, itemName: $itemName (reporte activo ID: ${existingActiveReport.id})")
            android.widget.Toast.makeText(
                context,
                "Ya enviaste un reporte para '$itemName'. Está pendiente de revisión por el administrador.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            onComplete?.invoke()
            return
        }

        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
        val report = com.example.models.ErrorReport(
            id = "rep_${java.util.UUID.randomUUID().toString().take(6)}",
            itemType = itemType,
            itemId = itemId,
            itemName = itemName,
            issueType = issueType,
            comments = comments,
            userEmail = email,
            profileName = profileName,
            timestamp = timestamp,
            status = "PENDIENTE"
        )

        // Update local memory state immediately so banner stays visible instantly and continuously
        _errorReports.value = (listOf(report) + _errorReports.value)
            .distinctBy { if (it.id.isNotBlank()) it.id else "${it.itemId}_${it.timestamp}" }
            .sortedByDescending { it.timestamp }

        android.util.Log.d("ErrorReport", "✅ Reporte enviado, ID: ${report.id}")

        // Trigger native notification on device for medinplayhub@gmail.com session alert
        try {
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            val channelId = "error_reports_channel"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Alertas y Reportes MedinPlay",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones de reportes de error"
                }
                notificationManager?.createNotificationChannel(channel)
            }
            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("⚠️ Reporte de Error: $itemName")
                .setContentText("Falla: $issueType ($profileName)")
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle()
                    .bigText("Elemento: $itemName ($itemType)\nFalla: $issueType\nUsuario: $profileName ($email)\nComentarios: ${comments.ifBlank { "Ninguno" }}"))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
            notificationManager?.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        firestoreRepository.saveErrorReport(report) { success ->
            val message = if (success) "✅ Reporte enviado correctamente" else "❌ Error al enviar reporte"
            android.widget.Toast.makeText(
                context,
                message,
                android.widget.Toast.LENGTH_LONG
            ).show()
            onComplete?.invoke()
        }
    }

    fun updateErrorReportStatus(reportId: String, status: String) {
        val resolvedAt = if (status.uppercase() == "RESUELTO") {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        } else ""
        // Update local state immediately
        _errorReports.value = _errorReports.value.map { rep ->
            if (rep.id == reportId) rep.copy(status = status, resolvedAt = resolvedAt) else rep
        }
        firestoreRepository.updateErrorReportStatus(reportId, status, resolvedAt)
    }

    fun deleteErrorReport(reportId: String) {
        // Update local state immediately
        _errorReports.value = _errorReports.value.filter { it.id != reportId }
        firestoreRepository.deleteErrorReport(reportId)
    }

    // Toggle Favorite Logic
    fun toggleFavoriteRadio(radioId: String) {
        val profile = _currentProfile.value ?: return
        val currentFavs = profile.favoriteRadioIds
        val updatedFavs = if (currentFavs.contains(radioId)) {
            currentFavs - radioId
        } else {
            currentFavs + radioId
        }
        val updatedProfile = profile.copy(favoriteRadioIds = updatedFavs)
        updateProfileInList(updatedProfile)
    }

    fun toggleFavoriteIptv(iptvId: String) {
        val profile = _currentProfile.value ?: return
        val currentFavs = profile.favoriteIptvIds
        val updatedFavs = if (currentFavs.contains(iptvId)) {
            currentFavs - iptvId
        } else {
            currentFavs + iptvId
        }
        val updatedProfile = profile.copy(favoriteIptvIds = updatedFavs)
        updateProfileInList(updatedProfile)
    }

    fun toggleFavoriteMovie(movieId: String) {
        val profile = _currentProfile.value ?: return
        val currentFavs = profile.favoriteMovieIds
        val updatedFavs = if (currentFavs.contains(movieId)) {
            currentFavs - movieId
        } else {
            currentFavs + movieId
        }
        val updatedProfile = profile.copy(favoriteMovieIds = updatedFavs)
        updateProfileInList(updatedProfile)
    }

    private fun updateProfileInList(updatedProfile: UserProfile) {
        val updatedList = _profiles.value.map {
            if (it.id == updatedProfile.id) updatedProfile else it
        }
        _profiles.value = updatedList
        _currentProfile.value = updatedProfile
        repository.saveProfiles(updatedList)
        viewModelScope.launch {
            firestoreRepository.saveProfile(updatedProfile)
        }
    }

    // Search and Filters
    fun setRadioSearch(query: String) {
        _radioSearchQuery.value = query
    }

    fun setIptvSearch(query: String) {
        _iptvSearchQuery.value = query
    }

    fun setMovieSearch(query: String) {
        _movieSearchQuery.value = query
    }

    fun toggleShowOnlyFavorites() {
        _showOnlyFavorites.update { !it }
    }

    fun getUnifiedConfigJson(): String {
        return repository.getUnifiedConfigJson()
    }

    fun importUnifiedConfigJson(json: String): Boolean {
        val success = repository.importUnifiedConfigJson(json)
        if (success) {
            loadAllData()
            viewModelScope.launch {
                firestoreRepository.saveAllRadios(_radios.value)
                firestoreRepository.saveAllIptv(_iptvList.value)
                firestoreRepository.saveAllMovies(_movies.value)
            }
        }
        return success
    }

    fun resetAppToDefaults() {
        repository.resetToDefaults()
        loadAllData()
        hasSyncedRadiosWithFirestore = false
        hasSyncedIptvWithFirestore = false
        hasSyncedMoviesWithFirestore = false
        viewModelScope.launch {
            firestoreRepository.deleteAllContent()
        }
        _currentProfile.value = _profiles.value.firstOrNull()
        stopAllPlayback()
        _isAdminMode.value = false
    }

    // --- PLAYBACK HISTORY IMPLEMENTATION ---
    private var historySyncJob: kotlinx.coroutines.Job? = null

    fun listenToProfileHistory(profileId: String?) {
        historySyncJob?.cancel()
        if (profileId.isNullOrBlank()) return
        historySyncJob = viewModelScope.launch {
            firestoreRepository.getProfileHistoryFlow(profileId).collect { remote ->
                if (remote != null) {
                    repository.saveRadioHistory(profileId, remote.radioHistory)
                    repository.saveIptvHistory(profileId, remote.iptvHistory)
                    repository.saveMovieHistory(profileId, remote.movieHistory)
                    repository.saveAdultHistory(profileId, remote.adultHistory)
                    loadHistoryForCurrentProfile()
                } else {
                    syncHistoryToFirestore(profileId)
                }
            }
        }
    }

    private fun syncHistoryToFirestore(profileId: String?) {
        if (profileId.isNullOrBlank()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val radios = repository.getRadioHistory(profileId)
            val iptv = repository.getIptvHistory(profileId)
            val movies = repository.getMovieHistory(profileId)
            val adult = repository.getAdultHistory(profileId)
            firestoreRepository.saveProfileHistory(profileId, radios, iptv, movies, adult)
        }
    }

    fun loadHistoryForCurrentProfile() {
        val profileId = _currentProfile.value?.id
        if (profileId == null) {
            _radioHistory.value = emptyList()
            _iptvHistory.value = emptyList()
            _movieHistory.value = emptyList()
            _adultIptvHistory.value = emptyList()
            _adultMovieHistory.value = emptyList()
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val radioIds = repository.getRadioHistory(profileId)
            val allRadios = _radios.value
            val radioMap = allRadios.associateBy { it.id }
            val radioHist = radioIds.mapNotNull { radioMap[it] }

            val iptvIds = repository.getIptvHistory(profileId)
            val allIptv = _iptvList.value
            val iptvMap = allIptv.associateBy { it.id }
            val iptvHist = iptvIds.mapNotNull { iptvMap[it] }

            val movieIds = repository.getMovieHistory(profileId)
            val allMovies = _movies.value
            val movieMap = allMovies.associateBy { it.id }
            val movieHist = movieIds.mapNotNull { movieMap[it] }

            val adultIds = repository.getAdultHistory(profileId)
            val adultIptvHist = adultIds.mapNotNull { id -> iptvMap[id]?.takeIf { it.isAdult || it.category == "+18" } }
            val adultMovieHist = adultIds.mapNotNull { id -> movieMap[id]?.takeIf { it.isAdult || it.genre.contains("+18") } }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                _radioHistory.value = radioHist
                _iptvHistory.value = iptvHist
                _movieHistory.value = movieHist
                _adultIptvHistory.value = adultIptvHist
                _adultMovieHistory.value = adultMovieHist
            }
        }
    }

    fun clearAllPlaybackHistory() {
        val profileId = _currentProfile.value?.id ?: return
        repository.clearAllHistory(profileId)
        repository.clearAdultHistory(profileId)
        syncHistoryToFirestore(profileId)
        loadHistoryForCurrentProfile()
    }

    fun clearRadioHistory() {
        val profileId = _currentProfile.value?.id ?: return
        repository.clearRadioHistory(profileId)
        syncHistoryToFirestore(profileId)
        loadHistoryForCurrentProfile()
    }

    fun clearIptvHistory() {
        val profileId = _currentProfile.value?.id ?: return
        repository.clearIptvHistory(profileId)
        syncHistoryToFirestore(profileId)
        loadHistoryForCurrentProfile()
    }

    fun clearMovieHistory() {
        val profileId = _currentProfile.value?.id ?: return
        repository.clearMovieHistory(profileId)
        syncHistoryToFirestore(profileId)
        loadHistoryForCurrentProfile()
    }

    fun clearAdultHistory() {
        val profileId = _currentProfile.value?.id ?: return
        repository.clearAdultHistory(profileId)
        syncHistoryToFirestore(profileId)
        loadHistoryForCurrentProfile()
    }

    fun removeRadioHistoryItems(ids: List<String>) {
        val profileId = _currentProfile.value?.id ?: return
        repository.removeRadioHistoryItems(profileId, ids)
        syncHistoryToFirestore(profileId)
        loadHistoryForCurrentProfile()
    }

    fun removeIptvHistoryItems(ids: List<String>) {
        val profileId = _currentProfile.value?.id ?: return
        repository.removeIptvHistoryItems(profileId, ids)
        syncHistoryToFirestore(profileId)
        loadHistoryForCurrentProfile()
    }

    fun removeMovieHistoryItems(ids: List<String>) {
        val profileId = _currentProfile.value?.id ?: return
        repository.removeMovieHistoryItems(profileId, ids)
        syncHistoryToFirestore(profileId)
        loadHistoryForCurrentProfile()
    }

    fun removeAdultHistoryItems(ids: List<String>) {
        val profileId = _currentProfile.value?.id ?: return
        repository.removeAdultHistoryItems(profileId, ids)
        syncHistoryToFirestore(profileId)
        loadHistoryForCurrentProfile()
    }

    // --- PAYMENT METHODS & SUBSCRIPTION PRICING ---
    fun addPaymentMethod(method: com.example.models.PaymentMethod) {
        val updated = _paymentMethods.value + method
        _paymentMethods.value = updated
        repository.savePaymentMethods(updated)
        firestoreRepository.savePaymentMethod(method)
    }

    fun addPaymentMethod(title: String, details: String, instructions: String) {
        val method = com.example.models.PaymentMethod(
            id = "pm_" + UUID.randomUUID().toString().take(6),
            title = title,
            details = details,
            instructions = instructions,
            isActive = true
        )
        addPaymentMethod(method)
    }

    fun updatePaymentMethod(method: com.example.models.PaymentMethod) {
        val updated = _paymentMethods.value.map {
            if (it.id == method.id) method else it
        }
        _paymentMethods.value = updated
        repository.savePaymentMethods(updated)
        firestoreRepository.savePaymentMethod(method)
    }

    fun updatePaymentMethod(id: String, title: String, details: String, instructions: String, isActive: Boolean) {
        val method = com.example.models.PaymentMethod(
            id = id,
            title = title,
            details = details,
            instructions = instructions,
            isActive = isActive
        )
        updatePaymentMethod(method)
    }

    fun deletePaymentMethod(id: String) {
        val updated = _paymentMethods.value.filter { it.id != id }
        _paymentMethods.value = updated
        repository.savePaymentMethods(updated)
        firestoreRepository.deletePaymentMethod(id)
    }

    fun setSubscriptionPrice(price: String) {
        val clean = price.trim()
        _subscriptionPrice.value = clean
        repository.setSubscriptionPrice(clean)
        firestoreRepository.saveSubscriptionPrice(clean)
    }

    fun submitPaymentRequest(userEmail: String, paymentMethodName: String, referenceCode: String, amount: String) {
        val genCode = "ACT-" + kotlin.random.Random.nextInt(100000, 999999).toString()
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        val startDateStr = sdf.format(cal.time)
        cal.add(java.util.Calendar.DAY_OF_MONTH, 30)
        val endDateStr = sdf.format(cal.time)

        val request = com.example.models.UserPaymentRequest(
            id = "req_" + UUID.randomUUID().toString().take(6),
            userEmail = userEmail,
            paymentMethodName = paymentMethodName,
            referenceCode = referenceCode,
            amount = amount.ifBlank { _subscriptionPrice.value },
            timestamp = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
            status = "PENDING",
            activationCode = genCode,
            codeSent = false,
            codeSentTimestamp = "",
            subscriptionStartDate = startDateStr,
            subscriptionEndDate = endDateStr,
            clientProfiles = _profiles.value,
            sharedByProfile = _currentProfile.value?.name ?: "Semilla"
        )
        repository.addPaymentRequest(request)
        _paymentRequests.value = repository.loadPaymentRequests()
        firestoreRepository.savePaymentRequest(request)

        // Automatically assign this email address to the Principal Profile in settings
        val cleanEmail = userEmail.trim()
        if (cleanEmail.isNotBlank()) {
            _registeredEmail.value = cleanEmail
            repository.setRegisteredEmail(cleanEmail)
            val principalProf = _profiles.value.find { it.isPrincipal } ?: _profiles.value.firstOrNull()
            if (principalProf != null) {
                updateProfile(
                    profileId = principalProf.id,
                    newName = principalProf.name,
                    newAvatar = principalProf.avatarName,
                    newColorHex = principalProf.accentColorHex,
                    newPin = principalProf.pin,
                    newAdultPin = principalProf.adultPin,
                    gender = principalProf.gender,
                    email = cleanEmail
                )
            }
        }

        repository.addAdminNotification(
            title = "Nuevo Comprobante de Pago Recibido",
            message = "El usuario $userEmail envió el pago de ${request.amount} vía $paymentMethodName (Ref: $referenceCode)."
        )
        _adminNotifications.value = repository.loadAdminNotifications()

        // Send Email notifications to user and admin (medinplayhub@gmail.com for native push notifications)
        viewModelScope.launch {
            // Notification to Admin (triggers native device notification)
            com.example.service.EmailSender.sendEmail(
                recipientEmail = "medinplayhub@gmail.com",
                subject = "🔔 [NUEVO PAGO REPORTADO] $userEmail",
                bodyText = "Hola Admin,\n\nSe ha reportado un nuevo pago de mensualidad en MedinPlay Hub:\n\nUsuario: $userEmail\nMétodo: $paymentMethodName\nReferencia: $referenceCode\nMonto: ${request.amount}\nCódigo Asignado: $genCode\nPeriodo Estimado: $startDateStr a $endDateStr\n\nIngresa al Panel Administrativo (Sección 'App Nuevo Usuario') para verificar y enviar el código de activación."
            )
            // Notification to User
            com.example.service.EmailSender.sendEmail(
                recipientEmail = userEmail,
                subject = "Comprobante Recibido - MedinPlay Hub",
                bodyText = "Hola,\n\nHemos recibido correctamente tu comprobante de pago ($paymentMethodName - Ref: $referenceCode por ${request.amount}).\n\nTu solicitud está siendo revisada por nuestro equipo. Una vez verificado el pago, recibirás a este mismo correo ($userEmail) tu código de activación.\n\n⚠️ NOTA IMPORTANTE: Revisa también tu carpeta de CORREO NO DESEADO / SPAM por si la notificación llega allí.\n\nAtentamente,\nEquipo MedinPlay Hub\nContacto: medinplayhub@gmail.com"
            )
        }
    }

    fun sendActivationCodeToClient(requestId: String, customCode: String = "") {
        val request = _paymentRequests.value.find { it.id == requestId } ?: return
        val codeToSend = if (customCode.isNotBlank()) customCode.trim().uppercase() else if (request.activationCode.isNotBlank()) request.activationCode else "ACT-" + kotlin.random.Random.nextInt(100000, 999999).toString()
        val nowStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        val startDateStr = if (request.subscriptionStartDate.isNotBlank()) request.subscriptionStartDate else sdf.format(cal.time)
        val endDateStr = if (request.subscriptionEndDate.isNotBlank()) request.subscriptionEndDate else {
            cal.add(java.util.Calendar.DAY_OF_MONTH, 30)
            sdf.format(cal.time)
        }

        val updatedReq = request.copy(
            status = "APPROVED",
            activationCode = codeToSend,
            codeSent = true,
            codeSentTimestamp = nowStr,
            subscriptionStartDate = startDateStr,
            subscriptionEndDate = endDateStr,
            clientProfiles = if (request.clientProfiles.isNotEmpty()) request.clientProfiles else _profiles.value
        )

        val updatedList = _paymentRequests.value.map { if (it.id == requestId) updatedReq else it }
        _paymentRequests.value = updatedList
        repository.savePaymentRequests(updatedList)
        firestoreRepository.savePaymentRequest(updatedReq)

        repository.addAdminNotification(
            title = "Código de Activación Enviado",
            message = "Se envió el código $codeToSend al usuario ${request.userEmail} (Vence: $endDateStr)."
        )
        _adminNotifications.value = repository.loadAdminNotifications()

        viewModelScope.launch {
            // Admin Notification Email
            com.example.service.EmailSender.sendEmail(
                recipientEmail = "medinplayhub@gmail.com",
                subject = "🔔 [LICENCIA ACTIVADA] ${request.userEmail}",
                bodyText = "Hola Admin,\n\nSe ha enviado el código de activación a ${request.userEmail}:\n\nCódigo: $codeToSend\nInicio: $startDateStr\nVencimiento: $endDateStr\nMonto: ${request.amount}"
            )

            // User Notification Email
            com.example.service.EmailSender.sendEmail(
                recipientEmail = request.userEmail,
                subject = "🔑 Código de Activación - MedinPlay Hub",
                bodyText = "Hola,\n\n¡Tu pago ha sido verificado y aprobado exitosamente por nuestro equipo!\n\nTu CÓDIGO DE ACTIVACIÓN para ingresar a MedinPlay Hub es:\n👉 $codeToSend\n\n📅 Fecha de Inicio de Suscripción: $startDateStr\n📅 Fecha de Vencimiento de Suscripción: $endDateStr\n\nInstrucciones de Activación:\n1. Abre la aplicación MedinPlay Hub en tu dispositivo.\n2. En el campo 'Código de Activación', ingresa: $codeToSend\n3. Presiona 'Activar Aplicación' para ingresar a la pantalla de tus perfiles.\n\n⚠️ NOTA IMPORTANTE: Si no visualizas este correo en tu bandeja de entrada principal, por favor revisa tu carpeta de CORREO NO DESEADO / SPAM.\n\n💡 Si pasados 5 minutos no recibes la notificación, puedes pulsar la opción 'Reenviar Código de Activación' dentro de la aplicación.\n\nAtentamente,\nEquipo MedinPlay Hub\nContacto: medinplayhub@gmail.com"
            )
        }
    }

    fun resendActivationCode(email: String, onResult: (Boolean, String) -> Unit) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            onResult(false, "Por favor ingresa tu correo electrónico.")
            return
        }
        val req = _paymentRequests.value.find { it.userEmail.equals(cleanEmail, ignoreCase = true) }
        if (req != null) {
            val codeToSend = if (req.activationCode.isNotBlank()) req.activationCode else "ACT-" + kotlin.random.Random.nextInt(100000, 999999).toString()
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val cal = java.util.Calendar.getInstance()
            val startDateStr = if (req.subscriptionStartDate.isNotBlank()) req.subscriptionStartDate else sdf.format(cal.time)
            val endDateStr = if (req.subscriptionEndDate.isNotBlank()) req.subscriptionEndDate else {
                cal.add(java.util.Calendar.DAY_OF_MONTH, 30)
                sdf.format(cal.time)
            }

            val updatedReq = req.copy(
                activationCode = codeToSend,
                codeSent = true,
                codeSentTimestamp = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                subscriptionStartDate = startDateStr,
                subscriptionEndDate = endDateStr
            )
            val updatedList = _paymentRequests.value.map { if (it.id == req.id) updatedReq else it }
            _paymentRequests.value = updatedList
            repository.savePaymentRequests(updatedList)
            firestoreRepository.savePaymentRequest(updatedReq)

            repository.addAdminNotification(
                title = "Reenvío de Código Solicitado",
                message = "El usuario $cleanEmail solicitó el reenvío de su código de activación."
            )
            _adminNotifications.value = repository.loadAdminNotifications()

            viewModelScope.launch {
                com.example.service.EmailSender.sendEmail(
                    recipientEmail = "medinplayhub@gmail.com",
                    subject = "📩 [REENVÍO DE CÓDIGO SOLICITADO] $cleanEmail",
                    bodyText = "Hola Admin,\n\nEl usuario $cleanEmail ha solicitado el reenvío de su código de activación desde la aplicación.\n\nCódigo: $codeToSend\nPeriodo: $startDateStr a $endDateStr"
                )

                com.example.service.EmailSender.sendEmail(
                    recipientEmail = cleanEmail,
                    subject = "🔑 Reenvío de Código de Activación - MedinPlay Hub",
                    bodyText = "Hola,\n\nTe reenviamos tu CÓDIGO DE ACTIVACIÓN para MedinPlay Hub:\n\n👉 $codeToSend\n\n📅 Fecha de Inicio: $startDateStr\n📅 Fecha de Vencimiento: $endDateStr\n\nInstrucciones:\n1. Abre MedinPlay Hub e ingresa el código $codeToSend\n2. Presiona 'Activar Aplicación'.\n\n⚠️ REVISA TU CARPETA DE SPAM / CORREO NO DESEADO si no lo ves en la bandeja de entrada.\n\nAtentamente,\nEquipo MedinPlay Hub\nContacto: medinplayhub@gmail.com"
                )
            }
            onResult(true, "¡Código reenviado a $cleanEmail! Revisa tu correo y carpeta de SPAM.")
        } else {
            onResult(false, "El correo $cleanEmail no posee un registro activo. Por favor reporta tu pago en el Paso 2.")
        }
    }

    fun approvePaymentRequest(requestId: String) {
        sendActivationCodeToClient(requestId)
    }

    fun rejectPaymentRequest(requestId: String, reason: String = "") {
        val request = _paymentRequests.value.find { it.id == requestId } ?: return
        val updatedReq = request.copy(status = "REJECTED", notes = reason)
        val updated = _paymentRequests.value.map {
            if (it.id == requestId) updatedReq else it
        }
        _paymentRequests.value = updated
        repository.savePaymentRequests(updated)
        firestoreRepository.savePaymentRequest(updatedReq)

        repository.addAdminNotification(
            title = "Pago Rechazado",
            message = "Se rechazó el pago del usuario ${request.userEmail} (Ref: ${request.referenceCode})."
        )
        _adminNotifications.value = repository.loadAdminNotifications()

        viewModelScope.launch {
            com.example.service.EmailSender.sendEmail(
                recipientEmail = request.userEmail,
                subject = "Actualización sobre tu Pago - MedinPlay Hub",
                bodyText = "Hola,\n\nLamentamos informarte que no pudimos validar tu comprobante de pago (Ref: ${request.referenceCode}).\n\nMotivo: ${reason.ifBlank { "Número de referencia no encontrado o incorrecto." }}\n\nPor favor, verifica los datos e intenta enviar nuevamente tu pago desde la aplicación o contáctanos a medinplayhub@gmail.com."
            )
        }
    }

    fun deletePaymentRequest(requestId: String) {
        val updated = _paymentRequests.value.filter { it.id != requestId }
        _paymentRequests.value = updated
        repository.savePaymentRequests(updated)
        firestoreRepository.deletePaymentRequest(requestId)
    }

    // --- ADMIN BULK LOADING IMPLEMENTATION ---
    fun bulkAddRadios(inputText: String): Int {
        var addedCount = 0
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) return 0
        
        if (trimmed.startsWith("[")) {
            try {
                val type = Types.newParameterizedType(List::class.java, RadioItem::class.java)
                val adapter = moshi.adapter<List<RadioItem>>(type)
                val items = adapter.fromJson(trimmed)
                if (items != null) {
                    val currentList = _radios.value.toMutableList()
                    for (item in items) {
                        val id = if (item.id.isBlank() || currentList.any { it.id == item.id }) {
                            "radio_" + UUID.randomUUID().toString().take(6)
                        } else {
                            item.id
                        }
                        val newItem = item.copy(
                            id = id,
                            imageUrl = item.imageUrl.ifBlank { "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?q=80&w=300" }
                        )
                        currentList.add(newItem)
                        addedCount++
                    }
                    _radios.value = currentList
                    repository.saveRadios(currentList)
                    viewModelScope.launch {
                        firestoreRepository.saveAllRadios(currentList)
                    }
                }
            } catch (e: Exception) {
                // fallback to csv if JSON parse fails
            }
        }
        
        if (addedCount == 0) {
            val lines = trimmed.split("\n")
            val currentList = _radios.value.toMutableList()
            for (line in lines) {
                val cleanLine = line.trim()
                if (cleanLine.isEmpty() || cleanLine.startsWith("#")) continue
                val parts = cleanLine.split(",")
                if (parts.size >= 3) {
                    val name = parts[0].trim()
                    val genre = parts[1].trim()
                    val url = parts[2].trim()
                    val imageUrl = if (parts.size > 3) parts[3].trim() else ""
                    val desc = if (parts.size > 4) parts[4].trim() else ""
                    
                    val newItem = RadioItem(
                        id = "radio_" + UUID.randomUUID().toString().take(6),
                        name = name,
                        genre = genre,
                        url = url,
                        imageUrl = imageUrl.ifBlank { "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?q=80&w=300" },
                        description = desc
                    )
                    currentList.add(newItem)
                    addedCount++
                }
            }
            if (addedCount > 0) {
                _radios.value = currentList
                repository.saveRadios(currentList)
                viewModelScope.launch {
                    firestoreRepository.saveAllRadios(currentList)
                }
            }
        }
        return addedCount
    }

    fun bulkAddIptv(inputText: String): Int {
        var addedCount = 0
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) return 0
        
        if (trimmed.startsWith("[")) {
            try {
                val type = Types.newParameterizedType(List::class.java, IptvItem::class.java)
                val adapter = moshi.adapter<List<IptvItem>>(type)
                val items = adapter.fromJson(trimmed)
                if (items != null) {
                    val currentList = _iptvList.value.toMutableList()
                    for (item in items) {
                        val id = if (item.id.isBlank() || currentList.any { it.id == item.id }) {
                            "iptv_" + UUID.randomUUID().toString().take(6)
                        } else {
                            item.id
                        }
                        val newItem = item.copy(
                            id = id,
                            logoUrl = item.logoUrl.ifBlank { "https://images.unsplash.com/photo-1526470608268-f674ce90ebd4?q=80&w=300" }
                        )
                        currentList.add(newItem)
                        addedCount++
                    }
                    _iptvList.value = currentList
                    repository.saveIptv(currentList)
                    viewModelScope.launch {
                        firestoreRepository.saveAllIptv(currentList)
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        
        if (addedCount == 0) {
            val lines = trimmed.split("\n")
            val currentList = _iptvList.value.toMutableList()
            for (line in lines) {
                val cleanLine = line.trim()
                if (cleanLine.isEmpty() || cleanLine.startsWith("#")) continue
                val parts = cleanLine.split(",")
                if (parts.size >= 3) {
                    val name = parts[0].trim()
                    val category = parts[1].trim()
                    val url = parts[2].trim()
                    val logoUrl = if (parts.size > 3) parts[3].trim() else ""
                    val country = if (parts.size > 4) parts[4].trim() else "General"
                    val isAdult = if (parts.size > 5) parts[5].trim().lowercase() == "true" else false
                    
                    val newItem = IptvItem(
                        id = "iptv_" + UUID.randomUUID().toString().take(6),
                        name = name,
                        category = category,
                        url = url,
                        logoUrl = logoUrl.ifBlank { "https://images.unsplash.com/photo-1526470608268-f674ce90ebd4?q=80&w=300" },
                        country = country.ifBlank { "General" },
                        isAdult = isAdult
                    )
                    currentList.add(newItem)
                    addedCount++
                }
            }
            if (addedCount > 0) {
                _iptvList.value = currentList
                repository.saveIptv(currentList)
                viewModelScope.launch {
                    firestoreRepository.saveAllIptv(currentList)
                }
            }
        }
        return addedCount
    }

    fun bulkAddMovies(inputText: String): Int {
        var addedCount = 0
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) return 0
        
        if (trimmed.startsWith("[")) {
            try {
                val type = Types.newParameterizedType(List::class.java, MovieItem::class.java)
                val adapter = moshi.adapter<List<MovieItem>>(type)
                val items = adapter.fromJson(trimmed)
                if (items != null) {
                    val currentList = _movies.value.toMutableList()
                    for (item in items) {
                        val id = if (item.id.isBlank() || currentList.any { it.id == item.id }) {
                            "movie_" + UUID.randomUUID().toString().take(6)
                        } else {
                            item.id
                        }
                        val newItem = item.copy(
                            id = id,
                            posterUrl = item.posterUrl.ifBlank { "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=300" }
                        )
                        currentList.add(newItem)
                        addedCount++
                    }
                    _movies.value = currentList
                    repository.saveMovies(currentList)
                    viewModelScope.launch {
                        firestoreRepository.saveAllMovies(currentList)
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        
        if (addedCount == 0) {
            val lines = trimmed.split("\n")
            val currentList = _movies.value.toMutableList()
            for (line in lines) {
                val cleanLine = line.trim()
                if (cleanLine.isEmpty() || cleanLine.startsWith("#")) continue
                val parts = cleanLine.split(",")
                if (parts.size >= 3) {
                    val title = parts[0].trim()
                    val genre = parts[1].trim()
                    val url = parts[2].trim()
                    val posterUrl = if (parts.size > 3) parts[3].trim() else ""
                    val desc = if (parts.size > 4) parts[4].trim() else ""
                    val year = if (parts.size > 5) parts[5].trim().toIntOrNull() ?: 2026 else 2026
                    val duration = if (parts.size > 6) parts[6].trim() else "120 min"
                    val isAdult = if (parts.size > 7) parts[7].trim().lowercase() == "true" else false
                    val rating = if (parts.size > 8) parts[8].trim() else "7.0/10"
                    
                    val newItem = MovieItem(
                        id = "movie_" + UUID.randomUUID().toString().take(6),
                        title = title,
                        genre = genre,
                        url = url,
                        posterUrl = posterUrl.ifBlank { "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=300" },
                        description = desc,
                        year = year,
                        duration = duration.ifBlank { "120 min" },
                        isAdult = isAdult,
                        rating = rating.ifBlank { "7.0/10" }
                    )
                    currentList.add(newItem)
                    addedCount++
                }
            }
            if (addedCount > 0) {
                _movies.value = currentList
                repository.saveMovies(currentList)
                viewModelScope.launch {
                    firestoreRepository.saveAllMovies(currentList)
                }
            }
        }
        return addedCount
    }

    // --- OMDB & OPENROUTER AI METHODS ---

    fun setOmdbSearchMode(enabled: Boolean) {
        _isOmdbSearchMode.value = enabled
        if (!enabled) {
            _omdbSearchResults.value = emptyList()
            _omdbSearchError.value = null
        }
    }

    fun searchOmdb(query: String) {
        val q = query.trim()
        if (q.isBlank()) {
            _omdbSearchResults.value = emptyList()
            _omdbSearchError.value = null
            return
        }
        viewModelScope.launch {
            _isOmdbSearching.value = true
            _omdbSearchError.value = null
            val result = omdbRepository.searchMovies(q)
            _isOmdbSearching.value = false
            _activeOmdbKeyLabel.value = omdbRepository.getActiveKeyLabel()
            result.onSuccess { list ->
                _omdbSearchResults.value = list
                if (list.isEmpty()) {
                    _omdbSearchError.value = "No se encontraron películas para '$q'"
                }
            }.onFailure { err ->
                _omdbSearchResults.value = emptyList()
                _omdbSearchError.value = err.message ?: "Error al buscar películas"
            }
        }
    }

    fun loadOmdbMovieDetails(imdbId: String, onLoaded: ((OmdbMovieDetail) -> Unit)? = null) {
        viewModelScope.launch {
            _isLoadingOmdbDetail.value = true
            val result = omdbRepository.getMovieDetails(imdbId)
            _isLoadingOmdbDetail.value = false
            _activeOmdbKeyLabel.value = omdbRepository.getActiveKeyLabel()
            result.onSuccess { detail ->
                _selectedOmdbMovieDetail.value = detail
                onLoaded?.invoke(detail)
            }.onFailure { err ->
                _omdbSearchError.value = err.message ?: "Error al cargar detalles de la película"
            }
        }
    }

    fun loadOmdbMovieByTitle(title: String, year: Int? = null, onLoaded: ((OmdbMovieDetail) -> Unit)? = null) {
        viewModelScope.launch {
            _isLoadingOmdbDetail.value = true
            val result = omdbRepository.getMovieByTitle(title, year)
            _isLoadingOmdbDetail.value = false
            _activeOmdbKeyLabel.value = omdbRepository.getActiveKeyLabel()
            result.onSuccess { detail ->
                _selectedOmdbMovieDetail.value = detail
                onLoaded?.invoke(detail)
            }.onFailure { err ->
                _omdbSearchError.value = err.message ?: "Película no encontrada"
            }
        }
    }

    fun setSelectedOmdbMovieDetail(detail: OmdbMovieDetail?) {
        _selectedOmdbMovieDetail.value = detail
    }

    fun analyzeMovieWithAi(title: String, year: String? = null, plot: String? = null) {
        viewModelScope.launch {
            _isLoadingAiAnalysis.value = true
            _aiAnalysisError.value = null
            val result = openRouterRepository.analyzeMovie(title, year, plot)
            _isLoadingAiAnalysis.value = false
            result.onSuccess { analysis ->
                _currentAiAnalysis.value = analysis
            }.onFailure { err ->
                _aiAnalysisError.value = err.message ?: "No se pudo generar el análisis de IA"
            }
        }
    }

    fun clearAiAnalysis() {
        _currentAiAnalysis.value = null
        _aiAnalysisError.value = null
        _isLoadingAiAnalysis.value = false
    }

    fun addOmdbMovieToCatalog(
        omdbMovie: OmdbMovieDetail,
        streamUrl: String = "",
        isAdult: Boolean = false,
        onAdded: (() -> Unit)? = null
    ) {
        val durationFormatted = if (omdbMovie.runtime.isNotBlank() && !omdbMovie.runtime.equals("N/A", ignoreCase = true)) {
            omdbMovie.runtime
        } else {
            "120 min"
        }
        val cleanGenre = if (omdbMovie.genre.isNotBlank() && !omdbMovie.genre.equals("N/A", ignoreCase = true)) {
            omdbMovie.genre
        } else {
            "General"
        }
        val cleanRating = if (!omdbMovie.imdbRating.isNullOrBlank() && !omdbMovie.imdbRating.equals("N/A", ignoreCase = true)) {
            "${omdbMovie.imdbRating}/10"
        } else {
            "7.0/10"
        }

        val newMovie = MovieItem(
            id = "movie_${UUID.randomUUID().toString().take(6)}",
            title = omdbMovie.title,
            genre = cleanGenre,
            genres = cleanGenre.split(",").map { it.trim() }.filter { it.isNotBlank() },
            url = streamUrl,
            posterUrl = omdbMovie.cleanPoster.ifBlank { "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=300" },
            description = omdbMovie.cleanPlot.ifBlank { "Película en streaming" },
            year = omdbMovie.cleanYear,
            duration = durationFormatted,
            isAdult = isAdult || omdbMovie.rated.contains("R", ignoreCase = true) || omdbMovie.rated.contains("NC-17", ignoreCase = true),
            rating = cleanRating
        )

        val updated = _movies.value.toMutableList().apply { add(newMovie) }
        _movies.value = updated
        repository.saveMovies(updated)
        viewModelScope.launch {
            firestoreRepository.saveAllMovies(updated)
        }
        onAdded?.invoke()
    }

    fun getMovieStreams(title: String, year: Int? = null, imdbId: String? = null): List<MovieStreamSource> {
        return streamResolver.getDefaultStreams()
    }

    fun resolveAndPlayMovie(
        title: String,
        year: Int = 0,
        imdbId: String = "",
        posterUrl: String = "",
        description: String = "",
        genre: String = "Cine",
        serverIndex: Int = 0
    ) {
        viewModelScope.launch {
            _isResolvingStream.value = true
            _resolvingStreamMovieTitle.value = title
            try {
                val streams = streamResolver.resolveStreams(title, year, imdbId)
                val selectedStream = streams.getOrElse(serverIndex) { streams.first() }
                val playableUrl = streamResolver.extractPlayableStream(selectedStream.url, serverIndex)
                val movieItem = MovieItem(
                    id = "cine_${imdbId.ifBlank { title.hashCode().toString() }}",
                    title = title,
                    genre = genre.ifBlank { "Cine" },
                    url = playableUrl,
                    posterUrl = posterUrl.ifBlank { "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=300" },
                    description = description.ifBlank { "Película en streaming de MedinPlay Hub" },
                    year = year,
                    duration = "120 min"
                )
                playMovie(movieItem, 0L)
            } finally {
                _isResolvingStream.value = false
                _resolvingStreamMovieTitle.value = null
            }
        }
    }

    fun resolveAndTransmitMovie(
        context: android.content.Context,
        title: String,
        year: Int = 0,
        imdbId: String = "",
        serverIndex: Int = 0
    ) {
        viewModelScope.launch {
            _isResolvingStream.value = true
            _resolvingStreamMovieTitle.value = title
            try {
                val streams = streamResolver.resolveStreams(title, year, imdbId)
                val selectedStream = streams.getOrElse(serverIndex) { streams.first() }
                val playableUrl = streamResolver.extractPlayableStream(selectedStream.url, serverIndex)
                launchExternalStreamCast(context, playableUrl, title)
            } catch (e: Exception) {
                android.util.Log.e("MediaViewModel", "Error al resolver transmisión externa", e)
                launchExternalStreamCast(context, "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", title)
            } finally {
                _isResolvingStream.value = false
                _resolvingStreamMovieTitle.value = null
            }
        }
    }

    enum class Tab {
        Radio, IPTV, Movies, AdminSettings
    }
}

enum class PlayerSizeMode {
    Normal, Fullscreen, PiP
}
