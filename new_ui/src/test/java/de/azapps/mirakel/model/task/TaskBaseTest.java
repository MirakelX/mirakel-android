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

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.List;

import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.MultiApiRobolectricTestRunner;
import de.azapps.mirakelandroid.test.RandomHelper;

import static com.google.common.truth.Truth.assertThat;
import static de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;


@RunWith(MultiApiRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class TaskBaseTest extends MirakelDatabaseTestCase {



    // Test for getting and setting Content
    @Test
    public void testContent1() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final String t = RandomHelper.getRandomString();
        obj.setContent(t);
        assertThat(obj.getContent()).isEqualTo(t);
    }


    // Test for getting and setting Due
    @Test
    public void testDue3() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final Optional<DateTime> t = RandomHelper.getRandomOptional_DateTime();
        obj.setDue(t);
        assertThat(obj.getDue()).isEqualTo(t);
    }

    // Test for getting and setting List
    @Test
    public void testList4() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final ListMirakel t = RandomHelper.getRandomListMirakel();
        obj.setList(t);
        assertThat(obj.getList()).isEqualTo(t);
    }

    // Test for getting and setting Name
    @Test
    public void testName5() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final String t = RandomHelper.getRandomString();
        obj.setName(t);
        assertThat(obj.getName()).isEqualTo(t);
    }

    // Test for getting and setting Priority
    @Test
    public void testPriority6() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final int t = RandomHelper.getRandomPriority();
        obj.setPriority(t);
        assertThat(obj.getPriority()).isEqualTo(t);
    }

    // Test for getting and setting Progress
    @Test
    public void testProgress7() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final int t = RandomHelper.getRandomint();
        obj.setProgress(t);
        assertThat(obj.getProgress()).isEqualTo(t);
    }

    // Test for getting and setting Recurrence
    @Test
    public void testRecurrence8() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final Optional<Recurring> t = RandomHelper.getRandomOptional_Recurring();
        obj.setRecurrence(t);
        assertThat(obj.getRecurrence()).isEqualTo(t);
    }

    // Test for getting and setting RecurringReminder
    @Test
    public void testRecurringReminder9() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        Optional<Recurring> r = RandomHelper.getRandomOptional_Recurring();
        long id = r.isPresent() ? r.get().getId() : -1L;
        obj.setRecurringReminder(id);
        if (r.isPresent()) {
            assertThat(r.get().getId()).isEqualTo(id);
        } else {
            assertThat(-1L).isEqualTo(id);
        }
    }

    // Test for getting and setting RecurringReminder
    @Test
    public void testRecurringReminder10() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final Optional<Recurring> t = RandomHelper.getRandomOptional_Recurring();
        obj.setRecurringReminder(t);
        assertThat(obj.getRecurringReminder()).isEqualTo(t);
    }

    // Test for getting and setting Reminder
    @Test
    public void testReminder11() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final Optional<DateTime> t = RandomHelper.getRandomOptional_DateTime();
        obj.setReminder(t);
        assertThat(obj.getReminder()).isEqualTo(t);
    }

    // Test for getting and setting Reminder
    @Test
    public void testReminder12() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final Optional<DateTime> t = RandomHelper.getRandomOptional_DateTime();
        obj.setReminder(t);
        assertThat(obj.getReminder()).isEqualTo(t);
    }

    // Test for getting and setting SyncState
    @Test
    public void testSyncState13() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final SYNC_STATE t = RandomHelper.getRandomSYNC_STATE();
        obj.setSyncState(t);
        assertThat(obj.getSyncState()).isEqualTo(t);
    }

    // Test for getting and setting UpdatedAt
    @Test
    public void testUpdatedAt14() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final DateTime t = RandomHelper.getRandomDateTime();
        obj.setUpdatedAt(t);
        assertThat(obj.getUpdatedAt()).isEqualTo(t);
    }

    // Test for getting and setting UUID
    @Test
    public void testUUID15() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final String t = RandomHelper.getRandomString();
        obj.setUUID(t);
        assertThat(obj.getUUID()).isEqualTo(t);
    }

    // Test for getting and setting IsStub
    @Test
    public void testIsStub16() {
        final List<Task> all = Task.all();
        final Task obj = RandomHelper.getRandomElem(all);
        final boolean t = RandomHelper.getRandomboolean();
        obj.setIsStub(t);
        assertThat(obj.isStub()).isEqualTo(t);
    }

}
