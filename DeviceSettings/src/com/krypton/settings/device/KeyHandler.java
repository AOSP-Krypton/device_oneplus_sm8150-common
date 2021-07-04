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

package com.krypton.settings.device;

import static com.android.internal.lineage.hardware.LineageHardwareManager.FEATURE_TOUCHSCREEN_GESTURES;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static android.media.AudioManager.ADJUST_LOWER;
import static android.media.AudioManager.ADJUST_RAISE;
import static android.media.AudioManager.RINGER_MODE_NORMAL;
import static android.media.AudioManager.RINGER_MODE_SILENT;
import static android.media.AudioManager.RINGER_MODE_VIBRATE;
import static android.media.AudioManager.STREAM_MUSIC;
import static android.Manifest.permission.STATUS_BAR_SERVICE;
import static android.os.PowerManager.WAKE_REASON_GESTURE;
import static android.os.UserHandle.CURRENT;
import static android.os.UserHandle.SYSTEM;
import static android.provider.Settings.System.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK;
import static android.view.KeyEvent.KEYCODE_MEDIA_NEXT;
import static android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
import static android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.android.internal.os.DeviceKeyHandler;
import com.android.internal.lineage.hardware.LineageHardwareManager;
import com.android.internal.lineage.hardware.TouchscreenGesture;
import com.android.internal.util.krypton.KryptonUtils;

import java.util.List;

@Keep
public class KeyHandler implements DeviceKeyHandler {

    private static final String TAG = "KeyHandler";
    private static final String GESTURE_WAKELOCK_TAG = "touchscreen-gesture-wakelock";
    private static final String GESTURE_WAKEUP_REASON = "touchscreen-gesture-wakeup";
    private static final int GESTURE_REQUEST = 1;
    private static final int GESTURE_WAKELOCK_DURATION = 1000;

    // AlertSlider key codes
    private static final int MODE_NORMAL = 601;
    private static final int MODE_VIBRATION = 602;
    private static final int MODE_SILENCE = 603;

    // Vibration effects
    private static final VibrationEffect MODE_NORMAL_EFFECT =
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK);
    private static final VibrationEffect MODE_VIBRATION_EFFECT =
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK);

    private final Context mContext;
    private final ContentResolver mResolver;
    private final AudioManager mAudioManager;
    private final PowerManager mPowerManager;
    private final EventHandler mHandler;
    private final Vibrator mVibrator;
    private final SparseArray<String> mSettingMap;

    public KeyHandler(Context context) {
        mContext = context;
        mResolver = mContext.getContentResolver();
        mHandler = new EventHandler();

        mAudioManager = mContext.getSystemService(AudioManager.class);
        mPowerManager = mContext.getSystemService(PowerManager.class);
        mVibrator = mContext.getSystemService(Vibrator.class);

        mSettingMap = new SparseArray<>();
        mapScanCode();
    }

    @Override
    public boolean handleKeyEvent(KeyEvent event) {
        int scanCode = event.getScanCode();
        int eventAction = event.getAction();

        // Handle AlertSlider KeyEvent
        if (eventAction == KeyEvent.ACTION_DOWN) {
            switch (scanCode) {
                case MODE_NORMAL:
                    mAudioManager.setRingerModeInternal(RINGER_MODE_NORMAL);
                    doHapticFeedback(MODE_NORMAL_EFFECT);
                    break;
                case MODE_VIBRATION:
                    mAudioManager.setRingerModeInternal(RINGER_MODE_VIBRATE);
                    doHapticFeedback(MODE_VIBRATION_EFFECT);
                    break;
                case MODE_SILENCE:
                    mAudioManager.setRingerModeInternal(RINGER_MODE_SILENT);
                    break;
                default:
                    return false;
            }
            sendSliderBroadcast(scanCode);
            return true;
        }

        // Return early so that KeyEvent can be processed sooner
        if (eventAction != KeyEvent.ACTION_UP) {
            return false;
        }

        String key = mSettingMap.get(scanCode);
        if (key == null) {
            return false;
        }
        // Handle gestures
        int action = Settings.System.getInt(mResolver, key, 0);
        if (action != 0 && !mHandler.hasMessages(GESTURE_REQUEST)) {
            mHandler.sendMessage(mHandler.obtainMessage(GESTURE_REQUEST, action, 0));
        }
        return true;
    }

    private void mapScanCode() {
        final LineageHardwareManager manager = LineageHardwareManager.getInstance(mContext);
        if (manager.isSupported(FEATURE_TOUCHSCREEN_GESTURES)) {
            for (TouchscreenGesture gesture: manager.getTouchscreenGestures()) {
                mSettingMap.put(gesture.keycode, Utils.getResName(gesture.name));
            }
        }
    }

    private void launchCamera() {
        wakeUp();
        mContext.sendBroadcastAsUser(new Intent(Intent.ACTION_SCREEN_CAMERA_GESTURE),
            CURRENT, STATUS_BAR_SERVICE);
    }

    private void launchBrowser() {
        startActivitySafely(getLaunchableIntent(
            new Intent(Intent.ACTION_VIEW, Uri.parse("http:"))));
    }

    private void launchDialer() {
        startActivitySafely(new Intent(Intent.ACTION_DIAL, null));
    }

    private void launchEmail() {
        startActivitySafely(getLaunchableIntent(
            new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"))));
    }

    private void launchMessages() {
        startActivitySafely(getLaunchableIntent(
                new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))));
    }

    private void toggleFlashlight() {
        KryptonUtils.toggleCameraFlash();
    }

    private void playPauseMusic() {
        dispatchMediaKeyToMediaSession(KEYCODE_MEDIA_PLAY_PAUSE);
    }

    private void previousTrack() {
        dispatchMediaKeyToMediaSession(KEYCODE_MEDIA_PREVIOUS);
    }

    private void nextTrack() {
        dispatchMediaKeyToMediaSession(KEYCODE_MEDIA_NEXT);
    }

    private void volumeDown() {
        mAudioManager.adjustStreamVolume(STREAM_MUSIC, ADJUST_LOWER, 0);
    }

    private void volumeUp() {
        mAudioManager.adjustStreamVolume(STREAM_MUSIC, ADJUST_RAISE, 0);
    }

    private void wakeUp() {
         mPowerManager.wakeUp(SystemClock.uptimeMillis(), WAKE_REASON_GESTURE, GESTURE_WAKEUP_REASON);
    }

    private void dispatchMediaKeyToMediaSession(int keycode) {
        MediaSessionLegacyHelper helper = MediaSessionLegacyHelper.getHelper(mContext);
        if (helper == null) {
            Log.w(TAG, "Unable to send media key event");
            return;
        }
        KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keycode, 0);
        helper.sendMediaButtonEvent(event, true);
        helper.sendMediaButtonEvent(KeyEvent.changeAction(event, KeyEvent.ACTION_UP), true);
    }

    private void startActivitySafely(Intent intent) {
        if (intent == null) {
            Log.w(TAG, "No intent passed to startActivitySafely");
            return;
        }
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_SINGLE_TOP |
            FLAG_ACTIVITY_CLEAR_TOP);
        try {
            mContext.startActivityAsUser(intent, null, CURRENT);
            wakeUp();
        } catch (ActivityNotFoundException e) {
            // Ignore
        }
    }

    private void doHapticFeedback() {
        if (Settings.System.getInt(mResolver, TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK, 1) != 1) {
            return;
        }
        if (mAudioManager.getRingerMode() != RINGER_MODE_SILENT) {
            doHapticFeedback(MODE_NORMAL_EFFECT);
        }
    }

    private void doHapticFeedback(VibrationEffect effect) {
        if (mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.vibrate(effect);
        }
    }

    private void sendSliderBroadcast(int code) {
        final Intent intent = new Intent(Intent.ACTION_SLIDER_POSITION_CHANGED);
        intent.putExtra(Intent.EXTRA_SLIDER_POSITION, code - MODE_NORMAL);
        mContext.sendBroadcastAsUser(intent, SYSTEM);
    }

    private Intent getLaunchableIntent(Intent intent) {
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> resInfo = pm.queryIntentActivities(intent, 0);
        if (resInfo.isEmpty()) {
            return null;
        }
        return pm.getLaunchIntentForPackage(resInfo.get(0).activityInfo.packageName);
    }

    private final class EventHandler extends Handler {

        EventHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what != GESTURE_REQUEST) {
                return;
            }
            switch (msg.arg1) {
                case Constants.ACTION_CAMERA:
                    launchCamera();
                    break;
                case Constants.ACTION_FLASHLIGHT:
                    toggleFlashlight();
                    break;
                case Constants.ACTION_BROWSER:
                    launchBrowser();
                    break;
                case Constants.ACTION_DIALER:
                    launchDialer();
                    break;
                case Constants.ACTION_EMAIL:
                    launchEmail();
                    break;
                case Constants.ACTION_MESSAGES:
                    launchMessages();
                    break;
                case Constants.ACTION_PLAY_PAUSE_MUSIC:
                    playPauseMusic();
                    break;
                case Constants.ACTION_PREVIOUS_TRACK:
                    previousTrack();
                    break;
                case Constants.ACTION_NEXT_TRACK:
                    nextTrack();
                    break;
                case Constants.ACTION_VOLUME_DOWN:
                    volumeDown();
                    break;
                case Constants.ACTION_VOLUME_UP:
                    volumeUp();
                    break;
                case Constants.ACTION_WAKEUP:
                    wakeUp();
            }
            doHapticFeedback();
        }
    }
}
