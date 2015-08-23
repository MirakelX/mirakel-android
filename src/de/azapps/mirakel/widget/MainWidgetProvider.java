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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.RemoteViews;

import com.google.common.base.Optional;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.WidgetHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;

public class MainWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "MainWidgetProvider";
    public static final String CLICK_TASK = "de.azapps.mirakel.CLICK_TASK",
                               EXTRA_TASK = "de.azapps.mirakel.WIDGET.EXTRA_TASK",
                               EXTRA_WIDGET_LAYOUT = "de.azapps.mirakel.EXTRA_WIDGET_LAYOUT",
                               EXTRA_WIDGET_ID = "de.azapps.mirakel.EXTRA_WIDGET_ID";

    public static final int NORMAL_WIDGET = 0;
    @Nullable
    private static SparseIntArray listMapping = null;

    public static void update(final SparseIntArray idMapping) {
        listMapping = idMapping;
    }

    @Override
    @SuppressLint("NewApi")
    public void onUpdate(final Context context,
                         final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        if (listMapping != null) {
            WidgetHelper.updateListIDs(context, listMapping, appWidgetIds);
            listMapping = null;
        }
        for (final int widgetId : appWidgetIds) {
            Log.v(TAG, "update Widget: " + widgetId);
            final boolean isDark = WidgetHelper.isDark(context, widgetId);
            final int layout_id = R.layout.widget_minimal;
            final RemoteViews views = new RemoteViews(context.getPackageName(),
                    layout_id);
            // Main Intent

            final Intent intent = new Intent(context,
                                             MainWidgetService.class);


            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            final Intent toastIntent  = new Intent(context, MainWidgetProvider.class);

            toastIntent.setData(Uri.parse(intent
                                          .toUri(Intent.URI_INTENT_SCHEME)));
            toastIntent.putExtra(EXTRA_WIDGET_LAYOUT, NORMAL_WIDGET);
            toastIntent.setExtrasClassLoader(Task.class.getClassLoader());
            final PendingIntent toastPendingIntent = PendingIntent
                    .getBroadcast(context, 0, toastIntent,
                                  PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.widget_tasks_list,
                                           toastPendingIntent);

            final int widgetBackground;
            if (WidgetHelper.gethasGradient(context, widgetId)) {
                widgetBackground = isDark ? R.drawable.widget_background_minimalistic_dark
                                   : R.drawable.widget_background_minimalistic;
            } else {
                widgetBackground = isDark ? R.drawable.widget_background_minimalistic_dark_wo_gradient
                                   : R.drawable.widget_background_minimalistic_wo_gradient;
            }
            final GradientDrawable drawable = (GradientDrawable) context
                                              .getResources().getDrawable(widgetBackground);
            drawable.setAlpha(WidgetHelper.getTransparency(context,
                              widgetId));
            final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            final Bitmap bitmap = Bitmap.createBitmap(metrics.widthPixels, metrics.heightPixels,
                                  Config.ARGB_8888);
            final Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            views.setImageViewBitmap(R.id.widget_background, bitmap);
            views.setTextColor(R.id.widget_list_name,
                               WidgetHelper.getFontColor(context, widgetId));
            final ListMirakel list = WidgetHelper.getList(context, widgetId);
            // Create an Intent to launch SettingsActivity
            final Intent settingsIntent = new Intent(context,
                    MainWidgetSettingsActivity.class);
            settingsIntent.putExtra(EXTRA_WIDGET_ID, widgetId);
            settingsIntent.setData(Uri.parse(settingsIntent
                                             .toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent settingsPendingIntent = PendingIntent
                    .getActivity(context, 0, settingsIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_preferences,
                                          settingsPendingIntent);
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
                        R.drawable.ic_action_new), new int[] {
                        52, 52, 52
                    }, 3));
            // Create an Intent to launch MainActivity and show the List
            final Optional<Class<?>> main = Helpers.getMainActivity();
            if (!main.isPresent()) {
                return;
            }
            final Intent mainIntent = new Intent(context, main.get());
            mainIntent.setAction(DefinitionsHelper.SHOW_LIST_FROM_WIDGET);
            mainIntent.putExtra(DefinitionsHelper.EXTRA_LIST, list);
            mainIntent.setData(Uri.parse(DefinitionsHelper.SHOW_LIST_FROM_WIDGET + list.getId()));
            final PendingIntent mainPendingIntent = PendingIntent.getActivity(
                    context, 0, mainIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_list_name,
                                          mainPendingIntent);
            // ListName
            views.setTextViewText(R.id.widget_list_name, list.getName());
            // Create an Intent to launch MainActivity and create a new Task

            final Intent addIntent = new Intent(context, main.get());
            addIntent.setAction(DefinitionsHelper.ADD_TASK_FROM_WIDGET);
            addIntent.putExtra(DefinitionsHelper.EXTRA_LIST, list);
            addIntent.setData(Uri.parse(DefinitionsHelper.ADD_TASK_FROM_WIDGET + list.getId()));
            final PendingIntent addPendingIntent = PendingIntent.getActivity(
                    context, 0, addIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_add_task,
                                          addPendingIntent);
            views.setRemoteAdapter(R.id.widget_tasks_list, intent);
            try {
                appWidgetManager.updateAppWidget(new int[] { widgetId },
                                                 views);

                appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_tasks_list);
            } catch (final RuntimeException e) {
                Log.d(TAG, "cannot create widget", e);
                return;
            }
            // Empty view
            views.setEmptyView(R.id.widget_tasks_list, R.id.empty_view);
            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }

    private static Bitmap colorizeBitmap(final int to, final Drawable c,
                                         final int[] oldColor, final int THRESHOLD) {
        final Bitmap bitmap;
        final Bitmap src = ((BitmapDrawable) c).getBitmap();
        bitmap = src.copy(Bitmap.Config.ARGB_8888, true);
        for (int x = 0; x < bitmap.getWidth(); x++) {
            for (int y = 0; y < bitmap.getHeight(); y++) {
                if (match(bitmap.getPixel(x, y), oldColor, THRESHOLD)) {
                    bitmap.setPixel(x, y, to);
                }
            }
        }
        return bitmap;
    }

    private static boolean match(final int pixel, final int[] FROM_COLOR,
                                 final int THRESHOLD) {
        // There may be a better way to match, but I wanted to do a comparison
        // ignoring
        // transparency, so I couldn't just do a direct integer compare.
        return (Math.abs(Color.red(pixel) - FROM_COLOR[0]) < THRESHOLD)
               && (Math.abs(Color.green(pixel) - FROM_COLOR[1]) < THRESHOLD)
               && (Math.abs(Color.blue(pixel) - FROM_COLOR[2]) < THRESHOLD);
    }

    @SuppressLint("NewApi")
    @Override
    public void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
        if (CLICK_TASK.equals(intent.getAction())) {
            final Task task = intent.getBundleExtra(DefinitionsHelper.BUNDLE_WRAPPER).getParcelable(EXTRA_TASK);
            final Optional<Class<?>> main = Helpers.getMainActivity();
            if (!main.isPresent()) {
                return;
            }

            final Intent startMainIntent = new Intent(context, main.get());
            startMainIntent.setAction(DefinitionsHelper.SHOW_TASK_FROM_WIDGET);
            startMainIntent.putExtra(DefinitionsHelper.EXTRA_TASK, task);
            startMainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startMainIntent.setData(Uri.parse(startMainIntent
                                              .toUri(Intent.URI_INTENT_SCHEME)));
            context.startActivity(startMainIntent);
        }
        if ("android.appwidget.action.APPWIDGET_UPDATE".equals(intent.getAction())) {
            final AppWidgetManager a = AppWidgetManager.getInstance(context);
            onUpdate(context, a, a.getAppWidgetIds(new ComponentName(context,
                                                   MainWidgetProvider.class)));
        }
        super.onReceive(context, intent);
    }
}
