package com.example.ui

import android.app.Activity
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AstrologyInsight
import com.example.data.ChatSession
import com.example.data.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    profile: UserProfile,
    insights: List<AstrologyInsight>,
    settings: com.example.data.UserSettings?,
    onClose: () -> Unit,
    onNavigateToModelHub: (() -> Unit)? = null
) {
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val isLoading by chatViewModel.isLoading.collectAsStateWithLifecycle()
    val sessions by chatViewModel.sessions.collectAsStateWithLifecycle()
    val currentSessionId by chatViewModel.currentSessionId.collectAsStateWithLifecycle()
    val retentionPeriodMillis by chatViewModel.retentionPeriodMillis.collectAsStateWithLifecycle()
    val isModelMounted by chatViewModel.isModelMounted.collectAsStateWithLifecycle()
    val hostedModel by chatViewModel.hostedModel.collectAsStateWithLifecycle()
    val rollingContextState by chatViewModel.rollingContextState.collectAsStateWithLifecycle()

    var tts by remember { mutableStateOf<android.speech.tts.TextToSpeech?>(null) }
    val context = LocalContext.current

    DisposableEffect(context) {
        val textToSpeech = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                // TTS initialized
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    var lastSpokenSessionId by remember { mutableIntStateOf(-1) }
    var lastSpokenMessageSize by remember { mutableIntStateOf(-1) }

    LaunchedEffect(messages, currentSessionId) {
        if (lastSpokenSessionId != currentSessionId) {
            lastSpokenSessionId = currentSessionId ?: -1
            lastSpokenMessageSize = messages.size
        } else {
            if (messages.size > lastSpokenMessageSize) {
                val lastMessage = messages.lastOrNull()
                if (lastMessage != null && !lastMessage.isUser && !lastMessage.isError && settings?.enableAudioFeedback == true) {
                    tts?.speak(lastMessage.content, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                }
                lastSpokenMessageSize = messages.size
            } else if (messages.size < lastSpokenMessageSize) {
                lastSpokenMessageSize = messages.size
            }
        }
    }

    var inputText by remember { mutableStateOf("") }
    var showSessionsSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    var sessionToRename by remember { mutableStateOf<ChatSession?>(null) }
    var renameText by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var sessionToExport by remember { mutableStateOf<ChatSession?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let { destUri ->
            coroutineScope.launch {
                sessionToExport?.let { session ->
                    val content = chatViewModel.getFormattedChatHistoryForSession(session.id)
                    try {
                        context.contentResolver.openOutputStream(destUri)?.use { os ->
                            os.write(content.toByteArray())
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                sessionToExport = null
            }
        }
    }

    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            chatViewModel.mountModelFromUri(uri) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
            }
        }
    }

    LaunchedEffect(profile, insights) {
        chatViewModel.initContext(profile, insights)
    }

    if (showSessionsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSessionsSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cosmic Sessions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        showSessionsSheet = false
                        showSettingsSheet = true
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "History Settings")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                val searchQuery by chatViewModel.searchQuery.collectAsStateWithLifecycle()
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { chatViewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search sessions...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.weight(1f, false)) {
                    items(sessions, key = { it.id }) { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (session.id == currentSessionId) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .combinedClickable(
                                    onClick = {
                                        chatViewModel.loadSession(session.id)
                                        showSessionsSheet = false
                                        chatViewModel.updateSearchQuery("")
                                    },
                                    onLongClick = {
                                        sessionToRename = session
                                        renameText = session.title
                                    }
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = session.title,
                                modifier = Modifier.weight(1f),
                                color = if (session.id == currentSessionId) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { chatViewModel.togglePinSession(session) }) {
                                Icon(
                                    if (session.isPinned) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = "Pin Session",
                                    tint = if (session.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                sessionToExport = session
                                exportLauncher.launch("Chat_Session_${session.id}.txt")
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Export Session", tint = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { chatViewModel.deleteSession(session.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Session", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        chatViewModel.startNewSession()
                        showSessionsSheet = false
                        chatViewModel.updateSearchQuery("")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Chat")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Chat History Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                val week = 7L * 24 * 60 * 60 * 1000
                val month = 30L * 24 * 60 * 60 * 1000
                val threeMonths = 90L * 24 * 60 * 60 * 1000
                val year = 365L * 24 * 60 * 60 * 1000
                val never = 0L

                val retentionOptions = listOf(
                    "1 week" to week,
                    "1 month" to month,
                    "3 months" to threeMonths,
                    "1 year" to year,
                    "Never" to never
                )

                Text("Delete unpinned chats after:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                retentionOptions.forEach { (label, duration) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { chatViewModel.setRetentionPeriod(duration) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = retentionPeriodMillis == duration,
                            onClick = { chatViewModel.setRetentionPeriod(duration) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (sessionToRename != null) {
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = { Text("Rename Chat Session") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionToRename?.let {
                            if (renameText.isNotBlank()) chatViewModel.renameSession(it.id, renameText.trim())
                        }
                        sessionToRename = null
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    com.example.ui.components.CosmicBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Chat")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cosmic Guide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigateToModelHub?.invoke() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isModelMounted) Color(0xFF4CAF50) else Color(0xFFFFA000))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isModelMounted) {
                                hostedModel?.let { "${it.fileName.take(18)} • 16k Context" } ?: "4B Model Active"
                            } else {
                                "Model Upload Required"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isModelMounted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                IconButton(onClick = { showSessionsSheet = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Chat Options")
                }
            }
            HorizontalDivider()

            // 16k Rolling Context Status Ribbon
            if (isModelMounted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "16k Rolling Context Buffer",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${rollingContextState.estimatedActiveTokens} / 16,384 tokens (${(rollingContextState.capacityPercent * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }

            // Message Area & Model Gate
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                reverseLayout = false
            ) {
                // If model is NOT mounted, show high-contrast requirement card at the top
                if (!isModelMounted) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .testTag("model_required_banner"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "4B On-Device Model Required",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "To preserve total privacy and eliminate cloud API keys, AstroLog runs on-device. Please upload or select your 4B quantized model (.gguf or .bin) from device storage or Google Drive.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            modelPickerLauncher.launch(arrayOf("*/*", "application/octet-stream"))
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Mount 4B Model")
                                    }
                                    if (onNavigateToModelHub != null) {
                                        OutlinedButton(
                                            onClick = onNavigateToModelHub,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Model Hub")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                items(messages) { msg ->
                    ChatBubble(msg, modifier = Modifier)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cosmic Guide is thinking on-device...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
                    .padding(16.dp)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(if (isModelMounted) "Ask about your chart on-device..." else "Mount 4B model to chat...")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    ),
                    maxLines = 4,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                }
                                try {
                                    speechRecognizerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            enabled = isModelMounted && !isLoading
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Dictate")
                        }
                    },
                    enabled = !isLoading && isModelMounted
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading && isModelMounted) {
                            chatViewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .background(
                            if (isLoading || !isModelMounted) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    enabled = !isLoading && isModelMounted
                ) {
                    AnimatedContent(targetState = isLoading, label = "SendButtonAnimation") { loading ->
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (isModelMounted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = message.isUser
    val bgColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else if (message.isError) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else if (message.isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(alignment)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(bgColor)
                .padding(14.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
