/*
 * Copyright (C) 2016 The CyanogenMod project
 * Copyright (C) 2017 The LineageOS Project
 * Copyright (C) 2021-2022 AOSP-Krypton Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import androidx.annotation.Keep
import androidx.preference.PreferenceFragmentCompat

import com.android.internal.lineage.hardware.LineageHardwareManager
import com.android.internal.lineage.hardware.LineageHardwareManager.FEATURE_TOUCHSCREEN_GESTURES
import com.android.internal.lineage.hardware.TouchscreenGesture

import com.krypton.settings.device.R
import com.krypton.settings.device.Utils
import com.krypton.settings.preference.SystemSettingListPreference

@Keep
class GestureSettingsFragment : PreferenceFragmentCompat() {

    private lateinit var hardwareManager: LineageHardwareManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        hardwareManager = LineageHardwareManager.getInstance(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.touchscreen_gesture_settings, rootKey)
        if (!hardwareManager.isSupported(FEATURE_TOUCHSCREEN_GESTURES)) return
        hardwareManager.touchscreenGestures.forEach { gesture: TouchscreenGesture ->
            val listPreference = SystemSettingListPreference(requireContext()).apply {
                key = Utils.getResName(gesture.name)
                summary = "%s"
                setEntries(R.array.touchscreen_gesture_action_entries)
                setEntryValues(R.array.touchscreen_gesture_action_values)
                setDialogTitle(R.string.touchscreen_gesture_action_dialog_title)
                setDefaultValue("0")
                setOnPreferenceChangeListener { _, newValue ->
                    val action = (newValue as String).toInt()
                    hardwareManager.setTouchscreenGestureEnabled(gesture, action > 0)
                }
            }
            with (requireContext()) {
                val resId = resources.getIdentifier(listPreference.key, "string", this.packageName)
                listPreference.title = if (resId != 0) getString(resId) else gesture.name
            }
            preferenceScreen.addPreference(listPreference)
        }
    }
}
