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

package de.azapps.mirakel.reminders;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Pair;

import com.google.common.base.Optional;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.MirakelContentObserver;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakel.services.TaskService;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class ReminderAlarm extends BroadcastReceiver {
    private static final String TAG = "ReminderAlarm";
    public static final String UPDATE_NOTIFICATION =
        "de.azapps.mirakel.reminders.ReminderAlarm.UPDATE_NOTIFICATION";
    public static final String SHOW_TASK = "de.azapps.mirakel.reminders.ReminderAlarm.SHOW_TASK";
    public static final String TASK_PAYLOAD = "de.azapps.mirakel.reminders.ReminderAlarm.TASK_PAYLOAD";

    private static ReminderHandler handler;



    public static void init(final Context ctx) {
        handler = new ReminderHandler(ctx);
    }

    public static void destroy(final Context ctx) {
        handler.destroy();
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (UPDATE_NOTIFICATION.equals(intent.getAction())) {
            NotificationService.updateServices(context);
        }
        if (!SHOW_TASK.equals(intent.getAction())) {
            return;
        }
        final Task t = intent.getParcelableExtra(TASK_PAYLOAD);
        if (t == null) {
            Log.wtf(TAG, "task is null");
            return;
        }
        handler.sendNotificantion(t);
    }

    private static void updateAlarms() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.updateAllReminders();
            }
        }).start();
    }


    public static void restart() {
        handler.clear();
        updateAlarms();
    }

    private static class ReminderHandler implements MirakelContentObserver.ObserverCallBack {

        @NonNull
        private final Set<Long> currentNotifications = new HashSet<>();
        private NotificationManager notificationManager;
        @NonNull
        private Optional<MirakelContentObserver> observer = absent();

        private AlarmManager alarmManager;
        private Context ctx;
        private final Map<Long, Pair<Task, PendingIntent>> activeReminders = new
        ConcurrentHashMap<>();

        ReminderHandler(final @NonNull Context ctx) {
            notificationManager = (NotificationManager) ctx
                                  .getSystemService(Context.NOTIFICATION_SERVICE);
            alarmManager = (AlarmManager) ctx
                           .getSystemService(Context.ALARM_SERVICE);
            //Update Dashclock etc every day
            final Intent intent = new Intent(ctx, ReminderAlarm.class);
            intent.setAction(UPDATE_NOTIFICATION);
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                                    ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            final Calendar triggerCal = new GregorianCalendar();
            triggerCal.set(Calendar.HOUR_OF_DAY, 0);
            triggerCal.set(Calendar.MINUTE, 0);
            triggerCal.add(Calendar.DAY_OF_MONTH, 1);
            alarmManager.setRepeating(AlarmManager.RTC,
                                      triggerCal.getTimeInMillis(),
                                      AlarmManager.INTERVAL_DAY, pendingIntent);
            this.ctx = ctx;
            observer = of(new MirakelContentObserver(new Handler(ctx.getMainLooper()), ctx, Task.URI, this));
            updateAlarms();
        }


        synchronized void updateAllReminders() {
            Log.w(TAG, "update Reminders");
            final Set<Long> activeReminderTasks = new HashSet<>(activeReminders.keySet());
            final List<Task> tasksWithReminder = Task.addBasicFiler(new MirakelQueryBuilder(ctx))
                                                 .and(Task.REMINDER, MirakelQueryBuilder.Operation.NOT_EQ, (String) null)
                                                 .and(Task.DONE, MirakelQueryBuilder.Operation.EQ, false)
                                                 .getList(Task.class);
            for (final Task t : tasksWithReminder) {
                if (t.getReminder().isPresent()) {
                    if (activeReminderTasks.contains(t.getId())) {
                        updateReminderFor(t);
                        activeReminderTasks.remove(t.getId());
                    } else {//Reminder is not shown
                        createReminderFor(t);
                    }
                }
            }
            for (final Long t : activeReminderTasks) {
                removeReminderFor(t);
            }
        }


        private synchronized void sendNotificantion(final Task task) {
            Log.w(TAG, task.getName());
            final Optional<Class<?>> main = Helpers.getMainActivity();
            if (!main.isPresent()) {
                return;
            }
            final Intent openIntent = new Intent(ctx, main.get());

            final Bundle withTask = new Bundle();
            withTask.putParcelable(DefinitionsHelper.EXTRA_TASK, task);
            openIntent.setAction(DefinitionsHelper.SHOW_TASK_REMINDER);
            openIntent.putExtra(DefinitionsHelper.EXTRA_TASK_REMINDER, task);
            openIntent.putExtra(String.valueOf(task.getId()), task.getId());
            openIntent
            .setData(Uri.parse(openIntent.toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent pOpenIntent = PendingIntent.getActivity(ctx, 0,
                                              openIntent, 0);
            final Intent doneIntent = new Intent(ctx, TaskService.class);
            doneIntent.setAction(TaskService.TASK_DONE);
            doneIntent.putExtra(DefinitionsHelper.BUNDLE_WRAPPER, withTask);
            doneIntent.putExtra(String.valueOf(task.getId()), task.getId());
            doneIntent
            .setData(Uri.parse(doneIntent.toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent pDoneIntent = PendingIntent.getService(ctx, 0,
                                              doneIntent, 0);
            final Intent laterIntent = new Intent(ctx, TaskService.class);
            laterIntent.setAction(TaskService.TASK_LATER);
            laterIntent.putExtra(DefinitionsHelper.BUNDLE_WRAPPER, withTask);
            laterIntent.putExtra(String.valueOf(task.getId()), task.getId());
            laterIntent.setData(Uri.parse(laterIntent
                                          .toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent pLaterIntent = PendingIntent.getService(ctx, 0,
                                               laterIntent, 0);
            final boolean persistent = MirakelCommonPreferences
                                       .usePersistentReminders();
            // Build Notification
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(
                ctx);
            builder.setContentTitle(
                ctx.getString(R.string.reminder_notification_title,
                              task.getName()))
            .setContentText(task.getContent())
            .setSmallIcon(R.drawable.ic_mirakel)
            .setLargeIcon(Helpers.getBitmap(R.drawable.mirakel, ctx))
            .setContentIntent(pOpenIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setLights(Color.BLUE, 1500, 300)
            .setOngoing(persistent)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSound(
                RingtoneManager
                .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .addAction(R.drawable.ic_checkmark_holo_light,
                       ctx.getString(R.string.reminder_notification_done),
                       pDoneIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                ctx.getString(R.string.reminder_notification_later),
                pLaterIntent);

            final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            final String priority = ((task.getPriority() > 0) ? ("+" + task.getPriority()) : String.valueOf(task
                                     .getPriority()));
            final CharSequence due;
            if (!task.getDue().isPresent()) {
                due = ctx.getString(R.string.no_date);
            } else {
                due = DateTimeHelper.formatDate(ctx, task.getDue());
            }
            inboxStyle.addLine(ctx.getString(
                                   R.string.reminder_notification_list, task.getList()
                                   .getName()));
            inboxStyle.addLine(ctx.getString(
                                   R.string.reminder_notification_priority, priority));
            inboxStyle.addLine(ctx.getString(
                                   R.string.reminder_notification_due, due));
            builder.setStyle(inboxStyle);
            // Build notification
            if (currentNotifications.contains(task.getId())) {
                notificationManager.cancel(DefinitionsHelper.NOTIF_REMINDER + (int) task.getId());
            } else {
                currentNotifications.add(task.getId());
            }
            notificationManager.notify(DefinitionsHelper.NOTIF_REMINDER + (int) task.getId(),
                                       builder.build());
        }


        private synchronized void removeReminderFor(final Long taskID) {
            alarmManager.cancel(activeReminders.remove(taskID).second);
            if (currentNotifications.contains(taskID)) {
                notificationManager.cancel(DefinitionsHelper.NOTIF_REMINDER + (int) ((long) taskID));
                currentNotifications.remove(taskID);
            }
        }

        private synchronized void createReminderFor(final Task t) {
            final Intent intent = new Intent(ctx, ReminderAlarm.class);
            intent.setAction(SHOW_TASK);
            intent.putExtra(TASK_PAYLOAD, t);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent p = PendingIntent.getBroadcast(
                                        ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            if (t.hasRecurringReminder()) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, getTriggerAtMillis(t.getReminder().get()),
                                          t.getRecurringReminder().get().getInterval(), p);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, getTriggerAtMillis(t.getReminder().get()), p);
            }
            activeReminders.put(t.getId(), new Pair<>(t, p));
        }

        private static long getTriggerAtMillis(final Calendar reminder) {
            return reminder.getTimeInMillis();
        }

        private synchronized void updateReminderFor(final Task t) {
            final Pair<Task, PendingIntent> pair = activeReminders.get(t.getId());
            final Calendar reminder = t.getReminder().get();
            if (pair.first.getReminder().get().compareTo(t.getReminder().get()) != 0) {
                alarmManager.cancel(pair.second);
                if (t.hasRecurringReminder()) {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, getTriggerAtMillis(t.getReminder().get()),
                                              t.getRecurringReminder().get().getInterval(), pair.second);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, getTriggerAtMillis(t.getReminder().get()), pair.second);
                }
            }
            if (currentNotifications.contains(t.getId()) &&
                (reminder.compareTo(new GregorianCalendar()) == 1)) {
                notificationManager.cancel((int) (DefinitionsHelper.NOTIF_REMINDER + t.getId()));
                currentNotifications.remove(t.getId());
            }
        }

        public synchronized void clear() {
            for (final Map.Entry<Long, Pair<Task, PendingIntent>> entry : activeReminders.entrySet()) {
                removeReminderFor(entry.getKey());
            }
        }

        @Override
        public void handleChange() {
            updateAlarms();
            NotificationService.updateServices(ctx);
        }

        @Override
        public synchronized void handleChange(final long id) {
            NotificationService.updateServices(ctx);
            final Optional<Task> t = Task.get(id);
            if (t.isPresent()) {
                final Task task = t.get();
                if (task.isDone() && activeReminders.containsKey(task.getId())) {
                    removeReminderFor(task.getId());
                } else if (task.getReminder().isPresent()) {
                    if (activeReminders.containsKey(task.getId())) {
                        updateReminderFor(task);
                    } else {
                        createReminderFor(task);
                    }
                } else if (activeReminders.containsKey(task.getId())) {
                    removeReminderFor(task.getId());
                }
            }

        }

        public void destroy() {
            if (observer.isPresent()) {
                observer.get().unregister(ctx);
                observer = absent();
            }
        }
    }
}
