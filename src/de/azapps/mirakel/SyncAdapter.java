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
package de.azapps.mirakel;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.  This sample shows a basic 2-way
 * sync between the client and a sample server.  It also contains an
 * example of how to update the contacts' status messages, which
 * would be useful for a messaging or social networking client.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "SyncAdapter";
    private static final String SYNC_MARKER_KEY = Mirakel.ACCOUNT_TYP+".marker";
    private static final boolean NOTIFY_AUTH_FAILURE = true;

    private final AccountManager mAccountManager;

    private final Context mContext;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        mAccountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {
    	TasksDataSource taskDataSource=new TasksDataSource(mContext);
    	ListsDataSource listsDataSource=new ListsDataSource(mContext);
    	taskDataSource.open();
    	listsDataSource.open();
    	Log.d(TAG,"Do sync");
    	try{
    		String password=mAccountManager.getPassword(account); 
    		//TODO get Url from Setings
    		String url=mAccountManager.getUserData(account, "url"); //"http://192.168.10.28:3000";
    		listsDataSource.sync_lists(account.name, password, url);
    		taskDataSource.sync_tasks(account.name, password, url);
    	}catch(ArrayIndexOutOfBoundsException e){
    		Log.e(TAG,"No Account");
    	}catch (SecurityException e) {
			Log.e(TAG, "No Rights");
		}catch (Exception e) {
			Log.e(TAG,e.toString());
			Log.w(TAG,Log.getStackTraceString(e));
		}
    }
}

