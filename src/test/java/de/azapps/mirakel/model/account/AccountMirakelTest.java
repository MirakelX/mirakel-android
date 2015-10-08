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
package de.azapps.mirakel.model.account;


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
public class AccountMirakelTest extends MirakelDatabaseTestCase {
    private static SQLiteDatabase database;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        database = DatabaseHelper.getDatabaseHelper(RuntimeEnvironment.application).getWritableDatabase();
        // Create at least one item to have something to test with

        AccountMirakel.newAccount(RandomHelper.getRandomString(), RandomHelper.getRandomACCOUNT_TYPES(),
                                  RandomHelper.getRandomboolean());
    }

    private static int countElems() {
        final Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM account", null);
        cursor.moveToFirst();
        return cursor.getInt(0);
    }


    @Test
    public void testNewCountNewAccount1() {
        final int countBefore = countElems();
        AccountMirakel.newAccount(RandomHelper.getRandomString(), RandomHelper.getRandomACCOUNT_TYPES(),
                                  RandomHelper.getRandomboolean());
        final int countAfter = countElems();
        assertThat(countBefore + 1).isEqualTo( countAfter);
    }

    @Test
    public void testNewInsertedNewAccount1() {
        final List<AccountMirakel>elems = AccountMirakel.all();
        final AccountMirakel elem = AccountMirakel.newAccount(RandomHelper.getRandomString(),
                                    RandomHelper.getRandomACCOUNT_TYPES(), RandomHelper.getRandomboolean());
        elems.add(elem);
        final List<AccountMirakel>newElems = AccountMirakel.all();
        assertThat(newElems).containsExactlyElementsIn(newElems);
    }

    @Test
    public void testNewEqualsNewAccount1() {
        final AccountMirakel elem = AccountMirakel.newAccount(RandomHelper.getRandomString(),
                                    RandomHelper.getRandomACCOUNT_TYPES(), RandomHelper.getRandomboolean());
        assertThat(elem).isNotNull();
        final Optional<AccountMirakel> newElem = AccountMirakel.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    // If nothing was changed the database should not be updated
    @Test
    public void testUpdateEqual() {
        final List<AccountMirakel>elems = AccountMirakel.all();
        final int randomItem = new Random().nextInt(elems.size());
        final AccountMirakel elem = elems.get(randomItem);
        elem.save();
        final List<AccountMirakel>newElems = AccountMirakel.all();
        assertThat(newElems).containsExactlyElementsIn(elems);
    }


    @Test
    public void testSetType1() {
        final List<AccountMirakel>elems = AccountMirakel.all();
        final int randomItem = new Random().nextInt(elems.size());
        final AccountMirakel elem = elems.get(randomItem);
        elem.setType(RandomHelper.getRandomACCOUNT_TYPES());
        elem.save();
        final Optional<AccountMirakel> newElem = AccountMirakel.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetEnabled2() {
        final List<AccountMirakel>elems = AccountMirakel.all();
        final int randomItem = new Random().nextInt(elems.size());
        final AccountMirakel elem = elems.get(randomItem);
        elem.setEnabled(RandomHelper.getRandomboolean());
        elem.save();
        final Optional<AccountMirakel> newElem = AccountMirakel.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testSetSyncKey3() {
        final List<AccountMirakel>elems = AccountMirakel.all();
        final int randomItem = new Random().nextInt(elems.size());
        final AccountMirakel elem = elems.get(randomItem);
        elem.setSyncKey(RandomHelper.getRandomOptional_String());
        elem.save();
        final Optional<AccountMirakel> newElem = AccountMirakel.get(elem.getId());
        assertThat(newElem).hasValue(elem);
    }

    @Test
    public void testDestroy() {
        final List<AccountMirakel>elems = AccountMirakel.all();
        if (elems.size() < 2) {
            fail("Only local account, cannot run this test");
        }
        AccountMirakel elem;
        do {
            elem = elems.get(RandomHelper.getRandomint(elems.size()));
        } while (elem.equals(AccountMirakel.getLocal()));
        final long id = elem.getId();
        elem.destroy();
        assertThat(AccountMirakel.get(id)).isAbsent();
        final List<AccountMirakel> newElems = AccountMirakel.all();
        elems.remove(elem);
        assertThat(newElems).containsExactlyElementsIn(elems);
        assertThat(newElems).doesNotContain(elem);
    }

}
