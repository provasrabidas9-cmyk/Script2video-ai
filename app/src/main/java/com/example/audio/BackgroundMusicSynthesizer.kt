package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Procedural ambient musical synthesizer that plays a gentle, cinematic chord progression.
 * Generates audio natively without requiring external MP3/WAV assets.
 */
class BackgroundMusicSynthesizer {

    private val sampleRate = 22050
    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var isPlaying = false

    fun start() {
        if (isPlaying) return
        isPlaying = true

        synthesisJob = scope.launch {
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minBufferSize, sampleRate * 2)

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.setVolume(0.22f) // Gentle backing volume
                audioTrack?.play()

                // Cinematic progression: F - Am - C - G (warm pentatonic frequencies)
                val chords = listOf(
                    floatArrayOf(174.61f, 220.00f, 261.63f, 329.63f), // Fmaj7
                    floatArrayOf(220.00f, 261.63f, 329.63f, 392.00f), // Am7
                    floatArrayOf(261.63f, 329.63f, 392.00f, 523.25f), // C
                    floatArrayOf(196.00f, 246.94f, 293.66f, 392.00f)  // G
                )

                val chordDurationSec = 3.5f
                val samplesPerChord = (sampleRate * chordDurationSec).toInt()
                val shortBuffer = ShortArray(1024)

                var chordIndex = 0
                var sampleCounter = 0

                while (isActive && isPlaying) {
                    val currentChord = chords[chordIndex % chords.size]
                    for (i in shortBuffer.indices) {
                        val t = sampleCounter.toDouble() / sampleRate
                        // Soft additive synthesis with warm harmonics
                        var sample = 0.0
                        for (freq in currentChord) {
                            sample += sin(2.0 * Math.PI * freq * t) * 0.25
                            sample += sin(2.0 * Math.PI * (freq * 0.5) * t) * 0.15 // warm sub
                            sample += sin(2.0 * Math.PI * (freq * 2.0) * t) * 0.05 // soft air shimmer
                        }

                        // Gentle envelope per chord
                        val progress = (sampleCounter % samplesPerChord).toFloat() / samplesPerChord
                        val envelope = if (progress < 0.15f) {
                            progress / 0.15f
                        } else if (progress > 0.85f) {
                            (1.0f - progress) / 0.15f
                        } else {
                            1.0f
                        }

                        val finalVal = (sample * envelope * 0.35 * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

                        shortBuffer[i] = finalVal.toShort()
                        sampleCounter++

                        if (sampleCounter % samplesPerChord == 0) {
                            chordIndex++
                        }
                    }

                    audioTrack?.write(shortBuffer, 0, shortBuffer.size)
                }
            } catch (e: Exception) {
                Log.e("BgmSynthesizer", "Audio synthesis stopped", e)
            } finally {
                cleanup()
            }
        }
    }

    fun stop() {
        isPlaying = false
        synthesisJob?.cancel()
        cleanup()
    }

    private fun cleanup() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            // Ignore track release errors
        }
    }
}
