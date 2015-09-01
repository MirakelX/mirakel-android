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
import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.generic.ModelBase;

import static com.google.common.base.Optional.absent;

abstract class RecurringBase extends ModelBase {
    public final static String LABEL = "label";
    public final static String MINUTES = "minutes";
    public final static String HOURS = "hours";
    public final static String DAYS = "days";
    public final static String MONTHS = "months";
    public final static String YEARS = "years";
    public final static String FOR_DUE = "for_due";
    public final static String START_DATE = "start_date";
    public final static String END_DATE = "end_date";
    public final static String TEMPORARY = "temporary";
    public final static String EXACT = "isExact";
    public final static String MONDAY = "monday";
    public final static String TUESDAY = "tuesday";
    public final static String WEDNESDAY = "wednesday";
    public final static String THURSDAY = "thursday";
    public final static String FRIDAY = "friday";
    public final static String SATURDAY = "saturday";
    public final static String SUNDAY = "sunnday";
    public final static String DERIVED = "derived_from";

    public final static String PARENT = "parent";
    public final static String CHILD = "child";
    public final static String OFFSET = "offset";
    public final static String OFFSET_COUNT = "offsetCount";

    protected Period recurringInterval;
    protected boolean forDue;
    @NonNull
    protected Optional<DateTime> startDate = absent();
    @NonNull
    protected Optional<DateTime> endDate = absent();
    protected boolean temporary;
    protected boolean isExact;
    @NonNull
    protected SparseBooleanArray weekdays = new SparseBooleanArray();
    @NonNull
    protected Optional<Long> derivedFrom = absent();

    public RecurringBase(final long id, @NonNull final String label, final @NonNull Period interval,
                         final boolean forDue, @NonNull final Optional<DateTime> startDate,
                         @NonNull final Optional<DateTime> endDate, final boolean temporary,
                         final boolean isExact, final SparseBooleanArray weekdays,
                         @NonNull final Optional<Long> derivedFrom) {
        super(id, label);
        this.forDue = forDue;
        this.recurringInterval = interval;
        this.setId(id);
        this.setStartDate(startDate);
        this.setEndDate(endDate);
        this.temporary = temporary;
        this.setExact(isExact);
        this.setWeekdays(weekdays);
        this.derivedFrom = derivedFrom;
    }
    protected RecurringBase(final long id, final String label) {
        super(id, label);
    }

    protected RecurringBase() {
        // Just for parcelable
    }

    public String getLabel() {
        return getName();
    }

    public void setLabel(@NonNull final String label) {
        setName(label);
    }

    public boolean isForDue() {
        return this.forDue;
    }

    public void setForDue(final boolean forDue) {
        this.forDue = forDue;
    }

    public void setInterval(final @NonNull Period interval) {
        recurringInterval = interval.normalizedStandard();
    }

    @NonNull
    public Period getInterval() {
        return recurringInterval;
    }

    public long getIntervalMs() {
        return getIntervalMs(new DateTime());
    }

    private long getIntervalMs(final @NonNull DateTime date) {
        return recurringInterval.toDurationFrom(date).getMillis();
    }

    @NonNull
    public Optional<DateTime> getStartDate() {
        return this.startDate;
    }

    public void setStartDate(@NonNull final Optional<DateTime> startDate) {
        this.startDate = startDate;
    }

    @NonNull
    public Optional<DateTime> getEndDate() {
        return this.endDate;
    }

    public void setEndDate(@NonNull final Optional<DateTime> endDate) {
        this.endDate = endDate;
    }

    public boolean isTemporary() {
        return this.temporary;
    }

    public void setTemporary(final boolean temporary) {
        this.temporary = temporary;
    }

    public boolean isExact() {
        return this.isExact;
    }

    public List<Integer> getWeekdays() {
        final List<Integer> ret = new ArrayList<>();
        if (this.weekdays.get(DateTimeConstants.SUNDAY, false)) {
            ret.add(DateTimeConstants.SUNDAY);
        }
        if (this.weekdays.get(DateTimeConstants.MONDAY, false)) {
            ret.add(DateTimeConstants.MONDAY);
        }
        if (this.weekdays.get(DateTimeConstants.TUESDAY, false)) {
            ret.add(DateTimeConstants.TUESDAY);
        }
        if (this.weekdays.get(DateTimeConstants.WEDNESDAY, false)) {
            ret.add(DateTimeConstants.WEDNESDAY);
        }
        if (this.weekdays.get(DateTimeConstants.THURSDAY, false)) {
            ret.add(DateTimeConstants.THURSDAY);
        }
        if (this.weekdays.get(DateTimeConstants.FRIDAY, false)) {
            ret.add(DateTimeConstants.FRIDAY);
        }
        if (this.weekdays.get(DateTimeConstants.SATURDAY, false)) {
            ret.add(DateTimeConstants.SATURDAY);
        }
        return ret;
    }

    public void setWeekdays(final SparseBooleanArray weekdays) {
        if (weekdays == null) {
            this.weekdays = new SparseBooleanArray();
        } else {
            this.weekdays = weekdays;
        }
    }

    @NonNull
    public Optional<Long> getDerivedFrom() {
        return this.derivedFrom;
    }

    public void setDerivedFrom(@NonNull final Optional<Long> derivedFrom) {
        this.derivedFrom = derivedFrom;
    }

    public void setExact(final boolean isExact) {
        this.isExact = isExact;
    }


    @NonNull
    @Override
    public ContentValues getContentValues() {
        final ContentValues cv = new ContentValues();
        cv.put(ModelBase.ID, getId());
        cv.put(MINUTES, this.recurringInterval.getMinutes());
        cv.put(HOURS, this.recurringInterval.getHours());
        cv.put(DAYS, this.recurringInterval.getDays() + (7 * this.recurringInterval.getWeeks()));
        cv.put(MONTHS, this.recurringInterval.getMonths());
        cv.put(YEARS, this.recurringInterval.getYears());
        cv.put(FOR_DUE, this.forDue);
        cv.put(LABEL, getName());
        if (startDate.isPresent()) {
            cv.put(START_DATE, startDate.get().getMillis());
        } else {
            cv.put(START_DATE, (Long)null);
        }
        if (endDate.isPresent()) {
            cv.put(END_DATE, endDate.get().getMillis());
        } else {
            cv.put(END_DATE, (Long)null);
        }
        cv.put(TEMPORARY, this.temporary);
        cv.put(EXACT, this.isExact);
        cv.put(MONDAY, this.weekdays.get(DateTimeConstants.MONDAY, false));
        cv.put(TUESDAY, this.weekdays.get(DateTimeConstants.TUESDAY, false));
        cv.put(WEDNESDAY, this.weekdays.get(DateTimeConstants.WEDNESDAY, false));
        cv.put(THURSDAY, this.weekdays.get(DateTimeConstants.THURSDAY, false));
        cv.put(FRIDAY, this.weekdays.get(DateTimeConstants.FRIDAY, false));
        cv.put(SATURDAY, this.weekdays.get(DateTimeConstants.SATURDAY, false));
        cv.put(SUNDAY, this.weekdays.get(DateTimeConstants.SUNDAY, false));
        cv.put(DERIVED, this.derivedFrom.orNull());
        return cv;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RecurringBase)) {
            return false;
        }
        final RecurringBase other = (RecurringBase) obj;
        if (this.getId() != other.getId()) {
            return false;
        }
        if (!Objects.equal(recurringInterval, other.recurringInterval)) {
            return false;
        }
        if ((!this.derivedFrom.isPresent() && other.derivedFrom.isPresent()) ||
            (this.derivedFrom.isPresent() && !this.derivedFrom.get().equals(other.derivedFrom.orNull()))) {
            return false;
        }
        if (!DateTimeHelper.equalsCalendar(this.endDate, other.endDate)) {
            return false;
        }
        if (this.forDue != other.forDue) {
            return false;
        }
        if (this.isExact != other.isExact) {
            return false;
        }
        if (!this.getName().equals(other.getName())) {
            return false;
        }
        if (!DateTimeHelper.equalsCalendar(this.startDate, other.startDate)) {
            return false;
        }
        if (this.temporary != other.temporary) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int)this.getId();
        result = prime * result + getInterval().hashCode();
        result = prime * result
                 + (!this.derivedFrom.isPresent() ? 0 : this.derivedFrom.get().hashCode());
        result = prime * result
                 + (!this.endDate.isPresent() ? 0 : this.endDate.get().hashCode());
        result = prime * result + (this.forDue ? 1231 : 1237);
        result = prime * result + (this.isExact ? 1231 : 1237);
        result = prime * result
                 + (this.getName() == null ? 0 : this.getName().hashCode());
        result = prime * result
                 + (!this.startDate.isPresent() ? 0 : this.startDate.get().hashCode());
        result = prime * result + (this.temporary ? 1231 : 1237);
        return result;
    }

}
