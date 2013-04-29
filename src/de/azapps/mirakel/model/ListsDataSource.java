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
/**
 * 
 */
package de.azapps.mirakel.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;

/**
 * @author weiznich
 * 
 */
public class ListsDataSource {
	private static final String TAG = "ListsDataSource";
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	private static final String[] allColumns = { "_id", "name", "sort_by", "created_at",
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
		values.put("created_at", new SimpleDateFormat(context.getString(R.string.dateTimeFormat), Locale.US).format(new Date()));
		values.put("updated_at", new SimpleDateFormat(context.getString(R.string.dateTimeFormat), Locale.US).format(new Date()));

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
			list.setSync_state(list.getSync_state() == Mirakel.SYNC_STATE_ADD|| list.getSync_state()==Mirakel.SYNC_STATE_IS_SYNCED? list.getSync_state()
					: Mirakel.SYNC_STATE_NEED_SYNC);
			list.setUpdated_at(new SimpleDateFormat(context.getString(R.string.dateTimeFormat), Locale.getDefault()).format(new Date()));
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
	 * @param l
	 *            List–ID
	 * @return List
	 */
	public List_mirakle getList(int l) {
		open();

		Cursor cursor = database.query(Mirakel.TABLE_LISTS, allColumns, "_id='" + l + "'",
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
		switch (l) {
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
		list.setId(l);

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


	public List<List_mirakle> getListsBySyncState(short state) {
		List<List_mirakle> lists = new ArrayList<List_mirakle>();
		Cursor c = database.query(Mirakel.TABLE_LISTS, allColumns, "sync_state="
				+ state, null, null, null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			lists.add(cursorToList(c));
			c.moveToNext();
		}
		c.close();
		return lists;
	}
}
