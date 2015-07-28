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

package de.azapps.mirakel.settings.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import java.io.File;

import de.azapps.mirakel.helper.AnalyticsWrapperBase;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.custom_views.Settings;
import de.azapps.mirakel.settings.model_settings.tag.TagSettingsActivity;
import de.azapps.tools.FileUtils;

public class DevSettingsFragment extends MirakelPreferencesFragment<Settings> {
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
                new AlertDialogWrapper.Builder(
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


        final SwitchPreference demoMode = (SwitchPreference) findPreference("demoMode");
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

        final Intent startTagIntent = new Intent(getActivity(),
                TagSettingsActivity.class);
        final Preference tag = findPreference("tags");
        tag.setIntent(startTagIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsWrapperBase.setScreen(this);
    }

    @NonNull
    @Override
    public Settings getItem() {
        return Settings.DEV;
    }
}
