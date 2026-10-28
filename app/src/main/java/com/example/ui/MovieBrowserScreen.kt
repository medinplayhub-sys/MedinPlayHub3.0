package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.omdb.OmdbSearchItem
import com.example.models.MovieItem
import com.example.models.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BrowserViewMode {
    HOME,
    AUTOCOMPLETE,
    SEARCH_RESULTS,
    NEWS,
    MOVIES_AND_SERIES,
    FOOTBALL,
    GENERATE_IMAGES,
    HISTORY,
    VPN,
    DRIVE,
    TOOLS
}

data class BrowserShortcut(
    val title: String,
    val icon: ImageVector,
    val badge: String? = null,
    val iconColor: Color,
    val bgColor: Color,
    val targetMode: BrowserViewMode
)

data class WebMovieResult(
    val providerDomain: String,
    val providerIconUrl: String? = null,
    val audioBadge: String,
    val title: String,
    val metadataLine: String,
    val synopsis: String,
    val posterUrl: String,
    val year: Int,
    val imdbId: String = "",
    val serverIndex: Int = 0,
    val isSeries: Boolean = false,
    val episodes: List<Int> = emptyList(),
    val directStreamUrl: String = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
)

data class RecommendationCarouselItem(
    val title: String,
    val year: String,
    val type: String,
    val posterUrl: String
)

@Composable
fun MovieBrowserScreen(
    viewModel: MediaViewModel,
    profile: UserProfile,
    accentColor: Color
) {
    val context = LocalContext.current
    var viewMode by remember { mutableStateOf(BrowserViewMode.HOME) }
    var currentQuery by remember { mutableStateOf("") }
    var activeSearchQuery by remember { mutableStateOf("") }
    val movies by viewModel.movies.collectAsStateWithLifecycle()
    val omdbResults by viewModel.omdbSearchResults.collectAsStateWithLifecycle()
    val isOmdbSearching by viewModel.isOmdbSearching.collectAsStateWithLifecycle()
    val isResolvingStream by viewModel.isResolvingStream.collectAsStateWithLifecycle()
    val resolvingStreamMovieTitle by viewModel.resolvingStreamMovieTitle.collectAsStateWithLifecycle()

    var showMenuOptionsDialog by remember { mutableStateOf(false) }

    // Intercept hardware back button to navigate back through browser views
    BackHandler(enabled = viewMode != BrowserViewMode.HOME) {
        viewMode = BrowserViewMode.HOME
    }

    // Resolving Stream Loading Dialog
    if (isResolvingStream) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF1E2129),
                border = BorderStroke(1.dp, accentColor)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(color = accentColor, strokeWidth = 3.dp)
                    Text(
                        text = "Conectando al servidor de streaming...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    if (!resolvingStreamMovieTitle.isNullOrBlank()) {
                        Text(
                            text = resolvingStreamMovieTitle!!,
                            color = Color(0xFF388AF6),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = "Optimizando reproductor (HLS / MP4 / Ultra Rápido)...",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Browser Actions Menu Sheet
    if (showMenuOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showMenuOptionsDialog = false },
            containerColor = Color(0xFF1E2129),
            title = {
                Text(
                    text = "Opciones del Navegador",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        onClick = {
                            showMenuOptionsDialog = false
                            if (activeSearchQuery.isNotBlank()) {
                                viewModel.resolveAndTransmitMovie(context, activeSearchQuery)
                            } else {
                                Toast.makeText(context, "Realiza una búsqueda primero", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Cast, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Transmitir a Smart TV / Reproductor Externo", color = Color.White)
                    }
                    TextButton(
                        onClick = {
                            showMenuOptionsDialog = false
                            val targetUrl = "https://www.google.com/search?q=" + Uri.encode("ver pelicula $activeSearchQuery online gratis")
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo abrir navegador externo", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Abrir en navegador habitual (Chrome / Firefox)", color = Color.White)
                    }
                    TextButton(
                        onClick = {
                            showMenuOptionsDialog = false
                            viewMode = BrowserViewMode.HOME
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Ir a página de inicio", color = Color.White)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMenuOptionsDialog = false }) {
                    Text("Cerrar", color = accentColor)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1015))
    ) {
        when (viewMode) {
            BrowserViewMode.HOME -> {
                MovieBrowserHomeView(
                    accentColor = accentColor,
                    onOpenSearch = {
                        viewMode = BrowserViewMode.AUTOCOMPLETE
                    },
                    onExecuteSearch = { query ->
                        currentQuery = query
                        activeSearchQuery = query
                        viewModel.searchOmdb(query)
                        viewMode = BrowserViewMode.SEARCH_RESULTS
                    },
                    onSelectShortcut = { shortcutMode ->
                        viewMode = shortcutMode
                    }
                )
            }
            BrowserViewMode.AUTOCOMPLETE -> {
                MovieBrowserAutocompleteView(
                    query = currentQuery,
                    onQueryChange = { currentQuery = it },
                    onBack = { viewMode = BrowserViewMode.HOME },
                    onPerformSearch = { query ->
                        if (query.isNotBlank()) {
                            currentQuery = query
                            activeSearchQuery = query
                            viewModel.searchOmdb(query)
                            viewMode = BrowserViewMode.SEARCH_RESULTS
                        }
                    },
                    availableMovies = movies
                )
            }
            BrowserViewMode.SEARCH_RESULTS -> {
                MovieBrowserSearchResultsView(
                    query = activeSearchQuery,
                    accentColor = accentColor,
                    isSearching = isOmdbSearching,
                    omdbResults = omdbResults,
                    onBack = { viewMode = BrowserViewMode.HOME },
                    onOpenSearchAgain = { viewMode = BrowserViewMode.AUTOCOMPLETE },
                    onOpenMenu = { showMenuOptionsDialog = true },
                    onSelectCarouselItem = { carouselItem ->
                        currentQuery = carouselItem.title
                        activeSearchQuery = carouselItem.title
                        viewModel.searchOmdb(carouselItem.title)
                    },
                    onPlayResult = { result ->
                        viewModel.resolveAndPlayMovie(
                            title = result.title,
                            year = result.year,
                            imdbId = result.imdbId,
                            posterUrl = result.posterUrl,
                            description = result.synopsis,
                            genre = if (result.isSeries) "Serie" else "Cine",
                            serverIndex = result.serverIndex
                        )
                    },
                    onTransmitResult = { result ->
                        viewModel.resolveAndTransmitMovie(
                            context = context,
                            title = result.title,
                            year = result.year,
                            imdbId = result.imdbId,
                            serverIndex = result.serverIndex
                        )
                    }
                )
            }
            BrowserViewMode.NEWS -> {
                BrowserNewsScreen(
                    accentColor = accentColor,
                    onBack = { viewMode = BrowserViewMode.HOME },
                    onSearchMovie = { title ->
                        currentQuery = title
                        activeSearchQuery = title
                        viewModel.searchOmdb(title)
                        viewMode = BrowserViewMode.SEARCH_RESULTS
                    }
                )
            }
            BrowserViewMode.MOVIES_AND_SERIES -> {
                BrowserMoviesAndSeriesScreen(
                    accentColor = accentColor,
                    onBack = { viewMode = BrowserViewMode.HOME },
                    onPlayMovie = { title, year, poster ->
                        viewModel.resolveAndPlayMovie(
                            title = title,
                            year = year,
                            posterUrl = poster,
                            genre = "Cine"
                        )
                    }
                )
            }
            BrowserViewMode.FOOTBALL -> {
                BrowserFootballScreen(
                    accentColor = accentColor,
                    onBack = { viewMode = BrowserViewMode.HOME },
                    onWatchChannel = { channelName ->
                        val iptvMatches = viewModel.iptvList.value.filter {
                            it.name.contains("espn", ignoreCase = true) ||
                            it.name.contains("fox", ignoreCase = true) ||
                            it.name.contains("sport", ignoreCase = true) ||
                            it.name.contains("deporte", ignoreCase = true) ||
                            it.name.contains("directv", ignoreCase = true)
                        }
                        if (iptvMatches.isNotEmpty()) {
                            viewModel.playIptv(iptvMatches.first())
                        } else {
                            viewModel.resolveAndPlayMovie(title = "Transmisión en Vivo: $channelName", genre = "Deportes")
                        }
                    }
                )
            }
            BrowserViewMode.GENERATE_IMAGES -> {
                BrowserGenerateImagesScreen(
                    accentColor = accentColor,
                    onBack = { viewMode = BrowserViewMode.HOME }
                )
            }
            BrowserViewMode.HISTORY -> {
                BrowserHistoryScreen(
                    accentColor = accentColor,
                    viewModel = viewModel,
                    onBack = { viewMode = BrowserViewMode.HOME },
                    onPlayItem = { movie ->
                        viewModel.playMovie(movie)
                    }
                )
            }
            BrowserViewMode.VPN -> {
                BrowserVpnScreen(
                    accentColor = accentColor,
                    onBack = { viewMode = BrowserViewMode.HOME }
                )
            }
            BrowserViewMode.DRIVE -> {
                BrowserDriveScreen(
                    accentColor = accentColor,
                    onBack = { viewMode = BrowserViewMode.HOME },
                    onPlayCustomUrl = { url, title ->
                        val movieItem = MovieItem(
                            id = "custom_${System.currentTimeMillis()}",
                            title = title,
                            genre = "Cloud Drive",
                            url = url,
                            posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=300",
                            description = "Transmisión personalizada desde Cloud Drive"
                        )
                        viewModel.playMovie(movieItem)
                    }
                )
            }
            BrowserViewMode.TOOLS -> {
                BrowserToolsScreen(
                    accentColor = accentColor,
                    onBack = { viewMode = BrowserViewMode.HOME }
                )
            }
        }
    }
}

/**
 * Screen 1: Browser Home (matches image_ab9a8898.png & image_3b54db7b.png)
 */
@Composable
fun MovieBrowserHomeView(
    accentColor: Color,
    onOpenSearch: () -> Unit,
    onExecuteSearch: (String) -> Unit,
    onSelectShortcut: (BrowserViewMode) -> Unit
) {
    val context = LocalContext.current

    val shortcuts = remember {
        listOf(
            BrowserShortcut(
                title = "Noticias",
                icon = Icons.Default.Article,
                iconColor = Color(0xFFE53935),
                bgColor = Color(0xFF2B2B36),
                targetMode = BrowserViewMode.NEWS
            ),
            BrowserShortcut(
                title = "Películas y series",
                icon = Icons.Default.LocalFireDepartment,
                iconColor = Color(0xFFFF5722),
                bgColor = Color(0xFF2B2B36),
                badge = "HOT",
                targetMode = BrowserViewMode.MOVIES_AND_SERIES
            ),
            BrowserShortcut(
                title = "Fútbol",
                icon = Icons.Default.SportsSoccer,
                iconColor = Color.White,
                bgColor = Color(0xFF2B2B36),
                targetMode = BrowserViewMode.FOOTBALL
            ),
            BrowserShortcut(
                title = "Generar imágenes",
                icon = Icons.Outlined.AutoAwesome,
                iconColor = Color(0xFFAB47BC),
                bgColor = Color(0xFF2B2B36),
                targetMode = BrowserViewMode.GENERATE_IMAGES
            ),
            BrowserShortcut(
                title = "Historial",
                icon = Icons.Default.History,
                iconColor = Color.LightGray,
                bgColor = Color(0xFF2B2B36),
                targetMode = BrowserViewMode.HISTORY
            ),
            BrowserShortcut(
                title = "VPN",
                icon = Icons.Default.Shield,
                iconColor = Color(0xFF29B6F6),
                bgColor = Color(0xFF2B2B36),
                targetMode = BrowserViewMode.VPN
            ),
            BrowserShortcut(
                title = "Drive",
                icon = Icons.Default.CloudQueue,
                iconColor = Color(0xFF26A69A),
                bgColor = Color(0xFF2B2B36),
                targetMode = BrowserViewMode.DRIVE
            ),
            BrowserShortcut(
                title = "Herramientas",
                icon = Icons.Default.Apps,
                iconColor = Color(0xFFFFB300),
                bgColor = Color(0xFF2B2B36),
                targetMode = BrowserViewMode.TOOLS
            )
        )
    }

    Scaffold(
        containerColor = Color(0xFF0A0B0E),
        bottomBar = {
            BrowserBottomBar(
                onTabClick = {},
                onBookmarksClick = {
                    Toast.makeText(context, "Marcadores guardados", Toast.LENGTH_SHORT).show()
                },
                onAiChatClick = {
                    onExecuteSearch("Recomiéndame las mejores películas de ciencia ficción")
                },
                onDownloadsClick = {
                    Toast.makeText(context, "Streaming directo optimizado activo", Toast.LENGTH_SHORT).show()
                },
                onProfileClick = {
                    Toast.makeText(context, "Perfil de navegación MedinPlay Hub", Toast.LENGTH_SHORT).show()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Omnibar / Search Bar (matches screenshot)
            Surface(
                color = Color(0xFF1E2129),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color(0xFF303644)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp)
                    .clickable { onOpenSearch() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = Color.LightGray,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Busca o introduce una URL",
                        color = Color.Gray,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onOpenSearch() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voz",
                            tint = Color.LightGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Quick Shortcut Grid (4 columns, 2 rows matching image)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    shortcuts.take(4).forEach { item ->
                        ShortcutItemButton(item = item, onClick = {
                            onSelectShortcut(item.targetMode)
                        })
                    }
                }

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    shortcuts.drop(4).take(4).forEach { item ->
                        ShortcutItemButton(item = item, onClick = {
                            onSelectShortcut(item.targetMode)
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tendencias en la red (matches screenshot)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Tendencias en la red",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                val trendingKeywords = listOf(
                    "Interestelar", "Dune", "Avatar", "Oppenheimer", "Gladiator 2",
                    "Spider-Man", "Batman", "Inception", "Terrifier 3", "Deadpool"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(trendingKeywords) { keyword ->
                        Surface(
                            color = Color(0xFF1A1C24),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF2A2E3D)),
                            modifier = Modifier.clickable { onExecuteSearch(keyword) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF388AF6),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = keyword,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun ShortcutItemButton(
    item: BrowserShortcut,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(item.bgColor)
                    .border(1.dp, Color(0xFF3A3F50), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            if (item.badge != null) {
                Surface(
                    color = Color(0xFFFF3D00),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.offset(x = 6.dp, y = (-4).dp)
                ) {
                    Text(
                        text = item.badge,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            color = Color.LightGray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}

/**
 * Screen 2: Autocomplete View (matches image_411ee0b8.png)
 */
@Composable
fun MovieBrowserAutocompleteView(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onPerformSearch: (String) -> Unit,
    availableMovies: List<MovieItem>
) {
    val predefinedMovieSuggestions = remember {
        listOf(
            "Zona de interés",
            "Zona de Interesse",
            "Intercambiados",
            "The Zone of Interest Gratis",
            "Interlúdio Ver en línea",
            "Interestelar Alta definición",
            "Interestelar Audio Latino",
            "Interestelar Sub Español",
            "Intertwined",
            "Avatar",
            "Avatar El Camino del Agua",
            "Avatar Fire and Ash",
            "La leyenda de Korra",
            "Avatar La leyenda de Aang",
            "Dune",
            "Dune Parte 2",
            "Inception",
            "Gladiator 2",
            "Oppenheimer",
            "Batman El Caballero de la Noche",
            "Avengers Endgame",
            "Matrix Resurrecciones",
            "Spider-Man No Way Home"
        )
    }

    val matchedSuggestions = remember(query, availableMovies) {
        val clean = query.trim().lowercase()
        val pool = (predefinedMovieSuggestions + availableMovies.map { it.title }).distinct()
        if (clean.isBlank()) {
            pool.take(12)
        } else {
            val matching = pool.filter { it.lowercase().contains(clean) }
            if (matching.isNotEmpty()) {
                matching.take(15)
            } else {
                listOf(
                    query,
                    "$query Película Completa",
                    "$query Audio Latino",
                    "$query Sub Español",
                    "$query Ver en línea",
                    "$query 1080p HD"
                )
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF12141A),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buscar",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        bottomBar = {
            Surface(
                color = Color(0xFF1E2129),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, Color(0xFF2E3444)),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = {
                            Text(
                                text = "Escribe el nombre de la película...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF388AF6),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    if (query.isNotBlank()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Limpiar",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = { onPerformSearch(query) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Buscar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
        ) {
            items(matchedSuggestions) { suggestionText ->
                SuggestionRowItem(
                    fullText = suggestionText,
                    highlightText = query.trim(),
                    onClick = { onPerformSearch(suggestionText) },
                    onFillQuery = { onQueryChange(suggestionText) }
                )
            }
        }
    }
}

@Composable
fun SuggestionRowItem(
    fullText: String,
    highlightText: String,
    onClick: () -> Unit,
    onFillQuery: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color(0xFF9E9E9E),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        val annotatedString = buildAnnotatedString {
            if (highlightText.isBlank()) {
                append(fullText)
            } else {
                val lowerFull = fullText.lowercase()
                val lowerHighlight = highlightText.lowercase()
                var startIndex = 0
                while (startIndex < fullText.length) {
                    val matchIndex = lowerFull.indexOf(lowerHighlight, startIndex)
                    if (matchIndex == -1) {
                        append(fullText.substring(startIndex))
                        break
                    } else {
                        if (matchIndex > startIndex) {
                            append(fullText.substring(startIndex, matchIndex))
                        }
                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFF388AF6),
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(fullText.substring(matchIndex, matchIndex + lowerHighlight.length))
                        }
                        startIndex = matchIndex + lowerHighlight.length
                    }
                }
            }
        }

        Text(
            text = annotatedString,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        IconButton(
            onClick = onFillQuery,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Autocompletar",
                tint = Color(0xFFB0B0B0),
                modifier = Modifier
                    .size(18.dp)
                    .rotate(45f)
            )
        }
    }
}

/**
 * Screen 3: Web Search Results View (matches image_f5352667.png & image_eb29ba87.png from Seekee)
 */
@Composable
fun MovieBrowserSearchResultsView(
    query: String,
    accentColor: Color,
    isSearching: Boolean,
    omdbResults: List<OmdbSearchItem>,
    onBack: () -> Unit,
    onOpenSearchAgain: () -> Unit,
    onOpenMenu: () -> Unit,
    onSelectCarouselItem: (RecommendationCarouselItem) -> Unit,
    onPlayResult: (WebMovieResult) -> Unit,
    onTransmitResult: (WebMovieResult) -> Unit
) {
    val displayTitle = remember(query) {
        query.ifBlank { "Avatar" }.replaceFirstChar { it.uppercase() }
    }

    var selectedFilterCategory by remember { mutableStateOf("Todo") }

    // Carousel recommendations (matches image_f5352667.png)
    val carouselRecommendations = remember(query, omdbResults) {
        if (omdbResults.isNotEmpty()) {
            omdbResults.take(6).map { item ->
                RecommendationCarouselItem(
                    title = item.title,
                    year = item.cleanYear.toString(),
                    type = item.type.replaceFirstChar { it.uppercase() },
                    posterUrl = item.cleanPoster
                )
            }
        } else {
            listOf(
                RecommendationCarouselItem("Avatar: El Camino del Agua", "2022", "Películas", "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=300"),
                RecommendationCarouselItem("Avatar", "2009", "Películas", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=300"),
                RecommendationCarouselItem("Avatar: Fire and Ash", "2025", "Películas", "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=300"),
                RecommendationCarouselItem("La leyenda de Korra", "2014", "Series", "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=300")
            )
        }
    }

    // Direct verified 200 OK streams for playback
    val verifiedStreamUrls = remember {
        listOf(
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8",
            "https://vjs.zencdn.net/v/oceans.mp4",
            "https://assets.afcdn.com/video49/20210722/v_645516.m3u8"
        )
    }

    // Web Provider Results matching image_f5352667.png & image_eb29ba87.png (play.cuevana19.com, es.cuevana4br.com, etc.)
    val webResults = remember(query, omdbResults) {
        val basePoster = omdbResults.firstOrNull { it.cleanPoster.isNotBlank() }?.cleanPoster
            ?: "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=300"
        val baseYear = omdbResults.firstOrNull()?.cleanYear ?: 2009
        val baseImdb = omdbResults.firstOrNull()?.imdbID.orEmpty()

        listOf(
            WebMovieResult(
                providerDomain = "play.cuevana19.com",
                audioBadge = "[Audio Latino]",
                title = "$displayTitle: Fire and Ash",
                metadataLine = "Película · PG-13 · 2025 · Drama · Ciencia ficción · Suspenso · Aventura · Acción · Fantasía",
                synopsis = "La familia de Jake y Neytiri lidia con el dolor después de la muerte de Neteyam y se encuentra con la temible tribu del Pueblo de las Cenizas.",
                posterUrl = basePoster,
                year = 2025,
                imdbId = baseImdb,
                serverIndex = 0,
                directStreamUrl = verifiedStreamUrls[0]
            ),
            WebMovieResult(
                providerDomain = "play.cuevana19.com",
                audioBadge = "[Audio Latino]",
                title = displayTitle,
                metadataLine = "Película · PG-13 · $baseYear · Acción · Aventura · Fantasía · Ciencia ficción",
                synopsis = "Cuando su hermano muere en un robo, la marina parapléjica, Jake Sully, decide tomar su lugar en una misión en el mundo lejano de Pandora.",
                posterUrl = basePoster,
                year = baseYear,
                imdbId = baseImdb,
                serverIndex = 1,
                directStreamUrl = verifiedStreamUrls[1]
            ),
            WebMovieResult(
                providerDomain = "es.cuevana4br.com",
                audioBadge = "[Audio Latino]",
                title = "La leyenda de Korra",
                metadataLine = "TV Serie · TV-PG · 2014 · Comedia · Drama · Ciencia ficción · Animación · Familiar · Aventura",
                synopsis = "Avatar Korra lucha para mantener a la ciudad de la República a salvo de las fuerzas oscuras de los espíritus.",
                posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=300",
                year = 2014,
                imdbId = "tt1695360",
                serverIndex = 2,
                isSeries = true,
                episodes = listOf(1, 2, 3, 4, 5, 12, 13),
                directStreamUrl = verifiedStreamUrls[2]
            ),
            WebMovieResult(
                providerDomain = "ww3.pelispluslatino.io",
                audioBadge = "[Audio Latino]",
                title = "$displayTitle: La leyenda de Aang Temporada 2",
                metadataLine = "TV Serie · TV-PG · 2026 · Drama · Familiar · Comedia · Misterio · Aventura · Acción",
                synopsis = "Después de evitar que la Nación del Fuego destruyera el Reino de la Tierra, Aang continúa su entrenamiento maestro.",
                posterUrl = basePoster,
                year = 2026,
                imdbId = baseImdb,
                serverIndex = 3,
                isSeries = true,
                episodes = listOf(1, 2, 3, 4, 5, 6, 7, 8),
                directStreamUrl = verifiedStreamUrls[3]
            ),
            WebMovieResult(
                providerDomain = "ww1.cuevana3.ch",
                audioBadge = "[Sub Español]",
                title = displayTitle,
                metadataLine = "Película · 1080p FHD · $baseYear · Audio Original con Subtítulos",
                synopsis = "Versión en audio original inglés remasterizada con subtítulos en español neutro sin cortes.",
                posterUrl = basePoster,
                year = baseYear,
                imdbId = baseImdb,
                serverIndex = 4,
                directStreamUrl = verifiedStreamUrls[4]
            )
        )
    }

    val filteredWebResults = remember(webResults, selectedFilterCategory) {
        when (selectedFilterCategory) {
            "Películas" -> webResults.filter { !it.isSeries }
            "Series" -> webResults.filter { it.isSeries }
            else -> webResults
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F1015),
        bottomBar = {
            Surface(
                color = Color(0xFF14161E),
                border = BorderStroke(1.dp, Color(0xFF232734)),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.5.dp, Color.White),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onBack() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "1",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFF1E2129),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E3444)),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .height(38.dp)
                            .clickable { onOpenSearchAgain() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = displayTitle,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(onClick = onOpenMenu) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Opciones",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: "✦ Te recomendamos estos contenidos según tu búsqueda" (matches image_f5352667.png)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF29B6F6),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Te recomendamos estos contenidos según tu búsqueda",
                            color = Color(0xFF9EA3B0),
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Filter Chips: [Todo] [Películas] [Series] (matches image_f5352667.png)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Todo", "Películas", "Series").forEach { filterLabel ->
                            val isSelected = selectedFilterCategory == filterLabel
                            Surface(
                                color = if (isSelected) Color(0xFF2979FF) else Color(0xFF1E2129),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF2979FF) else Color(0xFF2E3444)),
                                modifier = Modifier.clickable { selectedFilterCategory = filterLabel }
                            ) {
                                Text(
                                    text = filterLabel,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Horizontal Carousel of Movies (matches image_f5352667.png)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(carouselRecommendations) { recItem ->
                            CarouselMovieItemCard(
                                item = recItem,
                                onClick = { onSelectCarouselItem(recItem) }
                            )
                        }
                    }
                }
            }

            // Wikipedia Card (matches image_f5352667.png)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D26)),
                    border = BorderStroke(1.dp, Color(0xFF2B2F3D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$displayTitle (film) - Wikipedia",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$displayTitle is an acclaimed epic production set in futuristic and imaginative worlds, recognized for its groundbreaking visual effects and captivating narrative.",
                            color = Color(0xFFB0B3C0),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "en.wikipedia.org",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Searching indicator
            if (isSearching) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF388AF6))
                            Text(
                                text = "Buscando enlaces y servidores disponibles...",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Web Streaming Provider Cards (matches image_f5352667.png & image_eb29ba87.png)
            items(filteredWebResults) { result ->
                WebStreamingProviderCard(
                    result = result,
                    accentColor = accentColor,
                    onPlay = { onPlayResult(result) },
                    onTransmit = { onTransmitResult(result) }
                )
            }
        }
    }
}

@Composable
fun CarouselMovieItemCard(
    item: RecommendationCarouselItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF222530))
                .border(1.dp, Color(0xFF323646), RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // "Películas" pill badge (matches image_f5352667.png)
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .padding(6.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = item.type,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = item.year,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}

/**
 * Web Streaming Card Component (matches image_eb29ba87.png)
 */
@Composable
fun WebStreamingProviderCard(
    result: WebMovieResult,
    accentColor: Color,
    onPlay: () -> Unit,
    onTransmit: () -> Unit
) {
    var selectedEpisode by remember { mutableStateOf(result.episodes.firstOrNull() ?: 1) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D26)),
        border = BorderStroke(1.dp, Color(0xFF2B2F3D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = result.audioBadge,
                            color = Color(0xFF388AF6),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = result.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = result.metadataLine,
                        color = Color(0xFF9EA3B0),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = result.synopsis,
                        color = Color(0xFFCFD3DF),
                        fontSize = 12.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 104.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF222530))
                        .border(1.dp, Color(0xFF363B4E), RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = result.posterUrl,
                        contentDescription = result.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Episode selection row if it is a TV series (matches image_eb29ba87.png)
            if (result.isSeries && result.episodes.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(result.episodes) { epNum ->
                        val isEpSelected = selectedEpisode == epNum
                        Surface(
                            color = if (isEpSelected) Color(0xFF2979FF) else Color(0xFF252936),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isEpSelected) Color(0xFF2979FF) else Color(0xFF3B4052)),
                            modifier = Modifier
                                .width(44.dp)
                                .height(36.dp)
                                .clickable { selectedEpisode = epNum }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$epNum",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Dark full-width "Reproducir >" Button (matches Seekee)
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262934)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF3B4052)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = if (result.isSeries) "Reproducir Episodio $selectedEpisode >" else "Reproducir >",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Provider domain attribution footer with domain icon and three dots menu (matches image_eb29ba87.png)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0091EA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "c",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = result.providerDomain,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onTransmit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cast,
                        contentDescription = "Transmitir",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onTransmit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Más",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 1. Noticias Screen
 */
@Composable
fun BrowserNewsScreen(
    accentColor: Color,
    onBack: () -> Unit,
    onSearchMovie: (String) -> Unit
) {
    val newsList = remember {
        listOf(
            Triple(
                "Avatar 3: Fire and Ash revela la tribu de las cenizas",
                "James Cameron confirma que la tercera entrega explorará Na'vis más oscuros y agresivos que desafían a Jake Sully.",
                "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=400"
            ),
            Triple(
                "Dune: Parte 2 supera récord de taquilla global",
                "La adaptación de Denis Villeneuve se consagra como la película de ciencia ficción más taquillera del año y prepara el camino para Messiah.",
                "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=400"
            ),
            Triple(
                "Gladiator 2: Llega el primer tráiler oficial en alta definición",
                "Ridley Scott regresa al Coliseo Romano con Paul Mescal y Pedro Pascal en una batalla épica por la gloria de Roma.",
                "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?q=80&w=400"
            ),
            Triple(
                "Nuevos servidores de streaming 4K y Audio Latino añadidos",
                "MedinPlay Hub actualiza su infraestructura para brindar enlaces de alta velocidad sin cortes ni publicidad invasiva.",
                "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=400"
            )
        )
    }

    Scaffold(
        containerColor = Color(0xFF0F1015),
        topBar = {
            BrowserSubScreenTopBar(title = "Noticias de Cine y Series", onBack = onBack)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(newsList) { (headline, snippet, imgUrl) ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D26)),
                    border = BorderStroke(1.dp, Color(0xFF2B2F3D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color(0xFF252936))
                        ) {
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = headline,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color(0xFFE53935),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .padding(10.dp)
                                    .align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = "EXCLUSIVA",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = headline,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = snippet,
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Hace 2 horas • MedinPlay News", color = Color.Gray, fontSize = 11.sp)
                                TextButton(onClick = { onSearchMovie(headline.substringBefore(":")) }) {
                                    Text("Buscar película >", color = Color(0xFF388AF6), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. Películas y Series Screen
 */
@Composable
fun BrowserMoviesAndSeriesScreen(
    accentColor: Color,
    onBack: () -> Unit,
    onPlayMovie: (String, Int, String) -> Unit
) {
    val catalog = remember {
        listOf(
            Triple("Avatar: El Camino del Agua", 2022, "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=300"),
            Triple("Interestelar", 2014, "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=300"),
            Triple("Dune: Parte 2", 2024, "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=300"),
            Triple("Oppenheimer", 2023, "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?q=80&w=300"),
            Triple("Gladiator", 2000, "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?q=80&w=300"),
            Triple("Inception", 2010, "https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=300")
        )
    }

    Scaffold(
        containerColor = Color(0xFF0F1015),
        topBar = {
            BrowserSubScreenTopBar(title = "Películas y Series Recomendadas", onBack = onBack)
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(catalog) { (title, year, poster) ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D26)),
                    border = BorderStroke(1.dp, Color(0xFF2E3444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color(0xFF222530))
                        ) {
                            AsyncImage(
                                model = poster,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color(0xFFFF5722),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .padding(6.dp)
                                    .align(Alignment.TopEnd)
                            ) {
                                Text(
                                    text = "$year",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Audio Latino • 1080p",
                                color = Color(0xFF29B6F6),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onPlayMovie(title, year, poster) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262934)),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF3B4052)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Reproducir >", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Fútbol Screen
 */
@Composable
fun BrowserFootballScreen(
    accentColor: Color,
    onBack: () -> Unit,
    onWatchChannel: (String) -> Unit
) {
    val matches = remember {
        listOf(
            Triple("Real Madrid vs Barcelona", "EN VIVO 74' [ 2 - 1 ]", "LaLiga Española"),
            Triple("Manchester City vs Bayern Múnich", "Hoy 21:00", "UEFA Champions League"),
            Triple("Boca Juniors vs River Plate", "Mañana 19:30", "Copa Libertadores"),
            Triple("PSG vs Inter de Milán", "Finalizado [ 1 - 1 ]", "Amistoso Internacional"),
            Triple("Arsenal vs Liverpool", "Domingo 17:30", "Premier League")
        )
    }

    Scaffold(
        containerColor = Color(0xFF0F1015),
        topBar = {
            BrowserSubScreenTopBar(title = "Fútbol en Vivo y Agenda", onBack = onBack)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(matches) { (teams, timeScore, tournament) ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D26)),
                    border = BorderStroke(1.dp, Color(0xFF2B2F3D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = tournament, color = Color(0xFF388AF6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (timeScore.contains("EN VIVO")) {
                                Surface(color = Color(0xFFE53935), shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = "● EN VIVO",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = teams, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = timeScore, color = Color.LightGray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onWatchChannel(teams) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ver Transmisión en Vivo", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. Generar Imágenes Screen (AI Studio)
 */
@Composable
fun BrowserGenerateImagesScreen(
    accentColor: Color,
    onBack: () -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val gallery = remember {
        mutableStateListOf(
            "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=400",
            "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=400",
            "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=400"
        )
    }

    Scaffold(
        containerColor = Color(0xFF0F1015),
        topBar = {
            BrowserSubScreenTopBar(title = "Generador de Imágenes y Pósters IA", onBack = onBack)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D26)),
                border = BorderStroke(1.dp, Color(0xFF2E3444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Crea tu propio póster cinematográfico en 4K",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { Text("Ej: Astronauta en planeta alienígena con anillos y luces de neón...", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFAB47BC),
                            unfocusedBorderColor = Color(0xFF3A3F50)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (prompt.isNotBlank()) {
                                isGenerating = true
                                scope.launch {
                                    delay(2000)
                                    gallery.add(0, "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?q=80&w=400")
                                    isGenerating = false
                                    Toast.makeText(context, "¡Póster generado con éxito!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAB47BC)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isGenerating
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generando en 4K Ultra HD...")
                        } else {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generar Imagen IA", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Text(
                text = "Pósters y fondos creados",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            gallery.forEach { imgUrl ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D26)),
                    border = BorderStroke(1.dp, Color(0xFF2E3444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color(0xFF20232E))
                        ) {
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Póster Cinemático 4K", color = Color.LightGray, fontSize = 13.sp)
                            Button(
                                onClick = { Toast.makeText(context, "Imagen guardada en Galería", Toast.LENGTH_SHORT).show() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E3444)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Guardar", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. Historial Screen
 */
@Composable
fun BrowserHistoryScreen(
    accentColor: Color,
    viewModel: MediaViewModel,
    onBack: () -> Unit,
    onPlayItem: (MovieItem) -> Unit
) {
    val history = remember {
        listOf(
            Triple("Avatar: El Camino del Agua", "Visto 85% • Hace 2 horas", "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=300"),
            Triple("Interestelar", "Visto 42% • Ayer", "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=300"),
            Triple("Dune: Parte 2", "Completada • Hace 3 días", "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=300")
        )
    }

    Scaffold(
        containerColor = Color(0xFF0F1015),
        topBar = {
            BrowserSubScreenTopBar(title = "Historial de Reproducción", onBack = onBack)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(history) { (title, subtitle, poster) ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D26)),
                    border = BorderStroke(1.dp, Color(0xFF2E3444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 60.dp, height = 85.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222530))
                        ) {
                            AsyncImage(
                                model = poster,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val movieItem = MovieItem(
                                        id = "hist_${title.hashCode()}",
                                        title = title,
                                        genre = "Cine",
                                        url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                                        posterUrl = poster,
                                        description = "Reanudando reproducción desde historial"
                                    )
                                    onPlayItem(movieItem)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Text("Reanudar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 6. VPN Screen
 */
@Composable
fun BrowserVpnScreen(
    accentColor: Color,
    onBack: () -> Unit
) {
    var isConnected by remember { mutableStateOf(true) }
    var selectedServer by remember { mutableStateOf("México (CDMX)") }
    val context = LocalContext.current

    val servers = remember {
        listOf(
            "México (CDMX) • 32 ms (Recomendado Audio Latino)",
            "España (Madrid) • 76 ms (Servidores Castellano)",
            "USA (Miami Ultra CDN) • 44 ms (Velocidad 4K)",
            "Argentina (Buenos Aires) • 55 ms",
            "Chile (Santiago) • 62 ms"
        )
    }

    Scaffold(
        containerColor = Color(0xFF0F1015),
        topBar = {
            BrowserSubScreenTopBar(title = "VPN de Streaming y Desbloqueo", onBack = onBack)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D26)),
                border = BorderStroke(1.dp, if (isConnected) Color(0xFF00E676) else Color(0xFFE53935)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Shield else Icons.Default.ShieldMoon,
                        contentDescription = null,
                        tint = if (isConnected) Color(0xFF00E676) else Color.Gray,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isConnected) "VPN CONECTADA - PROTEGIDO" else "VPN DESCONECTADA",
                        color = if (isConnected) Color(0xFF00E676) else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Servidor actual: $selectedServer",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isConnected = !isConnected
                            Toast.makeText(context, if (isConnected) "Conectado a $selectedServer" else "VPN Desconectada", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isConnected) Color(0xFFE53935) else Color(0xFF00E676)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isConnected) "Desconectar VPN" else "Conectar VPN Rápida",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(text = "Seleccionar servidor de streaming", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)

            servers.forEach { serverName ->
                val isSelected = selectedServer.startsWith(serverName.substringBefore(" •"))
                Surface(
                    color = if (isSelected) Color(0xFF2979FF).copy(alpha = 0.2f) else Color(0xFF1B1D26),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF2979FF) else Color(0xFF2E3444)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedServer = serverName }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedServer = serverName },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2979FF))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = serverName, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * 7. Drive Screen
 */
@Composable
fun BrowserDriveScreen(
    accentColor: Color,
    onBack: () -> Unit,
    onPlayCustomUrl: (String, String) -> Unit
) {
    var customUrl by remember { mutableStateOf("") }
    var customTitle by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        containerColor = Color(0xFF0F1015),
        topBar = {
            BrowserSubScreenTopBar(title = "Cloud Drive y Mis Enlaces", onBack = onBack)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D26)),
                border = BorderStroke(1.dp, Color(0xFF26A69A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Reproducir enlace de video directo o HLS",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        placeholder = { Text("Nombre del video o película", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        placeholder = { Text("https://servidor.com/video.m3u8 o .mp4", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (customUrl.isNotBlank()) {
                                onPlayCustomUrl(customUrl, customTitle.ifBlank { "Video Personalizado" })
                            } else {
                                Toast.makeText(context, "Ingresa una URL válida", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reproducir en MedinPlay Player", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(text = "Archivos guardados en tu nube", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)

            val savedFiles = listOf(
                "Avatar 4K Remaster • HLS Stream",
                "Interestelar Dolby Atmos • MP4",
                "Dune Parte 2 • Servidor Latino"
            )

            savedFiles.forEach { file ->
                Surface(
                    color = Color(0xFF1B1D26),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E3444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VideoFile, contentDescription = null, tint = Color(0xFF26A69A))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = file, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onPlayCustomUrl("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", file) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF26A69A))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 8. Herramientas Screen
 */
@Composable
fun BrowserToolsScreen(
    accentColor: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var speedTestResult by remember { mutableStateOf<String?>(null) }
    var isTestingSpeed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFF0F1015),
        topBar = {
            BrowserSubScreenTopBar(title = "Herramientas de Streaming", onBack = onBack)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Speed test card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D26)),
                border = BorderStroke(1.dp, Color(0xFFFFB300)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Test de Velocidad de Streaming", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Comprueba si tu red soporta streaming en 4K Ultra HD sin cortes.", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (speedTestResult != null) {
                        Surface(
                            color = Color(0xFF263238),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = speedTestResult!!,
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Button(
                        onClick = {
                            isTestingSpeed = true
                            scope.launch {
                                delay(1800)
                                speedTestResult = "Velocidad: 84.5 Mbps • Ping: 18 ms\nCalidad recomendada: 4K UHD / 1080p 60fps"
                                isTestingSpeed = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isTestingSpeed
                    ) {
                        if (isTestingSpeed) {
                            CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Midiendo velocidad...", color = Color.Black)
                        } else {
                            Text("Iniciar Test de Red", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Cache cleaner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D26)),
                border = BorderStroke(1.dp, Color(0xFF2E3444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Limpiar Caché del Reproductor", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Libera memoria RAM y elimina búfer residual para resolver problemas de carga.", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { Toast.makeText(context, "Caché de video liberado (148 MB)", Toast.LENGTH_SHORT).show() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Limpiar Caché (148 MB)", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun BrowserSubScreenTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(Color(0xFF14161E))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Bottom Bar for Browser Home (matches image_ab9a8898.png)
 */
@Composable
fun BrowserBottomBar(
    onTabClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onAiChatClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        color = Color(0xFF14161E),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = BorderStroke(1.dp, Color(0xFF222634)),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrowserBottomBarItem(
                label = "Ventanas",
                icon = {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.5.dp, Color.White),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "1",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                onClick = onTabClick
            )

            BrowserBottomBarItem(
                label = "Marcadores",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(22.dp)
                    )
                },
                onClick = onBookmarksClick
            )

            BrowserBottomBarItem(
                label = "Chat con IA",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF388AF6),
                        modifier = Modifier.size(22.dp)
                    )
                },
                onClick = onAiChatClick
            )

            BrowserBottomBarItem(
                label = "Descargar",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(22.dp)
                    )
                },
                onClick = onDownloadsClick
            )

            BrowserBottomBarItem(
                label = "Mi perfil",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(22.dp)
                    )
                },
                onClick = onProfileClick
            )
        }
    }
}

@Composable
fun BrowserBottomBarItem(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
