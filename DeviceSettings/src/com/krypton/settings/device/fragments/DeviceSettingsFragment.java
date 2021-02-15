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

import static android.provider.Settings.System.CUSTOM_REFRESH_RATE_MODE;
import static android.provider.Settings.System.PEAK_REFRESH_RATE;
import static android.provider.Settings.System.MIN_REFRESH_RATE;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.krypton.settings.device.R;
import com.krypton.settings.device.Utils;

public class DeviceSettingsFragment extends PreferenceFragmentCompat {

    private static final String KEY_FORCE_90 = "force_90_preference";
    private static final String KEY_CUSTOM_MODE = "custom_refresh_rate_preference";

    private Context mContext;
    private ContentResolver mResolver;
    private Preference mCustomModePreference;

    @Override
    public void onCreatePreferences(Bundle bundle, String key) {
        setPreferencesFromResource(R.xml.device_settings, key);
        mContext = getContext();
        mResolver = mContext.getContentResolver();
        mCustomModePreference = findPreference(KEY_CUSTOM_MODE);
        mCustomModePreference.setEnabled(Settings.System.getInt(mResolver, CUSTOM_REFRESH_RATE_MODE, 0) == 1);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        if (key != null) {
            if (key.equals(KEY_FORCE_90)) {
                boolean state = Utils.isChecked(preference);
                int rate = state == true ? 90 : 60;
                setRefreshRate(rate);
                setCustomRefreshMode(state);
                mCustomModePreference.setEnabled(state);
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void setRefreshRate(int rate) {
        Settings.System.putInt(mResolver, PEAK_REFRESH_RATE, rate);
        Settings.System.putInt(mResolver, MIN_REFRESH_RATE, rate);
    }

    private void setCustomRefreshMode(boolean state) {
        Settings.System.putInt(mResolver, CUSTOM_REFRESH_RATE_MODE, state ? 1 : 0);
    }
}
