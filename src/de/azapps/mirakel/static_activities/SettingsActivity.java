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

import java.net.URISyntaxException;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.widget.Toast;
import de.azapps.mirakel.R;
import de.azapps.mirakel.helper.ExportImport;
import de.azapps.mirakel.helper.FileUtils;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.PreferencesHelper;

public class SettingsActivity extends PreferenceActivity {

	public static final int FILE_ASTRID = 0;
	private static final String TAG = "SettingsActivity";

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			// Display the fragment as the main content.
			getFragmentManager().beginTransaction()
					.replace(android.R.id.content, new SettingsFragment())
					.commit();
			getActionBar().setDisplayHomeAsUpEnabled(true);
		} else {
			addPreferencesFromResource(R.xml.preferences);
			new PreferencesHelper(this).setFunctionsApp();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case FILE_ASTRID:
			if (resultCode == RESULT_OK) {
				// Get the Uri of the selected file
				Uri uri = data.getData();
				Log.d(TAG, "File Uri: " + uri.toString());
				try {
					final String path = FileUtils.getPath(this, uri);
					final Context that = this;

					// Do the import in a background-task
					new AsyncTask<String, Void, Void>() {
						ProgressDialog dialog;

						@Override
						protected Void doInBackground(String... params) {
							ExportImport.importAstrid(that, path);
							return null;
						}

						@Override
						protected void onPostExecute(Void v) {
							dialog.dismiss();
							Toast.makeText(that, R.string.astrid_success,
									Toast.LENGTH_SHORT).show();
							android.os.Process.killProcess(android.os.Process
									.myPid()); // ugly but simple
						}

						@Override
						protected void onPreExecute() {
							dialog = ProgressDialog.show(that,
									that.getString(R.string.astrid_importing),
									that.getString(R.string.astrid_wait), true);
						}
					}.execute("");

					Log.d(TAG, "File Path: " + path);
				} catch (URISyntaxException e) {
					Toast.makeText(this, "Something terrible happened…",
							Toast.LENGTH_LONG).show();
				}
				// Get the file instance
				// File file = new File(path);
				// Initiate the upload
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
