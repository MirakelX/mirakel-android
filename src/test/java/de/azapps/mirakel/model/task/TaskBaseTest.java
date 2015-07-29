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

import com.google.common.base.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.List;

import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.RandomHelper;

import static de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import static org.junit.Assert.assertEquals;


@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class TaskBaseTest extends MirakelDatabaseTestCase {



    // Test for getting and setting Content
    @Test
    public void testContent1() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final String t = RandomHelper.getRandomString();
        obj.setContent(t);
        assertEquals("Getting and setting Content does not match", t, obj.getContent());
    }

    // Test for getting and setting CreatedAt
    @Test
    public void testCreatedAt2() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final Calendar t = RandomHelper.getRandomCalendar();
        obj.setCreatedAt(t);
        assertEquals("Getting and setting CreatedAt does not match", t, obj.getCreatedAt());
    }

    // Test for getting and setting Due
    @Test
    public void testDue3() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final Optional<Calendar> t = RandomHelper.getRandomOptional_Calendar();
        obj.setDue(t);
        assertEquals("Getting and setting Due does not match", t, obj.getDue());
    }

    // Test for getting and setting List
    @Test
    public void testList4() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final ListMirakel t = RandomHelper.getRandomListMirakel();
        obj.setList(t);
        assertEquals("Getting and setting List does not match", t, obj.getList());
    }

    // Test for getting and setting Name
    @Test
    public void testName5() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final String t = RandomHelper.getRandomString();
        obj.setName(t);
        assertEquals("Getting and setting Name does not match", t, obj.getName());
    }

    // Test for getting and setting Priority
    @Test
    public void testPriority6() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final int t = RandomHelper.getRandomPriority();
        obj.setPriority(t);
        assertEquals("Getting and setting Priority does not match", t, obj.getPriority());
    }

    // Test for getting and setting Progress
    @Test
    public void testProgress7() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final int t = RandomHelper.getRandomint();
        obj.setProgress(t);
        assertEquals("Getting and setting Progress does not match", t, obj.getProgress());
    }

    // Test for getting and setting Recurrence
    @Test
    public void testRecurrence8() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final Optional<Recurring> t = RandomHelper.getRandomOptional_Recurring();
        obj.setRecurrence(t);
        assertEquals("Getting and setting Recurrence does not match", t, obj.getRecurrence());
    }

    // Test for getting and setting RecurringReminder
    @Test
    public void testRecurringReminder9() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        Optional<Recurring> r = RandomHelper.getRandomOptional_Recurring();
        long id = r.isPresent() ? r.get().getId() : -1;
        obj.setRecurringReminder(id);
        assertEquals("Getting and setting RecurringReminder does not match", id,
                     obj.getRecurringReminder().isPresent() ? obj.getRecurringReminder().get().getId() : -1L);
    }

    // Test for getting and setting RecurringReminder
    @Test
    public void testRecurringReminder10() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final Optional<Recurring> t = RandomHelper.getRandomOptional_Recurring();
        obj.setRecurringReminder(t);
        assertEquals("Getting and setting RecurringReminder does not match", t, obj.getRecurringReminder());
    }

    // Test for getting and setting Reminder
    @Test
    public void testReminder11() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final Optional<Calendar> t = RandomHelper.getRandomOptional_Calendar();
        obj.setReminder(t);
        assertEquals("Getting and setting Reminder does not match", t, obj.getReminder());
    }

    // Test for getting and setting Reminder
    @Test
    public void testReminder12() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final Optional<Calendar> t = RandomHelper.getRandomOptional_Calendar();
        obj.setReminder(t);
        assertEquals("Getting and setting Reminder does not match", t, obj.getReminder());
    }

    // Test for getting and setting SyncState
    @Test
    public void testSyncState13() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final SYNC_STATE t = RandomHelper.getRandomSYNC_STATE();
        obj.setSyncState(t);
        assertEquals("Getting and setting SyncState does not match", t, obj.getSyncState());
    }

    // Test for getting and setting UpdatedAt
    @Test
    public void testUpdatedAt14() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final Calendar t = RandomHelper.getRandomCalendar();
        obj.setUpdatedAt(t);
        assertEquals("Getting and setting UpdatedAt does not match", t, obj.getUpdatedAt());
    }

    // Test for getting and setting UUID
    @Test
    public void testUUID15() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final String t = RandomHelper.getRandomString();
        obj.setUUID(t);
        assertEquals("Getting and setting UUID does not match", t, obj.getUUID());
    }

    // Test for getting and setting IsStub
    @Test
    public void testIsStub16() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final boolean t = RandomHelper.getRandomboolean();
        obj.setIsStub(t);
        assertEquals("Getting and setting IsStub does not match", t, obj.isStub());
    }

}
