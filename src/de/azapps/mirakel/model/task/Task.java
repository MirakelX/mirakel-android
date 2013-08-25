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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.sync.Network;
import de.azapps.mirakel.sync.Network.SYNC_STATE;
import de.azapps.mirakelandroid.R;

public class Task extends TaskBase {

	public static final String TABLE = "tasks";

	public Task(long id, String uuid, ListMirakel list, String name,
			String content, boolean done, Calendar due, Calendar reminder,
			int priority, Calendar created_at, Calendar updated_at,
			int sync_state, String additionalEntriesString) {
		super(id, uuid, list, name, content, done, due, reminder, priority,
				created_at, updated_at, sync_state, additionalEntriesString);
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
		setSyncState(getSync_state() == Network.SYNC_STATE.ADD
				|| getSync_state() == Network.SYNC_STATE.IS_SYNCED ? getSync_state()
				: Network.SYNC_STATE.NEED_SYNC);
		if (context != null)
			setUpdatedAt(new GregorianCalendar());
		ContentValues values = getContentValues();
		// this.edited= new HashMap<String, Boolean>();
		database.update(TABLE, values, "_id = " + getId(), null);
	}

	/**
	 * Delete a task
	 * 
	 * @param task
	 */
	public void delete() {
		delete(false);
	}

	public void delete(boolean force) {
		long id = getId();
		if (getSync_state() == Network.SYNC_STATE.ADD || force)
			database.delete(TABLE, "_id = " + id, null);
		else {
			ContentValues values = new ContentValues();
			values.put("sync_state", Network.SYNC_STATE.DELETE);
			database.update(TABLE, values, "_id=" + id, null);
		}
	}
	
	public static void deleteDoneTasks() {
		ContentValues values = new ContentValues();
		values.put("sync_state", Network.SYNC_STATE.DELETE);
		String where="sync_state!=" + Network.SYNC_STATE.ADD +" AND done=1";
		database.update(TABLE, values, where, null);
		database.delete(TABLE, where, null);
	}

	/**
	 * Delete all Tasks, marked as deleted permanently. Use it only in the
	 * Sync-Services!!!
	 */
	public static void deleteTasksPermanently(List<Task> tasks) {
		String where = "sync_state='" + Network.SYNC_STATE.DELETE + "' AND"
				+ "_id IN(" + Helpers.makePlaceholders(tasks.size()) + ")";
		database.delete(TABLE, where, getIdsFromTaskList(tasks));
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
		json += "\"sync_state\":" + getSync_state() + ",";
		json += "\"created_at\":\"" + getCreated_at() + "\",";
		json += "\"updated_at\":\"" + getUpdated_at() + "\"}";
		return json;
		// json+="\"name\":\""+getCreated_at()+"\"";
	}

	// Static Methods

	private static final String TAG = "TasksDataSource";
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	private static final String[] allColumns = { "_id", "uuid", "list_id",
			"name", "content", "done", "due", "reminder", "priority",
			"created_at", "updated_at", "sync_state", "additional_entries" };
	private static Context context;

	public static Task getDummy(Context ctx) {
		return new Task(ctx.getString(R.string.task_empty));
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

	/**
	 * Shortcut for creating a new Task
	 * 
	 * @param name
	 * @param list_id
	 * @return
	 */
	public static Task newTask(String name, long list_id) {
		return newTask(name, list_id, "", false, null, 0);
	}

	public static Task newTask(String name, long list_id,
			GregorianCalendar due, int prio) {
		return newTask(name, list_id, "", false, due, prio);
	}

	public static Task newTask(String name, ListMirakel list) {
		return newTask(name, list.getId(), "", false, null, 0);
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
	public static Task newTask(String name, long list_id, String content,
			boolean done, GregorianCalendar due, int priority) {
		Calendar now = new GregorianCalendar();
		Task t = new Task(0, java.util.UUID.randomUUID().toString(),
				ListMirakel.getList((int) list_id), name, content, done, due,
				null, priority, now, now, Network.SYNC_STATE.ADD, "");

		return t.create();
	}

	public Task create() {
		ContentValues values = new ContentValues();
		values.put("uuid", getUUID());
		values.put("name", getName());
		values.put("list_id", getList().getId());
		values.put("content", getContent());
		values.put("done", isDone());
		values.put("due",
				(getDue() == null ? null : DateTimeHelper.formatDate(getDue())));
		values.put("priority", getPriority());
		values.put("sync_state", Network.SYNC_STATE.ADD);
		values.put("created_at", DateTimeHelper.formatDateTime(getCreated_at()));
		values.put("updated_at", DateTimeHelper.formatDateTime(getUpdated_at()));
		long insertId = database.insertOrThrow(TABLE, null, values);
		Cursor cursor = database.query(TABLE, allColumns, "_id = " + insertId,
				null, null, null, null);
		cursor.moveToFirst();
		Task newTask = cursorToTask(cursor);
		cursor.close();
		return newTask;
	}

	/**
	 * Get all Tasks
	 * 
	 * @return
	 */
	public static List<Task> all() {
		List<Task> tasks = new ArrayList<Task>();
		Cursor c = database.query(TABLE, allColumns, "not sync_state= "
				+ Network.SYNC_STATE.DELETE, null, null, null, null);
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
		Cursor cursor = database.query(TABLE, allColumns, "_id='" + id
				+ "' and not sync_state=" + Network.SYNC_STATE.DELETE, null,
				null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			Task t = cursorToTask(cursor);
			cursor.close();
			return t;
		}
		return null;
	}

	public static Task getByUUID(String uuid) {
		Cursor cursor = database.query(TABLE, allColumns, "uuid='" + uuid
				+ "' and not sync_state=" + Network.SYNC_STATE.DELETE, null,
				null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			Task t = cursorToTask(cursor);
			cursor.close();
			return t;
		}
		return null;

	}

	/**
	 * Get a Task to sync it
	 * 
	 * @param id
	 * @return
	 */
	public static Task getToSync(long id) {
		Cursor cursor = database.query(TABLE, allColumns, "_id='" + id + "'",
				null, null, null, null);
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
	public static List<Task> search(String query) {
		String[] args = { "%" + query + "%" };
		Cursor cursor = database.query(TABLE, allColumns, "name LIKE ?", args,
				null, null, null);
		cursor.moveToFirst();
		List<Task> tasks = new ArrayList<Task>();
		while (!cursor.isAfterLast()) {
			tasks.add(cursorToTask(cursor));
			cursor.moveToNext();
		}
		cursor.close();
		return tasks;
	}

	/**
	 * Get tasks by Sync State
	 * 
	 * @param state
	 * @return
	 */
	public static List<Task> getBySyncState(short state) {
		List<Task> tasks_local = new ArrayList<Task>();
		Cursor c = database.query(TABLE, allColumns, "sync_state=" + state
				+ " and list_id>0", null, null, null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			tasks_local.add(cursorToTask(c));
			c.moveToNext();
		}
		return tasks_local;
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
		List<Task> tasks = new ArrayList<Task>();
		Cursor cursor = getTasksCursor(list.getId(), sorting, where);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			tasks.add(cursorToTask(cursor));
			cursor.moveToNext();
		}
		cursor.close();
		return tasks;
	}

	public static List<Task> getTasksWithReminders() {
		String where = "reminder NOT NULL and done=0";
		Cursor cursor = Mirakel.getReadableDatabase().query(TABLE, allColumns,
				where, null, null, null, null);
		List<Task> tasks = new ArrayList<Task>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			tasks.add(cursorToTask(cursor));
			cursor.moveToNext();
		}
		cursor.close();
		return tasks;
	}

	public static List<Task> getTasksToSync() {
		String where = "sync_state!='" + Network.SYNC_STATE.NOTHING + "'";
		Cursor cursor = Mirakel.getReadableDatabase().query(TABLE, allColumns,
				where, null, null, null, null);
		List<Task> tasks = new ArrayList<Task>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			tasks.add(cursorToTask(cursor));
			cursor.moveToNext();
		}
		cursor.close();
		return tasks;

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
		List<Task> tasks = new ArrayList<Task>();
		Cursor cursor = getTasksCursor(listId, sorting, showDone);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Task task = cursorToTask(cursor);
			tasks.add(task);
			cursor.moveToNext();
		}
		cursor.close();
		return tasks;
	}

	/**
	 * Parse a JSON–String to a List of Tasks
	 * 
	 * @param result
	 * @return
	 */
	public static List<Task> parse_json(String result) {
		try {
			List<Task> tasks = new ArrayList<Task>();
			Iterator<JsonElement> i = new JsonParser().parse(result)
					.getAsJsonArray().iterator();
			while (i.hasNext()) {
				JsonObject el = (JsonObject) i.next();
				Task t = parse_json(el);
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
	public static Task parse_json(JsonObject el) {
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
			if (key == null)
				continue;

			if (key.equals("name") || key.equals("description")) {
				t.setName(val.getAsString());
			} else if (key == "content") {
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
				if (list == null) {
					list = ListMirakel.newList(val.getAsString());
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
					t.setSyncState(Network.SYNC_STATE.DELETE);
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
			} else if (key.equals("reminder")) {
				Calendar reminder = parseDate(val.getAsString(), "yyyy-MM-dd");
				if (reminder == null) {
					reminder = parseDate(val.getAsString(),
							context.getString(R.string.TWDateFormat));
				}
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
			} else {
				t.addAdditionalEntry(key, val.getAsString());
			}
		}
		return t;
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
				created_at, updated_at, cursor.getInt(11), cursor.getString(12));
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
			where = SpecialList.getSpecialList(-1 * listId).getWhereQuery();
		} else {
			where = "list_id='" + listId + "'";
		}
		if (!showDone) {
			where += (where.trim().equals("") ? "" : " AND ") + " done=0";
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
	private static Cursor getTasksCursor(int listId, int sorting, String where) {
		if (!where.equals(""))
			where += " and ";
		where += " not sync_state=" + Network.SYNC_STATE.DELETE;
		Log.v(TAG, where);
		String order = "";

		switch (sorting) {
		case ListMirakel.SORT_BY_PRIO:
			order = "priority desc";
			break;
		case ListMirakel.SORT_BY_OPT:
			order = ", priority DESC";
		case ListMirakel.SORT_BY_DUE:
			order = " CASE WHEN (due IS NULL) THEN date('now','+1000 years') ELSE date(due) END ASC"
					+ order;
			break;
		default:
			order = "_id ASC";
		}
		if (listId < 0)
			order += ", list_id ASC";
		Log.v(TAG, order);
		return Mirakel.getReadableDatabase().query(TABLE, allColumns, where,
				null, null, null, "done, " + order);
	}

	public static void resetSyncState(List<Task> tasks) {
		ContentValues cv = new ContentValues();
		cv.put("sync_state", SYNC_STATE.NOTHING);
		String where = "_id IN(" + Helpers.makePlaceholders(tasks.size()) + ")";
		database.update(TABLE, cv, where, getIdsFromTaskList(tasks));
	}

	private static String[] getIdsFromTaskList(List<Task> tasks) {
		String ids[] = new String[tasks.size()];
		int i = 0;
		for (Task t : tasks) {
			ids[i] = String.valueOf(t.getId());
		}
		return ids;

	}

}
