/*
 * Copyright (C) 2021-2022 AOSP-Krypton Project
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

package com.krypton.settings.device.fragments

import android.content.Context
import android.os.Bundle
import android.os.SystemProperties
import android.os.VibrationEffect
import android.os.Vibrator

import androidx.preference.PreferenceFragmentCompat

import com.android.internal.R
import com.android.internal.util.krypton.FileUtils
import com.krypton.settings.preference.CustomSeekBarPreference

class DeviceSettingsFragment : PreferenceFragmentCompat() {

    private lateinit var vibrator: Vibrator

    override fun onAttach(context: Context) {
        super.onAttach(context)
        vibrator = context.getSystemService(Vibrator::class.java)
    }

    override fun onCreatePreferences(bundle: Bundle?, key: String?) {
        setPreferencesFromResource(com.krypton.settings.device.R.xml.device_settings, key)

        val device: String = SystemProperties.get(PROP_KRYPTON_DEVICE, null)
        if (device == GUACAMOLEB) {
            preferenceScreen.removePreferenceRecursively(KEY_VIBRATOR_CATEGORY)
        }
        if (device == GUACAMOLEB || device == HOTDOGB) {
            preferenceScreen.removePreferenceRecursively(KEY_CAMERA_CATEGORY)
        }

        findPreference<CustomSeekBarPreference>(KEY_VIBRATOR_PREFERENCE)
            ?.setOnPreferenceChangeListener { _, newValue ->
                if (vibrator.hasVibrator()) {
                    vibrator.vibrate(HEAVY_CLICK_EFFECT)
                }
                FileUtils.writeLine(FILE_LEVEL, (newValue as Int).toString())
            }
    }

    companion object {
        private const val KEY_VIBRATOR_CATEGORY = "vibrator"
        private const val PROP_KRYPTON_DEVICE = "ro.krypton.build.device"
        private const val GUACAMOLEB = "guacamoleb"
        private const val HOTDOGB = "hotdogb"

        private const val KEY_VIBRATOR_PREFERENCE = "device_setting_vib_strength"
        private const val FILE_LEVEL = "/sys/devices/platform/soc/89c000.i2c/i2c-2/2-005a/leds/vibrator/level"

        private val HEAVY_CLICK_EFFECT = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
    
        private const val KEY_CAMERA_CATEGORY = "camera";
    }
}
