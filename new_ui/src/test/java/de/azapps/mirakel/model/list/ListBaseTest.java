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

import android.net.Uri;

import com.google.common.base.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.List;

import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.MultiApiRobolectricTestRunner;
import de.azapps.mirakelandroid.test.RandomHelper;

import static com.google.common.truth.Truth.assertThat;
import static de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import static de.azapps.mirakel.model.list.ListMirakel.SORT_BY;


@RunWith(MultiApiRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class ListBaseTest extends MirakelDatabaseTestCase {



    // Test for getting and setting SortBy
    @Test
    public void testSortBy3() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final SORT_BY t = RandomHelper.getRandomSORT_BY();
        obj.setSortBy(t);
        assertThat(obj.getSortBy()).isEqualTo(t);
    }

    // Test for getting and setting Lft
    @Test
    public void testLft4() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final int t = RandomHelper.getRandomint();
        obj.setLft(t);
        assertThat(obj.getLft()).isEqualTo(t);
    }

    // Test for getting and setting Rgt
    @Test
    public void testRgt5() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final int t = RandomHelper.getRandomint();
        obj.setRgt(t);
        assertThat(obj.getRgt()).isEqualTo(t);
    }

    // Test for getting and setting Color
    @Test
    public void testColor6() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final int t = RandomHelper.getRandomint();
        obj.setColor(t);
        assertThat(obj.getColor()).isEqualTo(t);
    }

    // Test for getting and setting IconPath
    @Test
    public void testIconPath7() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final Optional<Uri> t = RandomHelper.getRandomOptional_Uri();
        obj.setIconPath(t);
        assertThat(obj.getIconPath()).isEqualTo(t);
    }

    // Test for getting and setting Account
    @Test
    public void testAccount8() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final AccountMirakel t = RandomHelper.getRandomAccountMirakel();
        obj.setAccount(t);
        assertThat(obj.getAccount()).isEqualTo(t);
    }

    // Test for getting and setting SyncState
    @Test
    public void testSyncState9() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final SYNC_STATE t = RandomHelper.getRandomSYNC_STATE();
        obj.setSyncState(t);
        assertThat(obj.getSyncState()).isEqualTo(t);
    }

}
