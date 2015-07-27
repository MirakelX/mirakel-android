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
package de.azapps.mirakel.widget;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import de.azapps.mirakel.settings.custom_views.Settings;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelDetailActivity;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelDetailFragment;
import de.azapps.tools.Log;

public class MainWidgetSettingsActivity extends GenericModelDetailActivity {
    private static int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mAppWidgetId = getIntent().getIntExtra(
                           MainWidgetProvider.EXTRA_WIDGET_ID, 0);
        Intent i = getIntent();
        i.putExtra(FRAGMENT, MainWidgetSettingsFragment.class);
        i.putExtra(GenericModelDetailFragment.ARG_ITEM, (Parcelable) Settings.WIDGET);
        setIntent(i);
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        if (!backstack.isEmpty()) {
            ((MainWidgetSettingsFragment)backstack.get(0)).setup(mAppWidgetId);
        }

    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("WIDGET", "updated");
        final Intent intent = new Intent(this, MainWidgetProvider.class);
        intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        // Use an array and EXTRA_APPWIDGET_IDS instead of
        // AppWidgetManager.EXTRA_APPWIDGET_ID,
        // since it seems the onUpdate() is only fired on that:
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { mAppWidgetId });
        AppWidgetManager.getInstance(this).notifyAppWidgetViewDataChanged(
            mAppWidgetId, R.id.widget_tasks_list);
        sendBroadcast(intent);
        // Finish this activity
        finish();
    }

    @Override
    public void onBackPressed() {
        /*
         * Show Homescreen
         */
        final Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

}
