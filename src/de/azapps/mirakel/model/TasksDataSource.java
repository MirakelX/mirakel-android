package de.azapps.mirakel.model;

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
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;

public class TasksDataSource {
	private static final String TAG = "TasksDataSource";
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	private static final String[] allColumns = { "_id", "list_id", "name", "content",
			"done", "due", "priority", "created_at", "updated_at", "sync_state" };
	private Context context;

	/**
	 * Initialize DataSource
	 * 
	 * @param context
	 */
	public TasksDataSource(Context context) {
		dbHelper = new DatabaseHelper(context);
		this.context = context;
	}

	/**
	 * Open the Database Connection
	 * 
	 * @throws SQLException
	 */
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	/**
	 * Close the DB–Connection
	 */
	public void close() {
		dbHelper.close();
	}

	/**
	 * Shortcut for creating a new Task
	 * 
	 * @param name
	 * @param list_id
	 * @return
	 */
	public Task createTask(String name, long list_id) {
		return createTask(name, list_id, "", false, null, 0);
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
	public Task createTask(String name, long list_id, String content,
			boolean done, GregorianCalendar due, int priority) {
		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("list_id", list_id);
		values.put("content", content);
		values.put("done", done);
		values.put("due", (due == null ? "0" : due.toString()));
		values.put("priority", priority);
		values.put("sync_state", Mirakel.SYNC_STATE_ADD);
		values.put("created_at", new SimpleDateFormat(context.getString(R.string.dateTimeFormat), Locale.US).format(new Date()));
		values.put("updated_at", new SimpleDateFormat(context.getString(R.string.dateTimeFormat), Locale.US).format(new Date()));
		long insertId = database.insert(Mirakel.TABLE_TASKS, null, values);
		Cursor cursor = database.query(Mirakel.TABLE_TASKS, allColumns,
				"_id = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		Task newTask = cursorToTask(cursor);
		cursor.close();
		return newTask;
	}

	/**
	 * Save a Task
	 * 
	 * @param task
	 */
	public void saveTask(Task task) {
		Log.v(TAG, "saveTask "+task.getId());
		task.setSync_state(task.getSync_state() == Mirakel.SYNC_STATE_ADD||task.getSync_state()==Mirakel.SYNC_STATE_IS_SYNCED ?task.getSync_state() 
				: Mirakel.SYNC_STATE_NEED_SYNC);
		task.setUpdated_at(new SimpleDateFormat(context.getString(R.string.dateTimeFormat), Locale.getDefault()).format(new Date()));
		ContentValues values = task.getContentValues();
		database.update(Mirakel.TABLE_TASKS, values, "_id = " + task.getId(), null);
	}
	

	/**
	 * Delete a task
	 * 
	 * @param task
	 */
	public void deleteTask(Task task) {
		long id = task.getId();
		if (task.getSync_state() == Mirakel.SYNC_STATE_ADD)
			database.delete(Mirakel.TABLE_TASKS, "_id = " + id, null);
		else {
			ContentValues values = new ContentValues();
			values.put("sync_state", Mirakel.SYNC_STATE_DELETE);
			database.update(Mirakel.TABLE_TASKS, values, "_id=" + id, null);
		}

	}
	public List<Task> getAllTasks(){
		List<Task> tasks=new ArrayList<Task>();
		Cursor c=database.query(Mirakel.TABLE_TASKS, allColumns, "not sync_state= "+Mirakel.SYNC_STATE_DELETE, null,null, null, null);
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
	public Task getTask(long id) {
		open();
		Cursor cursor = database.query(Mirakel.TABLE_TASKS, allColumns, "_id='" + id
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
	
	public Task getTaskToSync(long id) {
		open();
		Cursor cursor = database.query(Mirakel.TABLE_TASKS, allColumns, "_id='" + id
				+ "'", null,
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
	 * Create a task from a Cursor
	 * 
	 * @param cursor
	 * @return
	 */
	private Task cursorToTask(Cursor cursor) {
		int i = 0;
		GregorianCalendar t = new GregorianCalendar();
		try {
			t.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
					.parse(cursor.getString(5)));
		} catch (ParseException e) {
			t.setTime(new Date(0));
		} catch(NullPointerException e){
			t.setTime(new Date(0));
		}

		Task task = new Task(cursor.getLong(i++), cursor.getLong(i++),
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
	private Cursor getTasksCursor(int listId, int sorting) {
		String where = "";
		switch (listId) {
		case Mirakel.LIST_ALL:
		case Mirakel.LIST_DAILY:
		case Mirakel.LIST_WEEKLY:
			break;
		// Query Doesn't work
		/*
		 * case Mirakel.LIST_DAILY: where="due<=DATE('now') AND due>0 and ";
		 * case Mirakel.LIST_WEEKLY:
		 * where="due<=DATE('now','+7 days') AND due>0 and "; break;
		 */
		default:
			where = "list_id='" + listId + "' and ";
		}
		where += " not sync_state=" + Mirakel.SYNC_STATE_DELETE;
		Log.v(TAG, where);
		String order = "";
		switch (sorting) {
		case Mirakel.SORT_BY_PRIO:
			order = "priority desc";
			break;
		case Mirakel.SORT_BY_OPT:
			order = ", priority DESC";
		case Mirakel.SORT_BY_DUE:
			order = " CASE WHEN (due IS NULL) THEN date('now','+1000 years') ELSE date(due) END ASC"
					+ order;
			break;
		default:
			order = "_id ASC";
		}
		Log.v(TAG, order);
		return Mirakel.getReadableDatabase().query(Mirakel.TABLE_TASKS, allColumns, where,
				null, null, null, "done, " + order);
	}

	/**
	 * Shortcut for getting all Tasks of a List
	 * 
	 * @param list
	 * @param sorting
	 * @return
	 */
	public List<Task> getTasks(List_mirakle list, int sorting) {
		return getTasks(list.getId(), sorting);
	}

	/**
	 * Shortcut for getting all Tasks of a List
	 * 
	 * @param listId
	 * @return
	 */
	public List<Task> getTasks(int listId) {
		return getTasks(listId, Mirakel.SORT_BY_ID);
	}

	/**
	 * Get Tasks from a List
	 * 
	 * @param listId
	 * @param sorting
	 *            The Sorting (@see Mirakel.SORT_*)
	 * @return
	 */
	public List<Task> getTasks(int listId, int sorting) {
		return getTasks(listId, sorting, false);
	}

	/**
	 * Get Tasks from a List
	 * 
	 * @param listId
	 * @param sorting
	 *            The Sorting (@see Mirakel.SORT_*)
	 * @param showDone 
	 * @return
	 */
	public List<Task> getTasks(int listId, int sorting, boolean showDone) {
		List<Task> tasks = new ArrayList<Task>();
		Cursor cursor = getTasksCursor(listId, sorting);
		cursor.moveToFirst();
		GregorianCalendar toOld = new GregorianCalendar(1970, 1, 2);
		while (!cursor.isAfterLast()) {
			Task task = cursorToTask(cursor);
			switch (listId) {
			case Mirakel.LIST_DAILY:
				if (task.getDue().compareTo(new GregorianCalendar()) > 0
						|| task.isDone() || task.getDue().compareTo(toOld) <= 0)
					break;
			case Mirakel.LIST_WEEKLY:
				GregorianCalendar t = new GregorianCalendar();
				t.add(Calendar.DAY_OF_MONTH, 7);
				if (task.getDue().compareTo(t) > 0 || task.isDone()
						|| task.getDue().compareTo(toOld) <= 0)
					break;
			case Mirakel.LIST_ALL:
				if (task.isDone() && !showDone)
					break;
			default:
				tasks.add(task);
				break;
			}
			// tasks.add(task);
			cursor.moveToNext();
		}
		cursor.close();
		return tasks;
	}
	
	public List<Task> getTasksBySyncState(short state){
		List<Task> tasks_local = new ArrayList<Task>();
		Cursor c = database.query(Mirakel.TABLE_TASKS, allColumns, "sync_state="
				+ state+" and list_id>0", null, null, null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			tasks_local.add(cursorToTask(c));
			c.moveToNext();
		}
		return tasks_local;
	}

	/**
	 * Parse a JSON–String to a List of Tasks
	 * 
	 * @param result
	 * @return
	 */
	public List<Task> parse_json(String result) {
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
				t.setSync_state(Mirakel.SYNC_STATE_NOTHING);
				if (key_value[1] != "null") {
					t.setContent(key_value[1].substring(1,
							key_value[1].length() - 1));
				} else
					t.setContent(null);
			} else if (key.equals("\"done\"")) {
				t.setDone(key_value[1].indexOf("true") != -1);
			} else if (key.equals("\"due\"")) {
				GregorianCalendar temp = new GregorianCalendar();
				try {
					temp.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale
							.getDefault()).parse(key_value[1].substring(1,
							key_value[1].length() - 1)));
				} catch (Exception e) {
					temp.setTime(new Date(0));
					Log.e(TAG,"Can not parse Date! "+(key_value[1].substring(1,
							key_value[1].length() - 1)));
				}
				t.setDue(temp);
			} else if (key.equals("\"id\"")) {
				t.setId(Long.parseLong(key_value[1]));
			} else if (key.equals("\"list_id\"")) {
				t.setListId(Long.parseLong(key_value[1]));
			} else if (key.equals("\"name\"")) {
				t.setName(key_value[1].substring(1, key_value[1].length() - 1));
			} else if (key.equals("\"priority\"")) {
				t.setPriority(Integer.parseInt(key_value[1]));
			} else if (key.equals("\"created_at\"")) {
				t.setCreated_at(key_value.length == 4 ? key_value[1]
						.substring(1)
						+ key_value[2]
						+ key_value[3].substring(0, key_value[3].length() - 1)
						: "");
			} else if (key.equals("\"updated_at\"")) {
				t.setUpdated_at(key_value.length == 4 ? key_value[1]
						.substring(1)
						+ key_value[2]
						+ key_value[3].substring(0, key_value[3].length() - 2)
						: "");
				tasks.add(t);
			}
		}
		return tasks;
	}
}
