package de.azapps.mirakel.sync.mirakel;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.http.message.BasicNameValuePair;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Pair;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.DataDownloadCommand;
import de.azapps.mirakel.sync.Network;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_STATE;
import de.azapps.mirakelandroid.R;

public class MirakelSync {
	private static final String TAG = "MirakelSync";
	public static String TYPE = "Mirakel";
	private final List<Pair<Network, String>> AddLists;

	private final List<Pair<Network, String>> AddTasks;
	private int count;
	private final List<Pair<Network, String>> DeleteLists;
	private final List<Pair<Network, String>> DeleteTasks;
	private boolean finishList;
	private int listAdd;

	private final Context mContext;
	private String ServerUrl;
	private final List<Pair<Network, String>> SyncLists;

	private final List<Pair<Network, String>> SyncTasks;
	private String Token;

	public MirakelSync(Context ctx) {
		this.mContext = ctx;

		this.DeleteLists = new ArrayList<Pair<Network, String>>();
		this.DeleteTasks = new ArrayList<Pair<Network, String>>();
		this.AddLists = new ArrayList<Pair<Network, String>>();
		this.AddTasks = new ArrayList<Pair<Network, String>>();
		this.SyncLists = new ArrayList<Pair<Network, String>>();
		this.SyncTasks = new ArrayList<Pair<Network, String>>();
		this.listAdd = 0;
		this.count = 0;
		this.finishList = false;
	}

	/**
	 * Create a List on the Server
	 * 
	 * @param list
	 */
	private void add_list(final ListMirakel list) {
		List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("list[name]", list.getName()));
		this.AddLists.add(new Pair<Network, String>(new Network(
				new DataDownloadCommand() {
					@Override
					public void after_exec(String result) {
						ListMirakel list_response = new Gson().fromJson(result,
								ListMirakel.class);
						if (list_response == null) {
							Log.wtf(TAG, "Unable to add list to server");
							finishSync();
							return;
						}
						if (list.getId() < list_response.getId()) {
							long diff = list_response.getId() - list.getId();
							// Should be all lists with _id> list_id
							Cursor c = Mirakel.getReadableDatabase().rawQuery(
									"Select _id from " + ListMirakel.TABLE
									+ " WHERE sync_state="
									+ SYNC_STATE.ADD + " and _id>="
									+ list.getId(), null);
							c.moveToFirst();
							c.close();
							c = Mirakel.getReadableDatabase().rawQuery(
									"Select _id from " + ListMirakel.TABLE
									+ " WHERE sync_state="
									+ SYNC_STATE.ADD + " and _id>"
									+ list.getId(), null);
							c.moveToLast();
							while (!c.isBeforeFirst()) {
								Mirakel.getWritableDatabase().execSQL(
										"UPDATE " + ListMirakel.TABLE
										+ " SET _id=_id+" + diff
										+ " WHERE sync_state="
										+ SYNC_STATE.ADD + " and _id="
										+ c.getInt(0));
								c.moveToPrevious();
							}
							c.close();

						}
						ContentValues values = new ContentValues();
						values.put("_id", list_response.getId());
						values.put("sync_state", SYNC_STATE.NOTHING.toInt());
						Mirakel.getWritableDatabase().update(ListMirakel.TABLE,
								values, "_id=" + list.getId(), null);
						values = new ContentValues();
						values.put("list_id", list_response.getId());
						Mirakel.getWritableDatabase().update(Task.TABLE,
								values, "list_id=" + list.getId(), null);
						addTasks();
						finishSync();
					}
				}, Network.HttpMode.POST, data, this.mContext, this.Token), this.ServerUrl
				+ "/lists.json"));
	}

	/**
	 * Create a Task on the Server
	 * 
	 * @param task
	 *            , Task to add
	 */
	private void add_task(final Task task) {
		List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("task[name]", task.getName()));
		data.add(new BasicNameValuePair("task[priority]", task.getPriority()
				+ ""));
		data.add(new BasicNameValuePair("task[done]", task.isDone() + ""));
		Calendar due = task.getDue();
		data.add(new BasicNameValuePair("task[due]", due == null ? "null"
				: new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
		.format(due.getTime())));
		data.add(new BasicNameValuePair("task[content]", task.getContent()));

		this.AddTasks.add(new Pair<Network, String>(new Network(
				new DataDownloadCommand() {
					@Override
					public void after_exec(String result) {
						try {

							Task taskNew = Task.parse_json("[" + result + "]",
									AccountMirakel.getLocal())
									.get(0);
							if (taskNew.getId() > task.getId()) {
								// Prevent id-Collision
								long diff = taskNew.getId() - task.getId();

								Cursor c = Mirakel.getWritableDatabase()
										.rawQuery(
												"Select _id from " + Task.TABLE
												+ " WHERE sync_state="
												+ SYNC_STATE.ADD
												+ " and _id>"
												+ task.getId(), null);
								c.moveToLast();
								while (!c.isBeforeFirst()) {
									Mirakel.getWritableDatabase()
									.execSQL(
											"UPDATE "
													+ Task.TABLE
													+ " SET _id=_id+"
													+ diff
													+ " WHERE sync_state="
													+ SYNC_STATE.ADD
													+ " and _id="
													+ c.getInt(0));
									c.moveToPrevious();
								}
								c.close();
							}
							ContentValues values = new ContentValues();
							values.put("_id", taskNew.getId());
							values.put("sync_state", SYNC_STATE.NOTHING.toInt());
							Mirakel.getWritableDatabase().update(Task.TABLE,
									values, "_id=" + task.getId(), null);
						} catch (IndexOutOfBoundsException e) {
							Log.wtf(TAG, "Unable to add Task to server");
						}
						finishSync();
					}
				}, Network.HttpMode.POST, data, this.mContext, this.Token), this.ServerUrl
				+ "/lists/" + task.getList().getId() + "/tasks.json"));

	}

	private void addListFromServer(ListMirakel list_server) {
		list_server.setSyncState(SYNC_STATE.IS_SYNCED);
		long id = Mirakel.getWritableDatabase().insert(ListMirakel.TABLE, null,
				list_server.getContentValues());
		// TODO Get this from server!!
		Cursor c = Mirakel.getReadableDatabase().rawQuery(
				"Select max(lft),max(rgt) from " + ListMirakel.TABLE
				+ " where not sync_state=" + SYNC_STATE.DELETE, null);
		c.moveToFirst();
		int lft = 0, rgt = 0;
		if (c.getCount() != 0) {
			lft = c.getInt(0) + 2;
			rgt = c.getInt(1) + 2;
		}
		c.close();
		ContentValues values = new ContentValues();
		values.put("_id", list_server.getId());
		values.put("sync_state", SYNC_STATE.IS_SYNCED.toInt());
		values.put("lft", lft);
		values.put("rgt", rgt);
		Mirakel.getWritableDatabase().update(ListMirakel.TABLE, values,
				"_id=" + id, null);
	}

	private void addTaskFromServer(Task task_server) {

		// task_server.setUpdatedAt();
		task_server.setSyncState(SYNC_STATE.IS_SYNCED);
		long id;
		try {
			id = Mirakel.getWritableDatabase().insert(Task.TABLE, null,
					task_server.getContentValues());
		} catch (NoSuchListException e) {
			Log.wtf(TAG, "List vanished while Sync!?!?");
			return;
		}
		if (id > task_server.getId()) {
			long diff = id - task_server.getId();
			Mirakel.getWritableDatabase().execSQL(
					"UPDATE " + Task.TABLE + " SET _id=_id+" + diff
					+ " WHERE sync_state=" + SYNC_STATE.ADD
					+ "and _id>" + task_server.getId());
		}
		ContentValues values = new ContentValues();
		values.put("_id", task_server.getId());
		Mirakel.getWritableDatabase().update(Task.TABLE, values, "_id=" + id,
				null);
	}

	private void addTasks() {
		this.listAdd++;
		if (this.listAdd >= this.AddLists.size()) {
			List<Task> tasks_local = Task.getBySyncState(SYNC_STATE.ADD);
			for (int i = 0; i < tasks_local.size(); i++) {
				add_task(tasks_local.get(i));
			}
			this.finishList = true;
			if (this.AddTasks.size() == 0) {
				finishSync();
			}
			execute(this.AddTasks);
		}
	}

	/**
	 * Delete a List from the Server
	 * 
	 * @param list
	 */
	private void delete_list(final ListMirakel list) {
		this.DeleteLists.add(new Pair<Network, String>(new Network(
				new DataDownloadCommand() {
					@Override
					public void after_exec(String result) {
						list.setSyncState(SYNC_STATE.ADD);
						list.destroy();
						finishSync();
					}

				}, Network.HttpMode.DELETE, this.mContext, this.Token), this.ServerUrl
				+ "/lists/" + list.getId() + ".json"));

	}

	/**
	 * Delete a Task from the Server
	 * 
	 * @param task
	 *            , Task to Delete
	 */
	private void delete_task(final Task task) {
		this.DeleteTasks.add(new Pair<Network, String>(new Network(
				new DataDownloadCommand() {
					@Override
					public void after_exec(String result) {
						// Remove Task
						task.setSyncState(SYNC_STATE.ADD);
						task.destroy();
					}
				}, Network.HttpMode.DELETE, this.mContext, this.Token), this.ServerUrl
				+ "/lists/" + task.getList().getId() + "/tasks/" + task.getId()
				+ ".json"));

	}

	private void doSync() {
		List<ListMirakel> lists_local = ListMirakel.bySyncState(SYNC_STATE.ADD);
		for (ListMirakel list : lists_local) {
			add_list(list);
		}
		Log.d(TAG, "Execute Sync");
		execute(this.DeleteTasks);
		execute(this.DeleteLists);
		execute(this.SyncLists);
		execute(this.SyncTasks);
		execute(this.AddLists);
		if (this.AddLists.size() == 0) {
			addTasks();
		}
	}

	private void execute(List<Pair<Network, String>> CommandList) {
		for (Pair<Network, String> command : CommandList) {
			command.first.execute(command.second);
		}
	}

	private void finishSync() {
		++this.count;
		if (this.count >= this.DeleteLists.size() + this.DeleteTasks.size() + this.AddLists.size()
				+ this.AddTasks.size() + this.SyncLists.size() + this.SyncTasks.size()
				&& this.finishList) {
			new Network(new DataDownloadCommand() {
				@Override
				public void after_exec(String result) {
				}
			}, Network.HttpMode.DELETE, this.mContext, null).execute(this.ServerUrl
					+ "/tokens/" + this.Token);
		}

	}

	private void listMerge(ListMirakel list_server, ListMirakel list) {
		if (list.getSyncState() == SYNC_STATE.NOTHING
				|| list.getSyncState() == SYNC_STATE.NEED_SYNC) {
			DateFormat df = new SimpleDateFormat(
					this.mContext.getString(R.string.dateTimeFormat), Locale.US);// use
			// ASCII-Formating
			try {
				if (df.parse(list.getUpdatedAt()).getTime() > df.parse(
						list_server.getUpdatedAt()).getTime()) {
					Log.d(TAG, "Sync List to server");
					// local list newer,
					sync_list(list);
				} else {
					Log.d(TAG, "Sync List from server");
					// server list newer
					list_server.setLft(list.getLft());
					list_server.setRgt(list.getRgt());
					list_server.setSyncState(SYNC_STATE.IS_SYNCED);
					list_server.save();
				}
			} catch (ParseException e) {
				Log.e(TAG, "Unabel to parse Dates");
				Log.w(TAG, Log.getStackTraceString(e));
			}
		} else if (list.getSyncState() == SYNC_STATE.ADD) {
			Cursor c = Mirakel.getReadableDatabase().rawQuery(
					"Select max(_id) from " + ListMirakel.TABLE
					+ " where not sync_state=" + SYNC_STATE.ADD, null);
			c.moveToFirst();
			if (c.getCount() != 0) {
				int diff = c.getInt(0) - list.getId() < 0 ? 1 : c.getInt(0)
						- list.getId();
				c.close();
				c = Mirakel.getReadableDatabase().rawQuery(
						"Select _id from " + ListMirakel.TABLE
						+ " WHERE sync_state=" + SYNC_STATE.ADD
						+ " and _id>=" + list.getId(), null);
				c.moveToLast();
				while (!c.isBeforeFirst()) {
					Mirakel.getWritableDatabase().execSQL(
							"UPDATE " + ListMirakel.TABLE + " SET _id=_id+"
									+ diff + " WHERE sync_state="
									+ SYNC_STATE.ADD + " and _id="
									+ c.getInt(0));
					c.moveToPrevious();
				}
				c.close();
				Log.d(TAG, "Move list+Add list from server");
				addListFromServer(list_server);
			}
		} else if (list.getSyncState() == SYNC_STATE.IS_SYNCED) {
			list.setSyncState(SYNC_STATE.NOTHING);
			list.save();
			listMerge(list_server, list);
		} else {
			Log.wtf(TAG, "Syncronisation Error, Listmerge");
		}
	}

	/**
	 * Merge a Task with the Server
	 * 
	 * @param tasks_server
	 *            List<Task> with Tasks from server
	 */
	private void merge_with_server(List<Task> tasks_server) {
		if (tasks_server == null)
			return;
		for (Task task_server : tasks_server) {
			Task task = Task.getToSync(task_server.getId());
			if (task == null) {
				// New Task from server, add to db
				/*
				 * if (Mirakel.DEBUG) Log.d(TAG, "Add task from server to list "
				 * + task_server.getList().getId()); Throws NullPointerException
				 */
				addTaskFromServer(task_server);
				continue;
			} else {
				mergeTask(task_server, task);
			}
		}
		// Remove Tasks, which are deleted from server
		Mirakel.getWritableDatabase().execSQL(
				"Delete from " + Task.TABLE + " where sync_state="
						+ SYNC_STATE.NOTHING + " or sync_state="
						+ SYNC_STATE.NEED_SYNC);
		Mirakel.getWritableDatabase().execSQL(
				"Update " + Task.TABLE + " set sync_state="
						+ SYNC_STATE.NOTHING + " where sync_state="
						+ SYNC_STATE.IS_SYNCED);

	}

	// TASKS

	/**
	 * Merge Lists
	 * 
	 * @param lists_server
	 */
	private void merge_with_server(ListMirakel[] lists_server) {
		if (lists_server == null)
			return;
		for (ListMirakel list_server : lists_server) {
			list_server.setCreatedAt(list_server.getCreatedAt()
					.replace(":", ""));
			list_server.setUpdatedAt(list_server.getUpdatedAt()
					.replace(":", ""));
			ListMirakel list = ListMirakel.getList(list_server.getId());
			if (list == null) {
				Log.d(TAG, "add list from Server");
				addListFromServer(list_server);
				continue;
			} else {
				listMerge(list_server, list);
			}
		}
		// Remove Tasks, which are deleted from server
		Mirakel.getWritableDatabase().execSQL(
				"Delete from " + ListMirakel.TABLE + " where sync_state="
						+ SYNC_STATE.NOTHING + " or sync_state="
						+ SYNC_STATE.NEED_SYNC);
		// Set Sync state to Nothing
		Mirakel.getWritableDatabase().execSQL(
				"Update " + ListMirakel.TABLE + " set sync_state="
						+ SYNC_STATE.NOTHING + " where sync_state="
						+ SYNC_STATE.IS_SYNCED);
	}

	private void mergeTask(Task task_server, Task task) {
		if (task.getSyncState() == SYNC_STATE.NEED_SYNC
				|| task.getSyncState() == SYNC_STATE.NOTHING) {
			if (task.getUpdatedAt().compareTo(task_server.getUpdatedAt()) > 0) {
				// local task newer, push to server
				Log.d(TAG, "Sync task to server from list "
						+ task.getList().getId());
				sync_task(task);
			} else {
				// server task newer, use this task instated local
				Log.d(TAG, "Sync task from server to list "
						+ task_server.getList().getId());
				task_server.setSyncState(SYNC_STATE.IS_SYNCED);
				safeSafeTask(task_server);
			}
		} else if (task.getSyncState() == SYNC_STATE.ADD) {
			Cursor c = Mirakel.getReadableDatabase().rawQuery(
					"Select max(_id) from " + Task.TABLE
					+ " where not sync_state=" + SYNC_STATE.ADD, null);
			c.moveToFirst();
			if (c.getCount() != 0) {
				long diff = c.getInt(0) - task.getId() < 0 ? 1 : c.getInt(0)
						- task.getId();
				c.close();
				c = Mirakel.getReadableDatabase().rawQuery(
						"Select _id from " + Task.TABLE + " WHERE sync_state="
								+ SYNC_STATE.ADD + " and _id>=" + task.getId(),
								null);
				c.moveToLast();
				while (!c.isBeforeFirst()) {
					Mirakel.getWritableDatabase().execSQL(
							"UPDATE " + Task.TABLE + " SET _id=_id+" + diff
							+ " WHERE sync_state=" + SYNC_STATE.ADD
							+ " and _id=" + c.getInt(0));
					c.moveToPrevious();
				}
				c.close();
				Log.d(TAG, "Move task + add task from server to list "
						+ task_server.getList().getId());
				addTaskFromServer(task_server);
			}
		} else if (task.getSyncState() == SYNC_STATE.DELETE) {
			// Nothing
		} else if (task.getSyncState() == SYNC_STATE.IS_SYNCED) {
			task.setSyncState(SYNC_STATE.NOTHING);
			safeSafeTask(task);
			mergeTask(task_server, task);
		} else {
			Log.wtf(TAG, "Syncronisation Error, Taskmerge");
		}
	}

	private void perpareSync() {
		List<ListMirakel> deletedLists = ListMirakel
				.bySyncState(SYNC_STATE.DELETE);
		if (deletedLists != null) {
			for (ListMirakel deletedList : deletedLists) {
				delete_list(deletedList);
			}
		}

		// Remove Tasks from server
		List<Task> deletedTasks = Task.getBySyncState(SYNC_STATE.DELETE);
		if (deletedTasks != null) {
			for (Task deletedTask : deletedTasks) {
				delete_task(deletedTask);
			}
		}

		new Network(new DataDownloadCommand() {
			@Override
			public void after_exec(String result) {

				try {
					merge_with_server(new Gson().fromJson(result,
							ListMirakel[].class));
				} catch (JsonSyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				new Network(new DataDownloadCommand() {
					@Override
					public void after_exec(String result) {
						merge_with_server(Task.parse_json(result,
								AccountMirakel.getLocal()));
						doSync();
					}
				}, Network.HttpMode.GET, MirakelSync.this.mContext, MirakelSync.this.Token).execute(MirakelSync.this.ServerUrl
						+ "/lists/all/tasks.json");
			}
		}, Network.HttpMode.GET, this.mContext, this.Token).execute(this.ServerUrl
				+ "/lists.json");
	}

	private void safeSafeTask(Task task) {
		try {
			task.save();
		} catch (NoSuchListException e) {
			Log.wtf(TAG, "List vanished while Sync!?!?");
		}
	}

	public void sync(Account account) {
		Log.v(TAG, "Syncing");

		this.ServerUrl = AccountManager.get(this.mContext).getUserData(account,
				SyncAdapter.BUNDLE_SERVER_URL);

		List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("email", account.name));
		data.add(new BasicNameValuePair("password", AccountManager
				.get(this.mContext).getPassword(account)));

		new Network(new DataDownloadCommand() {

			@Override
			public void after_exec(String result) {
				String t = Network.getToken(result);
				if (t != null) {
					MirakelSync.this.Token = t;
					perpareSync();
				} else {
					Toast.makeText(
							MirakelSync.this.mContext,
							MirakelSync.this.mContext.getString(R.string.login_activity_loginfail_text_both),
							Toast.LENGTH_LONG).show();
				}
			}
		}, Network.HttpMode.POST, data, this.mContext, null).execute(this.ServerUrl
				+ "/tokens.json");
	}

	/**
	 * Sync one List with the Server
	 * 
	 * @param list
	 */
	private void sync_list(ListMirakel list) {
		List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("list[name]", list.getName()));
		list.setSyncState(SYNC_STATE.IS_SYNCED);
		list.save();
		this.SyncLists.add(new Pair<Network, String>(new Network(
				new DataDownloadCommand() {
					@Override
					public void after_exec(String result) {
						// Do Nothing
						finishSync();
					}
				}, Network.HttpMode.PUT, data, this.mContext, this.Token), this.ServerUrl
				+ "/lists/" + list.getId() + ".json"));
	}

	/**
	 * Sync a Task with the server
	 * 
	 * @param task
	 *            , Task to sync
	 */
	private void sync_task(Task task) {
		List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("task[name]", task.getName()));
		data.add(new BasicNameValuePair("task[priority]", task.getPriority()
				+ ""));
		data.add(new BasicNameValuePair("task[done]", task.isDone() + ""));
		String dueString = "null";
		if (task.getDue() != null) {
			dueString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
			.format(task.getDue().getTime());
		}
		data.add(new BasicNameValuePair("task[due]", dueString));
		data.add(new BasicNameValuePair("task[content]", task.getContent()));
		task.setSyncState(SYNC_STATE.IS_SYNCED);
		safeSafeTask(task);
		this.SyncTasks.add(new Pair<Network, String>(new Network(
				new DataDownloadCommand() {
					@Override
					public void after_exec(String result) {
						finishSync();
					}
				}, Network.HttpMode.PUT, data, this.mContext, this.Token), this.ServerUrl
				+ "/lists/" + task.getList().getId() + "/tasks/" + task.getId()
				+ ".json"));
	}
}
