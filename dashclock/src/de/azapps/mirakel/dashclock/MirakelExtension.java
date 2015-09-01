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
package de.azapps.mirakel.dashclock;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.google.common.base.Optional;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.model.MirakelContentObserver;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakel.model.query_builder.CursorWrapper;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.task.Task;

public class MirakelExtension extends DashClockExtension implements
    SharedPreferences.OnSharedPreferenceChangeListener, MirakelContentObserver.ObserverCallBack {
    private static final String TAG = "MirakelExtension";
    private static int notifId = 0;

    public static void reportError(final Context context, final String title, final String message) {
        final Notification notification = new NotificationCompat.Builder(context)
        .setContentText(message)
        .setContentTitle(title)
        .setSmallIcon(R.drawable.mirakel).build();
        final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notifId++, notification);
    }

    public static void init(final Context ctx) {
        DefinitionsHelper.init(ctx, "");
        MirakelPreferences.init(ctx);
        ErrorReporter.init(ctx);
        ModelBase.init(ctx);
        SettingsHelper.init(ctx);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifId = 0;

        init(this);

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(this);

        final Map<Uri, MirakelContentObserver.ObserverCallBack> observerCallBackMap = new HashMap<>(2);
        observerCallBackMap.put(Task.URI, this);
        observerCallBackMap.put(ListMirakel.URI, this);
        new MirakelContentObserver(new Handler(Looper.getMainLooper()), this, observerCallBackMap);
    }


    @Override
    protected void onUpdateData(final int reason) {
        // Get values from Settings
        final Optional<ListMirakel> listMirakelOptional = SettingsHelper.getList();
        if (!listMirakelOptional.isPresent()) {
            reportError(this, getString(R.string.list_not_found), getString(R.string.list_not_found_message));
            return;
        }
        final int maxTasks = SettingsHelper.getMaxTasks();
        final ListMirakel listMirakel = listMirakelOptional.get();
        final MirakelQueryBuilder mirakelQueryBuilder = listMirakel.getTasksQueryBuilder();
        final CursorWrapper cursor;
        try {
            cursor = mirakelQueryBuilder.query(Task.URI);
        } catch (final SecurityException ignored) {
            reportError(this, getString(R.string.no_permission_title), getString(R.string.no_permission));
            return;
        } catch (final RuntimeException e) {
            reportError(this, getString(R.string.cannot_communicate), getString(R.string.unexpected_error));
            Log.e(TAG, "Cannot communicate to Mirakel", e);
            return;
        }
        cursor.doWithCursor(new CursorWrapper.WithCursor() {
            @Override
            public void withOpenCursor(@NonNull final CursorGetter getter) {
                // Set Status
                if ((getter.getCount() == 0) && !SettingsHelper.showEmpty()) {
                    Log.d(TAG, "hide");
                    publishUpdate(new ExtensionData().visible(false));
                } else {
                    final boolean showDue = SettingsHelper.showDue();
                    final SimpleDateFormat dateFormat = new SimpleDateFormat(
                        getString(R.string.due_outformat), Locale.getDefault());

                    final String status = getResources().getQuantityString(R.plurals.status,
                                          getter.getCount(), getter.getCount());
                    final String tasks[] = new String[Math.min(maxTasks, getter.getCount())];
                    int i = 0;
                    while (getter.moveToNext() && (i < maxTasks)) {
                        final Task task = new Task(getter);
                        final Optional<DateTime> dueOptional = task.getDue();
                        final StringBuilder taskRow = new StringBuilder();
                        if (dueOptional.isPresent() && showDue) {
                            taskRow.append(dateFormat.format(dueOptional.get().toDate())).append(": ");
                        }
                        taskRow.append(task.getName());
                        tasks[i] = taskRow.toString();
                        i++;
                    }

                    // Add click-event
                    final Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(new ComponentName("de.azapps.mirakelandroid",
                                                          "de.azapps.mirakel.main_activity.MainActivity"));
                    intent.setAction("de.azapps.mirakel.SHOW_LIST");
                    intent.putExtra("de.azapps.mirakel.EXTRA_LIST_ID", listMirakel.getId());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    // Set Content
                    publishUpdate(new ExtensionData().visible(true)
                                  .icon(R.drawable.ic_mirakel).status(status)
                                  .expandedBody(TextUtils.join("\n", tasks)).clickIntent(intent));
                }
            }
        });

    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        onUpdateData(UPDATE_REASON_SETTINGS_CHANGED);
    }

    @Override
    public void handleChange() {
        onUpdateData(UPDATE_REASON_CONTENT_CHANGED);
    }

    @Override
    public void handleChange(final long id) {
        onUpdateData(UPDATE_REASON_CONTENT_CHANGED);
    }
}
