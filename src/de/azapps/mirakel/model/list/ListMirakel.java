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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.UndoHistory;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_STATE;
import de.azapps.mirakelandroid.R;

/**
 * @author az
 * 
 */
public class ListMirakel extends ListBase {
	public static final short SORT_BY_OPT = 0, SORT_BY_DUE = 1,
			SORT_BY_PRIO = 2, SORT_BY_ID = 3, SORT_BY_REVERT_DEFAULT = 4;
	public static final String TABLE = "lists";

	public boolean isSpecialList() {
		return false;
	}

	// private static final String TAG = "ListMirakel";

	/**
	 * Create an empty list
	 */
	/*
	 * private ListMirakel() { }
	 */

	protected ListMirakel(int id, String name, short sort_by,
			String created_at, String updated_at, SYNC_STATE sync_state,
			int lft, int rgt, int color, AccountMirakel account) {
		super(id, name, sort_by, created_at, updated_at, sync_state, lft, rgt,
				color, account);
	}

	ListMirakel(int id, String name) {
		super(id, name);
	}

	private ListMirakel() {
		super();
	}

	protected ListMirakel(int id, String name, short sort_by,
			String created_at, String updated_at, SYNC_STATE sync_state,
			int lft, int rgt, int color, int account) {
		super(id, name, sort_by, created_at, updated_at, sync_state, lft, rgt,
				color, account);
	}

	/**
	 * Update the List in the Database
	 * 
	 * @param list
	 *            The List
	 */
	public void save() {
		save(true);
	}

	public void save(boolean log) {
		SharedPreferences.Editor editor = MirakelPreferences.getEditor();
		// TODO implement for specialLists
		if (getId() > 0) {
			database.beginTransaction();
			setSyncState(getSyncState() == SYNC_STATE.ADD
					|| getSyncState() == SYNC_STATE.IS_SYNCED ? getSyncState()
					: SYNC_STATE.NEED_SYNC);
			setUpdatedAt(new SimpleDateFormat(
					context.getString(R.string.dateTimeFormat),
					Locale.getDefault()).format(new Date()));
			ContentValues values = getContentValues();
			if (log)
				UndoHistory.updateLog(ListMirakel.getList(getId()), context);
			database.update(ListMirakel.TABLE, values, DatabaseHelper.ID
					+ " = " + getId(), null);
			database.setTransactionSuccessful();
			database.endTransaction();

		}
		editor.commit();
	}

	/**
	 * Delete a List from the Database
	 * 
	 * @param list
	 */
	public void destroy() {
		destroy(false);
	}

	public void destroy(boolean force) {
		if (!force)
			UndoHistory.updateLog(this, context);
		long id = getId();
		if (id <= 0)
			return;
		database.beginTransaction();
		try {
			if (getSyncState() == SYNC_STATE.ADD || force) {
				database.delete(Task.TABLE, Task.LIST_ID + " = " + id, null);
				database.delete(ListMirakel.TABLE, DatabaseHelper.ID + " = "
						+ id, null);
			} else {
				ContentValues values = new ContentValues();
				values.put(SyncAdapter.SYNC_STATE, SYNC_STATE.DELETE.toInt());
				database.update(Task.TABLE, values, Task.LIST_ID + " = " + id,
						null);
				database.update(ListMirakel.TABLE, values, DatabaseHelper.ID
						+ "=" + id, null);
			}
			database.rawQuery("UPDATE " + ListMirakel.TABLE + " SET " + LFT
					+ "=" + LFT + "-2 WHERE " + LFT + ">" + getLft()
					+ "; UPDATE " + ListMirakel.TABLE + " SET " + RGT + "="
					+ RGT + "-2 WHERE " + LFT + ">" + getRgt() + ";", null);
			database.setTransactionSuccessful();
		} catch (Exception e) {
			Log.wtf(TAG, "cannot remove List");
		} finally {
			database.endTransaction();
		}
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
			where = ((SpecialList) this).getWhereQuery(true);
		} else {

			where = Task.LIST_ID + " = " + getId();
		}
		c = Mirakel.getReadableDatabase().rawQuery(
				"Select count(" + DatabaseHelper.ID + ") from " + Task.TABLE
						+ " where " + where
						+ (where.length() != 0 ? " and " : " ") + " "
						+ Task.DONE + "=0 and not " + SyncAdapter.SYNC_STATE
						+ "=" + SYNC_STATE.DELETE, null);
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
		String json = "{";
		json += "\"name\":\"" + getName() + "\",";
		json += "\"id\":" + getId() + ",";
		json += "\"created_at\":\"" + getCreatedAt() + "\",";
		json += "\"updated_at\":\"" + getName() + "\",";
		json += "\"lft\":" + getLft() + ",";
		json += "\"rgt\":" + getRgt() + ",";
		json += "\"sort_by\":" + getSortBy() + ",";
		json += "\"sync_state\":" + getSyncState() + "";
		json += "}";
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
	private static final String[] allColumns = { DatabaseHelper.ID,
			DatabaseHelper.NAME, SORT_BY, DatabaseHelper.CREATED_AT,
			DatabaseHelper.UPDATED_AT, SyncAdapter.SYNC_STATE, LFT, RGT, COLOR,
			ACCOUNT_ID };
	private static final String TAG = "ListMirakel";
	private static Context context;

	public static ListMirakel parseJson(JsonObject el) {
		ListMirakel t = null;
		JsonElement id = el.get("id");
		if (id != null)
			// use old List from db if existing
			t = ListMirakel.getList(id.getAsInt());
		if (t == null) {
			t = new ListMirakel();
		}
		JsonElement j = el.get("name");
		if (j != null)
			t.setName(j.getAsString());

		j = el.get("lft");
		if (j != null)
			t.setLft(j.getAsInt());

		j = el.get("rgt");
		if (j != null)
			t.setRgt(j.getAsInt());

		j = el.get("lft");
		if (j != null)
			t.setLft(j.getAsInt());

		j = el.get("updated_at");
		if (j != null) {
			t.setUpdatedAt(j.getAsString().replace(":", ""));
		}

		j = el.get("sort_by");
		if (j != null)
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
		return newList(name, sort_by, AccountMirakel.getDefault());
	}

	public static ListMirakel newList(String name, int sort_by,
			AccountMirakel account) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.NAME, name);
		values.put(ACCOUNT_ID, account.getId());
		values.put(SORT_BY, sort_by);
		values.put(SyncAdapter.SYNC_STATE, SYNC_STATE.ADD.toInt());
		values.put(DatabaseHelper.CREATED_AT,
				new SimpleDateFormat(
						context.getString(R.string.dateTimeFormat), Locale.US)
						.format(new Date()));
		values.put(DatabaseHelper.UPDATED_AT,
				new SimpleDateFormat(
						context.getString(R.string.dateTimeFormat), Locale.US)
						.format(new Date()));
		values.put(RGT, 0);
		values.put(LFT, 0);
		database.beginTransaction();
		long insertId;
		insertId = database.insert(ListMirakel.TABLE, null, values);
		// Dirty workaround
		database.execSQL("update " + ListMirakel.TABLE
				+ " SET lft=(SELECT MAX(" + RGT + ") from " + TABLE + ")+1, "
				+ RGT + "=(SELECT MAX(" + RGT + ") from lists)+2 where "
				+ DatabaseHelper.ID + "=" + insertId);
		database.setTransactionSuccessful();
		database.endTransaction();

		Cursor cursor = database.query(ListMirakel.TABLE, allColumns,
				DatabaseHelper.ID + " = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		ListMirakel newList = cursorToList(cursor);
		cursor.close();
		UndoHistory.logCreate(newList, context);
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
				cursor.getString(i++), SYNC_STATE.parseInt(cursor.getInt(i++)),
				cursor.getInt(i++), cursor.getInt(i++), cursor.getInt(i++),
				cursor.getInt(i++));
		return list;
	}

	public static ListMirakel findByName(String name) {
		String[] args = { name };
		Cursor cursor = database.query(ListMirakel.TABLE, allColumns,
				DatabaseHelper.NAME + "=?", args, null, null, null);
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

		Cursor cursor = database
				.query(ListMirakel.TABLE, allColumns, DatabaseHelper.ID + "='"
						+ listId + "'", null, null, null, null);
		if (cursor.moveToFirst()) {
			ListMirakel t = cursorToList(cursor);
			cursor.close();
			return t;
		}
		return null;
	}

	public static int count() {
		Cursor c = database.rawQuery("Select count(" + DatabaseHelper.ID
				+ ") from " + TABLE, null);
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		return count;
	}

	/**
	 * Get the first List
	 * 
	 * @return List
	 */
	public static ListMirakel first() {
		Cursor cursor = database.query(ListMirakel.TABLE, allColumns, "not "
				+ SyncAdapter.SYNC_STATE + "=" + SYNC_STATE.DELETE, null, null,
				null, LFT + " ASC");
		ListMirakel list = null;
		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			list = cursorToList(cursor);
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

	public static ListMirakel safeFirst(Context ctx) {
		ListMirakel s = first();
		if (s == null) {
			s = ListMirakel.newList(ctx.getString(R.string.inbox));
		}
		return s;
	}

	/**
	 * Get the last List
	 * 
	 * @return List
	 */
	public static ListMirakel last() {
		Cursor cursor = database.query(ListMirakel.TABLE, allColumns, "not "
				+ SyncAdapter.SYNC_STATE + "=" + SYNC_STATE.DELETE, null, null,
				null, DatabaseHelper.ID + " DESC");
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
				+ " AS n, " + ListMirakel.TABLE + " p " + "WHERE n." + LFT
				+ " BETWEEN p." + LFT + " AND p." + RGT + " " + " and not n."
				+ SyncAdapter.SYNC_STATE + "=" + SYNC_STATE.DELETE
				+ " GROUP BY n." + LFT + " " + "ORDER BY n." + LFT + ";", null);
		// query(ListMirakel.TABLE, allColumns,
		// "not sync_state=" + SYNC_STATE.DELETE, null, "lft",
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
	public static List<ListMirakel> bySyncState(SYNC_STATE state) {

		Cursor c = database.query(ListMirakel.TABLE, allColumns,
				SyncAdapter.SYNC_STATE + "=" + state, null, null, null, null);
		c.moveToFirst();
		List<ListMirakel> lists = cursorToListList(c);
		c.close();
		return lists;
	}

	public static List<ListMirakel> byAccount(AccountMirakel a) {
		Cursor c = database.query(ListMirakel.TABLE, allColumns, ACCOUNT_ID
				+ "=" + a.getId(), null, null, null, null);
		c.moveToFirst();
		List<ListMirakel> lists = cursorToListList(c);
		c.close();
		return lists;
	}

	private static List<ListMirakel> cursorToListList(Cursor c) {
		List<ListMirakel> lists = new ArrayList<ListMirakel>();
		while (!c.isAfterLast()) {
			lists.add(cursorToList(c));
			c.moveToNext();
		}
		return lists;
	}

	public static ListMirakel getSafeDefaultList(Context context) {
		ListMirakel list = MirakelPreferences.getImportDefaultList(true);
		return list;
	}

	public static List<ListMirakel> getListsForAccount(AccountMirakel account) {
		if (account == null || !account.isEnabeld()) {
			return new ArrayList<ListMirakel>();
		}
		Cursor c = database.query(TABLE, allColumns, "NOT "
				+ SyncAdapter.SYNC_STATE + "=" + SYNC_STATE.DELETE + " and "
				+ ACCOUNT_ID + "=" + account.getId(), null, null, null, null);
		c.moveToFirst();
		List<ListMirakel> list = cursorToListList(c);
		c.close();
		return list;
	}

}
