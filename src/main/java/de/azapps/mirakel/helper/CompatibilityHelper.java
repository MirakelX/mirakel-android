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

import android.util.SparseBooleanArray;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.transform.TransformerException;

import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDoneProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDueProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDueProperty.Unit;
import de.azapps.mirakel.model.list.meta.SpecialListsListProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsPriorityProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsReminderProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsSetProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsStringProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsStringProperty.Type;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.tools.Log;

public class CompatibilityHelper {

    private static final String TAG = "CompatibilityHelper";

    public static String serializeWhereSpecialLists(
        final Map<String, SpecialListsBaseProperty> whereQuery) {
        String ret = "{";
        boolean first = true;
        for (final Map.Entry<String, SpecialListsBaseProperty> w : whereQuery
             .entrySet()) {
            ret += (first ? "" : " , ") + w.getValue().serialize();
            if (first) {
                first = false;
            }
        }
        Log.i(TAG, ret);
        return ret + "}";
    }

    private static String cleanUp(String p) {
        int oldLenght = p.length();
        do {
            p = p.trim();
            oldLenght = p.length();
            if ((p.charAt(0) == '(') && (p.charAt(p.length() - 1) == ')')) {
                p = p.substring(1, p.length() - 1);
            }
        } while (oldLenght != p.length());
        return p.toLowerCase();
    }

    @SuppressWarnings("unchecked")
    public static <T extends SpecialListsSetProperty> T getSetProperty(
        String wherePart, final Class<T> clazz, final String propName)
    throws TransformerException {
        wherePart = cleanUp(wherePart);
        final boolean isNegated = wherePart.contains("not");
        if (isNegated) {
            wherePart = wherePart.replace("not", "").trim();
        }
        wherePart = wherePart.replace(propName, "").trim();
        wherePart = wherePart.replace("in", "").trim();
        wherePart = wherePart.replaceAll("[()]", "").trim();
        final String[] parts = wherePart.split(",");
        final List<Integer> content = new ArrayList<Integer>();
        for (final String part : parts) {
            content.add(Integer.parseInt(part.trim()));
        }
        if (clazz.getCanonicalName().equals(
                SpecialListsListProperty.class.getCanonicalName())) {
            return (T) new SpecialListsListProperty(isNegated, content);
        } else if (clazz.getCanonicalName().equals(
                       SpecialListsPriorityProperty.class.getCanonicalName())) {
            return (T) new SpecialListsPriorityProperty(isNegated, content);
        } else {
            Log.wtf(TAG, "unknown filtertype");
            throw new TransformerException("failed to instance setClass");
        }
    }

    public static <T extends SpecialListsStringProperty> T getStringProperty(
        String wherePart, final Class<T> clazz, final String propName)
    throws TransformerException {
        wherePart = cleanUp(wherePart);
        final boolean isNegated = wherePart.contains("not");
        if (isNegated) {
            wherePart = wherePart.replace("not", "").trim();
        }
        wherePart = wherePart.replace(propName + " like", "").trim();
        String searchString;
        Type type;
        if (wherePart.matches("[\"'].%['\"]")) {
            type = Type.BEGIN;
            searchString = wherePart.replaceAll("[\"'%]", "");
        } else if (wherePart.matches("[\"']%.['\"]")) {
            type = Type.END;
            searchString = wherePart.replaceAll("[\"'%]", "");
        } else {
            type = Type.CONTAINS;
            searchString = wherePart.replaceAll("[\"'%]", "");
        }
        T obj;
        try {
            obj = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new TransformerException("failed to instance setClass", e);
        }
        obj.setIsNegated(isNegated);
        obj.setSearchString(searchString);
        obj.setType(type);
        return obj;
    }

    public static SpecialListsDueProperty getDueProperty(String wherePart)
    throws TransformerException {
        wherePart = cleanUp(wherePart);
        final String[] dueParts = wherePart.split("and");
        for (String p : dueParts) {
            if (!p.contains("not null")) {
                p = p.split("<=date")[1];
                p = p.replace("(", "").replace(")", "").trim();
                final String[] parts = p.split(",");
                if ((parts.length == 2) && parts[1].contains("localtime")) {
                    return new SpecialListsDueProperty(Unit.DAY, 0, false);
                }
                p = parts[1];
                Unit unit;
                if (p.contains("year")) {
                    unit = Unit.YEAR;
                    p = p.replace(p.contains("years") ? "years" : "year", "")
                        .trim();
                } else if (p.contains("month")) {
                    unit = Unit.MONTH;
                    p = p.replace(p.contains("months") ? "months" : "month", "")
                        .trim();
                } else {
                    unit = Unit.DAY;
                    p = p.replace(p.contains("days") ? "days" : "day", "")
                        .trim();
                }
                return new SpecialListsDueProperty(unit, Integer.parseInt(p
                                                   .replace("\"", "").replace("'", "").replace("+", "")
                                                   .trim()), false);
            }
            throw new TransformerException("cannot parse due");
        }
        return null;// can not be reached stupid eclipse
    }

    public static SpecialListsDoneProperty getDoneProperty(
        final String wherePart) {
        return new SpecialListsDoneProperty(wherePart.contains("=1"));
    }

    public static SpecialListsReminderProperty getReminderProperty(
        final String wherePart) {
        return new SpecialListsReminderProperty(wherePart.toLowerCase()
                                                .contains("not"));
    }

    private static Period getInterval(final int years, final int months, final int days,
                                      final int hours, final int minutes) {
        return new Period(years, months, 0, days, hours, minutes, 0, 0);
    }

    public static Recurring parseTaskWarriorRecurrence(final String recur) {
        final Scanner in = new Scanner(recur);
        in.useDelimiter("[^0-9]+");
        int number = 1;
        if (in.hasNextInt()) {
            number = in.nextInt();
        }
        in.close();
        // remove number and possible sign(recurrence should be positive but who
        // knows)
        final Recurring r;
        switch (recur.replace(String.valueOf(number), "").replace("-", "")) {
        case "yearly":
        case "annual":
            number = 1;
        //$FALL-THROUGH$
        case "years":
        case "year":
        case "yrs":
        case "yr":
        case "y":
            r = new Recurring(0, recur, getInterval(number, 0, 0, 0, 0), true, Optional.<DateTime>absent(),
                              Optional.<DateTime>absent(),
                              true, false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "semiannual":
            r = new Recurring(0, recur, getInterval(0, 6, 0, 0, 0), true, Optional.<DateTime>absent(),
                              Optional.<DateTime>absent(), true,
                              false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "biannual":
        case "biyearly":
            r = new Recurring(0, recur, getInterval(2, 0, 0, 0, 0), true, Optional.<DateTime>absent(),
                              Optional.<DateTime>absent(), true,
                              false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "bimonthly":
            r = new Recurring(0, recur, getInterval(0, 2, 0, 0, 0), true, Optional.<DateTime>absent(),
                              Optional.<DateTime>absent(), true,
                              true, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "biweekly":
        case "fortnight":
            r = new Recurring(0, recur, getInterval(0, 0, 14, 0, 0), true, Optional.<DateTime>absent(),
                              Optional.<DateTime>absent(), true,
                              false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "daily":
            number = 1;
        //$FALL-THROUGH$
        case "days":
        case "day":
        case "d":
            r = new Recurring(0, recur, getInterval(0, 0, number, 0, 0), true, Optional.<DateTime>absent(),
                              Optional.<DateTime>absent(),
                              true, false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "hours":
        case "hour":
        case "hrs":
        case "hr":
        case "h":
            r = new Recurring(0, recur, getInterval(0, 0, 0, number, 0), true, Optional.<DateTime>absent(),
                              Optional.<DateTime>absent(),
                              true, false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "minutes":
        case "mins":
        case "min":
            r = new Recurring(0, recur, getInterval(0, 0, 0, 0, number), true, Optional.<DateTime>absent(),
                              Optional.<DateTime>absent(),
                              true, false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "monthly":
            number = 1;
        //$FALL-THROUGH$
        case "months":
        case "month":
        case "mnths":
        case "mths":
        case "mth":
        case "mos":
        case "mo":
            r = new Recurring(0, recur, getInterval(0, number, 0, 0, 0), true, Optional.<DateTime>absent(),
                              Optional.<DateTime>absent(),
                              true, false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        case "quarterly":
            number = 1;
        //$FALL-THROUGH$
        case "quarters":
        case "qrtrs":
        case "qtrs":
        case "qtr":
        case "q":
            r = new Recurring(0, recur, getInterval(0, 3 * number, 0, 0, 0), true, Optional.<DateTime>absent(),
                              Optional.<DateTime>absent(),
                              true, false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        default:
        case "seconds":
        case "secs":
        case "sec":
        case "s":
            Log.w(TAG, "mirakel des not support " + recur);
            r = null;
            break;
        case "weekdays":
            final SparseBooleanArray weekdays = new SparseBooleanArray(7);
            for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
                weekdays.put(i, i != Calendar.SATURDAY && i != Calendar.SUNDAY);
            }
            r = new Recurring(0, recur, new Period(), true, Optional.<DateTime>absent(),
                              Optional.<DateTime>absent(), true,
                              false, weekdays, Optional.<Long>absent());
            break;
        case "sennight":
        case "weekly":
            number = 1;
        //$FALL-THROUGH$
        case "weeks":
        case "week":
        case "wks":
        case "wk":
        case "w":
            r = new Recurring(0, recur, getInterval(0, 0, 7 * number, 0, 0), true, Optional.<DateTime>absent(),
                              Optional.<DateTime>absent(), true, false, new SparseBooleanArray(), Optional.<Long>absent());
            break;
        }
        return r.create();
    }
}
