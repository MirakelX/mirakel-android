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

package de.azapps.mirakel.settings.model_settings.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.widget.TimePicker;

import com.afollestad.materialdialogs.AlertDialogWrapper;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelDetailFragment;
import de.azapps.mirakel.sync.taskwarrior.services.SyncAdapter;
import de.azapps.tools.Log;

public class AccountDetailFragment extends GenericModelDetailFragment<AccountMirakel> {

    private static final String TAG = "AccountDetailFragment";

    @NonNull
    @Override
    protected AccountMirakel getDummyItem() {
        return AccountMirakel.getLocal();
    }

    @Override
    protected int getResourceId() {
        return R.xml.settings_account;
    }

    @Override
    protected boolean hasMenu() {
        return true;
    }

    @Override
    protected boolean isFabVisible() {
        return false;
    }

    @Override
    protected void setUp() {
        final AccountManager accountManager = AccountManager.get(getActivity());
        final AccountMirakel accountMirakel = mItem;
        final Account account = accountMirakel.getAndroidAccount();
        // Preference Fields
        final Preference syncUsername = findPreference("syncUsername");
        final EditTextPreference syncServer = (EditTextPreference) findPreference("syncServer");
        final SwitchPreference syncUse = (SwitchPreference) findPreference("syncUse");
        final Preference syncType = findPreference("sync_type");
        final SwitchPreference defaultAccount = (SwitchPreference) findPreference("defaultAccount");
        final Preference syncInterval = findPreference("syncFrequency");
        // Set Preferences
        syncUsername.setEnabled(false);
        syncUsername.setSummary(accountMirakel.getName());
        //sync Server
        if (AccountMirakel.ACCOUNT_TYPES.TASKWARRIOR == accountMirakel.getType()) {
            syncServer.setEnabled(false);
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
                accountMirakel.setEnabled((Boolean) newValue);
                accountMirakel.save();
                return true;
            }
        });
        if (accountMirakel.getType() == AccountMirakel.ACCOUNT_TYPES.LOCAL) {
            removePreference("syncUse");
            removePreference("syncServer");
            removePreference("syncUsername");
        }
        // Sync type
        syncType.setSummary(accountMirakel.getType().typeName(getActivity()));
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
                final TimePicker timePicker = new TimePicker(getActivity());
                timePicker.setIs24HourView(true);
                timePicker.setCurrentHour(hours);
                timePicker.setCurrentMinute(minutes);
                final AlertDialogWrapper.Builder dialog = new AlertDialogWrapper.Builder(getActivity())
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
                            .setSummary(getActivity().getString(R.string.sync_frequency_summary, newValue));
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
                            Log.d(TAG, "account does not exsist");
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
            removePreference("syncFrequency");
        } else {
            syncInterval.setEnabled(true);
        }
    }




}
