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
package $FULLPACKAGE;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.Random;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.MirakelContentProvider;


import com.google.common.base.Optional;

import de.azapps.mirakelandroid.test.RandomHelper;
import de.azapps.mirakelandroid.test.TestHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class ${TESTCLASS}Test {
    private static SQLiteDatabase database;

    @Before
    public void setUp() throws Exception{
    	TestHelper.init(Robolectric.application);
        database = DatabaseHelper.getDatabaseHelper(Robolectric.application).getWritableDatabase();
        RandomHelper.init(Robolectric.application);
        // Create at least one item to have something to test with
#foreach($CREATEFUNCTION in $CREATEFUNCTIONS)
        ${CREATEFUNCTION.function};
#end
    }

    private static int countElems() {
        final Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM $TABLE", null);
        cursor.moveToFirst();
        return cursor.getInt(0);
    }

#foreach($CREATEFUNCTION in $CREATEFUNCTIONS)
    @Test
    public void testNewCount${CREATEFUNCTION.name}${foreach.count}() {
        final int countBefore = countElems();
#if($CREATEFUNCTION.throw)
        try {
#end
            $CREATEFUNCTION.function;
#if($CREATEFUNCTION.throw)
        }catch (${TESTCLASS}.$CREATEFUNCTION.throw e) {
            fail("Exception thrown: " + e.getMessage());
        }
#end
        final int countAfter = countElems();
        assertEquals("Insert $TESTCLASS don't change the number of elements in database $CREATEFUNCTION",
                     countBefore + 1, countAfter);
    }

    @Test
    public void testNewInserted${CREATEFUNCTION.name}${foreach.count}() {
        final List<$TESTCLASS>elems = ${TESTCLASS}.${GETALL_FUNCTION};
#if($CREATEFUNCTION.throw)
        try {
#end
            final $TESTCLASS elem = $CREATEFUNCTION.function;
            elems.add(elem);
            final List<$TESTCLASS>newElems = ${TESTCLASS}.${GETALL_FUNCTION};
            final boolean result = TestHelper.listEquals(elems, newElems);
            assertTrue("Something changed while adding a new element to the database $CREATEFUNCTION", result);
#if($CREATEFUNCTION.throw)
        }catch (${TESTCLASS}.$CREATEFUNCTION.throw e) {
            fail("Exception thrown: " + e.getMessage());
        }
#end
    }

    @Test
    public void testNewEquals${CREATEFUNCTION.name}${foreach.count}() {
#if($CREATEFUNCTION.throw)
        try {
#end
            final $TESTCLASS elem = $CREATEFUNCTION.function;
            assertNotNull("Create new $TESTCLASS failed", elem);
            final long id = elem.getId();
            final Optional<$TESTCLASS> newElem = ${TESTCLASS}.get(id);
            assertEquals("get(id)!=insert()", newElem.orNull(), elem);
#if($CREATEFUNCTION.throw)
        }catch (${TESTCLASS}.$CREATEFUNCTION.throw e) {
            fail("Exception thrown: " + e.getMessage());
        }
#end
    }
#end

    // If nothing was changed the database should not be updated
    public void testUpdateEqual() {
        final List<$TESTCLASS>elems = ${TESTCLASS}.${GETALL_FUNCTION};
        final int randomItem = new Random().nextInt(elems.size());
        final $TESTCLASS elem = elems.get(randomItem);
        elem.save();
        final List<$TESTCLASS>newElems = ${TESTCLASS}.${GETALL_FUNCTION};
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("If nothing was changed the database should not be update", result);
    }

#foreach($UPDATEFUNCTION in $UPDATEFUNCTIONS)
    @Test
    public void test${UPDATEFUNCTION.name}${foreach.count}() {
        final List<$TESTCLASS>elems = ${TESTCLASS}.${GETALL_FUNCTION};
        final int randomItem = new Random().nextInt(elems.size());
        final $TESTCLASS elem = elems.get(randomItem);
#if($UPDATEFUNCTION.throw)
        try {
#end
            elem.$UPDATEFUNCTION.function;
#if($UPDATEFUNCTION.throw)
        }catch ($UPDATEFUNCTION.throw e) {
            fail("Exception thrown: " + e.getMessage());
        }
#end
        elem.save();
        final Optional<$TESTCLASS> newElem = ${TESTCLASS}.get(elem.getId());
        assertEquals("After update the elems are not equal ($UPDATEFUNCTION)", elem, newElem.orNull());
    }
#end

    @Test
    public void testDestroy() {
        final List<$TESTCLASS>elems = ${TESTCLASS}.${GETALL_FUNCTION};
        final int randomItem = new Random().nextInt(elems.size());
        final $TESTCLASS elem = elems.get(randomItem);
        final $ID_TYPE id = elem.getId();
        elem.destroy();
        assertFalse("Elem was not deleted", ${TESTCLASS}.get(id).isPresent());
        final List<$TESTCLASS>newElems = ${TESTCLASS}.${GETALL_FUNCTION};
        elems.remove(randomItem);
        // Now we have to iterate over the array and update each element
        for (int i = 0; i < elems.size(); i++) {
            elems.set(i, ${TESTCLASS}.get(elems.get(i).getId()).orNull());
        }
        final boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("Deleted more than needed", result);
    }

}
