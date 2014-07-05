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

    public abstract void tryVibrate();
}