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
package de.azapps.mirakel.reminders;


import java.util.ArrayList;
import java.util.Calendar;import java.util.GregorianCalendar;
import java.util.List;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakelandroid.R;

public class ReminderAlarm extends BroadcastReceiver {
	private static final String TAG = "ReminderAlarm";
	public static String UPDATE_NOTIFICATION = "de.azapps.mirakel.reminders.ReminderAlarm.UPDATE_NOTIFICATION";
	public static String SHOW_TASK = "de.azapps.mirakel.reminders.ReminderAlarm.SHOW_TASK";
	public static String EXTRA_ID = "de.azapps.mirakel.reminders.ReminderAlarm.EXTRA_ID";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(UPDATE_NOTIFICATION)) {
			NotificationService.updateNotificationAndWidget(context);
		}
		if (!intent.getAction().equals(SHOW_TASK)) {
			return;
		}

		long taskId = intent.getLongExtra(EXTRA_ID, 0);
		if (taskId == 0)
			return;

		Task task = Task.get(taskId);
		if (task == null) {
			Toast.makeText(context, R.string.task_vanished, Toast.LENGTH_LONG)
					.show();
			return;
		}
		if (task.getRecurringReminder() != null&&task.getReminder().compareTo(
				task.getRecurringReminder()
				.addRecurring(
						(Calendar) task.getReminder().clone()))<1) {
			task.setReminder(task.getRecurringReminder().addRecurring(
					task.getReminder()));
			alarmManager.set(AlarmManager.RTC_WAKEUP, task.getReminder()
					.getTimeInMillis(), PendingIntent.getBroadcast(context, 0,
							intent, PendingIntent.FLAG_UPDATE_CURRENT));
			try {
				task.save(false);
			} catch (NoSuchListException e) {
				Log.d(TAG, "cannot save Task");
			}
		}
		createNotification(context, task);
//		updateAlarms(context);
	}

	private static void createNotification(Context context, Task task) {
		Log.w(TAG, task.getName());
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(context);

		Intent openIntent = new Intent(context, MainActivity.class);
		openIntent.setAction(MainActivity.SHOW_TASK);
		openIntent.putExtra(MainActivity.EXTRA_ID, task.getId());
		openIntent
				.setData(Uri.parse(openIntent.toUri(Intent.URI_INTENT_SCHEME)));
		PendingIntent pOpenIntent = PendingIntent.getActivity(context, 0,
				openIntent, 0);

		Intent doneIntent = new Intent(context, MainActivity.class);
		doneIntent.setAction(MainActivity.TASK_DONE);
		doneIntent.putExtra(MainActivity.EXTRA_ID, task.getId());
		doneIntent
				.setData(Uri.parse(doneIntent.toUri(Intent.URI_INTENT_SCHEME)));
		PendingIntent pDoneIntent = PendingIntent.getActivity(context, 0,
				doneIntent, 0);

		Intent laterIntent = new Intent(context, MainActivity.class);
		laterIntent.setAction(MainActivity.TASK_LATER);
		laterIntent.putExtra(MainActivity.EXTRA_ID, task.getId());
		laterIntent.setData(Uri.parse(laterIntent
				.toUri(Intent.URI_INTENT_SCHEME)));
		PendingIntent pLaterIntent = PendingIntent.getActivity(context, 0,
				laterIntent, 0);

		boolean persistent = preferences
				.getBoolean("remindersPersistent", true);

		// Build Notification

		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				context);

		int icon = R.drawable.mirakel;
		if (preferences.getBoolean("oldLogo", false)) {
			icon = R.drawable.ic_launcher;
		}
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
				.addAction(icon,
						context.getString(R.string.reminder_notification_done),
						pDoneIntent)
				.addAction(
						icon,
						context.getString(R.string.reminder_notification_later),
						pLaterIntent);

		if (preferences.getBoolean("notificationsBig", true)) {
			NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

			String priority = ""
					+ (task.getPriority() > 0 ? "+" + task.getPriority() : task
							.getPriority());
			CharSequence due;
			if (task.getDue() == null) {
				due = context.getString(R.string.no_date);
			} else {
				due = Helpers.formatDate(context, task.getDue());
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
		nm.notify(Mirakel.NOTIF_REMINDER + ((int) task.getId()),
				builder.build());
	}

	private static AlarmManager alarmManager;

	private static List<Task> activeAlarms = new ArrayList<Task>();

	public static void updateAlarms(final Context ctx) {

		new Thread(new Runnable() {
			@Override
			public void run() {
				Log.e(TAG, "update");
				alarmManager = (AlarmManager) ctx
						.getSystemService(Context.ALARM_SERVICE);

				// Update the Notifications ad midnight
				Intent intent = new Intent(ctx, ReminderAlarm.class);
				intent.setAction(UPDATE_NOTIFICATION);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx,
						0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				Calendar triggerCal = new GregorianCalendar();
				triggerCal.set(Calendar.HOUR_OF_DAY, 0);
				triggerCal.set(Calendar.MINUTE, 0);
				triggerCal.add(Calendar.DAY_OF_MONTH, 1);

				alarmManager.setRepeating(AlarmManager.RTC,
						triggerCal.getTimeInMillis(),
						AlarmManager.INTERVAL_DAY, pendingIntent);

				// Alarms
				List<Task> tasks = Task.getTasksWithReminders();
				for (int i = 0; i < activeAlarms.size(); i++) {
					Task t = activeAlarms.get(i);
					if (t == null) {
						i = cancelAlarm(ctx, t, null, i);
						continue;
					}
					Task newTask = Task.get(t.getId());
					if (newTask == null || newTask.getReminder() == null
							|| newTask.isDone()) {
						i = cancelAlarm(ctx, t, newTask, i);
						continue;
					} else if (newTask.getReminder() != null) {
						Calendar now = new GregorianCalendar();
						if (newTask.getReminder().after(now)&&newTask.getRecurringReminder()==null) {
							closeNotificationFor(ctx, t.getId());
							updateAlarm(ctx, newTask);
						} else if (newTask.getReminder().after(now)
								&& newTask.getRecurringReminder() != null
								&& newTask.getReminder().compareTo(
										newTask.getRecurringReminder()
												.addRecurring(
														newTask.getReminder()))>0
								&& !now.after(newTask.getReminder())) {
							updateAlarm(ctx, newTask);
						}
					}
				}
				for (Task t : tasks) {
					if (!isAlarm(t)) {
						updateAlarm(ctx, t);
						activeAlarms.add(t);
					}
				}
			}
		}).start();
	}

	protected static void reloadNotification(Context ctx, Task t) {
		closeNotificationFor(ctx, t.getId());
		createNotification(ctx, t);
	}

	private static boolean isAlarm(Task t2) {
		for (Task t : activeAlarms) {
			if (t.getId() == t2.getId())
				return true;
		}
		return false;
	}

	private static int cancelAlarm(Context ctx, Task t, Task newTask, int i) {
		activeAlarms.remove(i--);
		closeNotificationFor(ctx, t.getId());
		if (newTask == null) {
			return i;
		}
		Intent intent = new Intent(ctx, ReminderAlarm.class);
		intent.setAction(SHOW_TASK);
		intent.putExtra(EXTRA_ID, t.getId());
		PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
		alarmManager.cancel(pendingIntent);
		return i;
	}

	private static void updateAlarm(Context ctx, Task task) {
		Intent intent = new Intent(ctx, ReminderAlarm.class);
		intent.setAction(SHOW_TASK);
		intent.putExtra(EXTRA_ID, task.getId());
		PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		if (pendingIntent == null || task.getReminder() == null)
			return;
		alarmManager.set(AlarmManager.RTC_WAKEUP, task.getReminder()
				.getTimeInMillis(), pendingIntent);
	}

	public static void closeNotificationFor(Context context, Long taskId) {
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(Mirakel.NOTIF_REMINDER + taskId.intValue());
	}

}
