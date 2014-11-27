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

package de.azapps.mirakel.settings.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.Settings;
import de.azapps.mirakel.settings.model_settings.semantic.SemanticSettingsActivity;

public class TaskSettingsFragment extends MirakelPreferencesFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings_tasks);


        final Intent startSemanticsIntent = new Intent(getActivity(),
                SemanticSettingsActivity.class);
        final Preference semantics = findPreference("semanticNewTaskSettings");
        if (semantics != null) {
            semantics.setIntent(startSemanticsIntent);
        }


        final CheckBoxPreference subTaskAddToSameList = (CheckBoxPreference)
                findPreference("subtaskAddToSameList");
        if (subTaskAddToSameList != null) {
            final Optional<ListMirakel> subtaskAddToList = MirakelModelPreferences
                    .subtaskAddToList();
            if (!MirakelCommonPreferences.addSubtaskToSameList()
                && subtaskAddToList.isPresent()) {
                subTaskAddToSameList.setSummary(getString(
                                                    R.string.settings_subtask_add_to_list_summary,
                                                    subtaskAddToList.get().getName()));
            } else {
                subTaskAddToSameList
                .setSummary(R.string.settings_subtask_add_to_same_list_summary);
            }
            subTaskAddToSameList
            .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(
                    final Preference preference,
                    final Object newValue) {
                    if (!(Boolean) newValue) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(
                            getActivity());
                        builder.setTitle(R.string.import_to);
                        final List<CharSequence> items = new ArrayList<>();
                        final List<Long> list_ids = new ArrayList<>();
                        final int currentItem = 0;
                        for (final ListMirakel list : ListMirakel.all()) {
                            if (list.getId() > 0) {
                                items.add(list.getName());
                                list_ids.add(list.getId());
                            }
                        }
                        builder.setSingleChoiceItems(
                            items.toArray(new CharSequence[items
                                                           .size()]), currentItem,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                final DialogInterface dialog,
                                final int item) {
                                subTaskAddToSameList
                                .setSummary(items
                                            .get(item));
                                subTaskAddToSameList
                                .setSummary(getString(
                                                R.string.settings_subtask_add_to_list_summary,
                                                items.get(item)));
                                final SharedPreferences.Editor editor = MirakelPreferences
                                                                        .getEditor();
                                editor.putLong(
                                    "subtaskAddToList",
                                    list_ids.get(item));
                                editor.commit();
                                dialog.dismiss();
                            }
                        });
                        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(
                                final DialogInterface dialog) {
                                subTaskAddToSameList.setChecked(false);
                                subTaskAddToSameList
                                .setSummary(R.string.settings_subtask_add_to_same_list_summary);
                            }
                        });
                        builder.create().show();
                    } else {
                        subTaskAddToSameList
                        .setSummary(R.string.settings_subtask_add_to_same_list_summary);
                    }
                    return true;
                }
            });
        }
    }

    @NonNull
    @Override
    public Settings getItem() {
        return Settings.TASK;
    }
}
