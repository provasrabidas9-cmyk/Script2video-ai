package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.ScriptLanguage
import com.example.data.model.VoiceGender
import com.example.data.model.VoiceSpeed
import java.io.File
import java.util.Locale
import java.util.UUID

class TtsAudioEngine(
    private val context: Context,
    private val onInitComplete: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private var onUtteranceStartCallback: ((String) -> Unit)? = null
    private var onUtteranceDoneCallback: ((String) -> Unit)? = null
    private var onUtteranceErrorCallback: ((String) -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("TtsAudioEngine", "Failed to create TTS instance", e)
            onInitComplete(false)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupUtteranceListener()
            onInitComplete(true)
        } else {
            isInitialized = false
            Log.w("TtsAudioEngine", "TTS initialization failed with code $status")
            onInitComplete(false)
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                utteranceId?.let { onUtteranceStartCallback?.invoke(it) }
            }

            override fun onDone(utteranceId: String?) {
                utteranceId?.let { onUtteranceDoneCallback?.invoke(it) }
            }

            override fun onError(utteranceId: String?) {
                utteranceId?.let { onUtteranceErrorCallback?.invoke(it) }
            }
        })
    }

    fun applySettings(language: ScriptLanguage, gender: VoiceGender, speed: VoiceSpeed) {
        val ttsInstance = tts ?: return
        if (!isInitialized) return

        // 1. Language configuration
        val locale = language.locale
        val langResult = ttsInstance.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Try generic language code without country
            val fallbackLocale = Locale(language.code)
            ttsInstance.setLanguage(fallbackLocale)
        }

        // 2. Gender pitch shaping
        val pitch = when (gender) {
            VoiceGender.MALE -> 0.85f
            VoiceGender.FEMALE -> 1.22f
        }
        ttsInstance.setPitch(pitch)

        // 3. Speech Rate
        ttsInstance.setSpeechRate(speed.rateMultiplier)
    }

    fun speak(
        text: String,
        utteranceId: String = UUID.randomUUID().toString(),
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized || text.isBlank()) {
            onDone?.invoke()
            return
        }

        onUtteranceStartCallback = { id ->
            if (id == utteranceId) onStart?.invoke()
        }
        onUtteranceDoneCallback = { id ->
            if (id == utteranceId) onDone?.invoke()
        }
        onUtteranceErrorCallback = { id ->
            if (id == utteranceId) onDone?.invoke()
        }

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun synthesizeToFile(
        text: String,
        outputFile: File,
        utteranceId: String = UUID.randomUUID().toString(),
        onComplete: (Boolean) -> Unit
    ) {
        if (!isInitialized || text.isBlank()) {
            onComplete(false)
            return
        }

        onUtteranceDoneCallback = { id ->
            if (id == utteranceId) onComplete(true)
        }
        onUtteranceErrorCallback = { id ->
            if (id == utteranceId) onComplete(false)
        }

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        val res = tts?.synthesizeToFile(text, params, outputFile, utteranceId)
        if (res != TextToSpeech.SUCCESS) {
            onComplete(false)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("TtsAudioEngine", "Error stopping TTS", e)
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e("TtsAudioEngine", "Error shutting down TTS", e)
        }
    }
}
