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
		this.derivedFrom=derivedFrom;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getYears() {
		return years;
	}

	public void setYears(int years) {
		this.years = years;
	}

	public boolean isForDue() {
		return forDue;
	}

	public void setForDue(boolean for_due) {
		this.forDue = for_due;
	}

	public int getMonths() {
		return months;
	}

	public void setMonths(int months) {
		this.months = months;
	}

	public int getDays() {
		return days;
	}

	public void setDays(int days) {
		this.days = days;
	}

	public int getHours() {
		return hours;
	}

	public void setHours(int hours) {
		this.hours = hours;
	}

	public int getMinutes() {
		return minutes;
	}

	public void setMinutes(int minutes) {
		this.minutes = minutes;
	}

	@Override
	public String toString() {
		return label;
	}

	public int getId() {
		return _id;
	}

	public void setId(int _id) {
		this._id = _id;
	}

	public Calendar getStartDate() {
		return startDate;
	}

	public void setStartDate(Calendar startDate) {
		this.startDate = startDate;
	}

	public Calendar getEndDate() {
		return endDate;
	}

	public void setEndDate(Calendar endDate) {
		this.endDate = endDate;
	}

	public boolean isTemporary() {
		return temporary;
	}

	public void setTemporary(boolean temporary) {
		this.temporary = temporary;
	}

	public boolean isExact() {
		return isExact;
	}

	public List<Integer> getWeekdays() {
		ArrayList<Integer> ret = new ArrayList<Integer>();
		if (weekdays.get(Calendar.SUNDAY, false)) {
			ret.add(Calendar.SUNDAY);
		}
		if (weekdays.get(Calendar.MONDAY, false)) {
			ret.add(Calendar.MONDAY);
		}
		if (weekdays.get(Calendar.TUESDAY, false)) {
			ret.add(Calendar.TUESDAY);
		}
		if (weekdays.get(Calendar.WEDNESDAY, false)) {
			ret.add(Calendar.WEDNESDAY);
		}
		if (weekdays.get(Calendar.THURSDAY, false)) {
			ret.add(Calendar.THURSDAY);
		}
		if (weekdays.get(Calendar.FRIDAY, false)) {
			ret.add(Calendar.FRIDAY);
		}
		if (weekdays.get(Calendar.SATURDAY, false)) {
			ret.add(Calendar.SATURDAY);
		}

		return ret;
	}
	
	protected SparseBooleanArray getWeekdaysRaw() {
		return weekdays;
	}

	public void setWeekdays(SparseBooleanArray weekdays) {
		this.weekdays = weekdays;
	}

	public Integer getDerivedFrom() {
		return derivedFrom;
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
		return (minutes * minute + hours * hour + days * day + months * month + years
				* year) * 1000;
	}

	public ContentValues getContentValues() {
		ContentValues cv = new ContentValues();
		cv.put("_id", _id);
		cv.put("minutes", minutes);
		cv.put("hours", hours);
		cv.put("days", days);
		cv.put("months", months);
		cv.put("years", years);
		cv.put("for_due", forDue);
		cv.put("label", label);
		cv.put("start_date", DateTimeHelper.formatDateTime(startDate));
		cv.put("end_date", DateTimeHelper.formatDateTime(endDate));
		cv.put("temporary", temporary);
		cv.put("isExact", isExact);
		cv.put("monday", weekdays.get(Calendar.MONDAY, false));
		cv.put("tuesday", weekdays.get(Calendar.TUESDAY, false));
		cv.put("wednesday", weekdays.get(Calendar.WEDNESDAY, false));
		cv.put("thursday", weekdays.get(Calendar.THURSDAY, false));
		cv.put("friday", weekdays.get(Calendar.FRIDAY, false));
		cv.put("saturday", weekdays.get(Calendar.SATURDAY, false));
		cv.put("sunnday", weekdays.get(Calendar.SUNDAY, false));
		cv.put("derived_from", derivedFrom);
		return cv;
	}

}
