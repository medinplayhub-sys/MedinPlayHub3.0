package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.omdb.OmdbMovieDetail
import com.example.data.omdb.OmdbSearchItem
import com.example.data.openrouter.MovieAiAnalysis
import com.example.data.openrouter.MovieAiRecommendation
import com.example.data.resolver.MovieStreamSource
import com.example.models.MovieItem

@Composable
fun OmdbMovieCard(
    movie: OmdbSearchItem,
    accentColor: Color,
    onClick: () -> Unit,
    onPlayNow: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
        border = BorderStroke(1.dp, SpaceBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .background(SpaceDarkBackground)
            ) {
                if (movie.cleanPoster.isNotBlank()) {
                    AsyncImage(
                        model = movie.cleanPoster,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Movie,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Subtle gradient overlay at bottom of poster
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                // Top-Left Badge: Release Year
                if (movie.year.isNotBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = movie.year,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Top-Right Badge: Quality HD
                Surface(
                    color = Color(0xFF1E88E5).copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "HD",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Bottom-Right Quick Play Button
                if (onPlayNow != null) {
                    IconButton(
                        onClick = onPlayNow,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(36.dp)
                            .background(accentColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir",
                            tint = Color(0xFF0F0D0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "★ HD 1080p",
                        color = Color(0xFFFFB300),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ver detalles →",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun OmdbMovieListItem(
    movie: OmdbSearchItem,
    accentColor: Color,
    onClick: () -> Unit,
    onPlayNow: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
        border = BorderStroke(1.dp, SpaceBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp, 76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SpaceDarkBackground)
            ) {
                if (movie.cleanPoster.isNotBlank()) {
                    AsyncImage(
                        model = movie.cleanPoster,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Movie,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (movie.year.isNotBlank()) {
                        Text(
                            text = movie.year,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Text(text = "•", color = Color.Gray, fontSize = 12.sp)
                    }
                    Text(
                        text = "Full HD",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (onPlayNow != null) {
                IconButton(
                    onClick = onPlayNow,
                    modifier = Modifier
                        .size(38.dp)
                        .background(accentColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color(0xFF0F0D0B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = "Ver", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun OmdbMovieDetailDialog(
    detail: OmdbMovieDetail,
    aiAnalysis: MovieAiAnalysis?,
    isLoadingAi: Boolean,
    aiError: String?,
    accentColor: Color,
    onDismiss: () -> Unit,
    onRequestAiAnalysis: () -> Unit,
    onSelectRecommendation: (String) -> Unit,
    onPlayCustomStream: (String) -> Unit,
    onPlayServerIndex: ((Int) -> Unit)? = null,
    onAddToCatalog: (String) -> Unit = {},
    onTransmitMovie: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current

    val defaultServers = remember {
        listOf(
            Triple("🇲🇽 Español Latino (Servidor 1 - Rápido)", "1080p FHD", 0),
            Triple("🇲🇽 Español Latino (Servidor 2 - HD)", "720p HD", 1),
            Triple("🇪🇸 Castellano (Servidor 3)", "1080p FHD", 2),
            Triple("🌐 Subtitulado (Servidor 4 - Original)", "4K UHD", 3)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = SpaceCardBg,
            border = BorderStroke(1.dp, SpaceBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(SpaceDarkBackground)
                ) {
                    if (detail.cleanPoster.isNotBlank()) {
                        AsyncImage(
                            model = detail.cleanPoster,
                            contentDescription = detail.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = accentColor, modifier = Modifier.size(64.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Transparent,
                                        SpaceCardBg
                                    )
                                )
                            )
                    )

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }

                    // Quality Badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "STREAMING FULL HD",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Details Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = detail.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Badges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (detail.year.isNotBlank()) {
                            Surface(
                                color = SpaceDarkBackground,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, SpaceBorderColor)
                            ) {
                                Text(
                                    text = detail.year,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (detail.runtime.isNotBlank() && !detail.runtime.equals("N/A", ignoreCase = true)) {
                            Surface(
                                color = SpaceDarkBackground,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, SpaceBorderColor)
                            ) {
                                Text(
                                    text = detail.runtime,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (detail.cleanRating.isNotBlank()) {
                            Surface(
                                color = Color(0xFFFFB300).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "★ ${detail.cleanRating}/10",
                                    color = Color(0xFFFFB300),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (detail.rated.isNotBlank() && !detail.rated.equals("N/A", ignoreCase = true)) {
                            Surface(
                                color = if (detail.rated.contains("R", ignoreCase = true) || detail.rated.contains("18")) Color.Red.copy(alpha = 0.8f) else SpaceDarkBackground,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, SpaceBorderColor)
                            ) {
                                Text(
                                    text = detail.rated,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (detail.genre.isNotBlank() && !detail.genre.equals("N/A", ignoreCase = true)) {
                        Text(
                            text = "🎭 ${detail.genre}",
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (detail.director.isNotBlank() && !detail.director.equals("N/A", ignoreCase = true)) {
                        Text(
                            text = "🎬 Dirección: ${detail.director}",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }

                    if (detail.actors.isNotBlank() && !detail.actors.equals("N/A", ignoreCase = true)) {
                        Text(
                            text = "👥 Reparto: ${detail.actors}",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }

                    // --- SERVIDORES DE STREAMING DISPONIBLES ---
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SpaceDarkBackground),
                        border = BorderStroke(1.dp, SpaceBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Dvr, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Servidores de Streaming en Tiempo Real:",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            defaultServers.forEach { (serverName, quality, index) ->
                                Surface(
                                    color = SpaceCardBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SpaceBorderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (onPlayServerIndex != null) {
                                                onPlayServerIndex(index)
                                            } else {
                                                onPlayCustomStream("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
                                            }
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = serverName,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Calidad: $quality • Reproducción directa y transmisión",
                                                color = Color.Gray,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (onTransmitMovie != null) {
                                                        onTransmitMovie(index)
                                                    } else {
                                                        val fallbackUrl = when (index) {
                                                            1 -> "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
                                                            2 -> "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8"
                                                            3 -> "https://vjs.zencdn.net/v/oceans.mp4"
                                                            else -> "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                                                        }
                                                        launchExternalStreamCast(context, fallbackUrl, "${detail.title} - $serverName")
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Tv,
                                                    contentDescription = "Transmitir con app externa",
                                                    tint = Color.LightGray,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    if (onPlayServerIndex != null) {
                                                        onPlayServerIndex(index)
                                                    } else {
                                                         onPlayCustomStream("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
                                                    }
                                                    onDismiss()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Reproducir servidor",
                                                    tint = accentColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons Row: Direct Play & Cast
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (onPlayServerIndex != null) {
                                    onPlayServerIndex(0)
                                } else {
                                    onPlayCustomStream("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
                                }
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0F0D0B))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reproducir Ahora", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (onTransmitMovie != null) {
                                    onTransmitMovie(0)
                                } else {
                                    val streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                                    launchExternalStreamCast(context, streamUrl, detail.title)
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, SpaceBorderColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Transmitir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    HorizontalDivider(color = SpaceBorderColor, thickness = 1.dp)

                    // Synopsis
                    Text(
                        text = "Sinopsis",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (detail.cleanPlot.isNotBlank()) detail.cleanPlot else "Sin sinopsis disponible para esta película.",
                        color = Color(0xFFD1C4B9),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    // --- IA RECOMENDACIONES & SAGA ---
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SpaceDarkBackground),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Análisis de Franquicia y Saga",
                                        color = accentColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (aiAnalysis != null) {
                                    IconButton(
                                        onClick = onRequestAiAnalysis,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Regenerar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            if (aiAnalysis == null && !isLoadingAi) {
                                Text(
                                    text = "Consulta el análisis inteligente para descubrir la saga completa y al menos 3 películas recomendadas similares.",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                                Button(
                                    onClick = onRequestAiAnalysis,
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, accentColor),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analizar Película con IA", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            } else if (isLoadingAi) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                    ) {
                                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Analizando saga y recomendaciones...", color = Color.LightGray, fontSize = 13.sp)
                                }
                            } else if (aiAnalysis != null) {
                                // SAGA & FRANQUICIA
                                Surface(
                                    color = SpaceCardBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SpaceBorderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "🍿 Franquicia / Secuelas:",
                                            color = accentColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = aiAnalysis.displayFranchise,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // SINOPSIS EN ESPAÑOL
                                if (aiAnalysis.synopsis.isNotBlank()) {
                                    Surface(
                                        color = SpaceCardBg,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, SpaceBorderColor),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "📖 Sinopsis en Español:",
                                                color = Color(0xFFC67C65),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = aiAnalysis.synopsis,
                                                color = Color(0xFFE0E0E0),
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }

                                // RECOMENDACIONES (AL MENOS 3)
                                if (aiAnalysis.recommendations.isNotEmpty()) {
                                    Text(
                                        text = "🎯 Películas recomendadas / similares:",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    aiAnalysis.recommendations.forEach { rec ->
                                        Surface(
                                            color = SpaceCardBg,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, SpaceBorderColor),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSelectRecommendation(rec.title) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "• ${rec.title} ${if (rec.displayYear.isNotBlank()) "(${rec.displayYear})" else ""}",
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (rec.reason.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = rec.reason,
                                                            color = Color.LightGray,
                                                            fontSize = 11.sp,
                                                            lineHeight = 15.sp
                                                        )
                                                    }
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = "Buscar",
                                                    tint = accentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (aiAnalysis.curiosities != null && aiAnalysis.curiosities.isNotEmpty()) {
                                    Text(
                                        text = "💡 Datos curiosos:",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    aiAnalysis.curiosities.forEach { cur ->
                                        Text(text = "• $cur", color = Color(0xFFB0BEC5), fontSize = 11.sp)
                                    }
                                }
                            }

                            if (aiError != null) {
                                Text(
                                    text = "⚠️ $aiError",
                                    color = Color(0xFFEF5350),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
