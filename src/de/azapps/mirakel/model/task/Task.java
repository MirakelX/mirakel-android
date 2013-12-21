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
package de.azapps.mirakel.model.task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.UndoHistory;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_STATE;
import de.azapps.mirakelandroid.R;

public class Task extends TaskBase {

	public static final String TABLE = "tasks";
	public static final String SUBTASK_TABLE = "subtasks";
	private String dependencies[];

	public Task(long id, String uuid, ListMirakel list, String name,
			String content, boolean done, Calendar due, Calendar reminder,
			int priority, Calendar created_at, Calendar updated_at,
			SYNC_STATE sync_state, String additionalEntriesString,
			int recurring, int recurring_reminder, int progress) {
		super(id, uuid, list, name, content, done, due, reminder, priority,
				created_at, updated_at, sync_state, additionalEntriesString,
				recurring, recurring_reminder, progress);
	}

	Task() {
		super();
	}

	public Task(String name) {
		super(name);
	}

	/**
	 * Save a Task
	 * 
	 * @param task
	 */
	public void save() throws NoSuchListException {
		save(true);
	}

	/**
	 * Save a Task
	 * 
	 * @param task
	 */
	public void safeSave() {
		try {
			save();
		} catch (NoSuchListException e) {
			Log.w(TAG, "List did vanish");
		}
	}

	public void save(boolean log) throws NoSuchListException {
		if (!isEdited()) {
			Log.d(TAG, "new Task equals old, didnt need to save it");
			return;
		}
		if (isEdited(DONE) && isDone()) {
			setSubTasksDone();
		}
		setSyncState(getSyncState() == SYNC_STATE.ADD
				|| getSyncState() == SYNC_STATE.IS_SYNCED ? getSyncState()
				: SYNC_STATE.NEED_SYNC);
		if (context != null)
			setUpdatedAt(new GregorianCalendar());
		ContentValues values = getContentValues();
		if (log) {
			Task old = Task.get(getId());
			UndoHistory.updateLog(old, context);
		}
		database.beginTransaction();
		database.update(TABLE, values, DatabaseHelper.ID + " = " + getId(),
				null);
		database.setTransactionSuccessful();
		database.endTransaction();
		if (isEdited(REMINDER) || isEdited(RECURRING_REMINDER)) {
			ReminderAlarm.updateAlarms(context);
		}
		clearEdited();
	}

	private void setSubTasksDone() {
		List<Task> subTasks = getSubtasks();
		for (Task t : subTasks) {
			t.setDone(true);
			try {
				t.save();
			} catch (NoSuchListException e) {
				Log.d(TAG, "List did vanish");
			}
		}
	}

	@Override
	public boolean equals(Object o) {

		if (!(o instanceof Task))
			return false;
		Task t = (Task) o;
		// Id
		if (getId() != t.getId())
			return false;

		// List
		if (t.getList() != null && getList() != null) {
			if (t.getList().getId() != getList().getId())
				return false;
		} else if (t.getList() != null || getList() != null)
			return false;

		// Name
		if (t.getName() != null && getName() != null) {
			if (!t.getName().equals(getName()))
				return false;
		} else if (getName() != null || t.getName() != null)
			return false;

		// Content
		if (t.getContent() != null && getContent() != null) {
			if (!t.getContent().equals(getContent()))
				return false;
		} else if (getContent() != null || t.getContent() != null)
			return false;

		// Done
		if (t.isDone() != isDone())
			return false;

		// Due
		if (t.getDue() != null && getDue() != null) {
			if (t.getDue().compareTo(getDue()) != 0)
				return false;
		} else if (t.getDue() != null || getDue() != null) {
			return false;
		}

		// Priority
		if (t.getPriority() != getPriority())
			return false;
		// Additional Entries
		if (t.getAdditionalEntries() != null && getAdditionalEntries() != null) {
			if (!t.getAdditionalEntries().equals(getAdditionalEntries()))
				return false;
		} else if (t.getAdditionalEntries() != null
				|| getAdditionalEntries() != null) {
			return false;
		}
		// Reminder
		if (t.getReminder() != null && getReminder() != null) {
			if (t.getReminder().compareTo(getReminder()) != 0)
				return false;
		} else if (getReminder() != null || t.getReminder() != null) {
			return false;
		}

		// progress
		if (t.getProgress() != getProgress()) {
			return false;
		}

		return true;
	}

	/**
	 * Delete a task
	 * 
	 * @param task
	 */
	public void destroy() {
		destroy(false);
	}

	public void destroy(boolean force) {
		if (!force)
			UndoHistory.updateLog(this, context);
		long id = getId();
		if (getSyncState() == SYNC_STATE.ADD || force) {
			database.delete(TABLE, DatabaseHelper.ID + " = " + id, null);
			FileMirakel.destroyForTask(this);
			database.delete(SUBTASK_TABLE, "parent_id=" + id + " or child_id="
					+ id, null);
		} else {
			ContentValues values = new ContentValues();
			values.put(SyncAdapter.SYNC_STATE, SYNC_STATE.DELETE.toInt());
			database.update(TABLE, values, DatabaseHelper.ID + "=" + id, null);
		}

	}

	public List<Task> getSubtasks() {
		Cursor c = Task.getTasksCursor(this);
		List<Task> subTasks = new ArrayList<Task>();
		c.moveToFirst();
		while (!c.isAfterLast()) {
			subTasks.add(cursorToTask(c));
			c.moveToNext();
		}
		c.close();
		return subTasks;

	}

	public int getSubtaskCount() {
		Cursor c = database.rawQuery("Select count(_id) from " + SUBTASK_TABLE
				+ " where parent_id=" + getId(), null);
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		return count;
	}

	public static List<Pair<Long, String>> getTaskNames() {
		Cursor c = database.query(TABLE, new String[] { DatabaseHelper.ID,
				DatabaseHelper.NAME }, "not " + SyncAdapter.SYNC_STATE + "="
				+ SYNC_STATE.DELETE + " and done = 0", null, null, null, null);
		c.moveToFirst();
		List<Pair<Long, String>> names = new ArrayList<Pair<Long, String>>();
		while (!c.isAfterLast()) {
			names.add(new Pair<Long, String>(c.getLong(0), c.getString(1)));
			c.moveToNext();
		}
		c.close();
		return names;
	}

	public void addSubtask(Task t) throws NoSuchListException {
		if (checkIfParent(t)) {
			return;
		}
		ContentValues cv = new ContentValues();
		cv.put("parent_id", getId());
		cv.put("child_id", t.getId());
		database.beginTransaction();
		database.insert(SUBTASK_TABLE, null, cv);
		database.setTransactionSuccessful();
		database.endTransaction();
	}

	public void deleteSubtask(Task s) {
		database.beginTransaction();
		database.delete(SUBTASK_TABLE, "parent_id=" + getId()
				+ " and child_id=" + s.getId(), null);
		database.setTransactionSuccessful();
		database.endTransaction();

	}

	public boolean isSubtaskOf(Task otherTask) {
		Cursor c = database.rawQuery("Select count(_id) from " + SUBTASK_TABLE
				+ " where parent_id=" + otherTask.getId() + " AND child_id="
				+ getId(), null);
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		return count > 0;
	}

	public boolean checkIfParent(Task t) {
		return isChildRec(t);
	}

	private boolean isChildRec(Task t) {
		List<Task> subtasks = getSubtasks();
		for (Task s : subtasks) {
			if (s.getId() == t.getId() || s.isChildRec(t)) {
				return true;
			}
		}
		return false;
	}

	public static void deleteDoneTasks() {
		database.beginTransaction();
		ContentValues values = new ContentValues();
		values.put("sync_state", SYNC_STATE.DELETE.toInt());
		String where = "sync_state!=" + SYNC_STATE.ADD + " AND done=1";
		database.update(TABLE, values, where, null);
		database.delete(TABLE, "sync_state=" + SYNC_STATE.ADD + " AND done=1",
				null);
		database.setTransactionSuccessful();
		database.endTransaction();
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
		json += "\"created_at\":\""
				+ DateTimeHelper.formatDateTime(getCreatedAt()) + "\",";
		json += "\"updated_at\":\""
				+ DateTimeHelper.formatDateTime(getUpdatedAt()) + "\"}";
		return json;
	}

	public List<FileMirakel> getFiles() {
		return FileMirakel.getForTask(this);
	}

	public FileMirakel addFile(Context ctx, String path) {
		return FileMirakel.newFile(ctx, this, path);
	}

	// Static Methods

	private static final String TAG = "TasksDataSource";
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	public static final String[] allColumns = { DatabaseHelper.ID, UUID,
			LIST_ID, DatabaseHelper.NAME, CONTENT, DONE, DUE, REMINDER,
			PRIORITY, DatabaseHelper.CREATED_AT, DatabaseHelper.UPDATED_AT,
			SyncAdapter.SYNC_STATE, ADDITIONAL_ENTRIES, RECURRING,
			RECURRING_REMINDER, PROGRESS };

	private static Context context;

	public static Task getDummy(Context ctx) {
		return new Task(ctx.getString(R.string.task_empty));
	}

	public static Task getDummy(Context ctx, ListMirakel list) {
		Task task = new Task(ctx.getString(R.string.task_empty));
		task.setList(list);
		return task;
	}

	/**
	 * Initialize the Database and the preferences
	 * 
	 * @param context
	 *            The Application-Context
	 */
	public static void init(Context context) {
		Task.context = context;
		dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
	}

	/**
	 * Close the Database-Connection
	 */
	public static void close() {
		dbHelper.close();
	}

	public static Task newTask(String name, ListMirakel list,
			GregorianCalendar due, int prio) {
		return newTask(name, list, "", false, due, prio);
	}

	public static Task newTask(String name, ListMirakel list) {
		return newTask(name, list, "", false, null, 0);
	}

	/**
	 * Create a new Task
	 * 
	 * @param name
	 * @param list_id
	 * @param content
	 * @param done
	 * @param due
	 * @param priority
	 * @return
	 */

	public static Task newTask(String name, ListMirakel list, String content,
			boolean done, GregorianCalendar due, int priority) {
		Calendar now = new GregorianCalendar();
		Task t = new Task(0, java.util.UUID.randomUUID().toString(), list,
				name, content, done, due, null, priority, now, now,
				SYNC_STATE.ADD, "", -1, -1, 0);

		try {
			return t.create();
		} catch (NoSuchListException e) {
			Log.wtf(TAG, "List vanish");
			Log.e(TAG, Log.getStackTraceString(e));
			Toast.makeText(context, R.string.no_lists, Toast.LENGTH_LONG)
					.show();
			return null;
		}
	}

	public Task create() throws NoSuchListException {
		ContentValues values = new ContentValues();
		values.put(UUID, getUUID());
		values.put(DatabaseHelper.NAME, getName());
		if (getList() == null)
			throw new NoSuchListException();
		values.put(LIST_ID, getList().getId());
		values.put(CONTENT, getContent());
		values.put(DONE, isDone());
		values.put(DUE,
				(getDue() == null ? null : DateTimeHelper.formatDate(getDue())));
		values.put(PRIORITY, getPriority());
		values.put(SyncAdapter.SYNC_STATE, SYNC_STATE.ADD.toInt());
		values.put(DatabaseHelper.CREATED_AT,
				DateTimeHelper.formatDateTime(getCreatedAt()));
		if (getUpdatedAt() == null)
			setUpdatedAt(new GregorianCalendar());
		values.put(DatabaseHelper.UPDATED_AT,
				DateTimeHelper.formatDateTime(getUpdatedAt()));
		values.put(PROGRESS, getProgress());
		database.beginTransaction();
		long insertId = database.insertOrThrow(TABLE, null, values);
		database.setTransactionSuccessful();
		database.endTransaction();
		Cursor cursor = database.query(TABLE, allColumns, DatabaseHelper.ID
				+ " = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		Task newTask = cursorToTask(cursor);
		cursor.close();
		UndoHistory.logCreate(newTask, context);
		return newTask;
	}

	/**
	 * Get all Tasks
	 * 
	 * @return
	 */
	public static List<Task> all() {
		List<Task> tasks = new ArrayList<Task>();
		Cursor c = database.query(TABLE, allColumns, "not "
				+ SyncAdapter.SYNC_STATE + "= " + SYNC_STATE.DELETE, null,
				null, null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			tasks.add(cursorToTask(c));
			c.moveToNext();
		}
		return tasks;
	}

	/**
	 * Get a Task by id
	 * 
	 * @param id
	 * @return
	 */
	public static Task get(long id) {
		Cursor cursor = database.query(TABLE, allColumns, DatabaseHelper.ID
				+ "='" + id + "' and not " + SyncAdapter.SYNC_STATE + "="
				+ SYNC_STATE.DELETE, null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			Task t = cursorToTask(cursor);
			cursor.close();
			return t;
		}
		return null;
	}

	public static Task getByUUID(String uuid) {
		Cursor cursor = database.query(TABLE, allColumns, UUID + "='" + uuid
				+ "' and not " + SyncAdapter.SYNC_STATE + "="
				+ SYNC_STATE.DELETE, null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			Task t = cursorToTask(cursor);
			cursor.close();
			return t;
		}
		cursor.close();
		return null;

	}

	/**
	 * Get a Task to sync it
	 * 
	 * @param id
	 * @return
	 */
	public static Task getToSync(long id) {
		Cursor cursor = database.query(TABLE, allColumns, DatabaseHelper.ID
				+ "='" + id + "'", null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			Task t = cursorToTask(cursor);
			cursor.close();
			return t;
		}
		return null;
	}

	/**
	 * Search Tasks
	 * 
	 * @param id
	 * @return
	 */
	public static List<Task> searchName(String query) {
		String[] args = { "%" + query + "%" };
		Cursor cursor = database.query(TABLE, allColumns, DatabaseHelper.NAME
				+ " LIKE ?", args, null, null, null);
		return cursorToTaskList(cursor);
	}

	private static List<Task> cursorToTaskList(Cursor cursor) {
		cursor.moveToFirst();
		List<Task> tasks = new ArrayList<Task>();
		while (!cursor.isAfterLast()) {
			tasks.add(cursorToTask(cursor));
			cursor.moveToNext();
		}
		cursor.close();
		return tasks;
	}

	public static List<Task> search(String query) {
		Cursor cursor = database.query(TABLE, allColumns, query, null, null,
				null, null);
		return cursorToTaskList(cursor);

	}

	/**
	 * Get tasks by Sync State
	 * 
	 * @param state
	 * @return
	 */
	public static List<Task> getBySyncState(SYNC_STATE state) {
		Cursor c = database.query(TABLE, allColumns, SyncAdapter.SYNC_STATE
				+ "=" + state.toInt() + " and " + LIST_ID + ">0", null, null,
				null, null);
		return cursorToTaskList(c);
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
	public static List<Task> getTasks(ListMirakel list, int sorting,
			boolean showDone) {
		return getTasks(list.getId(), sorting, showDone);
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
	public static List<Task> getTasks(ListMirakel list, int sorting,
			boolean showDone, String where) {
		Cursor cursor = getTasksCursor(list.getId(), sorting, where);
		return cursorToTaskList(cursor);
	}

	public static List<Task> getTasksWithReminders() {
		String where = REMINDER + " NOT NULL and " + DONE + "=0";
		Cursor cursor = Mirakel.getReadableDatabase().query(TABLE, allColumns,
				where, null, null, null, null);
		return cursorToTaskList(cursor);
	}

	public static List<Task> getTasksToSync(Account account) {
		AccountMirakel a = AccountMirakel.get(account);
		List<ListMirakel> lists = ListMirakel.getListsForAccount(a);
		String listIDs = "";
		boolean first = true;
		for (ListMirakel l : lists) {
			listIDs += (first ? "" : ",") + l.getId();
			first = false;
		}
		String where = "NOT " + SyncAdapter.SYNC_STATE + "='"
				+ SYNC_STATE.NOTHING + "' and " + LIST_ID + " IN (" + listIDs
				+ ")";
		Cursor cursor = Mirakel.getReadableDatabase().query(TABLE, allColumns,
				where, null, null, null, null);
		return cursorToTaskList(cursor);

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
	public static List<Task> getTasks(int listId, int sorting, boolean showDone) {
		Cursor cursor = getTasksCursor(listId, sorting, showDone);
		return cursorToTaskList(cursor);
	}

	/**
	 * Parse a JSON–String to a List of Tasks
	 * 
	 * @param result
	 * @return
	 */
	public static List<Task> parse_json(String result, AccountMirakel account) {
		try {
			List<Task> tasks = new ArrayList<Task>();
			Iterator<JsonElement> i = new JsonParser().parse(result)
					.getAsJsonArray().iterator();
			while (i.hasNext()) {
				JsonObject el = (JsonObject) i.next();
				Task t = parse_json(el, account);
				tasks.add(t);
			}
			return tasks;
		} catch (Exception e) {
			Log.e(TAG, "Cannot parse response");
			Log.e(TAG, result);
			Log.d(TAG, Log.getStackTraceString(e));
		}
		return new ArrayList<Task>();
	}

	/**
	 * Parses a JSON-Object to a task
	 * 
	 * @param el
	 * @return
	 */
	public static Task parse_json(JsonObject el, AccountMirakel account) {
		Task t = null;
		JsonElement id = el.get("id");
		if (id != null) {
			t = Task.get(id.getAsLong());
		} else {
			id = el.get("uuid");
			if (id != null)
				t = Task.getByUUID(id.getAsString());
		}
		if (t == null) {
			t = new Task();
		}

		// Name
		Set<Entry<String, JsonElement>> entries = el.entrySet();
		for (Entry<String, JsonElement> entry : entries) {
			String key = entry.getKey();
			JsonElement val = entry.getValue();
			if (key == null || key.equals("id"))
				continue;
			if (key.equals("uuid")) {
				t.setUUID(val.getAsString());
			} else if (key.equals("name") || key.equals("description")) {
				t.setName(val.getAsString());
			} else if (key.equals("content")) {
				String content = val.getAsString();
				if (content == null)
					content = "";
				t.setContent(content);
			} else if (key.equals("priority")) {
				String prioString = val.getAsString().trim();
				if (prioString.equals("L") && t.getPriority() != -1) {
					t.setPriority(-2);
				} else if (prioString.equals("M")) {
					t.setPriority(1);
				} else if (prioString.equals("H")) {
					t.setPriority(2);
				} else if (!prioString.equals("L")) {
					t.setPriority(val.getAsInt());
				}
			} else if (key.equals("list_id")) {
				ListMirakel list = ListMirakel.getList(val.getAsInt());
				if (list == null)
					list = SpecialList.firstSpecial().getDefaultList();
				t.setList(list);
			} else if (key.equals("project")) {
				ListMirakel list = ListMirakel.findByName(val.getAsString());
				if (list == null
						|| list.getAccount().getId() != account.getId()) {
					list = ListMirakel.newList(val.getAsString(),
							ListMirakel.SORT_BY_OPT, account);
				}
				t.setList(list);
			} else if (key.equals("created_at")) {
				t.setCreatedAt(val.getAsString().replace(":", ""));
			} else if (key.equals("updated_at")) {
				t.setUpdatedAt(val.getAsString().replace(":", ""));
			} else if (key.equals("entry")) {
				t.setCreatedAt(parseDate(val.getAsString(),
						context.getString(R.string.TWDateFormat)));
			} else if (key.equals("modification")) {
				t.setUpdatedAt(parseDate(val.getAsString(),
						context.getString(R.string.TWDateFormat)));
			} else if (key.equals("done")) {
				t.setDone(val.getAsBoolean());
			} else if (key.equals("status")) {
				String status = val.getAsString();
				if (status.equals("pending")) {
					t.setDone(false);
				} else if (status.equals("deleted")) {
					t.setSyncState(SYNC_STATE.DELETE);
				} else {
					t.setDone(true);
				}
				t.addAdditionalEntry(key, val.getAsString());
				// TODO don't ignore waiting and recurring!!!
			} else if (key.equals("due")) {
				Calendar due = parseDate(val.getAsString(), "yyyy-MM-dd");
				if (due == null) {
					due = parseDate(val.getAsString(),
							context.getString(R.string.TWDateFormat));
				}
				t.setDue(due);
			} else if (key.equals("reminder")) {
				Calendar reminder = parseDate(val.getAsString(), "yyyy-MM-dd");
				if (reminder == null) {
					reminder = parseDate(val.getAsString(),
							context.getString(R.string.TWDateFormat));
				}
				t.setReminder(reminder);
			} else if (key.equals("annotations")) {
				String content = "";
				try {
					JsonArray annotations = val.getAsJsonArray();
					boolean first = true;
					for (JsonElement a : annotations) {
						if (first)
							first = false;
						else
							content += "\n";
						content += a.getAsJsonObject().get("description")
								.getAsString();
					}
				} catch (Exception e) {
					Log.e(TAG, "cannot parse json");
				}
				t.setContent(content);
			} else if (key.equals("content")) {
				t.setContent(val.getAsString());
			} else if (key.equals("sync_state")) {
				t.setSyncState(SYNC_STATE.parseInt(val.getAsInt()));
			} else if (key.equals("depends")) {
				t.setDependencies(val.getAsString().split(","));
			} else {
				t.addAdditionalEntry(key, val.getAsString());
			}
		}
		if (t.getList() == null) {
			ListMirakel l = MirakelPreferences.getImportDefaultList(true);
			t.setList(l);
		}
		return t;
	}

	private void setDependencies(String[] dep) {
		dependencies = dep;
	}

	public String[] getDependencies() {
		return dependencies;
	}

	private static Calendar parseDate(String date, String format) {
		GregorianCalendar temp = new GregorianCalendar();
		try {
			temp.setTime(new SimpleDateFormat(format, Locale.getDefault())
					.parse(date));
			return temp;
		} catch (ParseException e) {
			return (Calendar) null;
		}
	}

	/**
	 * Create a task from a Cursor
	 * 
	 * @param cursor
	 * @return
	 */
	private static Task cursorToTask(Cursor cursor) {
		int i = 0;
		GregorianCalendar due = new GregorianCalendar();
		SimpleDateFormat dateTimeFormat = new SimpleDateFormat(
				"yyyy-MM-dd'T'kkmmss'Z'", Locale.getDefault());
		try {
			due.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
					.parse(cursor.getString(6)));
		} catch (ParseException e) {
			due = null;
		} catch (NullPointerException e) {
			due = null;
		}
		GregorianCalendar reminder = new GregorianCalendar();
		try {
			reminder.setTime(dateTimeFormat.parse(cursor.getString(7)));
		} catch (Exception e) {
			reminder = null;
		}
		GregorianCalendar created_at = new GregorianCalendar();
		try {
			created_at.setTime(dateTimeFormat.parse(cursor.getString(9)));
		} catch (Exception e) {
			created_at = new GregorianCalendar();
		}
		GregorianCalendar updated_at = new GregorianCalendar();
		try {
			updated_at.setTime(dateTimeFormat.parse(cursor.getString(10)));
		} catch (Exception e) {
			updated_at = new GregorianCalendar();
		}

		Task task = new Task(cursor.getLong(i++), cursor.getString(i++),
				ListMirakel.getList((int) cursor.getLong(i++)),
				cursor.getString(i++), cursor.getString(i++),
				cursor.getInt((i++)) == 1, due, reminder, cursor.getInt(8),
				created_at, updated_at, SYNC_STATE.parseInt(cursor.getInt(11)),
				cursor.getString(12), cursor.getInt(13), cursor.getInt(14),
				cursor.getInt(15));
		return task;
	}

	/**
	 * Get a Cursor with all Tasks of a list
	 * 
	 * @param listId
	 * @param sorting
	 * @return
	 */
	private static Cursor getTasksCursor(int listId, int sorting,
			boolean showDone) {
		String where;
		if (listId < 0) {
			where = SpecialList.getSpecialList(-1 * listId).getWhereQuery(true);
		} else {
			where = "list_id='" + listId + "'";
		}
		if (!showDone) {
			where += (where.trim().equals("") ? "" : " AND ") + " " + DONE
					+ "=0";
		}
		return getTasksCursor(listId, sorting, where);
	}

	private static String getSorting(int sorting) {
		String order = "";
		switch (sorting) {
		case ListMirakel.SORT_BY_PRIO:
			order = PRIORITY + " desc";
			break;
		case ListMirakel.SORT_BY_OPT:
			order = ", " + PRIORITY + " DESC";
		case ListMirakel.SORT_BY_DUE:
			order = " CASE WHEN (" + DUE
					+ " IS NULL) THEN date('now','+1000 years') ELSE date("
					+ DUE + ") END ASC" + order;
			break;
		case ListMirakel.SORT_BY_REVERT_DEFAULT:
			order = PRIORITY + " DESC,  CASE WHEN (" + DUE
					+ " IS NULL) THEN date('now','+1000 years') ELSE date("
					+ DUE + ") END ASC" + order;
		default:
			order = DatabaseHelper.ID + " ASC";
		}
		return order;
	}

	private static Cursor getTasksCursor(Task subtask) {
		String columns = "t." + allColumns[0];
		for (int i = 1; i < allColumns.length; i++) {
			columns += ", t." + allColumns[i];
		}
		return database.rawQuery("SELECT " + columns + " FROM " + TABLE
				+ " t INNER JOIN " + SUBTASK_TABLE
				+ " s on t._id=s.child_id WHERE s.parent_id=? ORDER BY "
				+ getSorting(ListMirakel.SORT_BY_OPT), new String[] { ""
				+ subtask.getId() });
	}

	/**
	 * Get a Cursor with all Tasks of a list
	 * 
	 * @param listId
	 * @param sorting
	 * @return
	 */
	private static Cursor getTasksCursor(int listId, int sorting, String where) {
		if (where == null) {
			where = "";
		} else if (!where.equals("")) {
			where += " and ";
		}
		where += " not " + SyncAdapter.SYNC_STATE + "=" + SYNC_STATE.DELETE;
		String order = getSorting(sorting);

		if (listId < 0)
			order += ", " + LIST_ID + " ASC";
		return database.query(TABLE, allColumns, where, null, null, null, DONE
				+ ", " + order);
	}

	public static void resetSyncState(List<Task> tasks) {
		if (tasks.size() == 0) {
			return;
		}
		for (Task t : tasks) {
			if (t.getSyncState() != SYNC_STATE.DELETE) {
				t.setSyncState(SYNC_STATE.NOTHING);
				try {
					database.update(TABLE, t.getContentValues(),
							DatabaseHelper.ID + " = " + t.getId(), null);
				} catch (NoSuchListException e) {
					Log.d(TAG, "List did vanish");
				} catch (Exception e) {
					t.destroy(false);
				}
			} else {
				t.destroy(true);
			}
		}
	}

	public static List<Task> rawQuery(String generateQuery) {
		Cursor c = database.rawQuery(generateQuery, null);
		List<Task> ret = cursorToTaskList(c);
		c.close();
		return ret;
	}
}
