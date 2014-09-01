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

package de.azapps.mirakel.settings.activities;

import android.content.DialogInterface;
import android.net.Uri;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import java.util.List;

import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.generic_list.GenericListSettingActivity;
import de.azapps.mirakel.settings.generic_list.GenericSettingsFragment;
import de.azapps.tools.OptionalUtils;
import de.azapps.widgets.DueDialog;

public class SemanticsSettingsActivity extends GenericListSettingActivity<Semantic> implements
    Preference.OnPreferenceChangeListener {
    private EditTextPreference semanticsCondition;
    private ListPreference semanticsPriority;
    private Preference semanticsDue;
    private ListPreference semanticsWeekday;
    private ListPreference semanticsList;
    private Semantic semantic;
    protected int dueDialogValue;
    private DueDialog.VALUE dueDialogDayYear;

    @Override
    protected void createModel() {
        Semantic semantic = Semantic.newSemantic(getString(R.string.semantic_new), null,
                            null, Optional.<ListMirakel>absent(), null);
        selectItem(semantic);
    }

    @NonNull
    @Override
    public String getTitle(Optional<Semantic> model) {
        if (model.isPresent()) {
            return model.get().getName();
        } else {
            return getString(R.string.no_semantic_selected);
        }
    }

    @Override
    public int getPreferenceResource() {
        return R.xml.settings_semantics;
    }

    @Override
    public Uri getUri() {
        return MirakelInternalContentProvider.SEMANTIC_URI;
    }

    @Override
    public Class<Semantic> getMyClass() {
        return Semantic.class;
    }


    @Override
    public void setUp(Optional<Semantic> model, GenericSettingsFragment fragment) {
        if (!model.isPresent()) {
            return;
        }
        final Semantic semantic = model.get();
        this.semantic = semantic;
        this.semanticsCondition = (EditTextPreference) fragment.findPreference("semantics_condition");
        this.semanticsCondition.setOnPreferenceChangeListener(this);
        this.semanticsCondition.setText(semantic.getCondition());
        this.semanticsCondition.setSummary(semantic.getCondition());
        // Priority
        this.semanticsPriority = (ListPreference) fragment.findPreference("semantics_priority");
        this.semanticsPriority.setOnPreferenceChangeListener(this);
        this.semanticsPriority.setEntries(R.array.priority_entries);
        this.semanticsPriority.setEntryValues(R.array.priority_entry_values);
        if (semantic.getPriority() == null) {
            this.semanticsPriority.setValueIndex(0);
            this.semanticsPriority.setSummary(getResources()
                                              .getStringArray(R.array.priority_entries)[0]);
        } else {
            this.semanticsPriority.setValue(semantic.getPriority()
                                            .toString());
            this.semanticsPriority
            .setSummary(this.semanticsPriority.getValue());
        }
        // Due
        this.semanticsDue = fragment.findPreference("semantics_due");
        this.semanticsDue.setOnPreferenceChangeListener(this);
        this.semanticsDue.setSummary(updateDueStuff());
        this.semanticsDue
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final DueDialog dueDialog = new DueDialog(
                    SemanticsSettingsActivity.this, false);
                dueDialog.setTitle(semanticsDue.getTitle());
                dueDialog.setValue(SemanticsSettingsActivity.this.dueDialogValue);
                dueDialog.setNegativeButton(android.R.string.cancel,
                                            null);
                dueDialog.setNeutralButton(R.string.no_date,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which) {
                        semantic.setDue(null);
                        semanticsDue.setSummary(updateDueStuff());
                        semantic.save();
                    }
                });
                dueDialog.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which) {
                        final int val = dueDialog.getValue();
                        final DueDialog.VALUE dayYear = dueDialog
                                                        .getDayYear();
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
                        case HOUR:
                        case MINUTE:
                        default:
                            // The other things aren't shown in
                            // the dialog so we haven't to care
                            // about them
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
        // Weekday
        final Integer weekday = semantic.getWeekday();
        this.semanticsWeekday = (ListPreference) fragment.findPreference("semantics_weekday");
        this.semanticsWeekday.setOnPreferenceChangeListener(this);
        this.semanticsWeekday.setEntries(R.array.weekdays);
        final CharSequence[] weekdaysNum = {"0", "1", "2", "3", "4", "5", "6",
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
        this.semanticsList = (ListPreference) fragment.findPreference("semantics_list");
        this.semanticsList.setOnPreferenceChangeListener(this);
        final List<ListMirakel> lists = ListMirakel.all(false);
        final CharSequence[] listEntries = new CharSequence[lists.size() + 1];
        final CharSequence[] listValues = new CharSequence[lists.size() + 1];
        listEntries[0] = getString(R.string.semantics_no_list);
        listValues[0] = "null";
        for (int i = 0; i < lists.size(); i++) {
            listValues[i + 1] = String.valueOf(lists.get(i).getId());
            listEntries[i + 1] = lists.get(i).getName();
        }
        this.semanticsList.setEntries(listEntries);
        this.semanticsList.setEntryValues(listValues);
        if (!semantic.getList().isPresent()) {
            this.semanticsList.setValueIndex(0);
            this.semanticsList.setSummary(getString(R.string.semantics_no_list));
        } else {
            ListMirakel listMirakel = semantic.getList().get();
            this.semanticsList.setValue(String.valueOf(listMirakel
                                        .getId()));
            this.semanticsList.setSummary(listMirakel.getName());
        }
    }


    @Override
    public boolean onPreferenceChange(final Preference preference,
                                      final Object nv) {
        final String newValue = String.valueOf(nv);
        final String key = preference.getKey();
        if (key.equals("semantics_priority")) {
            if (newValue.equals("null")) {
                semantic.setPriority(null);
                this.semanticsPriority.setValueIndex(0);
                this.semanticsPriority.setSummary(this.semanticsPriority
                                                  .getEntries()[0]);
            } else {
                semantic.setPriority(Integer.parseInt(newValue));
                this.semanticsPriority.setValue(newValue);
                this.semanticsPriority.setSummary(newValue);
            }
            semantic.save();
        } else if (key.equals("semantics_due")) {
        } else if (key.equals("semantics_weekday")) {
            Integer weekday = Integer.parseInt(newValue);
            if (weekday == 0) {
                weekday = null;
            }
            semantic.setWeekday(weekday);
            this.semanticsWeekday.setValue(newValue);
            this.semanticsWeekday.setSummary(this.semanticsWeekday.getEntry());
            semantic.save();
        } else if (key.equals("semantics_list")) {
            if (newValue.equals("null")) {
                semantic.setList(null);
                this.semanticsList.setValueIndex(0);
                this.semanticsList
                .setSummary(this.semanticsList.getEntries()[0]);
            } else {
                final Optional<ListMirakel> newList = ListMirakel.get(Integer
                                                      .parseInt(newValue));
                semantic.setList(newList);
                this.semanticsList.setValue(newValue);
                OptionalUtils.withOptional(newList, new OptionalUtils.Procedure<ListMirakel>() {
                    @Override
                    public void apply(ListMirakel input) {
                        semanticsList.setSummary(input.getName());
                    }
                });
            }
            semantic.save();
        } else if (key.equals("semantics_condition")) {
            semantic.setCondition(newValue);
            semantic.save();
            this.semanticsCondition.setSummary(newValue);
            this.semanticsCondition.setText(newValue);
        }
        return false;
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
            this.dueDialogDayYear = DueDialog.VALUE.DAY;
            this.dueDialogValue = 0;
            summary = getString(R.string.semantics_no_due);
        } else if (due % 365 == 0 && due != 0) {
            this.dueDialogValue = due / 365;
            this.dueDialogDayYear = DueDialog.VALUE.YEAR;
            summary = this.dueDialogValue
                      + " "
                      + getResources().getQuantityString(
                          R.plurals.due_year, this.dueDialogValue);
        } else if (due % 30 == 0 && due != 0) {
            this.dueDialogValue = due / 30;
            this.dueDialogDayYear = DueDialog.VALUE.MONTH;
            summary = this.dueDialogValue
                      + " "
                      + getResources().getQuantityString(
                          R.plurals.due_month, this.dueDialogValue);
        } else {
            this.dueDialogValue = due;
            this.dueDialogDayYear = DueDialog.VALUE.DAY;
            summary = this.dueDialogValue
                      + " "
                      + getResources().getQuantityString(
                          R.plurals.due_day, this.dueDialogValue);
        }
        return summary;
    }

}
