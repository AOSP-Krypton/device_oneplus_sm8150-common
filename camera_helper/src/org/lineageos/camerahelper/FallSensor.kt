/*
 * Copyright (c) 2019 The LineageOS Project
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

package org.lineageos.camerahelper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class FallSensor: SensorEventListener {
    companion object {
        private const val DEBUG = true;
        private const val TAG = "FallSensor";
    }

    private mExecutorService: ExecutorService;
    private mSensorManager: SensorManager;
    private mSensor: Sensor;

    constructor(context: Context) {
        mSensorManager = context.getSystemService(SensorManager.class);
        mExecutorService = Executors.newSingleThreadExecutor();

        for ( sensor : Sensor in mSensorManager.getSensorList(Sensor.TYPE_ALL)) {
            if (DEBUG) Log.d(TAG, "Sensor type: " + sensor.getStringType());
            if (TextUtils.equals(sensor.getStringType(), "oneplus.sensor.free_fall")) {
                if (DEBUG) Log.d(TAG, "Found fall sensor");
                mSensor = sensor;
                break;
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values[0] <= 0) {
            return;
        }

        Log.d(TAG, "Fall detected, ensuring front camera is closed");

        // We shouldn't really bother doing anything if motor is already closed
        if (CameraMotorController.getMotorPosition().equals(CameraMotorController.POSITION_DOWN)) {
            return;
        }

        // Close the camera
        CameraMotorController.setMotorDirection(CameraMotorController.DIRECTION_DOWN);
        CameraMotorController.setMotorEnabled();

        // Show alert dialog informing user that we closed the camera
        Handler(Looper.getMainLooper()).post({ -> {
            AlertDialog alertDialog = AlertDialog.Builder(context)
                    .setTitle(R.string.free_fall_detected_title)
                    .setMessage(R.string.free_fall_detected_message)
                    .setNegativeButton(R.string.raise_the_camera, {_, _ -> {
                        // Reopen the camera
                        CameraMotorController.setMotorDirection(CameraMotorController.DIRECTION_UP);
                        CameraMotorController.setMotorEnabled();
                    }})
                    .setPositiveButton(R.string.close, {_, _ -> {
                        // Go back to home screen
                        val intent: Intent = Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }})
                    .create();
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }});
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        /* Empty */
    }

    fun enable() {
        if (DEBUG) Log.d(TAG, "Enabling");
        mExecutorService.submit({ -> {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }});
    }

    fun disable() {
        if (DEBUG) Log.d(TAG, "Disabling");
        mExecutorService.submit({ -> {
            mSensorManager.unregisterListener(this, mSensor);
        }});
    }
}
