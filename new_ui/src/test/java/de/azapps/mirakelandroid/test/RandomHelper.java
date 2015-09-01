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
package de.azapps.mirakelandroid.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.SparseBooleanArray;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsDueProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsProgressProperty;
import de.azapps.mirakel.model.list.meta.SpecialListsStringProperty;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.FileUtils;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

public class RandomHelper {

    // its ok to use this here, it's only for testing
    @SuppressLint("TrulyRandom")
    private static SecureRandom random = new SecureRandom();
    private static Context ctx;
    private static Optional<Calendar> randomOptional_Calendar;
    private static Optional<String> randomOptional_String;

    public static void init(final Context ctx) {
        RandomHelper.ctx = ctx;
    }

    public static int getRandomint() {
        return random.nextInt();
    }
    public static short getRandomshort() {
        return (short) random.nextInt();
    }
    public static Long getRandomLong() {
        return random.nextLong();
    }

    public static boolean getRandomboolean() {
        return random.nextBoolean();
    }

    public static String getRandomString() {
        return new BigInteger(130, random).toString(32);
    }

    public static SYNC_STATE getRandomSYNC_STATE() {
        return SYNC_STATE.values()[random.nextInt(SYNC_STATE.values().length)];
    }

    public static AccountMirakel getRandomAccountMirakel() {
        List<AccountMirakel> all = AccountMirakel.all();
        if (all.isEmpty()) {
            return AccountMirakel.getLocal();
        } else {
            return all.get(random.nextInt(all.size()));
        }
    }

    public static Integer getRandomInteger() {
        return getRandomint();
    }

    public static ListMirakel getRandomListMirakel() {
        final List<ListMirakel> t = ListMirakel.all(false);
        return t.get(random.nextInt(t.size()));
    }

    public static Calendar getRandomCalendar() {
        return getRandomGregorianCalendar();
    }

    public static long getRandomlong() {
        return random.nextLong();
    }

    public static Uri getRandomUri() {
        // return a constant uri because uri must exist
        final String p = FileUtils.getMirakelDir() + "/databases/mirakel.db";
        final File f = new File(p);
        return Uri.fromFile(f);
    }

    public static ACCOUNT_TYPES getRandomACCOUNT_TYPES() {
        ACCOUNT_TYPES type;
        do {
            type = ACCOUNT_TYPES.values()[random
                                          .nextInt(ACCOUNT_TYPES.values().length)];
        } while (ACCOUNT_TYPES.LOCAL.equals(type));
        return type;
    }

    public static <T> T getRandomElem(final List<T> elems) {
        if (elems.isEmpty()) {
            return null;
        }
        return elems.get(random.nextInt(elems.size()));
    }

    public static Task getRandomTask() {
        Task.init(ctx);
        final List<Task> t = Task.all();
        return t.get(random.nextInt(t.size()));
    }

    public static SparseBooleanArray getRandomSparseBooleanArray() {
        final SparseBooleanArray ret = new SparseBooleanArray(7);
        ret.append(Calendar.SUNDAY, getRandomboolean());
        ret.append(Calendar.MONDAY, getRandomboolean());
        ret.append(Calendar.THURSDAY, getRandomboolean());
        ret.append(Calendar.WEDNESDAY, getRandomboolean());
        ret.append(Calendar.TUESDAY, getRandomboolean());
        ret.append(Calendar.FRIDAY, getRandomboolean());
        ret.append(Calendar.SATURDAY, getRandomboolean());
        return ret;
    }

    public static Map<String, String> getRandomMap_String_String() {
        final Map<String, String> ret = new HashMap<String, String>();
        for (int i = 0; i < random.nextInt(10); i++) {
            ret.put(getRandomString(), getRandomString());
        }
        return ret;
    }

    public static GregorianCalendar getRandomGregorianCalendar() {
        final GregorianCalendar cal = new GregorianCalendar();
        cal.add(Calendar.SECOND, random.nextInt(60));
        cal.add(Calendar.MINUTE, random.nextInt(60));
        cal.add(Calendar.HOUR, random.nextInt(24));
        cal.add(Calendar.DAY_OF_MONTH, random.nextInt(30));
        cal.add(Calendar.DAY_OF_YEAR, random.nextInt(1));
        return cal;
    }

    public static Context getRandomContext() {
        return ctx;
    }

    public static Map<String, SpecialListsBaseProperty> getRandomMap_String_SpecialListsBaseProperty() {
        return new HashMap<>();
    }

    public static int getRandomPriority() {
        return random.nextInt(5) - 2;
    }

    public static ListMirakel.SORT_BY getRandomSORT_BY() {
        return ListMirakel.SORT_BY.fromShort((short)random.nextInt(ListMirakel.SORT_BY.values().length));
    }


    public static Optional<Calendar> getRandomOptional_Calendar() {
        if (getRandomboolean()) {
            return of(getRandomCalendar());
        } else {
            return absent();
        }
    }

    public static Optional<Long> getRandomOptional_Long() {
        if (getRandomboolean()) {
            return of(getRandomLong());
        } else {
            return absent();
        }
    }

    public static Optional<String> getRandomOptional_String() {
        if (getRandomboolean()) {
            return of(getRandomString());
        } else {
            return absent();
        }
    }

    public static Optional<ListMirakel> getRandomOptional_ListMirakel() {
        if (getRandomboolean()) {
            return of(getRandomListMirakel());
        } else {
            return absent();
        }
    }

    public static Integer getRandomNullable_Integer() {
        if (getRandomboolean()) {
            return null;
        }
        return getRandomInteger();
    }

    public static int getRandomint(final int i) {
        return random.nextInt(i);
    }

    public static SpecialListsStringProperty.Type getRandomStringPropertyType() {
        return SpecialListsStringProperty.Type.values()[getRandomint(
                    SpecialListsStringProperty.Type.values().length)];
    }

    public static SpecialListsDueProperty.Unit getRandomUnit() {
        return SpecialListsDueProperty.Unit.values()[getRandomint(
                    SpecialListsDueProperty.Unit.values().length)];
    }

    public static List<Integer> getRandomList_ListMirakel_ID() {
        final List<ListMirakel> all = ListMirakel.all(false);
        return new ArrayList<>(Collections2.transform(all.subList(0, getRandomint(all.size())),
        new Function<ListMirakel, Integer>() {
            @Override
            public Integer apply(final ListMirakel input) {
                return (int)input.getId();
            }
        }));
    }

    public static List<SpecialList> getRandomList_SpecialList() {
        final List<SpecialList> all = SpecialList.allSpecial();
        return all.subList(0, getRandomint(all.size()));
    }

    public static SpecialListsProgressProperty.OPERATION getRandomOperation() {
        return SpecialListsProgressProperty.OPERATION.values()[getRandomint(
                    SpecialListsProgressProperty.OPERATION.values().length)];
    }

    public static Optional<Uri> getRandomOptional_Uri() {
        return absent();
    }

    public static Optional<Recurring> getRandomOptional_Recurring() {
        if (getRandomint(10) > 7) {
            return absent();
        }
        final List<Recurring> all = Recurring.all();
        return of(all.get(getRandomint(all.size())));
    }

    public static Optional<Integer> getRandomOptional_Integer() {
        if (getRandomint(10) > 7) {
            return absent();
        }
        return of(getRandomint());
    }

    public static DateTime getRandomDateTime() {
        return new DateTime().plusSeconds(random.nextInt(60)).plusMinutes(random.nextInt(60)).plusHours(
                   random.nextInt(24))
               .plusDays(random.nextInt(30)).plusMonths(random.nextInt(12));
    }

    public static Optional<DateTime> getRandomOptional_DateTime() {
        if (getRandomint(10) > 7) {
            return absent();
        }
        return of(getRandomDateTime());
    }

    public static Period getRandomPeriod() {
        return new Period(getRandomint(2), getRandomint(12), getRandomint(10), getRandomint(5),
                          getRandomint(5), getRandomint(5), 0, 0);
    }
}
