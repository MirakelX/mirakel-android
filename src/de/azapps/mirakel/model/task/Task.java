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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.GetChars;
import android.util.Log;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;

public class Task extends TaskBase {

	public static final String TABLE = "tasks";

	public Task(long id, ListMirakel list, String name, String content,
			boolean done, GregorianCalendar due, int priority,
			String created_at, String updated_at, int sync_state) {
		super(id, list, name, content, done, due, priority, created_at,
				updated_at, sync_state);
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
	public void save() {
		setSyncState(getSync_state() == Mirakel.SYNC_STATE_ADD
				|| getSync_state() == Mirakel.SYNC_STATE_IS_SYNCED ? getSync_state()
				: Mirakel.SYNC_STATE_NEED_SYNC);
		setUpdatedAt(new SimpleDateFormat(
				context.getString(R.string.dateTimeFormat), Locale.getDefault())
				.format(new Date()));
		ContentValues values = getContentValues();
		database.update(TABLE, values, "_id = " + getId(), null);
	}

	/**
	 * Delete a task
	 * 
	 * @param task
	 */
	public void delete() {
		long id = getId();
		if (getSync_state() == Mirakel.SYNC_STATE_ADD)
			database.delete(TABLE, "_id = " + id, null);
		else {
			ContentValues values = new ContentValues();
			values.put("sync_state", Mirakel.SYNC_STATE_DELETE);
			database.update(TABLE, values, "_id=" + id, null);
		}

	}

	// Static Methods

	private static final String TAG = "TasksDataSource";
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	private static final String[] allColumns = { "_id", "list_id", "name",
			"content", "done", "due", "priority", "created_at", "updated_at",
			"sync_state" };
	private static Context context;

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
		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("list_id", list_id);
		values.put("content", content);
		values.put("done", done);
		values.put("due", (due == null ? null : due.toString()));
		values.put("priority", priority);
		values.put("sync_state", Mirakel.SYNC_STATE_ADD);
		values.put("created_at",
				new SimpleDateFormat(
						context.getString(R.string.dateTimeFormat), Locale.US)
						.format(new Date()));
		values.put("updated_at",
				new SimpleDateFormat(
						context.getString(R.string.dateTimeFormat), Locale.US)
						.format(new Date()));
		long insertId = database.insert(TABLE, null, values);
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
				+ Mirakel.SYNC_STATE_DELETE, null, null, null, null);
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
				+ "' and not sync_state=" + Mirakel.SYNC_STATE_DELETE, null,
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
		Cursor cursor = getTasksCursor(listId, sorting);
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
		if (result.length() < 3)
			return null;
		List<Task> tasks = new ArrayList<Task>();
		result = result.substring(1, result.length() - 2);
		String tasks_str[] = result.split(",");
		Task t = null;
		for (int i = 0; i < tasks_str.length; i++) {
			String key_value[] = tasks_str[i].split(":");
			if (key_value.length < 2)
				continue;
			String key = key_value[0];
			if (key.equals("{\"content\"")) {
				t = new Task();
				t.setSyncState(Mirakel.SYNC_STATE_NOTHING);
				if (key_value[1].indexOf("null")<2) {
					t.setContent(null);
				} else{
					t.setContent(key_value[1].substring(1,
							key_value[1].length() - 1));
				}	
			} else if (key.equals("\"done\"")) {
				t.setDone(key_value[1].indexOf("true") != -1);
			} else if (key.equals("\"due\"")) {
				GregorianCalendar temp = new GregorianCalendar();
				try {
					temp.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale
							.getDefault()).parse(key_value[1].substring(1,
							key_value[1].length() - 1)));
				} catch (Exception e) {
					temp = null;
					// temp.setTime(new Date(0));
					Log.e(TAG,
							"Can not parse Date! "
									+ (key_value[1].substring(1,
											key_value[1].length() - 1)));
				}
				t.setDue(temp);
			} else if (key.equals("\"id\"")) {
				t.setId(Long.parseLong(key_value[1]));
			} else if (key.equals("\"list_id\"")) {
				t.setList(ListMirakel.getList((int) Long
						.parseLong(key_value[1])));
			} else if (key.equals("\"name\"")) {
				t.setName(key_value[1].substring(1, key_value[1].length() - 1));
			} else if (key.equals("\"priority\"")) {
				t.setPriority(Integer.parseInt(key_value[1]));
			} else if (key.equals("\"created_at\"")) {
				t.setCreatedAt(key_value.length == 4 ? key_value[1]
						.substring(1)
						+ key_value[2]
						+ key_value[3].substring(0, key_value[3].length() - 1)
						: "");
			} else if (key.equals("\"updated_at\"")) {
				t.setUpdatedAt(key_value.length == 4 ? key_value[1]
						.substring(1)
						+ key_value[2]
						+ key_value[3].substring(0, key_value[3].length() - 2)
						: "");
				tasks.add(t);
			}
		}
		return tasks;
	}

	/**
	 * Create a task from a Cursor
	 * 
	 * @param cursor
	 * @return
	 */
	private static Task cursorToTask(Cursor cursor) {
		int i = 0;
		GregorianCalendar t = new GregorianCalendar();
		try {
			t.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
					.parse(cursor.getString(5)));
		} catch (ParseException e) {
			t = null;
		} catch (NullPointerException e) {
			t = null;
		}

		Task task = new Task(cursor.getLong(i++),
				ListMirakel.getList((int) cursor.getLong(i++)),
				cursor.getString(i++), cursor.getString(i++),
				cursor.getInt((i++)) == 1, t, cursor.getInt(++i),
				cursor.getString(++i), cursor.getString(++i),
				cursor.getInt(++i));
		return task;
	}

	/**
	 * Get a Cursor with all Tasks of a list
	 * 
	 * @param listId
	 * @param sorting
	 * @return
	 */
	private static Cursor getTasksCursor(int listId, int sorting) {
		String where = "list_id='" + listId + "'";
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
		where += " not sync_state=" + Mirakel.SYNC_STATE_DELETE;
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
		Log.v(TAG, order);
		return Mirakel.getReadableDatabase().query(TABLE, allColumns, where,
				null, null, null, "done, " + order);
	}
}
