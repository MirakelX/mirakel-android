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
import static org.junit.Assert.fail;


@RunWith(MultiApiRobolectricTestRunner.class)
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
        assertThat(countAfter).isEqualTo(countBefore + 1);
    }



    @Test
    public void testNewInsertedNewList1() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        try {
            final ListMirakel elem = ListMirakel.newList(RandomHelper.getRandomString(),
                                     RandomHelper.getRandomSORT_BY(), RandomHelper.getRandomAccountMirakel());
            elems.add(elem);
            final List<ListMirakel> newElems = ListMirakel.all(false);
            assertThat(newElems).containsExactlyElementsIn(elems);
        } catch (ListMirakel.ListAlreadyExistsException e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    public void testNewEqualsNewList1() {
        try {
            final ListMirakel elem = ListMirakel.newList(RandomHelper.getRandomString(),
                                     RandomHelper.getRandomSORT_BY(), RandomHelper.getRandomAccountMirakel());
            assertThat(elem).isNotNull();
            final long id = elem.getId();
            final Optional<ListMirakel> newElem = ListMirakel.get(id);
            assertThat(newElem).hasValue(elem);
        } catch (final ListMirakel.ListAlreadyExistsException e) {
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
        assertThat(newElems).containsExactlyElementsIn(elems);
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
        assertThat(newElem).hasValue(elem);
    }


    @Test
    public void testSetSortBy4() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setSortBy(RandomHelper.getRandomSORT_BY());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetLft5() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setLft(RandomHelper.getRandomint());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
    }

    @Test
    public void testSetRgt6() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setRgt(RandomHelper.getRandomint());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetColor7() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setColor(RandomHelper.getRandomint());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetIconPath8() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setIconPath(RandomHelper.getRandomOptional_Uri());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetAccount9() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setAccount(RandomHelper.getRandomAccountMirakel());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetSyncState10() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        elem.setSyncState(RandomHelper.getRandomSYNC_STATE());
        elem.save();
        final Optional<ListMirakel> newElem = ListMirakel.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testDestroy() {
        final List<ListMirakel>elems = ListMirakel.all(false);
        final int randomItem = new Random().nextInt(elems.size());
        final ListMirakel elem = elems.get(randomItem);
        final long id = elem.getId();
        elem.destroy();
        assertThat(ListMirakel.get(id)).isAbsent();
        final List<ListMirakel>newElems = ListMirakel.all(false);
        elems.remove(randomItem);
        // Now we have to iterate over the array and update each element
        for (int i = 0; i < elems.size(); i++) {
            elems.set(i, ListMirakel.get(elems.get(i).getId()).orNull());
        }
        assertThat(newElems).containsExactlyElementsIn(elems);
    }

}
