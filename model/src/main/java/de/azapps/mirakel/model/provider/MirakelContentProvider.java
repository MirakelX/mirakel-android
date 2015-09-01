/*
 * Copyright (C) 2012 Marten Gajda <marten@dmfs.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.azapps.mirakel.model.provider;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.Time;

import org.dmfs.provider.tasks.Duration;
import org.dmfs.provider.tasks.TaskContract;
import org.dmfs.provider.tasks.TaskContract.Alarms;
import org.dmfs.provider.tasks.TaskContract.Categories;
import org.dmfs.provider.tasks.TaskContract.CategoriesColumns;
import org.dmfs.provider.tasks.TaskContract.CommonSyncColumns;
import org.dmfs.provider.tasks.TaskContract.Instances;
import org.dmfs.provider.tasks.TaskContract.Properties;
import org.dmfs.provider.tasks.TaskContract.PropertyColumns;
import org.dmfs.provider.tasks.TaskContract.TaskColumns;
import org.dmfs.provider.tasks.TaskContract.TaskListColumns;
import org.dmfs.provider.tasks.TaskContract.TaskListSyncColumns;
import org.dmfs.provider.tasks.TaskContract.TaskLists;
import org.dmfs.provider.tasks.TaskContract.TaskSyncColumns;
import org.dmfs.provider.tasks.TaskContract.Tasks;
import org.dmfs.provider.tasks.handler.CaldavDatabaseHelper;
import org.dmfs.provider.tasks.handler.PropertyHandler;
import org.dmfs.provider.tasks.handler.PropertyHandlerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.tools.Log;

public class MirakelContentProvider extends SQLiteContentProvider {

    private static final int LISTS = 1;
    private static final int LIST_ID = 2;
    private static final int TASKS = 101;
    private static final int TASK_ID = 102;
    private static final int INSTANCES = 103;
    private static final int INSTANCE_ID = 104;
    private static final int CATEGORIES = 1001;
    private static final int CATEGORY_ID = 1002;
    private static final int PROPERTIES = 1003;
    private static final int PROPERTY_ID = 1004;
    private static final int ALARMS = 1005;
    private static final int ALARM_ID = 1006;

    private static final UriMatcher uriMatcher;

    private static final String[] TASK_ID_PROJECTION = { Tasks._ID };
    private static final String[] TASK_SYNC_ID_PROJECTION = { Tasks._SYNC_ID };
    private static final String[] TASKLIST_ID_PROJECTION = { TaskLists._ID };

    private static final String SYNC_ID_SELECTION = Tasks._SYNC_ID + "=?";
    private static final String TASK_ID_SELECTION = Tasks._ID + "=?";
    private static final String TASKLISTS_ID_SELECTION = TaskLists._ID + "=?";

    private final static Set<String> TASK_LIST_SYNC_COLUMNS = new HashSet<String>(Arrays.asList(
                TaskLists.SYNC_ADAPTER_COLUMNS));

    /**
     * A helper to check {@link Integer} values for equality with <code>1</code>. You can use it like
     *
     * <pre>
     * ONE.equals(someLong)
     * </pre>
     *
     * which is shorter and less error prone (you can't forget the <code>null</code> check with the method above) than
     *
     * <pre>
     * someLong != null &amp;&amp; someLong == 1
     * </pre>
     */
    private final static Integer ONE = 1;

    /**
     * The task database helper that provides access to the actual database.
     */
    private DatabaseHelper mDBHelper;


    /**
     * Return true if the caller is a sync adapter (i.e. if the Uri contains the query parameter {@link TaskContract#CALLER_IS_SYNCADAPTER} and its value is
     * true).
     *
     * @param uri
     * The {@link Uri} to check.
     * @return <code>true</code> if the caller pretends to be a sync adapter, <code>false</code> otherwise.
     */
    @Override
    public boolean isCallerSyncAdapter(Uri uri) {
        String param = uri.getQueryParameter(TaskContract.CALLER_IS_SYNCADAPTER);
        return param != null && !"false".equals(param);
    }


    /**
     * Return true if the URI indicates to a load extended properties with {@link TaskContract#LOAD_PROPERTIES}.
     *
     * @param uri
     * The {@link Uri} to check.
     * @return <code>true</code> if the URI requests to load extended properties, <code>false</code> otherwise.
     */
    public boolean shouldLoadProperties(Uri uri) {
        String param = uri.getQueryParameter(TaskContract.LOAD_PROPERTIES);
        return param != null && !"false".equals(param);
    }


    /**
     * Get the account name from the given {@link Uri}.
     *
     * @param uri
     * The Uri to check.
     * @return The account name or null if no account name has been specified.
     */
    protected String getAccountName(Uri uri) {
        return uri.getQueryParameter(TaskContract.ACCOUNT_NAME);
    }


    /**
     * Get the account type from the given {@link Uri}.
     *
     * @param uri
     * The Uri to check.
     * @return The account type or null if no account type has been specified.
     */
    protected String getAccountType(Uri uri) {
        String accountType = uri.getQueryParameter(TaskContract.ACCOUNT_TYPE);
        return accountType;
    }


    /**
     * Get any id from the given {@link Uri}.
     *
     * @param uri
     * The Uri.
     * @return The last path segment (which should contain the id).
     */
    public static String getId(Uri uri) {
        return uri.getPathSegments().get(1);
    }


    /**
     * Build a selection string that selects the account specified in <code>uri</code>.
     *
     * @param uri
     * A {@link Uri} that specifies an account.
     * @return A {@link StringBuilder} with a selection string for the account.
     */
    protected StringBuilder selectAccount(Uri uri) {
        StringBuilder sb = new StringBuilder(256);
        return selectAccount(sb, uri);
    }


    /**
     * Append the selection of the account specified in <code>uri</code> to the {@link StringBuilder} <code>sb</code>.
     *
     * @param sb
     * A {@link StringBuilder} that the selection is appended to.
     * @param uri
     * A {@link Uri} that specifies an account.
     * @return <code>sb</code>.
     */
    protected StringBuilder selectAccount(StringBuilder sb, Uri uri) {
        String accountName = getAccountName(uri);
        String accountType = getAccountType(uri);
        if (accountName != null || accountType != null) {
            if (accountName != null) {
                if (sb.length() > 0) {
                    sb.append(" AND ");
                }
                sb.append(TaskListSyncColumns.ACCOUNT_NAME);
                sb.append("=");
                DatabaseUtils.appendEscapedSQLString(sb, accountName);
            }
            if (accountType != null) {
                if (sb.length() > 0) {
                    sb.append(" AND ");
                }
                sb.append(TaskListSyncColumns.ACCOUNT_TYPE);
                sb.append("=");
                DatabaseUtils.appendEscapedSQLString(sb, accountType);
            }
        }
        return sb;
    }

    private StringBuilder _selectId(StringBuilder sb, String id, String key) {
        if (sb.length() > 0) {
            sb.append(" AND ");
        }
        sb.append(key);
        sb.append("=");
        sb.append(id);
        return sb;
    }


    protected StringBuilder selectId(Uri uri) {
        StringBuilder sb = new StringBuilder(128);
        return selectId(sb, uri);
    }


    protected StringBuilder selectId(StringBuilder sb, Uri uri) {
        return _selectId(sb, getId(uri), TaskListColumns._ID);
    }


    protected StringBuilder selectTaskId(Uri uri) {
        StringBuilder sb = new StringBuilder(128);
        return selectTaskId(sb, uri);
    }


    protected StringBuilder selectTaskId(String id) {
        StringBuilder sb = new StringBuilder(128);
        return selectTaskId(sb, id);
    }


    protected StringBuilder selectTaskId(StringBuilder sb, Uri uri) {
        return selectTaskId(sb, getId(uri));
    }


    protected StringBuilder selectTaskId(StringBuilder sb, String id) {
        return _selectId(sb, id, Instances.TASK_ID);
    }


    protected StringBuilder selectPropertyId(Uri uri) {
        StringBuilder sb = new StringBuilder(128);
        return selectPropertyId(sb, uri);
    }


    protected StringBuilder selectPropertyId(StringBuilder sb, Uri uri) {
        return _selectId(sb, getId(uri), PropertyColumns.PROPERTY_ID);
    }


    /**
     * Add a selection by ID to the given {@link SQLiteQueryBuilder}. The id is taken from the given Uri.
     *
     * @param stringBuilder
     * The {@link SQLiteQueryBuilder} to append the selection to.
     * @param idColumn
     * The column that must match the id.
     * @param uri
     * An {@link Uri} that contains the id.
     */
    protected void selectId(StringBuilder stringBuilder, String idColumn, Uri uri) {
        stringBuilder.append(" AND ");
        stringBuilder.append(idColumn);
        stringBuilder.append("=");
        DatabaseUtils.appendEscapedSQLString(stringBuilder, getId(uri));
    }


    /**
     * Append any arbitrary selection string to the selection in <code>sb</code>
     *
     * @param sb
     * A {@link StringBuilder} that already contains a selection string.
     * @param selection
     * A valid SQL selection string.
     * @return A string with the final selection.
     */
    protected String updateSelection(StringBuilder sb, String selection) {
        if (selection != null) {
            if (sb.length() > 0) {
                sb.append("AND ( ").append(selection).append(" ) ");
            } else {
                sb.append(" ( ").append(selection).append(" ) ");
            }
        }
        return sb.toString();
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        final ContentResolver db = CaldavDatabaseHelper.getContentProvider(getContext());
        StringBuilder stringBuilder = new StringBuilder(selection == null ? "" : selection);
// initialize appendWhere, this allows us to append all other selections with a preceding "AND"
        if (stringBuilder.length() == 0) {
            stringBuilder.append(" 1=1 ");
        }
        boolean isSyncAdapter = isCallerSyncAdapter(uri);
        Uri newUri;
        switch (uriMatcher.match(uri)) {
        case LISTS:
// add account to selection if any
            selectAccount(stringBuilder, uri);
            newUri = CaldavDatabaseHelper.getListsUri();
            if (sortOrder == null || sortOrder.length() == 0) {
                sortOrder = TaskContract.TaskLists.DEFAULT_SORT_ORDER;
            }
            break;
        case LIST_ID:
// add account to selection if any
            selectAccount(stringBuilder, uri);
            newUri = CaldavDatabaseHelper.getListsUri();
            selectId(stringBuilder, TaskListColumns._ID, uri);
            if (sortOrder == null || sortOrder.length() == 0) {
                sortOrder = TaskContract.TaskLists.DEFAULT_SORT_ORDER;
            }
            break;
        case TASKS:
            if (shouldLoadProperties(uri)) {
// extended properties were requested, therefore change to task view that includes these properties
                newUri = CaldavDatabaseHelper.getTasksUri(true);
            } else {
                newUri = CaldavDatabaseHelper.getTasksUri(false);
            }
            if (!isSyncAdapter) {
// do not return deleted rows if caller is not a sync adapter
                stringBuilder.append(" AND ");
                stringBuilder.append(Tasks._DELETED);
                stringBuilder.append("=0");
            }
            if (sortOrder == null || sortOrder.length() == 0) {
                sortOrder = TaskContract.Tasks.DEFAULT_SORT_ORDER;
            }
            break;
        case TASK_ID:
            if (shouldLoadProperties(uri)) {
// extended properties were requested, therefore change to task view that includes these properties
                newUri = CaldavDatabaseHelper.getTasksUri(true);
            } else {
                newUri = CaldavDatabaseHelper.getTasksUri(false);
            }
            selectId(stringBuilder, TaskColumns._ID, uri);
            if (!isSyncAdapter) {
// do not return deleted rows if caller is not a sync adapter
                stringBuilder.append(" AND ");
                stringBuilder.append(Tasks._DELETED);
                stringBuilder.append("=0");
            }
            if (sortOrder == null || sortOrder.length() == 0) {
                sortOrder = TaskContract.Tasks.DEFAULT_SORT_ORDER;
            }
            break;
        case INSTANCES:
            if (shouldLoadProperties(uri)) {
// extended properties were requested, therefore change to instance view that includes these properties
                newUri = CaldavDatabaseHelper.getInstancesUri(true);
            } else {
                newUri = CaldavDatabaseHelper.getInstancesUri(false);
            }
            if (!isSyncAdapter) {
// do not return deleted rows if caller is not a sync adapter
                stringBuilder.append(" AND ");
                stringBuilder.append(Tasks._DELETED);
                stringBuilder.append("=0");
            }
            if (sortOrder == null || sortOrder.length() == 0) {
                sortOrder = TaskContract.Instances.DEFAULT_SORT_ORDER;
            }
            break;
        case INSTANCE_ID:
            if (shouldLoadProperties(uri)) {
// extended properties were requested, therefore change to instance view that includes these properties
                newUri = CaldavDatabaseHelper.getInstancesUri(true);
            } else {
                newUri = CaldavDatabaseHelper.getInstancesUri(false);
            }
            selectId(stringBuilder, Instances._ID, uri);
            if (!isSyncAdapter) {
// do not return deleted rows if caller is not a sync adapter
                stringBuilder.append(" AND ");
                stringBuilder.append(Tasks._DELETED);
                stringBuilder.append("=0");
            }
            if (sortOrder == null || sortOrder.length() == 0) {
                sortOrder = TaskContract.Instances.DEFAULT_SORT_ORDER;
            }
            break;
        case CATEGORIES:
            selectAccount(stringBuilder, uri);
            newUri = CaldavDatabaseHelper.getCategoriesUri();
            if (sortOrder == null || sortOrder.length() == 0) {
                sortOrder = TaskContract.Categories.DEFAULT_SORT_ORDER;
            }
            break;
        case CATEGORY_ID:
            selectAccount(stringBuilder, uri);
            newUri = CaldavDatabaseHelper.getCategoriesUri();
            selectId(stringBuilder, CategoriesColumns._ID, uri);
            if (sortOrder == null || sortOrder.length() == 0) {
                sortOrder = TaskContract.Categories.DEFAULT_SORT_ORDER;
            }
            break;
        case PROPERTIES:
            newUri = CaldavDatabaseHelper.getPropertiesUri();
            break;
        case PROPERTY_ID:
            newUri = CaldavDatabaseHelper.getPropertiesUri();
            selectId(stringBuilder, PropertyColumns.PROPERTY_ID, uri);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return db.query(newUri, projection, stringBuilder.toString(), selectionArgs, sortOrder);
    }


    @Override
    public int deleteInTransaction(Uri uri, String selection, String[] selectionArgs,
                                   boolean isSyncAdapter) {
        final ContentResolver db = CaldavDatabaseHelper.getContentProvider(getContext());
        int count = 0;
        String accountName = getAccountName(uri);
        String accountType = getAccountType(uri);
        switch (uriMatcher.match(uri)) {
        /*
        * Deleting task lists is only allowed to sync adapters. They must provide ACCOUNT_NAME and ACCOUNT_TYPE.
        */
        case LIST_ID:
// add _id to selection and fall through
            selection = updateSelection(selectId(uri), selection);
        case LISTS:
            if (isSyncAdapter) {
                if (TextUtils.isEmpty(accountType) || TextUtils.isEmpty(accountName)) {
                    throw new IllegalArgumentException("Sync adapters must specify an account and account type: " +
                                                       uri);
                }
                selection = updateSelection(selectAccount(uri), selection);
                count = db.delete(CaldavDatabaseHelper.getListsUri(), selection, selectionArgs);
            } else {
                throw new UnsupportedOperationException("Caller must be a sync adapter to delete task lists");
            }
            break;
        /*
        * Task won't be removed, just marked as deleted if the caller isn't a sync adapter. Sync adapters can remove tasks immediately.
        */
        case TASK_ID:
// add id to selection and fall through
            selection = updateSelection(selectId(uri), selection);
        case TASKS:
// TODO: filter by account name and type if present in uri.
            if (isSyncAdapter) {
                if (TextUtils.isEmpty(accountType) || TextUtils.isEmpty(accountName)) {
                    throw new IllegalArgumentException("Sync adapters must specify an account and account type: " +
                                                       uri);
                }
// only sync adapters can delete tasks
                count = db.delete(CaldavDatabaseHelper.getTasksUri(false), selection, selectionArgs);
            } else {
// mark task as deleted and dirty, the sync adapter will remove it later
                ContentValues values = new ContentValues();
                values.put(TaskSyncColumns._DELETED, true);
                values.put(CommonSyncColumns._DIRTY, true);
                count = db.update(CaldavDatabaseHelper.getTasksUri(false), values, selection, selectionArgs);
            }
            break;
        case ALARM_ID:
// add id to selection and fall through
            selection = updateSelection(selectId(uri), selection);
        case ALARMS:
            count = db.delete(CaldavDatabaseHelper.getAlarmsUri(), selection, selectionArgs);
            break;
        case PROPERTY_ID:
            String[] queryProjection = { Properties.MIMETYPE };
            selection = updateSelection(selectPropertyId(uri), selection);
            Cursor cursor = db.query(CaldavDatabaseHelper.getPropertiesUri(), queryProjection, selection,
                                     selectionArgs, null);
            String mimeType = null;
            try {
                if (cursor.moveToFirst()) {
                    mimeType = cursor.getString(0);
                } else if (new MirakelQueryBuilder(getContext())
                           .and(ModelBase.ID, Operation.EQ, ContentUris.parseId(uri)).count(Tag.URI) > 0) {
                    mimeType = TaskContract.Property.Category.CONTENT_ITEM_TYPE;
                }
            } finally {
                cursor.close();
            }
            if (mimeType != null) {
                PropertyHandler handler = PropertyHandlerFactory.create(mimeType);
                count = handler.delete(db, selection, selectionArgs, isSyncAdapter);
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (count > 0) {
            postNotifyUri(uri);
            postNotifyUri(Instances.CONTENT_URI);
            postNotifyUri(Tasks.CONTENT_URI);
        }
        return count;
    }


    @Override
    public Uri insertInTransaction(Uri uri, ContentValues values, boolean isSyncAdapter) {
        final ContentResolver db = CaldavDatabaseHelper.getContentProvider(getContext());
        Uri result_uri;
        String accountName = getAccountName(uri);
        String accountType = getAccountType(uri);
        switch (uriMatcher.match(uri)) {
        case LISTS:
            if (isSyncAdapter) {
                validateTaskListValues(values, true, isSyncAdapter);
// only sync adapter can create task lists!
                if (TextUtils.isEmpty(accountType) || TextUtils.isEmpty(accountName)) {
                    throw new IllegalArgumentException("Sync adapters must specify an account name and an account type: "
                                                       + uri);
                }
                values.put(TaskContract.ACCOUNT_NAME, accountName);
                values.put(TaskContract.ACCOUNT_TYPE, accountType);
                result_uri = db.insert(CaldavDatabaseHelper.getListsUri(), values);
            } else {
                throw new UnsupportedOperationException("Caller must be a sync adapter to create task lists");
            }
            break;
        case TASKS:
            validateTaskValues(db, values, true, isSyncAdapter);
            if (!isSyncAdapter) {
// new tasks are always dirty
                values.put(CommonSyncColumns._DIRTY, true);
// set creation time and last modified
                long currentMillis = System.currentTimeMillis();
                values.put(TaskColumns.CREATED, currentMillis);
                values.put(TaskColumns.LAST_MODIFIED, currentMillis);
            }
// insert task
            result_uri = db.insert(CaldavDatabaseHelper.getTasksUri(false), values);
// add entries to Instances
            createInstances(db, uri, values, result_uri);
            break;
        case PROPERTIES:
            PropertyHandler handler = PropertyHandlerFactory.create(values.getAsString(Properties.MIMETYPE));
            result_uri = handler.insert(db, values, isSyncAdapter, getContext());
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (result_uri != null) {
            return result_uri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public int updateInTransaction(Uri uri, ContentValues values, String selection,
                                   String[] selectionArgs, boolean isSyncAdapter) {
        final ContentResolver db = CaldavDatabaseHelper.getContentProvider(getContext());
        int count = 0;
        switch (uriMatcher.match(uri)) {
        case LISTS:
            validateTaskListValues(values, false, isSyncAdapter);
            count = db.update(CaldavDatabaseHelper.getListsUri(), values, selection, selectionArgs);
            break;
        case LIST_ID:
            String newListSelection = updateSelection(selectId(uri), selection);
            validateTaskListValues(values, false, isSyncAdapter);
            count = db.update(CaldavDatabaseHelper.getListsUri(), values, newListSelection, selectionArgs);
            break;
        case TASKS:
// validate tasks
            validateTaskValues(db, values, false, isSyncAdapter);
            if (!isSyncAdapter) {
// mark task as dirty
                values.put(CommonSyncColumns._DIRTY, true);
                values.put(TaskColumns.LAST_MODIFIED, System.currentTimeMillis());
            }
// perform updates
            count = db.update(CaldavDatabaseHelper.getTasksUri(false), values, selection, selectionArgs);
// update related instances
            updateInstancesOfAllTasks(db, values, selection, selectionArgs);
            break;
        case TASK_ID:
            String newSelection = updateSelection(selectId(uri), selection);
            validateTaskValues(db, values, false, isSyncAdapter);
            if (!isSyncAdapter) {
// mark task as dirty
                values.put(CommonSyncColumns._DIRTY, true);
                values.put(TaskColumns.LAST_MODIFIED, System.currentTimeMillis());
            }
            count = db.update(CaldavDatabaseHelper.getTasksUri(false), values, newSelection, selectionArgs);
            String taskSelection = updateSelection(selectTaskId(uri), selection).toString();
            updateInstancesOfOneTask(db, getId(uri), values, taskSelection, selectionArgs);
            break;
        case PROPERTY_ID:
            if (!values.containsKey(Properties.PROPERTY_ID)) {
                throw new IllegalArgumentException("PROPERTY_ID is required on UPDATE");
            }
            String newPropertySelection = updateSelection(selectId(uri), selection);
// query existing property to check mimetype
            Cursor cursor = db.query(CaldavDatabaseHelper.getPropertiesUri(), new String[] { Properties.MIMETYPE },
                                     values.getAsString(Properties.PROPERTY_ID) + " = "
                                     + Properties.PROPERTY_ID, null, null);
            try {
                if (cursor.moveToFirst()) {
// create handler from found mimetype
                    PropertyHandler handler = PropertyHandlerFactory.create(cursor.getString(0));
                    count = handler.update(db, values, newPropertySelection, selectionArgs, isSyncAdapter,
                                           getContext());
                    if (count > 0) {
                        postNotifyUri(Tasks.CONTENT_URI);
                        postNotifyUri(Instances.CONTENT_URI);
                    }
                }
            } finally {
                cursor.close();
            }
            break;
        case CATEGORY_ID:
            String newCategorySelection = updateSelection(selectId(uri), selection);
            validateCategoryValues(values, false, isSyncAdapter);
            count = db.update(CaldavDatabaseHelper.getCategoriesUri(), values, newCategorySelection,
                              selectionArgs);
            break;
        case ALARM_ID:
            String newAlarmSelection = updateSelection(selectId(uri), selection);
            validateAlarmValues(values, false, isSyncAdapter);
            count = db.update(CaldavDatabaseHelper.getAlarmsUri(), values, newAlarmSelection, selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
// get the keys in values
        Set<String> keys;
        if (android.os.Build.VERSION.SDK_INT < 11) {
            keys = new HashSet<String>();
            for (Map.Entry<String, Object> entry : values.valueSet()) {
                keys.add(entry.getKey());
            }
        } else {
            keys = values.keySet();
        }
        if (!TASK_LIST_SYNC_COLUMNS.containsAll(keys)) {
// send notifications, because non-sync columns have been updated
            postNotifyUri(uri);
        }
        return count;
    }


    /**
     * Create new {@link ContentValues} for insertion into the instances table and initialize dates & times with task {@link ContentValues}.
     *
     * @param values
     * {@link ContentValues} of a task.
     * @return {@link ContentValues} of the instance of this task.
     */
    private ContentValues setInstanceTimes(ContentValues values) {
        ContentValues instanceValues = new ContentValues();
// get the relevant values from values
        Long dtstart = values.getAsLong(Tasks.DTSTART);
        Long due = values.getAsLong(Tasks.DUE);
        String durationStr = values.getAsString(Tasks.DURATION);
        if (values.containsKey(TaskColumns.DTSTART)) {
// copy dtstart as is
            instanceValues.put(Instances.INSTANCE_START, dtstart);
        }
        if (values.containsKey(TaskColumns.DUE)) {
// copy due and calculate the actual duration, if any
            instanceValues.put(Instances.INSTANCE_DUE, due);
            if (dtstart != null && due != null) {
                Long instanceDuration = due - dtstart;
                instanceValues.put(Instances.INSTANCE_DURATION, instanceDuration);
            } else {
                instanceValues.putNull(Instances.INSTANCE_DURATION);
            }
        }
        if (values.containsKey(TaskColumns.DURATION) &&
            due == null) { // actually due and duration should not be set at the same time
            if (durationStr != null && dtstart != null) {
// calculate the actual due value from dtstart and the duration string
                Duration duration = new Duration(durationStr);
                Time tStart = new Time(values.getAsString(Tasks.TZ));
                Boolean isAllDay = values.getAsBoolean(Tasks.IS_ALLDAY);
                if (isAllDay != null) {
                    tStart.allDay = isAllDay;
                }
                tStart.set(dtstart);
                Long instanceDue = duration.addTo(tStart).toMillis(false);
                instanceValues.put(Instances.INSTANCE_DUE, instanceDue);
// actual duration is the difference between due and dtstart
                Long instanceDuration = instanceDue - dtstart;
                instanceValues.put(Instances.INSTANCE_DURATION, instanceDuration);
            } else {
                instanceValues.putNull(Instances.INSTANCE_DURATION);
                instanceValues.putNull(Instances.INSTANCE_DUE);
            }
        }
        return instanceValues;
    }


    /**
     * Creates new instances for the given task {@link ContentValues}.
     * <p>
     * TODO: expand recurrence
     * </p>
     *
     * @param uri
     * The {@link Uri} used when inserting the task.
     * @param values
     * The {@link ContentValues} of the task.
     * @param taskUri
     * The new {@link Uri} of the task.
     */
    private void createInstances(ContentResolver db, Uri uri, ContentValues values,
                                 Uri taskUri) {
        ContentValues instanceValues = setInstanceTimes(values);
// set rowID of current Task
        instanceValues.put(Instances.TASK_ID, getId(taskUri));
        String tz = values.getAsString(Instances.TZ);
        boolean allday = values.getAsInteger(Tasks.IS_ALLDAY) != null &&
                         values.getAsInteger(Tasks.IS_ALLDAY) > 0;
// add start sorting if start value is present
        Long instanceStart = instanceValues.getAsLong(Instances.INSTANCE_START);
        if (instanceStart != null) {
            instanceValues
            .put(Instances.INSTANCE_START_SORTING, instanceStart + (tz == null ||
                    allday ? 0 : TimeZone.getTimeZone(tz).getOffset(instanceStart)));
        }
// add due sorting if due value is present
        Long instanceDue = instanceValues.getAsLong(Instances.INSTANCE_DUE);
        if (instanceDue != null) {
            instanceValues.put(Instances.INSTANCE_DUE_SORTING, instanceDue + (tz == null ||
                               allday ? 0 : TimeZone.getTimeZone(tz).getOffset(instanceDue)));
        }
        db.insert(CaldavDatabaseHelper.getInstancesUri(false), instanceValues);
        postNotifyUri(Instances.CONTENT_URI);
    }


    /**
     * Updates the instances of all tasks that match the given selection.
     * <p>
     * This has to cover the following cases:
     * </p>
     * <ol>
     * <li>No columns that affect instances have been changed, in that case there is nothing to do.</li>
     * <li>The selection contains a single instance task and the new values don't add recurrence, in that case only this instance has to be updated.</li>
     * <li>The selection contains a single instance task and the new values add recurrence, in that case the first instance is to be updated and all other
     * instances are to be expanded.</li>
     * <li>The selection contains a recurring task and the new values change recurrence, in that case all instances have to be updated.</li>
     * <li>The selection contains a recurring task and the new values remove recurrence, in that case the first instance has to be updated and all recurring
     * instances have to be removed.</li>
     * <li>The selection contains an exception and the new values don't add recurrence, in that case update the single exception instance.</li>
     * <li>The selection contains an exception and the new values add recurrence, at present this is not allowed, throw an exception.</li>
     * </ol>
     *
     * TODO: implement the cases above.
     *
     * @param values
     * The new task {@link ContentValues}.
     * @param selection
     * The selection string.
     * @param selectionArgs
     * The selection arguments.
     */
    private void updateInstancesOfAllTasks(ContentResolver db, ContentValues values,
                                           String selection, String[] selectionArgs) {
        Log.i("UPDATE_INSTANCE", "In updateInstanceOfAllTask");
        Cursor cursor = db.query(CaldavDatabaseHelper.getTasksUri(false), TASK_ID_PROJECTION, selection,
                                 selectionArgs, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String taskId = cursor.getString(0);
                    String taskSelection = updateSelection(selectTaskId(taskId), selection).toString();
                    updateInstancesOfOneTask(db, taskId, values, taskSelection, selectionArgs);
                }
            } finally {
                cursor.close();
            }
        }
    }


    private void updateInstancesOfOneTask(ContentResolver db, String task_id,
                                          ContentValues values, String selection, String[] selectionArgs) {
// check if either one of the following has been updated: DTSTART, DUE, DURATION, RRULE, RDATE, EXDATE
// right now we only update DTSTART, DUE and DURATION
        /*
        * DTSTART, DUE, DURATION, RRULE, RDATE, EXDATE in values? => update recurrence set
        *
        * DTSTART, DUE, DURATION => update values (in this very instance)
        *
        * All values must be given! DTSTART + DURATION -> DUE must be omitted DSTART + DUE -> DURATION must be omitted
        */
        Log.i("UPDATE_INSTANCE", "In updateInstanceOfOneTask");
        ContentValues instanceValues = setInstanceTimes(values);
        instanceValues.put(Instances.TASK_ID, task_id);
        if (values.getAsString(Tasks.RRULE) != null || values.getAsString(Tasks.RDATE) != null ||
            values.getAsString(Tasks.EXDATE) != null) {
// TODO: update recurrence table
        }
        /*
        * Calculate sorting values for start and due times. If start or due values are non-null and non-allday we add the time zone offset to UTC. That ensures
        * allday events (which are always in UTC) are sorted properly, independent of any time zone.
        */
        String tz = values.getAsString(Instances.TZ);
        Integer allday = values.getAsInteger(Tasks.IS_ALLDAY);
        Long instanceStart = instanceValues.getAsLong(Instances.INSTANCE_START);
        if (instanceStart != null) {
            instanceValues.put(Instances.INSTANCE_START_SORTING, instanceStart
                               + (tz == null || (allday != null &&
                                                 allday > 0) ? 0 : TimeZone.getTimeZone(tz).getOffset(instanceStart)));
        } else if (values.containsKey(Tasks.DTSTART)) {
// dtstart must have been set to null, so remove sorting value
            instanceValues.putNull(Instances.INSTANCE_START_SORTING);
        }
        Long instanceDue = instanceValues.getAsLong(Instances.INSTANCE_DUE);
        if (instanceDue != null) {
            instanceValues.put(Instances.INSTANCE_DUE_SORTING, instanceDue
                               + (tz == null || (allday != null &&
                                                 allday > 0) ? 0 : TimeZone.getTimeZone(tz).getOffset(instanceDue)));
        } else if (values.containsKey(Tasks.DUE)) {
// due must have been set to null, so remove sorting value
            instanceValues.putNull(Instances.INSTANCE_DUE_SORTING);
        }
        db.update(CaldavDatabaseHelper.getInstancesUri(false), instanceValues, selection, selectionArgs);
        postNotifyUri(Instances.CONTENT_URI);
    }


    /**
     * Validate the given task list values.
     *
     * @param values
     * The task list properties to validate.
     * @throws IllegalArgumentException
     * if any of the values is invalid.
     */
    private void validateTaskListValues(ContentValues values, boolean isNew, boolean isSyncAdapter) {
// row id can not be changed or set manually
        if (values.containsKey(TaskColumns._ID)) {
            throw new IllegalArgumentException("_ID can not be set manually");
        }
        if (isNew != values.containsKey(TaskListSyncColumns.ACCOUNT_NAME) && (!isNew ||
                values.get(TaskListSyncColumns.ACCOUNT_NAME) != null)) {
            throw new IllegalArgumentException("ACCOUNT_NAME is write-once and required on INSERT");
        }
        if (isNew != values.containsKey(TaskListSyncColumns.ACCOUNT_TYPE) && (!isNew ||
                values.get(TaskListSyncColumns.ACCOUNT_TYPE) != null)) {
            throw new IllegalArgumentException("ACCOUNT_TYPE is write-once and required on INSERT");
        }
        if (!isSyncAdapter && values.containsKey(TaskLists.LIST_COLOR)) {
            throw new IllegalArgumentException("Only sync adapters can change the LIST_COLOR.");
        }
        if (!isSyncAdapter && values.containsKey(TaskLists._SYNC_ID)) {
            throw new IllegalArgumentException("Only sync adapters can change the _SYNC_ID.");
        }
        if (!isSyncAdapter && values.containsKey(TaskLists.SYNC_VERSION)) {
            throw new IllegalArgumentException("Only sync adapters can change SYNC_VERSION.");
        }
    }


    /**
     * Validate the given task values.
     *
     * @param values
     * The task properties to validate.
     * @throws IllegalArgumentException
     * if any of the values is invalid.
     */
    private void validateTaskValues(ContentResolver db, ContentValues values,
                                    boolean isNew, boolean isSyncAdapter) {
// row id can not be changed or set manually
        if (values.containsKey(TaskColumns._ID)) {
            throw new IllegalArgumentException("_ID can not be set manually");
        }
// setting a LIST_ID is allowed only for new tasks, it must also refer to an existing TaskList
// TODO: cache valid ids to speed up inserts
        if (isNew) {
            String[] listId = { values.getAsString(TaskColumns.LIST_ID) };
            if (listId[0] == null) {
                throw new IllegalArgumentException("LIST_ID is required on INSERT");
            }
            Cursor cursor = db.query(CaldavDatabaseHelper.getListsUri(), TASKLIST_ID_PROJECTION,
                                     TASKLISTS_ID_SELECTION, listId,
                                     null);
            try {
                if (cursor == null || cursor.getCount() != 1) {
                    throw new IllegalArgumentException("LIST_ID must refer to an existing TaskList");
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if (values.containsKey(TaskColumns.LIST_ID)) {
            throw new IllegalArgumentException("LIST_ID is write-once");
        }
        if (!isSyncAdapter && !isNew && (values.containsKey(Tasks.ORIGINAL_INSTANCE_SYNC_ID) ||
                                         values.containsKey(Tasks.ORIGINAL_INSTANCE_ID))) {
            throw new IllegalArgumentException("ORIGINAL_INSTANCE_SYNC_ID and ORIGINAL_INSTANCE_ID can be modified by sync adapters only");
        }
        if (values.containsKey(Tasks.ORIGINAL_INSTANCE_SYNC_ID) &&
            values.containsKey(Tasks.ORIGINAL_INSTANCE_ID)) {
            throw new IllegalArgumentException("ORIGINAL_INSTANCE_SYNC_ID and ORIGINAL_INSTANCE_ID must not be specified at the same time");
        }
// Find corresponding ORIGINAL_INSTANCE_ID
        if (values.get(Tasks.ORIGINAL_INSTANCE_SYNC_ID) != null) {
            String[] syncId = { values.getAsString(Tasks.ORIGINAL_INSTANCE_SYNC_ID) };
            Cursor cursor = db.query(CaldavDatabaseHelper.getTasksUri(false), TASK_ID_PROJECTION,
                                     SYNC_ID_SELECTION, syncId, null);
            try {
                if (cursor != null && cursor.getCount() == 1) {
                    cursor.moveToNext();
                    Long originalId = cursor.getLong(0);
                    values.put(Tasks.ORIGINAL_INSTANCE_ID, originalId);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if (values.get(Tasks.ORIGINAL_INSTANCE_ID) !=
                   null) { // Find corresponding ORIGINAL_INSTANCE_SYNC_ID
            String[] id = { values.getAsString(Tasks.ORIGINAL_INSTANCE_ID) };
            Cursor cursor = db.query(CaldavDatabaseHelper.getTasksUri(false), TASK_SYNC_ID_PROJECTION,
                                     TASK_ID_SELECTION, id, null);
            try {
                if (cursor != null && cursor.getCount() == 1) {
                    cursor.moveToNext();
                    String originalSyncId = cursor.getString(0);
                    values.put(Tasks.ORIGINAL_INSTANCE_SYNC_ID, originalSyncId);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
// account name is read only for tasks
        if (values.containsKey(Tasks.ACCOUNT_NAME)) {
            throw new IllegalArgumentException("ACCOUNT_NAME is read-only for tasks");
        }
// account type is read only for tasks
        if (values.containsKey(Tasks.ACCOUNT_TYPE)) {
            throw new IllegalArgumentException("ACCOUNT_TYPE is read-only for tasks");
        }
// list color is read only for tasks
        if (values.containsKey(Tasks.LIST_COLOR)) {
            throw new IllegalArgumentException("LIST_COLOR is read-only for tasks");
        }
// no one can undelete a task!
        if (values.containsKey(TaskSyncColumns._DELETED)) {
            throw new IllegalArgumentException("modification of _DELETE is not allowed");
        }
// only sync adapters are allowed to change the UID
        if (!isSyncAdapter && values.containsKey(TaskSyncColumns._UID)) {
            throw new IllegalArgumentException("modification of _UID is not allowed");
        }
// only sync adapters are allowed to remove the dirty flag
        if (!isSyncAdapter && values.containsKey(CommonSyncColumns._DIRTY)) {
            throw new IllegalArgumentException("modification of _DIRTY is not allowed");
        }
// only sync adapters are allowed to set creation time
        if (!isSyncAdapter && values.containsKey(TaskColumns.CREATED)) {
            throw new IllegalArgumentException("modification of CREATED is not allowed");
        }
// IS_NEW is set automatically
        if (values.containsKey(Tasks.IS_NEW)) {
            throw new IllegalArgumentException("modification of IS_NEW is not allowed");
        }
// IS_CLOSED is set automatically
        if (values.containsKey(Tasks.IS_CLOSED)) {
            throw new IllegalArgumentException("modification of IS_CLOSED is not allowed");
        }
// only sync adapters are allowed to set modification time
        if (!isSyncAdapter && values.containsKey(TaskColumns.LAST_MODIFIED)) {
            throw new IllegalArgumentException("modification of MODIFICATION_TIME is not allowed");
        }
// check that PRIORITY is an Integer between 0 and 9 if given
        if (values.containsKey(TaskColumns.PRIORITY)) {
            Integer priority = values.getAsInteger(TaskColumns.PRIORITY);
            if (priority != null && (priority < 0 || priority > 9)) {
                throw new IllegalArgumentException("PRIORITY must be an integer between 0 and 9");
            }
        }
// check that CLASSIFICATION is an Integer between 0 and 2 if given
        if (values.containsKey(TaskColumns.CLASSIFICATION)) {
            Integer classification = values.getAsInteger(TaskColumns.CLASSIFICATION);
            if (classification != null && (classification < 0 || classification > 2)) {
                throw new IllegalArgumentException("CLASSIFICATION must be an integer between 0 and 2");
            }
        }
// ensure that DUE and DURATION are set properly if DTSTART is given
        Long dtStart = values.getAsLong(TaskColumns.DTSTART);
        Long due = values.getAsLong(TaskColumns.DUE);
        String duration = values.getAsString(TaskColumns.DURATION);
        if (dtStart != null) {
            if (due != null && duration != null) {
                throw new IllegalArgumentException("Only one of DUE or DURATION must be supplied.");
            } else if (due != null) {
                if (due < dtStart) {
                    throw new IllegalArgumentException("DUE must not be < DTSTART");
                }
            } else if (duration != null) {
                Duration d = new Duration(duration); // throws exception if duration string is invalid
                if (d.sign == -1) {
                    throw new IllegalArgumentException("DURATION must not be negative");
                }
            }
        } else if (duration != null) {
            throw new IllegalArgumentException("DURATION must not be supplied without DTSTART");
        }
// if one of DTSTART or DUE is given, TZ must not be null
        if ((dtStart != null || due != null) && !ONE.equals(values.getAsInteger(TaskColumns.IS_ALLDAY)) &&
            values.getAsString(TaskColumns.TZ) == null) {
            throw new IllegalArgumentException("TIMEZONE must be supplied if one of DTSTART or DUE is not null");
        }
// set proper STATUS if task has been completed
        if (!isSyncAdapter && values.getAsLong(Tasks.COMPLETED) != null &&
            !values.containsKey(Tasks.STATUS)) {
            values.put(Tasks.STATUS, Tasks.STATUS_COMPLETED);
        }
// check that PERCENT_COMPLETE is an Integer between 0 and 100 if supplied also update status and completed accordingly
        if (values.containsKey(TaskColumns.PERCENT_COMPLETE)) {
            Integer percent = values.getAsInteger(TaskColumns.PERCENT_COMPLETE);
            if (percent != null && (percent < 0 || percent > 100)) {
                throw new IllegalArgumentException("PERCENT_COMPLETE must be null or an integer between 0 and 100");
            }
            if (!isSyncAdapter && percent != null && percent == 100) {
                if (!values.containsKey(Tasks.STATUS)) {
                    values.put(Tasks.STATUS, Tasks.STATUS_COMPLETED);
                }
                if (!values.containsKey(Tasks.COMPLETED)) {
                    values.put(Tasks.COMPLETED, System.currentTimeMillis());
                    values.put(Tasks.COMPLETED_IS_ALLDAY, 0);
                }
            } else if (!isSyncAdapter && percent != null) {
                if (!values.containsKey(Tasks.COMPLETED)) {
                    values.putNull(Tasks.COMPLETED);
                }
            }
        }
// validate STATUS and set IS_NEW and IS_CLOSED accordingly
        if (values.containsKey(Tasks.STATUS) || isNew) {
            Integer status = values.getAsInteger(Tasks.STATUS);
            if (status == null) {
                status = Tasks.STATUS_DEFAULT;
                values.put(Tasks.STATUS, status);
            } else if (status < Tasks.STATUS_NEEDS_ACTION || status > Tasks.STATUS_CANCELLED) {
                throw new IllegalArgumentException("invalid STATUS: " + status);
            }
            values.put(Tasks.IS_NEW, status == null || status == Tasks.STATUS_NEEDS_ACTION ? 1 : 0);
            values.put(Tasks.IS_CLOSED, status != null && (status == Tasks.STATUS_COMPLETED ||
                       status == Tasks.STATUS_CANCELLED) ? 1 : 0);
            /*
            * Update PERCENT_COMPLETE and COMPLETED (if not given). Sync adapters should know what they're doing, so don't update anything if caller is a sync
            * adapter.
            */
            if (status == Tasks.STATUS_COMPLETED && !isSyncAdapter) {
                values.put(Tasks.PERCENT_COMPLETE, 100);
                if (!values.containsKey(Tasks.COMPLETED)) {
                    values.put(Tasks.COMPLETED, System.currentTimeMillis());
                    values.put(Tasks.COMPLETED_IS_ALLDAY, 0);
                }
            } else if (!isSyncAdapter) {
                values.putNull(Tasks.COMPLETED);
            }
        }
    }


    /**
     * Validate the given category values.
     *
     * @param values
     * The category properties to validate.
     * @throws IllegalArgumentException
     * if any of the values is invalid.
     */
    private void validateCategoryValues(ContentValues values, boolean isNew, boolean isSyncAdapter) {
// row id can not be changed or set manually
        if (values.containsKey(Categories._ID)) {
            throw new IllegalArgumentException("_ID can not be set manually");
        }
        if (isNew != values.containsKey(Categories.ACCOUNT_NAME) && (!isNew ||
                values.get(Categories.ACCOUNT_NAME) != null)) {
            throw new IllegalArgumentException("ACCOUNT_NAME is write-once and required on INSERT");
        }
        if (isNew != values.containsKey(Categories.ACCOUNT_TYPE) && (!isNew ||
                values.get(Categories.ACCOUNT_TYPE) != null)) {
            throw new IllegalArgumentException("ACCOUNT_TYPE is write-once and required on INSERT");
        }
    }


    /**
     * Validate the given alarm values.
     *
     * @param values
     * The alarm values to validate
     * @throws IllegalArgumentException
     * if any of the values is invalid.
     */
    private void validateAlarmValues(ContentValues values, boolean isNew, boolean isSyncAdapter) {
        if (values.containsKey(Alarms.ALARM_ID)) {
            throw new IllegalArgumentException("ALARM_ID can not be set manually");
        }
    }


    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
        case LISTS:
            return TaskLists.CONTENT_TYPE;
        case LIST_ID:
            return TaskLists.CONTENT_ITEM_TYPE;
        case TASKS:
            return Tasks.CONTENT_TYPE;
        case TASK_ID:
            return Tasks.CONTENT_ITEM_TYPE;
        case INSTANCES:
            return Instances.CONTENT_TYPE;
        default:
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.TaskLists.CONTENT_URI_PATH, LISTS);
        uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.TaskLists.CONTENT_URI_PATH + "/#", LIST_ID);
        uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Tasks.CONTENT_URI_PATH, TASKS);
        uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Tasks.CONTENT_URI_PATH + "/#", TASK_ID);
        uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Instances.CONTENT_URI_PATH, INSTANCES);
        uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Instances.CONTENT_URI_PATH + "/#",
                          INSTANCE_ID);
        uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Properties.CONTENT_URI_PATH, PROPERTIES);
        uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Properties.CONTENT_URI_PATH + "/#",
                          PROPERTY_ID);
        uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Categories.CONTENT_URI_PATH, CATEGORIES);
        uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Categories.CONTENT_URI_PATH + "/#",
                          CATEGORY_ID);
        uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Alarms.CONTENT_URI_PATH, ALARMS);
        uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.Alarms.CONTENT_URI_PATH + "/#", ALARM_ID);
    }


    @Override
    protected void onEndTransaction(boolean callerIsSyncAdapter) {
        super.onEndTransaction(callerIsSyncAdapter);
    };


    @Override
    public SQLiteOpenHelper getDatabaseHelper(Context context) {
        synchronized (this) {
            if (mDBHelper == null) {
                mDBHelper = getDatabaseHelperStatic(context);
            }
            return mDBHelper;
        }
    }


    public static DatabaseHelper getDatabaseHelperStatic(final Context context) {
        return DatabaseHelper.getDatabaseHelper(context);
    }


    @Override
    protected boolean syncToNetwork(Uri uri) {
        return true;
    }
}
