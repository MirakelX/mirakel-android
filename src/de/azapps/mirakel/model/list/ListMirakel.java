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
import java.util.Date;
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

/**
 * @author az
 * 
 */
public class ListMirakel extends ListBase {
	public static final short SORT_BY_OPT = 0, SORT_BY_DUE = 1,
			SORT_BY_PRIO = 2, SORT_BY_ID = 3;

	public ListMirakel() {
	}

	public ListMirakel(int id, String name, short sort_by, String created_at,
			String updated_at, int sync_state) {
		super(id, name, sort_by, created_at, updated_at, sync_state);
	}

	public ListMirakel(int id, String name, int task_count) {
		super(id, name, task_count);
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

	public static void close() {
		dbHelper.close();
	}

	public static ListMirakel newList(String name) {
		return newList(name, SORT_BY_OPT);
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

		long insertId = database.insert(Mirakel.TABLE_LISTS, null, values);

		Cursor cursor = database.query(Mirakel.TABLE_LISTS, allColumns,
				"_id = " + insertId, null, null, null, null);
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
		Cursor cursor = database.query(Mirakel.TABLE_LISTS, allColumns, "_id='" + listId + "'",
				null, null, null, null);
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
		case Mirakel.LIST_ALL:
			list.setName(context.getString(R.string.list_all));
			list.setSortBy(settings.getInt("SortListAll", ListMirakel.SORT_BY_OPT));
			break;
		case Mirakel.LIST_DAILY:
			list.setName(context.getString(R.string.list_today));
			list.setSortBy(settings
					.getInt("SortListDaily", ListMirakel.SORT_BY_OPT));
			break;
		case Mirakel.LIST_WEEKLY:
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


}
