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
import com.google.common.base.Optional;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePicker;
import com.sleepbot.datetimepicker.time.TimePicker.OnTimeSetListener;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.date_time.R;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.tools.Log;

public class SupportDateTimeDialog extends DialogFragment {

    protected static final String TAG = "DateTimeDialog";

    public SupportDateTimeDialog() {
        super();
    }

    public static SupportDateTimeDialog newInstance(final OnDateTimeSetListener callback,
            final Optional<Calendar> dateTime) {
        final Calendar notNullDateTime = dateTime.or(new GregorianCalendar());
        final int year = notNullDateTime.get(Calendar.YEAR);
        final int month = notNullDateTime.get(Calendar.MONTH);
        final int day = notNullDateTime.get(Calendar.DAY_OF_MONTH);
        final int hour = notNullDateTime.get(Calendar.HOUR_OF_DAY);
        final int minute = notNullDateTime.get(Calendar.MINUTE);
        return newInstance(callback, year, month, day, hour, minute);
    }

    public static SupportDateTimeDialog newInstance(
        final OnDateTimeSetListener callback, final int year,
        final int month, final int dayOfMonth, final int hourOfDay,
        final int minute) {
        final SupportDateTimeDialog dt = new SupportDateTimeDialog();
        dt.init(year, month, dayOfMonth, hourOfDay, minute);
        dt.mCallback = callback;
        return dt;
    }

    private int mInitialYear;
    private int mInitialMonth;
    private int mInitialDay;
    private int mInitialHour;
    private int mInitialMinute;

    private void init(final int year, final int month, final int dayOfMonth,
                      final int hourOfDay, final int minute) {
        this.mInitialYear = year;
        this.mInitialMonth = month;
        this.mInitialDay = dayOfMonth;
        this.mInitialHour = hourOfDay;
        this.mInitialMinute = minute;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected ViewSwitcher viewSwitcher;
    protected TimePicker tp;
    protected DatePicker dp;
    protected boolean isCurrentDatepicker = true;
    protected OnDateTimeSetListener mCallback;

    void setOnDateTimeSetListner(final OnDateTimeSetListener listner) {
        this.mCallback = listner;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container, final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        try {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        } catch (final RuntimeException e) {
            Log.wtf(TAG, "could not remove title bar", e);
        }
        final View v = inflater.inflate(R.layout.date_time_picker, container);
        final Button switchToDate = (Button) v
                                    .findViewById(R.id.datetime_picker_date);
        final Button switchToTime = (Button) v
                                    .findViewById(R.id.datetime_picker_time);
        this.viewSwitcher = (ViewSwitcher) v
                            .findViewById(R.id.datetime_picker_animator);
        this.dp = (DatePicker) v.findViewById(R.id.date_picker);
        this.tp = (TimePicker) v.findViewById(R.id.time_picker);
        this.tp.set24HourMode(DateTimeHelper.is24HourLocale(Helpers.getLocal(getActivity())));
        this.tp.setTime(this.mInitialHour, this.mInitialMinute);
        this.tp.setOnKeyListener(this.tp.getNewKeyboardListner(getDialog()));
        this.tp.setOnTimeSetListener(new OnTimeSetListener() {
            @Override
            public void onTimeSet(final RadialPickerLayout view,
                                  final int hourOfDay, final int minute) {
                if (SupportDateTimeDialog.this.mCallback != null) {
                    SupportDateTimeDialog.this.mCallback.onDateTimeSet(
                        SupportDateTimeDialog.this.dp.getYear(),
                        SupportDateTimeDialog.this.dp.getMonth(),
                        SupportDateTimeDialog.this.dp.getDay(), hourOfDay, minute);
                }
                dismiss();
            }

            @Override
            public void onNoTimeSet() {
                if (SupportDateTimeDialog.this.mCallback != null) {
                    SupportDateTimeDialog.this.mCallback.onNoTimeSet();
                }
                dismiss();
            }
        });
        this.dp.setOnDateSetListener(new OnDateSetListener() {
            @Override
            public void onNoDateSet() {
                if (SupportDateTimeDialog.this.mCallback != null) {
                    SupportDateTimeDialog.this.mCallback.onNoTimeSet();
                }
                dismiss();
            }

            @Override
            public void onDateSet(final DatePicker datePickerDialog,
                                  final int year, final int month, final int day) {
                if (SupportDateTimeDialog.this.mCallback != null) {
                    SupportDateTimeDialog.this.mCallback.onDateTimeSet(year, month,
                            day, SupportDateTimeDialog.this.tp.getHour(),
                            SupportDateTimeDialog.this.tp.getMinute());
                }
                dismiss();
            }
        });
        switchToDate.setTextColor(ThemeManager.getColor(R.attr.colorTextGrey));
        switchToDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (!SupportDateTimeDialog.this.isCurrentDatepicker) {
                    SupportDateTimeDialog.this.viewSwitcher.showPrevious();
                    SupportDateTimeDialog.this.isCurrentDatepicker = true;
                }
            }
        });
        switchToTime.setTextColor(ThemeManager.getColor(R.attr.colorTextGrey));
        switchToTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (SupportDateTimeDialog.this.isCurrentDatepicker) {
                    SupportDateTimeDialog.this.viewSwitcher.showNext();
                    SupportDateTimeDialog.this.isCurrentDatepicker = false;
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

    public interface OnDateTimeSetListener {

        void onDateTimeSet(final int year, final int month,
                           final int dayOfMonth, final int hourOfDay, final int minute);

        void onNoTimeSet();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final Bundle time = (Bundle) this.tp.onSaveInstanceState();
        final Bundle date = (Bundle) this.dp.onSaveInstanceState();
        getDialog().setContentView(
            onCreateView(LayoutInflater.from(getDialog().getContext()),
                         null, null));
        if (this.isCurrentDatepicker
            && (this.viewSwitcher.getCurrentView().getId() != R.id.date_picker)) {
            this.viewSwitcher.showPrevious();
        } else if (!this.isCurrentDatepicker
                   && (this.viewSwitcher.getCurrentView().getId() != R.id.time_picker)) {
            this.viewSwitcher.showNext();
        }
        this.dp.onRestoreInstanceState(date);
        this.tp.onRestoreInstanceState(time);
    }

}
