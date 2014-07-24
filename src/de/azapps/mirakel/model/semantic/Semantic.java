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

package de.azapps.mirakel.model.semantic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsPriorityProperty;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.task.Task;

public class Semantic extends SemanticBase {

    public static final String[] allColumns = { ID, CONDITION, PRIORITY, DUE,
                                                LIST, WEEKDAY
                                              };
    private static Map<String, Semantic> semantics = new HashMap<>();
    public static final String TABLE = "semantic_conditions";
    public static final Uri URI = MirakelInternalContentProvider.SEMANTIC_URI;

    @Override
    protected Uri getUri() {
        return URI;
    }

    public static List<Semantic> all() {
        return new MirakelQueryBuilder(context).getList(Semantic.class);
    }

    // Static

    public static List<Semantic> cursorToSemanticList(final Cursor c) {
        List<Semantic> ret = new ArrayList<>();
        if (c.moveToFirst()) {
            do {
                ret.add(new Semantic(c));
            } while (c.moveToNext());
        }
        c.close();
        return ret;
    }

    public static Task createTask(String taskName, ListMirakel currentList,
                                  final boolean useSemantic, final Context context) {
        GregorianCalendar due = null;
        int prio = 0;
        if (currentList != null && currentList.isSpecial()) {
            try {
                final SpecialList slist = (SpecialList) currentList;
                currentList = slist.getDefaultList();
                if (slist.getDefaultDate() != null) {
                    due = new GregorianCalendar();
                    due.add(Calendar.DAY_OF_MONTH, slist.getDefaultDate());
                }
                if (slist.getWhere().containsKey(Task.PRIORITY)) {
                    final SpecialListsPriorityProperty prop = (SpecialListsPriorityProperty) slist
                            .getWhere().get(Task.PRIORITY);
                    final boolean not = prop.isNegated();
                    prio = not ? -2 : 2;
                    final List<Integer> content = prop.getContent();
                    Collections.sort(content);
                    final int length = prop.getContent().size();
                    for (int i = not ? 0 : length - 1; not ? i < length
                         : i >= 0; i += not ? 1 : -1) {
                        if (not && prio == content.get(i)) {
                            --prio;
                        } else if (!not && prio == content.get(i)) {
                            prio = content.get(i);
                        }
                    }
                }
            } catch (final NullPointerException e) {
                currentList = ListMirakel.safeFirst(context);
            }
        }
        if (useSemantic) {
            GregorianCalendar tempdue = new GregorianCalendar();
            final String lowername = taskName.toLowerCase(Locale.getDefault());
            final List<String> words = new ArrayList<String>(
                Arrays.asList(lowername.split("\\s+")));
            while (words.size() > 1) {
                final String word = words.get(0);
                final Semantic s = semantics.get(word);
                if (s == null) {
                    break;
                }
                // Set due
                if (s.getDue() != null) {
                    tempdue.add(Calendar.DAY_OF_MONTH, s.getDue());
                    due = tempdue;
                }
                // Set priority
                if (s.getPriority() != null) {
                    prio = s.getPriority();
                }
                // Set list
                if (s.getList() != null) {
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
                    due = tempdue;
                }
                taskName = taskName.substring(word.length()).trim();
                words.remove(0);
            }
            if (due != null) {
                due.set(Calendar.HOUR_OF_DAY, 0);
                due.set(Calendar.MINUTE, 0);
                due.set(Calendar.SECOND, 0);
                due.add(Calendar.SECOND,
                        DateTimeHelper.getTimeZoneOffset(false, due));
            }
        }
        if (currentList == null) {
            currentList = ListMirakel.safeFirst(context);
        }
        return Task.newTask(taskName, currentList, due, prio);
    }

    public Semantic(final Cursor c) {
        super(c.getInt(c.getColumnIndex(ID)), c.getString(c
                .getColumnIndex(CONDITION)));
        Integer priority = null;
        if (!c.isNull(c.getColumnIndex(PRIORITY))) {
            priority = c.getInt(c.getColumnIndex(PRIORITY));
        }
        setPriority(priority);
        Integer due = null;
        if (!c.isNull(c.getColumnIndex(DUE))) {
            due = c.getInt(c.getColumnIndex(DUE));
        }
        setDue(due);
        ListMirakel list = null;
        if (!c.isNull(c.getColumnIndex(LIST))) {
            list = ListMirakel.get(c.getInt(c.getColumnIndex(LIST)));
        }
        setList(list);
        Integer weekday = null;
        if (!c.isNull(c.getColumnIndex(WEEKDAY))) {
            weekday = c.getInt(c.getColumnIndex(WEEKDAY));
        }
        setWeekday(weekday);
    }

    public static Semantic first() {
        return new MirakelQueryBuilder(context).get(Semantic.class);
    }

    /**
     * Get a Semantic by id
     *
     * @param id
     * @return
     */
    public static Semantic get(final long id) {
        return new MirakelQueryBuilder(context).and(ID, Operation.EQ, id).get(
                   Semantic.class);
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
                                       final Integer priority, final Integer due, final ListMirakel list,
                                       final Integer weekday) {
        final Semantic m = new Semantic(0, condition, priority, due, list,
                                        weekday);
        return m.create();
    }

    Semantic(final int id, final String condition, final Integer priority,
             final Integer due, final ListMirakel list, final Integer weekday) {
        super(id, condition, priority, due, list, weekday);
    }

    public Semantic create() {
        final ContentValues values = getContentValues();
        values.remove(ID);
        final long insertId = insert(URI, values);
        initAll();
        return Semantic.get(insertId);
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
}
