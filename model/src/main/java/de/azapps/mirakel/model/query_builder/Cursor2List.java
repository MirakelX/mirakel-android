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

import android.support.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import de.azapps.tools.Either;


public  class  Cursor2List<T> implements CursorWrapper.CursorConverter<List<T>> {

    private static final String TAG = "Cursor2List";
    private final Either<Constructor<T>, CursorWrapper.CursorConverter<T>> converter;


    public Cursor2List(final Class<T> clazz) {
        try {
            converter = Either.Left(clazz.getConstructor(CursorGetter.class));
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException("go and implement a the constructor " + clazz.getCanonicalName()
                                               + "(CursorWrapper.CursorGetter)", e);
        }
    }

    public Cursor2List(@NonNull final CursorWrapper.CursorConverter<T> converter) {
        this.converter = Either.Right(converter);
    }



    @Override
    public List<T> convert(@NonNull final CursorGetter getter) {
        final List<T> list = new ArrayList<>(getter.getCount());
        if (converter.isLeft()) {
            fillWithConstructor(getter, list, converter.getLeftOrThrow());
        } else if (converter.isRight()) {
            fillWithConvertor(getter, list, converter.getRightOrThrow());
        } else {
            throw new IllegalStateException("Cannot be reached");
        }
        return list;
    }

    private static <T> void fillWithConvertor(@NonNull final CursorGetter getter,
            final List<T> list, final CursorWrapper.CursorConverter<T> converter) {
        while (getter.moveToNext()) {
            list.add(converter.convert(getter));
        }
    }

    private static <T> void fillWithConstructor(@NonNull final CursorGetter getter,
            final List<T> lists, final @NonNull Constructor<T> constructor) {
        try {
            while (getter.moveToNext()) {
                lists.add(constructor.newInstance(getter));
            }
        } catch (final InstantiationException e) {
            throw new IllegalArgumentException("go and make the constructor " + constructor.getName() +
                                               "(CursorWrapper.CursorGetter) accessible", e);
        } catch (final IllegalAccessException e) {
            throw new IllegalArgumentException("go and make the constructor " + constructor.getName() +
                                               "(CursorWrapper.CursorGetter) accessible", e);
        } catch (final InvocationTargetException e) {
            throw new IllegalArgumentException("go and make the constructor " + constructor.getName() +
                                               "(CursorWrapper.CursorGetter) accessible", e);
        }
    }
}
