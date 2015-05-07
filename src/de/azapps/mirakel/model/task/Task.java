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

package de.azapps.mirakel.model.task;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.UndoHistory;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.ListMirakel.SORT_BY;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Sorting;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.tools.Log;
import de.azapps.tools.OptionalUtils;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

public class Task extends TaskBase {

    // Constants
    public static final String[] allColumns = {ModelBase.ID, TaskBase.UUID,
                                               TaskBase.LIST_ID, ModelBase.NAME, TaskBase.CONTENT, TaskBase.DONE,
                                               TaskBase.DUE, TaskBase.REMINDER, TaskBase.PRIORITY,
                                               DatabaseHelper.CREATED_AT, DatabaseHelper.UPDATED_AT,
                                               DatabaseHelper.SYNC_STATE_FIELD, TaskBase.ADDITIONAL_ENTRIES,
                                               TaskBase.RECURRING, TaskBase.RECURRING_REMINDER, TaskBase.PROGRESS,
                                               TaskBase.RECURRING_SHOWN
                                              };
    public static final String BASIC_FILTER_DISPLAY_TASKS = " NOT "
            + DatabaseHelper.SYNC_STATE_FIELD + " = " + SYNC_STATE.DELETE
            + " AND " + RECURRING_SHOWN + "=1";

    public final static Uri URI = MirakelInternalContentProvider.TASK_URI;

    public static final String SUBTASK_TABLE = "subtasks";
    public static final String TABLE = "tasks";
    public static final String VIEW_TABLE = "tasks_view";
    public static final String NO_PROJECT = "NO_PROJECT";

    private static final String TAG = "TasksDataSource";


    private String dependencies[];

    public void setDependencies(final String[] dep) {
        this.dependencies = dep;
    }


    @Override
    protected Uri getUri() {
        return URI;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors

    Task() {
        super();
    }

    public Task(@NonNull final String name, @NonNull final ListMirakel list,
                final @NonNull Optional<Calendar> due, final int priority) {
        this(name, list, "", false, due, priority);
    }

    public Task(@NonNull final String name, @NonNull final ListMirakel list,
                @NonNull final String content, final boolean done,
                final @NonNull Optional<Calendar> due, final int priority) {
        this(INVALID_ID, java.util.UUID.randomUUID().toString(),
             list, name, content, done, due, Optional.<Calendar>absent(), priority, new GregorianCalendar(),
             new GregorianCalendar(),
             SYNC_STATE.ADD, "", -1, -1, 0, true);
    }

    public Task(final long id, @NonNull final String uuid, @NonNull final ListMirakel list,
                @NonNull final String name, @NonNull final String content, final boolean done,
                final @NonNull Optional<Calendar> due, final @NonNull Optional<Calendar> reminder,
                final int priority,
                @NonNull final Calendar created_at, @NonNull final Calendar updated_at,
                @NonNull final SYNC_STATE sync_state, @NonNull final String additionalEntriesString,
                final int recurring, final int recurring_reminder,
                final int progress, final boolean shown) {
        super(id, uuid, list, name, content, done, due, reminder, priority,
              created_at, updated_at, sync_state, additionalEntriesString,
              recurring, recurring_reminder, progress, shown);
    }

    public Task(@NonNull final Cursor cursor) {
        if (cursor.isAfterLast()) {
            throw new IllegalArgumentException("cursor out of bounds");
        }
        if (cursor.isNull(cursor.getColumnIndex(DUE))) {
            setDue(Optional.<Calendar>absent());
        } else {
            setDue(fromNullable(DateTimeHelper.createLocalCalendar(
                                    cursor.getLong(cursor.getColumnIndex(DUE)), true)));
        }
        if (cursor.isNull(cursor.getColumnIndex(REMINDER))) {
            setReminder(Optional.<Calendar>absent());
        } else {
            setReminder(fromNullable(DateTimeHelper.createLocalCalendar(cursor
                                     .getLong(cursor.getColumnIndex(REMINDER)))));
        }
        final Calendar createdAt;
        if (cursor.isNull(cursor.getColumnIndex(DatabaseHelper.CREATED_AT))) {
            createdAt = new GregorianCalendar();
        } else {
            createdAt = new GregorianCalendar();
            createdAt.setTimeInMillis(cursor.getLong(cursor
                                      .getColumnIndex(DatabaseHelper.CREATED_AT)) * 1000L);
        }
        setCreatedAt(createdAt);
        final Calendar updatedAt;
        if (cursor.isNull(cursor.getColumnIndex(DatabaseHelper.UPDATED_AT))) {
            updatedAt = new GregorianCalendar();
        } else {
            updatedAt = new GregorianCalendar();
            updatedAt.setTimeInMillis(cursor.getLong(cursor
                                      .getColumnIndex(DatabaseHelper.UPDATED_AT)) * 1000L);
        }
        setUpdatedAt(updatedAt);
        setId(cursor.getLong(cursor.getColumnIndex(ID)));
        setUUID(cursor.getString(cursor.getColumnIndex(UUID)));
        this.list = ListMirakel.get(cursor.getLong(cursor.getColumnIndex(LIST_ID))).get();
        setName(cursor.getString(cursor.getColumnIndex(NAME)));
        final String content = cursor.getString(cursor.getColumnIndex(CONTENT));
        setContent((content == null) ? "" : content); // keep that!
        setDone(cursor.getShort(cursor.getColumnIndex(DONE)) == 1);
        setPriority(cursor.getInt(cursor.getColumnIndex(PRIORITY)));
        setSyncState(SYNC_STATE.valueOf(cursor.getShort(cursor
                                        .getColumnIndex(DatabaseHelper.SYNC_STATE_FIELD))));
        setAdditionalEntries(cursor.getString(cursor
                                              .getColumnIndex(ADDITIONAL_ENTRIES)));
        setRecurrence(cursor.getLong(cursor.getColumnIndex(RECURRING)));
        setRecurringReminder(cursor.getLong(cursor
                                            .getColumnIndex(RECURRING_REMINDER)));
        setProgress(cursor.getInt(cursor.getColumnIndex(PROGRESS)));
        setIsRecurringShown(cursor.getShort(cursor
                                            .getColumnIndex(RECURRING_SHOWN)) == 1);
    }

    public Task(@NonNull final String name, @NonNull final ListMirakel listMirakel) {
        super(name, listMirakel);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Factory methods

    @NonNull
    public static List<Task> all() {
        return addBasicFiler(new MirakelQueryBuilder(context)).and(DONE,
                Operation.EQ, false).getList(Task.class);
    }

    @NonNull
    public static MirakelQueryBuilder getMirakelQueryBuilder(@NonNull final Optional<ListMirakel>
            listMirakelOptional) {
        final MirakelQueryBuilder qb;
        if (!listMirakelOptional.isPresent()) {
            qb = new MirakelQueryBuilder(context);
            if (MirakelCommonPreferences.showDoneMain()) {
                qb.and(Task.DONE, Operation.EQ, false);
            }
        } else {
            qb = listMirakelOptional.get().getWhereQueryForTasks();
        }
        addBasicFiler(qb);
        if (listMirakelOptional.isPresent()) {
            ListMirakel.addSortBy(qb, listMirakelOptional.get().getSortBy(),
                                  listMirakelOptional.get().getId() < 0);
        }
        return qb;
    }

    @NonNull
    public static List<Task> cursorToTaskList(@NonNull final Cursor cursor) {
        cursor.moveToFirst();
        final List<Task> tasks = new ArrayList<>();
        while (!cursor.isAfterLast()) {
            tasks.add(new Task(cursor));
            cursor.moveToNext();
        }
        cursor.close();
        return tasks;
    }

    public static void deleteDoneTasks() {
        final ContentValues values = new ContentValues();
        values.put("sync_state", SYNC_STATE.DELETE.toInt());
        MirakelInternalContentProvider
        .withTransaction(new MirakelInternalContentProvider.DBTransaction() {
            @Override
            public void exec() {
                update(URI, values, "sync_state!=" + SYNC_STATE.ADD
                       + " AND done=1", null);
                delete(URI, "sync_state=" + SYNC_STATE.ADD
                       + " AND done=1", null);
            }
        });
    }

    /**
     * Get a Task by id
     *
     * @param id id of the task
     * @return task
     */
    @NonNull
    public static Optional<Task> get(final long id) {
        return get(id, false);
    }

    /**
     * @param id    id of the task
     * @param force Search also for deleted tasks. Do not use it unless you
     *              exactly know what you are doing
     * @return task
     */
    @NonNull
    public static Optional<Task> get(final long id, final boolean force) {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(context).and(ID,
                Operation.EQ, id);
        if (!force) {
            qb.and(DatabaseHelper.SYNC_STATE_FIELD, Operation.NOT_EQ,
                   SYNC_STATE.DELETE.toInt());
        }
        return qb.get(Task.class);
    }

    @NonNull
    public static Optional<Task> getByUUID(final String uuid) {
        return new MirakelQueryBuilder(context)
               .and(UUID, Operation.EQ, uuid)
               .and(DatabaseHelper.SYNC_STATE_FIELD, Operation.NOT_EQ,
                    SYNC_STATE.DELETE.toInt()).get(Task.class);
    }

    public static Task getDummy(@NonNull final Context ctx, @NonNull final ListMirakel list) {
        return new Task(ctx.getString(R.string.task_empty), list);
    }

    public static List<Pair<Long, String>> getTaskNames() {
        final Cursor c = addBasicFiler(new MirakelQueryBuilder(context))
                         .select(ID, NAME).query(URI);
        final List<Pair<Long, String>> names = new ArrayList<>();
        c.moveToFirst();
        while (!c.isAfterLast()) {
            names.add(new Pair<>(c.getLong(0), c.getString(1)));
            c.moveToNext();
        }
        c.close();
        return names;
    }

    /**
     * Get Tasks from a List Use it only if really necessary!
     *
     * @param listId
     * @param sorting  The Sorting (@see ListMirakel.SORT_BY)
     * @param showDone
     * @return
     */
    public static List<Task> getTasks(final long listId, final SORT_BY sorting,
                                      final boolean showDone) {
        final Cursor cursor = getTasksCursor(listId, sorting, showDone);
        return cursorToTaskList(cursor);
    }

    /**
     * Get Tasks from a List Use it only if really necessary!
     *
     * @param list
     * @param sorting  The Sorting (@see ListMirakel.SORT_BY)
     * @param showDone
     * @return
     */
    public static List<Task> getTasks(final ListMirakel list,
                                      final SORT_BY sorting, final boolean showDone) {
        return getTasks(list.getId(), sorting, showDone);
    }

    /**
     * Get a Cursor with all Tasks of a list
     *
     * @param listId
     * @param sorting
     * @return
     */
    private static Cursor getTasksCursor(final long listId, final SORT_BY sorting,
                                         final boolean showDone) {
        final Optional<ListMirakel> l = ListMirakel.get(listId);
        if (!l.isPresent()) {
            Log.wtf(TAG, "list not found");
            // TODO throw something
            return new MatrixCursor(allColumns);
        } else {
            final MirakelQueryBuilder qb = l.get().getWhereQueryForTasks();
            if (!showDone) {
                qb.and(DONE, Operation.EQ, false);
            }
            return getTasksCursor(listId, sorting, qb);
        }
    }

    /**
     * Get a Cursor with all Tasks of a list
     *
     * @param listId
     * @param sorting
     * @return
     */
    private static Cursor getTasksCursor(final long listId, final SORT_BY sorting,
                                         final MirakelQueryBuilder qb) {
        addBasicFiler(qb);
        qb.sort(Task.DONE, Sorting.ASC);
        ListMirakel.addSortBy(qb, sorting, listId < 0);
        return qb.select(allColumns).query(URI);
    }

    private static Cursor getTasksCursor(final Task subtask) {
        return ListMirakel.addSortBy(
                   new MirakelQueryBuilder(context).select(
                       addPrefix(allColumns, TABLE)).and(
                       SUBTASK_TABLE + ".parent_id", Operation.EQ, subtask),
                   SORT_BY.OPT, false).query(
                   MirakelInternalContentProvider.TASK_SUBTASK_URI);
    }

    // Static Methods

    @NonNull
    public static List<Task> getTasksToSync(final Account account) {
        Optional<AccountMirakel> accountMirakelOptional = AccountMirakel
                .get(account);
        if (!accountMirakelOptional.isPresent()) {
            return new ArrayList<>();
        } else {
            return new MirakelQueryBuilder(context)
                   .and(DatabaseHelper.SYNC_STATE_FIELD, Operation.NOT_EQ,
                        SYNC_STATE.NOTHING.toInt())
                   .and(LIST_ID,
                        Operation.IN,
                        ListMirakel.getListsForAccount(accountMirakelOptional.get())).getList(Task.class);
        }
    }

    public static List<Task> getTasksWithReminders() {
        return new MirakelQueryBuilder(context)
               .and(REMINDER, Operation.NOT_EQ, (String) null)
               .and(DONE, Operation.EQ, false).getList(Task.class);
    }

    public static Task newTask(final String name, final ListMirakel list) {
        return newTask(name, list, "", false, Optional.<Calendar>absent(), 0);
    }

    public static Task newTask(final String name, final ListMirakel list,
                               final @NonNull Optional<Calendar> due, final int priority) {
        return newTask(name, list, "", false, due, priority);
    }

    /**
     * Create a new Task
     *
     * @param name
     * @param list
     * @param content
     * @param done
     * @param due
     * @param priority
     * @return
     */

    public static Task newTask(final String name, final ListMirakel list,
                               final String content, final boolean done,
                               final Optional<Calendar> due, final int priority) {
        final Task t = new Task(0, java.util.UUID.randomUUID().toString(),
                                list, name, content, done, due, Optional.<Calendar>absent(), priority, new GregorianCalendar(),
                                new GregorianCalendar(),
                                SYNC_STATE.ADD, "", -1, -1, 0, true);
        try {
            return t.create();
        } catch (final NoSuchListException e) {
            ErrorReporter.report(ErrorType.TASKS_NO_LIST);
            Log.e(Task.TAG, "NoSuchListException", e);
            return null;
        }
    }

    public Task create() throws NoSuchListException {
        return create(true);
    }

    public Task create(final boolean addFlag) throws NoSuchListException {
        return create(addFlag, false);
    }

    public Task create(final boolean addFlag, final boolean calledFromSync)
    throws NoSuchListException {
        final ContentValues values = getContentValues();
        values.remove(Task.ID);
        setId(insert(URI, values));
        final Task newTask = get(getId()).get();
        if (!calledFromSync) {
            UndoHistory.logCreate(newTask, Task.context);
        }
        return newTask;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    // helper methods

    public static MirakelQueryBuilder addBasicFiler(final MirakelQueryBuilder qb) {
        // If you change this do not forget to change the getBasicFilter() too
        return qb.and(DatabaseHelper.SYNC_STATE_FIELD, Operation.NOT_EQ,
                      SYNC_STATE.DELETE.toInt()).and(RECURRING_SHOWN, Operation.EQ,
                              true);
    }

    public static String getQualifiedColumn(final String column) {
        return TABLE + '.' + column;
    }

    public static String getBasicFilter() {
        // If you change this do not forget to change the addBasicFiler() too
        return ' ' + getQualifiedColumn(DatabaseHelper.SYNC_STATE_FIELD) + "!=" + SYNC_STATE.DELETE.toInt()
               + " AND " +  getQualifiedColumn(RECURRING_SHOWN) + "= 1 ";
    }

    public static void resetSyncState(final List<Task> tasks) {
        if (tasks.isEmpty()) {
            return;
        }
        for (final Task t : tasks) {
            if (t.getSyncState() != SYNC_STATE.DELETE) {
                t.setSyncState(SYNC_STATE.NOTHING);
                try {
                    update(URI, t.getContentValues(), ID + " = " + t.getId(),
                           null);
                } catch (final NoSuchListException e) {
                    Log.d(Task.TAG, "List did vanish", e);
                } catch (final Exception e) {
                    t.destroy(false);
                    Log.d(Task.TAG, "destroy: " + t.getName(), e);
                }
            } else {
                Log.d(Task.TAG, "destroy: " + t.getName());
                t.destroy(true);
            }
        }
    }

    public FileMirakel addFile(final Context ctx, final Uri uri) {
        return FileMirakel.newFile(ctx, this, uri);
    }

    public void addSubtask(final Task t) {
        if (hasSubtasksLoop(t)) {
            return;
        }
        final ContentValues cv = new ContentValues();
        cv.put("parent_id", getId());
        cv.put("child_id", t.getId());
        insert(MirakelInternalContentProvider.SUBTASK_URI, cv);
        if ((syncState != SYNC_STATE.DELETE) && (syncState != SYNC_STATE.ADD)) {
            final ContentValues values = new ContentValues();
            values.put(DatabaseHelper.SYNC_STATE_FIELD, SYNC_STATE.NEED_SYNC.toInt());
            update(URI, values, ID + "=?", new String[] {String.valueOf(getId())});
        }
    }

    public void deleteSubtask(final Task s, final boolean permanently) {
        if (permanently) {
            s.destroy();
        } else {
            delete(MirakelInternalContentProvider.SUBTASK_URI, "parent_id="
                   + getId() + " and child_id=" + s.getId(), null);
        }
    }

    public void destroySubtasks() {
        final List<Task> tasks = getSubtasks();
        for (final Task t : tasks) {
            t.destroy();
        }
    }

    /**
     * Delete a task
     */
    @Override
    public void destroy() {
        destroy(false);
    }

    public void destroy(final boolean force) {
        if (!force) {
            UndoHistory.updateLog(this, Task.context);
        }
        final long id = getId();
        updateRecurringMaster();
        final String subWhereQuery = " IN (SELECT child FROM "
                                     + Recurring.TW_TABLE + " WHERE parent=" + getId() + ')';
        final String whereQuery = ModelBase.ID + " = " + id + " OR "
                                  + ModelBase.ID + subWhereQuery;
        if ((getSyncState() == SYNC_STATE.ADD) || force) {
            delete(URI, whereQuery, null);
            delete(MirakelInternalContentProvider.FILE_URI, "task_id = " + id
                   + " OR task_id " + subWhereQuery, null);
            delete(MirakelInternalContentProvider.RECURRING_TW_URI, "parent=?",
                   new String[] {String.valueOf(getId())});
            destroyGarbage();
        } else {
            final ContentValues values = new ContentValues();
            values.put(DatabaseHelper.SYNC_STATE_FIELD,
                       SYNC_STATE.DELETE.toInt());
            update(URI, values, whereQuery, null);
            delete(MirakelInternalContentProvider.FILE_URI, "task_id = " + id
                   + " OR task_id " + subWhereQuery, null);
        }
    }

    public void destroyGarbage() {
        FileMirakel.destroyForTask(this);
        destroyRecurrenceGarbageForTask(getId());
    }

    public static void destroyRecurrenceGarbageForTask(final long id) {
        delete(MirakelInternalContentProvider.SUBTASK_URI, "parent_id=" + id
               + " or child_id=" + id, null);
    }

    public String[] getDependencies() {
        return this.dependencies;
    }

    public List<FileMirakel> getFiles() {
        return FileMirakel.getForTask(this);
    }

    public long countSubtasks() {
        return new MirakelQueryBuilder(context).and("parent_id", Operation.EQ,
                this).count(MirakelInternalContentProvider.SUBTASK_URI);
    }

    public List<Task> getSubtasks() {
        final Cursor c = Task.getTasksCursor(this);
        final List<Task> subTasks = new ArrayList<>();
        c.moveToFirst();
        while (!c.isAfterLast()) {
            subTasks.add(new Task(c));
            c.moveToNext();
        }
        c.close();

        if (getRecurrence().isPresent()) {
            final Optional<Task> master = getRecurrenceMaster();
            if (master.isPresent() && (master.get().getId() != getId())) {
                subTasks.addAll(master.get().getSubtasks());
            }
        }
        return subTasks;
    }

    public boolean hasSubtasksLoop(final Task t) {
        if (t.getId() == getId()) {
            return true;
        }
        final List<Task> subtasks = getSubtasks();
        for (final Task s : subtasks) {
            if ((s.getId() == t.getId()) || s.hasSubtasksLoop(t)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSubtaskOf(final Task otherTask) {
        if (otherTask == null) {
            return false;
        }
        return new MirakelQueryBuilder(context)
               .and("parent_id", Operation.EQ, otherTask)
               .and("child_id", Operation.EQ, this)
               .count(MirakelInternalContentProvider.SUBTASK_URI) > 0;
    }

    @Override
    public void save() {
        save(true);
    }

    /**
     * Save a Task
     */
    public void save(final boolean log) {
        save(log, false);
    }

    public void save(final boolean log, final boolean calledFromSync) {
        try {
            unsafeSave(log, calledFromSync, true);
        } catch (final NoSuchListException e) {
            Log.w(Task.TAG, "List did vanish");
        }
    }

    private void unsafeSave(final boolean log, final boolean calledFromSync,
                            final boolean updateUpdatedAt) throws NoSuchListException {
        if (!isEdited()) {
            Log.d(Task.TAG, "new Task equals old, did not need to save it");
            return;
        }
        if (isStub()) {
            Log.d(TAG, "It's a stub, don't save it");
            return;
        }
        if (isEdited(RECURRING) && !calledFromSync) {
            final Optional<Task> oldOptional = Task.get(getId());
            if (oldOptional.isPresent()) {
                final Task old = oldOptional.get();
                if ((old.getRecurrenceId() == -1) && (getRecurrenceId() != -1)) {
                    insertFirstRecurringChild();
                } else if (old.getRecurrenceId() != getRecurrenceId()) {
                    final Optional<Recurring> recurring = getRecurrence();
                    if (!recurring.isPresent()) {
                        final Cursor c = new MirakelQueryBuilder(context)
                        .select("parent")
                        .and("child", Operation.EQ, this)
                        .query(MirakelInternalContentProvider.RECURRING_TW_URI);
                        if (c.moveToFirst()) {
                            final long masterID = c.getLong(0);
                            final ContentValues cv = new ContentValues();
                            cv.put(RECURRING, -1);
                            update(URI, cv, ModelBase.ID
                                   + " IN (SELECT child FROM "
                                   + Recurring.TW_TABLE + " WHERE parent="
                                   + masterID + ')', null);
                            delete(MirakelInternalContentProvider.RECURRING_TW_URI,
                                   "parent=?", new String[] {String.valueOf(masterID)});
                            delete(URI, ModelBase.ID + "=?",
                                   new String[] {String.valueOf(masterID)});
                        }
                        c.close();
                    } else {
                        updateRecurringChilds(recurring.get());
                    }
                }
            }
        }
        if (isEdited(TaskBase.DONE) && isDone()) {
            setSubTasksDone();
        }
        setSyncState(((getSyncState() == SYNC_STATE.ADD)
                      || (getSyncState() == SYNC_STATE.IS_SYNCED)) ? getSyncState() : SYNC_STATE.NEED_SYNC);
        if (updateUpdatedAt && (Task.context != null)) {
            setUpdatedAt(new GregorianCalendar());
        }
        final ContentValues values = getContentValues();
        if (log && !calledFromSync) {
            final Optional<Task> old = Task.get(getId());
            OptionalUtils.withOptional(old, new OptionalUtils.Procedure<Task>() {
                @Override
                public void apply(Task input) {
                    UndoHistory.updateLog(input, Task.context);
                }
            });
        }
        final List<Tag> tags = getTags();
        MirakelInternalContentProvider
        .withTransaction(new MirakelInternalContentProvider.DBTransaction() {
            @Override
            public void exec() {
                if (isEdited(TaskBase.RECURRING)) {
                    long master = getId();
                    final Cursor c = new MirakelQueryBuilder(context)
                    .select("parent")
                    .and("child", Operation.EQ, master)
                    .query(MirakelInternalContentProvider.RECURRING_TW_URI);
                    if (c.moveToFirst()) {
                        master = c.getLong(0);
                    }
                    c.close();
                    final ContentValues cv = new ContentValues();
                    cv.put(RECURRING, getRecurrenceId());
                    update(URI, cv, ModelBase.ID
                           + " IN (SELECT child FROM "
                           + Recurring.TW_TABLE + " WHERE parent="
                           + master + ')', null);
                }
                update(URI, values, ModelBase.ID + " = " + getId(),
                       null);
                for (final Tag t : tags) {
                    saveTag(t);
                }
                clearEdited();
            }
        });
    }

    @NonNull
    public Optional<Task> getRecurrenceMaster() {
        final Cursor c = new MirakelQueryBuilder(context)
        .select(addPrefix(allColumns, TABLE))
        .and(Recurring.TW_TABLE + ".child", Operation.EQ, this)
        .sort(Recurring.TW_TABLE + '.' + Recurring.OFFSET_COUNT,
              Sorting.ASC)
        .query(MirakelInternalContentProvider.TASK_RECURRING_TW_PARENT_URI);
        if (c.moveToFirst()) {
            return Optional.of(new Task(c));
        } else {
            return absent();
        }
    }

    @NonNull
    public List<Task> getRecurrenceChilds() {
        final Cursor c = new MirakelQueryBuilder(context)
        .select(addPrefix(allColumns, TABLE))
        .and(Recurring.TW_TABLE + '.' + Recurring.PARENT, Operation.EQ, this)
        .sort(Recurring.TW_TABLE + '.' + Recurring.OFFSET_COUNT,
              Sorting.ASC)
        .query(MirakelInternalContentProvider.TASK_RECURRING_TW_CHILD_URI);
        return cursorToTaskList(c);
    }

    private void updateRecurringChilds(final Recurring r) {

        final String[] select = addPrefix(allColumns, TABLE, allColumns.length + 1);
        select[allColumns.length] = Recurring.TW_TABLE + '.' + Recurring.OFFSET_COUNT;
        final Cursor c = new MirakelQueryBuilder(context)
        .select(select)
        .and(Recurring.TW_TABLE + '.' + Recurring.PARENT,
             Operation.IN,
             new MirakelQueryBuilder(context).select(
                 Recurring.PARENT).and(Recurring.CHILD,
                                       Operation.EQ, this),
             MirakelInternalContentProvider.RECURRING_TW_URI)
        .sort(Recurring.TW_TABLE + '.' + Recurring.OFFSET_COUNT,
              Sorting.ASC)
        .query(MirakelInternalContentProvider.TASK_RECURRING_TW_CHILD_URI);
        if (c.moveToFirst()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Task old = null;
                    do {
                        final Task child = new Task(c);
                        final int offset = c.getInt(allColumns.length);
                        if ((offset > 0) && (old != null) && (r != null) && old.getDue().isPresent()) {
                            child.setDue(r.addRecurring(old.getDue()));
                            if (child.getId() == getId()) {
                                // this task:
                                setDue(child.getDue());
                            }
                        }
                        child.setRecurrence(getRecurrenceId());
                        child.save(false, true);
                        old = child;
                    } while (c.moveToNext());
                    c.close();
                }
            }).start();
        }
    }

    public void insertFirstRecurringChild() throws NoSuchListException {
        Log.i(TAG, "changed recurrence");
        // add first
        final long oldId = getId();
        final long recID = getRecurrenceId();
        final Task child = create();
        setIsRecurringShown(false);
        // switch ids
        // recurrence can be only modified from dueReminder view
        // there the task is reloaded
        setId(child.getId());
        child.setId(oldId);
        child.edited.remove(RECURRING);
        child.save(false);
        final ContentValues cv = new ContentValues();
        cv.put("parent", getId());
        cv.put("child", child.getId());
        cv.put("offset", 0);
        cv.put("offsetCount", 0);
        insert(MirakelInternalContentProvider.RECURRING_TW_URI, cv);
        // if not done manual this will cause a stackoverflow
        cv.clear();
        cv.put(RECURRING, recID);
        cv.put(UUID, java.util.UUID.randomUUID().toString());
        cv.put(DatabaseHelper.SYNC_STATE_FIELD, SYNC_STATE.ADD.toInt());
        update(URI, cv, ModelBase.ID + "=?", new String[] {String.valueOf(oldId)});

        final ContentValues subtaskCV = new ContentValues();
        subtaskCV.put("parent_id", getId());
        update(MirakelInternalContentProvider.SUBTASK_URI, subtaskCV, "parent_id=?", new String[] {String.valueOf(oldId)});
    }


    public static void fixRecurringShowing() {
        final ContentValues cv = new ContentValues();
        cv.put(RECURRING_SHOWN, Boolean.FALSE);
        update(URI, cv, ModelBase.ID + " IN (SELECT parent FROM "
               + Recurring.TW_TABLE + ')', null);
        cv.clear();
        cv.put(RECURRING_SHOWN, Boolean.TRUE);
        update(URI, cv, ModelBase.ID + " IN (SELECT r.child FROM "
               + Recurring.TW_TABLE + " AS r INNER JOIN " + TABLE
               + " AS t ON t._id=r.child)", null);
    }

    private void updateRecurringMaster() {
        // update master if child changed
        final Cursor c = new MirakelQueryBuilder(context).select("parent")
        .and("child", Operation.EQ, this)
        .query(MirakelInternalContentProvider.RECURRING_TW_URI);
        if (c.moveToFirst()) {
            final ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.SYNC_STATE_FIELD,
                   SYNC_STATE.NEED_SYNC.toInt());
            update(URI, cv, "_id=?", new String[] {String.valueOf(c.getLong(0))});
        }
        c.close();
    }

    private void setSubTasksDone() {
        if (!getRecurrence().isPresent()) {
            for (final Task t : getSubtasks()) {
                t.setDone(true);
                t.save(false);
            }
        }
    }

    public String toJson() {
        String json = "{";
        json += "\"id\":" + getId() + ',';
        json += "\"name\":\"" + getName() + "\",";
        json += "\"content\":\"" + getContent() + "\",";
        json += "\"done\":" + (isDone() ? "true" : "false") + ',';
        json += "\"priority\":" + getPriority() + ',';
        json += "\"progress\":" + getProgress() + ',';
        json += "\"list_id\":" + getList().getId() + ',';
        json += "\"sync_state\":" + getSyncState().toInt() + ',';
        String s = "";
        if (getDue().isPresent()) {
            s = String.valueOf(DateTimeHelper.getUTCTime(getDue()));
        }
        json += "\"due\":\"" + s + "\",";
        if (getReminder().isPresent()) {
            s = String.valueOf(DateTimeHelper.getUTCTime(getReminder()));
            json += "\"reminder\":\"" + s + "\",";
        }
        json += "\"sync_state\":" + getSyncState() + ',';
        json += Tag.serialize(getId()) + ',';
        json += "\"created_at\":\""
                + DateTimeHelper.formatDateTime(getCreatedAt()) + "\",";
        json += "\"show_recurring\":" + (isRecurringShown ? "true" : "false") + ',';
        return json + "\"updated_at\":\""
               + DateTimeHelper.formatDateTime(getUpdatedAt()) + "\"}";
    }

    @Override
    public void addTag(@NonNull final Tag t) {
        addTag(t, true);
    }

    public void addTag(final Tag t, final boolean log) {
        addTag(t, log, false);
    }

    public void addTag(final Tag t, final boolean log,
                       final boolean calledFromJsonParser) {
        super.addTag(t);
        if (!saveTag(t)) {
            return;
        }
        // save task to set log+modified
        this.edited.put("tags", Boolean.TRUE);
        try {
            unsafeSave(log, calledFromJsonParser, calledFromJsonParser);
        } catch (final NoSuchListException e) {
            Log.w(Task.TAG, "List did vanish");
        }
    }

    private boolean saveTag(final Tag t) {
        if (new MirakelQueryBuilder(context).and("task_id", Operation.EQ, this)
            .and("tag_id", Operation.EQ, t)
            .count(MirakelInternalContentProvider.TAG_CONNECTION_URI) > 0) {
            // already exists;
            return false;
        }
        if (getId() != 0) {
            final ContentValues cv = new ContentValues();
            cv.put("tag_id", t.getId());
            cv.put("task_id", getId());
            insert(MirakelInternalContentProvider.TAG_CONNECTION_URI, cv);
            return true;
        }
        return false;
    }

    @Override
    public void removeTag(final Tag t) {
        removeTag(t, true);
    }

    public void removeTag(final Tag t, final boolean log) {
        removeTag(t, log, false);
    }

    public void removeTag(final Tag t, final boolean log,
                          final boolean calledFromJsonParser) {
        // save task to set log+modified
        super.removeTag(t);
        this.edited.put("tags", Boolean.TRUE);
        try {
            unsafeSave(log, calledFromJsonParser, calledFromJsonParser);
        } catch (final NoSuchListException e) {
            Log.w(Task.TAG, "List did vanish");
        }
        delete(MirakelInternalContentProvider.TAG_CONNECTION_URI,
               "task_id=? and tag_id=?",
               new String[] {String.valueOf(getId()), String.valueOf(t.getId())});
    }

    // Parcelable stuff

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(this.dependencies);
        dest.writeString(this.additionalEntriesString);
        dest.writeString(this.content);
        dest.writeSerializable(this.createdAt);
        dest.writeByte(done ? (byte) 1 : (byte) 0);
        dest.writeSerializable(this.due.orNull());
        dest.writeLong(this.list.getId());
        dest.writeInt(this.priority);
        dest.writeInt(this.progress);
        dest.writeLong(this.recurrence);
        dest.writeLong(this.recurringReminder);
        dest.writeByte(isRecurringShown ? (byte) 1 : (byte) 0);
        dest.writeSerializable(this.reminder.orNull());
        dest.writeInt(this.syncState.ordinal());
        dest.writeSerializable(this.updatedAt);
        dest.writeString(this.uuid);
        dest.writeTypedList(getTags());
        dest.writeLong(this.getId());
        dest.writeString(this.getName());
    }

    private Task(Parcel in) {
        this.dependencies = in.createStringArray();
        this.additionalEntriesString = in.readString();
        this.content = in.readString();
        this.createdAt = (Calendar) in.readSerializable();
        this.done = in.readByte() != 0;
        this.due = fromNullable((Calendar) in.readSerializable());
        long listId = in.readLong();
        Optional<ListMirakel> listMirakelOptional = ListMirakel.get(listId);
        if (listMirakelOptional.isPresent()) {
            this.list = listMirakelOptional.get();
        }
        this.priority = in.readInt();
        this.progress = in.readInt();
        this.recurrence = in.readLong();
        this.recurringReminder = in.readLong();
        this.isRecurringShown = in.readByte() != 0;
        this.reminder = fromNullable((Calendar) in.readSerializable());
        final int tmpSyncState = in.readInt();
        this.syncState = tmpSyncState == -1 ? SYNC_STATE.NOTHING : SYNC_STATE.values()[tmpSyncState];
        this.updatedAt = (Calendar) in.readSerializable();
        this.uuid = in.readString();
        in.readTypedList(getTags(), Tag.CREATOR);
        this.setId(in.readLong());
        this.setName(in.readString());
        final Optional<Task> t = get(getId());
        if (t.isPresent() && !this.equals(t.get())) {
            Task other = t.get();
            dependencies = other.dependencies;
            additionalEntriesString = other.additionalEntriesString;
            content = other.content;
            createdAt = other.createdAt;
            done = other.done;
            due = other.due;
            list = other.list;
            priority = other.priority;
            progress = other.progress;
            reminder = other.reminder;
            syncState = other.syncState;
            updatedAt = other.updatedAt;
            setId(other.getId());
            setName(other.getName());
        } else if (!t.isPresent()) {
            throw new TaskVanishedException();
        }
    }

    public static final Parcelable.Creator<Task> CREATOR = new Parcelable.Creator<Task>() {
        @Override
        public Task createFromParcel(final Parcel source) {
            return new Task(source);
        }

        @Override
        public Task[] newArray(final int size) {
            return new Task[size];
        }
    };
}
