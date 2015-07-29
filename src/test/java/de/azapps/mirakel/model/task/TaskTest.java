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
package de.azapps.mirakel.model.task;


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
public class TaskTest extends MirakelDatabaseTestCase {
    private static SQLiteDatabase database;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        database = DatabaseHelper.getDatabaseHelper(RuntimeEnvironment.application).getWritableDatabase();
        // Create at least one item to have something to test with

        Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel());

        Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel(),
                     RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomPriority());

        Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel(),
                     RandomHelper.getRandomString(), RandomHelper.getRandomboolean(),
                     RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomPriority());
    }

    private static int countElems() {
        final Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM tasks", null);
        cursor.moveToFirst();
        return cursor.getInt(0);
    }


    @Test
    public void testNewCountNewTask1() {
        final int countBefore = countElems();
        Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel());
        final int countAfter = countElems();
        assertEquals("Insert Task don't change the number of elements in database {'function': 'Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel())', 'name': 'NewTask', 'throw': None}",
                     countBefore + 1, countAfter);
    }

    @Test
    public void testNewInsertedNewTask1() {
        final List<Task>elems = Task.all();
        final Task elem = Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel());
        elems.add(elem);
        final List<Task>newElems = Task.all();
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("Something changed while adding a new element to the database {'function': 'Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel())', 'name': 'NewTask', 'throw': None}",
                   result);
    }

    @Test
    public void testNewEqualsNewTask1() {
        final Task elem = Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel());
        assertNotNull("Create new Task failed", elem);
        final long id = elem.getId();
        final Optional<Task> newElem = Task.get(id);
        assertEquals("get(id)!=insert()", newElem.orNull(), elem);
    }

    @Test
    public void testNewCountNewTask2() {
        final int countBefore = countElems();
        Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel(),
                     RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomPriority());
        final int countAfter = countElems();
        assertEquals("Insert Task don't change the number of elements in database {'function': 'Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel(), RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomPriority())', 'name': 'NewTask', 'throw': None}",
                     countBefore + 1, countAfter);
    }

    @Test
    public void testNewInsertedNewTask2() {
        final List<Task>elems = Task.all();
        final Task elem = Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel(),
                                       RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomPriority());
        elems.add(elem);
        final List<Task>newElems = Task.all();
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("Something changed while adding a new element to the database {'function': 'Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel(), RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomPriority())', 'name': 'NewTask', 'throw': None}",
                   result);
    }

    @Test
    public void testNewEqualsNewTask2() {
        final Task elem = Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel(),
                                       RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomPriority());
        assertNotNull("Create new Task failed", elem);
        final long id = elem.getId();
        final Optional<Task> newElem = Task.get(id);
        assertEquals("get(id)!=insert()", newElem.orNull(), elem);
    }

    @Test
    public void testNewCountNewTask3() {
        final int countBefore = countElems();
        Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel(),
                     RandomHelper.getRandomString(), RandomHelper.getRandomboolean(),
                     RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomPriority());
        final int countAfter = countElems();
        assertEquals("Insert Task don't change the number of elements in database {'function': 'Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel(), RandomHelper.getRandomString(), RandomHelper.getRandomboolean(), RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomPriority())', 'name': 'NewTask', 'throw': None}",
                     countBefore + 1, countAfter);
    }

    @Test
    public void testNewInsertedNewTask3() {
        final List<Task>elems = Task.all();
        final Task elem = Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel(),
                                       RandomHelper.getRandomString(), RandomHelper.getRandomboolean(),
                                       RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomPriority());
        elems.add(elem);
        final List<Task>newElems = Task.all();
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("Something changed while adding a new element to the database {'function': 'Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel(), RandomHelper.getRandomString(), RandomHelper.getRandomboolean(), RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomPriority())', 'name': 'NewTask', 'throw': None}",
                   result);
    }

    @Test
    public void testNewEqualsNewTask3() {
        final Task elem = Task.newTask(RandomHelper.getRandomString(), RandomHelper.getRandomListMirakel(),
                                       RandomHelper.getRandomString(), RandomHelper.getRandomboolean(),
                                       RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomPriority());
        assertNotNull("Create new Task failed", elem);
        final long id = elem.getId();
        final Optional<Task> newElem = Task.get(id);
        assertEquals("get(id)!=insert()", newElem.orNull(), elem);
    }

    // If nothing was changed the database should not be updated
    @Test
    public void testUpdateEqual() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.save();
        final List<Task>newElems = Task.all();
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("If nothing was changed the database should not be update", result);
    }


    @Test
    public void testSetContent1() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setContent(RandomHelper.getRandomString());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setContent(RandomHelper.getRandomString())', 'name': 'SetContent', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetCreatedAt2() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setCreatedAt(RandomHelper.getRandomCalendar());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setCreatedAt(RandomHelper.getRandomCalendar())', 'name': 'SetCreatedAt', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetDue3() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setDue(RandomHelper.getRandomOptional_Calendar());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setDue(RandomHelper.getRandomOptional_Calendar())', 'name': 'SetDue', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetList4() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setList(RandomHelper.getRandomListMirakel());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setList(RandomHelper.getRandomListMirakel())', 'name': 'SetList', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetName5() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setName(RandomHelper.getRandomString());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setName(RandomHelper.getRandomString())', 'name': 'SetName', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetPriority6() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setPriority(RandomHelper.getRandomPriority());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setPriority(RandomHelper.getRandomPriority())', 'name': 'SetPriority', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetProgress7() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setProgress(RandomHelper.getRandomint());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setProgress(RandomHelper.getRandomint())', 'name': 'SetProgress', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetRecurrence8() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setRecurrence(RandomHelper.getRandomOptional_Recurring());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setRecurrence(RandomHelper.getRandomOptional_Recurring())', 'name': 'SetRecurrence', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetRecurringReminder9() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setRecurringReminder(RandomHelper.getRandomlong());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setRecurringReminder(RandomHelper.getRandomlong())', 'name': 'SetRecurringReminder', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetRecurringReminder10() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setRecurringReminder(RandomHelper.getRandomOptional_Recurring());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setRecurringReminder(RandomHelper.getRandomOptional_Recurring())', 'name': 'SetRecurringReminder', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetReminder11() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setReminder(RandomHelper.getRandomOptional_Calendar());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setReminder(RandomHelper.getRandomOptional_Calendar())', 'name': 'SetReminder', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetReminder12() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setReminder(RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomboolean());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setReminder(RandomHelper.getRandomOptional_Calendar(), RandomHelper.getRandomboolean())', 'name': 'SetReminder', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetSyncState13() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setSyncState(RandomHelper.getRandomSYNC_STATE());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setSyncState(RandomHelper.getRandomSYNC_STATE())', 'name': 'SetSyncState', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetUpdatedAt14() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setUpdatedAt(RandomHelper.getRandomCalendar());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setUpdatedAt(RandomHelper.getRandomCalendar())', 'name': 'SetUpdatedAt', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetUUID15() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setUUID(RandomHelper.getRandomString());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setUUID(RandomHelper.getRandomString())', 'name': 'SetUUID', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetIsStub16() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        elem.setIsStub(RandomHelper.getRandomboolean());
        elem.save();
        final Optional<Task> newElem = Task.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setIsStub(RandomHelper.getRandomboolean())', 'name': 'SetIsStub', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testDestroy() {
        final List<Task>elems = Task.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Task elem = elems.get(randomItem);
        final long id = elem.getId();
        elem.destroy();
        assertFalse("Elem was not deleted", Task.get(id).isPresent());
        final List<Task>newElems = Task.all();
        elems.remove(randomItem);
        // Now we have to iterate over the array and update each element
        for (int i = 0; i < elems.size(); i++) {
            elems.set(i, Task.get(elems.get(i).getId()).orNull());
        }
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("Deleted more than needed", result);
    }

}
