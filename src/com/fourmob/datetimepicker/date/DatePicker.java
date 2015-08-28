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
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.fourmob.datetimepicker.Utils;
import com.google.common.base.Optional;
import com.nineoldandroids.animation.ObjectAnimator;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.util.HashSet;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.date_time.R;
import de.azapps.mirakel.helper.Helpers;

import static com.google.common.base.Optional.of;

public class DatePicker extends LinearLayout implements View.OnClickListener,
    DatePickerController {
    private static final int MAX_YEAR = 2050;
    private static final int MIN_YEAR = 1902;
    private static final int VIEW_DATE_PICKER_YEAR = 1;
    private static final int VIEW_DATE_PICKER_MONTH_DAY = 0;
    private static final String TAG = "DatePicker";
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
    protected LocalDate mCalendar;
    private boolean mDelayAnimation;
    private int mCurrentView;
    private int mWeekStart;
    private final HashSet<OnDateChangedListener> mListeners = new HashSet<>();
    protected OnDateSetListener mCallBack;

    public DatePicker(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                             R.styleable.DatePicker, 0, 0);
        this.mCalendar = new LocalDate();
        try {
            mCalendar = mCalendar.withYear(a.getInt(R.styleable.DatePicker_initialYear,
                                                    this.mCalendar.getYear()));
            mCalendar = mCalendar.withMonthOfYear(a.getInt(R.styleable.DatePicker_initialMonth,
                                                  this.mCalendar.getMonthOfYear()));
            mCalendar = mCalendar.withDayOfMonth(a.getInt(R.styleable.DatePicker_initialDay,
                                                 this.mCalendar.getDayOfMonth()));
            this.mMaxYear = a.getInt(R.styleable.DatePicker_maxYear, MAX_YEAR);
            this.mMinYear = a.getInt(R.styleable.DatePicker_minYear, MIN_YEAR);
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



    @SuppressLint("NewApi")
    @Override
    public void onScreenStateChanged(final int screenState) {
        if (Build.VERSION.SDK_INT >= 16) {
            super.onScreenStateChanged(screenState);
        }
    }

    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.date_picker_year) {
            setCurrentView(VIEW_DATE_PICKER_YEAR);
            this.yearSelected = true;
        } else if (v.getId() == R.id.date_picker_month_and_day) {
            setCurrentView(VIEW_DATE_PICKER_MONTH_DAY);
            this.yearSelected = false;
        }
    }


    private void setCurrentView(final int currentView) {
        setCurrentView(currentView, false);
    }

    private void setCurrentView(final int currentView,
                                final boolean forceRefresh) {
        final long timeInMillis = this.mCalendar.toDateTimeAtStartOfDay().getMillis();
        switch (currentView) {
        case VIEW_DATE_PICKER_MONTH_DAY:
            final ObjectAnimator monthDayAnim = Utils.getPulseAnimator(
                                                    this.mMonthAndDayView, 0.9F, 1.05F);
            if (this.mDelayAnimation) {
                monthDayAnim.setStartDelay(500L);
                this.mDelayAnimation = false;
            }
            this.mDayPickerView.onDateChanged();
            if ((this.mCurrentView != currentView) || forceRefresh) {
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
            if ((this.mCurrentView != currentView) || forceRefresh) {
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
                if (DatePicker.this.mCallBack != null) {
                    DatePicker.this.mCallBack.onDateSet(DatePicker.this, of(mCalendar));
                }
            }
        });
        this.mNoDateButton = (Button) this.layout.findViewById(R.id.dismiss);
        this.mNoDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (DatePicker.this.mCallBack != null) {
                    DatePicker.this.mCallBack.onDateSet(DatePicker.this, Optional.<LocalDate>absent());
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
        header.setBackgroundColor(ThemeManager.getPrimaryThemeColor());
        if (this.mDayOfWeekView != null) {
            this.mDayOfWeekView.setBackgroundColor(ThemeManager.getPrimaryDarkThemeColor());
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
            ThemeManager.getColor(R.attr.colorTextWhite),
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
            mSelectedMonthTextView.setText(new DateTimeFormatterBuilder()
                                           .appendDayOfWeekShortText()
                                           .toFormatter()
                                           .print(mCalendar)
                                           .toUpperCase(Helpers.getLocale(getContext())));
        }
        mSelectedMonthTextView.setText(new DateTimeFormatterBuilder()
                                       .appendMonthOfYearShortText()
                                       .toFormatter()
                                       .print(mCalendar)
                                       .toUpperCase(Helpers.getLocale(getContext())));
        mSelectedDayTextView.setText(new DateTimeFormatterBuilder()
                                     .appendDayOfMonth(0)
                                     .toFormatter()
                                     .print(mCalendar));
        mYearView.setText(new DateTimeFormatterBuilder()
                          .appendYear(4, 4)
                          .toFormatter()
                          .print(mCalendar));
        final long timeInMillis = this.mCalendar.toDateTimeAtStartOfDay().getMillis();
        final String desc = DateUtils
                            .formatDateTime(this.ctx, timeInMillis, 24);
        this.mMonthAndDayView.setContentDescription(desc);
    }

    public LocalDate getDate() {
        return mCalendar;
    }

    public void setDate(final @NonNull LocalDate date) {
        this.mCalendar = date;
        updateDisplay();
        updatePickers();
        updateYearRange();
    }

    interface OnDateChangedListener {
        void onDateChanged();
    }


    public static class OnDateSetListener implements Parcelable {
        public void onDateSet(final @NonNull DatePicker picker,
                              final @NonNull Optional<LocalDate> newDate) {}

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {

        }

        public static final Parcelable.Creator<OnDateSetListener> CREATOR = new
        Parcelable.Creator<OnDateSetListener>() {
            @Override
            public OnDateSetListener createFromParcel(final Parcel source) {
                return new OnDateSetListener();
            }

            @Override
            public OnDateSetListener[] newArray(final int size) {
                return new OnDateSetListener[size];
            }
        };
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
        mCalendar = mCalendar.withYear(year).withMonthOfYear(month).withDayOfMonth(day);
        updatePickers();
        updateDisplay();
    }

    @Override
    public void onYearSelected(final int year) {
        adjustDayInMonthIfNeeded(this.mCalendar.getMonthOfYear(), year);
        this.mCalendar = mCalendar.withYear(year);
        updatePickers();
        setCurrentView(0);
        updateDisplay();
    }

    private void adjustDayInMonthIfNeeded(final int month, final int year) {
        final int currentDay = this.mCalendar.getDayOfMonth();
        final int day = Utils.getDaysInMonth(month, year);
        if (currentDay > day) {
            this.mCalendar = mCalendar.withDayOfMonth(day);
        }
    }

    private void updatePickers() {
        for (OnDateChangedListener mListener : this.mListeners) {
            mListener.onDateChanged();
        }
    }

    @Override
    protected void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // on display rotate reload dialog
        final Parcelable yearState = this.mYearPickerView.onSaveInstanceState();
        final Parcelable monthState = this.mDayPickerView.onSaveInstanceState();
        initLayout();
        if (this.yearSelected) {
            setCurrentView(VIEW_DATE_PICKER_YEAR);
        }
        mYearPickerView.onRestoreInstanceState(yearState);
        mDayPickerView.onRestoreInstanceState(monthState);
    }

    private static final String CALENDAR_KEY = "calendar";
    private static final String WEEK_START_KEY = "week_start";
    private static final String YEAR_START_KEY = "minYear";
    private static final String YEAR_END_KEY = "maxYear";
    private static final String CURRENT_VIEW_KEY = "currentView";
    private static final String MOST_VISIBLE_POSITION_KEY = "mostVisiblePosition";
    private static final String LIST_OFFSET_KEY = "listOffset";
    private static final String PARENT_KEY = "Parent";
    private static final String CALLBACK_KEY = "callback";

    @Override
    public Parcelable onSaveInstanceState() {
        final Bundle b = new Bundle();
        b.putParcelable(PARENT_KEY, super.onSaveInstanceState());
        b.putSerializable(CALENDAR_KEY, mCalendar);
        b.putInt(WEEK_START_KEY, this.mWeekStart);
        b.putInt(YEAR_START_KEY, this.mMinYear);
        b.putInt(YEAR_END_KEY, this.mMaxYear);
        b.putInt(CURRENT_VIEW_KEY, this.mCurrentView);
        b.putParcelable(CALLBACK_KEY, this.mCallBack);

        int mostVisiblePosition = -1;
        if (this.mCurrentView == 0) {
            mostVisiblePosition = this.mDayPickerView.getMostVisiblePosition();
        }
        if (this.mCurrentView == 1) {
            mostVisiblePosition = this.mYearPickerView
                                  .getFirstVisiblePosition();
            b.putInt(LIST_OFFSET_KEY,
                     this.mYearPickerView.getFirstPositionOffset());
        }
        b.putInt(MOST_VISIBLE_POSITION_KEY, mostVisiblePosition);
        return b;
    }

    @Override
    public void onRestoreInstanceState(final @Nullable Parcelable state) {
        if (state == null) {
            return;
        }
        // begin boilerplate code so parent classes can restore state
        final Bundle b = (Bundle) state;
        super.onRestoreInstanceState(b.getParcelable(PARENT_KEY));
        this.mWeekStart = b.getInt(WEEK_START_KEY);
        setMinYear(b.getInt(YEAR_START_KEY));
        setMaxYear(b.getInt(YEAR_END_KEY));
        final int currentView = b.getInt(CURRENT_VIEW_KEY);
        setCurrentView(currentView);
        setScroll(b.getInt(MOST_VISIBLE_POSITION_KEY, -1), currentView,
                  b.getInt(LIST_OFFSET_KEY, 0));
        setDate((LocalDate) b.getSerializable(CALENDAR_KEY));
        mCallBack = b.getParcelable(CALLBACK_KEY);
    }

    public int getYear() {
        return this.mCalendar.getYear();
    }

    public int getMonth() {
        return this.mCalendar.getMonthOfYear();
    }

    public int getDay() {
        return this.mCalendar.getDayOfMonth();
    }

    public void hideNoDate() {
        this.mNoDateButton.setVisibility(View.GONE);
    }

}
