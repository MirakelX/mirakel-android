/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.widget.DatePicker;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakel.settings.NumPickerPref;
import de.azapps.mirakelandroid.R;

public class RecurringSettings {
	private static final String TAG = "RecurringSettings";
	private Recurring recurring;
	private boolean v4_0;
	private Object settings;
	private Context ctx;

	@SuppressLint("NewApi")
	public RecurringSettings(RecurringFragment activity, Recurring recurring) {
		this.recurring = recurring;
		v4_0 = true;
		settings = activity;
		ctx = activity.getActivity();
	}

	public RecurringSettings(RecurringActivity activity, Recurring recurring) {
		ctx = activity;
		settings = activity;
		v4_0 = false;
		this.recurring = recurring;
	}

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
		final String begin = ctx.getString(R.string.beginning);
		final String end = ctx.getString(R.string.end);
		startDate.setSummary(recurring.getStartDate() == null ? ctx.getString(
				R.string.no_begin_end, begin) : DateTimeHelper.formatDate(
				recurring.getStartDate(),
				ctx.getString(R.string.humanDateTimeFormat)));
		endDate.setSummary(recurring.getEndDate() == null ? ctx.getString(
				R.string.no_begin_end, end) : DateTimeHelper.formatDate(
				recurring.getEndDate(),
				ctx.getString(R.string.humanDateTimeFormat)));

		recurring_day.setValue(recurring.getYears());
		recurring_hour.setValue(recurring.getHours());
		recurring_minute.setValue(recurring.getMinutes());
		recurring_month.setValue(recurring.getMonths());
		recurring_year.setValue(recurring.getYears());

		setSummary(recurring_day, R.plurals.every_days, recurring.getDays());
		setSummary(recurring_month, R.plurals.every_months,
				recurring.getMonths());
		setSummary(recurring_year, R.plurals.every_years, recurring.getYears());
		setSummary(recurring_hour, R.plurals.every_hours, recurring.getHours());
		setSummary(recurring_minute, R.plurals.every_minutes,
				recurring.getMinutes());
		hideForReminder(recurring.isForDue(), recurring_minute, recurring_hour);

		forDue.setChecked(recurring.isForDue());
		labelRecurring.setText(recurring.getLabel());
		labelRecurring.setSummary(recurring.getLabel());

		recurring_day
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						recurring.setDays(recurring_day.getValue());
						setSummary(recurring_day, R.plurals.every_days,
								recurring.getDays());
						recurring.save();
						return false;
					}
				});
		recurring_month
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						Log.d(TAG, "change");
						recurring.setMonths(recurring_month.getValue());
						setSummary(recurring_month, R.plurals.every_months,
								recurring.getMonths());
						recurring.save();
						return false;
					}
				});
		recurring_year
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						recurring.setYears(recurring_year.getValue());
						setSummary(recurring_year, R.plurals.every_years,
								recurring.getYears());
						recurring.save();
						return false;
					}
				});
		recurring_hour
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						recurring.setHours(recurring_hour.getValue());
						setSummary(recurring_hour, R.plurals.every_hours,
								recurring.getHours());
						recurring.save();
						return false;
					}
				});
		recurring_minute
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						recurring.setMinutes(recurring_minute.getValue());
						setSummary(recurring_minute, R.plurals.every_minutes,
								recurring.getMinutes());
						recurring.save();
						return false;
					}
				});

		labelRecurring
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@SuppressLint("NewApi")
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						recurring.setLabel(newValue.toString());
						preference.setSummary(recurring.getLabel());
						preference.setPersistent(false);
						((EditTextPreference) preference).setText(recurring
								.getLabel());
						recurring.save();
						if (MirakelPreferences.isTablet() && v4_0) {
							((ListSettings) ctx).invalidateHeaders();
						}
						return false;
					}
				});
		forDue.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				recurring.setForDue((Boolean) newValue);
				hideForReminder(recurring.isForDue(), recurring_minute,
						recurring_hour);
				preference.setPersistent(false);
				((CheckBoxPreference) preference).setChecked(recurring
						.isForDue());
				recurring.save();
				return false;
			}
		});
		startDate.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				DateDialog(true, startDate, begin);
				return false;
			}
		});
		endDate.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				DateDialog(false, endDate, end);
				return false;
			}
		});
	}

	@SuppressLint("NewApi")
	protected void DateDialog(final boolean start, final Preference date,
			final String s) {
		Calendar c = new GregorianCalendar();
		String no = ctx.getString(R.string.no_begin_end, s);
		if (start && recurring.getStartDate() != null) {
			c = recurring.getStartDate();
		} else if (start && recurring.getEndDate() != null) {
			c = recurring.getEndDate();
		}
		OnDateSetListener listner = (!v4_0 ? new OnDateSetListener() {

			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear,
					int dayOfMonth) {
				Calendar c = new GregorianCalendar(year, monthOfYear,
						dayOfMonth);
				if (start) {
					recurring.setStartDate(c);
				} else if (recurring.getStartDate() == null
						| (recurring.getStartDate().before(c))) {
					recurring.setEndDate(c);
				} else {
					recurring.save();
					return;
				}
				date.setSummary(DateTimeHelper.formatDate(c,
						ctx.getString(R.string.humanDateTimeFormat)));
				recurring.save();
			}
		} : null);
		final DatePickerDialog picker = new DatePickerDialog(ctx, listner,
				c.get(Calendar.YEAR), c.get(Calendar.MONTH),
				c.get(Calendar.DAY_OF_MONTH));
		if (v4_0) {
			picker.getDatePicker().setCalendarViewShown(false);
			picker.setButton(Dialog.BUTTON_POSITIVE,
					ctx.getString(android.R.string.ok),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == DialogInterface.BUTTON_POSITIVE) {
								DatePicker dp = picker.getDatePicker();
								Calendar c = new GregorianCalendar(
										dp.getYear(), dp.getMonth(), dp
												.getDayOfMonth());
								if (start) {
									recurring.setStartDate(c);
								} else if (recurring.getStartDate() == null
										| (recurring.getStartDate().before(c))) {
									recurring.setEndDate(c);
								} else {
									recurring.save();
									return;
								}
								date.setSummary(DateTimeHelper.formatDate(
										c,
										ctx.getString(R.string.humanDateTimeFormat)));
								recurring.save();
							}

						}
					});
		}
		picker.setButton(Dialog.BUTTON_NEGATIVE, no,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (start) {
							recurring.setStartDate(null);
						} else {
							recurring.setEndDate(null);
						}
						date.setSummary(ctx.getString(R.string.no_begin_end, s));
						recurring.save();
					}
				});
		picker.show();

	}

	private void hideForReminder(boolean forDue, Preference recurring_minute,
			Preference recurring_hour) {
		PreferenceCategory cat = (PreferenceCategory) findPreference("recurring_intervall");
		if (forDue) {
			cat.removePreference(recurring_hour);
			cat.removePreference(recurring_minute);
		} else {
			cat.addPreference(recurring_minute);
			cat.addPreference(recurring_hour);
		}

	}

	private void setSummary(NumPickerPref pref, int id, int val) {
		String summary = ctx.getResources().getQuantityString(id, val, val);
		if (val == 0) {
			summary = ctx.getString(R.string.nothing);
		}
		pref.setSummary(summary);

	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private Preference findPreference(String key) {
		if (v4_0) {
			return ((RecurringFragment) settings).findPreference(key);
		} else {
			return ((RecurringActivity) settings).findPreference(key);
		}
	}

}
