package de.azapps.mirakel.model.recurring;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.ContentValues;
import android.util.SparseBooleanArray;
import de.azapps.mirakel.helper.DateTimeHelper;

public class RecurringBase {
	private String label;
	private int _id;
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

	public RecurringBase(int _id, String label, int minutes, int hours,
			int days, int months, int years, boolean forDue,
			Calendar startDate, Calendar endDate, boolean temporary,
			boolean isExact, SparseBooleanArray weekdays, Integer derivedFrom) {
		super();
		this.days = days;
		this.forDue = forDue;
		this.hours = hours;
		this.label = label;
		this.minutes = minutes;
		this.months = months;
		this.years = years;
		this.setId(_id);
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

	public void setLabel(String label) {
		this.label = label;
	}

	public int getYears() {
		return this.years;
	}

	public void setYears(int years) {
		this.years = years;
	}

	public boolean isForDue() {
		return this.forDue;
	}

	public void setForDue(boolean for_due) {
		this.forDue = for_due;
	}

	public int getMonths() {
		return this.months;
	}

	public void setMonths(int months) {
		this.months = months;
	}

	public int getDays() {
		return this.days;
	}

	public void setDays(int days) {
		this.days = days;
	}

	public int getHours() {
		return this.hours;
	}

	public void setHours(int hours) {
		this.hours = hours;
	}

	public int getMinutes() {
		return this.minutes;
	}

	public void setMinutes(int minutes) {
		this.minutes = minutes;
	}

	@Override
	public String toString() {
		return this.label;
	}

	public int getId() {
		return this._id;
	}

	public void setId(int _id) {
		this._id = _id;
	}

	public Calendar getStartDate() {
		return this.startDate;
	}

	public void setStartDate(Calendar startDate) {
		this.startDate = startDate;
	}

	public Calendar getEndDate() {
		return this.endDate;
	}

	public void setEndDate(Calendar endDate) {
		this.endDate = endDate;
	}

	public boolean isTemporary() {
		return this.temporary;
	}

	public void setTemporary(boolean temporary) {
		this.temporary = temporary;
	}

	public boolean isExact() {
		return this.isExact;
	}

	public List<Integer> getWeekdays() {
		ArrayList<Integer> ret = new ArrayList<Integer>();
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

	public void setWeekdays(SparseBooleanArray weekdays) {
		this.weekdays = weekdays;
	}

	public Integer getDerivedFrom() {
		return this.derivedFrom;
	}

	public void setDerivedFrom(Integer derivedFrom) {
		this.derivedFrom = derivedFrom;
	}

	public void setExact(boolean isExact) {
		this.isExact = isExact;
	}

	/**
	 * Returns the intervall for a recurrence in ms
	 * 
	 * @return
	 */
	public long getIntervall() {
		int minute = 60;
		int hour = 3600;
		int day = 86400;
		int month = 2592000; // That's not right, but who cares?
		int year = 31536000; // nobody need this…
		return (this.minutes * minute + this.hours * hour + this.days * day
				+ this.months * month + this.years * year) * 1000;
	}

	public ContentValues getContentValues() {
		ContentValues cv = new ContentValues();
		cv.put("_id", this._id);
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

}
