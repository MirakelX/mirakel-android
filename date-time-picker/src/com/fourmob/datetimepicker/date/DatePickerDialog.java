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

import de.azapps.mirakel.date_time.R;


public class DatePickerDialog extends DialogFragment {

	protected DatePicker mDatePicker;
	private OnDateSetListener mCallback;
	protected int mInitYear;
	protected  int mInitMonth;
	protected  int mInitDay;
	private boolean mHasNoDate;

	public static DatePickerDialog newInstance(
			OnDateSetListener onDateSetListener, int year, int month, int day,
			boolean dark,boolean hasNoDate) {
		return newInstance(onDateSetListener, year, month, day, true, dark,hasNoDate);
	}

	public static DatePickerDialog newInstance(
			OnDateSetListener onDateSetListener, int year, int month, int day,
			boolean vibrate, boolean dark,boolean hasNoDate) {
		DatePickerDialog datePickerDialog = new DatePickerDialog();
		datePickerDialog.initialize(onDateSetListener, year, month, day,
				vibrate, dark,hasNoDate);
		return datePickerDialog;
	}

	public void setVibrate(boolean vibrate) {
		//nothing
	}

	public void initialize(final OnDateSetListener onDateSetListener, int year,
			int month, int day, boolean vibrate, boolean dark, boolean hasNoDate) {
		this.mCallback = new OnDateSetListener() {

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
		this.mInitYear=year;
		this.mInitMonth=month;
		this.mInitDay=day;
		this.mHasNoDate=hasNoDate;
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		Activity activity = getActivity();
		activity.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		final Bundle b = (Bundle) this.mDatePicker.onSaveInstanceState();
		getDialog().setContentView(
				onCreateView(LayoutInflater.from(getDialog().getContext()),
						null, b));
		this.mDatePicker.postDelayed(new Runnable() {
			@Override
			public void run() {
				DatePickerDialog.this.mDatePicker.onRestoreInstanceState(b);
			}
		}, 50);

	}

	@Override
	public View onCreateView(LayoutInflater layoutInflater, ViewGroup parent,
			Bundle bundle) {
		Log.d("DatePickerDialog", "onCreateView: ");
		try {
			getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		} catch (Exception e) {
			e.printStackTrace();
		}
		View view = layoutInflater.inflate(R.layout.date_picker_dialog, null);
		this.mDatePicker = (DatePicker) view.findViewById(R.id.date_picker);
		this.mDatePicker.setOnDateSetListener(this.mCallback);
		this.mDatePicker.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				DatePickerDialog.this.mDatePicker.setYear(DatePickerDialog.this.mInitYear);
				DatePickerDialog.this.mDatePicker.setMonth(DatePickerDialog.this.mInitMonth);
				DatePickerDialog.this.mDatePicker.setDay(DatePickerDialog.this.mInitDay);				
			}
		}, 0);
		if(!this.mHasNoDate)
			this.mDatePicker.hideNoDate();
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		bundle.putAll((Bundle) this.mDatePicker.onSaveInstanceState());
	}

}