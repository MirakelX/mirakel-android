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

import android.app.Notification;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.sync.caldav.CalDavSync;
import de.azapps.mirakel.sync.mirakel.MirakelSync;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSync;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSync.TW_ERRORS;
import de.azapps.mirakelandroid.R;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private final static String TAG = "SyncAdapter";
	public static final String BUNDLE_SERVER_URL = "url",
			BUNDLE_CERT = "de.azapps.mirakel.cert",
			BUNDLE_ORG = "de.azapps.mirakel.org";
	public static final String BUNDLE_SERVER_TYPE = "type";
	public static final String TASKWARRIOR_KEY = "key";
	public static final String SYNC_STATE = "sync_state";
	private static CharSequence last_message = null;
	private Context mContext;
	public static final String ACCOUNT_PREFIX = "ACCOUNT_";
    private NotificationManager mNotificationManager;
    private int notifyID = 1;


	

	public enum SYNC_STATE {
		NOTHING, DELETE, ADD, NEED_SYNC, IS_SYNCED;
		@Override
		public String toString() {
			return "" + toInt();
		}

		public short toInt() {
			switch (this) {
			case ADD:
				return 1;
			case DELETE:
				return -1;
			case IS_SYNCED:
				return 3;
			case NEED_SYNC:
				return 2;
			case NOTHING:
				return 0;
			default:
				return 0;
			}
		}

		public static SYNC_STATE parseInt(int i) {
			switch (i) {
			case -1:
				return DELETE;
			case 1:
				return ADD;
			case 2:
				return NEED_SYNC;
			case 3:
				return IS_SYNCED;
			case 0:
			default:
				return NOTHING;
			}
		}
	}

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		Log.v(TAG, "SyncAdapter");
        int icon = android.R.drawable.stat_notify_sync;//R.drawable.mirakel
        NotificationCompat.Builder mNB = new NotificationCompat.Builder(mContext)
          .setContentTitle("Mirakel")
          .setContentText("Sync")
          .setSmallIcon(icon);

        mNotificationManager.notify(notifyID, mNB.build());

		String type = (AccountManager.get(mContext)).getUserData(account,
				BUNDLE_SERVER_TYPE);
		if (type == null)
			type = MirakelSync.TYPE;
		if (type.equals(MirakelSync.TYPE)) {
			new MirakelSync(mContext).sync(account);
		} else if (type.equals(TaskWarriorSync.TYPE)) {
			TW_ERRORS error = new TaskWarriorSync(mContext).sync(account);
			switch (error) {
			case NO_ERROR:
				last_message = mContext.getText(R.string.finish_sync);
				break;
			case TRY_LATER:
				last_message = mContext.getText(R.string.message_try_later);
				break;
			case ACCESS_DENIED:
				last_message = mContext.getText(R.string.message_access_denied);
				break;
			case CANNOT_CREATE_SOCKET:
				last_message = mContext.getText(R.string.message_create_socket);
				break;
			case ACCOUNT_SUSPENDED:
				last_message = mContext
						.getText(R.string.message_account_suspended);
				break;
			case CANNOT_PARSE_MESSAGE:
				last_message = mContext.getText(R.string.message_parse_message);
				break;
			case MESSAGE_ERRORS:
				last_message = mContext.getText(R.string.message_message_error);
				break;
			case CONFIG_PARSE_ERROR:
				last_message = mContext.getText(R.string.wrong_config);
				break;
				
			}
			Looper.prepare();
			Toast.makeText(mContext, last_message, Toast.LENGTH_LONG).show();
			Log.d(TAG, "finish Sync");
		} else if (type.equals(CalDavSync.TYPE)) {
			new CalDavSync(mContext).sync(account);
		} else {
			Log.wtf(TAG, "Unknown SyncType");
		}
    mNotificationManager.cancelAll();
	}

	public static CharSequence getLastMessage() {
		CharSequence tmp = last_message;
		last_message = null;
		return tmp;
	}

}
