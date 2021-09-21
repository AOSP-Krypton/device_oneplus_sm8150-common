/*
 * Copyright (C) 2013 The OmniROM Project
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

package com.krypton.settings.device;

import static com.android.internal.lineage.hardware.LineageHardwareManager.FEATURE_TOUCHSCREEN_GESTURES;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.Keep;

import com.android.internal.lineage.hardware.LineageHardwareManager;
import com.android.internal.lineage.hardware.TouchscreenGesture;

@Keep
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            return;
        } else if (action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
            final ContentResolver resolver = context.getContentResolver();
            LineageHardwareManager mHardwareManager = LineageHardwareManager.getInstance(context);
            VibratorStrengthPreference.restore(context);
            if (mHardwareManager.isSupported(FEATURE_TOUCHSCREEN_GESTURES)) {
                for (TouchscreenGesture gesture: mHardwareManager.getTouchscreenGestures()) {
                    final int actionForGesture = Settings.System.getInt(
                        resolver, Utils.getResName(gesture.name), 0);
                    mHardwareManager.setTouchscreenGestureEnabled(gesture, actionForGesture > 0);
                }
            }
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            context.startServiceAsUser(new Intent(context, ClientPackageObserverService.class), UserHandle.SYSTEM);
        }
    }
}
