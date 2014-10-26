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

package de.azapps.mirakel.settings;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
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

import org.sufficientlysecure.donations.DonationsFragment;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.BuildHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.helper.export_import.AnyDoImport;
import de.azapps.mirakel.helper.export_import.ExportImport;
import de.azapps.mirakel.helper.export_import.WunderlistImport;
import de.azapps.mirakel.settings.fragments.AboutSettingsFragment;
import de.azapps.mirakel.settings.fragments.BackupSettingsFragment;
import de.azapps.mirakel.settings.fragments.CreditsFragment;
import de.azapps.mirakel.settings.fragments.DevSettingsFragment;
import de.azapps.mirakel.settings.fragments.NotificationSettingsFragment;
import de.azapps.mirakel.settings.fragments.TaskSettingsFragment;
import de.azapps.mirakel.settings.fragments.UISettingsFragment;
import de.azapps.mirakel.settings.taskfragment.TaskFragmentSettingsFragment;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

public class SettingsActivity extends PreferenceActivity {

    public static final String SHOW_DONATE = "donate";
    private static final String STATE_HEADERS_LIST = "header";
    private static final String STATE_CUR_HEADER_POS = "Current_pos";
    private static final int NEED_UPDATE = 42;
    @NonNull
    private FRAGMENTS currentFragment = FRAGMENTS.UI;
    private ArrayList<Header> headers = new ArrayList<>();

    /**
     * Flattr
     */
    private static final String FLATTR_PROJECT_URL = "http://mirakel.azapps.de/";
    // FLATTR_URL without http:// !
    private static final String FLATTR_URL = "flattr.com/thing/2188714";

    private static final String[] GOOGLE_CATALOG = new String[] {
        "mirakel.donation.50", "mirakel.donation.100",
        "mirakel.donation.200", "mirakel.donation.500",
        "mirakel.donation.1000", "mirakel.donation.1500",
        "mirakel.donation.2500", "mirakel.donation.19900",
    };
    /**
     * Google
     */
    private static final String GOOGLE_PUBKEY =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmjcA2Hmr/HVH5raLa6RMTbY/n5QbhqnGOvcLCVQqxj+A4N2vWke7N0Y2tvSS8LYvpdSt5INHtyl1DNaJ/42fdMoFnwLO9lEYvQ1AMPBPt7BtBm2qw/L4hybqYCg/nyzZ2GI/Te6pDgHBUxcaIR0b8IRFwc+3lZHCIxIqq7VjEcxV6hgbNC5Tx5Lt69eTDvZIPwIjU0h/hVDUNxZxWEOGpWRfSqCtTQWSA8Vo8ssAK7n/s8NtpAGn84ZJWFF8SyZc0Y3jjCb9FCRgF7D6xXLPbl1O6ekLIU6zG4RqaaxqymHiXpkq9cYmV/9A3RJathc9WyvPlj7oRlCYo12vmqIV+QIDAQAB";

    private static final String PAYPAL_CURRENCY_CODE = "EUR";
    /**
     * PayPal
     */
    private static final String PAYPAL_USER = "anatolij.z@web.de";

    private enum FRAGMENTS {
        ABOUT, BACKUP, DEV, NOTIFICATION, TASK, UI, TASKUI, DONATE, CREDITS;

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
            case DONATE:
                return DonationsFragment.class.getName();
            case CREDITS:
                return CreditsFragment.class.getName();

            }
            return super.toString();
        }

        public void restoreFragment(final @NonNull PreferenceActivity settingsActivity) {
            switch (this) {
            case TASKUI:
                settingsActivity.startPreferenceFragment(new TaskFragmentSettingsFragment(), true);
                break;
            case CREDITS:
                settingsActivity.startPreferenceFragment(new CreditsFragment(), true);
            case DONATE:
                DonationsFragment donationsFragment;
                if (BuildHelper.isForPlayStore()) {
                    donationsFragment = DonationsFragment.newInstance(
                                            MirakelCommonPreferences.isDebug(),
                                            true,
                                            GOOGLE_PUBKEY,
                                            GOOGLE_CATALOG,
                                            settingsActivity.getResources().getStringArray(
                                                R.array.donation_google_catalog_values), false,
                                            null, null, null, false, FLATTR_PROJECT_URL, FLATTR_URL);
                } else {
                    donationsFragment = DonationsFragment.newInstance(
                                            MirakelCommonPreferences.isDebug(), false, null, null,
                                            null, true, PAYPAL_USER, PAYPAL_CURRENCY_CODE,
                                            settingsActivity.getString(R.string.donation_paypal_item), true,
                                            FLATTR_PROJECT_URL, FLATTR_URL);
                }
                if (MirakelCommonPreferences.isTablet()) {
                    settingsActivity.startPreferenceFragment(donationsFragment, false);
                } else {
                    settingsActivity.startWithFragment(DONATE.toString(), donationsFragment.getArguments(), null,
                                                       NEED_UPDATE);
                }
                break;
            default:
                if (MirakelCommonPreferences.isTablet()) {
                    settingsActivity.switchToHeader(toString(), null);
                }
                settingsActivity.startWithFragment(toString(), null, null, NEED_UPDATE);
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
        if (MirakelCommonPreferences.useNewUI()) {
            setTheme(R.style.MirakelSettingsTheme);
        } else if (MirakelCommonPreferences.isDark()) {
            setTheme(R.style.AppBaseThemeDARK);
        }
        super.onCreate(savedInstanceState);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        isTablet = MirakelCommonPreferences.isTablet();
        if (getIntent() != null && getIntent().getBooleanExtra(SHOW_DONATE, false)) {
            FRAGMENTS.DONATE.restoreFragment(this);
        }
        if (onIsMultiPane() && onIsHidingHeaders()) {
            Intent i = new Intent();
            i.putExtra(STATE_CUR_HEADER_POS, currentFragment.ordinal());
            setResult(NEED_UPDATE, i);
            finish();
        }
    }

    @Override
    public Header onGetInitialHeader() {
        List<Header> filtered = new ArrayList<>(Collections2.filter(headers, new Predicate<Header>() {
            @Override
            public boolean apply(Header input) {
                return currentFragment.toString().equals(input.fragment);
            }
        }));
        if (filtered.isEmpty()) {
            return super.onGetInitialHeader();
        } else {
            return filtered.get(0);
        }
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
        headers = (ArrayList<Header>) target;
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        if (isTablet) {
            getActionBar().setTitle(getString(header.titleRes));
        }
        if (header.id == R.id.donation_settings) {
            FRAGMENTS.DONATE.restoreFragment(this);
        } else {
            super.onHeaderClick(header, position);
        }
    }

    @Override
    protected boolean isValidFragment(final String fragmentName) {
        for (FRAGMENTS f : validFragments) {
            if (f.toString().equals(fragmentName)) {
                currentFragment = f;
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
                        return WunderlistImport.exec(SettingsActivity.this.stream);
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
        case NEED_UPDATE:
            if (data != null) {
                currentFragment = FRAGMENTS.values()[data.getIntExtra(STATE_CUR_HEADER_POS,
                                                     FRAGMENTS.UI.ordinal())];
                currentFragment.restoreFragment(this);
            }
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
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        if (!onIsMultiPane() || (currentFragment != FRAGMENTS.TASKUI &&
                                 currentFragment != FRAGMENTS.CREDITS)) {
            super.onBackPressed();
        }
        if (currentFragment == FRAGMENTS.TASKUI) {
            FRAGMENTS.UI.restoreFragment(this);
        } else if (currentFragment == FRAGMENTS.CREDITS) {
            FRAGMENTS.ABOUT.restoreFragment(this);
        }
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        Locale.setDefault(Helpers.getLocal(this));
        super.onConfigurationChanged(newConfig);
        if (this.isTablet != MirakelCommonPreferences.isTablet()) {
            Bundle saved = new Bundle();
            onSaveInstanceState(saved);
            onCreate(saved);
            invalidateHeaders();
            onRestoreInstanceState(saved);
            if (!isTablet) {
                getActionBar().setTitle(R.string.title_settings);
            }
        }
    }


    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);

        //Retrieve our saved header list and last clicked position and ensure we switch to the proper header.
        headers = state.getParcelableArrayList(STATE_HEADERS_LIST);
        currentFragment = FRAGMENTS.values()[state.getInt(STATE_CUR_HEADER_POS)];
        if (headers != null) {
            currentFragment.restoreFragment(this);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Persist our list and last clicked position
        if (headers != null && headers.size() > 0) {
            outState.putInt(STATE_CUR_HEADER_POS, currentFragment.ordinal());
            outState.putParcelableArrayList(STATE_HEADERS_LIST, headers);
        }
    }

    @Override
    public void startPreferenceFragment(Fragment fragment, boolean push) {
        if (isValidFragment(fragment.getClass().getName())) {
            super.startPreferenceFragment(fragment, push);
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return MirakelCommonPreferences.isTablet();
    }
}
