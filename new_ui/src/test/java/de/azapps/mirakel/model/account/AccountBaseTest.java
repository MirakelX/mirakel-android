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

import com.google.common.base.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.List;

import de.azapps.mirakelandroid.BuildConfig;
import de.azapps.mirakelandroid.test.MirakelDatabaseTestCase;
import de.azapps.mirakelandroid.test.MultiApiRobolectricTestRunner;
import de.azapps.mirakelandroid.test.RandomHelper;

import static com.google.common.truth.Truth.assertThat;
import static de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;


@RunWith(MultiApiRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class AccountBaseTest extends MirakelDatabaseTestCase {



    // Test for getting and setting Type
    @Test
    public void testType1() {
        final List<AccountMirakel> all = AccountMirakel.all();
        final AccountMirakel obj = RandomHelper.getRandomElem(all);
        final ACCOUNT_TYPES t = RandomHelper.getRandomACCOUNT_TYPES();
        obj.setType(t);
        assertThat(obj.getType()).isEqualTo(t);
    }

    // Test for getting and setting Enabled
    @Test
    public void testEnabled2() {
        final List<AccountMirakel> all = AccountMirakel.all();
        final AccountMirakel obj = RandomHelper.getRandomElem(all);
        final boolean t = RandomHelper.getRandomboolean();
        obj.setEnabled(t);
        assertThat(obj.isEnabled()).isEqualTo(t);
    }

    // Test for getting and setting SyncKey
    @Test
    public void testSyncKey3() {
        final List<AccountMirakel> all = AccountMirakel.all();
        final AccountMirakel obj = RandomHelper.getRandomElem(all);
        final Optional<String> t = RandomHelper.getRandomOptional_String();
        obj.setSyncKey(t);
        assertThat(obj.getSyncKey()).isEqualTo(t);
    }

}
