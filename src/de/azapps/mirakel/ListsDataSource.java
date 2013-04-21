/**
 * 
 */
package de.azapps.mirakel;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

/**
 * @author weiznich
 * 
 */
public class ListsDataSource {
	private static final String TAG = "ListsDataSource";
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	public static final String[] allColumns = { "_id", "name", "sort_by", "created_at",
			"updated_at", "sync_state" };
	private Context context;
	private SharedPreferences preferences;

	/**
	 * Creates the DataSource and initialize the Database
	 * 
	 * @param context
	 */
	public ListsDataSource(Context context) {
		dbHelper = new DatabaseHelper(context);
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		this.context = context;
	}

	/**
	 * Open the Database
	 * 
	 * @throws SQLException
	 */
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	/**
	 * Close the Database
	 */
	public void close() {
		dbHelper.close();
	}

	/**
	 * Shortcut for creating a List
	 * 
	 * @param name
	 *            Name of the List
	 * @return new List
	 */
	public List_mirakle createList(String name) {
		return createList(name, 0);
	}

	/**
	 * Create a List
	 * 
	 * @param name
	 *            Name of the List
	 * @param sort_by
	 *            the default sorting
	 * @return new List
	 */
	public List_mirakle createList(String name, int sort_by) {
		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("sort_by", sort_by);
		values.put("sync_state", Mirakel.SYNC_STATE_ADD);

		long insertId = database.insert(Mirakel.TABLE_LISTS, null, values);

		Cursor cursor = database.query(Mirakel.TABLE_LISTS, allColumns,
				"_id = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		List_mirakle newList = cursorToList(cursor);
		cursor.close();
		return newList;
	}

	/**
	 * Update the List in the Database
	 * 
	 * @param list
	 *            The List
	 */
	public void saveList(List_mirakle list) {
		Log.v(TAG, "saveTask");
		SharedPreferences.Editor editor = preferences.edit();
		switch (list.getId()) {
		case Mirakel.LIST_ALL:
			editor.putInt("SortListAll", list.getSortBy());
			break;
		case Mirakel.LIST_DAILY:
			editor.putInt("SortListDaily", list.getSortBy());
			break;
		case Mirakel.LIST_WEEKLY:
			editor.putInt("SortListWeekly", list.getSortBy());
			break;
		default:
			list.setSync_state(list.getSync_state() == Mirakel.SYNC_STATE_ADD ? Mirakel.SYNC_STATE_ADD
					: Mirakel.SYNC_STATE_NEED_SYNC);
			ContentValues values = list.getContentValues();
			database.update(Mirakel.TABLE_LISTS, values, "_id = " + list.getId(), null);
		}
		editor.commit();
	}

	/**
	 * Delete a List from the Database
	 * 
	 * @param list
	 */
	public void deleteList(List_mirakle list) {
		long id = list.getId();
		if (id <= 0)
			return;

		if (list.getSync_state() == Mirakel.SYNC_STATE_ADD) {
			database.delete(Mirakel.TABLE_TASKS, "list_id = " + id, null);
			database.delete(Mirakel.TABLE_LISTS, "_id = " + id, null);
		} else {
			ContentValues values = new ContentValues();
			values.put("sync_state", Mirakel.SYNC_STATE_DELETE);
			database.update(Mirakel.TABLE_TASKS, values, "list_id = " + id, null);
			database.update(Mirakel.TABLE_LISTS, values, "_id=" + id, null);
		}
	}

	/**
	 * Get all Lists in the Database
	 * 
	 * @return List of Lists
	 */
	public List<List_mirakle> getAllLists() {
		List<List_mirakle> lists = new ArrayList<List_mirakle>();
		// TODO Get from strings.xml
		if (preferences.getBoolean("listAll", true))
			lists.add(new List_mirakle(Mirakel.LIST_ALL, context
					.getString(R.string.list_all), task_count(Mirakel.LIST_ALL)));
		if (preferences.getBoolean("listToday", true))
			lists.add(new List_mirakle(Mirakel.LIST_DAILY, context
					.getString(R.string.list_today),
					task_count(Mirakel.LIST_DAILY)));
		if (preferences.getBoolean("listWeek", true))
			lists.add(new List_mirakle(Mirakel.LIST_WEEKLY, context
					.getString(R.string.list_week),
					task_count(Mirakel.LIST_WEEKLY)));

		Cursor cursor = database.query(Mirakel.TABLE_LISTS, allColumns, "not sync_state="
				+ Mirakel.SYNC_STATE_DELETE, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			List_mirakle list = cursorToList(cursor);
			lists.add(list);
			cursor.moveToNext();
		}
		cursor.close();
		return lists;
	}

	/**
	 * Get the first List
	 * 
	 * @return List
	 */
	public List_mirakle getFirstList() {
		Cursor cursor = database.query(Mirakel.TABLE_LISTS, allColumns, "not sync_state="
				+ Mirakel.SYNC_STATE_DELETE, null, null, null, "_id ASC");
		List_mirakle list = null;
		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			list = cursorToList(cursor);
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

	/**
	 * Get a List by id
	 * 
	 * @param id
	 *            List–ID
	 * @return List
	 */
	public List_mirakle getList(int id) {
		open();

		Cursor cursor = database.query(Mirakel.TABLE_LISTS, allColumns, "_id='" + id + "'",
				null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			List_mirakle t = cursorToList(cursor);
			cursor.close();
			return t;
		}

		List_mirakle list = new List_mirakle();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context.getApplicationContext());
		switch (id) {
		case Mirakel.LIST_ALL:
			list.setName(this.context.getString(R.string.list_all));
			list.setSortBy(settings.getInt("SortListAll", Mirakel.SORT_BY_OPT));
			break;
		case Mirakel.LIST_DAILY:
			list.setName(this.context.getString(R.string.list_today));
			list.setSortBy(settings
					.getInt("SortListDaily", Mirakel.SORT_BY_OPT));
			break;
		case Mirakel.LIST_WEEKLY:
			list.setName(this.context.getString(R.string.list_week));
			list.setSortBy(settings.getInt("SortListWeekly",
					Mirakel.SORT_BY_OPT));
			break;
		default:
			Toast.makeText(context, "NO SUCH LIST!", Toast.LENGTH_LONG).show();
			return null;
		}
		list.setId(id);

		return list;
	}

	/**
	 * Count all Tasks in a List
	 * 
	 * @param list_id
	 *            List ID
	 * @return Count
	 */
	private int task_count(int list_id) {
		String count = "Select count(_id) from tasks where";
		switch (list_id) {
		case Mirakel.LIST_ALL:
			break;
		case Mirakel.LIST_DAILY:
			return new TasksDataSource(null).getTasks(Mirakel.LIST_DAILY)
					.size();
		case Mirakel.LIST_WEEKLY:
			return new TasksDataSource(null).getTasks(Mirakel.LIST_WEEKLY)
					.size();
		default:
			count += " list_id=" + list_id + " and";
		}
		count += " done=0 and not sync_state=" + Mirakel.SYNC_STATE_DELETE;
		open();
		Cursor c = database.rawQuery(count, null);
		c.moveToFirst();
		int task_count = c.getInt(0);
		c.close();
		return task_count;
	}

	/**
	 * Create a List from a Cursor
	 * 
	 * @param cursor
	 * @return
	 */
	private List_mirakle cursorToList(Cursor cursor) {
		int i = 0;
		int id = cursor.getInt(i++);
		List_mirakle list = new List_mirakle(id, cursor.getString(i++),
				cursor.getShort(i++), cursor.getString(i++),
				cursor.getString(i++), task_count(id), cursor.getInt(i));
		return list;
	}

	/**
	 * Returns a List of all Lists, which are marked as deleted
	 * 
	 * @return List of Lists
	 */
	protected List<List_mirakle> getdeletedLists() {
		List<List_mirakle> lists = new ArrayList<List_mirakle>();
		Cursor c = database.query(Mirakel.TABLE_LISTS, allColumns, "sync_state="
				+ Mirakel.SYNC_STATE_DELETE, null, null, null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			lists.add(cursorToList(c));
			c.moveToNext();
		}
		lists = c.getCount() == 0 ? null : lists;
		c.close();
		return lists;

	}

	/**
	 * Sync the lists with the Server
	 * 
	 * @param email
	 * @param password
	 * @param url
	 */
	public void sync_lists(final String email, final String password,
			final String url) {
		Log.v(TAG, "sync lists");
		List<List_mirakle> deleted = getdeletedLists();
		if (deleted != null) {
			for (int i = 0; i < deleted.size(); i++) {
				delete_list(deleted.get(i), email, password, url);
				deleted.get(i).setSync_state(Mirakel.SYNC_STATE_ADD);
				deleteList(deleted.get(i));
			}
		}
		new Network(new DataDownloadCommand() {
			@Override
			public void after_exec(String result) {
				List_mirakle lists_server[] = new Gson().fromJson(result,
						List_mirakle[].class);
				List<List_mirakle> lists_local = getAllLists();
				for (int i = 0; i < lists_local.size(); i++) {
					List_mirakle list = lists_local.get(i);
					switch (list.getSync_state()) {
					case Mirakel.SYNC_STATE_ADD:
						add_list(list, email, password, url);
						break;
					case Mirakel.SYNC_STATE_NEED_SYNC:
						sync_list(list, email, password, url);
						break;
					default:
					}
				}
				merge_with_server(lists_server);
				ContentValues values = new ContentValues();
				values.put("sync_state", Mirakel.SYNC_STATE_NOTHING);
				database.update(Mirakel.TABLE_LISTS, values, "not sync_state="
						+ Mirakel.SYNC_STATE_NOTHING, null);

			}
		}, email, password, Mirakel.Http_Mode.GET).execute(url + "/lists.json");
	}

	/**
	 * Delete a List from the Server
	 * 
	 * @param list
	 * @param email
	 * @param password
	 * @param url
	 */
	protected void delete_list(final List_mirakle list, final String email,
			final String password, final String url) {
		new Network(new DataDownloadCommand() {
			@Override
			public void after_exec(String result) {
				// Do Nothing

			}
		}, email, password, Mirakel.Http_Mode.DELETE).execute(url + "/lists/"
				+ list.getId() + ".json");

	}

	/**
	 * Sync one List with the Server
	 * 
	 * @param list
	 * @param email
	 * @param password
	 * @param url
	 */
	protected void sync_list(List_mirakle list, String email, String password,
			String url) {
		List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("list[name]", list.getName()));
		new Network(new DataDownloadCommand() {
			@Override
			public void after_exec(String result) {
				// Do Nothing

			}
		}, email, password, Mirakel.Http_Mode.PUT, data).execute(url
				+ "/lists/" + list.getId() + ".json");
	}

	/**
	 * Create a List on the Server
	 * 
	 * @param list
	 * @param email
	 * @param password
	 * @param url
	 */
	protected void add_list(final List_mirakle list, final String email,
			final String password, final String url) {
		List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
		data.add(new BasicNameValuePair("list[name]", list.getName()));
		new Network(new DataDownloadCommand() {

			@Override
			public void after_exec(String result) {
				List_mirakle list_response = new Gson().fromJson(result,
						List_mirakle.class);
				ContentValues values = new ContentValues();
				values.put("_id", list_response.getId());
				open();
				database.update(Mirakel.TABLE_LISTS, values, "_id=" + list.getId(), null);
				values = new ContentValues();
				values.put("list_id", list_response.getId());
				database.update(Mirakel.TABLE_TASKS, values, "list_id=" + list.getId(),
						null);

			}
		}, email, password, Mirakel.Http_Mode.POST, data).execute(url
				+ "/lists.json");

	}

	/**
	 * Merge Lists
	 * 
	 * @param lists_server
	 */
	protected void merge_with_server(List_mirakle[] lists_server) {
		for (int i = 0; i < lists_server.length; i++) {
			List_mirakle list = getList(lists_server[i].getId());
			if (list == null) {
				long id = database.insert(Mirakel.TABLE_LISTS, null,
						lists_server[i].getContentValues());
				ContentValues values = new ContentValues();
				values.put("_id", lists_server[i].getId());
				open();
				database.update(Mirakel.TABLE_LISTS, values, "_id=" + id, null);
			} else {
				if (list.getSync_state() == Mirakel.SYNC_STATE_NOTHING) {
					saveList(lists_server[i]);
				} else {
					Log.e(TAG, "Merging lists not implementet");
				}
			}
		}

	}
}
