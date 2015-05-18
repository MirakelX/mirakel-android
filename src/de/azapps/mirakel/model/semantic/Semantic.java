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

package de.azapps.mirakel.model.semantic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsPriorityProperty;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;
import de.azapps.tools.OptionalUtils;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class Semantic extends SemanticBase {

    public static final String[] allColumns = { ID, CONDITION, PRIORITY, DUE,
                                                LIST, WEEKDAY
                                              };
    private static final String TAG = "de.azapps.mirakel.model.semantic.Semantic";
    private static final Pattern SPLIT_BY_WHITESPACE = Pattern.compile("\\s+");
    private static Map<String, Semantic> semantics = new HashMap<>();
    public static final String TABLE = "semantic_conditions";
    public static final Uri URI = MirakelInternalContentProvider.SEMANTIC_URI;

    public Semantic(final Cursor c) {
        super(c.getLong(c.getColumnIndex(ID)), c.getString(c
                .getColumnIndex(CONDITION)));
        if (!c.isNull(c.getColumnIndex(PRIORITY))) {
            priority = of(c.getInt(c.getColumnIndex(PRIORITY)));
        }
        if (!c.isNull(c.getColumnIndex(DUE))) {
            due = of(c.getInt(c.getColumnIndex(DUE)));
        }
        if (!c.isNull(c.getColumnIndex(LIST))) {
            list = ListMirakel.get(c.getLong(c.getColumnIndex(LIST)));
        }
        if (!c.isNull(c.getColumnIndex(WEEKDAY))) {
            weekday = of(c.getInt(c.getColumnIndex(WEEKDAY)));
        }

    }

    private Semantic(final Parcel in) {
        super();
        setId(in.readLong());
        setName(in.readString());
        priority = OptionalUtils.readFromParcel(in, Integer.class);
        due = OptionalUtils.readFromParcel(in, Integer.class);
        list = OptionalUtils.readFromParcel(in, ListMirakel.class);
        weekday = OptionalUtils.readFromParcel(in, Integer.class);
    }

    public Semantic(final int id, final String condition, final Integer priority,
                    final Integer due, final Optional<ListMirakel> list, final Integer weekday) {
        super(id, condition, priority, due, list, weekday);
    }

    @Override
    protected Uri getUri() {
        return URI;
    }

    @NonNull
    public static List<Semantic> all() {
        return new MirakelQueryBuilder(context).getList(Semantic.class);
    }

    // Static

    @NonNull
    public static List<Semantic> cursorToSemanticList(final Cursor c) {
        final List<Semantic> ret = new ArrayList<>(c.getCount());
        if (c.moveToFirst()) {
            do {
                ret.add(new Semantic(c));
            } while (c.moveToNext());
        }
        c.close();
        return ret;
    }

    @NonNull
    public static Task createTask(final String taskName, final Optional<ListMirakel> currentList,
                                  final boolean useSemantic, final Context context) {
        Task stubTask = createStubTask(taskName, currentList, useSemantic, context);
        try {
            return stubTask.create();
        } catch (final DefinitionsHelper.NoSuchListException e) {
            ErrorReporter.report(ErrorType.TASKS_NO_LIST);
            Log.e(TAG, "NoSuchListException", e);
            // This could only happen if the list vanishes while calling this function
            throw new IllegalStateException("This can never ever happen", e);
        }
    }

    @NonNull
    public static Task createStubTask(String taskName, Optional<ListMirakel> currentList,
                                      final boolean useSemantic, final Context context) {
        Optional<Calendar> due = absent();
        int prio = 0;
        if (currentList.isPresent() && currentList.get().isSpecial()) {
            try {
                final SpecialList slist = currentList.get().toSpecial().get();
                currentList = Optional.fromNullable(slist.getDefaultList());
                if (slist.getDefaultDate() != null) {
                    due = of((Calendar)new GregorianCalendar());
                    due.get().add(Calendar.DAY_OF_MONTH, slist.getDefaultDate());
                }
                if (slist.getWhere().isPresent() &&
                slist.getWhere().transform(new Function<SpecialListsBaseProperty, Boolean>() {
                @Override
                public Boolean apply(final SpecialListsBaseProperty input) {
                        return input instanceof  SpecialListsPriorityProperty;
                    }
                }).or(Boolean.FALSE)) {
                    final SpecialListsPriorityProperty prop = (SpecialListsPriorityProperty) slist
                            .getWhere().get();
                    final boolean not = prop.isSet();
                    prio = not ? -2 : 2;
                    final List<Integer> content = prop.getContent();
                    Collections.sort(content);
                    final int length = prop.getContent().size();
                    for (int i = not ? 0 : (length - 1); not ? (i < length) : (i >= 0); i += not ? 1 : -1) {
                        if (not && (prio == content.get(i))) {
                            --prio;
                        } else if (!not && (prio == content.get(i))) {
                            prio = content.get(i);
                        }
                    }
                }
            } catch (final NullPointerException ignored) {
                currentList = Optional.fromNullable(ListMirakel.safeFirst());
            }
        }
        if (useSemantic) {
            Calendar tempdue = new GregorianCalendar();
            final String lowername = taskName.toLowerCase(Locale.getDefault());
            final List<String> words = new ArrayList<>(Arrays.asList(SPLIT_BY_WHITESPACE.split(lowername)));
            while (words.size() > 1) {
                final String word = words.get(0);
                final Semantic s = semantics.get(word);
                if (s == null) {
                    break;
                }
                // Set due
                if (s.getDue() != null) {
                    tempdue.add(Calendar.DAY_OF_MONTH, s.getDue());
                    due = of(tempdue);
                }
                // Set priority
                if (s.getPriority() != null) {
                    prio = s.getPriority();
                }
                // Set list
                if (s.getList().isPresent()) {
                    currentList = s.getList();
                }
                // Weekday?
                if (s.getWeekday() != null) {
                    tempdue = new GregorianCalendar();
                    int nextWeekday = s.getWeekday() + 1;
                    // Because there are some dudes which means, sunday is the
                    // first day of the week… That's obviously wrong!
                    if (nextWeekday == 8) {
                        nextWeekday = 1;
                    }
                    do {
                        tempdue.add(Calendar.DAY_OF_YEAR, 1);
                    } while (tempdue.get(Calendar.DAY_OF_WEEK) != nextWeekday);
                    due = of(tempdue);
                }
                taskName = taskName.substring(word.length()).trim();
                words.remove(0);
            }
            if (due.isPresent()) {
                due.get().set(Calendar.HOUR_OF_DAY, 0);
                due.get().set(Calendar.MINUTE, 0);
                due.get().set(Calendar.SECOND, 0);
                due.get().add(Calendar.SECOND, DateTimeHelper.getTimeZoneOffset(false, due.get()));
            }
        }
        if (!currentList.isPresent()) {
            currentList = Optional.fromNullable(ListMirakel.safeFirst());
        }
        return new Task(taskName, currentList.get(), due, prio);
    }

    public static Optional<Semantic> first() {
        return new MirakelQueryBuilder(context).get(Semantic.class);
    }

    /**
     * Get a Semantic by id
     *
     * @param id
     * @return
     */
    public static Optional<Semantic> get(final long id) {
        return new MirakelQueryBuilder(context).and(ID, Operation.EQ, id).get(Semantic.class);
    }

    /**
     * Initialize the Database and the preferences
     *
     * @param context
     *            The Application-Context
     */
    public static void init(final Context context) {
        ModelBase.init(context);
        initAll();
    }

    private static void initAll() {
        for (final Semantic s : all()) {
            semantics.put(s.getCondition(), s);
        }
    }

    public static Semantic newSemantic(final String condition,
                                       final Integer priority, final Integer due, final Optional<ListMirakel> list,
                                       final Integer weekday) {
        final Semantic semantic = new Semantic(0, condition, priority, due, list,
                                               weekday);
        return semantic.create();
    }


    public Semantic create() {
        final ContentValues values = getContentValues();
        values.remove(ID);
        final long insertId = insert(URI, values);
        initAll();
        return Semantic.get(insertId).get();
    }

    @Override
    public void destroy() {
        super.destroy();
        initAll();
    }

    @Override
    public void save() {
        super.save();
        initAll();
    }

    // Parcelable stuff

    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeLong(getId());
        dest.writeString(getName());
        OptionalUtils.writeToParcel(dest, priority);
        OptionalUtils.writeToParcel(dest, due);
        OptionalUtils.writeToParcel(dest, list);
        OptionalUtils.writeToParcel(dest, weekday);

    }

    public static final Creator<Semantic> CREATOR = new Creator<Semantic>() {
        @Override
        public Semantic createFromParcel(final Parcel source) {
            return new Semantic(source);
        }
        @Override
        public Semantic[] newArray(final int size) {
            return new Semantic[size];
        }
    };
}
