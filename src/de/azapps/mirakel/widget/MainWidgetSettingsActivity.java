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
package de.azapps.mirakel.widget;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.FrameLayout;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.PreferencesHelper;
import de.azapps.mirakelandroid.R;

public class MainWidgetSettingsActivity extends PreferenceActivity {
	@SuppressWarnings("unused")
	private static final String TAG = "MainWidgetSettingsActivity";

	private static int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				"DarkTheme", false))
			setTheme(R.style.AppBaseThemeDARK);
		super.onCreate(savedInstanceState);
		mAppWidgetId = getIntent().getIntExtra(
				MainWidgetProvider.EXTRA_WIDGET_ID, 0);
		if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
			addPreferencesFromResource(R.xml.settings_widget);
			new PreferencesHelper(this).setFunctionsWidget(this, mAppWidgetId);
		} else {
			// Display the fragment as the main content.
			((FrameLayout) findViewById(android.R.id.content)).removeAllViews();
			MainWidgetSettingsFragment fragment = new MainWidgetSettingsFragment();
			getFragmentManager().beginTransaction()
					.replace(android.R.id.content, fragment).commit();
			fragment.setup(mAppWidgetId);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.e("WIDGET", "updated");
		Intent intent = new Intent(this, MainWidgetProvider.class);
		intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		// Use an array and EXTRA_APPWIDGET_IDS instead of
		// AppWidgetManager.EXTRA_APPWIDGET_ID,
		// since it seems the onUpdate() is only fired on that:
		int widgets[] = { mAppWidgetId };
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgets);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB)
			AppWidgetManager.getInstance(this).notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.widget_tasks_list);
		sendBroadcast(intent);
		// Finish this activity
		finish();
	}

	@Override
	public void onBackPressed() {
		/*
		 * Show Homescreen
		 */
		Intent startMain = new Intent(Intent.ACTION_MAIN);
		startMain.addCategory(Intent.CATEGORY_HOME);
		startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(startMain);
	}

}
