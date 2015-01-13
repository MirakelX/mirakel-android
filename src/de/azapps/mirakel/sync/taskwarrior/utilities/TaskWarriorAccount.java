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

package de.azapps.mirakel.sync.taskwarrior.utilities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Optional;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.sync.taskwarrior.services.SyncAdapter;
import de.azapps.tools.Log;


public class TaskWarriorAccount {
    private static final String TAG = "TaskwarriorAccount";
    @NonNull
    private final AccountMirakel accountMirakel;
    @NonNull
    private final AccountManager accountManager;
    @Nullable
    private String server;
    @Nullable
    private String host;
    private int port;
    @NonNull
    private final Account account;
    @Nullable
    private String password = null;
    @Nullable
    private String userId;
    private String userPassword;

    public TaskWarriorAccount(@NonNull final AccountMirakel accountMirakel, final Context context) {
        this.accountMirakel = accountMirakel;
        accountManager = AccountManager.get(context);
        account = accountMirakel.getAndroidAccount();
    }

    public String getServer() throws TaskWarriorSyncFailedException {
        if (server == null) {
            server = this.accountManager.getUserData(accountMirakel.getAndroidAccount(),
                     SyncAdapter.BUNDLE_SERVER_URL);

            final String srv[] = server.trim().split(":");
            if (srv.length != 2) {
                Log.wtf(TAG, "cannot determine address of server");
                throw new TaskWarriorSyncFailedException(
                    TW_ERRORS.CONFIG_PARSE_ERROR,
                    "cannot determine address of server");
            }
            host = srv[0];
            port = Integer.parseInt(srv[1]);
        }
        return server;
    }

    public String getHost() throws TaskWarriorSyncFailedException {
        getServer();
        return host;
    }

    public int getPort() throws TaskWarriorSyncFailedException {
        getServer();
        return port;
    }

    public Optional<String> getSyncKey() {
        return accountMirakel.getSyncKey();
    }
    public void setSyncKey(@NonNull final Optional<String> syncKey) {
        accountMirakel.setSyncKey(syncKey);
        accountMirakel.save();
    }

    public String getUser() {
        return this.account.name;
    }

    public String getOrg() {
        return this.accountManager.getUserData(this.account,
                                               SyncAdapter.BUNDLE_ORG);
    }

    public String getRootCert() {
        return this.accountManager.getUserData(this.account,
                                               DefinitionsHelper.BUNDLE_CERT);
    }

    public String getUserCert() {
        return this.accountManager.getUserData(this.account,
                                               DefinitionsHelper.BUNDLE_CERT_CLIENT);
    }

    public String getPassword() throws TaskWarriorSyncFailedException {
        if (password == null) {
            password = this.accountManager.getPassword(this.account);
            final String[] pwds = password.split(":");
            if (pwds.length < 2) {
                Log.wtf(TAG, "cannot split pwds");
                throw new TaskWarriorSyncFailedException(
                    TW_ERRORS.CONFIG_PARSE_ERROR, "cannot split pwds");
            }
            if (pwds.length != 2) {
                // We have to remove the bad stuff and update the current password
                userId = pwds[pwds.length - 2].trim();
                userPassword = pwds[pwds.length - 1].trim();
                this.accountManager.setPassword(this.account,
                                                userId + "\n:" + userPassword);
            } else {
                userId = pwds[0].trim();
                userPassword = pwds[1].trim();
            }
            if (!userPassword.isEmpty() && (userPassword.length() != 36)) {
                Log.wtf(TAG, "Key is not valid");
                throw new TaskWarriorSyncFailedException(
                    TW_ERRORS.CONFIG_PARSE_ERROR, "Key is not valid");
            }
        }
        return password;
    }

    public String getUserId() throws TaskWarriorSyncFailedException {
        getPassword();
        return userId;
    }

    public String getUserPassword() throws TaskWarriorSyncFailedException {
        getPassword();
        return userPassword;
    }

    @NonNull
    public AccountMirakel getAccountMirakel() {
        return accountMirakel;
    }

    public Account getAndroidAccount() {
        return account;
    }
}
