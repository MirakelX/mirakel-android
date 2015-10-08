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
package de.azapps.mirakel.model.semantic;


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

import static com.google.common.base.Optional.of;
import static com.google.common.truth.Truth.assertThat;

@RunWith(MultiApiRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class SemanticTest extends MirakelDatabaseTestCase {
    private static SQLiteDatabase database;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        database = DatabaseHelper.getDatabaseHelper(RuntimeEnvironment.application).getWritableDatabase();
        // Create at least one item to have something to test with

        Semantic.newSemantic(RandomHelper.getRandomString(), RandomHelper.getRandomPriority(),
                             RandomHelper.getRandomInteger(), RandomHelper.getRandomOptional_ListMirakel(),
                             RandomHelper.getRandomInteger());
    }

    private static int countElems() {
        final Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM semantic_conditions", null);
        cursor.moveToFirst();
        return cursor.getInt(0);
    }


    @Test
    public void testNewCountNewSemantic1() {
        final int countBefore = countElems();
        Semantic.newSemantic(RandomHelper.getRandomString(), RandomHelper.getRandomPriority(),
                             RandomHelper.getRandomInteger(), RandomHelper.getRandomOptional_ListMirakel(),
                             RandomHelper.getRandomInteger());
        final int countAfter = countElems();
        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    @Test
    public void testNewInsertedNewSemantic1() {
        final List<Semantic>elems = Semantic.all();
        final Semantic elem = Semantic.newSemantic(RandomHelper.getRandomString(),
                              RandomHelper.getRandomPriority(), RandomHelper.getRandomInteger(),
                              RandomHelper.getRandomOptional_ListMirakel(), RandomHelper.getRandomInteger());
        elems.add(elem);
        final List<Semantic>newElems = Semantic.all();
        assertThat(newElems).hasSize(elems.size());
        assertThat(newElems).containsExactlyElementsIn(elems);
    }

    @Test
    public void testNewEqualsNewSemantic1() {
        final Semantic elem = Semantic.newSemantic(RandomHelper.getRandomString(),
                              RandomHelper.getRandomPriority(), RandomHelper.getRandomInteger(),
                              RandomHelper.getRandomOptional_ListMirakel(), RandomHelper.getRandomInteger());
        assertThat(elem).isNotNull();
        final long id = elem.getId();
        final Optional<Semantic> newElem = Semantic.get(id);
        assertThat(newElem).hasValue(elem);
    }

    // If nothing was changed the database should not be updated
    @Test
    public void testUpdateEqual() {
        final List<Semantic>elems = Semantic.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Semantic elem = elems.get(randomItem);
        elem.save();
        final List<Semantic>newElems = Semantic.all();
        assertThat(newElems).hasSize(elems.size());
        assertThat(newElems).containsExactlyElementsIn(newElems);
    }


    @Test
    public void testSetCondition1() {
        final List<Semantic>elems = Semantic.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Semantic elem = elems.get(randomItem);
        elem.setCondition(RandomHelper.getRandomString());
        elem.save();
        final Optional<Semantic> newElem = Semantic.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetPriority2() {
        final List<Semantic>elems = Semantic.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Semantic elem = elems.get(randomItem);
        elem.setPriority(RandomHelper.getRandomint(10) > 7 ? Optional.<Integer>absent() : of(
                             RandomHelper.getRandomPriority()));
        elem.save();
        final Optional<Semantic> newElem = Semantic.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetDue3() {
        final List<Semantic>elems = Semantic.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Semantic elem = elems.get(randomItem);
        elem.setDue(RandomHelper.getRandomOptional_Integer());
        elem.save();
        final Optional<Semantic> newElem = Semantic.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetList4() {
        final List<Semantic>elems = Semantic.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Semantic elem = elems.get(randomItem);
        elem.setList(RandomHelper.getRandomOptional_ListMirakel());
        elem.save();
        final Optional<Semantic> newElem = Semantic.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetWeekday5() {
        final List<Semantic>elems = Semantic.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Semantic elem = elems.get(randomItem);
        elem.setWeekday(RandomHelper.getRandomOptional_Integer());
        elem.save();
        final Optional<Semantic> newElem = Semantic.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testDestroy() {
        final List<Semantic>elems = Semantic.all();
        final int randomItem = new Random().nextInt(elems.size());
        final Semantic elem = elems.get(randomItem);
        final long id = elem.getId();
        elem.destroy();
        assertThat(Semantic.get(id)).isAbsent();
        final List<Semantic>newElems = Semantic.all();
        elems.remove(randomItem);
        // Now we have to iterate over the array and update each element
        for (int i = 0; i < elems.size(); i++) {
            elems.set(i, Semantic.get(elems.get(i).getId()).orNull());
        }
        assertThat(newElems).hasSize(elems.size());
        assertThat(newElems).containsExactlyElementsIn(elems);
    }

}
