package com.fourmob.datetimepicker.date;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import com.fourmob.datetimepicker.Utils;
import com.fourmob.datetimepicker.date.DayPickerView;
import com.fourmob.datetimepicker.date.SimpleMonthAdapter;
import com.fourmob.datetimepicker.date.SimpleMonthAdapter.CalendarDay;
import com.fourmob.datetimepicker.date.YearPickerView;
import com.nineoldandroids.animation.ObjectAnimator;

import de.azapps.mirakel.helper.Log;
import de.azapps.mirakelandroid.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Build;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;
import de.azapps.mirakel.helper.MirakelPreferences;

public class DatePicker extends LinearLayout implements View.OnClickListener,
		DatePickerController {
	private static final int MAX_YEAR = 2037;
	private static final int MIN_YEAR = 1902;
	private static final int VIEW_DATE_PICKER_YEAR = 1;
	private static final int VIEW_DATE_PICKER_MONTH_DAY = 0;
	private static final String TAG = "DatePicker";
	private static SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("dd",
			Locale.getDefault());
	private static SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy",
			Locale.getDefault());

	private int mMaxYear;
	private int mMinYear;
	private View layout;
	private boolean mDark;
	private TextView mSelectedMonthTextView;
	private TextView mDayOfWeekView;
	private LinearLayout mMonthAndDayView;
	private TextView mSelectedDayTextView;
	private TextView mYearView;
	private Context ctx;
	private String mDayPickerDescription;
	private ViewAnimator mAnimator;
	private DayPickerView mDayPickerView;
	private YearPickerView mYearPickerView;
	private String mYearPickerDescription;
	private Button mDoneButton;
	private Button mNoDateButton;
	private boolean yearSelected;
	private Vibrator mVibrator;
	private long mLastVibrate;
	private boolean mVibrate;
	private Calendar mCalendar;
	private boolean mDelayAnimation;
	private int mCurrentView;
	private int mWeekStart;
	private DateFormatSymbols dateformartsymbols = new DateFormatSymbols();
	private HashSet<OnDateChangedListener> mListeners = new HashSet<OnDateChangedListener>();
	protected OnDateSetListener mCallBack;

	public DatePicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
				R.styleable.DatePicker, 0, 0);
		mCalendar = new GregorianCalendar();

		try {
			setYear(a.getInt(R.styleable.DatePicker_initialYear,
					mCalendar.get(Calendar.YEAR)));
			setMonth(a.getInt(R.styleable.DatePicker_initialMonth,
					mCalendar.get(Calendar.MONTH)));
			setDay(a.getInt(R.styleable.DatePicker_initialDay,
					mCalendar.get(Calendar.DAY_OF_MONTH)));

			mMaxYear = a.getInt(R.styleable.DatePicker_maxYear, MAX_YEAR);
			mMinYear = a.getInt(R.styleable.DatePicker_minYear, MIN_YEAR);
			Log.w(TAG, "create");
		} finally {
			a.recycle();
		}
		setupView(context);

	}

	private void setupView(Context context) {
		mDark=MirakelPreferences.isDark();//Dirty get Theme or so
		ctx=context;
		layout=View.inflate(context, R.layout.date_picker_view, this);
		initLayout();
		updateYearRange();
	}

	void setMaxYear(int max) {
		if (max >= mMinYear && max < MAX_YEAR) {
			mMaxYear = max;
			updateYearRange();
		}
	}

	void setMinYear(int min) {
		if (min <= mMaxYear && min > MIN_YEAR) {
			mMinYear = min;
			updateYearRange();
		}
	}

	private void updateYearRange() {
		if (mMaxYear <= mMinYear)
			throw new IllegalArgumentException(
					"Year end must be larger than year start");
		if (mMaxYear > MAX_YEAR)
			throw new IllegalArgumentException("max year end must < "
					+ MAX_YEAR);
		if (mMinYear < MIN_YEAR)
			throw new IllegalArgumentException("min year end must > "
					+ MIN_YEAR);
		if (this.mDayPickerView != null)
			this.mDayPickerView.onChange();
	}

	public void setDay(int day) {
		Log.d(TAG, "SET DAY: " + day);
		mCalendar.set(Calendar.DAY_OF_MONTH, day);
		update();
	}

	private void update() {
		if (mSelectedMonthTextView != null) {// is init
			updateDisplay();
			mDayPickerView.goTo(new CalendarDay(mCalendar), false, true, true);
		}
	}

	public void setMonth(int month) {
		Log.d(TAG, "SET MONTH: " + month);
		mCalendar.set(Calendar.MONTH, month);
		update();
	}

	public void setYear(int year) {
		Log.d(TAG, "SET YEAR: " + year);
		mCalendar.set(Calendar.YEAR, year);
		update();
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onScreenStateChanged(int screenState) {
		if (Build.VERSION.SDK_INT >= 16)
			super.onScreenStateChanged(screenState);
	}

	public void onClick(View view) {
		tryVibrate();
		if (view.getId() == R.id.date_picker_year) {
			setCurrentView(VIEW_DATE_PICKER_YEAR);
			yearSelected = true;
		} else if (view.getId() == R.id.date_picker_month_and_day) {
			setCurrentView(VIEW_DATE_PICKER_MONTH_DAY);
			yearSelected = false;
		}
	}

	public void tryVibrate() {
		if (this.mVibrator != null && this.mVibrate) {
			long timeInMillis = SystemClock.uptimeMillis();
			if (timeInMillis - this.mLastVibrate >= 125L) {
				this.mVibrator.vibrate(5L);
				this.mLastVibrate = timeInMillis;
			}
		}
	}

	private void setCurrentView(int currentView) {
		setCurrentView(currentView, false);
	}

	private void setCurrentView(int currentView, boolean forceRefresh) {
		long timeInMillis = this.mCalendar.getTimeInMillis();
		switch (currentView) {
		case VIEW_DATE_PICKER_MONTH_DAY:
			ObjectAnimator monthDayAnim = Utils.getPulseAnimator(
					this.mMonthAndDayView, 0.9F, 1.05F);
			if (this.mDelayAnimation) {
				monthDayAnim.setStartDelay(500L);
				this.mDelayAnimation = false;
			}
			this.mDayPickerView.onDateChanged();
			if (this.mCurrentView != currentView || forceRefresh) {
				this.mMonthAndDayView.setSelected(true);
				this.mYearView.setSelected(false);
				this.mAnimator.setDisplayedChild(VIEW_DATE_PICKER_MONTH_DAY);
				this.mCurrentView = currentView;
			}
			monthDayAnim.start();
			String monthDayDesc = DateUtils.formatDateTime(ctx, timeInMillis,
					DateUtils.FORMAT_SHOW_DATE);
			this.mAnimator.setContentDescription(this.mDayPickerDescription
					+ ": " + monthDayDesc);
			return;
		case VIEW_DATE_PICKER_YEAR:
			ObjectAnimator yearAnim = Utils.getPulseAnimator(this.mYearView,
					0.85F, 1.1F);
			if (this.mDelayAnimation) {
				yearAnim.setStartDelay(500L);
				this.mDelayAnimation = false;
			}
			this.mYearPickerView.onDateChanged();
			if (this.mCurrentView != currentView || forceRefresh) {
				this.mMonthAndDayView.setSelected(false);
				this.mYearView.setSelected(true);
				this.mAnimator.setDisplayedChild(VIEW_DATE_PICKER_YEAR);
				this.mCurrentView = currentView;
			}
			yearAnim.start();
			String dayDesc = YEAR_FORMAT.format(Long.valueOf(timeInMillis));
			this.mAnimator.setContentDescription(this.mYearPickerDescription
					+ ": " + dayDesc);
		}

	}

	private void initLayout() {
		this.mDayOfWeekView = ((TextView) layout
				.findViewById(R.id.date_picker_header));
		this.mMonthAndDayView = ((LinearLayout) layout
				.findViewById(R.id.date_picker_month_and_day));
		this.mMonthAndDayView.setOnClickListener(this);
		this.mSelectedMonthTextView = ((TextView) layout
				.findViewById(R.id.date_picker_month));
		this.mSelectedMonthTextView.setTextColor(getResources()
				.getColorStateList(
						mDark ? R.color.date_picker_selector_dark
								: R.color.date_picker_selector));
		this.mSelectedDayTextView = ((TextView) layout
				.findViewById(R.id.date_picker_day));
		this.mSelectedDayTextView.setTextColor(getResources()
				.getColorStateList(
						mDark ? R.color.date_picker_selector_dark
								: R.color.date_picker_selector));
		this.mYearView = ((TextView) layout.findViewById(R.id.date_picker_year));
		this.mYearView.setTextColor(getResources().getColorStateList(
				mDark ? R.color.date_picker_selector_dark
						: R.color.date_picker_selector));
		this.mYearView.setOnClickListener(this);
		int listPosition = -1;
		int currentView = VIEW_DATE_PICKER_MONTH_DAY;
		int listPositionOffset = 0;
		/*
		 * if (bundle != null) { this.mWeekStart = bundle.getInt("week_start");
		 * this.mMinYear = bundle.getInt("year_start"); this.mMaxYear =
		 * bundle.getInt("year_end"); currentView =
		 * bundle.getInt("current_view"); listPosition =
		 * bundle.getInt("list_position"); listPositionOffset =
		 * bundle.getInt("list_position_offset"); }
		 */
		this.mDayPickerView = new DayPickerView(ctx, this);
		this.mYearPickerView = new YearPickerView(ctx, this);
		Resources resources = getResources();
		this.mDayPickerDescription = resources
				.getString(R.string.day_picker_description);
		// this.mSelectDay = resources.getString(R.string.select_day);
		this.mYearPickerDescription = resources
				.getString(R.string.year_picker_description);
		// this.mSelectYear = resources.getString(R.string.select_year);
		this.mAnimator = ((ViewAnimator) layout.findViewById(R.id.animator));
		this.mAnimator.addView(this.mDayPickerView);
		this.mAnimator.addView(this.mYearPickerView);
		// this.mAnimator.setDateMillis(this.mCalendar.getTimeInMillis());
		AlphaAnimation inAlphaAnimation = new AlphaAnimation(0.0F, 1.0F);
		inAlphaAnimation.setDuration(300L);
		this.mAnimator.setInAnimation(inAlphaAnimation);
		AlphaAnimation outAlphaAnimation = new AlphaAnimation(1.0F, 0.0F);
		outAlphaAnimation.setDuration(300L);
		this.mAnimator.setOutAnimation(outAlphaAnimation);
		this.mDoneButton = ((Button) layout.findViewById(R.id.done));
		this.mDoneButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				tryVibrate();
				if (mCallBack != null)
					mCallBack.onDateSet(DatePicker.this,
							DatePicker.this.mCalendar.get(Calendar.YEAR),
							DatePicker.this.mCalendar.get(Calendar.MONTH),
							DatePicker.this.mCalendar
									.get(Calendar.DAY_OF_MONTH));
			}
		});
		this.mNoDateButton = ((Button) layout.findViewById(R.id.dismiss));
		this.mNoDateButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				DatePicker.this.tryVibrate();
				if (DatePicker.this.mCallBack != null)
					DatePicker.this.mCallBack.onNoDateSet();
			}
		});
		updateDisplay();
		setCurrentView(currentView, true);

		setScroll(listPosition, currentView, listPositionOffset);
		Resources res = getResources();
		if (mDark) {
			View dialog = layout.findViewById(R.id.datepicker_dialog);
			dialog.setBackgroundColor(res.getColor(R.color.dialog_gray));
			View header = layout.findViewById(R.id.datepicker_header);
			header.setBackgroundColor(res.getColor(R.color.dialog_dark_gray));

			if (mDayOfWeekView != null) {
				mDayOfWeekView.setBackgroundColor(res
						.getColor(R.color.clock_gray));
			}

			mNoDateButton.setTextColor(res.getColor(R.color.clock_white));
			mDoneButton.setTextColor(res.getColor(R.color.clock_white));
		} else {
			View header = layout.findViewById(R.id.datepicker_header);
			header.setBackgroundColor(res.getColor(R.color.white));
		}

	}

	private void setScroll(int listPosition, int currentView,
			int listPositionOffset) {
		if (listPosition != -1) {
			if (currentView == VIEW_DATE_PICKER_MONTH_DAY) {
				this.mDayPickerView.postSetSelection(listPosition);
			}
			if (currentView == VIEW_DATE_PICKER_YEAR) {
				this.mYearPickerView.postSetSelectionFromTop(listPosition,
						listPositionOffset);
			}
		}
	}

	private void updateDisplay() {
		if (this.mDayOfWeekView != null) {
			this.mCalendar.setFirstDayOfWeek(mWeekStart);
			this.mDayOfWeekView
					.setText(dateformartsymbols.getWeekdays()[this.mCalendar
							.get(Calendar.DAY_OF_WEEK)].toUpperCase(Locale
							.getDefault()));
		}

		this.mSelectedMonthTextView
				.setText(dateformartsymbols.getMonths()[this.mCalendar
						.get(Calendar.MONTH)].toUpperCase(Locale.getDefault()));
		this.mSelectedDayTextView.setText(DAY_FORMAT.format(this.mCalendar
				.getTime()));
		this.mYearView.setText(YEAR_FORMAT.format(this.mCalendar.getTime()));
		long timeInMillis = this.mCalendar.getTimeInMillis();
		String desc = DateUtils.formatDateTime(ctx, timeInMillis, 24);
		this.mMonthAndDayView.setContentDescription(desc);
	}


	static abstract interface OnDateChangedListener {
		public abstract void onDateChanged();
	}

	public static abstract interface OnDateSetListener {
		public abstract void onDateSet(DatePicker datePickerDialog, int year,
				int month, int day);

		public abstract void onNoDateSet();
	}

	@Override
	public void registerOnDateChangedListener(
			OnDateChangedListener onDateChangedListener) {
		this.mListeners.add(onDateChangedListener);
	}

	@Override
	public int getFirstDayOfWeek() {
		return this.mWeekStart;
	}

	public int getMaxYear() {
		return this.mMaxYear;
	}

	public int getMinYear() {
		return this.mMinYear;
	}

	public void setOnDateSetListener(OnDateSetListener dt) {
		mCallBack = dt;
	}

	@Override
	public SimpleMonthAdapter.CalendarDay getSelectedDay() {
		return new SimpleMonthAdapter.CalendarDay(this.mCalendar);
	}

	public void onDayOfMonthSelected(int year, int month, int day) {
		this.mCalendar.set(Calendar.YEAR, year);
		this.mCalendar.set(Calendar.MONTH, month);
		this.mCalendar.set(Calendar.DAY_OF_MONTH, day);
		updatePickers();
		updateDisplay();
	}

	@Override
	public void onYearSelected(int year) {
		adjustDayInMonthIfNeeded(this.mCalendar.get(Calendar.MONTH), year);
		this.mCalendar.set(Calendar.YEAR, year);
		updatePickers();
		setCurrentView(0);
		updateDisplay();
	}

	private void adjustDayInMonthIfNeeded(int month, int year) {
		int currentDay = this.mCalendar.get(Calendar.DAY_OF_MONTH);
		int day = Utils.getDaysInMonth(month, year);
		if (currentDay > day)
			this.mCalendar.set(Calendar.DAY_OF_MONTH, day);
	}

	private void updatePickers() {
		Iterator<OnDateChangedListener> it = this.mListeners.iterator();
		while (it.hasNext())
			it.next().onDateChanged();
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d("foo", "config changed");
		// on display rotate reload dialog
		final Parcelable yearState = mYearPickerView.onSaveInstanceState();
		final Parcelable monthState = mDayPickerView.onSaveInstanceState();
		// layout=inflate(getContext(), R.layout.date_picker_view, this);
		initLayout();
		if (yearSelected)
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
		}, 100);
	}

	private static final String YEAR_KEY = "year";
	private static final String MONTH_KEY = "month";
	private static final String DAY_KEY = "day";
	private static final String WEEK_START_KEY = "week_start";
	private static final String YEAR_START_KEY = "minYear";
	private static final String YEAR_END_KEY = "maxYear";
	private static final String CURRENT_VIEW_KEY = "currentView";
	private static final String MOST_VISIBLE_POSITION_KEY = "mostVisiblePosition";
	private static final String LIST_OFFSET_KEY = "listOffset";

	@Override
	public Parcelable onSaveInstanceState() {
		super.onSaveInstanceState();
		// end
		Bundle b = new Bundle();
		b.putInt(YEAR_KEY, this.mCalendar.get(Calendar.YEAR));
		b.putInt(MONTH_KEY, this.mCalendar.get(Calendar.MONTH));
		b.putInt(DAY_KEY, this.mCalendar.get(Calendar.DAY_OF_MONTH));
		b.putInt(WEEK_START_KEY, mWeekStart);
		b.putInt(YEAR_START_KEY, mMinYear);
		b.putInt(YEAR_END_KEY, mMaxYear);
		b.putInt(CURRENT_VIEW_KEY, mCurrentView);

		int mostVisiblePosition = -1;
		if (this.mCurrentView == 0)
			mostVisiblePosition = this.mDayPickerView.getMostVisiblePosition();
		// ss.list_position= mostVisiblePosition;
		if (this.mCurrentView == 1) {
			mostVisiblePosition = this.mYearPickerView
					.getFirstVisiblePosition();
			b.putInt(LIST_OFFSET_KEY, mYearPickerView.getFirstPositionOffset());
			// ss.list_position_offset=
			// this.mYearPickerView.getFirstPositionOffset();
		}
		b.putInt(MOST_VISIBLE_POSITION_KEY, mostVisiblePosition);

		return b;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		// begin boilerplate code so parent classes can restore state
		if (!(state instanceof Bundle)) {
			super.onRestoreInstanceState(state);
			return;
		}
		// end
		final Bundle b = (Bundle) state;

		mWeekStart = b.getInt(WEEK_START_KEY);
		setMinYear(b.getInt(YEAR_START_KEY));
		setMaxYear(b.getInt(YEAR_END_KEY));
		int currentView = b.getInt(CURRENT_VIEW_KEY);
		setCurrentView(currentView);
		setScroll(b.getInt(MOST_VISIBLE_POSITION_KEY, -1), currentView,
				b.getInt(LIST_OFFSET_KEY, 0));
		int year=b.getInt(YEAR_KEY);
		int month=b.getInt(MONTH_KEY);
		int day=b.getInt(DAY_KEY);
		setYear(year);
		setMonth(month);
		setDay(day);
	}

	public int getYear() {
		return mCalendar.get(Calendar.YEAR);
	}

	public int getMonth() {
		return mCalendar.get(Calendar.MONTH);
	}

	public int getDay() {
		return mCalendar.get(Calendar.DAY_OF_MONTH);
	}

}
