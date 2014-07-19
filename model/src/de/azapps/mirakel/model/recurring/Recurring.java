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

package de.azapps.mirakel.model.recurring;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Pair;
import android.util.SparseBooleanArray;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class Recurring extends RecurringBase {
    public static final String TABLE = "recurring";
    private final static String TAG = "Recurring";

    private final static String[] allColumns = { ID, LABEL, MINUTES, HOURS, DAYS, MONTHS, YEARS, FOR_DUE, START_DATE, END_DATE, TEMPORARY, EXACT, MONDAY,
                                                 TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY, DERIVED
                                               };
    public final static String[] allTWColumns = { ID, PARENT, CHILD, OFFSET, OFFSET_COUNT};
    public static final String TW_TABLE = "recurring_tw_mask";

    protected Uri getUri() {
        return URI;
    }

    private static final Uri URI = MirakelInternalContentProvider.RECURRING_URI;

    public Recurring(final long _id, final String label, final int minutes,
                     final int hours, final int days, final int months, final int years,
                     final boolean forDue, final Calendar startDate,
                     final Calendar endDate, final boolean temporary,
                     final boolean isExact, final SparseBooleanArray weekdays,
                     final Long derivedFrom) {
        super(_id, label, minutes, hours, days, months, years, forDue,
              startDate, endDate, temporary, isExact, weekdays, derivedFrom);
    }


    // Static


    public static Recurring createTemporayCopy(final Recurring r) {
        final Recurring newR = new Recurring(0, r.getLabel(), r.getMinutes(),
                                             r.getHours(), r.getDays(), r.getMonths(), r.getYears(),
                                             r.isForDue(), r.getStartDate(), r.getEndDate(), true,
                                             r.isExact(), r.getWeekdaysRaw(), r.getId());
        return newR.create();
    }

    public static Recurring newRecurring(final String label, final int minutes,
                                         final int hours, final int days, final int months, final int years,
                                         final boolean forDue, final Calendar startDate,
                                         final Calendar endDate, final boolean temporary,
                                         final boolean isExact, final SparseBooleanArray weekdays) {
        final Recurring r = new Recurring(0, label, minutes, hours, days,
                                          months, years, forDue, startDate, endDate, temporary, isExact,
                                          weekdays, null);
        return r.create();
    }

    public Recurring create() {
        final ContentValues values = getContentValues();
        values.remove(ModelBase.ID);
        setId(insert(URI, values));
        return Recurring.get(getId());
    }

    /**
     * Get a Semantic by id
     *
     * @param id
     * @return
     */
    public static Recurring get(final long id) {
        final Cursor cursor = query(URI, allColumns, "_id=" + id,
                                    null, null);
        if (cursor.moveToFirst()) {
            final Recurring r = cursorToRecurring(cursor);
            cursor.close();
            return r;
        }
        cursor.close();
        return null;
    }

    public static Recurring get(final int minutes, final int hours,
                                final int days, final int month, final int years,
                                final Calendar start, final Calendar end) {
        String cal = "";
        if (start != null) {
            cal += " " + START_DATE + "=" + DateTimeHelper.formatDateTime(start);
        }
        if (end != null) {
            cal = cal + ("".equals(cal) ? "" : " AND") + " " + END_DATE + "="
                  + DateTimeHelper.formatDateTime(end);
        }
        final Cursor cursor = query(URI, allColumns, MINUTES + "="
                                    + minutes + " AND " + HOURS + "=" + hours + " AND " + DAYS + "=" + days
                                    + " AND " + MONTHS + "=" + month + " AND " + YEARS + "=" + years + cal, null, null);
        if (cursor.moveToFirst()) {
            final Recurring r = cursorToRecurring(cursor);
            cursor.close();
            return r;
        }
        cursor.close();
        return null;
    }

    public static Recurring get(final int days, final int month, final int years) {
        return get(0, 0, days, month, years, null, null);
    }

    public static Recurring first() {
        final Cursor cursor = query(URI, allColumns, null, null, null);
        if (cursor.moveToFirst()) {
            final Recurring r = cursorToRecurring(cursor);
            cursor.close();
            return r;
        }
        cursor.close();
        return null;
    }

    public static void destroyTemporary(final long recurrenceId) {
        delete(URI, TEMPORARY + "=1 AND " + ID + "=" + recurrenceId, null);
    }

    public void destroy() {
        MirakelInternalContentProvider.withTransaction(new MirakelInternalContentProvider.DBTransaction() {
            @Override
            public void exec() {
                delete(URI, ID + "=" + getId(), null);
                // Fix recurring onDelete in TaskTable
                ContentValues cv = new ContentValues();
                cv.put(Task.RECURRING, -1);
                update(MirakelInternalContentProvider.TASK_URI, cv, Task.RECURRING + "=" + getId(), null);
                cv = new ContentValues();
                cv.put(Task.RECURRING_REMINDER, -1);
                update(MirakelInternalContentProvider.TASK_URI, cv, Task.RECURRING_REMINDER + "=" + getId(), null);
            }
        });
    }

    public static List<Recurring> all() {
        final Cursor c = query(URI, allColumns, null, null, null);
        c.moveToFirst();
        final List<Recurring> all = new ArrayList<Recurring>();
        while (!c.isAfterLast()) {
            all.add(cursorToRecurring(c));
            c.moveToNext();
        }
        c.close();
        return all;
    }


    private static Recurring cursorToRecurring(final Cursor c) {
        Calendar start;
        try {
            start = DateTimeHelper.parseDateTime(c.getString(c.getColumnIndex(START_DATE)));
        } catch (final ParseException e) {
            start = null;
            Log.d(TAG, "cannot parse Date");
        }
        Calendar end;
        try {
            end = DateTimeHelper.parseDateTime(c.getString(c.getColumnIndex(END_DATE)));
        } catch (final ParseException e) {
            Log.d(TAG, "cannot parse Date");
            end = null;
        }
        final SparseBooleanArray weekdays = new SparseBooleanArray();
        weekdays.put(Calendar.MONDAY, c.getShort(c.getColumnIndex(MONDAY)) == 1);
        weekdays.put(Calendar.TUESDAY, c.getShort(c.getColumnIndex(TUESDAY)) == 1);
        weekdays.put(Calendar.WEDNESDAY, c.getShort(c.getColumnIndex(WEDNESDAY)) == 1);
        weekdays.put(Calendar.THURSDAY, c.getShort(c.getColumnIndex(THURSDAY)) == 1);
        weekdays.put(Calendar.FRIDAY, c.getShort(c.getColumnIndex(FRIDAY)) == 1);
        weekdays.put(Calendar.SATURDAY, c.getShort(c.getColumnIndex(SATURDAY)) == 1);
        weekdays.put(Calendar.SUNDAY, c.getShort(c.getColumnIndex(SUNDAY)) == 1);
        final Long derivedFrom = c.isNull(c.getColumnIndex(DERIVED)) ? null : c.getLong(c.getColumnIndex(
                                     DERIVED));
        return new Recurring(c.getLong(c.getColumnIndex(ID)), c.getString(c.getColumnIndex(LABEL)),
                             c.getInt(c.getColumnIndex(MINUTES)),
                             c.getInt(c.getColumnIndex(HOURS)), c.getInt(c.getColumnIndex(DAYS)),
                             c.getInt(c.getColumnIndex(MONTHS)), c.getInt(c.getColumnIndex(YEARS)),
                             c.getShort(c.getColumnIndex(FOR_DUE)) == 1, start, end,
                             c.getShort(c.getColumnIndex(TEMPORARY)) == 1,
                             c.getShort(c.getColumnIndex(EXACT)) == 1, weekdays, derivedFrom);
    }

    public Task incrementRecurringDue(final Task t) {
        if (t.getDue() == null) {
            return t;
        }
        final Calendar newDue = addRecurring((Calendar) t.getDue().clone(),
                                             true);
        long masterID = t.getId();
        long offset = 0;
        long offsetCount = 0;
        Cursor c = query(MirakelInternalContentProvider.RECURRING_TW_URI, allTWColumns, CHILD + "=?",
                         new String[] {t.getId() + ""}, null);
        if (c.moveToFirst()) {// this is already a child-task
            masterID = c.getLong(1);
            offset = c.getLong(3);
            offsetCount = c.getLong(4);
        }
        c.close();
        offset += newDue.getTimeInMillis() - t.getDue().getTimeInMillis();
        ++offsetCount;
        c = query(MirakelInternalContentProvider.RECURRING_TW_URI, new String[] {CHILD},
                  PARENT + "=? AND " + OFFSET_COUNT + "=?", new String[] {masterID + "", offsetCount + ""}, null);
        if (c.moveToFirst()) {
            final Task task = Task.get(c.getLong(0));
            c.close();
            if (task != null) {
                return task;
            }
        }
        c.close();
        t.setDue(newDue);
        Task newTask;
        try {
            newTask = t.create();
        } catch (final NoSuchListException e) {
            Log.wtf(TAG, "list vanished", e);
            ErrorReporter.report(ErrorType.LIST_VANISHED);
            return t;
        }
        ContentValues cv = new ContentValues();
        cv.put(Task.RECURRING, t.getRecurrenceId());
        cv.put(Task.UUID, UUID.randomUUID().toString());
        cv.put(DatabaseHelper.SYNC_STATE_FIELD, SYNC_STATE.ADD.toInt());
        update(MirakelInternalContentProvider.TASK_URI, cv, ModelBase.ID + "=?",
               new String[] {newTask.getId() + ""});
        cv = new ContentValues();
        cv.put(PARENT, masterID);
        cv.put(CHILD, newTask.getId());
        cv.put(OFFSET, offset);
        cv.put(OFFSET_COUNT, offsetCount);
        insert(MirakelInternalContentProvider.RECURRING_TW_URI, cv);
        return newTask;
    }

    public Calendar addRecurring(final Calendar c) {
        return addRecurring(c, false);
    }

    private Calendar addRecurring(Calendar c, final boolean onlyOnce) {
        final Calendar now = new GregorianCalendar();
        if (isExact()) {
            c = now;
        }
        now.set(Calendar.SECOND, 0);
        now.add(Calendar.MINUTE, -1);
        final List<Integer> weekdays = getWeekdays();
        if (weekdays.size() == 0) {
            if ((getStartDate() == null || getStartDate() != null
                 && now.after(getStartDate()))
                && (getEndDate() == null || getEndDate() != null
                    && now.before(getEndDate()))) {
                do {
                    c.add(Calendar.DAY_OF_MONTH, getDays());
                    c.add(Calendar.MONTH, getMonths());
                    c.add(Calendar.YEAR, getYears());
                    if (!isForDue()) {
                        c.add(Calendar.MINUTE, getMinutes());
                        c.add(Calendar.HOUR, getHours());
                    }
                } while (c.before(now) && !onlyOnce);
            }
        } else {
            int diff = 8;
            if (c.compareTo(now) < 0) {
                c = now;
            }
            c.add(Calendar.DAY_OF_MONTH, 1);
            for (final Integer day : weekdays) {
                int local_diff = day - c.get(Calendar.DAY_OF_WEEK);
                if (local_diff < 0) {
                    local_diff += 7;
                }
                if (diff > local_diff) {
                    diff = local_diff;
                }
            }
            c.add(Calendar.DAY_OF_MONTH, diff);
        }
        return c;
    }

    public static List<Pair<Integer, String>> getForDialog(final boolean isDue) {
        String where = TEMPORARY + "=0";
        if (isDue) {
            where += " AND " + FOR_DUE + "=1";
        }
        final Cursor c = query(URI, new String[] {ID, LABEL},
                               where, null, null);
        final List<Pair<Integer, String>> ret = new ArrayList<>();
        c.moveToFirst();
        while (!c.isAfterLast()) {
            ret.add(new Pair<>(c.getInt(0), c.getString(1)));
            c.moveToNext();
        }
        c.close();
        return ret;
    }

}
