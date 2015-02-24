/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.fourmob.datetimepicker.date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
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

import com.fourmob.datetimepicker.Utils;
import com.fourmob.datetimepicker.date.SimpleMonthAdapter.CalendarDay;
import com.nineoldandroids.animation.ObjectAnimator;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.date_time.R;
import de.azapps.tools.Log;

public class DatePicker extends LinearLayout implements View.OnClickListener,
    DatePickerController {
    private static final int MAX_YEAR = 2050;
    private static final int MIN_YEAR = 1902;
    private static final int VIEW_DATE_PICKER_YEAR = 1;
    private static final int VIEW_DATE_PICKER_MONTH_DAY = 0;
    private static final String TAG = "DatePicker";
    // TODO use local from pref...
    // private static SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("dd",
    // Locale.getDefault());
    // private static SimpleDateFormat YEAR_FORMAT = new
    // SimpleDateFormat("yyyy",
    // Locale.getDefault());
    private static DateTimeFormatter DAY_FORMAT = DateTimeFormat
            .forPattern("dd");
    private static DateTimeFormatter YEAR_FORMAT = DateTimeFormat
            .forPattern("yyyy");

    private int mMaxYear;
    private int mMinYear;
    private View layout;
    private TextView mSelectedMonthTextView;
    private TextView mDayOfWeekView;
    private LinearLayout mMonthAndDayView;
    private TextView mSelectedDayTextView;
    private TextView mYearView;
    private Context ctx;
    private String mDayPickerDescription;
    private ViewAnimator mAnimator;
    protected DayPickerView mDayPickerView;
    protected YearPickerView mYearPickerView;
    private String mYearPickerDescription;
    private Button mDoneButton;
    private Button mNoDateButton;
    private boolean yearSelected;
    private Vibrator mVibrator;
    private long mLastVibrate;
    private boolean mVibrate;
    protected Calendar mCalendar;
    private boolean mDelayAnimation;
    private int mCurrentView;
    private int mWeekStart;
    private final DateFormatSymbols dateformartsymbols = new DateFormatSymbols();
    private final HashSet<OnDateChangedListener> mListeners = new HashSet<OnDateChangedListener>();
    protected OnDateSetListener mCallBack;

    public DatePicker(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                             R.styleable.DatePicker, 0, 0);
        this.mCalendar = new GregorianCalendar();
        try {
            setYear(a.getInt(R.styleable.DatePicker_initialYear,
                             this.mCalendar.get(Calendar.YEAR)));
            setMonth(a.getInt(R.styleable.DatePicker_initialMonth,
                              this.mCalendar.get(Calendar.MONTH)));
            setDay(a.getInt(R.styleable.DatePicker_initialDay,
                            this.mCalendar.get(Calendar.DAY_OF_MONTH)));
            this.mMaxYear = a.getInt(R.styleable.DatePicker_maxYear, MAX_YEAR);
            this.mMinYear = a.getInt(R.styleable.DatePicker_minYear, MIN_YEAR);
            Log.w(TAG, "create");
        } finally {
            a.recycle();
        }
        setupView(context);
    }

    private void setupView(final Context context) {
        this.ctx = context;
        this.layout = View.inflate(context, R.layout.date_picker_view, this);
        initLayout();
        updateYearRange();
    }

    void setMaxYear(final int max) {
        if ((max >= this.mMinYear) && (max < MAX_YEAR)) {
            this.mMaxYear = max;
            updateYearRange();
        }
    }

    void setMinYear(final int min) {
        if ((min <= this.mMaxYear) && (min > MIN_YEAR)) {
            this.mMinYear = min;
            updateYearRange();
        }
    }

    private void updateYearRange() {
        if (this.mMaxYear <= this.mMinYear) {
            throw new IllegalArgumentException(
                "Year end must be larger than year start");
        }
        if (this.mMaxYear > MAX_YEAR) {
            throw new IllegalArgumentException("max year end must < "
                                               + MAX_YEAR);
        }
        if (this.mMinYear < MIN_YEAR) {
            throw new IllegalArgumentException("min year end must > "
                                               + MIN_YEAR);
        }
        if (this.mDayPickerView != null) {
            this.mDayPickerView.onChange();
        }
    }

    public void setDay(final int day) {
        this.mCalendar.set(Calendar.DAY_OF_MONTH, day);
        update();
    }

    private void update() {
        if (this.mSelectedMonthTextView != null) {// is init
            updateDisplay();
            this.mDayPickerView.goTo(new CalendarDay(this.mCalendar), false,
                                     true, true);
        }
    }

    public void setMonth(final int month) {
        this.mCalendar.set(Calendar.MONTH, month);
        update();
    }

    public void setYear(final int year) {
        this.mCalendar.set(Calendar.YEAR, year);
        update();
    }

    @SuppressLint("NewApi")
    @Override
    public void onScreenStateChanged(final int screenState) {
        if (Build.VERSION.SDK_INT >= 16) {
            super.onScreenStateChanged(screenState);
        }
    }

    @Override
    public void onClick(final View v) {
        tryVibrate();
        if (v.getId() == R.id.date_picker_year) {
            setCurrentView(VIEW_DATE_PICKER_YEAR);
            this.yearSelected = true;
        } else if (v.getId() == R.id.date_picker_month_and_day) {
            setCurrentView(VIEW_DATE_PICKER_MONTH_DAY);
            this.yearSelected = false;
        }
    }

    @Override
    public void tryVibrate() {
        if (this.mVibrator != null && this.mVibrate) {
            final long timeInMillis = SystemClock.uptimeMillis();
            if (timeInMillis - this.mLastVibrate >= 125L) {
                this.mVibrator.vibrate(5L);
                this.mLastVibrate = timeInMillis;
            }
        }
    }

    private void setCurrentView(final int currentView) {
        setCurrentView(currentView, false);
    }

    private void setCurrentView(final int currentView,
                                final boolean forceRefresh) {
        final long timeInMillis = this.mCalendar.getTimeInMillis();
        switch (currentView) {
        case VIEW_DATE_PICKER_MONTH_DAY:
            final ObjectAnimator monthDayAnim = Utils.getPulseAnimator(
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
            final String monthDayDesc = DateUtils.formatDateTime(this.ctx,
                                        timeInMillis, DateUtils.FORMAT_SHOW_DATE);
            this.mAnimator.setContentDescription(this.mDayPickerDescription
                                                 + ": " + monthDayDesc);
            return;
        case VIEW_DATE_PICKER_YEAR:
            final ObjectAnimator yearAnim = Utils.getPulseAnimator(
                                                this.mYearView, 0.85F, 1.1F);
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
            final String dayDesc = YEAR_FORMAT.print(timeInMillis);// format(Long.valueOf(timeInMillis));
            this.mAnimator.setContentDescription(this.mYearPickerDescription
                                                 + ": " + dayDesc);
            return;
        default:
            return;
        }
    }

    private void initLayout() {
        final View datepicker_dialog = this.layout
                                       .findViewById(R.id.datepicker_dialog);
        this.mDayOfWeekView = (TextView) this.layout
                              .findViewById(R.id.date_picker_header);
        this.mMonthAndDayView = (LinearLayout) this.layout
                                .findViewById(R.id.date_picker_month_and_day);
        this.mMonthAndDayView.setOnClickListener(this);
        this.mSelectedMonthTextView = (TextView) this.layout
                                      .findViewById(R.id.date_picker_month);

        final ColorStateList selectorColorStates = getSelectorColorStates();

        this.mSelectedMonthTextView.setTextColor(selectorColorStates);
        this.mSelectedDayTextView = (TextView) this.layout
                                    .findViewById(R.id.date_picker_day);
        this.mSelectedDayTextView.setTextColor(selectorColorStates);
        this.mYearView = (TextView) this.layout
                         .findViewById(R.id.date_picker_year);
        this.mYearView.setTextColor(selectorColorStates);
        this.mYearView.setOnClickListener(this);


        this.mDayPickerView = new DayPickerView(this.ctx, this);
        this.mYearPickerView = new YearPickerView(this.ctx, this);
        this.mDayPickerDescription = getResources().getString(R.string.day_picker_description);
        this.mYearPickerDescription = getResources().getString(R.string.year_picker_description);
        this.mAnimator = (ViewAnimator) this.layout.findViewById(R.id.animator);
        this.mAnimator.addView(this.mDayPickerView);
        this.mAnimator.addView(this.mYearPickerView);
        final AlphaAnimation inAlphaAnimation = new AlphaAnimation(0.0F, 1.0F);
        inAlphaAnimation.setDuration(300L);
        this.mAnimator.setInAnimation(inAlphaAnimation);
        final AlphaAnimation outAlphaAnimation = new AlphaAnimation(1.0F, 0.0F);
        outAlphaAnimation.setDuration(300L);
        this.mAnimator.setOutAnimation(outAlphaAnimation);
        this.mDoneButton = (Button) this.layout.findViewById(R.id.done);
        this.mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                tryVibrate();
                if (DatePicker.this.mCallBack != null) {
                    DatePicker.this.mCallBack.onDateSet(DatePicker.this,
                                                        DatePicker.this.mCalendar.get(Calendar.YEAR),
                                                        DatePicker.this.mCalendar.get(Calendar.MONTH),
                                                        DatePicker.this.mCalendar
                                                        .get(Calendar.DAY_OF_MONTH));
                }
            }
        });
        this.mNoDateButton = (Button) this.layout.findViewById(R.id.dismiss);
        this.mNoDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                DatePicker.this.tryVibrate();
                if (DatePicker.this.mCallBack != null) {
                    DatePicker.this.mCallBack.onNoDateSet();
                }
            }
        });
        updateDisplay();
        final int currentView = VIEW_DATE_PICKER_MONTH_DAY;
        setCurrentView(currentView, true);
        final int listPosition = -1;
        final int listPositionOffset = 0;
        setScroll(listPosition, currentView, listPositionOffset);
        datepicker_dialog.setBackgroundColor(ThemeManager.getColor(R.attr.colorBackground));
        final View header = this.layout
                            .findViewById(R.id.datepicker_header);
        header.setBackgroundColor(ThemeManager.getAccentThemeColor());
        if (this.mDayOfWeekView != null) {
            this.mDayOfWeekView.setBackgroundColor(ThemeManager.getDarkerShade(
                    ThemeManager.getAccentThemeColor()));
            this.mDayOfWeekView.setTextColor(ThemeManager.getColor(R.attr.colorControlNormal));
        }
        this.mNoDateButton.setTextColor(ThemeManager.getColor(R.attr.colorTextGrey));
        this.mDoneButton.setTextColor(ThemeManager.getColor(R.attr.colorTextGrey));
    }

    public static ColorStateList getSelectorColorStates() {
        final int[][] states = new int[][] {
            new int[] {android.R.attr.state_pressed},
            new int[] {android.R.attr.state_selected},
            new int[]{}
        };

        final int[] colors = new int[] {
            ThemeManager.getPrimaryDarkThemeColor(),
            ThemeManager.getPrimaryThemeColor(),
            ThemeManager.getColor(R.attr.colorControlNormal)
        };

        return new ColorStateList(states, colors);
    }

    private void setScroll(final int listPosition, final int currentView,
                           final int listPositionOffset) {
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
            this.mCalendar.setFirstDayOfWeek(this.mWeekStart);
            this.mDayOfWeekView
            .setText(this.dateformartsymbols.getWeekdays()[this.mCalendar
                     .get(Calendar.DAY_OF_WEEK)].toUpperCase(Locale
                             .getDefault()));
        }
        this.mSelectedMonthTextView
        .setText(this.dateformartsymbols.getMonths()[this.mCalendar
                 .get(Calendar.MONTH)].toUpperCase(Locale.getDefault()));
        this.mSelectedDayTextView.setText(DAY_FORMAT.print(this.mCalendar
                                          .getTimeInMillis()));
        this.mYearView.setText(YEAR_FORMAT.print(this.mCalendar
                               .getTimeInMillis()));
        final long timeInMillis = this.mCalendar.getTimeInMillis();
        final String desc = DateUtils
                            .formatDateTime(this.ctx, timeInMillis, 24);
        this.mMonthAndDayView.setContentDescription(desc);
    }

    static abstract interface OnDateChangedListener {
        public abstract void onDateChanged();
    }

    public static abstract interface OnDateSetListener {
        public abstract void onDateSet(final DatePicker datePickerDialog,
                                       final int year, final int month, final int day);

        public abstract void onNoDateSet();
    }

    @Override
    public void registerOnDateChangedListener(
        final OnDateChangedListener onDateChangedListener) {
        this.mListeners.add(onDateChangedListener);
    }

    @Override
    public int getFirstDayOfWeek() {
        return this.mWeekStart;
    }

    @Override
    public int getMaxYear() {
        return this.mMaxYear;
    }

    @Override
    public int getMinYear() {
        return this.mMinYear;
    }

    public void setOnDateSetListener(final OnDateSetListener dt) {
        this.mCallBack = dt;
    }

    @Override
    public SimpleMonthAdapter.CalendarDay getSelectedDay() {
        return new SimpleMonthAdapter.CalendarDay(this.mCalendar);
    }

    @Override
    public void onDayOfMonthSelected(final int year, final int month,
                                     final int day) {
        this.mCalendar.set(Calendar.YEAR, year);
        this.mCalendar.set(Calendar.MONTH, month);
        this.mCalendar.set(Calendar.DAY_OF_MONTH, day);
        updatePickers();
        updateDisplay();
    }

    @Override
    public void onYearSelected(final int year) {
        adjustDayInMonthIfNeeded(this.mCalendar.get(Calendar.MONTH), year);
        this.mCalendar.set(Calendar.YEAR, year);
        updatePickers();
        setCurrentView(0);
        updateDisplay();
    }

    private void adjustDayInMonthIfNeeded(final int month, final int year) {
        final int currentDay = this.mCalendar.get(Calendar.DAY_OF_MONTH);
        final int day = Utils.getDaysInMonth(month, year);
        if (currentDay > day) {
            this.mCalendar.set(Calendar.DAY_OF_MONTH, day);
        }
    }

    private void updatePickers() {
        final Iterator<OnDateChangedListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onDateChanged();
        }
    }

    @Override
    protected void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // on display rotate reload dialog
        final Parcelable yearState = this.mYearPickerView.onSaveInstanceState();
        final Parcelable monthState = this.mDayPickerView.onSaveInstanceState();
        // layout=inflate(getContext(), R.layout.date_picker_view, this);
        initLayout();
        if (this.yearSelected) {
            setCurrentView(VIEW_DATE_PICKER_YEAR);
        }
        this.mYearPickerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                DatePicker.this.mYearPickerView
                .onRestoreInstanceState(yearState);
            }
        }, 100);
        this.mDayPickerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                DatePicker.this.mDayPickerView
                .onRestoreInstanceState(monthState);
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
        final Bundle b = new Bundle();
        b.putInt(YEAR_KEY, this.mCalendar.get(Calendar.YEAR));
        b.putInt(MONTH_KEY, this.mCalendar.get(Calendar.MONTH));
        b.putInt(DAY_KEY, this.mCalendar.get(Calendar.DAY_OF_MONTH));
        b.putInt(WEEK_START_KEY, this.mWeekStart);
        b.putInt(YEAR_START_KEY, this.mMinYear);
        b.putInt(YEAR_END_KEY, this.mMaxYear);
        b.putInt(CURRENT_VIEW_KEY, this.mCurrentView);
        int mostVisiblePosition = -1;
        if (this.mCurrentView == 0) {
            mostVisiblePosition = this.mDayPickerView.getMostVisiblePosition();
        }
        // ss.list_position= mostVisiblePosition;
        if (this.mCurrentView == 1) {
            mostVisiblePosition = this.mYearPickerView
                                  .getFirstVisiblePosition();
            b.putInt(LIST_OFFSET_KEY,
                     this.mYearPickerView.getFirstPositionOffset());
            // ss.list_position_offset=
            // this.mYearPickerView.getFirstPositionOffset();
        }
        b.putInt(MOST_VISIBLE_POSITION_KEY, mostVisiblePosition);
        return b;
    }

    @Override
    public void onRestoreInstanceState(final Parcelable state) {
        // begin boilerplate code so parent classes can restore state
        if (!(state instanceof Bundle)) {
            super.onRestoreInstanceState(state);
            return;
        }
        // end
        final Bundle b = (Bundle) state;
        this.mWeekStart = b.getInt(WEEK_START_KEY);
        setMinYear(b.getInt(YEAR_START_KEY));
        setMaxYear(b.getInt(YEAR_END_KEY));
        final int currentView = b.getInt(CURRENT_VIEW_KEY);
        setCurrentView(currentView);
        setScroll(b.getInt(MOST_VISIBLE_POSITION_KEY, -1), currentView,
                  b.getInt(LIST_OFFSET_KEY, 0));
        final int year = b.getInt(YEAR_KEY);
        final int month = b.getInt(MONTH_KEY);
        final int day = b.getInt(DAY_KEY);
        setYear(year);
        setMonth(month);
        setDay(day);
    }

    public int getYear() {
        return this.mCalendar.get(Calendar.YEAR);
    }

    public int getMonth() {
        return this.mCalendar.get(Calendar.MONTH);
    }

    public int getDay() {
        return this.mCalendar.get(Calendar.DAY_OF_MONTH);
    }

    public void hideNoDate() {
        this.mNoDateButton.setVisibility(View.GONE);
    }

}
