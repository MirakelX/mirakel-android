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
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.task.Task;

public class Recurring extends RecurringBase {
	public final static String TABLE = "recurring";
	private final static String TAG = "Recurring";
	private final static String[] allColumns = { "_id", "label", "minutes",
			"hours", "days", "months", "years", "for_due", "start_date",
			"end_date", "temporary" };
	private static SQLiteDatabase database;
	private static DatabaseHelper dbHelper;

	public Recurring(int _id, String label, int minutes, int hours, int days,
			int months, int years, boolean forDue, Calendar startDate,
			Calendar endDate, boolean temporary) {
		super(_id, label, minutes, hours, days, months, years, forDue,
				startDate, endDate, temporary);
	}

	public void save() {
		database.beginTransaction();
		ContentValues values = getContentValues();
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
	public static void init(Context context) {
		dbHelper = new DatabaseHelper(context);
		database = dbHelper.getWritableDatabase();
	}

	/**
	 * Close the Database-Connection
	 */
	public static void close() {
		dbHelper.close();
	}

	public static Recurring newRecurring(String label, int minutes, int hours,
			int days, int months, int years, boolean forDue,
			Calendar startDate, Calendar endDate, boolean temporary) {
		Recurring r = new Recurring(0, label, minutes, hours, days, months,
				years, forDue, startDate, endDate, temporary);
		return r.create();
	}

	public Recurring create() {
		database.beginTransaction();
		ContentValues values = getContentValues();
		values.remove("_id");
		int insertId = (int) database.insertOrThrow(TABLE, null, values);
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
	public static Recurring get(int id) {
		Cursor cursor = database.query(TABLE, allColumns, "_id=" + id, null,
				null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			Recurring r = cursorToRecurring(cursor);
			cursor.close();
			return r;
		}
		cursor.close();
		return null;
	}

	public static Recurring get(int minutes, int hours, int days, int month,
			int years, Calendar start, Calendar end) {
		String cal = "";
		if (start != null) {
			cal += " start_date=" + DateTimeHelper.formatDateTime(start);
		}
		if (end != null) {
			cal = cal + (cal.equals("") ? "" : " and") + " end_date="
					+ DateTimeHelper.formatDateTime(end);
		}
		Cursor cursor = database.query(TABLE, allColumns, "minutes=" + minutes
				+ " and hours=" + hours + " and days=" + days + " and months="
				+ month + " and years=" + years + cal, null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			Recurring r = cursorToRecurring(cursor);
			cursor.close();
			return r;
		}
		cursor.close();
		return null;
	}

	public static Recurring get(int days, int month, int years) {
		return get(0, 0, days, month, years, null, null);
	}

	public static Recurring first() {
		Cursor cursor = database.query(TABLE, allColumns, null, null, null,
				null, null, "1");
		cursor.moveToFirst();
		if (cursor.getCount() != 0) {
			Recurring r = cursorToRecurring(cursor);
			cursor.close();
			return r;
		}
		cursor.close();
		return null;
	}

	public static void destroyTemporary(int recurrenceId) {
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
		Cursor c = database.query(TABLE, allColumns, null, null, null, null,
				null);
		c.moveToFirst();
		List<Recurring> all = new ArrayList<Recurring>();
		while (!c.isAfterLast()) {
			all.add(cursorToRecurring(c));
			c.moveToNext();
		}
		c.close();
		return all;
	}

	private static Recurring cursorToRecurring(Cursor c) {
		int i = 0;
		Calendar start;
		try {
			start = DateTimeHelper.parseDateTime(c.getString(8));
		} catch (ParseException e) {
			start = null;
			Log.d(TAG, "cannot parse Date");
		}
		Calendar end;
		try {
			end = DateTimeHelper.parseDateTime(c.getString(9));
		} catch (ParseException e) {
			Log.d(TAG, "cannot parse Date");
			end = null;
		}
		return new Recurring(c.getInt(i++), c.getString(i++), c.getInt(i++),
				c.getInt(i++), c.getInt(i++), c.getInt(i++), c.getInt(i++),
				c.getInt(i++) == 1, start, end, c.getInt(++i) == 1);
	}

	public Calendar addRecurring(Calendar c) {
		Calendar now = new GregorianCalendar();
		now.set(Calendar.SECOND, 0);
		now.add(Calendar.MINUTE, -1);
		if ((getStartDate() == null || (getStartDate() != null && now
				.after(getStartDate())))
				&& (getEndDate() == null || (getEndDate() != null && now
						.before((getEndDate()))))) {
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
		return c;
	}

	public static List<Pair<Integer, String>> getForDialog(boolean isDue,
			Task task) {
		String where = "temporary=0";
		if (isDue) {
			where += " AND for_due=1";
		}
		if (task != null) {
			int id = -1;
			if (isDue)
				id = task.getRecurrenceId();
			else
				id = task.getRecurrenceReminderId();
			if (id > -1)
				where += " OR _id=" + id;
		}
		Cursor c = database.query(TABLE, new String[] { "_id", "label" },
				where, null, null, null, null);
		List<Pair<Integer, String>> ret = new ArrayList<Pair<Integer, String>>();
		c.moveToFirst();
		while (!c.isAfterLast()) {
			ret.add(new Pair<Integer, String>(c.getInt(0), c.getString(1)));
			c.moveToNext();
		}
		c.close();
		return ret;
	}

}
