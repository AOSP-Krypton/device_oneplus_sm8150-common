/*
 * Copyright (C) 2016 The CyanogenMod project
 *               2017 The LineageOS Project
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

package com.krypton.settings.device.fragments;

import static android.provider.Settings.System.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK;
import static com.android.internal.lineage.hardware.LineageHardwareManager.FEATURE_TOUCHSCREEN_GESTURES;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.internal.lineage.hardware.LineageHardwareManager;
import com.android.internal.lineage.hardware.TouchscreenGesture;

import com.krypton.settings.device.R;
import com.krypton.settings.device.Utils;

public class GestureSettingsFragment extends PreferenceFragmentCompat {

    private Context mContext;
    private ContentResolver mResolver;
    private LineageHardwareManager mHardwareManager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mResolver = mContext.getContentResolver();
        mHardwareManager = LineageHardwareManager.getInstance(mContext);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.touchscreen_gesture_settings, rootKey);
        final PreferenceScreen screen = getPreferenceScreen();
        Preference hapticSwitch = screen.findPreference(TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK);
        if (hapticSwitch != null) {
            hapticSwitch.setOnPreferenceChangeListener((preference, newValue) ->
                Settings.System.putInt(mResolver, TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK, (Boolean) newValue ? 1 : 0));
        }
        if (mHardwareManager.isSupported(FEATURE_TOUCHSCREEN_GESTURES)) {
            for (TouchscreenGesture gesture: mHardwareManager.getTouchscreenGestures()) {
                screen.addPreference(new TouchscreenGesturePreference(gesture));
            }
        }
    }

    private final class TouchscreenGesturePreference extends ListPreference {

        @NonNull
        private final TouchscreenGesture mGesture;
        private final String key;

        TouchscreenGesturePreference(TouchscreenGesture gesture) {
            super(mContext);
            mGesture = gesture;
            key = Utils.getResName(mGesture.name);
            int action = Settings.System.getInt(mResolver, key, 0);
            setKey(key);
            setEntries(R.array.touchscreen_gesture_action_entries);
            setEntryValues(R.array.touchscreen_gesture_action_values);
            setDefaultValue(String.valueOf(action));
            setSummary("%s");
            setDialogTitle(R.string.touchscreen_gesture_action_dialog_title);
            Resources res = mContext.getResources();
            int resId = res.getIdentifier(key, "string", mContext.getPackageName());
            setTitle(resId <= 0 ? mGesture.name : res.getString(resId));
        }

        @Override
        public boolean callChangeListener(Object newValue) {
            int action = Integer.parseInt((String) newValue);
            if (!mHardwareManager.setTouchscreenGestureEnabled(mGesture, action > 0)) {
                return false;
            }
            return super.callChangeListener(newValue);
        }

        @Override
        protected boolean persistString(String value) {
            if (!super.persistString(value)) {
                return false;
            }
            Settings.System.putInt(mResolver, key, Integer.parseInt(value));
            return true;
        }
    }
}
