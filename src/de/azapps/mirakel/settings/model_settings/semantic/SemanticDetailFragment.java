/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
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

package de.azapps.mirakel.settings.model_settings.semantic;

import android.content.DialogInterface;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import java.util.List;

import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelDetailFragment;
import de.azapps.tools.OptionalUtils;
import de.azapps.widgets.DueDialog;

public class SemanticDetailFragment extends GenericModelDetailFragment<Semantic> implements
    Preference.OnPreferenceChangeListener {

    private static final String NULL_STR = "null";
    private EditTextPreference semanticsCondition;
    private ListPreference semanticsPriority;
    private Preference semanticsDue;
    private int dueDialogValue;
    private ListPreference semanticsWeekday;
    private ListPreference semanticsList;
    private DueDialog.VALUE dueDialogDayYear;

    @NonNull
    @Override
    protected Semantic getDummyItem() {
        final Optional<Semantic> s = Semantic.first();
        if (!s.isPresent()) {
            return Semantic.newSemantic("", null, null, Optional.<ListMirakel>absent(), null);
        } else {
            return s.get();
        }
    }

    @Override
    protected int getResourceId() {
        return R.xml.settings_semantics;
    }

    @Override
    protected void setUp() {
        this.semanticsCondition = (EditTextPreference) findPreference("semantics_condition");
        this.semanticsCondition.setOnPreferenceChangeListener(this);
        this.semanticsCondition.setText(mItem.getCondition());
        this.semanticsCondition.setSummary(mItem.getCondition());
        // Priority
        this.semanticsPriority = (ListPreference) findPreference("semantics_priority");
        this.semanticsPriority.setOnPreferenceChangeListener(this);
        this.semanticsPriority.setEntries(R.array.priority_entries);
        this.semanticsPriority.setEntryValues(R.array.priority_entry_values);
        if (mItem.getPriority() == null) {
            this.semanticsPriority.setValueIndex(0);
            this.semanticsPriority.setSummary(getResources()
                                              .getStringArray(R.array.priority_entries)[0]);
        } else {
            this.semanticsPriority.setValue(mItem.getPriority()
                                            .toString());
            this.semanticsPriority
            .setSummary(this.semanticsPriority.getValue());
        }
        // Due
        this.semanticsDue = findPreference("semantics_due");
        this.semanticsDue.setOnPreferenceChangeListener(this);
        this.semanticsDue.setSummary(updateDueStuff());
        this.semanticsDue
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final DueDialog dueDialog = new DueDialog(
                    getActivity(), false);
                dueDialog.setTitle(semanticsDue.getTitle());
                dueDialog.setValue(dueDialogValue);
                dueDialog.setNegativeButton(android.R.string.cancel,
                                            null);
                dueDialog.setNeutralButton(R.string.no_date,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which) {
                        mItem.setDue(null);
                        semanticsDue.setSummary(updateDueStuff());
                        mItem.save();
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
                            mItem.setDue(val);
                            break;
                        case MONTH:
                            mItem.setDue(val * 30);
                            break;
                        case YEAR:
                            mItem.setDue(val * 365);
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
                        mItem.save();
                    }
                });
                dueDialog.show();
                return false;
            }
        });
        // Weekday
        final Integer weekday = mItem.getWeekday();
        this.semanticsWeekday = (ListPreference) findPreference("semantics_weekday");
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
        this.semanticsList = (ListPreference) findPreference("semantics_list");
        this.semanticsList.setOnPreferenceChangeListener(this);
        final List<ListMirakel> lists = ListMirakel.all(false);
        final CharSequence[] listEntries = new CharSequence[lists.size() + 1];
        final CharSequence[] listValues = new CharSequence[lists.size() + 1];
        listEntries[0] = getString(R.string.semantics_no_list);
        listValues[0] = NULL_STR;
        for (int i = 0; i < lists.size(); i++) {
            listValues[i + 1] = String.valueOf(lists.get(i).getId());
            listEntries[i + 1] = lists.get(i).getName();
        }
        this.semanticsList.setEntries(listEntries);
        this.semanticsList.setEntryValues(listValues);
        if (!mItem.getList().isPresent()) {
            this.semanticsList.setValueIndex(0);
            this.semanticsList.setSummary(getString(R.string.semantics_no_list));
        } else {
            final ListMirakel listMirakel = mItem.getList().get();
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
        switch (key) {
        case "semantics_priority":
            if (NULL_STR.equals(newValue)) {
                mItem.setPriority(null);
                this.semanticsPriority.setValueIndex(0);
                this.semanticsPriority.setSummary(this.semanticsPriority
                                                  .getEntries()[0]);
            } else {
                mItem.setPriority(Integer.parseInt(newValue));
                this.semanticsPriority.setValue(newValue);
                this.semanticsPriority.setSummary(newValue);
            }
            break;
        case "semantics_weekday":
            Integer weekday = Integer.parseInt(newValue);
            if (weekday == 0) {
                weekday = null;
            }
            mItem.setWeekday(weekday);
            this.semanticsWeekday.setValue(newValue);
            this.semanticsWeekday.setSummary(this.semanticsWeekday.getEntry());
            break;
        case "semantics_list":
            if (NULL_STR.equals(newValue)) {
                mItem.setList(Optional.<ListMirakel>absent());
                this.semanticsList.setValueIndex(0);
                this.semanticsList.setSummary(this.semanticsList.getEntries()[0]);
            } else {
                final Optional<ListMirakel> newList = ListMirakel.get(Integer.parseInt(newValue));
                mItem.setList(newList);
                this.semanticsList.setValue(newValue);
                OptionalUtils.withOptional(newList, new OptionalUtils.Procedure<ListMirakel>() {
                    @Override
                    public void apply(ListMirakel input) {
                        semanticsList.setSummary(input.getName());
                    }
                });
            }
            break;
        case "semantics_condition":
            mItem.setCondition(newValue);
            this.semanticsCondition.setSummary(newValue);
            this.semanticsCondition.setText(newValue);
            updateList();
            break;
        default:
            return false;
        }
        mItem.save();
        return false;
    }


    /**
     * Updates the variables for the due Dialog and returns the summary for the
     * Due-Preference
     *
     * @return
     */
    protected String updateDueStuff() {
        final Integer due = mItem.getDue();
        final String summary;
        if (due == null) {
            this.dueDialogDayYear = DueDialog.VALUE.DAY;
            this.dueDialogValue = 0;
            summary = getString(R.string.semantics_no_due);
        } else if (((due % 365) == 0) && (due != 0)) {
            this.dueDialogValue = due / 365;
            this.dueDialogDayYear = DueDialog.VALUE.YEAR;
            summary = this.dueDialogValue
                      + " "
                      + getResources().getQuantityString(
                          R.plurals.due_year, this.dueDialogValue);
        } else if (((due % 30) == 0) && (due != 0)) {
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
