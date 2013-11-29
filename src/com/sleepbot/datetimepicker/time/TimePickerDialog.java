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

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.method.TransformationMethod;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fourmob.datetimepicker.Utils;
import com.sleepbot.datetimepicker.time.TimePicker.OnTimeSetListener;

import de.azapps.mirakelandroid.R;

/**
 * Dialog to set a time.
 */
@SuppressLint("ValidFragment")
public class TimePickerDialog extends DialogFragment  {
	private static final String KEY_HOUR_OF_DAY = "hour_of_day";
	private static final String KEY_MINUTE = "minute";
	private static final String KEY_IS_24_HOUR_VIEW = "is_24_hour_view";
	private static final String KEY_CURRENT_ITEM_SHOWING = "current_item_showing";
	private static final String KEY_IN_KB_MODE = "in_kb_mode";
	private static final String KEY_TYPED_TIMES = "typed_times";

	public static final int HOUR_INDEX = 0;
	public static final int MINUTE_INDEX = 1;
	// NOT a real index for the purpose of what's showing.
	public static final int AMPM_INDEX = 2;
	// Also NOT a real index, just used for keyboard mode.
	public static final int ENABLE_PICKER_INDEX = 3;
	public static final int AM = 0;
	public static final int PM = 1;

	private TimePicker mTimePicker;

	private OnTimeSetListener mCallback;
	/**
	 * The callback interface used to indicate the user is done filling in the
	 * time (they clicked on the 'Set' button).
	 */


	public TimePickerDialog() {
		// Empty constructor required for dialog fragment.
	}

	public TimePickerDialog(Context context, int theme,
			OnTimeSetListener callback, int hourOfDay, int minute,
			boolean is24HourMode, boolean dark) {
		// Empty constructor required for dialog fragment.
	}

	public static TimePickerDialog newInstance(OnTimeSetListener callback,
			int hourOfDay, int minute, boolean is24HourMode, boolean dark) {
		TimePickerDialog ret = new TimePickerDialog();
		ret.initialize(callback, hourOfDay, minute, is24HourMode, dark);
		return ret;
	}

	public void initialize(final OnTimeSetListener callback, int hourOfDay,
			int minute, boolean is24HourMode, boolean dark) {
		mCallback=new OnTimeSetListener() {
			
			@Override
			public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
				if(callback!=null)
					callback.onTimeSet(view, hourOfDay, minute);
				dismiss();
				
			}
			
			@Override
			public void onNoTimeSet() {
				if(callback!=null)
					callback.onNoTimeSet();
				dismiss();				
			}
		};
	}

	public void setOnTimeSetListener(OnTimeSetListener callback) {
		mCallback=callback;
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupValues(savedInstanceState);
	}

	private void setupValues(Bundle savedInstanceState) {
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(KEY_HOUR_OF_DAY)
				&& savedInstanceState.containsKey(KEY_MINUTE)
				&& savedInstanceState.containsKey(KEY_IS_24_HOUR_VIEW)) {
//			mInitialHourOfDay = savedInstanceState.getInt(KEY_HOUR_OF_DAY);
//			mInitialMinute = savedInstanceState.getInt(KEY_MINUTE);
//			mIs24HourMode = savedInstanceState.getBoolean(KEY_IS_24_HOUR_VIEW);
//			mInKbMode = savedInstanceState.getBoolean(KEY_IN_KB_MODE);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		final Bundle b=(Bundle) mTimePicker.onSaveInstanceState();
//		setupValues(saved);
		getDialog().setContentView(
				onCreateView(LayoutInflater.from(getDialog().getContext()),
						null, b));
		mTimePicker.postDelayed(new Runnable() {
			@Override
			public void run() {
				mTimePicker.onRestoreInstanceState(b);
			}
		}, 0);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		try {
			getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		View view = inflater.inflate(R.layout.time_picker_dialog, null);
		mTimePicker=(TimePicker) view.findViewById(R.id.time_picker);
		mTimePicker.setOnTimeSetListener(mCallback);
		mTimePicker.setOnKeyListener(mTimePicker.getNewKeyboardListner(getDialog()));
		return view;
	}



	@Override
	public void onSaveInstanceState(Bundle outState) {
//		if (mTimePicker != null) {
//			outState.putInt(KEY_HOUR_OF_DAY, mTimePicker.getHours());
//			outState.putInt(KEY_MINUTE, mTimePicker.getMinutes());
//			outState.putBoolean(KEY_IS_24_HOUR_VIEW, mIs24HourMode);
//			outState.putInt(KEY_CURRENT_ITEM_SHOWING,
//					mTimePicker.getCurrentItemShowing());
//			outState.putBoolean(KEY_IN_KB_MODE, mInKbMode);
//			if (mInKbMode) {
//				outState.putIntegerArrayList(KEY_TYPED_TIMES, mTypedTimes);
//			}
//		}
	}


	
}