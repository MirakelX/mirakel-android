/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
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
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.UndoHistory;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

/**
 * @author az
 */
public class ListMirakel extends ListBase {
    private static final String[] allColumns = { DatabaseHelper.ID,
                                                 DatabaseHelper.NAME, SORT_BY, DatabaseHelper.CREATED_AT,
                                                 DatabaseHelper.UPDATED_AT, DatabaseHelper.SYNC_STATE_FIELD, LFT,
                                                 RGT, COLOR, ACCOUNT_ID
                                               };
    private static Context context;

    private static SQLiteDatabase database;

    // private static final String TAG = "ListMirakel";

    private static DatabaseHelper dbHelper;

    public static final short SORT_BY_OPT = 0, SORT_BY_DUE = 1,
                              SORT_BY_PRIO = 2, SORT_BY_ID = 3, SORT_BY_REVERT_DEFAULT = 4;

    public static final String TABLE = "lists";

    private static final String TAG = "ListMirakel";

    /**
     * Get all Lists in the Database
     *
     * @return List of Lists
     */
    public static List<ListMirakel> all() {
        return all(true);
    }

    /**
     * CALL THIS ONLY FROM DBHelper
     *
     * @param db
     */
    public static void setDB(final SQLiteDatabase db) {
        database = db;
    }

    public static List<ListMirakel> all(final boolean withSpecial) {
        final List<ListMirakel> lists = new ArrayList<ListMirakel>();
        if (withSpecial) {
            final List<SpecialList> slists = SpecialList.allSpecial();
            for (final SpecialList slist : slists) {
                lists.add(slist);
            }
        }
        final Cursor cursor = database.rawQuery("  SELECT n.*, "
                                                + "COUNT(*)-1 AS level " + "FROM " + ListMirakel.TABLE
                                                + " AS n, " + ListMirakel.TABLE + " p " + "WHERE n." + LFT
                                                + " BETWEEN p." + LFT + " AND p." + RGT + " " + " and not n."
                                                + DatabaseHelper.SYNC_STATE_FIELD + "=" + SYNC_STATE.DELETE
                                                + " GROUP BY n." + LFT + " " + "ORDER BY n." + LFT + ";", null);
        // query(ListMirakel.TABLE, allColumns,
        // "not sync_state=" + SYNC_STATE.DELETE, null, "lft",
        // null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            final ListMirakel list = cursorToList(cursor);
            lists.add(list);
            cursor.moveToNext();
        }
        cursor.close();
        return lists;
    }

    public static List<ListMirakel> byAccount(final AccountMirakel a) {
        final Cursor c = database.query(ListMirakel.TABLE, allColumns,
                                        ACCOUNT_ID + "=" + a.getId(), null, null, null, null);
        c.moveToFirst();
        final List<ListMirakel> lists = cursorToListList(c);
        c.close();
        return lists;
    }

    /**
     * Get Lists by a sync state
     *
     * @param state
     * @see Mirakel.SYNC_STATE*
     * @return
     */
    public static List<ListMirakel> bySyncState(final SYNC_STATE state) {
        final Cursor c = database.query(ListMirakel.TABLE, allColumns,
                                        DatabaseHelper.SYNC_STATE_FIELD + "=" + state, null, null,
                                        null, null);
        c.moveToFirst();
        final List<ListMirakel> lists = cursorToListList(c);
        c.close();
        return lists;
    }

    /**
     * Close the Database–Connection
     */
    public static void close() {
        dbHelper.close();
    }

    public static int count() {
        final Cursor c = database.rawQuery("Select count(" + DatabaseHelper.ID
                                           + ") from " + TABLE, null);
        c.moveToFirst();
        final int count = c.getInt(0);
        c.close();
        return count;
    }

    /**
     * Create a List from a Cursor
     *
     * @param cursor
     * @return
     */
    private static ListMirakel cursorToList(final Cursor cursor) {
        int i = 0;
        final int id = cursor.getInt(i++);
        final ListMirakel list = new ListMirakel(id, cursor.getString(i++),
                cursor.getShort(i++), cursor.getString(i++),
                cursor.getString(i++), SYNC_STATE.parseInt(cursor.getInt(i++)),
                cursor.getInt(i++), cursor.getInt(i++), cursor.getInt(i++),
                cursor.getInt(i++));
        return list;
    }

    private static List<ListMirakel> cursorToListList(final Cursor c) {
        final List<ListMirakel> lists = new ArrayList<ListMirakel>();
        while (!c.isAfterLast()) {
            lists.add(cursorToList(c));
            c.moveToNext();
        }
        return lists;
    }

    public static ListMirakel findByName(final String name) {
        return findByName(name, null);
    }

    public static ListMirakel findByName(final String name,
                                         final AccountMirakel account) {
        final Cursor cursor = database.query(ListMirakel.TABLE, allColumns,
                                             DatabaseHelper.NAME
                                             + "='"
                                             + name
                                             + "'"
                                             + (account == null ? "" : " AND " + ACCOUNT_ID + "="
                                                + account.getId()), null, null, null, null);
        cursor.moveToFirst();
        if (cursor.getCount() != 0) {
            final ListMirakel t = cursorToList(cursor);
            cursor.close();
            return t;
        }
        cursor.close();
        return null;
    }

    // Static Methods

    /**
     * Get the first List
     *
     * @return List
     */
    public static ListMirakel first() {
        final Cursor cursor = database.query(ListMirakel.TABLE, allColumns,
                                             "not " + DatabaseHelper.SYNC_STATE_FIELD + "="
                                             + SYNC_STATE.DELETE, null, null, null, LFT + " ASC");
        ListMirakel list = null;
        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            list = cursorToList(cursor);
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    public static ListMirakel getInboxList(final AccountMirakel account) {
        final Cursor c = database.query(TABLE, allColumns, DatabaseHelper.NAME
                                        + "='" + context.getString(R.string.inbox) + "' and "
                                        + ACCOUNT_ID + "=" + account.getId(), null, null, null, null);
        c.moveToFirst();
        if (c.getCount() > 0) {
            Log.d(TAG, "get old eingang");
            final ListMirakel l = cursorToList(c);
            c.close();
            return l;
        }
        c.close();
        return newList(context.getString(R.string.inbox), SORT_BY_OPT, account);
    }

    public static ListMirakel get(final int listId) {
        if (listId < 0) {
            return SpecialList.get(-listId);
        }
        final Cursor cursor = database
                              .query(ListMirakel.TABLE, allColumns, DatabaseHelper.ID + "='"
                                     + listId + "'", null, null, null, null);
        if (cursor.moveToFirst()) {
            final ListMirakel t = cursorToList(cursor);
            cursor.close();
            return t;
        }
        cursor.close();
        return null;
    }

    public static void setDefaultAccount(final AccountMirakel account) {
        final ContentValues v = new ContentValues();
        v.put(ACCOUNT_ID, account.getId());
        database.beginTransaction();
        database.update(TABLE, v, null, null);
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    public static List<ListMirakel> getListsForAccount(
        final AccountMirakel account) {
        if (account == null || !account.isEnabled()) {
            return new ArrayList<ListMirakel>();
        }
        final Cursor c = database.query(TABLE, allColumns, "NOT "
                                        + DatabaseHelper.SYNC_STATE_FIELD + "=" + SYNC_STATE.DELETE
                                        + " and " + ACCOUNT_ID + "=" + account.getId(), null, null,
                                        null, null);
        c.moveToFirst();
        final List<ListMirakel> list = cursorToListList(c);
        c.close();
        return list;
    }

    public static ListMirakel getSafeDefaultList() {
        final ListMirakel list = MirakelModelPreferences
                                 .getImportDefaultList(true);
        return list;
    }

    /**
     * Initialize the Database and the preferences
     *
     * @param context
     *            The Application-Context
     */
    public static void init(final Context ctx) {
        ListMirakel.context = ctx;
        dbHelper = new DatabaseHelper(ctx);
        database = dbHelper.getWritableDatabase();
    }

    /**
     * Get the last List
     *
     * @return List
     */
    public static ListMirakel last() {
        final Cursor cursor = database.query(ListMirakel.TABLE, allColumns,
                                             "not " + DatabaseHelper.SYNC_STATE_FIELD + "="
                                             + SYNC_STATE.DELETE, null, null, null,
                                             DatabaseHelper.ID + " DESC");
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
     * Create and insert a new List
     *
     * @param name
     * @return
     */
    public static ListMirakel newList(final String name) {
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
    public static ListMirakel newList(final String name, final int sort_by) {
        return newList(name, sort_by,
                       MirakelModelPreferences.getDefaultAccount());
    }

    public static ListMirakel newList(final String name, final int sort_by,
                                      final AccountMirakel account) {
        final ContentValues values = new ContentValues();
        values.put(DatabaseHelper.NAME, name);
        values.put(ACCOUNT_ID, account.getId());
        values.put(SORT_BY, sort_by);
        values.put(DatabaseHelper.SYNC_STATE_FIELD, SYNC_STATE.ADD.toInt());
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
        final Cursor cursor = database.query(ListMirakel.TABLE, allColumns,
                                             DatabaseHelper.ID + " = " + insertId, null, null, null, null);
        cursor.moveToFirst();
        final ListMirakel newList = cursorToList(cursor);
        cursor.close();
        UndoHistory.logCreate(newList, context);
        return newList;
    }

    public static ListMirakel parseJson(final JsonObject el) {
        ListMirakel t = null;
        final JsonElement id = el.get("id");
        if (id != null) {
            // use old List from db if existing
            t = ListMirakel.get(id.getAsInt());
        }
        if (t == null) {
            t = new ListMirakel();
        }
        JsonElement j = el.get("name");
        if (j != null) {
            t.setName(j.getAsString());
        }
        j = el.get("lft");
        if (j != null) {
            t.setLft(j.getAsInt());
        }
        j = el.get("rgt");
        if (j != null) {
            t.setRgt(j.getAsInt());
        }
        j = el.get("lft");
        if (j != null) {
            t.setLft(j.getAsInt());
        }
        j = el.get("updated_at");
        if (j != null) {
            t.setUpdatedAt(j.getAsString().replace(":", ""));
        }
        j = el.get("sort_by");
        if (j != null) {
            t.setSortBy(j.getAsInt());
        }
        return t;
    }

    public static ListMirakel safeFirst(final Context ctx) {
        ListMirakel s = first();
        if (s == null) {
            s = ListMirakel.newList(ctx.getString(R.string.inbox));
        }
        return s;
    }

    public static ListMirakel safeGet(final int listId) {
        ListMirakel l = get(listId);
        if (l == null) {
            l = safeFirst(context);
        }
        return l;
    }

    private ListMirakel() {
        super();
    }

    ListMirakel(final int id, final String name) {
        super(id, name);
    }

    public ListMirakel(final int id, final String name, final short sort_by,
                       final String created_at, final String updated_at,
                       final SYNC_STATE sync_state, final int lft, final int rgt,
                       final int color, final AccountMirakel account) {
        super(id, name, sort_by, created_at, updated_at, sync_state, lft, rgt,
              color, account);
    }

    protected ListMirakel(final int id, final String name, final short sort_by,
                          final String created_at, final String updated_at,
                          final SYNC_STATE sync_state, final int lft, final int rgt,
                          final int color, final int account) {
        super(id, name, sort_by, created_at, updated_at, sync_state, lft, rgt,
              color, account);
    }

    /**
     * Count the tasks of that list
     *
     * @return
     */
    public int countTasks() {
        String where;
        if (getId() < 0) {
            where = ((SpecialList) this).getWhereQueryForTasks();
        } else {
            where = Task.LIST_ID + " = " + getId();
        }
        if (where.length() != 0) {
            where += " AND ";
        }
        where += Task.DONE + " =0 AND " + Task.BASIC_FILTER_DISPLAY_TASKS;
        final Cursor c = database.query(Task.TABLE,
                                        new String[] { "count(*)" }, where, null, null, null, null);
        c.moveToFirst();
        if (c.getCount() > 0) {
            final int n = c.getInt(0);
            c.close();
            return n;
        }
        c.close();
        return 0;
    }

    /**
     * Delete a List from the Database
     *
     * @param list
     */
    public void destroy() {
        destroy(false);
    }

    public void destroy(final boolean force) {
        if (!force) {
            UndoHistory.updateLog(this, context);
        }
        final long id = getId();
        if (id <= 0) {
            return;
        }
        database.beginTransaction();
        try {
            if (getSyncState() == SYNC_STATE.ADD || force) {
                database.delete(Task.TABLE, Task.LIST_ID + " = " + id, null);
                database.delete(ListMirakel.TABLE, DatabaseHelper.ID + " = "
                                + id, null);
            } else {
                final ContentValues values = new ContentValues();
                values.put(DatabaseHelper.SYNC_STATE_FIELD,
                           SYNC_STATE.DELETE.toInt());
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
        } catch (final Exception e) {
            Log.wtf(TAG, "cannot remove List", e);
        } finally {
            database.endTransaction();
        }
    }

    public Task getFirstTask() {
        final Cursor c = database.query(Task.TABLE, Task.allColumns,
                                        getWhereQueryForTasks() + " AND "
                                        + Task.BASIC_FILTER_DISPLAY_TASKS, null, null, null,
                                        Task.getSorting(getSortBy()), "1");
        Task t = null;
        if (c.getCount() > 0) {
            c.moveToFirst();
            t = Task.cursorToTask(c);
        }
        c.close();
        return t;
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

    public void save(final boolean log) {
        final SharedPreferences.Editor editor = MirakelPreferences.getEditor();
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
            if (log) {
                UndoHistory.updateLog(ListMirakel.get(getId()), context);
            }
            database.update(ListMirakel.TABLE, values, DatabaseHelper.ID
                            + " = " + getId(), null);
            values = new ContentValues();
            values.put(DatabaseHelper.UPDATED_AT,
                       new GregorianCalendar().getTimeInMillis() / 1000);
            values.put(DatabaseHelper.SYNC_STATE_FIELD,
                       SYNC_STATE.NEED_SYNC.toInt());
            database.update(Task.TABLE, values, Task.LIST_ID + "=?",
                            new String[] { getId() + "" });
            database.setTransactionSuccessful();
            database.endTransaction();
        }
        editor.commit();
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
    public List<Task> tasks(final boolean showDone) {
        return Task.getTasks(this, getSortBy(), showDone);
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

    public String getWhereQueryForTasks() {
        return Task.LIST_ID
               + "="
               + getId()
               + " AND " + Task.BASIC_FILTER_DISPLAY_TASKS
               + (MirakelCommonPreferences.showDoneMain() ? "" : " AND NOT "
                  + Task.DONE + "=1");
    }
}
