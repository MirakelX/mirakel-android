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
import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.AnalyticsWrapperBase;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsPriorityProperty;
import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.query_builder.CursorGetter;
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

    public Semantic(final @NonNull CursorGetter c) {
        super(c.getLong(ID), c.getString(CONDITION));
        priority = c.getOptional(PRIORITY, Integer.class);
        due = c.getOptional(DUE, Integer.class);
        list = c.getOptional(LIST, ListMirakel.class);
        weekday = c.getOptional(WEEKDAY, Integer.class);
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
    public static Task createTask(final String taskName, final Optional<ListMirakel> currentList,
                                  final boolean useSemantic) {
        final Task stubTask = createStubTask(taskName, currentList, useSemantic);
        try {
            return stubTask.create();
        } catch (final DefinitionsHelper.NoSuchListException e) {
            ErrorReporter.report(ErrorType.TASKS_NO_LIST);
            Log.e(TAG, "NoSuchListException", e);
            // This could only happen if the list vanishes while calling this function
            throw new IllegalStateException("This can never ever happen", e);
        }
    }

    private static DateTime getNormalizedCalendar() {
        return new DateTime().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0);
    }

    public void apply(@NonNull final Task task) {
        if (getPriority().isPresent()) {
            task.setPriority(getPriority().get());
        }
        if (getDue().isPresent()) {
            final DateTime due = getNormalizedCalendar();
            due.plusDays(getDue().get());
            task.setDue(of(due));
        }
        if (getList().isPresent()) {
            task.setList(getList().get());
        }
        if (getWeekday().isPresent()) {
            final DateTime due = getNormalizedCalendar();
            int nextWeekday = getWeekday().get() + 1;
            // Because there are some dudes which means, sunday is the
            // first day of the week… That's obviously wrong!
            final Calendar cal = due.toGregorianCalendar();
            if (nextWeekday == 8) {
                nextWeekday = 1;
            }
            do {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            } while (cal.get(Calendar.DAY_OF_WEEK) != nextWeekday);
            task.setDue(of(new DateTime(cal)));
        }
    }

    public static boolean applySemantics(@NonNull final Task task, @NonNull String taskName) {
        final String lowername = taskName.toLowerCase(Locale.getDefault());
        final String[] words = SPLIT_BY_WHITESPACE.split(lowername);
        boolean change = false;
        for (final String word : words) {
            final Semantic semantic = semantics.get(word);
            if (semantic == null) {
                break;
            }
            change = true;
            semantic.apply(task);
            taskName = taskName.substring(word.length()).trim();
        }
        task.setName(taskName);
        if (change) {
            AnalyticsWrapperBase.track(AnalyticsWrapperBase.ACTION.USED_SEMANTICS);
        }
        return change;
    }

    private static ListMirakel getDefaultList(final @NonNull Optional<AccountMirakel>
            accountMirakelOptional) {
        if (accountMirakelOptional.isPresent()) {
            return ListMirakel.getInboxList(accountMirakelOptional.get());
        } else {
            return getDefaultList(of(MirakelModelPreferences.getDefaultAccount()));
        }
    }

    @NonNull
    public static Task createStubTask(final @NonNull String taskName,
                                      @NonNull Optional<ListMirakel> currentList,
                                      final boolean useSemantic) {
        Optional<DateTime> due = absent();
        if (currentList.isPresent() && currentList.get().isSpecial()) {
            try {
                final SpecialList slist = currentList.get().toSpecial().get();
                currentList = Optional.fromNullable(slist.getDefaultList());
                if (slist.getDefaultDate().isPresent()) {
                    due = of(new DateTime().withDayOfMonth(slist.getDefaultDate().get()));
                }
                // calculate Priority
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
                    int prio = not ? -2 : 2;
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
                currentList = of(getDefaultList(of(currentList.get().getAccount())));
            }
        }
        final ListMirakel listMirakel;
        if (!currentList.isPresent()) {
            listMirakel = getDefaultList(Optional.<AccountMirakel>absent());
        } else {
            listMirakel = currentList.get();
        }
        final Task task = new Task(taskName, listMirakel, due, 0);
        if (useSemantic) {
            applySemantics(task, taskName);
        }
        return task;
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
    public static void init() {
        initAll();
    }

    private static void initAll() {
        for (final Semantic s : all()) {
            semantics.put(s.getCondition().toLowerCase(), s);
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
