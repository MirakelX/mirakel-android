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

package de.azapps.mirakel.settings.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.UndoHistory;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.activities.RecurringSettingsActivity;
import de.azapps.mirakel.settings.activities.TagsSettingsActivity;
import de.azapps.tools.FileUtils;

public class DevSettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings_dev);


        // Delete done tasks
        final Preference deleteDone = findPreference("deleteDone");
        deleteDone
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(
                final Preference preference) {
                new AlertDialog.Builder(
                    getActivity())
                .setTitle(R.string.delete_done_warning)
                .setMessage(
                    R.string.delete_done_warning_message)
                .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialogInterface,
                        final int i) {
                        Task.deleteDoneTasks();
                        Toast.makeText(
                            getActivity(),
                            R.string.delete_done_success,
                            Toast.LENGTH_SHORT)
                        .show();
                        android.os.Process
                        .killProcess(android.os.Process
                                     .myPid());
                    }
                })
                .setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialogInterface,
                        final int i) {
                    }
                }).show();
                return true;
            }
        });

        final Preference undoNumber = findPreference("UndoNumber");
        undoNumber.setSummary(getString(
                                  R.string.undo_number_summary,
                                  MirakelCommonPreferences.getUndoNumber()));
        undoNumber
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(
                final Preference preference) {
                final int old_val = MirakelCommonPreferences
                                    .getUndoNumber();
                final int max = 25;
                final int min = 1;
                final NumberPicker numberPicker;
                numberPicker = new NumberPicker(getActivity());
                numberPicker.setMaxValue(max);
                numberPicker.setMinValue(min);
                numberPicker.setWrapSelectorWheel(false);
                numberPicker.setValue(old_val);
                numberPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                new AlertDialog.Builder(
                    getActivity())
                .setTitle(R.string.undo_number)
                .setMessage(
                    getActivity()
                    .getString(
                        R.string.undo_number_summary,
                        MirakelCommonPreferences
                        .getUndoNumber()))
                .setView(numberPicker)
                .setPositiveButton(
                    android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int whichButton) {
                        final SharedPreferences.Editor editor = MirakelPreferences
                                                                .getEditor();
                        int val = numberPicker.getValue();
                        editor.putInt("UndoNumber", val);
                        undoNumber
                        .setSummary(getActivity()
                                    .getString(
                                        R.string.undo_number_summary,
                                        val));
                        if (old_val > val) {
                            for (int i = val; i < max; i++) {
                                editor.putString(
                                    UndoHistory.UNDO
                                    + i,
                                    "");
                            }
                        }
                        editor.commit();
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
                return true;
            }
        });

        final CheckBoxPreference demoMode = (CheckBoxPreference) findPreference("demoMode");
        demoMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference,
                                              final Object newValue) {
                MirakelCommonPreferences.setDemoMode((Boolean) newValue);
                Helpers.restartApp(getActivity());
                return false;
            }
        });

        final Preference demoDropDB = findPreference("demoDropDB");
        demoDropDB
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(
                final Preference preference) {
                final File db = new File(FileUtils.getMirakelDir(),
                                         "databases/"
                                         + MirakelModelPreferences
                                         .getDBName());
                db.delete();
                Helpers.restartApp(getActivity());
                return false;
            }
        });

        final Intent startRecurringIntent = new Intent(getActivity(),
                RecurringSettingsActivity.class);
        final Preference recurring = findPreference("recurring");
        recurring.setIntent(startRecurringIntent);

        final Intent startTagIntent = new Intent(getActivity(),
                TagsSettingsActivity.class);
        final Preference tag = findPreference("tags");
        tag.setIntent(startTagIntent);
    }
}
