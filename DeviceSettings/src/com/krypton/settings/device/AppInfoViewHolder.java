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

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class AppInfoViewHolder extends RecyclerView.ViewHolder {
    private final ImageView mImageView;
    private final TextView mTextView;
    private final CheckBox mCheckBox;

    public AppInfoViewHolder(View view) {
        super(view);
        mImageView = (ImageView) view.findViewById(R.id.imageView);
        mTextView = (TextView) view.findViewById(R.id.textView);
        mCheckBox = (CheckBox) view.findViewById(R.id.checkBox);
    }

    public TextView getTextView() {
        return mTextView;
    }

    public CheckBox getCheckBox() {
        return mCheckBox;
    }

    public ImageView getImageView() {
        return mImageView;
    }
}
