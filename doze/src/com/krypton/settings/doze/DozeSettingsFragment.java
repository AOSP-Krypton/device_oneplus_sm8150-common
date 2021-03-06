/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
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

package com.krypton.settings.doze;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragment;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

public class DozeSettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener,
        CompoundButton.OnCheckedChangeListener {

    private Activity mActivity;

    private TextView mTextView;
    private View mSwitchBar;

    private SwitchPreference mAlwaysOnDisplayPreference;

    private SwitchPreference mPickUpPreference;
    private SwitchPreference mRaiseToWakePreference;
    private SwitchPreference mCustomBrightnessPreference;
    private SeekBarPreference mBrightnessSeekBar;

    private Handler mHandler = new Handler();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.doze_settings);
        mActivity = getActivity();
        final ActionBar actionBar = mActivity.getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        SharedPreferences prefs = mActivity.getSharedPreferences("doze_settings",
                Activity.MODE_PRIVATE);
        if (savedInstanceState == null && !prefs.getBoolean("first_help_shown", false)) {
            showHelp();
        }

        boolean dozeEnabled = Utils.isDozeEnabled(mActivity);

        mAlwaysOnDisplayPreference = (SwitchPreference) findPreference(Utils.ALWAYS_ON_DISPLAY);
        mAlwaysOnDisplayPreference.setEnabled(dozeEnabled);
        mAlwaysOnDisplayPreference.setChecked(Utils.isAlwaysOnEnabled(mActivity));
        mAlwaysOnDisplayPreference.setOnPreferenceChangeListener(this);

        PreferenceCategory pickupSensorCategory = (PreferenceCategory) getPreferenceScreen().
                findPreference(Utils.CATEG_PICKUP_SENSOR);

        mPickUpPreference = (SwitchPreference) findPreference(Utils.GESTURE_PICK_UP_KEY);
        mPickUpPreference.setEnabled(dozeEnabled);
        mPickUpPreference.setOnPreferenceChangeListener(this);

        mRaiseToWakePreference = (SwitchPreference) findPreference(Utils.GESTURE_RAISE_TO_WAKE_KEY);
        mRaiseToWakePreference.setEnabled(dozeEnabled);
        mRaiseToWakePreference.setOnPreferenceChangeListener(this);

        mCustomBrightnessPreference = (SwitchPreference) findPreference(Utils.CUSTOM_AOD_BRIGHTNESS_KEY);
        mCustomBrightnessPreference.setEnabled(dozeEnabled);
        mCustomBrightnessPreference.setOnPreferenceChangeListener(this);

        mBrightnessSeekBar = (SeekBarPreference) findPreference(Utils.CUSTOM_AOD_BRIGHTNESS_SEEKBAR_KEY);
        mBrightnessSeekBar.setDependency(Utils.CUSTOM_AOD_BRIGHTNESS_KEY);
        mBrightnessSeekBar.setShowSeekBarValue(true);
        mBrightnessSeekBar.setMin(1);
        mBrightnessSeekBar.setOnPreferenceChangeListener(this);

        // Hide AOD if not supported and set all its dependents otherwise
        if (!Utils.alwaysOnDisplayAvailable(mActivity)) {
            getPreferenceScreen().removePreference(mAlwaysOnDisplayPreference);
        } else {
            pickupSensorCategory.setDependency(Utils.ALWAYS_ON_DISPLAY);
            mPickUpPreference.setDependency(Utils.GESTURE_RAISE_TO_WAKE_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.doze, container, false);
        ((ViewGroup) view).addView(super.onCreateView(inflater, container, savedInstanceState));
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean dozeEnabled = Utils.isDozeEnabled(mActivity);

        mTextView = view.findViewById(R.id.switch_text);
        mTextView.setText(getString(dozeEnabled ?
                R.string.switch_bar_on : R.string.switch_bar_off));

        mSwitchBar = view.findViewById(R.id.switch_bar);
        Switch switchWidget = mSwitchBar.findViewById(android.R.id.switch_widget);
        switchWidget.setChecked(dozeEnabled);
        switchWidget.setOnCheckedChangeListener(this);
        mSwitchBar.setActivated(dozeEnabled);
        mSwitchBar.setOnClickListener(v -> {
            switchWidget.setChecked(!switchWidget.isChecked());
            mSwitchBar.setActivated(switchWidget.isChecked());
        });
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case Utils.ALWAYS_ON_DISPLAY:
                Utils.enableAlwaysOn(mActivity, (Boolean) newValue);
                break;
            case Utils.GESTURE_RAISE_TO_WAKE_KEY:
                Utils.setPickUp(findPreference(Utils.GESTURE_PICK_UP_KEY), (Boolean) newValue);
                break;
            case Utils.CUSTOM_AOD_BRIGHTNESS_KEY:
                Utils.changeBrightnessMode(getContext(), ((Boolean) newValue).booleanValue());
                break;
            case Utils.CUSTOM_AOD_BRIGHTNESS_SEEKBAR_KEY:
                Utils.updateCustomBrightness(getContext(), ((Integer) newValue).intValue());
        }
        mHandler.post(() -> Utils.checkDozeService(mActivity));
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        Utils.enableDoze(mActivity, isChecked);
        Utils.checkDozeService(mActivity);

        mTextView.setText(getString(isChecked ? R.string.switch_bar_on : R.string.switch_bar_off));
        mSwitchBar.setActivated(isChecked);

        if (!isChecked) {
            Utils.enableAlwaysOn(mActivity, false);
            mAlwaysOnDisplayPreference.setChecked(false);
        }
        mAlwaysOnDisplayPreference.setEnabled(isChecked);

        mPickUpPreference.setEnabled(isChecked);
        mRaiseToWakePreference.setEnabled(isChecked);
        mCustomBrightnessPreference.setEnabled(isChecked);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            mActivity.onBackPressed();
            return true;
        }
        return false;
    }

    private class HelpDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.doze_settings_help_title)
                    .setMessage(R.string.doze_settings_help_text)
                    .setNegativeButton(R.string.dialog_ok, (dialog, which) -> dialog.cancel())
                    .create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            mActivity.getSharedPreferences("doze_settings", Activity.MODE_PRIVATE)
                    .edit()
                    .putBoolean("first_help_shown", true)
                    .commit();
        }
    }

    private void showHelp() {
        new HelpDialogFragment()
            .show(getFragmentManager(), "help_dialog");
    }
}
