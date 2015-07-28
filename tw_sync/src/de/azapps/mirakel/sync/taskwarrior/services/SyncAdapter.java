/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/


package de.azapps.mirakel.sync.taskwarrior.services;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.common.base.Optional;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.sync.R;
import de.azapps.mirakel.sync.taskwarrior.TaskWarriorSync;
import de.azapps.mirakel.sync.taskwarrior.utilities.TW_ERRORS;
import de.azapps.mirakel.sync.taskwarrior.utilities.TaskWarriorAccount;
import de.azapps.mirakel.sync.taskwarrior.utilities.TaskWarriorSyncFailedException;
import de.azapps.tools.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private final static String TAG = "SyncAdapter";
    public static final String BUNDLE_SERVER_URL = "url";
    public static final String BUNDLE_ORG = "de.azapps.mirakel.org";
    public static final String BUNDLE_SERVER_TYPE = "type";
    public static final String TASKWARRIOR_KEY = "key";
    public static final String SYNC_STATE = "sync_state";
    private static CharSequence last_message = null;
    private final Context mContext;
    private final NotificationManager mNotificationManager;
    private static final int NOTIFY_ID = 1;

    public SyncAdapter(final Context context, final boolean autoInitialize) {
        super(context, autoInitialize);
        this.mContext = context;
        this.mNotificationManager = (NotificationManager) this.mContext
                                    .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public SyncAdapter(final Context context, final boolean autoInitialize,
                       final boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        this.mContext = context;
        this.mNotificationManager = (NotificationManager) this.mContext
                                    .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void onPerformSync(final Account account, final Bundle extras,
                              final String authority, final ContentProviderClient provider,
                              final SyncResult syncResult) {
        // Do not sync if Mirakel is in demo mode
        if (MirakelCommonPreferences.isDemoMode()) {
            return;
        }
        // Mostly it is annoying if there is a notification. So don't show it
        boolean showNotification = false;
        // But if the user actively clicks on "sync" – then show it.
        if (extras.containsKey(ContentResolver.SYNC_EXTRAS_MANUAL)) {
            showNotification = true;
        }

        // Show notif
        try {
            showSyncNotification(showNotification);
        } catch (final ClassNotFoundException e) {
            Log.wtf(TAG, "no MainActivity found", e);
            return;
        }

        // get Sync Type
        String type = AccountManager.get(this.mContext).getUserData(account,
                      BUNDLE_SERVER_TYPE);
        if (type == null) {
            type = TaskWarriorSync.TYPE;
        }
        boolean success = false;

        // Handle Error
        if (type.equals(TaskWarriorSync.TYPE)) {
            TW_ERRORS error = TW_ERRORS.NO_ERROR;
            try {
                final Optional<AccountMirakel> accountMirakel = AccountMirakel.get(account);
                if (accountMirakel.isPresent()) {
                    final TaskWarriorAccount taskWarriorAccount = new TaskWarriorAccount(accountMirakel.get(),
                            getContext());
                    new TaskWarriorSync(this.mContext).sync(taskWarriorAccount, false);
                }
            } catch (final TaskWarriorSyncFailedException e) {
                Log.e(TAG, "SyncError", e);
                error = e.getError();
            }
            success = setLastMessage(success, error);
        } else {
            Log.wtf(TAG, "Unknown SyncType");
        }
        this.mNotificationManager.cancel(SyncAdapter.NOTIFY_ID);
        try {
            handleError(showNotification, success);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void showSyncNotification(final boolean showNotification) throws ClassNotFoundException {
        final Intent intent = new Intent(this.mContext,
                                         Class.forName(DefinitionsHelper.MIRAKEL_ACTIVITY_CLASS));
        intent.setAction(DefinitionsHelper.SHOW_LISTS);
        final PendingIntent p = PendingIntent.getService(this.mContext, 0,
                                intent, 0);
        final NotificationCompat.Builder mNB = new NotificationCompat.Builder(
            this.mContext).setContentTitle("Mirakel")
        .setContentText("Sync")
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setWhen(System.currentTimeMillis()).setOngoing(true)
        .setContentIntent(p);
        if (showNotification) {
            this.mNotificationManager.notify(SyncAdapter.NOTIFY_ID, mNB.build());
        }
    }

    /**
     * Takes the TW_ERROR and set the last_message variable
     *
     * @param success
     * @param error
     * @return
     */
    private boolean setLastMessage(boolean success, final TW_ERRORS error) {
        switch (error) {
        case NO_ERROR:
            last_message = this.mContext.getText(R.string.finish_sync);
            success = true;
            break;
        case TRY_LATER:
            last_message = this.mContext
                           .getText(R.string.message_try_later);
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
        case NO_SUCH_CERT:
            last_message = this.mContext.getText(R.string.cert_not_found);
            break;
        case COULD_NOT_FIND_COMMON_ANCESTOR:
            last_message = this.mContext
                           .getText(R.string.could_not_find_common_ancestor);
            break;
        case CLIENT_SYNC_KEY_NOT_FOUND:
            last_message = this.mContext
                           .getText(R.string.client_sync_key_not_found);
            break;
        case ACCOUNT_VANISHED:
            last_message = mContext.getText(R.string.account_vanished);
            break;
        case NOT_ENABLED:
        default:
            return true;
        }
        Log.d(TAG, "finish Sync");
        return success;
    }

    /**
     * Shows the Notification with the error message if needed
     *
     * @param showNotification
     * @param success
     * @throws ClassNotFoundException
     */
    private void handleError(final boolean showNotification,
                             final boolean success) throws ClassNotFoundException {
        if (showNotification && !success) {
            final String title = "Mirakel: "
                                 + this.mContext.getText(R.string.finish_sync);
            final Intent openIntent = new Intent(this.mContext,
                                                 Class.forName(DefinitionsHelper.MIRAKEL_ACTIVITY_CLASS));
            openIntent.setAction(DefinitionsHelper.SHOW_MESSAGE);
            openIntent.putExtra(Intent.EXTRA_SUBJECT, title);
            openIntent
            .putExtra(Intent.EXTRA_TEXT, last_message);
            openIntent.setData(Uri.parse(openIntent
                                         .toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent pOpenIntent = PendingIntent.getActivity(
                                                  this.mContext, 0, openIntent,
                                                  PendingIntent.FLAG_UPDATE_CURRENT);
            final Notification notification = new NotificationCompat.Builder(
                this.mContext).setContentTitle(title)
            .setContentText(last_message)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pOpenIntent).build();
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            this.mNotificationManager
            .notify(SyncAdapter.NOTIFY_ID, notification);
        }
    }

    public static CharSequence getLastMessage() {
        final CharSequence tmp = last_message;
        last_message = null;
        return tmp;
    }

}
