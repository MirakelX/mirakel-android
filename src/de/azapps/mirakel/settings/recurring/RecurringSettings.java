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

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.settings.NumPickerPref;
import de.azapps.mirakelandroid.R;

public class RecurringSettings  {
	private static final String TAG = "RecurringSettings";
	private Recurring recurring;
	private boolean v4_0;
	private Object settings;
	private Context ctx;

	@SuppressLint("NewApi")
	public RecurringSettings(RecurringFragment activity,
			Recurring recurring) {
		this.recurring = recurring;
		v4_0 = true;
		settings = activity;
		ctx = activity.getActivity();
	}

	public RecurringSettings(RecurringActivity activity,
			Recurring recurring) {
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

		recurring_day.setValue(recurring.getYears());
		recurring_hour.setValue(recurring.getHours());
		recurring_minute.setValue(recurring.getMinutes());
		recurring_month.setValue(recurring.getMonths());
		recurring_year.setValue(recurring.getYears());
		
		setSummary(recurring_day,ctx.getString(R.string.day),recurring.getDays());
		setSummary(recurring_month,ctx.getString(R.string.month),recurring.getMonths());
		setSummary(recurring_year,ctx.getString(R.string.year),recurring.getYears());
		setSummary(recurring_hour,ctx.getString(R.string.hour),recurring.getHours());
		setSummary(recurring_minute,ctx.getString(R.string.minute),recurring.getMinutes());

		forDue.setChecked(recurring.isForDue());
		labelRecurring.setText(recurring.getLabel());
		labelRecurring.setSummary(recurring.getLabel());

		recurring_day
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						recurring.setDays(recurring_day.getValue());
						setSummary(recurring_day,ctx.getString(R.string.day),recurring.getDays());
						return false;
					}
				});
		recurring_month
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						Log.d(TAG,"change");
						recurring.setMonths(recurring_month.getValue());
						setSummary(recurring_month,ctx.getString(R.string.month),recurring.getMonths());
						return false;
					}
				});
		recurring_year
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						recurring.setYears(recurring_year.getValue());
						setSummary(recurring_year,ctx.getString(R.string.year),recurring.getYears());
						return false;
					}
				});
		recurring_hour
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						recurring.setHours(recurring_hour.getValue());
						setSummary(recurring_hour,ctx.getString(R.string.hour),recurring.getHours());
						return false;
					}
				});
		recurring_minute
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						recurring.setMinutes(recurring_minute.getValue());
						setSummary(recurring_minute,ctx.getString(R.string.minute),recurring.getMinutes());
						return false;
					}
				});
		
		labelRecurring.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				recurring.setLabel(newValue.toString());
				setSummary(recurring_minute,ctx.getString(R.string.minute),recurring.getMinutes());
				return false;
			}
		});
		forDue.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				recurring.setForDue((Boolean)newValue);
				return false;
			}
		});
	}

	private void setSummary(NumPickerPref pref, String string, int val) {
		String summary=ctx.getString(R.string.recurring_summary,val==1?"":val+". ",string);
		if(val==0){
			summary=ctx.getString(R.string.nothing);
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
