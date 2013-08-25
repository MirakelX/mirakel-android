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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.Network;
import de.azapps.mirakelandroid.R;

/**
 * @author az
 * 
 */
public class ListMirakel extends ListBase {
	public static final short SORT_BY_OPT = 0, SORT_BY_DUE = 1,
			SORT_BY_PRIO = 2, SORT_BY_ID = 3;
	public static final String TABLE = "lists";

	// private static final String TAG = "ListMirakel";

	/**
	 * Create an empty list
	 */
	/*
	 * private ListMirakel() { }
	 */

	protected ListMirakel(int id, String name, short sort_by,
			String created_at, String updated_at, int sync_state, int lft,
			int rgt) {
		super(id, name, sort_by, created_at, updated_at, sync_state, lft, rgt);
	}

	ListMirakel(int id, String name) {
		super(id, name);
	}
	private  ListMirakel() {
		super();		
	}

	/**
	 * Update the List in the Database
	 * 
	 * @param list
	 *            The List
	 */
	public void save() {
		SharedPreferences.Editor editor = preferences.edit();
		// TODO implement for specialLists
		if (getId() > 0) {
			setSyncState(getSyncState() == Network.SYNC_STATE.ADD
					|| getSyncState() == Network.SYNC_STATE.IS_SYNCED ? getSyncState()
					: Network.SYNC_STATE.NEED_SYNC);
			setUpdatedAt(new SimpleDateFormat(
					context.getString(R.string.dateTimeFormat),
					Locale.getDefault()).format(new Date()));
			ContentValues values = getContentValues();
			Helpers.updateLog(ListMirakel.getList(getId()),context);
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
		Helpers.updateLog(this, context);
		long id = getId();
		if (id <= 0)
			return;

		if (getSyncState() == Network.SYNC_STATE.ADD) {
			database.delete(Task.TABLE, "list_id = " + id, null);
			database.delete(ListMirakel.TABLE, "_id = " + id, null);
		} else {
			ContentValues values = new ContentValues();
			values.put("sync_state", Network.SYNC_STATE.DELETE);
			database.update(Task.TABLE, values, "list_id = " + id, null);
			database.update(ListMirakel.TABLE, values, "_id=" + id, null);
		}
		database.rawQuery("UPDATE " + ListMirakel.TABLE
				+ " SET lft=lft-2 WHERE lft>" + getRgt() + "; UPDATE "
				+ ListMirakel.TABLE + " SET rgt=rgt-2 WHERE rgt>" + getRgt()
				+ ";", null);
	}

	/**
	 * Count the tasks of that list
	 * 
	 * @return
	 */
	public int countTasks() {
		Cursor c;
		String where;
		if (getId() < 0) {
			where = ((SpecialList) this).getWhereQuery();
		} else {
			where = "list_id = " + getId();
		}
		c = Mirakel.getReadableDatabase().rawQuery(
				"Select count(_id) from " + Task.TABLE + " where " + where
						+ (where.length() != 0 ? " and " : " ")
						+ " done=0 and not sync_state="
						+ Network.SYNC_STATE.DELETE, null);
		c.moveToFirst();
		if (c.getCount() > 0) {
			int n = c.getInt(0);
			c.close();
			return n;
		}
		c.close();
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
	
	public String toJson() {
		String json="{";
		json+="\"name\":\""+getName()+"\",";
		json+="\"id\":"+getId()+",";
		json+="\"created_at\":\""+getCreatedAt()+"\",";
		json+="\"updated_at\":\""+getName()+"\",";
		json+="\"lft\":"+getLft()+",";
		json+="\"rgt\":"+getRgt()+",";
		json+="\"sort_by\":"+getSortBy()+",";
		json+="\"sync_state\":"+getSyncState()+"";
		json+="}";
		return json;
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
			"created_at", "updated_at", "sync_state", "lft", "rgt" };
	@SuppressWarnings("unused")
	private static final String TAG = "ListMirakel";
	private static Context context;
	private static SharedPreferences preferences;

	
	public static ListMirakel parseJson(JsonObject el){
		ListMirakel t=null;
		JsonElement id=el.get("id");
		if(id!=null)
			//use old List from db if existing
			t=ListMirakel.getList(id.getAsInt());
		if(t==null){
			t = new ListMirakel();
		}
		JsonElement j=el.get("name");
		if(j!=null)
			t.setName(j.getAsString());
		
		j=el.get("lft");
		if(j!=null)
			t.setLft(j.getAsInt());
		
		j=el.get("rgt");
		if(j!=null)
			t.setRgt(j.getAsInt());
		
		j=el.get("lft");
		if(j!=null)
			t.setLft(j.getAsInt());
		
		j=el.get("updated_at");
		if(j!=null){
			t.setUpdatedAt(j.getAsString()
				.replace(":", ""));
		}
		
		j=el.get("sort_by");
		if(j!=null)
			t.setSortBy(j.getAsInt());
		
		return t;
	}


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
		values.put("sync_state", Network.SYNC_STATE.ADD);
		values.put("created_at",
				new SimpleDateFormat(
						context.getString(R.string.dateTimeFormat), Locale.US)
						.format(new Date()));
		values.put("updated_at",
				new SimpleDateFormat(
						context.getString(R.string.dateTimeFormat), Locale.US)
						.format(new Date()));

		long insertId = database.insert(ListMirakel.TABLE, null, values);

		// Dirty workaround
		database.execSQL("update "
				+ ListMirakel.TABLE
				+ " SET lft=(SELECT MAX(rgt) from lists)+1, rgt=(SELECT MAX(rgt) from lists)+2 where _id="
				+ insertId);
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
				cursor.getString(i++), cursor.getInt(i++), cursor.getInt(i++),
				cursor.getInt(i++));
		return list;
	}

	/**
	 * Get a List by id selectionArgs
	 * 
	 * @param listId
	 *            List–ID
	 * @return List
	 */
	public static ListMirakel getListForSync(int listId) {
		if (listId > 0) {
			Cursor cursor = database.query(ListMirakel.TABLE, allColumns,
					"_id='" + listId + "'", null, null, null, null);
			cursor.moveToFirst();
			if (cursor.getCount() != 0) {
				ListMirakel t = cursorToList(cursor);
				cursor.close();
				return t;
			}
		}
		return null;
	}

	public static ListMirakel findByName(String name) {
		String[] args = { name };
		Cursor cursor = database.query(ListMirakel.TABLE, allColumns, "name=?",
				args, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			ListMirakel t = cursorToList(cursor);
			cursor.close();
			return t;
		}
		return null;
	}

	public static ListMirakel getList(int listId) {
		if (listId < 0)
			return SpecialList.getSpecialList(-listId);
		ListMirakel t = getListForSync(listId);
		return t;
	}

	/**
	 * Get the first List
	 * 
	 * @return List
	 */
	public static ListMirakel first() {
		Cursor cursor = database.query(ListMirakel.TABLE, allColumns,
				"not sync_state=" + Network.SYNC_STATE.DELETE, null, null,
				null, "lft ASC");
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
				"not sync_state=" + Network.SYNC_STATE.DELETE, null, null,
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
		return all(true);
	}

	public static List<ListMirakel> all(boolean withSpecial) {
		List<ListMirakel> lists = new ArrayList<ListMirakel>();

		if (withSpecial) {
			List<SpecialList> slists = SpecialList.allSpecial();
			for (SpecialList slist : slists) {
				lists.add(slist);
			}
		}

		Cursor cursor = database.rawQuery("  SELECT n.*, "
				+ "COUNT(*)-1 AS level " + "FROM " + ListMirakel.TABLE
				+ " AS n, " + ListMirakel.TABLE + " p "
				+ "WHERE n.lft BETWEEN p.lft AND p.rgt "
				+ " and not n.sync_state=" + Network.SYNC_STATE.DELETE
				+ " GROUP BY n.lft " + "ORDER BY n.lft;", null);
		// query(ListMirakel.TABLE, allColumns,
		// "not sync_state=" + Network.SYNC_STATE.DELETE, null, "lft",
		// null, null);
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
