/*
 * Copyright (C) 2019 The LineageOS Project
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

package com.krypton.camerahelper;

import android.os.FileUtils;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public final class CameraMotorController {
    private static final String TAG = "CameraMotorController";

    // Camera motor paths
    private static final String CAMERA_MOTOR_ENABLE_PATH =
            "/sys/devices/platform/vendor/vendor:motor_pl/enable";
    private static final String CAMERA_MOTOR_HALL_CALIBRATION =
            "/sys/devices/platform/vendor/vendor:motor_pl/hall_calibration";
    private static final String CAMERA_MOTOR_DIRECTION_PATH =
            "/sys/devices/platform/vendor/vendor:motor_pl/direction";
    private static final String CAMERA_MOTOR_POSITION_PATH =
            "/sys/devices/platform/vendor/vendor:motor_pl/position";

    // Motor calibration data path
    private static final String CAMERA_PERSIST_HALL_CALIBRATION =
            "/mnt/vendor/persist/engineermode/hall_calibration";

    // Motor fallback calibration data
    private static final String HALL_CALIBRATION_DEFAULT =
            "170,170,480,0,0,480,500,0,0,500,1500";

    // Motor control values
    private static final String DIRECTION_DOWN = "0";
    private static final String DIRECTION_UP = "1";
    private static final String ENABLED = "1";
    private static final String POSITION_DOWN = "1";

    private static final File positionFile = new File(CAMERA_MOTOR_POSITION_PATH);

    private CameraMotorController() {
        // This class is not supposed to be instantiated
    }

    protected static void calibrate() {
        String calibrationData = HALL_CALIBRATION_DEFAULT;
        try {
            calibrationData = FileUtils.readTextFile(
                    new File(CAMERA_PERSIST_HALL_CALIBRATION), 0, null);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read " + CAMERA_PERSIST_HALL_CALIBRATION, e);
        }
        try {
            FileUtils.stringToFile(CAMERA_MOTOR_HALL_CALIBRATION, calibrationData);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to " + CAMERA_MOTOR_HALL_CALIBRATION, e);
        }
    }

    private static void setMotorDirection(String direction) {
        try {
            FileUtils.stringToFile(CAMERA_MOTOR_DIRECTION_PATH, direction);
            FileUtils.stringToFile(CAMERA_MOTOR_ENABLE_PATH, ENABLED);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write", e);
        }
    }

    protected static void closeCamera() {
        setMotorDirection(DIRECTION_DOWN);
    }

    protected static void openCamera() {
        setMotorDirection(DIRECTION_UP);
    }

    protected static boolean isCameraClosed() {
        try {
            return TextUtils.equals(
                FileUtils.readTextFile(positionFile, 1, null), POSITION_DOWN);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read " + CAMERA_MOTOR_POSITION_PATH, e);
        }
        return false;
    }
}
