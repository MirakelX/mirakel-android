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
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Modified by weiznich 2013
 */
package de.azapps.mirakel.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSync;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSync.TW_ERRORS;
import de.azapps.tools.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private final static String	TAG					= "SyncAdapter";
	public static final String	BUNDLE_SERVER_URL	= "url",
			BUNDLE_CERT = "de.azapps.mirakel.cert",
			BUNDLE_ORG = "de.azapps.mirakel.org";
	public static final String	BUNDLE_SERVER_TYPE	= "type";
	public static final String	TASKWARRIOR_KEY		= "key";
	public static final String	SYNC_STATE			= "sync_state";
	private static CharSequence	last_message		= null;
	private Context				mContext;
	public static final String	ACCOUNT_PREFIX		= "ACCOUNT_";
	private NotificationManager	mNotificationManager;
	private int					notifyID			= 1;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		this.mContext = context;
		this.mNotificationManager = (NotificationManager) this.mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		// Mostly it is annoying if there is a notification. So don't show it
		boolean showNotification = false;
		// But if the user actively clicks on "sync" – then show it.
		if (extras.containsKey(ContentResolver.SYNC_EXTRAS_MANUAL))
			showNotification = true;

		Log.v(TAG, "SyncAdapter");
		Intent intent;
		try {
			intent = new Intent(this.mContext, Class.forName(DefinitionsHelper.MAINACTIVITY_CLASS));
		} catch (ClassNotFoundException e) {
			Log.wtf(TAG, "no mainactivity found");
			return;
		}
		intent.setAction(DefinitionsHelper.MAIN_SHOW_LISTS);
		PendingIntent p = PendingIntent.getService(this.mContext, 0, intent, 0);

		NotificationCompat.Builder mNB = new NotificationCompat.Builder(
				this.mContext).setContentTitle("Mirakel").setContentText("Sync")
				.setSmallIcon(android.R.drawable.stat_notify_sync)
				.setWhen(System.currentTimeMillis()).setOngoing(true)
				.setContentIntent(p);
		if (showNotification)
			this.mNotificationManager.notify(this.notifyID, mNB.build());

		String type = (AccountManager.get(this.mContext)).getUserData(account,
				BUNDLE_SERVER_TYPE);
		boolean success = false;
		if (type == null) type = TaskWarriorSync.TYPE;
		if (type.equals(TaskWarriorSync.TYPE)) {
			TW_ERRORS error = new TaskWarriorSync(this.mContext).sync(account);
			switch (error) {
				case NO_ERROR:
					last_message = this.mContext.getText(R.string.finish_sync);
					success = true;
					break;
				case TRY_LATER:
					last_message = this.mContext.getText(R.string.message_try_later);
					break;
				case ACCESS_DENIED:
					last_message = this.mContext
							.getText(R.string.message_access_denied);
					break;
				case CANNOT_CREATE_SOCKET:
					last_message = this.mContext
							.getText(R.string.message_create_socket);
					break;
				case ACCOUNT_SUSPENDED:
					last_message = this.mContext
							.getText(R.string.message_account_suspended);
					break;
				case CANNOT_PARSE_MESSAGE:
					last_message = this.mContext
							.getText(R.string.message_parse_message);
					break;
				case MESSAGE_ERRORS:
					last_message = this.mContext
							.getText(R.string.message_message_error);
					break;
				case CONFIG_PARSE_ERROR:
					last_message = this.mContext.getText(R.string.wrong_config);
					break;
				case NOT_ENABLED:
				default:
					return;

			}
			Log.d(TAG, "finish Sync");
		} else {
			Log.wtf(TAG, "Unknown SyncType");
		}
		this.mNotificationManager.cancel(this.notifyID);
		if (showNotification && !success) {
			mNB = new NotificationCompat.Builder(this.mContext)
					.setContentTitle(
							"Mirakel: "
									+ this.mContext.getText(R.string.finish_sync))
					.setContentText(last_message)
					.setSmallIcon(android.R.drawable.stat_notify_sync)
					.setPriority(NotificationCompat.PRIORITY_LOW)
					.setContentIntent(p);
			this.mNotificationManager.notify(this.notifyID, mNB.build());
		}
		Intent i = new Intent(DefinitionsHelper.SYNC_FINISHED);
		this.mContext.sendBroadcast(i);
	}

	public static CharSequence getLastMessage() {
		CharSequence tmp = last_message;
		last_message = null;
		return tmp;
	}

}
