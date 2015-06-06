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

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


public class CursorWrapper {

    public interface CursorConverter<T> {
        T convert(final @NonNull CursorGetter getter);
    }

    public interface WithCursor {
        void withOpenCursor(final @NonNull CursorGetter getter);
    }

    @Nullable
    private Cursor cursor;

    public CursorWrapper(final @NonNull Cursor cursor) {
        this.cursor = cursor;
    }

    public <T> T doWithCursor(final @NonNull CursorConverter<T> exec) throws NullPointerException {
        if (cursor == null) {
            throw new NullPointerException("Cursor is null");
        }
        final T ret = exec.convert(new CursorGetter(cursor));
        if ((cursor != null) && !cursor.isClosed()) {
            cursor.close();
        }
        cursor = null;
        return ret;
    }

    public void doWithCursor(final @NonNull WithCursor exec) throws NullPointerException {
        if (cursor == null) {
            throw new NullPointerException("Cursor is null");
        }
        exec.withOpenCursor(new CursorGetter(cursor));
        if ((cursor != null) && !cursor.isClosed()) {
            cursor.close();
        }
        cursor = null;
    }

    //Do only use this if it's really necessary
    public Cursor getRawCursor() {
        return cursor;
    }

}
