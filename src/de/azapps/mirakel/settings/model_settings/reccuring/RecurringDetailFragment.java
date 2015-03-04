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

package de.azapps.mirakel.settings.model_settings.reccuring;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;
import android.widget.DatePicker;

import com.google.common.base.Optional;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelDetailFragment;
import de.azapps.tools.Log;
import de.azapps.widgets.NumPickerPref;

public class RecurringDetailFragment extends GenericModelDetailFragment<Recurring> {

    private static final String TAG = "AccountDetailFragment";

    @Override
    protected boolean hasMenu() {
        return !MirakelCommonPreferences.isTablet();
    }

    @NonNull
    @Override
    protected Recurring getDummyItem() {
        return Recurring.newRecurring(getString(R.string.new_recurring), 0, 0,
                                      0, 0, 1, true, Optional.<Calendar>absent(), Optional.<Calendar>absent(), false, false,
                                      new SparseBooleanArray());
    }

    @Override
    protected int getResourceId() {
        return R.xml.settings_recurring;
    }

    @Override
    protected void setUp() {
        final Recurring recurring = mItem;
        final NumPickerPref recurring_year = (NumPickerPref) findPreference("recurring_year");
        final NumPickerPref recurring_month = (NumPickerPref) findPreference("recurring_month");
        final NumPickerPref recurring_day = (NumPickerPref) findPreference("recurring_day");
        final NumPickerPref recurring_hour = (NumPickerPref) findPreference("recurring_hour");
        final NumPickerPref recurring_minute = (NumPickerPref) findPreference("recurring_min");
        final SwitchPreference forDue = (SwitchPreference) findPreference("forDue");
        final EditTextPreference labelRecurring = (EditTextPreference)
                findPreference("labelRecurring");
        final Preference startDate = findPreference("recurring_begin");
        final Preference endDate = findPreference("recurring_end");
        final String begin = getString(R.string.beginning);
        final String end = getString(R.string.end);
        startDate
        .setSummary(!recurring.getStartDate().isPresent() ? getString(R.string.no_begin_end, begin)
                    : DateTimeHelper.formatDate(recurring
                                                .getStartDate().get(), getString(R.string.humanDateTimeFormat)));
        endDate.setSummary(!recurring.getEndDate().isPresent() ? getString(R.string.no_begin_end,
                           end) : DateTimeHelper
                           .formatDate(recurring.getEndDate().get(),
                                       getString(R.string.humanDateTimeFormat)));
        recurring_day.setValue(recurring.getYears());
        recurring_hour.setValue(recurring.getHours());
        recurring_minute.setValue(recurring.getMinutes());
        recurring_month.setValue(recurring.getMonths());
        recurring_year.setValue(recurring.getYears());
        setSummary(recurring_day, R.plurals.every_days,
                   recurring.getDays());
        setSummary(recurring_month, R.plurals.every_months,
                   recurring.getMonths());
        setSummary(recurring_year, R.plurals.every_years,
                   recurring.getYears());
        setSummary(recurring_hour, R.plurals.every_hours,
                   recurring.getHours());
        setSummary(recurring_minute, R.plurals.every_minutes,
                   recurring.getMinutes());
        hideForReminder(recurring.isForDue(), recurring_minute,
                        recurring_hour);
        forDue.setChecked(recurring.isForDue());
        labelRecurring.setText(recurring.getLabel());
        labelRecurring.setSummary(recurring.getLabel());
        recurring_day
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                recurring.setDays(recurring_day
                                  .getValue());
                setSummary(recurring_day, R.plurals.every_days,
                           recurring.getDays());
                recurring.save();
                return false;
            }
        });
        recurring_month
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                Log.d(TAG, "change");
                recurring
                .setMonths(recurring_month.getValue());
                setSummary(recurring_month, R.plurals.every_months,
                           recurring.getMonths());
                recurring.save();
                return false;
            }
        });
        recurring_year
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                recurring
                .setYears(recurring_year.getValue());
                setSummary(recurring_year, R.plurals.every_years,
                           recurring.getYears());
                recurring.save();
                return false;
            }
        });
        recurring_hour
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                recurring
                .setHours(recurring_hour.getValue());
                setSummary(recurring_hour, R.plurals.every_hours,
                           recurring.getHours());
                recurring.save();
                return false;
            }
        });
        recurring_minute
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                recurring
                .setMinutes(recurring_minute.getValue());
                setSummary(recurring_minute, R.plurals.every_minutes,
                           recurring.getMinutes());
                recurring.save();
                return false;
            }
        });
        labelRecurring
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @SuppressLint("NewApi")
            @Override
            public boolean onPreferenceChange(
                final Preference preference, final Object newValue) {
                recurring.setLabel(newValue
                                   .toString());
                preference.setSummary(recurring
                                      .getLabel());
                preference.setPersistent(false);
                ((EditTextPreference) preference)
                .setText(recurring
                         .getLabel());
                recurring.save();
                updateList();
                return false;
            }
        });
        forDue.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference,
                                              final Object newValue) {
                recurring.setForDue((Boolean) newValue);
                hideForReminder(recurring.isForDue(),
                                recurring_minute, recurring_hour);
                preference.setPersistent(false);
                ((SwitchPreference) preference)
                .setChecked(recurring.isForDue());
                recurring.save();
                return false;
            }
        });
        startDate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                handleDateDialog(recurring, true, startDate, begin);
                return false;
            }
        });
        endDate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                handleDateDialog(recurring, false, endDate, end);
                return false;
            }
        });
    }

    protected void hideForReminder(final boolean forDue,
                                   final Preference recurring_minute, final Preference recurring_hour) {
        final PreferenceCategory cat = (PreferenceCategory) findPreference("recurring_interval");
        if (forDue) {
            cat.removePreference(recurring_hour);
            cat.removePreference(recurring_minute);
        } else {
            cat.addPreference(recurring_minute);
            cat.addPreference(recurring_hour);
        }
    }


    protected void setSummary(final NumPickerPref pref, final int id,
                              final int val) {
        String summary = getResources().getQuantityString(id,
                         val, val);
        if (val == 0) {
            summary = getString(R.string.nothing);
        }
        pref.setSummary(summary);
    }

    @SuppressLint("NewApi")
    protected void handleDateDialog(final Recurring recurring, final boolean start,
                                    final Preference date,
                                    final String s) {
        Calendar c = new GregorianCalendar();
        final String no = getString(R.string.no_begin_end, s);
        if (start && recurring.getStartDate().isPresent()) {
            c = recurring.getStartDate().get();
        } else if (start && recurring.getEndDate().isPresent()) {
            c = recurring.getEndDate().get();
        }
        final DatePickerDialog.OnDateSetListener listener = null;
        final DatePickerDialog picker = new DatePickerDialog(getActivity(),
                listener, c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setCalendarViewShown(false);
        picker.setButton(DialogInterface.BUTTON_POSITIVE,
                         getString(android.R.string.ok),
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    final DatePicker dp = picker.getDatePicker();
                    final Optional<Calendar> c = Optional.of((Calendar) new GregorianCalendar(dp
                                                 .getYear(), dp.getMonth(), dp
                                                 .getDayOfMonth()));
                    if (start) {
                        recurring
                        .setStartDate(c);
                    } else if (!recurring
                               .getStartDate().isPresent()
                               || recurring
                               .getStartDate().get().before(c)) {
                        recurring
                        .setEndDate(c);
                    } else {
                        recurring.save();
                        return;
                    }
                    date.setSummary(DateTimeHelper
                                    .formatDate(
                                        c.get(),
                                        getString(R.string.humanDateTimeFormat)));
                    recurring.save();
                }
            }
        });
        picker.setButton(DialogInterface.BUTTON_NEGATIVE, no,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                if (start) {
                    recurring.setStartDate(Optional.<Calendar>absent());
                } else {
                    recurring.setEndDate(Optional.<Calendar>absent());
                }
                date.setSummary(getString(R.string.no_begin_end, s));
                recurring.save();
            }
        });
        picker.show();
    }


}
