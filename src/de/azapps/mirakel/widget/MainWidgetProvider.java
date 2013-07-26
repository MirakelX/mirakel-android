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

import java.util.List;
import java.util.Random;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakelandroid.R;
import de.azapps.mirakel.helper.WidgetHelper;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;

public class MainWidgetProvider extends AppWidgetProvider {
	@SuppressWarnings("unused")
	private static final String TAG = "MainWidgetProvider";
	public static String EXTRA_LISTID = "de.azapps.mirakel.EXTRA_LISTID";
	public static String EXTRA_LISTSORT = "de.azapps.mirakel.EXTRA_LISTSORT";
	public static String EXTRA_SHOWDONE = "de.azapps.mirakel.EXTRA_SHOWDONE";
	public static String CLICK_TASK = "de.azapps.mirakel.CLICK_TASK";
	public static String EXTRA_TASKID = "de.azapps.mirakel.EXTRA_TASKID";

	@SuppressLint("NewApi")
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		Mirakel.widgets = appWidgetIds;

		// Perform this loop procedure for each App Widget that belongs to this
		// provider
		for (int appWidgetId : appWidgetIds) {
			RemoteViews views = new RemoteViews(
					context.getPackageName(),
					VERSION.SDK_INT < VERSION_CODES.HONEYCOMB ? R.layout.widget_main_layout_v10
							: R.layout.widget_main);

			int listId = Integer.parseInt(preferences.getString("widgetList",
					SpecialList.first().getId() + ""));
			int listSort = Integer.parseInt(preferences.getString("widgetSort",
					ListMirakel.SORT_BY_OPT + ""));

			// Create an Intent to launch SettingsActivity
			Intent settingsIntent = new Intent(context,
					MainWidgetSettingsActivity.class);
			PendingIntent settingsPendingIntent = PendingIntent.getActivity(
					context, 0, settingsIntent, 0);

			views.setOnClickPendingIntent(R.id.widget_preferences,
					settingsPendingIntent);
			// Create an Intent to launch MainActivity and show the List
			Intent mainIntent = new Intent(context, MainActivity.class);
			mainIntent.setAction(MainActivity.SHOW_LIST_FROM_WIDGET);
			mainIntent.putExtra(MainActivity.EXTRA_ID, listId);

			PendingIntent mainPendingIntent = PendingIntent.getActivity(
					context, 0, mainIntent, 0);

			views.setOnClickPendingIntent(R.id.widget_list_name,
					mainPendingIntent);

			views.setTextViewText(R.id.widget_list_name,
					ListMirakel.getList(listId).getName());

			// Create an Intent to launch MainActivity and create a new Task
			Intent addIntent = new Intent(context, MainActivity.class);
			addIntent.setAction(MainActivity.ADD_TASK_FROM_WIDGET);
			addIntent.putExtra(MainActivity.EXTRA_ID, listId);

			PendingIntent addPendingIntent = PendingIntent.getActivity(context,
					0, addIntent, 0);

			views.setOnClickPendingIntent(R.id.widget_add_task,
					addPendingIntent);
			if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
				// Here we setup the intent which points to the StackViewService
				// which will
				// provide the views for this collection.
				Intent intent = new Intent(context, MainWidgetService.class);
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
						appWidgetId);

				// When intents are compared, the extras are ignored, so we need
				// to
				// embed the extras
				// into the data so that the extras will not be ignored.
				intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
				intent.putExtra(EXTRA_LISTID, listId);
				intent.putExtra(EXTRA_LISTSORT, listSort);
				intent.putExtra(EXTRA_SHOWDONE,
						preferences.getBoolean("widgetDone", false));
				intent.putExtra("Random", new Random().nextInt());
				views.setRemoteAdapter(R.id.widget_tasks_list, intent);

				// The empty view is displayed when the collection has no items.
				// It
				// should be a sibling
				// of the collection view.
				views.setEmptyView(R.id.widget_tasks_list, R.id.empty_view);
				// Here we setup the a pending intent template. Individuals
				// items of
				// a collection
				// cannot setup their own pending intents, instead, the
				// collection
				// as a whole can
				// setup a pending intent template, and the individual items can
				// set
				// a fillInIntent
				// to create unique before on an item to item basis.
				Intent toastIntent = new Intent(context,
						MainWidgetProvider.class);
				intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
				PendingIntent toastPendingIntent = PendingIntent.getBroadcast(
						context, 0, toastIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);
				views.setPendingIntentTemplate(R.id.widget_tasks_list,
						toastPendingIntent);

			} else {
				views.removeAllViews(R.id.widget_main_view);
				List<Task> tasks = Task.getTasks(listId, listSort, false);
				if (tasks.size() == 0) {
					views.setViewVisibility(R.id.empty_view, View.VISIBLE);
				} else {
					views.setViewVisibility(R.id.empty_view, View.GONE);
					int end = tasks.size()>=7 ? 7 : tasks.size();
					try {
						for (Task t : tasks.subList(0, end)) {
							views.addView(R.id.widget_main_view, WidgetHelper
									.configureItem(
											new RemoteViews(context
													.getPackageName(),
													R.layout.widget_row), t,
											context, listId));
						}
					} catch (IndexOutOfBoundsException e) {
						Log.wtf(TAG, "The list has been shortened while processing it…");
					}
				}
			}
			appWidgetManager.updateAppWidget(appWidgetId, views);

		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(CLICK_TASK)) {
			int taskId = intent.getIntExtra(EXTRA_TASKID, 0);
			Intent startMainIntent = new Intent(context, MainActivity.class);
			startMainIntent.setAction(MainActivity.SHOW_TASK);
			startMainIntent.putExtra(MainActivity.EXTRA_ID, taskId);
			startMainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(startMainIntent);
		}
		super.onReceive(context, intent);
	}

}
