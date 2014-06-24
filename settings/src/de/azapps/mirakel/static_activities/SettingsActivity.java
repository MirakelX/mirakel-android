/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists Copyright (c) 2013-2014 Anatolij Zelenin, Georg
 * Semmler. This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or any later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.static_activities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.Toast;
import de.azapps.mirakel.adapter.SettingsAdapter;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.PreferencesAppHelper;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.helper.export_import.AnyDoImport;
import de.azapps.mirakel.helper.export_import.ExportImport;
import de.azapps.mirakel.helper.export_import.WunderlistImport;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.accounts.AccountSettingsActivity;
import de.azapps.mirakel.settings.special_list.SpecialListsSettingsActivity;
import de.azapps.mirakel.settings.tags.TagsSettingsActivity;
import de.azapps.mirakel.settings.taskfragment.TaskFragmentSettingsFragment;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

public class SettingsActivity extends PreferenceActivity {

	public static final int DONATE = 5;
	public static final int FILE_ASTRID = 0, FILE_IMPORT_DB = 1,
			NEW_ACCOUNT = 2, FILE_ANY_DO = 3, FILE_WUNDERLIST = 4;
	private static final String TAG = "SettingsActivity";
	private boolean darkTheme;
	private boolean isTablet;
	private SettingsAdapter mAdapter;
	private List<Header> mHeaders;

	@SuppressLint("NewApi")
	@Override
	public void invalidateHeaders() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			super.invalidateHeaders();
		}
	}

	@Override
	protected boolean isValidFragment(final String fragmentName) {
		return fragmentName.equals(SettingsFragment.class.getCanonicalName())
				|| fragmentName.equals(TaskFragmentSettingsFragment.class
						.getCanonicalName());
	}

	private FileInputStream stream;

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
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
							new OnClickListener() {

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
		case NEW_ACCOUNT:
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				PreferencesAppHelper.updateSyncText(
						(CheckBoxPreference) findPreference("syncUse"),
						findPreference("syncServer"),
						findPreference("syncFrequency"), this);
			}
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
						return AnyDoImport.exec(SettingsActivity.this,
								SettingsActivity.this.stream);
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
						android.os.Process.killProcess(android.os.Process
								.myPid());
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
			}.execute("");
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

	@SuppressLint("NewApi")
	@Override
	public void onBuildHeaders(final List<Header> target) {
		loadHeadersFromResource(R.xml.settings, target);
		if (!MirakelCommonPreferences.isEnabledDebugMenu()) {
			for (int i = 0; i < target.size(); i++) {
				final Header h = target.get(i);
				if (h.id == R.id.header_dev) {
					target.remove(i);
					break;
				}
			}
		}
		this.mHeaders = target;
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		Locale.setDefault(Helpers.getLocal(this));
		super.onConfigurationChanged(newConfig);
		if (this.isTablet != MirakelCommonPreferences.isTablet()) {
			onCreate(null);
		}
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		this.darkTheme = MirakelCommonPreferences.isDark();
		if (this.darkTheme) {
			setTheme(R.style.AppBaseThemeDARK);
		}
		super.onCreate(savedInstanceState);
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {

			final Intent i = getIntent();
			if (i == null) {
				Log.e(TAG, "intent==null");

			} else {
				if (i.getAction() == null) {
					addPreferencesFromResource(R.xml.settings_v10);
				} else {
					switch (i.getAction()) {
					case "de.azapps.mirakel.preferences.NOTIFICATION":
						addPreferencesFromResource(R.xml.settings_notifications);
						break;
					case "de.azapps.mirakel.preferences.GUI":
						addPreferencesFromResource(R.xml.settings_gui);
						break;
					case "de.azapps.mirakel.preferences.TASKS":
						addPreferencesFromResource(R.xml.settings_tasks);
						break;
					case "de.azapps.mirakel.preferences.ABOUT":
						addPreferencesFromResource(R.xml.settings_about);
						break;
					case "de.azapps.mirakel.preferences.HELP":
						Helpers.openHelp(this);
						finish();
						break;
					case "de.azapps.mirakel.preferences.DONATE":
						startActivityForResult(new Intent(this,
								DonationsActivity.class), DONATE);
						if (!MirakelCommonPreferences.isTablet()) {
							finish();
						}
						break;
					case "de.azapps.mirakel.preferences.MISC":
						addPreferencesFromResource(R.xml.settings_misc);
						break;
					case "de.azapps.mirakel.preferences.BACKUP":
						addPreferencesFromResource(R.xml.settings_backup);
						break;
					case "de.azapps.mirakel.preferences.ACCOUNTS":
						startActivity(new Intent(this,
								AccountSettingsActivity.class));
						if (!MirakelCommonPreferences.isTablet()) {
							finish();
						} else {
							addPreferencesFromResource(R.xml.settings_notifications);
						}
						break;
					case "de.azapps.mirakel.preferences.SPECIAL_LISTS":
						startActivity(new Intent(this,
								SpecialListsSettingsActivity.class));
						if (!MirakelCommonPreferences.isTablet()) {
							finish();
						}
						break;
					case "de.azapps.mirakel.preferences.TAG":
						startActivity(new Intent(this,
								TagsSettingsActivity.class));
						if (!MirakelCommonPreferences.isTablet()) {
							finish();
						}
						break;
					case "de.azapps.mirakel.preferences.DEV":
						addPreferencesFromResource(R.xml.settings_dev);
						break;
					default:
						Log.wtf(TAG, "unkown Preference");
					}
				}
			}
			new PreferencesAppHelper(this).setFunctionsApp();
		} else {
			getActionBar().setDisplayHomeAsUpEnabled(true);
			this.isTablet = MirakelCommonPreferences.isTablet();
			invalidateHeaders();
		}
	}

	@Override
	public boolean onIsMultiPane() {
		return MirakelCommonPreferences.isTablet();
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		Log.d(TAG, "Menu");
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (this.darkTheme != MirakelCommonPreferences.isDark()) {
			finish();
			startActivity(getIntent());
		}
		invalidateHeaders();
	}

	@Override
	public void setListAdapter(final ListAdapter adapter) {
		if (this.mHeaders == null) {
			this.mHeaders = new ArrayList<Header>();
			// When the saved state provides the list of headers,
			// onBuildHeaders is not called
			// so we build it from the adapter given, then use our own adapter
			if (adapter != null) {
				for (int i = 0; i < adapter.getCount(); ++i) {
					this.mHeaders.add((Header) adapter.getItem(i));
				}
			}
		}
		this.mAdapter = new SettingsAdapter(this, this.mHeaders);
		super.setListAdapter(this.mAdapter);
	}

}
