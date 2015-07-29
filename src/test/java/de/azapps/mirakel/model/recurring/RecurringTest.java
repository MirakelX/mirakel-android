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
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.Random;

import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.RandomHelper;
import de.azapps.mirakelandroid.test.TestHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class RecurringTest extends MirakelDatabaseTestCase {
    private static SQLiteDatabase database;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        database = DatabaseHelper.getDatabaseHelper(RuntimeEnvironment.application).getWritableDatabase();
        // Create at least one item to have something to test with

        Recurring.newRecurring(RandomHelper.getRandomString(), RandomHelper.getRandomint(),
                               RandomHelper.getRandomint(), RandomHelper.getRandomint(), RandomHelper.getRandomint(),
                               RandomHelper.getRandomint(), RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomOptional_Calendar(),
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
        Recurring.newRecurring(RandomHelper.getRandomString(), RandomHelper.getRandomint(),
                               RandomHelper.getRandomint(), RandomHelper.getRandomint(), RandomHelper.getRandomint(),
                               RandomHelper.getRandomint(), RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomOptional_Calendar(),
                               RandomHelper.getRandomboolean(), RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomSparseBooleanArray());
        final int countAfter = countElems();
        assertEquals("Insert Recurring don't change the number of elements in database {'function': 'Recurring.newRecurring(RandomHelper.getRandomString(), RandomHelper.getRandomint(), RandomHelper.getRandomint(), RandomHelper.getRandomint(), RandomHelper.getRandomint(), RandomHelper.getRandomint(), RandomHelper.getRandomboolean(), RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomboolean(), RandomHelper.getRandomboolean(), RandomHelper.getRandomSparseBooleanArray())', 'name': 'NewRecurring', 'throw': None}",
                     countBefore + 1, countAfter);
    }

    @Test
    public void testNewInsertedNewRecurring1() {
        final List<Recurring>elems = Recurring.all();
        final Recurring elem = Recurring.newRecurring(RandomHelper.getRandomString(),
                               RandomHelper.getRandomint(), RandomHelper.getRandomint(), RandomHelper.getRandomint(),
                               RandomHelper.getRandomint(), RandomHelper.getRandomint(), RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomOptional_Calendar(),
                               RandomHelper.getRandomboolean(), RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomSparseBooleanArray());
        elems.add(elem);
        final List<Recurring>newElems = Recurring.all();
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("Something changed while adding a new element to the database {'function': 'Recurring.newRecurring(RandomHelper.getRandomString(), RandomHelper.getRandomint(), RandomHelper.getRandomint(), RandomHelper.getRandomint(), RandomHelper.getRandomint(), RandomHelper.getRandomint(), RandomHelper.getRandomboolean(), RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomboolean(), RandomHelper.getRandomboolean(), RandomHelper.getRandomSparseBooleanArray())', 'name': 'NewRecurring', 'throw': None}",
                   result);
    }

    @Test
    public void testNewEqualsNewRecurring1() {
        final Recurring elem = Recurring.newRecurring(RandomHelper.getRandomString(),
                               RandomHelper.getRandomint(), RandomHelper.getRandomint(), RandomHelper.getRandomint(),
                               RandomHelper.getRandomint(), RandomHelper.getRandomint(), RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomOptional_Calendar(),
                               RandomHelper.getRandomboolean(), RandomHelper.getRandomboolean(),
                               RandomHelper.getRandomSparseBooleanArray());
        assertNotNull("Create new Recurring failed", elem);
        final long id = elem.getId();
        final Optional<Recurring> newElem = Recurring.get(id);
        assertEquals("get(id)!=insert()", newElem.orNull(), elem);
    }

    // If nothing was changed the database should not be updated
    @Test
    public void testUpdateEqual() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.save();
        final List<Recurring>newElems = Recurring.all();
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("If nothing was changed the database should not be update", result);
    }


    @Test
    public void testSetLabel1() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setLabel(RandomHelper.getRandomString());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setLabel(RandomHelper.getRandomString())', 'name': 'SetLabel', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetYears2() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setYears(RandomHelper.getRandomint());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setYears(RandomHelper.getRandomint())', 'name': 'SetYears', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetForDue3() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setForDue(RandomHelper.getRandomboolean());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setForDue(RandomHelper.getRandomboolean())', 'name': 'SetForDue', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetMonths4() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setMonths(RandomHelper.getRandomint());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setMonths(RandomHelper.getRandomint())', 'name': 'SetMonths', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetDays5() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setDays(RandomHelper.getRandomint());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setDays(RandomHelper.getRandomint())', 'name': 'SetDays', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetHours6() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setHours(RandomHelper.getRandomint());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setHours(RandomHelper.getRandomint())', 'name': 'SetHours', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetMinutes7() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setMinutes(RandomHelper.getRandomint());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setMinutes(RandomHelper.getRandomint())', 'name': 'SetMinutes', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetStartDate8() {
        final List<Recurring> elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setStartDate(RandomHelper.getRandomOptional_Calendar());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setStartDate(RandomHelper.getRandomOptional_Calendar())', 'name': 'SetStartDate', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetEndDate9() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setEndDate(RandomHelper.getRandomOptional_Calendar());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setEndDate(RandomHelper.getRandomOptional_Calendar())', 'name': 'SetEndDate', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetTemporary10() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setTemporary(RandomHelper.getRandomboolean());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setTemporary(RandomHelper.getRandomboolean())', 'name': 'SetTemporary', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetWeekdays11() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setWeekdays(RandomHelper.getRandomSparseBooleanArray());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setWeekdays(RandomHelper.getRandomSparseBooleanArray())', 'name': 'SetWeekdays', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetDerivedFrom12() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setDerivedFrom(RandomHelper.getRandomOptional_Long());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setDerivedFrom(RandomHelper.getRandomOptional_Long())', 'name': 'SetDerivedFrom', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetExact13() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        elem.setExact(RandomHelper.getRandomboolean());
        elem.save();
        final Optional<Recurring> newElem = Recurring.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setExact(RandomHelper.getRandomboolean())', 'name': 'SetExact', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testDestroy() {
        final List<Recurring>elems = Recurring.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Recurring elem = elems.get(randomItem);
        final long id = elem.getId();
        elem.destroy();
        assertFalse("Elem was not deleted", Recurring.get(id).isPresent());
        final List<Recurring>newElems = Recurring.all();
        elems.remove(randomItem);
        // Now we have to iterate over the array and update each element
        for (int i = 0; i < elems.size(); i++) {
            elems.set(i, Recurring.get(elems.get(i).getId()).orNull());
        }
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("Deleted more than needed", result);
    }

}
