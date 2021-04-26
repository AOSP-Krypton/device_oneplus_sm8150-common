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

public class CameraMotorController {
    private static final String TAG = "CameraMotorController";

    // Camera motor paths
    private static final String CAMERA_MOTOR_ENABLE_PATH =
            "/sys/devices/platform/vendor/vendor:motor_pl/enable";
    public static final String CAMERA_MOTOR_HALL_CALIBRATION =
            "/sys/devices/platform/vendor/vendor:motor_pl/hall_calibration";
    private static final String CAMERA_MOTOR_DIRECTION_PATH =
            "/sys/devices/platform/vendor/vendor:motor_pl/direction";
    private static final String CAMERA_MOTOR_POSITION_PATH =
            "/sys/devices/platform/vendor/vendor:motor_pl/position";

    // Motor calibration data path
    public static final String CAMERA_PERSIST_HALL_CALIBRATION =
            "/mnt/vendor/persist/engineermode/hall_calibration";

    // Motor fallback calibration data
    public static final String HALL_CALIBRATION_DEFAULT =
            "170,170,480,0,0,480,500,0,0,500,1500";

    // Motor control values
    public static final String DIRECTION_DOWN = "0";
    public static final String DIRECTION_UP = "1";
    public static final String ENABLED = "1";
    public static final String POSITION_DOWN = "1";
    public static final String POSITION_UP = "0";

    private CameraMotorController() {
        // This class is not supposed to be instantiated
    }

    protected static final void calibrate() {
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

    private static final void setMotorDirection(String direction) {
        try {
            FileUtils.stringToFile(CAMERA_MOTOR_DIRECTION_PATH, direction);
            FileUtils.stringToFile(CAMERA_MOTOR_ENABLE_PATH, ENABLED);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write", e);
        }
    }

    private static final String getMotorPosition() {
        try {
            return FileUtils.readTextFile(new File(CAMERA_MOTOR_POSITION_PATH), 1, null);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read " + CAMERA_MOTOR_POSITION_PATH, e);
        }
        return null;
    }

    protected static final void closeCamera() {
        setMotorDirection(DIRECTION_DOWN);
    }

    protected static final void openCamera() {
        setMotorDirection(DIRECTION_UP);
    }

    protected static final boolean isCameraClosed() {
        String pos = getMotorPosition();
        return pos != null && pos.equals(POSITION_DOWN);
    }
}
