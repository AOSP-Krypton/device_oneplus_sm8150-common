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

package com.krypton.settings.device

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.Intent.ACTION_SCREEN_ON
import android.content.IntentFilter
import android.os.FileObserver
import android.os.IBinder
import android.os.RemoteException
import android.util.Log

import com.android.internal.util.krypton.FileUtils
import com.android.internal.util.krypton.KryptonUtils

import java.io.File

import vendor.oneplus.hardware.camera.V1_0.IOnePlusCameraProvider

class ClientPackageObserverService: Service() {

    private var isOpCameraInstalledAndActive = false
    private var clientObserverRegistered = false

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                ACTION_SCREEN_OFF -> unregisterClientObserver()
                ACTION_SCREEN_ON -> registerClientObserver()
            }
        }
    }

    private val fileObserver = object : FileObserver(File(CLIENT_PACKAGE_PATH)) {
        override fun onEvent(event: Int , file: String) {
            setPackageName(file)
        }
    }

    override fun onCreate() {
        logD("onCreate")
        isOpCameraInstalledAndActive = KryptonUtils.isPackageInstalled(this,
            CLIENT_PACKAGE_NAME, false /** ignore state */)
        logD("isOpCameraInstalledAndActive = $isOpCameraInstalledAndActive")
        if (isOpCameraInstalledAndActive) {
            setPackageName(CLIENT_PACKAGE_PATH)
        } else {
            stopSelf()
        }
        registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(ACTION_SCREEN_OFF)
            addAction(ACTION_SCREEN_ON)
        })
        registerClientObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() { unregisterClientObserver() }

    private fun registerClientObserver() {
        isOpCameraInstalledAndActive = KryptonUtils.isPackageInstalled(this,
            CLIENT_PACKAGE_NAME, false /** ignore state */)
        if (isOpCameraInstalledAndActive && !clientObserverRegistered) {
            logD("registering client observer")
            fileObserver.startWatching()
            clientObserverRegistered = true
        }
    }

    private fun unregisterClientObserver() {
        if (clientObserverRegistered) {
            logD("unregistering client observer")
            fileObserver.stopWatching()
            clientObserverRegistered = false
        }
    }

    private fun setPackageName(file: String) {
        val pkgName: String? = FileUtils.readOneLine(file) ?: CLIENT_PACKAGE_NAME
        try {
            logD("client_package $file and pkg = $pkgName")
            IOnePlusCameraProvider.getService()?.setPackageName(pkgName)
        } catch (e: RemoteException) {
            Log.e(TAG, "Error communicating with IOnePlusCameraProvider", e)
        }
    }

    private fun logD(msg: String) {
        if (DEBUG) Log.d(TAG, msg)
    }

    companion object {
        private const val CLIENT_PACKAGE_NAME = "com.oneplus.camera"
        private const val CLIENT_PACKAGE_PATH = "/data/misc/aosp/client_package_name"

        private const val TAG = "ClientPackageObserverService"
        private const val DEBUG = false
    }
}
