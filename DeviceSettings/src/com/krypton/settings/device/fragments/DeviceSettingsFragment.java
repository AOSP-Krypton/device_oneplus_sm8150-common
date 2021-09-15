/*
 * Copyright (C) 2021 AOSP-Krypton Project
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

import static android.provider.Settings.System.ALERTSLIDER_MODE_POSITION_BOTTOM;
import static android.provider.Settings.System.ALERTSLIDER_MODE_POSITION_MIDDLE;
import static android.provider.Settings.System.ALERTSLIDER_MODE_POSITION_TOP;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragmentCompat;

import com.android.internal.R;

public class DeviceSettingsFragment extends PreferenceFragmentCompat
        implements OnPreferenceChangeListener {
    private static final String KEY_ALERT_SLIDER_BOTTOM = "alert_slider_bottom_preference";
    private static final String KEY_ALERT_SLIDER_MIDDLE = "alert_slider_middle_preference";
    private static final String KEY_ALERT_SLIDER_TOP = "alert_slider_top_preference";
    private Context mContext;

    @Override
    public void onCreatePreferences(Bundle bundle, String key) {
        setPreferencesFromResource(com.krypton.settings.device.R.xml.device_settings, key);
        mContext = getContext();

        final String[] entryValues = new String[] {
            mContext.getString(R.string.alert_slider_mode_normal),
            mContext.getString(R.string.alert_slider_mode_priority),
            mContext.getString(R.string.alert_slider_mode_vibrate),
            mContext.getString(R.string.alert_slider_mode_silent),
            mContext.getString(R.string.alert_slider_mode_dnd)
        };
        final ContentResolver contentResolver = mContext.getContentResolver();

        ListPreference alertSliderBottomPref = findPreference(KEY_ALERT_SLIDER_BOTTOM);
        alertSliderBottomPref.setEntryValues(entryValues);
        String value = Settings.System.getString(contentResolver, ALERTSLIDER_MODE_POSITION_BOTTOM);
        alertSliderBottomPref.setValue(value != null ? value : entryValues[0]);
        alertSliderBottomPref.setOnPreferenceChangeListener(this);

        ListPreference alertSliderMiddlePref = findPreference(KEY_ALERT_SLIDER_MIDDLE);
        alertSliderMiddlePref.setEntryValues(entryValues);
        value = Settings.System.getString(contentResolver, ALERTSLIDER_MODE_POSITION_MIDDLE);
        alertSliderMiddlePref.setValue(value != null ? value : entryValues[2]);
        alertSliderMiddlePref.setOnPreferenceChangeListener(this);

        ListPreference alertSliderTopPref = findPreference(KEY_ALERT_SLIDER_TOP);
        alertSliderTopPref.setEntryValues(entryValues);
        value = Settings.System.getString(contentResolver, ALERTSLIDER_MODE_POSITION_TOP);
        alertSliderTopPref.setValue(value != null ? value : entryValues[3]);
        alertSliderTopPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case KEY_ALERT_SLIDER_BOTTOM:
                return putString(ALERTSLIDER_MODE_POSITION_BOTTOM, newValue);
            case KEY_ALERT_SLIDER_MIDDLE:
                return putString(ALERTSLIDER_MODE_POSITION_MIDDLE, newValue);
            case KEY_ALERT_SLIDER_TOP:
                return putString(ALERTSLIDER_MODE_POSITION_TOP, newValue);
            default:
                return false;
        }
    }

    private boolean putString(String key, Object value) {
        return Settings.System.putString(mContext.getContentResolver(), key, (String) value);
    }
}
