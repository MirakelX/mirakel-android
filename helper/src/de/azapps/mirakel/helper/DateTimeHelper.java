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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
import android.os.Build;
import android.text.format.DateUtils;
import android.text.format.Time;

public class DateTimeHelper {

    public static final SimpleDateFormat caldavFormat = new SimpleDateFormat(
        "yyyyMMdd'T'kkmmss", Locale.getDefault());
    public static final SimpleDateFormat caldavDueFormat = new SimpleDateFormat(
        "yyyyMMdd", Locale.getDefault());
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat(
        "yyyy-MM-dd", Locale.getDefault());
    public static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat(
        "yyyy-MM-dd'T'kkmmss'Z'", Locale.getDefault());

    public static final SimpleDateFormat dbDateTimeFormat = new SimpleDateFormat(
        "yyyy-MM-dd kk:mm:ss", Locale.getDefault());

    public static final SimpleDateFormat taskwarriorFormat = new SimpleDateFormat(
        "yyyyMMdd'T'kkmmss'Z'", Locale.getDefault());

    public static String formatDate(final Calendar c) {
        return c == null ? null : dateFormat.format(c.getTime());
    }

    /**
     * Format a Date for showing it in the app
     *
     * @param date
     *            Date
     * @param format
     *            Format–String (like dd.MM.YY)
     * @return The formatted Date as String
     */
    public static CharSequence formatDate(final Calendar date,
                                          final String format) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat(format, Locale.getDefault()).format(date
                .getTime());
    }

    /**
     * Return the current offset to UTC
     *
     * @param inMS
     *            indicate if the offset is in milliseconds(true) or in
     *            seconds(false)
     * @param date
     *            the date for which the offset should be calculated
     *
     * @return The offset including dayligthsaving
     */
    public static int getTimeZoneOffset(final boolean inMS, final Calendar date) {
        return TimeZone.getDefault().getOffset(date.getTimeInMillis())
               / (inMS ? 1 : 1000);
    }

    /**
     *
     * @param time
     *            utc-time in s
     * @return local time as Calendar
     */
    public static Calendar createLocalCalendar(final long time) {
        return createLocalCalendar(time, false);
    }

    public static Calendar createLocalCalendar(final long time,
            final boolean isDue) {
        final Calendar c = new GregorianCalendar();
        c.setTimeInMillis(time * 1000);
        if (!isDue || c.get(Calendar.HOUR) != 0 && c.get(Calendar.HOUR) != 24
            || c.get(Calendar.MINUTE) != 0 || c.get(Calendar.SECOND) != 0) {
            c.setTimeInMillis(c.getTimeInMillis()
                              + DateTimeHelper.getTimeZoneOffset(true, c));
        }
        return c;
    }

    /**
     *
     * @param c
     *            the local Calendar
     * @return utc time in s, 0 if calendar is null
     */
    public static long getUTCTime(final Calendar c) {
        if (c == null) {
            return 0;
        }
        return c.getTimeInMillis() / 1000
               - DateTimeHelper.getTimeZoneOffset(false, c);
    }

    /**
     *
     * @param c
     *            the local Calendar
     * @return the calendar in UTC, null if c is null
     */
    public static Calendar getUTCCalendar(final Calendar c) {
        if (c == null) {
            return null;
        }
        final Calendar ret = (Calendar) c.clone();
        ret.setTimeInMillis(c.getTimeInMillis()
                            - DateTimeHelper.getTimeZoneOffset(true, c));
        return ret;
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
    public static CharSequence formatDate(final Context ctx, final Calendar date) {
        if (date == null) {
            return "";
        }
        if (MirakelCommonPreferences.isDateFormatRelative()) {
            return getRelativeDate(ctx, date, false);
        }
        return new SimpleDateFormat(ctx.getString(R.string.dateFormat),
                                    Locale.getDefault()).format(date.getTime());
    }

    private static CharSequence getRelativeDate(final Context ctx,
            final Calendar date, final boolean reminder) {
        final GregorianCalendar now = new GregorianCalendar();
        now.setTime(new Date());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1
            || !(now.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                 && now.get(Calendar.DAY_OF_MONTH) == date
                 .get(Calendar.DAY_OF_MONTH) && now
                 .get(Calendar.MONTH) == date.get(Calendar.MONTH))
            || reminder) {
            return DateUtils.getRelativeTimeSpanString(date.getTimeInMillis(),
                    new Date().getTime(), reminder ? DateUtils.MINUTE_IN_MILLIS
                    : DateUtils.DAY_IN_MILLIS);
        }
        return ctx.getString(R.string.today);
    }

    public static String formatDateTime(final Calendar c) {
        return c == null ? null : dateTimeFormat.format(c.getTime());
    }

    public static String formatDBDateTime(final Calendar c) {
        return c == null ? null : dbDateTimeFormat.format(c.getTime());
    }

    public static String formateCalDav(final Calendar c) {
        return c == null ? null : caldavFormat.format(c.getTime());
    }

    public static String formateCalDavDue(final Calendar c) {
        return c == null ? null : caldavDueFormat.format(c.getTime());
    }

    public static CharSequence formatReminder(final Context ctx,
            final Calendar date) {
        if (MirakelCommonPreferences.isDateFormatRelative()) {
            return getRelativeDate(ctx, date, true);
        }
        return DateTimeHelper.formatDate(date,
                                         ctx.getString(R.string.humanDateTimeFormat));
    }

    public static String formatTaskWarrior(final Calendar c) {
        return c == null ? null : taskwarriorFormat.format(c.getTime());
    }

    /**
     * Get first day of week as android.text.format.Time constant.
     *
     * @return the first day of week in android.text.format.Time
     */
    public static int getFirstDayOfWeek() {
        final int startDay = Calendar.getInstance().getFirstDayOfWeek();
        if (startDay == Calendar.SATURDAY) {
            return Time.SATURDAY;
        } else if (startDay == Calendar.MONDAY) {
            return Time.MONDAY;
        } else {
            return Time.SUNDAY;
        }
    }

    public static boolean is24HourLocale(final Locale l) {
        final String output = DateFormat.getTimeInstance(DateFormat.SHORT, l)
                              .format(new Date());
        if (output.contains(" AM") || output.contains(" PM")) {
            return false;
        }
        return true;
    }

    public static Calendar parseCalDav(final String date) throws ParseException {
        if (date == null || date.equals("")) {
            return null;
        }
        final GregorianCalendar temp = new GregorianCalendar();
        temp.setTime(caldavFormat.parse(date));
        return temp;
    }

    private static Calendar parseDate(final String date,
                                      final SimpleDateFormat format) throws ParseException {
        if (date == null || date.equals("")) {
            return null;
        }
        final GregorianCalendar temp = new GregorianCalendar();
        temp.setTime(format.parse(date));
        return temp;
    }

    public static Calendar parseCalDavDue(final String date)
    throws ParseException {
        return parseDate(date, caldavDueFormat);
    }

    public static Calendar parseDate(final String date) throws ParseException {
        return parseDate(date, dateFormat);
    }

    public static Calendar parseDateTime(final String date)
    throws ParseException {
        return parseDate(date, dateTimeFormat);
    }

    public static Calendar parseDBDateTime(final String date)
    throws ParseException {
        return parseDate(date, dbDateTimeFormat);
    }

    public static Calendar parseTaskWarrior(final String date)
    throws ParseException {
        return parseDate(date, taskwarriorFormat);
    }

    public static boolean equalsCalendar(Calendar a, Calendar b) {
        if (a == null || b == null) {
            if (a != b) {
                return false;
            }
        } else {
            long ta = a.getTimeInMillis() / 1000;
            long tb = b.getTimeInMillis() / 1000;
            boolean ret = Math.abs(ta - tb) < 1;
            return ret;
        }
        return true;
    }
}
