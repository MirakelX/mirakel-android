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

package de.azapps.mirakel.model.recurring;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.util.SparseBooleanArray;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Period;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import de.azapps.mirakel.DefinitionsHelper.NoSuchListException;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.R;
import de.azapps.mirakel.model.generic.ModelBase;
import de.azapps.mirakel.model.provider.MirakelInternalContentProvider;
import de.azapps.mirakel.model.query_builder.Cursor2List;
import de.azapps.mirakel.model.query_builder.CursorGetter;
import de.azapps.mirakel.model.query_builder.CursorWrapper;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class Recurring extends RecurringBase {
    private static final CursorWrapper.CursorConverter<List<Recurring>> LIST_FROM_CURSOR = new
    Cursor2List<>(Recurring.class);
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

    public Recurring(final long id, final String label, final @NonNull Period interval,
                     final boolean forDue, @NonNull final Optional<DateTime> startDate,
                     @NonNull final Optional<DateTime> endDate, final boolean temporary,
                     final boolean isExact, @NonNull final SparseBooleanArray weekdays,
                     @NonNull final Optional<Long> derivedFrom) {
        super(id, label, interval, forDue,
              startDate, endDate, temporary, isExact, weekdays, derivedFrom);
    }

    public Recurring(final @NonNull CursorGetter c) {
        super(c.getLong(ID), c.getString(LABEL));
        final SparseBooleanArray weekdays = new SparseBooleanArray();
        weekdays.put(DateTimeConstants.MONDAY, c.getBoolean(MONDAY));
        weekdays.put(DateTimeConstants.TUESDAY, c.getBoolean(TUESDAY));
        weekdays.put(DateTimeConstants.WEDNESDAY, c.getBoolean(WEDNESDAY));
        weekdays.put(DateTimeConstants.THURSDAY, c.getBoolean(THURSDAY));
        weekdays.put(DateTimeConstants.FRIDAY, c.getBoolean(FRIDAY));
        weekdays.put(DateTimeConstants.SATURDAY, c.getBoolean(SATURDAY));
        weekdays.put(DateTimeConstants.SUNDAY, c.getBoolean(SUNDAY));
        setInterval(new Period(c.getInt(YEARS), c.getInt(MONTHS), 0, c.getInt(DAYS), c.getInt(HOURS),
                               c.getInt(MINUTES), 0, 0));
        setForDue(c.getBoolean(FOR_DUE));
        setStartDate(c.getOptional(START_DATE, DateTime.class));
        setEndDate(c.getOptional(END_DATE, DateTime.class));
        setTemporary(c.getBoolean(TEMPORARY));
        setExact(c.getBoolean(EXACT));
        setWeekdays(weekdays);
        setDerivedFrom(c.getOptional(DERIVED, Long.class));
    }



    @NonNull
    public static Recurring newRecurring(final String label, final @NonNull Period interval,
                                         final boolean forDue, final Optional<DateTime> startDate,
                                         final Optional<DateTime> endDate, final boolean temporary,
                                         final boolean isExact, final SparseBooleanArray weekdays) {
        final Recurring r = new Recurring(INVALID_ID, label, interval, forDue, startDate, endDate,
                                          temporary, isExact,
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
        return new MirakelQueryBuilder(context).get(Recurring.class, id);
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
        return qb.get(Recurring.class);
    }

    @NonNull
    public static Optional<Recurring> get(final int days, final int month, final int years) {
        return get(0, 0, days, month, years, null, null);
    }


    public static void destroyTemporary(final long recurrenceId) {
        delete(URI, TEMPORARY + "=1 AND " + ID + '=' + recurrenceId, null);
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



    @NonNull
    public Task incrementRecurringDue(final Task t) {
        if (!t.getDue().isPresent()) {
            return t;
        }
        final DateTime newDue = addRecurring(of(new DateTime(t.getDue().get())), true).get();
        if (newDue.compareTo(t.getDue().get()) == 0) {
            return t;
        }
        return new MirakelQueryBuilder(context).select(allTWColumns).and(CHILD,
                Operation.EQ, t).query(MirakelInternalContentProvider.RECURRING_TW_URI)
        .doWithCursor(new CursorWrapper.CursorConverter<Task>() {
            @Override
            public Task convert(@NonNull CursorGetter getter) {
                long masterID = t.getId();
                long offset = 0L;
                long offsetCount = 0L;
                if (getter.moveToFirst()) {// this is already a child-task
                    masterID = getter.getLong(PARENT);
                    offset = getter.getLong(OFFSET);
                    offsetCount = getter.getLong(OFFSET_COUNT);
                }
                offset += newDue.getMillis() - t.getDue().get().getMillis();
                ++offsetCount;
                Optional<Task> child = new MirakelQueryBuilder(context).select(CHILD).and(PARENT, Operation.EQ,
                        masterID).and(OFFSET_COUNT, Operation.EQ,
                                      offsetCount).query(MirakelInternalContentProvider.RECURRING_TW_URI)
                .doWithCursor(new CursorWrapper.CursorConverter<Optional<Task>>() {
                    @Override
                    public Optional<Task> convert(@NonNull final CursorGetter getter) {
                        if (getter.moveToFirst()) {
                            return Task.get(getter.getLong(0));
                        }
                        return absent();
                    }
                });
                if (child.isPresent()) {
                    return child.get();
                }
                t.setDue(of(newDue));
                final Task newTask;
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
        });
    }

    @NonNull
    public Optional<DateTime> addRecurring(final @NonNull Optional<DateTime> c) {
        return addRecurring(c, false);
    }

    @NonNull
    private Optional<DateTime> addRecurring(@NonNull final Optional<DateTime> cal,
                                            final boolean onlyOnce) {
        if (!cal.isPresent()) {
            return absent();
        }
        DateTime c = cal.get();
        final DateTime now = new DateTime();
        if (isExact()) {
            c = now;
        }
        now.withSecondOfMinute(0);
        now.withMinuteOfHour(0);
        final List<Integer> weekdays = getWeekdays();
        if (weekdays.isEmpty()) {
            if ((!getStartDate().isPresent() || now.isAfter(getStartDate().get()))
                && (!getEndDate().isPresent() || now.isBefore(getEndDate().get()))) {
                do {
                    c = c.plus(recurringInterval);
                } while (c.isBefore(now) && !onlyOnce);
            }
        } else {
            int diff = 8;
            if (c.isBefore(now)) {
                c = now;
            }
            c = c.plusDays(1);
            for (final Integer day : weekdays) {
                int local_diff = day - c.getDayOfWeek();
                if (local_diff < 0) {
                    local_diff += 7;
                }
                if (diff > local_diff) {
                    diff = local_diff;
                }
            }
            c = c.plusDays(diff);
        }
        return of(c);
    }

    public static List<Pair<Integer, String>> getForDialog(final boolean isDue) {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(context).and(TEMPORARY,
                Operation.EQ, false);
        if (isDue) {
            qb.and(FOR_DUE, Operation.EQ, true);
        }
        return qb.select(ID, LABEL).query(URI).doWithCursor(new Cursor2List<>(new
        CursorWrapper.CursorConverter<Pair<Integer, String>>() {
            @Override
            public Pair<Integer, String> convert(@NonNull final CursorGetter getter) {
                return new Pair<>(getter.getInt(ID), getter.getString(LABEL));
            }
        }));
    }

    // Parcelable stuff

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeSerializable(recurringInterval);
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
    private Recurring(final Parcel in) {
        super();
        this.recurringInterval = (Period) in.readSerializable();
        this.forDue = in.readByte() != 0;
        this.startDate = (Optional<DateTime>) in.readSerializable();
        this.endDate = (Optional<DateTime>) in.readSerializable();
        this.temporary = in.readByte() != 0;
        this.isExact = in.readByte() != 0;
        this.weekdays = in.readSparseBooleanArray();
        this.derivedFrom = (Optional<Long>) in.readSerializable();
        setId(in.readLong());
        setName(in.readString());
    }

    public static final Creator<Recurring> CREATOR = new Creator<Recurring>() {
        @Override
        public Recurring createFromParcel(final Parcel source) {
            return new Recurring(source);
        }
        @Override
        public Recurring[] newArray(final int size) {
            return new Recurring[size];
        }
    };


    public String generateDescription() {
        final StringBuilder ret = new StringBuilder();
        if (startDate.isPresent()) {
            ret.append(context.getString(R.string.begin_repeat, DateTimeHelper.formatDate(context, startDate)));
            ret.append(' ');
        }
        final StringBuilder text = new StringBuilder();
        boolean first = true;
        for (final int day : getWeekdays()) {
            if (first) {
                first = false;
            } else {
                text.append(", ");
            }
            switch (day) {
            case DateTimeConstants.MONDAY:
                text.append(context.getString(R.string.monday));
                break;
            case DateTimeConstants.TUESDAY:
                text.append(context.getString(R.string.tuesday));
                break;
            case DateTimeConstants.WEDNESDAY:
                text.append(context.getString(R.string.wednesday));
                break;
            case DateTimeConstants.THURSDAY:
                text.append(context.getString(R.string.thursday));
                break;
            case DateTimeConstants.FRIDAY:
                text.append(context.getString(R.string.friday));
                break;
            case DateTimeConstants.SATURDAY:
                text.append(context.getString(R.string.saturday));
                break;
            case DateTimeConstants.SUNDAY:
                text.append(context.getString(R.string.sunday));
                break;
            }
        }
        if (text.length() == 0) {
            if (recurringInterval.getYears() > 0) {
                text.append(context.getResources().getQuantityString(R.plurals.repeat_year,
                            recurringInterval.getYears(), recurringInterval.getYears()));
            }
            if (recurringInterval.getMonths() > 0) {
                if (text.length() > 0) {
                    text.append(", ");
                }
                text.append(context.getResources().getQuantityString(R.plurals.repeat_month,
                            recurringInterval.getMonths(), recurringInterval.getMonths()));
            }

            if (recurringInterval.getWeeks() > 0) {
                if (text.length() > 0) {
                    text.append(", ");
                }
                text.append(context.getResources().getQuantityString(R.plurals.repeat_week,
                            recurringInterval.getWeeks(),
                            recurringInterval.getWeeks()));
            }
            if (recurringInterval.getHours() > 0) {
                if (text.length() > 0) {
                    text.append(", ");
                }
                text.append(context.getResources().getQuantityString(R.plurals.repeat_hour,
                            recurringInterval.getHours(), recurringInterval.getHours()));
            }
            if (recurringInterval.getMinutes() > 0) {
                if (text.length() > 0) {
                    text.append(", ");
                }
                text.append(context.getResources().getQuantityString(R.plurals.repeat_minute,
                            recurringInterval.getMinutes(), recurringInterval.getMinutes()));
            }

        }
        ret.append(context.getString(R.string.every_repeat, text.toString()));
        if (endDate.isPresent()) {
            ret.append(' ');
            ret.append(context.getString(R.string.end_repeat, DateTimeHelper.formatDate(context, endDate)));
        }
        return ret.toString();
    }
}
