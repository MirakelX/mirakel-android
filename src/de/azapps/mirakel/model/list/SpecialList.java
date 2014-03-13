package de.azapps.mirakel.model.list;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.MirakelContentProvider;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.task.Task;

public class SpecialList extends ListMirakel {
	private boolean active;
	private String whereQuery;
	private ListMirakel defaultList;
	private Integer defaultDate;

	@Override
	public boolean isSpecialList() {
		return true;
	}

	public boolean isActive() {
		return this.active;
	}

	public void setActive(final boolean active) {
		this.active = active;
	}

	@Override
	public String getWhereQueryForTasks(final boolean forQuery) {
		if (forQuery) {
			String tmpWhere = this.whereQuery;
			final Pattern p = Pattern
					.compile(Task.LIST_ID + " in[(]([^)]*)[)]");
			final Matcher m = p.matcher(this.whereQuery);

			if (m.find()) {
				final String origQuery = m.group(1);
				String newQuery = "";
				final String lists[] = origQuery.split(",");
				String listConditions = "";
				boolean first = true;
				for (final String list : lists) {
					final int listId = Integer.valueOf(list);
					if (listId > 0) {
						if (first) {
							first = false;
						} else {
							newQuery += ",";
						}

						newQuery += list;
						continue;
					}
					listConditions += " OR ("
							+ SpecialList.getSpecialList(-listId)
									.getWhereQueryForTasks(true) + ")";
				}
				if (!listConditions.equals("")) {
					tmpWhere = m.replaceFirst("(" + Task.LIST_ID + " in ("
							+ newQuery + ")");
					tmpWhere += listConditions + ")";
				}
			}
			return tmpWhere;
		}
		return this.whereQuery;
	}

	public void setWhereQuery(final String whereQuery) {
		this.whereQuery = whereQuery;
	}

	public ListMirakel getDefaultList() {
		if (this.defaultList == null) {
			return ListMirakel.first();
		}
		return this.defaultList;
	}

	public void setDefaultList(final ListMirakel defaultList) {
		this.defaultList = defaultList;
	}

	public Integer getDefaultDate() {
		return this.defaultDate;
	}

	public void setDefaultDate(final Integer defaultDate) {
		this.defaultDate = defaultDate;
	}

	SpecialList(final int id, final String name, final String whereQuery,
			final boolean active, final ListMirakel listMirakel,
			final Integer defaultDate, final short sort_by,
			final SYNC_STATE sync_state, final int color, final int lft,
			final int rgt) {

		super(-id, name, sort_by, "", "", sync_state, 0, 0, color,
				AccountMirakel.getLocal());
		this.active = active;
		this.whereQuery = whereQuery;
		this.defaultList = listMirakel;
		this.defaultDate = defaultDate;
		setLft(lft);
		setRgt(rgt);
	}

	/**
	 * Get all Tasks
	 * 
	 * @return
	 */
	@Override
	public List<Task> tasks() {
		return Task.getTasks(this, getSortBy(), false,
				getWhereQueryForTasks(true));
	}

	/**
	 * Get all Tasks
	 * 
	 * @param showDone
	 * @return
	 */
	@Override
	public List<Task> tasks(final boolean showDone) {
		return Task.getTasks(this, getSortBy(), showDone,
				getWhereQueryForTasks(true));
	}

	// Static Methods
	public static final String TABLE = "special_lists";
	// private static final String TAG = "TasksDataSource";
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;
	public static final String WHERE_QUERY = "whereQuery";
	public static final String ACTIVE = "active";
	public static final String DEFAULT_LIST = "def_list";
	public static final String DEFAULT_DUE = "def_date";
	private static final String[] allColumns = { DatabaseHelper.ID,
			DatabaseHelper.NAME, WHERE_QUERY, ACTIVE, DEFAULT_LIST,
			DEFAULT_DUE, SORT_BY, DatabaseHelper.SYNC_STATE_FIELD, COLOR, LFT,
			RGT };

	/**
	 * Initialize the Database and the preferences
	 * 
	 * @param context
	 *            The Application-Context
	 */
	public static void init(final Context context) {
		dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
	}

	/**
	 * Close the Database-Connection
	 */
	public static void close() {
		dbHelper.close();
	}

	public static SpecialList newSpecialList(final String name,
			final String whereQuery, final boolean active, final Context context) {
		final int listId = ListMirakel.safeFirst(context).getId();
		database.beginTransaction();

		final ContentValues values = new ContentValues();
		values.put(DatabaseHelper.NAME, name);
		values.put(WHERE_QUERY, whereQuery);
		values.put(ACTIVE, active);
		values.put(DEFAULT_LIST, listId);
		final long insertId = database.insert(TABLE, null, values);
		final Cursor cursor = database.query(TABLE, allColumns, "_id = "
				+ insertId, null, null, null, null);
		cursor.moveToFirst();
		database.execSQL("update " + TABLE + " SET " + LFT + "=(SELECT MAX("
				+ RGT + ") from " + TABLE + ")+1, " + RGT + "=(SELECT MAX("
				+ RGT + ") from " + TABLE + ")+2 where " + DatabaseHelper.ID
				+ "=" + insertId);
		database.setTransactionSuccessful();
		database.endTransaction();
		final SpecialList newSList = cursorToSList(cursor);
		cursor.close();
		return newSList;
	}

	/**
	 * Update the List in the Database
	 * 
	 * @param list
	 *            The List
	 */
	@Override
	public void save() {
		database.beginTransaction();
		setSyncState(getSyncState() == SYNC_STATE.ADD
				|| getSyncState() == SYNC_STATE.IS_SYNCED ? getSyncState()
				: SYNC_STATE.NEED_SYNC);
		final ContentValues values = getContentValues();
		database.update(TABLE, values,
				DatabaseHelper.ID + " = " + Math.abs(getId()), null);
		database.setTransactionSuccessful();
		database.endTransaction();
	}

	/**
	 * Delete a List from the Database
	 * 
	 * @param list
	 */
	@Override
	public void destroy() {
		database.beginTransaction();
		final long id = Math.abs(getId());

		if (getSyncState() != SYNC_STATE.ADD) {
			setSyncState(SYNC_STATE.DELETE);
			setActive(false);
			final ContentValues values = new ContentValues();
			values.put(DatabaseHelper.SYNC_STATE_FIELD, getSyncState().toInt());
			database.update(TABLE, values, DatabaseHelper.ID + "=" + id, null);
		} else {
			database.delete(TABLE, DatabaseHelper.ID + "=" + id, null);
		}
		database.rawQuery("UPDATE " + TABLE + " SET " + LFT + "=" + LFT
				+ "-2 WHERE " + LFT + ">" + getLft() + "; UPDATE " + TABLE
				+ " SET " + RGT + "=" + RGT + "-2 WHERE " + RGT + ">"
				+ getRgt() + ";", null);
		database.setTransactionSuccessful();
		database.endTransaction();

	}

	@Override
	public ContentValues getContentValues() {
		final ContentValues cv = new ContentValues();
		cv.put(DatabaseHelper.NAME, getName());
		cv.put(SORT_BY, getSortBy());
		cv.put(DatabaseHelper.SYNC_STATE_FIELD, getSyncState().toInt());
		cv.put(ACTIVE, isActive() ? 1 : 0);
		cv.put(WHERE_QUERY, getWhereQueryForTasks(false));
		cv.put(DEFAULT_LIST,
				this.defaultList == null ? null : this.defaultList.getId());
		cv.put(DEFAULT_DUE, this.defaultDate);
		cv.put(COLOR, getColor());
		cv.put(LFT, getLft());
		cv.put(RGT, getRgt());
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
	public static List<SpecialList> allSpecial(final boolean showAll) {
		final List<SpecialList> slists = new ArrayList<SpecialList>();
		final Cursor c = database.query(TABLE, allColumns, showAll ? ""
				: ACTIVE + "=1", null, null, null, LFT + " ASC");
		c.moveToFirst();
		while (!c.isAfterLast()) {
			slists.add(cursorToSList(c));
			c.moveToNext();
		}
		c.close();
		return slists;
	}

	/**
	 * Get a List by id selectionArgs
	 * 
	 * @param listId
	 *            List–ID
	 * @return List
	 */
	public static SpecialList getSpecialList(final int listId) {
		final Cursor cursor = database.query(SpecialList.TABLE, allColumns,
				DatabaseHelper.ID + "=" + listId, null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			final SpecialList t = cursorToSList(cursor);
			cursor.close();
			return t;
		}
		cursor.close();
		return firstSpecial();
	}

	/**
	 * Get the first List
	 * 
	 * @return List
	 */
	public static SpecialList firstSpecial() {
		final Cursor cursor = database.query(SpecialList.TABLE, allColumns,
				"not " + DatabaseHelper.SYNC_STATE_FIELD + "="
						+ SYNC_STATE.DELETE, null, null, null, LFT + " ASC");
		SpecialList list = null;
		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			list = cursorToSList(cursor);
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

	public static SpecialList firstSpecialSafe(final Context ctx) {
		SpecialList s = SpecialList.firstSpecial();
		if (s == null) {
			s = SpecialList.newSpecialList(ctx.getString(R.string.list_all),
					"", true, ctx);
			if (ListMirakel.count() == 0) {
				ListMirakel.safeFirst(ctx);
			}
			s.save(false);
		}
		return s;
	}

	/**
	 * Create a List from a Cursor
	 * 
	 * @param cursor
	 * @return
	 */
	private static SpecialList cursorToSList(final Cursor cursor) {
		int i = 0;
		Integer defDate = cursor.getInt(5);
		if (cursor.isNull(5)) {
			defDate = null;
		}
		final SpecialList slist = new SpecialList(cursor.getInt(i++),
				cursor.getString(i++), cursor.getString(i++),
				cursor.getInt(i++) == 1,
				ListMirakel.getList(cursor.getInt(i++)), defDate,
				(short) cursor.getInt(++i), SYNC_STATE.parseInt(cursor
						.getInt(++i)), cursor.getInt(++i), cursor.getInt(++i),
				cursor.getInt(++i));
		return slist;
	}

	public static int getSpecialListCount() {
		final Cursor c = MirakelContentProvider.getReadableDatabase().rawQuery(
				"Select count(" + DatabaseHelper.ID + ") from " + TABLE, null);
		c.moveToFirst();
		int r = 0;
		if (c.getCount() > 0) {
			r = c.getInt(0);
		}
		c.close();
		return r;
	}

}
