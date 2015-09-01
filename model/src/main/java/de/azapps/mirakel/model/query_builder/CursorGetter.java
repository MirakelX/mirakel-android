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
package de.azapps.mirakel.model.query_builder;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.google.common.base.Optional;

import org.joda.time.DateTime;

import de.azapps.mirakel.model.generic.ModelBase;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class CursorGetter {

    private static Context ctx;

    public static void init(@NonNull final Context ctx) {
        CursorGetter.ctx = ctx;
    }

    @NonNull
    private final Cursor cursor;

    CursorGetter(final @NonNull Cursor c) {
        cursor = c;
    }

    public int getColumnIndex(final String column) {
        return cursor.getColumnIndex(column);
    }

    public boolean getBoolean(final String column) {
        return 1 == cursor.getShort(getColumnIndex(column));
    }

    public String getString(final String column) {
        return cursor.getString(getColumnIndex(column));
    }

    public int getInt(final String column) {
        return cursor.getInt(getColumnIndex(column));
    }

    public long getLong(final String column) {
        return cursor.getLong(getColumnIndex(column));
    }

    public short getShort(String column) {
        return cursor.getShort(getColumnIndex(column));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOptional(final String column, final Class<T> itemClass) {
        final int index = getColumnIndex(column);
        if (cursor.isNull(index)) {
            return absent();
        }
        if (itemClass.equals(Integer.class)) {
            return (Optional<T>) of(cursor.getInt(index));
        } else if (itemClass.equals(Long.class)) {
            return (Optional<T>) of(cursor.getLong(index));
        } else if (itemClass.equals(Short.class)) {
            return (Optional<T>) of(cursor.getShort(index));
        } else if (itemClass.equals(String.class)) {
            return (Optional<T>) of(cursor.getString(index));
        } else if (ModelBase.class.isAssignableFrom(itemClass)) {
            return (Optional<T>) new MirakelQueryBuilder(ctx).get((Class<ModelBase>)itemClass,
                    cursor.getLong(index));
        } else  if (DateTime.class.isAssignableFrom(itemClass)) {
            return (Optional<T>) of(new DateTime(cursor.getLong(index)));
        }
        throw new IllegalStateException("Implement getOptional for " + itemClass.getCanonicalName());
    }

    public boolean moveToFirst() {
        return cursor.moveToFirst();
    }

    public int getCount() {
        return cursor.getCount();
    }

    public boolean moveToNext() {
        return cursor.moveToNext();
    }
    public boolean isAfterLast() {
        return cursor.isAfterLast();
    }
    public void moveToPosition(int which) {
        cursor.moveToPosition(which);
    }


    public int getInt(final int index) {
        return cursor.getInt(index);
    }

    public long getLong(final int index) {
        return cursor.getLong(index);
    }

    public static CursorGetter unsafeGetter(final @NonNull Cursor c) {
        return new CursorGetter(c);
    }


    public DateTime getDateTime(final @NonNull String fieldName) {
        return new DateTime(getLong(fieldName));
    }
}
