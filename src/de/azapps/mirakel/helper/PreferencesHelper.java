package de.azapps.mirakel.helper;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.special_lists_settings.SpecialListsSettings;
import de.azapps.mirakel.static_activities.SettingsActivity;
import de.azapps.mirakel.static_activities.SettingsFragment;
import de.azapps.mirakel.sync.AuthenticatorActivity;
import de.azapps.mirakel.sync.DataDownloadCommand;
import de.azapps.mirakel.sync.Network;
import de.azapps.mirakel.widget.MainWidgetSettingsActivity;
import de.azapps.mirakel.widget.MainWidgetSettingsFragment;

public class PreferencesHelper {

	private static final String TAG = "PreferencesHelper";
	private Object ctx;
	private boolean v4_0;

	public PreferencesHelper(SettingsActivity c) {
		ctx = c;
		v4_0 = false;
	}

	public PreferencesHelper(SettingsFragment c) {
		ctx = c;
		v4_0 = true;
	}
	
	public PreferencesHelper(MainWidgetSettingsActivity c) {
		ctx = c;
		v4_0 = false;
	}

	public PreferencesHelper(MainWidgetSettingsFragment c) {
		ctx = c;
		v4_0 = true;
	}
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public void setFunctionsWidget() {
		List<ListMirakel> lists = ListMirakel.all();
		CharSequence entryValues[] = new String[lists.size()];
		CharSequence entries[] = new String[lists.size()];
		int i = 0;
		for (ListMirakel list : lists) {
			entryValues[i] = String.valueOf(list.getId());
			entries[i] = list.getName();
			i++;
		}

		// Notifications List
		ListPreference notificationsListPreference = (ListPreference) (v4_0?((MainWidgetSettingsFragment)ctx).findPreference("widgetList"):((MainWidgetSettingsActivity)ctx).findPreference("widgetList"));
		notificationsListPreference.setEntries(entries);
		notificationsListPreference.setEntryValues(entryValues);
		
		
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public void setFunctionsApp() {
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

		// Notifications List
		ListPreference notificationsListPreference = (ListPreference) (v4_0 ? ((SettingsFragment) ctx)
				.findPreference("notificationsList") : ((SettingsActivity) ctx)
				.findPreference("notificationsList"));
		notificationsListPreference.setEntries(entries);
		notificationsListPreference.setEntryValues(entryValues);
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			CheckBoxPreference mCheckBoxPref = (CheckBoxPreference) (v4_0 ? ((SettingsFragment) ctx)
					.findPreference("notificationsBig")
					: ((SettingsActivity) ctx)
							.findPreference("notificationsBig"));
			PreferenceCategory mCategory = (PreferenceCategory) (v4_0 ? ((SettingsFragment) ctx)
					.findPreference("category_notifications")
					: ((SettingsActivity) ctx)
							.findPreference("category_notifications"));
			mCategory.removePreference(mCheckBoxPref);
		}

		// Startup
		CheckBoxPreference startupAllListPreference = (CheckBoxPreference) (v4_0 ? ((SettingsFragment) ctx)
				.findPreference("startupAllLists") : ((SettingsActivity) ctx)
				.findPreference("startupAllLists"));
		final ListPreference startupListPreference = (ListPreference) (v4_0 ? ((SettingsFragment) ctx)
				.findPreference("startupList") : ((SettingsActivity) ctx)
				.findPreference("startupList"));
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
		// Enable/Disbale Sync
		CheckBoxPreference sync = (CheckBoxPreference) (v4_0 ? ((SettingsFragment) ctx)
				.findPreference("syncUse") : ((SettingsActivity) ctx)
				.findPreference("syncUse"));
		sync.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				Log.e(TAG, "" + ((Boolean) newValue).toString());
				AccountManager am = AccountManager
						.get((v4_0 ? ((SettingsFragment) ctx).getActivity()
								: (SettingsActivity) ctx));
				Account[] accounts = am.getAccountsByType(Mirakel.ACCOUNT_TYP);
				if ((Boolean) newValue) {
					if (accounts.length == 0) {
						Intent intent = new Intent(
								(v4_0 ? ((SettingsFragment) ctx).getActivity()
										: (SettingsActivity) ctx),
								AuthenticatorActivity.class);
						intent.setAction(MainActivity.SHOW_LISTS);
						if (v4_0) {
							((SettingsFragment) ctx).startActivity(intent);
						} else {
							((SettingsActivity) ctx).startActivity(intent);
						}
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
		// Get Account if existing
		final AccountManager am = AccountManager
				.get((v4_0 ? ((SettingsFragment) ctx).getActivity()
						: (SettingsActivity) ctx));
		final Account account = getAccount(am);
		if (account == null) {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences((v4_0 ? ((SettingsFragment) ctx)
							.getActivity() : (SettingsActivity) ctx)
							.getApplicationContext());
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("syncUse", false);
			editor.commit();
			sync.setChecked(false);
		} else {
			sync.setChecked(true);
		}

		// Change Passwort
		final EditTextPreference Password = (EditTextPreference) (v4_0 ? ((SettingsFragment) ctx)
				.findPreference("syncPassword") : ((SettingsActivity) ctx)
				.findPreference("syncPassword"));
		Password.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					final Object password) {
				if (account != null) {
					ConnectivityManager cm = (ConnectivityManager) (v4_0 ? ((SettingsFragment) ctx)
							.getActivity() : (SettingsActivity) ctx)
							.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo netInfo = cm.getActiveNetworkInfo();
					if (netInfo != null && netInfo.isConnected()) {
						List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
						data.add(new BasicNameValuePair("email", account.name));
						data.add(new BasicNameValuePair("password",
								(String) password));

						new Network(
								new DataDownloadCommand() {
									@Override
									public void after_exec(String result) {
										String t = Network.getToken(result);
										if (t != null) {
											am.setPassword(account,
													(String) password);
											Password.setText(am
													.getPassword(account));
											new Network(
													new DataDownloadCommand() {
														@Override
														public void after_exec(
																String result) {
														}
													},
													Network.HttpMode.DELETE,
													(v4_0 ? ((SettingsFragment) ctx)
															.getActivity()
															: (SettingsActivity) ctx),
													null).execute(am
													.getUserData(
															account,
															Mirakel.BUNDLE_SERVER_URL)
													+ "/tokens/" + t);
										} else {
											Toast.makeText(
													(v4_0 ? ((SettingsFragment) ctx)
															.getActivity()
															: (SettingsActivity) ctx),
													(v4_0 ? ((SettingsFragment) ctx)
															.getString(R.string.inavlidPassword)
															: ((SettingsActivity) ctx)
																	.getString(R.string.inavlidPassword)),
													Toast.LENGTH_LONG).show();
										}
										SharedPreferences settings = PreferenceManager
												.getDefaultSharedPreferences((v4_0 ? ((SettingsFragment) ctx)
														.getActivity()
														: (SettingsActivity) ctx)
														.getApplicationContext());
										SharedPreferences.Editor editor = settings
												.edit();
										editor.putString("syncPassword", "");
										editor.commit();

									}
								}, Network.HttpMode.POST, data,
								(v4_0 ? ((SettingsFragment) ctx).getActivity()
										: (SettingsActivity) ctx), null)
								.execute(am.getUserData(account,
										Mirakel.BUNDLE_SERVER_URL)
										+ "/tokens.json");
					} else {
						Toast.makeText(
								(v4_0 ? ((SettingsFragment) ctx).getActivity()
										: (SettingsActivity) ctx),
								(v4_0 ? ((SettingsFragment) ctx)
										.getString(R.string.NoNetwork)
										: ((SettingsActivity) ctx)
												.getString(R.string.NoNetwork)),
								Toast.LENGTH_LONG).show();
					}
				}
				return true;
			}
		});

		// Set old Password to Textbox
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
		// Change Url
		EditTextPreference url = (EditTextPreference) (v4_0 ? ((SettingsFragment) ctx)
				.findPreference("syncServer") : ((SettingsActivity) ctx)
				.findPreference("syncServer"));
		if (account != null) {
			url.setText(am.getUserData(account, "url"));
		}
		url.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					final Object url) {
				if (account != null) {
					ConnectivityManager cm = (ConnectivityManager) (v4_0 ? ((SettingsFragment) ctx)
							.getActivity() : (SettingsActivity) ctx)
							.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo netInfo = cm.getActiveNetworkInfo();
					if (netInfo != null && netInfo.isConnected()) {
						List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
						data.add(new BasicNameValuePair("email", account.name));
						data.add(new BasicNameValuePair("password",
								(AccountManager
										.get((v4_0 ? ((SettingsFragment) ctx)
												.getActivity()
												: (SettingsActivity) ctx)))
										.getPassword(account)));

						new Network(
								new DataDownloadCommand() {
									@Override
									public void after_exec(String result) {
										String t = Network.getToken(result);
										if (t != null) {
											am.setUserData(account, "url",
													(String) url);
											Password.setText(am
													.getPassword(account));
											new Network(
													new DataDownloadCommand() {
														@Override
														public void after_exec(
																String result) {
														}
													},
													Network.HttpMode.DELETE,
													(v4_0 ? ((SettingsFragment) ctx)
															.getActivity()
															: (SettingsActivity) ctx),
													null).execute((String) url
													+ "/tokens/" + t);
										} else {
											Toast.makeText(
													(v4_0 ? ((SettingsFragment) ctx)
															.getActivity()
															: (SettingsActivity) ctx),
													(v4_0 ? ((SettingsFragment) ctx)
															.getString(R.string.inavlidUrl)
															: ((SettingsActivity) ctx)
																	.getString(R.string.inavlidUrl)),
													Toast.LENGTH_LONG).show();
											SharedPreferences settings = PreferenceManager
													.getDefaultSharedPreferences((v4_0 ? ((SettingsFragment) ctx)
															.getActivity()
															: (SettingsActivity) ctx)
															.getApplicationContext());
											SharedPreferences.Editor editor = settings
													.edit();
											editor.putString(
													"syncServer",
													am.getUserData(
															account,
															Mirakel.BUNDLE_SERVER_URL));
											editor.commit();
										}
									}
								}, Network.HttpMode.POST, data,
								(v4_0 ? ((SettingsFragment) ctx).getActivity()
										: (SettingsActivity) ctx), null)
								.execute((String) url + "/tokens.json");
					} else {
						Toast.makeText(
								(v4_0 ? ((SettingsFragment) ctx).getActivity()
										: (SettingsActivity) ctx),
								(v4_0 ? ((SettingsFragment) ctx)
										.getString(R.string.NoNetwork)
										: ((SettingsActivity) ctx)
												.getString(R.string.NoNetwork)),
								Toast.LENGTH_LONG).show();
					}

				}
				return true;
			}
		});

		// Change Sync-Intervall
		ListPreference syncIntervall = (ListPreference) (v4_0 ? ((SettingsFragment) ctx)
				.findPreference("syncFrequency") : ((SettingsActivity) ctx)
				.findPreference("syncFrequency"));
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
							// ContentResolver.setMasterSyncAutomatically(true);
							ContentResolver.addPeriodicSync(account,
									Mirakel.AUTHORITY_TYP, null,
									((Long) newValue) * 60);
						}
						return true;
					}
				});

		Intent startSpecialListsIntent = new Intent(
				(v4_0 ? ((SettingsFragment) ctx).getActivity()
						: (SettingsActivity) ctx), SpecialListsSettings.class);
		Preference specialLists = (Preference) (v4_0 ? ((SettingsFragment) ctx)
				.findPreference("special_lists") : ((SettingsActivity) ctx)
				.findPreference("special_lists"));
		specialLists.setIntent(startSpecialListsIntent);

		Preference backup = (Preference) (v4_0 ? ((SettingsFragment) ctx)
				.findPreference("backup") : ((SettingsActivity) ctx)
				.findPreference("backup"));

		backup.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@SuppressLint("NewApi")
			public boolean onPreferenceClick(Preference preference) {
				new Backup((v4_0 ? ((SettingsFragment) ctx).getActivity()
						: (SettingsActivity) ctx)).exportDB();
				return true;
			}
		});
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
