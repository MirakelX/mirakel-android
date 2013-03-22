package de.azapps.mirakel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.apache.http.message.BasicNameValuePair;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.Gson;

public class TasksDataSource {
	private static final String TAG = "TasksDataSource";
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	private String[] allColumns = { "_id", "list_id", "name", "content",
			"done", "due", "priority", "created_at", "updated_at","sync_state" };

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
		values.put("sync_state", Mirakel.SYNC_STATE_ADD);
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
		task.setSync_state(task.getSync_state()==Mirakel.SYNC_STATE_ADD?Mirakel.SYNC_STATE_ADD:Mirakel.SYNC_STATE_NEED_SYNC);
		ContentValues values = task.getContentValues();
		database.update("tasks", values, "_id = " + task.getId(), null);
	}

	public void deleteTask(Task task) {
		long id = task.getId();
		if(task.getSync_state()==Mirakel.SYNC_STATE_ADD)
			database.delete("tasks", "_id = " + id, null);
		else{
			ContentValues values=new ContentValues();
			values.put("sync_state", Mirakel.SYNC_STATE_DELETE);
			database.update("tasks", values, "_id="+id, null);
		}
			
	}

	public Task getTask(long id) {
		open();
		Cursor cursor = database.query("tasks", allColumns, "_id='" + id + "' and not sync_state="+Mirakel.SYNC_STATE_DELETE,
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
				cursor.getString(++i),cursor.getInt(++i));
		return task;
	}

	private Cursor updateListCursor(int listId, String sorting) {
		String where="";
		switch (listId) {
		case Mirakel.LIST_ALL:
			break;
		case Mirakel.LIST_DAILY:
			where="due<=DATE('now') AND due>0 and ";
		case Mirakel.LIST_WEEKLY:
			where="due<=DATE('now','+7 days') AND due>0 and ";
			break;
		default:
			where="list_id='" + listId + "'";
		}
		where+="not sync_state="+Mirakel.SYNC_STATE_DELETE;
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
			Log.e(TAG,task.getId()+" "+task.getName());
			cursor.moveToNext();
		}
		cursor.close();
		return tasks;
	}
	public void sync_tasks(final String email, final String password, final String url) {	
		Log.v(TAG,"sync tasks");
		new Network(new DataDownloadCommand() {			
			@Override
			public void after_exec(String result) {
				List<Task> tasks_server=parse_json(result);
				List<Task>tasks_local=getTasks(Mirakel.LIST_ALL, Mirakel.ORDER_BY_ID);
				for(int i=0; i<tasks_local.size();i++){
					Task task=tasks_local.get(i);
					switch(task.getSync_state()){
						case Mirakel.SYNC_STATE_ADD:
							add_task(task,email,password,url);
							break;
						case Mirakel.SYNC_STATE_DELETE:
							delete_task(task,email,password,url);
							task.setSync_state(Mirakel.SYNC_STATE_ADD);
							deleteTask(task);
							task=null;
							continue;
						case Mirakel.SYNC_STATE_NEED_SYNC:
							sync_task(task,email,password,url);
							break;
						default:
					}
				}
				merge_with_server(tasks_local, tasks_server);
				ContentValues values=new ContentValues();
				values.put("sync_state", Mirakel.SYNC_STATE_NOTHING);
				database.update("tasks", values, "not sync_state="+Mirakel.SYNC_STATE_NOTHING, null);
			}
		},email,password,Mirakel.Http_Mode.GET).execute(url+"/lists/all/tasks.json");	
	}

	protected List<Task> parse_json(String result) {
		List<Task> tasks=new ArrayList<Task>();
		result=result.substring(1,result.length()-2);
		String tasks_str[]=result.split(",");
		Task t=null;
		for(int i=0;i<tasks_str.length;i++){
			String key_value[]=tasks_str[i].split(":");
			if(key_value.length<2)
				continue;
			String key=key_value[0];
			if(key.equals("{\"content\"")){
				t=new Task();
				t.setContent(key_value[1].substring(1,key_value[1].length()-1));
			}else if(key.equals("\"done\"")){
				t.setDone(key_value[1].indexOf("true")!=-1);
			}else if(key.equals("\"due\"")){
				GregorianCalendar temp=new GregorianCalendar();
				try{
					temp.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN).parse(key_value[1].substring(1,key_value[1].length()-1)));
				}catch(Exception e){
					temp.setTime(new Date(0));
				}
				t.setDue(temp);
			}else if(key.equals("\"id\"")){
				t.setId(Long.parseLong(key_value[1]));
			}else if(key.equals("\"list_id\"")){
				t.setListId(Long.parseLong(key_value[1]));
			}else if(key.equals("\"name\"")){
				t.setName(key_value[1].substring(1,key_value[1].length()-1));
			}else if(key.equals("\"priority\"")){
				t.setPriority(Integer.parseInt(key_value[1]));
			}else if(key.equals("\"created_at\"")){
				t.setCreated_at(key_value.length==4?key_value[1].substring(1)+key_value[2]+key_value[3].substring(0, key_value[3].length()-1):"");
			}else if(key.equals("\"updated_at\"")){
				t.setUpdated_at(key_value.length==4?key_value[1].substring(1)+key_value[2]+key_value[3].substring(0, key_value[3].length()-2):"");
				tasks.add(t);
			}
		}
		return tasks;
	}

	protected void delete_task(final Task task,final String email,final String password,final String url) {
		new Network(new DataDownloadCommand() {
			@Override
			public void after_exec(String result) {
				// Do Nothing
			}
		},email,password,Mirakel.Http_Mode.DELETE).execute(url+"/lists/"+task.getListId()+"/tasks/"+task.getId()+".json");
		
	}

	protected void sync_task(Task task, String email, String password,
			String url) {
		List<BasicNameValuePair> data=new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("task[name]", task.getName()));
		data.add(new BasicNameValuePair("task[priority]", task.getPriority()+""));
		data.add(new BasicNameValuePair("task[done]", task.isDone()+""));
		GregorianCalendar due=task.getDue();
		data.add(new BasicNameValuePair("task[due]", due.get(Calendar.YEAR)+"-"+due.get(Calendar.MONTH)+"-"+due.get(Calendar.DAY_OF_MONTH)));
		data.add(new BasicNameValuePair("task[content]", task.getContent()));
		new Network(new DataDownloadCommand() {
			@Override
			public void after_exec(String result) {
				//Do Nothing
			}
		}, email, password, Mirakel.Http_Mode.PUT,data).execute(url+"/lists/"+task.getListId()+"/tasks/"+task.getId()+".json");	
	}

	protected void add_task(final Task task,final String email,final String password, final String url) {
		List<BasicNameValuePair> data=new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("task[name]", task.getName()));
		data.add(new BasicNameValuePair("task[priority]", task.getPriority()+""));
		data.add(new BasicNameValuePair("task[done]", task.isDone()+""));
		GregorianCalendar due=task.getDue();
		data.add(new BasicNameValuePair("task[due]", due.get(Calendar.YEAR)+"-"+due.get(Calendar.MONTH)+"-"+due.get(Calendar.DAY_OF_MONTH)));
		data.add(new BasicNameValuePair("task[content]", task.getContent()));
		new Network(new DataDownloadCommand() {	
			@Override
			public void after_exec(String result) {
				List_mirakle list_response=new Gson().fromJson(result, List_mirakle.class);
				ContentValues values=new ContentValues();
				values.put("_id", list_response.getId());
				open();
				database.update("tasks", values, "_id="+task.getId(), null);
			}
		},email,password,Mirakel.Http_Mode.POST,data).execute(url+"/lists/"+task.getListId()+"/tasks.json");
		
	}

	protected void merge_with_server(List<Task> tasks,
			List<Task> tasks_server) {
		for(int i=0;i<tasks_server.size();i++){
			Task task =getTask(tasks_server.get(i).getId());
			if(task==null){
				long id=database.insert("tasks", null, tasks_server.get(i).getContentValues());
				ContentValues values=new ContentValues();
				values.put("_id", tasks_server.get(i).getId());
				open();
				database.update("lists", values, "_id="+id, null);
			}else{
				if(task.getSync_state()==Mirakel.SYNC_STATE_NOTHING){
					saveTask(tasks_server.get(i));
				}else{
					Log.e(TAG,"Merging lists not implementet");
				}
			}
		}

	}
}
