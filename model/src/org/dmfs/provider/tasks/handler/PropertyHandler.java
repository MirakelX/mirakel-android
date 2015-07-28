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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;


/**
 * Abstract class that is used as template for specific property handlers.
 *
 * @author Tobias Reinsch <tobias@dmfs.org>
 *
 */
public abstract class PropertyHandler {

    /**
     * Validates the content of the property prior to insert and update transactions.
     *
     * @param db
     *            The {@link SQLiteDatabase}.
     * @param isNew
     *            Indicates that the content is new and not an update.
     * @param values
     *            The {@link ContentValues} to validate.
     * @param isSyncAdapter
     *            Indicates that the transaction was triggered from a SyncAdapter.
     *
     * @param ctx A generic Context
     *
     * @return The valid {@link ContentValues}.
     *
     * @throws IllegalArgumentException
     *             if the {@link ContentValues} are invalid.
     */
    public abstract ContentValues validateValues(ContentResolver db, boolean isNew,
            ContentValues values, boolean isSyncAdapter, final Context ctx);


    /**
     * Inserts the property {@link ContentValues} into the database.
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
     * @return The row id of the new property as <code>long</code>
     */
    public Uri insert(ContentResolver db, ContentValues values, boolean isSyncAdapter,
                      final Context ctx) {
        return db.insert(CaldavDatabaseHelper.getPropertiesUri(), values);
    }


    /**
     * Updates the property {@link ContentValues} in the database.
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
    public int update(ContentResolver db, ContentValues values, String selection,
                      String[] selectionArgs, boolean isSyncAdapter, final Context ctx) {
        return db.update(CaldavDatabaseHelper.getPropertiesUri(), values, selection, selectionArgs);
    }


    /**
     * Deletes the property in the database.
     *
     * @param db
     *            The belonging database.
     * @param selection
     *            The selection <code>String</code> to delete the correct row.
     * @param selectionArgs
     *            The arguments for the selection <code>String</code>
     * @param isSyncAdapter
     *            Indicates that the transaction was triggered from a SyncAdapter.
     * @return
     */
    public int delete(ContentResolver db, String selection, String[] selectionArgs,
                      boolean isSyncAdapter) {
        return db.delete(CaldavDatabaseHelper.getPropertiesUri(), selection, selectionArgs);
    }

}
