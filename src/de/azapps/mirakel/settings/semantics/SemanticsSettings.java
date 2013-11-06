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
package de.azapps.mirakel.settings.semantics;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import de.azapps.mirakel.helper.DueDialog;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.DueDialog.VALUE;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakelandroid.R;

public class SemanticsSettings implements OnPreferenceChangeListener {
	private Semantic semantic;
	private boolean v4_0;
	private Object settings;
	private Context ctx;

	protected AlertDialog alert;
	private EditTextPreference semanticsCondition;
	private ListPreference semanticsList, semanticsPriority;
	private Preference semanticsDue;
	private int dueDialogValue;
	private VALUE dueDialogDayYear;

	@SuppressLint("NewApi")
	public SemanticsSettings(SemanticsSettingsFragment activity,
			Semantic semantic) {
		this.semantic = semantic;
		v4_0 = true;
		settings = activity;
		ctx = activity.getActivity();
	}

	public SemanticsSettings(SemanticsSettingsActivity activity,
			Semantic semantic) {
		ctx = activity;
		settings = activity;
		v4_0 = false;
		this.semantic = semantic;
	}

	public void setup() {
		semanticsCondition = (EditTextPreference) findPreference("semantics_condition");
		semanticsCondition.setOnPreferenceChangeListener(this);
		semanticsCondition.setText(semantic.getCondition());
		semanticsCondition.setSummary(semantic.getCondition());

		// Priority
		semanticsPriority = (ListPreference) findPreference("semantics_priority");
		semanticsPriority.setOnPreferenceChangeListener(this);
		semanticsPriority.setEntries(R.array.priority_entries);
		semanticsPriority.setEntryValues(R.array.priority_entry_values);
		if (semantic.getPriority() == null) {
			semanticsPriority.setValueIndex(0);
			semanticsPriority.setSummary(ctx.getResources().getStringArray(
					R.array.priority_entries)[0]);
		} else {
			semanticsPriority.setValue(semantic.getPriority().toString());
			semanticsPriority.setSummary(semanticsPriority.getValue());
		}

		// Due
		semanticsDue = (Preference) findPreference("semantics_due");
		semanticsDue.setOnPreferenceChangeListener(this);

		semanticsDue.setSummary(updateDueStuff());
		semanticsDue
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						final DueDialog dueDialog = new DueDialog(ctx,false);
						dueDialog.setTitle(semanticsDue.getTitle());
						dueDialog.setValue(dueDialogValue, dueDialogDayYear);

						dueDialog.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
									}
								});
						dueDialog.setNeutralButton(R.string.no_date,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										semantic.setDue(null);
										semanticsDue
												.setSummary(updateDueStuff());
										semantic.save();
									}
								});
						dueDialog.setPositiveButton(android.R.string.ok,
								new OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										int val = dueDialog.getValue();
										VALUE dayYear = dueDialog.getDayYear();
										switch (dayYear) {
										case DAY:
											semantic.setDue(val);
											break;
										case MONTH:
											semantic.setDue(val * 30);
											break;
										case YEAR:
											semantic.setDue(val * 365);
											break;
										}
										semanticsDue
												.setSummary(updateDueStuff());
										semantic.save();
									}
								});

						dueDialog.show();
						return false;

					}
				});

		// List
		semanticsList = (ListPreference) findPreference("semantics_list");
		semanticsList.setOnPreferenceChangeListener(this);

		List<ListMirakel> lists = ListMirakel.all(false);
		final CharSequence[] listEntries = new CharSequence[lists.size() + 1];
		final CharSequence[] listValues = new CharSequence[lists.size() + 1];
		listEntries[0] = ctx.getString(R.string.semantics_no_list);
		listValues[0] = "null";
		for (int i = 0; i < lists.size(); i++) {
			listValues[i + 1] = String.valueOf(lists.get(i).getId());
			listEntries[i + 1] = lists.get(i).getName();
		}
		semanticsList.setEntries(listEntries);
		semanticsList.setEntryValues(listValues);

		if (semantic.getList() == null) {
			semanticsList.setValueIndex(0);
			semanticsList.setSummary(ctx.getString(R.string.semantics_no_list));
		} else {
			semanticsList.setValue(String.valueOf(semantic.getList().getId()));
			semanticsList.setSummary(semantic.getList().getName());
		}
	}

	/**
	 * Updates the variables for the due Dialog and returns the summary for the
	 * Due-Preference
	 * 
	 * @param due
	 * @return
	 */
	private String updateDueStuff() {
		Integer due = semantic.getDue();
		String summary;
		if (due == null) {
			dueDialogDayYear = VALUE.DAY;
			dueDialogValue = 0;
			summary = ctx.getString(R.string.semantics_no_due);
		} else if (due % 365 == 0 && due != 0) {
			dueDialogValue = due / 365;
			dueDialogDayYear = VALUE.YEAR;
			summary = dueDialogValue
					+ " "
					+ ctx.getResources().getQuantityString(R.plurals.due_year,
							dueDialogValue);
		} else if (due % 30 == 0 && due != 0) {
			dueDialogValue = due / 30;
			dueDialogDayYear = VALUE.MONTH;
			summary = dueDialogValue
					+ " "
					+ ctx.getResources().getQuantityString(R.plurals.due_month,
							dueDialogValue);
		} else {
			dueDialogValue = due;
			dueDialogDayYear = VALUE.DAY;
			summary = dueDialogValue
					+ " "
					+ ctx.getResources().getQuantityString(R.plurals.due_day,
							dueDialogValue);
		}
		return summary;
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private Preference findPreference(String key) {
		if (v4_0) {
			return ((SemanticsSettingsFragment) settings).findPreference(key);
		} else {
			return ((SemanticsSettingsActivity) settings).findPreference(key);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object nv) {
		String newValue = String.valueOf(nv);
		String key = preference.getKey();
		if (key.equals("semantics_priority")) {
			if (newValue.equals("null")) {
				semantic.setPriority(null);
				semanticsPriority.setValueIndex(0);
				semanticsPriority.setSummary(semanticsPriority.getEntries()[0]);
			} else {
				semantic.setPriority(Integer.parseInt(newValue));
				semanticsPriority.setValue(newValue);
				semanticsPriority.setSummary(newValue);
			}
			semantic.save();
		} else if (key.equals("semantics_due")) {

		} else if (key.equals("semantics_list")) {
			if (newValue.equals("null")) {
				semantic.setList(null);
				semanticsList.setValueIndex(0);
				semanticsList.setSummary(semanticsList.getEntries()[0]);
			} else {
				ListMirakel newList = ListMirakel.getList(Integer
						.parseInt(newValue));
				semantic.setList(newList);
				semanticsList.setValue(newValue);
				semanticsList.setSummary(newList.getName());
			}
			semantic.save();
		} else if (key.equals("semantics_condition")) {
			semantic.setCondition(newValue);
			semantic.save();
			semanticsCondition.setSummary(newValue);
			semanticsCondition.setText(newValue);
			if(Helpers.isTablet(ctx)&&v4_0){
				((ListSettings)ctx).invalidateHeaders();
			}
		}
		return false;
	}
	
}
