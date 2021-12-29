/**
 * Copyright (C) 2016 The CyanogenMod project
 *               2017-2018 The LineageOS Project
 *               2021 AOSP-Krypton Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.krypton.settings.device

import android.Manifest.permission.STATUS_BAR_SERVICE
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioSystem
import android.media.session.MediaSessionLegacyHelper
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.UserHandle.CURRENT
import android.os.UserHandle.SYSTEM
import android.os.Vibrator
import android.os.VibrationEffect
import android.provider.Settings
import android.provider.Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS
import android.provider.Settings.Global.ZEN_MODE_NO_INTERRUPTIONS
import android.provider.Settings.Global.ZEN_MODE_OFF
import android.provider.Settings.System.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK
import android.provider.Settings.System.ALERTSLIDER_MODE_POSITION_BOTTOM
import android.provider.Settings.System.ALERTSLIDER_MODE_POSITION_MIDDLE
import android.provider.Settings.System.ALERTSLIDER_MODE_POSITION_TOP
import android.util.Log
import android.util.SparseArray
import android.view.KeyEvent

import androidx.annotation.Keep

import com.android.internal.lineage.hardware.LineageHardwareManager
import com.android.internal.lineage.hardware.LineageHardwareManager.FEATURE_TOUCHSCREEN_GESTURES
import com.android.internal.lineage.hardware.TouchscreenGesture
import com.android.internal.os.DeviceKeyHandler
import com.android.internal.R
import com.android.internal.util.krypton.KryptonUtils

@Keep
class KeyHandler(private val context: Context): DeviceKeyHandler {
    // Supported modes for AlertSlider positions.
    private val MODE_NORMAL = context.getString(R.string.alert_slider_mode_normal)
    private val MODE_PRIORITY = context.getString(R.string.alert_slider_mode_priority)
    private val MODE_VIBRATE = context.getString(R.string.alert_slider_mode_vibrate)
    private val MODE_SILENT = context.getString(R.string.alert_slider_mode_silent)
    private val MODE_DND = context.getString(R.string.alert_slider_mode_dnd)

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val handler = EventHandler()
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private val packageManager = context.getSystemService(PackageManager::class.java)
    private val settingMap = SparseArray<String>()
    private var keyguardManager: KeyguardManager? = null

    private var wasMuted = false
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val stream = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1)
            val state = intent.getBooleanExtra(AudioManager.EXTRA_STREAM_VOLUME_MUTED, false)
            if (stream == AudioSystem.STREAM_MUSIC && state == false) {
                wasMuted = false
            }
        }
    }

    init {
        val manager = LineageHardwareManager.getInstance(context)
        if (manager.isSupported(FEATURE_TOUCHSCREEN_GESTURES)) {
            manager.touchscreenGestures.forEach { gesture: TouchscreenGesture ->
                settingMap.put(gesture.keycode, Utils.Companion.getResName(gesture.name))
            }
        }
        context.registerReceiver(
            broadcastReceiver,
            IntentFilter(AudioManager.STREAM_MUTE_CHANGED_ACTION)
        )
    }

    override fun handleKeyEvent(event: KeyEvent): Boolean {
        // Handle AlertSlider KeyEvent
        if (event.action == KeyEvent.ACTION_DOWN) {
            var mode: String?
            when (event.scanCode) {
                POSITION_BOTTOM ->
                    mode = performSliderAction(ALERTSLIDER_MODE_POSITION_BOTTOM, MODE_NORMAL)
                POSITION_MIDDLE ->
                    mode = performSliderAction(ALERTSLIDER_MODE_POSITION_MIDDLE, MODE_VIBRATE)
                POSITION_TOP ->
                    mode = performSliderAction(ALERTSLIDER_MODE_POSITION_TOP, MODE_SILENT)
                else -> return false
            }
            sendSliderBroadcast(event.scanCode, mode)
            return true
        }

        // Return early so that KeyEvent can be processed sooner
        if (event.action != KeyEvent.ACTION_UP) {
            return false
        }

        val key: String? = settingMap.get(event.scanCode)
        if (key == null) {
            return false
        } else if (key == Utils.getResName(SINGLE_TAP_GESTURE)) {
            if (getKeyguardManagerService()?.isDeviceLocked() == false) {
                // Wakeup the device if not locked
                wakeUp()
                return true
            }
        }
        // Handle gestures
        val action = Settings.System.getInt(context.contentResolver, key, 0)
        if (action > 0 && !handler.hasMessages(GESTURE_REQUEST)) {
            handler.sendMessage(handler.obtainMessage(GESTURE_REQUEST, action, 0))
        }
        return true
    }

    private fun performSliderAction(key: String, defMode: String): String {
        val mode = Settings.System.getString(context.contentResolver, key) ?: defMode
        val muteMedia = Settings.System.getInt(context.contentResolver, 
            MUTE_MEDIA_WITH_SILENT, 0) == 1
        when (mode) {
            MODE_NORMAL -> {
                audioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
                notificationManager.setZenMode(ZEN_MODE_OFF, null, TAG)
                doHapticFeedback(HEAVY_CLICK_EFFECT)
                if (muteMedia && wasMuted) {
                    audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, 0)
                }
            }
            MODE_PRIORITY -> {
                audioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
                notificationManager.setZenMode(ZEN_MODE_IMPORTANT_INTERRUPTIONS, null, TAG)
                doHapticFeedback(HEAVY_CLICK_EFFECT)
                if (muteMedia && wasMuted) {
                    audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, 0)
                }
            }
            MODE_VIBRATE -> {
                audioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE)
                notificationManager.setZenMode(ZEN_MODE_OFF, null, TAG)
                doHapticFeedback(DOUBLE_CLICK_EFFECT)
                if (muteMedia && wasMuted) {
                    audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, 0)
                }
            }
            MODE_SILENT -> {
                audioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT)
                notificationManager.setZenMode(ZEN_MODE_OFF, null, TAG)
                if (muteMedia) {
                    audioManager.adjustVolume(AudioManager.ADJUST_MUTE, 0)
                    wasMuted = true
                }
            }
            MODE_DND -> {
                audioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
                notificationManager.setZenMode(ZEN_MODE_NO_INTERRUPTIONS, null, TAG)
            }
        }
        return mode
    }

    private fun launchCamera() {
        wakeUp()
        context.sendBroadcastAsUser(Intent(Intent.ACTION_SCREEN_CAMERA_GESTURE),
            CURRENT, STATUS_BAR_SERVICE)
    }

    private fun launchBrowser() {
        startActivitySafely(getLaunchableIntent(
            Intent(Intent.ACTION_VIEW, Uri.parse("http:"))))
    }

    private fun launchDialer() {
        startActivitySafely(Intent(Intent.ACTION_DIAL, null))
    }

    private fun launchEmail() {
        startActivitySafely(getLaunchableIntent(
            Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"))))
    }

    private fun launchMessages() {
        startActivitySafely(getLaunchableIntent(
                Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))))
    }

    private fun toggleFlashlight() {
        KryptonUtils.toggleCameraFlash()
    }

    private fun playPauseMusic() {
        dispatchMediaKeyToMediaSession(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    private fun previousTrack() {
        dispatchMediaKeyToMediaSession(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    private fun nextTrack() {
        dispatchMediaKeyToMediaSession(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    private fun volumeDown() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
    }

    private fun volumeUp() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
    }

    private fun wakeUp() {
        powerManager.wakeUp(SystemClock.uptimeMillis(), PowerManager.WAKE_REASON_GESTURE, GESTURE_WAKEUP_REASON)
    }

    private fun launchDozePulse() {
        val dozeEnabled = Settings.Secure.getInt(context.contentResolver,
                Settings.Secure.DOZE_ENABLED, 1) == 1
        if (dozeEnabled) {
            context.sendBroadcastAsUser(Intent(PULSE_ACTION), CURRENT)
        }
    }

    private fun dispatchMediaKeyToMediaSession(keycode: Int) {
        val helper: MediaSessionLegacyHelper? = MediaSessionLegacyHelper.getHelper(context)
        if (helper == null) {
            Log.w(TAG, "Unable to send media key event")
            return
        }
        val event = KeyEvent(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keycode, 0)
        helper.sendMediaButtonEvent(event, true)
        helper.sendMediaButtonEvent(KeyEvent.changeAction(event, KeyEvent.ACTION_UP), true)
    }

    private fun startActivitySafely(intent: Intent?) {
        if (intent == null) {
            Log.w(TAG, "No intent passed to startActivitySafely")
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_SINGLE_TOP or
            Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            context.startActivityAsUser(intent, null, CURRENT)
            wakeUp()
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Activity not found to launch")
        }
    }

    private fun doHapticFeedback() {
        val hapticFeedbackEnabled = Settings.System.getInt(context.contentResolver,
            TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK, 1) == 1
        if (hapticFeedbackEnabled && audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT) {
            doHapticFeedback(HEAVY_CLICK_EFFECT)
        }
    }

    private fun doHapticFeedback(effect: VibrationEffect) {
        if (vibrator.hasVibrator()) vibrator.vibrate(effect)
    }

    private fun sendSliderBroadcast(code: Int, mode: String) {
        context.sendBroadcastAsUser(Intent(Intent.ACTION_SLIDER_POSITION_CHANGED).apply {
            putExtra(Intent.EXTRA_SLIDER_POSITION, code - POSITION_BOTTOM)
            putExtra(Intent.EXTRA_SLIDER_MODE, mode)
        }, SYSTEM)
    }

    private fun getLaunchableIntent(intent: Intent): Intent? {
        val resInfo = packageManager.queryIntentActivities(intent, 0)
        if (resInfo.isEmpty()) {
            return null
        }
        return packageManager.getLaunchIntentForPackage(resInfo[0].activityInfo.packageName)
    }

    private fun getKeyguardManagerService(): KeyguardManager? {
        if (keyguardManager == null) {
            keyguardManager = context.getSystemService(KeyguardManager::class.java)
        }
        return keyguardManager
    }

    private inner class EventHandler: Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what != GESTURE_REQUEST) return
            when (msg.arg1) {
                ACTION_CAMERA -> launchCamera()
                ACTION_FLASHLIGHT -> toggleFlashlight()
                ACTION_BROWSER -> launchBrowser()
                ACTION_DIALER -> launchDialer()
                ACTION_EMAIL -> launchEmail()
                ACTION_MESSAGES -> launchMessages()
                ACTION_PLAY_PAUSE_MUSIC -> playPauseMusic()
                ACTION_PREVIOUS_TRACK -> previousTrack()
                ACTION_NEXT_TRACK -> nextTrack()
                ACTION_VOLUME_DOWN -> volumeDown()
                ACTION_VOLUME_UP -> volumeUp()
                ACTION_WAKEUP -> wakeUp()
                ACTION_AMBIENT_DISPLAY -> launchDozePulse()
            }
            if (msg.arg1 !=  ACTION_AMBIENT_DISPLAY) {
                doHapticFeedback()
            }
        }
    }

    companion object {
        private const val TAG = "KeyHandler"
        private const val PULSE_ACTION = "com.android.systemui.doze.pulse"
        private const val GESTURE_WAKEUP_REASON = "touchscreen-gesture-wakeup"
        private const val GESTURE_REQUEST = 1

        // AlertSlider KeyEvent scanCodes
        private const val POSITION_BOTTOM = 601
        private const val POSITION_MIDDLE = 602
        private const val POSITION_TOP = 603

        // Single tap gesture action
        private const val SINGLE_TAP_GESTURE = "Single tap"

        // Vibration effects
        private val HEAVY_CLICK_EFFECT =
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        private val DOUBLE_CLICK_EFFECT =
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)

        // Touch Gestures
        private const val ACTION_FLASHLIGHT = 1
        private const val ACTION_CAMERA = 2
        private const val ACTION_BROWSER = 3
        private const val ACTION_DIALER = 4
        private const val ACTION_EMAIL = 5
        private const val ACTION_MESSAGES = 6
        private const val ACTION_PLAY_PAUSE_MUSIC = 7
        private const val ACTION_PREVIOUS_TRACK = 8
        private const val ACTION_NEXT_TRACK = 9
        private const val ACTION_VOLUME_DOWN = 10
        private const val ACTION_VOLUME_UP = 11
        private const val ACTION_WAKEUP = 12
        private const val ACTION_AMBIENT_DISPLAY = 13
        private const val MUTE_MEDIA_WITH_SILENT = "config_mute_media"
    }
}
