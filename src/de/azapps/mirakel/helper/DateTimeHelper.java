package de.azapps.mirakel.helper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class DateTimeHelper {

	private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'kkmmss'Z'", Locale.getDefault());
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd", Locale.getDefault());
	private static final SimpleDateFormat TWFormat = new SimpleDateFormat(
			"yyyyMMdd'T'kkmmss'Z'", Locale.getDefault());

	public static String formatDate(Calendar c) {
		return c == null ? null : dateFormat.format(c.getTime());
	}

	public static String formatDateTime(Calendar c) {
		return c == null ? null : dateTimeFormat.format(c.getTime());
	}

	public static String formatTaskWarrior(Calendar c) {
		return TWFormat.format(c.getTime());
	}

	public static Calendar parseDate(String date) throws ParseException {
		GregorianCalendar temp = new GregorianCalendar();
		temp.setTime(dateFormat.parse(date));
		return temp;
	}

	public static Calendar parseDateTime(String date) throws ParseException {
		GregorianCalendar temp = new GregorianCalendar();
		temp.setTime(dateTimeFormat.parse(date));
		return temp;
	}

	public static Calendar parseTaskWarrior(String date) throws ParseException {
		GregorianCalendar temp = new GregorianCalendar();
		temp.setTime(TWFormat.parse(date));
		return temp;
	}
}
