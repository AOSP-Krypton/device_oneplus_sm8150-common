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

import android.annotation.NonNull;
import android.app.Service;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraManager.AvailabilityCallback;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

public final class CameraMotorService extends Service implements Handler.Callback {
    private static final String TAG = "CameraMotorService";
    private static final String FRONT_CAMERA_ID = "1";
    private static final int CAMERA_EVENT_DELAY_TIME = 100; // ms
    private static final int MSG_CAMERA_CLOSED = 1000;
    private static final int MSG_CAMERA_OPEN = 1001;

    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private long mClosedEvent, mOpenEvent;

    private AvailabilityCallback mAvailabilityCallback = new AvailabilityCallback() {
        @Override
        public void onCameraAvailable(@NonNull String cameraId) {
            super.onCameraAvailable(cameraId);
            if (cameraId.equals(FRONT_CAMERA_ID)) {
                mClosedEvent = SystemClock.elapsedRealtime();
                if (mClosedEvent - mOpenEvent < CAMERA_EVENT_DELAY_TIME
                        && mHandler.hasMessages(MSG_CAMERA_OPEN)) {
                    mHandler.removeMessages(MSG_CAMERA_OPEN);
                }
                mHandler.sendEmptyMessageDelayed(MSG_CAMERA_CLOSED,
                    CAMERA_EVENT_DELAY_TIME);
            }
        }

        @Override
        public void onCameraUnavailable(@NonNull String cameraId) {
            super.onCameraUnavailable(cameraId);
            if (cameraId.equals(FRONT_CAMERA_ID)) {
                mOpenEvent = SystemClock.elapsedRealtime();
                if (mOpenEvent - mClosedEvent < CAMERA_EVENT_DELAY_TIME
                        && mHandler.hasMessages(MSG_CAMERA_CLOSED)) {
                    mHandler.removeMessages(MSG_CAMERA_CLOSED);
                }
                mHandler.sendEmptyMessageDelayed(MSG_CAMERA_OPEN,
                    CAMERA_EVENT_DELAY_TIME);
            }
        }
    };

    @Override
    public void onCreate() {
        mHandlerThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), this);
        CameraMotorController.calibrate();
        getSystemService(CameraManager.class)
            .registerAvailabilityCallback(mAvailabilityCallback, null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mHandlerThread.quitSafely();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_CAMERA_CLOSED:
                CameraMotorController.closeCamera();
                break;
            case MSG_CAMERA_OPEN:
                CameraMotorController.openCamera();
                break;
        }
        return true;
    }
}
