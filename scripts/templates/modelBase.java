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

import de.azapps.mirakelandroid.test.MirakelTestCase;
import de.azapps.mirakelandroid.test.RandomHelper;
import de.azapps.mirakelandroid.test.TestHelper;

import junit.framework.TestCase;
import android.test.suitebuilder.annotation.SmallTest;

import de.azapps.mirakel.DefinitionsHelper;
#foreach($IMPORT in $IMPORTS)
import $IMPORT;
#end

import static de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import static de.azapps.mirakel.model.list.ListMirakel.SORT_BY;
import static de.azapps.mirakel.model.account.AccountMirakel.ACCOUNT_TYPES;

import static org.junit.Assert.assertEquals;


@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class ${TESTCLASS}Test{

    @Before
    public void setUp() throws Exception {
        TestHelper.init(Robolectric.application);
        RandomHelper.init(Robolectric.application);
    }

#foreach($SETTER in $SETTERS)
    // Test for getting and setting ${SETTER.name}
    @Test
    public void test${SETTER.name}${foreach.count}() {
    final List<${MODELNAME}> all= ${MODELNAME}.all();
    final ${MODELNAME} obj = RandomHelper.getRandomElem(all);
    final ${SETTER.type.type} t = ${SETTER.randomFunction};
    obj.set${SETTER.name}(t);
    assertEquals("Getting and setting ${SETTER.name} does not match",t,obj.${SETTER.getterFunction});
    }
#end

}
