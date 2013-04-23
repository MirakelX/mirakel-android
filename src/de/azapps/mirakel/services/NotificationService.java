package de.azapps.mirakel.services;

import java.util.List;

import de.azapps.mirakel.List_mirakle;
import de.azapps.mirakel.ListsDataSource;
import de.azapps.mirakel.MainActivity;
import de.azapps.mirakel.MainWidgetProvider;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.Task;
import de.azapps.mirakel.TasksDataSource;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

public class NotificationService extends Service {
	private SharedPreferences preferences;
	private TasksDataSource taskDataSource;
	private ListsDataSource listDataSource;
	public static NotificationService notificationService;

	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public void onCreate() {
		preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		listDataSource = new ListsDataSource(getApplicationContext());
		listDataSource.open();
		taskDataSource = new TasksDataSource(getApplicationContext());
		taskDataSource.open();
		notifier();
		NotificationService.setNotificationService(this);
	}

	/**
	 * Updates the Notification
	 */
	public void notifier() {
		int listId = Integer.parseInt(preferences.getString(
				"notificationsList", "" + Mirakel.LIST_DAILY));
		// Set onClick Intent
		Intent intent = new Intent(this, MainActivity.class);
		intent.setAction(MainActivity.SHOW_LIST);
		intent.putExtra(MainActivity.EXTRA_ID, listId);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

		// Get the data
		List_mirakle todayList = listDataSource.getList(listId);
		List<Task> todayTasks = taskDataSource.getTasks(todayList,
				todayList.getSortBy());
		String notificationTitle;
		String notificationText;
		if (todayTasks.size() == 0) {
			notificationTitle = getString(R.string.notification_title_empty);
			notificationText = "";
		} else {
			switch (listId) {
			case Mirakel.LIST_ALL:
				if (todayTasks.size() == 1)
					notificationTitle = getString(R.string.notification_title_all_single);
				else
					notificationTitle = String.format(
							getString(R.string.notification_title_all),
							todayTasks.size());
				break;
			case Mirakel.LIST_DAILY:
				if (todayTasks.size() == 1)
					notificationTitle = getString(R.string.notification_title_daily_single);
				else
					notificationTitle = String.format(
							getString(R.string.notification_title_daily),
							todayTasks.size());
				break;
			case Mirakel.LIST_WEEKLY:
				if (todayTasks.size() == 1)
					notificationTitle = getString(R.string.notification_title_weekly_single);
				else
					notificationTitle = String.format(
							getString(R.string.notification_title_weekly),
							todayTasks.size());
				break;
			default:
				if (todayTasks.size() == 1)
					notificationTitle = getString(R.string.notification_title_general_single);
				else
					notificationTitle = String.format(
							getString(R.string.notification_title_general),
							todayTasks.size(), todayList.getName());

			}
			notificationText = todayTasks.get(0).getName();
		}

		boolean persistent = preferences.getBoolean("notificationsPersistent",
				true);
		// Build notification
		NotificationCompat.Builder noti = new NotificationCompat.Builder(this)
				.setContentTitle(notificationTitle)
				.setContentText(notificationText)
				.setSmallIcon(R.drawable.ic_launcher).setContentIntent(pIntent)
				.setOngoing(persistent);

		// Big View
		if (preferences.getBoolean("notificationsBig", true)) {
			NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
			for (Task task : todayTasks) {
				inboxStyle.addLine(task.getName());
			}
			noti.setStyle(inboxStyle);
		}

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(0, noti.build());
	}

	/**
	 * Set the NotificationService
	 * 
	 * @param service
	 */
	private static void setNotificationService(NotificationService service) {
		if (NotificationService.notificationService == null)
			NotificationService.notificationService = service;
	}

	/**
	 * Update the Mirakel–Notifications
	 * 
	 * @param context
	 */
	public static void updateNotificationAndWidget(Context context) {

		Intent widgetIntent = new Intent(context, MainWidgetProvider.class);
		widgetIntent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		// Use an array and EXTRA_APPWIDGET_IDS instead of
		// AppWidgetManager.EXTRA_APPWIDGET_ID,
		// since it seems the onUpdate() is only fired on that:
		widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
				Mirakel.widgets);
		context.sendBroadcast(widgetIntent);
		if (NotificationService.notificationService == null) {
			Intent intent = new Intent(context, NotificationService.class);
			context.startService(intent);
		} else {
			NotificationService.notificationService.notifier();
		}
	}

}
