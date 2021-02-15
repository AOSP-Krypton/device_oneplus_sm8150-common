/*
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

package com.krypton.settings.device.fragments;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.ArraySet;
import android.view.View;
import android.widget.Button;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.krypton.settings.device.R;
import com.krypton.settings.device.Utils;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.Set;

public class CustomRefreshModeFragment extends PreferenceFragmentCompat {

    private final ArrayList<AppInfo> mList = new ArrayList<>();
    private final Set<String> mStoredList = Settings.System.CUSTOM_REFRESH_RATE_MODE_APPS;
    private Context mContext;
    private ExecutorService mExecutor;
    private Handler mHandler;
    private PackageManager pm;
    private PreferenceScreen mScreen;
    private boolean mShowSystem = false;

    @Override
    public void onCreatePreferences(Bundle bundle, String key) {
        mContext = getContext();
        mHandler = new Handler(Looper.getMainLooper());
        mExecutor = Executors.newSingleThreadExecutor();
        mScreen = getPreferenceManager().createPreferenceScreen(mContext);
        pm = mContext.getPackageManager();
        setPreferenceScreen(mScreen);
        setView();
    }

    @Override
    public void onStart() {
        super.onStart();
        Button resetButton = getActivity().findViewById(R.id.custom_refresh_rate_resetbutton);
        if (resetButton != null) {
            resetButton.setOnClickListener(v -> {
                if (mStoredList.isEmpty()) return;
                mStoredList.clear();
                for (String key: mStoredList) {
                    CheckBoxPreference checkBox = (CheckBoxPreference) findPreference(key);
                    if (checkBox != null) checkBox.setChecked(false);
                }
            });
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        updateAppsList(preference);
        return super.onPreferenceTreeClick(preference);
    }

    public void setView() {
        mExecutor.execute(() -> {
            for (PackageInfo packageInfo: getSortedList()) {
                CheckBoxPreference checkBox = new CheckBoxPreference(mContext);
                String name = packageInfo.applicationInfo.loadLabel(pm).toString();
                String key = packageInfo.packageName;
                boolean flag = (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
                checkBox.setIcon(packageInfo.applicationInfo.loadIcon(pm));
                checkBox.setTitle(name);
                checkBox.setKey(key);
                checkBox.setVisible(flag);
                mHandler.post(() -> {
                    mScreen.addPreference(checkBox);
                });
                mList.add(new AppInfo(name, key, flag));
            }
        });
    }

    private List<PackageInfo> getSortedList() {
        List<PackageInfo> list = pm.getInstalledPackages(0);
        for (int i=0; i < list.size(); i++) {
            for (int j=0; j < list.size(); j++) {
                String first = list.get(i).applicationInfo.loadLabel(pm).toString();
                String second = list.get(j).applicationInfo.loadLabel(pm).toString();
                if (first.compareToIgnoreCase(second) < 0) {
                    PackageInfo temp = list.get(i);
                    list.set(i, list.get(j));
                    list.set(j, temp);
                }
            }
        }
        return list;
    }

    private void updateAppsList(Preference preference) {
        String prefPackageName = preference.getKey();
        if (prefPackageName == null) return;
        if (mStoredList.isEmpty()) {
            mStoredList.add(prefPackageName);
        } else {
            if (Utils.isChecked(preference)) {
                if (mStoredList.contains(prefPackageName)) {
                    mStoredList.remove(prefPackageName);
                } else {
                    mStoredList.add(prefPackageName);
                }
            }
        }
    }

    public void updateStatus(boolean showSystem) {
        mShowSystem = showSystem;
    }

    public void updateView() {
        mExecutor.execute(() -> {
            for (AppInfo appInfo: mList) {
                mHandler.post(() -> {
                    setVisibility(appInfo, comFlag(appInfo));
                });
            }
        });
    }

    public void filterApps(String query) {
        if (query != null) {
            mExecutor.execute(() -> {
                for (AppInfo appInfo: mList) {
                    mHandler.post(() -> {
                        setVisibility(appInfo, (query.isEmpty() && comFlag(appInfo))
                            || (comFlag(appInfo) && matchIgnoreCase(appInfo.name, query)));
                    });
                }
            });
        }
    }

    private void setVisibility(AppInfo appInfo, boolean flag) {
        Preference pref = findPreference(appInfo.key);
        if (pref != null) pref.setVisible(flag);
    }

    private boolean matchIgnoreCase(String one, String two) {
        return one.toLowerCase().contains(two.toLowerCase());
    }

    private boolean comFlag(AppInfo appInfo) {
        return mShowSystem || appInfo.isUser;
    }

    private class AppInfo {
        public String name;
        public String key;
        public boolean isUser;

        public AppInfo(String name, String key, boolean flag) {
            this.name = name;
            this.key = key;
            isUser = flag;
        }
    }
}
