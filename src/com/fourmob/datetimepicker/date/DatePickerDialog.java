package com.fourmob.datetimepicker.date;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.fourmob.datetimepicker.date.DatePicker.OnDateSetListener;

import de.azapps.mirakelandroid.R;

public class DatePickerDialog extends DialogFragment {

	private DatePicker mDatePicker;
	private OnDateSetListener mCallback;
	private int mInitYear;
	private int mInitMonth;
	private int mInitDay;

	public static DatePickerDialog newInstance(
			OnDateSetListener onDateSetListener, int year, int month, int day,
			boolean dark) {
		return newInstance(onDateSetListener, year, month, day, true, dark);
	}

	public static DatePickerDialog newInstance(
			OnDateSetListener onDateSetListener, int year, int month, int day,
			boolean vibrate, boolean dark) {
		DatePickerDialog datePickerDialog = new DatePickerDialog();
		datePickerDialog.initialize(onDateSetListener, year, month, day,
				vibrate, true);
		return datePickerDialog;
	}

	public void setVibrate(boolean vibrate) {
	}

	public void initialize(final OnDateSetListener onDateSetListener, int year,
			int month, int day, boolean vibrate, boolean dark) {
		mCallback = new OnDateSetListener() {

			@Override
			public void onNoDateSet() {
				if (onDateSetListener != null)
					onDateSetListener.onNoDateSet();
				dismiss();

			}

			@Override
			public void onDateSet(DatePicker datePickerDialog, int year,
					int month, int day) {
				if (onDateSetListener != null)
					onDateSetListener.onDateSet(datePickerDialog, year, month,
							day);
				dismiss();

			}
		};
		mInitYear=year;
		mInitMonth=month;
		mInitDay=day;
		
	}

	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		Activity activity = getActivity();
		activity.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		final Bundle b = (Bundle) mDatePicker.onSaveInstanceState();
		getDialog().setContentView(
				onCreateView(LayoutInflater.from(getDialog().getContext()),
						null, b));
		mDatePicker.postDelayed(new Runnable() {
			@Override
			public void run() {
				mDatePicker.onRestoreInstanceState(b);
			}
		}, 50);

	}

	public View onCreateView(LayoutInflater layoutInflater, ViewGroup parent,
			Bundle bundle) {
		Log.d("DatePickerDialog", "onCreateView: ");
		try {
			getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		View view = layoutInflater.inflate(R.layout.date_picker_dialog, null);
		mDatePicker = (DatePicker) view.findViewById(R.id.date_picker);
		mDatePicker.setOnDateSetListener(mCallback);
		mDatePicker.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				mDatePicker.setYear(mInitYear);
				mDatePicker.setMonth(mInitMonth);
				mDatePicker.setDay(mInitDay);				
			}
		}, 0);

		return view;
	}

	public void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		bundle.putAll((Bundle) mDatePicker.onSaveInstanceState());
	}

}