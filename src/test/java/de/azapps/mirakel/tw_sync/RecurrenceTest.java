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
package de.azapps.mirakel.tw_sync;



import com.google.common.base.Optional;
import com.google.gson.JsonObject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Period;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.sync.taskwarrior.model.TaskWarriorRecurrence;
import de.azapps.mirakel.sync.taskwarrior.model.TaskWarriorTaskSerializer;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.MultiApiRobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

@RunWith(MultiApiRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class RecurrenceTest extends MirakelDatabaseTestCase {

    private void performBackCheck(String rec, Recurring r) {
        final JsonObject e = new JsonObject();
        TaskWarriorTaskSerializer.handleRecurrence(e, r);
        String recur = e.get("recur").getAsString();
        assertThat(parseRecurring(recur).getInterval()).isEqualTo(r.getInterval());
    }

    private Recurring parseRecurring(String rec) {
        final Recurring r;
        try {
            r = new TaskWarriorRecurrence(rec, Optional.<DateTime>absent());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return r;
    }


    @Test
    public void test_1week() {
        final String rec = "1week";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 1, 0, 0, 0, 0, 0));
        performBackCheck(rec, r);
    }

    @Test
    public void test_10week() {
        final String rec = "10week";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 10, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2week() {
        final String rec = "2week";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 2, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5week() {
        final String rec = "5week";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 5, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6week() {
        final String rec = "6week";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 6, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1qtr() {
        final String rec = "1qtr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 3, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10qtr() {
        final String rec = "10qtr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 30, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2qtr() {
        final String rec = "2qtr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 6, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5qtr() {
        final String rec = "5qtr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 15, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6qtr() {
        final String rec = "6qtr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 18, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1hr() {
        final String rec = "1hr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 1, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10hr() {
        final String rec = "10hr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 10, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2hr() {
        final String rec = "2hr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 2, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5hr() {
        final String rec = "5hr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 5, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6hr() {
        final String rec = "6hr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 6, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1wk() {
        final String rec = "1wk";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 1, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10wk() {
        final String rec = "10wk";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 10, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2wk() {
        final String rec = "2wk";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 2, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5wk() {
        final String rec = "5wk";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 5, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6wk() {
        final String rec = "6wk";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 6, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1month() {
        final String rec = "1month";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10month() {
        final String rec = "10month";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 10, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2month() {
        final String rec = "2month";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 2, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5month() {
        final String rec = "5month";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 5, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6month() {
        final String rec = "6month";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 6, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1hours() {
        final String rec = "1hours";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 1, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10hours() {
        final String rec = "10hours";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 10, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2hours() {
        final String rec = "2hours";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 2, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5hours() {
        final String rec = "5hours";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 5, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6hours() {
        final String rec = "6hours";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 6, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1y() {
        final String rec = "1y";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(1, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10y() {
        final String rec = "10y";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(10, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2y() {
        final String rec = "2y";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(2, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5y() {
        final String rec = "5y";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(5, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6y() {
        final String rec = "6y";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(6, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1qrtrs() {
        final String rec = "1qrtrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 3, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10qrtrs() {
        final String rec = "10qrtrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 30, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2qrtrs() {
        final String rec = "2qrtrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 6, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5qrtrs() {
        final String rec = "5qrtrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 15, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6qrtrs() {
        final String rec = "6qrtrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 18, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1year() {
        final String rec = "1year";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(1, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10year() {
        final String rec = "10year";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(10, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2year() {
        final String rec = "2year";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(2, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5year() {
        final String rec = "5year";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(5, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6year() {
        final String rec = "6year";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(6, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1years() {
        final String rec = "1years";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(1, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10years() {
        final String rec = "10years";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(10, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2years() {
        final String rec = "2years";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(2, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5years() {
        final String rec = "5years";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(5, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6years() {
        final String rec = "6years";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(6, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1wks() {
        final String rec = "1wks";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 1, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10wks() {
        final String rec = "10wks";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 10, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2wks() {
        final String rec = "2wks";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 2, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5wks() {
        final String rec = "5wks";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 5, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6wks() {
        final String rec = "6wks";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 6, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1mins() {
        final String rec = "1mins";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 1, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10mins() {
        final String rec = "10mins";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 10, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2mins() {
        final String rec = "2mins";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 2, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5mins() {
        final String rec = "5mins";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 5, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6mins() {
        final String rec = "6mins";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 6, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1day() {
        final String rec = "1day";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 1, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10day() {
        final String rec = "10day";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 10, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2day() {
        final String rec = "2day";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 2, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5day() {
        final String rec = "5day";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 5, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6day() {
        final String rec = "6day";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 6, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1qtrs() {
        final String rec = "1qtrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 3, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10qtrs() {
        final String rec = "10qtrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 30, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2qtrs() {
        final String rec = "2qtrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 6, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5qtrs() {
        final String rec = "5qtrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 15, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6qtrs() {
        final String rec = "6qtrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 18, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1yr() {
        final String rec = "1yr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(1, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10yr() {
        final String rec = "10yr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(10, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2yr() {
        final String rec = "2yr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(2, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5yr() {
        final String rec = "5yr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(5, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6yr() {
        final String rec = "6yr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(6, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1mths() {
        final String rec = "1mths";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10mths() {
        final String rec = "10mths";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 10, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2mths() {
        final String rec = "2mths";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 2, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5mths() {
        final String rec = "5mths";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 5, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6mths() {
        final String rec = "6mths";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 6, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1d() {
        final String rec = "1d";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 1, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10d() {
        final String rec = "10d";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 10, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2d() {
        final String rec = "2d";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 2, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5d() {
        final String rec = "5d";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 5, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6d() {
        final String rec = "6d";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 6, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1hour() {
        final String rec = "1hour";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 1, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10hour() {
        final String rec = "10hour";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 10, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2hour() {
        final String rec = "2hour";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 2, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5hour() {
        final String rec = "5hour";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 5, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6hour() {
        final String rec = "6hour";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 6, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1min() {
        final String rec = "1min";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 1, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10min() {
        final String rec = "10min";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 10, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2min() {
        final String rec = "2min";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 2, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5min() {
        final String rec = "5min";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 5, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6min() {
        final String rec = "6min";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 6, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1h() {
        final String rec = "1h";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 1, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10h() {
        final String rec = "10h";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 10, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2h() {
        final String rec = "2h";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 2, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5h() {
        final String rec = "5h";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 5, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6h() {
        final String rec = "6h";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 6, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1mo() {
        final String rec = "1mo";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10mo() {
        final String rec = "10mo";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 10, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2mo() {
        final String rec = "2mo";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 2, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5mo() {
        final String rec = "5mo";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 5, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6mo() {
        final String rec = "6mo";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 6, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1months() {
        final String rec = "1months";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10months() {
        final String rec = "10months";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 10, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2months() {
        final String rec = "2months";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 2, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5months() {
        final String rec = "5months";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 5, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6months() {
        final String rec = "6months";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 6, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1days() {
        final String rec = "1days";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 1, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10days() {
        final String rec = "10days";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 10, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2days() {
        final String rec = "2days";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 2, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5days() {
        final String rec = "5days";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 5, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6days() {
        final String rec = "6days";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 6, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1hrs() {
        final String rec = "1hrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 1, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10hrs() {
        final String rec = "10hrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 10, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2hrs() {
        final String rec = "2hrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 2, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5hrs() {
        final String rec = "5hrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 5, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6hrs() {
        final String rec = "6hrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 6, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1q() {
        final String rec = "1q";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 3, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10q() {
        final String rec = "10q";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 30, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2q() {
        final String rec = "2q";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 6, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5q() {
        final String rec = "5q";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 15, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6q() {
        final String rec = "6q";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 18, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1mos() {
        final String rec = "1mos";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10mos() {
        final String rec = "10mos";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 10, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2mos() {
        final String rec = "2mos";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 2, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5mos() {
        final String rec = "5mos";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 5, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6mos() {
        final String rec = "6mos";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 6, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1mth() {
        final String rec = "1mth";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10mth() {
        final String rec = "10mth";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 10, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2mth() {
        final String rec = "2mth";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 2, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5mth() {
        final String rec = "5mth";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 5, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6mth() {
        final String rec = "6mth";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 6, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1w() {
        final String rec = "1w";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 1, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10w() {
        final String rec = "10w";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 10, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2w() {
        final String rec = "2w";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 2, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5w() {
        final String rec = "5w";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 5, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6w() {
        final String rec = "6w";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 6, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1quarters() {
        final String rec = "1quarters";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 3, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10quarters() {
        final String rec = "10quarters";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 30, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2quarters() {
        final String rec = "2quarters";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 6, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5quarters() {
        final String rec = "5quarters";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 15, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6quarters() {
        final String rec = "6quarters";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 18, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1mnths() {
        final String rec = "1mnths";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10mnths() {
        final String rec = "10mnths";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 10, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2mnths() {
        final String rec = "2mnths";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 2, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5mnths() {
        final String rec = "5mnths";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 5, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6mnths() {
        final String rec = "6mnths";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 6, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1weeks() {
        final String rec = "1weeks";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 1, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10weeks() {
        final String rec = "10weeks";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 10, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2weeks() {
        final String rec = "2weeks";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 2, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5weeks() {
        final String rec = "5weeks";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 5, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6weeks() {
        final String rec = "6weeks";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 6, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1yrs() {
        final String rec = "1yrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(1, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10yrs() {
        final String rec = "10yrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(10, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2yrs() {
        final String rec = "2yrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(2, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5yrs() {
        final String rec = "5yrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(5, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6yrs() {
        final String rec = "6yrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(6, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_1minutes() {
        final String rec = "1minutes";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 1, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_10minutes() {
        final String rec = "10minutes";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 10, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_2minutes() {
        final String rec = "2minutes";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 2, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_5minutes() {
        final String rec = "5minutes";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 5, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_6minutes() {
        final String rec = "6minutes";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 6, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_week() {
        final String rec = "week";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 1, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_qtr() {
        final String rec = "qtr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 3, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_hr() {
        final String rec = "hr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 1, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_wk() {
        final String rec = "wk";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 1, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_month() {
        final String rec = "month";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_hours() {
        final String rec = "hours";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 1, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_y() {
        final String rec = "y";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(1, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_qrtrs() {
        final String rec = "qrtrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 3, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_year() {
        final String rec = "year";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(1, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_years() {
        final String rec = "years";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(1, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_wks() {
        final String rec = "wks";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 1, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_mins() {
        final String rec = "mins";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 1, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_day() {
        final String rec = "day";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 1, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_qtrs() {
        final String rec = "qtrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 3, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_yr() {
        final String rec = "yr";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(1, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_mths() {
        final String rec = "mths";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_d() {
        final String rec = "d";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 1, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_hour() {
        final String rec = "hour";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 1, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_min() {
        final String rec = "min";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 1, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_h() {
        final String rec = "h";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 1, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_mo() {
        final String rec = "mo";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_months() {
        final String rec = "months";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_days() {
        final String rec = "days";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 1, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_hrs() {
        final String rec = "hrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 1, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_q() {
        final String rec = "q";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 3, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_mos() {
        final String rec = "mos";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_mth() {
        final String rec = "mth";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_w() {
        final String rec = "w";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 1, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_quarters() {
        final String rec = "quarters";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 3, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_mnths() {
        final String rec = "mnths";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_weeks() {
        final String rec = "weeks";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 1, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_yrs() {
        final String rec = "yrs";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(1, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_minutes() {
        final String rec = "minutes";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 0, 0, 1, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_yearly() {
        final String rec = "yearly";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(1, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_bimonthly() {
        final String rec = "bimonthly";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 2, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_biannual() {
        final String rec = "biannual";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(2, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_fortnight() {
        final String rec = "fortnight";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 2, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_biweekly() {
        final String rec = "biweekly";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 2, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_weekly() {
        final String rec = "weekly";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 1, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_monthly() {
        final String rec = "monthly";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 1, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_biyearly() {
        final String rec = "biyearly";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(2, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_annual() {
        final String rec = "annual";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(1, 0, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_semiannual() {
        final String rec = "semiannual";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 6, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_daily() {
        final String rec = "daily";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 0, 1, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_sennight() {
        final String rec = "sennight";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 0, 1, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_quarterly() {
        final String rec = "quarterly";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getInterval()).isEqualTo(new Period(0, 3, 0, 0, 0, 0, 0, 0).normalizedStandard());
        performBackCheck(rec, r);
    }

    @Test
    public void test_weekdays() {
        final String rec = "weekdays";
        final Recurring r = parseRecurring(rec);
        assertThat(r.getWeekdays()).containsExactly(DateTimeConstants.MONDAY, DateTimeConstants.TUESDAY,
                DateTimeConstants.WEDNESDAY, DateTimeConstants.THURSDAY, DateTimeConstants.FRIDAY);
        performBackCheck(rec, r);
    }
}
