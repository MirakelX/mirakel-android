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
package de.azapps.mirakel.sync.taskwarrior;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.export_import.ExportImport;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.model.task.TaskDeserializer;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.mirakel.sync.taskwarrior.services.SyncAdapter;
import de.azapps.mirakel.sync.taskwarrior.network_helper.Msg;
import de.azapps.mirakel.sync.taskwarrior.network_helper.TLSClient;
import de.azapps.mirakel.sync.taskwarrior.network_helper.TLSClient.NoSuchCertificateException;
import de.azapps.mirakel.sync.taskwarrior.utilities.TaskWarriorTaskSerializer;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class TaskWarriorSync {

    private static final String TW_PROTOCOL_VERSION = "v1";

    private int clientSyncKeyFailResyncCount = 0;
    public static class TaskWarriorSyncFailedException extends Exception {
        private static final long serialVersionUID = 3349776187699690118L;
        private final TW_ERRORS error;
        private final String message;

        TaskWarriorSyncFailedException(final TW_ERRORS type) {
            super();
            this.error = type;
            message = "";
        }

        TaskWarriorSyncFailedException(final TW_ERRORS type, final String message) {
            super();
            this.error = type;
            this.message = message;
        }

        TaskWarriorSyncFailedException(final TW_ERRORS type, final String message, final Throwable cause) {
            super(cause);
            this.error = type;
            this.message = message;
        }

        TaskWarriorSyncFailedException(final TW_ERRORS type,
                                       final Throwable cause) {
            super(cause);
            this.error = type;
            this.message = cause.getMessage();
        }

        public TW_ERRORS getError() {
            return this.error;
        }

        @Override
        public String getMessage() {
            return this.message;
        }
    }

    public enum TW_ERRORS {
        ACCESS_DENIED, ACCOUNT_SUSPENDED, CANNOT_CREATE_SOCKET, CANNOT_PARSE_MESSAGE,
        CONFIG_PARSE_ERROR, MESSAGE_ERRORS, NO_ERROR, NOT_ENABLED, TRY_LATER, NO_SUCH_CERT,
        COULD_NOT_FIND_COMMON_ANCESTOR, CLIENT_SYNC_KEY_NOT_FOUND, ACCOUNT_VANISHED;
        public static TW_ERRORS getError(final int code) {
            switch (code) {
            case 200:
                Log.d(TAG, "Success");
                break;
            case 201:
                Log.d(TAG, "No change");
                break;
            case 300:
                Log.d(TAG,
                "Deprecated message type\n"
                + "This message will not be supported in future task server releases.");
                break;
            case 301:
                Log.d(TAG,
                "Redirect\n"
                + "Further requests should be made to the specified server/port.");
                // TODO
                break;
            case 302:
                Log.d(TAG,
                "Retry\n"
                + "The client is requested to wait and retry the same request.  The wait\n"
                + "time is not specified, and further retry responses are possible.");
                return TW_ERRORS.TRY_LATER;
            case 400:
                Log.e(TAG, "Malformed data");
                return TW_ERRORS.MESSAGE_ERRORS;
            case 401:
                Log.e(TAG, "Unsupported encoding");
                return TW_ERRORS.MESSAGE_ERRORS;
            case 420:
                Log.e(TAG, "Server temporarily unavailable");
                return TW_ERRORS.TRY_LATER;
            case 421:
                Log.e(TAG, "Server shutting down at operator request");
                return TW_ERRORS.TRY_LATER;
            case 430:
                Log.e(TAG, "Access denied");
                return TW_ERRORS.ACCESS_DENIED;
            case 431:
                Log.e(TAG, "Account suspended");
                return TW_ERRORS.ACCOUNT_SUSPENDED;
            case 432:
                Log.e(TAG, "Account terminated");
                return TW_ERRORS.ACCOUNT_SUSPENDED;
            case 500:
                Log.e(TAG, "Syntax error in request");
                return TW_ERRORS.MESSAGE_ERRORS;
            case 501:
                Log.e(TAG, "Syntax error, illegal parameters");
                return TW_ERRORS.MESSAGE_ERRORS;
            case 502:
                Log.e(TAG, "Not implemented");
                return TW_ERRORS.MESSAGE_ERRORS;
            case 503:
                Log.e(TAG, "Command parameter not implemented");
                return TW_ERRORS.MESSAGE_ERRORS;
            case 504:
                Log.e(TAG, "Request too big");
                return TW_ERRORS.MESSAGE_ERRORS;
            default:
                Log.d(TAG, "Unknown code: " + code);
                break;
            }
            return NO_ERROR;
        }
    }

    // Outgoing.
    // private static int _debug_level = 0;
    // private static int _limit = (1024 * 1024);
    private static String _host = "localhost";
    private static String _key = "";
    private static String sync_key = "";
    private static String _org = "";
    private static int _port = 6544;
    private static String _user = "";

    private static String root;
    private static final String TAG = "TaskWarriorSync";
    public static final String TYPE = "TaskWarrior";
    private static String user_ca;
    private static String user_key;


    private Account account;

    private AccountManager accountManager;

    private Map<String, String[]> dependencies;

    private final Context mContext;

    public TaskWarriorSync(final Context ctx) {
        this.mContext = ctx;
    }

    private void doSync(final Account a, final Msg sync)
    throws TaskWarriorSyncFailedException {
        final Optional<AccountMirakel> accountMirakelOptional = AccountMirakel.get(this.account);
        if (!accountMirakelOptional.isPresent()) {
            // This should never, ever happen
            throw new TaskWarriorSyncFailedException(
                TW_ERRORS.ACCOUNT_VANISHED, "Account vanished");
        }
        final AccountMirakel accountMirakel = accountMirakelOptional.get();
        Log.longInfo(sync.getPayload());
        final TLSClient client = new TLSClient();
        // The following should not happen – except the user is tinkering in our files or so…
        if (user_ca == null) {
            throw new TaskWarriorSyncFailedException(
                TW_ERRORS.CONFIG_PARSE_ERROR, "could not find user ca file");
        }
        if (user_key == null) {
            throw new TaskWarriorSyncFailedException(
                TW_ERRORS.CONFIG_PARSE_ERROR, "could not find user private key file");
        }
        if (root == null) {
            throw new TaskWarriorSyncFailedException(
                TW_ERRORS.CONFIG_PARSE_ERROR, "could not find root certificate");
        }
        try {
            client.init(root, user_ca, user_key);
        } catch (final ParseException e) {
            Log.e(TAG, "cannot open certificate", e);
            throw new TaskWarriorSyncFailedException(
                TW_ERRORS.CONFIG_PARSE_ERROR, "cannot open certificate", e);
        } catch (final CertificateException e) {
            Log.e(TAG, "general problem with init", e);
            throw new TaskWarriorSyncFailedException(
                TW_ERRORS.CONFIG_PARSE_ERROR, "general problem with init", e);
        } catch (final NoSuchCertificateException e) {
            Log.e(TAG, "NoSuchCertificateException", e);
            throw new TaskWarriorSyncFailedException(TW_ERRORS.NO_SUCH_CERT,
                    "general problem with init", e);
        }
        try {
            client.connect(_host, _port);
        } catch (final IOException e) {
            Log.e(TAG, "cannot create socket", e);
            client.close();
            throw new TaskWarriorSyncFailedException(
                TW_ERRORS.CANNOT_CREATE_SOCKET, "cannot create socket", e);
        }
        client.send(sync.serialize());
        final String response = client.recv();
        if (MirakelCommonPreferences.isEnabledDebugMenu()
            && MirakelCommonPreferences.isDumpTw()) {
            try {
                FileUtils.writeToFile(new File(FileUtils.getLogDir(), getTime()
                                               + ".tw_down.log"), response);
            } catch (final IOException e) {
                Log.e(TAG, "Error writing tw_down.log", e);
            }
        }
        // longInfo(response);
        final Msg remotes = new Msg();
        try {
            remotes.parse(response);
        } catch (final MalformedInputException e) {
            Log.e(TAG, "cannot parse message", e);
            client.close();
            throw new TaskWarriorSyncFailedException(
                TW_ERRORS.CANNOT_PARSE_MESSAGE, "cannot parse message", e);
        } catch (final NullPointerException e) {
            Log.wtf(TAG, "remotes.parse throwed NullPointer", e);
            client.close();
            throw new TaskWarriorSyncFailedException(
                TW_ERRORS.CANNOT_PARSE_MESSAGE,
                "remotes.parse throwed NullPointer", e);
        }
        final int code = Integer.parseInt(remotes.get("code"));
        final TW_ERRORS error = TW_ERRORS.getError(code);
        if (error != TW_ERRORS.NO_ERROR) {
            client.close();
            final String status = remotes.get("status");
            if (status != null) {
                if (status.contains("Could not find common ancestor")) {
                    // Ok, lets backup, reset the sync key and sync with empty message

                    // backup
                    Looper.prepare();
                    ExportImport.exportDB(mContext);

                    // reset sync key
                    accountMirakel.setSyncKey(Optional.<String>absent());
                    accountMirakel.save();

                    // sync
                    sync(accountMirakel.getAndroidAccount(), true);
                    throw new TaskWarriorSyncFailedException(
                        TW_ERRORS.COULD_NOT_FIND_COMMON_ANCESTOR,
                        "sync() throwed error");
                } else if (status.contains("Access denied")) {
                    throw new TaskWarriorSyncFailedException(TW_ERRORS.ACCESS_DENIED, "Access denied");
                }
            }
            throw new TaskWarriorSyncFailedException(error);
        }
        if ("Client sync key not found.".equals(remotes.get("status"))) {
            Log.d(TAG, "reset sync-key");
            clientSyncKeyFailResyncCount++;
            // How this could happen? Nobody knows but one user was able to do this…
            if (clientSyncKeyFailResyncCount > 2) {
                throw new TaskWarriorSyncFailedException(
                    TW_ERRORS.CLIENT_SYNC_KEY_NOT_FOUND,
                    "sync() throwed error");
            }
            this.accountManager.setUserData(a, SyncAdapter.TASKWARRIOR_KEY,
                                            null);
            try {
                sync(a, false);
            } catch (final TaskWarriorSyncFailedException e) {
                if (e.getError() != TW_ERRORS.NOT_ENABLED) {
                    client.close();
                    throw new TaskWarriorSyncFailedException(e.getError(), e);
                }
            } finally {
                clientSyncKeyFailResyncCount = 0;
            }
        }
        // parse tasks
        if (remotes.getPayload() == null || remotes.getPayload().isEmpty()) {
            Log.i(TAG, "there is no Payload");
        } else {
            Optional<String> newSyncKey = absent();
            final String tasksString[] = remotes.getPayload().split("\n");
            final Gson gson = new GsonBuilder().registerTypeAdapter(Task.class,
                    new TaskDeserializer(true, accountMirakel, this.mContext))
            .create();
            final List<Task> recurringTasksCreate = new ArrayList<>();
            final List<Task> recurringTasksSave = new ArrayList<>();
            final List<String> taskDeleteUUID = new ArrayList<>();
            for (final String taskString : tasksString) {
                if (taskString.charAt(0) != '{') {
                    Log.d(TAG, "Key: " + taskString);
                    newSyncKey = of(taskString);
                    continue;
                }
                final Optional<Task> localTask;
                final Task serverTask;
                try {
                    Log.i(TAG, taskString);
                    serverTask = gson.fromJson(taskString, Task.class);
                    this.dependencies.put(serverTask.getUUID(),
                                          serverTask.getDependencies());
                    localTask = Task.getByUUID(serverTask.getUUID());
                } catch (final Exception e) {
                    Log.e(TAG, "malformed JSON", e);
                    Log.e(TAG, taskString);
                    continue;
                }
                if (serverTask.getSyncState() == SYNC_STATE.DELETE) {
                    Log.d(TAG, "destroy " + serverTask.getName());
                    taskDeleteUUID.add(serverTask.getUUID());
                } else if (!localTask.isPresent()) {
                    if (serverTask.hasRecurringParent()) {
                        recurringTasksCreate.add(serverTask);
                    } else {
                        try {
                            serverTask.create(false, true);
                            Log.d(TAG, "create " + serverTask.getName());
                        } catch (final NoSuchListException e) {
                            Log.wtf(TAG, "List vanish", e);
                        }
                    }
                } else {
                    serverTask.takeIdFrom(localTask.get());
                    Log.d(TAG, "update " + serverTask.getName());
                    if (serverTask.hasRecurringParent()) {
                        recurringTasksSave.add(serverTask);
                    } else {
                        serverTask.save(false, true);
                    }
                }
            }
            for (final Task t : recurringTasksCreate) {
                final Optional<Task> localTask = Task.getByUUID(t.getUUID());
                if (localTask.isPresent()) {
                    t.takeIdFrom(localTask.get());
                    t.save(false, true);
                } else {
                    try {
                        t.create(false, true);
                    } catch (final NoSuchListException e) {
                        Log.wtf(TAG, "list vanished", e);
                    }
                }
            }
            for (final Task t : recurringTasksSave) {
                t.save(false, true);
            }
            for (final String uuid : taskDeleteUUID) {
                final Optional<Task> t = Task.getByUUID(uuid);
                // Force because we are in the sync – we know what we are doing ;)
                if (t.isPresent()) {
                    t.get().destroy(true);
                }
            }
            accountMirakel.setSyncKey(newSyncKey);
            accountMirakel.save();
        }
        final String message = remotes.get("message");
        if (message != null && !message.isEmpty()) {
            Log.v(TAG, "Message from Server: " + message);
        }
        client.close();
        NotificationService.updateServices(this.mContext, true);
    }

    /**
     * Initialize the variables
     *
     * @param aMirakel MirakelAccount
     * @throws de.azapps.mirakel.sync.taskwarrior.TaskWarriorSync.TaskWarriorSyncFailedException
     */
    private void init(final AccountMirakel aMirakel)
    throws TaskWarriorSyncFailedException {
        final String server = this.accountManager.getUserData(this.account,
                              SyncAdapter.BUNDLE_SERVER_URL);
        final String srv[] = server.trim().split(":");
        if (srv.length != 2) {
            Log.wtf(TAG, "cannot determine address of server");
            throw new TaskWarriorSyncFailedException(
                TW_ERRORS.CONFIG_PARSE_ERROR,
                "cannot determine address of server");
        }
        sync_key = aMirakel.getSyncKey().orNull();
        _host = srv[0];
        _port = Integer.parseInt(srv[1]);
        _user = this.account.name;
        _org = this.accountManager.getUserData(this.account,
                                               SyncAdapter.BUNDLE_ORG);
        TaskWarriorSync.root = this.accountManager.getUserData(this.account,
                               DefinitionsHelper.BUNDLE_CERT);
        TaskWarriorSync.user_ca = this.accountManager.getUserData(this.account,
                                  DefinitionsHelper.BUNDLE_CERT_CLIENT);
        final String[] pwds = this.accountManager.getPassword(this.account)
                              .split(":");
        if (pwds.length < 2) {
            Log.wtf(TAG, "cannot split pwds");
            throw new TaskWarriorSyncFailedException(
                TW_ERRORS.CONFIG_PARSE_ERROR, "cannot split pwds");
        }
        if (pwds.length != 2) {
            // We have to remove the bad stuff and update the current password
            TaskWarriorSync.user_key = pwds[pwds.length - 2].trim();
            _key = pwds[pwds.length - 1].trim();
            this.accountManager.setPassword(this.account,
                                            TaskWarriorSync.user_key + "\n:" + _key);
        } else {
            TaskWarriorSync.user_key = pwds[0].trim();
            _key = pwds[1].trim();
        }
        if (!_key.isEmpty() && _key.length() != 36) {
            Log.wtf(TAG, "Key is not valid");
            throw new TaskWarriorSyncFailedException(
                TW_ERRORS.CONFIG_PARSE_ERROR, "Key is not valid");
        }
    }

    private void setDependencies() {
        for (final String uuid : this.dependencies.keySet()) {
            final Optional<Task> parent = Task.getByUUID(uuid);
            if (uuid == null || this.dependencies == null) {
                continue;
            }
            final String[] childs = this.dependencies.get(uuid);
            if (childs == null) {
                continue;
            }
            for (final String childUuid : childs) {
                final Optional<Task> child = Task.getByUUID(childUuid);
                if (!child.isPresent()) {
                    continue;
                }
                if (child.get().isSubtaskOf(parent.orNull())) {
                    continue;
                }
                try {
                    if (parent.isPresent()) {
                        parent.get().addSubtask(child.get());
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "eat it", e);
                    // eat it
                }
            }
        }
    }

    String getTime() {
        return new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss",
                                    Helpers.getLocal(this.mContext)).format(new Date());
    }

    public void sync(final Account a,
                     boolean couldNotFindCommonAncestorWorkaround) throws TaskWarriorSyncFailedException {
        this.accountManager = AccountManager.get(this.mContext);
        this.account = a;
        final Optional<AccountMirakel> aMirakel = AccountMirakel.get(a);
        if (!aMirakel.isPresent() || !aMirakel.get().isEnabled()) {
            throw new TaskWarriorSyncFailedException(TW_ERRORS.NOT_ENABLED,
                    "TW sync is not enabled");
        }
        init(aMirakel.get());
        final Msg sync = new Msg();
        sync.set("protocol", TW_PROTOCOL_VERSION);
        sync.set("type", "sync");
        sync.set("org", _org);
        sync.set("user", _user);
        sync.set("key", _key);
        final StringBuilder payload = new StringBuilder();
        if (sync_key != null) {
            payload.append(sync_key).append('\n');
        }
        final List<Task> localTasks;
        if (couldNotFindCommonAncestorWorkaround) {
            localTasks = new ArrayList<>(0);
        } else {
            localTasks = Task.getTasksToSync(a);
        }
        for (final Task task : localTasks) {
            payload.append(taskToJson(task)).append('\n');
        }
        // Format: {UUID:[UUID]}
        this.dependencies = new HashMap<>();
        final String oldKey = this.accountManager.getUserData(a,
                              SyncAdapter.TASKWARRIOR_KEY);
        if (oldKey != null && !oldKey.isEmpty()) {
            payload.append(oldKey).append('\n');
        }
        // Build sync-request
        sync.setPayload(payload.toString());
        if (MirakelCommonPreferences.isDumpTw()) {
            try {
                final FileWriter f = new FileWriter(new File(
                                                        FileUtils.getLogDir(), getTime() + ".tw_up.log"));
                f.write(payload.toString());
                f.close();
            } catch (final Exception e) {
                Log.e(TAG, "Eat it", e);
                // eat it
            }
        }
        try {
            doSync(a, sync);
        } catch (final TaskWarriorSyncFailedException e) {
            setDependencies();
            throw new TaskWarriorSyncFailedException(e.getError(), e);
        }
        Log.w(TAG, "clear sync state");
        Task.resetSyncState(localTasks);
        setDependencies();
    }

    /**
     * Converts a task to the json-format we need
     *
     * @param task Task
     * @return Task as json
     */
    @NonNull
    String taskToJson(@NonNull final Task task) {
        return new GsonBuilder()
               .registerTypeAdapter(Task.class,
                                    new TaskWarriorTaskSerializer(this.mContext)).create()
               .toJson(task);
    }
}
