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
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.google.common.base.Optional;
import com.sleepbot.datetimepicker.time.TimePicker.OnTimeSetListener;

import org.joda.time.LocalTime;

import de.azapps.mirakel.date_time.R;

/**
 * Dialog to set a time.
 */
@SuppressLint("ValidFragment")
public class TimePickerDialog extends DialogFragment {

    private TimePicker mTimePicker;

    private OnTimeSetListener mCallback;

    private LocalTime mInitialTime;

    /**
     * The callback interface used to indicate the user is done filling in the
     * time (they clicked on the 'Set' button).
     */

    public TimePickerDialog() {
        // Empty constructor required for dialog fragment.
    }



    public void initialize(final OnTimeSetListener callback,
                           final @NonNull LocalTime initTime) {
        this.mInitialTime = initTime;
        this.mCallback = new OnTimeSetListener() {
            @Override
            public void onTimeSet(final RadialPickerLayout view, final @NonNull Optional<LocalTime> newTime) {
                if (callback != null) {
                    callback.onTimeSet(view, newTime);
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
        this.mTimePicker.setTime(this.mInitialTime);
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