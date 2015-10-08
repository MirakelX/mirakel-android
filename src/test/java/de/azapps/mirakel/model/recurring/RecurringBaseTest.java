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
package de.azapps.mirakel.model.recurring;

import android.util.SparseBooleanArray;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.List;

import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.MultiApiRobolectricTestRunner;
import de.azapps.mirakelandroid.test.RandomHelper;

import static com.google.common.truth.Truth.assertThat;


@RunWith(MultiApiRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class RecurringBaseTest extends MirakelDatabaseTestCase {



    // Test for getting and setting Label
    @Test
    public void testLabel1() {
        final List<Recurring> all = Recurring.all();
        final Recurring obj = RandomHelper.getRandomElem(all);
        final String t = RandomHelper.getRandomString();
        obj.setLabel(t);
        assertThat(obj.getLabel()).isEqualTo(t);
    }


    // Test for getting and setting ForDue
    @Test
    public void testForDue3() {
        final List<Recurring> all = Recurring.all();
        final Recurring obj = RandomHelper.getRandomElem(all);
        final boolean t = RandomHelper.getRandomboolean();
        obj.setForDue(t);
        assertThat(obj.isForDue()).isEqualTo(t);
    }


    // Test for getting and setting Minutes
    @Test
    public void testInterval() {
        final List<Recurring> all = Recurring.all();
        final Recurring obj = RandomHelper.getRandomElem(all);
        final Period t = RandomHelper.getRandomPeriod();
        obj.setInterval(t);
        assertThat(obj.getInterval()).isEqualTo(t);
    }

    // Test for getting and setting StartDate
    @Test
    public void testStartDate8() {
        final List<Recurring> all = Recurring.all();
        final Recurring obj = RandomHelper.getRandomElem(all);
        final Optional<DateTime> t = RandomHelper.getRandomOptional_DateTime();
        obj.setStartDate(t);
        assertThat(obj.getStartDate()).isEqualTo(t);
    }

    // Test for getting and setting EndDate
    @Test
    public void testEndDate9() {
        final List<Recurring> all = Recurring.all();
        final Recurring obj = RandomHelper.getRandomElem(all);
        final Optional<DateTime> t = RandomHelper.getRandomOptional_DateTime();
        obj.setEndDate(t);
        assertThat(obj.getEndDate()).isEqualTo(t);
    }

    // Test for getting and setting Temporary
    @Test
    public void testTemporary10() {
        final List<Recurring> all = Recurring.all();
        final Recurring obj = RandomHelper.getRandomElem(all);
        final boolean t = RandomHelper.getRandomboolean();
        obj.setTemporary(t);
        assertThat(obj.isTemporary()).isEqualTo(t);
    }

    // Test for getting and setting Weekdays
    @Test
    public void testWeekdays11() {
        final List<Recurring> all = Recurring.all();
        final Recurring obj = RandomHelper.getRandomElem(all);
        final SparseBooleanArray t = RandomHelper.getRandomSparseBooleanArray();
        obj.setWeekdays(t);
        List<Integer> days = obj.getWeekdays();
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i)) {
                assertThat(days.contains(i)).isTrue();
            } else {
                assertThat(days.contains(i)).isFalse();
            }
        }
    }

    // Test for getting and setting DerivedFrom
    @Test
    public void testDerivedFrom12() {
        final List<Recurring> all = Recurring.all();
        final Recurring obj = RandomHelper.getRandomElem(all);
        final Optional<Long> t = RandomHelper.getRandomOptional_Long();
        obj.setDerivedFrom(t);
        assertThat(obj.getDerivedFrom()).isEqualTo(t);
    }

    // Test for getting and setting Exact
    @Test
    public void testExact13() {
        final List<Recurring> all = Recurring.all();
        final Recurring obj = RandomHelper.getRandomElem(all);
        final boolean t = RandomHelper.getRandomboolean();
        obj.setExact(t);
        assertThat(obj.isExact()).isEqualTo(t);
    }

}
