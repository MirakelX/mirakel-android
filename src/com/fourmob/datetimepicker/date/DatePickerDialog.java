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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.fourmob.datetimepicker.date.DatePicker.OnDateSetListener;
import com.google.common.base.Optional;

import org.joda.time.LocalDate;

import de.azapps.mirakel.date_time.R;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DatePickerDialog extends DialogFragment {

    protected DatePicker mDatePicker;
    private OnDateSetListener mCallback;
    protected LocalDate mInitDate;
    private boolean mHasNoDate;

    public DatePickerDialog() {
        super();
    }


    public void initialize(final OnDateSetListener onDateSetListener,
                           final @NonNull LocalDate initDate, final boolean hasNoDate) {
        this.mCallback = new OnDateSetListener() {

            @Override
            public void onDateSet(final DatePicker datePickerDialog,
                                  final @NonNull Optional<LocalDate> newDate) {
                if (onDateSetListener != null) {
                    onDateSetListener.onDateSet(datePickerDialog, newDate);
                }
                dismiss();
            }
        };
        this.mInitDate = initDate;
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
                DatePickerDialog.this.mDatePicker.onRestoreInstanceState(b);
            }
        }, 50L);
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
                DatePickerDialog.this.mDatePicker.setDate(mInitDate);
            }
        }, 0L);
        if (!this.mHasNoDate) {
            this.mDatePicker.hideNoDate();
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(final Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putAll((Bundle) this.mDatePicker.onSaveInstanceState());
    }

}