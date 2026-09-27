package com.example.ui.audio

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

object AudioAlertsManager {
    private const val TAG = "AudioAlertsManager"
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            // Use STREAM_MUSIC as standard, with standard medium-high volume of 80% (range 0 to 100)
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate ToneGenerator", e)
        }
    }

    /**
     * Plays a distinct, clean audio tone for Stopwatch Time Capture (distinct double-beep acknowledgment tone, e.g. 1200Hz)
     */
    fun playCaptureTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling playCaptureTone", e)
        }
    }

    /**
     * Plays a subtle, medium-high frequency tone for Effective activities (e.g., Bundle, Bobbin) - a fast 100ms 1200Hz beep.
     */
    fun playEffectiveTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling playEffectiveTone", e)
        }
    }

    /**
     * Plays a distinct medium frequency warning-type tone for Non-Effective activities (e.g., Thread break, needle break).
     */
    fun playNonEffectiveTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling playNonEffectiveTone", e)
        }
    }
}
