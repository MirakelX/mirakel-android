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
package de.azapps.mirakel.settings.semantics;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;

import com.google.common.base.Optional;

import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.PreferencesHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakel.settings.R;
import de.azapps.tools.OptionalUtils;
import de.azapps.widgets.DueDialog;
import de.azapps.widgets.DueDialog.VALUE;

public class SemanticsSettings extends PreferencesHelper implements
    OnPreferenceChangeListener {
    protected Semantic semantic;

    protected AlertDialog alert;
    private EditTextPreference semanticsCondition;
    private ListPreference semanticsList, semanticsPriority, semanticsWeekday;
    protected Preference semanticsDue;
    protected int dueDialogValue;
    private VALUE dueDialogDayYear;

    @SuppressLint("NewApi")
    public SemanticsSettings(final SemanticsSettingsFragment activity,
                             final Semantic semantic) {
        super(activity);
        this.semantic = semantic;
    }

    public SemanticsSettings(final SemanticsSettingsActivity activity,
                             final Semantic semantic) {
        super(activity);
        this.semantic = semantic;
    }

    public void setup() {
        this.semanticsCondition = (EditTextPreference) findPreference("semantics_condition");
        this.semanticsCondition.setOnPreferenceChangeListener(this);
        this.semanticsCondition.setText(this.semantic.getCondition());
        this.semanticsCondition.setSummary(this.semantic.getCondition());
        // Priority
        this.semanticsPriority = (ListPreference) findPreference("semantics_priority");
        this.semanticsPriority.setOnPreferenceChangeListener(this);
        this.semanticsPriority.setEntries(R.array.priority_entries);
        this.semanticsPriority.setEntryValues(R.array.priority_entry_values);
        if (this.semantic.getPriority() == null) {
            this.semanticsPriority.setValueIndex(0);
            this.semanticsPriority.setSummary(this.activity.getResources()
                                              .getStringArray(R.array.priority_entries)[0]);
        } else {
            this.semanticsPriority.setValue(this.semantic.getPriority()
                                            .toString());
            this.semanticsPriority
            .setSummary(this.semanticsPriority.getValue());
        }
        // Due
        this.semanticsDue = findPreference("semantics_due");
        this.semanticsDue.setOnPreferenceChangeListener(this);
        this.semanticsDue.setSummary(updateDueStuff());
        this.semanticsDue
        .setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final DueDialog dueDialog = new DueDialog(
                    SemanticsSettings.this.activity, false);
                dueDialog.setTitle(SemanticsSettings.this.semanticsDue
                                   .getTitle());
                dueDialog.setValue(
                    SemanticsSettings.this.dueDialogValue,
                    SemanticsSettings.this.dueDialogDayYear);
                dueDialog.setNegativeButton(android.R.string.cancel,
                                            null);
                dueDialog.setNeutralButton(R.string.no_date,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which) {
                        SemanticsSettings.this.semantic
                        .setDue(null);
                        SemanticsSettings.this.semanticsDue
                        .setSummary(updateDueStuff());
                        SemanticsSettings.this.semantic.save();
                    }
                });
                dueDialog.setPositiveButton(android.R.string.ok,
                new OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which) {
                        final int val = dueDialog.getValue();
                        final VALUE dayYear = dueDialog
                                              .getDayYear();
                        switch (dayYear) {
                        case DAY:
                            SemanticsSettings.this.semantic
                            .setDue(val);
                            break;
                        case MONTH:
                            SemanticsSettings.this.semantic
                            .setDue(val * 30);
                            break;
                        case YEAR:
                            SemanticsSettings.this.semantic
                            .setDue(val * 365);
                            break;
                        case HOUR:
                        case MINUTE:
                        default:
                            // The other things aren't shown in
                            // the dialog so we haven't to care
                            // about them
                            break;
                        }
                        SemanticsSettings.this.semanticsDue
                        .setSummary(updateDueStuff());
                        SemanticsSettings.this.semantic.save();
                    }
                });
                dueDialog.show();
                return false;
            }
        });
        // Weekday
        final Integer weekday = this.semantic.getWeekday();
        this.semanticsWeekday = (ListPreference) findPreference("semantics_weekday");
        this.semanticsWeekday.setOnPreferenceChangeListener(this);
        this.semanticsWeekday.setEntries(R.array.weekdays);
        final CharSequence[] weekdaysNum = { "0", "1", "2", "3", "4", "5", "6",
                                             "7"
                                           };
        this.semanticsWeekday.setEntryValues(weekdaysNum);
        if (weekday == null) {
            this.semanticsWeekday.setValueIndex(0);
        } else {
            this.semanticsWeekday.setValueIndex(weekday);
        }
        this.semanticsWeekday.setSummary(this.semanticsWeekday.getEntry());
        // List
        this.semanticsList = (ListPreference) findPreference("semantics_list");
        this.semanticsList.setOnPreferenceChangeListener(this);
        final List<ListMirakel> lists = ListMirakel.all(false);
        final CharSequence[] listEntries = new CharSequence[lists.size() + 1];
        final CharSequence[] listValues = new CharSequence[lists.size() + 1];
        listEntries[0] = this.activity.getString(R.string.semantics_no_list);
        listValues[0] = "null";
        for (int i = 0; i < lists.size(); i++) {
            listValues[i + 1] = String.valueOf(lists.get(i).getId());
            listEntries[i + 1] = lists.get(i).getName();
        }
        this.semanticsList.setEntries(listEntries);
        this.semanticsList.setEntryValues(listValues);
        if (!this.semantic.getList().isPresent()) {
            this.semanticsList.setValueIndex(0);
            this.semanticsList.setSummary(this.activity
                                          .getString(R.string.semantics_no_list));
        } else {
            ListMirakel listMirakel = this.semantic.getList().get();
            this.semanticsList.setValue(String.valueOf(listMirakel
                                        .getId()));
            this.semanticsList.setSummary(listMirakel.getName());
        }
    }

    /**
     * Updates the variables for the due Dialog and returns the summary for the
     * Due-Preference
     *
     * @return
     */
    protected String updateDueStuff() {
        final Integer due = this.semantic.getDue();
        String summary;
        if (due == null) {
            this.dueDialogDayYear = VALUE.DAY;
            this.dueDialogValue = 0;
            summary = this.activity.getString(R.string.semantics_no_due);
        } else if (due % 365 == 0 && due != 0) {
            this.dueDialogValue = due / 365;
            this.dueDialogDayYear = VALUE.YEAR;
            summary = this.dueDialogValue
                      + " "
                      + this.activity.getResources().getQuantityString(
                          R.plurals.due_year, this.dueDialogValue);
        } else if (due % 30 == 0 && due != 0) {
            this.dueDialogValue = due / 30;
            this.dueDialogDayYear = VALUE.MONTH;
            summary = this.dueDialogValue
                      + " "
                      + this.activity.getResources().getQuantityString(
                          R.plurals.due_month, this.dueDialogValue);
        } else {
            this.dueDialogValue = due;
            this.dueDialogDayYear = VALUE.DAY;
            summary = this.dueDialogValue
                      + " "
                      + this.activity.getResources().getQuantityString(
                          R.plurals.due_day, this.dueDialogValue);
        }
        return summary;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onPreferenceChange(final Preference preference,
                                      final Object nv) {
        final String newValue = String.valueOf(nv);
        final String key = preference.getKey();
        if (key.equals("semantics_priority")) {
            if (newValue.equals("null")) {
                this.semantic.setPriority(null);
                this.semanticsPriority.setValueIndex(0);
                this.semanticsPriority.setSummary(this.semanticsPriority
                                                  .getEntries()[0]);
            } else {
                this.semantic.setPriority(Integer.parseInt(newValue));
                this.semanticsPriority.setValue(newValue);
                this.semanticsPriority.setSummary(newValue);
            }
            this.semantic.save();
        } else if (key.equals("semantics_due")) {
        } else if (key.equals("semantics_weekday")) {
            Integer weekday = Integer.parseInt(newValue);
            if (weekday == 0) {
                weekday = null;
            }
            this.semantic.setWeekday(weekday);
            this.semanticsWeekday.setValue(newValue);
            this.semanticsWeekday.setSummary(this.semanticsWeekday.getEntry());
            this.semantic.save();
        } else if (key.equals("semantics_list")) {
            if (newValue.equals("null")) {
                this.semantic.setList(null);
                this.semanticsList.setValueIndex(0);
                this.semanticsList
                .setSummary(this.semanticsList.getEntries()[0]);
            } else {
                final Optional<ListMirakel> newList = ListMirakel.get(Integer
                                                      .parseInt(newValue));
                this.semantic.setList(newList);
                this.semanticsList.setValue(newValue);
                OptionalUtils.withOptional(newList, new OptionalUtils.Procedure<ListMirakel>() {
                    @Override
                    public void apply(ListMirakel input) {
                        semanticsList.setSummary(input.getName());
                    }
                });
            }
            this.semantic.save();
        } else if (key.equals("semantics_condition")) {
            this.semantic.setCondition(newValue);
            this.semantic.save();
            this.semanticsCondition.setSummary(newValue);
            this.semanticsCondition.setText(newValue);
            if (MirakelCommonPreferences.isTablet() && this.v4_0) {
                ((ListSettings) this.activity).invalidateHeaders();
            }
        }
        return false;
    }

}
