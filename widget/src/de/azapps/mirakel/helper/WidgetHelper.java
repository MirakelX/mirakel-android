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
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.widget.R;
import de.azapps.tools.Log;

public class WidgetHelper {
	public static RemoteViews configureItem(final RemoteViews rv,
			final Task task, final Context context, final int listId,
			final boolean isMinimal, final int widgetId) {
		Intent openIntent;
		try {
			openIntent = new Intent(context,
					Class.forName(DefinitionsHelper.MAINACTIVITY_CLASS));
		} catch (final ClassNotFoundException e) {
			Log.wtf(TAG, "no mainactivity found");
			return null;
		}
		openIntent.setAction(DefinitionsHelper.SHOW_TASK);
		openIntent.putExtra(DefinitionsHelper.EXTRA_ID, task.getId());
		openIntent
				.setData(Uri.parse(openIntent.toUri(Intent.URI_INTENT_SCHEME)));
		final PendingIntent pOpenIntent = PendingIntent.getActivity(context, 0,
				openIntent, 0);

		rv.setOnClickPendingIntent(R.id.tasks_row, pOpenIntent);
		rv.setOnClickPendingIntent(R.id.tasks_row_name, pOpenIntent);
		if (isMinimal) {
			if (task.getDue() != null) {
				rv.setViewVisibility(R.id.tasks_row_due, View.VISIBLE);
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
			final GradientDrawable drawable = (GradientDrawable) context
					.getResources().getDrawable(R.drawable.priority_rectangle);
			drawable.setColor(TaskHelper.getPrioColor(task.getPriority()));
			final Bitmap bitmap = Bitmap.createBitmap(40, 40, Config.ARGB_8888);
			final Canvas canvas = new Canvas(bitmap);
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

	private static SharedPreferences getSettings(final Context ctx) {
		if (settings == null) {
			settings = ctx.getSharedPreferences(PREFS_NAME, 0);
		}
		return settings;
	}

	private static String getKey(final int widgetId, final String key) {
		return PREF_PREFIX + widgetId + "_" + key;
	}

	private static boolean getBoolean(final Context context,
			final int widgetId, final String key, final boolean def) {
		return getSettings(context).getBoolean(getKey(widgetId, key), def);

	}

	private static int getInt(final Context context, final int widgetId,
			final String key, final int white) {
		return getSettings(context).getInt(getKey(widgetId, key), white);
	}

	public static boolean isDark(final Context context, final int widgetId) {
		return getBoolean(context, widgetId, "isDark", true);
	}

	public static boolean isMinimalistic(final Context context,
			final int widgetId) {
		return true;
	}

	public static boolean showDone(final Context context, final int widgetId) {
		return getBoolean(context, widgetId, "showDone", false);
	}

	public static boolean dueColors(final Context context, final int widgetId) {
		return getBoolean(context, widgetId, "widgetDueColors", true);
	}

	public static int getFontColor(final Context context, final int widgetId) {
		return getInt(context, widgetId, "widgetFontColor", context
				.getResources().getColor(android.R.color.white));
	}

	public static int getTransparency(final Context context, final int widgetId) {
		return getInt(context, widgetId, "widgetTransparency", 100);
	}

	public static ListMirakel getList(final Context context, final int widgetId) {

		final int listId = getSettings(context).getInt(
				getKey(widgetId, "list_id"), 0);
		ListMirakel list = ListMirakel.get(listId);
		if (list == null) {
			list = SpecialList.firstSpecial();
			if (list == null) {
				list = ListMirakel.first();
				if (list == null) {
					ErrorReporter.report(ErrorType.TASKS_NO_LIST);
					return null;
				}
			}
		}
		return list;
	}

	public static void setList(final Context context, final int widgetId,
			final int listId) {
		final Editor editor = getSettings(context).edit();
		editor.putInt(getKey(widgetId, "list_id"), listId);
		editor.commit();
	}

	public static void putBool(final Context context, final int widgetId,
			final String key, final boolean value) {
		final Editor editor = getSettings(context).edit();
		editor.putBoolean(getKey(widgetId, key), value);
		editor.commit();

	}

	public static void putInt(final Context context, final int widgetId,
			final String key, final int value) {
		final Editor editor = getSettings(context).edit();
		editor.putInt(getKey(widgetId, key), value);
		editor.commit();

	}

	public static void setDone(final Context context, final int widgetId,
			final boolean done) {
		putBool(context, widgetId, "showDone", done);
	}

	public static void setMinimalistic(final Context context,
			final int widgetId, final boolean minimalistic) {
		putBool(context, widgetId, "isMinimalistic", minimalistic);
	}

	public static void setDark(final Context context, final int widgetId,
			final boolean dark) {
		putBool(context, widgetId, "isDark", dark);
	}

	public static void setDueColors(final Context context, final int widgetId,
			final boolean done) {
		putBool(context, widgetId, "widgetDueColors", done);
	}

	public static void setFontColor(final Context context, final int widgetId,
			final int color) {
		putInt(context, widgetId, "widgetFontColor", color);
	}

	public static void setTransparency(final Context context,
			final int widgetId, final int transparency) {
		putInt(context, widgetId, "widgetTransparency", transparency);
	}

	public static void setHasGradient(final Context context,
			final int widgetId, final Boolean newValue) {
		putBool(context, widgetId, "widgetUseGradient", newValue);

	}

	public static boolean gethasGradient(final Context context,
			final int widgetId) {
		return getBoolean(context, widgetId, "widgetUseGradient", true);
	}
}
