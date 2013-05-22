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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.sync.Network;

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
		setSyncState(getSync_state() == Network.SYNC_STATE.ADD
				|| getSync_state() == Network.SYNC_STATE.IS_SYNCED ? getSync_state()
				: Network.SYNC_STATE.NEED_SYNC);
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
		if (getSync_state() == Network.SYNC_STATE.ADD)
			database.delete(TABLE, "_id = " + id, null);
		else {
			ContentValues values = new ContentValues();
			values.put("sync_state", Network.SYNC_STATE.DELETE);
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
		values.put("sync_state", Network.SYNC_STATE.ADD);
		values.put("created_at",
				new SimpleDateFormat(
						context.getString(R.string.dateTimeFormat), Locale.US)
						.format(new Date()));
		values.put("updated_at",
				new SimpleDateFormat(
						context.getString(R.string.dateTimeFormat), Locale.US)
						.format(new Date()));
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
		try {
			List<Task> tasks = new ArrayList<Task>();
			Iterator<JsonElement> i = new JsonParser().parse(result)
					.getAsJsonArray().iterator();
			while (i.hasNext()) {
				JsonObject el = (JsonObject) i.next();
				Task t = new Task();
				t.setId(el.get("id").getAsLong());
				t.setName(el.get("name").getAsString());
				try{
					t.setContent(el.get("content").getAsString() == null ? "" : el
						.get("content").getAsString());
				}catch(Exception e){
					if (Mirakel.DEBUG) {
						Log.d(TAG,"Content=NULL?");
					}
					t.setContent("");
				}
				t.setPriority(el.get("priority").getAsInt());
				t.setList(ListMirakel.getList(el.get("list_id").getAsInt()));
				t.setCreatedAt(el.get("created_at").getAsString()
						.replace(":", ""));
				t.setUpdatedAt(el.get("updated_at").getAsString()
						.replace(":", ""));
				t.setDone(el.get("done").getAsBoolean());
				try {
					GregorianCalendar temp = new GregorianCalendar();
					temp.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale
							.getDefault()).parse(el.get("due").getAsString()));
					t.setDue(temp);
				} catch (Exception e) {
					t.setDue(null);
					Log.v(TAG, "Due is null");
					if (Mirakel.DEBUG)
						Log.e(TAG, "Can not parse Date! ");
				}
				tasks.add(t);
			}
			return tasks;
		} catch (Exception e) {
			Log.e(TAG, "Cannot parse response");
			if (Mirakel.DEBUG) {
				Log.e(TAG, result);
				Log.d(TAG, Log.getStackTraceString(e));
			}
		}
		return new ArrayList<Task>();
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
		Log.v(TAG, order);
		return Mirakel.getReadableDatabase().query(TABLE, allColumns, where,
				null, null, null, "done, " + order);
	}
}
