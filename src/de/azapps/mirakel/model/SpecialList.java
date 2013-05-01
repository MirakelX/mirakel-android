package de.azapps.mirakel.model;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SpecialList {
	private String name;
	private boolean active;
	private String whereQuery;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

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

	public SpecialList(String name, String whereQuery, boolean active) {
		this.name = name;
		this.active = active;
		this.whereQuery = whereQuery;
	}

	// Static Methods
	public static final String TABLE = "special_lists";
	private static final String TAG = "TasksDataSource";
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	private static final String[] allColumns = { "_id", "list_id", "name",
			"content", "done", "due", "priority", "created_at", "updated_at",
			"sync_state" };
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
	 * Get all Tasks
	 * 
	 * @return
	 */
	public static List<SpecialList> all() {
		List<SpecialList> slists = new ArrayList<SpecialList>();
		Cursor c = database.query(TABLE, allColumns,
				"active=1", null, null,
				null, null);
		c.moveToFirst();
		while (!c.isAfterLast()) {
			slists.add(cursorToSList(c));
			c.moveToNext();
		}
		return slists;
	}

	/**
	 * Create a List from a Cursor
	 * 
	 * @param cursor
	 * @return
	 */
	private static SpecialList cursorToSList(Cursor cursor) {
		int i = 0;
		SpecialList slist = new SpecialList(cursor.getString(i++),
				cursor.getString(i++), cursor.getInt(i++) == 1);
		return slist;
	}

}
