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
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import org.joda.time.LocalDate;

import java.util.HashMap;

public class SimpleMonthAdapter extends BaseAdapter implements
    SimpleMonthView.OnDayClickListener {
    private final Context mContext;
    private final DatePickerController mController;
    private CalendarDay mSelectedDay;

    public SimpleMonthAdapter(final Context context,
                              final DatePickerController datePickerController) {
        this.mContext = context;
        this.mController = datePickerController;
        init();
        setSelectedDay(this.mController.getSelectedDay());
    }

    private boolean isSelectedDayInMonth(final int year, final int month) {
        return this.mSelectedDay.year == year
               && this.mSelectedDay.month == month;
    }

    @Override
    public int getCount() {
        return 12 * (1 + this.mController.getMaxYear() - this.mController
                     .getMinYear());
    }

    @Override
    public Object getItem(final int position) {
        return null;
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getView(final int position, final View convertView,
                        final ViewGroup parent) {
        SimpleMonthView simpleMonthView;
        if (convertView != null) {
            simpleMonthView = (SimpleMonthView) convertView;
        } else {
            simpleMonthView = new SimpleMonthView(this.mContext);
            simpleMonthView.setLayoutParams(new AbsListView.LayoutParams(
                                                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            simpleMonthView.setClickable(true);
            simpleMonthView.setOnDayClickListener(this);
        }
        simpleMonthView.setBackgroundColor(this.mContext.getResources()
                                           .getColor(android.R.color.transparent));
        @SuppressWarnings("unchecked")
        HashMap<String, Integer> monthParams = (HashMap<String, Integer>) simpleMonthView
                                               .getTag();
        if (monthParams == null) {
            monthParams = new HashMap<>();
        }
        monthParams.clear();
        final int month = position % 12 + 1;
        final int year = position / 12 + this.mController.getMinYear();
        int selectedDay = -1;
        if (isSelectedDayInMonth(year, month)) {
            selectedDay = this.mSelectedDay.day;
        }
        simpleMonthView.reuse();
        monthParams.put("selected_day", Integer.valueOf(selectedDay));
        monthParams.put("year", Integer.valueOf(year));
        monthParams.put("month", Integer.valueOf(month));
        monthParams.put("week_start",
                        Integer.valueOf(this.mController.getFirstDayOfWeek()));
        simpleMonthView.setMonthParams(monthParams);
        simpleMonthView.invalidate();
        return simpleMonthView;
    }

    protected void init() {
        this.mSelectedDay = new CalendarDay(System.currentTimeMillis());
    }

    @Override
    public void onDayClick(final SimpleMonthView simpleMonthView,
                           final CalendarDay calendarDay) {
        if (calendarDay != null) {
            onDayTapped(calendarDay);
        }
    }

    protected void onDayTapped(final CalendarDay calendarDay) {
        this.mController.onDayOfMonthSelected(calendarDay.year,
                                              calendarDay.month, calendarDay.day);
        setSelectedDay(calendarDay);
    }

    public void setSelectedDay(final CalendarDay calendarDay) {
        this.mSelectedDay = calendarDay;
        notifyDataSetChanged();
    }

    public static class CalendarDay {
        private LocalDate calendar;
        int day;
        int month;
        int year;

        public CalendarDay() {
            calendar = new LocalDate();
            init();
        }

        public CalendarDay(final int year, final int month, final int day) {
            setDay(year, month, day);
            calendar = new LocalDate(year, month, day);
        }

        public CalendarDay(final long timeInMillis) {
            setTime(timeInMillis);
        }

        public CalendarDay(final LocalDate calendar) {
            this.calendar = calendar;
            init();
        }

        private void init() {
            this.year = calendar.getYear();
            this.month = calendar.getMonthOfYear();
            this.day = calendar.getDayOfMonth();
        }

        private void setTime(final long timeInMillis) {
            calendar = new LocalDate(timeInMillis);
            init();
        }

        public void set(final CalendarDay calendarDay) {
            this.year = calendarDay.year;
            this.month = calendarDay.month;
            this.day = calendarDay.day;
        }

        public void setDay(final int year, final int month, final int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }
    }
}