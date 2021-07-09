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

package com.krypton.settings.device;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

public class Utils {

    private static final String TOUCHSCREEN_GESTURE_FORMAT = "touchscreen_gesture_%s_title";

    public static boolean isChecked(Preference preference) {
        boolean checked = false;
        if (preference instanceof SwitchPreference) {
            checked = ((SwitchPreference) preference).isChecked();
        } else if (preference instanceof CheckBoxPreference) {
            checked = ((CheckBoxPreference) preference).isChecked();
        }
        return checked;
    }

    public static String getResName(String name) {
        return String.format(TOUCHSCREEN_GESTURE_FORMAT, name.toLowerCase().replace(" ", "_"));
    }

    public static String getStringFromSettings(Context context, String key) {
        return Settings.System.getString(context.getContentResolver(), key);
    }

    public static void putStringInSettings(Context context, String key, String value) {
        Settings.System.putString(context.getContentResolver(), key, value);
    }
}
