package com.aegis.ui.assistant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.agents.GuardianCore
import com.aegis.ui.components.GradientTopBar
import com.aegis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    guardianCore: GuardianCore,
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val isModelLoaded by viewModel.isModelLoaded.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadProgress by viewModel.loadProgress.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val installError by viewModel.installError.collectAsState()
    val isOnlineMode by viewModel.isOnlineMode.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importModel(it) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("AEGIS Cloud Intelligence", fontWeight = FontWeight.Bold)
                        if (isOnlineMode) {
                            Text(
                                "AI Guardian: Cloud Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = SafeGreen
                            )
                        } else if (isModelLoaded) {
                            Text(
                                "AI Guardian: Privacy Mode (Offline)",
                                style = MaterialTheme.typography.labelSmall,
                                color = SafeGreen
                            )
                        } else {
                            Text(
                                "AI Guardian: Initializing...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Privacy", style = MaterialTheme.typography.labelSmall)
                        Switch(
                            checked = !isOnlineMode,
                            onCheckedChange = { viewModel.togglePrivacyMode(it) },
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Column {
                    if (messages.size <= 1) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val replies = viewModel.getQuickReplies()
                            items(replies) { reply ->
                                SuggestionChip(
                                    onClick = {
                                        inputText = reply
                                        viewModel.sendMessage(reply)
                                        inputText = ""
                                    },
                                    label = { Text(reply, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask about cybersecurity...") },
                            shape = AegisPillShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank(),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = AegisPrimaryLight
                            )
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GradientTopBar()
            
            if (!isOnlineMode && !isModelLoaded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        downloadProgress != null && downloadProgress!! < 100 -> {
                            Text(
                                "Downloading Gemma 3N: $downloadProgress%",
                                style = MaterialTheme.typography.labelSmall,
                                color = AegisPrimaryLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress!!.toFloat() / 100f },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = AegisPrimaryLight,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        isLoading -> {
                            Text(
                                "Initializing Local Guardian Engine...",
                                style = MaterialTheme.typography.labelSmall,
                                color = AegisPrimaryLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { loadProgress },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = AegisPrimaryLight,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        else -> {
                            if (installError != null) {
                                Text(
                                    installError!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.triggerInstall() },
                                    modifier = Modifier.weight(1f),
                                    shape = AegisButtonShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = AegisPrimary, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download", fontWeight = FontWeight.Bold)
                                }
                                
                                OutlinedButton(
                                    onClick = { viewModel.triggerScan() },
                                    modifier = Modifier.weight(1f),
                                    shape = AegisButtonShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AegisPrimary)
                                ) {
                                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = AegisPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scan", fontWeight = FontWeight.Bold, color = AegisPrimary)
                                }

                                OutlinedButton(
                                    onClick = { filePickerLauncher.launch(arrayOf("application/octet-stream")) },
                                    modifier = Modifier.weight(1f),
                                    shape = AegisButtonShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AegisPrimary)
                                ) {
                                    Icon(Icons.Filled.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp), tint = AegisPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Choose", fontWeight = FontWeight.Bold, color = AegisPrimary)
                                }
                            }
                            
                            Text(
                                "Gemma 3N: 100% Offline AI reasoning",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message = message)
                }
                if (isTyping) {
                    item {
                ChatBubble(
                    message = AssistantViewModel.ChatMessage(
                        text = "Thinking...",
                        isUser = false
                    ),
                    isTyping = true
                )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: AssistantViewModel.ChatMessage,
    isTyping: Boolean = false
) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val color = if (message.isUser) AegisPrimaryLight else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isUser) 20.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 20.dp
            ),
            color = color,
            tonalElevation = 2.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
