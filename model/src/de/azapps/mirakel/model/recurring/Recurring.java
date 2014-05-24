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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;
import android.util.SparseBooleanArray;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class Recurring extends RecurringBase {
	public static final String TABLE = "recurring";
	private final static String TAG = "Recurring";
	private final static String[] allColumns = { "_id", "label", "minutes",
			"hours", "days", "months", "years", "for_due", "start_date",
			"end_date", "temporary", "isExact", "monday", "tuesday",
			"wednesday", "thursday", "friday", "saturday", "sunnday",
			"derived_from" };
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;

	public Recurring(final int _id, final String label, final int minutes,
			final int hours, final int days, final int months, final int years,
			final boolean forDue, final Calendar startDate,
			final Calendar endDate, final boolean temporary,
			final boolean isExact, final SparseBooleanArray weekdays,
			final Integer derivedFrom) {
		super(_id, label, minutes, hours, days, months, years, forDue,
				startDate, endDate, temporary, isExact, weekdays, derivedFrom);
	}

	public void save() {
		database.beginTransaction();
		final ContentValues values = getContentValues();
		database.update(TABLE, values, "_id = " + getId(), null);
		database.setTransactionSuccessful();
		database.endTransaction();
	}

	// Static

	/**
	 * Initialize the Database and the preferences
	 * 
	 * @param context
	 *            The Application-Context
	 */
	public static void init(final Context context) {
		dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
	}

	/**
	 * Close the Database-Connection
	 */
	public static void close() {
		dbHelper.close();
	}

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
		database.beginTransaction();
		final ContentValues values = getContentValues();
		values.remove("_id");
		final int insertId = (int) database.insertOrThrow(TABLE, null, values);
		database.setTransactionSuccessful();
		database.endTransaction();
		return Recurring.get(insertId);
	}

	/**
	 * Get a Semantic by id
	 * 
	 * @param id
	 * @return
	 */
	public static Recurring get(final int id) {
		final Cursor cursor = database.query(TABLE, allColumns, "_id=" + id,
				null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
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
			cal += " start_date=" + DateTimeHelper.formatDateTime(start);
		}
		if (end != null) {
			cal = cal + (cal.equals("") ? "" : " and") + " end_date="
					+ DateTimeHelper.formatDateTime(end);
		}
		final Cursor cursor = database.query(TABLE, allColumns, "minutes="
				+ minutes + " and hours=" + hours + " and days=" + days
				+ " and months=" + month + " and years=" + years + cal, null,
				null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
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
		final Cursor cursor = database.query(TABLE, allColumns, null, null,
				null, null, null, "1");
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			final Recurring r = cursorToRecurring(cursor);
			cursor.close();
			return r;
		}
		cursor.close();
		return null;
	}

	public static void destroyTemporary(final int recurrenceId) {
		database.beginTransaction();
		database.delete(TABLE, "temporary=1 AND _id=" + recurrenceId, null);
		database.setTransactionSuccessful();
		database.endTransaction();
	}

	public void destroy() {
		database.beginTransaction();
		database.delete(TABLE, "_id=" + getId(), null);
		// Fix recurring onDelete in TaskTable
		ContentValues cv = new ContentValues();
		cv.put("recurring", -1);
		database.update(Task.TABLE, cv, "recurring=" + getId(), null);
		cv = new ContentValues();
		cv.put("recurring_reminder", -1);
		database.update(Task.TABLE, cv, "recurring_reminder=" + getId(), null);
		database.setTransactionSuccessful();
		database.endTransaction();
	}

	public static List<Recurring> all() {
		final Cursor c = database.query(TABLE, allColumns, null, null, null,
				null, null);
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
		int i = 0;
		Calendar start;
		try {
			start = DateTimeHelper.parseDateTime(c.getString(8));
		} catch (final ParseException e) {
			start = null;
			Log.d(TAG, "cannot parse Date");
		}
		Calendar end;
		try {
			end = DateTimeHelper.parseDateTime(c.getString(9));
		} catch (final ParseException e) {
			Log.d(TAG, "cannot parse Date");
			end = null;
		}
		final SparseBooleanArray weekdays = new SparseBooleanArray();
		weekdays.put(Calendar.MONDAY, c.getInt(12) == 1);
		weekdays.put(Calendar.TUESDAY, c.getInt(13) == 1);
		weekdays.put(Calendar.WEDNESDAY, c.getInt(14) == 1);
		weekdays.put(Calendar.THURSDAY, c.getInt(15) == 1);
		weekdays.put(Calendar.FRIDAY, c.getInt(16) == 1);
		weekdays.put(Calendar.SATURDAY, c.getInt(17) == 1);
		weekdays.put(Calendar.SUNDAY, c.getInt(18) == 1);
		final Integer derivedFrom = c.isNull(19) ? null : c.getInt(19);
		return new Recurring(c.getInt(i++), c.getString(i++), c.getInt(i++),
				c.getInt(i++), c.getInt(i++), c.getInt(i++), c.getInt(i++),
				c.getInt(i++) == 1, start, end, c.getInt(10) == 1,
				c.getInt(11) == 1, weekdays, derivedFrom);
	}

	public Calendar addRecurring(Calendar c) {
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
				} while (c.before(now));
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

	public static List<Pair<Integer, String>> getForDialog(final boolean isDue/*
																			 * ,
																			 * Task
																			 * task
																			 */) {
		String where = "temporary=0";
		if (isDue) {
			where += " AND for_due=1";
		}
		/*
		 * if (task != null) { int id = -1; if (isDue) id =
		 * task.getRecurrenceId(); else id = task.getRecurringReminderId(); if
		 * (id > -1) where += " OR _id=" + id; }
		 */
		final Cursor c = database.query(TABLE, new String[] { "_id", "label" },
				where, null, null, null, null);
		final List<Pair<Integer, String>> ret = new ArrayList<Pair<Integer, String>>();
		c.moveToFirst();
		while (!c.isAfterLast()) {
			ret.add(new Pair<Integer, String>(c.getInt(0), c.getString(1)));
			c.moveToNext();
		}
		c.close();
		return ret;
	}

}
