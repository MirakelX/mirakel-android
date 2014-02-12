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
package de.azapps.mirakel.helper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import android.content.Context;
import android.os.Build;
import android.text.format.DateUtils;
import android.text.format.Time;
import de.azapps.mirakelandroid.R;

public class DateTimeHelper {

	private static final SimpleDateFormat	CalDav			= new SimpleDateFormat(
			"yyyyMMdd'T'kkmmss",
			Locale.getDefault());
	private static final SimpleDateFormat	CalDavDue		= new SimpleDateFormat(
			"yyyyMMdd",
			Locale.getDefault());
	private static final SimpleDateFormat	dateFormat		= new SimpleDateFormat(
			"yyyy-MM-dd",
			Locale.getDefault());
	private static final SimpleDateFormat	dateTimeFormat	= new SimpleDateFormat(
			"yyyy-MM-dd'T'kkmmss'Z'",
			Locale.getDefault());

	private static final SimpleDateFormat	TWFormat		= new SimpleDateFormat(
			"yyyyMMdd'T'kkmmss'Z'",
			Locale.getDefault());

	public static String formatDate(Calendar c) {
		return c == null ? null : dateFormat.format(c.getTime());
	}

	/**
	 * Format a Date for showing it in the app
	 * 
	 * @param date
	 *        Date
	 * @param format
	 *        Format–String (like dd.MM.YY)
	 * @return The formatted Date as String
	 */
	public static CharSequence formatDate(Calendar date, String format) {
		if (date == null) return "";
		return new SimpleDateFormat(format, Locale.getDefault()).format(date
				.getTime());
	}

	/**
	 * Formats the Date in the format, the user want to see. The default
	 * configuration is the relative date format. So the due date is for example
	 * „tomorrow“ instead of yyyy-mm-dd
	 * 
	 * @param ctx
	 * @param date
	 * @return
	 */
	public static CharSequence formatDate(Context ctx, Calendar date) {
		if (date == null) return "";
		if (MirakelPreferences.isDateFormatRelative()) {
			GregorianCalendar now = new GregorianCalendar();
			now.setTime(new Date());
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1
					|| !(now.get(Calendar.YEAR) == date.get(Calendar.YEAR)
					&& now.get(Calendar.DAY_OF_MONTH) == date
					.get(Calendar.DAY_OF_MONTH) && now
					.get(Calendar.MONTH) == date.get(Calendar.MONTH)))
				return DateUtils.getRelativeTimeSpanString(
						date.getTimeInMillis(), new Date().getTime(),
						DateUtils.DAY_IN_MILLIS);
			return ctx.getString(R.string.today);
		}
		return new SimpleDateFormat(ctx.getString(R.string.dateFormat),
				Locale.getDefault()).format(date.getTime());
	}

	public static String formatDateTime(Calendar c) {
		return c == null ? null : dateTimeFormat.format(c.getTime());
	}

	public static String formateCalDav(Calendar c) {
		return c == null ? null : CalDav.format(c.getTime());
	}

	public static String formateCalDavDue(Calendar c) {
		return c == null ? null : CalDavDue.format(c.getTime());
	}

	public static CharSequence formatReminder(Context ctx, Calendar date) {
		return DateTimeHelper.formatDate(date,
				ctx.getString(R.string.humanDateTimeFormat));
	}

	public static String formatTaskWarrior(Calendar c) {
		return c == null ? null : TWFormat.format(c.getTime());
	}

	/**
	 * Get first day of week as android.text.format.Time constant.
	 * 
	 * @return the first day of week in android.text.format.Time
	 */
	public static int getFirstDayOfWeek() {
		int startDay = Calendar.getInstance().getFirstDayOfWeek();

		if (startDay == Calendar.SATURDAY) return Time.SATURDAY;
		else if (startDay == Calendar.MONDAY) return Time.MONDAY;
		else return Time.SUNDAY;
	}

	public static boolean is24HourLocale(Locale l) {
		String output = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT,l).format(new Date());
		if (output.contains(" AM") || output.contains(" PM")) return false;
		return true;
	}

	public static Calendar parseCalDav(String date) throws ParseException {
		if (date == null || date.equals("")) return null;
		GregorianCalendar temp = new GregorianCalendar();
		temp.setTime(CalDav.parse(date));
		return temp;
	}

	public static Calendar parseCalDavDue(String date) throws ParseException {
		if (date == null || date.equals("")) return null;
		GregorianCalendar temp = new GregorianCalendar();
		temp.setTime(CalDavDue.parse(date));
		return temp;
	}

	public static Calendar parseDate(String date) throws ParseException {
		if (date == null || date.equals("")) return null;
		GregorianCalendar temp = new GregorianCalendar();
		temp.setTime(dateFormat.parse(date));
		return temp;
	}

	public static Calendar parseDateTime(String date) throws ParseException {
		if (date == null || date.equals("")) return null;
		GregorianCalendar temp = new GregorianCalendar();
		temp.setTime(dateTimeFormat.parse(date));
		return temp;
	}

	public static Calendar parseTaskWarrior(String date) throws ParseException {
		if (date == null || date.equals("")) return null;
		GregorianCalendar temp = new GregorianCalendar();
		temp.setTime(TWFormat.parse(date));
		return temp;
	}
}
