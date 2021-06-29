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

import static android.view.KeyEvent.ACTION_DOWN;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import androidx.annotation.Keep;

import com.android.internal.os.DeviceKeyHandler;

@Keep
public final class KeyHandler implements DeviceKeyHandler {
    private static final String TAG = "KeyHandler";

    // Camera motor event key codes
    private static final int MOTOR_EVENT_MANUAL_TO_DOWN = 184;
    private static final int MOTOR_EVENT_UP_ABNORMAL = 186;
    private static final int MOTOR_EVENT_DOWN_ABNORMAL = 189;

    private final Context mContext;
    private final Handler mHandler;
    private Context mPackageContext;

    public KeyHandler(Context context) {
        mContext = context;
        try {
            mPackageContext = mContext.createPackageContext("com.krypton.camerahelper", 0);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create package context", e);
        }
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public boolean handleKeyEvent(KeyEvent event) {
        if (event.getAction() != ACTION_DOWN) {
            return false;
        }
        switch (event.getScanCode()) {
            case MOTOR_EVENT_MANUAL_TO_DOWN:
                showCameraMotorPressWarning();
                break;
            case MOTOR_EVENT_UP_ABNORMAL:
                showCameraMotorCannotGoUpWarning();
                break;
            case MOTOR_EVENT_DOWN_ABNORMAL:
                showCameraMotorCannotGoDownWarning();
                break;
            default:
                return false;
        }
        return true;
    }

    private void showCameraMotorCannotGoDownWarning() {
        if (mPackageContext != null) {
            showAlertDialog(getBuilder(R.string.motor_cannot_go_down_message)
                .setPositiveButton(R.string.retry, (dialog, which) ->
                    CameraMotorController.closeCamera()));
        }
    }

    private void showCameraMotorCannotGoUpWarning() {
        if (mPackageContext != null) {
            showAlertDialog(getBuilder(R.string.motor_cannot_go_up_message)
                .setNegativeButton(R.string.retry, (dialog, which) ->
                    CameraMotorController.openCamera())
                .setPositiveButton(R.string.close, (dialog, which) -> {
                    CameraMotorController.closeCamera();
                    goHome();
                }));
        }
    }

    private void showCameraMotorPressWarning() {
        goHome();
        if (mPackageContext != null) {
            showAlertDialog(getBuilder(R.string.motor_press_message)
                .setPositiveButton(android.R.string.ok, null));
        }
    }

    private AlertDialog.Builder getBuilder(int msgId) {
        return new AlertDialog.Builder(mPackageContext)
                .setTitle(R.string.warning)
                .setMessage(msgId);
    }

    private void showAlertDialog(AlertDialog.Builder builder) {
        // Show the alert
        mHandler.post(() -> {
            AlertDialog dialog = builder.create();
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        });
    }

    private void goHome() {
        // Go back to home to close all camera apps first
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }
}
