package com.example.ui.sheets

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.LiveUstadSessionManager
import com.example.ai.LiveVoiceConnectionStatus
import com.example.ui.theme.LocalKaamWalaExtras
import com.example.util.PlatformHelpers

@Composable
fun LiveVoiceAssistantDialog(
    city: String,
    onDismiss: () -> Unit,
    onBookRecommendedService: (String) -> Unit
) {
    val context = LocalContext.current
    val extras = LocalKaamWalaExtras.current
    val scope = rememberCoroutineScope()
    val manager = remember(city) { LiveUstadSessionManager(city = city) }
    val voiceState by manager.state.collectAsStateWithLifecycle()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasAudioPermission = granted
            if (granted) {
                manager.startConversation(scope)
            }
        }
    )

    DisposableEffect(manager) {
        onDispose {
            manager.stopConversation(scope)
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "voice_pulse")
    val orbScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (voiceState.status == LiveVoiceConnectionStatus.LIVE && voiceState.isAudioActive) 1.14f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )

    Dialog(
        onDismissRequest = {
            manager.stopConversation(scope)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, extras.cardBorder),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(16.dp)
                .testTag("live_voice_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Ustad Live Voice",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Urdu ya English mein bol kar masla batayein",
                            style = MaterialTheme.typography.bodySmall,
                            color = extras.secondaryText
                        )
                    }
                    IconButton(
                        onClick = {
                            manager.stopConversation(scope)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("close_live_voice_button")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close AI Ustad Voice")
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Pulsing Voice Orb Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(extras.heroStart, extras.heroEnd)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .scale(orbScale)
                                .clip(CircleShape)
                                .background(
                                    when (voiceState.status) {
                                        LiveVoiceConnectionStatus.LIVE -> extras.emeraldMint.copy(alpha = 0.25f)
                                        LiveVoiceConnectionStatus.ERROR -> extras.dangerBg
                                        else -> Color.White.copy(alpha = 0.14f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (voiceState.status) {
                                            LiveVoiceConnectionStatus.LIVE -> MaterialTheme.colorScheme.primary
                                            LiveVoiceConnectionStatus.ERROR -> extras.dangerText
                                            else -> Color.White.copy(alpha = 0.22f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        voiceState.status == LiveVoiceConnectionStatus.LIVE && voiceState.isAudioActive ->
                                            Icons.Filled.GraphicEq
                                        voiceState.status == LiveVoiceConnectionStatus.LIVE ->
                                            Icons.Filled.MicOff
                                        else -> Icons.Filled.Mic
                                    },
                                    contentDescription = "Live Voice Status",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = when (voiceState.status) {
                                LiveVoiceConnectionStatus.IDLE -> "Tap Start Call to talk live with AI Ustad"
                                LiveVoiceConnectionStatus.CONNECTING -> "Connecting to Gemini Live Audio..."
                                LiveVoiceConnectionStatus.LIVE ->
                                    if (voiceState.isAudioActive) "Live Audio Active • Speak Naturally"
                                    else "Microphone Paused"
                                LiveVoiceConnectionStatus.ERROR -> "Connection Interrupted"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Error Message
                if (voiceState.errorMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = extras.dangerBg,
                        border = BorderStroke(1.dp, extras.dangerText.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = voiceState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = extras.dangerText,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Tool-recommended Service Card
                voiceState.recommendedService?.let { svc ->
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = extras.emeraldTint,
                        border = BorderStroke(1.dp, extras.emeraldBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Recommended: ${svc.nameEn}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = extras.heroStart
                                )
                                Text(
                                    text = "${voiceState.recommendationReason ?: svc.desc} • Visit ${PlatformHelpers.formatPkr(svc.fee)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extras.secondaryText
                                )
                            }
                            Button(
                                onClick = {
                                    manager.stopConversation(scope)
                                    onBookRecommendedService(svc.id)
                                },
                                modifier = Modifier.heightIn(min = 42.dp)
                            ) {
                                Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Book", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Quick topic chips when live
                if (voiceState.status == LiveVoiceConnectionStatus.LIVE) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        listOf(
                            "AC cooling kam hai aur E1 error aa raha hai",
                            "Solar panels wash karwane hain",
                            "Bathroom tap leak ho raha hai",
                            "Female salon ghar par chahiye"
                        ).forEach { topic ->
                            SuggestionChip(
                                onClick = { manager.sendQuickTopic(scope, topic) },
                                label = { Text(topic, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Live Transcript
                if (voiceState.transcript.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, extras.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = 180.dp)
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(voiceState.transcript, key = { it.id }) { entry ->
                                val isUstad = entry.speaker == "USTAD"
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isUstad) extras.emeraldTint else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = if (isUstad) "AI Ustad" else "Aap",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isUstad) MaterialTheme.colorScheme.primary else extras.mutedText,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(entry.text, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (voiceState.status) {
                        LiveVoiceConnectionStatus.IDLE,
                        LiveVoiceConnectionStatus.ERROR -> {
                            Button(
                                onClick = {
                                    if (hasAudioPermission) {
                                        manager.startConversation(scope)
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("start_live_voice_button")
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (hasAudioPermission) "Start Live Voice Call" else "Allow Mic & Start",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }

                        LiveVoiceConnectionStatus.CONNECTING -> {
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Connecting...", style = MaterialTheme.typography.titleSmall)
                            }
                        }

                        LiveVoiceConnectionStatus.LIVE -> {
                            OutlinedButton(
                                onClick = { manager.toggleMic(scope) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    if (voiceState.isAudioActive) Icons.Filled.MicOff else Icons.Filled.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (voiceState.isAudioActive) "Pause Mic" else "Resume Mic")
                            }
                            Button(
                                onClick = {
                                    manager.stopConversation(scope)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = extras.dangerText,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Filled.CallEnd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("End Call")
                            }
                        }
                    }
                }
            }
        }
    }
}
