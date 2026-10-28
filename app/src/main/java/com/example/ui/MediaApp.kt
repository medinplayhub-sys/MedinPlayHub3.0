package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import java.io.File
import java.io.FileOutputStream
import com.example.models.IptvItem
import com.example.models.MovieItem
import com.example.models.RadioItem
import com.example.models.UserProfile
import kotlinx.coroutines.delay

// Sophisticated Dark Palette for Streaming
val SpaceDarkBackground = Color(0xFF0F0D0B)
val SpaceCardBg = Color(0xFF24211E)
val SpaceBorderColor = Color(0xFF3D3834)

data class ColorThemeOption(val hex: String, val name: String, val category: String)

val MetallicColorsList = listOf(
    ColorThemeOption("#D4AF37", "Oro Metálico", "Metálicos"),
    ColorThemeOption("#C0C0C0", "Plata Metálica", "Metálicos"),
    ColorThemeOption("#B87333", "Cobre Metálico", "Metálicos"),
    ColorThemeOption("#E5E4E2", "Platino", "Metálicos"),
    ColorThemeOption("#B76E79", "Oro Rosa", "Metálicos")
)

val FluorescentColorsList = listOf(
    ColorThemeOption("#00FF66", "Verde Neón", "Fluorescentes"),
    ColorThemeOption("#00F0FF", "Cian Neón", "Fluorescentes"),
    ColorThemeOption("#FF007F", "Rosa Neón", "Fluorescentes"),
    ColorThemeOption("#FFEE00", "Amarillo Eléctrico", "Fluorescentes"),
    ColorThemeOption("#B000FF", "Púrpura Neón", "Fluorescentes")
)

val GradientColorsList = listOf(
    ColorThemeOption("#A1C4FD", "Azul Suave", "Degradados"),
    ColorThemeOption("#E0C3FC", "Lavanda Tenue", "Degradados"),
    ColorThemeOption("#FFD1DC", "Pastel Rosa", "Degradados"),
    ColorThemeOption("#84FAB0", "Menta Tenue", "Degradados"),
    ColorThemeOption("#FCCB90", "Durazno Tenue", "Degradados")
)

val ClassicColorsList = listOf(
    ColorThemeOption("#D1C4B9", "Avena", "Clásicos"),
    ColorThemeOption("#C67C65", "Terracota", "Clásicos"),
    ColorThemeOption("#8A9A86", "Salvia", "Clásicos"),
    ColorThemeOption("#708D9C", "Pizarra", "Clásicos"),
    ColorThemeOption("#BC8F8F", "Rosa Muteado", "Clásicos"),
    ColorThemeOption("#CDBA96", "Ocre", "Clásicos"),
    ColorThemeOption("#8B7D6B", "Bronce", "Clásicos")
)

val AllColorThemeOptions = MetallicColorsList + FluorescentColorsList + GradientColorsList + ClassicColorsList
val AccentColorsList = AllColorThemeOptions.map { it.hex }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaApp(
    viewModel: MediaViewModel = viewModel(),
    onExitApp: () -> Unit = {},
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {}
) {
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val isAdminMode by viewModel.isAdminMode.collectAsStateWithLifecycle()
    val isAppActivated by viewModel.isAppActivated.collectAsStateWithLifecycle()

    val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()
    val skippedVersion by viewModel.skippedVersion.collectAsStateWithLifecycle()
    val isDownloadingUpdate by viewModel.isDownloadingUpdate.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadError by viewModel.downloadError.collectAsStateWithLifecycle()
    var updateDismissed by remember(appUpdateInfo?.versionCode) { mutableStateOf(false) }
    var showVersionModal by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val hasUpdateAvailable = appUpdateInfo != null &&
        appUpdateInfo!!.versionCode > viewModel.currentLocalVersionCode &&
        (appUpdateInfo!!.isMandatoryUpdate || appUpdateInfo!!.versionCode > skippedVersion)

    if ((hasUpdateAvailable && !updateDismissed) || showVersionModal) {
        val infoToShow = appUpdateInfo ?: com.example.models.AppUpdateInfo(
            versionCode = viewModel.currentLocalVersionCode,
            versionName = "1.0.0",
            releaseNotes = "La aplicación se encuentra en la versión v1.0.0. No hay nuevas actualizaciones pendientes en ConfiguracionGlobal.",
            isForceUpdate = false
        )
        AppUpdateAlertDialog(
            updateInfo = infoToShow,
            isDownloading = isDownloadingUpdate,
            progress = downloadProgress,
            errorMessage = downloadError,
            onDownloadClick = {
                if (infoToShow.apkUrl.isNotBlank()) {
                    viewModel.downloadAndInstallUpdate(context, infoToShow.apkUrl)
                } else {
                    Toast.makeText(context, "No hay URL de APK configurada para descargar.", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = {
                if (!infoToShow.isMandatoryUpdate) {
                    viewModel.skipVersion(infoToShow.versionCode)
                }
                updateDismissed = true
                showVersionModal = false
            }
        )
    }

    val profileColor = remember(currentProfile) {
        val hex = currentProfile?.accentColorHex ?: "#D1C4B9"
        try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            Color(0xFFD1C4B9)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SpaceDarkBackground
    ) {
        if (currentProfile == null) {
            // Profile Selection Screen
            ProfileSelectionScreen(
                profiles = profiles,
                onSelectProfile = { viewModel.selectProfile(it) },
                onCreateProfile = { name, avatar, color, pin, gender, email ->
                    viewModel.createProfile(name, avatar, color, pin, adultPin = "", gender = gender, email = email)
                },
                onDeleteProfile = { viewModel.deleteProfile(it) },
                isAdminMode = false,
                onAuthenticateAdmin = { false },
                onLogoutAdmin = { },
                viewModel = viewModel
            )
        } else {
            // Main Dashboard Screen
            MainDashboardScreen(
                viewModel = viewModel,
                currentProfile = currentProfile!!,
                profileColor = profileColor,
                isInPipMode = isInPipMode,
                onEnterPip = onEnterPip
            )
        }
    }
}

@Composable
fun ProfileSelectionScreen(
    profiles: List<UserProfile>,
    onSelectProfile: (String) -> Unit,
    onCreateProfile: (String, String, String, String, String, String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    isAdminMode: Boolean = false,
    onAuthenticateAdmin: (String) -> Boolean = { false },
    onLogoutAdmin: () -> Unit = {},
    viewModel: MediaViewModel
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<UserProfile?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }
    
    // Profile password PIN state
    var selectedProfileForPinCheck by remember { mutableStateOf<UserProfile?>(null) }
    var profilePinInput by remember { mutableStateOf("") }
    var profilePinError by remember { mutableStateOf(false) }
    var isSendingProfileRecovery by remember { mutableStateOf(false) }
    var profileRecoveryStatusMsg by remember { mutableStateOf("") }

    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        // App title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "MedinPlay Hub",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Selecciona un perfil para continuar",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
        )

        // Profiles Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        ) {
            items(profiles) { profile ->
                ProfileCard(
                    profile = profile,
                    onClick = {
                        if (profile.pin.isNotEmpty()) {
                            selectedProfileForPinCheck = profile
                            profilePinInput = ""
                            profilePinError = false
                        } else {
                            onSelectProfile(profile.id)
                        }
                    },
                    onDelete = { profileToDelete = profile }
                )
            }

            item {
                val disableSharingDialog by viewModel.disableSharingDialog.collectAsStateWithLifecycle()
                val maxProfilesAllowed = if (disableSharingDialog) 1 else 6
                val canAddProfile = profiles.size < maxProfilesAllowed
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (canAddProfile) SpaceCardBg else SpaceCardBg.copy(alpha = 0.5f))
                        .border(1.dp, if (canAddProfile) SpaceBorderColor else SpaceBorderColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable(enabled = canAddProfile) { showCreateDialog = true }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (canAddProfile) Icons.Default.Add else Icons.Default.Lock,
                            contentDescription = "Añadir Perfil",
                            tint = if (canAddProfile) Color.White else Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (canAddProfile) "Añadir Perfil" else if (disableSharingDialog) "Límite Alcanzado (Perfil Único)" else "Límite Alcanzado (Máx 6)",
                            color = if (canAddProfile) Color.White else Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        }

        // Profile Password / PIN Check Dialog
        if (selectedProfileForPinCheck != null) {
            AlertDialog(
                onDismissRequest = { 
                    selectedProfileForPinCheck = null
                    profilePinInput = ""
                    profilePinError = false
                },
                containerColor = SpaceCardBg,
                title = { 
                    Text(
                        text = "Perfil Protegido 🔒", 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                text = {
                    Column {
                        Text(
                            text = "Por favor, introduce la clave de acceso para ingresar al perfil de ${selectedProfileForPinCheck?.name}:",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = profilePinInput,
                            onValueChange = { input -> 
                                if (input.all { it.isLetterOrDigit() } && input.length <= 12) {
                                    profilePinInput = input
                                    profilePinError = false
                                }
                            },
                            label = { Text("Clave del Perfil") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFFD1C4B9),
                                unfocusedLabelColor = Color.Gray,
                                focusedBorderColor = Color(0xFFD1C4B9),
                                unfocusedBorderColor = SpaceBorderColor
                            ),
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (profilePinError) {
                            Text(
                                text = "Clave incorrecta. Inténtalo de nuevo.",
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = {
                                isSendingProfileRecovery = true
                                profileRecoveryStatusMsg = "Enviando..."
                                viewModel.sendProfilePinRecoveryEmail(selectedProfileForPinCheck) { success, msg ->
                                    isSendingProfileRecovery = false
                                    profileRecoveryStatusMsg = msg
                                }
                            },
                            enabled = !isSendingProfileRecovery,
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text(
                                text = if (isSendingProfileRecovery) "Enviando clave..." else "¿Olvidaste tu clave?",
                                color = Color(0xFFC67C65),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (profileRecoveryStatusMsg.isNotEmpty()) {
                            Text(
                                text = profileRecoveryStatusMsg,
                                color = if (profileRecoveryStatusMsg.contains("éxito") || profileRecoveryStatusMsg.contains("enviada")) Color.Green else Color.Yellow,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (profilePinInput == selectedProfileForPinCheck?.pin) {
                                val profileId = selectedProfileForPinCheck!!.id
                                selectedProfileForPinCheck = null
                                onSelectProfile(profileId)
                            } else {
                                profilePinError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD1C4B9))
                    ) {
                        Text("Ingresar", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            selectedProfileForPinCheck = null
                            profilePinInput = ""
                            profilePinError = false
                        }
                    ) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }

        if (showCreateDialog) {
            CreateProfileDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, avatar, color, pin, gender, email ->
                    onCreateProfile(name, avatar, color, pin, gender, email)
                    showCreateDialog = false
                }
            )
        }

        if (profileToDelete != null) {
            AlertDialog(
                onDismissRequest = { profileToDelete = null },
                containerColor = SpaceCardBg,
                title = { Text("Eliminar Perfil", color = Color.White) },
                text = { Text("¿Estás seguro de que quieres eliminar el perfil de ${profileToDelete?.name}?", color = Color.LightGray) },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteProfile(profileToDelete!!.id)
                            profileToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Eliminar", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { profileToDelete = null }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
        
        val disableSharingDialog by viewModel.disableSharingDialog.collectAsStateWithLifecycle()

        if (showShareDialog) {
            ShareAppDialog(
                viewModel = viewModel,
                onDismiss = { showShareDialog = false }
            )
        }
        
        IconButton(
            onClick = {
                if (disableSharingDialog) {
                    shareAppApk(context)
                } else {
                    showShareDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Compartir Aplicación",
                tint = Color.White
            )
        }
    }
}

@Composable
fun ProfileCard(
    profile: UserProfile,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val color = remember(profile.accentColorHex) {
        try { Color(android.graphics.Color.parseColor(profile.accentColorHex)) } catch (e: Exception) { Color(0xFF1E88E5) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SpaceCardBg)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Principal Profile Badge (Crown or Tiara depending on gender)
        if (profile.isPrincipal || profile.name.contains("Principal", ignoreCase = true)) {
            val badgeSymbol = if (profile.gender.equals("Femenino", ignoreCase = true)) "👸" else "👑"
            Surface(
                color = Color(0xFFC67C65),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset((-4).dp, (-4).dp)
            ) {
                Text(
                    text = badgeSymbol,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }

        // Delete profile top-right trigger
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(8.dp, (-8).dp)
                .size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Borrar",
                tint = Color.Gray.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Avatar Circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(color)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(SpaceDarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.avatarName.startsWith("avatar_")) {
                        val bgCol = getAvatarColor(profile.avatarName)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.radialGradient(listOf(bgCol.copy(alpha = 0.8f), bgCol))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getAvatarIcon(profile.avatarName),
                                contentDescription = profile.name,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    } else {
                        AsyncImage(
                            model = profile.avatarName,
                            contentDescription = profile.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = profile.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun getAvatarIcon(avatarName: String): ImageVector {
    return when (avatarName) {
        "avatar_1" -> Icons.Default.Person
        "avatar_2" -> Icons.Default.Face
        "avatar_3" -> Icons.Default.ChildCare
        "avatar_4" -> Icons.Default.SportsEsports
        "avatar_5" -> Icons.Default.Star
        "avatar_6" -> Icons.Default.Movie
        "avatar_7" -> Icons.Default.Pets
        "avatar_8" -> Icons.Default.MusicNote
        "avatar_9" -> Icons.Default.LocalFlorist
        "avatar_10" -> Icons.Default.Favorite
        "avatar_11" -> Icons.Default.Build
        "avatar_12" -> Icons.Default.Eco
        "avatar_13" -> Icons.Default.Lightbulb
        "avatar_14" -> Icons.Default.Flight
        "avatar_15" -> Icons.Default.Palette
        "avatar_16" -> Icons.Default.EmojiPeople
        "avatar_17" -> Icons.Default.ThumbUp
        "avatar_18" -> Icons.Default.CameraAlt
        "avatar_19" -> Icons.Default.DirectionsCar
        "avatar_20" -> Icons.Default.Info
        else -> Icons.Default.Person
    }
}

fun getAvatarColor(avatarName: String): Color {
    val index = avatarName.removePrefix("avatar_").toIntOrNull() ?: 1
    return when (index % 10) {
        0 -> Color(0xFFEF5350) // Red
        1 -> Color(0xFFEC407A) // Pink
        2 -> Color(0xFFAB47BC) // Purple
        3 -> Color(0xFF5C6BC0) // Indigo
        4 -> Color(0xFF42A5F5) // Soft Blue
        5 -> Color(0xFF26A69A) // Teal
        6 -> Color(0xFF66BB6A) // Green
        7 -> Color(0xFFFFCA28) // Amber
        8 -> Color(0xFFFFA726) // Orange
        9 -> Color(0xFFFF7043) // Deep Orange
        else -> Color(0xFFEF5350)
    }
}

fun saveUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, "custom_avatar_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Masculino") }
    var email by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("avatar_1") }
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = saveUriToInternalStorage(context, it)
            if (path != null) {
                selectedAvatar = path
            }
        }
    }
    var selectedColor by remember { mutableStateOf(AccentColorsList.first()) }

    val avatars = remember { (1..20).map { "avatar_$it" } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SpaceBorderColor, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nuevo Perfil",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Nombre / Apodo *") },
                    isError = nameError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        focusedBorderColor = if (nameError) Color.Red else Color.White,
                        unfocusedBorderColor = if (nameError) Color.Red else SpaceBorderColor,
                        errorBorderColor = Color.Red,
                        errorLabelColor = Color.Red
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Gender Selection
                Text(
                    text = "Sexo / Género (Para insignia del perfil)",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = gender == "Masculino",
                        onClick = { gender = "Masculino" },
                        label = { Text("👑 Masculino", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = gender == "Femenino",
                        onClick = { gender = "Femenino" },
                        label = { Text("👸 Femenino", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (it.isNotBlank()) emailError = false
                    },
                    label = { Text("Correo Electrónico (Obligatorio) *") },
                    placeholder = { Text("ejemplo@correo.com") },
                    isError = emailError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        focusedBorderColor = if (emailError) Color.Red else Color.White,
                        unfocusedBorderColor = if (emailError) Color.Red else SpaceBorderColor,
                        errorBorderColor = Color.Red,
                        errorLabelColor = Color.Red
                    ),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { input ->
                        if (input.all { it.isLetterOrDigit() } && input.length <= 12) {
                            pin = input
                        }
                    },
                    label = { Text("Clave de Acceso (Opcional)") },
                    placeholder = { Text("Ej. 1234 o mi-clave") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = SpaceBorderColor
                    ),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Avatar Selection
                Text(
                    text = "Selecciona un Avatar",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item for loading from Gallery
                    item {
                        val isCustomSelected = !selectedAvatar.startsWith("avatar_")
                        val color = Color(android.graphics.Color.parseColor(selectedColor))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isCustomSelected) color else SpaceBorderColor)
                                .clickable { imagePickerLauncher.launch("image/*") }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(SpaceDarkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCustomSelected) {
                                    AsyncImage(
                                        model = selectedAvatar,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "Cargar Imagen",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    items(avatars) { avatar ->
                        val isSelected = avatar == selectedAvatar
                        val color = Color(android.graphics.Color.parseColor(selectedColor))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) color else SpaceBorderColor)
                                .clickable { selectedAvatar = avatar }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(SpaceDarkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                val bgCol = getAvatarColor(avatar)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.radialGradient(listOf(bgCol.copy(alpha = 0.8f), bgCol))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getAvatarIcon(avatar),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Color Selection
                ProfileColorThemePicker(
                    selectedColorHex = selectedColor,
                    onColorSelected = { selectedColor = it }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val cleanName = name.trim()
                            val cleanEmail = email.trim()
                            if (cleanName.isBlank()) {
                                nameError = true
                                errorMessage = "El Nombre del Perfil es obligatorio"
                                android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
                            } else if (cleanEmail.isBlank()) {
                                emailError = true
                                errorMessage = "El Correo Electrónico es obligatorio"
                                android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
                            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                                emailError = true
                                errorMessage = "Formato de correo electrónico inválido"
                                android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                nameError = false
                                emailError = false
                                errorMessage = ""
                                onCreate(cleanName, selectedAvatar, selectedColor, pin, gender, cleanEmail)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(android.graphics.Color.parseColor(selectedColor))
                        )
                    ) {
                        Text("Crear", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    viewModel: MediaViewModel,
    currentProfile: UserProfile,
    profileColor: Color,
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {}
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val activeRadio by viewModel.activeRadio.collectAsStateWithLifecycle()
    val activeIptv by viewModel.activeIptv.collectAsStateWithLifecycle()
    val activeMovie by viewModel.activeMovie.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playerSizeMode by viewModel.playerSizeMode.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()
    val skippedVersion by viewModel.skippedVersion.collectAsStateWithLifecycle()
    val isDownloadingUpdate by viewModel.isDownloadingUpdate.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadError by viewModel.downloadError.collectAsStateWithLifecycle()
    var updateDismissed by remember(appUpdateInfo?.versionCode) { mutableStateOf(false) }
    var showVersionModal by remember { mutableStateOf(false) }

    val hasUpdateAvailable = appUpdateInfo != null &&
        appUpdateInfo!!.versionCode > viewModel.currentLocalVersionCode &&
        (appUpdateInfo!!.isMandatoryUpdate || appUpdateInfo!!.versionCode > skippedVersion)

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val shouldHideSystemUi = (activeIptv != null || activeMovie != null) && (isLandscape || playerSizeMode == com.example.ui.PlayerSizeMode.Fullscreen || playerSizeMode == com.example.ui.PlayerSizeMode.PiP || isInPipMode)

    // Dynamic Fullscreen / Immersive controller for system bars (hiding native notifications, system status, and system gesture bars in horizontal view)
    LaunchedEffect(shouldHideSystemUi) {
        val activity = context as? android.app.Activity
        val window = activity?.window
        if (window != null) {
            val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            if (shouldHideSystemUi) {
                windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Sync state with background RadioPlaybackService
    LaunchedEffect(activeRadio) {
        if (activeRadio != null) {
            com.example.service.PlaybackStateManager.activeRadio.value = activeRadio
            com.example.service.PlaybackStateManager.isPlaying.value = viewModel.isPlaying.value

            val intent = android.content.Intent(context, com.example.service.RadioPlaybackService::class.java).apply {
                action = com.example.service.RadioPlaybackService.ACTION_START
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            com.example.service.PlaybackStateManager.activeRadio.value = null
            com.example.service.PlaybackStateManager.isPlaying.value = false
            context.stopService(android.content.Intent(context, com.example.service.RadioPlaybackService::class.java))
        }
    }

    // Lock +18 category immediately when application goes to background
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.unlockAdultContent(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle background notification clicks to redirect and focus the active station
    LaunchedEffect(Unit) {
        com.example.service.PlaybackStateManager.notificationClickTrigger.collect { trigger ->
            if (trigger) {
                viewModel.setTab(MediaViewModel.Tab.Radio)
                val targetRadio = viewModel.activeRadio.value ?: com.example.service.PlaybackStateManager.activeRadio.value
                if (targetRadio != null) {
                    if (viewModel.activeRadio.value == null) {
                        viewModel.playRadio(targetRadio)
                    }
                    viewModel.triggerScrollToActiveRadio(targetRadio.id)
                }
                com.example.service.PlaybackStateManager.notificationClickTrigger.value = false
            }
        }
    }

    LaunchedEffect(Unit) {
        com.example.service.PlaybackStateManager.onStop = {
            viewModel.stopAllPlayback()
        }
    }

    if ((hasUpdateAvailable && !updateDismissed) || showVersionModal) {
        val infoToShow = appUpdateInfo ?: com.example.models.AppUpdateInfo(
            versionCode = viewModel.currentLocalVersionCode,
            versionName = "1.0.0",
            releaseNotes = "La aplicación se encuentra en la versión v1.0.0. No hay nuevas actualizaciones pendientes en ConfiguracionGlobal.",
            isForceUpdate = false
        )
        AppUpdateAlertDialog(
            updateInfo = infoToShow,
            isDownloading = isDownloadingUpdate,
            progress = downloadProgress,
            errorMessage = downloadError,
            onDownloadClick = {
                if (infoToShow.apkUrl.isNotBlank()) {
                    viewModel.downloadAndInstallUpdate(context, infoToShow.apkUrl)
                } else {
                    android.widget.Toast.makeText(context, "No hay URL de APK configurada en Firestore.", android.widget.Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = {
                if (!infoToShow.isMandatoryUpdate) {
                    viewModel.skipVersion(infoToShow.versionCode)
                }
                showVersionModal = false
                updateDismissed = true
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SpaceDarkBackground,
        topBar = {
            if (!shouldHideSystemUi) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(profileColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentProfile.avatarName.startsWith("avatar_")) {
                                    val bgCol = getAvatarColor(currentProfile.avatarName)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.radialGradient(listOf(bgCol.copy(alpha = 0.8f), bgCol))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getAvatarIcon(currentProfile.avatarName),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    AsyncImage(
                                        model = currentProfile.avatarName,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "MedinPlay Hub",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE6E1DE)
                                )
                                Text(
                                    text = "Bienvenido, ${currentProfile.name}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFA09994),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    actions = {
                        Box {
                            IconButton(
                                onClick = { showVersionModal = true },
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SpaceCardBg)
                                    .border(
                                        1.dp,
                                        if (hasUpdateAvailable) Color.Red else SpaceBorderColor,
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = if (hasUpdateAvailable) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = "Notificaciones de Actualización",
                                    tint = if (hasUpdateAvailable) Color.Red else profileColor
                                )
                            }
                            if (hasUpdateAvailable) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 2.dp, end = 8.dp)
                                        .size(10.dp)
                                        .background(Color.Red, CircleShape)
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.selectProfile(null) },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpaceCardBg)
                                .border(1.dp, SpaceBorderColor, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Cambiar Perfil",
                                tint = Color(0xFFD1C4B9)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SpaceDarkBackground,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            if (!shouldHideSystemUi) {
                NavigationBar(
                    containerColor = SpaceCardBg,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = currentTab == MediaViewModel.Tab.Radio,
                        onClick = { viewModel.setTab(MediaViewModel.Tab.Radio) },
                        icon = { Icon(Icons.Default.Radio, contentDescription = "Radio") },
                        label = { Text("Radio") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = profileColor
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == MediaViewModel.Tab.IPTV,
                        onClick = { viewModel.setTab(MediaViewModel.Tab.IPTV) },
                        icon = { Icon(Icons.Default.Tv, contentDescription = "IPTV") },
                        label = { Text("IPTV") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = profileColor
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == MediaViewModel.Tab.Movies,
                        onClick = { viewModel.setTab(MediaViewModel.Tab.Movies) },
                        icon = { Icon(Icons.Default.Movie, contentDescription = "Películas") },
                        label = { Text("Cine") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = profileColor
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == MediaViewModel.Tab.AdminSettings,
                        onClick = { viewModel.setTab(MediaViewModel.Tab.AdminSettings) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                        label = { Text("Ajustes") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = profileColor
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main views based on active tab
            when (currentTab) {
                MediaViewModel.Tab.Radio -> RadioTabScreen(viewModel, currentProfile, profileColor)
                MediaViewModel.Tab.IPTV -> IptvTabScreen(viewModel, currentProfile, profileColor)
                MediaViewModel.Tab.Movies -> MovieBrowserScreen(viewModel, currentProfile, profileColor)
                MediaViewModel.Tab.AdminSettings -> SettingsTabScreen(viewModel, currentProfile, profileColor)
            }

            // Global persistent mini radio player at bottom if radio is active and we are not on players
            if (activeRadio != null && activeIptv == null && activeMovie == null) {
                val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
                val isRadioBuffering by viewModel.isRadioBuffering.collectAsStateWithLifecycle()
                val isRadioActuallyPlaying by viewModel.isRadioActuallyPlaying.collectAsStateWithLifecycle()
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(profileColor.copy(alpha = 0.95f))
                        .clickable {
                            viewModel.setTab(MediaViewModel.Tab.Radio)
                            viewModel.triggerScrollToActiveRadio(activeRadio!!.id)
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AsyncImage(
                            model = activeRadio!!.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeRadio!!.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = if (isRadioBuffering) "Conectando emisora..." else if (isRadioActuallyPlaying) "Reproduciendo en vivo • ${activeRadio!!.genre}" else "Pausado • ${activeRadio!!.genre}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }

                        SoundVisualizer(
                            isPlaying = (isRadioActuallyPlaying || isPlaying) && !isRadioBuffering,
                            isBuffering = isRadioBuffering,
                            modifier = Modifier.height(24.dp).width(40.dp),
                            color = Color.White
                        )

                        IconButton(onClick = { viewModel.pauseOrResumeCurrentPlayback() }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.stopAllPlayback() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            val isPiP = playerSizeMode == com.example.ui.PlayerSizeMode.PiP && !isInPipMode

            // Custom high fidelity TV player fullscreen overlay
            AnimatedVisibility(
                visible = activeIptv != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = if (isPiP && activeIptv != null) {
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 100.dp)
                        .size(240.dp, 150.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, profileColor, RoundedCornerShape(16.dp))
                } else {
                    Modifier.fillMaxSize()
                }
            ) {
                if (activeIptv != null) {
                    IptvPlayerOverlay(
                        iptv = activeIptv!!,
                        onClose = { viewModel.stopAllPlayback() },
                        viewModel = viewModel,
                        currentProfile = currentProfile,
                        accentColor = profileColor,
                        isInPipMode = isInPipMode,
                        onEnterPip = onEnterPip
                    )
                }
            }

            // Custom high fidelity Movie cinema player fullscreen overlay
            AnimatedVisibility(
                visible = activeMovie != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = if (isPiP && activeMovie != null) {
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 100.dp)
                        .size(240.dp, 150.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, profileColor, RoundedCornerShape(16.dp))
                } else {
                    Modifier.fillMaxSize()
                }
            ) {
                if (activeMovie != null) {
                    MoviePlayerOverlay(
                        movie = activeMovie!!,
                        onClose = { viewModel.stopAllPlayback() },
                        viewModel = viewModel,
                        currentProfile = currentProfile,
                        accentColor = profileColor,
                        isInPipMode = isInPipMode,
                        onEnterPip = onEnterPip
                    )
                }
            }
        }
    }
}

// ---------------------- TAB 1: RADIO SCREEN ----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioTabScreen(
    viewModel: MediaViewModel,
    profile: UserProfile,
    accentColor: Color
) {
    val radios by viewModel.radios.collectAsStateWithLifecycle()
    val activeRadio by viewModel.activeRadio.collectAsStateWithLifecycle()
    val query by viewModel.radioSearchQuery.collectAsStateWithLifecycle()
    val showOnlyFavs by viewModel.showOnlyFavorites.collectAsStateWithLifecycle()
    val scrollToActiveRadioTrigger by viewModel.scrollToActiveRadioTrigger.collectAsStateWithLifecycle()

    val isRadioActuallyPlaying by viewModel.isRadioActuallyPlaying.collectAsStateWithLifecycle()
    val isRadioBuffering by viewModel.isRadioBuffering.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("Todos") }

    val categories = remember(radios) {
        val normalGenres = radios
            .flatMap { radio ->
                val list = mutableListOf<String>()
                if (radio.genres.isNotEmpty()) list.addAll(radio.genres)
                if (radio.genre.isNotBlank()) list.addAll(radio.genre.split(",", "/", "|", "•", ";"))
                list.map { it.trim() }
            }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        listOf("Todos") + normalGenres
    }

    val filteredRadios = remember(radios, query, showOnlyFavs, selectedCategory, profile) {
        radios.filter { radio ->
            val radioCats = (radio.genres + radio.genre.split(",", "/", "|", "•", ";")).map { it.trim() }.filter { it.isNotBlank() }
            val matchesSearch = radio.name.contains(query, ignoreCase = true) || radio.genre.contains(query, ignoreCase = true)
            val matchesFav = !showOnlyFavs || profile.favoriteRadioIds.contains(radio.id)
            val matchesCategory = when (selectedCategory) {
                "Todos" -> true
                else -> radioCats.any { it.equals(selectedCategory, ignoreCase = true) }
            }
            matchesSearch && matchesFav && matchesCategory
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(scrollToActiveRadioTrigger) {
        scrollToActiveRadioTrigger?.let { targetId ->
            val index = filteredRadios.indexOfFirst { it.id == targetId }
            if (index >= 0) {
                coroutineScope.launch {
                    listState.animateScrollToItem(index)
                }
            }
            viewModel.clearScrollToActiveRadioTrigger()
        }
    }

    val isGridView by viewModel.radioViewModeIsGrid.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search & Favorites & View Mode Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setRadioSearch(it) },
                placeholder = { Text("Buscar Radios...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = SpaceBorderColor
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { viewModel.toggleShowOnlyFavorites() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (showOnlyFavs) accentColor else SpaceCardBg)
                    .border(1.dp, if (showOnlyFavs) accentColor else SpaceBorderColor, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = if (showOnlyFavs) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favoritos",
                    tint = if (showOnlyFavs) Color.White else Color.Gray
                )
            }

            IconButton(
                onClick = { viewModel.toggleRadioViewMode() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isGridView) accentColor else SpaceCardBg)
                    .border(1.dp, if (isGridView) accentColor else SpaceBorderColor, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Default.GridView else Icons.Default.ViewList,
                    contentDescription = "Cambiar Vista",
                    tint = if (isGridView) Color.White else Color.Gray
                )
            }
        }

        // Categories List Filter
        if (categories.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) accentColor else SpaceCardBg)
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Banners publicitarios para Radio
        val bannersState by viewModel.banners.collectAsStateWithLifecycle()
        val errorReports by viewModel.errorReports.collectAsStateWithLifecycle()
        val radioBanners = remember(bannersState) { viewModel.getBannersForLocation("radio") }
        val radioDisplayList = remember(filteredRadios, radioBanners) {
            buildListWithBanners(filteredRadios, radioBanners)
        }

        if (filteredRadios.isEmpty()) {
            EmptyStatePlaceholder(
                title = "No se encontraron radios",
                description = if (showOnlyFavs) "Agrega emisoras a tus favoritos para verlas aquí." else "Intenta buscar otra palabra o pídele al administrador que añada más radios."
            )
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = radioDisplayList.size,
                        span = { index ->
                            if (radioDisplayList[index] is com.example.models.AdBanner) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                        }
                    ) { index ->
                        when (val item = radioDisplayList[index]) {
                            is com.example.models.AdBanner -> {
                                SingleAdBannerCard(banner = item)
                            }
                            is RadioItem -> {
                                val radio = item
                                val isFavorite = profile.favoriteRadioIds.contains(radio.id)
                                val isCurrent = activeRadio?.id == radio.id
                                val hasReportError = errorReports.any { (it.itemId == radio.id || (it.itemName.isNotBlank() && it.itemName.equals(radio.name, ignoreCase = true))) && it.status.trim().uppercase() == "PENDIENTE" }

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isCurrent || hasReportError) 1.5.dp else 1.dp,
                                            color = if (hasReportError) Color.Red else (if (isCurrent) accentColor else SpaceBorderColor),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { if (isCurrent) viewModel.pauseOrResumeCurrentPlayback() else viewModel.playRadio(radio) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                                            IconButton(
                                                onClick = { viewModel.toggleFavoriteRadio(radio.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                    contentDescription = null,
                                                    tint = if (isFavorite) Color.Red else Color.Gray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        AsyncImage(
                                            model = radio.imageUrl,
                                            contentDescription = radio.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(SpaceDarkBackground)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = radio.name,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = radio.genre,
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                        if (hasReportError) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                color = Color(0xFFD32F2F),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text("⚠️ Reportado", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            SoundVisualizer(
                                                isPlaying = (isRadioActuallyPlaying || isPlaying) && isCurrent && !isRadioBuffering,
                                                isBuffering = isRadioBuffering && isCurrent,
                                                modifier = Modifier.height(14.dp).width(26.dp),
                                                color = accentColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(count = radioDisplayList.size) { index ->
                        when (val item = radioDisplayList[index]) {
                            is com.example.models.AdBanner -> {
                                SingleAdBannerCard(banner = item)
                            }
                            is RadioItem -> {
                                val radio = item
                                val isFavorite = profile.favoriteRadioIds.contains(radio.id)
                                val isCurrent = activeRadio?.id == radio.id
                                val hasReportError = errorReports.any { (it.itemId == radio.id || (it.itemName.isNotBlank() && it.itemName.equals(radio.name, ignoreCase = true))) && it.status.trim().uppercase() == "PENDIENTE" }

                                RadioCard(
                                    radio = radio,
                                    isFavorite = isFavorite,
                                    isCurrent = isCurrent,
                                    isActuallyPlaying = (isRadioActuallyPlaying || isPlaying) && isCurrent && !isRadioBuffering,
                                    isBuffering = isRadioBuffering && isCurrent,
                                    accentColor = accentColor,
                                    hasReportError = hasReportError,
                                    onPlay = { if (isCurrent) viewModel.pauseOrResumeCurrentPlayback() else viewModel.playRadio(radio) },
                                    onFavoriteToggle = { viewModel.toggleFavoriteRadio(radio.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RadioCard(
    radio: RadioItem,
    isFavorite: Boolean,
    isCurrent: Boolean,
    isActuallyPlaying: Boolean = false,
    isBuffering: Boolean = false,
    accentColor: Color,
    hasReportError: Boolean = false,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrent || hasReportError) 1.5.dp else 1.dp,
                color = if (hasReportError) Color.Red else (if (isCurrent) accentColor else SpaceBorderColor),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onPlay() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Radio Cover Image
            AsyncImage(
                model = radio.imageUrl,
                contentDescription = radio.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SpaceDarkBackground)
            )

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = radio.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (hasReportError) {
                        Surface(
                            color = Color(0xFFD32F2F),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("⚠️ Reportado", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    if (isCurrent) {
                        SoundVisualizer(
                            isPlaying = isActuallyPlaying,
                            isBuffering = isBuffering,
                            modifier = Modifier.height(16.dp).width(28.dp),
                            color = accentColor
                        )
                    }
                }
                Text(
                    text = radio.genre,
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                if (radio.description.isNotBlank()) {
                    Text(
                        text = radio.description,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Play & Favorite actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) Color.Red else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = if (isCurrent && isActuallyPlaying) Icons.Default.Pause else if (isCurrent) Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                        contentDescription = if (isCurrent && isActuallyPlaying) "Pausar" else "Reproducir",
                        tint = if (isCurrent) accentColor else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}


// ---------------------- TAB 2: IPTV TV SCREEN ----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IptvTabScreen(
    viewModel: MediaViewModel,
    profile: UserProfile,
    accentColor: Color
) {
    val iptvList by viewModel.iptvList.collectAsStateWithLifecycle()
    val activeIptv by viewModel.activeIptv.collectAsStateWithLifecycle()
    val query by viewModel.iptvSearchQuery.collectAsStateWithLifecycle()
    val showOnlyFavs by viewModel.showOnlyFavorites.collectAsStateWithLifecycle()

    val isAdultUnlocked by viewModel.isAdultUnlocked.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("Todos") }
    var adultIptvToPlay by remember { mutableStateOf<IptvItem?>(null) }
    var showAdultPinDialog by remember { mutableStateOf(false) }

    var showCategoryAdultPinDialog by remember { mutableStateOf(false) }
    var tempCategoryToSelect by remember { mutableStateOf("") }
    var adultPinErrorMsg by remember { mutableStateOf("") }

    val categories = remember(iptvList) {
        val normalCategories = iptvList
            .filter { !it.isAdultChannel }
            .flatMap { channel ->
                val list = mutableListOf<String>()
                if (channel.categories.isNotEmpty()) list.addAll(channel.categories)
                if (channel.category.isNotBlank()) list.addAll(channel.category.split(",", "/", "|", "•", ";"))
                list.map { it.trim() }
            }
            .filter { it.isNotBlank() && !it.contains("+18", ignoreCase = true) && !it.contains("Adult", ignoreCase = true) }
            .distinct()
            .sorted()
        listOf("Todos") + normalCategories + "+18"
    }

    val filteredIptv = remember(iptvList, query, showOnlyFavs, selectedCategory, isAdultUnlocked, profile) {
        val sortedList = iptvList.sortedWith(compareBy({ it.category.lowercase() }, { it.name.lowercase() }))
        sortedList.filter { channel ->
            val isChannelAdult = channel.isAdultChannel
            val channelCats = (channel.categories + channel.category.split(",", "/", "|", "•", ";")).map { it.trim() }.filter { it.isNotBlank() }
            val matchesSearch = channel.name.contains(query, ignoreCase = true) || channel.country.contains(query, ignoreCase = true) || channel.category.contains(query, ignoreCase = true)
            val matchesFav = !showOnlyFavs || profile.favoriteIptvIds.contains(channel.id)
            val matchesCategory = if (isChannelAdult) {
                isAdultUnlocked && selectedCategory == "+18"
            } else {
                if (selectedCategory == "+18") {
                    false
                } else {
                    selectedCategory == "Todos" || channelCats.any { it.equals(selectedCategory, ignoreCase = true) }
                }
            }
            matchesSearch && matchesFav && matchesCategory
        }
    }

    val isGridView by viewModel.iptvViewModeIsGrid.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search & Favorites & View Mode Header
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            val isCompact = maxWidth < 360.dp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.setIptvSearch(it) },
                    placeholder = {
                        Text(
                            "Buscar Canales IPTV...",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = SpaceBorderColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f).widthIn(min = 0.dp)
                )

                IconButton(
                    onClick = { viewModel.toggleShowOnlyFavorites() },
                    modifier = Modifier
                        .size(if (isCompact) 48.dp else 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (showOnlyFavs) accentColor else SpaceCardBg)
                        .border(1.dp, if (showOnlyFavs) accentColor else SpaceBorderColor, RoundedCornerShape(12.dp))
                ) {
                Icon(
                    imageVector = if (showOnlyFavs) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favoritos",
                    tint = if (showOnlyFavs) Color.White else Color.Gray
                )
            }

                IconButton(
                    onClick = { viewModel.toggleIptvViewMode() },
                    modifier = Modifier
                        .size(if (isCompact) 48.dp else 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isGridView) accentColor else SpaceCardBg)
                        .border(1.dp, if (isGridView) accentColor else SpaceBorderColor, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.GridView else Icons.Default.ViewList,
                        contentDescription = "Cambiar Vista",
                        tint = if (isGridView) Color.White else Color.Gray
                    )
                }
            }
        }

        // Categories List Filter
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) accentColor else SpaceCardBg)
                        .clickable {
                            if (category == "+18") {
                                if (isAdultUnlocked) {
                                    selectedCategory = category
                                } else {
                                    tempCategoryToSelect = category
                                    showCategoryAdultPinDialog = true
                                    adultPinErrorMsg = ""
                                }
                            } else {
                                selectedCategory = category
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Banners publicitarios para IPTV
        val bannersState by viewModel.banners.collectAsStateWithLifecycle()
        val errorReports by viewModel.errorReports.collectAsStateWithLifecycle()
        val iptvBanners = remember(bannersState) { viewModel.getBannersForLocation("iptv") }
        val iptvDisplayList = remember(filteredIptv, iptvBanners) {
            buildListWithBanners(filteredIptv, iptvBanners)
        }

        if (filteredIptv.isEmpty()) {
            EmptyStatePlaceholder(
                title = "No se encontraron canales de televisión",
                description = "Intenta cambiar de categoría o pídele al administrador que añada nuevos canales IPTV en el panel de Ajustes."
            )
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = iptvDisplayList.size,
                        span = { index ->
                            if (iptvDisplayList[index] is com.example.models.AdBanner) GridItemSpan(2) else GridItemSpan(1)
                        }
                    ) { index ->
                        when (val item = iptvDisplayList[index]) {
                            is com.example.models.AdBanner -> {
                                SingleAdBannerCard(banner = item)
                            }
                            is IptvItem -> {
                                val tvChannel = item
                                val isFavorite = profile.favoriteIptvIds.contains(tvChannel.id)
                                val hasReportError = errorReports.any { (it.itemId == tvChannel.id || (it.itemName.isNotBlank() && it.itemName.equals(tvChannel.name, ignoreCase = true))) && it.status.trim().uppercase() == "PENDIENTE" }
                                IptvChannelCard(
                                    channel = tvChannel,
                                    isFavorite = isFavorite,
                                    accentColor = accentColor,
                                    hasReportError = hasReportError,
                                    onPlay = {
                                        if (tvChannel.isAdultChannel && !isAdultUnlocked) {
                                            adultIptvToPlay = tvChannel
                                            showAdultPinDialog = true
                                        } else {
                                            viewModel.playIptv(tvChannel)
                                        }
                                    },
                                    onFavoriteToggle = { viewModel.toggleFavoriteIptv(tvChannel.id) }
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(count = iptvDisplayList.size) { index ->
                        when (val item = iptvDisplayList[index]) {
                            is com.example.models.AdBanner -> {
                                SingleAdBannerCard(banner = item)
                            }
                            is IptvItem -> {
                                val tvChannel = item
                                val isFavorite = profile.favoriteIptvIds.contains(tvChannel.id)
                                val hasReportError = errorReports.any { (it.itemId == tvChannel.id || (it.itemName.isNotBlank() && it.itemName.equals(tvChannel.name, ignoreCase = true))) && it.status.trim().uppercase() == "PENDIENTE" }
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, if (hasReportError) Color.Red else SpaceBorderColor, RoundedCornerShape(16.dp))
                                        .clickable {
                                            if (tvChannel.isAdultChannel && !isAdultUnlocked) {
                                                adultIptvToPlay = tvChannel
                                                showAdultPinDialog = true
                                            } else {
                                                viewModel.playIptv(tvChannel)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AsyncImage(
                                            model = tvChannel.logoUrl,
                                            contentDescription = tvChannel.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(SpaceDarkBackground)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tvChannel.name,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${tvChannel.country} • ${tvChannel.category}",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                            if (hasReportError) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Surface(
                                                    color = Color(0xFFD32F2F),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text("⚠️ Reportado", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        IconButton(onClick = { viewModel.toggleFavoriteIptv(tvChannel.id) }) {
                                            Icon(
                                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = null,
                                                tint = if (isFavorite) Color.Red else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdultPinDialog) {
        val pin = profile.adultPin
        if (pin.isBlank()) {
            AlertDialog(
                onDismissRequest = { showAdultPinDialog = false },
                containerColor = SpaceCardBg,
                title = {
                    Text(
                        text = "⚠️ Control Parental Activo",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "El canal seleccionado es de categoría +18. Debes configurar una clave de control parental en la sección 'Ajustes' de tu perfil para poder visualizar contenido para adultos.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showAdultPinDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Entendido", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
                    }
                }
            )
        } else {
            var enteredPin by remember { mutableStateOf("") }
            var errorMsg by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAdultPinDialog = false },
                containerColor = SpaceCardBg,
                title = {
                    Text(
                        text = "🔒 Control Parental (+18)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Este canal es para adultos (+18). Ingresa la clave de control parental de tu perfil para reproducir.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = { enteredPin = it },
                            label = { Text("Clave Control Parental") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = SpaceBorderColor
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )

                        if (errorMsg.isNotEmpty()) {
                            Text(
                                text = errorMsg,
                                color = Color.Red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (enteredPin == pin) {
                                showAdultPinDialog = false
                                viewModel.unlockAdultContent(true)
                                val toPlay = adultIptvToPlay
                                if (toPlay != null) {
                                    viewModel.playIptv(toPlay)
                                }
                                enteredPin = ""
                                errorMsg = ""
                            } else {
                                errorMsg = "Clave de control parental incorrecta."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Acceder", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdultPinDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }

    if (showCategoryAdultPinDialog) {
        val pin = profile.adultPin
        if (pin.isEmpty()) {
            AlertDialog(
                onDismissRequest = { showCategoryAdultPinDialog = false },
                containerColor = SpaceCardBg,
                title = { Text("Control Parental Requerido", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "Para acceder a la categoría +18, debes configurar una clave de control parental en la sección de Ajustes.",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showCategoryAdultPinDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Aceptar", color = Color.White)
                    }
                }
            )
        } else {
            var pinInput by remember { mutableStateOf("") }
            var isPinError by remember { mutableStateOf(false) }
            
            AlertDialog(
                onDismissRequest = { showCategoryAdultPinDialog = false },
                containerColor = SpaceCardBg,
                title = { Text("Ingresar Clave Parental", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Por favor, ingresa la clave de control parental para ver el contenido +18.",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { input ->
                                if (input.all { it.isLetterOrDigit() } && input.length <= 12) {
                                    pinInput = input
                                    isPinError = false
                                }
                            },
                            label = { Text("Clave Parental", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.Gray
                            ),
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                            ),
                            isError = isPinError,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (isPinError) {
                            Text(
                                text = "Clave incorrecta. Inténtalo de nuevo.",
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pinInput == pin) {
                                viewModel.unlockAdultContent(true)
                                selectedCategory = tempCategoryToSelect
                                showCategoryAdultPinDialog = false
                            } else {
                                isPinError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Desbloquear", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCategoryAdultPinDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun IptvChannelCard(
    channel: IptvItem,
    isFavorite: Boolean,
    accentColor: Color,
    hasReportError: Boolean = false,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (hasReportError) Color.Red else SpaceBorderColor, RoundedCornerShape(16.dp))
            .clickable { onPlay() }
    ) {
        Column {
            // Channel Logo Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(SpaceDarkBackground),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (hasReportError) {
                    Surface(
                        color = Color(0xFFD32F2F),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Text(
                            text = "⚠️ Reportado",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                // Favorite icon top-right overlay
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Adult content tag overlay
                if (channel.isAdultChannel) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Red.copy(alpha = 0.9f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("+18", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Category Tag bottom-left overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor.copy(alpha = 0.9f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = channel.category,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Channel description info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = channel.country,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


// ---------------------- TAB 3: MOVIES SCREEN ----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesTabScreen(
    viewModel: MediaViewModel,
    profile: UserProfile,
    accentColor: Color
) {
    val movies by viewModel.movies.collectAsStateWithLifecycle()
    val query by viewModel.movieSearchQuery.collectAsStateWithLifecycle()
    val showOnlyFavs by viewModel.showOnlyFavorites.collectAsStateWithLifecycle()

    val isAdultUnlocked by viewModel.isAdultUnlocked.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf("Todos") }
    var showCategoryAdultPinDialog by remember { mutableStateOf(false) }
    var tempCategoryToSelect by remember { mutableStateOf("") }

    val categories = remember(movies) {
        val normalGenres = movies
            .filter { !it.isAdultMovie }
            .flatMap { movie ->
                val list = mutableListOf<String>()
                if (movie.genres.isNotEmpty()) list.addAll(movie.genres)
                if (movie.genre.isNotBlank()) list.addAll(movie.genre.split(",", "/", "|", "•", ";"))
                list.map { it.trim() }
            }
            .filter { it.isNotBlank() && !it.contains("+18", ignoreCase = true) && !it.contains("Adult", ignoreCase = true) }
            .distinct()
            .sorted()
        listOf("Todos") + normalGenres + "+18"
    }

    var adultMovieToPlay by remember { mutableStateOf<MovieItem?>(null) }
    var showAdultPinDialog by remember { mutableStateOf(false) }

    var selectedMovieForDetails by remember { mutableStateOf<MovieItem?>(null) }
    var reportingItem by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    var movieToPlayWithProgressCheck by remember { mutableStateOf<MovieItem?>(null) }
    var savedProgressToShowDialog by remember { mutableStateOf(0L) }
    var showResumeDialog by remember { mutableStateOf(false) }

    val handleMoviePlayWithProgress: (MovieItem) -> Unit = { movie ->
        val progress = viewModel.getMovieProgress(profile.id, movie.id)
        val total = try {
            val durationStr = movie.duration
            if (durationStr.contains("h", ignoreCase = true)) {
                val parts = durationStr.split(Regex("[hH]"))
                val hours = parts.getOrNull(0)?.trim()?.filter { it.isDigit() }?.toLongOrNull() ?: 0L
                val minutes = parts.getOrNull(1)?.trim()?.filter { it.isDigit() }?.toLongOrNull() ?: 0L
                (hours * 60 + minutes) * 60 * 1000L
            } else {
                val minutes = durationStr.filter { it.isDigit() }.toLongOrNull() ?: 14L
                minutes * 60 * 1000L
            }
        } catch (e: Exception) {
            840000L
        }
        if (progress > 0L && progress < (total * 0.95f).toLong()) {
            movieToPlayWithProgressCheck = movie
            savedProgressToShowDialog = progress
            showResumeDialog = true
        } else {
            viewModel.playMovie(movie, 0L)
        }
    }

    val filteredMovies = remember(movies, query, showOnlyFavs, selectedCategory, isAdultUnlocked, profile) {
        val sortedMovies = movies.sortedWith(compareBy({ it.genre.lowercase() }, { it.title.lowercase() }))
        sortedMovies.filter { movie ->
            val isMovieAdult = movie.isAdultMovie
            val movieCats = (movie.genres + movie.genre.split(",", "/", "|", "•", ";")).map { it.trim() }.filter { it.isNotBlank() }
            val matchesSearch = movie.title.contains(query, ignoreCase = true) || movie.genre.contains(query, ignoreCase = true)
            val matchesFav = !showOnlyFavs || profile.favoriteMovieIds.contains(movie.id)
            val matchesCategory = if (isMovieAdult) {
                isAdultUnlocked && selectedCategory == "+18"
            } else {
                if (selectedCategory == "+18") {
                    false
                } else {
                    selectedCategory == "Todos" || movieCats.any { it.equals(selectedCategory, ignoreCase = true) }
                }
            }
            matchesSearch && matchesFav && matchesCategory
        }
    }

    val isGridView by viewModel.movieViewModeIsGrid.collectAsStateWithLifecycle()
    val errorReports by viewModel.errorReports.collectAsStateWithLifecycle()

    val omdbSearchResults by viewModel.omdbSearchResults.collectAsStateWithLifecycle()
    val isOmdbSearching by viewModel.isOmdbSearching.collectAsStateWithLifecycle()
    val omdbSearchError by viewModel.omdbSearchError.collectAsStateWithLifecycle()
    val isResolvingStream by viewModel.isResolvingStream.collectAsStateWithLifecycle()
    val resolvingStreamMovieTitle by viewModel.resolvingStreamMovieTitle.collectAsStateWithLifecycle()
    var omdbSearchText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (omdbSearchResults.isEmpty() && omdbSearchText.isBlank()) {
            viewModel.searchOmdb("Avatar")
        }
    }

    // Resolving Stream Loading Dialog
    if (isResolvingStream) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SpaceCardBg,
                border = BorderStroke(1.dp, accentColor)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(color = accentColor, strokeWidth = 3.dp)
                    Text(
                        text = "Conectando con servidor de streaming...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    if (!resolvingStreamMovieTitle.isNullOrBlank()) {
                        Text(
                            text = resolvingStreamMovieTitle!!,
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = "Extrayendo enlace directo en alta definición...",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Modern Search Bar Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp)
        ) {
            OutlinedTextField(
                value = omdbSearchText,
                onValueChange = { omdbSearchText = it },
                placeholder = {
                    Text(
                        text = "Buscar películas o series...",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = accentColor
                    )
                },
                trailingIcon = {
                    if (omdbSearchText.isNotBlank()) {
                        IconButton(onClick = {
                            omdbSearchText = ""
                            viewModel.searchOmdb("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.Gray)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = SpaceBorderColor
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )

            Button(
                onClick = { viewModel.searchOmdb(omdbSearchText) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color(0xFF0F0D0B))
            }

            IconButton(
                onClick = { viewModel.toggleMovieViewMode() },
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpaceCardBg)
                    .border(1.dp, SpaceBorderColor, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Default.GridView else Icons.Default.ViewList,
                    contentDescription = "Cambiar Vista",
                    tint = if (isGridView) accentColor else Color.Gray
                )
            }
        }

        // Suggestions Horizontal Scrollable Row (No overflow, responsive)
        val quickSuggestions = remember {
            listOf("Avatar", "Inception", "Gladiator", "Interstellar", "Batman", "Spider-Man", "Matrix", "Titanic", "Dune", "Oppenheimer", "Avengers")
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(quickSuggestions) { title ->
                val isSelected = omdbSearchText.equals(title, ignoreCase = true)
                Surface(
                    color = if (isSelected) accentColor else SpaceCardBg,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isSelected) accentColor else SpaceBorderColor),
                    modifier = Modifier.clickable {
                        omdbSearchText = title
                        viewModel.searchOmdb(title)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieFilter,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF0F0D0B) else accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            color = if (isSelected) Color(0xFF0F0D0B) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (isOmdbSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = accentColor)
                    Text(
                        text = "Buscando películas en streaming...",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }
        } else if (omdbSearchError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                    border = BorderStroke(1.dp, Color(0xFFE53935)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(36.dp))
                        Text(
                            text = omdbSearchError ?: "No se encontraron resultados",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.searchOmdb(omdbSearchText.ifBlank { "Avatar" }) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Reintentar búsqueda", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (omdbSearchResults.isNotEmpty()) {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(omdbSearchResults) { omdbItem ->
                        OmdbMovieCard(
                            movie = omdbItem,
                            accentColor = accentColor,
                            onClick = { viewModel.loadOmdbMovieDetails(omdbItem.imdbID) },
                            onPlayNow = {
                                viewModel.resolveAndPlayMovie(
                                    title = omdbItem.title,
                                    year = omdbItem.cleanYear,
                                    imdbId = omdbItem.imdbID,
                                    posterUrl = omdbItem.cleanPoster
                                )
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(omdbSearchResults) { omdbItem ->
                        OmdbMovieListItem(
                            movie = omdbItem,
                            accentColor = accentColor,
                            onClick = { viewModel.loadOmdbMovieDetails(omdbItem.imdbID) },
                            onPlayNow = {
                                viewModel.resolveAndPlayMovie(
                                    title = omdbItem.title,
                                    year = omdbItem.cleanYear,
                                    imdbId = omdbItem.imdbID,
                                    posterUrl = omdbItem.cleanPoster
                                )
                            }
                        )
                    }
                }
            }
        } else {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Explorar Cine en Streaming",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ingresa el nombre de cualquier película para consultar metadatos y reproducir al instante.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        omdbSearchText = "Inception"
                        viewModel.searchOmdb("Inception")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Ver Películas Populares", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
                }
            }
        }




    }

    if (showAdultPinDialog) {
        val pin = profile.adultPin
        if (pin.isBlank()) {
            AlertDialog(
                onDismissRequest = { showAdultPinDialog = false },
                containerColor = SpaceCardBg,
                title = {
                    Text(
                        text = "⚠️ Control Parental Activo",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "La película seleccionada es de categoría +18. Debes configurar una clave de control parental en la sección 'Ajustes' de tu perfil para poder visualizar contenido para adultos.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showAdultPinDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Entendido", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
                    }
                }
            )
        } else {
            var enteredPin by remember { mutableStateOf("") }
            var errorMsg by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAdultPinDialog = false },
                containerColor = SpaceCardBg,
                title = {
                    Text(
                        text = "🔒 Control Parental (+18)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Esta película es para adultos (+18). Ingresa la clave de control parental de tu perfil para reproducir.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = { enteredPin = it },
                            label = { Text("Clave Control Parental") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = SpaceBorderColor
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )

                        if (errorMsg.isNotEmpty()) {
                            Text(
                                text = errorMsg,
                                color = Color.Red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (enteredPin == pin) {
                                showAdultPinDialog = false
                                viewModel.unlockAdultContent(true)
                                val toPlay = adultMovieToPlay
                                if (toPlay != null) {
                                    handleMoviePlayWithProgress(toPlay)
                                }
                                enteredPin = ""
                                errorMsg = ""
                            } else {
                                errorMsg = "Clave de control parental incorrecta."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Acceder", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdultPinDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }

    if (showResumeDialog && movieToPlayWithProgressCheck != null) {
        val movie = movieToPlayWithProgressCheck!!
        val progressMillis = savedProgressToShowDialog
        val seconds = (progressMillis / 1000) % 60
        val minutes = (progressMillis / (1000 * 60)) % 60
        val timeStr = String.format("%02d:%02d", minutes, seconds)

        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = {
                Text(
                    text = "Continuar Reproducción",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "¿Deseas continuar viendo '${movie.title}' desde donde la dejaste ($timeStr) o prefieres verla de nuevo desde el principio?",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResumeDialog = false
                        viewModel.playMovie(movie, progressMillis)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Continuar", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResumeDialog = false
                        viewModel.playMovie(movie, 0L)
                    }
                ) {
                    Text("Ver de nuevo", color = Color.White)
                }
            },
            containerColor = SpaceCardBg,
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }

    if (showCategoryAdultPinDialog) {
        val pin = profile.adultPin
        if (pin.isEmpty()) {
            AlertDialog(
                onDismissRequest = { showCategoryAdultPinDialog = false },
                containerColor = SpaceCardBg,
                title = { Text("Control Parental Requerido", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "Para acceder a la categoría +18, debes configurar una clave de control parental en la sección de Ajustes.",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showCategoryAdultPinDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Aceptar", color = Color.White)
                    }
                }
            )
        } else {
            var pinInput by remember { mutableStateOf("") }
            var isPinError by remember { mutableStateOf(false) }
            
            AlertDialog(
                onDismissRequest = { showCategoryAdultPinDialog = false },
                containerColor = SpaceCardBg,
                title = { Text("Ingresar Clave Parental", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Por favor, ingresa la clave de control parental para ver el contenido +18.",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { input ->
                                if (input.all { it.isLetterOrDigit() } && input.length <= 12) {
                                    pinInput = input
                                    isPinError = false
                                }
                            },
                            label = { Text("Clave Parental", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.Gray
                            ),
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                            ),
                            isError = isPinError,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (isPinError) {
                            Text(
                                text = "Clave incorrecta. Inténtalo de nuevo.",
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pinInput == pin) {
                                viewModel.unlockAdultContent(true)
                                selectedCategory = tempCategoryToSelect
                                showCategoryAdultPinDialog = false
                            } else {
                                isPinError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Desbloquear", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCategoryAdultPinDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }

    if (selectedMovieForDetails != null) {
        val movie = selectedMovieForDetails!!
        val hasReportError = errorReports.any { (it.itemId == movie.id || (it.itemName.isNotBlank() && it.itemName.equals(movie.title, ignoreCase = true))) && it.status.trim().uppercase() == "PENDIENTE" }
        MovieDetailDialog(
            movie = movie,
            isFavorite = profile.favoriteMovieIds.contains(movie.id),
            accentColor = accentColor,
            hasReportError = hasReportError,
            onDismiss = { selectedMovieForDetails = null },
            onPlay = {
                if (movie.isAdultMovie && !isAdultUnlocked) {
                    adultMovieToPlay = movie
                    showAdultPinDialog = true
                } else {
                    handleMoviePlayWithProgress(movie)
                }
            },
            onCast = {
                launchExternalStreamCast(context, movie.url, movie.title)
            },
            onFavoriteToggle = {
                viewModel.toggleFavoriteMovie(movie.id)
            },
            onReportError = {
                if (hasReportError) {
                    Toast.makeText(context, "Ya existe un reporte activo para esta película. Espera a que sea resuelto por el administrador.", Toast.LENGTH_LONG).show()
                } else {
                    reportingItem = Triple("PELÍCULA", movie.id, movie.title)
                    selectedMovieForDetails = null
                }
            },
            onOpenAiAnalysis = {
                viewModel.analyzeMovieWithAi(movie.title, movie.year.toString(), movie.description)
                viewModel.loadOmdbMovieByTitle(movie.title, movie.year)
            }
        )
    }

    val selectedOmdbMovieDetail by viewModel.selectedOmdbMovieDetail.collectAsStateWithLifecycle()
    val currentAiAnalysis by viewModel.currentAiAnalysis.collectAsStateWithLifecycle()
    val isLoadingAiAnalysis by viewModel.isLoadingAiAnalysis.collectAsStateWithLifecycle()
    val aiAnalysisError by viewModel.aiAnalysisError.collectAsStateWithLifecycle()

    if (selectedOmdbMovieDetail != null) {
        val detail = selectedOmdbMovieDetail!!
        OmdbMovieDetailDialog(
            detail = detail,
            aiAnalysis = currentAiAnalysis,
            isLoadingAi = isLoadingAiAnalysis,
            aiError = aiAnalysisError,
            accentColor = accentColor,
            onDismiss = {
                viewModel.setSelectedOmdbMovieDetail(null)
                viewModel.clearAiAnalysis()
            },
            onRequestAiAnalysis = {
                viewModel.analyzeMovieWithAi(detail.title, detail.year, detail.plot)
            },
            onSelectRecommendation = { recTitle ->
                omdbSearchText = recTitle
                viewModel.searchOmdb(recTitle)
                viewModel.loadOmdbMovieByTitle(recTitle)
            },
            onPlayCustomStream = { _ ->
                viewModel.resolveAndPlayMovie(
                    title = detail.title,
                    year = detail.cleanYear,
                    imdbId = detail.imdbID,
                    posterUrl = detail.cleanPoster,
                    description = detail.cleanPlot,
                    genre = detail.genre,
                    serverIndex = 0
                )
            },
            onPlayServerIndex = { serverIndex ->
                viewModel.resolveAndPlayMovie(
                    title = detail.title,
                    year = detail.cleanYear,
                    imdbId = detail.imdbID,
                    posterUrl = detail.cleanPoster,
                    description = detail.cleanPlot,
                    genre = detail.genre,
                    serverIndex = serverIndex
                )
            },
            onAddToCatalog = { streamUrl ->
                viewModel.addOmdbMovieToCatalog(detail, streamUrl)
            },
            onTransmitMovie = { serverIndex ->
                viewModel.resolveAndTransmitMovie(
                    context = context,
                    title = detail.title,
                    year = detail.cleanYear,
                    imdbId = detail.imdbID,
                    serverIndex = serverIndex
                )
            }
        )
    }

    if (reportingItem != null) {
        val (type, id, name) = reportingItem!!
        ErrorReportDialog(
            itemName = name,
            itemType = type,
            itemId = id,
            accentColor = accentColor,
            onDismiss = { reportingItem = null },
            onSubmit = { issueType, comments ->
                viewModel.submitErrorReport(
                    context = context,
                    itemType = type,
                    itemId = id,
                    itemName = name,
                    issueType = issueType,
                    comments = comments
                ) {
                    reportingItem = null
                }
            }
        )
    }
}

@Composable
fun MovieCard(
    movie: MovieItem,
    isFavorite: Boolean,
    accentColor: Color,
    hasReportError: Boolean = false,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (hasReportError) Color.Red.copy(alpha = 0.9f) else SpaceBorderColor, RoundedCornerShape(16.dp))
            .clickable { onPlay() }
    ) {
        Column {
            // Poster Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(SpaceDarkBackground),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Backdrop visual shadow overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                )

                // Favorite overlay button
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Rating and Adult content tag overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (movie.rating.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFD700)) // Golden color
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFF0F0D0B),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = movie.rating,
                                    color = Color(0xFF0F0D0B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (movie.isAdult) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Red.copy(alpha = 0.9f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("+18", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Play action hover simulation
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Ver",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.Center)
                )

                // Duration tag
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = movie.duration,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Global Failure Warning Banner Overlay
                if (hasReportError) {
                    Surface(
                        color = Color(0xFFD32F2F),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "⚠️ REPORTADO CON FALLAS",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Movie textual meta
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = movie.genre,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = movie.year.toString(),
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}


// ---------------------- TAB 4: SETTINGS & ADMIN PORTAL ----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTabScreen(
    viewModel: MediaViewModel,
    profile: UserProfile,
    accentColor: Color
) {
    val context = LocalContext.current
    val isAdminMode by viewModel.isAdminMode.collectAsStateWithLifecycle()

    val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()
    val skippedVersion by viewModel.skippedVersion.collectAsStateWithLifecycle()
    val isDownloadingUpdate by viewModel.isDownloadingUpdate.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadError by viewModel.downloadError.collectAsStateWithLifecycle()
    var updateDismissed by remember(appUpdateInfo?.versionCode) { mutableStateOf(false) }
    var showVersionModal by remember { mutableStateOf(false) }
    val hasUpdateAvailable = appUpdateInfo != null &&
        appUpdateInfo!!.versionCode > viewModel.currentLocalVersionCode &&
        (appUpdateInfo!!.isMandatoryUpdate || appUpdateInfo!!.versionCode > skippedVersion)
    
    // Top Tab selection state: 0 = "Ajustes del Perfil", 1 = "Historial"
    var selectedTopTab by remember { mutableIntStateOf(0) }
    var showShareDialog by remember { mutableStateOf(false) }

    // Local states for editing profile properties
    var editName by remember(profile.id) { mutableStateOf(profile.name) }
    var editPin by remember(profile.id) { mutableStateOf(profile.pin) }
    var editAdultPin by remember(profile.id) { mutableStateOf(profile.adultPin) }
    var editGender by remember(profile.id) { mutableStateOf(if (profile.gender.isNotBlank()) profile.gender else "Masculino") }
    var editEmail by remember(profile.id) { mutableStateOf(profile.email) }
    var selectedAvatar by remember(profile.id) { mutableStateOf(profile.avatarName) }
    var isAccessKeyVisible by remember { mutableStateOf(false) }
    var isParentalPinVisible by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = saveUriToInternalStorage(context, it)
            if (path != null) {
                selectedAvatar = path
            }
        }
    }
    var selectedColor by remember(profile.id) { mutableStateOf(profile.accentColorHex) }
    
    var showDeleteFavsConfirmation by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var isForceRefreshing by remember { mutableStateOf(false) }

    // Imperceptible auto-sync every time user enters the profile screen
    LaunchedEffect(profile.id) {
        viewModel.forceServerSync()
    }
    
    // Generate the 20 pre-defined avatar keys
    val avatarKeys = remember { (1..20).map { "avatar_$it" } }

    LaunchedEffect(successMessage) {
        if (successMessage.isNotEmpty()) {
            kotlinx.coroutines.delay(2000)
            successMessage = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Main Two-Tab Header bar
        TabRow(
            selectedTabIndex = selectedTopTab,
            containerColor = SpaceCardBg,
            contentColor = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, SpaceBorderColor, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTopTab == 0,
                onClick = { selectedTopTab = 0 }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (selectedTopTab == 0) accentColor else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ajustes del Perfil",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTopTab == 0) Color.White else Color.Gray
                    )
                }
            }
            Tab(
                selected = selectedTopTab == 1,
                onClick = { selectedTopTab = 1 }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = if (selectedTopTab == 1) accentColor else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Historial",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTopTab == 1) Color.White else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            if (selectedTopTab == 0) {
                // TAB 0: AJUSTES DEL PERFIL
                item {
                    // App Identity Header
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SpaceBorderColor, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "MedinPlay Hub",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "v1.2 • Premium Streaming Hub",
                                fontSize = 12.sp,
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Esta aplicación agrupa tus estaciones de radio, transmisiones de TV IPTV y películas favoritas de manera local. Gestiona perfiles individuales con su propia lista de favoritos y personalizaciones.",
                                fontSize = 12.sp,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SpaceBorderColor, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "👤 Configuración de ${profile.name}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Profile Name
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Nombre del Perfil") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = accentColor,
                                    unfocusedLabelColor = Color.Gray,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = SpaceBorderColor
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Sexo / Género Selection (Para insignia del perfil principal)
                            Text(
                                text = "Sexo / Género (Insignia: 👑 Corona para Masculino / 👸 Diadema para Femenino)",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                FilterChip(
                                    selected = editGender.lowercase() == "masculino",
                                    onClick = { editGender = "Masculino" },
                                    label = { Text("👑 Masculino", fontSize = 12.sp) }
                                )
                                FilterChip(
                                    selected = editGender.lowercase() == "femenino",
                                    onClick = { editGender = "Femenino" },
                                    label = { Text("👸 Femenino", fontSize = 12.sp) }
                                )
                            }

                            // Correo Electrónico (Para recuperación de clave)
                            OutlinedTextField(
                                value = editEmail,
                                onValueChange = { editEmail = it },
                                label = { Text("Correo Electrónico (Recuperación de Clave)") },
                                placeholder = { Text("ejemplo@correo.com") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = accentColor,
                                    unfocusedLabelColor = Color.Gray,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = SpaceBorderColor
                                ),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Se utilizará para recuperar tu clave de acceso si la olvidas.",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Profile PIN/Password Lock
                            OutlinedTextField(
                                value = editPin,
                                onValueChange = { editPin = it },
                                label = { Text("Clave de Acceso (Opcional)") },
                                placeholder = { Text("Ej. 1234 o mi-clave") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = accentColor,
                                    unfocusedLabelColor = Color.Gray,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = SpaceBorderColor
                                ),
                                singleLine = true,
                                visualTransformation = if (isAccessKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isAccessKeyVisible = !isAccessKeyVisible }) {
                                        Icon(
                                            imageVector = if (isAccessKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle Access Key Visibility",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Deja este campo en blanco si no deseas requerir una contraseña para ingresar a este perfil al abrir la app.",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                                lineHeight = 14.sp
                            )

                            // Profile Adult PIN Parental Control
                            OutlinedTextField(
                                value = editAdultPin,
                                onValueChange = { input ->
                                    if (input.all { it.isLetterOrDigit() } && input.length <= 12) {
                                        editAdultPin = input
                                    }
                                },
                                label = { Text("Clave Control Parental / +18 (Opcional)") },
                                placeholder = { Text("Ej. 9988 o mi-clave") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = accentColor,
                                    unfocusedLabelColor = Color.Gray,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = SpaceBorderColor
                                ),
                                singleLine = true,
                                visualTransformation = if (isParentalPinVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isParentalPinVisible = !isParentalPinVisible }) {
                                        Icon(
                                            imageVector = if (isParentalPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle Parental PIN Visibility",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Esta clave se solicitará al reproducir canales de TV o películas con etiqueta +18 para adultos.",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                                lineHeight = 14.sp
                            )

                            // 20 Predetermined Avatars (Carousel / LazyRow)
                            Text(
                                text = "Seleccionar Imagen de Perfil (20 disponibles)",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    val isCustomSelected = !selectedAvatar.startsWith("avatar_")
                                    val currentThemeColor = Color(android.graphics.Color.parseColor(selectedColor))
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(if (isCustomSelected) currentThemeColor else SpaceBorderColor)
                                            .clickable { imagePickerLauncher.launch("image/*") }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(SpaceDarkBackground),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCustomSelected) {
                                                AsyncImage(
                                                    model = selectedAvatar,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.AddAPhoto,
                                                    contentDescription = "Cargar Imagen",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                items(avatarKeys) { avatarKey ->
                                    val isSelected = avatarKey == selectedAvatar
                                    val currentThemeColor = Color(android.graphics.Color.parseColor(selectedColor))
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) currentThemeColor else SpaceBorderColor)
                                            .clickable { selectedAvatar = avatarKey }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(SpaceDarkBackground),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val bgCol = getAvatarColor(avatarKey)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Brush.radialGradient(listOf(bgCol.copy(alpha = 0.8f), bgCol))),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getAvatarIcon(avatarKey),
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Dynamic Accent Color
                            ProfileColorThemePicker(
                                selectedColorHex = selectedColor,
                                onColorSelected = { selectedColor = it }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Buttons to Save Profile Changes
                            Button(
                                onClick = {
                                    if (editName.isNotBlank()) {
                                        viewModel.updateProfile(
                                            profileId = profile.id,
                                            newName = editName,
                                            newAvatar = selectedAvatar,
                                            newColorHex = selectedColor,
                                            newPin = editPin,
                                            newAdultPin = editAdultPin,
                                            gender = editGender,
                                            email = editEmail.trim()
                                        )
                                        successMessage = "¡Perfil actualizado con éxito!"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(android.graphics.Color.parseColor(selectedColor))),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Guardar")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar Cambios del Perfil", fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
                            }

                            if (successMessage.isNotEmpty()) {
                                Text(
                                    text = successMessage,
                                    color = Color.Green,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Option to share profile
                            val disableSharingDialog by viewModel.disableSharingDialog.collectAsStateWithLifecycle()
                            if (!disableSharingDialog) {
                                Button(
                                    onClick = { showShareDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC67C65)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Compartir Usuario / Licencia App", fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = SpaceBorderColor, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Clear Favorites Option
                            Text(
                                text = "Zona de Peligro",
                                fontSize = 14.sp,
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            OutlinedButton(
                                onClick = { showDeleteFavsConfirmation = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Borrar Favoritos")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Borrar Todos los Favoritos", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Notifications & Logout Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { showVersionModal = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (hasUpdateAvailable) Color.Red else SpaceCardBg
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (hasUpdateAvailable) Color.Red else SpaceBorderColor
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(0.42f)
                                ) {
                                    Icon(
                                        imageVector = if (hasUpdateAvailable) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                        contentDescription = "Notificaciones",
                                        tint = if (hasUpdateAvailable) Color.White else accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (hasUpdateAvailable) "Novedades (!)" else "Novedades",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.selectProfile(null) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorderColor),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(0.58f)
                                ) {
                                    Icon(imageVector = Icons.Default.Logout, contentDescription = "Cerrar Perfil")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Salir / Perfil", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 1: HISTORIAL DE REPRODUCCIÓN + GEMINI IA
                item {
                    ProfileHistoryGridCard(
                        viewModel = viewModel,
                        profile = profile,
                        accentColor = accentColor
                    )
                }

                item {
                    GeminiRecommendationsCard(
                        viewModel = viewModel,
                        profile = profile,
                        accentColor = accentColor
                    )
                }
            }
        }
    }

    if (showShareDialog) {
        ShareAppDialog(
            viewModel = viewModel,
            onDismiss = { showShareDialog = false }
        )
    }

    // Confirmation dialog for clearing favorites
    if (showDeleteFavsConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteFavsConfirmation = false },
            containerColor = SpaceCardBg,
            title = { Text("¿Borrar Favoritos?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Se eliminarán todas tus estaciones de radio, IPTV y películas marcadas como favoritas en este perfil. Esta acción no se puede deshacer.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearProfileFavorites(profile.id)
                        showDeleteFavsConfirmation = false
                        successMessage = "¡Favoritos borrados con éxito!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Borrar Todo", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFavsConfirmation = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    if ((hasUpdateAvailable && !updateDismissed) || showVersionModal) {
        val infoToShow = appUpdateInfo ?: com.example.models.AppUpdateInfo(
            versionCode = viewModel.currentLocalVersionCode,
            versionName = "1.0.0",
            releaseNotes = "La aplicación se encuentra en la versión v1.0.0. No hay nuevas actualizaciones pendientes en ConfiguracionGlobal.",
            isForceUpdate = false
        )
        AppUpdateAlertDialog(
            updateInfo = infoToShow,
            isDownloading = isDownloadingUpdate,
            progress = downloadProgress,
            errorMessage = downloadError,
            onDownloadClick = {
                if (infoToShow.apkUrl.isNotBlank()) {
                    viewModel.downloadAndInstallUpdate(context, infoToShow.apkUrl)
                } else {
                    android.widget.Toast.makeText(context, "No hay URL de APK configurada en Firestore.", android.widget.Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = {
                if (!infoToShow.isMandatoryUpdate) {
                    viewModel.skipVersion(infoToShow.versionCode)
                }
                showVersionModal = false
                updateDismissed = true
            }
        )
    }
}

@Composable
fun AdminDashboardScreen(
    viewModel: MediaViewModel,
    onExit: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val radios by viewModel.radios.collectAsStateWithLifecycle()
    val iptvList by viewModel.iptvList.collectAsStateWithLifecycle()
    val movies by viewModel.movies.collectAsStateWithLifecycle()
    val paymentRequests by viewModel.paymentRequests.collectAsStateWithLifecycle()
    val paymentMethods by viewModel.paymentMethods.collectAsStateWithLifecycle()
    val subscriptionPrice by viewModel.subscriptionPrice.collectAsStateWithLifecycle()
    val errorReports by viewModel.errorReports.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val configImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val jsonText = inputStream?.bufferedReader()?.use { it.readText() }
                if (jsonText != null) {
                    val success = viewModel.importUnifiedConfigJson(jsonText)
                    if (success) {
                        android.widget.Toast.makeText(context, "¡Colección importada con éxito! Se han actualizado los canales.", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        android.widget.Toast.makeText(context, "El archivo seleccionado no contiene una configuración válida.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error al importar: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    var activeTab by remember { mutableStateOf(0) } // 0 = Radios, 1 = IPTV, 2 = Cine
    var showSeedCodeDialog by remember { mutableStateOf(false) }

    // Aesthetic color
    val adminColor = Color(0xFFC67C65)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HEADER ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🔑 Panel de Gestión Global",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Consola Administrativa Secreta",
                        fontSize = 12.sp,
                        color = adminColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Salir", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cerrar Panel Admin", color = Color.White)
                }
            }
        }

        // --- PREMIUM STATISTICS SECTION ---
        item {
            Text(
                text = "📊 Estadísticas de Uso y Contenido",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Devices
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, tint = adminColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dispositivos", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("1 Activo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("142 Estimados en Red", fontSize = 9.sp, color = Color.Green)
                    }
                }

                // Card 2: Users (Profiles)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.People, contentDescription = null, tint = adminColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Usuarios/Perfiles", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${profiles.size} / 6", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("580 Totales en Red", fontSize = 9.sp, color = Color.Green)
                    }
                }

                // Card 3: Multimedia Elements
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = null, tint = adminColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Elementos", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val totalElements = radios.size + iptvList.size + movies.size
                        Text("$totalElements Cargados", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${radios.size} R • ${iptvList.size} TV • ${movies.size} M", fontSize = 9.sp, color = Color.LightGray)
                    }
                }
            }
        }

        // --- DYNAMIC CODE COMPILATION EXPORT SECTION (CRITICAL USER REQUEST) ---
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, adminColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🚀 Guardar Fijo en el Código Fuente",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(adminColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Recomendado", color = adminColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Los elementos agregados arriba se guardan en el dispositivo local. Para que queden incrustados permanentemente en el código de la app y cualquiera con quien compartas el APK los tenga cargados por defecto, genera el código Kotlin semilla para pegarlo en DataRepository.kt.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showSeedCodeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = adminColor),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generar Código Semilla Kotlin", fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
                    }
                }
            }
        }

        // --- SHARE & IMPORT CONFIGURATION SECTION ---
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, adminColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🔄 Compartir y Sincronizar Colección",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = adminColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¡Comparte todos los canales, emisoras de radio y películas que hayas incorporado con tus amigos! Exporta tu colección a un archivo JSON para que otros puedan importarla al instante en sus dispositivos.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val json = viewModel.getUnifiedConfigJson()
                                shareCollectionJson(context, json)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = adminColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar / Compartir", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
                        }

                        Button(
                            onClick = {
                                configImportLauncher.launch("application/json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importar JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // --- CONTENT MANAGEMENT TABS ---
        item {
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = SpaceCardBg,
                contentColor = Color.White,
                edgePadding = 8.dp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = if (activeTab == 0) adminColor else Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Radios (${radios.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                        Icon(imageVector = Icons.Default.Tv, contentDescription = null, tint = if (activeTab == 1) adminColor else Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("IPTV (${iptvList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                        Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = if (activeTab == 2) adminColor else Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cine (${movies.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                        Icon(imageVector = Icons.Default.People, contentDescription = null, tint = if (activeTab == 3) adminColor else Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Usuarios/Perfiles (${profiles.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Tab(selected = activeTab == 4, onClick = { activeTab = 4 }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                        Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = if (activeTab == 4) adminColor else Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🌱 App Nuevo Usuario (${paymentRequests.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Tab(selected = activeTab == 5, onClick = { activeTab = 5 }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, tint = if (activeTab == 5) adminColor else Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("💳 Métodos de Pago (${paymentMethods.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Tab(selected = activeTab == 6, onClick = { activeTab = 6 }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                        Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = if (activeTab == 6) adminColor else Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("💲 Precio Licencia ($subscriptionPrice)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Tab(selected = activeTab == 7, onClick = { activeTab = 7 }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = if (activeTab == 7) adminColor else Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("⚙️ Ajustes & Actualización", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                val pendingReports = errorReports.count { it.status == "PENDIENTE" }
                Tab(selected = activeTab == 8, onClick = { activeTab = 8 }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = if (pendingReports > 0) Color.Red else (if (activeTab == 8) adminColor else Color.Gray), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("⚠️ Reportes ($pendingReports)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (pendingReports > 0) Color.Red else Color.White)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SpaceBorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (activeTab) {
                        0 -> ManageRadiosPanel(viewModel, radios, adminColor)
                        1 -> ManageIptvPanel(viewModel, iptvList, adminColor)
                        2 -> ManageMoviesPanel(viewModel, movies, adminColor)
                        3 -> ManageProfilesPanel(viewModel, profiles, adminColor)
                        4 -> ManageNewUserClientsPanel(viewModel, paymentRequests, adminColor)
                        5 -> ManagePaymentMethodsPanel(viewModel, paymentMethods, adminColor)
                        6 -> ManageSubscriptionPricePanel(viewModel, subscriptionPrice, adminColor)
                        7 -> ManageSettingsAndUpdatePanel(viewModel, adminColor)
                        8 -> ManageErrorReportsPanel(viewModel, errorReports, adminColor, onNavigateToAdminTab = { activeTab = it })
                    }
                }
            }
        }

        // --- FACTORY RESET SECTION ---
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SpaceBorderColor, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🛠️ Sistema y Restablecimiento",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "Si deseas descartar tus cambios y volver a cargar el contenido por defecto del sistema, utiliza esta opción de restablecimiento.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.resetAppToDefaults() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restablecer Contenido de Fábrica", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Code Generator Dialog (User Request fulfillment)
    if (showSeedCodeDialog) {
        val context = LocalContext.current
        val clipboardManager = remember { context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager }

        // Compile Kotlin seed lists dynamically!
        val seedCode = remember(radios, iptvList, movies) {
            val radiosCode = radios.joinToString(",\n") { r ->
                "        RadioItem(id = \"${r.id}\", name = \"${r.name}\", genre = \"${r.genre}\", url = \"${r.url}\", imageUrl = \"${r.imageUrl}\", description = \"${r.description}\")"
            }
            val iptvCode = iptvList.joinToString(",\n") { t ->
                "        IptvItem(id = \"${t.id}\", name = \"${t.name}\", category = \"${t.category}\", url = \"${t.url}\", logoUrl = \"${t.logoUrl}\", country = \"${t.country}\")"
            }
            val moviesCode = movies.joinToString(",\n") { m ->
                "        MovieItem(id = \"${m.id}\", title = \"${m.title}\", genre = \"${m.genre}\", url = \"${m.url}\", posterUrl = \"${m.posterUrl}\", description = \"${m.description.replace("\"", "\\\"")}\", year = ${m.year}, duration = \"${m.duration}\", rating = \"${m.rating}\")"
            }

            """
// === CÓDIGO SEMILLA PARA DATAREPOSITORY.KT ===
// Copia estas tres variables y reemplázalas en DataRepository.kt

private val defaultRadios = listOf(
$radiosCode
)

private val defaultIptv = listOf(
$iptvCode
)

private val defaultMovies = listOf(
$moviesCode
)
            """.trimIndent()
        }

        var copySuccess by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSeedCodeDialog = false },
            containerColor = SpaceCardBg,
            title = {
                Text(
                    text = "Código Semilla Generado",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Copia este código y pégalo en DataRepository.kt en tu entorno de desarrollo para incrustar fijamente tus emisoras, canales de TV y películas en el APK compilado para compartirlo.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Surface(
                        color = SpaceDarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .border(1.dp, SpaceBorderColor, RoundedCornerShape(8.dp))
                    ) {
                        Box(modifier = Modifier.padding(10.dp)) {
                            LazyColumn {
                                item {
                                    Text(
                                        text = seedCode,
                                        color = adminColor,
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    if (copySuccess) {
                        Text(
                            text = "¡Copiado al portapapeles con éxito! ✔",
                            color = Color.Green,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clip = android.content.ClipData.newPlainText("Kotlin Seed Code", seedCode)
                        clipboardManager.setPrimaryClip(clip)
                        copySuccess = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = adminColor)
                ) {
                    Text("Copiar Código", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSeedCodeDialog = false }) {
                    Text("Cerrar", color = Color.Gray)
                }
            }
        )
    }
}

// Full admin panel content to easily add and delete radios, iptv, movies in memory/sharedprefs
@Composable
fun AdminControlConsole(
    viewModel: MediaViewModel,
    accentColor: Color
) {
    val radios by viewModel.radios.collectAsStateWithLifecycle()
    val iptvList by viewModel.iptvList.collectAsStateWithLifecycle()
    val movies by viewModel.movies.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableStateOf(0) } // 0 = Radios, 1 = IPTV, 2 = Películas

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SpaceCardBg)
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Console de Control",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Los cambios se guardan localmente en el almacenamiento interno de la app.",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Subtabs
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = SpaceDarkBackground,
            contentColor = Color.White,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }) {
                Text("Radios", fontSize = 11.sp, modifier = Modifier.padding(vertical = 10.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }) {
                Text("IPTV TV", fontSize = 11.sp, modifier = Modifier.padding(vertical = 10.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeSubTab == 2, onClick = { activeSubTab = 2 }) {
                Text("Cine", fontSize = 11.sp, modifier = Modifier.padding(vertical = 10.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeSubTab == 3, onClick = { activeSubTab = 3 }) {
                Text("Banners", fontSize = 11.sp, modifier = Modifier.padding(vertical = 10.dp), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (activeSubTab) {
            0 -> ManageRadiosPanel(viewModel, radios, accentColor)
            1 -> ManageIptvPanel(viewModel, iptvList, accentColor)
            2 -> ManageMoviesPanel(viewModel, movies, accentColor)
            3 -> ManageBannersPanel(viewModel, accentColor)
        }
    }
}

@Composable
fun ManageBannersPanel(
    viewModel: MediaViewModel,
    accentColor: Color
) {
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val firestoreRepository = remember { com.example.data.FirestoreRepository() }
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("image") } // "image" or "html"
    var imageUrl by remember { mutableStateOf("") }
    var clickUrl by remember { mutableStateOf("") }
    var htmlCode by remember { mutableStateOf("") }
    var intervalStr by remember { mutableStateOf("6") }
    var priorityStr by remember { mutableStateOf("1") }
    var isActive by remember { mutableStateOf(true) }

    var locHome by remember { mutableStateOf(true) }
    var locRadio by remember { mutableStateOf(true) }
    var locIptv by remember { mutableStateOf(true) }
    var locMovies by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Agregar Nuevo Banner Publicitario", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor)
        Spacer(modifier = Modifier.height(8.dp))

        // Type selection: Imagen vs HTML (AdMob)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpaceCardBg, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { type = "image" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "image") accentColor else Color.Transparent,
                    contentColor = if (type == "image") Color.White else Color.Gray
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Imagen + Enlace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { type = "html" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "html") accentColor else Color.Transparent,
                    contentColor = if (type == "html") Color.White else Color.Gray
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Código HTML (AdMob)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título / Identificador Anuncio") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (type == "image") {
            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("URL de la Imagen (banner.jpg / png)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = clickUrl,
                onValueChange = { clickUrl = it },
                label = { Text("Enlace de Destino (clickUrl / actionUrl)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(6.dp))
        } else {
            OutlinedTextField(
                value = htmlCode,
                onValueChange = { htmlCode = it },
                label = { Text("Código HTML / Script (AdMob, Google Ads)") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = intervalStr,
                onValueChange = { intervalStr = it.filter { c -> c.isDigit() } },
                label = { Text("Insertar cada N items") },
                placeholder = { Text("6") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )

            OutlinedTextField(
                value = priorityStr,
                onValueChange = { priorityStr = it.filter { c -> c.isDigit() } },
                label = { Text("Prioridad (1 = Mayor)") },
                placeholder = { Text("1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Ubicaciones / Secciones activas:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = locHome, onCheckedChange = { locHome = it })
                Text("Home", fontSize = 11.sp, color = Color.White)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = locRadio, onCheckedChange = { locRadio = it })
                Text("Radio", fontSize = 11.sp, color = Color.White)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = locIptv, onCheckedChange = { locIptv = it })
                Text("IPTV", fontSize = 11.sp, color = Color.White)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = locMovies, onCheckedChange = { locMovies = it })
                Text("Cine", fontSize = 11.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (type == "image" && imageUrl.isBlank()) {
                    Toast.makeText(context, "Ingresa la URL de la imagen del banner", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (type == "html" && htmlCode.isBlank()) {
                    Toast.makeText(context, "Ingresa el código HTML / AdMob del banner", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val newBanner = com.example.models.AdBanner(
                    id = "",
                    title = title.trim(),
                    type = type,
                    imageUrl = imageUrl.trim(),
                    clickUrl = clickUrl.trim(),
                    actionUrl = clickUrl.trim(),
                    htmlCode = htmlCode.trim(),
                    interval = intervalStr.toIntOrNull() ?: 6,
                    active = isActive,
                    priority = priorityStr.toIntOrNull() ?: 1,
                    locations = mapOf(
                        "home" to locHome,
                        "radio" to locRadio,
                        "radios" to locRadio,
                        "iptv" to locIptv,
                        "tv" to locIptv,
                        "movies" to locMovies,
                        "cine" to locMovies,
                        "cinema" to locMovies,
                        "peliculas" to locMovies
                    )
                )
                firestoreRepository.saveBanner(newBanner) { success ->
                    if (success) {
                        Toast.makeText(context, "Banner guardado correctamente", Toast.LENGTH_SHORT).show()
                        title = ""
                        imageUrl = ""
                        clickUrl = ""
                        htmlCode = ""
                    } else {
                        Toast.makeText(context, "Error al guardar el banner", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar Banner", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = SpaceBorderColor)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Banners Registrados (${banners.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))

        if (banners.isEmpty()) {
            Text("No hay banners registrados en Firestore.", fontSize = 12.sp, color = Color.Gray)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                banners.forEach { banner ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SpaceDarkBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (banner.title.isNotBlank()) banner.title else "Banner (${banner.type.uppercase()})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tipo: ${banner.type.uppercase()} | Intervalo: cada ${banner.interval} items | Prioridad: ${banner.priority}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Ubicaciones: ${banner.locations.filterValues { it }.keys.distinct().joinToString(", ")}",
                                    fontSize = 10.sp,
                                    color = accentColor
                                )
                            }
                            IconButton(
                                onClick = {
                                    firestoreRepository.deleteBanner(banner.id) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Banner eliminado", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManageRadiosPanel(
    viewModel: MediaViewModel,
    radios: List<RadioItem>,
    accentColor: Color
) {
    var name by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    var searchQuery by remember { mutableStateOf("") }
    var editingRadio by remember { mutableStateOf<RadioItem?>(null) }
    var testingRadio by remember { mutableStateOf<RadioItem?>(null) }

    var bulkText by remember { mutableStateOf("") }
    var showBulkArea by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filteredRadios = remember(radios, searchQuery) {
        if (searchQuery.isBlank()) radios
        else radios.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.genre.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showBulkArea) "Carga Masiva de Radios" else "Añadir Nueva Radio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            TextButton(onClick = { showBulkArea = !showBulkArea }) {
                Text(if (showBulkArea) "Formulario Simple" else "Carga Masiva", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showBulkArea) {
            Text(
                text = "Pega un arreglo JSON con el formato de RadioItem, o texto separado por comas (un elemento por línea):\nNombre, Género, URL de Audio, URL Imagen (Opcional), Descripción (Opcional)",
                fontSize = 11.sp,
                color = Color.LightGray,
                lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = bulkText,
                onValueChange = { bulkText = it },
                placeholder = { Text("Pega el JSON o CSV aquí...", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(150.dp),
                maxLines = 10
            )
            Button(
                onClick = {
                    val count = viewModel.bulkAddRadios(bulkText)
                    if (count > 0) {
                        android.widget.Toast.makeText(context, "¡Se han importado $count emisoras de radio con éxito!", android.widget.Toast.LENGTH_LONG).show()
                        bulkText = ""
                        showBulkArea = false
                    } else {
                        android.widget.Toast.makeText(context, "No se pudieron importar elementos. Verifica el formato.", android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cargar Emisoras de Manera Masiva", fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
            }
        } else {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Nombre Emisora") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = genre, onValueChange = { genre = it }, label = { Text("Género (Ej: Pop, Rock)") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = url, onValueChange = { url = it }, label = { Text("URL Streaming de Audio") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("URL Imagen Portada (Opcional)") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = desc, onValueChange = { desc = it }, label = { Text("Descripción Corta") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        viewModel.addRadio(name, genre, url, imageUrl, desc)
                        name = ""; genre = ""; url = ""; imageUrl = ""; desc = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Registrar Emisora", fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
            }
        }

        HorizontalDivider(color = SpaceBorderColor, modifier = Modifier.padding(vertical = 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gestión de Radios (${filteredRadios.size} / ${radios.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("🔍 Buscar radio por nombre, género o URL...", color = Color.Gray, fontSize = 12.sp) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.Gray)
                    }
                } else {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = SpaceBorderColor
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (filteredRadios.isEmpty()) {
            Text("No se encontraron emisoras que coincidan con la búsqueda.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
        } else {
            filteredRadios.forEach { radio ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, SpaceBorderColor, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Logo / Image Preview
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(1.dp, SpaceBorderColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (radio.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = radio.imageUrl,
                                    contentDescription = radio.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Radio, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                            }
                        }

                        // Text details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(radio.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (radio.genre.isBlank()) "Sin género" else radio.genre, color = accentColor, fontSize = 11.sp, maxLines = 1)
                            Text(radio.url, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        // Actions: Probar, Editar, Eliminar
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = { testingRadio = radio },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Probar", tint = accentColor)
                            }

                            IconButton(
                                onClick = { editingRadio = radio },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.LightGray)
                            }

                            IconButton(
                                onClick = { viewModel.deleteRadio(radio.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    editingRadio?.let { radio ->
        AdminEditRadioDialog(
            radio = radio,
            accentColor = accentColor,
            onDismiss = { editingRadio = null },
            onSave = { updatedName, updatedGenre, updatedUrl, updatedImageUrl, updatedDesc ->
                viewModel.updateRadio(radio.id, updatedName, updatedGenre, updatedUrl, updatedImageUrl, updatedDesc)
                editingRadio = null
                android.widget.Toast.makeText(context, "Emisora actualizada correctamente", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    testingRadio?.let { radio ->
        AdminTestStreamDialog(
            title = radio.name,
            subtitle = radio.genre,
            url = radio.url,
            imageUrl = radio.imageUrl,
            isVideo = false,
            accentColor = accentColor,
            onDismiss = { testingRadio = null },
            onPlayInMainApp = {
                viewModel.playRadio(radio)
                testingRadio = null
                android.widget.Toast.makeText(context, "Reproduciendo ${radio.name} en el reproductor principal", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ManageIptvPanel(
    viewModel: MediaViewModel,
    iptvList: List<IptvItem>,
    accentColor: Color
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var isAdult by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var editingIptv by remember { mutableStateOf<IptvItem?>(null) }
    var testingIptv by remember { mutableStateOf<IptvItem?>(null) }

    var bulkText by remember { mutableStateOf("") }
    var showBulkArea by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filteredIptv = remember(iptvList, searchQuery) {
        if (searchQuery.isBlank()) iptvList
        else iptvList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.country.contains(searchQuery, ignoreCase = true) ||
            it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showBulkArea) "Carga Masiva de Canales" else "Añadir Canal de TV (IPTV)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            TextButton(onClick = { showBulkArea = !showBulkArea }) {
                Text(if (showBulkArea) "Formulario Simple" else "Carga Masiva", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showBulkArea) {
            Text(
                text = "Pega un arreglo JSON con el formato de IptvItem, o texto separado por comas (un elemento por línea):\nNombre, Categoría, URL Transmisión, URL Logo (Opcional), País (Opcional), Adulto (true/false) (Opcional)",
                fontSize = 11.sp,
                color = Color.LightGray,
                lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = bulkText,
                onValueChange = { bulkText = it },
                placeholder = { Text("Pega el JSON o CSV aquí...", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(150.dp),
                maxLines = 10
            )
            Button(
                onClick = {
                    val count = viewModel.bulkAddIptv(bulkText)
                    if (count > 0) {
                        android.widget.Toast.makeText(context, "¡Se han importado $count canales de IPTV con éxito!", android.widget.Toast.LENGTH_LONG).show()
                        bulkText = ""
                        showBulkArea = false
                    } else {
                        android.widget.Toast.makeText(context, "No se pudieron importar elementos. Verifica el formato.", android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cargar Canales de Manera Masiva", fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
            }
        } else {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Nombre Canal") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = category, onValueChange = { category = it }, label = { Text("Categoría (Ej: Deportes, Noticias)") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = url, onValueChange = { url = it }, label = { Text("URL de Transmisión (.m3u8, etc.)") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = logoUrl, onValueChange = { logoUrl = it }, label = { Text("URL de Logotipo") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = country, onValueChange = { country = it }, label = { Text("País / Región") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isAdult,
                    onCheckedChange = { isAdult = it },
                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                )
                Text("Contenido Adulto +18 (Requiere clave control parental)", color = Color.White, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank() && category.isNotBlank()) {
                        viewModel.addIptv(name, category, url, logoUrl, country, isAdult)
                        name = ""; category = ""; url = ""; logoUrl = ""; country = ""; isAdult = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Registrar Canal", fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
            }
        }

        HorizontalDivider(color = SpaceBorderColor, modifier = Modifier.padding(vertical = 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gestión de IPTV (${filteredIptv.size} / ${iptvList.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("🔍 Buscar canal por nombre, categoría o país...", color = Color.Gray, fontSize = 12.sp) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.Gray)
                    }
                } else {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = SpaceBorderColor
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (filteredIptv.isEmpty()) {
            Text("No se encontraron canales que coincidan con la búsqueda.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
        } else {
            filteredIptv.forEach { iptv ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, SpaceBorderColor, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Logo / Image Preview
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(1.dp, SpaceBorderColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (iptv.logoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = iptv.logoUrl,
                                    contentDescription = iptv.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(Icons.Default.Tv, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                            }
                        }

                        // Text details
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(iptv.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (iptv.isAdult) {
                                    Surface(
                                        color = Color.Red.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("+18", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            Text("${iptv.category} • ${if (iptv.country.isBlank()) "Global" else iptv.country}", color = accentColor, fontSize = 11.sp, maxLines = 1)
                            Text(iptv.url, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        // Actions: Probar, Editar, Eliminar
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = { testingIptv = iptv },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Probar", tint = accentColor)
                            }

                            IconButton(
                                onClick = { editingIptv = iptv },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.LightGray)
                            }

                            IconButton(
                                onClick = { viewModel.deleteIptv(iptv.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    editingIptv?.let { iptv ->
        AdminEditIptvDialog(
            iptv = iptv,
            accentColor = accentColor,
            onDismiss = { editingIptv = null },
            onSave = { updatedName, updatedCategory, updatedUrl, updatedLogoUrl, updatedCountry, updatedIsAdult ->
                viewModel.updateIptv(iptv.id, updatedName, updatedCategory, updatedUrl, updatedLogoUrl, updatedCountry, updatedIsAdult)
                editingIptv = null
                android.widget.Toast.makeText(context, "Canal de IPTV actualizado correctamente", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    testingIptv?.let { iptv ->
        AdminTestStreamDialog(
            title = iptv.name,
            subtitle = "${iptv.category} • ${iptv.country}",
            url = iptv.url,
            imageUrl = iptv.logoUrl,
            isVideo = true,
            accentColor = accentColor,
            onDismiss = { testingIptv = null },
            onPlayInMainApp = {
                viewModel.playIptv(iptv)
                testingIptv = null
                android.widget.Toast.makeText(context, "Reproduciendo ${iptv.name} en el reproductor principal", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ManageMoviesPanel(
    viewModel: MediaViewModel,
    movies: List<MovieItem>,
    accentColor: Color
) {
    var title by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var posterUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2026") }
    var duration by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("") }
    var isAdult by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var editingMovie by remember { mutableStateOf<MovieItem?>(null) }
    var testingMovie by remember { mutableStateOf<MovieItem?>(null) }

    var bulkText by remember { mutableStateOf("") }
    var showBulkArea by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filteredMovies = remember(movies, searchQuery) {
        if (searchQuery.isBlank()) movies
        else movies.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.genre.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.url.contains(searchQuery, ignoreCase = true) ||
            it.year.toString().contains(searchQuery)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showBulkArea) "Carga Masiva de Cine" else "Añadir Nueva Película", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            TextButton(onClick = { showBulkArea = !showBulkArea }) {
                Text(if (showBulkArea) "Formulario Simple" else "Carga Masiva", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showBulkArea) {
            Text(
                text = "Pega un arreglo JSON con el formato de MovieItem, o texto separado por comas (un elemento por línea):\nTítulo, Género, URL Video, Poster, Sinopsis, Año, Duración, Adulto (true/false), Rating",
                fontSize = 11.sp,
                color = Color.LightGray,
                lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = bulkText,
                onValueChange = { bulkText = it },
                placeholder = { Text("Pega el JSON o CSV aquí...", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(150.dp),
                maxLines = 10
            )
            Button(
                onClick = {
                    val count = viewModel.bulkAddMovies(bulkText)
                    if (count > 0) {
                        android.widget.Toast.makeText(context, "¡Se han importado $count películas con éxito!", android.widget.Toast.LENGTH_LONG).show()
                        bulkText = ""
                        showBulkArea = false
                    } else {
                        android.widget.Toast.makeText(context, "No se pudieron importar elementos. Verifica el formato.", android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cargar Películas de Manera Masiva", fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
            }
        } else {
            var isTmdbSearching by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = title, onValueChange = { title = it }, label = { Text("Título de la Película") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Ingresa el nombre de la película para consultar TMDb API", Toast.LENGTH_SHORT).show()
                    } else {
                        isTmdbSearching = true
                        viewModel.searchTmdbMovie(title) { result ->
                            isTmdbSearching = false
                            if (result != null) {
                                title = result.title
                                genre = result.genre
                                description = result.synopsis
                                year = result.year.toString()
                                duration = result.duration
                                rating = result.rating
                                if (result.posterUrl.isNotBlank()) posterUrl = result.posterUrl
                                Toast.makeText(context, "¡Datos cargados exitosamente desde TMDb!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No se hallaron resultados para '$title' en TMDb API.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                enabled = !isTmdbSearching,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF01B4E4)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isTmdbSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buscando en TMDb...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🎬 Autocompletar con TMDb API", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedTextField(
                value = genre, onValueChange = { genre = it }, label = { Text("Género (Ej: Acción, Terror)") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = url, onValueChange = { url = it }, label = { Text("URL Streaming de Video (.mp4)") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = posterUrl, onValueChange = { posterUrl = it }, label = { Text("URL Imagen Poster") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it }, label = { Text("Sinopsis de la Película") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = year, onValueChange = { year = it }, label = { Text("Año") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = duration, onValueChange = { duration = it }, label = { Text("Duración") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.weight(1.2f)
                )
                OutlinedTextField(
                    value = rating, onValueChange = { rating = it }, label = { Text("Rating (Ej: 7.4/10)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.weight(1.5f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isAdult,
                    onCheckedChange = { isAdult = it },
                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                )
                Text("Contenido Adulto +18 (Requiere clave control parental)", color = Color.White, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    val parsedYear = year.toIntOrNull() ?: 2026
                    if (title.isNotBlank() && url.isNotBlank() && genre.isNotBlank()) {
                        viewModel.addMovie(title, genre, url, posterUrl, description, parsedYear, duration, isAdult, rating)
                        title = ""; genre = ""; url = ""; posterUrl = ""; description = ""; year = "2026"; duration = ""; rating = ""; isAdult = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Registrar Película", fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
            }
        }

        HorizontalDivider(color = SpaceBorderColor, modifier = Modifier.padding(vertical = 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gestión de Cine (${filteredMovies.size} / ${movies.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("🔍 Buscar película por título, género o año...", color = Color.Gray, fontSize = 12.sp) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.Gray)
                    }
                } else {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.Gray)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = SpaceBorderColor
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (filteredMovies.isEmpty()) {
            Text("No se encontraron películas que coincidan con la búsqueda.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
        } else {
            filteredMovies.forEach { movie ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, SpaceBorderColor, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Poster / Image Preview
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(60.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(1.dp, SpaceBorderColor, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (movie.posterUrl.isNotBlank()) {
                                AsyncImage(
                                    model = movie.posterUrl,
                                    contentDescription = movie.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Movie, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                            }
                        }

                        // Text details
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(movie.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (movie.isAdult) {
                                    Surface(
                                        color = Color.Red.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("+18", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            Text("${movie.genre} • ${movie.year}${if (movie.rating.isNotBlank()) " • ★ ${movie.rating}" else ""}", color = accentColor, fontSize = 11.sp, maxLines = 1)
                            Text(movie.url, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        // Actions: Probar, Editar, Eliminar
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = { testingMovie = movie },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Probar", tint = accentColor)
                            }

                            IconButton(
                                onClick = { editingMovie = movie },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.LightGray)
                            }

                            IconButton(
                                onClick = { viewModel.deleteMovie(movie.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    editingMovie?.let { movie ->
        AdminEditMovieDialog(
            movie = movie,
            accentColor = accentColor,
            viewModel = viewModel,
            onDismiss = { editingMovie = null },
            onSave = { updatedTitle, updatedGenre, updatedUrl, updatedPosterUrl, updatedDescription, updatedYear, updatedDuration, updatedIsAdult, updatedRating ->
                viewModel.updateMovie(movie.id, updatedTitle, updatedGenre, updatedUrl, updatedPosterUrl, updatedDescription, updatedYear, updatedDuration, updatedIsAdult, updatedRating)
                editingMovie = null
                android.widget.Toast.makeText(context, "Película actualizada correctamente", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    testingMovie?.let { movie ->
        AdminTestStreamDialog(
            title = movie.title,
            subtitle = "${movie.genre} • ${movie.year}",
            url = movie.url,
            imageUrl = movie.posterUrl,
            isVideo = true,
            accentColor = accentColor,
            onDismiss = { testingMovie = null },
            onPlayInMainApp = {
                viewModel.playMovie(movie)
                testingMovie = null
                android.widget.Toast.makeText(context, "Reproduciendo ${movie.title} en el reproductor principal", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// -----------------------------------------------------------------------------------------
// Helper Dialogs for Admin Panel: Edit Radio, Edit IPTV, Edit Movie, and Test Stream
// -----------------------------------------------------------------------------------------

@Composable
fun AdminEditRadioDialog(
    radio: RadioItem,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (name: String, genre: String, url: String, imageUrl: String, description: String) -> Unit
) {
    var editName by remember { mutableStateOf(radio.name) }
    var editGenre by remember { mutableStateOf(radio.genre) }
    var editUrl by remember { mutableStateOf(radio.url) }
    var editImageUrl by remember { mutableStateOf(radio.imageUrl) }
    var editDesc by remember { mutableStateOf(radio.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .heightIn(max = 620.dp),
        containerColor = SpaceDarkBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Radio, contentDescription = null, tint = accentColor)
                Text("Editar Emisora de Radio", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Logo Preview Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .border(1.dp, accentColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (editImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = editImageUrl,
                                contentDescription = "Vista Previa Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Radio, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                        }
                    }
                    Column {
                        Text("Vista Previa del Logo", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(if (editImageUrl.isNotBlank()) "Logotipo cargado" else "Sin URL de imagen", color = Color.Gray, fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = editName, onValueChange = { editName = it }, label = { Text("Nombre Emisora") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editGenre, onValueChange = { editGenre = it }, label = { Text("Género") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editUrl, onValueChange = { editUrl = it }, label = { Text("URL Streaming Audio") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editImageUrl, onValueChange = { editImageUrl = it }, label = { Text("URL Imagen / Logo") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editDesc, onValueChange = { editDesc = it }, label = { Text("Descripción") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (editName.isNotBlank() && editUrl.isNotBlank()) {
                        onSave(editName, editGenre, editUrl, editImageUrl, editDesc)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Guardar Cambios", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@Composable
fun AdminEditIptvDialog(
    iptv: IptvItem,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, url: String, logoUrl: String, country: String, isAdult: Boolean) -> Unit
) {
    var editName by remember { mutableStateOf(iptv.name) }
    var editCategory by remember { mutableStateOf(iptv.category) }
    var editUrl by remember { mutableStateOf(iptv.url) }
    var editLogoUrl by remember { mutableStateOf(iptv.logoUrl) }
    var editCountry by remember { mutableStateOf(iptv.country) }
    var editIsAdult by remember { mutableStateOf(iptv.isAdult) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .heightIn(max = 620.dp),
        containerColor = SpaceDarkBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = accentColor)
                Text("Editar Canal de IPTV", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Logo Preview Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .border(1.dp, accentColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (editLogoUrl.isNotBlank()) {
                            AsyncImage(
                                model = editLogoUrl,
                                contentDescription = "Vista Previa Logo IPTV",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                        }
                    }
                    Column {
                        Text("Vista Previa del Logotipo", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(if (editLogoUrl.isNotBlank()) "Logotipo cargado" else "Sin URL de imagen", color = Color.Gray, fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = editName, onValueChange = { editName = it }, label = { Text("Nombre Canal") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editCategory, onValueChange = { editCategory = it }, label = { Text("Categoría") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editUrl, onValueChange = { editUrl = it }, label = { Text("URL de Transmisión (.m3u8)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editLogoUrl, onValueChange = { editLogoUrl = it }, label = { Text("URL Logotipo") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editCountry, onValueChange = { editCountry = it }, label = { Text("País / Región") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = editIsAdult,
                        onCheckedChange = { editIsAdult = it },
                        colors = CheckboxDefaults.colors(checkedColor = accentColor)
                    )
                    Text("Contenido Adulto +18", color = Color.White, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (editName.isNotBlank() && editUrl.isNotBlank() && editCategory.isNotBlank()) {
                        onSave(editName, editCategory, editUrl, editLogoUrl, editCountry, editIsAdult)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Guardar Cambios", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@Composable
fun AdminEditMovieDialog(
    movie: MovieItem,
    accentColor: Color,
    viewModel: MediaViewModel,
    onDismiss: () -> Unit,
    onSave: (title: String, genre: String, url: String, posterUrl: String, description: String, year: Int, duration: String, isAdult: Boolean, rating: String) -> Unit
) {
    var editTitle by remember { mutableStateOf(movie.title) }
    var editGenre by remember { mutableStateOf(movie.genre) }
    var editUrl by remember { mutableStateOf(movie.url) }
    var editPosterUrl by remember { mutableStateOf(movie.posterUrl) }
    var editDescription by remember { mutableStateOf(movie.description) }
    var editYear by remember { mutableStateOf(movie.year.toString()) }
    var editDuration by remember { mutableStateOf(movie.duration) }
    var editRating by remember { mutableStateOf(movie.rating) }
    var editIsAdult by remember { mutableStateOf(movie.isAdult) }
    var isTmdbSearching by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .heightIn(max = 620.dp),
        containerColor = SpaceDarkBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Movie, contentDescription = null, tint = accentColor)
                Text("Editar Película", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Poster Preview Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .border(1.dp, accentColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (editPosterUrl.isNotBlank()) {
                            AsyncImage(
                                model = editPosterUrl,
                                contentDescription = "Vista Previa Poster",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                        }
                    }
                    Column {
                        Text("Vista Previa del Poster", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(if (editPosterUrl.isNotBlank()) "Poster cargado correctamente" else "Sin URL de poster", color = Color.Gray, fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (editTitle.isBlank()) {
                            Toast.makeText(context, "Ingresa el nombre para buscar en TMDb", Toast.LENGTH_SHORT).show()
                        } else {
                            isTmdbSearching = true
                            viewModel.searchTmdbMovie(editTitle) { result ->
                                isTmdbSearching = false
                                if (result != null) {
                                    editTitle = result.title
                                    editGenre = result.genre
                                    editDescription = result.synopsis
                                    editYear = result.year.toString()
                                    editDuration = result.duration
                                    editRating = result.rating
                                    if (result.posterUrl.isNotBlank()) editPosterUrl = result.posterUrl
                                    Toast.makeText(context, "¡Información cargada desde TMDb!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No se hallaron resultados en TMDb.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = !isTmdbSearching,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF01B4E4)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isTmdbSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buscando en TMDb...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🎬 Autocompletar con TMDb API", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedTextField(
                    value = editGenre, onValueChange = { editGenre = it }, label = { Text("Género") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editUrl, onValueChange = { editUrl = it }, label = { Text("URL Streaming Video (.mp4)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editPosterUrl, onValueChange = { editPosterUrl = it }, label = { Text("URL Imagen Poster") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editDescription, onValueChange = { editDescription = it }, label = { Text("Sinopsis") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editYear, onValueChange = { editYear = it }, label = { Text("Año") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editDuration, onValueChange = { editDuration = it }, label = { Text("Duración") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editRating, onValueChange = { editRating = it }, label = { Text("Rating") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = editIsAdult,
                        onCheckedChange = { editIsAdult = it },
                        colors = CheckboxDefaults.colors(checkedColor = accentColor)
                    )
                    Text("Contenido Adulto +18", color = Color.White, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedYear = editYear.toIntOrNull() ?: 2026
                    if (editTitle.isNotBlank() && editUrl.isNotBlank() && editGenre.isNotBlank()) {
                        onSave(editTitle, editGenre, editUrl, editPosterUrl, editDescription, parsedYear, editDuration, editIsAdult, editRating)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Guardar Cambios", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@Composable
fun AdminTestStreamDialog(
    title: String,
    subtitle: String,
    url: String,
    imageUrl: String,
    isVideo: Boolean,
    accentColor: Color,
    onDismiss: () -> Unit,
    onPlayInMainApp: () -> Unit
) {
    var isTestPlaying by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceDarkBackground,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = accentColor)
                    Text("Probar Transmisión", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(if (isVideo) Icons.Default.Tv else Icons.Default.Radio, contentDescription = null, tint = accentColor)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(subtitle, color = accentColor, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }

                // Player Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isVideo) 200.dp else 120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(1.dp, SpaceBorderColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        VideoPlayer(
                            url = url,
                            isPlaying = isTestPlaying,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Live Audio Stream Player via background VideoPlayer + Sound Visualizer
                        if (isTestPlaying) {
                            Box(modifier = Modifier.size(1.dp).alpha(0.01f)) {
                                VideoPlayer(
                                    url = url,
                                    isPlaying = true,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            SoundVisualizer(isPlaying = isTestPlaying, isBuffering = false)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isTestPlaying) "🟢 Reproduciendo prueba de audio en vivo..." else "⏸️ Prueba pausada",
                                color = if (isTestPlaying) Color.Green else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Stream URL display
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "URL: $url",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { isTestPlaying = !isTestPlaying },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isTestPlaying) Color.DarkGray else accentColor)
                    ) {
                        Icon(if (isTestPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isTestPlaying) "Pausar Prueba" else "Probar Transmisión", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPlayInMainApp,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Abrir en Reproductor Principal", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color.Gray)
            }
        }
    )
}

@Composable
fun ManageNewUserClientsPanel(
    viewModel: MediaViewModel,
    paymentRequests: List<com.example.models.UserPaymentRequest>,
    accentColor: Color
) {
    var actionMessage by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🌱 Clientes 'App Nuevo Usuario'",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Gestión de licencias, verificación de pagos y envío de códigos de activación por correo (medinplayhub@gmail.com).",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        if (actionMessage.isNotBlank()) {
            Surface(
                color = Color(0xFF1B5E20),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = actionMessage,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (paymentRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay solicitudes de clientes registradas por el momento.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            paymentRequests.reversed().forEach { req ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SpaceBorderColor, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header row: Email & Timestamp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = req.userEmail.ifBlank { "Cliente sin correo" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = req.timestamp,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Status Badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            // Payment Status
                            val (statusText, statusBg, statusFg) = when (req.status) {
                                "APPROVED" -> Triple("✅ PAGO APROBADO / CLIENTE ACTIVO", Color(0xFF2E7D32), Color.White)
                                "REJECTED" -> Triple("❌ PAGO RECHAZADO", Color(0xFFC62828), Color.White)
                                else -> Triple("⌛ PENDIENTE POR VERIFICAR PAGO", Color(0xFFF57F17), Color.Black)
                            }
                            Surface(color = statusBg, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusFg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Code Sent Status
                            val (codeText, codeBg, codeFg) = if (req.codeSent) {
                                Triple("📩 CÓDIGO ENVIADO", Color(0xFF1565C0), Color.White)
                            } else {
                                Triple("⚠️ CÓDIGO NO ENVIADO", Color(0xFFE65100), Color.White)
                            }
                            Surface(color = codeBg, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = codeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = codeFg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Payment Details
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF181512), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text("💳 Método: ${req.paymentMethodName}", fontSize = 12.sp, color = Color.LightGray)
                            Text("🔢 Referencia: ${req.referenceCode}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("💵 Monto: ${req.amount}", fontSize = 12.sp, color = Color.LightGray)
                            if (req.activationCode.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("🔑 Código de Activación: ${req.activationCode}", fontSize = 13.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                            }
                            if (req.codeSent && req.codeSentTimestamp.isNotBlank()) {
                                Text("🕒 Enviado el: ${req.codeSentTimestamp}", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Enviar / Reenviar Código Button
                            Button(
                                onClick = {
                                    viewModel.sendActivationCodeToClient(req.id)
                                    actionMessage = "¡Código ${req.activationCode.ifBlank { "de activación" }} enviado por correo a ${req.userEmail}!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (!req.codeSent) Color(0xFF2E7D32) else Color(0xFF1565C0)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (!req.codeSent) "✉️ Enviar Código de Activación al Cliente" else "✉️ Reenviar Código por Correo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (req.status != "APPROVED") {
                                    Button(
                                        onClick = {
                                            viewModel.approvePaymentRequest(req.id)
                                            actionMessage = "¡Pago aprobado y código enviado a ${req.userEmail}!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("✅ Aprobar Pago", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (req.status != "REJECTED") {
                                    Button(
                                        onClick = {
                                            viewModel.rejectPaymentRequest(req.id, "Comprobante no verificado")
                                            actionMessage = "Pago rechazado para ${req.userEmail}."
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("❌ Rechazar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.deletePaymentRequest(req.id)
                                        actionMessage = "Registro eliminado."
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                                    modifier = Modifier.weight(0.8f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Eliminar", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManagePaymentMethodsPanel(
    viewModel: MediaViewModel,
    paymentMethods: List<com.example.models.PaymentMethod>,
    accentColor: Color
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMethod by remember { mutableStateOf<com.example.models.PaymentMethod?>(null) }
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "💳 Métodos de Pago Habilitados",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Agrega, edita o elimina los métodos de pago que los nuevos usuarios verán para abonar su suscripción.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Agregar", fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B), fontSize = 12.sp)
            }
        }

        if (paymentMethods.isEmpty()) {
            Text(
                text = "No hay métodos de pago registrados. Agrega uno nuevo para que los clientes puedan realizar transferencias o pagos.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            paymentMethods.forEach { method ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SpaceBorderColor, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = SpaceDarkBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = accentColor)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = method.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = if (method.isActive) Color.Green.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (method.isActive) "ACTIVO" else "INACTIVO",
                                        color = if (method.isActive) Color.Green else Color.Red,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (method.details.isNotBlank()) {
                                Text(text = method.details, color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (method.instructions.isNotBlank()) {
                                Text(text = method.instructions, color = Color.LightGray, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { editingMethod = method }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.LightGray)
                            }
                            IconButton(onClick = {
                                viewModel.deletePaymentMethod(method.id)
                                android.widget.Toast.makeText(context, "Método de pago eliminado", android.widget.Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditPaymentMethodDialog(
            existingMethod = null,
            accentColor = accentColor,
            onDismiss = { showAddDialog = false },
            onSave = { title, details, instructions, isActive ->
                val newMethod = com.example.models.PaymentMethod(
                    id = "pm_${System.currentTimeMillis()}",
                    title = title,
                    details = details,
                    instructions = instructions,
                    isActive = isActive
                )
                viewModel.addPaymentMethod(newMethod)
                showAddDialog = false
                android.widget.Toast.makeText(context, "Método de pago registrado con éxito", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    editingMethod?.let { method ->
        AddEditPaymentMethodDialog(
            existingMethod = method,
            accentColor = accentColor,
            onDismiss = { editingMethod = null },
            onSave = { title, details, instructions, isActive ->
                val updated = method.copy(
                    title = title,
                    details = details,
                    instructions = instructions,
                    isActive = isActive
                )
                viewModel.updatePaymentMethod(updated)
                editingMethod = null
                android.widget.Toast.makeText(context, "Método de pago actualizado", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ManageSubscriptionPricePanel(
    viewModel: MediaViewModel,
    currentPrice: String,
    accentColor: Color
) {
    var priceInput by remember(currentPrice) { mutableStateOf(currentPrice) }
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "💲 Configurar Precio de la Licencia",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Text(
            text = "Establece el costo mensual de la licencia que deben abonar los usuarios nuevos para activar la aplicación. Este monto se mostrará en las instrucciones de pago.",
            color = Color.Gray,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SpaceDarkBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$", color = accentColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Precio Actual de la Licencia", color = Color.Gray, fontSize = 11.sp)
                    Text(
                        text = if (currentPrice.isBlank()) "$5.00 USD/mes" else if (currentPrice.contains("$")) currentPrice else "$$currentPrice USD/mes",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        OutlinedTextField(
            value = priceInput,
            onValueChange = { priceInput = it },
            label = { Text("Nuevo Precio (Ej: $5.00 USD/mes)") },
            placeholder = { Text("5.00 USD", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = SpaceBorderColor
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (priceInput.isNotBlank()) {
                    viewModel.setSubscriptionPrice(priceInput)
                    android.widget.Toast.makeText(context, "¡Precio de la licencia actualizado a $priceInput!", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Guardar Precio de Licencia", fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
        }
    }
}

@Composable
fun ManageSettingsAndUpdatePanel(
    viewModel: MediaViewModel,
    adminColor: Color
) {
    val disableSharingDialog by viewModel.disableSharingDialog.collectAsStateWithLifecycle()
    val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingSwitchState by remember { mutableStateOf<Boolean?>(null) }
    var showConfirmSwitchDialog by remember { mutableStateOf(false) }

    // Update publish state fields
    var versionCodeText by remember(appUpdateInfo) { mutableStateOf((appUpdateInfo?.versionCode ?: 2L).toString()) }
    var versionNameText by remember(appUpdateInfo) { mutableStateOf(appUpdateInfo?.versionName ?: "1.1.0") }
    var apkUrlText by remember(appUpdateInfo) { mutableStateOf(appUpdateInfo?.apkUrl ?: "") }
    var releaseNotesText by remember(appUpdateInfo) { mutableStateOf(appUpdateInfo?.releaseNotes ?: "") }
    var isForceUpdateCheck by remember(appUpdateInfo) { mutableStateOf(appUpdateInfo?.isForceUpdate ?: false) }

    if (showConfirmSwitchDialog && pendingSwitchState != null) {
        val nextDisabled = pendingSwitchState!!
        AlertDialog(
            onDismissRequest = {
                showConfirmSwitchDialog = false
                pendingSwitchState = null
            },
            containerColor = SpaceCardBg,
            title = {
                Text(
                    text = if (nextDisabled) "Confirmar Inhabilitación de Opciones de Compartir" else "Confirmar Habilitación de Opciones de Compartir",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = if (nextDisabled)
                        "Al INHABILITAR las opciones de compartir, cuando los usuarios presionen el botón de compartir se compartirá directamente la aplicación base con UN SOLO PERFIL (el principal), omitiendo el menú de opciones. Además, se ocultará el botón 'Compartir Usuario / Licencia App' en los ajustes de perfil.\n\n¿Deseas guardar este cambio?"
                    else
                        "Al HABILITAR las opciones de compartir, los usuarios podrán elegir qué perfil o licencia compartir mediante el diálogo emergente.\n\n¿Deseas guardar este cambio?",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setDisableSharingDialog(nextDisabled)
                        showConfirmSwitchDialog = false
                        pendingSwitchState = null
                        android.widget.Toast.makeText(context, "Ajuste de compartir actualizado", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = adminColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Confirmar y Guardar", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmSwitchDialog = false
                    pendingSwitchState = null
                }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("⚙️ Configuración Global del Sistema", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

        // Switch Card for Disabling Share Dialog
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceDarkBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, adminColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Inhabilitar Ventana de Compartir",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Comparte únicamente la APK base (Perfil Principal) y oculta el botón de compartir en ajustes de perfil.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = disableSharingDialog,
                        onCheckedChange = { newChecked ->
                            pendingSwitchState = newChecked
                            showConfirmSwitchDialog = true
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0F0D0B),
                            checkedTrackColor = adminColor,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = SpaceCardBg
                        )
                    )
                }
            }
        }

        HorizontalDivider(color = SpaceBorderColor, thickness = 1.dp)

        Text("🚀 Gestor de Actualización In-App (APK)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

        OutlinedTextField(
            value = versionCodeText,
            onValueChange = { versionCodeText = it },
            label = { Text("Código de Versión Remota (ej. 2)", color = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = adminColor, unfocusedBorderColor = SpaceBorderColor, focusedLabelColor = adminColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = versionNameText,
            onValueChange = { versionNameText = it },
            label = { Text("Nombre de Versión Remota (ej. 1.1.0)", color = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = adminColor, unfocusedBorderColor = SpaceBorderColor, focusedLabelColor = adminColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = apkUrlText,
            onValueChange = { apkUrlText = it },
            label = { Text("Enlace / URL del archivo APK público", color = Color.Gray) },
            supportingText = {
                Text(
                    text = "Acepta GitHub Releases (github.com/.../releases/download/...), Archive.org (archive.org/download/...), Google Drive, Dropbox o HTTP/HTTPS directo.",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = adminColor, unfocusedBorderColor = SpaceBorderColor, focusedLabelColor = adminColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = releaseNotesText,
            onValueChange = { releaseNotesText = it },
            label = { Text("Notas del Cambio / Novedades", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = adminColor, unfocusedBorderColor = SpaceBorderColor, focusedLabelColor = adminColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { isForceUpdateCheck = !isForceUpdateCheck }.padding(vertical = 4.dp)
        ) {
            Checkbox(
                checked = isForceUpdateCheck,
                onCheckedChange = { isForceUpdateCheck = it },
                colors = CheckboxDefaults.colors(checkedColor = adminColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Actualización Obligatoria (Force Update)", color = Color.White, fontSize = 13.sp)
        }

        Button(
            onClick = {
                val vCode = versionCodeText.toLongOrNull() ?: 1L
                if (apkUrlText.isBlank()) {
                    android.widget.Toast.makeText(context, "Ingresa una URL de APK válida", android.widget.Toast.LENGTH_SHORT).show()
                    return@Button
                }
                viewModel.publishAppUpdateInfo(
                    versionCode = vCode,
                    versionName = versionNameText,
                    apkUrl = apkUrlText,
                    releaseNotes = releaseNotesText,
                    isForce = isForceUpdateCheck
                )
                android.widget.Toast.makeText(context, "¡Información de actualización publicada!", android.widget.Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = adminColor),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Publish, contentDescription = null, tint = Color(0xFF0F0D0B))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publicar / Actualizar Versión Remota", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AppUpdateAlertDialog(
    updateInfo: com.example.models.AppUpdateInfo,
    isDownloading: Boolean,
    progress: Int,
    errorMessage: String?,
    onDownloadClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var showInterstitial by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(3) }

    LaunchedEffect(showInterstitial) {
        if (showInterstitial) {
            while (countdown > 0) {
                kotlinx.coroutines.delay(1000)
                countdown--
            }
            showInterstitial = false
            onDownloadClick()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.isMandatoryUpdate && !isDownloading && !showInterstitial) {
                onDismiss()
            }
        },
        containerColor = SpaceCardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color(0xFFC67C65))
                Text(
                    text = if (updateInfo.title.isNotBlank()) updateInfo.title else if (updateInfo.isMandatoryUpdate) "Actualización Obligatoria" else "Actualización Disponible",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (showInterstitial) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(color = Color(0xFFC67C65))
                            Text(
                                text = "Preparando enlace seguro...\nRedirigiendo en $countdown segundos",
                                color = Color.White,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (updateInfo.description.isNotBlank()) updateInfo.description else "Nueva versión v${updateInfo.versionName} disponible para descargar.",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (updateInfo.releaseNotes.isNotBlank()) {
                        Text(
                            text = "Novedades:\n${updateInfo.releaseNotes}",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    if (updateInfo.isMandatoryUpdate) {
                        Text(
                            text = "⚠️ Esta actualización es requerida para continuar utilizando la aplicación.",
                            color = Color(0xFFFFB74D),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Descargando actualización: $progress%", color = Color(0xFFC67C65), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFC67C65),
                            trackColor = SpaceBorderColor
                        )
                    }
                    if (errorMessage != null) {
                        Text(errorMessage, color = Color(0xFFE53935), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (!showInterstitial) {
                Button(
                    onClick = {
                        if (updateInfo.linkType.lowercase().trim() == "interstitial") {
                            countdown = 3
                            showInterstitial = true
                        } else {
                            onDownloadClick()
                        }
                    },
                    enabled = !isDownloading && updateInfo.apkUrl.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC67C65)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Descargando...", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF0F0D0B))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Descargar e Instalar APK", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (!updateInfo.isMandatoryUpdate && !isDownloading && !showInterstitial) {
                TextButton(onClick = onDismiss) {
                    Text("Más tarde", color = Color.Gray)
                }
            }
        }
    )
}

@Composable
fun AddEditPaymentMethodDialog(
    existingMethod: com.example.models.PaymentMethod?,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (title: String, details: String, instructions: String, isActive: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(existingMethod?.title ?: "") }
    var details by remember { mutableStateOf(existingMethod?.details ?: "") }
    var instructions by remember { mutableStateOf(existingMethod?.instructions ?: "") }
    var isActive by remember { mutableStateOf(existingMethod?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .heightIn(max = 580.dp),
        containerColor = SpaceCardBg,
        title = {
            Text(
                text = if (existingMethod == null) "Agregar Método de Pago" else "Editar Método de Pago",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre del Método (Ej: Zelle, Pago Móvil, Binance)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Datos / Titular / Teléfono / Correo") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instrucciones Adicionales (Opcional)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Método Activo", color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.5f))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, details, instructions, isActive)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Guardar", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@Composable
fun ProfileColorThemePicker(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    var activeCategory by remember { mutableStateOf("Metálicos") }
    
    val categoryList = listOf(
        "Metálicos" to "✨ Metálicos",
        "Fluorescentes" to "⚡ Fluorescentes",
        "Degradados" to "🌈 Degradados",
        "Clásicos" to "🎨 Clásicos"
    )
    
    val filteredColors = remember(activeCategory) {
        when (activeCategory) {
            "Metálicos" -> MetallicColorsList
            "Fluorescentes" -> FluorescentColorsList
            "Degradados" -> GradientColorsList
            else -> ClassicColorsList
        }
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "🎨 Estilo y Color de Acento del Perfil",
            fontSize = 13.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        // Category Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            items(categoryList) { (catKey, catTitle) ->
                val isCatSelected = activeCategory == catKey
                FilterChip(
                    selected = isCatSelected,
                    onClick = { activeCategory = catKey },
                    label = { Text(catTitle, fontSize = 11.sp, fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFC67C65),
                        selectedLabelColor = Color.White,
                        containerColor = SpaceDarkBackground,
                        labelColor = Color.Gray
                    )
                )
            }
        }
        
        // Color items in chosen category
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filteredColors) { option ->
                val isSelected = option.hex.equals(selectedColorHex, ignoreCase = true)
                val colorVal = try { Color(android.graphics.Color.parseColor(option.hex)) } catch (e: Exception) { Color(0xFFC67C65) }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onColorSelected(option.hex) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (option.category == "Degradados") {
                                    Brush.linearGradient(listOf(colorVal, colorVal.copy(alpha = 0.5f)))
                                } else {
                                    androidx.compose.ui.graphics.SolidColor(colorVal)
                                }
                            )
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else SpaceBorderColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            val isBright = (colorVal.red * 0.299f + colorVal.green * 0.587f + colorVal.blue * 0.114f) > 0.5f
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = if (isBright) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = option.name,
                        fontSize = 10.sp,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ManageProfilesPanel(
    viewModel: MediaViewModel,
    profiles: List<com.example.models.UserProfile>,
    accentColor: Color
) {
    val paymentRequests by viewModel.paymentRequests.collectAsStateWithLifecycle()
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Perfiles Locales, 1: Drill-down Usuarios App Nuevo Usuario
    
    var editingProfileId by remember { mutableStateOf<String?>(null) }
    var editName by remember { mutableStateOf("") }
    var editPin by remember { mutableStateOf("") }
    var editAdultPin by remember { mutableStateOf("") }
    var editAvatarName by remember { mutableStateOf("") }
    var editColorHex by remember { mutableStateOf("") }
    var editGender by remember { mutableStateOf("Masculino") }
    var editEmail by remember { mutableStateOf("") }
    var actionMessage by remember { mutableStateOf("") }

    Column {
        Text(
            text = "👥 Usuarios / Perfiles",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Gestión de perfiles del sistema y consulta detallada por cliente de 'App Nuevo Usuario'.",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // SubTab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                label = { Text("👤 Perfiles de Dispositivo (${profiles.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.White)
            )
            FilterChip(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                label = { Text("🌱 Drill-down Clientes (${paymentRequests.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accentColor, selectedLabelColor = Color.White)
            )
        }

        if (actionMessage.isNotBlank()) {
            Surface(
                color = Color(0xFF1B5E20),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    text = actionMessage,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        if (selectedSubTab == 0) {
            // Local Device Profiles List
            if (profiles.isEmpty()) {
                Text(
                    text = "No hay perfiles configurados en este dispositivo.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                profiles.forEach { profile ->
                    val isEditing = editingProfileId == profile.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(1.dp, if (isEditing) accentColor else SpaceBorderColor, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = SpaceDarkBackground)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (isEditing) {
                                Text("Editar Perfil: ${profile.name}", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("Nombre del Perfil") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = Color.Gray
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Sexo / Género (Para insignia del perfil)", color = Color.Gray, fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FilterChip(
                                        selected = editGender.lowercase() == "masculino",
                                        onClick = { editGender = "Masculino" },
                                        label = { Text("👑 Masculino", fontSize = 12.sp) }
                                    )
                                    FilterChip(
                                        selected = editGender.lowercase() == "femenino",
                                        onClick = { editGender = "Femenino" },
                                        label = { Text("👸 Femenino", fontSize = 12.sp) }
                                    )
                                }

                                OutlinedTextField(
                                    value = editEmail,
                                    onValueChange = { editEmail = it },
                                    label = { Text("Correo Electrónico del Cliente") },
                                    placeholder = { Text("ejemplo@correo.com") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = Color.Gray
                                    ),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = editPin,
                                    onValueChange = { input ->
                                        if (input.all { it.isLetterOrDigit() } && input.length <= 12) {
                                            editPin = input
                                        }
                                    },
                                    label = { Text("Clave de Acceso (Opcional)") },
                                    placeholder = { Text("Ej. 1234 o mi-clave") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = Color.Gray
                                    ),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = editAdultPin,
                                    onValueChange = { input ->
                                        if (input.all { it.isLetterOrDigit() } && input.length <= 12) {
                                            editAdultPin = input
                                        }
                                    },
                                    label = { Text("Clave Control Parental / +18 (Opcional)") },
                                    placeholder = { Text("Ej. 9988 o mi-parental") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = Color.Gray
                                    ),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                ProfileColorThemePicker(
                                    selectedColorHex = editColorHex.ifBlank { AccentColorsList.first() },
                                    onColorSelected = { editColorHex = it }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { editingProfileId = null }) {
                                        Text("Cancelar", color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.updateProfile(
                                                profileId = profile.id,
                                                newName = editName.trim(),
                                                newAvatar = editAvatarName,
                                                newColorHex = editColorHex,
                                                newPin = editPin,
                                                newAdultPin = editAdultPin,
                                                gender = editGender,
                                                email = editEmail.trim()
                                            )
                                            editingProfileId = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                    ) {
                                        Text("Guardar", color = Color.White)
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val avatarIcon = getAvatarIcon(profile.avatarName)
                                        val avatarColor = getAvatarColor(profile.avatarName)
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(avatarColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                                .border(1.dp, avatarColor, RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = avatarIcon,
                                                contentDescription = null,
                                                tint = avatarColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(profile.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(if (profile.gender.equals("Femenino", ignoreCase = true)) "👸" else "👑", fontSize = 12.sp)
                                            }
                                            Text(
                                                text = buildString {
                                                    if (profile.email.isNotBlank()) append("${profile.email} • ")
                                                    append("Clave: ")
                                                    if (profile.pin.isEmpty()) append("Ninguna") else append(profile.pin)
                                                    append(" • +18: ")
                                                    if (profile.adultPin.isEmpty()) append("Ninguna") else append(profile.adultPin)
                                                },
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingProfileId = profile.id
                                                editName = profile.name
                                                editPin = profile.pin
                                                editAdultPin = profile.adultPin
                                                editAvatarName = profile.avatarName
                                                editColorHex = profile.accentColorHex
                                                editGender = profile.gender
                                                editEmail = profile.email
                                            }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = accentColor)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteProfile(profile.id) }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Drill-down View: Clients from "App Nuevo Usuario" and their linked profiles
            if (paymentRequests.isEmpty()) {
                Text("No hay registros de clientes 'App Nuevo Usuario' aún.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                paymentRequests.reversed().forEach { req ->
                    val isApproved = req.status == "APPROVED"
                    val linkedProfiles = profiles.filter { it.email.equals(req.userEmail, ignoreCase = true) }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SpaceDarkBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(1.dp, SpaceBorderColor, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header Client Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = req.userEmail.ifBlank { "Sin Correo Registrado" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                                Surface(
                                    color = if (isApproved) Color(0xFF2E7D32) else Color(0xFFE65100),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isApproved) "✅ ACTIVO" else "⌛ PENDIENTE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Subscription Details
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF181512), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text("💳 Método: ${req.paymentMethodName} | Ref: ${req.referenceCode}", fontSize = 11.sp, color = Color.LightGray)
                                Text("🔑 Código: ${req.activationCode.ifBlank { "Pendiente" }}", fontSize = 11.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                                if (req.subscriptionStartDate.isNotBlank() && req.subscriptionEndDate.isNotBlank()) {
                                    Text("📅 Licencia: ${req.subscriptionStartDate} al ${req.subscriptionEndDate}", fontSize = 11.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Linked Profiles Drill-Down
                            Text("👤 Perfil Asociado (App Nuevo Usuario):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            if (linkedProfiles.isEmpty()) {
                                Text("• Perfil Principal (Predeterminado al activar la app)", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                            } else {
                                linkedProfiles.forEach { p ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(start = 8.dp, top = 4.dp)
                                            .fillMaxWidth()
                                    ) {
                                        Text("• ${p.name}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text(" (${p.gender})", fontSize = 11.sp, color = Color.Gray)
                                        if (p.pin.isNotBlank()) {
                                            Text(" • Clave: ${p.pin}", fontSize = 11.sp, color = Color(0xFFFFD54F))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.sendActivationCodeToClient(req.id)
                                        actionMessage = "✉️ Código de activación enviado por correo a ${req.userEmail}"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Enviar Código", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                if (!isApproved) {
                                    Button(
                                        onClick = {
                                            viewModel.approvePaymentRequest(req.id)
                                            actionMessage = "✅ Licencia activada para ${req.userEmail}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Aprobar Licencia", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ---------------------- GENERIC EMPTY STATE COMPONENT ----------------------
@Composable
fun EmptyStatePlaceholder(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

enum class GestureType { BRIGHTNESS, VOLUME }

@Composable
fun GestureVolumeBrightnessBox(
    modifier: Modifier = Modifier,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    isFloatingPip: Boolean = false,
    onTap: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    var gestureType by remember { mutableStateOf<GestureType?>(null) }
    var isGestureActive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val currentBrightness = rememberUpdatedState(brightness)
    val currentVolume = rememberUpdatedState(volume)
    val currentOnBrightnessChange = rememberUpdatedState(onBrightnessChange)
    val currentOnVolumeChange = rememberUpdatedState(onVolumeChange)
    val currentOnTap = rememberUpdatedState(onTap)

    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager }

    Box(
        modifier = modifier
            .pointerInput(isFloatingPip) {
                if (isFloatingPip) return@pointerInput
                detectTapGestures(
                    onTap = {
                        currentOnTap.value()
                    }
                )
            }
            .pointerInput(isFloatingPip) {
                if (isFloatingPip) return@pointerInput

                var startValue = 0f
                var accumulatedDelta = 0f

                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val isLeft = offset.x < size.width / 2f
                        gestureType = if (isLeft) GestureType.BRIGHTNESS else GestureType.VOLUME
                        isGestureActive = true
                        hideJob?.cancel()

                        if (isLeft) {
                            val curAttrB = activity?.window?.attributes?.screenBrightness ?: -1f
                            startValue = if (curAttrB >= 0f) curAttrB else currentBrightness.value
                        } else {
                            val maxV = audioManager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 1
                            val curV = audioManager?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 0
                            val sysPercent = (curV.toFloat() / maxV.toFloat()).coerceIn(0f, 1f)
                            startValue = sysPercent
                        }
                        accumulatedDelta = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val height = size.height.toFloat().coerceAtLeast(1f)
                        accumulatedDelta += -dragAmount / height
                        val targetVal = (startValue + accumulatedDelta).coerceIn(0.0f, 1.0f)
                        if (gestureType == GestureType.BRIGHTNESS) {
                            val clampedB = targetVal.coerceIn(0.02f, 1.0f)
                            currentOnBrightnessChange.value(clampedB)
                        } else {
                            currentOnVolumeChange.value(targetVal)
                        }
                    },
                    onDragEnd = {
                        hideJob = scope.launch {
                            delay(1200)
                            isGestureActive = false
                        }
                    },
                    onDragCancel = {
                        hideJob = scope.launch {
                            delay(1200)
                            isGestureActive = false
                        }
                    }
                )
            }
    ) {
        content()

        // Gesture HUD Overlay
        AnimatedVisibility(
            visible = isGestureActive && gestureType != null && !isFloatingPip,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(if (gestureType == GestureType.BRIGHTNESS) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 28.dp)
        ) {
            val isBrightness = gestureType == GestureType.BRIGHTNESS
            val curVol = currentVolume.value
            val curBright = currentBrightness.value
            val icon = if (isBrightness) {
                Icons.Default.WbSunny
            } else {
                if (curVol == 0f) Icons.Default.VolumeOff else if (curVol < 0.5f) Icons.Default.VolumeDown else Icons.Default.VolumeUp
            }
            val title = if (isBrightness) "Brillo" else "Volumen"
            val valPercent = if (isBrightness) (curBright * 100).toInt() else (curVol * 100).toInt()
            val activeColor = if (isBrightness) Color(0xFFFFB74D) else Color(0xFF64B5F6)

            Surface(
                color = Color.Black.copy(alpha = 0.82f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SpaceBorderColor),
                shadowElevation = 8.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(vertical = 18.dp, horizontal = 16.dp)
                        .width(56.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = activeColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Vertical Progress Bar
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(90.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction = if (isBrightness) curBright.coerceIn(0f, 1f) else curVol.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(3.dp))
                                .background(activeColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "$valPercent%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------------- HIGH FIDELITY IPTV FULLSCREEN PLAYER ----------------------
@Composable
fun IptvPlayerOverlay(
    iptv: IptvItem,
    onClose: () -> Unit,
    viewModel: MediaViewModel,
    currentProfile: UserProfile,
    accentColor: Color,
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {}
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isFavorite = currentProfile.favoriteIptvIds.contains(iptv.id)
    val playerSizeMode by viewModel.playerSizeMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(isLandscape) {
        if (isLandscape) {
            viewModel.setPlayerSizeMode(com.example.ui.PlayerSizeMode.Fullscreen)
        } else {
            viewModel.setPlayerSizeMode(com.example.ui.PlayerSizeMode.Normal)
        }
    }

    val activity = context as? android.app.Activity

    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    var brightness by remember {
        val curAttrB = activity?.window?.attributes?.screenBrightness ?: -1f
        val initialB = if (curAttrB < 0f) {
            try {
                android.provider.Settings.System.getInt(
                    context.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS
                ) / 255f
            } catch (e: Exception) {
                0.5f
            }
        } else curAttrB
        mutableStateOf(initialB.coerceIn(0.05f, 1.0f))
    }

    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager }
    var volume by remember {
        val currentSysVol = audioManager?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 0
        val maxSysVol = audioManager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 1
        mutableStateOf((currentSysVol.toFloat() / maxSysVol.toFloat()).coerceIn(0f, 1f))
    }
    var isMuted by remember { mutableStateOf(false) }

    // Remove early return if (isInPipMode)
    var selectedResolution by remember { mutableStateOf("1080p") }
    var showCastOptionsDialog by remember { mutableStateOf(false) }
    var channelSearchQuery by remember { mutableStateOf("") }

    if (showCastOptionsDialog) {
        CastOptionsDialog(
            streamUrl = iptv.url,
            streamTitle = iptv.name,
            accentColor = accentColor,
            onDismiss = { showCastOptionsDialog = false }
        )
    }
    var showQualities by remember { mutableStateOf(false) }

    // Channel drawer toggle
    var showChannelsDrawer by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(controlsVisible, playerSizeMode) {
        if (controlsVisible && playerSizeMode == com.example.ui.PlayerSizeMode.Fullscreen) {
            delay(3000)
            controlsVisible = false
        }
    }
    val allChannels by viewModel.iptvList.collectAsStateWithLifecycle()
    val isAdultUnlocked by viewModel.isAdultUnlocked.collectAsStateWithLifecycle()

    val allowedChannelsForNav = remember(allChannels, iptv, isAdultUnlocked) {
        val playingIsAdult = iptv.isAdultChannel
        if (isAdultUnlocked && playingIsAdult) {
            allChannels.filter { it.isAdultChannel }
        } else {
            allChannels.filter { !it.isAdultChannel }
        }
    }

    val isFloatingPip = isInPipMode || playerSizeMode == com.example.ui.PlayerSizeMode.PiP

    GestureVolumeBrightnessBox(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        volume = volume,
        onVolumeChange = { newV ->
            volume = newV
            val maxVol = audioManager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 100
            val targetIndex = (newV * maxVol).toInt().coerceIn(0, maxVol)
            audioManager?.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetIndex, 0)
        },
        brightness = brightness,
        onBrightnessChange = { newB ->
            brightness = newB
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = newB.coerceIn(0.02f, 1.0f)
            }
        },
        isFloatingPip = isFloatingPip,
        onTap = {
            if (!isFloatingPip) controlsVisible = !controlsVisible
        }
    ) {
        // SINGLE PERSISTENT UNIFIED VIDEO PLAYER
        VideoPlayer(
            url = iptv.url,
            isPlaying = isPlaying,
            volume = volume,
            isMuted = isMuted,
            modifier = Modifier.fillMaxSize()
        )

        if (isFloatingPip) {
            // Clean Picture-in-Picture display with no overlays or buttons
        } else {
            // Header overlay: Back, Title, Favorite
        AnimatedVisibility(
            visible = playerSizeMode != com.example.ui.PlayerSizeMode.Fullscreen || controlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = iptv.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "SEÑAL EN VIVO • ${iptv.category}",
                        color = Color.Green,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleFavoriteIptv(iptv.id) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) Color.Red else Color.White
                    )
                }
            }
        }

        // Bottom Custom Media HUD
        AnimatedVisibility(
            visible = playerSizeMode != com.example.ui.PlayerSizeMode.Fullscreen || controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val hideSystemBars = isLandscape || playerSizeMode == com.example.ui.PlayerSizeMode.PiP || isInPipMode
            Column(
                modifier = Modifier
                    .then(if (hideSystemBars) Modifier.navigationBarsPadding() else Modifier)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(SpaceCardBg)
                    .border(1.dp, SpaceBorderColor, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(18.dp)
            ) {
                // Live progress simulation (Shows continuous timeline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                        Text("LIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("HLS Streaming Link Active", color = Color.Gray, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main Controls (Adaptive based on vertical/horizontal orientation)
                if (!isLandscape) {
                    // Vertical View Layout (Split into 2 clean rows to prevent layout overflow)
                    // Row 1: Playback controllers centered
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val index = allowedChannelsForNav.indexOfFirst { it.id == iptv.id }
                                if (index > 0) {
                                    viewModel.playIptv(allowedChannelsForNav[index - 1])
                                }
                            },
                            enabled = allowedChannelsForNav.indexOfFirst { it.id == iptv.id } > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Anterior",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                                .clickable { viewModel.pauseOrResumeCurrentPlayback() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Reproducir",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = {
                                val index = allowedChannelsForNav.indexOfFirst { it.id == iptv.id }
                                if (index >= 0 && index < allowedChannelsForNav.size - 1) {
                                    viewModel.playIptv(allowedChannelsForNav[index + 1])
                                }
                            },
                            enabled = allowedChannelsForNav.indexOfFirst { it.id == iptv.id } < allowedChannelsForNav.size - 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Siguiente",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 2: Volume on left, sizes and channels drawer on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Volume Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.width(120.dp)
                        ) {
                            IconButton(onClick = { isMuted = !isMuted }) {
                                Icon(
                                    imageVector = if (isMuted || volume == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                    contentDescription = "Volumen",
                                    tint = Color.White
                                )
                            }
                            Slider(
                                value = if (isMuted) 0f else volume,
                                onValueChange = {
                                    volume = it
                                    isMuted = false
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor,
                                    inactiveTrackColor = SpaceBorderColor
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Right: PiP, Fullscreen & Channels Drawer toggles
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { onEnterPip() }) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "PIP",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(onClick = {
                                val activity = context as? android.app.Activity
                                if (isLandscape) {
                                    activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    viewModel.setPlayerSizeMode(com.example.ui.PlayerSizeMode.Normal)
                                } else {
                                    activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    viewModel.setPlayerSizeMode(com.example.ui.PlayerSizeMode.Fullscreen)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Pantalla Completa",
                                    tint = if (playerSizeMode == com.example.ui.PlayerSizeMode.Fullscreen) accentColor else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(onClick = {
                                showCastOptionsDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = "Transmitir",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(onClick = { showChannelsDrawer = !showChannelsDrawer }) {
                                Icon(
                                    imageVector = Icons.Default.MenuOpen,
                                    contentDescription = "Canales",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Landscape View Layout - Perfectly horizontally centered using weight-based layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Volume
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.width(120.dp)
                            ) {
                                IconButton(onClick = { isMuted = !isMuted }) {
                                    Icon(
                                        imageVector = if (isMuted || volume == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                        contentDescription = "Volumen",
                                        tint = Color.White
                                    )
                                }
                                Slider(
                                    value = if (isMuted) 0f else volume,
                                    onValueChange = {
                                        volume = it
                                        isMuted = false
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = accentColor,
                                        activeTrackColor = accentColor,
                                        inactiveTrackColor = SpaceBorderColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Center: Playback Controllers (Perfectly Centered horizontally relative to screen borders)
                        Box(
                            modifier = Modifier.weight(1.5f),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val index = allowedChannelsForNav.indexOfFirst { it.id == iptv.id }
                                        if (index > 0) {
                                            viewModel.playIptv(allowedChannelsForNav[index - 1])
                                        }
                                    },
                                    enabled = allowedChannelsForNav.indexOfFirst { it.id == iptv.id } > 0
                                ) {
                                    Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(28.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                        .clickable { viewModel.pauseOrResumeCurrentPlayback() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Reproducir",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val index = allowedChannelsForNav.indexOfFirst { it.id == iptv.id }
                                        if (index >= 0 && index < allowedChannelsForNav.size - 1) {
                                            viewModel.playIptv(allowedChannelsForNav[index + 1])
                                        }
                                    },
                                    enabled = allowedChannelsForNav.indexOfFirst { it.id == iptv.id } < allowedChannelsForNav.size - 1
                                ) {
                                    Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }
                        }

                        // Right: Actions (PiP replaced the AspectRatio Normal button)
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(onClick = { onEnterPip() }) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "PIP",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(onClick = {
                                    val activity = context as? android.app.Activity
                                    if (isLandscape) {
                                        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        viewModel.setPlayerSizeMode(com.example.ui.PlayerSizeMode.Normal)
                                    } else {
                                        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        viewModel.setPlayerSizeMode(com.example.ui.PlayerSizeMode.Fullscreen)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Pantalla Completa",
                                        tint = if (playerSizeMode == com.example.ui.PlayerSizeMode.Fullscreen) accentColor else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(onClick = {
                                    showCastOptionsDialog = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = "Transmitir",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(onClick = { showChannelsDrawer = !showChannelsDrawer }) {
                                    Icon(imageVector = Icons.Default.MenuOpen, contentDescription = "Canales", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Channels Switch Drawer
        if (showChannelsDrawer) {
            val filteredDrawerChannels = remember(allowedChannelsForNav, channelSearchQuery) {
                if (channelSearchQuery.isBlank()) allowedChannelsForNav
                else allowedChannelsForNav.filter {
                    it.name.contains(channelSearchQuery, ignoreCase = true) ||
                    it.category.contains(channelSearchQuery, ignoreCase = true) ||
                    it.country.contains(channelSearchQuery, ignoreCase = true)
                }
            }

            if (isLandscape) {
                // Horizontal View: Left-side channel guide drawer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showChannelsDrawer = false },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(320.dp)
                            .clickable(enabled = false) {},
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                        border = BorderStroke(1.dp, SpaceBorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Guía de Canales",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                IconButton(onClick = { showChannelsDrawer = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = channelSearchQuery,
                                onValueChange = { channelSearchQuery = it },
                                placeholder = { Text("Buscar canal...", color = Color.Gray, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = SpaceBorderColor
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(filteredDrawerChannels) { channel ->
                                    val isSelected = channel.id == iptv.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable {
                                                viewModel.playIptv(channel)
                                                showChannelsDrawer = false
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AsyncImage(
                                            model = channel.logoUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(channel.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("${channel.category} • ${channel.country}", color = Color.Gray, fontSize = 10.sp)
                                        }

                                        if (isSelected) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Active", tint = accentColor)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Dialog(onDismissRequest = { showChannelsDrawer = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .border(1.dp, SpaceBorderColor, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SpaceCardBg)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Guía de Canales",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = channelSearchQuery,
                                onValueChange = { channelSearchQuery = it },
                                placeholder = { Text("Buscar canal...", color = Color.Gray, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = SpaceBorderColor
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(filteredDrawerChannels) { channel ->
                                    val isSelected = channel.id == iptv.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable {
                                                viewModel.playIptv(channel)
                                                showChannelsDrawer = false
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AsyncImage(
                                            model = channel.logoUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(channel.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("${channel.category} • ${channel.country}", color = Color.Gray, fontSize = 10.sp)
                                        }

                                        if (isSelected) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Active", tint = accentColor)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun EarthBroadcastingCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "earth_rotation"
    )

    val signalPulse = infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 160f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "signal_pulse"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)

        // Draw deep starry space
        drawRect(color = Color(0xFF02040A))

        // Signal pulses
        drawCircle(
            color = Color(0xFF1E88E5).copy(alpha = (1f - (signalPulse.value / 160f)).coerceIn(0f, 1f)),
            radius = signalPulse.value,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = Color(0xFF1E88E5).copy(alpha = (1f - ((signalPulse.value + 40f) % 160f / 160f)).coerceIn(0f, 1f)),
            radius = (signalPulse.value + 40f) % 160f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )

        // Draw Earth backdrop core
        drawCircle(
            color = Color(0xFF0D1B2A),
            radius = 60.dp.toPx(),
            center = center
        )

        // Draw abstract continent waves rotating inside
        drawCircle(
            color = Color(0xFF1B4965),
            radius = 58.dp.toPx(),
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
        )

        // Satellite icons orbiting or static items
        drawCircle(
            color = Color(0xFF00ACC1),
            radius = 6.dp.toPx(),
            center = Offset(center.x + 90.dp.toPx() * kotlin.math.cos(Math.toRadians(rotation.value.toDouble())).toFloat(),
                           center.y + 90.dp.toPx() * kotlin.math.sin(Math.toRadians(rotation.value.toDouble())).toFloat())
        )
    }
}


// ---------------------- HIGH FIDELITY MOVIE FULLSCREEN PLAYER ----------------------
@Composable
fun MoviePlayerOverlay(
    movie: MovieItem,
    onClose: () -> Unit,
    viewModel: MediaViewModel,
    currentProfile: UserProfile,
    accentColor: Color,
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {}
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isFavorite = currentProfile.favoriteMovieIds.contains(movie.id)
    val playerSizeMode by viewModel.playerSizeMode.collectAsStateWithLifecycle()
    val startMillis by viewModel.movieStartMillis.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(isLandscape) {
        if (isLandscape) {
            viewModel.setPlayerSizeMode(com.example.ui.PlayerSizeMode.Fullscreen)
        } else {
            viewModel.setPlayerSizeMode(com.example.ui.PlayerSizeMode.Normal)
        }
    }

    val activity = context as? android.app.Activity

    var brightness by remember {
        val curAttrB = activity?.window?.attributes?.screenBrightness ?: -1f
        val initialB = if (curAttrB < 0f) {
            try {
                android.provider.Settings.System.getInt(
                    context.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS
                ) / 255f
            } catch (e: Exception) {
                0.5f
            }
        } else curAttrB
        mutableStateOf(initialB.coerceIn(0.05f, 1.0f))
    }

    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager }
    var volume by remember {
        val currentSysVol = audioManager?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 0
        val maxSysVol = audioManager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 1
        mutableStateOf((currentSysVol.toFloat() / maxSysVol.toFloat()).coerceIn(0f, 1f))
    }
    var isMuted by remember { mutableStateOf(false) }
    var seekToMillis by remember(movie.id) { mutableStateOf(startMillis) }

    // Remove early return if (isInPipMode)
    var selectedResolution by remember { mutableStateOf("1080p") }
    var showCastOptionsDialog by remember { mutableStateOf(false) }
    var movieSearchQuery by remember { mutableStateOf("") }
    var showQualities by remember { mutableStateOf(false) }

    if (showCastOptionsDialog) {
        CastOptionsDialog(
            streamUrl = movie.url,
            streamTitle = movie.title,
            accentColor = accentColor,
            onDismiss = { showCastOptionsDialog = false }
        )
    }

    // Movie list drawer toggle
    var showMoviesDrawer by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(controlsVisible, playerSizeMode) {
        if (controlsVisible && playerSizeMode == com.example.ui.PlayerSizeMode.Fullscreen) {
            delay(3000)
            controlsVisible = false
        }
    }
    val allMovies by viewModel.movies.collectAsStateWithLifecycle()
    val isAdultUnlocked by viewModel.isAdultUnlocked.collectAsStateWithLifecycle()

    val allowedMoviesForNav = remember(allMovies, movie, isAdultUnlocked) {
        val playingIsAdult = movie.isAdultMovie
        if (isAdultUnlocked && playingIsAdult) {
            allMovies.filter { it.isAdultMovie }
        } else {
            allMovies.filter { !it.isAdultMovie }
        }
    }

    var currentMillis by remember(movie.id, startMillis) { mutableStateOf(startMillis) }
    var totalMillis by remember(movie.id, movie.duration) {
        mutableStateOf(
            try {
                val durationStr = movie.duration
                if (durationStr.contains("h", ignoreCase = true)) {
                    val parts = durationStr.split(Regex("[hH]"))
                    val hours = parts.getOrNull(0)?.trim()?.filter { it.isDigit() }?.toLongOrNull() ?: 0L
                    val minutes = parts.getOrNull(1)?.trim()?.filter { it.isDigit() }?.toLongOrNull() ?: 0L
                    (hours * 60 + minutes) * 60 * 1000L
                } else {
                    val minutes = durationStr.filter { it.isDigit() }.toLongOrNull() ?: 14L
                    minutes * 60 * 1000L
                }
            } catch (e: Exception) {
                840000L
            }
        )
    }

    var isUserSeeking by remember { mutableStateOf(false) }
    var bufferPercent by remember(movie.id) { mutableIntStateOf(0) }

    DisposableEffect(movie.id) {
        onDispose {
            viewModel.saveMovieProgress(currentProfile.id, movie.id, currentMillis)
        }
    }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    val progress = if (totalMillis > 0) (currentMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f) else 0f

    fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = ms / (1000 * 60 * 60)
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    val isFloatingPip = isInPipMode || playerSizeMode == com.example.ui.PlayerSizeMode.PiP

    GestureVolumeBrightnessBox(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        volume = volume,
        onVolumeChange = { newV ->
            volume = newV
            val maxVol = audioManager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 100
            val targetIndex = (newV * maxVol).toInt().coerceIn(0, maxVol)
            audioManager?.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetIndex, 0)
        },
        brightness = brightness,
        onBrightnessChange = { newB ->
            brightness = newB
            activity?.window?.attributes = activity?.window?.attributes?.apply {
                screenBrightness = newB.coerceIn(0.02f, 1.0f)
            }
        },
        isFloatingPip = isFloatingPip,
        onTap = {
            if (!isFloatingPip) controlsVisible = !controlsVisible
        }
    ) {
        // SINGLE PERSISTENT UNIFIED VIDEO PLAYER
        VideoPlayer(
            url = movie.url,
            isPlaying = isPlaying,
            volume = volume,
            isMuted = isMuted,
            seekToMillis = seekToMillis,
            modifier = Modifier.fillMaxSize(),
            onProgressUpdate = { current, total ->
                if (!isUserSeeking && current >= 0) {
                    currentMillis = current
                    if (total > 0) {
                        totalMillis = total
                    }
                }
            },
            onBufferUpdate = { percent ->
                bufferPercent = percent
            }
        )

        if (isFloatingPip) {
            // Clean video view for PiP mode: no floating control buttons or navigation overlays
        } else {
            // Rating hidden in horizontal mode per requirement

            // Header Toolbar overlay
            AnimatedVisibility(
                visible = playerSizeMode != com.example.ui.PlayerSizeMode.Fullscreen || controlsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = movie.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "CINE • ${movie.genre}",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleFavoriteMovie(movie.id) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) Color.Red else Color.White
                        )
                    }
                }
            }

            // Bottom cinematic player controls console
            AnimatedVisibility(
                visible = playerSizeMode != com.example.ui.PlayerSizeMode.Fullscreen || controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val hideSystemBars = isLandscape || playerSizeMode == com.example.ui.PlayerSizeMode.PiP || isInPipMode
                Column(
                    modifier = Modifier
                        .then(if (hideSystemBars) Modifier.navigationBarsPadding() else Modifier)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(SpaceCardBg)
                        .border(1.dp, SpaceBorderColor, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Timeline Scrubber Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(formatTime(currentMillis), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Custom Layered Track Bar (Inactive, Buffer, Active)
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                            ) {
                                val trackHeight = size.height
                                val cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2, trackHeight / 2)

                                // 1. Background inactive track
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.2f),
                                    size = size,
                                    cornerRadius = cornerRadius
                                )

                                // 2. Buffer track (loaded buffer percentage)
                                val bufferFraction = (bufferPercent / 100f).coerceIn(0f, 1f)
                                if (bufferFraction > 0f) {
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.5f),
                                        size = androidx.compose.ui.geometry.Size(size.width * bufferFraction, trackHeight),
                                        cornerRadius = cornerRadius
                                    )
                                }

                                // 3. Active played position track
                                val activeFraction = if (totalMillis > 0) (currentMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f) else 0f
                                if (activeFraction > 0f) {
                                    drawRoundRect(
                                        color = accentColor,
                                        size = androidx.compose.ui.geometry.Size(size.width * activeFraction, trackHeight),
                                        cornerRadius = cornerRadius
                                    )
                                }
                            }

                            Slider(
                                value = progress,
                                onValueChange = {
                                    val target = (it * totalMillis).toLong()
                                    currentMillis = target
                                    seekToMillis = target
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = Color.Transparent,
                                    inactiveTrackColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        val remainingMillis = (totalMillis - currentMillis).coerceAtLeast(0L)
                        Text("-" + formatTime(remainingMillis), color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isLandscape) {
                        // Vertical View Layout (2 clean rows)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val index = allowedMoviesForNav.indexOfFirst { it.id == movie.id }
                                    if (index > 0) {
                                        viewModel.playMovie(allowedMoviesForNav[index - 1])
                                    }
                                },
                                enabled = allowedMoviesForNav.indexOfFirst { it.id == movie.id } > 0
                            ) {
                                Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(28.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            IconButton(onClick = {
                                val target = (currentMillis - 10000L).coerceAtLeast(0L)
                                currentMillis = target
                                seekToMillis = target
                            }) {
                                Icon(imageVector = Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(28.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                                    .clickable { viewModel.pauseOrResumeCurrentPlayback() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            IconButton(onClick = {
                                val target = (currentMillis + 10000L).coerceAtLeast(0L).coerceAtMost(totalMillis)
                                currentMillis = target
                                seekToMillis = target
                            }) {
                                Icon(imageVector = Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(28.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            IconButton(
                                onClick = {
                                    val index = allowedMoviesForNav.indexOfFirst { it.id == movie.id }
                                    if (index >= 0 && index < allowedMoviesForNav.size - 1) {
                                        viewModel.playMovie(allowedMoviesForNav[index + 1])
                                    }
                                },
                                enabled = allowedMoviesForNav.indexOfFirst { it.id == movie.id } < allowedMoviesForNav.size - 1
                            ) {
                                Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Volume
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.width(120.dp)
                            ) {
                                IconButton(onClick = { isMuted = !isMuted }) {
                                    Icon(
                                        imageVector = if (isMuted || volume == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                        contentDescription = "Volumen",
                                        tint = Color.White
                                    )
                                }
                                Slider(
                                    value = if (isMuted) 0f else volume,
                                    onValueChange = {
                                        volume = it
                                        isMuted = false
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = accentColor,
                                        activeTrackColor = accentColor,
                                        inactiveTrackColor = SpaceBorderColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Right Actions
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(onClick = { onEnterPip() }) {
                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "PIP", tint = Color.White, modifier = Modifier.size(22.dp))
                                }

                                IconButton(onClick = {
                                    val activity = context as? android.app.Activity
                                    if (isLandscape) {
                                        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        viewModel.setPlayerSizeMode(com.example.ui.PlayerSizeMode.Normal)
                                    } else {
                                        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        viewModel.setPlayerSizeMode(com.example.ui.PlayerSizeMode.Fullscreen)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Pantalla Completa",
                                        tint = if (playerSizeMode == com.example.ui.PlayerSizeMode.Fullscreen) accentColor else Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(onClick = { showCastOptionsDialog = true }) {
                                    Icon(imageVector = Icons.Default.Tv, contentDescription = "Transmitir", tint = Color.White, modifier = Modifier.size(22.dp))
                                }

                                IconButton(onClick = { showMoviesDrawer = !showMoviesDrawer }) {
                                    Icon(imageVector = Icons.Default.MenuOpen, contentDescription = "Películas", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    } else {
                        // Landscape View Layout - Centered controllers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Volume
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.width(120.dp)
                                ) {
                                    IconButton(onClick = { isMuted = !isMuted }) {
                                        Icon(
                                            imageVector = if (isMuted || volume == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                            contentDescription = "Volumen",
                                            tint = Color.White
                                        )
                                    }
                                    Slider(
                                        value = if (isMuted) 0f else volume,
                                        onValueChange = {
                                            volume = it
                                            isMuted = false
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = accentColor,
                                            activeTrackColor = accentColor,
                                            inactiveTrackColor = SpaceBorderColor
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Center Playback Controllers
                            Box(
                                modifier = Modifier.weight(1.5f),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val index = allowedMoviesForNav.indexOfFirst { it.id == movie.id }
                                            if (index > 0) {
                                                viewModel.playMovie(allowedMoviesForNav[index - 1])
                                            }
                                        },
                                        enabled = allowedMoviesForNav.indexOfFirst { it.id == movie.id } > 0
                                    ) {
                                        Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(28.dp))
                                    }

                                    IconButton(onClick = {
                                        val target = (currentMillis - 10000L).coerceAtLeast(0L)
                                        currentMillis = target
                                        seekToMillis = target
                                    }) {
                                        Icon(imageVector = Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(28.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                            .clickable { viewModel.pauseOrResumeCurrentPlayback() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    IconButton(onClick = {
                                        val target = (currentMillis + 10000L).coerceAtLeast(0L).coerceAtMost(totalMillis)
                                        currentMillis = target
                                        seekToMillis = target
                                    }) {
                                        Icon(imageVector = Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(28.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val index = allowedMoviesForNav.indexOfFirst { it.id == movie.id }
                                            if (index >= 0 && index < allowedMoviesForNav.size - 1) {
                                                viewModel.playMovie(allowedMoviesForNav[index + 1])
                                            }
                                        },
                                        enabled = allowedMoviesForNav.indexOfFirst { it.id == movie.id } < allowedMoviesForNav.size - 1
                                    ) {
                                        Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                }
                            }

                            // Right Actions
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(onClick = { onEnterPip() }) {
                                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "PIP", tint = Color.White, modifier = Modifier.size(22.dp))
                                    }

                                    IconButton(onClick = {
                                        val activity = context as? android.app.Activity
                                        if (isLandscape) {
                                            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            viewModel.setPlayerSizeMode(com.example.ui.PlayerSizeMode.Normal)
                                        } else {
                                            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            viewModel.setPlayerSizeMode(com.example.ui.PlayerSizeMode.Fullscreen)
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = "Pantalla Completa",
                                            tint = if (playerSizeMode == com.example.ui.PlayerSizeMode.Fullscreen) accentColor else Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    IconButton(onClick = { showCastOptionsDialog = true }) {
                                        Icon(imageVector = Icons.Default.Tv, contentDescription = "Transmitir", tint = Color.White, modifier = Modifier.size(22.dp))
                                    }

                                    IconButton(onClick = { showMoviesDrawer = !showMoviesDrawer }) {
                                        Icon(imageVector = Icons.Default.MenuOpen, contentDescription = "Películas", tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Movies Switch Drawer
            if (showMoviesDrawer) {
                val filteredDrawerMovies = remember(allowedMoviesForNav, movieSearchQuery) {
                    if (movieSearchQuery.isBlank()) allowedMoviesForNav
                    else allowedMoviesForNav.filter {
                        it.title.contains(movieSearchQuery, ignoreCase = true) ||
                        it.genre.contains(movieSearchQuery, ignoreCase = true)
                    }
                }

                if (isLandscape) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showMoviesDrawer = false },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(340.dp)
                                .clickable(enabled = false) {},
                            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                            border = BorderStroke(1.dp, SpaceBorderColor)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Catálogo de Películas",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    IconButton(onClick = { showMoviesDrawer = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = movieSearchQuery,
                                    onValueChange = { movieSearchQuery = it },
                                    placeholder = { Text("Buscar película...", color = Color.Gray, fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                                    trailingIcon = {
                                        if (movieSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { movieSearchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                        focusedBorderColor = accentColor, unfocusedBorderColor = SpaceBorderColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(filteredDrawerMovies) { item ->
                                        val isCurrent = item.id == movie.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isCurrent) accentColor.copy(alpha = 0.2f) else SpaceDarkBackground)
                                                .border(1.dp, if (isCurrent) accentColor else SpaceBorderColor, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    viewModel.playMovie(item)
                                                    showMoviesDrawer = false
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            AsyncImage(
                                                model = item.posterUrl,
                                                contentDescription = item.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(36.dp, 50.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${item.genre} • ${item.duration}", color = accentColor, fontSize = 11.sp, maxLines = 1)
                                            }

                                            if (isCurrent) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Portrait View: Fullscreen dialog
                    AlertDialog(
                        onDismissRequest = { showMoviesDrawer = false },
                        containerColor = SpaceCardBg,
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Catálogo de Películas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                IconButton(onClick = { showMoviesDrawer = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                                }
                            }
                        },
                        text = {
                            Column(modifier = Modifier.height(380.dp)) {
                                OutlinedTextField(
                                    value = movieSearchQuery,
                                    onValueChange = { movieSearchQuery = it },
                                    placeholder = { Text("Buscar película...", color = Color.Gray, fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                                    trailingIcon = {
                                        if (movieSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { movieSearchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                        focusedBorderColor = accentColor, unfocusedBorderColor = SpaceBorderColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(filteredDrawerMovies) { item ->
                                        val isCurrent = item.id == movie.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isCurrent) accentColor.copy(alpha = 0.2f) else SpaceDarkBackground)
                                                .border(1.dp, if (isCurrent) accentColor else SpaceBorderColor, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    viewModel.playMovie(item)
                                                    showMoviesDrawer = false
                                                }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            AsyncImage(
                                                model = item.posterUrl,
                                                contentDescription = item.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(36.dp, 50.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${item.genre} • ${item.duration}", color = accentColor, fontSize = 11.sp, maxLines = 1)
                                            }

                                            if (isCurrent) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {}
                    )
                }
            }
        }
    }
}

// Custom simulated modifier helper for semi-blur background card effect
fun Modifier.blurSemiSimulated(): Modifier = this.then(
    Modifier.background(Color.Black.copy(alpha = 0.85f))
)


// ---------------------- ANIMATED SOUND VISUALIZER ----------------------
@Composable
fun SoundVisualizer(
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        val maxHeightDp = maxHeight
        if (isBuffering) {
            val infiniteTransition = rememberInfiniteTransition(label = "visualizer_buffering")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
                label = "alpha"
            )
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(maxHeightDp * 0.45f)
                            .clip(RoundedCornerShape(1.dp))
                            .background(color.copy(alpha = alpha))
                    )
                }
            }
        } else if (isPlaying) {
            val infiniteTransition = rememberInfiniteTransition(label = "visualizer_playing")
            val b1 by infiniteTransition.animateFloat(
                initialValue = 0.20f, targetValue = 0.95f,
                animationSpec = infiniteRepeatable(tween(240, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "b1"
            )
            val b2 by infiniteTransition.animateFloat(
                initialValue = 0.15f, targetValue = 0.75f,
                animationSpec = infiniteRepeatable(tween(330, easing = LinearEasing), RepeatMode.Reverse),
                label = "b2"
            )
            val b3 by infiniteTransition.animateFloat(
                initialValue = 0.30f, targetValue = 1.00f,
                animationSpec = infiniteRepeatable(tween(200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "b3"
            )
            val b4 by infiniteTransition.animateFloat(
                initialValue = 0.20f, targetValue = 0.85f,
                animationSpec = infiniteRepeatable(tween(370, easing = LinearEasing), RepeatMode.Reverse),
                label = "b4"
            )
            val b5 by infiniteTransition.animateFloat(
                initialValue = 0.15f, targetValue = 0.90f,
                animationSpec = infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "b5"
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom
            ) {
                listOf(b1, b2, b3, b4, b5).forEach { fraction ->
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(maxHeightDp * fraction)
                            .clip(RoundedCornerShape(1.dp))
                            .background(color)
                    )
                }
            }
        } else {
            // Stopped / Paused static bars
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom
            ) {
                listOf(0.20f, 0.35f, 0.20f, 0.30f, 0.15f).forEach { fraction ->
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(maxHeightDp * fraction)
                            .clip(RoundedCornerShape(1.dp))
                            .background(color.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(
    url: String,
    isPlaying: Boolean,
    volume: Float = 1.0f,
    isMuted: Boolean = false,
    seekToMillis: Long = -1L,
    modifier: Modifier = Modifier,
    onProgressUpdate: (currentMillis: Long, totalMillis: Long) -> Unit = { _, _ -> },
    onBufferUpdate: (percent: Int) -> Unit = {}
) {
    val context = LocalContext.current
    var isBuffering by remember { mutableStateOf(true) }

    val processUrl: (String) -> String = remember {
        { rawUrl ->
            var formatted = rawUrl.trim()
            if (formatted.contains("dropbox.com")) {
                formatted = formatted.replace("dl=0", "dl=1").replace("www.dropbox.com", "dl.dropboxusercontent.com")
            } else if (formatted.contains("drive.google.com/file/d/")) {
                val fileId = formatted.substringAfter("/d/").substringBefore("/")
                if (fileId.isNotBlank()) {
                    formatted = "https://drive.google.com/uc?export=download&id=$fileId"
                }
            }
            formatted
        }
    }

    val finalUrl = remember(url) { processUrl(url) }

    val detectMimeType: (String) -> String? = remember {
        { streamUrl ->
            val lower = streamUrl.lowercase()
            when {
                lower.contains(".m3u8") || lower.contains("/hls/") -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
                lower.contains(".mpd") || lower.contains("/dash/") -> androidx.media3.common.MimeTypes.APPLICATION_MPD
                lower.contains(".mp4") -> androidx.media3.common.MimeTypes.VIDEO_MP4
                lower.contains(".mkv") -> androidx.media3.common.MimeTypes.VIDEO_MATROSKA
                lower.contains(".webm") -> androidx.media3.common.MimeTypes.VIDEO_WEBM
                lower.contains(".avi") -> androidx.media3.common.MimeTypes.VIDEO_AVI
                lower.contains(".flv") -> androidx.media3.common.MimeTypes.VIDEO_FLV
                lower.contains(".ts") -> androidx.media3.common.MimeTypes.VIDEO_MP2T
                lower.contains(".mov") -> androidx.media3.common.MimeTypes.VIDEO_MP4
                else -> null
            }
        }
    }

    val exoPlayer = remember(context) {
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1500, // minBufferMs: Fast start
                5000, // maxBufferMs
                500,  // bufferForPlaybackMs
                1000  // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        }

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)

        androidx.media3.exoplayer.ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    androidx.media3.common.Player.STATE_BUFFERING -> {
                        isBuffering = true
                        onBufferUpdate(exoPlayer.bufferedPercentage)
                    }
                    androidx.media3.common.Player.STATE_READY -> {
                        isBuffering = false
                        onBufferUpdate(100)
                    }
                    androidx.media3.common.Player.STATE_ENDED,
                    androidx.media3.common.Player.STATE_IDLE -> {
                        isBuffering = false
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("VideoPlayer", "ExoPlayer error [${error.errorCodeName}]: ${error.message}", error)
                isBuffering = false
                val fallbackUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                if (finalUrl != fallbackUrl) {
                    android.widget.Toast.makeText(context, "Servidor no disponible. Reconectando a servidor alternativo...", android.widget.Toast.LENGTH_SHORT).show()
                    try {
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                        val mediaItem = androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(fallbackUrl))
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    } catch (e: Exception) {
                        android.util.Log.e("VideoPlayer", "Error during fallback recovery", e)
                    }
                }
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            try { exoPlayer.stop() } catch (e: Exception) {}
            try { exoPlayer.release() } catch (e: Exception) {}
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                val act = context as? android.app.Activity
                if (act?.isInPictureInPictureMode != true) {
                    try {
                        exoPlayer.playWhenReady = false
                        exoPlayer.stop()
                    } catch (e: Exception) { }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(finalUrl) {
        if (finalUrl.isNotBlank()) {
            isBuffering = true
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                val builder = androidx.media3.common.MediaItem.Builder()
                    .setUri(android.net.Uri.parse(finalUrl))
                val mime = detectMimeType(finalUrl)
                if (mime != null) {
                    builder.setMimeType(mime)
                }
                val mediaItem = builder.build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = isPlaying
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayer", "Error loading finalUrl: $finalUrl", e)
            }
        }
    }

    LaunchedEffect(volume, isMuted) {
        exoPlayer.volume = if (isMuted) 0f else volume
    }

    LaunchedEffect(isPlaying) {
        try {
            exoPlayer.playWhenReady = isPlaying
            if (isPlaying) {
                exoPlayer.play()
            } else {
                exoPlayer.pause()
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoPlayer", "Error toggling play/pause", e)
        }
    }

    LaunchedEffect(seekToMillis) {
        if (seekToMillis >= 0L) {
            exoPlayer.seekTo(seekToMillis)
        }
    }

    LaunchedEffect(isPlaying, finalUrl) {
        while (isPlaying) {
            try {
                if (exoPlayer.isPlaying) {
                    val current = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val duration = exoPlayer.duration.coerceAtLeast(0L)
                    if (duration > 0) {
                        onProgressUpdate(current, duration)
                    }
                    onBufferUpdate(exoPlayer.bufferedPercentage)
                }
            } catch (e: Exception) { }
            delay(500)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    isClickable = false
                    isFocusable = false
                    setOnTouchListener { _, _ -> false }
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Cargando...",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun shareAppApk(context: android.content.Context) {
    try {
        val appInfo = context.applicationInfo
        val sourceFile = java.io.File(appInfo.sourceDir)
        val destFile = java.io.File(context.cacheDir, "MedinPlayHub.apk")
        
        sourceFile.inputStream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        val authority = "${context.packageName}.fileprovider"
        val apkUri = androidx.core.content.FileProvider.getUriForFile(context, authority, destFile)
        
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(android.content.Intent.EXTRA_STREAM, apkUri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir Aplicación"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error al compartir la aplicación: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

fun launchSystemCast(context: android.content.Context) {
    try {
        android.widget.Toast.makeText(context, "Buscando pantallas de transmisión (Chromecast / Smart TV)...", android.widget.Toast.LENGTH_SHORT).show()
        val intent = android.content.Intent(android.provider.Settings.ACTION_CAST_SETTINGS).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = android.content.Intent("android.settings.WIFI_DISPLAY_SETTINGS").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e3: Exception) {
                android.widget.Toast.makeText(context, "Tu dispositivo no admite la transmisión directa por pantalla.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}

fun launchExternalStreamCast(context: android.content.Context, streamUrl: String, title: String) {
    try {
        val uri = android.net.Uri.parse(streamUrl)
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            putExtra("title", title)
            putExtra(android.content.Intent.EXTRA_TITLE, title)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(intent, "Transmitir o reproducir con...").apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.util.Log.e("StreamCast", "Error launching external player chooser", e)
        try {
            val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(streamUrl)).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(android.content.Intent.createChooser(fallbackIntent, "Abrir transmisión").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e2: Exception) {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("URL del Stream", streamUrl)
            clipboard?.setPrimaryClip(clip)
            android.widget.Toast.makeText(context, "Enlace copiado al portapapeles: $streamUrl", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

fun shareCollectionJson(context: android.content.Context, json: String) {
    try {
        val file = java.io.File(context.cacheDir, "MediaPlayHub_Coleccion.json")
        file.writeText(json)
        
        val authority = "${context.packageName}.fileprovider"
        val fileUri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir Colección"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error al compartir colección: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

// ---------------------- REORGANIZED PLAYBACK HISTORY GRID CARD ----------------------
@Composable
fun ProfileHistoryGridCard(
    viewModel: MediaViewModel,
    profile: UserProfile,
    accentColor: Color
) {
    val radioHistory by viewModel.radioHistory.collectAsStateWithLifecycle()
    val iptvHistory by viewModel.iptvHistory.collectAsStateWithLifecycle()
    val movieHistory by viewModel.movieHistory.collectAsStateWithLifecycle()
    val adultIptvHistory by viewModel.adultIptvHistory.collectAsStateWithLifecycle()
    val adultMovieHistory by viewModel.adultMovieHistory.collectAsStateWithLifecycle()

    var activeCategory by remember { mutableIntStateOf(0) } // 0 = Radio, 1 = IPTV, 2 = Cine, 3 = Adultos (+18)
    
    // Selection sets for multi-deletion
    var selectedRadioIds by remember { mutableStateOf(setOf<String>()) }
    var selectedIptvIds by remember { mutableStateOf(setOf<String>()) }
    var selectedMovieIds by remember { mutableStateOf(setOf<String>()) }
    var selectedAdultIds by remember { mutableStateOf(setOf<String>()) }

    val isAdultUnlocked by viewModel.isAdultUnlocked.collectAsStateWithLifecycle()
    var adultPinInput by remember { mutableStateOf("") }
    var adultPinError by remember { mutableStateOf(false) }
    var showAdultPinDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SpaceBorderColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📜 Historial de Reproducción",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                TextButton(
                    onClick = { viewModel.clearAllPlaybackHistory() }
                ) {
                    Text("Borrar Todo", color = Color(0xFFE53935), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "Historial en cuadrícula (hasta 3 columnas x 5 filas = 15 elementos). Toca cualquier elemento para reproducir.",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Category Sub-Tabs
            TabRow(
                selectedTabIndex = activeCategory,
                containerColor = SpaceDarkBackground,
                contentColor = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                Tab(selected = activeCategory == 0, onClick = { activeCategory = 0 }) {
                    Text("📻 Radios (${radioHistory.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 10.dp))
                }
                Tab(selected = activeCategory == 1, onClick = { activeCategory = 1 }) {
                    Text("📺 IPTV (${iptvHistory.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 10.dp))
                }
                Tab(selected = activeCategory == 2, onClick = { activeCategory = 2 }) {
                    Text("🎬 Cine (${movieHistory.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 10.dp))
                }
                Tab(selected = activeCategory == 3, onClick = { activeCategory = 3 }) {
                    Text("🔞 +18", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B6B), modifier = Modifier.padding(vertical = 10.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (activeCategory) {
                0 -> { // Radio History Grid
                    val items = radioHistory.take(15) // Max 15 items
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mostrando ${items.size} de ${radioHistory.size} elementos", fontSize = 11.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectedRadioIds.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        viewModel.removeRadioHistoryItems(selectedRadioIds.toList())
                                        selectedRadioIds = emptySet()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Eliminar (${selectedRadioIds.size})", fontSize = 10.sp, color = Color.White)
                                }
                            }
                            TextButton(onClick = { viewModel.clearRadioHistory() }) {
                                Text("Limpiar Sección", color = Color(0xFFE53935), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (items.isEmpty()) {
                        Text("No hay reproducciones recientes de Radio.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .padding(top = 8.dp)
                        ) {
                            items(items) { item ->
                                val isSelected = selectedRadioIds.contains(item.id)
                                HistoryGridItemCard(
                                    title = item.name,
                                    imageUrl = item.imageUrl,
                                    subtitle = item.genre,
                                    isSelected = isSelected,
                                    accentColor = accentColor,
                                    onSelectToggle = {
                                        selectedRadioIds = if (isSelected) selectedRadioIds - item.id else selectedRadioIds + item.id
                                    },
                                    onClick = {
                                        viewModel.playRadio(item)
                                        viewModel.setTab(MediaViewModel.Tab.Radio)
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> { // IPTV History Grid
                    val items = iptvHistory.take(15)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mostrando ${items.size} de ${iptvHistory.size} elementos", fontSize = 11.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectedIptvIds.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        viewModel.removeIptvHistoryItems(selectedIptvIds.toList())
                                        selectedIptvIds = emptySet()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Eliminar (${selectedIptvIds.size})", fontSize = 10.sp, color = Color.White)
                                }
                            }
                            TextButton(onClick = { viewModel.clearIptvHistory() }) {
                                Text("Limpiar Sección", color = Color(0xFFE53935), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (items.isEmpty()) {
                        Text("No hay reproducciones recientes de IPTV.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .padding(top = 8.dp)
                        ) {
                            items(items) { item ->
                                val isSelected = selectedIptvIds.contains(item.id)
                                HistoryGridItemCard(
                                    title = item.name,
                                    imageUrl = item.logoUrl,
                                    subtitle = item.category,
                                    isSelected = isSelected,
                                    accentColor = accentColor,
                                    onSelectToggle = {
                                        selectedIptvIds = if (isSelected) selectedIptvIds - item.id else selectedIptvIds + item.id
                                    },
                                    onClick = {
                                        viewModel.playIptv(item)
                                        viewModel.setTab(MediaViewModel.Tab.IPTV)
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> { // Movies History Grid
                    val items = movieHistory.take(15)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mostrando ${items.size} de ${movieHistory.size} elementos", fontSize = 11.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectedMovieIds.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        viewModel.removeMovieHistoryItems(selectedMovieIds.toList())
                                        selectedMovieIds = emptySet()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Eliminar (${selectedMovieIds.size})", fontSize = 10.sp, color = Color.White)
                                }
                            }
                            TextButton(onClick = { viewModel.clearMovieHistory() }) {
                                Text("Limpiar Sección", color = Color(0xFFE53935), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (items.isEmpty()) {
                        Text("No hay reproducciones recientes de Películas.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .padding(top = 8.dp)
                        ) {
                            items(items) { item ->
                                val isSelected = selectedMovieIds.contains(item.id)
                                HistoryGridItemCard(
                                    title = item.title,
                                    imageUrl = item.posterUrl,
                                    subtitle = item.genre,
                                    isSelected = isSelected,
                                    accentColor = accentColor,
                                    onSelectToggle = {
                                        selectedMovieIds = if (isSelected) selectedMovieIds - item.id else selectedMovieIds + item.id
                                    },
                                    onClick = {
                                        viewModel.playMovie(item)
                                        viewModel.setTab(MediaViewModel.Tab.Movies)
                                    }
                                )
                            }
                        }
                    }
                }
                3 -> { // Adult Content (+18) Grid
                    if (!isAdultUnlocked) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Acceso Protegido por Control Parental", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Introduce la clave de control parental para visualizar o administrar el historial +18.", color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showAdultPinDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))
                            ) {
                                Text("Ingresar PIN Parental", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        val items = (adultIptvHistory.map { "iptv_" + it.id to (it.name to it.logoUrl) } + adultMovieHistory.map { "movie_" + it.id to (it.title to it.posterUrl) }).take(15)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mostrando ${items.size} elementos +18", fontSize = 11.sp, color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (selectedAdultIds.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            viewModel.removeAdultHistoryItems(selectedAdultIds.toList())
                                            selectedAdultIds = emptySet()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Eliminar (${selectedAdultIds.size})", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                                TextButton(onClick = { viewModel.clearAdultHistory() }) {
                                    Text("Limpiar Sección", color = Color(0xFFE53935), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (items.isEmpty()) {
                            Text("No hay reproducción de contenido +18 registrada.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 380.dp)
                                    .padding(top = 8.dp)
                            ) {
                                items(items) { (idPair, details) ->
                                    val isSelected = selectedAdultIds.contains(idPair)
                                    HistoryGridItemCard(
                                        title = details.first,
                                        imageUrl = details.second,
                                        subtitle = "+18 Adultos",
                                        isSelected = isSelected,
                                        accentColor = Color(0xFFFF6B6B),
                                        onSelectToggle = {
                                            selectedAdultIds = if (isSelected) selectedAdultIds - idPair else selectedAdultIds + idPair
                                        },
                                        onClick = { }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdultPinDialog) {
        AlertDialog(
            onDismissRequest = { showAdultPinDialog = false; adultPinInput = ""; adultPinError = false },
            containerColor = SpaceCardBg,
            title = { Text("🔒 Control Parental +18", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Introduce el PIN de Control Parental:", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = adultPinInput,
                        onValueChange = { adultPinInput = it; adultPinError = false },
                        label = { Text("PIN Parental") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = accentColor, unfocusedBorderColor = SpaceBorderColor)
                    )
                    if (adultPinError) {
                        Text("PIN incorrecto.", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val requiredPin = profile.adultPin.ifBlank { "1234" }
                        if (adultPinInput.trim() == requiredPin) {
                            viewModel.unlockAdultContent(true)
                            showAdultPinDialog = false
                            adultPinInput = ""
                            adultPinError = false
                        } else {
                            adultPinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Acceder", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdultPinDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun HistoryGridItemCard(
    title: String,
    imageUrl: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    onSelectToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceDarkBackground),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else SpaceBorderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    fontSize = 8.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Selection Checkbox on top-right
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelectToggle() },
                colors = CheckboxDefaults.colors(checkedColor = accentColor),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .padding(2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareAppDialog(
    viewModel: MediaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    
    var selectedOption by remember { mutableIntStateOf(0) } // 0 = Compartir Usuario, 1 = App Nuevo Usuario

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SpaceBorderColor, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📤 Compartir MedinPlay Hub",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Tab Switcher between Option 1 and Option 2
                TabRow(
                    selectedTabIndex = selectedOption,
                    containerColor = SpaceDarkBackground,
                    contentColor = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedOption == 0,
                        onClick = { selectedOption = 0 }
                    ) {
                        Text(
                            text = "Compartir Usuario",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Tab(
                        selected = selectedOption == 1,
                        onClick = { selectedOption = 1 }
                    ) {
                        Text(
                            text = "App Nuevo Usuario",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedOption == 0) {
                    // Option 1: Compartir Usuario (Perfiles)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SpaceDarkBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SpaceBorderColor, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "👥 Copia de Usuario (Perfiles Actuales)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Límite de la Licencia: ${profiles.size} / 6 perfiles en uso.",
                                fontSize = 12.sp,
                                color = Color(0xFFC67C65),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Cada licencia de usuario permite compartir su copia hasta un máximo de 6 perfiles. Los perfiles actuales (${profiles.joinToString { it.name }}) se mantendrán sincronizados.",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                lineHeight = 16.sp
                            )
                            if (profiles.size >= 6) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "⚠️ Se ha alcanzado el límite máximo de 6 perfiles para esta cuenta.",
                                    fontSize = 11.sp,
                                    color = Color.Yellow,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            shareAppApk(context)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC67C65)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFF0F0D0B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar Copia con Perfiles", fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
                    }
                } else {
                    // Option 2: Compartir App para Nuevo Usuario
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SpaceDarkBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SpaceBorderColor, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "🌱 Copia para Nuevo Cliente / Sub Usuario",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Al compartir la aplicación con esta opción, el nuevo usuario la recibirá sin activar. Al instalarla en su dispositivo, la app le solicitará realizar el pago e ingresar su código de activación para acceder a sus propios perfiles independientes.",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            shareAppApk(context)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC67C65)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFF0F0D0B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar Copia a Nuevo Usuario", fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AppActivationScreen(
    viewModel: MediaViewModel
) {
    val context = LocalContext.current
    val paymentMethods by viewModel.paymentMethods.collectAsStateWithLifecycle()
    val subscriptionPrice by viewModel.subscriptionPrice.collectAsStateWithLifecycle()
    val expirationNotice by viewModel.expirationNotice.collectAsStateWithLifecycle()
    
    var emailInput by remember { mutableStateOf("") }
    var refInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf(subscriptionPrice.ifBlank { "5.00" }) }
    var selectedPaymentMethodTitle by remember { mutableStateOf("") }
    var isPaymentMethodDropdownExpanded by remember { mutableStateOf(false) }
    
    var activationCodeInput by remember { mutableStateOf("") }
    var activationError by remember { mutableStateOf(false) }
    var paymentSubmittedMsg by remember { mutableStateOf("") }
    var resendCodeMsg by remember { mutableStateOf("") }
    
    // Hidden 5-click admin entry
    var mClickCount by remember { mutableIntStateOf(0) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var adminError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header Title with M secret admin click
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "M",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC67C65),
                    modifier = Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        mClickCount++
                        if (mClickCount >= 5) {
                            mClickCount = 0
                            showAdminDialog = true
                        }
                    }
                )
                Text(
                    text = "edinPlay Hub",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Expiration warning notice if subscription has expired
            if (!expirationNotice.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFC62828).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = expirationNotice!!,
                            color = Color.White,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Surface(
                color = Color(0xFFC67C65).copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC67C65))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFC67C65), modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("🔐 Activación de Licencia Requerida", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Precio de Licencia: ${if (subscriptionPrice.startsWith("$")) subscriptionPrice else "$$subscriptionPrice"} USD / Mes",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Para acceder a la pantalla de perfiles y disfrutar de los contenidos, realiza tu pago de activación e ingresa tu código de licencia enviado a tu correo.",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 1. Payment Methods Selection
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SpaceBorderColor, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💳 1. Métodos de Pago Disponibles", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Surface(color = Color(0xFFC67C65).copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "Monto: ${if (subscriptionPrice.startsWith("$")) subscriptionPrice else "$$subscriptionPrice"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val activeMethods = paymentMethods.filter { it.isActive }
                    if (activeMethods.isEmpty()) {
                        Text("No hay métodos de pago configurados. Por favor contacta al administrador.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        activeMethods.forEach { pm ->
                            val isSelected = selectedPaymentMethodTitle == pm.title
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF2E231B) else SpaceDarkBackground),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(1.dp, if (isSelected) Color(0xFFC67C65) else SpaceBorderColor, RoundedCornerShape(10.dp))
                                    .clickable { selectedPaymentMethodTitle = pm.title }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPaymentMethodTitle = pm.title },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC67C65), unselectedColor = Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pm.title, fontWeight = FontWeight.Bold, color = Color(0xFFC67C65), fontSize = 13.sp)
                                        Text(pm.details, color = Color.White, fontSize = 12.sp)
                                        if (pm.instructions.isNotBlank()) {
                                            Text(pm.instructions, color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 2. Report Payment
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SpaceBorderColor, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📄 2. Reportar Comprobante de Pago", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Correo Electrónico del Cliente") },
                        placeholder = { Text("ejemplo@correo.com") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFC67C65)),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "✉️ Ingresa tu correo personal. A este correo te llegará el código de activación con la fecha de inicio y finalización de tu suscripción, y se guardará automáticamente en los ajustes de tu Perfil Principal.",
                        fontSize = 11.sp,
                        color = Color(0xFFFFD54F),
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = refInput,
                        onValueChange = { refInput = it },
                        label = { Text("Número de Referencia / Comprobante") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFC67C65)),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Monto Pagado ($ / Bs.)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFC67C65)),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            if (selectedPaymentMethodTitle.isBlank()) {
                                paymentSubmittedMsg = "⚠️ Debes seleccionar un método de pago en el Paso 1."
                                return@Button
                            }
                            if (emailInput.isBlank()) {
                                paymentSubmittedMsg = "⚠️ Por favor ingresa tu correo electrónico."
                                return@Button
                            }
                            if (refInput.isBlank()) {
                                paymentSubmittedMsg = "⚠️ Por favor ingresa el número de referencia de tu pago."
                                return@Button
                            }
                            viewModel.submitPaymentRequest(
                                userEmail = emailInput.trim(),
                                paymentMethodName = selectedPaymentMethodTitle,
                                referenceCode = refInput.trim(),
                                amount = amountInput.ifBlank { subscriptionPrice }
                            )
                            paymentSubmittedMsg = "¡Comprobante enviado con éxito! Al verificar tu pago recibirás tu código de activación por correo (medinplayhub@gmail.com)."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar Registro de Pago", fontWeight = FontWeight.Bold)
                    }
                    
                    if (paymentSubmittedMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(paymentSubmittedMsg, color = if (paymentSubmittedMsg.startsWith("⚠️")) Color.Yellow else Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 3. Activation Code Input & Resend Code
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFC67C65), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔑 3. Ingresar Código de Activación", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = activationCodeInput,
                        onValueChange = { 
                            activationCodeInput = it
                            activationError = false
                        },
                        label = { Text("Código de Activación") },
                        placeholder = { Text("Ej. MEDINPLAY2026") },
                        isError = activationError,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFC67C65)),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (activationError) {
                        Text("Código de activación incorrecto. Verifica el código enviado a tu correo.", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            val success = viewModel.activateApp(activationCodeInput)
                            if (!success) {
                                activationError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC67C65)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color(0xFF0F0D0B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Activar Licencia 🔓", fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SpaceBorderColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Reenviar Código Button for re-installs or active clients
                    Text(
                        text = "¿Ya pagaste o reinstalaste la app y no tienes tu código?",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            if (emailInput.isBlank()) {
                                resendCodeMsg = "⚠️ Ingresa tu correo electrónico en el Paso 2 para consultar o solicitar tu código."
                            } else {
                                viewModel.resendActivationCode(emailInput.trim()) { success, message ->
                                    resendCodeMsg = message
                                }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF90CAF9)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1565C0)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MarkEmailRead, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Solicitar Reenvío de Código a mi Correo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (resendCodeMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = resendCodeMsg,
                            color = if (resendCodeMsg.startsWith("⚠️")) Color.Yellow else Color(0xFFFFD54F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Admin Dialog secret access OTP
        AdminOtpDialog(
            showDialog = showAdminDialog,
            onDismiss = { showAdminDialog = false },
            isAdminMode = viewModel.isAdminMode.collectAsStateWithLifecycle().value,
            onLogoutAdmin = { viewModel.logoutAdmin() },
            viewModel = viewModel
        )
    }
}

// ---------------------- REORGANIZED PLAYBACK HISTORY CARD ----------------------
@Composable
fun ProfileHistoryCard(
    viewModel: MediaViewModel,
    profile: UserProfile,
    accentColor: Color
) {
    val radioHistory by viewModel.radioHistory.collectAsStateWithLifecycle()
    val iptvHistory by viewModel.iptvHistory.collectAsStateWithLifecycle()
    val movieHistory by viewModel.movieHistory.collectAsStateWithLifecycle()
    val adultIptvHistory by viewModel.adultIptvHistory.collectAsStateWithLifecycle()
    val adultMovieHistory by viewModel.adultMovieHistory.collectAsStateWithLifecycle()
    
    val isAdultUnlocked by viewModel.isAdultUnlocked.collectAsStateWithLifecycle()
    var adultPinInput by remember { mutableStateOf("") }
    var adultPinError by remember { mutableStateOf(false) }
    var showAdultPinDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SpaceBorderColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📜 Historial de Reproducción",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                TextButton(
                    onClick = { viewModel.clearAllPlaybackHistory() }
                ) {
                    Text("Borrar Todo", color = Color(0xFFE53935), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "Historial independiente por categoría para el perfil '${profile.name}'.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 1. Radio History
            HistoryCategorySection(
                title = "📻 Historial de Radios (${radioHistory.size})",
                items = radioHistory.map { "${it.name} (${it.genre})" },
                onClear = { viewModel.clearRadioHistory() },
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. IPTV History
            HistoryCategorySection(
                title = "📺 Historial de Canales IPTV (${iptvHistory.size})",
                items = iptvHistory.map { "${it.name} (${it.category})" },
                onClear = { viewModel.clearIptvHistory() },
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Movies History
            HistoryCategorySection(
                title = "🎬 Historial de Películas (${movieHistory.size})",
                items = movieHistory.map { "${it.title} (${it.genre})" },
                onClear = { viewModel.clearMovieHistory() },
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Adult Content (+18) History
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1012)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF8B0000), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🔞 Historial Contenido Adulto (+18)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B6B)
                        )
                        if (isAdultUnlocked) {
                            TextButton(onClick = { viewModel.clearAdultHistory() }) {
                                Text("Borrar +18", color = Color(0xFFE53935), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (!isAdultUnlocked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Acceso protegido por Control Parental. Ingresa el PIN para ver o limpiar este historial.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showAdultPinDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Desbloquear con PIN Parental", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        val adultTitles = adultIptvHistory.map { "📺 " + it.name } + adultMovieHistory.map { "🎬 " + it.title }
                        if (adultTitles.isEmpty()) {
                            Text("No hay reproducción de contenido +18 registrada.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                        } else {
                            adultTitles.take(5).forEach { title ->
                                Text("• $title", fontSize = 11.sp, color = Color.LightGray, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdultPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdultPinDialog = false
                adultPinInput = ""
                adultPinError = false
            },
            containerColor = SpaceCardBg,
            title = { Text("🔒 Control Parental +18", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Introduce el PIN de Control Parental para acceder al historial +18:", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = adultPinInput,
                        onValueChange = { adultPinInput = it; adultPinError = false },
                        label = { Text("PIN Parental") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = SpaceBorderColor
                        )
                    )
                    if (adultPinError) {
                        Text("PIN incorrecto.", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val requiredPin = profile.adultPin.ifBlank { "1234" }
                        if (adultPinInput.trim() == requiredPin) {
                            viewModel.unlockAdultContent(true)
                            showAdultPinDialog = false
                            adultPinInput = ""
                            adultPinError = false
                        } else {
                            adultPinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Acceder", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdultPinDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun HistoryCategorySection(
    title: String,
    items: List<String>,
    onClear: () -> Unit,
    accentColor: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceDarkBackground),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SpaceBorderColor, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (items.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("Borrar", color = Color(0xFFE53935), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (items.isEmpty()) {
                Text("Sin reproducciones recientes.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
            } else {
                items.take(4).forEach { itemText ->
                    Text("• $itemText", fontSize = 11.sp, color = Color.LightGray, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

// ---------------------- GEMINI RECOMMENDATIONS CARD ----------------------
@Composable
fun GeminiRecommendationsCard(
    viewModel: MediaViewModel,
    profile: UserProfile,
    accentColor: Color
) {
    val recs by viewModel.geminiRecommendations.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingRecommendations.collectAsStateWithLifecycle()
    val radios by viewModel.radios.collectAsStateWithLifecycle()
    val iptvList by viewModel.iptvList.collectAsStateWithLifecycle()
    val movies by viewModel.movies.collectAsStateWithLifecycle()

    LaunchedEffect(profile.id) {
        if (recs == null) {
            viewModel.loadGeminiRecommendations()
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1824)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "✨ Recomendaciones IA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Gemini", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(
                    onClick = { viewModel.loadGeminiRecommendations() },
                    enabled = !isGenerating
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar Recomendaciones", tint = accentColor, modifier = Modifier.size(20.dp))
                }
            }

            if (isGenerating) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accentColor, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analizando tu historial con Gemini AI...", fontSize = 12.sp, color = Color.LightGray)
                }
            } else if (recs != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = recs!!.reason,
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                val recommendedMoviesList = movies.filter { recs!!.recommendedMovieIds.contains(it.id) }
                val recommendedIptvList = iptvList.filter { recs!!.recommendedIptvIds.contains(it.id) }

                if (recommendedMoviesList.isNotEmpty()) {
                    Text("🎬 Películas Sugeridas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recommendedMoviesList) { movie ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                                modifier = Modifier
                                    .width(110.dp)
                                    .clickable { viewModel.playMovie(movie) }
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    AsyncImage(
                                        model = movie.posterUrl,
                                        contentDescription = movie.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(movie.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(movie.genre, fontSize = 9.sp, color = Color.Gray, maxLines = 1)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (recommendedIptvList.isNotEmpty()) {
                    Text("📺 Canales TV Sugeridos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recommendedIptvList) { iptv ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                                modifier = Modifier
                                    .width(110.dp)
                                    .clickable { viewModel.playIptv(iptv) }
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    AsyncImage(
                                        model = iptv.logoUrl,
                                        contentDescription = iptv.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(iptv.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(iptv.category, fontSize = 9.sp, color = Color.Gray, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CastOptionsDialog(
    streamUrl: String,
    streamTitle: String,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceCardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Opciones de Transmisión", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Friendly recommendation card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SpaceDarkBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("💡", fontSize = 18.sp)
                        Column {
                            Text(
                                text = "Sugerencia para transmitir con éxito:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Para reproducir o transmitir sin cortes a tu Smart TV o pantalla externa, te recomendamos seleccionar aplicaciones instaladas compatibles como VLC, Web Video Caster, BubbleUPnP, MX Player o LocalCast.",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onDismiss()
                        launchSystemCast(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ScreenShare, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Duplicar Pantalla (Sistema)", color = Color.White, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        android.widget.Toast.makeText(
                            context,
                            "💡 Se recomienda elegir VLC, Web Video Caster, BubbleUPnP u otro reproductor compatible.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        launchExternalStreamCast(context, streamUrl, streamTitle)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, SpaceBorderColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Transmitir a Pantalla externa / App", color = Color.White, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@Composable
fun MovieDetailDialog(
    movie: MovieItem,
    isFavorite: Boolean,
    accentColor: Color,
    hasReportError: Boolean = false,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onCast: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onReportError: () -> Unit,
    onOpenAiAnalysis: (() -> Unit)? = null
) {
    var showCastOptions by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showCastOptions) {
        CastOptionsDialog(
            streamUrl = movie.url,
            streamTitle = movie.title,
            accentColor = accentColor,
            onDismiss = { showCastOptions = false }
        )
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = SpaceCardBg,
            border = BorderStroke(1.dp, SpaceBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Poster Header with Gradient overlay and Close Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(SpaceDarkBackground)
                ) {
                    if (movie.posterUrl.isNotBlank()) {
                        AsyncImage(
                            model = movie.posterUrl,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E1C1A)),
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
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
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
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }

                    // Favorite Button
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) Color.Red else Color.White
                        )
                    }
                }

                // Content Details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (hasReportError) {
                        Surface(
                            color = Color(0xFFD32F2F),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                                Text(
                                    text = "⚠️ ATENCIÓN: Esta película presenta fallas de reproducción reportadas. El equipo técnico está trabajando en la solución.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = movie.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Metadata Badges Row: Year, Duration, Rating, Adult
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (movie.year > 0) {
                            Surface(
                                color = SpaceDarkBackground,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, SpaceBorderColor)
                            ) {
                                Text(
                                    text = movie.year.toString(),
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (movie.duration.isNotBlank()) {
                            Surface(
                                color = SpaceDarkBackground,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, SpaceBorderColor)
                            ) {
                                Text(
                                    text = movie.duration,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (movie.rating.isNotBlank()) {
                            Surface(
                                color = Color(0xFFC67C65).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFC67C65).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "★ ${movie.rating}",
                                    color = Color(0xFFC67C65),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (movie.isAdult) {
                            Surface(
                                color = Color.Red.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "+18",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (movie.displayGenre.isNotBlank()) {
                        Text(
                            text = "🎭 ${movie.displayGenre}",
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Divider(color = SpaceBorderColor, thickness = 1.dp)

                    Text(
                        text = "Sinopsis / Argumento",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (movie.description.isNotBlank()) movie.description else "Sin sinopsis disponible para esta película.",
                        color = Color(0xFFD1C4B9),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    if (onOpenAiAnalysis != null) {
                        OutlinedButton(
                            onClick = onOpenAiAnalysis,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✨ Análisis IA, Saga y Similares (OpenRouter)", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onPlay()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0F0D0B))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reproducir", color = Color(0xFF0F0D0B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                showCastOptions = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, SpaceBorderColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Cast, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Transmitir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    TextButton(
                        onClick = {
                            if (hasReportError) {
                                Toast.makeText(context, "Ya existe un reporte activo para esta película. Espera a que sea resuelto por el administrador.", Toast.LENGTH_LONG).show()
                            } else {
                                onReportError()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = if (hasReportError) Color.Gray else Color(0xFFE53935), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasReportError) "⚠️ Reporte de falla en revisión por admin" else "Reportar error en esta película",
                            color = if (hasReportError) Color.Gray else Color(0xFFE53935),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorReportDialog(
    itemName: String,
    itemType: String,
    itemId: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSubmit: (issueType: String, comments: String) -> Unit
) {
    val issueOptions = listOf(
        "Sin señal / No reproduce",
        "Audio o video desfasado / cortado",
        "Enlace roto / error de transmisión",
        "Información o portada incorrecta",
        "Otro problema"
    )
    var selectedIssue by remember { mutableStateOf(issueOptions[0]) }
    var comments by remember { mutableStateOf("") }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            containerColor = SpaceCardBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = accentColor)
                    Text("Confirmar Envío de Reporte", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "¿Estás seguro de que deseas enviar este reporte de error?",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = SpaceDarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Elemento: $itemName ($itemType)",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Falla: $selectedIssue",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        onSubmit(selectedIssue, comments)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Confirmar Envío", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceCardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE53935))
                Text("Reportar Error", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Elemento: $itemName",
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Selecciona el tipo de falla que presenta este contenido:",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                issueOptions.forEach { issue ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedIssue = issue }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = (selectedIssue == issue),
                            onClick = { selectedIssue = issue },
                            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                        )
                        Text(text = issue, color = Color.White, fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = comments,
                    onValueChange = { comments = it },
                    label = { Text("Comentarios adicionales (opcional)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { showConfirmationDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("Enviar Reporte", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@Composable
fun ManageErrorReportsPanel(
    viewModel: MediaViewModel,
    errorReports: List<com.example.models.ErrorReport>,
    accentColor: Color,
    onNavigateToAdminTab: ((tabIndex: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val movies by viewModel.movies.collectAsStateWithLifecycle()
    val iptvList by viewModel.iptvList.collectAsStateWithLifecycle()
    val radios by viewModel.radios.collectAsStateWithLifecycle()

    var editingMovieForReport by remember { mutableStateOf<MovieItem?>(null) }
    var editingIptvForReport by remember { mutableStateOf<IptvItem?>(null) }
    var editingRadioForReport by remember { mutableStateOf<RadioItem?>(null) }

    // Dialogs for Direct Editing from Error Reports Panel
    editingMovieForReport?.let { movie ->
        AdminEditMovieDialog(
            movie = movie,
            accentColor = accentColor,
            viewModel = viewModel,
            onDismiss = { editingMovieForReport = null },
            onSave = { title, genre, url, posterUrl, description, year, duration, isAdult, rating ->
                viewModel.updateMovie(
                    id = movie.id,
                    title = title,
                    genre = genre,
                    url = url,
                    posterUrl = posterUrl,
                    description = description,
                    year = year,
                    duration = duration,
                    isAdult = isAdult,
                    rating = rating
                )
                editingMovieForReport = null
                Toast.makeText(context, "Película '$title' actualizada correctamente", Toast.LENGTH_SHORT).show()
            }
        )
    }

    editingIptvForReport?.let { iptv ->
        AdminEditIptvDialog(
            iptv = iptv,
            accentColor = accentColor,
            onDismiss = { editingIptvForReport = null },
            onSave = { name, category, url, logoUrl, country, isAdult ->
                viewModel.updateIptv(
                    id = iptv.id,
                    name = name,
                    category = category,
                    url = url,
                    logoUrl = logoUrl,
                    country = country,
                    isAdult = isAdult
                )
                editingIptvForReport = null
                Toast.makeText(context, "Canal '$name' actualizado correctamente", Toast.LENGTH_SHORT).show()
            }
        )
    }

    editingRadioForReport?.let { radio ->
        AdminEditRadioDialog(
            radio = radio,
            accentColor = accentColor,
            onDismiss = { editingRadioForReport = null },
            onSave = { name, genre, url, imageUrl, description ->
                viewModel.updateRadio(
                    id = radio.id,
                    name = name,
                    genre = genre,
                    url = url,
                    imageUrl = imageUrl,
                    description = description
                )
                editingRadioForReport = null
                Toast.makeText(context, "Emisora '$name' actualizada correctamente", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Grouping logic: Consolidate reports per content item
    val groupedReports = remember(errorReports) {
        errorReports
            .groupBy { report ->
                val cleanKey = if (report.itemId.isNotBlank()) report.itemId else report.itemName.lowercase().trim()
                "${report.itemType}__$cleanKey"
            }
            .values
            .sortedByDescending { group -> group.count { it.status == "PENDIENTE" } }
    }

    val pendingCount = errorReports.count { it.status == "PENDIENTE" }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "⚠️ Alertas y Reportes de Error",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "$pendingCount reportes pendientes • ${groupedReports.size} elementos reportados",
                    color = if (pendingCount > 0) Color.Red else Color.Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (groupedReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay reportes de error registrados por los usuarios.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            groupedReports.forEach { reportGroup ->
                val firstReport = reportGroup.first()
                val itemName = firstReport.itemName
                val rawItemType = firstReport.itemType
                val itemId = firstReport.itemId

                val normalizedType = when {
                    rawItemType.contains("pelicula", ignoreCase = true) || rawItemType.contains("movie", ignoreCase = true) || rawItemType.contains("cine", ignoreCase = true) -> "Película"
                    rawItemType.contains("iptv", ignoreCase = true) || rawItemType.contains("canal", ignoreCase = true) || rawItemType.contains("tv", ignoreCase = true) -> "IPTV"
                    rawItemType.contains("radio", ignoreCase = true) || rawItemType.contains("emisora", ignoreCase = true) -> "Radio"
                    else -> rawItemType
                }

                val pendingReportsInGroup = reportGroup.filter { it.status == "PENDIENTE" }
                val groupPendingCount = pendingReportsInGroup.size
                val groupTotalCount = reportGroup.size
                val isPendingGroup = groupPendingCount > 0

                var isExpanded by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SpaceDarkBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isPendingGroup) Color.Red.copy(alpha = 0.8f) else SpaceBorderColor,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header Row with Title, Type & Count Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = itemName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        color = SpaceCardBg,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, SpaceBorderColor)
                                    ) {
                                        Text(
                                            text = normalizedType,
                                            color = accentColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Counter badge: "Reportado X veces"
                                Text(
                                    text = if (isPendingGroup) "⚠️ Reportado $groupTotalCount ${if (groupTotalCount == 1) "vez" else "veces"} ($groupPendingCount pendiente${if (groupPendingCount > 1) "s" else ""})" else "✅ Reportado $groupTotalCount ${if (groupTotalCount == 1) "vez" else "veces"} (Solucionado)",
                                    color = if (isPendingGroup) Color(0xFFFF5252) else Color(0xFF4CAF50),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                color = if (isPendingGroup) Color.Red else Color(0xFF2E7D32),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isPendingGroup) "PENDIENTE" else "SOLUCIONADO",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Action Buttons Bar: Direct Edit, Mark Resolved, Expand/Collapse
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Direct Edit Button with redirection to specific admin module
                            OutlinedButton(
                                onClick = {
                                    when (normalizedType) {
                                        "Película" -> {
                                            val targetMovie = movies.find { it.id == itemId || it.title.equals(itemName, ignoreCase = true) }
                                            onNavigateToAdminTab?.invoke(2) // Subtab 2 = Gestión de Cine
                                            if (targetMovie != null) {
                                                editingMovieForReport = targetMovie
                                            } else {
                                                Toast.makeText(context, "Redirigiendo a Cine... No se encontró la película '$itemName'", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        "IPTV" -> {
                                            val targetIptv = iptvList.find { it.id == itemId || it.name.equals(itemName, ignoreCase = true) }
                                            onNavigateToAdminTab?.invoke(1) // Subtab 1 = Gestión de IPTV
                                            if (targetIptv != null) {
                                                editingIptvForReport = targetIptv
                                            } else {
                                                Toast.makeText(context, "Redirigiendo a IPTV... No se encontró el canal '$itemName'", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        "Radio" -> {
                                            val targetRadio = radios.find { it.id == itemId || it.name.equals(itemName, ignoreCase = true) }
                                            onNavigateToAdminTab?.invoke(0) // Subtab 0 = Gestión de Radios
                                            if (targetRadio != null) {
                                                editingRadioForReport = targetRadio
                                            } else {
                                                Toast.makeText(context, "Redirigiendo a Radios... No se encontró la emisora '$itemName'", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        else -> {
                                            Toast.makeText(context, "Sin opción de edición directa para este tipo de elemento ($rawItemType)", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                                border = BorderStroke(1.dp, accentColor),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Editar $normalizedType",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Resolution option: "Marcar como Solucionado"
                            if (isPendingGroup) {
                                Button(
                                    onClick = {
                                        pendingReportsInGroup.forEach { rep ->
                                            viewModel.updateErrorReportStatus(rep.id, "RESUELTO")
                                        }
                                        Toast.makeText(context, "Reportes de '$itemName' marcados como solucionados", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Marcar Solucionado",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Dropdown toggle button
                            OutlinedButton(
                                onClick = { isExpanded = !isExpanded },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, SpaceBorderColor),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isExpanded) "Ocultar ($groupTotalCount) ▲" else "Ver Detalle ($groupTotalCount) ▼",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Expandable Dropdown List with individual details
                        if (isExpanded) {
                            HorizontalDivider(color = SpaceBorderColor, thickness = 1.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Desglose de reportes recibidos:",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                reportGroup.forEach { rep ->
                                    val isRepPending = rep.status == "PENDIENTE"
                                    Surface(
                                        color = SpaceCardBg,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, SpaceBorderColor),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Falla: ${rep.issueType}",
                                                    color = accentColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = rep.status,
                                                    color = if (isRepPending) Color.Red else Color(0xFF4CAF50),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            if (rep.comments.isNotBlank()) {
                                                Text(
                                                    text = "Comentarios: ${rep.comments}",
                                                    color = Color.White,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "👤 ${rep.profileName} (${rep.userEmail}) • 🕒 ${rep.timestamp}",
                                                    color = Color.Gray,
                                                    fontSize = 10.sp
                                                )

                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteErrorReport(rep.id)
                                                        Toast.makeText(context, "Reporte individual eliminado", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Eliminar",
                                                        tint = Color.Red.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOtpDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    isAdminMode: Boolean,
    onLogoutAdmin: () -> Unit,
    viewModel: MediaViewModel
) {
    if (!showDialog) return

    val localContext = LocalContext.current
    var adminEmailInput by remember { mutableStateOf("Medinplayhub@gmail.com") }
    var adminOtpInput by remember { mutableStateOf("") }
    var adminOtpRequested by remember { mutableStateOf(false) }
    var adminOtpTimer by remember { mutableIntStateOf(60) }
    var adminOtpMessage by remember { mutableStateOf<String?>(null) }
    var adminOtpIsError by remember { mutableStateOf(false) }

    val isSendingOtp by viewModel.isSendingOtp.collectAsStateWithLifecycle()

    LaunchedEffect(adminOtpRequested, adminOtpTimer) {
        if (adminOtpRequested && adminOtpTimer > 0) {
            kotlinx.coroutines.delay(1000L)
            adminOtpTimer--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceCardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFC67C65))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAdminMode) "Panel de Gestión Activo" else "Acceso Administrativo OTP",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                if (isAdminMode) {
                    Text(
                        "El Modo Administrador está activo. Tu perfil tiene permisos de escritura para gestionar la app en tiempo real.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2420)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFC67C65).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFFC67C65),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Notificación Nativa en Dispositivo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "El código OTP de 6 dígitos llegará como notificación nativa al dispositivo. Válido por 60s.",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!adminOtpRequested) {
                        Button(
                            onClick = {
                                adminOtpMessage = "Generando OTP y enviando notificación..."
                                adminOtpIsError = false
                                viewModel.requestAdminOtp(adminEmailInput, localContext) { success, msg ->
                                    adminOtpMessage = msg
                                    if (success) {
                                        adminOtpRequested = true
                                        adminOtpTimer = 60
                                    } else {
                                        adminOtpIsError = true
                                    }
                                }
                            },
                            enabled = !isSendingOtp,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC67C65)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSendingOtp) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generando Notificación...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Solicitar Código OTP", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F0D0B))
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF231F1C), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (adminOtpTimer > 0) Color(0xFF4CAF50) else Color.Red,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (adminOtpTimer > 0) "Código expira en: ${adminOtpTimer}s" else "Código Expirado (0s)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (adminOtpTimer > 0) Color(0xFF4CAF50) else Color.Red
                                )
                            }
                            TextButton(
                                onClick = {
                                    adminOtpRequested = false
                                    adminOtpTimer = 60
                                    adminOtpInput = ""
                                    adminOtpMessage = null
                                }
                            ) {
                                Text("Reenviar", fontSize = 12.sp, color = Color(0xFFC67C65), fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 6-digit OTP Code input
                        OutlinedTextField(
                            value = adminOtpInput,
                            onValueChange = {
                                if (it.length <= 6) {
                                    adminOtpInput = it
                                    adminOtpIsError = false
                                }
                            },
                            label = { Text("Código OTP (6 dígitos)") },
                            placeholder = { Text("123456") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFC67C65)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = adminOtpIsError,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFC67C65),
                                unfocusedBorderColor = SpaceBorderColor,
                                focusedLabelColor = Color(0xFFC67C65)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (adminOtpMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = adminOtpMessage!!,
                            fontSize = 11.sp,
                            color = if (adminOtpIsError) Color(0xFFFF6B6B) else Color(0xFF81C784)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (isAdminMode) {
                    Button(
                        onClick = {
                            onLogoutAdmin()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Desactivar Admin", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = {
                            if (!adminOtpRequested) {
                                viewModel.requestAdminOtp(adminEmailInput, localContext) { success, msg ->
                                    adminOtpMessage = msg
                                    if (success) {
                                        adminOtpRequested = true
                                        adminOtpTimer = 60
                                    } else {
                                        adminOtpIsError = true
                                    }
                                }
                            } else {
                                if (adminOtpTimer <= 0) {
                                    adminOtpMessage = "El código OTP ha expirado (máximo 60 segundos). Reenvía un nuevo código."
                                    adminOtpIsError = true
                                    return@Button
                                }
                                viewModel.verifyAdminOtp(adminEmailInput, adminOtpInput) { success, msg ->
                                    adminOtpMessage = msg
                                    if (success) {
                                        onDismiss()
                                        adminOtpRequested = false
                                        adminOtpInput = ""
                                        adminOtpMessage = null
                                    } else {
                                        adminOtpIsError = true
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC67C65))
                    ) {
                        Text(
                            text = if (adminOtpRequested) "Verificar Código" else "Solicitar Acceso",
                            color = Color(0xFF0F0D0B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

fun <T> buildListWithBanners(
    items: List<T>,
    banners: List<com.example.models.AdBanner>
): List<Any> {
    if (banners.isEmpty() || items.isEmpty()) return items.map { it as Any }
    val interval = banners.firstOrNull()?.interval?.coerceAtLeast(1) ?: 6
    val result = mutableListOf<Any>()
    var bannerIndex = 0
    for (i in items.indices) {
        result.add(items[i] as Any)
        if ((i + 1) % interval == 0) {
            result.add(banners[bannerIndex % banners.size])
            bannerIndex++
        }
    }
    return result
}

@Composable
fun SingleAdBannerCard(
    banner: com.example.models.AdBanner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isHtmlType = banner.type.lowercase().trim() == "html" || (banner.type.isBlank() && banner.htmlCode.isNotBlank())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBg)
    ) {
        if (isHtmlType) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFC67C65).copy(alpha = 0.95f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "PUBLICIDAD",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (banner.title.isNotBlank()) {
                        Text(
                            text = banner.title,
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            android.webkit.WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                webViewClient = android.webkit.WebViewClient()
                                setBackgroundColor(0)
                                loadDataWithBaseURL("https://google.com", banner.htmlCode, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            val clickTarget = banner.effectiveClickUrl
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clickable(enabled = clickTarget.isNotBlank()) {
                        try {
                            var url = clickTarget.trim()
                            if (url.isNotBlank()) {
                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    url = "https://$url"
                                }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
                        }
                    }
            ) {
                if (banner.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = banner.imageUrl,
                        contentDescription = banner.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF2C2420), Color(0xFF1E1B18))))
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = Color(0xFFC67C65).copy(alpha = 0.95f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "PUBLICIDAD",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            if (banner.title.isNotBlank()) {
                                Text(
                                    text = banner.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (clickTarget.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0xFFC67C65), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Abrir",
                                    color = Color(0xFF0F0D0B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = Color(0xFF0F0D0B),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdBannerWidget(
    seccion: String,
    viewModel: MediaViewModel,
    modifier: Modifier = Modifier
) {
    val bannersState by viewModel.banners.collectAsStateWithLifecycle()

    val sectionBanners = remember(bannersState, seccion) {
        viewModel.getBannersForLocation(seccion)
    }

    if (sectionBanners.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(sectionBanners.size) {
        if (sectionBanners.size > 1) {
            while (true) {
                kotlinx.coroutines.delay(5000L)
                currentIndex = (currentIndex + 1) % sectionBanners.size
            }
        } else {
            currentIndex = 0
        }
    }

    val currentBanner = sectionBanners.getOrNull(currentIndex.coerceIn(0, sectionBanners.size - 1)) ?: return

    SingleAdBannerCard(banner = currentBanner, modifier = modifier)
}



