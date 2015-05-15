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
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.UndoHistory;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Sorting;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.FileUtils;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

/**
 * @author az
 */
public class ListMirakel extends ListBase implements ListMirakelInterface {

    /**
     * This is used by the Spinner in the MirakelActivity to identify the All Accounts selection
     */
    public static final int ALL_ACCOUNTS_ID = 0;

    public static class ListAlreadyExistsException extends Exception {
        public ListAlreadyExistsException(String detailMessage) {
            super(detailMessage);
        }
    }

    public static final String[] allColumns = {ModelBase.ID,
                                               ModelBase.NAME, SORT_BY_FIELD, DatabaseHelper.CREATED_AT,
                                               DatabaseHelper.UPDATED_AT, DatabaseHelper.SYNC_STATE_FIELD, LFT,
                                               RGT, COLOR, ACCOUNT_ID, ICON_PATH
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


    // private static final String TAG = "ListMirakel";


    public static final String TABLE = "lists";

    private static final String TAG = "ListMirakel";

    protected Uri getUri() {
        return URI;
    }

    @NonNull
    public MirakelQueryBuilder addSortBy(final MirakelQueryBuilder qb) {
        return addSortBy(qb, getSortBy(), getId() < 0);
    }

    @NonNull
    public static MirakelQueryBuilder addSortBy(final MirakelQueryBuilder qb, final SORT_BY sorting,
            final boolean isSpecial) {
        final String dueSort = "CASE WHEN (" + Task.DUE
                               + " IS NULL) THEN datetime('now','+50 years') ELSE datetime("
                               + Task.DUE
                               + ",'unixepoch','localtime','start of day') END ";
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
        final Cursor cursor = allCursor(withSpecial);
        final List<ListMirakel> lists = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            lists.add(new ListMirakel(cursor));
        }
        cursor.close();
        return lists;
    }

    public static List<ListMirakel> cursorToList(Cursor c) {
        final List<ListMirakel> ret = new ArrayList<>();
        if (c.moveToFirst()) {
            do {
                ret.add(new ListMirakel(c));
            } while (c.moveToNext());
        }
        c.close();
        return ret;
    }

    @NonNull
    private static MirakelQueryBuilder getBasicMQB() {
        return getBasicMQB(Optional.<AccountMirakel>absent());
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
    public static CursorLoader allWithSpecialCursorLoader(@NonNull final Optional<AccountMirakel>
            accountMirakelOptional) {
        return allWithSpecialMQB(accountMirakelOptional).toCursorLoader(
                   MirakelInternalContentProvider.LIST_WITH_SPECIAL_URI);
    }

    @NonNull
    public static android.support.v4.content.CursorLoader allWithSpecialSupportCursorLoader(
        @NonNull final Optional<AccountMirakel> accountMirakelOptional) {
        return allWithSpecialMQB(accountMirakelOptional).toSupportCursorLoader(
                   MirakelInternalContentProvider.LIST_WITH_SPECIAL_URI);
    }

    @NonNull
    public static Cursor allCursor(final boolean withSpecial) {
        return  allCursor(Optional.<AccountMirakel>absent(), withSpecial);
    }
    @NonNull
    public static Cursor allCursor(@NonNull final Optional<AccountMirakel> accountMirakelOptional,
                                   final boolean withSpecial) {
        if (withSpecial) {
            return allWithSpecialMQB(Optional.<AccountMirakel>absent()).query(
                       MirakelInternalContentProvider.LIST_WITH_SPECIAL_URI);
        } else {
            return getBasicMQB(accountMirakelOptional).query(MirakelInternalContentProvider.LIST_URI);
        }
    }

    public static long count() {
        return new MirakelQueryBuilder(context).count(URI);
    }

    /**
     * Create a List from a Cursor
     *
     * @param c cursor
     */
    public ListMirakel(final Cursor c) {
        super(c.getLong(c.getColumnIndex(ID)), c.getString(c.getColumnIndex(NAME)),
              SORT_BY.fromShort(c.getShort(c.getColumnIndex(SORT_BY_FIELD))),
              c.getString(c.getColumnIndex(DatabaseHelper.CREATED_AT)),
              c.getString(c.getColumnIndex(DatabaseHelper.UPDATED_AT)),
              SYNC_STATE.valueOf(c.getShort(c.getColumnIndex(DatabaseHelper.SYNC_STATE_FIELD))),
              c.getInt(c.getColumnIndex(LFT)), c.getInt(c.getColumnIndex(RGT)), c.getInt(c.getColumnIndex(COLOR)),
              c.getInt(c.getColumnIndex(ACCOUNT_ID)),
              FileUtils.parsePath(c.getString(c.getColumnIndex(ICON_PATH))));
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
        if (listId < 0L) {
            final Optional<SpecialList> specialList = SpecialList.getSpecial(-listId);
            if (specialList.isPresent()) {
                return of((ListMirakel) specialList.get());
            } else {
                return absent();
            }
        }
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

    /**
     * Create and insert a new List
     *
     * @param name    Name of the List
     * @param sort_by the default sorting
     * @return new List
     */
    @NonNull
    private static ListMirakel newList(final String name,
                                       final SORT_BY sort_by) throws ListAlreadyExistsException {
        return newList(name, sort_by,
                       MirakelModelPreferences.getDefaultAccount());
    }

    @NonNull
    private static ListMirakel newList(final String name,
                                       final AccountMirakel account) throws ListAlreadyExistsException {
        return newList(name, SORT_BY.OPT, account);
    }

    @NonNull
    public static ListMirakel newList(final String name, final SORT_BY sort_by,
                                      final AccountMirakel account) throws ListAlreadyExistsException {
        final String now = new SimpleDateFormat(context.getString(R.string.dateTimeFormat), Locale.US)
        .format(new Date());
        final ListMirakel l = new ListMirakel(0, name, sort_by, now, now, SYNC_STATE.ADD, 0, 0, 0, account,
                                              Optional.<Uri>absent());
        final ListMirakel newList = l.create();
        UndoHistory.logCreate(newList, context);
        return newList;
    }

    @NonNull
    public static Optional<ListMirakel> getByName(final String name,
            final AccountMirakel accountMirakel) {
        return new MirakelQueryBuilder(context)
               .and(ListBase.NAME, Operation.EQ, name)
               .and(ListMirakel.ACCOUNT_ID, Operation.EQ, accountMirakel)
               .get(ListMirakel.class);
    }

    @NonNull
    private ListMirakel create() throws ListAlreadyExistsException {
        final Optional<ListMirakel> listMirakel = getByName(getName(), getAccount());
        if (listMirakel.isPresent()) {
            throw new ListAlreadyExistsException("List" + listMirakel.get().getName() + " already exists:" +
                                                 listMirakel.get().getId());
        }
        final ContentValues values = new ContentValues();
        values.put(ModelBase.NAME, getName());
        values.put(ACCOUNT_ID, getAccount().getId());
        values.put(SORT_BY_FIELD, getSortBy().getShort());
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
        MirakelInternalContentProvider.withTransaction(new
        MirakelInternalContentProvider.DBTransaction() {
            @Override
            public void exec() {
                setId(insert(URI, values));
                // Dirty workaround
                final Cursor c = new MirakelQueryBuilder(context).select("MAX(" + RGT + ')').query(URI);
                c.moveToFirst();
                final int maxRGT = c.getInt(0);
                final ContentValues cv = new ContentValues();
                cv.put(LFT, maxRGT + 1);
                cv.put(RGT, maxRGT + 2);
                update(URI, cv, ModelBase.ID + '=' + getId(), null);
                c.close();
            }
        });
        final ListMirakel newList = get(getId()).get();
        UndoHistory.logCreate(newList, context);
        return newList;
    }

    /**
     * Use this function only if you know what you are doing
     * @param el Json Object
     * @return List
     */
    @NonNull
    public static ListMirakel unsafeParseJson(final JsonObject el) {
        ListMirakel t = null;
        final JsonElement id = el.get("id");
        if (id != null) {
            // use old List from db if existing
            t = ListMirakel.get(id.getAsInt()).or(new ListMirakel());
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
        if (j != null && j.getAsJsonPrimitive().isNumber()) {
            t.setSortBy(SORT_BY.fromShort(j.getAsShort()));
        }
        j = el.get("sync_state");
        if ((j != null) && j.getAsJsonPrimitive().isNumber()) {
            t.setSyncState(SYNC_STATE.valueOf((short) j.getAsFloat()));
        }
        j = el.get("color");
        if ((j != null) && j.getAsJsonPrimitive().isNumber()) {
            t.setColor(j.getAsInt());
        }
        return t;
    }

    @NonNull
    public static ListMirakel safeFirst() {
        Optional<ListMirakel> s = new MirakelQueryBuilder(context).and(DatabaseHelper.SYNC_STATE_FIELD,
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

    protected ListMirakel() {
        super();
    }

    public ListMirakel(final long id, @NonNull final String name, @NonNull final SORT_BY sort_by,
                       @NonNull final String created_at, @NonNull final String updated_at,
                       @NonNull final SYNC_STATE sync_state, final int lft, final int rgt,
                       final int color, @NonNull final AccountMirakel account, @NonNull final Optional<Uri> iconPath) {
        super(id, name, sort_by, created_at, updated_at, sync_state, lft, rgt,
              color, account, iconPath);
    }

    protected ListMirakel(final long id, @NonNull final String name, @NonNull final SORT_BY sort_by,
                          @NonNull final String created_at, @NonNull final String updated_at,
                          @NonNull final SYNC_STATE sync_state, final int lft, final int rgt,
                          final int color, final int account, @NonNull final Optional<Uri> iconPath) {
        super(id, name, sort_by, created_at, updated_at, sync_state, lft, rgt,
              color, account, iconPath);
    }

    public static ListMirakel getStub() {
        final ListMirakel stub = new ListMirakel();
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
        MirakelQueryBuilder qb;
        if (getId() < 0) {
            final Optional<SpecialList> specialList = toSpecial();
            qb = specialList.isPresent() ? specialList.get().getWhereQueryForTasks() : new MirakelQueryBuilder(
                     context);
        } else {
            qb = new MirakelQueryBuilder(context).and(Task.LIST_ID, Operation.EQ, this);
        }
        qb.and(Task.DONE, Operation.EQ, false);
        return Task.addBasicFiler(qb).count(Task.URI);
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
        if (!force) {
            UndoHistory.updateLog(this, context);
        }
        final long id = getId();
        if (id <= 0L) {
            final SpecialList slist = (SpecialList) this;
            slist.destroy();
            return;
        }
        MirakelInternalContentProvider.withTransaction(new
        MirakelInternalContentProvider.DBTransaction() {
            @Override
            public void exec() {
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

    public Optional<Task> getFirstTask() {
        return addSortBy(Task.addBasicFiler(getWhereQueryForTasks())).get(Task.class);
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
                    setUpdatedAt(new SimpleDateFormat(
                                     context.getString(R.string.dateTimeFormat),
                                     Locale.getDefault()).format(new Date()));
                    final ContentValues values = getContentValues();
                    if (log) {
                        UndoHistory.updateLog(ListMirakel.get(getId()).get(), context);
                    }
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

    private Optional<SpecialList> toSpecial() {
        final Optional<SpecialList> specialListOptional = SpecialList.getSpecial(getId());
        if (specialListOptional.isPresent() && accountID != ALL_ACCOUNTS_ID &&
            getAccount().getType() != AccountMirakel.ACCOUNT_TYPES.ALL) {
            specialListOptional.get().setAccount(getAccount());
        }
        return specialListOptional;
    }

    @NonNull
    public MirakelQueryBuilder getTasksQueryBuilder() {
        if (getId() < 0L) {
            // We look like a List but we are better than one MUHAHA
            final Optional<SpecialList> specialListOptional = toSpecial();
            if (specialListOptional.isPresent()) {
                final SpecialList specialList = specialListOptional.get();
                final MirakelQueryBuilder mirakelQueryBuilder = specialList.getWhereQueryForTasks();
                addSortBy(mirakelQueryBuilder);
                return mirakelQueryBuilder;
            } else {
                throw new RuntimeException("No such special list");
            }
        } else {
            return Task.getMirakelQueryBuilder(of(this));
        }
    }

    @NonNull
    public CursorLoader getTasksCursorLoader() {
        return getTasksQueryBuilder().toCursorLoader(MirakelInternalContentProvider.TASK_URI);
    }

    @NonNull
    public android.support.v4.content.CursorLoader getTasksSupportCursorLoader() {
        return getTasksQueryBuilder().toSupportCursorLoader(MirakelInternalContentProvider.TASK_URI);
    }

    public static MirakelQueryBuilder addTaskOverviewSelection(final MirakelQueryBuilder
            mirakelQueryBuilder) {
        return mirakelQueryBuilder.select(Task.VIEW_TABLE + '.' + Task.ID + " AS " + Task.ID,
                                          Task.VIEW_TABLE + '.' + Task.NAME + " AS " + Task.NAME, Task.DONE, Task.PROGRESS, Task.DUE,
                                          Task.LIST_ID, "list_name", ACCOUNT_ID, Task.PRIORITY);
    }

    @NonNull
    public android.support.v4.content.CursorLoader getTaskOverviewSupportCursorLoader() {
        return addTaskOverviewSelection(getTasksQueryBuilder()).toSupportCursorLoader(
                   MirakelInternalContentProvider.TASK_URI);
    }

    @NonNull
    public String toJson() {
        String json = "{";
        json += "\"name\":\"" + getName() + "\",";
        json += "\"id\":" + getId() + ',';
        json += "\"created_at\":\"" + getCreatedAt() + "\",";
        json += "\"updated_at\":\"" + getName() + "\",";
        json += "\"lft\":" + getLft() + ',';
        json += "\"rgt\":" + getRgt() + ',';
        json += "\"sort_by\":" + getSortBy().getShort() + ',';
        json += "\"sync_state\":" + getSyncState().toInt() + ',';
        json += "\"color\":" + getColor();
        json += "}";
        return json;
    }

    @NonNull
    public MirakelQueryBuilder getWhereQueryForTasks() {
        final MirakelQueryBuilder qb = Task.addBasicFiler(new MirakelQueryBuilder(context).and(Task.LIST_ID,
                                       Operation.EQ, this));
        if (!MirakelCommonPreferences.showDoneMain()) {
            qb.and(Task.DONE, Operation.EQ, false);
        }
        return qb;
    }

    // Parcelable stuff


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(this.sortBy.ordinal());
        dest.writeString(this.createdAt);
        dest.writeString(this.updatedAt);
        dest.writeInt(this.syncState.ordinal());
        dest.writeInt(this.lft);
        dest.writeInt(this.rgt);
        dest.writeInt(this.color);
        dest.writeLong(this.accountID);
        dest.writeByte(isSpecial ? (byte) 1 : (byte) 0);
        dest.writeLong(this.getId());
        dest.writeString(this.getName());
    }

    private ListMirakel(Parcel in) {
        final int tmpSortBy = in.readInt();
        this.sortBy = SORT_BY.values()[tmpSortBy];
        this.createdAt = in.readString();
        this.updatedAt = in.readString();
        final int tmpSyncState = in.readInt();
        this.syncState = SYNC_STATE.values()[tmpSyncState];
        this.lft = in.readInt();
        this.rgt = in.readInt();
        this.color = in.readInt();
        this.accountID = in.readLong();
        this.isSpecial = in.readByte() != 0;
        this.setId(in.readLong());
        this.setName(in.readString());
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
        // TODO This is buggy I think we need to refactor the ListMirakel handling
        if (getId() < 0 || accountID == INVALID_ID) {
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
