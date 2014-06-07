package de.azapps.mirakel.model.recurring;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.ContentValues;
import android.util.SparseBooleanArray;
import de.azapps.mirakel.helper.DateTimeHelper;

public class RecurringBase {
	private String label;
	private int id;
	private int minutes;
	private int hours;
	private int days;
	private int months;
	private int years;
	private boolean forDue;
	private Calendar startDate;
	private Calendar endDate;
	private boolean temporary;
	private boolean isExact;
	private SparseBooleanArray weekdays;
	private Integer derivedFrom;

	public RecurringBase(final int id, final String label, final int minutes,
			final int hours, final int days, final int months, final int years,
			final boolean forDue, final Calendar startDate,
			final Calendar endDate, final boolean temporary,
			final boolean isExact, final SparseBooleanArray weekdays,
			final Integer derivedFrom) {
		super();
		this.days = days;
		this.forDue = forDue;
		this.hours = hours;
		this.label = label;
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

	public String getLabel() {
		return this.label;
	}

	public void setLabel(final String label) {
		this.label = label;
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

	@Override
	public String toString() {
		return this.label;
	}

	public int getId() {
		return this.id;
	}

	protected void setId(final int id) {
		this.id = id;
	}

	public Calendar getStartDate() {
		return this.startDate;
	}

	public void setStartDate(final Calendar startDate) {
		this.startDate = startDate;
	}

	public Calendar getEndDate() {
		return this.endDate;
	}

	public void setEndDate(final Calendar endDate) {
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
		this.weekdays = weekdays;
	}

	public Integer getDerivedFrom() {
		return this.derivedFrom;
	}

	public void setDerivedFrom(final Integer derivedFrom) {
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

	public ContentValues getContentValues() {
		final ContentValues cv = new ContentValues();
		cv.put("_id", this.id);
		cv.put("minutes", this.minutes);
		cv.put("hours", this.hours);
		cv.put("days", this.days);
		cv.put("months", this.months);
		cv.put("years", this.years);
		cv.put("for_due", this.forDue);
		cv.put("label", this.label);
		cv.put("start_date", DateTimeHelper.formatDateTime(this.startDate));
		cv.put("end_date", DateTimeHelper.formatDateTime(this.endDate));
		cv.put("temporary", this.temporary);
		cv.put("isExact", this.isExact);
		cv.put("monday", this.weekdays.get(Calendar.MONDAY, false));
		cv.put("tuesday", this.weekdays.get(Calendar.TUESDAY, false));
		cv.put("wednesday", this.weekdays.get(Calendar.WEDNESDAY, false));
		cv.put("thursday", this.weekdays.get(Calendar.THURSDAY, false));
		cv.put("friday", this.weekdays.get(Calendar.FRIDAY, false));
		cv.put("saturday", this.weekdays.get(Calendar.SATURDAY, false));
		cv.put("sunnday", this.weekdays.get(Calendar.SUNDAY, false));
		cv.put("derived_from", this.derivedFrom);
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
		if (this.id != other.id) {
			return false;
		}
		if (this.days != other.days) {
			return false;
		}
		if (this.derivedFrom == null) {
			if (other.derivedFrom != null) {
				return false;
			}
		} else if (!this.derivedFrom.equals(other.derivedFrom)) {
			return false;
		}
		if (this.endDate == null) {
			if (other.endDate != null) {
				return false;
			}
		} else if (!this.endDate.equals(other.endDate)) {
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
		if (this.label == null) {
			if (other.label != null) {
				return false;
			}
		} else if (!this.label.equals(other.label)) {
			return false;
		}
		if (this.minutes != other.minutes) {
			return false;
		}
		if (this.months != other.months) {
			return false;
		}
		if (this.startDate == null) {
			if (other.startDate != null) {
				return false;
			}
		} else if (!this.startDate.equals(other.startDate)) {
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
		result = prime * result + this.id;
		result = prime * result + this.days;
		result = prime * result
				+ (this.derivedFrom == null ? 0 : this.derivedFrom.hashCode());
		result = prime * result
				+ (this.endDate == null ? 0 : this.endDate.hashCode());
		result = prime * result + (this.forDue ? 1231 : 1237);
		result = prime * result + this.hours;
		result = prime * result + (this.isExact ? 1231 : 1237);
		result = prime * result
				+ (this.label == null ? 0 : this.label.hashCode());
		result = prime * result + this.minutes;
		result = prime * result + this.months;
		result = prime * result
				+ (this.startDate == null ? 0 : this.startDate.hashCode());
		result = prime * result + (this.temporary ? 1231 : 1237);
		result = prime * result + this.years;
		return result;
	}

}
