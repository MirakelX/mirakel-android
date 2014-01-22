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

import de.azapps.mirakelandroid.R;

public class DateTimeDialog extends DialogFragment {

	protected static final String	TAG	= "DateTimeDialog";

	public static DateTimeDialog newInstance(OnDateTimeSetListner callback, int year, int month, int dayOfMonth, int hourOfDay, int minute, boolean vibrate, boolean dark) {
		DateTimeDialog dt = new DateTimeDialog();
		dt.init(year, month, dayOfMonth, hourOfDay, minute);
		dt.setOnDateTimeSetListner(callback);
		// dt.initialize(callback, year, month, dayOfMonth, hourOfDay, minute,
		// vibrate, dark);
		return dt;
	}

	private int	mInitialYear;
	private int	mInitialMonth;
	private int	mInitialDay;
	private int	mInitialHour;
	private int	mInitialMinute;

	private void init(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
		mInitialYear = year;
		mInitialMonth = month;
		mInitialDay = dayOfMonth;
		mInitialHour = hourOfDay;
		mInitialMinute = minute;

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	float							startX;
	float							startY;
	private ViewSwitcher			viewSwitcher;
	private TimePicker				tp;
	private DatePicker				dp;
	private boolean					isCurrentDatepicker	= true;
	private OnDateTimeSetListner	mCallback;

	void setOnDateTimeSetListner(OnDateTimeSetListner listner) {
		mCallback = listner;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
		viewSwitcher = (ViewSwitcher) v
				.findViewById(R.id.datetime_picker_animator);
		dp = (DatePicker) v.findViewById(R.id.date_picker);
		tp = (TimePicker) v.findViewById(R.id.time_picker);
		tp.set24HourMode(true);
		tp.setTime(mInitialHour, mInitialMinute);
		tp.setOnKeyListener(tp.getNewKeyboardListner(getDialog()));
		tp.setOnTimeSetListener(new OnTimeSetListener() {

			@Override
			public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
				if (mCallback != null) {
					mCallback.onDateTimeSet(dp.getYear(), dp.getMonth(),
							dp.getDay(), hourOfDay, minute);
				}
				dismiss();

			}

			@Override
			public void onNoTimeSet() {
				if (mCallback != null) {
					mCallback.onNoTimeSet();
				}
				dismiss();

			}
		});

		dp.setOnDateSetListener(new OnDateSetListener() {

			@Override
			public void onNoDateSet() {
				if (mCallback != null) {
					mCallback.onNoTimeSet();
				}
				dismiss();

			}

			@Override
			public void onDateSet(DatePicker datePickerDialog, int year, int month, int day) {
				if (mCallback != null) {
					mCallback.onDateTimeSet(year, month, day, tp.getHour(),
							tp.getMinute());
				}
				dismiss();

			}
		});

		switchToDate.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (!isCurrentDatepicker) {
					viewSwitcher.showPrevious();
					isCurrentDatepicker = true;
				}
			}
		});

		switchToTime.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (isCurrentDatepicker) {
					viewSwitcher.showNext();
					isCurrentDatepicker = false;

				}
			}
		});
		dp.setYear(mInitialYear);
		dp.setMonth(mInitialMonth);
		dp.setDay(mInitialDay);
		tp.setHour(mInitialHour, false);
		tp.setMinute(mInitialMinute);
		return v;

	}

	public interface OnDateTimeSetListner {

		/**
		 * @param view
		 *        The view associated with this listener.
		 * @param hourOfDay
		 *        The hour that was set.
		 * @param minute
		 *        The minute that was set.
		 */
		void onDateTimeSet(int year, int month, int dayOfMonth, int hourOfDay, int minute);

		void onNoTimeSet();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Bundle time = (Bundle) tp.onSaveInstanceState();
		Bundle date = (Bundle) dp.onSaveInstanceState();
		getDialog().setContentView(
				onCreateView(LayoutInflater.from(getDialog().getContext()),
						null, null));
		if (isCurrentDatepicker
				&& viewSwitcher.getCurrentView().getId() != R.id.date_picker) {
			viewSwitcher.showPrevious();
		} else if (!isCurrentDatepicker
				&& viewSwitcher.getCurrentView().getId() != R.id.time_picker) {
			viewSwitcher.showNext();
		}
		dp.onRestoreInstanceState(date);
		tp.onRestoreInstanceState(time);
	}

}
