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
package de.azapps.mirakel.model.tags;


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
public class TagTest extends MirakelDatabaseTestCase {
    private static SQLiteDatabase database;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        database = DatabaseHelper.getDatabaseHelper(RuntimeEnvironment.application).getWritableDatabase();
        // Create at least one item to have something to test with

        Tag.newTag(RandomHelper.getRandomString());

        Tag.newTag(RandomHelper.getRandomString(), RandomHelper.getRandomboolean(),
                   RandomHelper.getRandomint());
    }

    private static int countElems() {
        final Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM tag", null);
        cursor.moveToFirst();
        return cursor.getInt(0);
    }


    @Test
    public void testNewCountNewTag1() {
        final int countBefore = countElems();
        Tag.newTag(RandomHelper.getRandomString());
        final int countAfter = countElems();
        assertEquals("Insert Tag don't change the number of elements in database {'function': 'Tag.newTag(RandomHelper.getRandomString())', 'name': 'NewTag', 'throw': None}",
                     countBefore + 1, countAfter);
    }

    @Test
    public void testNewInsertedNewTag1() {
        final List<Tag>elems = Tag.all();
        final Tag elem = Tag.newTag(RandomHelper.getRandomString());
        elems.add(elem);
        final List<Tag>newElems = Tag.all();
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("Something changed while adding a new element to the database {'function': 'Tag.newTag(RandomHelper.getRandomString())', 'name': 'NewTag', 'throw': None}",
                   result);
    }

    @Test
    public void testNewEqualsNewTag1() {
        final Tag elem = Tag.newTag(RandomHelper.getRandomString());
        assertNotNull("Create new Tag failed", elem);
        final long id = elem.getId();
        final Optional<Tag> newElem = Tag.get(id);
        assertEquals("get(id)!=insert()", newElem.orNull(), elem);
    }

    @Test
    public void testNewCountNewTag2() {
        final int countBefore = countElems();
        Tag.newTag(RandomHelper.getRandomString(), RandomHelper.getRandomboolean(),
                   RandomHelper.getRandomint());
        final int countAfter = countElems();
        assertEquals("Insert Tag don't change the number of elements in database {'function': 'Tag.newTag(RandomHelper.getRandomString(), RandomHelper.getRandomboolean(), RandomHelper.getRandomint())', 'name': 'NewTag', 'throw': None}",
                     countBefore + 1, countAfter);
    }

    @Test
    public void testNewInsertedNewTag2() {
        final List<Tag>elems = Tag.all();
        final Tag elem = Tag.newTag(RandomHelper.getRandomString(), RandomHelper.getRandomboolean(),
                                    RandomHelper.getRandomint());
        elems.add(elem);
        final List<Tag>newElems = Tag.all();
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("Something changed while adding a new element to the database {'function': 'Tag.newTag(RandomHelper.getRandomString(), RandomHelper.getRandomboolean(), RandomHelper.getRandomint())', 'name': 'NewTag', 'throw': None}",
                   result);
    }

    @Test
    public void testNewEqualsNewTag2() {
        final Tag elem = Tag.newTag(RandomHelper.getRandomString(), RandomHelper.getRandomboolean(),
                                    RandomHelper.getRandomint());
        assertNotNull("Create new Tag failed", elem);
        final long id = elem.getId();
        final Optional<Tag> newElem = Tag.get(id);
        assertEquals("get(id)!=insert()", newElem.orNull(), elem);
    }

    // If nothing was changed the database should not be updated
    @Test
    public void testUpdateEqual() {
        final List<Tag>elems = Tag.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Tag elem = elems.get(randomItem);
        elem.save();
        final List<Tag>newElems = Tag.all();
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("If nothing was changed the database should not be update", result);
    }


    @Test
    public void testSetBackgroundColor1() {
        final List<Tag>elems = Tag.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Tag elem = elems.get(randomItem);
        elem.setBackgroundColor(RandomHelper.getRandomint());
        elem.save();
        final Optional<Tag> newElem = Tag.get(elem.getId());
        assertEquals("After update the elems are not equal ({'function': 'setBackgroundColor(RandomHelper.getRandomint())', 'name': 'SetBackgroundColor', 'throw': None})",
                     elem, newElem.orNull());
    }

    @Test
    public void testDestroy() {
        final List<Tag>elems = Tag.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Tag elem = elems.get(randomItem);
        final long id = elem.getId();
        elem.destroy();
        assertFalse("Elem was not deleted", Tag.get(id).isPresent());
        final List<Tag>newElems = Tag.all();
        elems.remove(randomItem);
        // Now we have to iterate over the array and update each element
        for (int i = 0; i < elems.size(); i++) {
            elems.set(i, Tag.get(elems.get(i).getId()).orNull());
        }
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("Deleted more than needed", result);
    }

}
