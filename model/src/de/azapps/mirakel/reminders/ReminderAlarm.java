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
package de.azapps.mirakel.reminders;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import android.support.v4.app.NotificationCompat;
import android.util.Pair;
import android.widget.Toast;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakel.services.TaskService;
import de.azapps.tools.Log;

public class ReminderAlarm extends BroadcastReceiver {
	private static final String TAG = "ReminderAlarm";
	public static final String UPDATE_NOTIFICATION = "de.azapps.mirakel.reminders.ReminderAlarm.UPDATE_NOTIFICATION";
	public static final String SHOW_TASK = "de.azapps.mirakel.reminders.ReminderAlarm.SHOW_TASK";
	public static final String EXTRA_ID = "de.azapps.mirakel.reminders.ReminderAlarm.EXTRA_ID";
	private static Set<Long> allReminders = new HashSet<Long>();

	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (intent.getAction().equals(UPDATE_NOTIFICATION)) {
			NotificationService.updateNotificationAndWidget(context);
		}
		if (!intent.getAction().equals(SHOW_TASK)) {
			return;
		}

		final long taskId = intent.getLongExtra(EXTRA_ID, 0);
		if (taskId == 0) {
			return;
		}

		final Task task = Task.get(taskId);
		if (task == null) {
			Toast.makeText(context, R.string.task_vanished, Toast.LENGTH_LONG)
					.show();
			return;
		}
		createNotification(context, task);
		// updateAlarms(context);
	}

	private static void createNotification(final Context context,
			final Task task) {
		Log.w(TAG, task.getName());
		final NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		Intent openIntent;
		try {
			openIntent = new Intent(context,
					Class.forName(DefinitionsHelper.MAINACTIVITY_CLASS));
		} catch (final ClassNotFoundException e) {
			Log.wtf(TAG, "mainactivtity not found");
			return;
		}
		openIntent.setAction(DefinitionsHelper.SHOW_TASK);
		openIntent.putExtra(DefinitionsHelper.EXTRA_ID, task.getId());
		openIntent
				.setData(Uri.parse(openIntent.toUri(Intent.URI_INTENT_SCHEME)));
		final PendingIntent pOpenIntent = PendingIntent.getActivity(context, 0,
				openIntent, 0);

		final Intent doneIntent = new Intent(context, TaskService.class);
		doneIntent.setAction(TaskService.TASK_DONE);
		doneIntent.putExtra(DefinitionsHelper.EXTRA_ID, task.getId());
		doneIntent
				.setData(Uri.parse(doneIntent.toUri(Intent.URI_INTENT_SCHEME)));

		final PendingIntent pDoneIntent = PendingIntent.getService(context, 0,
				doneIntent, 0);

		final Intent laterIntent = new Intent(context, TaskService.class);
		laterIntent.setAction(TaskService.TASK_LATER);
		laterIntent.putExtra(DefinitionsHelper.EXTRA_ID, task.getId());
		laterIntent.setData(Uri.parse(laterIntent
				.toUri(Intent.URI_INTENT_SCHEME)));
		final PendingIntent pLaterIntent = PendingIntent.getService(context, 0,
				laterIntent, 0);

		final boolean persistent = MirakelCommonPreferences
				.usePersistentReminders();

		// Build Notification

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(
				context);

		final int icon = R.drawable.mirakel;
		builder.setContentTitle(
				context.getString(R.string.reminder_notification_title,
						task.getName()))
				.setContentText(task.getContent())
				.setSmallIcon(icon)
				.setContentIntent(pOpenIntent)
				.setLights(Color.BLUE, 1500, 300)
				.setOngoing(persistent)
				.setDefaults(Notification.DEFAULT_VIBRATE)
				.setSound(
						RingtoneManager
								.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
				.addAction(R.drawable.ic_checkmark_holo_light,
						context.getString(R.string.reminder_notification_done),
						pDoneIntent)
				.addAction(
						android.R.drawable.ic_menu_close_clear_cancel,
						context.getString(R.string.reminder_notification_later),
						pLaterIntent);

		if (MirakelCommonPreferences.useBigNotifications()) {
			final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

			final String priority = ""
					+ (task.getPriority() > 0 ? "+" + task.getPriority() : task
							.getPriority());
			CharSequence due;
			if (task.getDue() == null) {
				due = context.getString(R.string.no_date);
			} else {
				due = DateTimeHelper.formatDate(context, task.getDue());
			}

			inboxStyle.addLine(context.getString(
					R.string.reminder_notification_list, task.getList()
							.getName()));
			inboxStyle.addLine(context.getString(
					R.string.reminder_notification_priority, priority));
			inboxStyle.addLine(context.getString(
					R.string.reminder_notification_due, due));
			builder.setStyle(inboxStyle);
		}

		// Build notification
		allReminders.add(task.getId());
		nm.notify(DefinitionsHelper.NOTIF_REMINDER + (int) task.getId(),
				builder.build());
	}

	private static AlarmManager alarmManager;

	private static List<Pair<Task, PendingIntent>> activeAlarms = new ArrayList<Pair<Task, PendingIntent>>();

	public static void updateAlarms(final Context ctx) {

		new Thread(new Runnable() {
			@Override
			public void run() {
				Log.e(TAG, "update");
				alarmManager = (AlarmManager) ctx
						.getSystemService(Context.ALARM_SERVICE);

				// Update the Notifications at midnight
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

				// Alarms
				final List<Task> tasks = Task.getTasksWithReminders();
				for (int i = 0; i < activeAlarms.size(); i++) {
					final Pair<Task, PendingIntent> p = activeAlarms.get(i);
					final Task t = p.first;
					if (t == null) {
						cancelAlarm(ctx, t, null, p, p.second);
						continue;
					}
					final Task newTask = Task.get(t.getId());
					if (newTask == null
							|| newTask.getReminder() == null
							|| newTask.isDone()
							|| newTask.getReminder().after(
									new GregorianCalendar())) {
						cancelAlarm(ctx, t, newTask, p, p.second);
						continue;
					} else if (newTask.getReminder() != null) {
						final Calendar now = new GregorianCalendar();
						if (newTask.getReminder().after(now)
								&& newTask.getRecurringReminder() == null) {
							closeNotificationFor(ctx, t.getId());
							updateAlarm(ctx, newTask);
						} else if (newTask.getReminder().after(now)
								&& newTask.getRecurringReminder() != null
								&& newTask.getReminder().compareTo(
										newTask.getRecurringReminder()
												.addRecurring(
														newTask.getReminder())) > 0
								&& !now.after(newTask.getReminder())) {
							updateAlarm(ctx, newTask);
						} else if (t.getRecurringReminderId() != newTask
								.getRecurringReminderId()) {
							updateAlarm(ctx, newTask);
							cancelAlarm(ctx, t, newTask, p, p.second);
						}
					}
				}
				for (final Task t : tasks) {
					if (!isAlarm(t)) {
						Log.d(TAG, "add: " + t.getName());
						final PendingIntent p = updateAlarm(ctx, t);
						activeAlarms.add(new Pair<Task, PendingIntent>(t, p));
					}
				}
			}
		}).start();
	}

	protected static void reloadNotification(final Context ctx, final Task t) {
		closeNotificationFor(ctx, t.getId());
		createNotification(ctx, t);
	}

	private static boolean isAlarm(final Task t2) {
		for (int i = 0; i < activeAlarms.size(); i++) {
			final Task t = activeAlarms.get(i).first;
			if (t.getId() == t2.getId()) {
				return true;
			}
		}
		return false;
	}

	private static void cancelAlarm(final Context ctx, final Task t,
			final Task newTask, final Pair<Task, PendingIntent> p,
			final PendingIntent pendingIntent) {
		activeAlarms.remove(p);
		closeNotificationFor(ctx, t.getId());
		if (newTask == null) {
			return;
		}
		alarmManager.cancel(pendingIntent);
	}

	public static void cancelAlarm(final Context ctx, final Task task) {
		try {
			final Pair<Task, PendingIntent> p = findTask(task);
			cancelAlarm(ctx, task, Task.get(task.getId()), p, p.second);
		} catch (final IndexOutOfBoundsException e) {
			Log.d(TAG, "task not found");
		}
	}

	private static Pair<Task, PendingIntent> findTask(final Task task) {
		for (final Pair<Task, PendingIntent> p : activeAlarms) {
			if (task.getId() == p.first.getId()) {
				return p;
			}
		}
		throw new IndexOutOfBoundsException();
	}

	private static PendingIntent updateAlarm(final Context ctx, final Task task) {
		final Intent intent = new Intent(ctx, ReminderAlarm.class);
		intent.setAction(SHOW_TASK);
		intent.putExtra(EXTRA_ID, task.getId());
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
		final PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		if (pendingIntent == null || task.getReminder() == null) {
			return null;
		}
		Log.v(TAG, "Set alarm for " + task.getName() + " on "
				+ task.getReminder().getTimeInMillis());
		final Recurring recurrence = task.getRecurringReminder();
		if (recurrence == null) {
			alarmManager.set(AlarmManager.RTC_WAKEUP, task.getReminder()
					.getTimeInMillis(), pendingIntent);
		} else {
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, task
					.getReminder().getTimeInMillis(),
					recurrence.getInterval(), pendingIntent);

		}
		return pendingIntent;
	}

	public static void closeNotificationFor(final Context context,
			final Long taskId) {
		final NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(DefinitionsHelper.NOTIF_REMINDER + taskId.intValue());
		allReminders.remove(taskId);
	}

	public static void stopAll(final Context context) {
		final NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		// This hack is a must because otherwise we get a
		// concurrentModificationException
		final Long[] reminders = allReminders.toArray(new Long[] {});
		for (final Long id : reminders) {
			nm.cancel(DefinitionsHelper.NOTIF_REMINDER + id.intValue());
			cancelAlarm(context, Task.get(id));
		}
	}
}
