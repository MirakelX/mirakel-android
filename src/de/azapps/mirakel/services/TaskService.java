package de.azapps.mirakel.services;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;

public class TaskService extends Service {
	public static final String TASK_ID = "de.azapps.mirakel.services.TaskService.TASK_ID",
			ACTION_ID = "de.azapps.mirakel.services.TaskService.ACTION_ID";
	public static final String TASK_DONE = "de.azapps.mirakel.services.TaskService.TASK_DONE",
			TASK_LATER = "de.azapps.mirakel.services.TaskService.TASK_LATER";

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	private void handleCommand(Intent intent) {
		Task task = TaskHelper.getTaskFromIntent(intent);
		if (task == null)
			return;
		if (intent.getAction() == TASK_DONE) {
			task.setDone(true);
			task.safeSave();
			Toast.makeText(this,
					getString(R.string.reminder_notification_done_confirm),
					Toast.LENGTH_LONG).show();
		} else if (intent.getAction() == TASK_LATER
				&& !task.hasRecurringReminder()) {
			GregorianCalendar reminder = new GregorianCalendar();
			int addMinutes = MirakelCommonPreferences.getAlarmLater();
			reminder.add(Calendar.MINUTE, addMinutes);
			task.setReminder(reminder);
			task.safeSave();
			Toast.makeText(
					this,
					getString(R.string.reminder_notification_later_confirm,
							addMinutes), Toast.LENGTH_LONG).show();
		}
		ReminderAlarm.closeNotificationFor(this, task.getId());
		ReminderAlarm.updateAlarms(this);
		stopSelf();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		handleCommand(intent);
		return START_STICKY;
	}

}
