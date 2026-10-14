package com.example.ui

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.exp
import kotlin.math.sin

object ThemeSoundEffect {

    fun playTransitionSound(toDark: Boolean) {
        Thread {
            try {
                val sampleRate = 44100
                val durationMs = 800
                val numSamples = durationMs * sampleRate / 1000
                val generatedSnd = ByteArray(2 * numSamples)

                // Base frequencies
                val freq1 = if (toDark) 220.0 else 440.0
                val freq2 = if (toDark) 277.18 else 554.37 // Major third
                val freq3 = if (toDark) 329.63 else 659.25 // Perfect fifth

                for (i in 0 until numSamples) {
                    val time = i.toDouble() / sampleRate
                    
                    // Generate sound by mixing frequencies
                    val wave1 = sin(2 * Math.PI * freq1 * time)
                    val wave2 = sin(2 * Math.PI * freq2 * time) * 0.5
                    val wave3 = sin(2 * Math.PI * freq3 * time) * 0.25
                    
                    val mixed = (wave1 + wave2 + wave3) / 1.75
                    
                    // Envelope: fast attack, exponential decay
                    val envelope = if (time < 0.05) {
                        time / 0.05 // attack
                    } else {
                        exp(-(time - 0.05) * 4.0) // decay
                    }

                    val currentVal = mixed * envelope * 0.3 // Volume reduced for subtlety

                    // Convert to 16-bit PCM
                    var valInt = (currentVal * 32767).toInt()
                    if (valInt > 32767) valInt = 32767
                    if (valInt < -32768) valInt = -32768

                    val idx = i * 2
                    generatedSnd[idx] = (valInt and 0x00ff).toByte()
                    generatedSnd[idx + 1] = ((valInt and 0xff00) ushr 8).toByte()
                }

                val audioTrack = android.media.AudioTrack.Builder()
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(generatedSnd.size)
                    .setTransferMode(android.media.AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()

                Thread.sleep(durationMs.toLong() + 100)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
