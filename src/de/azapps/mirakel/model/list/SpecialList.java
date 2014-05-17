package de.azapps.mirakel.model.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.MirakelContentProvider;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.meta.DueDeserializer;
import de.azapps.mirakel.model.list.meta.ProgressDeserializer;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsContentProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDoneProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDueProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsFileProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsListProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsNameProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsPriorityProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsProgressProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsReminderProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsSubtaskProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsTagProperty;
import de.azapps.mirakel.model.list.meta.StringDeserializer;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class SpecialList extends ListMirakel {
	private boolean active;
	private ListMirakel defaultList;
	private Integer defaultDate;
	private Map<String, SpecialListsBaseProperty> where;

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
	public String getWhereQueryForTasks() {
		return packWhere(this.where);
	}

	public Map<String, SpecialListsBaseProperty> getWhere() {
		return this.where;
	}

	public void setWhere(final Map<String, SpecialListsBaseProperty> where) {
		this.where = where;
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

	SpecialList(final int id, final String name,
			final Map<String, SpecialListsBaseProperty> whereQuery,
			final boolean active, final ListMirakel listMirakel,
			final Integer defaultDate, final short sort_by,
			final SYNC_STATE sync_state, final int color, final int lft,
			final int rgt) {

		super(-id, name, sort_by, "", "", sync_state, 0, 0, color,
				AccountMirakel.getLocal());
		this.active = active;
		this.where = whereQuery;
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
		return Task.getTasks(this, getSortBy(), false, getWhereQueryForTasks());
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
				getWhereQueryForTasks());
	}

	// Static Methods
	public static final String TABLE = "special_lists";
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
	private static final String TAG = "SpecialList";

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
			final Map<String, SpecialListsBaseProperty> whereQuery,
			final boolean active, final Context context) {
		final int listId = ListMirakel.safeFirst(context).getId();
		database.beginTransaction();

		final ContentValues values = new ContentValues();
		values.put(DatabaseHelper.NAME, name);
		values.put(WHERE_QUERY, serializeWhere(whereQuery));
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

	public static String serializeWhere(
			final Map<String, SpecialListsBaseProperty> whereQuery) {
		String ret = "{";
		boolean first = true;
		for (final Entry<String, SpecialListsBaseProperty> w : whereQuery
				.entrySet()) {
			ret += (first ? "" : " , ") + w.getValue().serialize();
			if (first) {
				first = false;
			}
		}
		Log.i(TAG, ret);
		return ret + "}";
	}

	private static String packWhere(
			final Map<String, SpecialListsBaseProperty> where) {
		String ret = "";
		boolean first = true;
		for (final Entry<String, SpecialListsBaseProperty> w : where.entrySet()) {
			ret += (first ? "" : " AND ") + "(" + w.getValue().getWhereQuery()
					+ ")";
			if (first) {
				first = false;
			}
		}
		return ret;
	}

	@Override
	public void save(final boolean log) {
		save();
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
		cv.put(WHERE_QUERY, serializeWhere(getWhere()));
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
	public static SpecialList get(final int listId) {
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
					new HashMap<String, SpecialListsBaseProperty>(), true, ctx);
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
				cursor.getString(i++), deserializeWhere(cursor.getString(i++)),
				cursor.getInt(i++) == 1, ListMirakel.get(cursor.getInt(i++)),
				defDate, (short) cursor.getInt(++i), SYNC_STATE.parseInt(cursor
						.getInt(++i)), cursor.getInt(++i), cursor.getInt(++i),
				cursor.getInt(++i));
		return slist;
	}

	private static Map<String, SpecialListsBaseProperty> deserializeWhere(
			final String whereQuery) {
		final Map<String, SpecialListsBaseProperty> ret = new HashMap<String, SpecialListsBaseProperty>();
		final JsonObject all = new JsonParser().parse(whereQuery)
				.getAsJsonObject();
		final Gson gson = new GsonBuilder()
				.registerTypeAdapter(SpecialListsDueProperty.class,
						new DueDeserializer())
				.registerTypeAdapter(
						SpecialListsContentProperty.class,
						new StringDeserializer<SpecialListsContentProperty>(
								SpecialListsContentProperty.class))
				.registerTypeAdapter(
						SpecialListsNameProperty.class,
						new StringDeserializer<SpecialListsNameProperty>(
								SpecialListsNameProperty.class))
				.registerTypeAdapter(SpecialListsProgressProperty.class,
						new ProgressDeserializer()).create();
		for (final Entry<String, JsonElement> entry : all.entrySet()) {
			final String key = entry.getKey();
			Class<? extends SpecialListsBaseProperty> className;
			switch (key) {
			case Task.LIST_ID:
				className = SpecialListsListProperty.class;
				break;
			case DatabaseHelper.NAME:
				className = SpecialListsNameProperty.class;
				break;
			case Task.PRIORITY:
				className = SpecialListsPriorityProperty.class;
				break;
			case Task.DONE:
				className = SpecialListsDoneProperty.class;
				break;
			case Task.DUE:
				className = SpecialListsDueProperty.class;
				break;
			case Task.CONTENT:
				className = SpecialListsContentProperty.class;
				break;
			case Task.REMINDER:
				className = SpecialListsReminderProperty.class;
				break;
			case Task.PROGRESS:
				className = SpecialListsProgressProperty.class;
				break;
			case Task.SUBTASK_TABLE:
				className = SpecialListsSubtaskProperty.class;
				break;
			case FileMirakel.TABLE:
				className = SpecialListsFileProperty.class;
				break;
			case Tag.TABLE:
				className = SpecialListsTagProperty.class;
				break;
			default:
				Log.wtf(TAG, "unkown key: " + key);
				return new HashMap<String, SpecialListsBaseProperty>();
			}
			final SpecialListsBaseProperty prop = gson.fromJson(
					entry.getValue(), className);
			ret.put(key, prop);
		}
		return ret;
	}

	public static int getSpecialListCount(final boolean respectEnable) {
		String where = "";
		if (respectEnable) {
			where = " WHERE " + ACTIVE + "=1";
		}
		final Cursor c = MirakelContentProvider.getReadableDatabase()
				.rawQuery(
						"Select count(" + DatabaseHelper.ID + ") from " + TABLE
								+ where, null);
		c.moveToFirst();
		int r = 0;
		if (c.getCount() > 0) {
			r = c.getInt(0);
		}
		c.close();
		return r;
	}

}
