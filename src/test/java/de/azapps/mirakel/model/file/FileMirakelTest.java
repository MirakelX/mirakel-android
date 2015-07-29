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
package de.azapps.mirakel.model.file;


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
public class FileMirakelTest extends MirakelDatabaseTestCase {
    private static SQLiteDatabase database;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        database = DatabaseHelper.getDatabaseHelper(RuntimeEnvironment.application).getWritableDatabase();
        // Create at least one item to have something to test with

        FileMirakel.newFile(RandomHelper.getRandomTask(), RandomHelper.getRandomString(),
                            RandomHelper.getRandomUri());
    }

    private static int countElems() {
        final Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM files", null);
        cursor.moveToFirst();
        return cursor.getInt(0);
    }


    @Test
    public void testNewCountNewFile1() {
        final int countBefore = countElems();
        FileMirakel.newFile(RandomHelper.getRandomTask(), RandomHelper.getRandomString(),
                            RandomHelper.getRandomUri());
        final int countAfter = countElems();
        assertEquals("Insert FileMirakel don't change the number of elements in database {'function': 'FileMirakel.newFile(RandomHelper.getRandomTask(), RandomHelper.getRandomString(), RandomHelper.getRandomUri())', 'name': 'NewFile', 'throw': None}",
                     countBefore + 1, countAfter);
    }

    @Test
    public void testNewInsertedNewFile1() {
        final List<FileMirakel>elems = FileMirakel.all();
        final FileMirakel elem = FileMirakel.newFile(RandomHelper.getRandomTask(),
                                 RandomHelper.getRandomString(), RandomHelper.getRandomUri());
        elems.add(elem);
        final List<FileMirakel>newElems = FileMirakel.all();
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("Something changed while adding a new element to the database {'function': 'FileMirakel.newFile(RandomHelper.getRandomTask(), RandomHelper.getRandomString(), RandomHelper.getRandomUri())', 'name': 'NewFile', 'throw': None}",
                   result);
    }

    @Test
    public void testNewEqualsNewFile1() {
        final FileMirakel elem = FileMirakel.newFile(RandomHelper.getRandomTask(),
                                 RandomHelper.getRandomString(), RandomHelper.getRandomUri());
        assertNotNull("Create new FileMirakel failed", elem);
        final long id = elem.getId();
        final Optional<FileMirakel> newElem = FileMirakel.get(id);
        assertEquals("get(id)!=insert()", newElem.orNull(), elem);
    }

    // If nothing was changed the database should not be updated
    @Test
    public void testUpdateEqual() {
        final List<FileMirakel>elems = FileMirakel.all();
        final int randomItem = new Random().nextInt(elems.size());
        final FileMirakel elem = elems.get(randomItem);
        elem.save();
        final List<FileMirakel>newElems = FileMirakel.all();
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("If nothing was changed the database should not be update", result);
    }


    @Test
    public void testSetTask1() {
        final List<FileMirakel>elems = FileMirakel.all();
        final int randomItem = new Random().nextInt(elems.size());
        final FileMirakel elem = elems.get(randomItem);
        elem.setTask(RandomHelper.getRandomTask());
        elem.save();
        final Optional<FileMirakel> newElem = FileMirakel.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setTask(RandomHelper.getRandomTask())', 'name': 'SetTask', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testSetFileUri2() {
        final List<FileMirakel>elems = FileMirakel.all();
        final int randomItem = new Random().nextInt(elems.size());
        final FileMirakel elem = elems.get(randomItem);
        elem.setFileUri(RandomHelper.getRandomUri());
        elem.save();
        final Optional<FileMirakel> newElem = FileMirakel.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setFileUri(RandomHelper.getRandomUri())', 'name': 'SetFileUri', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testDestroy() {
        final List<FileMirakel>elems = FileMirakel.all();
        final int randomItem = new Random().nextInt(elems.size());
        final FileMirakel elem = elems.get(randomItem);
        final long id = elem.getId();
        elem.destroy();
        assertFalse("Elem was not deleted", FileMirakel.get(id).isPresent());
        final List<FileMirakel>newElems = FileMirakel.all();
        elems.remove(randomItem);
        // Now we have to iterate over the array and update each element
        for (int i = 0; i < elems.size(); i++) {
            elems.set(i, FileMirakel.get(elems.get(i).getId()).orNull());
        }
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("Deleted more than needed", result);
    }

}
