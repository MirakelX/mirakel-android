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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.MotionEvent;
import android.view.View;

import com.fourmob.datetimepicker.Utils;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.HashMap;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.date_time.R;
import de.azapps.mirakel.helper.Helpers;

public class SimpleMonthView extends View {
    public static abstract interface OnDayClickListener {
        public abstract void onDayClick(final SimpleMonthView simpleMonthView,
                                        final SimpleMonthAdapter.CalendarDay calendarDay);
    }

    protected static int DAY_SELECTED_CIRCLE_SIZE;
    protected static int DAY_SEPARATOR_WIDTH = 1;
    protected static int DEFAULT_HEIGHT = 32;
    protected static int MIN_HEIGHT = 10;
    protected static int MINI_DAY_NUMBER_TEXT_SIZE;
    protected static final int mNumDays = 7;
    protected static int MONTH_DAY_LABEL_TEXT_SIZE;
    protected static int MONTH_HEADER_SIZE;
    protected static int MONTH_LABEL_TEXT_SIZE;
    private final int mBackground;
    private LocalDate mCalendar;
    private LocalDate mDayLabelCalendar;
    private int mDayOfWeekStart = 0;
    private final String mDayOfWeekTypeface;
    protected int mDayTextColor;
    protected boolean mHasToday = false;
    protected int mMonth;
    protected Paint mMonthDayLabelPaint;
    protected Paint mMonthNumPaint;
    protected int mMonthTitleBGColor;
    protected Paint mMonthTitleBGPaint;
    protected int mMonthTitleColor;
    protected Paint mMonthTitlePaint;
    private final String mMonthTitleTypeface;
    protected int mNumCells = SimpleMonthView.mNumDays;
    private int mNumRows = 6;
    private OnDayClickListener mOnDayClickListener;
    protected int mPadding = 0;
    protected int mRowHeight = DEFAULT_HEIGHT;
    protected Paint mSelectedCirclePaint;
    protected int mSelectedDay = -1;
    private final StringBuilder mStringBuilder;
    protected int mToday = -1;
    protected int mTodayNumberColor;
    protected int mWeekStart = 1;
    protected int mWidth;

    protected int mYear;

    public SimpleMonthView(final Context context) {
        super(context);
        this.mDayLabelCalendar = new LocalDate();
        this.mCalendar = mDayLabelCalendar;
        this.mDayOfWeekTypeface = getResources()
                                  .getString(R.string.day_of_week_label_typeface);
        this.mMonthTitleTypeface = getResources().getString(R.string.sans_serif);
        this.mDayTextColor = ThemeManager.getColor(R.attr.colorTextBlack);
        this.mTodayNumberColor = ThemeManager.getPrimaryThemeColor();
        this.mMonthTitleColor = ThemeManager.getColor(R.attr.colorTextGrey);
        this.mMonthTitleBGColor = ThemeManager.getColor(R.attr.colorBackground);
        this.mStringBuilder = new StringBuilder(50);
        MINI_DAY_NUMBER_TEXT_SIZE = getResources()
                                    .getDimensionPixelSize(R.dimen.day_number_size);
        MONTH_LABEL_TEXT_SIZE = getResources()
                                .getDimensionPixelSize(R.dimen.month_label_size);
        MONTH_DAY_LABEL_TEXT_SIZE = getResources()
                                    .getDimensionPixelSize(R.dimen.month_day_label_text_size);
        MONTH_HEADER_SIZE = getResources()
                            .getDimensionPixelOffset(R.dimen.month_list_item_header_height);
        DAY_SELECTED_CIRCLE_SIZE = getResources()
                                   .getDimensionPixelSize(R.dimen.day_number_select_circle_radius);
        this.mRowHeight = (getResources()
                           .getDimensionPixelOffset(R.dimen.date_picker_view_animator_height) - MONTH_HEADER_SIZE) / 6;
        this.mBackground = ThemeManager.getColor(R.attr.colorBackground);
        initView();
    }

    private int calculateNumRows() {
        final int dayOffset = findDayOffset();
        final int nbRows = (dayOffset + this.mNumCells)
                           / SimpleMonthView.mNumDays;
        int plusOne = 0;
        if ((dayOffset + this.mNumCells) % SimpleMonthView.mNumDays > 0) {
            plusOne = 1;
        }
        return plusOne + nbRows;
    }

    private void drawMonthDayLabels(final Canvas canvas) {
        final int y = MONTH_HEADER_SIZE - MONTH_DAY_LABEL_TEXT_SIZE / 2;
        final int space = (this.mWidth - 2 * this.mPadding)
                          / (2 * SimpleMonthView.mNumDays);
        for (int day = 0; day < SimpleMonthView.mNumDays; day++) {
            int dayOfWeek = (day + this.mWeekStart);
            if (dayOfWeek > DateTimeConstants.SUNDAY) {
                dayOfWeek -= DateTimeConstants.SUNDAY;
            }
            final int x = space * (1 + day * 2) + this.mPadding;
            mDayLabelCalendar = mDayLabelCalendar.withDayOfWeek(dayOfWeek);
            canvas.drawText(new DateTimeFormatterBuilder()
                            .appendDayOfWeekShortText()
                            .toFormatter()
                            .print(mDayLabelCalendar), x, y, this.mMonthDayLabelPaint);
        }
    }

    protected void drawMonthNums(final Canvas canvas) {
        int y = (this.mRowHeight + MINI_DAY_NUMBER_TEXT_SIZE) / 2
                - DAY_SEPARATOR_WIDTH + MONTH_HEADER_SIZE;
        final int paddingDay = (this.mWidth - 2 * this.mPadding)
                               / (2 * SimpleMonthView.mNumDays);
        int dayOffset = findDayOffset();
        int day = 1;
        while (day <= this.mNumCells) {
            final int x = paddingDay * (1 + dayOffset * 2) + this.mPadding;
            if (this.mSelectedDay == day) {
                canvas.drawCircle(x, y - MINI_DAY_NUMBER_TEXT_SIZE / 3,
                                  DAY_SELECTED_CIRCLE_SIZE, this.mSelectedCirclePaint);
            }
            if (this.mHasToday && this.mToday == day
                || this.mSelectedDay == day) {
                this.mMonthNumPaint.setColor(this.mTodayNumberColor);
            } else {
                this.mMonthNumPaint.setColor(this.mDayTextColor);
            }
            canvas.drawText(String.format("%d", day), x, y, this.mMonthNumPaint);
            dayOffset++;
            if (dayOffset == SimpleMonthView.mNumDays) {
                dayOffset = 0;
                y += this.mRowHeight;
            }
            day++;
        }
    }

    private void drawMonthTitle(final Canvas canvas) {
        final int x = (this.mWidth + 2 * this.mPadding) / 2;
        final int y = (MONTH_HEADER_SIZE - MONTH_DAY_LABEL_TEXT_SIZE) / 2
                      + MONTH_LABEL_TEXT_SIZE / 3;
        canvas.drawText(getMonthAndYearString(), x, y, this.mMonthTitlePaint);
    }

    private int findDayOffset() {
        int off;
        if (this.mDayOfWeekStart < this.mWeekStart) {
            off = this.mDayOfWeekStart + SimpleMonthView.mNumDays;
        } else {
            off = this.mDayOfWeekStart;
        }
        off = off - this.mWeekStart;
        while (off > 6) {
            off -= 7;
        }
        return off;
    }

    public SimpleMonthAdapter.CalendarDay getDayFromLocation(final float x,
            final float y) {
        final int padding = this.mPadding;
        if (x < padding || x > this.mWidth - this.mPadding) {
            return null;
        }
        final int yDay = (int) (y - MONTH_HEADER_SIZE) / this.mRowHeight;
        final int day = 1
                        + (int) ((x - padding) * SimpleMonthView.mNumDays / (this.mWidth
                                 - padding - this.mPadding)) - findDayOffset() + yDay
                        * SimpleMonthView.mNumDays;
        return new SimpleMonthAdapter.CalendarDay(this.mYear, this.mMonth, day);
    }

    private String getMonthAndYearString() {
        this.mStringBuilder.setLength(0);
        final long dateInMillis = this.mCalendar.toDateTimeAtStartOfDay().getMillis();
        return DateUtils.formatDateRange(getContext(), dateInMillis,
                                         dateInMillis, 52).toString();
    }

    protected void initView() {
        this.mMonthTitlePaint = new Paint();
        this.mMonthTitlePaint.setFakeBoldText(true);
        this.mMonthTitlePaint.setAntiAlias(true);
        this.mMonthTitlePaint.setTextSize(MONTH_LABEL_TEXT_SIZE);
        this.mMonthTitlePaint.setTypeface(Typeface.create(
                                              this.mMonthTitleTypeface, 1));
        this.mMonthTitlePaint.setColor(this.mDayTextColor);
        this.mMonthTitlePaint.setTextAlign(Paint.Align.CENTER);
        this.mMonthTitlePaint.setStyle(Paint.Style.FILL);
        this.mMonthTitleBGPaint = new Paint();
        this.mMonthTitleBGPaint.setFakeBoldText(true);
        this.mMonthTitleBGPaint.setAntiAlias(true);
        this.mMonthTitleBGPaint.setColor(this.mMonthTitleBGColor);
        this.mMonthTitleBGPaint.setTextAlign(Paint.Align.CENTER);
        this.mMonthTitleBGPaint.setStyle(Paint.Style.FILL);
        this.mSelectedCirclePaint = new Paint();
        this.mSelectedCirclePaint.setFakeBoldText(true);
        this.mSelectedCirclePaint.setAntiAlias(true);
        this.mSelectedCirclePaint.setColor(this.mTodayNumberColor);
        this.mSelectedCirclePaint.setTextAlign(Paint.Align.CENTER);
        this.mSelectedCirclePaint.setStyle(Paint.Style.FILL);
        this.mSelectedCirclePaint.setAlpha(70);
        this.mMonthDayLabelPaint = new Paint();
        this.mMonthDayLabelPaint.setAntiAlias(true);
        this.mMonthDayLabelPaint.setTextSize(MONTH_DAY_LABEL_TEXT_SIZE);
        this.mMonthDayLabelPaint.setColor(this.mDayTextColor);
        this.mMonthDayLabelPaint.setTypeface(Typeface.create(
                this.mDayOfWeekTypeface, 0));
        this.mMonthDayLabelPaint.setStyle(Paint.Style.FILL);
        this.mMonthDayLabelPaint.setTextAlign(Paint.Align.CENTER);
        this.mMonthDayLabelPaint.setFakeBoldText(true);
        this.mMonthNumPaint = new Paint();
        this.mMonthNumPaint.setAntiAlias(true);
        this.mMonthNumPaint.setTextSize(MINI_DAY_NUMBER_TEXT_SIZE);
        this.mMonthNumPaint.setStyle(Paint.Style.FILL);
        this.mMonthNumPaint.setTextAlign(Paint.Align.CENTER);
        this.mMonthNumPaint.setFakeBoldText(false);
        setBackgroundColor(this.mBackground);
    }

    private void onDayClick(final SimpleMonthAdapter.CalendarDay calendarDay) {
        if (this.mOnDayClickListener != null) {
            this.mOnDayClickListener.onDayClick(this, calendarDay);
        }
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        drawMonthTitle(canvas);
        drawMonthDayLabels(canvas);
        drawMonthNums(canvas);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec,
                             final int heightMeasureSpec) {
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec),
                             this.mRowHeight * this.mNumRows + MONTH_HEADER_SIZE);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw,
                                 final int oldh) {
        this.mWidth = w;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            final SimpleMonthAdapter.CalendarDay calendarDay = getDayFromLocation(
                        motionEvent.getX(), motionEvent.getY());
            if (calendarDay != null) {
                onDayClick(calendarDay);
            }
        }
        return true;
    }

    public void reuse() {
        this.mNumRows = 6;
        requestLayout();
    }


    public void setMonthParams(final HashMap<String, Integer> monthParams) {
        if (!monthParams.containsKey("month")
            && !monthParams.containsKey("year")) {
            throw new InvalidParameterException(
                "You must specify the month and year for this view");
        }
        setTag(monthParams);
        if (monthParams.containsKey("height")) {
            this.mRowHeight = monthParams.get("height").intValue();
            if (this.mRowHeight < MIN_HEIGHT) {
                this.mRowHeight = MIN_HEIGHT;
            }
        }
        if (monthParams.containsKey("selected_day")) {
            this.mSelectedDay = monthParams.get("selected_day").intValue();
        }
        this.mMonth = monthParams.get("month").intValue();
        this.mYear = monthParams.get("year").intValue();
        this.mHasToday = false;
        this.mToday = -1;
        mCalendar = mCalendar.withYear(mYear).withMonthOfYear(mMonth).withDayOfMonth(1);
        this.mDayOfWeekStart = this.mCalendar.getDayOfWeek();
        this.mWeekStart = ((Calendar.getInstance(Helpers.getLocale(getContext())).getFirstDayOfWeek() + 5) %
                           SimpleMonthView.mNumDays) + 1;
        this.mNumCells = Utils.getDaysInMonth(this.mMonth, this.mYear);
        final LocalDate now = new LocalDate();
        if (mMonth == now.getMonthOfYear()) {
            for (int day = 0; day < this.mNumCells; day++) {
                if (now.getDayOfMonth() == day + 1) {
                    this.mHasToday = true;
                    this.mToday = now.getDayOfMonth();
                }
            }
        }
        this.mNumRows = calculateNumRows();
    }

    public void setOnDayClickListener(
        final OnDayClickListener onDayClickListener) {
        this.mOnDayClickListener = onDayClickListener;
    }
}