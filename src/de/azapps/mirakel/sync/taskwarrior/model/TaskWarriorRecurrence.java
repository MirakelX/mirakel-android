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

package de.azapps.mirakel.sync.taskwarrior.model;

import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;

import com.google.common.base.Optional;

import java.util.Calendar;
import java.util.Scanner;

import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.sync.taskwarrior.utilities.TW_ERRORS;
import de.azapps.mirakel.sync.taskwarrior.utilities.TaskWarriorSyncFailedException;
import de.azapps.tools.Log;

public class TaskWarriorRecurrence extends Recurring {

    private static final String TAG = "TaskWarriorRecurrence";

    public class NotSupportedRecurrenceExeption extends TaskWarriorSyncFailedException {

        public NotSupportedRecurrenceExeption() {
            super(TW_ERRORS.NOT_SUPPORTED_SECONED_RECURRING);
        }
    }

    public TaskWarriorRecurrence(final @NonNull String recur,
                                 final @NonNull Optional<Calendar> end) throws NotSupportedRecurrenceExeption {
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
            setYears(number);
            break;
        case "semiannual":
            setMonths(6);
            break;
        case "biannual":
        case "biyearly":
            setYears(2);
            break;
        case "bimonthly":
            setMonths(2);
            break;
        case "biweekly":
        case "fortnight":
            setDays(14);
            break;
        case "daily":
            number = 1;
        //$FALL-THROUGH$
        case "days":
        case "day":
        case "d":
            setDays(number);
            break;
        case "hours":
        case "hour":
        case "hrs":
        case "hr":
        case "h":
            setHours(number);
            break;
        case "minutes":
        case "mins":
        case "min":
            setMinutes(number);
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
            setMonths(number);
            break;
        case "quarterly":
            number = 1;
        //$FALL-THROUGH$
        case "quarters":
        case "qrtrs":
        case "qtrs":
        case "qtr":
        case "q":
            setMonths(3 * number);
            break;
        default:
        case "seconds":
        case "secs":
        case "sec":
        case "s":
            Log.w(TAG, "mirakel des not support " + recur);
            throw new NotSupportedRecurrenceExeption();
        case "weekdays":
            final SparseBooleanArray weekdays = new SparseBooleanArray(7);
            for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
                weekdays.put(i, i != Calendar.SATURDAY && i != Calendar.SUNDAY);
            }
            setWeekdays(weekdays);
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
            setDays(7 * number);
            break;
        }
        setForDue(true);
        setEndDate(end);
        setExact(true);
        setName(recur);
    }
}
