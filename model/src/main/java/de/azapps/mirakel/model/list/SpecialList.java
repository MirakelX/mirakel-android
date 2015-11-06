/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.model.list;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

import org.joda.time.DateTime;

import java.util.List;

import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountVanishedException;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Sorting;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static de.azapps.tools.OptionalUtils.transformOrNull;

public class SpecialList extends ListMirakel {
    private boolean active;
    @NonNull
    private Optional<ListMirakel> defaultList = absent();
    @NonNull
    private Optional<Integer> defaultDate;
    @NonNull
    private Optional<SpecialListsBaseProperty> where = absent();
    private String whereString;

    public static final Uri URI = MirakelInternalContentProvider.LIST_URI;

    public boolean isActive() {
        return this.active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    @Override
    @NonNull
    public MirakelQueryBuilder getWhereQueryForTasks() {
        final MirakelQueryBuilder mirakelQueryBuilder = packWhere(getWhere());
        try {
            if (getAccount().getType() != AccountMirakel.ACCOUNT_TYPES.ALL) {
                mirakelQueryBuilder.and(ACCOUNT_ID, Operation.EQ, getAccount());
            }
        } catch (final AccountVanishedException ignored) {
            //ignore this
        }
        return mirakelQueryBuilder;
    }



    public Optional<SpecialListsBaseProperty> getWhere() {
        if (!where.isPresent()) {
            where = SpecialListsWhereDeserializer.deserializeWhere(whereString, getName());
        }
        return this.where;
    }

    public void setWhere(final @NonNull Optional<SpecialListsBaseProperty> where) {
        this.whereString = serializeWhere(where);
        this.where = where;
    }

    public ListMirakel getDefaultList() {
        if (!this.defaultList.isPresent()) {
            return ListMirakel.safeFirst();
        } else {
            return this.defaultList.get();
        }
    }

    public void setDefaultList(@NonNull final Optional<ListMirakel> defaultList) {
        this.defaultList = defaultList;
    }

    @NonNull
    public Optional<Integer> getDefaultDate() {
        return defaultDate;
    }

    public void setDefaultDate(@NonNull final Optional<Integer> defaultDate) {
        this.defaultDate = defaultDate;
    }

    SpecialList(final long id, @NonNull final String name,
                final @NonNull Optional<SpecialListsBaseProperty> whereQuery,
                final boolean active, @NonNull final Optional<ListMirakel> defaultList,
                final @NonNull Optional<Integer> defaultDate, final SORT_BY sortBy,
                final @NonNull DateTime createdAt, final @NonNull DateTime updatedAt,
                final SYNC_STATE syncState, final int color, final int lft,
                final int rgt, @NonNull final Optional<Uri> iconPath) {
        super(id, name, sortBy, createdAt, updatedAt, syncState, lft, rgt, color,
              AccountMirakel.getLocal(), iconPath, true);
        this.active = active;
        this.where = whereQuery;
        this.whereString = serializeWhere(whereQuery);
        this.defaultList = defaultList;
        this.defaultDate = defaultDate;
    }

    /**
     * Get all Tasks
     *
     * @return
     */
    @Override
    @NonNull
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
    @NonNull
    public List<Task> tasks(final boolean showDone) {
        if (showDone) {
            return addSortBy(getWhereQueryForTasks()).getList(Task.class);
        } else {
            return addSortBy(getWhereQueryForTasks().and(Task.DONE, Operation.EQ,
                             false)).getList(Task.class);
        }
    }

    // Static Methods
    public static final String WHERE_QUERY = "whereQuery";
    public static final String ACTIVE = "active";
    public static final String DEFAULT_LIST = "def_list";
    public static final String DEFAULT_DUE = "def_date";
    public static final String[] allColumns = {ModelBase.ID,
                                               ModelBase.NAME, SORT_BY_FIELD, DatabaseHelper.CREATED_AT,
                                               DatabaseHelper.UPDATED_AT, DatabaseHelper.SYNC_STATE_FIELD, LFT,
                                               RGT, COLOR, ACCOUNT_ID, ICON_PATH, IS_SPECIAL, WHERE_QUERY, ACTIVE, DEFAULT_DUE, DEFAULT_LIST
                                              };
    private static final String TAG = "SpecialList";


    public static SpecialList newList(final String name,
                                      final Optional<SpecialListsBaseProperty> whereQuery,
                                      final boolean active) throws ListAlreadyExistsException {
        final DateTime now = new DateTime();
        final SpecialList meta = new SpecialList(ModelBase.INVALID_ID, name, whereQuery, active,
                Optional.<ListMirakel>absent(),
                Optional.<Integer>absent(), SORT_BY.OPT, now, now,
                SYNC_STATE.ADD, 0,
                0, 0, Optional.<Uri>absent());
        return (SpecialList) meta.create();
    }

    public static String serializeWhere(
        final Optional<SpecialListsBaseProperty> whereQuery) {
        if (whereQuery.isPresent()) {
            return whereQuery.get().serialize();
        } else {
            return "";
        }
    }

    private static MirakelQueryBuilder packWhere(
        final @NonNull Optional<SpecialListsBaseProperty> where) {

        final MirakelQueryBuilder qb;
        if (where.isPresent()) {
            qb = where.get().getWhereQueryBuilder(context);
        } else {
            qb = new MirakelQueryBuilder(context);
        }
        // Check for account
        Log.d(TAG, "Query:<" + qb.getSelection() + ">");
        return Task.addBasicFiler(qb);
    }


    @NonNull
    @Override
    public ContentValues getContentValues() {
        final ContentValues cv = super.getContentValues();
        cv.put(ACTIVE, isActive() ? 1 : 0);
        cv.put(WHERE_QUERY, whereString);
        cv.put(DEFAULT_LIST, transformOrNull(this.defaultList, new Function<ListMirakel, Long>() {
            @Override
            public Long apply(ListMirakel input) {
                return input.getId();
            }
        }));
        cv.put(DEFAULT_DUE, this.defaultDate.orNull());
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
        MirakelQueryBuilder qb = new MirakelQueryBuilder(context).and(ListMirakel.IS_SPECIAL, Operation.EQ,
                true).sort(LFT, Sorting.ASC);
        if (!showAll) {
            qb.and(ACTIVE, Operation.EQ, true);
        }
        return qb.getList(SpecialList.class);
    }

    @NonNull
    @Override
    public MirakelQueryBuilder getTasksQueryBuilder() {
        return addSortBy(getWhereQueryForTasks());
    }

    /**
     * Get the first List
     *
     * @return List
     */
    public static Optional<SpecialList> firstSpecial() {
        return new MirakelQueryBuilder(context).and(DatabaseHelper.SYNC_STATE_FIELD,
                Operation.NOT_EQ, SYNC_STATE.DELETE.toInt()).and(IS_SPECIAL, Operation.EQ, true).sort(LFT,
                        Sorting.ASC).get(SpecialList.class);
    }

    public static SpecialList firstSpecialSafe() {
        Optional<SpecialList> s = SpecialList.firstSpecial();
        if (!s.isPresent()) {
            try {
                s = fromNullable(SpecialList.newList(context.getString(R.string.list_all),
                                                     Optional.<SpecialListsBaseProperty>absent(), true));
            } catch (final ListAlreadyExistsException e) {
                //creating a new Special list if there is no special list should not fail
                throw new RuntimeException(e);
            }
            if (ListMirakel.count() == 0L) {
                ListMirakel.safeFirst();
            }
            s.get().save(false);
        }
        return s.get();
    }

    /**
     * Create a List from a Cursor
     *
     * @param c
     */
    public SpecialList(final @NonNull CursorGetter c) {
        super(c);
        this.whereString = c.getString(WHERE_QUERY);
        this.active = c.getBoolean(ACTIVE);
        this.defaultList = ListMirakel.get(c.getLong(DEFAULT_LIST));
        this.defaultDate = c.getOptional(DEFAULT_DUE, Integer.class);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof SpecialList)) {
            return false;
        }
        final SpecialList other = (SpecialList) obj;
        if (other.active != this.active) {
            return false;
        }
        if (!Objects.equal(other.whereString, this.whereString)) {
            return false;
        }
        if (!Objects.equal(other.defaultDate, this.defaultDate)) {
            return false;
        }
        if (!Objects.equal(other.defaultList, this.defaultList)) {
            return false;
        }
        return true;
    }

    // Parcelable stuff

    @Override
    public int describeContents() {
        return 0;
    }
    public static final int NULL_DATE_VALUE = -1337;

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
        dest.writeByte(active ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.defaultList.orNull(), 0);
        dest.writeInt(this.defaultDate.or(NULL_DATE_VALUE));
        dest.writeString(this.whereString);
    }

    private SpecialList(final Parcel in) {
        super(in);
        this.active = in.readByte() != 0;
        this.defaultList = fromNullable((ListMirakel) in.readParcelable(
                                            ListMirakel.class.getClassLoader()));
        final int tmpDefaultDate = in.readInt();
        this.defaultDate = (tmpDefaultDate == NULL_DATE_VALUE) ? Optional.<Integer>absent() : Optional.of(
                               tmpDefaultDate);
        this.whereString = in.readString();
    }

    public static final Creator<SpecialList> CREATOR = new Creator<SpecialList>() {
        public SpecialList createFromParcel(final Parcel source) {
            return new SpecialList(source);
        }

        public SpecialList[] newArray(final int size) {
            return new SpecialList[size];
        }
    };
}
