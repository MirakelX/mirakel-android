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


import android.support.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.tools.Log;

public final class ModelFactory {

    private static final String TAG = "ModelFactory";

    @SuppressWarnings("unchecked")
    public static<T> T createModel(final @NonNull CursorGetter cursor, final Class<T> c) {
        final Class<T> clazz;
        if (ListMirakel.class.equals(c) || SpecialList.class.equals(c)) {
            if (cursor.getBoolean(ListMirakel.IS_SPECIAL)) {
                clazz = (Class<T>) SpecialList.class;
            } else {
                clazz = (Class<T>) ListMirakel.class;
            }
        } else {
            clazz = c;
        }
        try {
            final Constructor<T> constructor = clazz.getConstructor(cursor.getClass());
            return constructor.newInstance(cursor);
        } catch (final NoSuchMethodException e) {
            Log.wtf(TAG, "Construtor not found");
            throw new RuntimeException(e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            Log.wtf(TAG, "Cannot call constructor");
            throw new RuntimeException(e);
        }
    }
}
