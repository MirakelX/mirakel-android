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
package de.azapps.mirakel.sync.taskwarrior;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.Looper;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.export_import.ExportImport;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakel.model.query_builder.CursorWrapper;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.taskwarrior.model.TaskWarriorRecurrence;
import de.azapps.mirakel.sync.taskwarrior.model.TaskWarriorTask;
import de.azapps.mirakel.sync.taskwarrior.model.TaskWarriorTaskDeserializer;
import de.azapps.mirakel.sync.taskwarrior.model.TaskWarriorTaskSerializer;
import de.azapps.mirakel.sync.taskwarrior.network_helper.Msg;
import de.azapps.mirakel.sync.taskwarrior.network_helper.TLSClient;
import de.azapps.mirakel.sync.taskwarrior.network_helper.TLSClient.NoSuchCertificateException;
import de.azapps.mirakel.sync.taskwarrior.utilities.TW_ERRORS;
import de.azapps.mirakel.sync.taskwarrior.utilities.TaskWarriorAccount;
import de.azapps.mirakel.sync.taskwarrior.utilities.TaskWarriorSyncFailedException;
import de.azapps.mirakel.sync.taskwarrior.utilities.TaskWarriorTaskDeletedException;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;
import de.azapps.tools.OptionalUtils;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class TaskWarriorSync {

    @VisibleForTesting
    public static final String TW_PROTOCOL_VERSION = "v1";

    private int clientSyncKeyFailResyncCount = 0;

    private static final int MAX_TASKS_PER_TRANSACTION = 100;

    // Outgoing.

    private static final String TAG = "TaskWarriorSync";
    public static final String TYPE = "TaskWarrior";


    private final Context mContext;

    public TaskWarriorSync(final Context ctx) {
        this.mContext = ctx;
    }

    private void doSync(final TaskWarriorAccount taskWarriorAccount, final Msg syncMessage)
    throws TaskWarriorSyncFailedException {
        Log.longInfo(syncMessage.getPayload());
        final TLSClient client = setupConnection(taskWarriorAccount);
        final Msg remotes = queryServer(syncMessage, client);

        checkError(taskWarriorAccount, client, remotes);
        // parse tasks
        parseTasks(taskWarriorAccount, remotes);
        client.close();
    }

    @VisibleForTesting
    public void parseTasks(TaskWarriorAccount taskWarriorAccount,
                           Msg remotes) throws TaskWarriorSyncFailedException {
        if ((remotes.getPayload() == null) || remotes.getPayload().isEmpty()) {
            Log.i(TAG, "there is no Payload");
        } else {
            final Map<String, TaskWarriorTask> remoteTasks = new HashMap<>(0);

            final Optional<String> newSyncKey = parseTasks(remotes, remoteTasks);

            // lookup tables
            final Map<String, Long> projectMapping = createProjects(taskWarriorAccount, remoteTasks);
            final Map<String, Long> tagMapping = createTags(remoteTasks);
            final Map<String, Long> idMapping = new HashMap<>(remoteTasks.size());

            final ListMirakel inbox = ListMirakel.getInboxList(taskWarriorAccount.getAccountMirakel());

            // lists for deletion
            final List<Long> allUpdatedTasks = new ArrayList<>(remoteTasks.size());
            final List<Long> allDeletedTasks = new ArrayList<>(remoteTasks.size());

            final List<String> uuids = new ArrayList<>(remoteTasks.keySet());
            if (!uuids.isEmpty()) {
                for (int i = 0; i < ((remoteTasks.size() / MAX_TASKS_PER_TRANSACTION) + 1); i++) {
                    int end = (i + 1) * MAX_TASKS_PER_TRANSACTION;
                    if (end > uuids.size()) {
                        end = uuids.size();
                    }
                    if (end <= i * MAX_TASKS_PER_TRANSACTION) {
                        break;
                    }
                    final List<String> transactionUUIDS = uuids.subList(i * MAX_TASKS_PER_TRANSACTION, end);
                    final List<String> newUUIDS = new ArrayList<>(0);

                    // updated tasks
                    final ArrayList<ContentProviderOperation> pendingOperations = handleUpdatedTasks(remoteTasks,
                            projectMapping, inbox, allUpdatedTasks, allDeletedTasks, transactionUUIDS, idMapping);

                    handleInsertNewTasks(remoteTasks, projectMapping, inbox, transactionUUIDS, newUUIDS,
                                         pendingOperations);

                    try {
                        mContext.getContentResolver().applyBatch(DefinitionsHelper.AUTHORITY_INTERNAL, pendingOperations);
                    } catch (RemoteException | OperationApplicationException e) {
                        Log.wtf(TAG, "failed to execute sync operations", e);
                        throw new TaskWarriorSyncFailedException(TW_ERRORS.CANNOT_PARSE_MESSAGE, e);
                    }
                    if (!newUUIDS.isEmpty()) {
                        new MirakelQueryBuilder(mContext).select(Task.UUID, Task.ID).and(Task.UUID,
                                Operation.IN,
                        newUUIDS).query(Task.URI).doWithCursor(new CursorWrapper.WithCursor() {
                            @Override
                            public void withOpenCursor(@NonNull CursorGetter getter) {
                                while (getter.moveToNext()) {
                                    idMapping.put(getter.getString(Task.UUID), getter.getLong(Task.ID));
                                }
                            }
                        });
                    }
                }
                // delete deleted tasks
                mContext.getContentResolver().delete(Task.URI, Task.ID + " IN (" + TextUtils.join(",",
                                                     allDeletedTasks) + ')', null);
                handleReferences(remoteTasks, tagMapping, allUpdatedTasks, idMapping);
            }
            taskWarriorAccount.setSyncKey(newSyncKey);
        }
        final Optional<String> message = remotes.getHeader("message");
        if (message.isPresent() && !message.get().isEmpty()) {
            Log.v(TAG, "Message from Server: " + message.get());
        }
    }

    private void checkError(TaskWarriorAccount taskWarriorAccount, TLSClient client,
                            Msg remotes) throws TaskWarriorSyncFailedException {
        final int code = Integer.parseInt(remotes.getHeader("code").or("400"));
        final TW_ERRORS error = TW_ERRORS.getError(code);
        if (error != TW_ERRORS.NO_ERROR) {
            client.close();
            final Optional<String> status = remotes.getHeader("status");
            if (status.isPresent()) {
                if (status.get().contains("Could not find common ancestor")) {
                    // Ok, lets backup, reset the sync key and sync with empty message

                    // backup
                    Looper.prepare();
                    ExportImport.exportDB(mContext);

                    // reset sync key
                    taskWarriorAccount.setSyncKey(Optional.<String>absent());

                    // sync
                    sync(taskWarriorAccount, true);
                    throw new TaskWarriorSyncFailedException(
                        TW_ERRORS.COULD_NOT_FIND_COMMON_ANCESTOR,
                        "sync() throwed error");
                } else if (status.get().contains("Access denied")) {
                    throw new TaskWarriorSyncFailedException(TW_ERRORS.ACCESS_DENIED, "Access denied");
                }
            }
            throw new TaskWarriorSyncFailedException(error);
        }
        if ("Client sync key not found.".equals(remotes.getHeader("status").or(""))) {
            Log.d(TAG, "reset sync-key");
            clientSyncKeyFailResyncCount++;
            // How this could happen? Nobody knows but one user was able to do this…
            if (clientSyncKeyFailResyncCount > 2) {
                throw new TaskWarriorSyncFailedException(
                    TW_ERRORS.CLIENT_SYNC_KEY_NOT_FOUND,
                    "sync() throwed error");
            }
            taskWarriorAccount.setSyncKey(Optional.<String>absent());
            try {
                sync(taskWarriorAccount, false);
            } catch (final TaskWarriorSyncFailedException e) {
                if (e.getError() != TW_ERRORS.NOT_ENABLED) {
                    client.close();
                    throw new TaskWarriorSyncFailedException(e.getError(), e);
                }
            } finally {
                clientSyncKeyFailResyncCount = 0;
            }
        }
    }

    @NonNull
    private Msg queryServer(final @NonNull Msg syncMessage,
                            final @NonNull TLSClient client) throws TaskWarriorSyncFailedException {
        client.send(syncMessage.serialize());
        final String response = client.recv();
        if (MirakelCommonPreferences.isEnabledDebugMenu()
            && MirakelCommonPreferences.isDumpTw()) {
            Log.longInfo(response);
            try {
                FileUtils.writeToFile(new File(FileUtils.getLogDir(), getTime()
                                               + ".tw_down.log"), response);
            } catch (final IOException e) {
                Log.e(TAG, "Error writing tw_down.log", e);
            }
        }
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
        return remotes;
    }

    @NonNull
    private static TLSClient setupConnection(@NonNull final TaskWarriorAccount taskWarriorAccount)
    throws
        TaskWarriorSyncFailedException {
        final TLSClient client = new TLSClient();
        try {
            client.init(taskWarriorAccount.getRootCert(), taskWarriorAccount.getUserCert(),
                        taskWarriorAccount.getUserId());
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
            client.connect(taskWarriorAccount.getHost(), taskWarriorAccount.getPort());
        } catch (final IOException e) {
            Log.e(TAG, "cannot create socket", e);
            client.close();
            throw new TaskWarriorSyncFailedException(
                TW_ERRORS.CANNOT_CREATE_SOCKET, "cannot create socket", e);
        }
        return client;
    }

    @NonNull
    private static Optional<String> parseTasks(final @NonNull Msg remotes,
            final @NonNull Map<String, TaskWarriorTask> remoteTasks) {
        Optional<String> newSyncKey = absent();
        final String tasksString[] = remotes.getPayload().split("\n");
        final Gson gson = new GsonBuilder().registerTypeAdapter(TaskWarriorTask.class,
                new TaskWarriorTaskDeserializer()).create();

        // parse tasks
        for (final String taskString : tasksString) {
            if (taskString.charAt(0) != '{') {
                Log.d(TAG, "Key: " + taskString);
                newSyncKey = of(taskString);
                continue;
            }
            final TaskWarriorTask t = gson.fromJson(taskString, TaskWarriorTask.class);
            remoteTasks.put(t.getUUID(), t);
        }
        return newSyncKey;
    }

    private void handleReferences(final @NonNull Map<String, TaskWarriorTask> remoteTasks,
                                  final @NonNull Map<String, Long> tagMapping, @NonNull final List<Long> allUpdatedTasks,
                                  final @NonNull Map<String, Long> idMapping) throws TaskWarriorSyncFailedException {
        final String taskList = TextUtils.join(",", allUpdatedTasks);
        // delete all subtasks
        mContext.getContentResolver().delete(MirakelInternalContentProvider.SUBTASK_URI,
                                             "child_id IN(" + taskList + ')', null);
        // delete all tags
        mContext.getContentResolver().delete(MirakelInternalContentProvider.TAG_CONNECTION_URI,
                                             "task_id IN(" + taskList + ')', null);
        // delete recurring
        mContext.getContentResolver().delete(MirakelInternalContentProvider.RECURRING_TW_URI,
                                             Recurring.CHILD + " IN(" + taskList + ')', null);

        final ArrayList<ContentProviderOperation> pendingOperations = new ArrayList<>(remoteTasks.size());
        final Map<String, Long> recurringMapping = new HashMap<>(0);
        for (final TaskWarriorTask t : remoteTasks.values()) {
            if (t.isNotDeleted()) {
                for (final String tag : t.getTags()) {
                    final ContentValues cv = new ContentValues();
                    cv.put("task_id", idMapping.get(t.getUUID()));
                    cv.put("tag_id", tagMapping.get(tag));
                    pendingOperations.add(ContentProviderOperation.newInsert(
                                              MirakelInternalContentProvider.TAG_CONNECTION_URI).withValues(cv).build());
                }
                for (final String child : t.getDependencies()) {
                    final ContentValues cv = new ContentValues();
                    cv.put("parent_id", idMapping.get(t.getUUID()));
                    cv.put("child_id", idMapping.get(child));
                    pendingOperations.add(ContentProviderOperation.newInsert(
                                              MirakelInternalContentProvider.SUBTASK_URI).withValues(cv).build());
                }
                if (t.isRecurringMaster()) {
                    try {
                        final Optional<TaskWarriorRecurrence> r = t.getRecurrence();
                        if (r.isPresent()) {
                            r.get().create();
                            recurringMapping.put(t.getUUID(), r.get().getId());
                            final ContentValues cv = new ContentValues();
                            cv.put(Task.RECURRING, r.get().getId());
                            pendingOperations.add(ContentProviderOperation.newUpdate(Task.URI).withSelection(Task.UUID + "=?",
                                                  new String[] {t.getUUID()}).withValues(cv).build());
                        }
                    } catch (final TaskWarriorRecurrence.NotSupportedRecurrenceException ignored) {
                        // eat it for now
                    }

                }
            }
        }
        final Collection<TaskWarriorTask> recurringChilds = Collections2.filter(remoteTasks.values(),
        new Predicate<TaskWarriorTask>() {
            @Override
            public boolean apply(TaskWarriorTask input) {
                return input.isRecurringChild();
            }
        });
        for (final TaskWarriorTask t : recurringChilds) {
            final String parentUUID = t.getParent();
            final ContentValues updateCV = new ContentValues();
            updateCV.put(Task.RECURRING, recurringMapping.get(parentUUID));
            pendingOperations.add(ContentProviderOperation.newUpdate(Task.URI).withSelection(Task.UUID + "=?",
                                  new String[] {t.getUUID()}).withValues(updateCV).build());
            final ContentValues insertCV = new ContentValues();
            insertCV.put(Recurring.CHILD, idMapping.get(t.getUUID()));
            insertCV.put(Recurring.PARENT, idMapping.get(parentUUID));
            insertCV.put(Recurring.OFFSET_COUNT, t.getImask());
            pendingOperations.add(ContentProviderOperation.newInsert(
                                      MirakelInternalContentProvider.RECURRING_TW_URI).withValues(insertCV).build());
        }
        try {
            mContext.getContentResolver().applyBatch(DefinitionsHelper.AUTHORITY_INTERNAL, pendingOperations);
        } catch (RemoteException | OperationApplicationException e) {
            Log.wtf(TAG, "failed to execute sync operations", e);
            throw new TaskWarriorSyncFailedException(TW_ERRORS.CANNOT_PARSE_MESSAGE, e);
        }
    }

    private void handleInsertNewTasks(final @NonNull Map<String, TaskWarriorTask> remoteTasks,
                                      final @NonNull Map<String, Long> projectMapping, final @NonNull ListMirakel inbox,
                                      final @NonNull List<String> uuids, final @NonNull List<String> newUUIDS,
                                      final @NonNull ArrayList<ContentProviderOperation> pendingOperations) {
        for (final String uuid : uuids) {
            try {
                pendingOperations.add(remoteTasks.get(uuid).getInsert(inbox.getId(), projectMapping));
            } catch (final TaskWarriorTaskDeletedException e) {
                Log.d(TAG, "task is deleted, we do not need to handle this here", e);
            }
            newUUIDS.add(uuid);
        }
    }

    @NonNull
    private ArrayList<ContentProviderOperation> handleUpdatedTasks(final @NonNull
            Map<String, TaskWarriorTask> remoteTasks, final @NonNull Map<String, Long> projectMapping,
            final @NonNull ListMirakel inbox, final @NonNull List<Long> allUpdatedTasks,
            final @NonNull List<Long> allDeletedTasks, final @NonNull List<String> uuids,
            final @NonNull Map<String, Long> idMapping) {
        final ArrayList<ContentProviderOperation> pendingOperations = new ArrayList<>(remoteTasks.size());
        new MirakelQueryBuilder(mContext).select(Task.UUID, Task.ID,
                Task.ADDITIONAL_ENTRIES).and(Task.UUID, Operation.IN,
        uuids).query(Task.URI).doWithCursor(new CursorWrapper.WithCursor() {
            @Override
            public void withOpenCursor(@NonNull CursorGetter getter) {
                while (getter.moveToNext()) {
                    final String uuid = getter.getString(Task.UUID);
                    final long localId = getter.getLong(Task.ID);
                    final String additionals = getter.getString(Task.ADDITIONAL_ENTRIES);
                    final TaskWarriorTask remoteTask = remoteTasks.get(uuid);
                    if (remoteTask.isNotDeleted()) {
                        try {
                            pendingOperations.add(remoteTask.getUpdate(localId, additionals, projectMapping, inbox.getId()));
                            allUpdatedTasks.add(localId);
                            idMapping.put(uuid, localId);
                        } catch (final TaskWarriorTaskDeletedException e) {
                            Log.w(TAG, "however this task can be deleted here, anyway delete it", e);
                            allDeletedTasks.add(localId);
                        }
                    } else {
                        allDeletedTasks.add(localId);
                    }
                    uuids.remove(uuid);
                }
            }
        });
        return pendingOperations;
    }

    private Map<String, Long> createProjects(final @NonNull TaskWarriorAccount taskWarriorAccount,
            final @NonNull Map<String, TaskWarriorTask> remoteTasks) {
        final Set<String> projects = new HashSet<>(0);
        for (final TaskWarriorTask t : remoteTasks.values()) {
            if (t.hasProject()) {
                projects.add(t.getProject());
            }
        }
        final Map<String, Long> projectMapping = new HashMap<>(projects.size());
        new MirakelQueryBuilder(mContext).and(ListMirakel.NAME, Operation.IN,
                                              new ArrayList<>(projects))
        .and(ListMirakel.ACCOUNT_ID, Operation.EQ, taskWarriorAccount.getAccountMirakel().getId())
        .select(Arrays.asList(new String[] {ListMirakel.ID, ListMirakel.NAME})).query(ListMirakel.URI)
        .doWithCursor(new CursorWrapper.WithCursor() {
            @Override
            public void withOpenCursor(@NonNull CursorGetter getter) {
                while (getter.moveToNext()) {
                    final String name = getter.getString(ListMirakel.NAME);
                    projectMapping.put(name, getter.getLong(ListMirakel.ID));
                    projects.remove(name);
                }
            }
        });

        for (final String project : projects) {
            try {
                final ListMirakel list = ListMirakel.newList(project, ListMirakel.SORT_BY.DUE,
                                         taskWarriorAccount.getAccountMirakel());
                projectMapping.put(list.getName(), list.getId());
            } catch (final ListMirakel.ListAlreadyExistsException e) {
                // how ever this could happen???
                throw new IllegalStateException("List wasn't there but here is this list???", e);
            }
        }
        return projectMapping;
    }

    private Map<String, Long> createTags(final @NonNull Map<String, TaskWarriorTask> remoteTasks) {
        final Set<String> tags = new HashSet<>();
        for (TaskWarriorTask t : remoteTasks.values()) {
            tags.addAll(t.getTags());
        }


        final Map<String, Long> tagMapping = new HashMap<>(remoteTasks.size());
        new MirakelQueryBuilder(mContext).and(ListMirakel.NAME, Operation.IN,
                                              new ArrayList<>(tags)).select(Arrays.asList(new String[] {Tag.ID, Tag.NAME})).query(Tag.URI)
        .doWithCursor(new CursorWrapper.WithCursor() {
            @Override
            public void withOpenCursor(@NonNull final CursorGetter getter) {
                while (getter.moveToNext()) {
                    final String name = getter.getString(Tag.NAME);
                    if (remoteTasks.containsKey(name)) {
                        remoteTasks.remove(name);
                    }
                }
            }
        });
        for (final String tag : tags) {
            final Tag t = Tag.newTag(tag);
            tagMapping.put(t.getName(), t.getId());
        }
        return tagMapping;
    }


    String getTime() {
        return new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss",
                                    Helpers.getLocale(this.mContext)).format(new Date());
    }

    public void sync(final @NonNull TaskWarriorAccount taskWarriorAccount,
                     final boolean couldNotFindCommonAncestorWorkaround) throws TaskWarriorSyncFailedException {
        final List<Task> localTasks;
        if (couldNotFindCommonAncestorWorkaround) {
            localTasks = new ArrayList<>(0);
        } else {
            Optional<AccountMirakel> a = AccountMirakel.get(taskWarriorAccount.getAndroidAccount());
            if (a.isPresent()) {
                localTasks = Task.getTasksToSync(a.get());
            } else {
                localTasks = new ArrayList<>(0);
            }

        }
        final Msg sync = getMsg(taskWarriorAccount, localTasks);
        try {
            doSync(taskWarriorAccount, sync);
        } catch (final TaskWarriorSyncFailedException e) {
            //setDependencies();
            throw new TaskWarriorSyncFailedException(e.getError(), e);
        }
        Log.w(TAG, "clear sync state");
        Task.resetSyncState(localTasks);
    }

    @NonNull
    @VisibleForTesting
    public Msg getMsg(@NonNull TaskWarriorAccount taskWarriorAccount,
                      List<Task> localTasks) throws TaskWarriorSyncFailedException {
        final Msg sync = new Msg();
        sync.set("protocol", TW_PROTOCOL_VERSION);
        sync.set("type", "sync");
        sync.set("org", taskWarriorAccount.getOrg());
        sync.set("user", taskWarriorAccount.getUser());
        sync.set("key", taskWarriorAccount.getUserPassword());
        final StringBuilder payload = new StringBuilder();
        OptionalUtils.withOptional(taskWarriorAccount.getSyncKey(), new OptionalUtils.Procedure<String>() {
            @Override
            public void apply(final String input) {
                payload.append(input).append('\n');
            }
        });

        for (final Task task : localTasks) {
            payload.append(taskToJson(task)).append('\n');
        }
        // Build sync-request
        sync.setPayload(payload.toString());
        if (MirakelCommonPreferences.isDumpTw()) {
            try {
                final FileWriter fileWriter = new FileWriter(new File(
                            FileUtils.getLogDir(), getTime() + ".tw_up.log"));
                fileWriter.write(payload.toString());
                fileWriter.close();
            } catch (final IOException e) {
                Log.e(TAG, "Eat it", e);
                // eat it
            }
        }
        return sync;
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
