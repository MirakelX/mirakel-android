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

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import de.azapps.mirakel.helper.WidgetHelper;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class MainWidgetProvider extends AppWidgetProvider {
	private static final String TAG = "MainWidgetProvider";
	public static final String EXTRA_LISTID = "de.azapps.mirakel.EXTRA_LISTID",
			EXTRA_LISTSORT = "de.azapps.mirakel.EXTRA_LISTSORT",
			EXTRA_SHOWDONE = "de.azapps.mirakel.EXTRA_SHOWDONE",
			CLICK_TASK = "de.azapps.mirakel.CLICK_TASK",
			EXTRA_TASKID = "de.azapps.mirakel.EXTRA_TASKID",
			EXTRA_WIDGET_LAYOUT = "de.azapps.mirakel.EXTRA_WIDGET_LAYOUT",
			EXTRA_WIDGET_ID = "de.azapps.mirakel.EXTRA_WIDGET_ID";

	public static final int MINIMAL_WIDGET = 1;
	public static final int NORMAL_WIDGET = 0;
	private static final boolean oldAPI = VERSION.SDK_INT < VERSION_CODES.HONEYCOMB;

	@SuppressLint("NewApi")
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		for (int widgetId : appWidgetIds) {
			Log.v(TAG, "update Widget: " + widgetId);
			RemoteViews views = new RemoteViews(context.getPackageName(),
					oldAPI ? R.layout.widget_main_layout_v10
							: R.layout.widget_main);
			boolean isDark = WidgetHelper.isDark(context, widgetId);
			boolean isMinimalistic = WidgetHelper.isMinimalistic(context,
					widgetId);
			if (!isMinimalistic) {
				views.setInt(R.id.widget_main, "setBackgroundResource",
						isDark ? R.drawable.widget_background_dark
								: R.drawable.widget_background);
				views.setTextColor(
						R.id.widget_list_name,
						context.getResources().getColor(
								isDark ? R.color.White : R.color.Black));
			}
			ListMirakel list = WidgetHelper.getList(context, widgetId);
			if (list == null)
				continue;

			// Create an Intent to launch SettingsActivity
			Intent settingsIntent = new Intent(context,
					MainWidgetSettingsActivity.class);
			settingsIntent.putExtra(EXTRA_WIDGET_ID, widgetId);
			settingsIntent.setData(Uri.parse(settingsIntent
					.toUri(Intent.URI_INTENT_SCHEME)));
			PendingIntent settingsPendingIntent = PendingIntent.getActivity(
					context, 0, settingsIntent, 0);
			views.setOnClickPendingIntent(R.id.widget_preferences,
					settingsPendingIntent);

			// Create an Intent to launch MainActivity and show the List
			Intent mainIntent = new Intent(context, MainActivity.class);
			mainIntent.setAction(MainActivity.SHOW_LIST_FROM_WIDGET);
			mainIntent.putExtra(MainActivity.EXTRA_ID, list.getId());
			PendingIntent mainPendingIntent = PendingIntent.getActivity(
					context, 0, mainIntent, 0);
			views.setOnClickPendingIntent(R.id.widget_list_name,
					mainPendingIntent);

			// ListName
			views.setTextViewText(R.id.widget_list_name, list.getName());

			// Create an Intent to launch MainActivity and create a new Task
			Intent addIntent = new Intent(context, MainActivity.class);
			addIntent.setAction(MainActivity.ADD_TASK_FROM_WIDGET);
			addIntent.putExtra(MainActivity.EXTRA_ID, list.getId());
			PendingIntent addPendingIntent = PendingIntent.getActivity(context,
					0, addIntent, 0);
			views.setOnClickPendingIntent(R.id.widget_add_task,
					addPendingIntent);
			boolean showDone = WidgetHelper.showDone(context, widgetId);
			if (!oldAPI) {
				Intent intent = new Intent(context, MainWidgetService.class);
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
				intent.putExtra(EXTRA_LISTID, list.getId());
				intent.putExtra(EXTRA_SHOWDONE, showDone);
				intent.putExtra(EXTRA_WIDGET_ID, widgetId);
				intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
				views.setRemoteAdapter(R.id.widget_tasks_list, intent);

				// Empty view
				views.setEmptyView(R.id.widget_tasks_list, R.id.empty_view);

				// Main Intent
				Intent toastIntent = new Intent(context,
						MainWidgetProvider.class);
				intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
				intent.putExtra(EXTRA_WIDGET_LAYOUT, NORMAL_WIDGET);
				PendingIntent toastPendingIntent = PendingIntent.getBroadcast(
						context, 0, toastIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);
				views.setPendingIntentTemplate(R.id.widget_tasks_list,
						toastPendingIntent);

			} else {
				views.removeAllViews(R.id.widget_main_view);
				List<Task> tasks = Task.getTasks(list.getId(),
						list.getSortBy(), showDone);
				if (tasks.size() == 0) {
					views.setViewVisibility(R.id.empty_view, View.VISIBLE);
				} else {
					views.setViewVisibility(R.id.empty_view, View.GONE);
					int end = tasks.size() >= 7 ? 7 : tasks.size();
					try {
						int row_id = isMinimalistic ? R.layout.widget_row_minimal
								: R.layout.widget_row;
						for (Task t : tasks.subList(0, end)) {
							views.addView(R.id.widget_main_view, WidgetHelper
									.configureItem(
											new RemoteViews(context
													.getPackageName(), row_id),
											t, context, list.getId(), false));
						}
					} catch (IndexOutOfBoundsException e) {
						Log.wtf(TAG,
								"The list has been shortened while processing it…");
					}
				}
			}
			Log.e("Blubb", list.getName() + widgetId);
			appWidgetManager.updateAppWidget(widgetId, views);

		}

	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(CLICK_TASK)) {
			int taskId = intent.getIntExtra(EXTRA_TASKID, 0);
			Log.e("Blubb", "click on: " + Task.get(taskId).getName());
			Intent startMainIntent = new Intent(context, MainActivity.class);
			startMainIntent.setAction(MainActivity.SHOW_TASK);
			startMainIntent.putExtra(MainActivity.EXTRA_ID, taskId);
			startMainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startMainIntent.setData(Uri.parse(startMainIntent
					.toUri(Intent.URI_INTENT_SCHEME)));
			context.startActivity(startMainIntent);
		}
		super.onReceive(context, intent);
	}

}
