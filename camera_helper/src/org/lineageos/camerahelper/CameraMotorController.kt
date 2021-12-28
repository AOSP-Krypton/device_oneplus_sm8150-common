/*
 * Copyright (c) 2019 The LineageOS Project
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

package org.lineageos.camerahelper

import android.os.FileUtils
import android.text.TextUtils
import android.util.Log

import java.io.File
import java.io.IOException

public class CameraMotorController private constructor() {
   companion object {
        private const val TAG = "CameraMotorController"

        // Camera motor paths
        private const val CAMERA_MOTOR_ENABLE_PATH =
                "/sys/devices/platform/vendor/vendor:motor_pl/enable"
        const val CAMERA_MOTOR_HALL_CALIBRATION =
                "/sys/devices/platform/vendor/vendor:motor_pl/hall_calibration"
        private const val CAMERA_MOTOR_DIRECTION_PATH =
                "/sys/devices/platform/vendor/vendor:motor_pl/direction"
        private const val CAMERA_MOTOR_POSITION_PATH =
                "/sys/devices/platform/vendor/vendor:motor_pl/position"

        // Motor calibration data path
        const val CAMERA_PERSIST_HALL_CALIBRATION =
                "/mnt/vendor/persist/engineermode/hall_calibration"

        // Motor fallback calibration data
        const val HALL_CALIBRATION_DEFAULT =
                "170,170,480,0,0,480,500,0,0,500,1500"

        // Motor control values
        const val DIRECTION_DOWN = "0"
        const val DIRECTION_UP = "1"
        const val ENABLED = "1"
        const val POSITION_DOWN = "1"
        const val POSITION_UP = "0"

        fun calibrate() {
            var calibrationData = HALL_CALIBRATION_DEFAULT
    
            try {
                calibrationData = FileUtils.readTextFile(
                        File(CAMERA_PERSIST_HALL_CALIBRATION), 0, null)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to read " + CAMERA_PERSIST_HALL_CALIBRATION, e)
            }
    
            try {
                FileUtils.stringToFile(CAMERA_MOTOR_HALL_CALIBRATION, calibrationData)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to write to " + CAMERA_MOTOR_HALL_CALIBRATION, e)
            }
        }
    
        fun setMotorDirection(direction: String) {
            try {
                FileUtils.stringToFile(CAMERA_MOTOR_DIRECTION_PATH, direction)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to write to " + CAMERA_MOTOR_DIRECTION_PATH, e)
            }
        }
    
        fun setMotorEnabled() {
            try {
                FileUtils.stringToFile(CAMERA_MOTOR_ENABLE_PATH, ENABLED)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to write to " + CAMERA_MOTOR_ENABLE_PATH, e)
            }
        }
    
        fun getMotorPosition(): String? {
            try {
                return FileUtils.readTextFile(File(CAMERA_MOTOR_POSITION_PATH), 1, null)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to read " + CAMERA_MOTOR_POSITION_PATH, e)
            }
            return null
        }
   }
}
