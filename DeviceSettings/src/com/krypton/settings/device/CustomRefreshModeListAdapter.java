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

package com.krypton.settings.device;

import static android.provider.Settings.System.CUSTOM_REFRESH_RATE_MODE_APPS;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CustomRefreshModeListAdapter extends ListAdapter<AppInfo, AppInfoViewHolder> {
    private static final String mSeparator = "|";
    private final Context mContext;
    private List<AppInfo> mAppsList;

    private static final ItemCallback<AppInfo> mItemCallback = new ItemCallback<>() {
        @Override
         public boolean areItemsTheSame(@NonNull AppInfo oldInfo,
                @NonNull AppInfo newInfo) {
             return oldInfo.getPackageName().equals(newInfo.getPackageName());
         }

         @Override
         public boolean areContentsTheSame(@NonNull AppInfo oldInfo,
                @NonNull AppInfo newInfo) {
             return oldInfo.equals(newInfo);
         }
    };

    public CustomRefreshModeListAdapter(Context context) {
        super(mItemCallback);
        mContext = context;
    }

    @Override
    public AppInfoViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new AppInfoViewHolder(LayoutInflater.from(mContext).inflate(
            R.layout.custom_refresh_rate_list_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(AppInfoViewHolder viewHolder, final int position) {
        final AppInfo appInfo = mAppsList.get(position);
        viewHolder.getImageView().setImageDrawable(appInfo.getIcon());
        viewHolder.getTextView().setText(appInfo.getAppName());
        final CheckBox checkBox = viewHolder.getCheckBox();
        checkBox.setChecked(Utils.getStringFromSettings(mContext,
            CUSTOM_REFRESH_RATE_MODE_APPS).contains(appInfo.getPackageName()));
        checkBox.setOnClickListener(v ->
            updateAppsListInSettings(appInfo.getPackageName(), checkBox.isChecked()));
    }

    @Override
    public int getItemCount() {
        if (mAppsList == null) {
            return 0;
        }
        return mAppsList.size();
    }

    public void updateList(List<AppInfo> appsList) {
        mAppsList = appsList;
    }

    private void updateAppsListInSettings(String key, boolean checked) {
        String list = Utils.getStringFromSettings(mContext, CUSTOM_REFRESH_RATE_MODE_APPS);
        if (list != null) {
            if (checked && !list.contains(key)) {
                list = list.concat(key + mSeparator);
            } else if (!checked && list.contains(key)) {
                list = list.replace(key + mSeparator, "");
            }
        } else if (checked) {
            list = key + mSeparator;
        }
        Utils.putStringInSettings(mContext, CUSTOM_REFRESH_RATE_MODE_APPS, list);
    }
}
