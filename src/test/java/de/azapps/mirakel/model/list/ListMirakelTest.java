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
package de.azapps.mirakel.model.list;


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

import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.RandomHelper;
import de.azapps.mirakelandroid.test.TestHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class ListMirakelTest extends MirakelDatabaseTestCase {
    private static SQLiteDatabase database;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        database = DatabaseHelper.getDatabaseHelper(RuntimeEnvironment.application).getWritableDatabase();
        // Create at least one item to have something to test with

        ListMirakel.newList(RandomHelper.getRandomString(), RandomHelper.getRandomSORT_BY(),
                            RandomHelper.getRandomAccountMirakel());
    }

    private static int countElems() {
        final Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM lists", null);
        cursor.moveToFirst();
        return cursor.getInt(0);
    }


    @Test
    public void testNewCountNewList1() {
        final int countBefore = countElems();
        try {
            ListMirakel.newList(RandomHelper.getRandomString(), RandomHelper.getRandomSORT_BY(),
                                RandomHelper.getRandomAccountMirakel());
        } catch (ListMirakel.ListAlreadyExistsException e) {
            fail("Exception thrown: " + e.getMessage());
        }
        final int countAfter = countElems();
        assertEquals("Insert ListMirakel don't change the number of elements in database {'function': 'ListMirakel.newList(RandomHelper.getRandomString(), RandomHelper.getRandomSORT_BY(), RandomHelper.getRandomAccountMirakel())', 'name': 'NewList', 'throw': 'ListAlreadyExistsException'}",
                     countBefore + 1, countAfter);
    }

    @Test
    public void testNewInsertedNewList1() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        try {
            final ListMirakel elem = ListMirakel.newList(RandomHelper.getRandomString(),
                                     RandomHelper.getRandomSORT_BY(), RandomHelper.getRandomAccountMirakel());
            elems.add(elem);
            final List<ListMirakel>newElems = ListMirakel.all(false);
            final boolean result = TestHelper.listEquals(elems, newElems);
            assertTrue("Something changed while adding a new element to the database {'function': 'ListMirakel.newList(RandomHelper.getRandomString(), RandomHelper.getRandomSORT_BY(), RandomHelper.getRandomAccountMirakel())', 'name': 'NewList', 'throw': 'ListAlreadyExistsException'}",
                       result);
        } catch (ListMirakel.ListAlreadyExistsException e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    public void testNewEqualsNewList1() {
        try {
            final ListMirakel elem = ListMirakel.newList(RandomHelper.getRandomString(),
                                     RandomHelper.getRandomSORT_BY(), RandomHelper.getRandomAccountMirakel());
            assertNotNull("Create new ListMirakel failed", elem);
            final long id = elem.getId();
            final Optional<ListMirakel> newElem = ListMirakel.get(id);
            assertEquals("get(id)!=insert()", newElem.orNull(), elem);
        } catch (ListMirakel.ListAlreadyExistsException e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    // If nothing was changed the database should not be updated
    @Test
    public void testUpdateEqual() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.save();
        final List<ListMirakel>newElems = ListMirakel.all(false);
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("If nothing was changed the database should not be update", result);
    }


    @Test
    public void testSetListName1() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        try {
            elem.setListName(RandomHelper.getRandomString());
        } catch (ListMirakel.ListAlreadyExistsException e) {
            fail("Exception thrown: " + e.getMessage());
        }
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setListName(RandomHelper.getRandomString())', 'name': 'SetListName', 'throw': 'ListMirakel.ListAlreadyExistsException'})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetCreatedAt2() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setCreatedAt(RandomHelper.getRandomString());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setCreatedAt(RandomHelper.getRandomString())', 'name': 'SetCreatedAt', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetUpdatedAt3() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setUpdatedAt(RandomHelper.getRandomString());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setUpdatedAt(RandomHelper.getRandomString())', 'name': 'SetUpdatedAt', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetSortBy4() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setSortBy(RandomHelper.getRandomSORT_BY());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setSortBy(RandomHelper.getRandomSORT_BY())', 'name': 'SetSortBy', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetLft5() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setLft(RandomHelper.getRandomint());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setLft(RandomHelper.getRandomint())', 'name': 'SetLft', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetRgt6() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setRgt(RandomHelper.getRandomint());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setRgt(RandomHelper.getRandomint())', 'name': 'SetRgt', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetColor7() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setColor(RandomHelper.getRandomint());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setColor(RandomHelper.getRandomint())', 'name': 'SetColor', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetIconPath8() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setIconPath(RandomHelper.getRandomOptional_Uri());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setIconPath(RandomHelper.getRandomOptional_Uri())', 'name': 'SetIconPath', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetAccount9() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setAccount(RandomHelper.getRandomAccountMirakel());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setAccount(RandomHelper.getRandomAccountMirakel())', 'name': 'SetAccount', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetSyncState10() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setSyncState(RandomHelper.getRandomSYNC_STATE());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setSyncState(RandomHelper.getRandomSYNC_STATE())', 'name': 'SetSyncState', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testDestroy() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        final long id = elem.getId();
        elem.destroy();
        assertFalse("Elem was not deleted", ListMirakel.get(id).isPresent());
        final List<ListMirakel>newElems = ListMirakel.all(false);
        elems.remove(randomItem);
        // Now we have to iterate over the array and update each element
        for (int i = 0; i < elems.size(); i++) {
            elems.set(i, ListMirakel.get(elems.get(i).getId()).orNull());
        }
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("Deleted more than needed", result);
    }

}
