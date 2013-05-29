package de.azapps.mirakel.reminders;

import java.util.ArrayList;
import java.util.Date;
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
import de.azapps.mirakel.MirakelHelper;
import de.azapps.mirakel.R;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.task.Task;

public class ReminderAlarm extends BroadcastReceiver {
	@SuppressWarnings("unused")
	private static final String TAG = "ReminderAlarm";
	public static String SHOW_TASK = "de.azapps.mirakel.reminders.ReminderAlarm.SHOW_TASK";
	public static String EXTRA_ID = "de.azapps.mirakel.reminders.ReminderAlarm.EXTRA_ID";
	private SharedPreferences preferences;

	NotificationManager nm;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction() != SHOW_TASK) {
			return;
		}

		long taskId = intent.getLongExtra(EXTRA_ID, 0);
		if (taskId == 0)
			return;

		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		Task task = Task.get(taskId);

		Toast.makeText(context, task.getName(), Toast.LENGTH_LONG).show();

		nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

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
		boolean persistent = true;

		// Build Notification

		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				context);

		builder.setContentTitle(
				context.getString(R.string.reminder_notification_title,
						task.getName()))
				.setContentText(task.getContent())
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentIntent(pOpenIntent)
				.setLights(Color.BLUE, 1500, 300)
				.setOngoing(persistent)
				.setDefaults(Notification.DEFAULT_VIBRATE)
				.setSound(
						RingtoneManager
								.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
				.addAction(R.drawable.ic_launcher,
						context.getString(R.string.reminder_notification_done),
						pDoneIntent)
				.addAction(
						R.drawable.ic_launcher,
						context.getString(R.string.reminder_notification_later),
						pLaterIntent);

		if (preferences.getBoolean("notificationsBig", true)) {
			NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

			String priority = ""
					+ (task.getPriority() > 0 ? "+" + task.getPriority() : task
							.getPriority());
			String due;
			if (task.getDue() == null) {
				due = context.getString(R.string.no_date);
			} else {
				due = MirakelHelper.formatDate(task.getDue(),
						context.getString(R.string.dateFormat));
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
	private static List<Task> activeAlarms=new ArrayList<Task>();

	public static void updateAlarms(final Context ctx) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				alarmManager = (AlarmManager) ctx
						.getSystemService(Context.ALARM_SERVICE);
				List<Task> tasks = Task.getTasksWithReminders();
				for(int i=0;i<activeAlarms.size();i++){
					Task t=activeAlarms.get(i);
					Task newTask= Task.get(t.getId());
					if(newTask==null){
						i=cancelAlarm(ctx, t, newTask,i);
						continue;
					}
					if(newTask.isDone()){
						i=cancelAlarm(ctx, t, newTask,i);
					}else if(newTask.getReminder().getTimeInMillis()>new Date().getTime()){
						closeNotificationFor(ctx, t.getId());
						updateAlarm(ctx, newTask);
					}
				}
				for(Task t:tasks){
					if(!isAlarm(t)){
						updateAlarm(ctx, t);
						activeAlarms.add(t);
					}
				}
			}
		}).start();
	}

	private static boolean isAlarm(Task t2) {
		for(Task t:activeAlarms){
			if(t.getId()==t2.getId())
				return true;
		}
		return false;
	}

	private static int cancelAlarm(Context ctx, Task t, Task newTask,int i) {
		closeNotificationFor(ctx, newTask.getId());
		Intent intent = new Intent(ctx, ReminderAlarm.class);
		intent.setAction(SHOW_TASK);
		intent.putExtra(EXTRA_ID, t.getId());
		PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
		alarmManager.cancel(pendingIntent);
		activeAlarms.remove(i--);
		return i;
	}


	private static void updateAlarm(Context ctx, Task task) {
		Intent intent = new Intent(ctx, ReminderAlarm.class);
		intent.setAction(SHOW_TASK);
		intent.putExtra(EXTRA_ID, task.getId());
		PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.set(AlarmManager.RTC_WAKEUP, task.getReminder()
					.getTimeInMillis(), pendingIntent);
	}

	public static void closeNotificationFor(Context context, Long taskId) {
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(Mirakel.NOTIF_REMINDER + taskId.intValue());
	}

}
