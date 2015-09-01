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
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Pair;

import com.google.common.base.Optional;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public static final String REMINDER = "de.azapps.mirakel.reminders.ReminderAlarm.REMINDER";
    public static final String REMINDER_PAYLOAD =
        "de.azapps.mirakel.reminders.ReminderAlarm.REMINDER_PAYLOAD";

    @Nullable
    private static ReminderHandler handler = null;



    public static void init(final Context ctx) {
        handler = new ReminderHandler(ctx);
    }

    public static void destroy() {
        if (handler != null) {
            handler.destroy();
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d(TAG, "receive trigger");
        if (UPDATE_NOTIFICATION.equals(intent.getAction())) {
            NotificationService.updateServices(context);
            if (handler != null) {
                handler.rebuildNotifications();
            }
        }
        if (!REMINDER.equals(intent.getAction())) {
            return;
        }
        final ArrayList<Task> tasks = intent.getParcelableArrayListExtra(REMINDER_PAYLOAD);
        if (tasks == null) {
            return;
        }
        for (final Task t : tasks) {
            if (handler != null) {
                handler.sendNotificantion(t, true);
            }
        }
    }

    private static void updateAlarms() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (handler != null) {
                    handler.updateAllReminders();
                }
            }
        }).start();
    }


    public static void restart() {
        if (handler != null) {
            handler.clear();
        }
        updateAlarms();
    }

    private static class ReminderHandler implements MirakelContentObserver.ObserverCallBack {

        @NonNull
        private final Map<Long, Task> currentNotifications = new HashMap<>();
        @NonNull
        private final NotificationManager notificationManager;
        @NonNull
        private Optional<MirakelContentObserver> observer = absent();
        @NonNull
        private final AlarmManager alarmManager;
        @NonNull
        private final Context ctx;
        @NonNull
        private final Map<Long, Pair<ArrayList<Task>, PendingIntent>> timeToTasks = new HashMap<>();
        @NonNull
        private final Map<Long, Pair<Long, Task>> activeReminders = new HashMap<>();
        @Nullable
        private DateTime now;


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
            if (!DefinitionsHelper.freshInstall) {
                updateAlarms();
            }
        }


        private synchronized void updateAllReminders() {
            Log.d(TAG, "update Reminders");
            updateNow();
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
            for (final Long id : activeReminderTasks) {
                removeReminderFor(activeReminders.get(id).second);
            }
        }


        private synchronized void sendNotificantion(final Task task, final boolean fromReceiver) {
            Log.d(TAG, "create reminder for: " + task.getName());
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
                ctx)
            .setContentTitle(ctx.getString(R.string.reminder_notification_title, task.getName()))
            .setContentText(task.getContent())
            .setSmallIcon(R.drawable.ic_mirakel)
            .setLargeIcon(Helpers.getBitmap(R.drawable.mirakel, ctx))
            .setContentIntent(pOpenIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(persistent)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .addAction(R.drawable.ic_check_grey600_24dp,
                       ctx.getString(R.string.reminder_notification_done),
                       pDoneIntent)
            .addAction(
                R.drawable.ic_alarm_grey600_24dp,
                ctx.getString(R.string.reminder_notification_later),
                pLaterIntent).setOnlyAlertOnce(true);

            if (fromReceiver && !currentNotifications.containsKey(task.getId())) {
                builder.setSound(RingtoneManager
                                 .getActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_NOTIFICATION))
                .setLights(Color.BLUE, 1500, 300)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
            }

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
            currentNotifications.put(task.getId(), task);
            notificationManager.notify(DefinitionsHelper.NOTIF_REMINDER + (int) task.getId(),
                                       builder.build());
        }

        private void closeNotificationFor(final Task task) {
            closeNotificationForTaskId(task.getId());
        }
        private void closeNotificationForTaskId(final long id) {
            notificationManager.cancel(DefinitionsHelper.NOTIF_REMINDER + (int) id);
            currentNotifications.remove(id);
        }


        private synchronized void removeReminderFor(final Task task) {
            final Pair<Long, Task> old = activeReminders.get(task.getId());
            if (old == null) {
                return;
            }
            final long oldTime = old.first;
            final Pair<ArrayList<Task>, PendingIntent> oldPair = timeToTasks.get(oldTime);
            if (oldPair != null) {
                final ArrayList<Task> tasks = oldPair.first;
                alarmManager.cancel(oldPair.second);
                tasks.remove(old.second);
                if (tasks.isEmpty()) {
                    timeToTasks.remove(oldTime);
                } else {
                    timeToTasks.put(oldTime, new Pair<>(tasks, startAlarmFor(oldTime, tasks)));
                }
            }
            activeReminders.remove(task.getId());
            closeNotificationFor(old.second);
        }

        private synchronized void createReminderFor(final Task t) {
            if (!t.getReminder().isPresent()) {
                return;
            }
            final long time = getTriggerAtMillis(t.getReminder().get());
            final ArrayList<Task> tasks;
            if (timeToTasks.containsKey(time)) {
                alarmManager.cancel(timeToTasks.get(time).second);
                tasks = timeToTasks.get(time).first;
            } else {
                tasks = new ArrayList<>(1);
            }
            tasks.add(t);
            timeToTasks.put(time, new Pair<>(tasks, startAlarmFor(time, tasks)));
            activeReminders.put(t.getId(), new Pair<>(time, t));
        }

        private PendingIntent getPendingIntent(final ArrayList<Task> tasks) {
            final Intent intent = new Intent(ctx, ReminderAlarm.class);
            intent.setAction(REMINDER);
            intent.putParcelableArrayListExtra(REMINDER_PAYLOAD, tasks);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            return PendingIntent.getBroadcast(
                       ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        private long getTriggerAtMillis(final DateTime reminder) {
            if (now == null) {
                updateNow();
            }
            if (reminder.isBefore(now)) {
                return now.getMillis();
            } else {
                return reminder.getMillis();
            }
        }

        private synchronized void updateReminderFor(final Task newTask) {
            final Pair<Long, Task> old = activeReminders.get(newTask.getId());
            if (old == null) {
                createReminderFor(newTask);
                return;
            }
            if (!old.second.getReminder().isPresent()) {
                createReminderFor(newTask);
                return;
            }
            if (!newTask.getReminder().isPresent()) {
                removeReminderFor(newTask);
            }
            final long oldTime = old.first;
            final long newTime = getTriggerAtMillis(newTask.getReminder().get());
            if (oldTime == newTime) {
                return;
            }
            removeReminderFor(old.second);
            final Pair<ArrayList<Task>, PendingIntent> newPair = timeToTasks.get(newTime);
            if (newPair != null) {
                final ArrayList<Task> tasks = newPair.first;
                tasks.add(newTask);
                timeToTasks.put(newTime, new Pair<>(tasks, startAlarmFor(newTime, tasks)));
                activeReminders.put(newTask.getId(), new Pair<>(newTime, newTask));
            } else {
                activeReminders.remove(newTask.getId());
                createReminderFor(newTask);
            }
        }

        private PendingIntent startAlarmFor(final long time, final ArrayList<Task> tasks) {
            final PendingIntent intent = getPendingIntent(tasks);
            alarmManager.set(AlarmManager.RTC_WAKEUP, time, intent);
            Log.d(TAG, "trigger new alarm on " + new Date(time).toString());
            return intent;
        }

        public synchronized void clear() {
            final Set<Long> current = new HashSet<>(activeReminders.keySet());
            for (final Long key : current) {
                if (activeReminders.containsKey(key)) {
                    removeReminderFor(activeReminders.get(key).second);
                }
            }
        }

        @Override
        public void handleChange() {
            updateAlarms();
            NotificationService.updateServices(ctx);
            rebuildNotifications();
        }

        @Override
        public synchronized void handleChange(final long id) {
            updateNow();
            NotificationService.updateServices(ctx);
            final Optional<Task> t = Task.get(id);
            if (t.isPresent()) {
                final Task task = t.get();
                if (task.isDone()) {
                    removeReminderFor(task);
                } else if (task.getReminder().isPresent()) {
                    if (activeReminders.containsKey(task.getId())) {
                        updateReminderFor(task);
                    } else {
                        createReminderFor(task);
                    }
                    if (!task.equals(currentNotifications.get(task.getId()))) {
                        sendNotificantion(task, false);
                    }
                } else {
                    removeReminderFor(task);
                }
            }

        }

        private void updateNow() {
            now = new DateTime();
            now.minusSeconds(10);
        }

        public void destroy() {
            if (observer.isPresent()) {
                observer.get().unregister(ctx);
                observer = absent();
            }
        }

        public void rebuildNotifications() {
            final Set<Long> current = new HashSet<>(currentNotifications.keySet());
            for (final Long e : current) {
                final Optional<Task> task = Task.get(e);
                if (task.isPresent()) {
                    if (!task.get().isDone() &&
                        task.get().getReminder().isPresent()
                        && task.get().getReminder().get().isBefore(new DateTime())) {
                        sendNotificantion(task.get(), false);
                    }
                } else {
                    closeNotificationForTaskId(e);
                }
            }
        }
    }
}
