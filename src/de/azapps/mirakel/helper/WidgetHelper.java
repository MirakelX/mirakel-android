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
package de.azapps.mirakel.helper;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.SparseIntArray;
import android.widget.RemoteViews;

import com.google.common.base.Optional;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.widget.R;

public class WidgetHelper {
    public static RemoteViews configureItem(final RemoteViews rv,
                                            final Task task, final Context context,
                                            final int widgetId) {

        final Optional<Class<?>> main = Helpers.getMainActivity();
        if (!main.isPresent()) {
            return null;
        }
        final Intent openIntent = new Intent(context, main.get());


        openIntent.setAction(DefinitionsHelper.SHOW_TASK);
        final Bundle wrapper = new Bundle();
        wrapper.putParcelable(DefinitionsHelper.EXTRA_TASK, task);
        openIntent.putExtra(DefinitionsHelper.BUNDLE_WRAPPER, wrapper);
        openIntent
        .setData(Uri.parse(openIntent.toUri(Intent.URI_INTENT_SCHEME)));
        final PendingIntent pOpenIntent = PendingIntent.getActivity(context, 0,
                                          openIntent, 0);
        rv.setOnClickPendingIntent(R.id.tasks_row, pOpenIntent);

        SpannableStringBuilder textBuilder = new SpannableStringBuilder();

        rv.setInt(R.id.tasks_row_priority, "setBackgroundColor",
                  TaskHelper.getPrioColor(task.getPriority()));

        final int dueTextLength;
        if (task.getDue().isPresent()) {
            textBuilder.append(DateTimeHelper.formatDate(context, task.getDue()));
            textBuilder.setSpan(new ForegroundColorSpan(TaskHelper.getTaskDueColor(task.getDue(),
                                task.isDone())), 0, textBuilder.length(), 0);
            textBuilder.append(" ");
            dueTextLength = textBuilder.length();
        } else {
            dueTextLength = 0;
        }
        textBuilder.append(task.getName());
        textBuilder.setSpan(new ForegroundColorSpan(WidgetHelper.getFontColor(context, widgetId)),
                            dueTextLength, textBuilder.length(), 0);
        rv.setTextViewText(R.id.tasks_row_text, textBuilder);
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
        return PREF_PREFIX + widgetId + '_' + key;
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

    @NonNull
    public static ListMirakel getList(final Context context, final int widgetId) {
        final int listId = getSettings(context).getInt(
                               getKey(widgetId, "list_id"), 0);
        final Optional<ListMirakel> list = ListMirakel.get(listId);
        if (!list.isPresent()) {
            return SpecialList.firstSpecialSafe();
        } else {
            return list.get();
        }
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

    public static void updateListIDs(final Context context, final SparseIntArray listMapping,
                                     final int[] appWidgetIds) {
        for (final int widgetId : appWidgetIds) {
            final int listId = getSettings(context).getInt(
                                   getKey(widgetId, "list_id"), 0);
            setList(context, widgetId, listMapping.get(listId, listId));
        }
    }
}
