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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
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

    private static final Uri URI = MirakelInternalContentProvider.SPECIAL_LISTS_URI;

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

    @Override
    protected Uri getUri() {
        return URI;
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

    SpecialList(final long id, final String name,
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
        this.isSpecial = true;
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
    public static final String WHERE_QUERY = "whereQuery";
    public static final String ACTIVE = "active";
    public static final String DEFAULT_LIST = "def_list";
    public static final String DEFAULT_DUE = "def_date";
    private static final String[] allColumns = { ModelBase.ID,
                                                 ModelBase.NAME, WHERE_QUERY, ACTIVE, DEFAULT_LIST,
                                                 DEFAULT_DUE, SORT_BY, DatabaseHelper.SYNC_STATE_FIELD, COLOR, LFT,
                                                 RGT
                                               };
    private static final String TAG = "SpecialList";

    private SpecialList create() {
        final long listId = ListMirakel.safeFirst(context).getId();
        MirakelInternalContentProvider.withTransaction(new
        MirakelInternalContentProvider.DBTransaction() {
            @Override
            public void exec() {
                final ContentValues values = new ContentValues();
                values.put(ModelBase.NAME, getName());
                values.put(WHERE_QUERY, serializeWhere(getWhere()));
                values.put(ACTIVE, active);
                values.put(DEFAULT_LIST, listId);
                setId(insert(URI, values));
                final Cursor c = query(URI, new String[] {"MAX(" + RGT + ")"}, null, null, null);
                c.moveToFirst();
                int maxRGT = c.getInt(0);
                ContentValues cv = new ContentValues();
                cv.put(LFT, maxRGT + 1);
                cv.put(RGT, maxRGT + 2);
                update(URI, cv, ModelBase.ID + "=" + getId(), null);
            }
        });
        final Cursor cursor = query(URI, allColumns, "_id = "
                                    + getId(), null, null);
        cursor.moveToFirst();
        final SpecialList newSList = cursorToSList(cursor);
        cursor.close();
        return newSList;
    }



    public static SpecialList newSpecialList(final String name,
            final Map<String, SpecialListsBaseProperty> whereQuery,
            final boolean active) {
        final SpecialList s = new SpecialList(0, name, whereQuery, active, null, null, (short)0,
                                              SYNC_STATE.ADD, 0,
                                              0, 0);
        return s.create();
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
        if (!"".equals(ret)) {
            ret += " AND ";
        }
        ret += Task.BASIC_FILTER_DISPLAY_TASKS;
        return ret;
    }

    @Override
    public void save(final boolean log) {
        save();
    }

    /**
     * Update the List in the Database
     */
    @Override
    public void save() {
        setSyncState(getSyncState() == SYNC_STATE.ADD ||
                     getSyncState() == SYNC_STATE.IS_SYNCED ? getSyncState() : SYNC_STATE.NEED_SYNC);
        update(URI, getContentValues(),
               ModelBase.ID + " = " + Math.abs(getId()), null);
    }

    /**
     * Delete a List from the Database
     */
    @Override
    public void destroy() {
        MirakelInternalContentProvider.withTransaction(new MirakelInternalContentProvider.DBTransaction() {
            @Override
            public void exec() {
                final long id = Math.abs(getId());
                if (getSyncState() != SYNC_STATE.ADD) {
                    setSyncState(SYNC_STATE.DELETE);
                    setActive(false);
                    final ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.SYNC_STATE_FIELD, getSyncState().toInt());
                    update(URI, values, ModelBase.ID + "=" + id, null);
                } else {
                    delete(URI, ModelBase.ID + "=" + id, null);
                }
                final ContentValues cv = new ContentValues();
                cv.put("TABLE", TABLE);
                update(MirakelInternalContentProvider.UPDATE_LIST_ORDER_URI, cv, LFT + ">" + getLft(), null);
            }
        });
    }

    @Override
    public ContentValues getContentValues() {
        final ContentValues cv = new ContentValues();
        cv.put(ModelBase.NAME, getName());
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
        final Cursor c = query(URI, allColumns, showAll ? ""
                               : ACTIVE + "=1", null, LFT + " ASC");
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
    public static SpecialList get(final long listId) {
        final Cursor cursor = query(URI, allColumns,
                                    ModelBase.ID + "=" + listId, null, null);
        if (cursor.moveToFirst()) {
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
        final Cursor cursor = query(URI, allColumns,
                                    "not " + DatabaseHelper.SYNC_STATE_FIELD + "="
                                    + SYNC_STATE.DELETE, null, LFT + " ASC"
                                   );
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
                                           new HashMap<String, SpecialListsBaseProperty>(), true);
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
     * @param c
     * @return
     */
    private static SpecialList cursorToSList(final Cursor c) {
        int defDateCol = c.getColumnIndex(DEFAULT_DUE);
        return new SpecialList(c.getLong(c.getColumnIndex(ID)), c.getString(c.getColumnIndex(NAME)),
                               deserializeWhere(c.getString(c.getColumnIndex(WHERE_QUERY))),
                               c.getShort(c.getColumnIndex(ACTIVE)) == 1,
                               ListMirakel.get(c.getInt(c.getColumnIndex(DEFAULT_LIST))),
                               c.isNull(defDateCol) ? null : c.getInt(defDateCol),
                               c.getShort(c.getColumnIndex(SORT_BY)),
                               SYNC_STATE.parseInt(c.getInt(c.getColumnIndex(DatabaseHelper.SYNC_STATE_FIELD))),
                               c.getInt(c.getColumnIndex(COLOR)), c.getInt(c.getColumnIndex(LFT)),
                               c.getInt(c.getColumnIndex(RGT)));
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
                SpecialListsContentProperty.class)
        )
        .registerTypeAdapter(
            SpecialListsNameProperty.class,
            new StringDeserializer<SpecialListsNameProperty>(
                SpecialListsNameProperty.class)
        )
        .registerTypeAdapter(SpecialListsProgressProperty.class,
                             new ProgressDeserializer()).create();
        for (final Entry<String, JsonElement> entry : all.entrySet()) {
            final String key = entry.getKey();
            Class<? extends SpecialListsBaseProperty> className;
            switch (key) {
            case Task.LIST_ID:
                className = SpecialListsListProperty.class;
                break;
            case ModelBase.NAME:
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
                Log.v(TAG, "implement this?");
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
            where = ACTIVE + "=1";
        }
        final Cursor c = query(URI, new String[] {"count(*)"}, where, null, null);
        int r = 0;
        if (c.moveToFirst()) {
            r = c.getInt(0);
        }
        c.close();
        return r;
    }

}
