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
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;

import com.google.common.base.Function;
import com.google.common.base.Optional;
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
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Sorting;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static de.azapps.tools.OptionalUtils.*;

public class SpecialList extends ListMirakel {
    private boolean active;
    private Optional<ListMirakel> defaultList = absent();
    private Integer defaultDate;
    private Map<String, SpecialListsBaseProperty> where;
    private String whereString;

    public static final Uri URI = MirakelInternalContentProvider.SPECIAL_LISTS_URI;

    public boolean isActive() {
        return this.active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    @Override
    public MirakelQueryBuilder getWhereQueryForTasks() {
        return packWhere(getWhere());
    }

    @Override
    protected Uri getUri() {
        return URI;
    }

    @Override
    public long getId() {
        return -1 * super.getId();
    }

    public Map<String, SpecialListsBaseProperty> getWhere() {
        if (where == null) {
            where = deserializeWhere(whereString);
        }
        return this.where;
    }

    public void setWhere(final Map<String, SpecialListsBaseProperty> where) {
        this.whereString = serializeWhere(where);
        this.where = where;
    }

    public ListMirakel getDefaultList() {
        if (!this.defaultList.isPresent()) {
            return ListMirakel.first();
        } else {
            return this.defaultList.get();
        }
    }

    public static List<SpecialList> cursorToSpecialLists(final Cursor c) {
        List<SpecialList> ret = new ArrayList<>();
        if (c.moveToFirst()) {
            do {
                ret.add(new SpecialList(c));
            } while (c.moveToNext());
        }
        c.close();
        return ret;
    }

    public void setDefaultList(final Optional<ListMirakel> defaultList) {
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
                final boolean active, final Optional<ListMirakel> defaultList,
                final Integer defaultDate, final SORT_BY sort_by,
                final SYNC_STATE sync_state, final int color, final int lft,
                final int rgt) {
        super(-id, name, sort_by, "", "", sync_state, 0, 0, color,
              AccountMirakel.getLocal());
        this.active = active;
        this.where = whereQuery;
        this.whereString = serializeWhere(whereQuery);
        this.defaultList = defaultList;
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
        return tasks(false);
    }

    /**
     * Get all Tasks
     *
     * @param showDone
     * @return
     */
    @Override
    public List<Task> tasks(final boolean showDone) {
        if (showDone) {
            return addSortBy(getWhereQueryForTasks()).getList(Task.class);
        } else {
            return addSortBy(getWhereQueryForTasks().and(Task.DONE, Operation.EQ,
                             false)).getList(Task.class);
        }
    }

    // Static Methods
    public static final String TABLE = "special_lists";
    public static final String WHERE_QUERY = "whereQuery";
    public static final String ACTIVE = "active";
    public static final String DEFAULT_LIST = "def_list";
    public static final String DEFAULT_DUE = "def_date";
    public static final String[] allColumns = {ModelBase.ID,
                                               ModelBase.NAME, WHERE_QUERY, ACTIVE, DEFAULT_LIST,
                                               DEFAULT_DUE, SORT_BY_FIELD, DatabaseHelper.SYNC_STATE_FIELD, COLOR, LFT,
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
                final Cursor c = new MirakelQueryBuilder(context).select("MAX(" + RGT + ")").query(URI);
                c.moveToFirst();
                int maxRGT = c.getInt(0);
                ContentValues cv = new ContentValues();
                cv.put(LFT, maxRGT + 1);
                cv.put(RGT, maxRGT + 2);
                update(URI, cv, ModelBase.ID + "=" + getId(), null);
            }
        });
        return getSpecial(getId()).get();
    }


    public static SpecialList newSpecialList(final String name,
            final Map<String, SpecialListsBaseProperty> whereQuery,
            final boolean active) {
        final SpecialList s = new SpecialList(0, name, whereQuery, active, null, null, SORT_BY.OPT,
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

    private static MirakelQueryBuilder packWhere(
        final Map<String, SpecialListsBaseProperty> where) {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(context);
        for (final Entry<String, SpecialListsBaseProperty> w : where.entrySet()) {
            qb.and(w.getValue().getWhereQuery(context));
            Log.w(TAG, qb.getSelection());
        }
        return Task.addBasicFiler(qb);
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
        cv.put(SORT_BY_FIELD, getSortBy().getShort());
        cv.put(DatabaseHelper.SYNC_STATE_FIELD, getSyncState().toInt());
        cv.put(ACTIVE, isActive() ? 1 : 0);
        cv.put(WHERE_QUERY, serializeWhere(getWhere()));
        cv.put(DEFAULT_LIST, transformOrNull(this.defaultList, new Function<ListMirakel, Long>() {
            @Override
            public Long apply(ListMirakel input) {
                return input.getId();
            }
        }));
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
        MirakelQueryBuilder qb = new MirakelQueryBuilder(context).and(ACTIVE, Operation.EQ,
                true).sort(LFT, Sorting.ASC);
        if (!showAll) {
            qb.and(ACTIVE, Operation.EQ, true);
        }
        return qb.getList(SpecialList.class);
    }

    /**
     * Get a List by id
     *
     * @param listId List–ID
     * @return List
     */
    public static Optional<SpecialList> getSpecial(final long listId) {
        SpecialList l = new MirakelQueryBuilder(context).get(SpecialList.class, Math.abs(listId));
        return fromNullable(l);
    }

    /**
     * Get the first List
     *
     * @return List
     */
    public static Optional<SpecialList> firstSpecial() {
        return fromNullable(new MirakelQueryBuilder(context).and(DatabaseHelper.SYNC_STATE_FIELD,
                            Operation.NOT_EQ, SYNC_STATE.DELETE.toInt()).sort(LFT,
                                    Sorting.ASC).get(SpecialList.class));
    }

    public static SpecialList firstSpecialSafe() {
        Optional<SpecialList> s = SpecialList.firstSpecial();
        if (!s.isPresent()) {
            s = fromNullable(SpecialList.newSpecialList(context.getString(R.string.list_all),
                             new HashMap<String, SpecialListsBaseProperty>(), true));
            if (ListMirakel.count() == 0) {
                ListMirakel.safeFirst(context);
            }
            s.get().save(false);
        }
        return s.get();
    }

    /**
     * Create a List from a Cursor
     *
     * @param c
     * @return
     */
    public SpecialList(final Cursor c) {
        super(c.getLong(c.getColumnIndex(ID)), c.getString(c.getColumnIndex(NAME)),
              SORT_BY.fromShort(c.getShort(c.getColumnIndex(SORT_BY_FIELD))), "", "",
              SYNC_STATE.parseInt(c.getInt(c.getColumnIndex(DatabaseHelper.SYNC_STATE_FIELD))),
              c.getInt(c.getColumnIndex(LFT)), c.getInt(c.getColumnIndex(RGT)), c.getInt(c.getColumnIndex(COLOR)),
              AccountMirakel.getLocal());
        int defDateCol = c.getColumnIndex(DEFAULT_DUE);
        whereString = c.getString(c.getColumnIndex(WHERE_QUERY));
        setActive(c.getShort(c.getColumnIndex(ACTIVE)) == 1);
        setDefaultList(ListMirakel.get(c.getInt(c.getColumnIndex(DEFAULT_LIST))));
        setDefaultDate(c.isNull(defDateCol) ? null : c.getInt(defDateCol));
        isSpecial = true;
    }

    private static Map<String, SpecialListsBaseProperty> deserializeWhere(
        final String whereQuery) {
        final Map<String, SpecialListsBaseProperty> ret = new HashMap<>();
        final JsonObject all = new JsonParser().parse(whereQuery)
        .getAsJsonObject();
        final Gson gson = new GsonBuilder()
        .registerTypeAdapter(SpecialListsDueProperty.class,
                             new DueDeserializer())
        .registerTypeAdapter(
            SpecialListsContentProperty.class,
            new StringDeserializer<>(
                SpecialListsContentProperty.class)
        )
        .registerTypeAdapter(
            SpecialListsNameProperty.class,
            new StringDeserializer<>(
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
                return new HashMap<>();
            }
            final SpecialListsBaseProperty prop = gson.fromJson(
                    entry.getValue(), className);
            ret.put(key, prop);
        }
        return ret;
    }

    public static long getSpecialListCount(final boolean respectEnable) {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(context);
        if (respectEnable) {
            qb.and(ACTIVE, Operation.EQ, true);
        }
        return qb.count(URI);
    }

    // Parcelable stuff


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(active ? (byte) 1 : (byte) 0);
        dest.writeSerializable(this.defaultList);
        dest.writeValue(this.defaultDate);
        dest.writeString(this.whereString);
        dest.writeInt(this.sortBy == null ? -1 : this.sortBy.ordinal());
        dest.writeString(this.createdAt);
        dest.writeString(this.updatedAt);
        dest.writeInt(this.syncState == null ? -1 : this.syncState.ordinal());
        dest.writeInt(this.lft);
        dest.writeInt(this.rgt);
        dest.writeInt(this.color);
        dest.writeLong(this.accountID);
        dest.writeByte(isSpecial ? (byte) 1 : (byte) 0);
        dest.writeLong(this.getId());
        dest.writeString(this.getName());
    }

    private SpecialList(Parcel in) {
        super();
        this.active = in.readByte() != 0;
        this.defaultList = (Optional<ListMirakel>) in.readSerializable();
        this.defaultDate = (Integer) in.readValue(Integer.class.getClassLoader());
        this.whereString = in.readString();
        int tmpSortBy = in.readInt();
        this.sortBy = tmpSortBy == -1 ? null : SORT_BY.values()[tmpSortBy];
        this.createdAt = in.readString();
        this.updatedAt = in.readString();
        int tmpSyncState = in.readInt();
        this.syncState = tmpSyncState == -1 ? null : SYNC_STATE.values()[tmpSyncState];
        this.lft = in.readInt();
        this.rgt = in.readInt();
        this.color = in.readInt();
        this.accountID = in.readLong();
        this.isSpecial = in.readByte() != 0;
        this.setId(in.readLong());
        this.setName(in.readString());
    }

    public static final Creator<SpecialList> CREATOR = new Creator<SpecialList>() {
        public SpecialList createFromParcel(Parcel source) {
            return new SpecialList(source);
        }
        public SpecialList[] newArray(int size) {
            return new SpecialList[size];
        }
    };
}
