/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
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
package de.azapps.mirakel.static_activities;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.special_lists_settings.SpecialListsSettings;
import de.azapps.mirakel.sync.AuthenticatorActivity;
import de.azapps.mirakel.sync.DataDownloadCommand;
import de.azapps.mirakel.sync.Network;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment extends PreferenceFragment {
	private static final String TAG = "SettingsFragment";
	private ListPreference startupListPreference;

	// private MainActivity main;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Initialize needed Arrays
		List<ListMirakel> lists = ListMirakel.all();
		CharSequence entryValues[] = new String[lists.size()];
		CharSequence entries[] = new String[lists.size()];
		int i = 0;
		for (ListMirakel list : lists) {
			entryValues[i] = String.valueOf(list.getId());
			entries[i] = list.getName();
			i++;
		}

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		// Notifications List
		ListPreference notificationsListPreference = (ListPreference) findPreference("notificationsList");
		notificationsListPreference.setEntries(entries);
		notificationsListPreference.setEntryValues(entryValues);

		// Startup
		CheckBoxPreference startupAllListPreference = (CheckBoxPreference) findPreference("startupAllLists");
		startupListPreference = (ListPreference) findPreference("startupList");
		if (startupAllListPreference.isChecked()) {
			startupListPreference.setEnabled(false);
		}
		startupAllListPreference
				.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						if ((Boolean) newValue) {
							startupListPreference.setEnabled(false);
						} else {
							startupListPreference.setEnabled(true);
						}
						return true;
					}

				});
		startupListPreference.setEntries(entries);
		startupListPreference.setEntryValues(entryValues);
		//Enable/Disbale Sync
		CheckBoxPreference sync = (CheckBoxPreference) findPreference("syncUse");
		sync.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				Log.e(TAG, "" + ((Boolean) newValue).toString());
				AccountManager am = AccountManager.get(getActivity());
				Account[] accounts = am.getAccountsByType(Mirakel.ACCOUNT_TYP);
				if ((Boolean) newValue) {
					if (accounts.length == 0) {
						Intent intent = new Intent(getActivity(),
								AuthenticatorActivity.class);
						intent.setAction(MainActivity.SHOW_LISTS);
						startActivity(intent);
					} else {
						// account=accounts[0];
					}
				} else {
					try {
						am.removeAccount(accounts[0], null, null);
					} catch (Exception e) {
						Log.e(TAG, "Cannot remove Account");
					}
				}
				return true;
			}
		});
		//Get Account if existing
		final AccountManager am = AccountManager.get(getActivity());
		final Account account = getAccount(am);
		if (account == null) {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(getActivity()
							.getApplicationContext());
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("syncUse", false);
			editor.commit();
			sync.setChecked(false);
		} else {
			sync.setChecked(true);
		}

		//Change Passwort
		final EditTextPreference Password = (EditTextPreference) findPreference("syncPassword");
		Password.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					final Object password) {
				if (account != null) {
					ConnectivityManager cm = (ConnectivityManager) getActivity()
							.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo netInfo = cm.getActiveNetworkInfo();
					if (netInfo != null && netInfo.isConnected()) {
						List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
						data.add(new BasicNameValuePair("email", account.name));
						data.add(new BasicNameValuePair("password",
								(String) password));

						new Network(new DataDownloadCommand() {
							@Override
							public void after_exec(String result) {
								String t = Network.getToken(result);
								if (t != null) {
					am.setPassword(account, (String) password);
									Password.setText(am.getPassword(account));
									new Network(new DataDownloadCommand() {
										@Override
										public void after_exec(String result) {
										}
									}, Network.HttpMode.DELETE, getActivity(),
											null).execute(am.getUserData(
											account, Mirakel.BUNDLE_SERVER_URL)
											+ "/tokens/" + t);
								} else {
									Toast.makeText(
											getActivity(),
											getString(R.string.inavlidPassword),
											Toast.LENGTH_LONG).show();
								}
								SharedPreferences settings = PreferenceManager
										.getDefaultSharedPreferences(getActivity()
												.getApplicationContext());
								SharedPreferences.Editor editor = settings
										.edit();
								editor.putString("syncPassword", "");
								editor.commit();

							}
						}, Network.HttpMode.POST, data, getActivity(), null)
								.execute(am.getUserData(account,
										Mirakel.BUNDLE_SERVER_URL)
										+ "/tokens.json");
					} else {
						Toast.makeText(getActivity(),
								getString(R.string.NoNetwork),
								Toast.LENGTH_LONG).show();
					}
				}
				return true;
			}
		});
		
		//Set old Password to Textbox
		Password.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
		if (account != null) {
					Password.setText(am.getPassword(account));
		}
				return false;
			}
		});

		// TODO Add Option To Resync all
		//Change Url
		EditTextPreference url = (EditTextPreference) findPreference("syncServer");
		if (account != null) {
			url.setText(am.getUserData(account, "url"));
		}
		url.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					final Object url) {
				if (account != null) {
					ConnectivityManager cm = (ConnectivityManager) getActivity()
							.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo netInfo = cm.getActiveNetworkInfo();
					if (netInfo != null && netInfo.isConnected()) {
						List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
						data.add(new BasicNameValuePair("email", account.name));
						data.add(new BasicNameValuePair("password",
								(AccountManager.get(getActivity()))
										.getPassword(account)));

						new Network(new DataDownloadCommand() {
			@Override
							public void after_exec(String result) {
								String t = Network.getToken(result);
								if (t != null) {
					am.setUserData(account, "url", (String) url);
									Password.setText(am.getPassword(account));
									new Network(new DataDownloadCommand() {
										@Override
										public void after_exec(String result) {
										}
									}, Network.HttpMode.DELETE, getActivity(),
											null).execute((String) url
											+ "/tokens/" + t);
								} else {
									Toast.makeText(getActivity(),
											getString(R.string.inavlidUrl),
											Toast.LENGTH_LONG).show();
									SharedPreferences settings = PreferenceManager
											.getDefaultSharedPreferences(getActivity()
													.getApplicationContext());
									SharedPreferences.Editor editor = settings
											.edit();
									editor.putString("syncServer", am
											.getUserData(account,
													Mirakel.BUNDLE_SERVER_URL));
									editor.commit();
								}
							}
						}, Network.HttpMode.POST, data, getActivity(), null)
								.execute((String) url + "/tokens.json");
					} else {
						Toast.makeText(getActivity(),
								getString(R.string.NoNetwork),
								Toast.LENGTH_LONG).show();
					}

				}
				return true;
			}
		});

		//Change Sync-Intervall
		ListPreference syncIntervall = (ListPreference) findPreference("syncFrequency");
		syncIntervall
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						Log.e(TAG, "" + newValue.toString());
						Bundle bundle = new Bundle();
						ContentResolver.removePeriodicSync(account,
								Mirakel.AUTHORITY_TYP, bundle);
						if (account != null
								&& Long.parseLong(newValue.toString()) != -1) {
							ContentResolver.setSyncAutomatically(account,
									Mirakel.AUTHORITY_TYP, true);
							ContentResolver.setIsSyncable(account,
									Mirakel.AUTHORITY_TYP, 1);
							//ContentResolver.setMasterSyncAutomatically(true);
							ContentResolver.addPeriodicSync(account,
									Mirakel.AUTHORITY_TYP, null,
									((Long) newValue) * 60);
						}
						return true;
					}
				});

		Intent startSpecialListsIntent = new Intent(getActivity(), SpecialListsSettings.class);
		Preference specialLists=(Preference) findPreference("special_lists");
		specialLists.setIntent(startSpecialListsIntent);
	}

	private Account getAccount(AccountManager am) {
		try {
			return am.getAccountsByType(Mirakel.ACCOUNT_TYP)[0];
		} catch (ArrayIndexOutOfBoundsException f) {
			Log.e(TAG, "No Account found");
			return null;
		}
	}

}
