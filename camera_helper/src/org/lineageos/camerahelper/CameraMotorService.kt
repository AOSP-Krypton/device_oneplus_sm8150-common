/*
 * Copyright (C) 2019 The LineageOS Project
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

import android.annotation.NonNull
import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.WindowManager

class CameraMotorService: Service(), Handler.Callback {

    companion object{
        private const val DEBUG = true
        private const val TAG = "CameraMotorService"

        private const val ALWAYS_ON_DIALOG_KEY = "always_on_camera_dialog"

        const val CAMERA_EVENT_DELAY_TIME: Long = 100 // ms

        const val FRONT_CAMERA_ID = "1"

        const val MSG_CAMERA_CLOSED = 1000
        const val MSG_CAMERA_OPEN = 1001  
    }

    private val handler = Handler(this)

    private var mAlertDialog: AlertDialog? = null
    private lateinit var displayManager: DisplayManager

    private var mClosedEvent:Long = 0
    private var mOpenEvent:Long = 0

    private var mAvailabilityCallback =
        object : CameraManager.AvailabilityCallback() {
                override fun onCameraClosed(cameraId: String) {
                    super.onCameraClosed(cameraId)

                    if (cameraId.equals(FRONT_CAMERA_ID)) {
                        mClosedEvent = SystemClock.elapsedRealtime()
                        if (mClosedEvent - mOpenEvent < CAMERA_EVENT_DELAY_TIME
                                && handler.hasMessages(MSG_CAMERA_OPEN)) {
                            handler.removeMessages(MSG_CAMERA_OPEN)
                        }
                        handler.sendEmptyMessageDelayed(MSG_CAMERA_CLOSED,
                                CAMERA_EVENT_DELAY_TIME)
                    }
                }

                override fun onCameraOpened(cameraId: String, packageId: String) {
                    super.onCameraClosed(cameraId)

                    if (cameraId.equals(FRONT_CAMERA_ID)) {
                        mOpenEvent = SystemClock.elapsedRealtime()
                        if (mOpenEvent - mClosedEvent < CAMERA_EVENT_DELAY_TIME
                                && handler.hasMessages(MSG_CAMERA_CLOSED)) {
                            handler.removeMessages(MSG_CAMERA_CLOSED)
                        }
                        handler.sendEmptyMessageDelayed(MSG_CAMERA_OPEN,
                                CAMERA_EVENT_DELAY_TIME)
                    }
                }
            }

    override fun onCreate() {
        CameraMotorController.calibrate()

        val cameraManager = getSystemService(CameraManager::class.java)
        cameraManager.registerAvailabilityCallback(mAvailabilityCallback, null)

        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        logD("Starting service")
        return START_STICKY
    }

    override fun onDestroy() {
        logD("Destroying service")
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_CAMERA_CLOSED -> lowerCamera()
            MSG_CAMERA_OPEN -> maybeRaiseCamera()
        }
        return true
    }

    private fun maybeRaiseCamera() {
        var screenOn = false
        for (display : Display in displayManager.getDisplays()) {
            if (display.getState() != Display.STATE_OFF) {
                screenOn = true
                break
            }
        }
        val alwaysOnDialog = Settings.System.getInt(getContentResolver(),
                ALWAYS_ON_DIALOG_KEY, 0) == 1
        if (screenOn && !alwaysOnDialog) {
            raiseCamera()
        } else {
            if (mAlertDialog == null) {
                mAlertDialog = AlertDialog.Builder(this)
                        .setMessage(R.string.popup_camera_dialog_message)
                        .setNegativeButton(R.string.popup_camera_dialog_no,
                                {_, _ -> {
                            // Go back to home screen
                            val intent: Intent = Intent(Intent.ACTION_MAIN)
                            intent.addCategory(Intent.CATEGORY_HOME)
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            this.startActivity(intent)
                                }})
                        .setPositiveButton(R.string.popup_camera_dialog_raise,
                                {_:Any, _:Any -> raiseCamera()}
                        .create()
                mAlertDialog?.getWindow().setType(WindowManager?.LayoutParams.TYPE_SYSTEM_ERROR)
                mAlertDialog?.setCanceledOnTouchOutside(false)
            }
            if (mAlertDialog?.isShowing() == false) mAlertDialog?.show()
        }
    }

    private fun raiseCamera() {
        logD("Raising camera")
        CameraMotorController.setMotorDirection(CameraMotorController.DIRECTION_UP)
        CameraMotorController.setMotorEnabled()
    }

    private fun lowerCamera() {
        logD("Lowering camera")
        if (mAlertDialog?.isShowing() == true) mAlertDialog?.dismiss()
        CameraMotorController.setMotorDirection(CameraMotorController.DIRECTION_DOWN)
        CameraMotorController.setMotorEnabled()
    }

    private fun logD(msg: String){
        if (DEBUG) Log.d(TAG, msg)
    }
}
