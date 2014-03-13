package de.azapps.mirakel.services;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;
import de.azapps.mirakel.DefinitionsHelper;
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
	public IBinder onBind(final Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	private void handleCommand(final Intent intent) {
		final Task task = TaskHelper.getTaskFromIntent(intent);
		if (task == null) {
			return;
		}
		if (intent.getAction() == TASK_DONE) {
			task.setDone(true);
			task.safeSave();
			Toast.makeText(this,
					getString(R.string.reminder_notification_done_confirm),
					Toast.LENGTH_LONG).show();
		} else if (intent.getAction() == TASK_LATER
				&& !task.hasRecurringReminder()) {
			final GregorianCalendar reminder = new GregorianCalendar();
			final int addMinutes = MirakelCommonPreferences.getAlarmLater();
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
		final Intent i = new Intent(DefinitionsHelper.SYNC_FINISHED);
		sendBroadcast(i);
		stopSelf();
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startid) {
		handleCommand(intent);
		return START_STICKY;
	}

}
