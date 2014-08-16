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

 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.text.Html;
import android.widget.TimePicker;

import com.google.common.base.Optional;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.settings.generic_list.GenericListSettingActivity;
import de.azapps.mirakel.settings.generic_list.GenericSettingsFragment;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSetupActivity;
import de.azapps.tools.Log;

public class AccountSettingsActivity extends GenericListSettingActivity<AccountMirakel> implements
    GenericSettingsFragment.Callbacks<AccountMirakel> {

    private static final String TAG = "AccountSettingsActivity";

    @Override
    public Uri getUri() {
        return MirakelInternalContentProvider.ACCOUNT_URI;
    }

    @Override
    public Class<AccountMirakel> getMyClass() {
        return AccountMirakel.class;
    }

    @Override
    protected void createModel() {
        final CharSequence[] items = getResources().getTextArray(
                                         R.array.sync_types);
        new AlertDialog.Builder(this).setTitle(R.string.sync_add)
        .setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                switch (which) {
                case 0:
                    showAlert(
                        R.string.alert_caldav_title,
                        R.string.alert_caldav,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            final DialogInterface dialog,
                            final int which) {
                            handleCalDAV();
                        }
                    });
                    break;
                case 1:
                    showAlert(
                        R.string.alert_taskwarrior_title,
                        R.string.alert_taskwarrior,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                            final DialogInterface dialog,
                            final int which) {
                            startActivity(new Intent(
                                              AccountSettingsActivity.this,
                                              TaskWarriorSetupActivity.class));
                        }
                    });
                    break;
                default:
                    break;
                }
                dialog.dismiss();
            }
        }).show();
    }

    @Override
    public String getTitle(Optional<AccountMirakel> model) {
        if (model.isPresent()) {
            return model.get().getName();
        } else {
            return getString(R.string.no_account_selected);
        }
    }

    @Override
    public int getPreferenceResource() {
        return R.xml.settings_account;
    }

    @Override
    public void setUp(Optional<AccountMirakel> modelBaseOptional, GenericSettingsFragment settings) {
        if (!modelBaseOptional.isPresent()) {
            return;
        }
        final AccountManager accountManager = AccountManager.get(this);
        final AccountMirakel accountMirakel = modelBaseOptional.get();
        final Account account = accountMirakel.getAndroidAccount();
        // Preference Fields
        final Preference syncUsername = settings.findPreference("syncUsername");
        final EditTextPreference syncServer = (EditTextPreference) settings.findPreference("syncServer");
        final CheckBoxPreference syncUse = (CheckBoxPreference) settings.findPreference("syncUse");
        final Preference syncType = settings.findPreference("sync_type");
        final CheckBoxPreference defaultAccount = (CheckBoxPreference)
                settings.findPreference("defaultAccount");
        final Preference syncInterval = settings.findPreference("syncFrequency");
        // Set Preferences
        syncUsername.setEnabled(false);
        syncUsername.setSummary(accountMirakel.getName());
        //sync Server
        if (AccountMirakel.ACCOUNT_TYPE_MIRAKEL.equals(accountMirakel.getType())) {
            syncServer.setEnabled(true);
            syncServer.setSummary(accountManager.getUserData(account,
                                  SyncAdapter.BUNDLE_SERVER_URL));
            syncServer.setText(accountManager.getUserData(account,
                               SyncAdapter.BUNDLE_SERVER_URL));
            syncServer
            .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(
                    final Preference preference,
                    final Object newValue) {
                    accountManager.setUserData(account,
                                               SyncAdapter.BUNDLE_SERVER_URL,
                                               (String) newValue);
                    syncServer.setSummary((String) newValue);
                    syncServer.setText((String) newValue);
                    return false;
                }
            });
        } else {
            syncServer.setEnabled(false);
            syncServer.setSummary("");
        }
        // sync use
        syncUse.setChecked(accountMirakel.isEnabled());
        syncUse.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference,
                                              final Object newValue) {
                accountMirakel.setEnabeld((Boolean) newValue);
                accountMirakel.save();
                return true;
            }
        });
        if (accountMirakel.getType() == AccountMirakel.ACCOUNT_TYPES.LOCAL) {
            settings.removePreference("syncUse");
            settings.removePreference("syncServer");
            settings.removePreference("syncUsername");
        }
        // Sync type
        syncType.setSummary(accountMirakel.getType().typeName(this));
        // Default Account
        defaultAccount.setChecked(MirakelModelPreferences
                                  .getDefaultAccount().getId() == accountMirakel.getId());
        defaultAccount
        .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(
                final Preference preference,
                final Object newValue) {
                if ((Boolean) newValue) {
                    MirakelModelPreferences
                    .setDefaultAccount(accountMirakel);
                } else {
                    MirakelModelPreferences
                    .setDefaultAccount(AccountMirakel
                                       .getLocal());
                }
                defaultAccount.setChecked((Boolean) newValue);
                return false;
            }
        });
        // Interval
        syncInterval.setEnabled(false);
        if (MirakelModelPreferences.getSyncFrequency(accountMirakel) == -1) {
            syncInterval.setSummary(R.string.sync_frequency_summary_man);
        } else {
            syncInterval
            .setSummary(this.getString(
                            R.string.sync_frequency_summary,
                            MirakelModelPreferences
                            .getSyncFrequency(accountMirakel)));
        }
        syncInterval
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(
                final Preference preference) {
                int hours = MirakelModelPreferences
                            .getSyncFrequency(accountMirakel);
                if (hours == -1) {
                    hours = 0;
                }
                final int minutes = hours % 60;
                hours = (int) Math.floor(hours / 60.);
                final TimePicker timePicker = new TimePicker(
                    AccountSettingsActivity.this);
                timePicker.setIs24HourView(true);
                timePicker.setCurrentHour(hours);
                timePicker.setCurrentMinute(minutes);
                final AlertDialog.Builder dialog = new AlertDialog.Builder(
                    AccountSettingsActivity.this)
                .setTitle(R.string.sync_frequency)
                .setView(timePicker)
                .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        final DialogInterface dialog,
                        final int which) {
                        int newValue = timePicker
                                       .getCurrentHour()
                                       * 60
                                       + timePicker
                                       .getCurrentMinute();
                        if (newValue == 0) {
                            newValue = -1;
                        }
                        final Bundle bundle = new Bundle();
                        if (newValue == -1) {
                            syncInterval
                            .setSummary(R.string.sync_frequency_summary_man);
                        } else {
                            syncInterval
                            .setSummary(AccountSettingsActivity.this
                                        .getString(
                                            R.string.sync_frequency_summary,
                                            newValue));
                        }
                        if (accountMirakel != null) {
                            ContentResolver
                            .removePeriodicSync(
                                account,
                                DefinitionsHelper.AUTHORITY_TYP,
                                bundle);
                            if (newValue != -1) {
                                ContentResolver
                                .setSyncAutomatically(
                                    account,
                                    DefinitionsHelper.AUTHORITY_TYP,
                                    true);
                                ContentResolver
                                .setIsSyncable(
                                    account,
                                    DefinitionsHelper.AUTHORITY_TYP,
                                    1);
                                ContentResolver
                                .addPeriodicSync(
                                    account,
                                    DefinitionsHelper.AUTHORITY_TYP,
                                    bundle,
                                    newValue * 60);
                            }
                        } else {
                            Log.d(TAG,
                                  "account does not exsist");
                        }
                        MirakelModelPreferences
                        .setSyncFrequency(
                            accountMirakel,
                            newValue);
                    }
                });
                dialog.show();
                return false;
            }
        });
        if (accountMirakel.getType() != AccountMirakel.ACCOUNT_TYPES.TASKWARRIOR) {
            // we can control this only for tw
            settings.removePreference("syncFrequency");
        } else {
            syncInterval.setEnabled(true);
        }
    }


    // Helper stuff
    private void showAlert(final int titleId, final int messageId,
                           final android.content.DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(this).setTitle(titleId).setMessage(messageId)
        .setPositiveButton(android.R.string.ok, listener).show();
    }

    protected void handleCalDAV() {
        new AlertDialog.Builder(this)
        .setTitle(R.string.sync_caldav)
        .setMessage(
            Html.fromHtml(this
                          .getString(R.string.sync_caldav_howto_)))
        .setNegativeButton(R.string.download,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                Helpers.openURL(getBaseContext(),
                                "http://mirakel.azapps.de/releases.html#davdroid");
            }
        })
        .setPositiveButton(R.string.sync_add_account,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                startActivity(new Intent(
                                  Settings.ACTION_ADD_ACCOUNT));
            }
        }).show();
    }
}
