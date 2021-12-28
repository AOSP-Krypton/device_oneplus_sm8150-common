/*
 * Copyright (C) 2013 The OmniROM Project
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

package com.krypton.settings.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.provider.Settings

import androidx.annotation.Keep

import com.android.internal.lineage.hardware.LineageHardwareManager
import com.android.internal.lineage.hardware.LineageHardwareManager.FEATURE_TOUCHSCREEN_GESTURES
import com.android.internal.lineage.hardware.TouchscreenGesture
import com.android.internal.util.krypton.FileUtils

@Keep
class BootCompletedReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                restoreVibrationStrengthPreference(context)
                val hardwareManager = LineageHardwareManager.getInstance(context)
                if (hardwareManager.isSupported(FEATURE_TOUCHSCREEN_GESTURES)) {
                    hardwareManager.touchscreenGestures.forEach { gesture: TouchscreenGesture ->
                        val actionForGesture = Settings.System.getInt(context.contentResolver,
                            Utils.getResName(gesture.name), 0)
                        hardwareManager.setTouchscreenGestureEnabled(gesture, actionForGesture > 0)
                    }
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> context.startServiceAsUser(Intent(context,
                ClientPackageObserverService::class.java), UserHandle.SYSTEM)
        }
    }

    private fun restoreVibrationStrengthPreference(context: Context) {
        if (!FileUtils.isFileWritable(FILE_LEVEL)) return
        val storedValue = Settings.System.getString(context.contentResolver,
            KEY_VIBSTRENGTH) ?: DEFAULT
        FileUtils.writeLine(FILE_LEVEL, storedValue)
    }

    companion object {
        private const val KEY_VIBSTRENGTH = "device_setting_vib_strength"
        private const val FILE_LEVEL = "/sys/devices/platform/soc/89c000.i2c/i2c-2/2-005a/leds/vibrator/level"
        private const val DEFAULT = "2"
    }
}
