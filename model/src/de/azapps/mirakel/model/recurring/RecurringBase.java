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
import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.ModelBase;

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

    protected int minutes;
    protected int hours;
    protected int days;
    protected int months;
    protected int years;
    protected boolean forDue;
    @NonNull
    protected Optional<Calendar> startDate;
    @NonNull
    protected Optional<Calendar> endDate;
    protected boolean temporary;
    protected boolean isExact;
    @NonNull
    protected SparseBooleanArray weekdays = new SparseBooleanArray();
    @NonNull
    protected Optional<Long> derivedFrom;

    public RecurringBase(final long id, @NonNull final String label, final int minutes,
                         final int hours, final int days, final int months, final int years,
                         final boolean forDue, @NonNull final Optional<Calendar> startDate,
                         @NonNull final Optional<Calendar> endDate, final boolean temporary,
                         final boolean isExact, final SparseBooleanArray weekdays,
                         @NonNull final Optional<Long> derivedFrom) {
        super(id, label);
        this.days = days;
        this.forDue = forDue;
        this.hours = hours;
        this.minutes = minutes;
        this.months = months;
        this.years = years;
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

    public int getYears() {
        return this.years;
    }

    public void setYears(final int years) {
        this.years = years;
    }

    public boolean isForDue() {
        return this.forDue;
    }

    public void setForDue(final boolean forDue) {
        this.forDue = forDue;
    }

    public int getMonths() {
        return this.months;
    }

    public void setMonths(final int months) {
        this.months = months;
    }

    public int getDays() {
        return this.days;
    }

    public void setDays(final int days) {
        this.days = days;
    }

    public int getHours() {
        return this.hours;
    }

    public void setHours(final int hours) {
        this.hours = hours;
    }

    public int getMinutes() {
        return this.minutes;
    }

    public void setMinutes(final int minutes) {
        this.minutes = minutes;
    }

    @NonNull
    public Optional<Calendar> getStartDate() {
        return this.startDate;
    }

    public void setStartDate(@NonNull final Optional<Calendar> startDate) {
        this.startDate = startDate;
    }

    @NonNull
    public Optional<Calendar> getEndDate() {
        return this.endDate;
    }

    public void setEndDate(@NonNull final Optional<Calendar> endDate) {
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
        final List<Integer> ret = new ArrayList<Integer>();
        if (this.weekdays.get(Calendar.SUNDAY, false)) {
            ret.add(Calendar.SUNDAY);
        }
        if (this.weekdays.get(Calendar.MONDAY, false)) {
            ret.add(Calendar.MONDAY);
        }
        if (this.weekdays.get(Calendar.TUESDAY, false)) {
            ret.add(Calendar.TUESDAY);
        }
        if (this.weekdays.get(Calendar.WEDNESDAY, false)) {
            ret.add(Calendar.WEDNESDAY);
        }
        if (this.weekdays.get(Calendar.THURSDAY, false)) {
            ret.add(Calendar.THURSDAY);
        }
        if (this.weekdays.get(Calendar.FRIDAY, false)) {
            ret.add(Calendar.FRIDAY);
        }
        if (this.weekdays.get(Calendar.SATURDAY, false)) {
            ret.add(Calendar.SATURDAY);
        }
        return ret;
    }

    protected SparseBooleanArray getWeekdaysRaw() {
        return this.weekdays;
    }

    public void setWeekdays(final SparseBooleanArray weekdays) {
        if (weekdays == null) {
            this.weekdays = new SparseBooleanArray();
        } else {
            this.weekdays = weekdays;
        }
    }

    public Optional<Long> getDerivedFrom() {
        return this.derivedFrom;
    }

    public void setDerivedFrom(final Optional<Long> derivedFrom) {
        this.derivedFrom = derivedFrom;
    }

    public void setExact(final boolean isExact) {
        this.isExact = isExact;
    }

    /**
     * Returns the interval for a recurrence in ms
     *
     * @return
     */
    public long getInterval() {
        final long minute = 60;
        final long hour = 3600;
        final long day = 86400;
        final long month = 2592000; // That's not right, but who cares?
        final long year = 31536000; // nobody need this…
        return (this.minutes * minute + this.hours * hour + this.days * day
                + this.months * month + this.years * year) * 1000;
    }

    @Override
    public ContentValues getContentValues() {
        final ContentValues cv = new ContentValues();
        cv.put(ModelBase.ID, getId());
        cv.put(MINUTES, this.minutes);
        cv.put(HOURS, this.hours);
        cv.put(DAYS, this.days);
        cv.put(MONTHS, this.months);
        cv.put(YEARS, this.years);
        cv.put(FOR_DUE, this.forDue);
        cv.put(LABEL, getName());
        cv.put(START_DATE, DateTimeHelper.formatDateTime(this.startDate));
        cv.put(END_DATE, DateTimeHelper.formatDateTime(this.endDate));
        cv.put(TEMPORARY, this.temporary);
        cv.put(EXACT, this.isExact);
        cv.put(MONDAY, this.weekdays.get(Calendar.MONDAY, false));
        cv.put(TUESDAY, this.weekdays.get(Calendar.TUESDAY, false));
        cv.put(WEDNESDAY, this.weekdays.get(Calendar.WEDNESDAY, false));
        cv.put(THURSDAY, this.weekdays.get(Calendar.THURSDAY, false));
        cv.put(FRIDAY, this.weekdays.get(Calendar.FRIDAY, false));
        cv.put(SATURDAY, this.weekdays.get(Calendar.SATURDAY, false));
        cv.put(SUNDAY, this.weekdays.get(Calendar.SUNDAY, false));
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
        if (this.days != other.days) {
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
        if (this.hours != other.hours) {
            return false;
        }
        if (this.isExact != other.isExact) {
            return false;
        }
        if (!this.getName().equals(other.getName())) {
            return false;
        }
        if (this.minutes != other.minutes) {
            return false;
        }
        if (this.months != other.months) {
            return false;
        }
        if (!DateTimeHelper.equalsCalendar(this.startDate, other.startDate)) {
            return false;
        }
        if (this.temporary != other.temporary) {
            return false;
        }
        if (this.years != other.years) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int)this.getId();
        result = prime * result + this.days;
        result = prime * result
                 + (!this.derivedFrom.isPresent() ? 0 : this.derivedFrom.get().hashCode());
        result = prime * result
                 + (!this.endDate.isPresent() ? 0 : this.endDate.get().hashCode());
        result = prime * result + (this.forDue ? 1231 : 1237);
        result = prime * result + this.hours;
        result = prime * result + (this.isExact ? 1231 : 1237);
        result = prime * result
                 + (this.getName() == null ? 0 : this.getName().hashCode());
        result = prime * result + this.minutes;
        result = prime * result + this.months;
        result = prime * result
                 + (!this.startDate.isPresent() ? 0 : this.startDate.get().hashCode());
        result = prime * result + (this.temporary ? 1231 : 1237);
        result = prime * result + this.years;
        return result;
    }

}
