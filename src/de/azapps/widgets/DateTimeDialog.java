package de.azapps.widgets;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ViewSwitcher;

import com.fourmob.datetimepicker.date.DatePicker;
import com.fourmob.datetimepicker.date.DatePicker.OnDateSetListener;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePicker;
import com.sleepbot.datetimepicker.time.TimePicker.OnTimeSetListener;

import de.azapps.mirakel.date_time.R;

public class DateTimeDialog extends DialogFragment {

	protected static final String TAG = "DateTimeDialog";

	public static DateTimeDialog newInstance(OnDateTimeSetListner callback,
			int year, int month, int dayOfMonth, int hourOfDay, int minute,
			boolean vibrate, boolean dark) {
		DateTimeDialog dt = new DateTimeDialog();
		dt.init(year, month, dayOfMonth, hourOfDay, minute);
		dt.setOnDateTimeSetListner(callback);
		// dt.initialize(callback, year, month, dayOfMonth, hourOfDay, minute,
		// vibrate, dark);
		return dt;
	}

	private int mInitialYear;
	private int mInitialMonth;
	private int mInitialDay;
	private int mInitialHour;
	private int mInitialMinute;

	private void init(int year, int month, int dayOfMonth, int hourOfDay,
			int minute) {
		this.mInitialYear = year;
		this.mInitialMonth = month;
		this.mInitialDay = dayOfMonth;
		this.mInitialHour = hourOfDay;
		this.mInitialMinute = minute;

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	float startX;
	float startY;
	protected ViewSwitcher viewSwitcher;
	protected TimePicker tp;
	protected DatePicker dp;
	protected boolean isCurrentDatepicker = true;
	protected OnDateTimeSetListner mCallback;

	void setOnDateTimeSetListner(OnDateTimeSetListner listner) {
		this.mCallback = listner;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		try {
			getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		View v = inflater.inflate(R.layout.date_time_picker, container);
		Button switchToDate = (Button) v
				.findViewById(R.id.datetime_picker_date);
		Button switchToTime = (Button) v
				.findViewById(R.id.datetime_picker_time);
		this.viewSwitcher = (ViewSwitcher) v
				.findViewById(R.id.datetime_picker_animator);
		this.dp = (DatePicker) v.findViewById(R.id.date_picker);
		this.tp = (TimePicker) v.findViewById(R.id.time_picker);
		this.tp.set24HourMode(true);
		this.tp.setTime(this.mInitialHour, this.mInitialMinute);
		this.tp.setOnKeyListener(this.tp.getNewKeyboardListner(getDialog()));
		this.tp.setOnTimeSetListener(new OnTimeSetListener() {

			@Override
			public void onTimeSet(RadialPickerLayout view, int hourOfDay,
					int minute) {
				if (DateTimeDialog.this.mCallback != null) {
					DateTimeDialog.this.mCallback.onDateTimeSet(
							DateTimeDialog.this.dp.getYear(),
							DateTimeDialog.this.dp.getMonth(),
							DateTimeDialog.this.dp.getDay(), hourOfDay, minute);
				}
				dismiss();

			}

			@Override
			public void onNoTimeSet() {
				if (DateTimeDialog.this.mCallback != null) {
					DateTimeDialog.this.mCallback.onNoTimeSet();
				}
				dismiss();

			}
		});

		this.dp.setOnDateSetListener(new OnDateSetListener() {

			@Override
			public void onNoDateSet() {
				if (DateTimeDialog.this.mCallback != null) {
					DateTimeDialog.this.mCallback.onNoTimeSet();
				}
				dismiss();

			}

			@Override
			public void onDateSet(DatePicker datePickerDialog, int year,
					int month, int day) {
				if (DateTimeDialog.this.mCallback != null) {
					DateTimeDialog.this.mCallback.onDateTimeSet(year, month,
							day, DateTimeDialog.this.tp.getHour(),
							DateTimeDialog.this.tp.getMinute());
				}
				dismiss();

			}
		});

		switchToDate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!DateTimeDialog.this.isCurrentDatepicker) {
					DateTimeDialog.this.viewSwitcher.showPrevious();
					DateTimeDialog.this.isCurrentDatepicker = true;
				}
			}
		});

		switchToTime.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (DateTimeDialog.this.isCurrentDatepicker) {
					DateTimeDialog.this.viewSwitcher.showNext();
					DateTimeDialog.this.isCurrentDatepicker = false;

				}
			}
		});
		this.dp.setYear(this.mInitialYear);
		this.dp.setMonth(this.mInitialMonth);
		this.dp.setDay(this.mInitialDay);
		this.tp.setHour(this.mInitialHour, false);
		this.tp.setMinute(this.mInitialMinute);
		return v;

	}

	public interface OnDateTimeSetListner {

		/**
		 * @param view
		 *            The view associated with this listener.
		 * @param hourOfDay
		 *            The hour that was set.
		 * @param minute
		 *            The minute that was set.
		 */
		void onDateTimeSet(int year, int month, int dayOfMonth, int hourOfDay,
				int minute);

		void onNoTimeSet();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Bundle time = (Bundle) this.tp.onSaveInstanceState();
		Bundle date = (Bundle) this.dp.onSaveInstanceState();
		getDialog().setContentView(
				onCreateView(LayoutInflater.from(getDialog().getContext()),
						null, null));
		if (this.isCurrentDatepicker
				&& this.viewSwitcher.getCurrentView().getId() != R.id.date_picker) {
			this.viewSwitcher.showPrevious();
		} else if (!this.isCurrentDatepicker
				&& this.viewSwitcher.getCurrentView().getId() != R.id.time_picker) {
			this.viewSwitcher.showNext();
		}
		this.dp.onRestoreInstanceState(date);
		this.tp.onRestoreInstanceState(time);
	}

}
