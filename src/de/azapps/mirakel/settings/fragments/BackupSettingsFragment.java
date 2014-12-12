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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.export_import.AnyDoImport;
import de.azapps.mirakel.helper.export_import.ExportImport;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.Settings;
import de.azapps.mirakel.settings.SettingsActivity;
import de.azapps.tools.FileUtils;

public class BackupSettingsFragment extends MirakelPreferencesFragment<Settings> {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings_backup);


        final CheckBoxPreference importDefaultList = (CheckBoxPreference)
                findPreference("importDefaultList");
        final Optional<ListMirakel> list = MirakelModelPreferences
                                           .getImportDefaultList();
        if (list.isPresent()) {
            importDefaultList.setSummary(getString(
                                             R.string.import_default_list_summary, list.get().getName()));
        } else {
            importDefaultList
            .setSummary(R.string.import_no_default_list_summary);
        }
        importDefaultList
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference,
                final Object newValue) {
                if ((Boolean) newValue) {
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
                            importDefaultList
                            .setSummary(getActivity()
                                        .getString(
                                            R.string.import_default_list_summary,
                                            items.get(item)));
                            final SharedPreferences.Editor editor = MirakelPreferences
                                                                    .getEditor();
                            editor.putLong(
                                "defaultImportList",
                                list_ids.get(item));
                            editor.commit();
                            dialog.dismiss();
                        }
                    });
                    builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(
                            final DialogInterface dialog) {
                            importDefaultList.setChecked(false);
                            importDefaultList
                            .setSummary(R.string.import_no_default_list_summary);
                        }
                    });
                    builder.create().show();
                } else {
                    importDefaultList
                    .setSummary(R.string.import_no_default_list_summary);
                }
                return true;
            }
        });

        final EditTextPreference importFileType = (EditTextPreference) findPreference("import_file_title");
        if (importFileType != null) {
            importFileType.setSummary(MirakelCommonPreferences
                                      .getImportFileTitle());
        }
        final Preference backup = findPreference("backup");
        backup.setSummary(getString(
                              R.string.backup_click_summary, FileUtils.getExportDir()
                              .getAbsolutePath()));
        backup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                ExportImport.exportDB(getActivity());
                return true;
            }
        });

        final Preference importDB = findPreference("import");
        importDB.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                Helpers.showFileChooser(SettingsActivity.FILE_IMPORT_DB,
                                        getActivity()
                                        .getString(R.string.import_title),
                                        getActivity());
                return true;
            }
        });

        final Preference importAstrid = findPreference("import_astrid");
        importAstrid
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(
                final Preference preference) {
                Helpers.showFileChooser(
                    SettingsActivity.FILE_ASTRID,
                    getActivity()
                    .getString(R.string.astrid_import_title),
                    getActivity());
                return true;
            }
        });

        final Preference anyDo = findPreference("import_any_do");
        anyDo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                AnyDoImport
                .handleImportAnyDo(getActivity());
                return true;
            }
        });

        final Preference importWunderlist = findPreference("import_wunderlist");
        importWunderlist
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(
                final Preference preference) {
                new AlertDialog.Builder(
                    getActivity())
                .setTitle(R.string.import_wunderlist_howto)
                .setMessage(
                    R.string.import_wunderlist_howto_text)
                .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which) {
                        Helpers.showFileChooser(
                            SettingsActivity.FILE_WUNDERLIST,
                            getActivity()
                            .getString(R.string.import_wunderlist_title),
                            getActivity());
                    }
                }).show();
                return true;
            }
        });

        final Preference autoBackupInterval = findPreference("autoBackupInterval");
        autoBackupInterval.setSummary(getString(
                                          R.string.auto_backup_interval_summary,
                                          MirakelCommonPreferences.getAutoBackupInterval()));
        autoBackupInterval
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(
                final Preference preference) {
                final int old_val = MirakelCommonPreferences
                                    .getAutoBackupInterval();
                final int max = 31;
                final int min = 0;
                final NumberPicker numberPicker = new NumberPicker(
                    getActivity());
                numberPicker.setMaxValue(max);
                numberPicker.setMinValue(min);
                numberPicker.setWrapSelectorWheel(false);
                numberPicker.setValue(old_val);
                numberPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                new AlertDialog.Builder(
                    getActivity())
                .setTitle(R.string.auto_backup_interval)
                .setView(numberPicker)
                .setPositiveButton(
                    android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int whichButton) {
                        int val = numberPicker.getValue();
                        MirakelCommonPreferences.setAutoBackupInterval(val);
                        autoBackupInterval.setSummary(getActivity()
                                                      .getString(
                                                          R.string.auto_backup_interval_summary,
                                                          val));
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
                return false;
            }
        });
    }

    @NonNull
    @Override
    public Settings getItem() {
        return Settings.BACKUP;
    }
}
