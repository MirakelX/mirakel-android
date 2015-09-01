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
package de.azapps.mirakel.helper;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.text.format.Time;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimeHelper {

    public static final SimpleDateFormat dateFormat = new SimpleDateFormat(
        "yyyy-MM-dd", Locale.getDefault());
    public static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat(
        "yyyy-MM-dd'T'kkmmss'Z'", Locale.getDefault());


    /**
     * Format a Date for showing it in the app
     *
     * @param date
     *            Date
     * @param format
     *            Format–String (like dd.MM.YY)
     * @return The formatted Date as String
     */
    public static CharSequence formatDate(final @Nullable DateTime date,
                                          final String format) {
        if (date == null) {
            return "";
        }
        return DateTimeFormat.forPattern(format).print(date);
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
     * Formats the Date in the format, the user want to see. The default
     * configuration is the relative date format. So the due date is for example
     * „tomorrow“ instead of yyyy-mm-dd
     *
     * @param ctx
     * @param date
     * @return
     */
    public static CharSequence formatDate(@NonNull final Context ctx,
                                          @NonNull final Optional<DateTime> date) {
        if (!date.isPresent()) {
            return "";
        } else {
            return getRelativeDate(ctx, date.get(), false);
        }
    }

    private static CharSequence getRelativeDate(final Context ctx,
            final DateTime date, final boolean reminder) {
        final DateTime now = new LocalDate().toDateTimeAtStartOfDay();
        if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1)
            || !(now.isBefore(date) && now.plusDays(1).isAfter(date))
            || reminder) {
            return DateUtils.getRelativeTimeSpanString(date.getMillis(),
                    new Date().getTime(), reminder ? DateUtils.MINUTE_IN_MILLIS
                    : DateUtils.DAY_IN_MILLIS);
        }
        return ctx.getString(R.string.today);
    }

    public static String formatDateTime(final Calendar c) {
        return (c == null) ? null : dateTimeFormat.format(c.getTime());
    }


    public static CharSequence formatReminder(final Context ctx,
            final DateTime date) {
        return getRelativeDate(ctx, date, true);
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
        return !(output.contains(" AM") || output.contains(" PM"));
    }

    private static Calendar parseDate(final String date,
                                      final SimpleDateFormat format) throws ParseException {
        if ((date == null) || date.isEmpty()) {
            return null;
        }
        final GregorianCalendar temp = new GregorianCalendar();
        temp.setTime(format.parse(date));
        return temp;
    }

    public static Calendar parseDate(final String date) throws ParseException {
        return parseDate(date, dateFormat);
    }



    public static boolean equalsCalendar(@NonNull final Optional<DateTime> a,
                                         @NonNull final Optional<DateTime> b) {
        if (!a.isPresent() || !b.isPresent()) {
            if (a.isPresent() != b.isPresent()) {
                return false;
            }
        } else {
            final long ta = a.get().getMillis() / 1000L;
            final long tb = b.get().getMillis() / 1000L;
            return Math.abs(ta - tb) < 1L;
        }
        return true;
    }
}
