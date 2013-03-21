package de.azapps.mirakel;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TasksDataSource {
	private static final String TAG = "TasksDataSource";
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	private String[] allColumns = { "_id", "list_id", "name", "content",
			"done", "due", "priority", "created_at", "updated_at" };

	public TasksDataSource(Context context) {
		dbHelper = new DatabaseHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public Task createTask(String name, long list_id) {
		return createTask(name, list_id, "", false, null, 0);
	}

	public Task createTask(String name, long list_id, String content,
			boolean done, GregorianCalendar due, int priority) {
		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("list_id", list_id);
		values.put("content", content);
		values.put("done", done);
		values.put("due", (due == null ? "0" : due.toString()));
		values.put("priority", priority);
		long insertId = database.insert("tasks", null, values);
		Cursor cursor = database.query("tasks", allColumns,
				"_id = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		Task newTask = cursorToTask(cursor);
		cursor.close();
		return newTask;
	}

	public void saveTask(Task task) {
		Log.v(TAG, "saveTask");	
		ContentValues values = task.getContentValues();
		database.update("tasks", values, "_id = " + task.getId(), null);
	}

	public void deleteTask(Task task) {
		long id = task.getId();
		database.delete("tasks", "_id = " + id, null);
	}

	public Task getTask(long id) {
		Cursor cursor = database.query("tasks", allColumns, "_id='" + id + "'",
				null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			Task t = cursorToTask(cursor);
			cursor.close();
			return t;
		}
		return null;
	}

	private Task cursorToTask(Cursor cursor) {
		int i = 0;
		GregorianCalendar t=new GregorianCalendar();
		try {
			t.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).parse(cursor.getString(5)));
		} catch (ParseException e) {
			t.setTime(new Date(0));
			Log.e(TAG,"Unable to parse Date");
		}
		
		Task task = new Task(cursor.getLong(i++), cursor.getLong(i++),
				cursor.getString(i++), cursor.getString(i++),
				cursor.getInt((i++)) == 1, t,
				cursor.getInt(++i), cursor.getString(++i),
				cursor.getString(++i));
		return task;
	}

	private Cursor updateListCursor(int listId, String sorting) {
		Cursor tasks;
		String where="";
		switch (listId) {
		case Mirakel.LIST_ALL:
			break;
		case Mirakel.LIST_DAILY:
			where="due<=DATE('now') AND due>0";
		case Mirakel.LIST_WEEKLY:
			where="due<=DATE('now','+7 days') AND due>0";
			break;
		default:
			where="list_id='" + listId + "'";
		}
		Log.v(TAG,where);
		return Mirakel.getReadableDatabase().query("tasks", allColumns,
				where, null, null, null, sorting);
	}

	public List<Task> getTasks(int listId, String sorting) {
		List<Task> tasks = new ArrayList<Task>();
		Cursor cursor = updateListCursor(listId, sorting);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Task task = cursorToTask(cursor);
			/*switch (listId) {
			case Mirakel.LIST_DAILY:
				if(task.getDue().compareTo(new GregorianCalendar())>0||task.isDone())
					break;
			case Mirakel.LIST_WEEKLY:
				GregorianCalendar t=new GregorianCalendar();
				t.add(Calendar.DAY_OF_MONTH, 7);
				if(task.getDue().compareTo(t)>0||task.isDone())
					break;
			default:
				tasks.add(task);
				break;
			}	*/
			tasks.add(task);
			cursor.moveToNext();
		}
		cursor.close();
		return tasks;
	}
}
