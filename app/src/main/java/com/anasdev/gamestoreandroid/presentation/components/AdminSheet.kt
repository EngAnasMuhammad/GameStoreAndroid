package com.anasdev.gamestoreandroid.presentation.components

import android.content.res.Configuration
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
import androidx.compose.material3.BottomSheetDefaults
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
import com.anasdev.gamestoreandroid.util.consumeUpwardSheetOverscroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anasdev.gamestoreandroid.R
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Add Game, 1 = Upload Keys

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        sheetMaxWidth = if (isLandscape) 840.dp else BottomSheetDefaults.SheetMaxWidth,
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .consumeUpwardSheetOverscroll()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = stringResource(R.string.admin_dashboard),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_cancel),
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
                    text = { Text(stringResource(R.string.admin_tab_add_game)) },
                    icon = { Icon(Icons.Rounded.PostAdd, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onClearSuccess()
                    },
                    text = { Text(stringResource(R.string.admin_tab_upload_keys)) },
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
                    isLandscape = isLandscape,
                    onSubmit = onCreateGame
                )
                1 -> UploadKeysForm(
                    games = games,
                    isLoading = isLoading,
                    successMessage = successMessage,
                    isLandscape = isLandscape,
                    onSubmit = onUploadKeys
                )
            }
        }
    }
}
}

@Composable
fun AddGameForm(
    isLoading: Boolean,
    successMessage: String?,
    isLandscape: Boolean = false,
    onSubmit: (CreateGameRequest, ByteArray?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var genre by rememberSaveable { mutableStateOf("") }
    var releaseDate by rememberSaveable { mutableStateOf("2024-01-01") }
    var priceStr by rememberSaveable { mutableStateOf("") }
    var localSelectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
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

    val imagePickerCard: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.admin_cover_image_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (localSelectedImageUri != null) {
                    Text(
                        text = stringResource(R.string.admin_ready_to_upload),
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
                        AsyncImage(
                            model = localSelectedImageUri,
                            contentDescription = "Cover Image Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f))
                        )

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
                                        contentDescription = stringResource(R.string.admin_remove_image),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.admin_remove_image),
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
                                Text(stringResource(R.string.admin_change_image), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = stringResource(R.string.admin_tap_to_choose_image),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = stringResource(R.string.admin_tap_to_choose_image),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.admin_image_format_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    val submitButton: @Composable () -> Unit = {
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
                    text = if (localSelectedImageUri != null) {
                        stringResource(R.string.admin_publishing_with_image)
                    } else {
                        stringResource(R.string.admin_publishing)
                    },
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.admin_action_publish_game), fontWeight = FontWeight.Bold)
            }
        }
    }

    val fieldsContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.admin_field_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text(stringResource(R.string.admin_field_genre)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text(stringResource(R.string.admin_field_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = releaseDate,
                onValueChange = { releaseDate = it },
                label = { Text(stringResource(R.string.admin_field_release_date)) },
                isError = releaseDate.isNotBlank() && !isDateValid,
                supportingText = {
                    if (releaseDate.isNotBlank() && !isDateValid) {
                        Text(stringResource(R.string.admin_error_date_format))
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.admin_field_description)) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (isLandscape) {
        // Landscape 2-Column Layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                imagePickerCard()
                submitButton()
            }

            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                fieldsContent()
            }
        }
    } else {
        // Portrait Layout
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            imagePickerCard()
            fieldsContent()
            Spacer(modifier = Modifier.height(4.dp))
            submitButton()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadKeysForm(
    games: List<GameResponse>,
    isLoading: Boolean,
    successMessage: String?,
    isLandscape: Boolean = false,
    onSubmit: (gameId: String, keys: List<String>) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedGameId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedGame = remember(selectedGameId, games) { games.find { it.id == selectedGameId } }
    var rawKeysText by rememberSaveable { mutableStateOf("") }

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

    val submitButton: @Composable () -> Unit = {
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
                Text(stringResource(R.string.admin_uploading_keys))
            } else {
                Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (parsedKeys.isEmpty()) {
                        stringResource(R.string.admin_action_upload_keys)
                    } else {
                        stringResource(R.string.admin_action_upload_n_keys, parsedKeys.size)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    val dropdownSelector: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.admin_select_target_game),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedGame?.let { "${it.title} (${it.availableStock})" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.admin_choose_game)) },
                    placeholder = { Text(stringResource(R.string.admin_tap_to_select_game)) },
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
                            text = { Text(stringResource(R.string.admin_no_games_published)) },
                            onClick = { expanded = false }
                        )
                    } else {
                        games.forEach { game ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(game.title, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${game.availableStock} • $${"%.2f".format(game.currentPrice)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedGameId = game.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    val keysInputField: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.admin_activation_keys_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = rawKeysText,
                onValueChange = { rawKeysText = it },
                label = { Text(stringResource(R.string.admin_paste_keys_label)) },
                placeholder = { Text("STEAM-AAAA-BBBB-CCCC\nSTEAM-XXXX-YYYY-ZZZZ") },
                minLines = if (isLandscape) 3 else 4,
                maxLines = 8,
                supportingText = {
                    Text(
                        text = if (parsedKeys.isEmpty()) {
                            stringResource(R.string.admin_enter_at_least_one_key)
                        } else {
                            stringResource(R.string.admin_keys_detected, parsedKeys.size)
                        },
                        color = if (parsedKeys.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                dropdownSelector()
                Spacer(modifier = Modifier.height(4.dp))
                submitButton()
            }

            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                keysInputField()
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            dropdownSelector()
            keysInputField()
            Spacer(modifier = Modifier.height(4.dp))
            submitButton()
        }
    }
}
