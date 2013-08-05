package de.azapps.mirakel.model.task;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.azapps.mirakel.model.DatabaseHelper;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

public class TaskHistory {
	public final static String TABLE="TaskHistory";
	private final static String[] all={"_id","new","old","timestamp","task_id"};
	private SQLiteDatabase database;
	@SuppressWarnings("unused")
	private Context ctx;
	
	public TaskHistory(Context ctx){
		this.ctx=ctx;
		database=new DatabaseHelper(ctx).getReadableDatabase();
	}
	
	public Pair<Task, Task> getLast(){
		Cursor c=database.query(TABLE, all, null, null, null, null, "_id DESC", "1");
		Pair<Task, Task> p = historyToPair(c);
		c.close();
		return p;
	}

	private Pair<Task, Task> historyToPair(Cursor c) {
		c.moveToFirst();
		Pair<Task, Task> p=null;
		if(!c.isAfterLast()){
			JsonParser parser = new JsonParser();
			
			Task old= c.getString(2)==null?null:Task.parse_json((JsonObject)parser.parse(c.getString(2)));
			Task newTask= c.getString(1)==null?null:Task.parse_json((JsonObject)parser.parse(c.getString(1)));
			p=new Pair<Task, Task>(old,newTask);
		}
		return p;
	}	
	
	public Pair<Task, Task> getLastById(long TaskId){
		Cursor c=database.query(TABLE, all, "task_id="+TaskId, null, null, null, "_id DESC", "1");
		Pair<Task, Task> p = historyToPair(c);
		c.close();
		return p;
	}	

}
