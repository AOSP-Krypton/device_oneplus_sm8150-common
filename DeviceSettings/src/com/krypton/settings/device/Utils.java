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

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

public class Utils {

    public static boolean isChecked(Preference preference) {
        boolean checked = false;
        if (preference instanceof SwitchPreference) {
            checked = ((SwitchPreference) preference).isChecked();
        } else if (preference instanceof SwitchPreference) {
            checked = ((CheckBoxPreference) preference).isChecked();
        }
        return checked;
    }

}
