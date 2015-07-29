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
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import de.azapps.mirakel.model.account.AccountMirakel;
import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.RandomHelper;

import static de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import static de.azapps.mirakel.model.list.ListMirakel.SORT_BY;
import static org.junit.Assert.assertEquals;


@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class ListBaseTest extends MirakelDatabaseTestCase {



    // Test for getting and setting CreatedAt
    @Test
    public void testCreatedAt1() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final String t = RandomHelper.getRandomString();
        obj.setCreatedAt(t);
        assertEquals("Getting and setting CreatedAt does not match", t, obj.getCreatedAt());
    }

    // Test for getting and setting UpdatedAt
    @Test
    public void testUpdatedAt2() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final String t = RandomHelper.getRandomString();
        obj.setUpdatedAt(t);
        assertEquals("Getting and setting UpdatedAt does not match", t, obj.getUpdatedAt());
    }

    // Test for getting and setting SortBy
    @Test
    public void testSortBy3() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final SORT_BY t = RandomHelper.getRandomSORT_BY();
        obj.setSortBy(t);
        assertEquals("Getting and setting SortBy does not match", t, obj.getSortBy());
    }

    // Test for getting and setting Lft
    @Test
    public void testLft4() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final int t = RandomHelper.getRandomint();
        obj.setLft(t);
        assertEquals("Getting and setting Lft does not match", t, obj.getLft());
    }

    // Test for getting and setting Rgt
    @Test
    public void testRgt5() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final int t = RandomHelper.getRandomint();
        obj.setRgt(t);
        assertEquals("Getting and setting Rgt does not match", t, obj.getRgt());
    }

    // Test for getting and setting Color
    @Test
    public void testColor6() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final int t = RandomHelper.getRandomint();
        obj.setColor(t);
        assertEquals("Getting and setting Color does not match", t, obj.getColor());
    }

    // Test for getting and setting IconPath
    @Test
    public void testIconPath7() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final Optional<Uri> t = RandomHelper.getRandomOptional_Uri();
        obj.setIconPath(t);
        assertEquals("Getting and setting IconPath does not match", t, obj.getIconPath());
    }

    // Test for getting and setting Account
    @Test
    public void testAccount8() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final AccountMirakel t = RandomHelper.getRandomAccountMirakel();
        obj.setAccount(t);
        assertEquals("Getting and setting Account does not match", t, obj.getAccount());
    }

    // Test for getting and setting SyncState
    @Test
    public void testSyncState9() {
        final List<ListMirakel> all = ListMirakel.all();
        final ListMirakel obj = RandomHelper.getRandomElem(all);
        final SYNC_STATE t = RandomHelper.getRandomSYNC_STATE();
        obj.setSyncState(t);
        assertEquals("Getting and setting SyncState does not match", t, obj.getSyncState());
    }

}
