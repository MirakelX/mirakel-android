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

package de.azapps.mirakel.model.query_builder;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.io.InvalidClassException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.tools.Log;

/**
 * Created by az on 15.07.14.
 */
public class MirakelQueryBuilder {
    private static final String TAG = "MirakelQueryBuilder";
    private Context context;
    private Uri uri;
    private List<String> projection = new ArrayList<>();
    private StringBuilder selection = new StringBuilder();
    private List<String> selectionArgs = new ArrayList<>();
    private StringBuilder sortOrder = new StringBuilder();

    public MirakelQueryBuilder(Context context) {
        this.context = context;
    }

    private void appendConjunction(Conjunction conjunction) {
        if (selection.length() != 0) {
            selection.append(" " + conjunction.toString() + " ");
        }
    }

    /**
     * Appends a selection to the current WHERE part
     *
     * @param conjunction How to connect the old query with the new one
     * @param selection   The selection to add
     * @return
     */
    private MirakelQueryBuilder appendCondition(Conjunction conjunction, String selection) {
        appendConjunction(conjunction);
        this.selection.append('(').append(selection).append(')');
        return this;
    }

    private MirakelQueryBuilder appendCondition(Conjunction conjunction, String selection,
            String selectionArguments) {
        appendCondition(conjunction, selection);
        selectionArgs.add(selectionArguments);
        return this;
    }

    private MirakelQueryBuilder appendCondition(Conjunction conjunction, String selection,
            List<String> selectionArgument) {
        appendCondition(conjunction, selection);
        selectionArgs.addAll(selectionArgument);
        return this;
    }

    /**
     * Appends a selection to the current WHERE part
     * <p/>
     * The subQuery must be suitable for the getQuery() function
     *
     * @param conjunction
     * @param selection
     * @param subQuery
     * @return
     */
    private MirakelQueryBuilder appendCondition(Conjunction conjunction, String selection,
            MirakelQueryBuilder subQuery) {
        appendCondition(conjunction, selection + " (" + subQuery.getQuery() + ")",
                        subQuery.getSelectionArguments());
        return this;
    }

    private <T> MirakelQueryBuilder appendCondition(final Conjunction conjunction, final String field,
            final Operation op, final List<T> filterInput, final List<String> selectionArgs) {
        if (filterInput.isEmpty()) {
            return null;
        }
        Class clazz = filterInput.get(0).getClass();
        boolean isModel = clazz.isAssignableFrom(ModelBase.class);
        Method getId = null;
        if (isModel) {
            try {
                getId = clazz.getMethod("getId");
            } catch (NoSuchMethodException e) {
                Log.wtf(TAG, "go and implement getId in " + clazz.getCanonicalName());
                throw new IllegalArgumentException("go and implement getId in " + clazz.getCanonicalName());
            }
        }
        List<String> filter = new ArrayList<>(filterInput.size());
        for (T el : filterInput) {
            if (isModel) {
                try {
                    filter.add("" + getId.invoke(el));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    Log.wtf(TAG, "go and make getId in " + clazz.getCanonicalName() + " accessible");
                    throw new IllegalArgumentException("go and implement getId in " + clazz.getCanonicalName());
                }
            } else {
                filter.add(el.toString());
            }
        }
        if (op == Operation.IN) {
            appendCondition(conjunction, field + " " + op + " " + TextUtils.join(", ", filter), selectionArgs);
        } else {
            for (final String a : filter) {
                appendCondition(conjunction, field + " " + op + " " + a, selectionArgs);
            }
        }
        return this;
    }

    /**
     * Builds the query and returns it
     * <p/>
     * This is currently just a very primitive function and is not suitable for production use
     *
     * @return
     */
    public String getQuery() {
        StringBuilder query = new StringBuilder(selection.length() + projection.size() * 15 +
                                                selectionArgs.size() * 15 + 100);
        query.append("SELECT ");
        query.append(TextUtils.join(", ", projection));
        query.append(" FROM ");
        query.append(MirakelInternalContentProvider.getTableName(uri));
        query.append(" WHERE ");
        return query.toString();
    }

    public String getSelection() {
        return this.selection.toString();
    }

    public List<String> getSelectionArguments() {
        return selectionArgs;
    }

    public Cursor query() {
        ContentResolver contentResolver = context.getContentResolver();
        return contentResolver.query(uri, projection.toArray(new String[projection.size()]),
                                     selection.toString(),
                                     selectionArgs.toArray(new String[selectionArgs.size()]), sortOrder.toString());
    }




    //and
    public <T extends Number> MirakelQueryBuilder and
    (final String field, final Operation op, final T filter) {
        return and (field, op, filter.toString(), new ArrayList<String>());
    }

    public MirakelQueryBuilder and (final String field, final Operation op, final String filter) {
        return and (field, op, Arrays.asList(new String[] {filter}), new ArrayList<String>());
    }

    /*
     * Do not call this with something other then T extends Number or T=String
     * java does not allow to define functions in this way
     */
    public <T> MirakelQueryBuilder and (final String field, final Operation op, List<T> filter) {
        return and (field, op, filter, new ArrayList<String>());
    }

    public <T extends Number> MirakelQueryBuilder and
    (final String field, final Operation op, final T filter, List<String> selectionArgs) {
        return and (field, op, filter.toString(), selectionArgs);
    }

    public MirakelQueryBuilder and
    (final String field, final Operation op, final String filter, List<String> selectionArgs) {
        return and (field, op, Arrays.asList(new String[] {filter}), selectionArgs);
    }

    /*
     * Do not call this with something other then T extends Number or T=String
     * java does not allow to define functions in this way
     */
    public <T> MirakelQueryBuilder and
    (final String field, final Operation op, List<T> filter, final List<String> selectionArgs) {
        return appendCondition(Conjunction.AND, field, op, filter, selectionArgs);
    }


    public MirakelQueryBuilder and (final MirakelQueryBuilder other) {
        return appendCondition(Conjunction.AND, other.getSelection(), other.getSelectionArguments());
    }

    public MirakelQueryBuilder and
    (final String field, final Operation op, final MirakelQueryBuilder subQuery) {
        return appendCondition(Conjunction.AND, field + " " + op, subQuery);
    }


    //or
    public <T extends Number> MirakelQueryBuilder or
    (final String field, final Operation op, final T filter) {
        return or (field, op, filter.toString(), new ArrayList<String>());
    }

    public MirakelQueryBuilder or (final String field, final Operation op, final String filter) {
        return or (field, op, Arrays.asList(new String[] {filter}), new ArrayList<String>());
    }

    /*
     * Do not call this with something other then T extends Number or T=String
     * java does not allow to define functions in this way
     */
    public <T> MirakelQueryBuilder or (final String field, final Operation op, List<T> filter) {
        return or (field, op, filter, new ArrayList<String>());
    }

    public <T extends Number> MirakelQueryBuilder or
    (final String field, final Operation op, final T filter, List<String> selectionArgs) {
        return or (field, op, filter.toString(), selectionArgs);
    }

    public MirakelQueryBuilder or
    (final String field, final Operation op, final String filter, List<String> selectionArgs) {
        return or (field, op, Arrays.asList(new String[] {filter}), selectionArgs);
    }

    /*
     * Do not call this with something other then T extends Number or T=String
     * java does not allow to define functions in this way
     */
    public <T> MirakelQueryBuilder or
    (final String field, final Operation op, List<T> filter, final List<String> selectionArgs) {
        return appendCondition(Conjunction.OR, field, op, filter, selectionArgs);
    }


    public MirakelQueryBuilder or (final MirakelQueryBuilder other) {
        return appendCondition(Conjunction.OR, other.getSelection(), other.getSelectionArguments());
    }

    public MirakelQueryBuilder or
    (final String field, final Operation op, final MirakelQueryBuilder subQuery) {
        return appendCondition(Conjunction.OR, field + " " + op, subQuery);
    }


    private <T> T cursorToObject(final Cursor c, final Class<T> clazz) {
        try {
            return (T) clazz.getConstructor(Cursor.class).newInstance(c);
        } catch (NoSuchMethodException e) {
            Log.wtf(TAG, "go and implement a the constructor " + clazz.getCanonicalName() + "(Cursor)");
            throw new IllegalArgumentException("go and implement a the constructor " + clazz.getCanonicalName()
                                               + "(Cursor)");
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            Log.wtf(TAG, "go and make the constructor " + clazz.getCanonicalName() + "(Cursor) accessible");
            throw new IllegalArgumentException("go and make the constructor " + clazz.getCanonicalName() +
                                               "(Cursor) accessible");
        }
    }


    public <T extends ModelBase> List<T> getList(final Class<T> clazz) {
        List<T> l = new ArrayList<>();
        setupQueryBuilder(clazz);
        final Cursor c = query();
        if (c.moveToFirst()) {
            do {
                T obj = cursorToObject(c, clazz);
                l.add(obj);
            } while (c.moveToNext());
        }
        c.close();
        return l;
    }

    public <T extends ModelBase> T get(final Class<T> clazz) {
        setupQueryBuilder(clazz);
        T a = null;
        final Cursor c = query();
        if (c.moveToFirst()) {
            a = cursorToObject(c, clazz);
        }
        c.close();
        return a;
    }

    private<T> void setupQueryBuilder(final Class<T> clazz) {
        try {
            uri = (Uri) clazz.getField("URI").get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.wtf(TAG, "go and implement a URI  for" + clazz.getCanonicalName());
            throw new IllegalArgumentException("go and implement a URI  for" + clazz.getCanonicalName());
        }
        try {
            //can be null, because field should be static
            projection = Arrays.asList((String[]) clazz.getField("allColumns").get(null));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.wtf(TAG, "go and implement allColumns for " + clazz.getCanonicalName());
            throw new IllegalArgumentException("go and implement allColumns for " + clazz.getCanonicalName());
        }
    }

    public MirakelQueryBuilder sort(final String field, final Sorting s) {
        if (sortOrder.length() > 0) {
            sortOrder.append(", ");
        }
        sortOrder.append(field).append(" ").append(s);
        return this;
    }


    public enum Conjunction {
        AND, OR;
    }

    public enum Sorting {
        ASC, DESC;
    }

    public enum Operation {
        EQ, LIKE, GT, GE, LT, LE, IN;

        @Override
        public String toString() {
            switch (this) {
            case EQ:
                return "=";
            case LIKE:
                return "LIKE";
            case GT:
                return ">";
            case GE:
                return ">=";
            case LT:
                return "<";
            case LE:
                return "<=";
            case IN:
                return "IN";
            default:
                throw new IllegalArgumentException("Unkown Operation " + super.toString());
            }
        }
    }
}
