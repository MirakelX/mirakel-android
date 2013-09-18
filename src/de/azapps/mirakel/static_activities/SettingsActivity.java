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

import java.io.File;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.helper.ExportImport;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.PreferencesHelper;
import de.azapps.mirakel.special_lists_settings.SpecialListsSettings;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.sync.mirakel.MirakelSync;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSync;
import de.azapps.mirakelandroid.R;

public class SettingsActivity extends PreferenceActivity {

	public static final int FILE_ASTRID = 0, FILE_IMPORT_DB = 1,
			NEW_ACCOUNT = 2;
	private static final String TAG = "SettingsActivity";
	private SettingsFragment fragment;
	private boolean darkTheme;

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		darkTheme = PreferenceManager.getDefaultSharedPreferences(this)
				.getBoolean("DarkTheme", false);
		if (darkTheme)
			setTheme(R.style.AppBaseThemeDARK);
		super.onCreate(savedInstanceState);
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {

			Intent i = getIntent();
			if (i == null) {
				Log.e(TAG, "intent==null");

			} else {
				if (i.getAction() == null) {
					addPreferencesFromResource(R.xml.preferences_v10);
				} else if (i.getAction().equals(
						"de.azapps.mirakel.preferences.NOTIFICATION")) {
					addPreferencesFromResource(R.xml.notification_prefernces);
				} else if (i.getAction().equals(
						"de.azapps.mirakel.preferences.GUI")) {
					addPreferencesFromResource(R.xml.gui_prefernces);
				} else if (i.getAction().equals(
						"de.azapps.mirakel.preferences.ABOUT")) {
					addPreferencesFromResource(R.xml.about_prefernces);
				} else if (i.getAction().equals(
						"de.azapps.mirakel.preferences.MISC")) {
					addPreferencesFromResource(R.xml.misc_prefernces);
				} else if (i.getAction().equals(
						"de.azapps.mirakel.preferences.BACKUP")) {
					addPreferencesFromResource(R.xml.backup_prefernces);
				} else if (i.getAction().equals(
						"de.azapps.mirakel.preferences.SYNC")) {
					addPreferencesFromResource(R.xml.sync_prefernces);
				} else if (i.getAction().equals(
						"de.azapps.mirakel.preferences.SPECIAL_LISTS")) {
					startActivity(new Intent(this, SpecialListsSettings.class));
					if (!getResources().getBoolean(R.bool.isTablet))
						finish();
				} else {
					Log.wtf(TAG, "unkown Preference");
				}
			}
			new PreferencesHelper(this).setFunctionsApp();
		} else {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG, "Menu");
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (darkTheme != PreferenceManager.getDefaultSharedPreferences(this)
				.getBoolean("DarkTheme", false)
				&& !getResources().getBoolean(R.bool.isTablet)) {
			finish();
			startActivity(getIntent());
		}
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		final Context that = this;
		Log.e(TAG, "foo");
		switch (requestCode) {
		case FILE_ASTRID:
			if (resultCode != RESULT_OK)
				return;
			final String path_astrid = Helpers.getPathFromUri(data.getData(),
					this);

			// Do the import in a background-task
			new AsyncTask<String, Void, Boolean>() {
				ProgressDialog dialog;

				@Override
				protected Boolean doInBackground(String... params) {
					return ExportImport.importAstrid(that, path_astrid);
				}

				@Override
				protected void onPostExecute(Boolean success) {
					dialog.dismiss();
					if (!success) {
						Toast.makeText(that, R.string.astrid_unsuccess,
								Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(that, R.string.astrid_success,
								Toast.LENGTH_SHORT).show();
						android.os.Process.killProcess(android.os.Process
								.myPid()); // ugly
											// but
											// simple
					}
				}

				@Override
				protected void onPreExecute() {
					dialog = ProgressDialog.show(that,
							that.getString(R.string.astrid_importing),
							that.getString(R.string.astrid_wait), true);
				}
			}.execute("");

			break;
		case FILE_IMPORT_DB:
			if (resultCode != RESULT_OK)
				return;
			final String path_db = Helpers.getPathFromUri(data.getData(), this);
			// Check if this is an database file
			if (!path_db.endsWith(".db")) {
				Toast.makeText(that, R.string.import_wrong_type,
						Toast.LENGTH_LONG).show();
				return;
			}
			new AlertDialog.Builder(this)
					.setTitle(R.string.import_sure)
					.setMessage(
							this.getString(R.string.import_sure_summary,
									path_db))
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
									ExportImport.importDB(that, new File(
											path_db));
								}
							}).create().show();
		case NEW_ACCOUNT:
			CheckBoxPreference sync;
			Preference server;
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				sync = (CheckBoxPreference) findPreference("syncUse");
				server = findPreference("syncServer");
			} else {
				sync = (CheckBoxPreference) fragment.findPreference("syncUse");
				server = fragment.findPreference("syncServer");
			}

			AccountManager am = AccountManager.get(this);
			Account[] accounts = am.getAccountsByType(Mirakel.ACCOUNT_TYPE);
			if (accounts.length > 0) {
				if (am.getUserData(accounts[0], SyncAdapter.BUNDLE_SERVER_TYPE)
						.equals(TaskWarriorSync.TYPE)) {
					sync.setSummary(getString(
							R.string.sync_use_summary_taskwarrior,
							accounts[0].name));
				} else if (am.getUserData(accounts[0],
						SyncAdapter.BUNDLE_SERVER_TYPE)
						.equals(MirakelSync.TYPE)) {
					sync.setSummary(getString(
							R.string.sync_use_summary_mirakel, accounts[0].name));
				} else {
					sync.setChecked(false);
					sync.setSummary(R.string.sync_use_summary_nothing);
					am.removeAccount(accounts[0], null, null);
				}
				server.setSummary(getString(R.string.sync_server_summary,
						am.getUserData(accounts[0],
								SyncAdapter.BUNDLE_SERVER_URL)));
			} else {
				sync.setChecked(false);
				sync.setSummary(R.string.sync_use_summary_nothing);
				server.setSummary("");
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preferences, target);
	}
}
