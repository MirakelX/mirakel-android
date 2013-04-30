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

package de.azapps.mirakel.model.list;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.task.Task;

/**
 * @author az
 * 
 */
public class ListMirakel extends ListBase {
	public static final int ALL = 0, DAILY = -1, WEEKLY = -2;
	public static final short SORT_BY_OPT = 0, SORT_BY_DUE = 1,
			SORT_BY_PRIO = 2, SORT_BY_ID = 3;
	public static final String TABLE = "lists";

	/**
	 * Create an empty list
	 */
	private ListMirakel() {
	}

	ListMirakel(int id, String name, short sort_by, String created_at,
			String updated_at, int sync_state) {
		super(id, name, sort_by, created_at, updated_at, sync_state);
	}

	ListMirakel(int id, String name) {
		super(id, name);
	}

	/**
	 * Update the List in the Database
	 * 
	 * @param list
	 *            The List
	 */
	public void save() {
		SharedPreferences.Editor editor = preferences.edit();
		switch (getId()) {
		case ListMirakel.ALL:
			editor.putInt("SortListAll", getSortBy());
			break;
		case ListMirakel.DAILY:
			editor.putInt("SortListDaily", getSortBy());
			break;
		case ListMirakel.WEEKLY:
			editor.putInt("SortListWeekly", getSortBy());
			break;
		default:
			setSync_state(getSync_state() == Mirakel.SYNC_STATE_ADD
					|| getSync_state() == Mirakel.SYNC_STATE_IS_SYNCED ? getSync_state()
					: Mirakel.SYNC_STATE_NEED_SYNC);
			setUpdated_at(new SimpleDateFormat(
					context.getString(R.string.dateTimeFormat),
					Locale.getDefault()).format(new Date()));
			ContentValues values = getContentValues();
			database.update(ListMirakel.TABLE, values, "_id = " + getId(), null);
		}
		editor.commit();
	}

	/**
	 * Delete a List from the Database
	 * 
	 * @param list
	 */
	public void destroy() {
		long id = getId();
		if (id <= 0)
			return;

		if (getSync_state() == Mirakel.SYNC_STATE_ADD) {
			database.delete(Mirakel.TABLE_TASKS, "list_id = " + id, null);
			database.delete(ListMirakel.TABLE, "_id = " + id, null);
		} else {
			ContentValues values = new ContentValues();
			values.put("sync_state", Mirakel.SYNC_STATE_DELETE);
			database.update(Mirakel.TABLE_TASKS, values, "list_id = " + id,
					null);
			database.update(ListMirakel.TABLE, values, "_id=" + id, null);
		}
	}

	/**
	 * Count the tasks of that list
	 * 
	 * @return
	 */
	public int countTasks() {
		// TODO implement it
		return 0;
	}

	/**
	 * Get all Tasks
	 * 
	 * @return
	 */
	public List<Task> tasks() {
		return Task.getTasks(this, getSortBy(), false);
	}

	/**
	 * Get all Tasks
	 * 
	 * @param showDone
	 * @return
	 */
	public List<Task> tasks(boolean showDone) {
		return Task.getTasks(this, getSortBy(), showDone);
	}

	// Static Methods

	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	private static final String[] allColumns = { "_id", "name", "sort_by",
			"created_at", "updated_at", "sync_state" };
	private static Context context;
	private static SharedPreferences preferences;

	/**
	 * Initialize the Database and the preferences
	 * 
	 * @param context
	 *            The Application-Context
	 */
	public static void init(Context context) {
		ListMirakel.context = context;
		dbHelper = new DatabaseHelper(context);
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		database = dbHelper.getWritableDatabase();
	}

	/**
	 * Close the Database–Connection
	 */
	public static void close() {
		dbHelper.close();
	}

	/**
	 * Create and insert a new List
	 * 
	 * @param name
	 * @return
	 */
	public static ListMirakel newList(String name) {
		return newList(name, SORT_BY_OPT);
	}

	/**
	 * Create and insert a new List
	 * 
	 * @param name
	 *            Name of the List
	 * @param sort_by
	 *            the default sorting
	 * @return new List
	 */
	public static ListMirakel newList(String name, int sort_by) {
		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("sort_by", sort_by);
		values.put("sync_state", Mirakel.SYNC_STATE_ADD);
		values.put("created_at",
				new SimpleDateFormat(
						context.getString(R.string.dateTimeFormat), Locale.US)
						.format(new Date()));
		values.put("updated_at",
				new SimpleDateFormat(
						context.getString(R.string.dateTimeFormat), Locale.US)
						.format(new Date()));

		long insertId = database.insert(ListMirakel.TABLE, null, values);

		Cursor cursor = database.query(ListMirakel.TABLE, allColumns, "_id = "
				+ insertId, null, null, null, null);
		cursor.moveToFirst();
		ListMirakel newList = cursorToList(cursor);
		cursor.close();
		return newList;
	}

	/**
	 * Create a List from a Cursor
	 * 
	 * @param cursor
	 * @return
	 */
	private static ListMirakel cursorToList(Cursor cursor) {
		int i = 0;
		int id = cursor.getInt(i++);
		ListMirakel list = new ListMirakel(id, cursor.getString(i++),
				cursor.getShort(i++), cursor.getString(i++),
				cursor.getString(i++), cursor.getInt(i));
		return list;
	}

	/**
	 * Get a List by id
	 * 
	 * @param listId
	 *            List–ID
	 * @return List
	 */
	public static ListMirakel getList(int listId) {
		Cursor cursor = database.query(ListMirakel.TABLE, allColumns, "_id='"
				+ listId + "'", null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			ListMirakel t = cursorToList(cursor);
			cursor.close();
			return t;
		}

		ListMirakel list = new ListMirakel();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context.getApplicationContext());
		switch (listId) {
		case ListMirakel.ALL:
			list.setName(context.getString(R.string.list_all));
			list.setSortBy(settings.getInt("SortListAll",
					ListMirakel.SORT_BY_OPT));
			break;
		case ListMirakel.DAILY:
			list.setName(context.getString(R.string.list_today));
			list.setSortBy(settings.getInt("SortListDaily",
					ListMirakel.SORT_BY_OPT));
			break;
		case ListMirakel.WEEKLY:
			list.setName(context.getString(R.string.list_week));
			list.setSortBy(settings.getInt("SortListWeekly",
					ListMirakel.SORT_BY_OPT));
			break;
		default:
			Toast.makeText(context, "NO SUCH LIST!", Toast.LENGTH_LONG).show();
			return null;
		}
		list.setId(listId);

		return list;
	}

	/**
	 * Get the first List
	 * 
	 * @return List
	 */
	public static ListMirakel first() {
		Cursor cursor = database.query(ListMirakel.TABLE, allColumns,
				"not sync_state=" + Mirakel.SYNC_STATE_DELETE, null, null,
				null, "_id ASC");
		ListMirakel list = null;
		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			list = cursorToList(cursor);
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

	/**
	 * Get the last List
	 * 
	 * @return List
	 */
	public static ListMirakel last() {
		Cursor cursor = database.query(ListMirakel.TABLE, allColumns,
				"not sync_state=" + Mirakel.SYNC_STATE_DELETE, null, null,
				null, "_id DESC");
		ListMirakel list = null;
		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			list = cursorToList(cursor);
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

	/**
	 * Get all Lists in the Database
	 * 
	 * @return List of Lists
	 */
	public static List<ListMirakel> all() {
		List<ListMirakel> lists = new ArrayList<ListMirakel>();
		// TODO Get from strings.xml
		if (preferences.getBoolean("listAll", true))
			lists.add(new ListMirakel(ListMirakel.ALL, context
					.getString(R.string.list_all)));
		if (preferences.getBoolean("listToday", true))
			lists.add(new ListMirakel(ListMirakel.DAILY, context
					.getString(R.string.list_today)));
		if (preferences.getBoolean("listWeek", true))
			lists.add(new ListMirakel(ListMirakel.WEEKLY, context
					.getString(R.string.list_week)));

		Cursor cursor = database.query(ListMirakel.TABLE, allColumns,
				"not sync_state=" + Mirakel.SYNC_STATE_DELETE, null, null,
				null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			ListMirakel list = cursorToList(cursor);
			lists.add(list);
			cursor.moveToNext();
		}
		cursor.close();
		return lists;
	}

	/**
	 * Get Lists by a sync state
	 * 
	 * @param state
	 * @see Mirakel.SYNC_STATE*
	 * @return
	 */
	public static List<ListMirakel> bySyncState(short state) {
		List<ListMirakel> lists = new ArrayList<ListMirakel>();
		Cursor c = database.query(ListMirakel.TABLE, allColumns, "sync_state="
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
