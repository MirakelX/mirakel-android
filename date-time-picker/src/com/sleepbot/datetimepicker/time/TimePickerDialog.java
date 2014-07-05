package com.sleepbot.datetimepicker.time;

/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.sleepbot.datetimepicker.time.TimePicker.OnTimeSetListener;

import de.azapps.mirakel.date_time.R;

/**
 * Dialog to set a time.
 */
@SuppressLint("ValidFragment")
public class TimePickerDialog extends DialogFragment {

    private TimePicker mTimePicker;

    private OnTimeSetListener mCallback;

    private int mInitialHour;

    private int mInitialMinute;

    /**
     * The callback interface used to indicate the user is done filling in the
     * time (they clicked on the 'Set' button).
     */

    public TimePickerDialog() {
        // Empty constructor required for dialog fragment.
    }

    public TimePickerDialog(final Context context, final int theme,
                            final OnTimeSetListener callback, final int hourOfDay,
                            final int minute, final boolean is24HourMode, final boolean dark) {
        // Empty constructor required for dialog fragment.
    }

    public static TimePickerDialog newInstance(
        final OnTimeSetListener callback, final int hourOfDay,
        final int minute, final boolean is24HourMode, final boolean dark) {
        final TimePickerDialog ret = new TimePickerDialog();
        ret.initialize(callback, hourOfDay, minute, is24HourMode, dark);
        return ret;
    }

    public void initialize(final OnTimeSetListener callback,
                           final int hourOfDay, final int minute, final boolean is24HourMode,
                           final boolean dark) {
        this.mInitialHour = hourOfDay;
        this.mInitialMinute = minute;
        this.mCallback = new OnTimeSetListener() {
            @Override
            public void onTimeSet(final RadialPickerLayout view,
                                  final int hourOfDay, final int minute) {
                if (callback != null) {
                    callback.onTimeSet(view, hourOfDay, minute);
                }
                dismiss();
            }
            @Override
            public void onNoTimeSet() {
                if (callback != null) {
                    callback.onNoTimeSet();
                }
                dismiss();
            }
        };
    }

    public void setOnTimeSetListener(final OnTimeSetListener callback) {
        this.mCallback = callback;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final Bundle b = (Bundle) this.mTimePicker.onSaveInstanceState();
        // setupValues(saved);
        getDialog().setContentView(
            onCreateView(LayoutInflater.from(getDialog().getContext()),
                         null, b));
        this.mTimePicker.postDelayed(new Runnable() {
            @Override
            public void run() {
                TimePickerDialog.this.mTimePicker.onRestoreInstanceState(b);
            }
        }, 0);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container, final Bundle savedInstanceState) {
        try {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        final View view = inflater.inflate(R.layout.time_picker_dialog, null);
        this.mTimePicker = (TimePicker) view.findViewById(R.id.time_picker);
        this.mTimePicker.setOnTimeSetListener(this.mCallback);
        this.mTimePicker.setOnKeyListener(this.mTimePicker
                                          .getNewKeyboardListner(getDialog()));
        this.mTimePicker.setTime(this.mInitialHour, this.mInitialMinute);
        return view;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        // if (mTimePicker != null) {
        // outState.putInt(KEY_HOUR_OF_DAY, mTimePicker.getHours());
        // outState.putInt(KEY_MINUTE, mTimePicker.getMinutes());
        // outState.putBoolean(KEY_IS_24_HOUR_VIEW, mIs24HourMode);
        // outState.putInt(KEY_CURRENT_ITEM_SHOWING,
        // mTimePicker.getCurrentItemShowing());
        // outState.putBoolean(KEY_IN_KB_MODE, mInKbMode);
        // if (mInKbMode) {
        // outState.putIntegerArrayList(KEY_TYPED_TIMES, mTypedTimes);
        // }
        // }
    }

}