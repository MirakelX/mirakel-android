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

abstract interface DatePickerController {
    public abstract int getFirstDayOfWeek();

    public abstract int getMaxYear();

    public abstract int getMinYear();

    public abstract SimpleMonthAdapter.CalendarDay getSelectedDay();

    public abstract void onDayOfMonthSelected(final int year, final int month,
            final int day);

    public abstract void onYearSelected(final int year);

    public abstract void registerOnDateChangedListener(
        final DatePicker.OnDateChangedListener onDateChangedListener);
}