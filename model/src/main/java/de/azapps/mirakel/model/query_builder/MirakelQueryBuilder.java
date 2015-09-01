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

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.common.base.Optional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.generic.ModelFactory;
import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

/**
 * Created by az on 15.07.14.
 */
public class MirakelQueryBuilder {
    private static final String TAG = "MirakelQueryBuilder";
    private final Context context;
    private List<String> projection = new ArrayList<>(5);
    private final StringBuilder selection = new StringBuilder();
    private final List<String> selectionArgs = new ArrayList<>(2);
    private final StringBuilder sortOrder = new StringBuilder();
    private boolean distinct = false;


    public android.support.v4.content.CursorLoader toSupportCursorLoader(final Uri uri) {
        return new android.support.v4.content.CursorLoader(
                   this.context,
                   uri,
                   this.projection.toArray(new String[this.projection.size()]),
                   this.selection.toString(),
                   this.selectionArgs.toArray(new String[this.selectionArgs.size()]),
                   this.sortOrder.toString());
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public android.content.CursorLoader toCursorLoader(final Uri uri) {
        return new android.content.CursorLoader(
                   this.context,
                   uri,
                   this.projection.toArray(new String[this.projection.size()]),
                   this.selection.toString(),
                   this.selectionArgs.toArray(new String[this.selectionArgs.size()]),
                   this.sortOrder.toString());
    }

    public MirakelQueryBuilder(final Context context) {
        this.context = context;
    }

    public MirakelQueryBuilder distinct() {
        this.distinct = true;
        return this;
    }

    private void appendConjunction(final Conjunction conjunction) {
        if (this.selection.length() != 0) {
            this.selection.append(' ').append(conjunction.toString()).append(' ');
        }
    }

    public MirakelQueryBuilder select(final String... projection) {
        this.projection = Arrays.asList(projection);
        return this;
    }

    public MirakelQueryBuilder select(final List<String> projection) {
        this.projection = projection;
        return this;
    }

    public long count(final Uri uri) {
        select("count(*)");
        return query(uri).doWithCursor(new CursorWrapper.CursorConverter<Long>() {
            @Override
            public Long convert(@NonNull CursorGetter getter) {
                if (getter.moveToFirst()) {
                    return getter.getLong(0);
                } else {
                    return 0L;
                }
            }
        });
    }


    private <T extends ModelBase> String getId(T filter) {
        return (filter == null) ? null : String.valueOf(filter.getId());
    }

    /**
     * Appends a selection to the current WHERE part
     *
     * @param conjunction
     *            How to connect the old query with the new one
     * @param selection
     *            The selection to add
     * @return
     */
    private MirakelQueryBuilder appendCondition(final Conjunction conjunction,
            final String selection) {
        if (selection.trim().isEmpty()) {
            return this;
        }
        appendConjunction(conjunction);
        this.selection.append(selection);
        return this;
    }

    private MirakelQueryBuilder appendCondition(final Conjunction conjunction,
            final String selection, final List<String> selectionArguments) {
        appendCondition(conjunction, selection);
        this.selectionArgs.addAll(selectionArguments);
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
    private MirakelQueryBuilder appendCondition(final Conjunction conjunction,
            final String selection, final MirakelQueryBuilder subQuery,
            final Uri u) {
        appendCondition(conjunction, selection + " (" + subQuery.getQuery(u)
                        + ')', subQuery.getSelectionArguments());
        return this;
    }

    @SuppressWarnings("unchecked")
    private <T> MirakelQueryBuilder appendCondition(
        final Conjunction conjunction, final String field,
        final Operation op, final List<T> filterInput,
        final List<String> selectionArgs) {
        if (filterInput.isEmpty()) {
            //is useless to call this without
            return this;
        }
        final boolean isNull = filterInput.get(0) == null;
        final Class clazz = isNull ? null : filterInput.get(0).getClass();
        final boolean isModel = !isNull && (filterInput.get(0) instanceof ModelBase);
        final boolean isBoolean = !isNull && ((clazz.equals(boolean.class))
                                              || (clazz.equals(Boolean.class)));
        Method getId = null;
        if (isModel) {
            try {
                getId = clazz.getMethod("getId");
            } catch (final NoSuchMethodException e) {
                Log.wtf(TAG,
                        "go and implement getId in " + clazz.getCanonicalName());
                throw new IllegalArgumentException("go and implement getId in "
                                                   + clazz.getCanonicalName(), e);
            }
        }
        final List<String> filter = new ArrayList<>(filterInput.size());
        for (final T el : filterInput) {
            if (isModel) {
                try {
                    filter.add(String.valueOf(getId.invoke(el)));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    Log.wtf(TAG,
                            "go and make getId in " + clazz.getCanonicalName()
                            + " accessible");
                    throw new IllegalArgumentException(
                        "go and implement getId in "
                        + clazz.getCanonicalName(), e);
                }
            } else if (isBoolean) {
                filter.add((Boolean) el ? "1" : "0");
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
        if ((op == Operation.IN) || (op == Operation.NOT_IN)) {
            appendConjunction(conjunction);
            this.selection.append(not).append(field).append(' ')
            .append(op.toString()).append('(');
            for (final String f : filter) {
                this.selectionArgs.add(f);
                this.selection.append("?,");
            }
            this.selection.deleteCharAt(this.selection.length() - 1);
            this.selection.append(')');
        } else {
            for (final String a : filter) {
                if (a != null) {
                    appendCondition(conjunction, not + field + ' ' + op + ' ' + a);
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
     * This is currently just a very primitive function and is not suitable for
     * production use
     *
     * @return
     */
    public String getQuery(final Uri uri) {
        final StringBuilder query = new StringBuilder(this.selection.length()
                + (this.projection.size() * 15) + (this.selectionArgs.size() * 15)
                + 100);
        query.append("SELECT ");
        if (this.distinct) {
            query.append("DISTINCT ");
        }
        query.append(TextUtils.join(", ", this.projection));
        query.append(" FROM ");
        query.append(MirakelInternalContentProvider.getTableName(uri));
        if (this.selection.length() > 0) {
            final String where = this.selection.toString();
            query.append(" WHERE ").append(where);
        }
        if (this.sortOrder.length() != 0) {
            query.append(" ORDER BY ").append(this.sortOrder);
        }
        return query.toString();
    }

    public String toString(final Uri u) {
        String query = getQuery(u);
        for (final String proj : selectionArgs) {
            query = query.replaceFirst("\\?", proj);
        }
        return query;
    }

    public String toString() {
        return toString(MirakelInternalContentProvider.TASK_VIEW_TAG_JOIN_URI);
    }

    public String getSelection() {
        return this.selection.toString();
    }

    public List<String> getSelectionArguments() {
        return this.selectionArgs;
    }

    public CursorWrapper query(final Uri uri) {
        final ContentResolver contentResolver = this.context
                                                .getContentResolver();
        return new CursorWrapper(contentResolver.query(uri, this.projection
                                 .toArray(new String[this.projection.size()]), this.selection
                                 .toString(), this.selectionArgs
                                 .toArray(new String[this.selectionArgs.size()]), this.sortOrder
                                 .toString()));
    }

    // and
    public <T extends Number> MirakelQueryBuilder and (final String field,
            final Operation op, final T filter) {
        return and (field, op, filter.toString());
    }

    public <T extends ModelBase> MirakelQueryBuilder and (final String field,
            final Operation op, final T filter) {
        return and (field, op, getId(filter));
    }

    public MirakelQueryBuilder and (final String field, final Operation op,
                                    final boolean filter) {
        return and (field, op, filter ? "1" : "0");
    }

    public MirakelQueryBuilder and (final String field, final Operation op,
                                    final String filter) {
        if (filter == null) {
            return and (field, op, Arrays.asList(new String[] { null }),
                        new ArrayList<String>(0));
        } else if ((op == Operation.IN) || (op == Operation.NOT_IN)) {
            return appendCondition(Conjunction.AND, field, op, Collections.singletonList(filter),
                                   new ArrayList<String>(0));
        }
        return and (field, op, Arrays.asList(new String[] { "?" }),
                    Collections.singletonList(filter));
    }

    /*
     * Do not call this with something other then T extends Number, T extends
     * ModelBase or T=String java does not allow to define functions in this way
     */
    public <T> MirakelQueryBuilder and (final String field, final Operation op,
                                        final List<T> filter) {
        return and (field, op, filter, new ArrayList<String>(0));
    }

    /*
     * Do not call this with something other then T extends Number, T extends
     * ModelBase or T=String java does not allow to define functions in this way
     */
    private <T> MirakelQueryBuilder and (final String field, final Operation op,
                                         final List<T> filter, final List<String> selectionArgs) {
        if ((op == Operation.IN) && !selectionArgs.isEmpty()) {
            throw new IllegalArgumentException("Call condition with in is only without selectionags supported");
        }
        return appendCondition(Conjunction.AND, field, op, filter,
                               selectionArgs);
    }

    public MirakelQueryBuilder and (final MirakelQueryBuilder other) {
        if (other.isNotEmpty()) {
            return appendCondition(Conjunction.AND, '(' + other.getSelection()
                                   + ')', other.selectionArgs);
        }
        return this;
    }

    public MirakelQueryBuilder and (final String field, final Operation op,
                                    final MirakelQueryBuilder subQuery, final Uri subqueryUri) {
        String not = "";
        if (NOT.contains(op)) {
            not = " NOT ";
        }
        return appendCondition(Conjunction.AND, not + field + ' ' + op,
                               subQuery, subqueryUri);
    }

    public MirakelQueryBuilder and (final String condition) {
        return appendCondition(Conjunction.AND, condition);
    }

    // or
    public <T extends Number> MirakelQueryBuilder or (final String field,
            final Operation op, final T filter) {
        return or (field, op, filter.toString());
    }

    public <T extends ModelBase> MirakelQueryBuilder or (final String field,
            final Operation op, final T filter) {
        return or (field, op, getId(filter));
    }

    public <T extends Number> MirakelQueryBuilder or (final String field,
            final Operation op, final boolean filter) {
        return or (field, op, filter ? "1" : "0");
    }

    /*
     * Do not call this with something other then T extends Number, T extends
     * ModelBase or T=String
     *
     * java does not allow to define functions in this way
     */
    public <T> MirakelQueryBuilder or (final String field, final Operation op,
                                       final List<T> filter) {
        return or (field, op, filter, new ArrayList<String>(0));
    }


    public MirakelQueryBuilder or (final String field, final Operation op,
                                   final String filter) {
        if (filter == null) {
            return or (field, op, Arrays.asList(new String[] { null }),
                       new ArrayList<String>(0));
        } else if (op == Operation.IN) {
            return appendCondition(Conjunction.OR, field, Operation.IN, Collections.singletonList(filter),
                                   new ArrayList<String>(0));
        }
        return or (field, op, Arrays.asList(new String[] {"?"}),
                   Collections.singletonList(filter));
    }

    /*
     * Do not call this with something other then T extends Number, T extends
     * ModelBase or T=String
     *
     * java does not allow to define functions in this way
     */
    private <T> MirakelQueryBuilder or (final String field, final Operation op,
                                        final List<T> filter, final List<String> selectionArgs) {
        if ((op == Operation.IN) && (!selectionArgs.isEmpty())) {
            throw new IllegalArgumentException("Call condition with in is only without selectionargs supported");
        }
        return appendCondition(Conjunction.OR, field, op, filter, selectionArgs);
    }

    public MirakelQueryBuilder or (final MirakelQueryBuilder other) {
        return appendCondition(Conjunction.OR,
                               '(' + other.getSelection() + ')', other.selectionArgs);
    }

    public MirakelQueryBuilder or (final String field, final Operation op,
                                   final MirakelQueryBuilder subQuery, final Uri subqueryUri) {
        String not = "";
        if (NOT.contains(op)) {
            not = " NOT ";
        }
        return appendCondition(Conjunction.OR, not + field + ' ' + op,
                               subQuery, subqueryUri);
    }

    public MirakelQueryBuilder or (final String condition) {
        return appendCondition(Conjunction.OR, condition);
    }

    public MirakelQueryBuilder not(final MirakelQueryBuilder other) {
        if (other.isNotEmpty()) {
            this.selection.append(" NOT (").append(other.getSelection())
            .append(')');
            this.selectionArgs.addAll(other.selectionArgs);
        }
        return this;
    }


    public <T extends ModelBase> List<T> getList(final Class<T> clazz) {
        return query(setupQueryBuilder(clazz)).doWithCursor(new Cursor2List<T>(clazz));
    }

    @NonNull
    public <T extends ModelBase> Optional<T> get(final Class<T> clazz, final long id) {
        and (ModelBase.ID, Operation.EQ, id);
        return get(clazz);
    }

    @NonNull
    public <T extends ModelBase> Optional<T> get(final Class<T> clazz) {
        return query(setupQueryBuilder(clazz)).doWithCursor(new
        CursorWrapper.CursorConverter<Optional<T>>() {
            @Override
            public Optional<T> convert(@NonNull final CursorGetter getter) {
                if (getter.moveToFirst()) {
                    return of(ModelFactory.createModel(getter, clazz));
                } else {
                    return absent();
                }
            }
        });
    }

    private <T> Uri setupQueryBuilder(final Class<T> clazz) {
        final Uri uri;
        try {
            uri = (Uri) clazz.getField("URI").get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException("go and implement a URI for "
                                               + clazz.getCanonicalName(), e);
        }
        if (this.projection.isEmpty()) {
            try {
                // can be null, because field should be static
                this.projection = Arrays.asList((String[]) clazz.getField(
                                                    "allColumns").get(null));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalArgumentException(
                    "go and implement allColumns for "
                    + clazz.getCanonicalName(), e);
            }
        }
        return uri;
    }

    public MirakelQueryBuilder sort(final String field, final Sorting s) {
        sort(field, s, null);
        return this;
    }

    public MirakelQueryBuilder sort(final String field, final Sorting s,
                                    final List<String> selectionArgs) {
        if (this.sortOrder.length() > 0) {
            this.sortOrder.append(", ");
        }
        this.sortOrder.append(field).append(' ').append(s);
        if (selectionArgs != null) {
            this.selectionArgs.addAll(selectionArgs);
        }
        return this;
    }

    public enum Conjunction {
        AND, OR;
    }

    public enum Sorting {
        ASC, DESC;
    }

    static final List<Operation> NOT = Arrays.asList(Operation.NOT_EQ,
                                       Operation.NOT_LIKE, Operation.NOT_GT, Operation.NOT_GE,
                                       Operation.NOT_LT, Operation.NOT_LE, Operation.NOT_IN);

    public enum Operation {
        EQ, LIKE, GT, GE, LT, LE, IN, NOT_EQ, NOT_LIKE, NOT_GT, NOT_GE, NOT_LT, NOT_LE, NOT_IN;

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
                throw new IllegalArgumentException("Unknown Operation "
                + super.toString());
            }
        }
    }

    public boolean isEmpty() {
        return selection.toString().trim().isEmpty() && selectionArgs.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }
}
