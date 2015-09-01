
/*
 * Copyright (C) 2014 Marten Gajda <marten@dmfs.org>
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

package org.dmfs.provider.tasks.handler;

import org.dmfs.provider.tasks.TaskContract.Categories;
import org.dmfs.provider.tasks.TaskContract.Properties;
import org.dmfs.provider.tasks.TaskContract.Property.Category;
import org.dmfs.provider.tasks.TaskContract.Tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import de.azapps.mirakel.model.provider.MirakelContentProvider;
import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.tools.Log;


/**
 * This class is used to handle category property values during database transactions.
 *
 * @author Tobias Reinsch <tobias@dmfs.org>
 *
 */
public class CategoryHandler extends PropertyHandler {

    private static final String[] CATEGORY_ID_PROJECTION = { Categories._ID, Categories.NAME, Categories.COLOR };

    private static final String CATEGORY_ID_SELECTION = Categories._ID + "=? and " +
            Categories.ACCOUNT_NAME + "=? and " + Categories.ACCOUNT_TYPE + "=?";
    private static final String CATEGORY_NAME_SELECTION = Categories.NAME + "=? and " +
            Categories.ACCOUNT_NAME + "=? and " + Categories.ACCOUNT_TYPE + "=?";

    public static final String IS_NEW_CATEGORY = "is_new_category";
    private static final String TAG = "CategoryHandler";


    /**
     * Validates the content of the category prior to insert and update transactions.
     *
     * @param db
     *            The {@link SQLiteDatabase}.
     * @param isNew
     *            Indicates that the content is new and not an update.
     * @param values
     *            The {@link ContentValues} to validate.
     * @param isSyncAdapter
     *            Indicates that the transaction was triggered from a SyncAdapter.
     * @param ctx
     *            A generic context
     *
     * @return The valid {@link ContentValues}.
     *
     * @throws IllegalArgumentException
     *             if the {@link ContentValues} are invalid.
     */
    @Override
    public ContentValues validateValues(ContentResolver db, boolean isNew,
                                        ContentValues values,
                                        boolean isSyncAdapter, final Context ctx) {
        // the category requires a name or an id
        if (!values.containsKey(Category.CATEGORY_ID) && !values.containsKey(Category.CATEGORY_NAME)) {
            throw new IllegalArgumentException("Neiter an id nor a category name was supplied for the category property.");
        }
        // get the matching task & account for the property
        if (!values.containsKey(Properties.TASK_ID)) {
            throw new IllegalArgumentException("No task id was supplied for the category property");
        }
        String[] queryArgs = { values.getAsString(Properties.TASK_ID) };
        String[] queryProjection = { Tasks.ACCOUNT_NAME, Tasks.ACCOUNT_TYPE };
        String querySelection = Tasks._ID + "=?";
        Cursor taskCursor = db.query(CaldavDatabaseHelper.getTasksUri(false), queryProjection,
                                     querySelection, queryArgs, null);
        String accountName = null;
        String accountType = null;
        try {
            taskCursor.moveToNext();
            accountName = taskCursor.getString(0);
            accountType = taskCursor.getString(1);
            values.put(Categories.ACCOUNT_NAME, accountName);
            values.put(Categories.ACCOUNT_TYPE, accountType);
        } finally {
            if (taskCursor != null) {
                taskCursor.close();
            }
        }
        if (accountName != null && accountType != null) {
            // search for matching categories
            String[] categoryArgs;
            Cursor cursor;
            if (values.containsKey(Categories._ID)) {
                // serach by ID
                categoryArgs = new String[] { values.getAsString(Category.CATEGORY_ID), accountName, accountType };
                cursor = db.query(CaldavDatabaseHelper.getCategoriesUri(), CATEGORY_ID_PROJECTION,
                                  CATEGORY_ID_SELECTION, categoryArgs,
                                  null);
            } else {
                // search by name
                categoryArgs = new String[] { values.getAsString(Category.CATEGORY_NAME), accountName, accountType };
                cursor = db.query(CaldavDatabaseHelper.getCategoriesUri(), CATEGORY_ID_PROJECTION,
                                  CATEGORY_NAME_SELECTION, categoryArgs,
                                  null);
            }
            try {
                if (cursor != null && cursor.getCount() == 1) {
                    cursor.moveToNext();
                    Long categoryID = cursor.getLong(0);
                    String categoryName = cursor.getString(1);
                    int color = cursor.getInt(2);
                    values.put(Category.CATEGORY_ID, categoryID);
                    values.put(Category.CATEGORY_NAME, categoryName);
                    values.put(Category.CATEGORY_COLOR, color);
                    values.put(IS_NEW_CATEGORY, false);
                } else {
                    values.put(IS_NEW_CATEGORY, true);
                    if (!values.containsKey(Category.CATEGORY_COLOR)) {
                        if (cursor != null) {
                            cursor.close();
                        }
                        cursor = db.query(Tag.URI, new String[] {"count(*)"}, null, null, null);
                        if (cursor.moveToFirst()) {
                            values.put(Category.CATEGORY_COLOR, Tag.getNextColor(cursor.getLong(0), ctx));
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return values;
    }


    /**
     * Inserts the category into the database.
     *
     * @param db
     *            The {@link SQLiteDatabase}.
     * @param values
     *            The {@link ContentValues} to insert.
     * @param isSyncAdapter
     *            Indicates that the transaction was triggered from a SyncAdapter.
     * @param ctx
     *            A generic context
     *
     * @return The row id of the new category as <code>long</code>
     */
    @Override
    public Uri insert(ContentResolver db, ContentValues values, boolean isSyncAdapter,
                      final Context ctx) {
        values = validateValues(db, true, values, isSyncAdapter, ctx);
        values = getOrInsertCategory(db, values);
        insertRelation(db, values.getAsString(Category.TASK_ID), values.getAsString(Category.CATEGORY_ID));
        // insert property row and create relation
        return super.insert(db, values, isSyncAdapter, ctx);
    }


    /**
     * Updates the category in the database.
     *
     * @param db
     *            The {@link SQLiteDatabase}.
     * @param values
     *            The {@link ContentValues} to update.
     * @param selection
     *            The selection <code>String</code> to update the right row.
     * @param selectionArgs
     *            The arguments for the selection <code>String</code>.
     * @param isSyncAdapter
     *            Indicates that the transaction was triggered from a SyncAdapter.
     * @param ctx
     *            A generic context
     *
     * @return The number of rows affected.
     */
    @Override
    public int update(ContentResolver db, ContentValues values, String selection,
                      String[] selectionArgs,
                      boolean isSyncAdapter, final Context ctx) {
        super.update(db, values, selection, selectionArgs, isSyncAdapter, ctx);
        values = validateValues(db, true, values, isSyncAdapter, ctx);
        values = getOrInsertCategory(db, values);
        return super.update(db, values, selection, selectionArgs, isSyncAdapter, ctx);
    }


    /**
     * Check if a category with matching {@link ContentValues} exists and returns the existing category or creates a new category in the database.
     *
     * @param db
     *            The {@link SQLiteDatabase}.
     * @param values
     *            The {@link ContentValues} of the category.
     * @return The {@link ContentValues} of the existing or new category.
     */
    private ContentValues getOrInsertCategory(ContentResolver db, ContentValues values) {
        if (values.getAsBoolean(IS_NEW_CATEGORY)) {
            // insert new category in category table
            ContentValues newCategoryValues = new ContentValues();
            newCategoryValues.put(Categories.ACCOUNT_NAME, values.getAsString(Categories.ACCOUNT_NAME));
            newCategoryValues.put(Categories.ACCOUNT_TYPE, values.getAsString(Categories.ACCOUNT_TYPE));
            newCategoryValues.put(Categories.NAME, values.getAsString(Category.CATEGORY_NAME));
            newCategoryValues.put(Categories.COLOR, values.getAsInteger(Category.CATEGORY_COLOR));
            Uri categoryUri = db.insert(CaldavDatabaseHelper.getCategoriesUri(), newCategoryValues);
            values.put(Category.CATEGORY_ID, MirakelContentProvider.getId(categoryUri));
        }
        // remove redundant values
        values.remove(IS_NEW_CATEGORY);
        values.remove(Categories.ACCOUNT_NAME);
        values.remove(Categories.ACCOUNT_TYPE);
        return values;
    }


    /**
     * Inserts a relation entry in the database to link task and category.
     *
     * @param db
     *            The {@link SQLiteDatabase}.
     * @param taskId
     *            The row id of the task.
     * @param categoryId
     *            The row id of the category.
     * @return The row id of the inserted relation.
     */
    private Uri insertRelation(ContentResolver db, String taskId, String categoryId) {
        ContentValues relationValues = new ContentValues();
        relationValues.put("task_id", taskId);
        relationValues.put("tag_id", categoryId);
        return db.insert(MirakelInternalContentProvider.TAG_CONNECTION_URI, relationValues);
    }

    @Override
    public int delete(ContentResolver db, String selection, String[] selectionArgs,
                      boolean isSyncAdapter) {
        try {
            return db.delete(MirakelInternalContentProvider.TAG_CONNECTION_URI, selection.replace("property",
                             "tag"), selectionArgs);
        } catch (Exception e) {
            Log.wtf(TAG, "somehow this is a strange query", e);
        }
        return 0;
    }
}
