/*
 * Copyright (C) 2013 Marten Gajda <marten@dmfs.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.dmfs.provider.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dmfs.provider.tasks.TaskContract.Instances;
import org.dmfs.provider.tasks.TaskContract.TaskListColumns;
import org.dmfs.provider.tasks.TaskContract.TaskListSyncColumns;
import org.dmfs.provider.tasks.TaskContract.TaskLists;
import org.dmfs.provider.tasks.TaskContract.Tasks;
import org.dmfs.provider.tasks.handler.CaldavDatabaseHelper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;


/**
 * The Class Utils.
 *
 * @author Tobias Reinsch <tobias@dmfs.org>
 * @author Marten Gajda <marten@dmfs.org>
 */
public class Utils {
    public static void sendActionProviderChangedBroadCast(Context context) {
        Intent providerChangedIntent = new Intent(Intent.ACTION_PROVIDER_CHANGED, TaskContract.CONTENT_URI);
        context.sendBroadcast(providerChangedIntent);
    }


    public static void cleanUpLists(Context context) {
        AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccounts();
        cleanUpLists(context, context.getContentResolver(), accounts);
    }


    public static void cleanUpLists(Context context, ContentResolver db,
                                    Account[] accounts) {
        // make a list of the accounts array
        List<Account> accountList = Arrays.asList(accounts);
        // TODO How to do this?
        // db.beginTransaction();
        try {
            Cursor c = db.query(CaldavDatabaseHelper.getListsUri(), new String[] { TaskListColumns._ID, TaskListSyncColumns.ACCOUNT_NAME, TaskListSyncColumns.ACCOUNT_TYPE },
                                null, null, null);
            // build a list of all task list ids that no longer have an account
            List<Long> obsoleteLists = new ArrayList<Long>();
            try {
                while (c.moveToNext()) {
                    String accountType = c.getString(2);
                    // mark list for removal if it is non-local and the account
                    // is not in accountList
                    if (!TaskContract.LOCAL_ACCOUNT.equals(accountType)) {
                        Account account = new Account(c.getString(1), accountType);
                        if (!accountList.contains(account)) {
                            obsoleteLists.add(c.getLong(0));
                        }
                    }
                }
            } finally {
                c.close();
            }
            if (obsoleteLists.size() == 0) {
                // nothing to do here
                return;
            }
            // remove all accounts in the list
            for (Long id : obsoleteLists) {
                if (id != null) {
                    db.delete(CaldavDatabaseHelper.getListsUri(), TaskListColumns._ID + "=" + id, null);
                }
            }
            // TODO How to do this?
            // db.setTransactionSuccessful();
        } finally {
            // TODO How to do this?
            // db.endTransaction();
        }
        // notify all observers
        ContentResolver cr = context.getContentResolver();
        cr.notifyChange(TaskLists.CONTENT_URI, null);
        cr.notifyChange(Tasks.CONTENT_URI, null);
        cr.notifyChange(Instances.CONTENT_URI, null);
        Utils.sendActionProviderChangedBroadCast(context);
    }

}
