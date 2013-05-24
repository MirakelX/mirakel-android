package de.azapps.mirakel.reminders;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;
import de.azapps.mirakel.R;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.model.task.Task;

public class ReminderHandleActivity extends Activity {
	public static String TASK_DONE = "de.azapps.mirakel.TASK_DONE",
			TASK_LATER = "de.azapps.mirakel.TASK_LATER";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		Intent intent = getIntent();
		Task task = Helpers.getTaskFromIntent(intent);
		if (task == null)
			return;
		if (intent.getAction() == TASK_DONE) {
			task.setDone(true);
			task.save();
			Toast.makeText(this,
					getString(R.string.reminder_notification_done_confirm),
					Toast.LENGTH_LONG).show();

		} else if (intent.getAction() == TASK_LATER) {
			GregorianCalendar reminder = new GregorianCalendar();
			int addMinutes = preferences.getInt("alarm_later", 15);
			reminder.add(Calendar.MINUTE, addMinutes);
			task.setReminder(reminder);
			task.save();
			Toast.makeText(
					this,
					getString(R.string.reminder_notification_later_confirm,
							addMinutes), Toast.LENGTH_LONG).show();

		}
		ReminderAlarm.updateAlarms(this);
		ReminderAlarm.closeNotificationFor(this, task);
		finish();
	}

}
