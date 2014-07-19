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
import android.support.v4.content.CursorLoader;
import android.text.TextUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    private List<String> projection = new ArrayList<>();
    private StringBuilder selection = new StringBuilder();
    private List<String> selectionArgs = new ArrayList<>();
    private StringBuilder sortOrder = new StringBuilder();
    private boolean distinct = false;


    public CursorLoader toCursorLoader(final Uri uri) {
        return new CursorLoader(context, uri, projection.toArray(new String[projection.size()]),
                                selection.toString(), selectionArgs.toArray(new String[selectionArgs.size()]),
                                sortOrder.toString());
    }

    public MirakelQueryBuilder(Context context) {
        this.context = context;
    }

    public MirakelQueryBuilder distinct() {
        distinct = true;
        return this;
    }

    private void appendConjunction(Conjunction conjunction) {
        if (selection.length() != 0) {
            selection.append(" " + conjunction.toString() + " ");
        }
    }

    public MirakelQueryBuilder select(final String ... projection) {
        this.projection = Arrays.asList(projection);
        return this;
    }

    public MirakelQueryBuilder select(final List<String> projection) {
        this.projection = projection;
        return this;
    }

    public long count(final Uri uri) {
        select("count(*)");
        final Cursor c = query(uri);
        long count = 0;
        if (c.moveToFirst()) {
            count = c.getLong(0);
        }
        c.close();
        return count;
    }

    /**
     * Appends a selection to the current WHERE part
     *
     * @param conjunction How to connect the old query with the new one
     * @param selection   The selection to add
     * @return
     */
    private MirakelQueryBuilder appendCondition(Conjunction conjunction, String selection) {
        if (selection.trim().length() == 0) {
            return this;
        }
        appendConjunction(conjunction);
        this.selection.append(selection);
        return this;
    }

    private MirakelQueryBuilder appendCondition(Conjunction conjunction, String selection,
            List<String> selectionArguments) {
        appendCondition(conjunction, selection);
        selectionArgs.addAll(selectionArguments);
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
            MirakelQueryBuilder subQuery, final Uri u) {
        appendCondition(conjunction, selection + " (" + subQuery.getQuery(u) + ")",
                        subQuery.getSelectionArguments());
        return this;
    }

    private <T> MirakelQueryBuilder appendCondition(final Conjunction conjunction, final String field,
            final Operation op, final List<T> filterInput, final List<String> selectionArgs) {
        if (filterInput.isEmpty()) {
            return null;
        }
        boolean isNull = filterInput.get(0) == null;
        Class clazz = isNull ? null : filterInput.get(0).getClass();
        boolean isModel = isNull ? false : clazz.isAssignableFrom(ModelBase.class);
        boolean isBoolean = isNull ? false : clazz == boolean.class || clazz == Boolean.class;
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
            } else if (isBoolean) {
                filter.add(((Boolean)el) == true ? "1" : "0");
            } else if (!isNull) {
                filter.add(el.toString());
            } else {
                filter.add(null);
                break;
            }
        }
        String not = "";
        if (NOT.contains(op)) {
            not = "NOT ";
        }
        if (op == Operation.IN || op == Operation.NOT_IN) {
            appendCondition(conjunction, not + field + " " + op + " (" + TextUtils.join(", ", filter) + ")",
                            selectionArgs);
        } else {
            for (final String a : filter) {
                if (a != null) {
                    appendCondition(conjunction, not + field + " " + op + " " + a);
                } else {
                    appendCondition(conjunction, field + " IS " + not + "NULL ");
                }
            }
            if (!selectionArgs.isEmpty()) {
                this.selectionArgs.addAll(selectionArgs);
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
    public String getQuery(final Uri uri) {
        StringBuilder query = new StringBuilder(selection.length() + projection.size() * 15 +
                                                selectionArgs.size() * 15 + 100);
        query.append("SELECT ");
        if (distinct) {
            query.append("DISTINCT ");
        }
        query.append(TextUtils.join(", ", projection));
        query.append(" FROM ");
        query.append(MirakelInternalContentProvider.getTableName(uri));
        if (selection.length() > 0) {
            String where = selection.toString();
            query.append(" WHERE ").append(where);
        }
        if (sortOrder.length() != 0) {
            query.append(" ORDER BY ").append(sortOrder);
        }
        return query.toString();
    }

    public String getSelection() {
        return this.selection.toString();
    }

    public List<String> getSelectionArguments() {
        return selectionArgs;
    }

    public Cursor query(final Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        return contentResolver.query(uri, projection.toArray(new String[projection.size()]),
                                     selection.toString(),
                                     selectionArgs.toArray(new String[selectionArgs.size()]), sortOrder.toString());
    }

    //and
    public <T extends Number> MirakelQueryBuilder and
    (final String field, final Operation op, final T filter) {
        return and (field, op, filter.toString());
    }
    public <T extends ModelBase> MirakelQueryBuilder and
    (final String field, final Operation op, final T filter) {
        return and (field, op, filter.getId() + "");
    }

    public MirakelQueryBuilder and
    (final String field, final Operation op, final boolean filter) {
        return and (field, op, Arrays.asList(filter ? 1 : 0), new ArrayList<String>());
    }

    public MirakelQueryBuilder and (final String field, final Operation op, final String filter) {
        if (filter == null) {
            return and (field, op, Arrays.asList(new String[] {null}), new ArrayList<String>());
        }
        return and (field, op, Arrays.asList(new String[] {"?"}), Arrays.asList(new String[] {filter}));
    }

    /*
     * Do not call this with something other then T extends Number or T=String
     * java does not allow to define functions in this way
     */
    public <T> MirakelQueryBuilder and (final String field, final Operation op, final List<T> filter) {
        return and (field, op, filter, new ArrayList<String>());
    }

    public <T extends Number> MirakelQueryBuilder and
    (final String field, final Operation op, final T filter, final String selectionArgs) {
        return and (field, op, Arrays.asList(new String[] {filter.toString()}), Arrays.asList(new String[] {selectionArgs}));
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
        return appendCondition(Conjunction.AND, "(" + other.getSelection() + ")",
                               other.getSelectionArguments());
    }

    public MirakelQueryBuilder and
    (final String field, final Operation op, final MirakelQueryBuilder subQuery,
     final Uri subqueryUri) {
        String not = "";
        if (NOT.contains(op)) {
            not = " NOT ";
        }
        return appendCondition(Conjunction.AND, not + field + " " + op, subQuery, subqueryUri);
    }


    //or
    public <T extends Number> MirakelQueryBuilder or
    (final String field, final Operation op, final T filter) {
        return or (field, op, filter.toString());
    }
    public <T extends ModelBase> MirakelQueryBuilder or
    (final String field, final Operation op, final T filter) {
        return or (field, op, filter.getId() + "");
    }

    public <T extends Number> MirakelQueryBuilder or
    (final String field, final Operation op, final boolean filter) {
        return or (field, op, Arrays.asList(filter ? 1 : 0), new ArrayList<String>());
    }

    /*
     * Do not call this with something other then T extends Number, T extends ModelBase or T=String
     * java does not allow to define functions in this way
     */
    public <T> MirakelQueryBuilder or (final String field, final Operation op, List<T> filter) {
        return or (field, op, filter, new ArrayList<String>());
    }

    public <T extends Number> MirakelQueryBuilder or
    (final String field, final Operation op, final T filter, final String selectionArgs) {
        return or (field, op, Arrays.asList(new String[] {filter.toString()}),  Arrays.asList(new String[] {selectionArgs}));
    }

    public MirakelQueryBuilder or
    (final String field, final Operation op, final String filter) {
        if (filter == null) {
            return or (field, op, Arrays.asList(new String[] {null}), new ArrayList<String>());
        }
        return or (field, op,  Arrays.asList(new String[] {"?"}), Arrays.asList(new String[] {filter}));
    }

    /*
     * Do not call this with something other then T extends Number, T extends ModelBase or T=String
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
    (final String field, final Operation op, final MirakelQueryBuilder subQuery,
     final Uri subqueryUri) {
        String not = "";
        if (NOT.contains(op)) {
            not = " NOT ";
        }
        return appendCondition(Conjunction.OR, not + field + " " + op, subQuery, subqueryUri);
    }

    public MirakelQueryBuilder not(final MirakelQueryBuilder other) {
        selection.append(" NOT (").append(other.getSelection()).append(")");
        selectionArgs.addAll(other.selectionArgs);
        return this;
    }


    private <T> T cursorToObject(final Cursor c, final Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getConstructor(Cursor.class);
            return constructor.newInstance(c);
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
        final Cursor c = query(setupQueryBuilder(clazz));
        if (c.moveToFirst()) {
            do {
                T obj = cursorToObject(c, clazz);
                l.add(obj);
            } while (c.moveToNext());
        }
        c.close();
        return l;
    }

    public <T extends ModelBase> T get(final Class<T> clazz, final long id) {
        and (ModelBase.ID, Operation.EQ, id);
        return get(clazz);
    }

    public <T extends ModelBase> T get(final Class<T> clazz) {
        T a = null;
        final Cursor c = query(setupQueryBuilder(clazz));
        if (c.moveToFirst()) {
            a = cursorToObject(c, clazz);
        }
        c.close();
        return a;
    }

    private<T> Uri setupQueryBuilder(final Class<T> clazz) {
        Uri uri;
        try {
            uri = (Uri) clazz.getField("URI").get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.wtf(TAG, "go and implement a URI  for" + clazz.getCanonicalName());
            throw new IllegalArgumentException("go and implement a URI for " + clazz.getCanonicalName());
        }
        if (projection.isEmpty()) {
            try {
                //can be null, because field should be static
                projection = Arrays.asList((String[]) clazz.getField("allColumns").get(null));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                Log.wtf(TAG, "go and implement allColumns for " + clazz.getCanonicalName());
                throw new IllegalArgumentException("go and implement allColumns for " + clazz.getCanonicalName());
            }
        }
        return uri;
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

    static final List<Operation> NOT = Arrays.asList(Operation.NOT_EQ, Operation.NOT_LIKE,
                                       Operation.NOT_GT, Operation.NOT_GE, Operation.NOT_LT, Operation.NOT_LE, Operation.NOT_IN);
    public enum Operation {
        EQ, LIKE, GT, GE, LT, LE, IN,
        NOT_EQ, NOT_LIKE, NOT_GT, NOT_GE, NOT_LT, NOT_LE, NOT_IN;

        @Override
        public String toString() {
            switch (this) {
            case EQ:
            case NOT_EQ:
                return "=";
            case LIKE:
            case NOT_LIKE:
                return "LIKE";
            case GT:
            case NOT_GT:
                return ">";
            case GE:
            case NOT_GE:
                return ">=";
            case LT:
            case NOT_LT:
                return "<";
            case LE:
            case NOT_LE:
                return "<=";
            case IN:
            case NOT_IN:
                return "IN";
            default:
                throw new IllegalArgumentException("Unkown Operation " + super.toString());
            }
        }
    }
}
