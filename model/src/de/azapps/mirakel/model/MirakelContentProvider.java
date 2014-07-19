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

package de.azapps.mirakel.model;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;

import org.dmfs.provider.tasks.TaskContract;
import org.dmfs.provider.tasks.TaskContract.CommonSyncColumns;
import org.dmfs.provider.tasks.TaskContract.TaskColumns;
import org.dmfs.provider.tasks.TaskContract.TaskListColumns;
import org.dmfs.provider.tasks.TaskContract.TaskSyncColumns;

import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class MirakelContentProvider extends ContentProvider implements
    OnAccountsUpdateListener {
    private static final int CATEGORIES = 6;
    private static final int CATEGORY_ID = 7;
    // TODO for what we will need this?
    private static final int INSTANCE_ID = 0;
    private static final int INSTANCES = 1;
    private static final int LIST_ID = 6;
    private static final int LISTS = 5;

    private static final String TAG = "MirakelContentProvider";
    private static final int TASK_ID = 3;
    private static final int TASKS = 2;
    private static final UriMatcher URI_MATCHER;
    private static DatabaseHelper openHelper;
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(TaskContract.AUTHORITY,
                           TaskContract.TaskLists.CONTENT_URI_PATH, LISTS);
        URI_MATCHER.addURI(TaskContract.AUTHORITY,
                           TaskContract.TaskLists.CONTENT_URI_PATH + "/#", LIST_ID);
        URI_MATCHER.addURI(TaskContract.AUTHORITY,
                           TaskContract.Tasks.CONTENT_URI_PATH, TASKS);
        URI_MATCHER.addURI(TaskContract.AUTHORITY,
                           TaskContract.Tasks.CONTENT_URI_PATH + "/#", TASK_ID);
        URI_MATCHER.addURI(TaskContract.AUTHORITY,
                           TaskContract.Instances.CONTENT_URI_PATH, INSTANCES);
        URI_MATCHER.addURI(TaskContract.AUTHORITY,
                           TaskContract.Instances.CONTENT_URI_PATH + "/#", INSTANCE_ID);
        URI_MATCHER.addURI(TaskContract.AUTHORITY,
                           TaskContract.Categories.CONTENT_URI_PATH, CATEGORIES);
        URI_MATCHER.addURI(TaskContract.AUTHORITY,
                           TaskContract.Categories.CONTENT_URI_PATH + "/#", CATEGORY_ID);
    }

    private static String addSegment(final String ownName,
                                     final String remoteName, final boolean comma) {
        return (comma ? " , " : " ") + ownName + " as " + remoteName;
    }

    private static ContentValues convertValues(final ContentValues values,
            final boolean isSyncadapter) {
        final ContentValues newValues = new ContentValues();
        if (values.containsKey(TaskColumns.TITLE)) {
            newValues.put(ModelBase.NAME,
                          values.getAsString(TaskColumns.TITLE));
        }
        if (values.containsKey(TaskListColumns.LIST_NAME)) {
            newValues.put(ModelBase.NAME,
                          values.getAsString(TaskColumns.TITLE));
        }
        if (values.containsKey(TaskColumns.DESCRIPTION)) {
            newValues.put(Task.CONTENT,
                          values.getAsString(TaskColumns.DESCRIPTION));
        }
        if (values.containsKey(TaskColumns.DUE)) {
            newValues.put(Task.DUE, values.getAsLong(TaskColumns.DUE) / 1000);
        }
        if (values.containsKey(TaskColumns.PRIORITY)) {
            int prio = values.getAsInteger(TaskColumns.PRIORITY);
            if (isSyncadapter) {
                switch (prio) {
                case 1:
                case 2:
                case 3:
                    prio = 2;
                    break;
                case 4:
                case 5:
                case 6:
                    prio = 1;
                    break;
                case 7:
                case 8:
                case 9:
                    prio = -1;
                    break;
                default:
                    prio = 0;
                    break;
                }
            } else {
                if (prio > 2) {
                    prio = 2;
                } else if (prio < -2) {
                    prio = -2;
                }
            }
            newValues.put(Task.PRIORITY, prio);
        }
        if (values.containsKey(TaskColumns.PERCENT_COMPLETE)) {
            newValues.put(Task.PROGRESS,
                          values.getAsInteger(TaskColumns.PERCENT_COMPLETE));
        }
        if (values.containsKey(TaskColumns.STATUS)) {
            final int status = values.getAsInteger(TaskColumns.STATUS);
            final boolean done = status == TaskColumns.STATUS_COMPLETED;
            Log.wtf(TAG, "status: " + status + "  COMPLETED: "
                    + TaskColumns.STATUS_COMPLETED);
            newValues.put(Task.DONE, done);
        }
        if (values.containsKey(TaskColumns.LIST_ID)) {
            // newValues.put(Task.LIST_ID, values.getAsInteger(Tasks.LIST_ID));
        }
        if (isSyncadapter) {
            if (values.containsKey(TaskColumns._ID)) {
                newValues.put(ModelBase.ID,
                              values.getAsInteger(TaskColumns._ID));
            }
            if (values.containsKey(TaskListColumns._ID)) {
                newValues.put(ModelBase.ID,
                              values.getAsInteger(TaskListColumns._ID));
            }
            if (values.containsKey(TaskListColumns.LIST_COLOR)) {
                newValues.put(ListMirakel.COLOR,
                              values.getAsInteger(TaskListColumns.LIST_COLOR));
            }
            if (values.containsKey(TaskColumns.CREATED)) {
                newValues.put(DatabaseHelper.CREATED_AT,
                              values.getAsLong(TaskColumns.CREATED) / 1000);
            }
            if (values.containsKey(TaskColumns.LAST_MODIFIED)) {
                newValues.put(DatabaseHelper.UPDATED_AT,
                              values.getAsLong(TaskColumns.LAST_MODIFIED) / 1000);
            }
            if (values.containsKey(CommonSyncColumns._DIRTY)) {
                final boolean val = values
                                    .getAsBoolean(CommonSyncColumns._DIRTY);
                if (!values.containsKey(TaskSyncColumns._DELETED)) {
                    newValues.put(DatabaseHelper.SYNC_STATE_FIELD,
                                  val ? SYNC_STATE.NEED_SYNC.toInt()
                                  : SYNC_STATE.NOTHING.toInt());
                } else if (values.getAsBoolean(TaskSyncColumns._DELETED)) {
                    newValues.put(DatabaseHelper.SYNC_STATE_FIELD,
                                  SYNC_STATE.DELETE.toInt());
                } else if (val) {
                    newValues.put(DatabaseHelper.SYNC_STATE_FIELD,
                                  SYNC_STATE.NEED_SYNC.toInt());
                } else {
                    newValues.put(DatabaseHelper.SYNC_STATE_FIELD,
                                  SYNC_STATE.NOTHING.toInt());
                }
            }
            if (values.containsKey(TaskSyncColumns._DELETED)
                && !values.containsKey(CommonSyncColumns._DIRTY)) {
                newValues
                .put(DatabaseHelper.SYNC_STATE_FIELD,
                     values.getAsBoolean(TaskSyncColumns._DELETED) ? SYNC_STATE.DELETE
                     .toInt() : SYNC_STATE.NOTHING.toInt());
            }
        }
        return newValues;
    }

    private static String getId(final Uri uri) {
        return uri.getPathSegments().get(1);
    }

    private static String getListQuery(final boolean isSyncAdapter) {
        String query = getListQueryBase(isSyncAdapter);
        query += addSegment(ModelBase.ID, TaskListColumns._ID, true);
        query += " FROM " + ListMirakel.TABLE;
        Log.d(TAG, query);
        return query;
    }

    private static String getListQueryBase(final boolean isSyncAdapter) {
        String query = "SELECT ";
        query += addSegment(ModelBase.NAME, TaskListColumns.LIST_NAME,
                            false);
        query += addSegment(ListMirakel.COLOR, TaskListColumns.LIST_COLOR, true);
        if (isSyncAdapter) {
            query += addSegment("CASE " + DatabaseHelper.SYNC_STATE_FIELD
                                + " WHEN " + SYNC_STATE.NEED_SYNC + " THEN 1 ELSE 0 END",
                                CommonSyncColumns._DIRTY, true);
            query += addSegment(ModelBase.ID, TaskColumns._ID, true);
            // query += addSegment("CASE " + SyncAdapter.SYNC_STATE + " WHEN "
            // + SYNC_STATE.DELETE + " THEN 1 ELSE 0 END",
            // TaskLists._DELETED, true);
            // query += addSegment("CASE " + SyncAdapter.SYNC_STATE + " WHEN "
            // + SYNC_STATE.ADD + " THEN 1 ELSE 0 END",
            // TaskLists.IS_NEW, true);
            query += addSegment(ModelBase.ID, CommonSyncColumns._SYNC_ID,
                                true);
        }
        return query;
    }

    private static String getListQuerySpecial() {
        String query = getListQuery(false);
        query += " UNION ";
        query += getListQueryBase(false);
        query += addSegment(ModelBase.ID + "*-1", TaskListColumns._ID,
                            true);
        query += " FROM " + SpecialList.TABLE;
        return query;
    }

    private static String getTaskQuery(final boolean isSyncAdapter) {
        return getTaskQuery(false, 0, isSyncAdapter);
    }

    private static String getTaskQuery(final boolean isSpecial,
                                       final int listId, final boolean isSyncadapter) {
        String query = "SELECT ";
        query += addSegment(Task.TABLE + "." + ModelBase.NAME,
                            TaskColumns.TITLE, false);
        query += addSegment(Task.TABLE + "." + Task.CONTENT,
                            TaskColumns.DESCRIPTION, true);
        query += addSegment(" NULL ", TaskColumns.LOCATION, true);
        query += addSegment(Task.TABLE + "." + Task.DUE + "*1000",
                            TaskColumns.DUE, true);
        query += addSegment("(CASE " + Task.TABLE + "." + Task.DONE
                            + " WHEN 1 THEN 2 ELSE 0 END)", TaskColumns.STATUS, true);
        query += addSegment(Task.TABLE + "." + Task.PROGRESS,
                            TaskColumns.PERCENT_COMPLETE, true);
        if (isSyncadapter) {
            query += addSegment("CASE " + Task.TABLE + "."
                                + DatabaseHelper.SYNC_STATE_FIELD + " WHEN "
                                + SYNC_STATE.NEED_SYNC + " THEN 1 WHEN " + SYNC_STATE.ADD
                                + " THEN 1 ELSE 0 END", CommonSyncColumns._DIRTY, true);
            query += addSegment(Task.TABLE + "." + ModelBase.ID,
                                TaskColumns._ID, true);
            query += addSegment("CASE " + Task.TABLE + "."
                                + DatabaseHelper.SYNC_STATE_FIELD + " WHEN "
                                + SYNC_STATE.DELETE + " THEN 1 ELSE 0 END",
                                TaskSyncColumns._DELETED, true);
            query += addSegment("CASE " + Task.TABLE + "." + Task.PRIORITY
                                + " WHEN 2 THEN 1 WHEN 1 THEN 5 WHEN -1 THEN 9"
                                + " WHEN -2 THEN 9 ELSE 0 END", TaskColumns.PRIORITY, true);
            // query += addSegment("CASE " +
            // Task.TABLE+"."+SyncAdapter.SYNC_STATE + " WHEN "
            // + SYNC_STATE.ADD + " THEN 1 ELSE 0 END",
            // TaskContract.Tasks.IS_NEW, true);
            query += addSegment("caldav_extra.SYNC_ID",
                                CommonSyncColumns._SYNC_ID, true);
            query += addSegment("caldav_extra.ETAG", CommonSyncColumns.SYNC1,
                                true);
            query += addSegment(AccountMirakel.TABLE + "."
                                + ModelBase.NAME, TaskContract.ACCOUNT_NAME, true);
            query += addSegment("caldav_extra.REMOTE_NAME",
                                TaskColumns.LIST_ID, true);
        } else {
            query += addSegment(Task.TABLE + "." + Task.PRIORITY,
                                TaskColumns.PRIORITY, true);
            if (isSpecial) {
                query += addSegment(
                             "CASE " + Task.TABLE + "." + Task.LIST_ID
                             + " WHEN 1 THEN " + listId + " ELSE " + listId
                             + " END", TaskColumns.LIST_ID, true);
            } else {
                query += addSegment(Task.TABLE + "." + Task.LIST_ID,
                                    TaskColumns.LIST_ID, true);
            }
        }
        query += addSegment(Task.TABLE + "." + DatabaseHelper.UPDATED_AT
                            + "*1000", TaskColumns.LAST_MODIFIED, true);
        query += addSegment(Task.TABLE + "." + DatabaseHelper.CREATED_AT
                            + "*1000", TaskColumns.CREATED, true);
        // query += " FROM " + Task.TABLE;
        if (isSyncadapter) {
            query += " FROM (" + Task.TABLE + " inner join "
                     + ListMirakel.TABLE;
            query += " on " + Task.TABLE + "." + Task.LIST_ID + "="
                     + ListMirakel.TABLE + "." + ModelBase.ID + ")";
            query += " inner join " + AccountMirakel.TABLE + " on "
                     + ListMirakel.TABLE + "." + ListMirakel.ACCOUNT_ID;
            query += "=" + AccountMirakel.TABLE + "." + ModelBase.ID;
            query += " LEFT JOIN caldav_extra ON " + Task.TABLE + "."
                     + ModelBase.ID + "=caldav_extra." + ModelBase.ID;
        } else {
            query += " FROM " + Task.TABLE;
        }
        return query;
    }

    private static String handleListID(final String selection,
                                       final boolean isSyncAdapter, String taskQuery) throws SQLWarning {
        final String[] t = selection.split(TaskColumns.LIST_ID);
        if (t.length < 2) {
            return taskQuery;
        }
        boolean not;
        try {
            not = "not".equalsIgnoreCase(t[0].trim().substring(
                                             t[0].trim().length() - 3));
        } catch (final Exception e) {
            not = false;
        }
        if (t[1].trim().charAt(0) == '=') {
            taskQuery = handleListIDEqual(isSyncAdapter, taskQuery, t, not);
        } else {
            taskQuery = handleListIDIn(isSyncAdapter, taskQuery, t, not);
        }
        return taskQuery;
    }

    private static String handleListIDEqual(final boolean isSyncAdapter,
                                            String taskQuery, final String[] t, final boolean not)
    throws SQLWarning {
        t[1] = t[1].trim().substring(1);
        int listId = 0;
        try {
            final boolean negative = t[1].trim().charAt(0) == '-';
            final Matcher matcher = Pattern.compile("\\d+").matcher(t[1]);
            matcher.find();
            listId = (negative ? -1 : 1) * Integer.valueOf(matcher.group());
        } catch (final Exception e) {
            Log.e(TAG, "cannot parse list_id");
            throw new SQLWarning();
        }
        // is special list...
        if (listId < 0) {
            final SpecialList s = SpecialList.get(-1 * listId);
            if (s != null) {
                taskQuery = getTaskQuery(true, not ? 0 : listId, isSyncAdapter);
                if (s.getWhereQueryForTasks() != null
                    && s.getWhereQueryForTasks().trim().length() != 0) {
                    taskQuery += " WHERE " + (not ? "NOT ( " : "")
                                 + s.getWhereQueryForTasks() + (not ? " )" : "");
                }
            } else {
                Log.e(TAG, "no matching list found");
                throw new SQLWarning();
            }
        }
        return taskQuery;
    }

    private static String handleListIDIn(final boolean isSyncAdapter,
                                         String taskQuery, final String[] t, final boolean not)
    throws SQLWarning {
        if ("in".equalsIgnoreCase(t[1].trim().substring(0, 2))) {
            t[1] = t[1].trim().substring(3).trim();
            int counter = 1;
            String buffer = "";
            final List<Integer> idList = new ArrayList<Integer>();
            while (t[1].charAt(counter) >= '0' && t[1].charAt(counter) <= '9'
                   || t[1].charAt(counter) == ','
                   || t[1].charAt(counter) == ' '
                   || t[1].charAt(counter) == '-') {
                if (t[1].charAt(counter) == ',') {
                    try {
                        idList.add(Integer.parseInt(buffer));
                        buffer = "";
                    } catch (final NumberFormatException e) {
                        Log.e(TAG, "cannot parse list id");
                        throw new SQLWarning();
                    }
                } else if (t[1].charAt(counter) >= '0'
                           && t[1].charAt(counter) <= '9'
                           || t[1].charAt(counter) == '-') {
                    buffer += t[1].charAt(counter);
                }
                ++counter;
            }
            try {
                idList.add(Integer.parseInt(buffer));
            } catch (final NumberFormatException e) {
                Log.e(TAG, "cannot parse list id");
                throw new SQLWarning();
            }
            if (idList.isEmpty()) {
                Log.e(TAG, "inavlid SQL");
                throw new SQLWarning();
            }
            final List<String> wheres = new ArrayList<String>();
            final List<Integer> ordonaryIds = new ArrayList<Integer>();
            for (final int id : idList) {
                if (id < 0) {
                    final SpecialList s = SpecialList.get(-1 * id);
                    if (s != null) {
                        wheres.add(s.getWhereQueryForTasks());
                    } else {
                        Log.e(TAG, "no matching list found");
                        throw new SQLWarning();
                    }
                } else {
                    ordonaryIds.add(id);
                }
            }
            taskQuery = getTaskQuery(true, not ? 0 : idList.get(0),
                                     isSyncAdapter) + " WHERE " + (not ? " NOT (" : "");
            for (int i = 0; i < wheres.size(); i++) {
                taskQuery += (i != 0 ? " AND " : " ") + wheres.get(i);
            }
            if (!ordonaryIds.isEmpty()) {
                if (!wheres.isEmpty()) {
                    taskQuery += " OR ";
                }
                taskQuery += Task.LIST_ID + " IN (";
                for (int i = 0; i < ordonaryIds.size(); i++) {
                    taskQuery += (i != 0 ? "," : "") + ordonaryIds.get(i);
                }
                taskQuery += ")";
            }
            taskQuery += not ? ")" : "";
        }
        return taskQuery;
    }

    private static String insertSelectionArgs(String selection,
            final String[] selectionArgs) {
        if (selectionArgs != null) {
            for (final String selectionArg : selectionArgs) {
                selection = selection.replace("?", selectionArg);
            }
        }
        return selection;
    }

    // public static final String PROVIDER_NAME = Mirakel.AUTHORITY_TYP;
    // public static final Uri CONTENT_URI = Uri.parse("content://" +
    // PROVIDER_NAME);

    private long createNewList(final Uri uri) {
        final String name = getContext().getString(R.string.inbox);
        final AccountMirakel a = AccountMirakel.getByName(getAccountName(uri));
        if (a == null) {
            throw new IllegalArgumentException("Unkown account");
        }
        final Cursor c = MirakelContentProvider.openHelper
                         .getWritableDatabase().query(
                             ListMirakel.TABLE,
                             new String[] { ModelBase.ID },
                             ModelBase.NAME + "='" + name + "' and "
                             + ListMirakel.ACCOUNT_ID + "=" + a.getId(),
                             null, null, null, null);
        ListMirakel l;
        if (c.getCount() < 1) {
            c.close();
            l = ListMirakel.newList(name);
            l.setAccount(a);
            l.save(false);
            return l.getId();
        }
        c.moveToFirst();
        final int id = c.getInt(0);
        c.close();
        return id;
    }

    @Override
    public int delete(final Uri uri, final String selection,
                      final String[] selectionArgs) {
        return 0;
    }

    protected static String getAccountName(final Uri uri) {
        return uri.getQueryParameter(TaskContract.ACCOUNT_NAME);
    }

    protected static String getAccountType(final Uri uri) {
        return uri.getQueryParameter(TaskContract.ACCOUNT_TYPE);
    }

    private String getIdsFromSelection(final Uri uri, final String selection,
                                       final String[] selectionArgs, final boolean isList) {
        final Cursor c = query(uri, new String[] { isList ? TaskListColumns._ID
                               : TaskColumns._ID
                                                 }, selection, selectionArgs, null);
        String s = "";
        if (c.getCount() > 0 && c.moveToFirst()) {
            while (!c.isAfterLast()) {
                s += ("".equals(s) ? "" : ",") + c.getInt(0);
                c.moveToNext();
            }
        } else {
            c.close();
            throw new RuntimeException("id not found");
        }
        c.close();
        return s;
    }

    @Override
    public String getType(final Uri uri) {
        switch (URI_MATCHER.match(uri)) {
        case LISTS:
            return TaskContract.TaskLists.CONTENT_TYPE;
        case LIST_ID:
            return TaskContract.TaskLists.CONTENT_ITEM_TYPE;
        case TASKS:
            return TaskContract.Tasks.CONTENT_TYPE;
        case TASK_ID:
            return TaskContract.Tasks.CONTENT_ITEM_TYPE;
        case INSTANCES:
            return TaskContract.Instances.CONTENT_TYPE;
        default:
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        return null;
    }

    protected static boolean isCallerSyncAdapter(final Uri uri) {
        final String param = uri
                             .getQueryParameter(TaskContract.CALLER_IS_SYNCADAPTER);
        return param != null && !"false".equals(param);
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private static Cursor listQuery(final String[] projection,
                                    final String selection, final String sortOrder,
                                    final SQLiteQueryBuilder sqlBuilder, final boolean isSyncAdapter,
                                    final boolean hasId, final String id) {
        String listQuery;
        if ("1=1".equals(selection)) {
            listQuery = getListQuerySpecial();
        } else {
            listQuery = getListQuery(isSyncAdapter);
        }
        if (hasId) {
            listQuery += "WHERE " + TaskListColumns._ID + "=" + id;
        }
        sqlBuilder.setTables("(" + listQuery + ")");
        String query;
        if (Build.VERSION.SDK_INT >= 11) {
            query = sqlBuilder.buildQuery(projection, selection, null, null,
                                          sortOrder, null);
        } else {
            query = sqlBuilder.buildQuery(projection, selection, null, null,
                                          sortOrder, null, null);
        }
        Log.d(TAG, query);
        return MirakelContentProvider.openHelper.getReadableDatabase()
               .rawQuery(query, null);
    }

    @Override
    public void onAccountsUpdated(final Account[] accounts) {
        AccountMirakel.update(accounts);
    }

    public static SQLiteDatabase getReadableDatabase() {
        return openHelper.getReadableDatabase();
    }

    public static SQLiteDatabase getWritableDatabase() {
        return openHelper.getWritableDatabase();
    }

    @Override
    public boolean onCreate() {
        // this.database = new
        // DatabaseHelper(getContext()).getWritableDatabase();
        init(getContext());
        // register for account updates and check immediately
        AccountManager.get(getContext()).addOnAccountsUpdatedListener(this,
                null, true);
        return MirakelContentProvider.openHelper == null;
    }

    public static void init(final Context ctx) {
        openHelper = new DatabaseHelper(ctx);
        openHelper.getWritableDatabase().execSQL("PRAGMA foreign_keys=ON;");
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection,
                        String selection, final String[] selectionArgs,
                        final String sortOrder) {
        return new MatrixCursor(projection);
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private static Cursor taskQuery(final String[] projection,
                                    final String selection, final String sortOrder,
                                    final SQLiteQueryBuilder sqlBuilder, final boolean isSyncAdapter,
                                    final Uri uri, final String id, final boolean hasID) {
        String taskQuery = getTaskQuery(isSyncAdapter);
        if (isSyncAdapter) {
            taskQuery += " WHERE " + AccountMirakel.TABLE + "."
                         + ModelBase.NAME + "='" + getAccountName(uri) + "' ";
        }
        if (hasID) {
            taskQuery += (isSyncAdapter ? " AND " : " WHERE ") + Task.TABLE
                         + "." + ModelBase.ID + "=" + id;
        }
        if (selection != null && selection.contains(TaskColumns.LIST_ID)
            && !isSyncAdapter) {
            try {
                taskQuery = handleListID(selection, isSyncAdapter, taskQuery);
            } catch (final SQLWarning s) {
                return new MatrixCursor(projection);
            }
        }
        sqlBuilder.setTables("(" + taskQuery + ")");
        String query;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            query = sqlBuilder.buildQuery(projection, selection, null, null,
                                          null, sortOrder, null);
        } else {
            query = sqlBuilder.buildQuery(projection, selection, null, null,
                                          sortOrder, null);
        }
        Log.d(TAG, query);
        return MirakelContentProvider.openHelper.getReadableDatabase()
               .rawQuery(query, null);
    }

    @Override
    public int update(final Uri uri, final ContentValues values,
                      final String selection, final String[] selectionArgs) {
        return 0;
    }

    private static int updateTask(final Uri uri, final ContentValues newValues,
                                  final boolean hasExtras, final ContentValues extras) {
        int count = 0;
        if (newValues.size() > 0) {
            count = MirakelContentProvider.openHelper.getWritableDatabase()
                    .update(Task.TABLE, newValues,
                            ModelBase.ID + "=" + getId(uri), null);
        }
        if (hasExtras && extras.size() > 0) {
            count = MirakelContentProvider.openHelper.getWritableDatabase()
                    .update("caldav_extra", extras,
                            ModelBase.ID + "=" + getId(uri), null);
            if (count != 1) {
                extras.put(ModelBase.ID, Integer.parseInt(getId(uri)));
                MirakelContentProvider.openHelper.getWritableDatabase().insert(
                    "caldav_extra", null, extras);
            }
        }
        return count;
    }

}
