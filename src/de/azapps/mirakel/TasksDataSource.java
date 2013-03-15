package de.azapps.mirakel;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TasksDataSource {
	private static final String TAG="TasksDataSource";
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	private String[] allColumns = { "_id", "list_id", "name", "content", "done", "due",
			"priority", "created_at", "updated_at" };

	public TasksDataSource(Context context) {
		dbHelper= new DatabaseHelper(context);
	}
	
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}
	public void close() {
		dbHelper.close();
	}
	public Task createTask(String name, long list_id) {
		return createTask(name,list_id,"",false,null,0);
	}
	public Task createTask(String name, long list_id, String content, boolean done, Date due, int priority) {
		ContentValues values= new ContentValues();
		values.put("name", name);
		values.put("list_id",list_id);
		values.put("content",content);
		values.put("done",done);
		values.put("due",(due==null?"":due.toString()));
		values.put("priority",priority);
		long insertId=database.insert("tasks", null, values);
		Cursor cursor = database.query("tasks", allColumns, "_id = " + insertId,null,null,null,null);
		cursor.moveToFirst();
		Task newTask=cursorToTask(cursor);
		cursor.close();
		return newTask;
	}
	
	public void saveTask(Task task) {
		Log.v(TAG,"saveTask");
		ContentValues values=task.getContentValues();
		database.update("tasks", values, "_id = " + task.getId(),null);
	}
	
	public void deleteTask(Task task) {
		long id = task.getId();
		database.delete("tasks", "_id = " + id, null);
	}
	
	public List<Task> getAllTasks() {
		List<Task> tasks = new ArrayList<Task>();
		Cursor cursor = database.query("tasks", allColumns, null, null, null, null, null);
		cursor.moveToFirst();
		while(!cursor.isAfterLast()) {
			Task task = cursorToTask(cursor);
			tasks.add(task);
			cursor.moveToNext();
		}
		cursor.close();
		return tasks;
	}
	public Task getTask(long id){
		Cursor cursor = database.query("tasks", allColumns, "_id='"+id+"'", null, null, null, null);
		cursor.moveToFirst();
		if(cursor.getCount()!=0){
			Task t=cursorToTask(cursor);
			cursor.close();
			return t;
		}
		return null;
	}

	private Task cursorToTask(Cursor cursor) {
		int i=0;
		Task task = new Task(
				cursor.getLong(i++),
				cursor.getLong(i++),
				cursor.getString(i++),
				cursor.getString(i++),
				cursor.getInt(i++) == 1,
				new Date(cursor.getLong(i++)),
				cursor.getInt(i++),
				cursor.getString(i++),
				cursor.getString(i++));
		return task;
	}
}
