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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.tools.Log;

/**
 * Created by weiznich on 02.07.14.
 */
abstract public class ModelBase {
    private static final String TAG = "ModelBase";

    public static final String ID = "_id";
    public static final String NAME = "name";

    private long id;
    private String name;

    protected static Context context;

    public ModelBase(final Cursor c) {}

    protected ModelBase(long newId, String newName) {
        setId(newId);
        setName(newName);
    }


    /**
     * Initialize the model and the preferences
     *
     * @param ctx
     *            The Application-Context
     */
    public static void init(final Context ctx) {
        if (ctx == null) {
            return;
        }
        context = ctx;
    }

    public long getId() {
        return this.id;
    }
    public String getName() {
        return this.name;
    }
    protected void setId(final long newId) {
        this.id = newId;
    }
    public void setName(final String newName) {
        this.name = newName;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public ContentValues getContentValues()throws DefinitionsHelper.NoSuchListException {
        ContentValues cv = new ContentValues();
        cv.put(ID, id);
        cv.put(NAME, name);
        return cv;
    }

    protected static Cursor query(final Uri u, final String[] projection,
                                  final String selection, final String[] selectionArgs,
                                  final String sortOrder) {
        return context.getContentResolver().query(u, projection,
                selection, selectionArgs, sortOrder);
    }

    protected static int update(final Uri u, final ContentValues values,
                                final String selection, final String[] selectionArgs) {
        return context.getContentResolver().update(u, values,
                selection, selectionArgs);
    }

    protected  static int delete(final Uri u, final String selection,
                                 final String[] selectionArgs) {
        return context.getContentResolver().delete(u, selection,
                selectionArgs);
    }

    protected static long insert(final Uri uri, final ContentValues values) {
        Uri u = context.getContentResolver().insert(uri, values);
        try {
            return ContentUris.parseId(u);
        } catch (NullPointerException e) {
            return -1;
        }
    }

    protected abstract Uri getUri();

    public void destroy() {
        delete(getUri(), ID + "=?", new String[] {getId() + ""});
    }
    public void save() {
        try {
            update(getUri(), getContentValues(), ID + "=?", new String[] {getId() + ""});
        } catch (DefinitionsHelper.NoSuchListException e) {
            Log.wtf(TAG, "this could not happen because task has his own implementation", e);
        }
    }

    public static String[] addPrefix(final String[] columns, final String prefix) {
        String[] ret = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            ret[i] = prefix + "." + columns[i];
        }
        return ret;
    }
}
