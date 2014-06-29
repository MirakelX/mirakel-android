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
package de.azapps.mirakel.model.task;

import java.lang.String;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Pair;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.UndoHistory;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.MirakelContentProvider;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.services.NotificationService;
import de.azapps.tools.Log;

public class Task extends TaskBase {

	public static final String[] allColumns = { DatabaseHelper.ID,
			TaskBase.UUID, TaskBase.LIST_ID, DatabaseHelper.NAME,
			TaskBase.CONTENT, TaskBase.DONE, TaskBase.DUE, TaskBase.REMINDER,
			TaskBase.PRIORITY, DatabaseHelper.CREATED_AT,
			DatabaseHelper.UPDATED_AT, DatabaseHelper.SYNC_STATE_FIELD,
			TaskBase.ADDITIONAL_ENTRIES, TaskBase.RECURRING,
			TaskBase.RECURRING_REMINDER, TaskBase.PROGRESS,
			TaskBase.RECURRING_SHOWN };
	public static final String BASIC_FILTER_DISPLAY_TASKS = " NOT "
			+ DatabaseHelper.SYNC_STATE_FIELD + " = " + SYNC_STATE.DELETE
			+ " AND " + RECURRING_SHOWN + "=1";
	private static Context context;
	private static SQLiteDatabase database;

	private static DatabaseHelper dbHelper;
	private static boolean calledFromDBHelper;

	public static final String SUBTASK_TABLE = "subtasks";
	public static final String TABLE = "tasks";

	private static final String TAG = "TasksDataSource";
	private Pair<String, Integer> recurrenceParent;

	public boolean hasRecurringParent() {
		return this.recurrenceParent != null;
	}

	public static String concatColumsForQuery() {
		String cols = "";
		boolean first = true;
		for (final String c : Task.allColumns) {
			if (first) {
				first = false;
			} else {
				cols += ", ";
			}
			cols += "t." + c;
		}
		return cols;
	}

	/**
	 * Get all Tasks
	 * 
	 * @return
	 */
	public static List<Task> all() {
		final Cursor c = Task.database.query(Task.TABLE, Task.allColumns,
				BASIC_FILTER_DISPLAY_TASKS + " AND " + DONE + "=0", null, null,
				null, null);
		final List<Task> list = cursorToTaskList(c);
		c.close();
		return list;
	}

	/**
	 * Close the Database-Connection
	 */
	public static void close() {
		Task.dbHelper.close();
	}

	/**
	 * CALL THIS ONLY FROM DBHelper
	 * 
	 * @param db
	 */
	public static void setDB(final SQLiteDatabase db) {
		calledFromDBHelper = true;
		database = db;
	}

	/**
	 * Create a task from a Cursor
	 * 
	 * @param cursor
	 * @return
	 */
	public static Task cursorToTask(final Cursor cursor) {
		int i = 0;
		Calendar due;
		if (cursor.isNull(6)) {
			due = null;
		} else {
			due = DateTimeHelper.createLocalCalendar(cursor.getLong(6), true);
		}

		Calendar reminder;
		if (cursor.isNull(7)) {
			reminder = null;
		} else {
			reminder = DateTimeHelper.createLocalCalendar(cursor.getLong(7));
		}

		Calendar created_at;
		if (cursor.isNull(9)) {
			created_at = null;
		} else {
			created_at = new GregorianCalendar();
			created_at.setTimeInMillis(cursor.getLong(9) * 1000);
		}
		Calendar updated_at;
		if (cursor.isNull(10)) {
			updated_at = null;
		} else {
			updated_at = new GregorianCalendar();
			updated_at.setTimeInMillis(cursor.getLong(10) * 1000);
		}

		return new Task(cursor.getLong(i++), cursor.getString(i++),
				ListMirakel.get((int) cursor.getLong(i++)),
				cursor.getString(i++), cursor.getString(i++),
				cursor.getInt(i) == 1, due, reminder, cursor.getInt(8),
				created_at, updated_at, SYNC_STATE.valueOf(cursor.getInt(11)),
				cursor.getString(12), cursor.getInt(13), cursor.getInt(14),
				cursor.getInt(15), cursor.getShort(16) == 1);
	}

	private static List<Task> cursorToTaskList(final Cursor cursor) {
		cursor.moveToFirst();
		final List<Task> tasks = new ArrayList<>();
		while (!cursor.isAfterLast()) {
			tasks.add(cursorToTask(cursor));
			cursor.moveToNext();
		}
		cursor.close();
		return tasks;
	}

	public static void deleteDoneTasks() {
		Task.database.beginTransaction();
		final ContentValues values = new ContentValues();
		values.put("sync_state", SYNC_STATE.DELETE.toInt());
		final String where = "sync_state!=" + SYNC_STATE.ADD + " AND done=1";
		Task.database.update(Task.TABLE, values, where, null);
		Task.database.delete(Task.TABLE, "sync_state=" + SYNC_STATE.ADD
				+ " AND done=1", null);
		Task.database.setTransactionSuccessful();
		Task.database.endTransaction();
		NotificationService.updateServices(context, false);
	}

	/**
	 * Get a Task by id
	 * 
	 * @param id
	 * @return
	 */
	public static Task get(final long id) {
		return get(id,false);
	}

	/**
	 *
	 * @param id
	 * @param force Search also for deleted tasks. Do not use it unless you exactly
	 *                 know what you are doing
	 * @return
	 */
	public static Task get(final long id,boolean force) {
		String extraSQL = " and not "
				+ DatabaseHelper.SYNC_STATE_FIELD + "="
				+ SYNC_STATE.DELETE;
		if(force)
			extraSQL="";
		final Cursor cursor = Task.database.query(Task.TABLE, Task.allColumns,
				DatabaseHelper.ID + "=" + id + extraSQL, null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			final Task t = cursorToTask(cursor);
			cursor.close();
			return t;
		}
		cursor.close();
		return null;
	}

	/**
	 * Get tasks by Sync State
	 * 
	 * @param state
	 * @return
	 */
	public static List<Task> getBySyncState(final SYNC_STATE state) {
		final Cursor c = Task.database.query(Task.TABLE, Task.allColumns,
				DatabaseHelper.SYNC_STATE_FIELD + "=" + state.toInt() + " and "
						+ TaskBase.LIST_ID + ">0", null, null, null, null);
		return cursorToTaskList(c);
	}

	public static Task getByUUID(final String uuid) {
		final Cursor cursor = Task.database.query(Task.TABLE, Task.allColumns,
				TaskBase.UUID + "='" + uuid + "' AND NOT "
						+ DatabaseHelper.SYNC_STATE_FIELD + "="
						+ SYNC_STATE.DELETE, null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			final Task t = cursorToTask(cursor);
			cursor.close();
			return t;
		}
		cursor.close();
		return null;

	}

	public static Task getDummy(final Context ctx) {
		return new Task(ctx.getString(R.string.task_empty));
	}

	public static Task getDummy(final Context ctx, final ListMirakel list) {
		final Task task = new Task(ctx.getString(R.string.task_empty));
		task.setList(list, false);
		return task;
	}

	public static Task getEmpty() {
		final Task t = new Task("");
		t.setId(0);
		return t;
	}

	public static String getSorting(final int sorting) {
		String order = "";
		final String dueSort = "CASE WHEN (" + TaskBase.DUE
				+ " IS NULL) THEN datetime('now','+50 years') ELSE datetime("
				+ TaskBase.DUE
				+ ",'unixepoch','localtime','start of day') END ASC";
		switch (sorting) {
		case ListMirakel.SORT_BY_PRIO:
			order = TaskBase.PRIORITY + " desc";
			break;
		case ListMirakel.SORT_BY_OPT:
			order = ", " + TaskBase.PRIORITY + " DESC";
			//$FALL-THROUGH$
		case ListMirakel.SORT_BY_DUE:
			order = "done ASC, " + dueSort + order;
			break;
		case ListMirakel.SORT_BY_REVERT_DEFAULT:
			order = TaskBase.PRIORITY + " DESC, " + dueSort + order;
			//$FALL-THROUGH$
		default:
			if (!order.equals("")) {
				order += ", ";
			}
			order += DatabaseHelper.ID + " ASC";
		}
		return order;
	}

	public static List<Pair<Long, String>> getTaskNames() {
		final Cursor c = Task.database.query(Task.TABLE, new String[] {
				DatabaseHelper.ID, DatabaseHelper.NAME },
				BASIC_FILTER_DISPLAY_TASKS, null, null, null, null);
		c.moveToFirst();
		final List<Pair<Long, String>> names = new ArrayList<>();
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
	 * @param sorting
	 *            The Sorting (@see Mirakel.SORT_*)
	 * @param showDone
	 * @return
	 */
	public static List<Task> getTasks(final int listId, final int sorting,
			final boolean showDone) {
		final Cursor cursor = getTasksCursor(listId, sorting, showDone);
		return cursorToTaskList(cursor);
	}

	/**
	 * Get Tasks from a List Use it only if really necessary!
	 * 
	 * @param list
	 * @param sorting
	 *            The Sorting (@see Mirakel.SORT_*)
	 * @param showDone
	 * @return
	 */
	public static List<Task> getTasks(final ListMirakel list,
			final int sorting, final boolean showDone) {
		return getTasks(list.getId(), sorting, showDone);
	}

	/**
	 * Get Tasks from a List Use it only if really necessary!
	 * 
	 * @param list
	 * @param sorting
	 *            The Sorting (@see Mirakel.SORT_*)
	 * @param showDone
	 * @return
	 */
	public static List<Task> getTasks(final ListMirakel list,
			final int sorting, final boolean showDone, final String where) {
		final Cursor cursor = getTasksCursor(list.getId(), sorting, where);
		return cursorToTaskList(cursor);
	}

	/**
	 * Get a Cursor with all Tasks of a list
	 * 
	 * @param listId
	 * @param sorting
	 * @return
	 */
	private static Cursor getTasksCursor(final int listId, final int sorting,
			final boolean showDone) {
		final ListMirakel l = ListMirakel.get(listId);
		if (l == null) {
			Log.wtf(TAG, "list not found");
			return new MatrixCursor(allColumns);
		}
		String where = l.getWhereQueryForTasks();
		if (!showDone) {
			where += (where.trim().equals("") ? "" : " AND ") + " "
					+ TaskBase.DONE + "=0";
		}
		return getTasksCursor(listId, sorting, where);
	}

	/**
	 * Get a Cursor with all Tasks of a list
	 * 
	 * @param listId
	 * @param sorting
	 * @return
	 */
	private static Cursor getTasksCursor(final int listId, final int sorting,
			String where) {
		if (where == null) {
			where = "";
		} else if (!where.equals("")) {
			where += " AND ";
		}
		where += " " + BASIC_FILTER_DISPLAY_TASKS;
		String order = getSorting(sorting);

		if (listId < 0) {
			order += ", " + TaskBase.LIST_ID + " ASC";
		}
		return Task.database.query(Task.TABLE, Task.allColumns, where, null,
				null, null, TaskBase.DONE + ", " + order);
	}

	private static Cursor getTasksCursor(final Task subtask) {
		String columns = "t." + Task.allColumns[0];
		for (int i = 1; i < Task.allColumns.length; i++) {
			columns += ", t." + Task.allColumns[i];
		}
		return Task.database.rawQuery("SELECT " + columns + " FROM "
				+ Task.TABLE + " t INNER JOIN " + Task.SUBTASK_TABLE
				+ " s on t._id=s.child_id WHERE s.parent_id=? ORDER BY "
				+ getSorting(ListMirakel.SORT_BY_OPT), new String[] { ""
				+ subtask.getId() });
	}

	// Static Methods

	public static List<Task> getTasksToSync(final Account account) {
		final AccountMirakel a = AccountMirakel.get(account);
		final List<ListMirakel> lists = ListMirakel.getListsForAccount(a);
		String listIDs = "";
		boolean first = true;
		for (final ListMirakel l : lists) {
			listIDs += (first ? "" : ",") + l.getId();
			first = false;
		}
		final String where = "NOT " + DatabaseHelper.SYNC_STATE_FIELD + " IN ("
				+ SYNC_STATE.NOTHING + ") and " + TaskBase.LIST_ID + " IN ("
				+ listIDs + ")";
		final Cursor cursor = MirakelContentProvider.getReadableDatabase()
				.query(Task.TABLE, Task.allColumns, where, null, null, null,
						null);
		return cursorToTaskList(cursor);

	}

	public static List<Task> getTasksWithReminders() {
		final String where = TaskBase.REMINDER + " NOT NULL and "
				+ TaskBase.DONE + "=0";
		final Cursor cursor = MirakelContentProvider.getReadableDatabase()
				.query(Task.TABLE, Task.allColumns, where, null, null, null,
						null);
		return cursorToTaskList(cursor);
	}

	/**
	 * Get a Task to sync it
	 * 
	 * @param id
	 * @return
	 */
	public static Task getToSync(final long id) {
		final Cursor cursor = Task.database.query(Task.TABLE, Task.allColumns,
				DatabaseHelper.ID + "='" + id + "'", null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			final Task t = cursorToTask(cursor);
			cursor.close();
			return t;
		}
		cursor.close();
		return null;
	}

	/**
	 * Initialize the Database and the preferences
	 * 
	 * @param ctx
	 *            The Application-Context
	 */
	public static void init(final Context ctx) {
		Task.context = ctx;
		Task.dbHelper = new DatabaseHelper(Task.context);
		Task.database = Task.dbHelper.getWritableDatabase();
		calledFromDBHelper = false;
	}

	public static Task newTask(final String name, final ListMirakel list) {
		return newTask(name, list, "", false, null, 0);
	}

	public static Task newTask(final String name, final ListMirakel list,
			final GregorianCalendar due, final int prio) {
		return newTask(name, list, "", false, due, prio);
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
			final GregorianCalendar due, final int priority) {
		final Task t = new Task(0, java.util.UUID.randomUUID().toString(),
				list, name, content, done, due, null, priority, null, null,
				SYNC_STATE.ADD, "", -1, -1, 0, true);

		try {
			return t.create();
		} catch (final NoSuchListException e) {
			ErrorReporter.report(ErrorType.TASKS_NO_LIST);
			Log.e(Task.TAG, "NoSuchListException",e);
			return null;
		}
	}

	public static List<Task> rawQuery(final String generateQuery) {
		final Cursor c = Task.database.rawQuery(generateQuery, null);
		final List<Task> ret = cursorToTaskList(c);
		c.close();
		return ret;
	}

	public static void resetSyncState(final List<Task> tasks) {
		if (tasks.size() == 0) {
			return;
		}
		for (final Task t : tasks) {
			if (t.getSyncState() != SYNC_STATE.DELETE) {
				t.setSyncState(SYNC_STATE.NOTHING);
				try {
					Task.database.update(Task.TABLE, t.getContentValues(),
							DatabaseHelper.ID + " = " + t.getId(), null);
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

	private String dependencies[];

	public void setDependencies(final String[] dep) {
		this.dependencies = dep;
	}

	Task() {
		super();
		this.recurrenceParent = null;
	}

	public Task(final long id, final String uuid, final ListMirakel list,
			final String name, final String content, final boolean done,
			final Calendar due, final Calendar reminder, final int priority,
			final Calendar created_at, final Calendar updated_at,
			final SYNC_STATE sync_state, final String additionalEntriesString,
			final int recurring, final int recurring_reminder,
			final int progress, final boolean shown) {
		super(id, uuid, list, name, content, done, due, reminder, priority,
				created_at, updated_at, sync_state, additionalEntriesString,
				recurring, recurring_reminder, progress, shown);
		this.recurrenceParent = null;
	}

	public Task(final String name) {
		super(name);
		this.recurrenceParent = null;

	}

	void addRecurringChild(final Pair<String, Integer> newRec) {
		this.recurrenceParent = newRec;
	}

	public FileMirakel addFile(final Context ctx, final Uri uri) {
		return FileMirakel.newFile(ctx, this, uri);
	}

	public void addSubtask(final Task t) {
		if (checkIfParent(t)) {
			return;
		}
		final ContentValues cv = new ContentValues();
		cv.put("parent_id", getId());
		cv.put("child_id", t.getId());
		Task.database.beginTransaction();
		Task.database.insert(Task.SUBTASK_TABLE, null, cv);
		Task.database.setTransactionSuccessful();
		Task.database.endTransaction();
	}

	public boolean checkIfParent(final Task t) {
		return isChildRec(t);
	}

	public Task create() throws NoSuchListException {
		return create(true);
	}

	public Task create(final boolean addFlag) throws NoSuchListException {
		return create(addFlag, false);
	}

	public Task create(final boolean addFlag, final boolean calledFromSync)
			throws NoSuchListException {
		final ContentValues values = new ContentValues();
		values.put(TaskBase.UUID, getUUID());
		values.put(DatabaseHelper.NAME, getName());
		if (getList() == null) {
			throw new NoSuchListException();
		}
		values.put(TaskBase.LIST_ID, getList().getId());
		values.put(TaskBase.CONTENT, getContent());
		values.put(TaskBase.DONE, isDone());
		values.put(TaskBase.DUE,
				getDue() == null ? null : DateTimeHelper.getUTCTime(getDue()));
		values.put(TaskBase.REMINDER, getReminder() == null ? null
				: DateTimeHelper.getUTCTime(getReminder()));
		values.put(TaskBase.PRIORITY, getPriority());
		values.put(DatabaseHelper.SYNC_STATE_FIELD,
				addFlag ? SYNC_STATE.ADD.toInt() : SYNC_STATE.NOTHING.toInt());
		values.put(RECURRING_SHOWN, isRecurringShown());
		values.put(RECURRING, getRecurrenceId());
		if (getCreatedAt() == null) {
			setCreatedAt(new GregorianCalendar());
		}
		values.put(DatabaseHelper.CREATED_AT,
				getCreatedAt().getTimeInMillis() / 1000);
		if (getUpdatedAt() == null) {
			setUpdatedAt(new GregorianCalendar());
		}
		values.put(DatabaseHelper.UPDATED_AT,
				getUpdatedAt().getTimeInMillis() / 1000);
		values.put(TaskBase.PROGRESS, getProgress());

		values.put(ADDITIONAL_ENTRIES, getAdditionalEntriesString());
		Task.database.beginTransaction();
		final long insertId = Task.database.insertOrThrow(Task.TABLE, null,
				values);
		this.setId(insertId);
		Task.database.setTransactionSuccessful();
		Task.database.endTransaction();
		handleInsertTWRecurring();
		final Cursor cursor = Task.database.query(Task.TABLE, Task.allColumns,
				DatabaseHelper.ID + " = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		final Task newTask = cursorToTask(cursor);
		cursor.close();
		if (!calledFromSync) {
			UndoHistory.logCreate(newTask, Task.context);
			if (!calledFromDBHelper) {
				NotificationService.updateServices(context,
						getReminder() != null);
			}
		}
		return newTask;
	}

	public void deleteSubtask(final Task s) {
		Task.database.beginTransaction();
		Task.database.delete(Task.SUBTASK_TABLE, "parent_id=" + getId()
				+ " and child_id=" + s.getId(), null);
		Task.database.setTransactionSuccessful();
		Task.database.endTransaction();

	}

	/**
	 * Delete a task
	 *
	 */
	public void destroy() {
		destroy(false);
	}

	public void destroy(final boolean force) {
		if (!force) {
			UndoHistory.updateLog(this, Task.context);
		}
		final long id = getId();
		updateRecurringMaster();
		if (getSyncState() == SYNC_STATE.ADD || force) {
			Task.database.delete(Task.TABLE, DatabaseHelper.ID + " = " + id,
					null);
            database.execSQL("DELETE FROM "+TABLE+" WHERE "+DatabaseHelper.ID+" IN (SELECT child FROM "+Recurring.TW_TABLE+" WHERE parent="+getId()+")");
            database.execSQL("DELETE FROM "+FileMirakel.TABLE+" WHERE task"+DatabaseHelper.ID+" IN (SELECT child FROM "+Recurring.TW_TABLE+" WHERE parent="+getId()+")");
            database.delete(Recurring.TW_TABLE,"parent=?",new String[]{getId()+""});
			destroyGarbage();
		} else {
			final ContentValues values = new ContentValues();
			values.put(DatabaseHelper.SYNC_STATE_FIELD,
					SYNC_STATE.DELETE.toInt());
			Task.database.update(Task.TABLE, values, DatabaseHelper.ID + "="
					+ id, null);
		}
		NotificationService.updateServices(context, getReminder() != null);
	}

	public void destroyGarbage() {
		FileMirakel.destroyForTask(this);
		destroyRecurrenceGarbageForTask(getId());
	}

	public static void destroyRecurrenceGarbageForTask(long id) {
		Task.database.beginTransaction();
		Task.database.delete(Task.SUBTASK_TABLE, "parent_id=" + id
				+ " or child_id=" + id, null);
		Task.database.setTransactionSuccessful();
		Task.database.endTransaction();
	}

	public String[] getDependencies() {
		return this.dependencies;
	}

	public List<FileMirakel> getFiles() {
		return FileMirakel.getForTask(this);
	}

	public int getSubtaskCount() {
		final Cursor c = Task.database.rawQuery("Select count(_id) from "
				+ Task.SUBTASK_TABLE + " where parent_id=" + getId(), null);
		c.moveToFirst();
		final int count = c.getInt(0);
		c.close();
		return count;
	}

	public List<Task> getSubtasks() {
		final Cursor c = Task.getTasksCursor(this);
		final List<Task> subTasks = new ArrayList<>();
		c.moveToFirst();
		while (!c.isAfterLast()) {
			subTasks.add(cursorToTask(c));
			c.moveToNext();
		}
		c.close();
		return subTasks;

	}

	private boolean isChildRec(final Task t) {
		final List<Task> subtasks = getSubtasks();
		for (final Task s : subtasks) {
			if (s.getId() == t.getId() || s.isChildRec(t)) {
				return true;
			}
		}
		return false;
	}

	public boolean isSubtaskOf(final Task otherTask) {
		if (otherTask == null) {
			return false;
		}
		final Cursor c = Task.database.rawQuery("Select count(_id) from "
				+ Task.SUBTASK_TABLE + " where parent_id=" + otherTask.getId()
				+ " AND child_id=" + getId(), null);
		c.moveToFirst();
		final int count = c.getInt(0);
		c.close();
		return count > 0;
	}

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

	private void unsafeSave(boolean log, final boolean calledFromSync,
			final boolean updateUpdatedAt) throws NoSuchListException {
		if (!isEdited()) {
			Log.d(Task.TAG, "new Task equals old, did not need to save it");
			return;
		}
		if (calledFromDBHelper) {
			log = false;
		}
		if (isEdited(RECURRING)) {
			final Task old = Task.get(getId());
			if (old.getRecurrenceId() == -1 && getRecurrenceId() != -1
					&& !calledFromSync) {
				insertFirstRecurringChild();
			} else if (old.getRecurrenceId() != getRecurrenceId()
					&& !calledFromSync) {
				final Recurring r = getRecurring();
				if (r == null) {
                    final Cursor c=database.query(Recurring.TW_TABLE,new String[]{"parent"},"child=?",new String[]{getId()+""},null,null,null);
                    if(c.moveToFirst()&&c.getCount()>0){
                        long masterID=c.getLong(0);
                        database.execSQL("UPDATE "+TABLE+" SET "+RECURRING+"=-1 WHERE "+DatabaseHelper.ID+" IN (SELECT child FROM "+Recurring.TW_TABLE+" WHERE parent="+masterID+")");
                        database.delete(Recurring.TW_TABLE,"parent=?",new String[]{masterID+""});
                        database.delete(TABLE,DatabaseHelper.ID+"=?",new String[]{masterID+""});
                    }
                    c.close();
				} else {
					updateRecurringChilds(r);
				}

			}
		}

		if (isEdited(TaskBase.DONE) && isDone()) {
			setSubTasksDone();
		}

		setSyncState(getSyncState() == SYNC_STATE.ADD
				|| getSyncState() == SYNC_STATE.IS_SYNCED ? getSyncState()
				: SYNC_STATE.NEED_SYNC);
		if (updateUpdatedAt && Task.context != null) {
			setUpdatedAt(new GregorianCalendar());
		}
		handleInsertTWRecurring();
		final ContentValues values = getContentValues();
		if (log && !calledFromSync) {
			final Task old = Task.get(getId());
			UndoHistory.updateLog(old, Task.context);
		}
		final List<Tag> tags = getTags();
		Task.database.beginTransaction();
		if (isEdited(TaskBase.RECURRING)) {
			long master = getId();
			Cursor c = database.query(Recurring.TW_TABLE,
					new String[] { "parent" }, "child=?", new String[] { master
							+ "" }, null, null, null);
			c.moveToFirst();
			if (c.getCount() > 0) {
				master = c.getLong(0);
			}
			c.close();
			c = database.query(Recurring.TW_TABLE, new String[] { "child" },
					"parent=?", new String[] { master + "" }, null, null, null);
			boolean first = true;
			String ids = "";
			for (c.moveToFirst(); c.moveToNext();) {
				if (first) {
					first = false;
				} else {
					ids += ",";
				}
				ids += c.getLong(0);
			}
			c.close();
			final ContentValues cv = new ContentValues();
			cv.put(RECURRING, getRecurrenceId());
			database.update(TABLE, cv, DatabaseHelper.ID + " IN (?)",
					new String[] { ids });
		}
		Task.database.update(Task.TABLE, values, DatabaseHelper.ID + " = "
				+ getId(), null);
		for (final Tag t : tags) {
			saveTag(t);
		}
		boolean updateReminders = false;
		if (isEdited(TaskBase.DONE) || isEdited(TaskBase.REMINDER)
				|| isEdited(TaskBase.RECURRING_REMINDER)) {
			updateReminders = true;
		}
		clearEdited();
		Task.database.setTransactionSuccessful();
		Task.database.endTransaction();
		if (!calledFromDBHelper && !calledFromSync) {
			NotificationService.updateServices(Task.context, updateReminders);
		}

	}

	public List<Task> getRecurrenceChilds() {
		final Cursor c = database.rawQuery("SELECT " + concatColumsForQuery()
				+ " FROM " + TABLE + " AS t INNER JOIN "
				+ Recurring.TW_TABLE
				+ " AS r ON t._id=r.child WHERE r.parent =" + getId() + "  ORDER BY r.offsetCount ASC;", null);
		return cursorToTaskList(c);
	}

	public Task getRecurrenceMaster() {
		final Cursor c = database.rawQuery("SELECT " + concatColumsForQuery()
				+ " FROM " + TABLE + " AS t INNER JOIN "
				+ Recurring.TW_TABLE
				+ " AS r ON t._id=r.parent WHERE r.child =" + getId() + ";", null);
		if(c.moveToFirst()) {
			return cursorToTask(c);
		} else {
			return null;
		}
	}

	private void updateRecurringChilds(final Recurring r) {
		final Cursor c = database.rawQuery("SELECT " + concatColumsForQuery()
				+ ", r.offsetCount FROM " + TABLE + " AS t INNER JOIN "
				+ Recurring.TW_TABLE
				+ " AS r ON t._id=r.child WHERE parent IN (SELECT parent FROM "
				+ Recurring.TW_TABLE + " WHERE child=" + getId()
				+ ") ORDER BY r.offsetCount ASC;", null);
		c.moveToFirst();
		if (c.getCount() > 0) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					Task old = null;

					do {
						final Task child = cursorToTask(c);
						final int offset = c.getInt(allColumns.length);
						if (offset > 0 && old != null && r != null) {
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
					final Intent i = new Intent(DefinitionsHelper.SYNC_FINISHED);
					context.sendBroadcast(i);
				}
			}).start();
		}
	}

	public void insertFirstRecurringChild() throws NoSuchListException {
		Log.i(TAG, "changed recurrence");
		// add first
		final long oldId = getId();
		final int recID = getRecurrenceId();
		final Task child = create();
		setIsRecurringShown(false);
		// switch ids
		// recurrence can be only modified from dueReminder view
		// there the task is reloaded
		setId(child.getId());
		child.setId(oldId);
		child.save(false);
		final ContentValues cv = new ContentValues();
		cv.put("parent", getId());
		cv.put("child", child.getId());
		cv.put("offset", 0);
		cv.put("offsetCount", 0);
		database.insert(Recurring.TW_TABLE, null, cv);
		// if not done manual this will cause a stackoverflow
		cv.clear();
		cv.put(RECURRING, recID);
		cv.put(UUID, java.util.UUID.randomUUID().toString());
		cv.put(DatabaseHelper.SYNC_STATE_FIELD, SYNC_STATE.ADD.toInt());
		database.update(TABLE, cv, DatabaseHelper.ID + "=?",
				new String[] { oldId + "" });
	}

	private void handleInsertTWRecurring() {

		if (this.recurrenceParent != null) {
			final Task master = Task.getByUUID(this.recurrenceParent.first);
			if (master == null || master.getRecurring() == null) {
				// Something is very strange
				Log.wtf(TAG, toString() + " is null!?");
				return;
			}

			final Cursor c = database.query(Recurring.TW_TABLE,
					Recurring.allTWColumns, "parent=? AND child=?",
					new String[] { master.getId() + "", getId() + "" }, null,
					null, null);
			c.moveToFirst();
			if (c.getCount() < 1) {
				final ContentValues cv = new ContentValues();
				cv.put("parent", master.getId());
				cv.put("child", getId());
				cv.put("offsetCount", this.recurrenceParent.second);
				cv.put("offset", master.getRecurring().getInterval());
				database.insert(Recurring.TW_TABLE, null, cv);
			}
			c.close();
			setRecurrence(master.getRecurrenceId());
		}
		updateRecurringMaster();
		// fix showing
		fixRecurringShowing();

	}

	public static void fixRecurringShowing() {
		database.execSQL("UPDATE " + TABLE + " SET " + RECURRING_SHOWN
				+ "=0 WHERE _id IN (SELECT parent FROM " + Recurring.TW_TABLE
				+ ");");
		database.execSQL("UPDATE " + TABLE + " SET " + RECURRING_SHOWN
				+ "=1 WHERE _id IN (SELECT r.child FROM " + Recurring.TW_TABLE
				+ " AS r INNER JOIN " + TABLE + " AS t ON t._id=r.child);");
	}

	private void updateRecurringMaster() {
		// update master if child changed
		final Cursor c = database.query(Recurring.TW_TABLE,
				new String[] { "parent" }, "child=?", new String[] { getId()
						+ "" }, null, null, null);
		c.moveToFirst();
		if (c.getCount() > 0) {
			final ContentValues cv = new ContentValues();
			cv.put(DatabaseHelper.SYNC_STATE_FIELD,
					SYNC_STATE.NEED_SYNC.toInt());
			database.update(TABLE, cv, "_id=?", new String[] { c.getLong(0)
					+ "" });
		}
		c.close();
	}

	private void setSubTasksDone() {
		final List<Task> subTasks = getSubtasks();
		for (final Task t : subTasks) {
			t.setDone(true);
			t.save(false);
		}
	}

	public String toJson() {
		String json = "{";
		json += "\"id\":" + getId() + ",";
		json += "\"name\":\"" + getName() + "\",";
		json += "\"content\":\"" + getContent() + "\",";
		json += "\"done\":" + (isDone() ? "true" : "false") + ",";
		json += "\"priority\":" + getPriority() + ",";
		json += "\"list_id\":" + getList().getId() + ",";
		String s = "";
		if (getDue() != null) {
			s = DateTimeHelper.formatDate(getDue());
		}
		json += "\"due\":\"" + s + "\",";
		s = "";
		if (getReminder() != null) {
			s = DateTimeHelper.formatDateTime(getReminder());
		}
		json += "\"reminder\":\"" + s + "\",";
		json += "\"sync_state\":" + getSyncState() + ",";
		json += Tag.serialize(getId()) + ",";
		json += "\"created_at\":\""
				+ DateTimeHelper.formatDateTime(getCreatedAt()) + "\",";
		json += "\"updated_at\":\""
				+ DateTimeHelper.formatDateTime(getUpdatedAt()) + "\"}";
		return json;
	}

	@Override
	public void addTag(final Tag t) {
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
		this.edited.put("tags", true);
		try {
			unsafeSave(log, calledFromJsonParser, calledFromJsonParser);
		} catch (final NoSuchListException e) {
			Log.w(Task.TAG, "List did vanish");
		}

	}

	private boolean saveTag(final Tag t) {
		final Cursor c = database.query(Tag.TAG_CONNECTION_TABLE,
				new String[] { "count(*)" }, "task_id=? and tag_id=?",
				new String[] { getId() + "", t.getId() + "" }, null, null,
				null, null);
		c.moveToFirst();
		if (c.getCount() > 0 && c.getInt(0) > 0) {
			c.close();
			// already exists;
			return false;
		}
		c.close();
		if (getId() != 0) {
			final ContentValues cv = new ContentValues();
			cv.put("tag_id", t.getId());
			cv.put("task_id", getId());
			database.insert(Tag.TAG_CONNECTION_TABLE, null, cv);
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
		this.edited.put("tags", true);
		try {
			unsafeSave(log, calledFromJsonParser, calledFromJsonParser);
		} catch (final NoSuchListException e) {
			Log.w(Task.TAG, "List did vanish");
		}
		database.delete(Tag.TAG_CONNECTION_TABLE, "task_id=? and tag_id=?",
				new String[] { getId() + "", t.getId() + "" });
	}
}
