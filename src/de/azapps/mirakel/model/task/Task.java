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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.JsonHelper;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.sync.Network;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.R;

public class Task extends TaskBase {

	public static final String TABLE = "tasks";

	public Task(long id,String uuid, ListMirakel list, String name, String content,
			boolean done, GregorianCalendar due, GregorianCalendar reminder,
			int priority, String created_at, String updated_at, int sync_state) {
		super(id,uuid, list, name, content, done, due, reminder, priority,
				created_at, updated_at, sync_state);
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
		if(context!=null)
		setUpdatedAt(new SimpleDateFormat(
				context.getString(R.string.dateTimeFormat), Locale.getDefault())
				.format(new Date()));
		ContentValues values = getContentValues();
		newChange(this, Task.get(getId()));
//		this.edited= new HashMap<String, Boolean>();
		database.update(TABLE, values, "_id = " + getId(), null);
	}

	/**
	 * Delete a task
	 * 
	 * @param task
	 */
	public void delete() {
		long id = getId();
		newChange(null, this);
		if (getSync_state() == Network.SYNC_STATE.ADD)
			database.delete(TABLE, "_id = " + id, null);
		else {
			ContentValues values = new ContentValues();
			values.put("sync_state", Network.SYNC_STATE.DELETE);
			database.update(TABLE, values, "_id=" + id, null);
		}

	}
	public String toJson(){
		String json="{";
		json+="\"id\":"+getId()+",";
		json+="\"name\":\""+getName()+"\",";
		json+="\"content\":\""+getContent()+"\",";
		json+="\"done\":"+(isDone()?"true":"false")+",";
		json+="\"priority\":"+getPriority()+",";
		json+="\"list_id\":"+getList().getId()+",";
		String s="";
		if(getDue()!=null)
		{
			s=new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(getDue().getTime());
		}
		json+="\"due\":\""+s+"\",";
		s="";
		if(getReminder()!=null)
		{
			s=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(getReminder().getTime());
		}
		json+="\"reminder\":\""+s+"\",";
		json+="\"sync_state\":"+getSync_state()+",";
		json+="\"created_at\":\""+getCreated_at()+"\",";
		json+="\"updated_at\":\""+getUpdated_at()+"\"}";
		return json;
//		json+="\"name\":\""+getCreated_at()+"\"";
	}

	// Static Methods

	private static final String TAG = "TasksDataSource";
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	private static final String[] allColumns = { "_id", "uuid", "list_id", "name",
			"content", "done", "due", "reminder", "priority", "created_at",
			"updated_at", "sync_state" };
	private static Context context;
	
	public static void newChange(Task newTask, Task oldTask){
		//TODO enable if needed in Production
		if(!BuildConfig.DEBUG)
			return;
		if(oldTask==null&&newTask==null){
			Log.wtf(TAG,"Cannot change an nonexisting Task to a none existing Task");
			return;
		}
		ContentValues cv =new ContentValues();
		String nullCol=null;
		if(oldTask==null||newTask==null){
			//write complete task to db
			if(oldTask==null)
				nullCol="old";
			else
				cv.put("old", oldTask.toJson());
			if(newTask==null)
				nullCol="new";
			else
				cv.put("new", newTask.toJson());
		}else{
			//only changes to db
			String oldJson="{",newJson="{";
			boolean changed=false;
			if(oldTask.getName()==null&&newTask.getName()!=null||(oldTask.getName()!=null&&!oldTask.getName().equals(newTask.getName()))){
				oldJson=JsonHelper.addToJsonString("name",oldTask.getName(),oldJson);
				newJson=JsonHelper.addToJsonString("name",newTask.getName(),newJson);
				changed=true;
			}
			
			if(oldTask.getContent()==null&&newTask.getContent()!=null||(oldTask.getContent()!=null&&!oldTask.getContent().equals(newTask.getContent()))){
				oldJson=JsonHelper.addToJsonString("content",oldTask.getContent(),oldJson);
				newJson=JsonHelper.addToJsonString("content",newTask.getContent(),newJson);
				changed=true;
			}
			
			if(oldTask.getList().getId()!=newTask.getList().getId()){
				oldJson=JsonHelper.addToJsonString("list_id",oldTask.getList().getId(),oldJson);
				newJson=JsonHelper.addToJsonString("list_id",newTask.getList().getId(),newJson);
				changed=true;
			}
			
			if(oldTask.getPriority()!=newTask.getPriority()){
				oldJson=JsonHelper.addToJsonString("priority",oldTask.getPriority(),oldJson);
				newJson=JsonHelper.addToJsonString("priority",newTask.getPriority(),newJson);
				changed=true;
			}
			
			if(oldTask.isDone()!=newTask.isDone()){
				oldJson=JsonHelper.addToJsonString("done",oldTask.isDone(),oldJson);
				newJson=JsonHelper.addToJsonString("done",newTask.isDone(),newJson);
				changed=true;
			}
			
			if(oldTask.getDue()==null&&newTask.getDue()!=null||(oldTask.getDue()!=null&&!oldTask.getDue().equals(newTask.getDue()))){
				String s="";
				if(oldTask.getDue()!=null){
					s=new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(oldTask.getDue().getTime());
				}
				oldJson=JsonHelper.addToJsonString("due",s,oldJson);
				s="";
				if(newTask.getDue()!=null){
					s=new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(newTask.getDue().getTime());
				}
				newJson=JsonHelper.addToJsonString("due",s,newJson);
				changed=true;
			}
			
			if(oldTask.getReminder()==null&&newTask.getReminder()!=null||(oldTask.getReminder()!=null&&!oldTask.getReminder().equals(newTask.getReminder()))){
				String s="";
				if(oldTask.getReminder()!=null){
					s=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(oldTask.getDue().getTime());
				}					
				s="";
				if(newTask.getReminder()!=null){
					s=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(newTask.getDue().getTime());
				}	
				newJson=JsonHelper.addToJsonString("reminder",s,newJson);
				changed=true;
			}
			if(!changed)
				return;
			oldJson=JsonHelper.addToJsonString("id", oldTask.getId(), oldJson);
			newJson=JsonHelper.addToJsonString("id", newTask.getId(), newJson);
			oldJson=JsonHelper.addToJsonString("updated_at", oldTask.getUpdated_at(), oldJson);
			newJson=JsonHelper.addToJsonString("updated_at", newTask.getUpdated_at(), newJson);
			oldJson+="}";
			newJson+="}";
			
			cv.put("new", newJson);
			cv.put("old", oldJson);
		}
		cv.put("task_id", newTask==null?oldTask.getId():newTask.getId());
		database.insert(TaskHistory.TABLE, nullCol, cv);
	}


	

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
	
	public static Task newTask(String name, long list_id,GregorianCalendar due , int prio) {
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

		ContentValues values = new ContentValues();
		values.put("uuid",java.util.UUID.randomUUID().toString());
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
		newChange(newTask, null);
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

	public static Task parse_json(JsonObject el) {
		Task t=null;
		JsonElement id=el.get("id");
		if(id!=null)
			//use old Task from db if existing
			t=Task.get(id.getAsLong());
		if(t==null){
			t = new Task();
		}
//		if(el.get("id")!=null)
//			t.setId(el.get("id").getAsLong());
		JsonElement j=el.get("name");
		if(j!=null)
			t.setName(j.getAsString());
		try {
			j=el.get("content");
			t.setContent(j.getAsString() == null ? ""
					: el.get("content").getAsString());
		} catch (Exception e) {
			Log.d(TAG, "Content=NULL?");
			if(j!=null||id==null)
				t.setContent("");
		}
		j=el.get("priority");
		if(j!=null)
			t.setPriority(j.getAsInt());
		j=el.get("list_id");
		if(j!=null)
			t.setList(ListMirakel.getList(j.getAsInt()));
		j=el.get("created_at");
		if(j!=null){
			t.setCreatedAt(j.getAsString()
				.replace(":", ""));
		}
		j=el.get("updated_at");
		if(j!=null){
			t.setUpdatedAt(j.getAsString()
				.replace(":", ""));
		}
		j=el.get("done");
		if(j!=null)
			t.setDone(j.getAsBoolean());
		try {
			j=el.get("due");
			GregorianCalendar temp = new GregorianCalendar();
			temp.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale
					.getDefault()).parse(j.getAsString()));
			t.setDue(temp);
		} catch (Exception e) {
			if(j!=null||id==null)
				t.setDue(null);
			Log.v(TAG, "Due is null");
			Log.e(TAG, "Can not parse Date! ");
		}
		try{
			j=el.get("reminder");
			GregorianCalendar temp = new GregorianCalendar();
			temp.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale
					.getDefault()).parse(j.getAsString()));
			t.setReminder(temp);
		}catch(Exception e){
			if(j!=null||id==null)
				t.setDue(null);
			Log.v(TAG, "Reminder is null");
			Log.e(TAG, "Can not parse Date! ");
		}		
		return t;
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
			reminder.setTime(new SimpleDateFormat("yyyy-MM-dd'T'kkmmss'Z'",
					Locale.getDefault()).parse(cursor.getString(7)));
		} catch (ParseException e) {
			reminder = null;
		} catch (NullPointerException e) {
			reminder = null;
		}

		Task task = new Task(cursor.getLong(i++),cursor.getString(i++),
				ListMirakel.getList((int) cursor.getLong(i++)),
				cursor.getString(i++), cursor.getString(i++),
				cursor.getInt((i++)) == 1, due, reminder, cursor.getInt(8),
				cursor.getString(9), cursor.getString(10), cursor.getInt(11));
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
			where += (where.trim().equals("")?"":" AND ")+" done=0";
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

}
