package de.azapps.mirakel.model.task;


import java.util.GregorianCalendar;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.azapps.mirakel.helper.JsonHelper;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.DatabaseHelper;
import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

public class TaskHistory {
	public final static String TABLE="TaskHistory";
	private final static String[] all={"_id","new","old","timestamp","task_id"};
	private static final String TAG = "TaskHistory";
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
	
	@SuppressLint("SimpleDateFormat")
	public String getChangesForSync(long TaskId,GregorianCalendar lastSync){
		String json="{";
		String[] coll={"new"};
		String newer="timestamp>="+lastSync.getTimeInMillis();
		String id=" task_id="+TaskId;
		
		Cursor c=database.query(TABLE, coll , "new like '%\"name\":%' and "+newer+" and "+id, null, null, null, "_id DESC", "1");
		json+=addCurser(c,"\"name\":");
		c.close();
		
		c=database.query(TABLE, coll , "new like '%\"content\":%' and "+newer+" and "+id, null, null, null, "_id DESC", "1");
		json+=addCurser(c,"\"content\":");
		c.close();
		
		c=database.query(TABLE, coll , "new like '%\"priority\":%' and "+newer+" and "+id, null, null, null, "_id DESC", "1");
		json+=addCurser(c,"\"priority\":");
		c.close();
		
		c=database.query(TABLE, coll , "new like '%\"due\":%' and "+newer+" and "+id, null, null, null, "_id DESC", "1");
		json+=addCurser(c,"\"due\":");
		c.close();
		
		c=database.query(TABLE, coll , "new like '%\"list_id\":%' and "+newer+" and "+id, null, null, null, "_id DESC", "1");
		json+=addCurser(c,"\"list_id\":");
		c.close();
		
		c=database.query(TABLE, coll , "new like '%\"done\":%' and "+newer+" and "+id, null, null, null, "_id DESC", "1");
		json+=addCurser(c,"\"done\":");
		c.close();
		if(json.equals("{")){
			Log.w(TAG,"no changes to Report");
			return null;
		}
		
		c=database.query(TABLE, coll , newer+" and "+id, null, null, null, "_id DESC", "1");
		json+=addCurser(c,"\"updated_at\":");
		c.close();
				
		json+="\"id\":"+TaskId+"}";
		
		return json;
	}

	private String addCurser(Cursor c, String key) {
		c.moveToFirst();
		String s=null;
		if(!c.isAfterLast()){
			s=JsonHelper.getPart(key, c.getString(0));
		}			
		return s==null?"":s+",";
	}

}
