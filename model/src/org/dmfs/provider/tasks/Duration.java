/* //device/content/providers/pim/Duration.java
 **
 ** Copyright 2006, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package org.dmfs.provider.tasks;

import java.text.ParseException;
import java.util.Calendar;
import java.util.TimeZone;

import android.text.format.Time;


/**
 * According to RFC2445, durations are like this: WEEKS | DAYS [ HOURS [ MINUTES [ SECONDS ] ] ] | HOURS [ MINUTES [ SECONDS ] ] it doesn't specifically, say,
 * but this sort of implies that you can't have 70 seconds.
 */
public class Duration {

    public int sign; // 1 or -1
    public int weeks;
    public int days;
    public int hours;
    public int minutes;
    public int seconds;


    public Duration() {
        sign = 1;
    }


    /**
     * Returns a Duration object with an already specified time string.
     *
     * @param str
     *            The duration time string
     */
    public Duration(String str) {
        try {
            parse(str);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid duration string.", e);
        }
    }


    /**
     * Parse according to RFC2445 ss4.3.6. (It's actually a little loose with its parsing, for better or for worse)
     */
    public void parse(String str) throws ParseException {
        sign = 1;
        weeks = 0;
        days = 0;
        hours = 0;
        minutes = 0;
        seconds = 0;
        int len = str.length();
        int index = 0;
        char c;
        if (len < 1) {
            return;
        }
        c = str.charAt(0);
        if (c == '-') {
            sign = -1;
            index++;
        } else if (c == '+') {
            index++;
        }
        if (len < index) {
            return;
        }
        c = str.charAt(index);
        if (c != 'P') {
            throw new ParseException("Duration.parse(str='" + str + "') expected 'P' at index=" + index, index);
        }
        index++;
        int n = 0;
        for (; index < len; index++) {
            c = str.charAt(index);
            if (c >= '0' && c <= '9') {
                n *= 10;
                n += ((int) (c - '0'));
            } else if (c == 'W') {
                weeks = n;
                n = 0;
            } else if (c == 'H') {
                hours = n;
                n = 0;
            } else if (c == 'M') {
                minutes = n;
                n = 0;
            } else if (c == 'S') {
                seconds = n;
                n = 0;
            } else if (c == 'D') {
                days = n;
                n = 0;
            } else if (c == 'T') {
            } else {
                throw new ParseException("Duration.parse(str='" + str + "') unexpected char '" + c + "' at index=" +
                                         index, index);
            }
        }
    }


    /**
     * Add this to the calendar provided, in place, in the calendar.
     */
    public void addTo(Calendar cal) {
        cal.add(Calendar.DAY_OF_MONTH, sign * weeks * 7);
        cal.add(Calendar.DAY_OF_MONTH, sign * days);
        cal.add(Calendar.HOUR, sign * hours);
        cal.add(Calendar.MINUTE, sign * minutes);
        cal.add(Calendar.SECOND, sign * seconds);
    }


    /**
     * Add the duration to a time stamp
     *
     * @param dt
     *            Reference time in UTC we're adding the duration to
     * @return New time in milliseconds
     */
    public long addTo(long dt) {
        return dt + getMillis();
    }


    /**
     * Add the specified (negative) duration to a given {@link Time} and return a new instance with the new time.
     *
     * @param dt
     *            Reference time we're adding the duration to.
     * @return {@link Time} object with the new time.
     */
    public Time addTo(Time dt) {
        if (dt.allDay && (hours != 0 || minutes != 0 || seconds != 0)) {
            throw new IllegalArgumentException("Hours/minutes/seconds must be 0 if allDay is set");
        }
        Time utcTime = new Time(Time.TIMEZONE_UTC);
        utcTime.set(dt.toMillis(false));
        TimeZone originalTZ = TimeZone.getTimeZone(dt.timezone);
        long utcOffsetBefore = originalTZ.getOffset(utcTime.toMillis(false));
        utcTime.monthDay += sign * (weeks * 7 + days);
        utcTime.hour += sign * hours;
        utcTime.minute += sign * minutes;
        utcTime.second += sign * seconds;
        utcTime.normalize(false);
        long utcOffsetAfter = originalTZ.getOffset(utcTime.toMillis(true));
        utcTime.switchTimezone(dt.timezone);
        if (days != 0) {
            // Adding whole days is a special case
            utcTime.set(utcTime.toMillis(false) - (utcOffsetAfter - utcOffsetBefore));
        }
        return utcTime;
    }


    /**
     * Return the duration in milliseconds.
     *
     * This is somewhat incorrect when you intend to add the duration to a time stamp, because it doesn't take summer/winter time switches into account.
     *
     * @return the duration in milliseconds.
     */
    public long getMillis() {
        long factor = 1000 * sign;
        return factor * ((7 * 24 * 60 * 60 * weeks) + (24 * 60 * 60 * days) + (60 * 60 * hours) +
                         (60 * minutes) + seconds);
    }


    /**
     * Returns the duration in milliseconds with respect to a specific reference time.
     * <p>
     * This method returns the correct absolute duration in cases when a day is not exactly 24 hours (like when switching to summer time).
     * </p>
     *
     * @param ref
     *            The reference time.
     * @return The duration in milliseconds.
     *
     * @author Marten Gajda <marten@dmfs.org>
     */
    public long getMillis(Time ref) {
        Time temp = addTo(ref);
        return temp.toMillis(false) - ref.toMillis(false);
    }


    /**
     * Set duration from milliseconds.
     *
     * @param millis
     *            duration in milliseconds
     *
     * @author Marten Gajda <marten@dmfs.org>
     */
    public void fromMillis(long millis) {
        final long secs = Math.abs(millis) / 1000;
        final long mins = secs / 60;
        final long hours = mins / 60;
        final long days = hours / 24;
        final long weeks = days / 7;
        this.sign = millis < 0 ? -1 : 1;
        this.seconds = (int) (secs % 60);
        this.minutes = (int) (mins % 60);
        this.hours = (int) (hours % 24);
        if (this.seconds == 0 && this.minutes == 0 && this.hours == 0 && days % 7 == 0) {
            this.weeks = (int) weeks;
            this.days = 0;
        } else {
            this.days = (int) days;
            this.weeks = 0;
        }
    }


    /**
     * Return formated duration string.
     *
     * @return the properly formated duration string
     *
     * @author Marten Gajda <marten@dmfs.org>
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(32);
        if (sign < 0) {
            result.append("-P");
        } else {
            result.append("P");
        }
        if (days == 0 && hours == 0 && minutes == 0 && seconds == 0 && weeks != 0) {
            result.append(weeks);
            result.append("W");
        } else {
            if (days > 0) {
                result.append(days);
                result.append("D");
            }
            if (hours != 0 || minutes != 0 || seconds != 0 || days == 0) {
                result.append("T");
                if (hours > 0) {
                    result.append(hours);
                    result.append("H");
                }
                if (minutes > 0) {
                    result.append(minutes);
                    result.append("M");
                }
                if (seconds > 0 || (days == 0 && hours == 0 && minutes == 0)) {
                    result.append(seconds);
                    result.append("S");
                }
            }
        }
        return result.toString();
    }
}
