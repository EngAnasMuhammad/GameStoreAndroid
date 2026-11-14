package com.anasdev.gamestoreandroid.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PostAdd
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anasdev.gamestoreandroid.domain.model.CreateGameRequest
import com.anasdev.gamestoreandroid.domain.model.GameResponse
import com.anasdev.gamestoreandroid.util.ImageCompressor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSheet(
    games: List<GameResponse>,
    isLoading: Boolean,
    isUploadingImage: Boolean = false,
    uploadedImagePath: String? = null,
    successMessage: String?,
    onDismiss: () -> Unit,
    onCreateGame: (CreateGameRequest, ByteArray?) -> Unit,
    onUploadKeys: (gameId: String, keys: List<String>) -> Unit,
    onUploadImage: (bytes: ByteArray, fileName: String, mimeType: String) -> Unit = { _, _, _ -> },
    onClearSuccess: () -> Unit,
    onClearUploadedImage: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Add Game, 1 = Upload Keys

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with explicit Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Admin Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tabs Toggle
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        onClearSuccess()
                    },
                    text = { Text("Add New Game") },
                    icon = { Icon(Icons.Rounded.PostAdd, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onClearSuccess()
                    },
                    text = { Text("Upload Keys") },
                    icon = { Icon(Icons.Rounded.VpnKey, contentDescription = null) }
                )
            }

            // Success feedback banner
            if (!successMessage.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = successMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> AddGameForm(
                    isLoading = isLoading,
                    successMessage = successMessage,
                    onSubmit = onCreateGame
                )
                1 -> UploadKeysForm(
                    games = games,
                    isLoading = isLoading,
                    successMessage = successMessage,
                    onSubmit = onUploadKeys
                )
            }
        }
    }
}

@Composable
fun AddGameForm(
    isLoading: Boolean,
    successMessage: String?,
    onSubmit: (CreateGameRequest, ByteArray?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var releaseDate by remember { mutableStateOf("2024-01-01") }
    var priceStr by remember { mutableStateOf("") }
    var localSelectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isCompressing by remember { mutableStateOf(false) }

    // Launcher for picking images locally without immediate network upload
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            localSelectedImageUri = uri
        }
    }

    // Clear fields upon successful creation
    LaunchedEffect(successMessage) {
        if (!successMessage.isNullOrBlank()) {
            title = ""
            description = ""
            genre = ""
            releaseDate = "2024-01-01"
            priceStr = ""
            localSelectedImageUri = null
        }
    }

    val priceDouble = priceStr.toDoubleOrNull()
    val isDateValid = releaseDate.matches(Regex("""\d{4}-\d{2}-\d{2}"""))
    val isFormValid = title.isNotBlank() &&
            genre.isNotBlank() &&
            isDateValid &&
            priceDouble != null &&
            priceDouble >= 0.0

    val isSubmitting = isLoading || isCompressing

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Image Picker Box Section (16:9 Aspect Ratio with ContentScale.Crop)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Game Cover Image (Optional)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (localSelectedImageUri != null) {
                Text(
                    text = "Ready to upload",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = if (localSelectedImageUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(enabled = !isSubmitting) {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (localSelectedImageUri != null) {
                    // Local Image preview using Coil with uniform ContentScale.Crop
                    AsyncImage(
                        model = localSelectedImageUri,
                        contentDescription = "Cover Image Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Subtle scrim overlay for action buttons
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f))
                    )

                    // Action buttons: Remove & Change Image
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier.clickable { localSelectedImageUri = null }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Remove",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Image", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    // Empty Picker Prompt
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Select Cover Image",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Tap to choose cover image",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "PNG, JPG, or WEBP (auto-compressed on submit)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = genre,
            onValueChange = { genre = it },
            label = { Text("Genre * (e.g. Action RPG, Strategy)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = releaseDate,
            onValueChange = { releaseDate = it },
            label = { Text("Release Date * (YYYY-MM-DD)") },
            isError = releaseDate.isNotBlank() && !isDateValid,
            supportingText = {
                if (releaseDate.isNotBlank() && !isDateValid) {
                    Text("Format must be YYYY-MM-DD")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = priceStr,
            onValueChange = { priceStr = it },
            label = { Text("Price in USD * (e.g. 59.99)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                if (isFormValid && priceDouble != null) {
                    coroutineScope.launch {
                        isCompressing = true
                        val compressedBytes = localSelectedImageUri?.let { uri ->
                            try {
                                ImageCompressor.compressImage(context, uri)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                null
                            }
                        }
                        isCompressing = false

                        onSubmit(
                            CreateGameRequest(
                                title = title.trim(),
                                genre = genre.trim(),
                                releaseDate = releaseDate.trim(),
                                currentPrice = priceDouble,
                                imagePath = "",
                                description = description.trim().ifEmpty { null }
                            ),
                            compressedBytes
                        )
                    }
                }
            },
            enabled = isFormValid && !isSubmitting,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (localSelectedImageUri != null) "Uploading & Creating..." else "Creating Game...",
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publish Game", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadKeysForm(
    games: List<GameResponse>,
    isLoading: Boolean,
    successMessage: String?,
    onSubmit: (gameId: String, keys: List<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedGame by remember { mutableStateOf<GameResponse?>(null) }
    var rawKeysText by remember { mutableStateOf("") }

    // Clear keys input upon success
    LaunchedEffect(successMessage) {
        if (!successMessage.isNullOrBlank()) {
            rawKeysText = ""
        }
    }

    // Live parsing of keys separated by newline or comma
    val parsedKeys = remember(rawKeysText) {
        rawKeysText.split('\n', ',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    val canSubmit = selectedGame != null && parsedKeys.isNotEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Select Target Game",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )

        // Dropdown menu box to select game
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedGame?.let { "${it.title} (Stock: ${it.availableStock})" } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Choose Game") },
                placeholder = { Text("Tap to select a game") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (games.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No games published yet") },
                        onClick = { expanded = false }
                    )
                } else {
                    games.forEach { game ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(game.title, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Stock: ${game.availableStock} • $${"%.2f".format(game.currentPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                selectedGame = game
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Activation Keys",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = rawKeysText,
            onValueChange = { rawKeysText = it },
            label = { Text("Paste Keys (separated by lines or commas)") },
            placeholder = { Text("STEAM-AAAA-BBBB-CCCC\nSTEAM-XXXX-YYYY-ZZZZ") },
            minLines = 4,
            maxLines = 8,
            supportingText = {
                Text(
                    text = if (parsedKeys.isEmpty()) "Enter at least 1 activation key" else "${parsedKeys.size} valid key(s) detected",
                    color = if (parsedKeys.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                val game = selectedGame
                if (game != null && parsedKeys.isNotEmpty()) {
                    onSubmit(game.id, parsedKeys)
                }
            },
            enabled = canSubmit && !isLoading,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uploading Keys...")
            } else {
                Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (parsedKeys.isEmpty()) "Upload Keys" else "Upload ${parsedKeys.size} Key(s)",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
