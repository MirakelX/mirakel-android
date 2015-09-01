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

import android.app.Dialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakel.date_time.R;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.of;

public class SupportDateTimeDialog extends DialogFragment {

    protected static final String TAG = "DateTimeDialog";
    private DateTime mStartDate;

    public static SupportDateTimeDialog newInstance(final OnDateTimeSetListener callback,
            final Optional<DateTime> dateTime) {
        return newInstance(callback, dateTime.or(new DateTime()));
    }

    public static SupportDateTimeDialog newInstance(
        final OnDateTimeSetListener callback, final @NonNull DateTime startDate) {
        final SupportDateTimeDialog dt = new SupportDateTimeDialog();
        dt.mStartDate = startDate;
        dt.mCallback = callback;
        return dt;
    }


    //dirty hack to get a reference to the
    // originally created dialog if the screen was rotated
    private static Dialog dialog;


    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        dialog = super.onCreateDialog(savedInstanceState);
        return dialog;
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
        this.tp.set24HourMode(DateTimeHelper.is24HourLocale(Helpers.getLocale(getActivity())));
        this.tp.setTime(mStartDate.toLocalTime());
        this.tp.setOnKeyListener(this.tp.getNewKeyboardListner(getDialog()));
        this.tp.setOnTimeSetListener(new OnTimeSetListener() {
            @Override
            public void onTimeSet(final RadialPickerLayout view, final @NonNull Optional<LocalTime> newTime) {
                if (SupportDateTimeDialog.this.mCallback != null) {
                    if (newTime.isPresent()) {
                        SupportDateTimeDialog.this.mCallback.onDateTimeSet(of(dp.getDate().toDateTime(newTime.get())));
                    } else {
                        SupportDateTimeDialog.this.mCallback.onDateTimeSet(Optional.<DateTime>absent());
                    }
                }
                safeDismiss();
            }

        });
        this.dp.setOnDateSetListener(new OnDateSetListener() {
            @Override
            public void onDateSet(final DatePicker datePickerDialog,
                                  final @NonNull Optional<LocalDate> newDate) {
                if (SupportDateTimeDialog.this.mCallback != null) {
                    if (newDate.isPresent()) {
                        SupportDateTimeDialog.this.mCallback.onDateTimeSet(of(newDate.get().toDateTime(tp.getTime())));
                    } else {
                        SupportDateTimeDialog.this.mCallback.onDateTimeSet(Optional.<DateTime>absent());
                    }
                }
                safeDismiss();
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
        this.dp.setDate(mStartDate.toLocalDate());
        this.tp.setTime(mStartDate.toLocalTime());
        return v;
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

    private final static String CURRENT_VIEW = "current_view";

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(CURRENT_VIEW, isCurrentDatepicker);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState == null) {
            return;
        }
        isCurrentDatepicker = savedInstanceState.getBoolean(CURRENT_VIEW);
        if (this.isCurrentDatepicker
            && (this.viewSwitcher.getCurrentView().getId() != R.id.date_picker)) {
            this.viewSwitcher.showPrevious();
        } else if (!this.isCurrentDatepicker
                   && (this.viewSwitcher.getCurrentView().getId() != R.id.time_picker)) {
            this.viewSwitcher.showNext();
        }
    }

    private void safeDismiss() {
        try {
            dismiss();
        } catch (final NullPointerException ignored) {
            // if the user rotates the screen the current dialog is gone
            // so use this dirty hack to get it back
            dialog.dismiss();

        }
    }

}
