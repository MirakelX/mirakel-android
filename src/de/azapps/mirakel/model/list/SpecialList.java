package de.azapps.mirakel.model.list;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.Network;

public class SpecialList extends ListMirakel {
	private boolean active;
	private String whereQuery;
	private ListMirakel defaultList;
	private Integer defaultDate;

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

	public ListMirakel getDefaultList() {
		if (defaultList == null) {
			return ListMirakel.first();
		}
		return defaultList;
	}

	public void setDefaultList(ListMirakel defaultList) {
		this.defaultList = defaultList;
	}

	public Integer getDefaultDate() {
		return defaultDate;
	}

	public void setDefaultDate(Integer defaultDate) {
		this.defaultDate = defaultDate;
	}

	SpecialList(int id, String name, String whereQuery, boolean active,
			ListMirakel listMirakel, Integer defaultDate, short sort_by,
			int sync_state) {

		super(-id, name, sort_by, "", "", sync_state, 0, 0);
		this.active = active;
		this.whereQuery = whereQuery;
		this.defaultList = listMirakel;
		this.defaultDate = defaultDate;
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
	// private static final String TAG = "TasksDataSource";
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	private static final String[] allColumns = { "_id", "name", "whereQuery",
			"active", "def_list", "def_date", "sort_by", "sync_state" };
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
		values.put("def_list", ListMirakel.first().getId());
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
		setSyncState(getSyncState() == Network.SYNC_STATE.ADD
				|| getSyncState() == Network.SYNC_STATE.IS_SYNCED ? getSyncState()
				: Network.SYNC_STATE.NEED_SYNC);
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

		if (getSyncState() != Network.SYNC_STATE.ADD) {
			setSyncState(Network.SYNC_STATE.DELETE);
		}else{
			database.delete(TABLE, "_id="+id, null);
			return;
		}
		setActive(false);
		ContentValues values = new ContentValues();
		database.update(TABLE, values, "_id=" + id, null);
	}

	public ContentValues getContentValues() {
		ContentValues cv = new ContentValues();
		cv.put("name", getName());
		cv.put("sort_by", getSortBy());
		cv.put("sync_state", getSyncState());
		cv.put("active", isActive() ? 1 : 0);
		cv.put("whereQuery", getWhereQuery());
		cv.put("def_list", defaultList == null ? null : defaultList.getId());
		cv.put("def_date", defaultDate);
		return cv;
	}

	/**
	 * Get all SpecialLists
	 * 
	 * @return
	 */
	public static List<SpecialList> allSpecial() {
		return allSpecial(false);
	}

	/**
	 * Get all SpecialLists
	 * 
	 * @return
	 */
	public static List<SpecialList> allSpecial(boolean showAll) {
		List<SpecialList> slists = new ArrayList<SpecialList>();
		Cursor c = database.query(TABLE, allColumns, showAll?"":"active=1", null, null,
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
		return firstSpecial();
	}

	/**
	 * Get the first List
	 * 
	 * @return List
	 */
	public static SpecialList firstSpecial() {
		Cursor cursor = database.query(SpecialList.TABLE, allColumns,
				"not sync_state=" + Network.SYNC_STATE.DELETE, null, null,
				null, "_id ASC");
		SpecialList list = null;
		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			list = cursorToSList(cursor);
			cursor.moveToNext();
		} else {
			list = new SpecialList(0, context.getString(R.string.list_all), "",
					true, ListMirakel.first(), null, SORT_BY_OPT,
					Network.SYNC_STATE.NOTHING);

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
		Integer defDate=cursor.getInt(5);
		if(cursor.isNull(5))
			defDate=null;
		SpecialList slist = new SpecialList(cursor.getInt(i++),
				cursor.getString(i++), cursor.getString(i++),
				cursor.getInt(i++) == 1,
				ListMirakel.getList(cursor.getInt(i++)), defDate,
				(short) cursor.getInt(++i), cursor.getInt(++i));
		return slist;
	}

	public static int getSpecialListCount() {
		Cursor c = Mirakel.getReadableDatabase().rawQuery(
				"Select count(_id) from " + TABLE, null);
		c.moveToFirst();
		int r = 0;
		if (c.getCount() > 0) {
			r = c.getInt(0);
		}
		c.close();
		return r;
	}

}
