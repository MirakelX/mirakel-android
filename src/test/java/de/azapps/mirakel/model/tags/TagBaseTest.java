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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.RandomHelper;

import static com.google.common.truth.Truth.assertThat;


@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class TagBaseTest extends MirakelDatabaseTestCase {



    // Test for getting and setting BackgroundColor
    @Test
    public void testBackgroundColor1() {
        final List<Tag> all = Tag.all();
        final Tag obj = RandomHelper.getRandomElem(all);
        final int t = RandomHelper.getRandomint();
        obj.setBackgroundColor(t);
        assertThat(obj.getBackgroundColor()).isEqualTo(t);
    }

}
