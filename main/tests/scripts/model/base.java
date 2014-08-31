/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package $FULLPACKAGE;


import java.util.List;
import java.util.Random;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.MirakelContentProvider;

import android.test.suitebuilder.annotation.MediumTest;

import de.azapps.mirakelandroid.test.MirakelTestCase;
import de.azapps.mirakelandroid.test.RandomHelper;
import de.azapps.mirakelandroid.test.TestHelper;


public class ${TESTCLASS} Test extends MirakelTestCase {
    private static SQLiteDatabase database;

    @Override
    protected void setUp()throws Exception{
        super.setUp();
        database = DatabaseHelper.getDatabaseHelper(getContext()).getWritableDatabase();
        RandomHelper.init(getContext());
        // Create at least one item to have something to test with
#foreach($CREATEFUNCTION in $CREATEFUNCTIONS)
        ${CREATEFUNCTION.function};
#end
    }

    private int countElems() {
        Cursor c = database.rawQuery("SELECT COUNT(*) FROM $TABLE", null);
        c.moveToFirst();
        return c.getInt(0);
    }

#foreach($CREATEFUNCTION in $CREATEFUNCTIONS)
    @MediumTest
    public void testNewCount${CREATEFUNCTION.name} ${foreach.count}() {
        int count_before = countElems();
#if($CREATEFUNCTION.throw)
        try {
#end
            $CREATEFUNCTION.function;
#if($CREATEFUNCTION.throw)
        } catch ($ {TESTCLASS} .$CREATEFUNCTION.throw e) {
            fail("Exception thrown: " + e.getMessage());
        }
#end
        int count_after = countElems();
        assertEquals("Insert $TESTCLASS don't change the number of elements in database $CREATEFUNCTION",
                     count_before + 1, count_after);
    }

    @MediumTest
    public void testNewInserted${CREATEFUNCTION.name} ${foreach.count}() {
        List<$TESTCLASS>elems = $ {TESTCLASS} .$ {GETALL_FUNCTION};
#if($CREATEFUNCTION.throw)
        try {
#end
            $TESTCLASS elem = $CREATEFUNCTION.function;
            elems.add(elem);
            List<$TESTCLASS>newElems = $ {TESTCLASS} .$ {GETALL_FUNCTION};
            boolean result = TestHelper.listEquals(elems, newElems);
            assertTrue("Something changed while adding a new element to the database $CREATEFUNCTION", result);
#if($CREATEFUNCTION.throw)
        } catch ($ {TESTCLASS} .$CREATEFUNCTION.throw e) {
            fail("Exception thrown: " + e.getMessage());
        }
#end
    }

    @MediumTest
    public void testNewEquals${CREATEFUNCTION.name} ${foreach.count}() {
#if($CREATEFUNCTION.throw)
        try {
#end
            $TESTCLASS elem = $CREATEFUNCTION.function;
            assertNotNull("Create new $TESTCLASS failed", elem);
            int id = (int)elem.getId();
            $TESTCLASS newElem = $ {TESTCLASS} .get(id);
            assertEquals("get(id)!=insert()", newElem, elem);
#if($CREATEFUNCTION.throw)
        } catch ($ {TESTCLASS} .$CREATEFUNCTION.throw e) {
            fail("Exception thrown: " + e.getMessage());
        }
#end
    }
#end

    // If nothing was changed the database should not be updated
    public void testUpdateEqual() {
        List<$TESTCLASS>elems = $ {TESTCLASS} .$ {GETALL_FUNCTION};
        int randomItem = new Random().nextInt(elems.size());
        $TESTCLASS elem = elems.get(randomItem);
        elem.save();
        List<$TESTCLASS>newElems = $ {TESTCLASS} .$ {GETALL_FUNCTION};
        boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("If nothing was changed the database should not be update", result);
    }

#foreach($UPDATEFUNCTION in $UPDATEFUNCTIONS)
    @MediumTest
    public void test${UPDATEFUNCTION.name} ${foreach.count}() {
        List<$TESTCLASS>elems = $ {TESTCLASS} .$ {GETALL_FUNCTION};
        int randomItem = new Random().nextInt(elems.size());
        $TESTCLASS elem = elems.get(randomItem);
#if($UPDATEFUNCTION.throw)
        try {
#end
            elem.$UPDATEFUNCTION.function;
#if($UPDATEFUNCTION.throw)
        } catch ($UPDATEFUNCTION.throw e) {
            fail("Exception thrown: " + e.getMessage());
        }
#end
        elem.save();
        $TESTCLASS newElem = $ {TESTCLASS} .get(elem.getId());
        assertEquals("After update the elems are not equal ($UPDATEFUNCTION)", elem, newElem);
    }
#end

    @MediumTest
    public void testDestroy() {
        List<$TESTCLASS>elems = $ {TESTCLASS} .$ {GETALL_FUNCTION};
        int randomItem = new Random().nextInt(elems.size());
        $TESTCLASS elem = elems.get(randomItem);
        $ID_TYPE id = elem.getId();
        elem.destroy();
        assertNull("Elem was not deleted", $ {TESTCLASS} .get(id));
        List<$TESTCLASS>newElems = $ {TESTCLASS} .$ {GETALL_FUNCTION};
        elems.remove(randomItem);
        // Now we have to iterate over the array and update each element
        for (int i = 0; i < elems.size(); i++) {
            elems.set(i, $ {TESTCLASS} .get(elems.get(i).getId()));
        }
        boolean result = TestHelper.listEquals(elems, newElems);
        assertTrue("Deleted more than needed", result);
    }

}
