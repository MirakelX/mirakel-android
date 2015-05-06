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

package de.azapps.mirakel.model;

import android.database.Cursor;

import com.google.common.base.Optional;

import java.util.Calendar;

import de.azapps.mirakel.helper.DateTimeHelper;

import static com.google.common.base.Optional.fromNullable;

/**
 * Created by az on 06.05.15.
 */
public class CursorGetter {
    private Cursor cursor;

    public CursorGetter(Cursor cursor) {
        this.cursor = cursor;
    }

    private int getColumnIndex(final String column) {
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

    public Optional<Calendar> getOptionalCalendar(final String column) {
        final int index = getColumnIndex(column);
        if (cursor.isNull(index)) {
            return Optional.absent();
        } else {
            return fromNullable(DateTimeHelper.createLocalCalendar(
                                    cursor.getLong(index), true));
        }
    }
}
