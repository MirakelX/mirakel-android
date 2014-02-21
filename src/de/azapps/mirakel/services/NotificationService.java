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
package de.azapps.mirakel.services;

import java.util.List;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class NotificationService extends Service {
	private static final String TAG = "NotificationService";
	private boolean						existsNotification	= false;
	public static NotificationService	notificationService;

	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public void onCreate() {
		notifier();
		NotificationService.setNotificationService(this);
	}

	@Override
	public void onDestroy() {
		// Do nothing
	}

	public static void stop(Context ctx) {
		NotificationManager notificationManager = (NotificationManager) ctx
				.getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(DefinitionsHelper.NOTIF_DEFAULT);
	}

	/**
	 * Updates the Notification
	 */
	public void notifier() {
		if (!MirakelCommonPreferences.useNotifications() && !existsNotification) {
			return;
		}
		int listId = MirakelCommonPreferences.getNotificationsListId();
		int listIdToOpen = MirakelCommonPreferences.getNotificationsListOpenId();
		// Set onClick Intent
		
		Intent openIntent;
		try {
			openIntent = new Intent(this, Class.forName(DefinitionsHelper.MAINACTIVITY_CLASS));
		} catch (ClassNotFoundException e) {
			Log.wtf(TAG,"mainactivity not found");
			return;
		}
		openIntent.setAction(DefinitionsHelper.MAIN_SHOW_LIST);
		openIntent.putExtra(DefinitionsHelper.MAIN_EXTRA_ID, listIdToOpen);
		openIntent
				.setData(Uri.parse(openIntent.toUri(Intent.URI_INTENT_SCHEME)));
		PendingIntent pOpenIntent = PendingIntent.getActivity(this, 0,
				openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Get the data
		ListMirakel todayList = ListMirakel.getList(listId);
		if (todayList == null) return;
		List<Task> todayTasks = todayList.tasks(false);
		String notificationTitle;
		String notificationText;
		if (todayTasks.size() == 0) {
			notificationTitle = getString(R.string.notification_title_empty);
			notificationText = "";
		} else {
			if (todayTasks.size() == 1) notificationTitle = getString(
					R.string.notification_title_general_single,
					todayList.getName());
			else notificationTitle = String.format(
					getString(R.string.notification_title_general),
					todayTasks.size(), todayList.getName());

			notificationText = todayTasks.get(0).getName();
		}

		boolean persistent = MirakelCommonPreferences.usePersistentNotifications();

		int icon = R.drawable.mirakel;
		// Build notification
		NotificationCompat.Builder noti = new NotificationCompat.Builder(this)
				.setContentTitle(notificationTitle)
				.setContentText(notificationText).setSmallIcon(icon)
				.setContentIntent(pOpenIntent).setOngoing(persistent);

		// Big View
		if (MirakelCommonPreferences.useBigNotifications() && todayTasks.size() > 1
				&& VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
			NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
			for (Task task : todayTasks) {
				inboxStyle.addLine(task.getName());
			}
			noti.setStyle(inboxStyle);
		}

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(DefinitionsHelper.NOTIF_DEFAULT, noti.build());
		if ((MirakelCommonPreferences.hideEmptyNotifications() && todayTasks.size() == 0)
				|| !MirakelCommonPreferences.useNotifications()) {
			notificationManager.cancel(DefinitionsHelper.NOTIF_DEFAULT);
			existsNotification = false;
		} else {
			existsNotification = true;
		}
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
		// Widget update
		Intent widgetIntent;
		try {
			widgetIntent = new Intent(context, Class.forName(DefinitionsHelper.MAINWIDGET_CLASS));
		} catch (ClassNotFoundException e) {
			Log.wtf(TAG, "widget not found");
			return;
		}
		widgetIntent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
				DefinitionsHelper.widgets);
		context.sendBroadcast(widgetIntent);

		// Dashclock update
		Intent dashclockIntent = new Intent();
		dashclockIntent.setAction("de.azapps.mirakel.dashclock.UPDATE");
		context.sendBroadcast(dashclockIntent);

		if (NotificationService.notificationService == null) {
			Intent intent = new Intent(context, NotificationService.class);
			context.startService(intent);
		} else {
			NotificationService.notificationService.notifier();
		}
	}
}
