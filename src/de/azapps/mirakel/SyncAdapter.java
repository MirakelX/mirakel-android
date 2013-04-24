/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Modified by weiznich 2013
 */
package de.azapps.mirakel;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.apache.http.message.BasicNameValuePair;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "SyncAdapter";

    private final AccountManager mAccountManager;
    private String Email;
    private String Password;
    private String ServerUrl;

    private ListsDataSource listDataSource;
    private TasksDataSource taskDataSource;
    	
    private final Context mContext;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        mAccountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {
    	taskDataSource=new TasksDataSource(mContext);
    	listDataSource=new ListsDataSource(mContext);
    	taskDataSource.open();
    	listDataSource.open();
    	//TODO close datasouces, by where???
    	Log.v(TAG,"Syncing");
    	
    	Email=account.name;
		Password=mAccountManager.getPassword(account);
		ServerUrl=mAccountManager.getUserData(account, Mirakel.BUNDLE_SERVER_URL);
		
		//Remove Lists server
		List<List_mirakle> deletedLists= listDataSource.getListsBySyncState(Mirakel.SYNC_STATE_DELETE);
		if (deletedLists != null) {
			for (int i = 0; i < deletedLists.size(); i++) {
				delete_list(deletedLists.get(i));
				deletedLists.get(i).setSync_state(Mirakel.SYNC_STATE_ADD);
				listDataSource.deleteList(deletedLists.get(i));
				Log.v(TAG,"Remove List "+deletedLists.get(i));
			}
		}
		
		//Remove Tasks from server
		List<Task> deletedTasks = taskDataSource.getTasksBySyncState(Mirakel.SYNC_STATE_DELETE);		
		if (deletedTasks != null) {
			for (int i = 0; i < deletedTasks.size(); i++) {
				delete_task(deletedTasks.get(i));
				deletedTasks.get(i).setSync_state(Mirakel.SYNC_STATE_ADD);
				taskDataSource.deleteTask(deletedTasks.get(i));
				Log.v(TAG,"Remove Task "+deletedTasks.get(i));
			}
		}
		
		
		//get Server-Tasklist
		new Network(new DataDownloadCommand() {
			@Override
			public void after_exec(String result) {
				List<Task> tasks_server = taskDataSource.parse_json(result);
				merge_with_server(tasks_server);
				//AddTasks
				List<Task> tasks_local = taskDataSource.getTasksBySyncState(Mirakel.SYNC_STATE_ADD);
				for (int i = 0; i < tasks_local.size(); i++) {
					add_task(tasks_local.get(i));
				}
				
				
				ContentValues values = new ContentValues();
				values.put("sync_state", Mirakel.SYNC_STATE_NOTHING);
				Mirakel.getWritableDatabase().update(Mirakel.TABLE_TASKS, values, "not sync_state="
						+ Mirakel.SYNC_STATE_NOTHING, null);
			}
		}, Email, Password, Mirakel.Http_Mode.GET).execute(ServerUrl
				+ "/lists/all/tasks.json");
		
		//get Server List-List
		new Network(new DataDownloadCommand() {
			@Override
			public void after_exec(String result) {
				List_mirakle lists_server[] = new Gson().fromJson(result,List_mirakle[].class);
				merge_with_server(lists_server);
				List<List_mirakle> lists_local = listDataSource.getListsBySyncState(Mirakel.SYNC_STATE_ADD);
				for (List_mirakle list:lists_local) {
					add_list(list);
				}
				ContentValues values = new ContentValues();
				values.put("sync_state", Mirakel.SYNC_STATE_NOTHING);
				Mirakel.getWritableDatabase().update(Mirakel.TABLE_LISTS, values, "not sync_state="
						+ Mirakel.SYNC_STATE_NOTHING, null);

			}
		}, Email, Password, Mirakel.Http_Mode.GET).execute(ServerUrl + "/lists.json");
    	
    	
    }
    
    
    //Own Functions
    //TODO NEED TO CLEANUP
    

	/**
	 * Delete a List from the Server
	 * 
	 * @param list
	 */
	protected void delete_list(final List_mirakle list) {
		new Network(new DataDownloadCommand() {
			@Override
			public void after_exec(String result) {
				// Do Nothing

			}
		}, Email, Password, Mirakel.Http_Mode.DELETE).execute(ServerUrl + "/lists/"
				+ list.getId() + ".json");

	}

	/**
	 * Sync one List with the Server
	 * 
	 * @param list
	 */
	public void sync_list(List_mirakle list) {
		List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("list[name]", list.getName()));
		new Network(new DataDownloadCommand() {
			@Override
			public void after_exec(String result) {
				// Do Nothing

			}
		}, Email, Password, Mirakel.Http_Mode.PUT, data).execute(ServerUrl
				+ "/lists/" + list.getId() + ".json");
	}

	/**
	 * Create a List on the Server
	 * 
	 * @param list
	 */
	public void add_list(final List_mirakle list) {
		List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("list[name]", list.getName()));
		new Network(new DataDownloadCommand() {

			@Override
			public void after_exec(String result) {
				List_mirakle list_response = new Gson().fromJson(result,
						List_mirakle.class);
				if(list.getId()<list_response.getId()){
					long diff=list_response.getId()-list.getId();
					//Should be all lists with _id> list_id
					Mirakel.getWritableDatabase().execSQL("UPDATE "+Mirakel.TABLE_LISTS+" SET _id=_id+"+diff+" WHERE sync_state="+Mirakel.SYNC_STATE_ADD+"and _id>"+list.getId());
				}
				ContentValues values = new ContentValues();
				values.put("_id", list_response.getId());
				Mirakel.getWritableDatabase().update(Mirakel.TABLE_LISTS, values, "_id=" + list.getId(), null);
				values = new ContentValues();
				values.put("list_id", list_response.getId());
				Mirakel.getWritableDatabase().update(Mirakel.TABLE_TASKS, values, "list_id=" + list.getId(),
						null);

			}
		}, Email, Password, Mirakel.Http_Mode.POST, data).execute(ServerUrl
				+ "/lists.json");
	}

	/**
	 * Merge Lists
	 * 
	 * @param lists_server
	 */
	protected void merge_with_server(List_mirakle[] lists_server) {
		for (int i = 0; i < lists_server.length; i++) {
			List_mirakle list = listDataSource.getList(lists_server[i].getId());
			if (list == null) {
				long id = Mirakel.getWritableDatabase().insert(Mirakel.TABLE_LISTS, null,
						lists_server[i].getContentValues());
				ContentValues values = new ContentValues();
				values.put("_id", lists_server[i].getId());
				Mirakel.getWritableDatabase().update(Mirakel.TABLE_LISTS, values, "_id=" + id, null);
			} else {
				if (list.getSync_state() == Mirakel.SYNC_STATE_NOTHING) {
					listDataSource.saveList(lists_server[i]);
				} else if(list.getSync_state()==Mirakel.SYNC_STATE_NEED_SYNC){
					DateFormat df = new SimpleDateFormat(mContext.getString(R.string.dateFormat),Locale.US);//use ASCII-Formating
					try {
						if(df.parse(list.getUpdated_at()).getTime()>df.parse(lists_server[i].getUpdated_at()).getTime()){
							//local list newer, 
							sync_list(list);
						}else{
							//server list newer
							lists_server[i].setSync_state(Mirakel.SYNC_STATE_IS_SYNCED);
							listDataSource.saveList(lists_server[i]);
						}
					} catch (ParseException e) {
						Log.e(TAG,"Unabel to parse Dates");
						e.printStackTrace();
					}
				}else{
					Log.wtf(TAG, "Syncronisation Error, Listmerge");
				}
			}
		}
		//Remove Tasks, which are deleted from server
		Mirakel.getWritableDatabase().execSQL("Delete from "+Mirakel.TABLE_LISTS+" where sync_state="+Mirakel.SYNC_STATE_NOTHING+" or sync_state="+Mirakel.SYNC_STATE_NEED_SYNC);
		Mirakel.getWritableDatabase().execSQL("Update "+Mirakel.TABLE_LISTS+" set sync_state="+Mirakel.SYNC_STATE_NOTHING+" where sync_state="+Mirakel.SYNC_STATE_IS_SYNCED);

	}
	
	//TASKS
	
	/**
	 * Delete a Task from the Server
	 * 
	 * @param task, Task to Delete
	 */
	protected void delete_task(final Task task) {
		new Network(new DataDownloadCommand() {
			@Override
			public void after_exec(String result) {
				// Do Nothing
			}
		}, Email, Password, Mirakel.Http_Mode.DELETE).execute(ServerUrl + "/lists/"
				+ task.getListId() + "/tasks/" + task.getId() + ".json");

	}

	/**
	 * Sync a Task with the server
	 * 
	 * @param task, Task to sync
	 */
	protected void sync_task(Task task) {
		List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("task[name]", task.getName()));
		data.add(new BasicNameValuePair("task[priority]", task.getPriority()
				+ ""));
		data.add(new BasicNameValuePair("task[done]", task.isDone() + ""));
		GregorianCalendar due = task.getDue();
		String dueString=due.compareTo(new GregorianCalendar(1970, 1, 10))<0?"null":new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(due.getTime());
		data.add(new BasicNameValuePair("task[due]", dueString));
		data.add(new BasicNameValuePair("task[content]", task.getContent()));
		task.setSync_state(Mirakel.SYNC_STATE_IS_SYNCED);
		taskDataSource.saveTask(task);
		new Network(new DataDownloadCommand() {
			@Override
			public void after_exec(String result) {
				// Do Nothing
			}
		}, Email, Password, Mirakel.Http_Mode.PUT, data).execute(ServerUrl
				+ "/lists/" + task.getListId() + "/tasks/" + task.getId()
				+ ".json");
	}

	/**
	 * Create a Task on the Server
	 * 
	 * @param task, Task to add
	 */
	protected void add_task(final Task task) {
		List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("task[name]", task.getName()));
		data.add(new BasicNameValuePair("task[priority]", task.getPriority()
				+ ""));
		data.add(new BasicNameValuePair("task[done]", task.isDone() + ""));
		GregorianCalendar due = task.getDue();
		data.add(new BasicNameValuePair("task[due]", due.before(new GregorianCalendar(1970,2,2))?"null":(new SimpleDateFormat(
				"yyyy-MM-dd", Locale.getDefault()).format(due.getTime()))));
		data.add(new BasicNameValuePair("task[content]", task.getContent()));
		//List must exists before syncing task!!
		//TODO remove dirty cast
		List_mirakle list=listDataSource.getList((int)task.getListId());
		
		if(list.getSync_state()==Mirakel.SYNC_STATE_ADD){
			sync_list(list);
		}
		list.setSync_state(Mirakel.SYNC_STATE_IS_SYNCED);
		listDataSource.saveList(list);
		
		new Network(new DataDownloadCommand() {
			@Override
			public void after_exec(String result) {
				try{
					Task taskNew = taskDataSource.parse_json("[" + result + "]").get(0);
					if(taskNew.getId()>task.getId()){
						//Prevent id-Collision
						long diff=taskNew.getId()-task.getId();
						Mirakel.getWritableDatabase().execSQL("UPDATE "+Mirakel.TABLE_TASKS+" SET _id=_id+"+diff+" WHERE sync_state="+Mirakel.SYNC_STATE_ADD+"and _id>"+task.getId());
					}
					ContentValues values = new ContentValues();
					values.put("_id", taskNew.getId());
					values.put("sync_state", Mirakel.SYNC_STATE_NOTHING);
					Mirakel.getWritableDatabase().update(Mirakel.TABLE_TASKS, values, "_id=" + taskNew.getId(), null);
				}catch(IndexOutOfBoundsException e){
					Log.e(TAG, "unknown Respons" );
				}
			}
		}, Email, Password, Mirakel.Http_Mode.POST, data).execute(ServerUrl
				+ "/lists/" + task.getListId() + "/tasks.json");

	}

	/**
	 * Merge a Task with the Server
	 * 
	 * @param tasks_server List<Task> with Tasks from server
	 */
	protected void merge_with_server(List<Task> tasks_server) {
		if (tasks_server == null)
			return;
		for (int i = 0; i < tasks_server.size(); i++) {
			Task task = taskDataSource.getTask(tasks_server.get(i).getId());
			if (task == null) {
				long id = Mirakel.getWritableDatabase().insert(Mirakel.TABLE_TASKS, null, tasks_server.get(i)
						.getContentValues());
				ContentValues values = new ContentValues();
				values.put("_id", tasks_server.get(i).getId());
				Mirakel.getWritableDatabase().update(Mirakel.TABLE_TASKS, values, "_id=" + id, null);
			} else {
				if (task.getSync_state() == Mirakel.SYNC_STATE_NOTHING) {
					tasks_server.get(i).setSync_state(Mirakel.SYNC_STATE_IS_SYNCED);
					taskDataSource.saveTask(tasks_server.get(i));
				} else if(task.getSync_state()==Mirakel.SYNC_STATE_NEED_SYNC){
					DateFormat df = new SimpleDateFormat(mContext.getString(R.string.dateFormat),Locale.US);//use ASCII-Formating
					try {
						if(df.parse(task.getUpdated_at()).getTime()>df.parse(tasks_server.get(i).getUpdated_at()).getTime()){
							//local task newer, 
							sync_task(task);
						}else{
							//server task newer
							tasks_server.get(i).setSync_state(Mirakel.SYNC_STATE_IS_SYNCED);
							taskDataSource.saveTask(tasks_server.get(i));
						}
					} catch (ParseException e) {
						Log.e(TAG,"Unabel to parse Dates");
						e.printStackTrace();
					}
				}else{
					Log.wtf(TAG, "Syncronisation Error, Taskmerge");
				}
			}
		}
		//Remove Tasks, which are deleted from server
		Mirakel.getWritableDatabase().execSQL("Delete from "+Mirakel.TABLE_TASKS+" where sync_state="+Mirakel.SYNC_STATE_NOTHING+" or sync_state="+Mirakel.SYNC_STATE_NEED_SYNC);
		Mirakel.getWritableDatabase().execSQL("Update "+Mirakel.TABLE_TASKS+" set sync_state="+Mirakel.SYNC_STATE_NOTHING+" where sync_state="+Mirakel.SYNC_STATE_IS_SYNCED);
		
	}
	    
}

