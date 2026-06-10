package com.deepak.alarm.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Manages alarm audio playback with USAGE_ALARM attributes.
 * Supports built-in sounds, custom URIs, looping, and increasing volume.
 * Handles AudioFocus to act politely during phone calls.
 */
class MediaPlayerManager(private val context: Context) : AudioManager.OnAudioFocusChangeListener {

    companion object {
        private const val TAG = "MediaPlayerManager"
        private const val VOLUME_RAMP_DURATION_MS = 30_000L // 30 seconds
        private const val VOLUME_RAMP_INTERVAL_MS = 1_000L  // 1 second
    }

    private var mediaPlayer: MediaPlayer? = null
    private var volumeHandler: Handler? = null
    private var currentVolume = 0f
    private var targetVolume = 1f
    private var isIncreasingVolume = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    /**
     * Start playing the alarm sound.
     * @param soundUri URI of the sound to play, or empty for default alarm sound
     * @param increaseVolume if true, gradually increase volume over 30 seconds
     */
    fun start(soundUri: String = "", increaseVolume: Boolean = true) {
        stop() // Stop any existing playback

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // Request Audio focus
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this, Handler(Looper.getMainLooper()))
                .build()
                
            audioManager.requestAudioFocus(audioFocusRequest!!)

            val uri = if (soundUri.isNotEmpty()) {
                Uri.parse(soundUri)
            } else {
                getDefaultAlarmUri()
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(context, uri)
                isLooping = true

                setOnPreparedListener { mp ->
                    if (increaseVolume) {
                        mp.setVolume(0f, 0f)
                        startVolumeRamp()
                    } else {
                        mp.setVolume(1f, 1f)
                    }
                    mp.start()
                    Log.d(TAG, "Alarm audio started: $uri")
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    if (soundUri.isNotEmpty()) {
                        Log.d(TAG, "Retrying with default alarm sound")
                        stop()
                        start("", increaseVolume)
                    }
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm audio", e)
            if (soundUri.isNotEmpty()) {
                start("", increaseVolume)
            }
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus transient loss/duck, lowering volume")
                // Pause volume ramp if active and lower volume
                isIncreasingVolume = false
                mediaPlayer?.setVolume(0.1f, 0.1f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus gained, restoring volume")
                if (targetVolume == 1f && currentVolume < 1f) {
                    // Resume volume ramp
                    isIncreasingVolume = true
                    startVolumeRampFromCurrent()
                } else {
                    mediaPlayer?.setVolume(1f, 1f)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Audio focus lost, abandoning focus")
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            }
        }
    }

    private fun startVolumeRampFromCurrent() {
        volumeHandler?.removeCallbacksAndMessages(null)
        volumeHandler = Handler(Looper.getMainLooper())
        val steps = (VOLUME_RAMP_DURATION_MS / VOLUME_RAMP_INTERVAL_MS).toInt()
        val increment = 1f / steps

        val runnable = object : Runnable {
            override fun run() {
                if (!isIncreasingVolume || mediaPlayer == null) return

                currentVolume = (currentVolume + increment).coerceAtMost(targetVolume)
                mediaPlayer?.setVolume(currentVolume, currentVolume)

                if (currentVolume < targetVolume) {
                    volumeHandler?.postDelayed(this, VOLUME_RAMP_INTERVAL_MS)
                }
            }
        }

        volumeHandler?.postDelayed(runnable, VOLUME_RAMP_INTERVAL_MS)
    }

    private fun startVolumeRamp() {
        isIncreasingVolume = true
        currentVolume = 0.05f // Start at 5% volume so it's immediately audible
        targetVolume = 1f
        startVolumeRampFromCurrent()
    }

    /**
     * Stop playback and release resources.
     */
    fun stop() {
        isIncreasingVolume = false
        volumeHandler?.removeCallbacksAndMessages(null)
        volumeHandler = null

        audioFocusRequest?.let { 
            audioManager.abandonAudioFocusRequest(it) 
            audioFocusRequest = null
        }

        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
    }

    private fun getDefaultAlarmUri(): Uri {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }
}
