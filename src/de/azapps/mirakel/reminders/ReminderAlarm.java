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
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;

public class ReminderAlarm extends BroadcastReceiver {
	public static String SHOW_TASK = "de.azapps.mirakel.reminders.ReminderAlarm.SHOW_TASK";
	public static String EXTRA_ID = "de.azapps.mirakel.reminders.ReminderAlarm.EXTRA_ID";

	NotificationManager nm;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction() != SHOW_TASK) {
			return;
		}

		int taskId = intent.getIntExtra(EXTRA_ID, 0);
		if (taskId == 0)
			return;
		Task task = Task.get(taskId);

		Toast.makeText(context, task.getName(), Toast.LENGTH_LONG).show();
		/*
		 * try { Uri notification = RingtoneManager
		 * .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); Ringtone r =
		 * RingtoneManager.getRingtone(context, notification); r.play(); } catch
		 * (Exception e) { }
		 */

		nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		CharSequence from = "Nithin";
		CharSequence message = "Crazy About Android...";

		Intent myintent = new Intent(context, MainActivity.class);
		myintent.setAction(MainActivity.SHOW_TASK);
		myintent.putExtra(MainActivity.EXTRA_ID, task.getId());
		PendingIntent pIntent = PendingIntent.getActivity(context, 0, myintent, 0);
		boolean persistent = true;
		
		
		//Build Noti
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				context);
		builder.setContentTitle(from)
				.setContentText(message)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentIntent(pIntent)
				.setLights(Color.BLUE, 1000, 300)
				.setOngoing(persistent)
			    .setDefaults(Notification.DEFAULT_VIBRATE)
				// builder.setVibrate(NotificationCompat.Style)
				.setSound(
						RingtoneManager
								.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

		// Build notification
		nm.notify(Mirakel.NOTIF_REMINDER, builder.build());
	}

	private static AlarmManager alarmManager;

	public static void updateAlarms(Context ctx) {
		alarmManager = (AlarmManager) ctx
				.getSystemService(Context.ALARM_SERVICE);

		Intent intent = new Intent(ctx, ReminderAlarm.class);
		intent.setAction(SHOW_TASK);
		intent.putExtra(EXTRA_ID, (int) ListMirakel.first().tasks().get(0)
				.getId());
		PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0,
				intent, PendingIntent.FLAG_ONE_SHOT);
		alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
				+ (5 * 1000), pendingIntent);
	}

}
