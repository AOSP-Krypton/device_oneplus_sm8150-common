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
import static android.provider.Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
import static android.provider.Settings.Global.ZEN_MODE_NO_INTERRUPTIONS;
import static android.provider.Settings.Global.ZEN_MODE_OFF;
import static android.provider.Settings.System.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK;
import static android.provider.Settings.System.ALERTSLIDER_MODE_POSITION_BOTTOM;
import static android.provider.Settings.System.ALERTSLIDER_MODE_POSITION_MIDDLE;
import static android.provider.Settings.System.ALERTSLIDER_MODE_POSITION_TOP;
import static android.view.KeyEvent.KEYCODE_MEDIA_NEXT;
import static android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
import static android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS;
import static com.android.internal.lineage.hardware.LineageHardwareManager.FEATURE_TOUCHSCREEN_GESTURES;

import android.app.KeyguardManager;
import android.app.NotificationManager;
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

import com.android.internal.lineage.hardware.LineageHardwareManager;
import com.android.internal.lineage.hardware.TouchscreenGesture;
import com.android.internal.os.DeviceKeyHandler;
import com.android.internal.R;
import com.android.internal.util.krypton.KryptonUtils;

import java.util.List;

@Keep
public class KeyHandler implements DeviceKeyHandler {
    private static final String TAG = "KeyHandler";
    private static final String PULSE_ACTION = "com.android.systemui.doze.pulse";
    private static final String GESTURE_WAKEUP_REASON = "touchscreen-gesture-wakeup";
    private static final int GESTURE_REQUEST = 1;

    // AlertSlider KeyEvent scanCodes
    private static final int POSITION_BOTTOM = 601;
    private static final int POSITION_MIDDLE = 602;
    private static final int POSITION_TOP = 603;

    // Single tap gesture action
    private static final String SINGLE_TAP_GESTURE = "Single tap";

    // Vibration effects
    private static final VibrationEffect HEAVY_CLICK_EFFECT =
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK);
    private static final VibrationEffect DOUBLE_CLICK_EFFECT =
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK);

    // Supported modes for AlertSlider positions.
    private final String MODE_NORMAL;
    private final String MODE_PRIORITY;
    private final String MODE_VIBRATE;
    private final String MODE_SILENT;
    private final String MODE_DND;

    private final Context mContext;
    private final ContentResolver mResolver;
    private final AudioManager mAudioManager;
    private final NotificationManager mNotificationManager;
    private final PowerManager mPowerManager;
    private final EventHandler mHandler;
    private final Vibrator mVibrator;
    private final SparseArray<String> mSettingMap;
    private KeyguardManager mKeyguardManager;

    public KeyHandler(Context context) {
        mContext = context;
        mResolver = mContext.getContentResolver();
        mHandler = new EventHandler();

        mAudioManager = mContext.getSystemService(AudioManager.class);
        mNotificationManager = mContext.getSystemService(NotificationManager.class);
        mPowerManager = mContext.getSystemService(PowerManager.class);
        mVibrator = mContext.getSystemService(Vibrator.class);

        mSettingMap = new SparseArray<>();
        mapScanCode();

        MODE_NORMAL = mContext.getString(R.string.alert_slider_mode_normal);
        MODE_PRIORITY = mContext.getString(R.string.alert_slider_mode_priority);
        MODE_VIBRATE = mContext.getString(R.string.alert_slider_mode_vibrate);
        MODE_SILENT = mContext.getString(R.string.alert_slider_mode_silent);
        MODE_DND = mContext.getString(R.string.alert_slider_mode_dnd);
    }

    @Override
    public boolean handleKeyEvent(KeyEvent event) {
        int scanCode = event.getScanCode();
        int eventAction = event.getAction();

        // Handle AlertSlider KeyEvent
        if (eventAction == KeyEvent.ACTION_DOWN) {
            String mode = null;
            switch (scanCode) {
                case POSITION_BOTTOM:
                    mode = performSliderAction(ALERTSLIDER_MODE_POSITION_BOTTOM, MODE_NORMAL);
                    break;
                case POSITION_MIDDLE:
                    mode = performSliderAction(ALERTSLIDER_MODE_POSITION_MIDDLE, MODE_VIBRATE);
                    break;
                case POSITION_TOP:
                    mode = performSliderAction(ALERTSLIDER_MODE_POSITION_TOP, MODE_SILENT);
                    break;
                default:
                    return false;
            }
            sendSliderBroadcast(scanCode, mode);
            return true;
        }

        // Return early so that KeyEvent can be processed sooner
        if (eventAction != KeyEvent.ACTION_UP) {
            return false;
        }

        String key = mSettingMap.get(scanCode);
        if (key == null) {
            return false;
        } else if (key.equals(Utils.getResName(SINGLE_TAP_GESTURE))) {
            getKeyguardManagerService();
            if (mKeyguardManager != null && !mKeyguardManager.isDeviceLocked()) {
                // Wakeup the device if not locked
                wakeUp();
                return true;
            }
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

    private String performSliderAction(String key, String defMode) {
        String mode = Settings.System.getString(mResolver, key);
        if (mode == null) {
            mode = defMode;
        }
        if (mode.equals(MODE_NORMAL)) {
            mAudioManager.setRingerModeInternal(RINGER_MODE_NORMAL);
            mNotificationManager.setZenMode(ZEN_MODE_OFF, null, TAG);
            doHapticFeedback(HEAVY_CLICK_EFFECT);
        } else if (mode.equals(MODE_PRIORITY)) {
            mAudioManager.setRingerModeInternal(RINGER_MODE_NORMAL);
            mNotificationManager.setZenMode(ZEN_MODE_IMPORTANT_INTERRUPTIONS, null, TAG);
            doHapticFeedback(HEAVY_CLICK_EFFECT);
        } else if (mode.equals(MODE_VIBRATE)) {
            mAudioManager.setRingerModeInternal(RINGER_MODE_VIBRATE);
            mNotificationManager.setZenMode(ZEN_MODE_OFF, null, TAG);
            doHapticFeedback(DOUBLE_CLICK_EFFECT);
        } else if (mode.equals(MODE_SILENT)) {
            mAudioManager.setRingerModeInternal(RINGER_MODE_SILENT);
            mNotificationManager.setZenMode(ZEN_MODE_OFF, null, TAG);
        } else if (mode.equals(MODE_DND)) {
            mAudioManager.setRingerModeInternal(RINGER_MODE_NORMAL);
            mNotificationManager.setZenMode(ZEN_MODE_NO_INTERRUPTIONS, null, TAG);
        }
        return mode;
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

    private void launchDozePulse() {
        final boolean dozeEnabled = Settings.Secure.getInt(mResolver,
                Settings.Secure.DOZE_ENABLED, 1) != 0;
        if (dozeEnabled) {
            mContext.sendBroadcastAsUser(new Intent(PULSE_ACTION), CURRENT);
        }
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
            doHapticFeedback(HEAVY_CLICK_EFFECT);
        }
    }

    private void doHapticFeedback(VibrationEffect effect) {
        if (mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.vibrate(effect);
        }
    }

    private void sendSliderBroadcast(int code, String mode) {
        final Intent intent = new Intent(Intent.ACTION_SLIDER_POSITION_CHANGED);
        intent.putExtra(Intent.EXTRA_SLIDER_POSITION, code - POSITION_BOTTOM);
        intent.putExtra(Intent.EXTRA_SLIDER_MODE, mode);
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

    private void getKeyguardManagerService() {
        if (mKeyguardManager != null) {
            return;
        }
        mKeyguardManager = mContext.getSystemService(KeyguardManager.class);
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
                case Constants.ACTION_AMBIENT_DISPLAY:
                    launchDozePulse();
            }
            if (msg.arg1 != Constants.ACTION_AMBIENT_DISPLAY) {
                doHapticFeedback();
            }
        }
    }
}
