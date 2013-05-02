package de.azapps.mirakel.model.list;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.task.Task;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SpecialList extends ListMirakel {
	private boolean active;
	private String whereQuery;

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getWhereQuery() {
		return whereQuery;
	}

	public void setWhereQuery(String whereQuery) {
		this.whereQuery = whereQuery;
	}

	SpecialList(int id, String name, String whereQuery, boolean active,
			short sort_by, int sync_state) {
		super(-id, name, sort_by, "", "", sync_state);
		this.active = active;
		this.whereQuery = whereQuery;
	}

	/**
	 * Get all Tasks
	 * 
	 * @return
	 */
	public List<Task> tasks() {
		return Task.getTasks(this, getSortBy(), false, getWhereQuery());
	}

	/**
	 * Get all Tasks
	 * 
	 * @param showDone
	 * @return
	 */
	public List<Task> tasks(boolean showDone) {
		return Task.getTasks(this, getSortBy(), showDone, getWhereQuery());
	}

	// Static Methods
	public static final String TABLE = "special_lists";
	private static final String TAG = "TasksDataSource";
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	private static final String[] allColumns = { "_id", "name", "whereQuery",
			"active", "sort_by", "sync_state" };
	private static Context context;

	/**
	 * Initialize the Database and the preferences
	 * 
	 * @param context
	 *            The Application-Context
	 */
	public static void init(Context context) {
		SpecialList.context = context;
		dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
	}

	/**
	 * Close the Database-Connection
	 */
	public static void close() {
		dbHelper.close();
	}

	public static SpecialList newSpecialList(String name, String whereQuery,
			boolean active) {

		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("whereQuery", whereQuery);
		values.put("active", active);
		long insertId = database.insert(TABLE, null, values);
		Cursor cursor = database.query(TABLE, allColumns, "_id = " + insertId,
				null, null, null, null);
		cursor.moveToFirst();
		SpecialList newSList = cursorToSList(cursor);
		return newSList;
	}

	/**
	 * Update the List in the Database
	 * 
	 * @param list
	 *            The List
	 */
	public void save() {
		setSync_state(getSync_state() == Mirakel.SYNC_STATE_ADD
				|| getSync_state() == Mirakel.SYNC_STATE_IS_SYNCED ? getSync_state()
				: Mirakel.SYNC_STATE_NEED_SYNC);
		ContentValues values = getContentValues();
		database.update(TABLE, values, "_id = " + Math.abs(getId()), null);
	}

	/**
	 * Delete a List from the Database
	 * 
	 * @param list
	 */
	public void destroy() {
		long id = Math.abs(getId());

		if (getSync_state() != Mirakel.SYNC_STATE_ADD) {
			setSync_state(Mirakel.SYNC_STATE_DELETE);
		}
		setActive(false);
		ContentValues values = new ContentValues();
		database.update(TABLE, values, "_id=" + id, null);
	}

	public ContentValues getContentValues() {
		ContentValues cv = new ContentValues();
		cv.put("name", getName());
		cv.put("sort_by", getSortBy());
		cv.put("sync_state", getSync_state());
		cv.put("active", isActive() ? 1 : 0);
		cv.put("whereQuery", getWhereQuery());
		return cv;
	}

	/**
	 * Get all Tasks
	 * 
	 * @return
	 */
	public static List<SpecialList> allSpecial() {
		List<SpecialList> slists = new ArrayList<SpecialList>();
		Cursor c = database.query(TABLE, allColumns, "active=1", null, null,
				null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			slists.add(cursorToSList(c));
			c.moveToNext();
		}
		return slists;
	}

	/**
	 * Get a List by id selectionArgs
	 * 
	 * @param listId
	 *            List–ID
	 * @return List
	 */
	public static SpecialList getSpecialList(int listId) {
		Cursor cursor = database.query(SpecialList.TABLE, allColumns, "_id='"
				+ listId + "'", null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			SpecialList t = cursorToSList(cursor);
			cursor.close();
			return t;
		}
		Log.e("Blubb", "WARNING: No such specialList: " + listId);
		return firstSpecial();
	}

	/**
	 * Get the first List
	 * 
	 * @return List
	 */
	public static SpecialList firstSpecial() {
		Cursor cursor = database.query(SpecialList.TABLE, allColumns,
				"not sync_state=" + Mirakel.SYNC_STATE_DELETE, null, null,
				null, "_id ASC");
		SpecialList list = null;
		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			list = cursorToSList(cursor);
			cursor.moveToNext();
		} else {
			list = new SpecialList(0, context.getString(R.string.list_all), "",
					true, SORT_BY_OPT, (int) Mirakel.SYNC_STATE_NOTHING);
		}
		cursor.close();
		return list;
	}

	/**
	 * Create a List from a Cursor
	 * 
	 * @param cursor
	 * @return
	 */
	private static SpecialList cursorToSList(Cursor cursor) {
		int i = 0;
		SpecialList slist = new SpecialList(cursor.getInt(i++),
				cursor.getString(i++), cursor.getString(i++),
				cursor.getInt(i++) == 1, cursor.getShort(i++),
				cursor.getInt(i++));
		return slist;
	}

}
