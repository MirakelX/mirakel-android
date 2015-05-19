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

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.fourmob.datetimepicker.date.DatePicker.OnDateSetListener;
import com.google.common.base.Optional;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.azapps.mirakel.date_time.R;

public class SupportDatePickerDialog extends DialogFragment {

    protected DatePicker mDatePicker;
    private OnDateSetListener mCallback;
    protected int mInitYear;
    protected int mInitMonth;
    protected int mInitDay;
    private boolean mHasNoDate;

    public SupportDatePickerDialog() {
        super();
    }

    public static SupportDatePickerDialog newInstance(final OnDateSetListener onDateSetListener,
            Optional<Calendar> date, final boolean dark) {
        final Calendar notNullDate = date.or(new GregorianCalendar());
        final boolean hasNoDate = !date.isPresent();
        final int year = notNullDate.get(Calendar.YEAR);
        final int month = notNullDate.get(Calendar.MONTH);
        final int day = notNullDate.get(Calendar.DAY_OF_MONTH);
        return newInstance(onDateSetListener, year, month, day, hasNoDate);
    }
    public static SupportDatePickerDialog newInstance(
        final OnDateSetListener onDateSetListener, final int year,
        final int month, final int day,
        final boolean hasNoDate) {
        return newInstance(onDateSetListener, year, month, day, true,
                           hasNoDate);
    }

    public static SupportDatePickerDialog newInstance(
        final OnDateSetListener onDateSetListener, final int year,
        final int month, final int day, final boolean vibrate, final boolean hasNoDate) {
        final SupportDatePickerDialog datePickerDialog = new SupportDatePickerDialog();
        datePickerDialog.initialize(onDateSetListener, year, month, day,
                                    vibrate, hasNoDate);
        return datePickerDialog;
    }

    public void setVibrate(final boolean vibrate) {
        // nothing
    }

    public void initialize(final OnDateSetListener onDateSetListener,
                           final int year, final int month, final int day,
                           final boolean vibrate, final boolean hasNoDate) {
        this.mCallback = new OnDateSetListener() {
            @Override
            public void onNoDateSet() {
                if (onDateSetListener != null) {
                    onDateSetListener.onNoDateSet();
                }
                dismiss();
            }
            @Override
            public void onDateSet(final DatePicker datePickerDialog,
                                  final int year, final int month, final int day) {
                if (onDateSetListener != null) {
                    onDateSetListener.onDateSet(datePickerDialog, year, month,
                                                day);
                }
                dismiss();
            }
        };
        this.mInitYear = year;
        this.mInitMonth = month;
        this.mInitDay = day;
        this.mHasNoDate = hasNoDate;
    }

    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        final Activity activity = getActivity();
        activity.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final Bundle b = (Bundle) this.mDatePicker.onSaveInstanceState();
        getDialog().setContentView(
            onCreateView(LayoutInflater.from(getDialog().getContext()),
                         null, b));
        this.mDatePicker.postDelayed(new Runnable() {
            @Override
            public void run() {
                SupportDatePickerDialog.this.mDatePicker.onRestoreInstanceState(b);
            }
        }, 50);
    }

    @Override
    public View onCreateView(final LayoutInflater layoutInflater,
                             final ViewGroup parent, final Bundle bundle) {
        Log.d("DatePickerDialog", "onCreateView: ");
        try {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        final View view = layoutInflater.inflate(R.layout.date_picker_dialog,
                          null);
        this.mDatePicker = (DatePicker) view.findViewById(R.id.date_picker);
        this.mDatePicker.setOnDateSetListener(this.mCallback);
        this.mDatePicker.postDelayed(new Runnable() {
            @Override
            public void run() {
                SupportDatePickerDialog.this.mDatePicker
                .setYear(SupportDatePickerDialog.this.mInitYear);
                SupportDatePickerDialog.this.mDatePicker
                .setMonth(SupportDatePickerDialog.this.mInitMonth);
                SupportDatePickerDialog.this.mDatePicker
                .setDay(SupportDatePickerDialog.this.mInitDay);
            }
        }, 0);
        return view;
    }

    @Override
    public void onSaveInstanceState(final Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putAll((Bundle) this.mDatePicker.onSaveInstanceState());
    }

}