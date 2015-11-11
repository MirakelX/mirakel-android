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


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.common.base.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.Random;

import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.MultiApiRobolectricTestRunner;
import de.azapps.mirakelandroid.test.RandomHelper;

import static com.google.common.truth.Truth.assertThat;


@RunWith(MultiApiRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class RecurringTest extends MirakelDatabaseTestCase {
    private static SQLiteDatabase database;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        database = DatabaseHelper.getDatabaseHelper(RuntimeEnvironment.application).getWritableDatabase();
        // Create at least one item to have something to test with

        Recurring.newRecurring(RandomHelper.getRandomString(), RandomHelper.getRandomPeriod(),
                               RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomOptional_DateTime(), RandomHelper.getRandomOptional_DateTime(),
                               RandomHelper.getRandomboolean(), RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomSparseBooleanArray());
    }

    private static int countElems() {
        final Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM recurring", null);
        cursor.moveToFirst();
        return cursor.getInt(0);
    }


    @Test
    public void testNewCountNewRecurring1() {
        final int countBefore = countElems();
        Recurring.newRecurring(RandomHelper.getRandomString(), RandomHelper.getRandomPeriod(),
                               RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomOptional_DateTime(), RandomHelper.getRandomOptional_DateTime(),
                               RandomHelper.getRandomboolean(), RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomSparseBooleanArray());
        final int countAfter = countElems();
        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    @Test
    public void testNewInsertedNewRecurring1() {
        final List<Recurring>elems = Recurring.all();
        final Recurring elem = Recurring.newRecurring(RandomHelper.getRandomString(),
                               RandomHelper.getRandomPeriod(), RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomOptional_DateTime(), RandomHelper.getRandomOptional_DateTime(),
                               RandomHelper.getRandomboolean(), RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomSparseBooleanArray());
        elems.add(elem);
        final List<Recurring> newElems = Recurring.all();
        assertThat(newElems).hasSize(elems.size());
        assertThat(newElems).containsExactlyElementsIn(elems);
    }

    @Test
    public void testNewEqualsNewRecurring1() {
        final Recurring elem = Recurring.newRecurring(RandomHelper.getRandomString(),
                               RandomHelper.getRandomPeriod(), RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomOptional_DateTime(), RandomHelper.getRandomOptional_DateTime(),
                               RandomHelper.getRandomboolean(), RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomSparseBooleanArray());
        assertThat(elem).isNotNull();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    // If nothing was changed the database should not be updated
    @Test
    public void testUpdateEqual() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.save();
        final List<Recurring>newElems = Recurring.all();
        assertThat(newElems).hasSize(elems.size());
        assertThat(newElems).containsExactlyElementsIn(elems);
    }


    @Test
    public void testSetLabel1() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setLabel(RandomHelper.getRandomString());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }


    @Test
    public void testSetForDue3() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setForDue(RandomHelper.getRandomboolean());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }


    @Test
    public void testSetInterval7() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setInterval(RandomHelper.getRandomPeriod());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetStartDate8() {
        final List<Recurring> elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setStartDate(RandomHelper.getRandomOptional_DateTime());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetEndDate9() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setEndDate(RandomHelper.getRandomOptional_DateTime());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetTemporary10() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setTemporary(RandomHelper.getRandomboolean());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetWeekdays11() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setWeekdays(RandomHelper.getRandomSparseBooleanArray());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetDerivedFrom12() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setDerivedFrom(RandomHelper.getRandomOptional_Long());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetExact13() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setExact(RandomHelper.getRandomboolean());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testDestroy() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        final long id = elem.getId();
        elem.destroy();
        assertThat(Recurring.get(id)).isAbsent();
        final List<Recurring>newElems = Recurring.all();
        elems.remove(randomItem);
        // Now we have to iterate over the array and update each element
        for (int i = 0; i < elems.size(); i++) {
            elems.set(i, Recurring.get(elems.get(i).getId()).orNull());
        }
        assertThat(newElems).hasSize(elems.size());
        assertThat(newElems).containsExactlyElementsIn(elems);
    }

}
