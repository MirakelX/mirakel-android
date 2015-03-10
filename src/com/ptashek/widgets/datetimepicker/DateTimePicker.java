package com.ptashek.widgets.datetimepicker;

/**
 * Copyright 2010 Lukasz Szmit <devmail@szmit.eu>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 Modified by azapps,weiznich 2013
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import android.widget.ViewSwitcher;

import java.util.Calendar;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.date_time.R;

public class DateTimePicker extends RelativeLayout implements
    View.OnClickListener, OnDateChangedListener, OnTimeChangedListener {

    // DatePicker reference
    private final DatePicker datePicker;
    // TimePicker reference
    private final TimePicker timePicker;
    // ViewSwitcher reference
    private final ViewSwitcher viewSwitcher;
    // Calendar reference
    private final Calendar mCalendar;

    // Constructor start
    public DateTimePicker(final Context context) {
        this(context, null);
    }

    public DateTimePicker(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DateTimePicker(final Context context, final AttributeSet attrs,
                          final int defStyle) {
        super(context, attrs, defStyle);
        // Get LayoutInflater instance
        final LayoutInflater inflater = (LayoutInflater) context
                                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Inflate myself
        inflater.inflate(R.layout.datetimepicker, this, true);
        // Inflate the date and time picker views
        final LinearLayout datePickerView = (LinearLayout) inflater.inflate(
                                                R.layout.datepicker, null);
        final LinearLayout timePickerView = (LinearLayout) inflater.inflate(
                                                R.layout.timepicker, null);
        // Grab a Calendar instance
        this.mCalendar = Calendar.getInstance();
        // Grab the ViewSwitcher so we can attach our picker views to it
        this.viewSwitcher = (ViewSwitcher) this
                            .findViewById(R.id.DateTimePickerVS);
        // Init date picker
        this.datePicker = (DatePicker) datePickerView
                          .findViewById(R.id.DatePicker);
        this.datePicker.init(this.mCalendar.get(Calendar.YEAR),
                             this.mCalendar.get(Calendar.MONTH),
                             this.mCalendar.get(Calendar.DAY_OF_MONTH), this);
        // Init time picker
        this.timePicker = (TimePicker) timePickerView
                          .findViewById(R.id.TimePicker);
        this.timePicker.setOnTimeChangedListener(this);
        // Handle button clicks
        findViewById(R.id.SwitchToTime).setOnClickListener(this);// shows the time picker

        findViewById(R.id.SwitchToDate).setOnClickListener(this); // shows the date picker
        ((Button) findViewById(R.id.SwitchToDate)).setTextColor(ThemeManager.getPrimaryThemeColor());
        // Populate ViewSwitcher
        this.viewSwitcher.addView(datePickerView, 0);
        this.viewSwitcher.addView(timePickerView, 1);
    }

    // Constructor end

    // Called every time the user changes DatePicker values
    @Override
    public void onDateChanged(final DatePicker view, final int year,
                              final int monthOfYear, final int dayOfMonth) {
        // Update the internal Calendar instance
        this.mCalendar.set(year, monthOfYear, dayOfMonth,
                           this.mCalendar.get(Calendar.HOUR_OF_DAY),
                           this.mCalendar.get(Calendar.MINUTE));
    }

    // Called every time the user changes TimePicker values
    @Override
    public void onTimeChanged(final TimePicker view, final int hourOfDay,
                              final int minute) {
        // Update the internal Calendar instance
        this.mCalendar.set(this.mCalendar.get(Calendar.YEAR),
                           this.mCalendar.get(Calendar.MONTH),
                           this.mCalendar.get(Calendar.DAY_OF_MONTH), hourOfDay, minute);
    }

    // Handle button clicks
    @Override
    public void onClick(final View v) {
        ((Button) v).setTextColor(getResources().getColor(ThemeManager.getColor(
                                      R.attr.colorControlHighlight)));
        if (v.getId() == R.id.SwitchToDate) {
            v.setEnabled(false);
            findViewById(R.id.SwitchToTime).setEnabled(true);
            this.viewSwitcher.showPrevious();
        } else if (v.getId() == R.id.SwitchToTime) {
            v.setEnabled(false);
            findViewById(R.id.SwitchToDate).setEnabled(true);
            this.viewSwitcher.showNext();
        }
    }

    // Convenience wrapper for internal Calendar instance
    public int get(final int field) {
        return this.mCalendar.get(field);
    }

    // Reset DatePicker, TimePicker and internal Calendar instance
    public void reset() {
        final Calendar c = Calendar.getInstance();
        updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                   c.get(Calendar.DAY_OF_MONTH));
        updateTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    // Convenience wrapper for internal Calendar instance
    public long getDateTimeMillis() {
        return this.mCalendar.getTimeInMillis();
    }

    // Convenience wrapper for internal TimePicker instance
    public void setIs24HourView(final boolean is24HourView) {
        this.timePicker.setIs24HourView(is24HourView);
    }

    // Convenience wrapper for internal TimePicker instance
    public boolean is24HourView() {
        return this.timePicker.is24HourView();
    }

    // Convenience wrapper for internal DatePicker instance
    public void updateDate(final int year, final int monthOfYear,
                           final int dayOfMonth) {
        this.datePicker.updateDate(year, monthOfYear, dayOfMonth);
    }

    // Convenience wrapper for internal TimePicker instance
    public void updateTime(final int currentHour, final int currentMinute) {
        this.timePicker.setCurrentHour(currentHour);
        this.timePicker.setCurrentMinute(currentMinute);
    }
}
