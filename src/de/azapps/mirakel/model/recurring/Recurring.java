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
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.util.SparseBooleanArray;

import com.google.common.base.Optional;

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
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;

public class Recurring extends RecurringBase {
    public static final String TABLE = "recurring";
    private final static String TAG = "Recurring";

    public final static String[] allColumns = { ID, LABEL, MINUTES, HOURS, DAYS, MONTHS, YEARS, FOR_DUE, START_DATE, END_DATE, TEMPORARY, EXACT, MONDAY,
                                                TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY, DERIVED
                                              };
    public final static String[] allTWColumns = { ID, PARENT, CHILD, OFFSET, OFFSET_COUNT};
    public static final String TW_TABLE = "recurring_tw_mask";

    protected Recurring() {
        // nothing
    }

    protected Uri getUri() {
        return URI;
    }

    public static final Uri URI = MirakelInternalContentProvider.RECURRING_URI;

    public Recurring(final long _id, final String label, final int minutes,
                     final int hours, final int days, final int months, final int years,
                     final boolean forDue, @NonNull final Optional<Calendar> startDate,
                     @NonNull final Optional<Calendar> endDate, final boolean temporary,
                     final boolean isExact, @NonNull final SparseBooleanArray weekdays,
                     @NonNull final Optional<Long> derivedFrom) {
        super(_id, label, minutes, hours, days, months, years, forDue,
              startDate, endDate, temporary, isExact, weekdays, derivedFrom);
    }


    // Static

    @NonNull
    public static Recurring createTemporaryCopy(final Recurring r) {
        final Recurring newR = new Recurring(0, r.getLabel(), r.getMinutes(),
                                             r.getHours(), r.getDays(), r.getMonths(), r.getYears(),
                                             r.isForDue(), r.getStartDate(), r.getEndDate(), true,
                                             r.isExact(), r.getWeekdaysRaw(), Optional.of(r.getId()));
        return newR.create();
    }

    @NonNull
    public static Recurring newRecurring(final String label, final int minutes,
                                         final int hours, final int days, final int months, final int years,
                                         final boolean forDue, final Optional<Calendar> startDate,
                                         final Optional<Calendar> endDate, final boolean temporary,
                                         final boolean isExact, final SparseBooleanArray weekdays) {
        final Recurring r = new Recurring(0, label, minutes, hours, days,
                                          months, years, forDue, startDate, endDate, temporary, isExact,
                                          weekdays, Optional.<Long>absent());
        return r.create();
    }

    @NonNull
    public Recurring create() {
        final ContentValues values = getContentValues();
        values.remove(ModelBase.ID);
        setId(insert(URI, values));
        return Recurring.get(getId()).get();
    }

    /**
     * Get a Semantic by id
     *
     * @param id
     * @return
     */
    @NonNull
    public static Optional<Recurring> get(final long id) {
        return fromNullable(new MirakelQueryBuilder(context).get(Recurring.class, id));
    }

    @NonNull
    public static Optional<Recurring> get(final int minutes, final int hours,
                                          final int days, final int month, final int years,
                                          final Calendar start, final Calendar end) {
        MirakelQueryBuilder qb = new MirakelQueryBuilder(context).and(MINUTES,
                Operation.EQ, minutes).and(HOURS, Operation.EQ, hours)
        .and(DAYS, Operation.EQ, days).and(MONTHS, Operation.EQ,
                                           month).and(YEARS, Operation.EQ, years);
        if (start != null) {
            qb.and(START_DATE, Operation.EQ, DateTimeHelper.formatDateTime(start));
        } else {
            qb.and(START_DATE, Operation.EQ, (String)null);
        }
        if (end != null) {
            qb.and(END_DATE, Operation.EQ, DateTimeHelper.formatDateTime(end));
        } else {
            qb.and(END_DATE, Operation.EQ, (String)null);
        }
        return fromNullable(qb.get(Recurring.class));
    }

    @NonNull
    public static Optional<Recurring> get(final int days, final int month, final int years) {
        return get(0, 0, days, month, years, null, null);
    }


    public static void destroyTemporary(final long recurrenceId) {
        delete(URI, TEMPORARY + "=1 AND " + ID + "=" + recurrenceId, null);
    }

    public void destroy() {
        MirakelInternalContentProvider.withTransaction(new MirakelInternalContentProvider.DBTransaction() {
            @Override
            public void exec() {
                delete(URI, ID + '=' + getId(), null);
                // Fix recurring onDelete in TaskTable
                final ContentValues cv = new ContentValues();
                cv.put(Task.RECURRING, -1);
                update(MirakelInternalContentProvider.TASK_URI, cv, Task.RECURRING + '=' + getId(), null);
                final ContentValues contentValues = new ContentValues();
                contentValues.put(Task.RECURRING_REMINDER, -1);
                update(MirakelInternalContentProvider.TASK_URI, contentValues,
                       Task.RECURRING_REMINDER + '=' + getId(), null);
            }
        });
    }

    public static List<Recurring> all() {
        return new MirakelQueryBuilder(context).getList(Recurring.class);
    }


    public Recurring(final Cursor c) {
        super(c.getLong(c.getColumnIndex(ID)), c.getString(c.getColumnIndex(LABEL)));
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
        setMinutes(c.getInt(c.getColumnIndex(MINUTES)));
        setHours(c.getInt(c.getColumnIndex(HOURS)));
        setDays(c.getInt(c.getColumnIndex(DAYS)));
        setMonths(c.getInt(c.getColumnIndex(MONTHS)));
        setYears(c.getInt(c.getColumnIndex(YEARS)));
        setForDue(c.getShort(c.getColumnIndex(FOR_DUE)) == 1);
        setStartDate(fromNullable(start));
        setEndDate(fromNullable(end));
        setTemporary(c.getShort(c.getColumnIndex(TEMPORARY)) == 1);
        setExact(c.getShort(c.getColumnIndex(EXACT)) == 1);
        setWeekdays(weekdays);
        setDerivedFrom(fromNullable(derivedFrom));
    }

    @NonNull
    public Task incrementRecurringDue(final Task t) {
        if (!t.getDue().isPresent()) {
            return t;
        }
        final Calendar newDue = addRecurring( of((Calendar)t.getDue().get().clone()), true).get();
        if (newDue.compareTo(t.getDue().get()) == 0) {
            return t;
        }
        long masterID = t.getId();
        long offset = 0;
        long offsetCount = 0;
        Cursor c = new MirakelQueryBuilder(context).select(allTWColumns).and(CHILD,
                Operation.EQ, t).query(MirakelInternalContentProvider.RECURRING_TW_URI);
        if (c.moveToFirst()) {// this is already a child-task
            masterID = c.getLong(1);
            offset = c.getLong(3);
            offsetCount = c.getLong(4);
        }
        c.close();
        offset += newDue.getTimeInMillis() - t.getDue().get().getTimeInMillis();
        ++offsetCount;
        c = new MirakelQueryBuilder(context).select(CHILD).and(PARENT, Operation.EQ,
                masterID).and(OFFSET_COUNT, Operation.EQ,
                              offsetCount).query(MirakelInternalContentProvider.RECURRING_TW_URI);
        if (c.moveToFirst()) {
            final Optional<Task> task = Task.get(c.getLong(0));
            c.close();
            if (task.isPresent()) {
                return task.get();
            }
        }
        c.close();
        t.setDue(of(newDue));
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
               new String[] {String.valueOf(newTask.getId())});
        cv = new ContentValues();
        cv.put(PARENT, masterID);
        cv.put(CHILD, newTask.getId());
        cv.put(OFFSET, offset);
        cv.put(OFFSET_COUNT, offsetCount);
        insert(MirakelInternalContentProvider.RECURRING_TW_URI, cv);
        return newTask;
    }

    @NonNull
    public Optional<Calendar> addRecurring(final @NonNull Optional<Calendar> c) {
        return addRecurring(c, false);
    }

    @NonNull
    private Optional<Calendar> addRecurring(@NonNull Optional<Calendar> cal, final boolean onlyOnce) {
        if (!cal.isPresent()) {
            return absent();
        }
        Calendar c = cal.get();
        final Calendar now = new GregorianCalendar();
        if (isExact()) {
            c = now;
        }
        now.set(Calendar.SECOND, 0);
        now.add(Calendar.MINUTE, -1);
        final List<Integer> weekdays = getWeekdays();
        if (weekdays.size() == 0) {
            if ((!getStartDate().isPresent() || now.after(getStartDate().get()))
                && (!getEndDate().isPresent() || now.before(getEndDate().get()))) {
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
        return of(c);
    }

    public static List<Pair<Integer, String>> getForDialog(final boolean isDue) {
        MirakelQueryBuilder qb = new MirakelQueryBuilder(context).and(TEMPORARY,
                Operation.EQ, false);
        if (isDue) {
            qb.and(FOR_DUE, Operation.EQ, true);
        }
        final Cursor c = qb.select(ID, LABEL).query(URI);
        final List<Pair<Integer, String>> ret = new ArrayList<>();
        c.moveToFirst();
        while (!c.isAfterLast()) {
            ret.add(new Pair<>(c.getInt(0), c.getString(1)));
            c.moveToNext();
        }
        c.close();
        return ret;
    }

    // Parcelable stuff

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.minutes);
        dest.writeInt(this.hours);
        dest.writeInt(this.days);
        dest.writeInt(this.months);
        dest.writeInt(this.years);
        dest.writeByte(forDue ? (byte) 1 : (byte) 0);
        dest.writeSerializable(this.startDate);
        dest.writeSerializable(this.endDate);
        dest.writeByte(temporary ? (byte) 1 : (byte) 0);
        dest.writeByte(isExact ? (byte) 1 : (byte) 0);
        dest.writeSparseBooleanArray(this.weekdays);
        dest.writeSerializable(this.derivedFrom);
        dest.writeLong(getId());
        dest.writeString(getName());
    }

    @SuppressWarnings("unchecked") // Unchecked cast
    private Recurring(Parcel in) {
        super();
        this.minutes = in.readInt();
        this.hours = in.readInt();
        this.days = in.readInt();
        this.months = in.readInt();
        this.years = in.readInt();
        this.forDue = in.readByte() != 0;
        this.startDate = (Optional<Calendar>) in.readSerializable();
        this.endDate = (Optional<Calendar>) in.readSerializable();
        this.temporary = in.readByte() != 0;
        this.isExact = in.readByte() != 0;
        this.weekdays = in.readSparseBooleanArray();
        this.derivedFrom = (Optional<Long>) in.readSerializable();
        setId(in.readLong());
        setName(in.readString());
    }

    public static final Creator<Recurring> CREATOR = new Creator<Recurring>() {
        public Recurring createFromParcel(Parcel source) {
            return new Recurring(source);
        }
        public Recurring[] newArray(int size) {
            return new Recurring[size];
        }
    };

    public static Recurring getSafeFirst() {
        List<Recurring> all = all();
        if (all.isEmpty()) {
            return Recurring.newRecurring(context.getString(R.string.new_recurring), 0, 0,
                                          0, 0, 1, true, Optional.<Calendar>absent(), Optional.<Calendar>absent(), false, false,
                                          new SparseBooleanArray());
        } else {
            return all.get(0);
        }
    }
}
