package de.azapps.mirakel;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class TasksDataSource {
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	private String[] allColumns = { "_id", "list_id", "name", "content", "done",
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
		//values.put("due",due.toString());
		values.put("priority",priority);
		long insertId=database.insert("tasks", null, values);
		Cursor cursor = database.query("tasks", allColumns, "_id = " + insertId,null,null,null,null);
		cursor.moveToFirst();
		Task newTask=cursorToTask(cursor);
		cursor.close();
		return newTask;
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

	private Task cursorToTask(Cursor cursor) {
		Task task = new Task();
		int i=0;
		task.setId(cursor.getLong(i++));
		task.setListId(cursor.getLong(i++));
		task.setName(cursor.getString(i++));
		task.setContent(cursor.getString(i++));
		task.setDone(cursor.getInt(i++) == 1);
		//task.setDue(new Date(cursor.getLong(i++)));
		task.setPriority(cursor.getInt(i++));
		task.setCreated_at(cursor.getString(i++));
		task.setUpdated_at(cursor.getString(i++));
		return task;
	}
}
