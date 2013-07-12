package de.azapps.mirakel.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
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
	private final Object ctx;
	private final Activity activity;
	private final boolean v4_0;

	public PreferencesHelper(SettingsActivity c) {
		ctx = c;
		v4_0 = false;
		activity = c;
	}

	@SuppressLint("NewApi")
	public PreferencesHelper(SettingsFragment c) {
		ctx = c;
		v4_0 = true;
		activity = c.getActivity();
	}

	public PreferencesHelper(MainWidgetSettingsActivity c) {
		ctx = c;
		v4_0 = false;
		activity = c;
	}

	@SuppressLint("NewApi")
	public PreferencesHelper(MainWidgetSettingsFragment c) {
		ctx = c;
		v4_0 = true;
		activity = c.getActivity();
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private Preference findPreference(String key) {
		if (v4_0) {
			return ((PreferenceFragment) ctx).findPreference(key);
		} else {
			return ((PreferenceActivity) ctx).findPreference(key);
		}
	}

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
		ListPreference notificationsListPreference = (ListPreference) findPreference("widgetList");
		notificationsListPreference.setEntries(entries);
		notificationsListPreference.setEntryValues(entryValues);

	}

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
		ListPreference notificationsListPreference = (ListPreference) findPreference("notificationsList");
		notificationsListPreference.setEntries(entries);
		notificationsListPreference.setEntryValues(entryValues);
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			CheckBoxPreference mCheckBoxPref = (CheckBoxPreference) findPreference("notificationsBig");
			PreferenceCategory mCategory = (PreferenceCategory) findPreference("category_notifications");
			mCategory.removePreference(mCheckBoxPref);
		}

		// Startup
		CheckBoxPreference startupAllListPreference = (CheckBoxPreference) findPreference("startupAllLists");
		final ListPreference startupListPreference = (ListPreference) findPreference("startupList");
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
		CheckBoxPreference sync = (CheckBoxPreference) findPreference("syncUse");
		sync.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {

				Log.e(TAG, "" + newValue.toString());
				AccountManager am = AccountManager.get(activity);
				final Account[] accounts = am
						.getAccountsByType(Mirakel.ACCOUNT_TYP);
				if ((Boolean) newValue) {
					new AlertDialog.Builder(activity)
							.setTitle(R.string.sync_warning)
							.setMessage(R.string.sync_warning_message)
							.setPositiveButton(android.R.string.ok,
									new OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialogInterface,
												int i) {
											if (accounts.length == 0) {
												Intent intent = new Intent(
														activity,
														AuthenticatorActivity.class);
												intent.setAction(MainActivity.SHOW_LISTS);
												activity.startActivity(intent);
											}
										}
									})
							.setNegativeButton(android.R.string.cancel,
									new OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialogInterface,
												int i) {
											((CheckBoxPreference) findPreference("syncUse"))
													.setChecked(false);
										}
									}).show();
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
		final AccountManager am = AccountManager.get(activity);
		final Account account = getAccount(am);
		if (account == null) {
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(activity);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("syncUse", false);
			editor.commit();
			sync.setChecked(false);
		} else {
			sync.setChecked(true);
		}

		// Change Password
		final EditTextPreference Password = (EditTextPreference) findPreference("syncPassword");
		Password.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					final Object password) {
				if (account != null) {
					ConnectivityManager cm = (ConnectivityManager) activity
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
									}, Network.HttpMode.DELETE, activity, null)
											.execute(am.getUserData(account,
													Mirakel.BUNDLE_SERVER_URL)
													+ "/tokens/" + t);
								} else {
									Toast.makeText(
											activity,
											activity.getString(R.string.inavlidPassword),
											Toast.LENGTH_LONG).show();
								}
								SharedPreferences settings = PreferenceManager
										.getDefaultSharedPreferences(activity);
								SharedPreferences.Editor editor = settings
										.edit();
								editor.putString("syncPassword", "");
								editor.commit();

							}
						}, Network.HttpMode.POST, data, activity, null)
								.execute(am.getUserData(account,
										Mirakel.BUNDLE_SERVER_URL)
										+ "/tokens.json");
					} else {
						Toast.makeText(activity,
								activity.getString(R.string.NoNetwork),
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
		EditTextPreference url = (EditTextPreference) findPreference("syncServer");
		if (account != null) {
			url.setText(am.getUserData(account, "url"));
		}
		url.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					final Object url) {
				if (account != null) {
					ConnectivityManager cm = (ConnectivityManager) activity
							.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo netInfo = cm.getActiveNetworkInfo();
					if (netInfo != null && netInfo.isConnected()) {
						List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
						data.add(new BasicNameValuePair("email", account.name));
						data.add(new BasicNameValuePair("password",
								AccountManager.get(activity).getPassword(
										account)));

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
									}, Network.HttpMode.DELETE, activity, null)
											.execute(url + "/tokens/" + t);
								} else {
									Toast.makeText(
											activity,
											activity.getString(R.string.inavlidUrl),
											Toast.LENGTH_LONG).show();
									SharedPreferences settings = PreferenceManager
											.getDefaultSharedPreferences(activity);
									SharedPreferences.Editor editor = settings
											.edit();
									editor.putString("syncServer", am
											.getUserData(account,
													Mirakel.BUNDLE_SERVER_URL));
									editor.commit();
								}
							}
						}, Network.HttpMode.POST, data, activity, null)
								.execute((String) url + "/tokens.json");
					} else {
						Toast.makeText(activity,
								activity.getString(R.string.NoNetwork),
								Toast.LENGTH_LONG).show();
					}

				}
				return true;
			}
		});

		// Change Sync-Interval
		ListPreference syncInterval = (ListPreference) findPreference("syncFrequency");
		syncInterval
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

		Intent startSpecialListsIntent = new Intent(activity,
				SpecialListsSettings.class);
		Preference specialLists = findPreference("special_lists");
		specialLists.setIntent(startSpecialListsIntent);

		Preference backup = findPreference("backup");

		backup.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@SuppressLint("NewApi")
			public boolean onPreferenceClick(Preference preference) {
				File exportDir = new File(Environment
						.getExternalStorageDirectory(), "");
				ExportImport.exportDB(activity, exportDir);
				return true;
			}
		});

		Preference importDB = findPreference("import");

		importDB.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@SuppressLint("NewApi")
			public boolean onPreferenceClick(Preference preference) {
				new AlertDialog.Builder(activity)
						.setTitle(R.string.import_sure)
						.setMessage(R.string.import_sure_summary)
						.setNegativeButton(android.R.string.cancel,
								new OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {

									}
								})
						.setPositiveButton(android.R.string.yes,
								new OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										File exportDir = new File(Environment
												.getExternalStorageDirectory(),
												"");
										File file = new File(exportDir,
												"mirakel.db");
										ExportImport.importDB(activity, file);
									}
								}).create().show();
				return true;
			}
		});

		Preference changelog = findPreference("changelog");

		changelog.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@SuppressLint("NewApi")
			public boolean onPreferenceClick(Preference preference) {
				ChangeLog cl = new ChangeLog(activity);
				cl.getFullLogDialog().show();
				return true;
			}
		});

		Preference importAstrid = findPreference("import_astrid");
		importAstrid
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						showFileChooser(SettingsActivity.FILE_ASTRID);
						return true;
					}
				});
	}

	private void showFileChooser(int code) {

		Intent fileDialogIntent = new Intent(Intent.ACTION_GET_CONTENT);
		fileDialogIntent.setType("*/*");
		fileDialogIntent.addCategory(Intent.CATEGORY_OPENABLE);

		try {
			activity.startActivityForResult(Intent.createChooser(
					fileDialogIntent, "Select a File to Upload"), code);
		} catch (android.content.ActivityNotFoundException ex) {
			// Potentially direct the user to the Market with a
			// Dialog
			Toast.makeText(activity, "Please install a File Manager.",
					Toast.LENGTH_SHORT).show();
		}
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
