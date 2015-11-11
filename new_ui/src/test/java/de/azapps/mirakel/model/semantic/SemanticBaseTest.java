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

import com.google.common.base.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.List;

import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.MultiApiRobolectricTestRunner;
import de.azapps.mirakelandroid.test.RandomHelper;

import static com.google.common.base.Optional.of;
import static com.google.common.truth.Truth.assertThat;


@RunWith(MultiApiRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class SemanticBaseTest extends MirakelDatabaseTestCase {



    // Test for getting and setting Condition
    @Test
    public void testCondition1() {
        final List<Semantic> all = Semantic.all();
        final Semantic obj = RandomHelper.getRandomElem(all);
        final String t = RandomHelper.getRandomString();
        obj.setCondition(t);
        assertThat(obj.getCondition()).isEqualTo(t);
    }

    // Test for getting and setting Priority
    @Test
    public void testPriority2() {
        final List<Semantic> all = Semantic.all();
        final Semantic obj = RandomHelper.getRandomElem(all);
        final Optional<Integer> t = RandomHelper.getRandomint(10) > 7 ? Optional.<Integer>absent() : of(
                                        RandomHelper.getRandomPriority());
        obj.setPriority(t);
        assertThat(obj.getPriority()).isEqualTo(t);
    }

    // Test for getting and setting Due
    @Test
    public void testDue3() {
        final List<Semantic> all = Semantic.all();
        final Semantic obj = RandomHelper.getRandomElem(all);
        final Optional<Integer> t = RandomHelper.getRandomOptional_Integer();
        obj.setDue(t);
        assertThat(obj.getDue()).isEqualTo(t);
    }

    // Test for getting and setting List
    @Test
    public void testList4() {
        final List<Semantic> all = Semantic.all();
        final Semantic obj = RandomHelper.getRandomElem(all);
        final Optional<ListMirakel> t = RandomHelper.getRandomOptional_ListMirakel();
        obj.setList(t);
        assertThat(obj.getList()).isEqualTo(t);
    }

    // Test for getting and setting Weekday
    @Test
    public void testWeekday5() {
        final List<Semantic> all = Semantic.all();
        final Semantic obj = RandomHelper.getRandomElem(all);
        final Optional<Integer> t = RandomHelper.getRandomOptional_Integer();
        obj.setWeekday(t);
        assertThat(obj.getWeekday()).isEqualTo(t);
    }

}
