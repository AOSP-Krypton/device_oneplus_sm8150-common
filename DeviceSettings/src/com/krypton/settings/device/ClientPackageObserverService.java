/**
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

package com.krypton.settings.device;

import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.FileObserver;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.util.krypton.FileUtils;
import com.android.internal.util.krypton.KryptonUtils;

import java.io.File;

import vendor.oneplus.hardware.camera.V1_0.IOnePlusCameraProvider;

public class ClientPackageObserverService extends Service {
    private static final String TAG = "ClientPackageObserverService";
    private static final String CLIENT_PACKAGE_NAME = "com.oneplus.camera";
    private static final String CLIENT_PACKAGE_PATH = "/data/misc/aosp/client_package_name";
    private static final boolean DEBUG = false;

    private IOnePlusCameraProvider mProvider;
    private boolean mIsOpCameraInstalledAndActive;
    private boolean mIsClientObserverRegistered;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            logD("action = " + action);
            if (action == null) {
                return;
            }
            switch (action) {
                case ACTION_SCREEN_OFF:
                    unregisterClientObserver();
                    break;
                case ACTION_SCREEN_ON:
                    registerClientObserver();
            }
        }
    };

    private final FileObserver mFileObserver = new FileObserver(new File(CLIENT_PACKAGE_PATH)) {
        @Override
        public void onEvent(int event, String file) {
            setPackageName(file);
        }
    };

    @Override
    public void onCreate() {
        logD("onCreate");
        mIsOpCameraInstalledAndActive = KryptonUtils.isPackageInstalled(this,
            CLIENT_PACKAGE_NAME, false /** ignore state */);
        logD("mIsOpCameraInstalledAndActive = " + mIsOpCameraInstalledAndActive);
        if (mIsOpCameraInstalledAndActive) {
            setPackageName(CLIENT_PACKAGE_PATH);
        }
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SCREEN_OFF);
        filter.addAction(ACTION_SCREEN_ON);
        registerReceiver(mBroadcastReceiver, filter);
        registerClientObserver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        logD("onDestroy");
        unregisterClientObserver();
    }

    private void registerClientObserver() {
        mIsOpCameraInstalledAndActive = KryptonUtils.isPackageInstalled(this,
            CLIENT_PACKAGE_NAME, false /** ignore state */);
        if (mIsOpCameraInstalledAndActive && mFileObserver != null &&
                !mIsClientObserverRegistered) {
            logD("registering client observer");
            mFileObserver.startWatching();
            mIsClientObserverRegistered = true;
        }
    }

    private void unregisterClientObserver() {
        if (mFileObserver != null && mIsClientObserverRegistered) {
            logD("unregistering client observer");
            mFileObserver.stopWatching();
            mIsClientObserverRegistered = false;
        }
    }

    private void setPackageName(String file) {
        String pkgName = FileUtils.readOneLine(file);
        pkgName = pkgName == null ? CLIENT_PACKAGE_NAME : pkgName;
        try {
            logD(" client_package " + file + " and pkg = " + pkgName);
            if (mProvider == null) {
                mProvider = IOnePlusCameraProvider.getService();
            }
            mProvider.setPackageName(pkgName);
        } catch (RemoteException e) {
            Log.e(TAG, "Error communicating with IOnePlusCameraProvider", e);
        }
    }

    private static void logD(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
