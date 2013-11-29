package com.fourmob.datetimepicker.date;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.fourmob.datetimepicker.Utils;
import com.fourmob.datetimepicker.date.DatePicker.OnDateSetListener;

import de.azapps.mirakelandroid.R;

public class DatePickerDialog extends DialogFragment {
	// https://code.google.com/p/android/issues/detail?id=13050
	private static final int MAX_YEAR = 2037;
	private static final int MIN_YEAR = 1902;


	private final Calendar mCalendar = Calendar.getInstance();
	private int mCurrentView = -1;
	private DayPickerView mDayPickerView;
	private int mMaxYear = MAX_YEAR;
	private int mMinYear = MIN_YEAR;
	private int mWeekStart = this.mCalendar.getFirstDayOfWeek();
	private YearPickerView mYearPickerView;
	private DatePicker mDatePicker;
	private OnDateSetListener mCallback;



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
		if (year > MAX_YEAR)
			throw new IllegalArgumentException("year end must < " + MAX_YEAR);
		if (year < MIN_YEAR)
			throw new IllegalArgumentException("year end must > " + MIN_YEAR);
		this.mCalendar.set(Calendar.YEAR, year);
		this.mCalendar.set(Calendar.MONTH, month);
		this.mCalendar.set(Calendar.DAY_OF_MONTH, day);
		mCallback=new OnDateSetListener() {
			
			@Override
			public void onNoDateSet() {
				if(onDateSetListener!=null)
					onDateSetListener.onNoDateSet();
				dismiss();
				
			}
			
			@Override
			public void onDateSet(DatePicker datePickerDialog, int year, int month,
					int day) {
				if(onDateSetListener!=null)
					onDateSetListener.onDateSet(datePickerDialog, year, month, day);
				dismiss();
				
			}
		};
	}

	

	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		Activity activity = getActivity();
		activity.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (bundle != null) {
			this.mCalendar.set(Calendar.YEAR, bundle.getInt("year"));
			this.mCalendar.set(Calendar.MONTH, bundle.getInt("month"));
			this.mCalendar.set(Calendar.DAY_OF_MONTH, bundle.getInt("day"));
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if(mDatePicker!=null){
			mDatePicker.invalidate();
		}
		//on display rotate reload dialog
		/*final Parcelable yearState = mYearPickerView.onSaveInstanceState();
		final Parcelable monthState = mDayPickerView.onSaveInstanceState();
		getDialog().setContentView(onCreateView(LayoutInflater.from(getDialog().getContext()), null, null));
		if(yearSelected)
			setCurrentView(VIEW_DATE_PICKER_YEAR);
		mYearPickerView.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				mYearPickerView.onRestoreInstanceState(yearState);
			}
		}, 100);
		mDayPickerView.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				mDayPickerView.onRestoreInstanceState(monthState);
			}
		}, 100);*/
	}

	public View onCreateView(LayoutInflater layoutInflater, ViewGroup parent,
			Bundle bundle) {
		Log.d("DatePickerDialog", "onCreateView: ");
		try {
			getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		View view = layoutInflater.inflate(R.layout.date_picker_dialog, null);
		mDatePicker =(DatePicker) view.findViewById(R.id.date_picker);		
		mDatePicker.setOnDateSetListener(mCallback);
		return view;
	}


	public void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		bundle.putInt("year", this.mCalendar.get(Calendar.YEAR));
		bundle.putInt("month", this.mCalendar.get(Calendar.MONTH));
		bundle.putInt("day", this.mCalendar.get(Calendar.DAY_OF_MONTH));
		bundle.putInt("week_start", this.mWeekStart);
		bundle.putInt("year_start", this.mMinYear);
		bundle.putInt("year_end", this.mMaxYear);
		bundle.putInt("current_view", this.mCurrentView);
		int mostVisiblePosition = -1;
		if (this.mCurrentView == 0)
			mostVisiblePosition = this.mDayPickerView.getMostVisiblePosition();
		bundle.putInt("list_position", mostVisiblePosition);
		if (this.mCurrentView == 1) {
			mostVisiblePosition = this.mYearPickerView
					.getFirstVisiblePosition();
			bundle.putInt("list_position_offset",
					this.mYearPickerView.getFirstPositionOffset());
		}
	}


}