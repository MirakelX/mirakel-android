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
package de.azapps.mirakel.widget;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.WidgetHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;

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

	@Override
	@SuppressLint("NewApi")
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		for (int widgetId : appWidgetIds) {
			Log.v(TAG, "update Widget: " + widgetId);
			boolean isDark = WidgetHelper.isDark(context, widgetId);
			boolean isMinimalistic = WidgetHelper.isMinimalistic(context,
					widgetId);
			int layout_id;
			if (isMinimalistic && !oldAPI) {
				layout_id = R.layout.widget_minimal;
			} else {
				isMinimalistic = false;
				layout_id = oldAPI ? R.layout.widget_main_layout_v10
						: R.layout.widget_main;
			}
			RemoteViews views = new RemoteViews(context.getPackageName(),
					layout_id);

			int widgetBackground;
			if (isMinimalistic) {
				widgetBackground = isDark ? R.drawable.widget_background_minimalistic_dark
						: R.drawable.widget_background_minimalistic;
			} else {
				widgetBackground = isDark ? R.drawable.widget_background_dark
						: R.drawable.widget_background;
			}
			if (!oldAPI) {
				GradientDrawable drawable = (GradientDrawable) context
						.getResources().getDrawable(widgetBackground);
				drawable.setAlpha(WidgetHelper.getTransparency(context,
						widgetId));
				Bitmap bitmap = Bitmap.createBitmap(500, 500, Config.ARGB_8888);
				Canvas canvas = new Canvas(bitmap);
				drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
				drawable.draw(canvas);
				views.setImageViewBitmap(R.id.widget_background, bitmap);
				views.setTextColor(R.id.widget_list_name,
						WidgetHelper.getFontColor(context, widgetId));
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
			if (!oldAPI) {
				views.setImageViewBitmap(
						R.id.widget_preferences,
						colorizeBitmap(
								WidgetHelper.getFontColor(context, widgetId),
								context.getResources().getDrawable(
										R.drawable.ic_action_overflow),
								new int[] { 52, 52, 52 }, 3));
				views.setImageViewBitmap(
						R.id.widget_add_task,
						colorizeBitmap(
								WidgetHelper.getFontColor(context, widgetId),
								context.getResources().getDrawable(
										R.drawable.ic_action_new),
								new int[] { 52, 52, 52 }, 3));
			}

			if (!isMinimalistic && !oldAPI) {
				views.setImageViewBitmap(
						R.id.widget_add_task,
						colorizeBitmap(
								WidgetHelper.getFontColor(context, widgetId),
								context.getResources().getDrawable(
										android.R.drawable.ic_menu_add),
								new int[] { 250, 250, 250 }, 200));
			}

			// Create an Intent to launch MainActivity and show the List
			Intent mainIntent;
			try {
				mainIntent = new Intent(context,  Class.forName(DefinitionsHelper.MAINACTIVITY_CLASS));
			} catch (ClassNotFoundException e) {
				Log.wtf(TAG, "mainactivity not found");
				return;
			}
			mainIntent.setAction(DefinitionsHelper.SHOW_LIST_FROM_WIDGET
					+ list.getId());
			PendingIntent mainPendingIntent = PendingIntent.getActivity(
					context, 0, mainIntent, 0);
			views.setOnClickPendingIntent(R.id.widget_list_name,
					mainPendingIntent);

			// ListName
			views.setTextViewText(R.id.widget_list_name, list.getName());

			// Create an Intent to launch MainActivity and create a new Task
			Intent addIntent;
			try {
				addIntent = new Intent(context,  Class.forName(DefinitionsHelper.MAINACTIVITY_CLASS));
			} catch (ClassNotFoundException e) {
				Log.wtf(TAG, "mainactivity not found");
				return;
			}
			addIntent.setAction(DefinitionsHelper.ADD_TASK_FROM_WIDGET
					+ list.getId());
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
				try {
					appWidgetManager.updateAppWidget(new int[] { widgetId }, views);
				} catch (Exception e) {
					Log.d(TAG, "cannot create widget");
					return;
				}
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
					boolean darkTheme = WidgetHelper.isDark(context, widgetId);
					views.setInt(R.id.widget_main, "setBackgroundResource",
							darkTheme ? R.drawable.widget_background_dark
									: R.drawable.widget_background);
					views.setTextColor(
							R.id.widget_list_name,
							context.getResources().getColor(
									darkTheme ? R.color.White : R.color.Black));
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
											t, context, list.getId(), false,
											widgetId));
						}
					} catch (IndexOutOfBoundsException e) {
						Log.wtf(TAG,
								"The list has been shortened while processing it…");
					}
				}
			}
			appWidgetManager.updateAppWidget(widgetId, views);

		}
//		if (!oldAPI)
//			appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds,
//					R.id.tasks_list);

	}

	private static Bitmap colorizeBitmap(int to, Drawable c, int[] oldColor,
			int THRESHOLD) {
		Bitmap bitmap;
		Bitmap src = ((BitmapDrawable) c).getBitmap();
		bitmap = src.copy(Bitmap.Config.ARGB_8888, true);
		for (int x = 0; x < bitmap.getWidth(); x++) {
			for (int y = 0; y < bitmap.getHeight(); y++) {
				if (match(bitmap.getPixel(x, y), oldColor, THRESHOLD))
					bitmap.setPixel(x, y, to);
			}
		}
		return bitmap;
	}

	private static boolean match(int pixel, int[] FROM_COLOR, int THRESHOLD) {
		// There may be a better way to match, but I wanted to do a comparison
		// ignoring
		// transparency, so I couldn't just do a direct integer compare.
		return Math.abs(Color.red(pixel) - FROM_COLOR[0]) < THRESHOLD
				&& Math.abs(Color.green(pixel) - FROM_COLOR[1]) < THRESHOLD
				&& Math.abs(Color.blue(pixel) - FROM_COLOR[2]) < THRESHOLD;
	}

	@SuppressLint("NewApi")
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(CLICK_TASK)) {
			int taskId = intent.getIntExtra(EXTRA_TASKID, 0);
			Intent startMainIntent;
			try {
				startMainIntent = new Intent(context,  Class.forName(DefinitionsHelper.MAINACTIVITY_CLASS));
			} catch (ClassNotFoundException e) {
				Log.wtf(TAG, "mainactivity not found");
				return;
			}
			startMainIntent.setAction(DefinitionsHelper.SHOW_TASK_FROM_WIDGET);
			startMainIntent.putExtra(DefinitionsHelper.EXTRA_ID, taskId);
			startMainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startMainIntent.setData(Uri.parse(startMainIntent
					.toUri(Intent.URI_INTENT_SCHEME)));
			context.startActivity(startMainIntent);
		}
		Log.d(TAG, "" + intent.getAction());
		if (intent.getAction().equals(
				"android.appwidget.action.APPWIDGET_UPDATE")) {
			AppWidgetManager a = AppWidgetManager.getInstance(context);
			for (int w : a.getAppWidgetIds(new ComponentName(context,
					MainWidgetProvider.class))) {
//				if (!oldAPI)
//					a.notifyAppWidgetViewDataChanged(w, R.id.tasks_list);
			}
			onUpdate(context, a, a.getAppWidgetIds(new ComponentName(context,
					MainWidgetProvider.class)));
		}
		super.onReceive(context, intent);
	}
}
