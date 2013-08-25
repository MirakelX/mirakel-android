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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.main_activity.MainActivity;
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
	private static CharSequence last_message=null;
	private Context mContext;

	public enum SYNC_TYPES {
		MIRAKEL, TASKWARRIOR
	};

	public static SYNC_TYPES getSyncType(String type) {
		if (type.equals("Mirakel")) {
			return SYNC_TYPES.MIRAKEL;
		} else if (type.equals("Taskwarrior")) {
			return SYNC_TYPES.TASKWARRIOR;
		} else
			return null;
	}

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		Log.v(TAG, "SyncAdapter");
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
				last_message = mContext.getText(R.string.message_account_suspended);
				break;
			case CANNOT_PARSE_MESSAGE:
				last_message = mContext.getText(R.string.message_parse_message);
				break;
			case MESSAGE_ERRORS:
				last_message = mContext.getText(R.string.message_message_error);
				break;
			}			
		} else {
			Log.wtf(TAG, "Unknown SyncType");
		}
	}
	
	public static CharSequence getLastMessage() {
		CharSequence tmp=last_message;
		last_message=null;
		return tmp;
	}
}
