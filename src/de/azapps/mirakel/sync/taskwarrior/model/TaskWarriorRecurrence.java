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

package de.azapps.mirakel.sync.taskwarrior.model;

import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Period;

import java.util.Scanner;

import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.sync.taskwarrior.utilities.TW_ERRORS;
import de.azapps.mirakel.sync.taskwarrior.utilities.TaskWarriorSyncFailedException;
import de.azapps.tools.Log;

public class TaskWarriorRecurrence extends Recurring {

    private static final String TAG = "TaskWarriorRecurrence";

    public static class NotSupportedRecurrenceException extends TaskWarriorSyncFailedException {

        public NotSupportedRecurrenceException(final String message) {
            super(TW_ERRORS.NOT_SUPPORTED_SECONED_RECURRING, message);
        }
    }

    public TaskWarriorRecurrence(final @NonNull String recur,
                                 final @NonNull Optional<DateTime> end) throws NotSupportedRecurrenceException {
        super();
        final Scanner in = new Scanner(recur);
        in.useDelimiter("[^0-9]+");
        int number = 1;
        if (in.hasNextInt()) {
            number = in.nextInt();
        }
        in.close();
        // remove number and possible sign(recurrence should be positive but who knows)
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
            recurringInterval = new Period().withYears(number);
            break;
        case "semiannual":
            recurringInterval = new Period().withMonths(6);
            break;
        case "biannual":
        case "biyearly":
            recurringInterval = new Period().withYears(2);
            break;
        case "bimonthly":
            recurringInterval = new Period().withMonths(2);
            break;
        case "biweekly":
        case "fortnight":
            recurringInterval = new Period().withWeeks(2);
            break;
        case "daily":
            number = 1;
        //$FALL-THROUGH$
        case "days":
        case "day":
        case "d":
            recurringInterval = new Period().withDays(number);
            break;
        case "hours":
        case "hour":
        case "hrs":
        case "hr":
        case "h":
            recurringInterval = new Period().withHours(number);
            break;
        case "minutes":
        case "mins":
        case "min":
            recurringInterval = new Period().withMinutes(number);
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
            recurringInterval = new Period().withMonths(number);
            break;
        case "quarterly":
            number = 1;
        //$FALL-THROUGH$
        case "quarters":
        case "qrtrs":
        case "qtrs":
        case "qtr":
        case "q":
            recurringInterval = new Period().withMonths(3 * number);
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
            recurringInterval = new Period().withWeeks(number);
            break;
        case "weekdays":
            final SparseBooleanArray weekdays = new SparseBooleanArray(7);
            for (int i = DateTimeConstants.MONDAY; i <= DateTimeConstants.SUNDAY; i++) {
                weekdays.put(i, (i != DateTimeConstants.SATURDAY) && (i != DateTimeConstants.SUNDAY));
            }
            setWeekdays(weekdays);
            break;
        case "seconds":
        case "secs":
        case "sec":
        case "s":
        default:
            Log.w(TAG, "mirakel des not support " + recur);
            throw new NotSupportedRecurrenceException("mirakel des not support " + recur);

        }
        setForDue(true);
        setEndDate(end);
        setExact(false);
        setName(recur);
        setTemporary(true);
        if (recurringInterval == null) {
            recurringInterval = new Period();
        }
        recurringInterval = recurringInterval.normalizedStandard();
    }
}
