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

package de.azapps.mirakel.model.generic;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.tools.Log;


abstract public class ModelBase implements IGenericElementInterface {
    private static final String TAG = "ModelBase";

    public static final String ID = "_id";
    public static final String NAME = "name";
    protected static final long INVALID_ID = 0L;

    private long id = INVALID_ID;
    private String name = "";

    protected static Context context;

    protected ModelBase() {}

    public ModelBase(final Cursor c) {}

    protected ModelBase(final long newId, String newName) {
        id = newId;
        if (newName == null) {
            newName = "";
        }
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
    @Override
    @NonNull
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

    @NonNull
    public ContentValues getContentValues() throws DefinitionsHelper.NoSuchListException {
        final ContentValues cv = new ContentValues();
        cv.put(ID, Math.abs(id));
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
        final Uri u = context.getContentResolver().insert(uri, values);
        try {
            return ContentUris.parseId(u);
        } catch (final NullPointerException ignored) {
            return -1L;
        }
    }

    protected abstract Uri getUri();

    public void destroy() {
        delete(getUri(), ID + "=?", new String[] {String.valueOf(getId())});
    }

    public void save() {
        try {
            update(getUri(), getContentValues(), ID + "=?", new String[] {String.valueOf(getId())});
        } catch (final DefinitionsHelper.NoSuchListException e) {
            Log.wtf(TAG, "this could not happen because task has his own implementation", e);
        }
    }

    @NonNull
    public static String[] addPrefix(@NonNull final String[] columns, @NonNull final String prefix) {
        return addPrefix(columns, prefix, columns.length);
    }

    @NonNull
    public static String[] addPrefix(@NonNull final String[] columns, @NonNull final String prefix,
                                     final int newSize) {
        final String[] ret = new String[(newSize > columns.length) ? newSize : columns.length];
        for (int i = 0; i < columns.length; i++) {
            ret[i] = prefix + '.' + columns[i];
        }
        return ret;
    }

    public boolean isStub() {
        return getId() == INVALID_ID;
    }
}
