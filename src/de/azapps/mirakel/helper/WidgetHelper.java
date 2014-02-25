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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.widget.R;
import de.azapps.tools.Log;

public class WidgetHelper {
	public static RemoteViews configureItem(RemoteViews rv, Task task,
			Context context, int listId, boolean isMinimal, int widgetId) {
		Intent openIntent;
		try {
			openIntent = new Intent(context, Class.forName(DefinitionsHelper.MAINACTIVITY_CLASS));
		} catch (ClassNotFoundException e) {
			Log.wtf(TAG,"no mainactivity found");
			return null;
		}
		openIntent.setAction(DefinitionsHelper.SHOW_TASK);
		openIntent.putExtra(DefinitionsHelper.EXTRA_ID, task.getId());
		openIntent
				.setData(Uri.parse(openIntent.toUri(Intent.URI_INTENT_SCHEME)));
		PendingIntent pOpenIntent = PendingIntent.getActivity(context, 0,
				openIntent, 0);

		rv.setOnClickPendingIntent(R.id.tasks_row, pOpenIntent);
		rv.setOnClickPendingIntent(R.id.tasks_row_name, pOpenIntent);
		if (isMinimal) {
			if (task.getDue() != null) {
				rv.setTextViewText(R.id.tasks_row_due,
						DateTimeHelper.formatDate(context, task.getDue()));
			} else {
				rv.setViewVisibility(R.id.tasks_row_due, View.GONE);
			}
			rv.setInt(R.id.tasks_row_priority, "setBackgroundColor",
					TaskHelper.getPrioColor(task.getPriority()));
		}
		rv.setTextColor(R.id.tasks_row_name,
				WidgetHelper.getFontColor(context, widgetId));
		if (getBoolean(context, widgetId, "widgetDueColors", true)) {
			rv.setTextColor(
					R.id.tasks_row_due,
					context.getResources().getColor(
							TaskHelper.getTaskDueColor(task.getDue(),
									task.isDone())));
		} else {
			rv.setTextColor(R.id.tasks_row_due,
					WidgetHelper.getFontColor(context, widgetId));
		}
		rv.setTextViewText(R.id.tasks_row_name, task.getName());
		if (task.isDone()) {
			rv.setTextColor(R.id.tasks_row_name, context.getResources()
					.getColor(R.color.Grey));
		} else {
			/*
			 * Is this meaningful? I mean the widget is transparent…
			 * rv.setTextColor( R.id.tasks_row_name, context.getResources()
			 * .getColor( preferences.getBoolean("darkWidget", false) ?
			 * R.color.White : R.color.Black));
			 */
		}
		if (!isMinimal) {

			rv.setTextViewText(R.id.tasks_row_priority, task.getPriority() + "");
			rv.setTextColor(R.id.tasks_row_priority, context.getResources()
					.getColor(R.color.Black));
			GradientDrawable drawable = (GradientDrawable) context
					.getResources().getDrawable(R.drawable.priority_rectangle);
			drawable.setColor(TaskHelper.getPrioColor(task.getPriority()));
			Bitmap bitmap = Bitmap.createBitmap(40, 40, Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			drawable.draw(canvas);
			rv.setImageViewBitmap(R.id.label_bg, bitmap);

			if (listId <= 0) {
				rv.setViewVisibility(R.id.tasks_row_list_name, View.VISIBLE);
				rv.setTextViewText(R.id.tasks_row_list_name, task.getList()
						.getName());
			} else {
				rv.setViewVisibility(R.id.tasks_row_list_name, View.GONE);
			}
			if (task.getContent().length() != 0 || task.getSubtaskCount() > 0
					|| task.getFiles().size() > 0) {
				rv.setViewVisibility(R.id.tasks_row_has_content, View.VISIBLE);
			} else {
				rv.setViewVisibility(R.id.tasks_row_has_content, View.GONE);
			}

			if (task.getDue() != null) {
				rv.setViewVisibility(R.id.tasks_row_due, View.VISIBLE);

				rv.setTextViewText(R.id.tasks_row_due,
						DateTimeHelper.formatDate(context, task.getDue()));
				if (!isMinimal) {
					rv.setTextColor(
							R.id.tasks_row_due,
							context.getResources().getColor(
									TaskHelper.getTaskDueColor(task.getDue(),
											task.isDone())));
				}
			} else {
				rv.setViewVisibility(R.id.tasks_row_due, View.GONE);
			}
		}
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			rv.setTextColor(
					R.id.tasks_row_name,
					context.getResources()
							.getColor(
									WidgetHelper.isDark(context, widgetId) ? R.color.White
											: R.color.Black));
		}
		return rv;
	}

	// For settings
	private static final String PREFS_NAME = "de.azapps.mirakelandroid.appwidget.MainWidgetProvider";
	private static final String PREF_PREFIX = "widget_";
	private static final String TAG = "WidgetHelper";
	private static SharedPreferences settings = null;

	private static SharedPreferences getSettings(Context ctx) {
		if (settings == null) {
			settings = ctx.getSharedPreferences(PREFS_NAME, 0);
		}
		return settings;
	}

	private static String getKey(int widgetId, String key) {
		return PREF_PREFIX + widgetId + "_" + key;
	}

	private static boolean getBoolean(Context context, int widgetId,
			String key, boolean def) {
		return getSettings(context).getBoolean(getKey(widgetId, key), def);

	}

	private static int getInt(Context context, int widgetId, String key,
			int white) {
		return getSettings(context).getInt(getKey(widgetId, key), white);
	}

	public static boolean isDark(Context context, int widgetId) {
		return getBoolean(context, widgetId, "isDark", true);
	}

	public static boolean isMinimalistic(Context context, int widgetId) {
		return true;
	}

	public static boolean showDone(Context context, int widgetId) {
		return getBoolean(context, widgetId, "showDone", false);
	}

	public static boolean dueColors(Context context, int widgetId) {
		return getBoolean(context, widgetId, "widgetDueColors", true);
	}

	public static int getFontColor(Context context, int widgetId) {
		return getInt(context, widgetId, "widgetFontColor", context
				.getResources().getColor(android.R.color.white));
	}

	public static int getTransparency(Context context, int widgetId) {
		return getInt(context, widgetId, "widgetTransparency", 100);
	}

	public static ListMirakel getList(Context context, int widgetId) {

		int listId = getSettings(context)
				.getInt(getKey(widgetId, "list_id"), 0);
		ListMirakel list = ListMirakel.getList(listId);
		if (list == null) {
			list = SpecialList.firstSpecial();
			if (list == null) {
				list = ListMirakel.first();
				if (list == null) {
					Toast.makeText(context, "You have no Lists!",
							Toast.LENGTH_SHORT).show();
					return null;
				}
			}
		}
		return list;
	}

	public static void setList(Context context, int widgetId, int listId) {
		Editor editor = getSettings(context).edit();
		editor.putInt(getKey(widgetId, "list_id"), listId);
		editor.commit();
	}

	public static void putBool(Context context, int widgetId, String key,
			boolean value) {
		Editor editor = getSettings(context).edit();
		editor.putBoolean(getKey(widgetId, key), value);
		editor.commit();

	}

	public static void putInt(Context context, int widgetId, String key,
			int value) {
		Editor editor = getSettings(context).edit();
		editor.putInt(getKey(widgetId, key), value);
		editor.commit();

	}

	public static void setDone(Context context, int widgetId, boolean done) {
		putBool(context, widgetId, "showDone", done);
	}

	public static void setMinimalistic(Context context, int widgetId,
			boolean minimalistic) {
		putBool(context, widgetId, "isMinimalistic", minimalistic);
	}

	public static void setDark(Context context, int widgetId, boolean dark) {
		putBool(context, widgetId, "isDark", dark);
	}
	public static void setDueColors(Context context, int widgetId, boolean done) {
		putBool(context, widgetId, "widgetDueColors", done);
	}

	public static void setFontColor(Context context, int widgetId, int color) {
		putInt(context, widgetId, "widgetFontColor", color);
	}

	public static void setTransparency(Context context, int widgetId,
			int transparency) {
		putInt(context, widgetId, "widgetTransparency", transparency);
	}
}
