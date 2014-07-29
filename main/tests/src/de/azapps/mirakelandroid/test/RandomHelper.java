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
package de.azapps.mirakelandroid.test;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.SparseBooleanArray;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.meta.SpecialListsBaseProperty;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.FileUtils;

public class RandomHelper {

    // its ok to use this here, it's only for testing
    @SuppressLint("TrulyRandom")
    private static SecureRandom random = new SecureRandom();
    private static Context ctx;

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
        return all.get(random.nextInt(all.size()));
    }

    public static Integer getRandomInteger() {
        return getRandomint();
    }

    public static ListMirakel getRandomListMirakel() {
        ListMirakel.init(ctx);
        final List<ListMirakel> t = ListMirakel.all();
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
        return ACCOUNT_TYPES.values()[random
                                      .nextInt(ACCOUNT_TYPES.values().length)];
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
}
