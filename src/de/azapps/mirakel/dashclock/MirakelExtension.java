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
package de.azapps.mirakel.dashclock;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.dmfs.provider.tasks.TaskContract;
import org.dmfs.provider.tasks.TaskContract.Tasks;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

public class MirakelExtension extends DashClockExtension {

	private static final String TAG = "MirakelExtension";
	private static boolean initialized = false;
	private SharedPreferences settings;
	private static MirakelExtension singleton;
    private static final String MIRAKEL_AUTHORITY = "de.azapps.mirakel.provider.internal";
    private static final Uri contentUri = Uri.parse("content://" + MIRAKEL_AUTHORITY
            + "/tasks");

	@Override
	protected void onUpdateData(int reason) {
		singleton = this;
		if (settings == null)
			settings = PreferenceManager.getDefaultSharedPreferences(this);
		// Get values from Settings
		int list_id = Integer.parseInt(settings.getString("startupList", "-1"));
		int maxTasks = settings.getInt("showTaskNumber", 1);
		// Get Tasks
		String[] col = { Tasks.TITLE, Tasks.PRIORITY, Tasks.DUE };
		Cursor c = null;

		try {
			c = getContentResolver()
					.query(contentUri,
							col,
							Tasks.LIST_ID + " =?" + " and "
									+ TaskContract.Tasks.STATUS + " = "
									+ TaskContract.Tasks.STATUS_NEEDS_ACTION,
							new String[] { "" + list_id },
							Tasks.PRIORITY
									+ " desc, case when ("
									+ Tasks.DUE
									+ " is NULL) then date('now','+1000 years') else date("
									+ Tasks.DUE + ") end asc");
		} catch (SecurityException e) {
			Notification notif = new NotificationCompat.Builder(this)
					.setContentText(getString(R.string.no_permission))
					.setContentTitle(getString(R.string.no_permission_title))
					.setSmallIcon(R.drawable.mirakel).build();
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.notify(0, notif);
			return;
		} catch (Exception e) {
			Log.e(TAG, "Cannot communicate to Mirakel");
			Log.w(TAG, Log.getStackTraceString(e));
			return;
		}
		c.moveToFirst();
		// Set Status
		String status = getResources().getQuantityString(R.plurals.status,
				c.getCount(), c.getCount());
		if (c.getCount() == 0) {
			if (!settings.getBoolean("showEmpty", true)) {
				Log.d(TAG, "hide");
				publishUpdate(new ExtensionData().visible(false));
				return;
			}
		}
		// Set Body
		String expBody = "";
		if (c.getCount() > 0) {
			SimpleDateFormat out = new SimpleDateFormat(
					getString(R.string.due_outformat), Locale.getDefault());
			int counter = 0;
			while (!c.isAfterLast() && counter < maxTasks) {

				Date t = null;
				if (!c.isNull(2)) {
					try {
						t = new Date(c.getLong(2));
					} catch (NullPointerException e) {
						// Nothing
					}
				}
				if (t != null && settings.getBoolean("showDueDate", true)) {
					expBody += getString(R.string.due, c.getString(0),
							out.format(t));
				} else {
					expBody += c.getString(0);
				}
				c.moveToNext();
				if (counter < maxTasks - 1 && !c.isAfterLast())
					expBody += "\n";
				++counter;
			}
		}
		// Add click-event
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setComponent(new ComponentName("de.azapps.mirakelandroid",
				"de.azapps.mirakel.main_activity.MainActivity"));
		intent.setAction("de.azapps.mirakel.SHOW_LIST");
		intent.putExtra("de.azapps.mirakel.EXTRA_TASKID", list_id);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		// Set Content
		publishUpdate(new ExtensionData().visible(true)
				.icon(R.drawable.bw_mirakel).status(status)
				.expandedBody(expBody).clickIntent(intent));
		initialized = true;
	}

	public static boolean isInitialized() {
		return initialized;
	}

	public static void updateWidget() {
		if (singleton == null)
			return;
		singleton.onUpdateData(UPDATE_REASON_CONTENT_CHANGED);
	}
}
