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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.query_builder.Cursor2List;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakel.model.query_builder.CursorWrapper;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Sorting;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.FileUtils;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;

/**
 * @author az
 */
public class ListMirakel extends ListBase implements ListMirakelInterface {

    /**
     * This is used by the Spinner in the MirakelActivity to identify the All Accounts selection
     */
    public static final int ALL_ACCOUNTS_ID = 0;
    private static final CursorWrapper.CursorConverter<List<ListMirakel>> LIST_FROM_CURSOR = new
    Cursor2List<>(ListMirakel.class);

    public static class ListAlreadyExistsException extends Exception {
        public ListAlreadyExistsException(String detailMessage) {
            super(detailMessage);
        }
    }

    public static final String[] allColumns = {ModelBase.ID,
                                               ModelBase.NAME, SORT_BY_FIELD, DatabaseHelper.CREATED_AT,
                                               DatabaseHelper.UPDATED_AT, DatabaseHelper.SYNC_STATE_FIELD, LFT,
                                               RGT, COLOR, ACCOUNT_ID, ICON_PATH, IS_SPECIAL, SpecialList.WHERE_QUERY,
                                               SpecialList.ACTIVE, SpecialList.DEFAULT_DUE, SpecialList.DEFAULT_LIST
                                              };

    public enum SORT_BY {
        OPT, DUE, PRIO, ID, REVERT_DEFAULT;

        public short getShort() {
            switch (this) {
            case OPT:
                return 0;
            case DUE:
                return 1;
            case PRIO:
                return 2;
            case ID:
                return 3;
            case REVERT_DEFAULT:
                return 4;
            default:
                throw new IllegalArgumentException("Unkown SORT_BY type " + this.toString());
            }
        }

        public static SORT_BY fromShort(final short s) {
            switch (s) {
            case 0:
                return OPT;
            case 1:
                return DUE;
            case 2:
                return PRIO;
            case 3:
                return ID;
            case 4:
                return REVERT_DEFAULT;
            default:
                throw new IllegalArgumentException("Cannot transform " + s + " to SORT_BY");
            }
        }
    }
    public static final Uri URI = MirakelInternalContentProvider.LIST_URI;


    public static final String TABLE = "lists";

    private static final String TAG = "ListMirakel";

    protected Uri getUri() {
        return URI;
    }

    @NonNull
    public MirakelQueryBuilder addSortBy(final MirakelQueryBuilder qb) {
        return addSortBy(qb, getSortBy(), isSpecial());
    }

    @NonNull
    public static MirakelQueryBuilder addSortBy(final MirakelQueryBuilder qb, final SORT_BY sorting,
            final boolean isSpecial) {
        final String dueSort = "CASE WHEN (" + Task.DUE
                               + " IS NULL) THEN strftime('%s','now','+50 years')*1000 ELSE "
                               + Task.DUE + " END ";
        switch (sorting) {
        case PRIO:
            qb.sort(Task.PRIORITY, Sorting.DESC);
            break;
        case OPT:
            qb.sort(Task.DONE, Sorting.ASC);
            qb.sort(dueSort, Sorting.ASC);
            qb.sort(Task.PRIORITY, Sorting.DESC);
            qb.sort(Task.PROGRESS, Sorting.DESC);
            break;
        case DUE:
            qb.sort(Task.DONE, Sorting.ASC);
            qb.sort(dueSort, Sorting.ASC);
            break;
        case REVERT_DEFAULT:
            qb.sort(Task.PRIORITY, Sorting.DESC);
            qb.sort(dueSort, Sorting.ASC);
        //$FALL-THROUGH$
        default:
            qb.sort(Task.ID, Sorting.ASC);
        }
        if (isSpecial) {
            qb.sort(Task.LIST_ID, Sorting.ASC);
        }
        return qb;
    }

    /**
     * Get all Lists in the Database
     *
     * @return List of Lists
     */
    @NonNull
    public static List<ListMirakel> all() {
        return all(true);
    }

    @NonNull
    public static List<ListMirakel> all(final boolean withSpecial) {
        return allCursor(withSpecial).doWithCursor(LIST_FROM_CURSOR);
    }
    @NonNull
    public static List<ListMirakel> all(final Optional<AccountMirakel> accountMirakelOptional,
                                        final boolean withSpecial) {
        return allCursor(accountMirakelOptional, withSpecial).doWithCursor(LIST_FROM_CURSOR);
    }

    public static List<ListMirakel> cursorToList(final @NonNull CursorWrapper c) {
        return  c.doWithCursor(LIST_FROM_CURSOR);
    }

    @NonNull
    private static MirakelQueryBuilder getBasicMQB(@NonNull final Optional<AccountMirakel>
            accountMirakelOptional) {
        final MirakelQueryBuilder mirakelQueryBuilder = new MirakelQueryBuilder(context).and(
            DatabaseHelper.SYNC_STATE_FIELD, Operation.NOT_EQ,
            SYNC_STATE.DELETE.toString()).sort(LFT,
                                               Sorting.ASC);
        if (accountMirakelOptional.isPresent()) {
            mirakelQueryBuilder.and(ACCOUNT_ID, Operation.EQ, accountMirakelOptional.get());
        }

        return mirakelQueryBuilder;
    }

    @NonNull
    public static MirakelQueryBuilder allWithSpecialMQB(@NonNull final Optional<AccountMirakel>
            accountMirakelOptional) {
        final MirakelQueryBuilder mirakelQueryBuilder = new MirakelQueryBuilder(context).and(
            DatabaseHelper.SYNC_STATE_FIELD, Operation.NOT_EQ, SYNC_STATE.DELETE.toString());
        if (accountMirakelOptional.isPresent()) {
            mirakelQueryBuilder.and(ACCOUNT_ID, Operation.EQ, accountMirakelOptional.get());
        } else {
            mirakelQueryBuilder.and(new MirakelQueryBuilder(context).or(ModelBase.ID, Operation.GE,
                                    0).or(ACCOUNT_ID, Operation.EQ, (ModelBase) null));
        }
        return mirakelQueryBuilder;
    }

    @NonNull
    public static android.support.v4.content.CursorLoader allWithSpecialSupportCursorLoader(
        @NonNull final Optional<AccountMirakel> accountMirakelOptional) {
        return allWithSpecialMQB(accountMirakelOptional).toSupportCursorLoader(
                   MirakelInternalContentProvider.LIST_WITH_COUNT_URI);
    }

    @NonNull
    public static CursorWrapper allCursor(final boolean withSpecial) {
        return  allCursor(Optional.<AccountMirakel>absent(), withSpecial);
    }
    @NonNull
    public static CursorWrapper allCursor(@NonNull final Optional<AccountMirakel>
                                          accountMirakelOptional,
                                          final boolean withSpecial) {
        MirakelQueryBuilder qb = getBasicMQB(accountMirakelOptional);
        if (!withSpecial) {
            qb.and(IS_SPECIAL, Operation.EQ, false);
        } else {
            qb.and(SpecialList.ACTIVE, Operation.EQ, true);
        }
        return qb.query(URI);
    }

    public static long count() {
        return new MirakelQueryBuilder(context).count(URI);
    }

    /**
     * Create a List from a Cursor
     *
     * @param c cursor
     */
    public ListMirakel(final @NonNull CursorGetter c) {
        super(c.getLong(ID), c.getString(NAME),
              SORT_BY.fromShort(c.getShort(SORT_BY_FIELD)),
              c.getDateTime(DatabaseHelper.CREATED_AT),
              c.getDateTime(DatabaseHelper.UPDATED_AT),
              SYNC_STATE.valueOf(c.getShort(DatabaseHelper.SYNC_STATE_FIELD)),
              c.getInt(LFT), c.getInt(RGT), c.getInt(COLOR),
              c.getInt(ACCOUNT_ID),
              FileUtils.parsePath(c.getString(ICON_PATH)),
              c.getBoolean(IS_SPECIAL));
    }


    protected ListMirakel(final long id, @NonNull final String name, @NonNull final SORT_BY sortBy,
                          @NonNull final DateTime createdAt, @NonNull final DateTime updatedAt,
                          @NonNull final SYNC_STATE syncState, final int lft, final int rgt,
                          final int color, @NonNull final AccountMirakel account, @NonNull final Optional<Uri> iconPath,
                          final boolean isSpecial) {
        super(id, name, sortBy, createdAt, updatedAt, syncState, lft, rgt,
              color, account, iconPath, isSpecial);
    }

    @NonNull
    public static Optional<ListMirakel> findByName(final String name) {
        return findByName(name, null);
    }

    @NonNull
    public static Optional<ListMirakel> findByName(final String name,
            final AccountMirakel account) {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(context).and(NAME,
                Operation.EQ, name);
        if (account != null) {
            qb.and(ACCOUNT_ID, Operation.EQ, account);
        }
        return qb.get(ListMirakel.class);
    }

    // Static Methods

    @NonNull
    public static ListMirakel getInboxList(final AccountMirakel account) {
        final Optional<ListMirakel> listMirakelOptional = new MirakelQueryBuilder(context).and(NAME,
                Operation.EQ,
                context.getString(R.string.inbox)).and(ACCOUNT_ID, Operation.EQ,
                        account).get(ListMirakel.class);
        if (listMirakelOptional.isPresent()) {
            return listMirakelOptional.get();
        }
        try {
            return newList(context.getString(R.string.inbox), SORT_BY.OPT, account);
        } catch (final ListAlreadyExistsException e) {
            // WTF? This could never happen (theoretically)
            throw new RuntimeException("getInboxList() failed somehow", e);
        }
    }

    @NonNull
    public static Optional<ListMirakel> get(final long listId) {
        return new MirakelQueryBuilder(context).get(ListMirakel.class, listId);
    }

    public static void setDefaultAccount(@NonNull final AccountMirakel account) {
        final ContentValues v = new ContentValues();
        v.put(ACCOUNT_ID, account.getId());
        update(URI, v, null, null);
    }

    public static List<ListMirakel> getListsForAccount(
        final AccountMirakel account) {
        if ((account == null) || !account.isEnabled()) {
            return new ArrayList<>(0);
        }
        return new MirakelQueryBuilder(context).and(DatabaseHelper.SYNC_STATE_FIELD,
                Operation.NOT_EQ, SYNC_STATE.DELETE.toInt()).and(ACCOUNT_ID,
                        Operation.EQ, account).getList(ListMirakel.class);
    }

    @NonNull
    private static ListMirakel safeNewList(String name, final AccountMirakel accountMirakel,
                                           final int iteration) {
        final ListMirakel listMirakel;
        try {
            if (iteration > 0) {
                name += " " + iteration;
            }
            listMirakel = ListMirakel.newList(name, accountMirakel);
        } catch (final ListMirakel.ListAlreadyExistsException ignored) {
            return safeNewList(name, accountMirakel, iteration + 1);
        }
        return listMirakel;
    }

    @NonNull
    public static ListMirakel safeNewList(final String name) {
        return safeNewList(name, AccountMirakel.getLocal(), 0);
    }

    @NonNull
    public static ListMirakel safeNewList(final String name, final AccountMirakel accountMirakel) {
        return safeNewList(name, accountMirakel, 0);
    }

    @NonNull
    private static ListMirakel newList(final String name,
                                       final AccountMirakel account) throws ListAlreadyExistsException {
        return newList(name, SORT_BY.OPT, account);
    }

    @NonNull
    public static ListMirakel newList(final String name, final SORT_BY sort_by,
                                      final AccountMirakel account) throws ListAlreadyExistsException {
        final DateTime now = new DateTime();
        final ListMirakel list = new ListMirakel(0L, name, sort_by, now, now, SYNC_STATE.ADD, 0, 0, 0,
                account,
                Optional.<Uri>absent(), false);
        return list.create();
    }

    @NonNull
    public static Optional<ListMirakel> getByName(final String name,
            final AccountMirakel accountMirakel) {
        return new MirakelQueryBuilder(context)
               .and(ListBase.NAME, Operation.EQ, name)
               .and(ListMirakel.ACCOUNT_ID, Operation.EQ, accountMirakel)
               .get(ListMirakel.class);
    }


    protected ListMirakel create() throws ListAlreadyExistsException {
        final Optional<ListMirakel> listMirakel = getByName(getName(), getAccount());
        if (listMirakel.isPresent()) {
            throw new ListAlreadyExistsException("List" + listMirakel.get().getName() + " already exists:" +
                                                 listMirakel.get().getId());
        }
        final ContentValues values = getContentValues();
        values.remove(ID);
        values.remove(RGT);
        values.remove(LFT);
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
        MirakelInternalContentProvider.withTransaction(new
        MirakelInternalContentProvider.DBTransaction() {
            @Override
            public void exec() {
                setId(insert(URI, values));
                // Dirty workaround
                final CursorWrapper c = new MirakelQueryBuilder(context).select("MAX(" + RGT + ')').query(URI);
                c.doWithCursor(new CursorWrapper.WithCursor() {
                    @Override
                    public void withOpenCursor(@NonNull final CursorGetter getter) {
                        getter.moveToFirst();
                        final int maxRGT = getter.getInt(0);
                        final ContentValues cv = new ContentValues();
                        cv.put(LFT, maxRGT + 1);
                        cv.put(RGT, maxRGT + 2);
                        update(URI, cv, ModelBase.ID + '=' + getId(), null);
                    }
                });
            }
        });
        return ListMirakel.get(getId()).get();
    }


    @NonNull
    public static ListMirakel safeFirst() {
        final Optional<ListMirakel> s = new MirakelQueryBuilder(context).and(
            DatabaseHelper.SYNC_STATE_FIELD,
            Operation.NOT_EQ, SYNC_STATE.DELETE.toInt()).sort(LFT,
                    Sorting.ASC).get(ListMirakel.class);
        if (!s.isPresent()) {
            return getInboxList(MirakelModelPreferences.getDefaultAccount());
        } else {
            return s.get();
        }
    }

    @NonNull
    public static ListMirakel safeGet(final int listId) {
        final Optional<ListMirakel> l = get(listId);
        if (!l.isPresent()) {
            return safeFirst();
        } else {
            return l.get();
        }
    }


    public static ListMirakel getStub() {
        final ListMirakel stub = new ListMirakel(ModelBase.INVALID_ID,
                context.getString(R.string.stub_list_name),
                SORT_BY.ID, new DateTime(), new DateTime(), SYNC_STATE.ADD, 0, 0, 0, AccountMirakel.getLocal(),
                Optional.<Uri>absent(), false);
        stub.setName(context.getString(R.string.stub_list_name));
        stub.setAccount(AccountMirakel.getLocal());
        return stub;
    }

    /**
     * Count the tasks of that list
     *
     * @return
     */
    public long countTasks() {
        final MirakelQueryBuilder qb = getWhereQueryForTasks();
        qb.and(Task.DONE, Operation.EQ, false);
        return Task.addBasicFiler(qb).count(Task.URI);
    }

    @Override
    public ShowDoneCases shouldShowDoneToggle() {
        if (isSpecial()) {
            return ShowDoneCases.NOTHING;
        }
        return new MirakelQueryBuilder(context).and(Task.LIST_ID,
                MirakelQueryBuilder.Operation.EQ,
                this).select("sum(" + Task.DONE + ')', "count(*)").query(Task.URI)
        .doWithCursor(new CursorWrapper.CursorConverter<ShowDoneCases>() {
            @NonNull
            @Override
            public ShowDoneCases convert(@NonNull final CursorGetter getter) {
                if (!getter.moveToFirst()) {
                    return ShowDoneCases.NOTHING;
                }
                final long sum = getter.getLong(0);
                if (sum == 0L) {
                    return ShowDoneCases.ONLY_UNDONE;
                }
                if (sum == getter.getLong(1)) {
                    return ShowDoneCases.ONLY_DONE;
                }
                return ShowDoneCases.BOTH;
            }
        });
    }


    public void destroy() {
        destroy(false);
    }

    /**
     * Delete a List from the Database
     *
     * @param force, do not respect sync_state
     */
    public void destroy(final boolean force) {
        MirakelInternalContentProvider.withTransaction(new
        MirakelInternalContentProvider.DBTransaction() {
            @Override
            public void exec() {
                final long id = getId();
                if ((getSyncState() == SYNC_STATE.ADD) || force) {
                    delete(MirakelInternalContentProvider.TASK_URI,
                           Task.LIST_ID + " = " + id, null);
                    delete(URI, ModelBase.ID + " = "
                           + id, null);
                } else {
                    final ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.SYNC_STATE_FIELD,
                               SYNC_STATE.DELETE.toInt());
                    update(MirakelInternalContentProvider.TASK_URI, values,
                           Task.LIST_ID + " = " + id,
                           null);
                    update(URI, values, ModelBase.ID
                           + '=' + id, null);
                }
                final ContentValues cv = new ContentValues();
                cv.put("TABLE", TABLE);
                update(MirakelInternalContentProvider.UPDATE_LIST_ORDER_URI, cv, LFT + '>' + getLft(), null);
            }
        });
    }


    public void save() {
        save(true);
    }

    /**
     * Update the List in the Database
     *
     * @param log, save a undo_log
     *            The List
     */
    public void save(final boolean log) {
        final SharedPreferences.Editor editor = MirakelPreferences.getEditor();
        // TODO implement for specialLists
        if (getId() > 0) {
            MirakelInternalContentProvider.withTransaction(new
            MirakelInternalContentProvider.DBTransaction() {
                @Override
                public void exec() {
                    setSyncState(((getSyncState() == SYNC_STATE.ADD)
                                  || (getSyncState() == SYNC_STATE.IS_SYNCED)) ? getSyncState() : SYNC_STATE.NEED_SYNC);
                    setUpdatedAt(new DateTime());
                    final ContentValues values = getContentValues();
                    update(URI, values, ModelBase.ID
                           + " = " + getId(), null);
                    final ContentValues taskContentValues = new ContentValues();
                    taskContentValues.put(DatabaseHelper.UPDATED_AT,
                                          new GregorianCalendar().getTimeInMillis() / 1000L);
                    taskContentValues.put(DatabaseHelper.SYNC_STATE_FIELD,
                                          SYNC_STATE.NEED_SYNC.toInt());
                    update(MirakelInternalContentProvider.TASK_URI, taskContentValues,
                           Task.LIST_ID + "=?",
                           new String[] {String.valueOf(getId())});
                }
            });
        } else {
            final ContentValues values = getContentValues();
            update(SpecialList.URI, values, ModelBase.ID + " = " + Math.abs(getId()), null);
        }
        editor.commit();
    }

    /**
     * Get all Tasks
     *
     * @return
     */
    @NonNull
    public List<Task> tasks() {
        return Task.getTasks(this, getSortBy(), false);
    }

    /**
     * Get all Tasks
     *
     * @param showDone
     * @return
     */
    @NonNull
    public List<Task> tasks(final boolean showDone) {
        return Task.getTasks(this, getSortBy(), showDone);
    }

    public Optional<SpecialList> toSpecial() {
        if(isSpecial()){
            return of((SpecialList)this);
        }else {
            return absent();
        }
    }

    @NonNull
    public MirakelQueryBuilder getTasksQueryBuilder() {
        return Task.getMirakelQueryBuilder(of(this));
    }

    public static MirakelQueryBuilder addTaskOverviewSelection(final MirakelQueryBuilder
            mirakelQueryBuilder) {
        return mirakelQueryBuilder.select(Task.VIEW_TABLE + '.' + Task.ID + " AS " + Task.ID,
                                          Task.VIEW_TABLE + '.' + Task.NAME + " AS " + Task.NAME, Task.DONE, Task.PROGRESS, Task.DUE,
                                          Task.LIST_ID, "list_name", ACCOUNT_ID, Task.PRIORITY);
    }


    @NonNull
    public MirakelQueryBuilder getWhereQueryForTasks() {
        return Task.addBasicFiler(new MirakelQueryBuilder(context).and(Task.LIST_ID,
                                  Operation.EQ, this));
    }

    // Parcelable stuff


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(this.sortBy.ordinal());
        dest.writeSerializable(this.createdAt);
        dest.writeSerializable(this.updatedAt);
        dest.writeInt(this.syncState.ordinal());
        dest.writeInt(this.lft);
        dest.writeInt(this.rgt);
        dest.writeInt(this.color);
        dest.writeLong(this.accountID);
        dest.writeLong(this.getId());
        dest.writeString(this.getName());
        dest.writeParcelable(iconPath.orNull(), 3);
        dest.writeByte((byte) (isSpecial ? 1 : 0));
    }

    protected ListMirakel(final Parcel in) {
        final int tmpSortBy = in.readInt();
        this.sortBy = SORT_BY.values()[tmpSortBy];
        this.createdAt = (DateTime) in.readSerializable();
        this.updatedAt = (DateTime) in.readSerializable();
        final int tmpSyncState = in.readInt();
        this.syncState = SYNC_STATE.values()[tmpSyncState];
        this.lft = in.readInt();
        this.rgt = in.readInt();
        this.color = in.readInt();
        this.accountID = in.readLong();
        this.setId(in.readLong());
        this.setName(in.readString());
        iconPath = fromNullable(in.<Uri>readParcelable(Uri.class.getClassLoader()));
        isSpecial = in.readByte() == 1;
    }

    public static final Creator<ListMirakel> CREATOR = new Creator<ListMirakel>() {
        @Override
        public ListMirakel createFromParcel(final Parcel source) {
            return new ListMirakel(source);
        }

        @Override
        public ListMirakel[] newArray(final int size) {
            return new ListMirakel[size];
        }
    };

    public boolean isEditable() {
        if (isSpecial() || (accountID == INVALID_ID)) {
            return true;
        } else {
            return getAccount().getType().isListEditable();
        }
    }

    public boolean isDeletable() {
        // Same as isEditable
        return isEditable();
    }
}
