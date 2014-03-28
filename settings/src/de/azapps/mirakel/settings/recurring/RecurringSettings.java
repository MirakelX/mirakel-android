/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.settings.recurring;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.DialogInterface;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.widget.DatePicker;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.PreferencesHelper;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakel.settings.NumPickerPref;
import de.azapps.mirakel.settings.R;
import de.azapps.tools.Log;

public class RecurringSettings extends PreferencesHelper {
	public RecurringSettings(final PreferenceActivity c,
			final Recurring recurring) {
		super(c);
		this.recurring = recurring;
	}

	public RecurringSettings(final PreferenceFragment c,
			final Recurring recurring) {
		super(c);
		this.recurring = recurring;
	}

	private static final String TAG = "RecurringSettings";
	protected Recurring recurring;

	public void setup() {
		final NumPickerPref recurring_year = (NumPickerPref) findPreference("recurring_year");
		final NumPickerPref recurring_month = (NumPickerPref) findPreference("recurring_month");
		final NumPickerPref recurring_day = (NumPickerPref) findPreference("recurring_day");
		final NumPickerPref recurring_hour = (NumPickerPref) findPreference("recurring_hour");
		final NumPickerPref recurring_minute = (NumPickerPref) findPreference("recurring_min");

		final CheckBoxPreference forDue = (CheckBoxPreference) findPreference("forDue");
		final EditTextPreference labelRecurring = (EditTextPreference) findPreference("labelRecurring");

		final Preference startDate = findPreference("recurring_begin");
		final Preference endDate = findPreference("recurring_end");
		final String begin = this.activity.getString(R.string.beginning);
		final String end = this.activity.getString(R.string.end);
		startDate
				.setSummary(this.recurring.getStartDate() == null ? this.activity
						.getString(R.string.no_begin_end, begin)
						: DateTimeHelper.formatDate(this.recurring
								.getStartDate(), this.activity
								.getString(R.string.humanDateTimeFormat)));
		endDate.setSummary(this.recurring.getEndDate() == null ? this.activity
				.getString(R.string.no_begin_end, end) : DateTimeHelper
				.formatDate(this.recurring.getEndDate(),
						this.activity.getString(R.string.humanDateTimeFormat)));

		recurring_day.setValue(this.recurring.getYears());
		recurring_hour.setValue(this.recurring.getHours());
		recurring_minute.setValue(this.recurring.getMinutes());
		recurring_month.setValue(this.recurring.getMonths());
		recurring_year.setValue(this.recurring.getYears());

		setSummary(recurring_day, R.plurals.every_days,
				this.recurring.getDays());
		setSummary(recurring_month, R.plurals.every_months,
				this.recurring.getMonths());
		setSummary(recurring_year, R.plurals.every_years,
				this.recurring.getYears());
		setSummary(recurring_hour, R.plurals.every_hours,
				this.recurring.getHours());
		setSummary(recurring_minute, R.plurals.every_minutes,
				this.recurring.getMinutes());
		hideForReminder(this.recurring.isForDue(), recurring_minute,
				recurring_hour);

		forDue.setChecked(this.recurring.isForDue());
		labelRecurring.setText(this.recurring.getLabel());
		labelRecurring.setSummary(this.recurring.getLabel());

		recurring_day
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(
							final Preference preference, final Object newValue) {
						RecurringSettings.this.recurring.setDays(recurring_day
								.getValue());
						setSummary(recurring_day, R.plurals.every_days,
								RecurringSettings.this.recurring.getDays());
						RecurringSettings.this.recurring.save();
						return false;
					}
				});
		recurring_month
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(
							final Preference preference, final Object newValue) {
						Log.d(TAG, "change");
						RecurringSettings.this.recurring
								.setMonths(recurring_month.getValue());
						setSummary(recurring_month, R.plurals.every_months,
								RecurringSettings.this.recurring.getMonths());
						RecurringSettings.this.recurring.save();
						return false;
					}
				});
		recurring_year
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(
							final Preference preference, final Object newValue) {
						RecurringSettings.this.recurring
								.setYears(recurring_year.getValue());
						setSummary(recurring_year, R.plurals.every_years,
								RecurringSettings.this.recurring.getYears());
						RecurringSettings.this.recurring.save();
						return false;
					}
				});
		recurring_hour
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(
							final Preference preference, final Object newValue) {
						RecurringSettings.this.recurring
								.setHours(recurring_hour.getValue());
						setSummary(recurring_hour, R.plurals.every_hours,
								RecurringSettings.this.recurring.getHours());
						RecurringSettings.this.recurring.save();
						return false;
					}
				});
		recurring_minute
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(
							final Preference preference, final Object newValue) {
						RecurringSettings.this.recurring
								.setMinutes(recurring_minute.getValue());
						setSummary(recurring_minute, R.plurals.every_minutes,
								RecurringSettings.this.recurring.getMinutes());
						RecurringSettings.this.recurring.save();
						return false;
					}
				});

		labelRecurring
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@SuppressLint("NewApi")
					@Override
					public boolean onPreferenceChange(
							final Preference preference, final Object newValue) {
						RecurringSettings.this.recurring.setLabel(newValue
								.toString());
						preference.setSummary(RecurringSettings.this.recurring
								.getLabel());
						preference.setPersistent(false);
						((EditTextPreference) preference)
								.setText(RecurringSettings.this.recurring
										.getLabel());
						RecurringSettings.this.recurring.save();
						if (MirakelCommonPreferences.isTablet()
								&& RecurringSettings.this.v4_0) {
							((ListSettings) RecurringSettings.this.activity)
									.invalidateHeaders();
						}
						return false;
					}
				});
		forDue.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(final Preference preference,
					final Object newValue) {
				RecurringSettings.this.recurring.setForDue((Boolean) newValue);
				hideForReminder(RecurringSettings.this.recurring.isForDue(),
						recurring_minute, recurring_hour);
				preference.setPersistent(false);
				((CheckBoxPreference) preference)
						.setChecked(RecurringSettings.this.recurring.isForDue());
				RecurringSettings.this.recurring.save();
				return false;
			}
		});
		startDate.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				DateDialog(true, startDate, begin);
				return false;
			}
		});
		endDate.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				DateDialog(false, endDate, end);
				return false;
			}
		});
	}

	@SuppressLint("NewApi")
	protected void DateDialog(final boolean start, final Preference date,
			final String s) {
		Calendar c = new GregorianCalendar();
		final String no = this.activity.getString(R.string.no_begin_end, s);
		if (start && this.recurring.getStartDate() != null) {
			c = this.recurring.getStartDate();
		} else if (start && this.recurring.getEndDate() != null) {
			c = this.recurring.getEndDate();
		}
		final OnDateSetListener listner = !this.v4_0 ? new OnDateSetListener() {

			@Override
			public void onDateSet(final DatePicker view, final int year,
					final int monthOfYear, final int dayOfMonth) {
				final Calendar c = new GregorianCalendar(year, monthOfYear,
						dayOfMonth);
				if (start) {
					RecurringSettings.this.recurring.setStartDate(c);
				} else if (RecurringSettings.this.recurring.getStartDate() == null
						| RecurringSettings.this.recurring.getStartDate()
								.before(c)) {
					RecurringSettings.this.recurring.setEndDate(c);
				} else {
					RecurringSettings.this.recurring.save();
					return;
				}
				date.setSummary(DateTimeHelper.formatDate(c,
						RecurringSettings.this.activity
								.getString(R.string.humanDateTimeFormat)));
				RecurringSettings.this.recurring.save();
			}
		}
				: null;
		final DatePickerDialog picker = new DatePickerDialog(this.activity,
				listner, c.get(Calendar.YEAR), c.get(Calendar.MONTH),
				c.get(Calendar.DAY_OF_MONTH));
		if (this.v4_0) {
			picker.getDatePicker().setCalendarViewShown(false);
			picker.setButton(DialogInterface.BUTTON_POSITIVE,
					this.activity.getString(android.R.string.ok),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							if (which == DialogInterface.BUTTON_POSITIVE) {
								final DatePicker dp = picker.getDatePicker();
								final Calendar c = new GregorianCalendar(dp
										.getYear(), dp.getMonth(), dp
										.getDayOfMonth());
								if (start) {
									RecurringSettings.this.recurring
											.setStartDate(c);
								} else if (RecurringSettings.this.recurring
										.getStartDate() == null
										| RecurringSettings.this.recurring
												.getStartDate().before(c)) {
									RecurringSettings.this.recurring
											.setEndDate(c);
								} else {
									RecurringSettings.this.recurring.save();
									return;
								}
								date.setSummary(DateTimeHelper
										.formatDate(
												c,
												RecurringSettings.this.activity
														.getString(R.string.humanDateTimeFormat)));
								RecurringSettings.this.recurring.save();
							}

						}
					});
		}
		picker.setButton(DialogInterface.BUTTON_NEGATIVE, no,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						if (start) {
							RecurringSettings.this.recurring.setStartDate(null);
						} else {
							RecurringSettings.this.recurring.setEndDate(null);
						}
						date.setSummary(RecurringSettings.this.activity
								.getString(R.string.no_begin_end, s));
						RecurringSettings.this.recurring.save();
					}
				});
		picker.show();

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
		String summary = this.activity.getResources().getQuantityString(id,
				val, val);
		if (val == 0) {
			summary = this.activity.getString(R.string.nothing);
		}
		pref.setSummary(summary);

	}

}
