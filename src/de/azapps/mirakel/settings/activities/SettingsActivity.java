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

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.helper.export_import.AnyDoImport;
import de.azapps.mirakel.helper.export_import.ExportImport;
import de.azapps.mirakel.helper.export_import.WunderlistImport;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.fragments.AboutSettingsFragment;
import de.azapps.mirakel.settings.fragments.BackupSettingsFragment;
import de.azapps.mirakel.settings.fragments.DevSettingsFragment;
import de.azapps.mirakel.settings.fragments.NotificationSettingsFragment;
import de.azapps.mirakel.settings.fragments.TaskSettingsFragment;
import de.azapps.mirakel.settings.fragments.UISettingsFragment;
import de.azapps.mirakel.settings.taskfragment.TaskFragmentSettingsFragment;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

public class SettingsActivity extends PreferenceActivity {

    @NonNull
    private FRAGMENTS currentFragment = FRAGMENTS.UI;

    private enum FRAGMENTS {
        ABOUT, BACKUP, DEV, NOTIFICATION, TASK, UI, TASKUI;

        @Override
        public String toString() {
            switch (this) {
            case ABOUT:
                return AboutSettingsFragment.class.getName();
            case BACKUP:
                return BackupSettingsFragment.class.getName();
            case DEV:
                return DevSettingsFragment.class.getName();
            case NOTIFICATION:
                return NotificationSettingsFragment.class.getName();
            case TASK:
                return TaskSettingsFragment.class.getName();
            case UI:
                return UISettingsFragment.class.getName();
            case TASKUI:
                return TaskFragmentSettingsFragment.class.getName();
            }
            return super.toString();
        }

        public void restoreFragment(final @NonNull PreferenceActivity settingsActivity) {
            switch (this) {
            case TASKUI:
                settingsActivity.startPreferenceFragment(new TaskFragmentSettingsFragment(), false);
                break;
            default:
                settingsActivity.switchToHeader(toString(), null);
            }
        }
    }

    @NonNull
    private static final Set<FRAGMENTS> validFragments = new HashSet<>();
    static {
        validFragments.addAll(Arrays.asList(FRAGMENTS.values()));
    }

    public static final int DONATE = 5;
    public static final int FILE_ASTRID = 0, FILE_IMPORT_DB = 1,
                            NEW_ACCOUNT = 2, FILE_ANY_DO = 3, FILE_WUNDERLIST = 4;
    private static final String TAG = "SettingsActivity";
    private FileInputStream stream;
    private boolean isTablet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (MirakelCommonPreferences.isDark()) {
            setTheme(R.style.AppBaseThemeDARK);
        }
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        isTablet = MirakelCommonPreferences.isTablet();
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings, target);
        updateHeaderList(target);
    }

    private void updateHeaderList(List<Header> target) {
        if (target == null) {
            return;
        }
        final boolean showDev = MirakelCommonPreferences.isEnabledDebugMenu();
        int i = 0;
        boolean isDark = MirakelCommonPreferences.isDark();
        while (i < target.size()) {
            Header header = target.get(i);
            int id = (int) header.id;
            if (id == R.id.development_settings && !showDev) {
                target.remove(header);
                i--;
            }
            // Change the icon
            if (isDark) {
                if (id == R.id.ui_settings) {
                    header.iconRes = R.drawable.settings_ui_dark;
                } else if (id == R.id.sync_settings) {
                    header.iconRes = R.drawable.settings_sync_dark;
                } else if (id == R.id.tasks_settings) {
                    header.iconRes = R.drawable.settings_tasks_dark;
                } else if (id == R.id.meta_lists_settings) {
                    //header.iconRes = R.drawable.;
                } else if (id == R.id.notifications_settings) {
                    header.iconRes = R.drawable.settings_notifications_dark;
                } else if (id == R.id.backup_settings) {
                    header.iconRes = R.drawable.settings_backup_dark;
                } else if (id == R.id.development_settings) {
                    header.iconRes = R.drawable.settings_dev_dark;
                } else if (id == R.id.about_settings) {
                    header.iconRes = R.drawable.settings_about_dark;
                } else if (id == R.id.donation_settings) {
                    //header.iconRes = R.drawable.;
                }
            }
            i++;
        }
    }


    @Override
    protected boolean isValidFragment(final String fragmentName) {
        for (FRAGMENTS f : validFragments) {
            if (f.toString().equals(fragmentName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent data) {
        switch (requestCode) {
        case FILE_IMPORT_DB:
            if (resultCode != RESULT_OK) {
                return;
            }
            final Uri uri = data.getData();
            try {
                if (uri == null
                    || !"db".equals(FileUtils.getFileExtension(uri))) {
                    ErrorReporter.report(ErrorType.FILE_NOT_MIRAKEL_DB);
                }
                this.stream = FileUtils.getStreamFromUri(this, uri);
            } catch (final FileNotFoundException e) {
                ErrorReporter.report(ErrorType.FILE_NOT_MIRAKEL_DB);
                break;
            }
            new AlertDialog.Builder(this)
            .setTitle(R.string.import_sure)
            .setMessage(
                this.getString(R.string.import_sure_summary,
                               FileUtils.getNameFromUri(
                                   SettingsActivity.this, uri)))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.yes,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(
                    final DialogInterface dialog,
                    final int which) {
                    ExportImport.importDB(
                        SettingsActivity.this,
                        SettingsActivity.this.stream);
                }
            }).create().show();
            break;
        case FILE_ASTRID:
        case FILE_ANY_DO:
        case FILE_WUNDERLIST:
            if (resultCode != RESULT_OK) {
                return;
            }
            try {
                this.stream = FileUtils.getStreamFromUri(this, data.getData());
            } catch (final FileNotFoundException e) {
                ErrorReporter.report(ErrorType.FILE_NOT_FOUND);
                break;
            }
            // Do the import in a background-task
            new AsyncTask<String, Void, Boolean>() {
                ProgressDialog dialog;

                @Override
                protected Boolean doInBackground(final String... params) {
                    switch (requestCode) {
                    case FILE_ASTRID:
                        return ExportImport.importAstrid(SettingsActivity.this,
                                                         SettingsActivity.this.stream,
                                                         FileUtils.getMimeType(data.getData()));
                    case FILE_ANY_DO:
                        try {
                            return AnyDoImport.exec(SettingsActivity.this,
                                                    SettingsActivity.this.stream);
                        } catch (DefinitionsHelper.NoSuchListException e) {
                            ErrorReporter
                            .report(ErrorType.LIST_VANISHED);
                            Log.wtf(TAG, "list vanished", e);
                            return true;
                        }
                    case FILE_WUNDERLIST:
                        return WunderlistImport.exec(SettingsActivity.this,
                                                     SettingsActivity.this.stream);
                    default:
                        return false;
                    }
                }

                @Override
                protected void onPostExecute(final Boolean success) {
                    this.dialog.dismiss();
                    if (!success) {
                        ErrorReporter.report(ErrorType.ASTRID_ERROR);
                    } else {
                        Toast.makeText(SettingsActivity.this,
                                       R.string.astrid_success, Toast.LENGTH_SHORT)
                        .show();
                        // ugly but simple
                        Helpers.restartApp(SettingsActivity.this);
                    }
                }

                @Override
                protected void onPreExecute() {
                    this.dialog = ProgressDialog
                                  .show(SettingsActivity.this, SettingsActivity.this
                                        .getString(R.string.importing),
                                        SettingsActivity.this
                                        .getString(R.string.wait), true);
                }
            } .execute("");
            break;
        case DONATE:
            if (resultCode != RESULT_OK) {
                return;
            }
            if (!onIsMultiPane()) {
                finish();
            }
            break;
        default:
            break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
        case android.R.id.home:
            if (!isTablet && currentFragment == FRAGMENTS.TASKUI) {
                FRAGMENTS.UI.restoreFragment(this);
            } else {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (currentFragment == FRAGMENTS.TASKUI) {
            FRAGMENTS.UI.restoreFragment(this);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void startPreferenceFragment(Fragment fragment, boolean push) {
        isValidFragment(fragment.getClass().getName());
        super.startPreferenceFragment(fragment, push);
    }

    @Override
    public boolean onIsMultiPane() {
        return MirakelCommonPreferences.isTablet();
    }
}
