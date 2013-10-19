package de.azapps.mirakel.helper;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakel.settings.recurring.RecurringActivity;
import de.azapps.mirakel.settings.semantics.SemanticsSettingsActivity;
import de.azapps.mirakel.settings.special_list.SpecialListsSettingsActivity;
import de.azapps.mirakel.static_activities.CreditsActivity;
import de.azapps.mirakel.static_activities.SettingsActivity;
import de.azapps.mirakel.static_activities.SettingsFragment;
import de.azapps.mirakel.sync.AuthenticatorActivity;
import de.azapps.mirakel.sync.DataDownloadCommand;
import de.azapps.mirakel.sync.Network;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.sync.caldav.CalDavSync;
import de.azapps.mirakel.sync.mirakel.MirakelSync;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSync;
import de.azapps.mirakel.widget.MainWidgetSettingsActivity;
import de.azapps.mirakel.widget.MainWidgetSettingsFragment;
import de.azapps.mirakelandroid.R;

@SuppressLint("SimpleDateFormat")
public class PreferencesHelper {

	private static final String TAG = "PreferencesHelper";
	private final Object ctx;
	private final Activity activity;
	private static boolean v4_0;
	static View numberPicker;
	private SharedPreferences settings;
	public Switch actionBarSwitch;

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
		settings = PreferenceManager.getDefaultSharedPreferences(activity);
		List<ListMirakel> lists = ListMirakel.all();
		CharSequence entryValues[] = new String[lists.size()];
		CharSequence entries[] = new String[lists.size()];
		int i = 0;
		for (ListMirakel list : lists) {
			entryValues[i] = String.valueOf(list.getId());
			entries[i] = list.getName();
			i++;
		}

		final ListPreference widgetListPreference = (ListPreference) findPreference("widgetList");
		widgetListPreference.setEntries(entries);
		widgetListPreference.setEntryValues(entryValues);
		widgetListPreference.setSummary(activity.getString(
				R.string.widget_list_summary, ListMirakel.getList(Integer
						.parseInt(settings.getString("widgetList", SpecialList
								.firstSpecialSafe(activity).getId() + "")))));
		widgetListPreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						String list = ListMirakel.getList(
								Integer.parseInt((String) newValue)).getName();
						widgetListPreference.setSummary(activity.getString(
								R.string.notifications_list_summary, list));
						return true;
					}
				});

		final Preference widgetListSort = findPreference("widgetSort");
		widgetListSort.setSummary(activity.getString(
				R.string.widget_sort_summary, activity.getResources()
						.getStringArray(R.array.task_sorting_items)[Integer
						.parseInt(settings.getString("widgetSort", "0"))]));
		widgetListSort
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						widgetListSort.setSummary(activity.getString(
								R.string.widget_sort_summary,
								activity.getResources().getStringArray(
										R.array.task_sorting_items)[Integer
										.parseInt((String) newValue)]));
						return true;
					}
				});

	}

	private ListMirakel getListFromIdString(String preference) {
		try {
			return ListMirakel.getList(Integer.parseInt(preference));
		} catch (NumberFormatException e) {
			ListMirakel list = SpecialList.firstSpecial();
			if (list == null)
				list = ListMirakel.first();
			return list;
		}
	}

	@SuppressLint("NewApi")
	public void setFunctionsApp() {
		settings = PreferenceManager.getDefaultSharedPreferences(activity);

		// Initialize needed Arrays
		final List<ListMirakel> lists = ListMirakel.all();
		CharSequence entryValues[] = new String[lists.size()];
		CharSequence entries[] = new String[lists.size()];
		CharSequence entryValuesWithDefault[] = new String[lists.size() + 1];
		CharSequence entriesWithDefault[] = new String[lists.size() + 1];
		int i = 0;
		for (ListMirakel list : lists) {
			String id = String.valueOf(list.getId());
			String name = list.getName();
			entryValues[i] = id;
			entries[i] = name;
			entryValuesWithDefault[i + 1] = id;
			entriesWithDefault[i + 1] = name;
			i++;
		}
		entriesWithDefault[0] = activity.getString(R.string.default_list);
		entryValuesWithDefault[0] = "default";

		// Load the preferences from an XML resource

		// Notifications List
		final ListPreference notificationsListPreference = (ListPreference) findPreference("notificationsList");
		if (notificationsListPreference != null) {
			notificationsListPreference.setEntries(entries);
			notificationsListPreference.setEntryValues(entryValues);
			ListMirakel notificationsList = getListFromIdString(settings
					.getString("notificationsList", "-1"));

			notificationsListPreference.setSummary(activity.getString(
					R.string.notifications_list_summary,
					notificationsList.getName()));
			notificationsListPreference
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							String list = ListMirakel.getList(
									Integer.parseInt((String) newValue))
									.getName();
							notificationsListPreference.setSummary(activity
									.getString(
											R.string.notifications_list_summary,
											list));
							return true;
						}
					});
		}

		final ListPreference notificationsListOpenPreference = (ListPreference) findPreference("notificationsListOpen");
		if (notificationsListOpenPreference != null) {
			notificationsListOpenPreference.setEntries(entriesWithDefault);
			notificationsListOpenPreference
					.setEntryValues(entryValuesWithDefault);
			ListMirakel notificationsListOpen = getListFromIdString(settings
					.getString("notificationsListOpen", "-1"));
			notificationsListOpenPreference.setSummary(activity.getString(
					R.string.notifications_list_open_summary,
					notificationsListOpen.getName()));
			notificationsListOpenPreference
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							String list = ListMirakel.getList(
									Integer.parseInt((String) newValue))
									.getName();
							notificationsListOpenPreference.setSummary(activity
									.getString(
											R.string.notifications_list_summary,
											list));
							return true;
						}
					});
		}

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			removePreference("dashclock");
		}

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			PreferenceCategory cat = (PreferenceCategory) findPreference("notifications");
			Preference notificationsBig = findPreference("notificationsBig");
			if (cat != null && notificationsBig != null) {
				cat.removePreference(notificationsBig);
			}
		}

		final CheckBoxPreference darkTheme = (CheckBoxPreference) findPreference("DarkTheme");
		if (darkTheme != null) {
			darkTheme
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							activity.finish();
							activity.startActivity(activity.getIntent());

							return true;
						}
					});
		}

		// Startup
		final CheckBoxPreference startupAllListPreference = (CheckBoxPreference) findPreference("startupAllLists");
		if (startupAllListPreference != null) {
			final ListPreference startupListPreference = (ListPreference) findPreference("startupList");
			startupAllListPreference
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							if (!(Boolean) newValue) {
								startupListPreference.setSummary(activity
										.getString(
												R.string.startup_list_summary,
												ListMirakel.getList(Integer.parseInt(settings
														.getString(
																"startupList",
																"-1")))));
								startupListPreference.setEnabled(true);
							} else {
								startupListPreference.setSummary(" ");
								startupListPreference.setEnabled(false);
							}
							return true;
						}
					});
			startupListPreference.setEntries(entries);
			startupListPreference.setEntryValues(entryValues);
			if (settings.getBoolean("startupAllLists", false)) {
				startupListPreference.setSummary(" ");
				startupListPreference.setEnabled(false);
			} else {
				ListMirakel startupList = getListFromIdString(settings
						.getString("startupList", "-1"));
				startupListPreference.setSummary(activity.getString(
						R.string.startup_list_summary, startupList.getName()));
				startupListPreference.setEnabled(true);
			}
			startupListPreference
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							startupListPreference.setSummary(activity
									.getString(
											R.string.startup_list_summary,
											ListMirakel.getList(Integer
													.parseInt((String) newValue))));
							return true;
						}
					});
		}
		// Enable/Disbale Sync
		final CheckBoxPreference sync = (CheckBoxPreference) findPreference("syncUse");
		final Preference syncType = findPreference("syncType");
		final AccountManager am = AccountManager.get(activity);
		final Account[] accounts = am.getAccountsByType(Mirakel.ACCOUNT_TYPE);
		if (syncType != null && am != null) {
			if (settings.getBoolean("syncUse", false) && accounts.length > 0) {
				if (am.getUserData(accounts[0], SyncAdapter.BUNDLE_SERVER_TYPE)
						.equals(TaskWarriorSync.TYPE)) {
					syncType.setSummary(activity.getString(
							R.string.sync_use_summary_taskwarrior,
							accounts[0].name));
				} else if (am.getUserData(accounts[0],
						SyncAdapter.BUNDLE_SERVER_TYPE)
						.equals(MirakelSync.TYPE)) {
					syncType.setSummary(activity
							.getString(R.string.sync_use_summary_mirakel,
									accounts[0].name));
				} else if (am.getUserData(accounts[0],
						SyncAdapter.BUNDLE_SERVER_TYPE).equals(CalDavSync.TYPE)) {

					syncType.setSummary(activity.getString(
							R.string.sync_use_summary_caldav, accounts[0].name));

				} else {
					// sync.setChecked(false);
					sync.setSummary(R.string.sync_use_summary_nothing);
					am.removeAccount(accounts[0], null, null);
				}
			} else {
				// sync.setChecked(false);
				sync.setSummary(R.string.sync_use_summary_nothing);
			}
		}
		if (sync != null) {
			sync.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					boolean isChecked = (Boolean) newValue;
					createAuthActivity(isChecked, activity, sync, false);
					findPreference("syncServer").setEnabled(isChecked);
					findPreference("syncPassword").setEnabled(isChecked);
					findPreference("syncFrequency").setEnabled(isChecked);
					return false;
				}

			});
			// Get Account if existing
			final Account account;
			if (!(accounts.length > 0)) {
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(activity);
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean("syncUse", false);
				editor.commit();
				sync.setChecked(false);
				account = null;
			} else {
				sync.setChecked(true);
				account = accounts[0];
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
							data.add(new BasicNameValuePair("email",
									account.name));
							data.add(new BasicNameValuePair("password",
									(String) password));

							new Network(new DataDownloadCommand() {
								@Override
								public void after_exec(String result) {
									String t = Network.getToken(result);
									if (t != null) {
										am.setPassword(account,
												(String) password);
										Password.setText(am
												.getPassword(account));
										new Network(new DataDownloadCommand() {
											@Override
											public void after_exec(String result) {
											}
										}, Network.HttpMode.DELETE,
												activity, null).execute(am
												.getUserData(
														account,
														SyncAdapter.BUNDLE_SERVER_URL)
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
											SyncAdapter.BUNDLE_SERVER_URL)
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
				url.setSummary(activity.getString(R.string.sync_server_summary,
						am.getUserData(account, "url")));
			} else {
				url.setSummary("");
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
							data.add(new BasicNameValuePair("email",
									account.name));
							data.add(new BasicNameValuePair("password",
									AccountManager.get(activity).getPassword(
											account)));

							new Network(new DataDownloadCommand() {
								@Override
								public void after_exec(String result) {
									String t = Network.getToken(result);
									if (t != null) {
										am.setUserData(account, "url",
												(String) url);
										Password.setText(am
												.getPassword(account));
										new Network(new DataDownloadCommand() {
											@Override
											public void after_exec(String result) {
											}
										}, Network.HttpMode.DELETE,
												activity, null).execute(url
												+ "/tokens/" + t);
									} else {
										Toast.makeText(
												activity,
												activity.getString(R.string.inavlidUrl),
												Toast.LENGTH_LONG).show();
										SharedPreferences settings = PreferenceManager
												.getDefaultSharedPreferences(activity);
										SharedPreferences.Editor editor = settings
												.edit();
										editor.putString(
												"syncServer",
												am.getUserData(
														account,
														SyncAdapter.BUNDLE_SERVER_URL));
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
			final ListPreference syncInterval = (ListPreference) findPreference("syncFrequency");
			if (settings.getString("syncFrequency", "-1").equals("-1")) {
				syncInterval.setSummary(R.string.sync_frequency_summary_man);
			} else {
				syncInterval.setSummary(activity.getString(
						R.string.sync_frequency_summary,
						settings.getString("syncFrequency", "-1")));
			}
			syncInterval
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							Log.e(TAG, "" + newValue.toString());
							final Bundle bundle = new Bundle();
							long longVal = Long.parseLong(newValue.toString());
							if (longVal == -1) {
								syncInterval
										.setSummary(R.string.sync_frequency_summary_man);
							} else {
								syncInterval.setSummary(activity.getString(
										R.string.sync_frequency_summary,
										longVal));
							}
							if (account != null) {
								ContentResolver.removePeriodicSync(account,
										Mirakel.AUTHORITY_TYP, bundle);
								if (longVal != -1) {
									ContentResolver.setSyncAutomatically(
											account, Mirakel.AUTHORITY_TYP,
											true);
									ContentResolver.setIsSyncable(account,
											Mirakel.AUTHORITY_TYP, 1);
									// ContentResolver.setMasterSyncAutomatically(true);
									ContentResolver.addPeriodicSync(account,
											Mirakel.AUTHORITY_TYP, bundle,
											longVal * 60);
								}
							} else {
								Log.d(TAG, "account does not exsist");
							}
							return true;
						}
					});

			if (!settings.getBoolean("syncUse", false)) {
				findPreference("syncServer").setEnabled(false);
				findPreference("syncPassword").setEnabled(false);
				findPreference("syncFrequency").setEnabled(false);
			}

			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				removePreference("syncUse");
				ActionBar actionbar = activity.getActionBar();

				actionBarSwitch = new Switch(activity);
				actionBarSwitch.setId(R.id.preferences_switch_top);

				actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
						ActionBar.DISPLAY_SHOW_CUSTOM);
				actionbar.setCustomView(actionBarSwitch,
						new ActionBar.LayoutParams(
								ActionBar.LayoutParams.WRAP_CONTENT,
								ActionBar.LayoutParams.WRAP_CONTENT,
								Gravity.CENTER_VERTICAL | Gravity.RIGHT));
				actionBarSwitch.setChecked(settings
						.getBoolean("syncUse", false));
				actionBarSwitch
						.setOnCheckedChangeListener(new OnCheckedChangeListener() {

							@Override
							public void onCheckedChanged(
									CompoundButton buttonView, boolean isChecked) {
								PreferencesHelper.createAuthActivity(isChecked,
										(Fragment) ctx, actionBarSwitch, true);
								findPreference("syncServer").setEnabled(
										isChecked);
								findPreference("syncPassword").setEnabled(
										isChecked);
								findPreference("syncFrequency").setEnabled(
										isChecked);
								if (activity.getResources().getBoolean(
										R.bool.isTablet)) {
									final Switch s = ((Switch) activity
											.findViewById(R.id.switchWidget));
									s.setOnCheckedChangeListener(null);
									s.setChecked(isChecked);
									s.setOnCheckedChangeListener(new OnCheckedChangeListener() {
										@Override
										public void onCheckedChanged(
												CompoundButton buttonView,
												boolean isChecked) {
											PreferencesHelper
													.createAuthActivity(
															isChecked,
															(Activity) ctx, s,
															false);
										}
									});

								}
							}
						});
			}
		}

		final CheckBoxPreference notificationsUse = (CheckBoxPreference) findPreference("notificationsUse");
		if (notificationsUse != null) {
			notificationsUse
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							if ((Boolean) newValue) {
								activity.startService(new Intent(activity,
										NotificationService.class));
							} else {
								if (activity.startService(new Intent(activity,
										NotificationService.class)) != null) {
									activity.stopService(new Intent(activity,
											NotificationService.class));
								}
							}
							Editor e = preference.getEditor();
							e.putBoolean("notificationsUse", (Boolean) newValue);
							e.commit();
							NotificationService
									.updateNotificationAndWidget(activity);
							return true;
						}
					});
		}

		Intent startSpecialListsIntent = new Intent(activity,
				SpecialListsSettingsActivity.class);
		Preference specialLists = findPreference("special_lists");
		if (specialLists != null) {
			specialLists.setIntent(startSpecialListsIntent);
		}

		Preference backup = findPreference("backup");
		if (backup != null) {

			backup.setSummary(activity.getString(R.string.backup_click_summary,
					ExportImport.getBackupDir().getAbsolutePath()));

			backup.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@SuppressLint("NewApi")
				public boolean onPreferenceClick(Preference preference) {
					ExportImport.exportDB(activity);
					return true;
				}
			});
		}

		Preference importDB = findPreference("import");
		if (importDB != null) {
			importDB.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Helpers.showFileChooser(SettingsActivity.FILE_IMPORT_DB,
							activity.getString(R.string.import_title), activity);
					return true;
				}
			});
		}

		Preference changelog = findPreference("changelog");
		if (changelog != null) {
			changelog
					.setOnPreferenceClickListener(new OnPreferenceClickListener() {
						@SuppressLint("NewApi")
						public boolean onPreferenceClick(Preference preference) {
							ChangeLog cl = new ChangeLog(activity);
							cl.getFullLogDialog().show();
							return true;
						}
					});
		}
		Preference importAstrid = findPreference("import_astrid");
		if (importAstrid != null) {
			importAstrid
					.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(Preference preference) {
							Helpers.showFileChooser(
									SettingsActivity.FILE_ASTRID,
									activity.getString(R.string.astrid_import_title),
									activity);
							return true;
						}
					});
		}
		final CheckBoxPreference killButton = (CheckBoxPreference) findPreference("KillButton");
		if (killButton != null) {
			killButton
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							if ((Boolean) newValue) {
								AlertDialog.Builder builder = new AlertDialog.Builder(
										activity);
								builder.setTitle(R.string.kill_sure);
								builder.setMessage(R.string.kill_sure_message)
										.setPositiveButton(
												android.R.string.yes,
												new DialogInterface.OnClickListener() {
													@Override
													public void onClick(
															DialogInterface dialog,
															int which) {
													}
												})
										.setNegativeButton(
												android.R.string.cancel,
												new OnClickListener() {
													@Override
													public void onClick(
															DialogInterface dialog,
															int which) {
														((CheckBoxPreference) findPreference("KillButton"))
																.setChecked(false);

													}
												}).show();
							}
							return true;
						}
					});
		}
		final EditTextPreference importFileType = (EditTextPreference) findPreference("import_file_title");
		if (importFileType != null) {
			importFileType.setSummary(settings.getString("import_file_title",
					activity.getString(R.string.file_default_title)));
		}
		final CheckBoxPreference importDefaultList = (CheckBoxPreference) findPreference("importDefaultList");
		if (importDefaultList != null) {
			if (settings.getBoolean("importDefaultList", false)) {
				ListMirakel list = ListMirakel.getList(settings.getInt(
						"defaultImportList", -1));
				if (list == null)
					list = ListMirakel.first();
				importDefaultList.setSummary(activity.getString(
						R.string.import_default_list_summary, list.getName()));
			} else {
				importDefaultList
						.setSummary(R.string.import_no_default_list_summary);
			}

			importDefaultList
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							if ((Boolean) newValue) {
								AlertDialog.Builder builder = new AlertDialog.Builder(
										activity);
								builder.setTitle(R.string.import_to);
								final List<CharSequence> items = new ArrayList<CharSequence>();
								final List<Integer> list_ids = new ArrayList<Integer>();
								int currentItem = 0;
								for (ListMirakel list : ListMirakel.all()) {
									if (list.getId() > 0) {
										items.add(list.getName());
										list_ids.add(list.getId());
									}

								}
								builder.setSingleChoiceItems(
										items.toArray(new CharSequence[items
												.size()]), currentItem,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int item) {
												importDefaultList.setSummary(activity
														.getString(
																R.string.import_default_list_summary,
																items.get(item)));
												SharedPreferences.Editor editor = settings
														.edit();
												editor.putInt(
														"defaultImportList",
														list_ids.get(item));
												editor.commit();
												dialog.dismiss();
											}
										});
								builder.setOnCancelListener(new OnCancelListener() {
									@Override
									public void onCancel(DialogInterface dialog) {
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
		}

		// Delete done tasks
		Preference deleteDone = (Preference) findPreference("deleteDone");
		if (deleteDone != null) {
			deleteDone
					.setOnPreferenceClickListener(new OnPreferenceClickListener() {

						@Override
						public boolean onPreferenceClick(Preference preference) {
							new AlertDialog.Builder(activity)
									.setTitle(R.string.delete_done_warning)
									.setMessage(
											R.string.delete_done_warning_message)
									.setPositiveButton(android.R.string.ok,
											new OnClickListener() {
												@Override
												public void onClick(
														DialogInterface dialogInterface,
														int i) {
													Task.deleteDoneTasks();
													Toast.makeText(
															activity,
															R.string.delete_done_success,
															Toast.LENGTH_SHORT)
															.show();
													android.os.Process
															.killProcess(android.os.Process
																	.myPid());
												}
											})
									.setNegativeButton(android.R.string.cancel,
											new OnClickListener() {
												@Override
												public void onClick(
														DialogInterface dialogInterface,
														int i) {
												}
											}).show();
							return true;
						}
					});
		}
		final Preference undoNumber = (Preference) findPreference("UndoNumber");
		if (undoNumber != null) {
			undoNumber.setSummary(activity.getString(
					R.string.undo_number_summary,
					settings.getInt("UndoNumber", 10)));
			undoNumber
					.setOnPreferenceClickListener(new OnPreferenceClickListener() {

						@Override
						public boolean onPreferenceClick(Preference preference) {
							final int old_val = PreferenceManager
									.getDefaultSharedPreferences(activity)
									.getInt("UndoNumber", 10);
							final int max = 25;
							final int min = 1;
							if (v4_0) {
								numberPicker = new NumberPicker(activity);
								((NumberPicker) numberPicker).setMaxValue(max);
								((NumberPicker) numberPicker).setMinValue(min);
								((NumberPicker) numberPicker)
										.setWrapSelectorWheel(false);
								((NumberPicker) numberPicker).setValue(old_val);
								((NumberPicker) numberPicker)
										.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
							} else {
								numberPicker = ((LayoutInflater) activity
										.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
										.inflate(
												R.layout.dialog_num_picker_v10,
												null);

								((TextView) numberPicker
										.findViewById(R.id.dialog_num_pick_val))
										.setText(old_val + "");
								((Button) numberPicker
										.findViewById(R.id.dialog_num_pick_plus))
										.setOnClickListener(new View.OnClickListener() {
											@Override
											public void onClick(View v) {
												int val = Integer
														.parseInt(((TextView) numberPicker
																.findViewById(R.id.dialog_num_pick_val))
																.getText()
																.toString());
												if (val < max) {
													((TextView) numberPicker
															.findViewById(R.id.dialog_num_pick_val))
															.setText(++val + "");
												}
											}
										});
								((Button) numberPicker
										.findViewById(R.id.dialog_num_pick_minus))
										.setOnClickListener(new View.OnClickListener() {
											@Override
											public void onClick(View v) {
												int val = Integer
														.parseInt(((TextView) numberPicker
																.findViewById(R.id.dialog_num_pick_val))
																.getText()
																.toString());
												if (val > min) {
													((TextView) numberPicker
															.findViewById(R.id.dialog_num_pick_val))
															.setText(--val + "");
												}
											}
										});
							}
							new AlertDialog.Builder(activity)
									.setTitle(R.string.undo_number)
									.setMessage(R.string.undo_number_summary)
									.setView(numberPicker)
									.setPositiveButton(
											android.R.string.ok,
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialog,
														int whichButton) {
													SharedPreferences.Editor editor = PreferenceManager
															.getDefaultSharedPreferences(
																	activity)
															.edit();
													int val;
													if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
														val = ((NumberPicker) numberPicker)
																.getValue();
													} else {
														val = Integer
																.parseInt(((TextView) numberPicker
																		.findViewById(R.id.dialog_num_pick_val))
																		.getText()
																		.toString());
													}
													editor.putInt("UndoNumber",
															val);
													undoNumber
															.setSummary(activity
																	.getString(
																			R.string.undo_number_summary,
																			val));
													if (old_val > val) {
														for (int i = val; i < max; i++) {
															editor.putString(
																	Helpers.UNDO
																			+ i,
																	"");
														}
													}
													editor.commit();
												}
											})
									.setNegativeButton(
											android.R.string.cancel,
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialog,
														int whichButton) {
													// Do nothing.
												}
											}).show();
							return true;
						}
					});
		}

		Preference dashclock = findPreference("dashclock");
		if (dashclock != null) {
			Intent startdashclockIntent = new Intent(
					Intent.ACTION_VIEW,
					Uri.parse("http://play.google.com/store/apps/details?id=de.azapps.mirakel.dashclock"));
			dashclock.setIntent(startdashclockIntent);
		}
		Preference credits = findPreference("credits");
		if (credits != null) {
			Intent startCreditsIntent = new Intent(activity,
					CreditsActivity.class);
			credits.setIntent(startCreditsIntent);
		}

		Preference contact = findPreference("contact");
		if (contact != null) {
			contact.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					Helpers.contact(activity);
					return false;
				}
			});
		}

		Intent startSemanticsIntent = new Intent(activity,
				SemanticsSettingsActivity.class);
		Preference semantics = findPreference("semanticNewTaskSettings");
		if (semantics != null) {
			semantics.setIntent(startSemanticsIntent);
		}

		Intent startRecurringIntent = new Intent(activity,
				RecurringActivity.class);
		Preference recurring = findPreference("recurring");
		if (recurring != null) {
			recurring.setIntent(startRecurringIntent);
		}

		final CheckBoxPreference subTaskAddToSameList = (CheckBoxPreference) findPreference("subtaskAddToSameList");
		if (subTaskAddToSameList != null) {
			if (!settings.getBoolean("subtaskAddToSameList", true)) {
				subTaskAddToSameList.setSummary(activity.getString(
						R.string.settings_subtask_add_to_list_summary,
						ListMirakel.getList(
								settings.getInt("subtaskAddToList", -1))
								.getName()));
			} else {
				subTaskAddToSameList
						.setSummary(R.string.settings_subtask_add_to_same_list_summary);
			}

			subTaskAddToSameList
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							if (!(Boolean) newValue) {
								AlertDialog.Builder builder = new AlertDialog.Builder(
										activity);
								builder.setTitle(R.string.import_to);
								final List<CharSequence> items = new ArrayList<CharSequence>();
								final List<Integer> list_ids = new ArrayList<Integer>();
								int currentItem = 0;
								for (ListMirakel list : ListMirakel.all()) {
									if (list.getId() > 0) {
										items.add(list.getName());
										list_ids.add(list.getId());
									}

								}
								builder.setSingleChoiceItems(
										items.toArray(new CharSequence[items
												.size()]), currentItem,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int item) {
												subTaskAddToSameList
														.setSummary(items
																.get(item));
												subTaskAddToSameList.setSummary(activity
														.getString(
																R.string.settings_subtask_add_to_list_summary,
																items.get(item)));
												SharedPreferences.Editor editor = settings
														.edit();
												editor.putInt(
														"subtaskAddToList",
														list_ids.get(item));
												editor.commit();
												dialog.dismiss();
											}
										});
								builder.setOnCancelListener(new OnCancelListener() {
									@Override
									public void onCancel(DialogInterface dialog) {
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

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private void removePreference(String which) {
		Preference pref = findPreference(which);
		if (pref != null) {
			if (v4_0) {
				((PreferenceFragment) ctx).getPreferenceScreen()
						.removePreference(pref);
			} else {
				((PreferenceActivity) activity).getPreferenceScreen()
						.removePreference(pref);
			}
		}
	}

	@SuppressLint("NewApi")
	public static void createAuthActivity(boolean newValue,
			final Object activity, final Object box, final boolean fragment) {
		final Context ctx;
		if (fragment) {
			ctx = ((Fragment) activity).getActivity();
		} else {
			ctx = (Activity) activity;
		}
		final AccountManager am = AccountManager.get(ctx);
		final Account[] accounts = am.getAccountsByType(Mirakel.ACCOUNT_TYPE);
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		if (newValue) {
			new AlertDialog.Builder(ctx)
					.setTitle(R.string.sync_warning)
					.setMessage(R.string.sync_warning_message)
					.setPositiveButton(android.R.string.ok,
							new OnClickListener() {
								@SuppressLint("NewApi")
								@Override
								public void onClick(
										DialogInterface dialogInterface, int i) {
									for (Account a : accounts) {
										try {
											am.removeAccount(a, null, null);
										} catch (Exception e) {
											Log.e(TAG, "Cannot remove Account");
										}
									}
									Intent intent = new Intent(ctx,
											AuthenticatorActivity.class);
									intent.setAction(MainActivity.SHOW_LISTS);
									if (fragment) {
										((Fragment) activity)
												.startActivityForResult(
														intent,
														SettingsActivity.NEW_ACCOUNT);
									} else {
										((Activity) activity)
												.startActivityForResult(
														intent,
														SettingsActivity.NEW_ACCOUNT);
									}
									SharedPreferences.Editor editor = settings
											.edit();
									editor.putBoolean("syncUse", true);
									editor.commit();
								}
							})
					.setNegativeButton(android.R.string.cancel,
							new OnClickListener() {
								@SuppressLint("NewApi")
								@Override
								public void onClick(
										DialogInterface dialogInterface, int i) {
									SharedPreferences.Editor editor = settings
											.edit();
									editor.putBoolean("syncUse", false);
									editor.commit();
									if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
										((CheckBoxPreference) box)
												.setChecked(false);
										((CheckBoxPreference) box)
												.setSummary(R.string.sync_use_summary_nothing);
									} else {
										((Switch) box).setChecked(false);
									}
								}
							}).show();
		} else {
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("syncUse", false);
			editor.commit();
			try {
				am.removeAccount(accounts[0], null, null);
			} catch (Exception e) {
				Log.e(TAG, "Cannot remove Account");
			}
		}
	}

	public static void updateSyncText(CheckBoxPreference sync,
			Preference server, Preference syncFrequency, Context ctx) {
		AccountManager am = AccountManager.get(ctx);
		Account[] accounts = am.getAccountsByType(Mirakel.ACCOUNT_TYPE);
		if (accounts.length > 0) {
			if (sync != null) {
				if (am.getUserData(accounts[0], SyncAdapter.BUNDLE_SERVER_TYPE)
						.equals(TaskWarriorSync.TYPE)) {
					sync.setSummary(ctx.getString(
							R.string.sync_use_summary_taskwarrior,
							accounts[0].name));
				} else if (am.getUserData(accounts[0],
						SyncAdapter.BUNDLE_SERVER_TYPE)
						.equals(MirakelSync.TYPE)) {
					sync.setSummary(ctx
							.getString(R.string.sync_use_summary_mirakel,
									accounts[0].name));
				} else {
					sync.setChecked(false);
					sync.setSummary(R.string.sync_use_summary_nothing);
					am.removeAccount(accounts[0], null, null);
				}
			}
			server.setSummary(ctx.getString(R.string.sync_server_summary,
					am.getUserData(accounts[0], SyncAdapter.BUNDLE_SERVER_URL)));
		} else {
			if (sync != null) {
				sync.setChecked(false);
				sync.setSummary(R.string.sync_use_summary_nothing);
			}
			server.setSummary("");
		}
		Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx)
				.edit();
		editor.putString("syncFrequency", "-1");
		editor.commit();
		if (syncFrequency != null) {
			syncFrequency.setSummary(ctx
					.getString(R.string.sync_frequency_summary_man));
		}
	}

}
