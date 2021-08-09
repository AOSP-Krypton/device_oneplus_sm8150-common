/*
* Copyright (C) 2016 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.krypton.settings.device;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.util.krypton.FileUtils;

public class VibratorStrengthPreference extends Preference implements
        SeekBar.OnSeekBarChangeListener {
    private static final String FILE_LEVEL = "/sys/devices/platform/soc/89c000.i2c/i2c-2/2-005a/leds/vibrator/level";
    private static final String SETTINGS_KEY = Constants.KEY_SETTINGS_PREFIX + Constants.KEY_VIBSTRENGTH;
    private static final String DEFAULT = "2";
    private static final long testVibrationPattern[] = {0,250};
    private static final int mMinValue = 0;
    private static final int mMaxValue = 10;
    private final Context mContext;
    private final Vibrator mVibrator;
    private SeekBar mSeekBar;
    private TextView mProgressValue;

    public VibratorStrengthPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mVibrator = context.getSystemService(Vibrator.class);
        setLayoutResource(R.layout.preference_seek_bar);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mSeekBar = (SeekBar) holder.findViewById(R.id.seekBar);
        mProgressValue = (TextView) holder.findViewById(R.id.progress_value);
        final String currentValue = getValue();
        mSeekBar.setMin(mMinValue);
        mSeekBar.setMax(mMaxValue);
        mSeekBar.setProgress(Integer.parseInt(currentValue));
        mSeekBar.setOnSeekBarChangeListener(this);
        mProgressValue.setText(currentValue);
    }

	private static String getValue() {
        final String valueInFile = FileUtils.readOneLine(FILE_LEVEL);
        return valueInFile == null ? DEFAULT : valueInFile;
	}

	private void setValue(String newValue, boolean withFeedback) {
        mProgressValue.setText(newValue);
	    FileUtils.writeLine(FILE_LEVEL, newValue);
        Settings.System.putString(mContext.getContentResolver(), SETTINGS_KEY, newValue);
        if (withFeedback) {
            mVibrator.vibrate(testVibrationPattern, -1);
        }
	}

    public static void restore(Context context) {
        if (!FileUtils.isFileWritable(FILE_LEVEL)) {
            return;
        }
        String storedValue = Settings.System.getString(context.getContentResolver(), SETTINGS_KEY);
        if (storedValue == null) {
            storedValue = DEFAULT;
        }
        FileUtils.writeLine(FILE_LEVEL, storedValue);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromTouch) {
        setValue(String.valueOf(progress), true);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // NA
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // NA
    }
}
