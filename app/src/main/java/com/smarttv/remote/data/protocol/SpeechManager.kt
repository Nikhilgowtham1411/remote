package com.smarttv.remote.data.protocol

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.smarttv.remote.data.model.RemoteCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceAction {
    data class Command(val command: RemoteCommand) : VoiceAction()
    data class LaunchApp(val appName: String) : VoiceAction()
    data class SearchQuery(val query: String) : VoiceAction()
}

class SpeechManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    var onActionDetected: ((VoiceAction) -> Unit)? = null

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("SpeechManager", "Speech recognition not available on device.")
            return
        }

        stopListening()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _isListening.value = false
                }
                override fun onError(error: Int) {
                    _isListening.value = false
                    Log.w("SpeechManager", "Speech error code: $error")
                }
                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        _recognizedText.value = spokenText
                        processSpokenText(spokenText)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _recognizedText.value = matches[0]
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        _isListening.value = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    private fun processSpokenText(text: String) {
        val lower = text.lowercase(Locale.ROOT).trim()
        val action = when {
            lower.contains("volume up") || lower.contains("increase volume") ->
                VoiceAction.Command(RemoteCommand.VOLUME_UP)
            lower.contains("volume down") || lower.contains("decrease volume") ->
                VoiceAction.Command(RemoteCommand.VOLUME_DOWN)
            lower.contains("mute") || lower.contains("unmute") ->
                VoiceAction.Command(RemoteCommand.MUTE)
            lower.contains("home") ->
                VoiceAction.Command(RemoteCommand.HOME)
            lower.contains("back") ->
                VoiceAction.Command(RemoteCommand.BACK)
            lower.contains("play") || lower.contains("pause") ->
                VoiceAction.Command(RemoteCommand.MEDIA_PLAY_PAUSE)
            lower.contains("netflix") ->
                VoiceAction.LaunchApp("netflix")
            lower.contains("youtube") ->
                VoiceAction.LaunchApp("youtube")
            lower.contains("hotstar") || lower.contains("jio") ->
                VoiceAction.LaunchApp("jiohotstar")
            lower.contains("prime") || lower.contains("amazon") ->
                VoiceAction.LaunchApp("primevideo")
            else ->
                VoiceAction.SearchQuery(text)
        }
        onActionDetected?.invoke(action)
    }
}
