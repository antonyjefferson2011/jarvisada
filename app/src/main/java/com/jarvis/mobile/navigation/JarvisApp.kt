package com.jarvis.mobile.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import com.jarvis.mobile.ui.ChatViewModel
import com.jarvis.mobile.ui.JarvisScreen
import com.jarvis.mobile.ui.SettingsScreen
import com.jarvis.mobile.ui.AssistantStatus
import com.jarvis.mobile.voice.VoiceController

@Composable
fun JarvisApp(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val voiceController = remember(context) {
        VoiceController(
            context = context,
            onText = viewModel::sendMessage,
            onListeningChanged = viewModel::setListening,
            onError = viewModel::setError,
        )
    }
    DisposableEffect(voiceController) {
        onDispose { voiceController.release() }
    }
    DisposableEffect(lifecycleOwner, voiceController) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                voiceController.cancelListening()
                voiceController.stopSpeaking()
                viewModel.setListening(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state.speechSequence) {
        if (state.speechSequence > 0 && state.voiceEnabled) {
            voiceController.speak(state.speechText, state.voiceSpeed, state.voiceVolume)
        }
    }

    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            voiceController.startListening()
        } else {
            viewModel.setError("Permita o acesso ao microfone para usar a entrada por voz.")
        }
    }

    val onMicToggle = {
        if (state.status == AssistantStatus.LISTENING) {
            voiceController.cancelListening()
        } else if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            voiceController.startListening()
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (showSettings) {
        SettingsScreen(
            state = state,
            onBack = { showSettings = false },
            onSaveBackend = viewModel::saveBackend,
            onSignIn = viewModel::signIn,
            onCreateAccount = viewModel::createAccount,
            onSignOut = viewModel::signOut,
            onSaveVoiceSettings = viewModel::saveVoiceSettings,
            onAddMemory = viewModel::addMemory,
            onRemoveMemory = viewModel::removeMemory,
            onClearHistory = viewModel::clearHistory,
            onDismissMessages = viewModel::dismissMessages,
        )
    } else {
        JarvisScreen(
            state = state,
            onSend = viewModel::sendMessage,
            onOpenSettings = { showSettings = true },
            onMicToggle = onMicToggle,
            onStopSpeaking = voiceController::stopSpeaking,
            onClearHistory = viewModel::clearHistory,
            onDismissMessages = viewModel::dismissMessages,
        )
    }
}
