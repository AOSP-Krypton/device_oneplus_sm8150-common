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

package com.krypton.settings.device.fragments

import android.content.Context
import android.os.Bundle
import android.os.SystemProperties
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.provider.Settings.System.ALERTSLIDER_MODE_POSITION_BOTTOM
import android.provider.Settings.System.ALERTSLIDER_MODE_POSITION_MIDDLE
import android.provider.Settings.System.ALERTSLIDER_MODE_POSITION_TOP

import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceFragmentCompat

import com.android.internal.R
import com.android.internal.util.krypton.FileUtils
import com.krypton.settings.preference.CustomSeekBarPreference

class DeviceSettingsFragment: PreferenceFragmentCompat(), OnPreferenceChangeListener {

    private lateinit var vibrator: Vibrator

    override fun onAttach(context: Context) {
        super.onAttach(context)
        vibrator = context.getSystemService(Vibrator::class.java)
    }

    override fun onCreatePreferences(bundle: Bundle?, key: String?) {
        setPreferencesFromResource(com.krypton.settings.device.R.xml.device_settings, key)

        val entryValues = arrayOf(
            getString(R.string.alert_slider_mode_normal).toString(),
            getString(R.string.alert_slider_mode_priority).toString(),
            getString(R.string.alert_slider_mode_vibrate).toString(),
            getString(R.string.alert_slider_mode_silent).toString(),
            getString(R.string.alert_slider_mode_dnd).toString()
        )

        findPreference<ListPreference>(KEY_ALERT_SLIDER_BOTTOM)?.also {
            it.setEntryValues(entryValues)
            val value = Settings.System.getString(context?.contentResolver, ALERTSLIDER_MODE_POSITION_BOTTOM)
            it.value = value ?: entryValues[0]
            it.setOnPreferenceChangeListener(this)
        }

        findPreference<ListPreference>(KEY_ALERT_SLIDER_MIDDLE)?.also {
            it.setEntryValues(entryValues)
            val value = Settings.System.getString(context?.contentResolver, ALERTSLIDER_MODE_POSITION_MIDDLE)
            it.value = value ?: entryValues[2]
            it.setOnPreferenceChangeListener(this)
        }

        findPreference<ListPreference>(KEY_ALERT_SLIDER_TOP)?.also {
            it.setEntryValues(entryValues)
            val value = Settings.System.getString(context?.contentResolver, ALERTSLIDER_MODE_POSITION_TOP)
            it.value = value ?: entryValues[3]
            it.setOnPreferenceChangeListener(this)
        }

        val device: String = SystemProperties.get(PROP_KRYPTON_DEVICE, "")
        if (device == GUACAMOLEB)
            preferenceScreen.removePreferenceRecursively(KEY_VIBRATOR_CATEGORY)

        findPreference<CustomSeekBarPreference>(KEY_VIBRATOR_PREFERENCE)
            ?.setOnPreferenceChangeListener { _, newValue ->
                if (vibrator.hasVibrator())
                    vibrator.vibrate(HEAVY_CLICK_EFFECT)
                FileUtils.writeLine(FILE_LEVEL, (newValue as Int).toString())
            }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean =
        when (preference.key) {
            KEY_ALERT_SLIDER_BOTTOM -> putString(ALERTSLIDER_MODE_POSITION_BOTTOM, newValue)
            KEY_ALERT_SLIDER_MIDDLE -> putString(ALERTSLIDER_MODE_POSITION_MIDDLE, newValue)
            KEY_ALERT_SLIDER_TOP -> putString(ALERTSLIDER_MODE_POSITION_TOP, newValue)
            KEY_MUTE_MEDIA_WITH_SILENT -> putInt(KEY_MUTE_MEDIA_WITH_SILENT, 
                if(newValue as Boolean) 1 else 0)
            else -> false
        }

    private fun putInt(key: String, value: Any): Boolean =
        Settings.System.putInt(context?.contentResolver, key, value as Int)

    private fun putString(key: String, value: Any): Boolean =
        Settings.System.putString(context?.contentResolver, key, value as String)

    companion object {
        private const val KEY_ALERT_SLIDER_BOTTOM = "alert_slider_bottom_preference"
        private const val KEY_ALERT_SLIDER_MIDDLE = "alert_slider_middle_preference"
        private const val KEY_ALERT_SLIDER_TOP = "alert_slider_top_preference"

        private const val KEY_VIBRATOR_CATEGORY = "vibrator"
        private const val PROP_KRYPTON_DEVICE = "ro.krypton.build.device"
        private const val GUACAMOLEB = "guacamoleb"

        private const val KEY_VIBRATOR_PREFERENCE = "device_setting_vib_strength"
        private const val FILE_LEVEL = "/sys/devices/platform/soc/89c000.i2c/i2c-2/2-005a/leds/vibrator/level"
        private const val DEFAULT = "2"

        private val HEAVY_CLICK_EFFECT = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
    
        private const val KEY_MUTE_MEDIA_WITH_SILENT = "config_mute_media"
    }
}
