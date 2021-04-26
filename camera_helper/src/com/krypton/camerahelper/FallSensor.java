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
import android.view.WindowManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FallSensor implements SensorEventListener {

    private ExecutorService mExecutorService;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Context mContext;

    public FallSensor(Context context) {
        mContext = context;
        mSensorManager = mContext.getSystemService(SensorManager.class);
        mExecutorService = Executors.newSingleThreadExecutor();

        for (Sensor sensor : mSensorManager.getSensorList(Sensor.TYPE_ALL)) {
            if (TextUtils.equals(sensor.getStringType(), "oneplus.sensor.free_fall")) {
                mSensor = sensor;
                break;
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values[0] <= 0) {
            return;
        }

        // We shouldn't really bother doing anything if motor is already closed
        if (CameraMotorController.isCameraClosed()) {
            return;
        }

        // Close the camera
        CameraMotorController.closeCamera();

        // Show alert dialog informing user that we closed the camera
        new Handler(Looper.getMainLooper()).post(() -> {
            AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                    .setTitle(R.string.free_fall_detected_title)
                    .setMessage(R.string.free_fall_detected_message)
                    .setNegativeButton(R.string.raise_the_camera, (dialog, which) ->
                        CameraMotorController.openCamera())
                    .setPositiveButton(R.string.close, (dialog, which) -> {
                        // Go back to home screen
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                    })
                    .create();
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /* Empty */
    }

    void enable() {
        mExecutorService.submit(() -> {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        });
    }

    void disable() {
        mExecutorService.submit(() -> {
            mSensorManager.unregisterListener(this, mSensor);
        });
    }
}
