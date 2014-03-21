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
package de.azapps.mirakel.helper;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import de.azapps.changelog.Changelog;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.export_import.AnyDoImport;
import de.azapps.mirakel.helper.export_import.ExportImport;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.recurring.RecurringActivity;
import de.azapps.mirakel.settings.semantics.SemanticsSettingsActivity;
import de.azapps.mirakel.settings.special_list.SpecialListsSettingsActivity;
import de.azapps.mirakel.static_activities.CreditsActivity;
import de.azapps.mirakel.static_activities.SettingsActivity;
import de.azapps.mirakel.static_activities.SettingsFragment;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSync;
import de.azapps.tools.FileUtils;

@SuppressLint("SimpleDateFormat")
public class PreferencesAppHelper extends PreferencesHelper {

	public PreferencesAppHelper(final PreferenceActivity c) {
		super(c);
	}

	public PreferencesAppHelper(final PreferenceFragment c) {
		super(c);
	}

	static View numberPicker;

	// private static final String TAG = "PreferencesHelper";
	// @SuppressLint("NewApi")
	// public static void createAuthActivity(boolean newValue, final Object
	// activity, final Object box, final boolean fragment) {
	// final Context ctx;
	// if (fragment) {
	// ctx = ((Fragment) activity).getActivity();
	// } else {
	// ctx = (Activity) activity;
	// }
	// final AccountManager am = AccountManager.get(ctx);
	// final Account[] accounts = am
	// .getAccountsByType(AccountMirakel.ACCOUNT_TYPE_MIRAKEL);
	// if (newValue) {
	// new AlertDialog.Builder(ctx)
	// .setTitle(de.azapps.mirakel.sync.R.string.sync_warning)
	// .setMessage(de.azapps.mirakel.sync.R.string.sync_warning_message)
	// .setPositiveButton(android.R.string.ok,
	// new OnClickListener() {
	// @SuppressLint("NewApi")
	// @Override
	// public void onClick(DialogInterface dialogInterface, int i) {
	// for (Account a : accounts) {
	// try {
	// am.removeAccount(a, null, null);
	// } catch (Exception e) {
	// Log.e(TAG, "Cannot remove Account");
	// }
	// }
	// Intent intent = new Intent(ctx,
	// AuthenticatorActivity.class);
	// intent.setAction(DefinitionsHelper.MAIN_SHOW_LISTS);
	// if (fragment) {
	// ((Fragment) activity)
	// .startActivityForResult(
	// intent,
	// SettingsActivity.NEW_ACCOUNT);
	// } else {
	// ((Activity) activity)
	// .startActivityForResult(
	// intent,
	// SettingsActivity.NEW_ACCOUNT);
	// }
	// SharedPreferences.Editor editor = MirakelCommonPreferences
	// .getEditor();
	// editor.putBoolean("syncUse", true);
	// editor.commit();
	// }
	// })
	// .setNegativeButton(android.R.string.cancel,
	// new OnClickListener() {
	// @SuppressLint("NewApi")
	// @Override
	// public void onClick(DialogInterface dialogInterface, int i) {
	// SharedPreferences.Editor editor = MirakelCommonPreferences
	// .getEditor();
	// editor.putBoolean("syncUse", false);
	// editor.commit();
	// if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
	// ((CheckBoxPreference) box)
	// .setChecked(false);
	// ((CheckBoxPreference) box)
	// .setSummary(de.azapps.mirakel.sync.R.string.sync_use_summary_nothing);
	// } else {
	// ((Switch) box).setChecked(false);
	// }
	// }
	// }).show();
	// } else {
	// SharedPreferences.Editor editor = MirakelCommonPreferences.getEditor();
	// editor.putBoolean("syncUse", false);
	// editor.commit();
	// try {
	// am.removeAccount(accounts[0], null, null);
	// } catch (Exception e) {
	// Log.e(TAG, "Cannot remove Account");
	// }
	// }
	// }
	public static void updateSyncText(final CheckBoxPreference sync,
			final Preference server, final Preference syncFrequency,
			final Context ctx) {
		final AccountManager am = AccountManager.get(ctx);
		final Account[] accounts = am
				.getAccountsByType(AccountMirakel.ACCOUNT_TYPE_MIRAKEL);
		if (accounts.length > 0) {
			if (sync != null) {
				if (am.getUserData(accounts[0], SyncAdapter.BUNDLE_SERVER_TYPE)
						.equals(TaskWarriorSync.TYPE)) {
					sync.setSummary(ctx.getString(
							R.string.sync_use_summary_taskwarrior,
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
		final Editor editor = MirakelPreferences.getEditor();
		editor.putString("syncFrequency", "-1");
		editor.commit();
		if (syncFrequency != null) {
			syncFrequency.setSummary(ctx
					.getString(R.string.sync_frequency_summary_man));
		}
	}

	public Switch actionBarSwitch;
	protected int debugCounter;

	@SuppressLint("NewApi")
	public void setFunctionsApp() {

		// Initialize needed Arrays
		final List<ListMirakel> lists = ListMirakel.all();
		final CharSequence entryValues[] = new String[lists.size()];
		final CharSequence entries[] = new String[lists.size()];
		final CharSequence entryValuesWithDefault[] = new String[lists.size() + 1];
		final CharSequence entriesWithDefault[] = new String[lists.size() + 1];
		int i = 0;
		for (final ListMirakel list : lists) {
			final String id = String.valueOf(list.getId());
			final String name = list.getName();
			entryValues[i] = id;
			entries[i] = name;
			entryValuesWithDefault[i + 1] = id;
			entriesWithDefault[i + 1] = name;
			i++;
		}
		entriesWithDefault[0] = this.activity.getString(R.string.default_list);
		entryValuesWithDefault[0] = "default";

		// Load the preferences from an XML resource

		// Notifications List
		final ListPreference notificationsListPreference = (ListPreference) findPreference("notificationsList");
		if (notificationsListPreference != null) {
			final ListPreference notificationsListOpenPreference = (ListPreference) findPreference("notificationsListOpen");
			notificationsListPreference.setEntries(entries);
			notificationsListPreference.setEntryValues(entryValues);
			final ListMirakel notificationsList = MirakelModelPreferences
					.getNotificationsList();

			notificationsListPreference.setSummary(this.activity.getString(
					R.string.notifications_list_summary,
					notificationsList.getName()));
			notificationsListPreference
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						@Override
						public boolean onPreferenceChange(
								final Preference preference,
								final Object newValue) {
							final String list = ListMirakel.getList(
									Integer.parseInt((String) newValue))
									.getName();
							notificationsListPreference
									.setSummary(PreferencesAppHelper.this.activity
											.getString(
													R.string.notifications_list_summary,
													list));
							if (MirakelCommonPreferences
									.isNotificationListOpenDefault()) {
								notificationsListOpenPreference
										.setSummary(PreferencesAppHelper.this.activity
												.getString(
														R.string.notifications_list_summary,
														list));
							}
							return true;
						}
					});

			notificationsListOpenPreference.setEntries(entriesWithDefault);
			notificationsListOpenPreference
					.setEntryValues(entryValuesWithDefault);
			final ListMirakel notificationsListOpen = MirakelModelPreferences
					.getNotificationsListOpen();
			notificationsListOpenPreference.setSummary(this.activity.getString(
					R.string.notifications_list_open_summary,
					notificationsListOpen.getName()));
			notificationsListOpenPreference
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						@Override
						public boolean onPreferenceChange(
								final Preference preference,
								final Object newValue) {
							String list;
							if (!"default".equals(newValue.toString())) {
								list = ListMirakel.getList(
										Integer.parseInt((String) newValue))
										.getName();
							} else {
								list = MirakelModelPreferences
										.getNotificationsList().getName();
							}
							notificationsListOpenPreference
									.setSummary(PreferencesAppHelper.this.activity
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
			final PreferenceCategory cat = (PreferenceCategory) findPreference("notifications");
			final Preference notificationsBig = findPreference("notificationsBig");
			if ((cat != null) && (notificationsBig != null)) {
				cat.removePreference(notificationsBig);
			}
		}

		final CheckBoxPreference darkTheme = (CheckBoxPreference) findPreference("DarkTheme");
		if (darkTheme != null) {
			darkTheme
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(
								final Preference preference,
								final Object newValue) {
							PreferencesAppHelper.this.activity.finish();
							PreferencesAppHelper.this.activity
									.startActivity(PreferencesAppHelper.this.activity
											.getIntent());

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
								final Preference preference,
								final Object newValue) {
							if (!(Boolean) newValue) {
								startupListPreference
										.setSummary(PreferencesAppHelper.this.activity
												.getString(
														R.string.startup_list_summary,
														MirakelModelPreferences
																.getStartupList()
																.getName()));
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
			if (MirakelCommonPreferences.isStartupAllLists()) {
				startupListPreference.setSummary(" ");
				startupListPreference.setEnabled(false);
			} else {
				ListMirakel startupList = MirakelModelPreferences
						.getStartupList();
				if (startupList == null) {
					startupList = SpecialList.firstSpecialSafe(this.activity);
				}
				startupListPreference.setSummary(this.activity.getString(
						R.string.startup_list_summary, startupList.getName()));
				startupListPreference.setEnabled(true);
			}
			startupListPreference
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(
								final Preference preference,
								final Object newValue) {
							startupListPreference
									.setSummary(PreferencesAppHelper.this.activity
											.getString(
													R.string.startup_list_summary,
													ListMirakel.getList(Integer
															.parseInt((String) newValue))));
							return true;
						}
					});
		}

		// Change Sync-Interval
		// FIXME move to accountsettings
		// final ListPreference syncInterval = (ListPreference)
		// findPreference("syncFrequency");
		// if (MirakelPreferences.getSyncFrequency() == -1) {
		// syncInterval.setSummary(R.string.sync_frequency_summary_man);
		// } else {
		// syncInterval.setSummary(activity.getString(
		// R.string.sync_frequency_summary,
		// MirakelPreferences.getSyncFrequency()));
		// }
		// syncInterval
		// .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
		//
		// @Override
		// public boolean onPreferenceChange(
		// Preference preference, Object newValue) {
		// Log.e(TAG, "" + newValue.toString());
		// final Bundle bundle = new Bundle();
		// long longVal = Long.parseLong(newValue.toString());
		// if (longVal == -1) {
		// syncInterval
		// .setSummary(R.string.sync_frequency_summary_man);
		// } else {
		// syncInterval.setSummary(activity.getString(
		// R.string.sync_frequency_summary,
		// longVal));
		// }
		// if (account != null) {
		// ContentResolver.removePeriodicSync(account,
		// Mirakel.AUTHORITY_TYP, bundle);
		// if (longVal != -1) {
		// ContentResolver.setSyncAutomatically(
		// account, Mirakel.AUTHORITY_TYP,
		// true);
		// ContentResolver.setIsSyncable(account,
		// Mirakel.AUTHORITY_TYP, 1);
		// // ContentResolver.setMasterSyncAutomatically(true);
		// ContentResolver.addPeriodicSync(account,
		// Mirakel.AUTHORITY_TYP, bundle,
		// longVal * 60);
		// }
		// } else {
		// Log.d(TAG, "account does not exsist");
		// }
		// return true;
		// }
		// });

		final CheckBoxPreference notificationsUse = (CheckBoxPreference) findPreference("notificationsUse");
		if (notificationsUse != null) {
			notificationsUse
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(
								final Preference preference,
								final Object newValue) {
							if ((Boolean) newValue) {
								PreferencesAppHelper.this.activity
										.startService(new Intent(
												PreferencesAppHelper.this.activity,
												NotificationService.class));
							} else {
								if (PreferencesAppHelper.this.activity
										.startService(new Intent(
												PreferencesAppHelper.this.activity,
												NotificationService.class)) != null) {
									PreferencesAppHelper.this.activity
											.stopService(new Intent(
													PreferencesAppHelper.this.activity,
													NotificationService.class));
								}
							}
							final Editor e = preference.getEditor();
							e.putBoolean("notificationsUse", (Boolean) newValue);
							e.commit();
							NotificationService
									.updateNotificationAndWidget(PreferencesAppHelper.this.activity);
							return true;
						}
					});
		}
		final String[] settings = { "notificationsPersistent",
				"notificationsZeroHide", "notificationsBig" };
		for (final String key : settings) {
			final CheckBoxPreference notifSetting = (CheckBoxPreference) findPreference(key);
			if (notifSetting != null) {
				notifSetting
						.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

							@Override
							public boolean onPreferenceChange(
									final Preference preference,
									final Object newValue) {
								final Editor e = preference.getEditor();
								e.putBoolean(key, (Boolean) newValue);
								e.commit();
								NotificationService
										.updateNotificationAndWidget(PreferencesAppHelper.this.activity);
								return true;
							}
						});

			}
		}

		final CheckBoxPreference remindersPersistent = (CheckBoxPreference) findPreference("remindersPersistent");
		if (remindersPersistent != null) {
			remindersPersistent
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(
								final Preference preference,
								final Object newValue) {
							final Editor e = preference.getEditor();
							e.putBoolean("remindersPersistent",
									(Boolean) newValue);
							e.commit();
							ReminderAlarm
									.stopAll(PreferencesAppHelper.this.activity);
							ReminderAlarm
									.updateAlarms(PreferencesAppHelper.this.activity);
							return true;
						}
					});

		}

		final Intent startSpecialListsIntent = new Intent(this.activity,
				SpecialListsSettingsActivity.class);
		final Preference specialLists = findPreference("special_lists");
		if (specialLists != null) {
			specialLists.setIntent(startSpecialListsIntent);
		}

		final Preference backup = findPreference("backup");
		if (backup != null) {

			backup.setSummary(this.activity.getString(
					R.string.backup_click_summary, FileUtils.getExportDir()
							.getAbsolutePath()));

			backup.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				@SuppressLint("NewApi")
				public boolean onPreferenceClick(final Preference preference) {
					ExportImport.exportDB(PreferencesAppHelper.this.activity);
					return true;
				}
			});
		}

		final ListPreference isTablet = (ListPreference) findPreference("useTabletLayoutNew");
		if (isTablet != null) {
			final String[] values = { "0", "1", "2", "3" };
			final String[] e = this.activity.getResources().getStringArray(
					R.array.tablet_options);
			isTablet.setEntries(e);
			isTablet.setEntryValues(values);
			isTablet.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(final Preference preference,
						final Object newValue) {
					final int value = Integer.parseInt(newValue.toString());
					isTablet.setSummary(e[value]);
					return true;
				}
			});
		}

		final Preference importDB = findPreference("import");
		if (importDB != null) {
			importDB.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(final Preference preference) {
					Helpers.showFileChooser(SettingsActivity.FILE_IMPORT_DB,
							PreferencesAppHelper.this.activity
									.getString(R.string.import_title),
							PreferencesAppHelper.this.activity);
					return true;
				}
			});
		}

		final Preference changelog = findPreference("changelog");
		if (changelog != null) {
			changelog
					.setOnPreferenceClickListener(new OnPreferenceClickListener() {
						@Override
						@SuppressLint("NewApi")
						public boolean onPreferenceClick(
								final Preference preference) {

							final Changelog cl = new Changelog(
									PreferencesAppHelper.this.activity);
							cl.showChangelog(Changelog.NO_VERSION);
							return true;
						}
					});
		}
		final Preference anyDo = findPreference("import_any_do");
		if (anyDo != null) {
			anyDo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(final Preference preference) {
					AnyDoImport
							.handleImportAnyDo(PreferencesAppHelper.this.activity);
					return true;
				}
			});
		}
		final Preference importAstrid = findPreference("import_astrid");
		if (importAstrid != null) {
			importAstrid
					.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(
								final Preference preference) {
							Helpers.showFileChooser(
									SettingsActivity.FILE_ASTRID,
									PreferencesAppHelper.this.activity
											.getString(R.string.astrid_import_title),
									PreferencesAppHelper.this.activity);
							return true;
						}
					});
		}
		final Preference importWunderlist = findPreference("import_wunderlist");
		if (importWunderlist != null) {
			importWunderlist
					.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(
								final Preference preference) {
							new AlertDialog.Builder(
									PreferencesAppHelper.this.activity)
									.setTitle(R.string.import_wunderlist_howto)
									.setMessage(
											R.string.import_wunderlist_howto_text)
									.setPositiveButton(android.R.string.ok,
											new OnClickListener() {

												@Override
												public void onClick(
														final DialogInterface dialog,
														final int which) {

													Helpers.showFileChooser(
															SettingsActivity.FILE_WUNDERLIST,
															PreferencesAppHelper.this.activity
																	.getString(R.string.import_wunderlist_title),
															PreferencesAppHelper.this.activity);

												}
											}).show();
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
								final Preference preference,
								final Object newValue) {
							if ((Boolean) newValue) {
								final AlertDialog.Builder builder = new AlertDialog.Builder(
										PreferencesAppHelper.this.activity);
								builder.setTitle(R.string.kill_sure);
								builder.setMessage(R.string.kill_sure_message)
										.setPositiveButton(
												android.R.string.yes,
												new DialogInterface.OnClickListener() {
													@Override
													public void onClick(
															final DialogInterface dialog,
															final int which) {
													}
												})
										.setNegativeButton(
												android.R.string.cancel,
												new OnClickListener() {
													@Override
													public void onClick(
															final DialogInterface dialog,
															final int which) {
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
			importFileType.setSummary(MirakelCommonPreferences
					.getImportFileTitle());
		}
		final CheckBoxPreference importDefaultList = (CheckBoxPreference) findPreference("importDefaultList");
		if (importDefaultList != null) {
			final ListMirakel list = MirakelModelPreferences
					.getImportDefaultList(false);
			if (list != null) {
				importDefaultList.setSummary(this.activity.getString(
						R.string.import_default_list_summary, list.getName()));
			} else {
				importDefaultList
						.setSummary(R.string.import_no_default_list_summary);
			}

			importDefaultList
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(
								final Preference preference,
								final Object newValue) {
							if ((Boolean) newValue) {
								final AlertDialog.Builder builder = new AlertDialog.Builder(
										PreferencesAppHelper.this.activity);
								builder.setTitle(R.string.import_to);
								final List<CharSequence> items = new ArrayList<CharSequence>();
								final List<Integer> list_ids = new ArrayList<Integer>();
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
														.setSummary(PreferencesAppHelper.this.activity
																.getString(
																		R.string.import_default_list_summary,
																		items.get(item)));
												final SharedPreferences.Editor editor = MirakelPreferences
														.getEditor();
												editor.putInt(
														"defaultImportList",
														list_ids.get(item));
												editor.commit();
												dialog.dismiss();
											}
										});
								builder.setOnCancelListener(new OnCancelListener() {
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
		}

		// Delete done tasks
		final Preference deleteDone = findPreference("deleteDone");
		if (deleteDone != null) {
			deleteDone
					.setOnPreferenceClickListener(new OnPreferenceClickListener() {

						@Override
						public boolean onPreferenceClick(
								final Preference preference) {
							new AlertDialog.Builder(
									PreferencesAppHelper.this.activity)
									.setTitle(R.string.delete_done_warning)
									.setMessage(
											R.string.delete_done_warning_message)
									.setPositiveButton(android.R.string.ok,
											new OnClickListener() {
												@Override
												public void onClick(
														final DialogInterface dialogInterface,
														final int i) {
													Task.deleteDoneTasks();
													Toast.makeText(
															PreferencesAppHelper.this.activity,
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
														final DialogInterface dialogInterface,
														final int i) {
												}
											}).show();
							return true;
						}
					});
		}
		final Preference autoBackupIntervall = findPreference("autoBackupIntervall");
		if (autoBackupIntervall != null) {
			autoBackupIntervall.setSummary(this.activity.getString(
					R.string.auto_backup_intervall_summary,
					MirakelCommonPreferences.getAutoBackupIntervall()));
			autoBackupIntervall
					.setOnPreferenceClickListener(new OnPreferenceClickListener() {

						@Override
						public boolean onPreferenceClick(
								final Preference preference) {

							final int old_val = MirakelCommonPreferences
									.getAutoBackupIntervall();
							final int max = 31;
							final int min = 0;
							if (PreferencesAppHelper.this.v4_0) {
								numberPicker = new NumberPicker(
										PreferencesAppHelper.this.activity);
								((NumberPicker) numberPicker).setMaxValue(max);
								((NumberPicker) numberPicker).setMinValue(min);
								((NumberPicker) numberPicker)
										.setWrapSelectorWheel(false);
								((NumberPicker) numberPicker).setValue(old_val);
								((NumberPicker) numberPicker)
										.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
							} else {
								numberPicker = ((LayoutInflater) PreferencesAppHelper.this.activity
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
											public void onClick(final View v) {
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
											public void onClick(final View v) {
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
							new AlertDialog.Builder(
									PreferencesAppHelper.this.activity)
									.setTitle(R.string.auto_backup_intervall)
									.setView(numberPicker)
									.setPositiveButton(
											android.R.string.ok,
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(
														final DialogInterface dialog,
														final int whichButton) {
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
													MirakelCommonPreferences
															.setAutoBackupIntervall(val);
													autoBackupIntervall
															.setSummary(PreferencesAppHelper.this.activity
																	.getString(
																			R.string.auto_backup_intervall_summary,
																			val));
												}
											})
									.setNegativeButton(
											android.R.string.cancel,
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(
														final DialogInterface dialog,
														final int whichButton) {
													// Do nothing.
												}
											}).show();
							return false;
						}
					});
		}
		final Preference undoNumber = findPreference("UndoNumber");
		if (undoNumber != null) {
			undoNumber.setSummary(this.activity.getString(
					R.string.undo_number_summary,
					MirakelCommonPreferences.getUndoNumber()));
			undoNumber
					.setOnPreferenceClickListener(new OnPreferenceClickListener() {

						@Override
						public boolean onPreferenceClick(
								final Preference preference) {
							final int old_val = MirakelCommonPreferences
									.getUndoNumber();
							final int max = 25;
							final int min = 1;
							if (PreferencesAppHelper.this.v4_0) {
								numberPicker = new NumberPicker(
										PreferencesAppHelper.this.activity);
								((NumberPicker) numberPicker).setMaxValue(max);
								((NumberPicker) numberPicker).setMinValue(min);
								((NumberPicker) numberPicker)
										.setWrapSelectorWheel(false);
								((NumberPicker) numberPicker).setValue(old_val);
								((NumberPicker) numberPicker)
										.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
							} else {
								numberPicker = ((LayoutInflater) PreferencesAppHelper.this.activity
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
											public void onClick(final View v) {
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
											public void onClick(final View v) {
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
							new AlertDialog.Builder(
									PreferencesAppHelper.this.activity)
									.setTitle(R.string.undo_number)
									.setMessage(
											PreferencesAppHelper.this.activity
													.getString(
															R.string.undo_number_summary,
															MirakelCommonPreferences
																	.getUndoNumber()))
									.setView(numberPicker)
									.setPositiveButton(
											android.R.string.ok,
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(
														final DialogInterface dialog,
														final int whichButton) {
													final SharedPreferences.Editor editor = MirakelPreferences
															.getEditor();
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
															.setSummary(PreferencesAppHelper.this.activity
																	.getString(
																			R.string.undo_number_summary,
																			val));
													if (old_val > val) {
														for (int i = val; i < max; i++) {
															editor.putString(
																	UndoHistory.UNDO
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
												@Override
												public void onClick(
														final DialogInterface dialog,
														final int whichButton) {
													// Do nothing.
												}
											}).show();
							return true;
						}
					});
		}

		final Preference dashclock = findPreference("dashclock");
		if (dashclock != null) {
			final Intent startdashclockIntent = new Intent(
					Intent.ACTION_VIEW,
					Uri.parse("http://play.google.com/store/apps/details?id=de.azapps.mirakel.dashclock"));
			dashclock.setIntent(startdashclockIntent);
		}
		final Preference credits = findPreference("credits");
		if (credits != null) {
			final Intent startCreditsIntent = new Intent(this.activity,
					CreditsActivity.class);
			credits.setIntent(startCreditsIntent);
		}

		final Preference contact = findPreference("contact");
		if (contact != null) {
			contact.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(final Preference preference) {
					Helpers.contact(PreferencesAppHelper.this.activity);
					return false;
				}
			});
		}

		final Intent startSemanticsIntent = new Intent(this.activity,
				SemanticsSettingsActivity.class);
		final Preference semantics = findPreference("semanticNewTaskSettings");
		if (semantics != null) {
			semantics.setIntent(startSemanticsIntent);
		}

		final Intent startRecurringIntent = new Intent(this.activity,
				RecurringActivity.class);
		final Preference recurring = findPreference("recurring");
		if (recurring != null) {
			recurring.setIntent(startRecurringIntent);
		}
		// Intent startTaskFragmentIntent = new Intent(activity,
		// TaskFragmentSettings.class);
		final Preference taskFragment = findPreference("task_fragment");
		if (taskFragment != null) {
			// taskFragment.setIntent(startTaskFragmentIntent);
			taskFragment
					.setOnPreferenceClickListener(new OnPreferenceClickListener() {

						@Override
						public boolean onPreferenceClick(
								final Preference preference) {
							if (PreferencesAppHelper.this.v4_0) {
								((SettingsFragment) PreferencesAppHelper.this.ctx)
										.showTaskFragmentSettings();
							}
							return false;
						}
					});
		}

		final CheckBoxPreference subTaskAddToSameList = (CheckBoxPreference) findPreference("subtaskAddToSameList");
		if (subTaskAddToSameList != null) {
			if (!MirakelCommonPreferences.addSubtaskToSameList()) {
				subTaskAddToSameList.setSummary(this.activity.getString(
						R.string.settings_subtask_add_to_list_summary,
						MirakelModelPreferences.subtaskAddToList().getName()));
			} else {
				subTaskAddToSameList
						.setSummary(R.string.settings_subtask_add_to_same_list_summary);
			}

			subTaskAddToSameList
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(
								final Preference preference,
								final Object newValue) {
							if (!(Boolean) newValue) {
								final AlertDialog.Builder builder = new AlertDialog.Builder(
										PreferencesAppHelper.this.activity);
								builder.setTitle(R.string.import_to);
								final List<CharSequence> items = new ArrayList<CharSequence>();
								final List<Integer> list_ids = new ArrayList<Integer>();
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
												subTaskAddToSameList
														.setSummary(items
																.get(item));
												subTaskAddToSameList
														.setSummary(PreferencesAppHelper.this.activity
																.getString(
																		R.string.settings_subtask_add_to_list_summary,
																		items.get(item)));
												final SharedPreferences.Editor editor = MirakelPreferences
														.getEditor();
												editor.putInt(
														"subtaskAddToList",
														list_ids.get(item));
												editor.commit();
												dialog.dismiss();
											}
										});
								builder.setOnCancelListener(new OnCancelListener() {
									@Override
									public void onCancel(
											final DialogInterface dialog) {
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
		final ListPreference language = (ListPreference) findPreference("language");
		if (language != null) {
			setLanguageSummary(language, MirakelCommonPreferences.getLanguage());
			language.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(final Preference preference,
						final Object newValue) {
					setLanguageSummary(language, newValue.toString());
					MirakelPreferences.getEditor()
							.putString("language", newValue.toString())
							.commit();
					Helpers.restartApp(PreferencesAppHelper.this.activity);
					return false;
				}
			});

		}

		final CheckBoxPreference useTabletLayout = (CheckBoxPreference) findPreference("useTabletLayout");
		if (useTabletLayout != null) {
			useTabletLayout
					.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(
								final Preference preference,
								final Object newValue) {
							MirakelPreferences
									.getEditor()
									.putBoolean("useTabletLayout",
											(Boolean) newValue).commit();
							Helpers.restartApp(PreferencesAppHelper.this.activity);
							return false;
						}
					});
		}
		final CheckBoxPreference demoMode = (CheckBoxPreference) findPreference("demoMode");
		if (demoMode != null) {
			demoMode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(final Preference preference,
						final Object newValue) {
					MirakelCommonPreferences.setDemoMode((Boolean) newValue);
					Helpers.restartApp(PreferencesAppHelper.this.activity);
					return false;
				}
			});
		}

		final Preference version = findPreference("version");
		if (version != null) {
			version.setSummary(DefinitionsHelper.VERSIONS_NAME);
			version.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				private Toast toast;

				@Override
				public boolean onPreferenceClick(final Preference preference) {
					if (++PreferencesAppHelper.this.debugCounter > 6) {
						MirakelCommonPreferences.toogleDebugMenu();
						PreferencesAppHelper.this.debugCounter = 0;
						if (this.toast != null) {
							this.toast.cancel();
						}
						this.toast = Toast
								.makeText(
										PreferencesAppHelper.this.activity,
										PreferencesAppHelper.this.activity
												.getString(
														R.string.change_dev_mode,
														PreferencesAppHelper.this.activity
																.getString(MirakelCommonPreferences
																		.isEnabledDebugMenu() ? R.string.enabled
																		: R.string.disabled)),
										Toast.LENGTH_LONG);
						this.toast.show();
						((SettingsActivity) PreferencesAppHelper.this.activity)
								.invalidateHeaders();
					} else if ((PreferencesAppHelper.this.debugCounter > 3)
							|| MirakelCommonPreferences.isEnabledDebugMenu()) {
						if (this.toast != null) {
							this.toast.cancel();
						}
						this.toast = Toast
								.makeText(
										PreferencesAppHelper.this.activity,
										PreferencesAppHelper.this.activity
												.getResources()
												.getQuantityString(
														R.plurals.dev_toast,
														7 - PreferencesAppHelper.this.debugCounter,
														7 - PreferencesAppHelper.this.debugCounter,
														PreferencesAppHelper.this.activity
																.getString(!MirakelCommonPreferences
																		.isEnabledDebugMenu() ? R.string.enable
																		: R.string.disable)),
										Toast.LENGTH_SHORT);
						this.toast.show();
					}
					return false;
				}
			});
		}
	}

	private void setLanguageSummary(final ListPreference language,
			final String current) {
		final String[] keys = this.activity.getResources().getStringArray(
				R.array.language_keys);
		language.setSummary(keys[0]);
		for (int j = 0; j < keys.length; j++) {
			if (current.equals(keys[j])) {
				language.setSummary(this.activity.getResources()
						.getStringArray(R.array.language_values)[j]);
				break;
			}
		}
	}

	// The „real“ helper functions

}
