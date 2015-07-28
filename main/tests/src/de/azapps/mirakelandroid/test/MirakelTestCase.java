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

package de.azapps.mirakelandroid.test;

import android.content.ContentResolver;

import org.junit.After;
import org.junit.Before;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

import de.azapps.mirakel.model.MirakelInternalContentProvider;

public class MirakelTestCase{

    private MirakelInternalContentProvider mProvider;
    private ContentResolver mContentResolver;
    //private ShadowContentResolver mShadowContentResolver;

    @Before
    public void setUp() throws Exception {
        mProvider = new MirakelInternalContentProvider();
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        //mShadowContentResolver = Shadow.shadowOf(mContentResolver);
        ShadowContentResolver.registerProvider(
            "de.azapps.mirakel.provider.internal",    //authority of your provider
            mProvider  //instance of your ContentProvider (you can just use default constructor)
        );
        mProvider.onCreate();
        TestHelper.init(RuntimeEnvironment.application);
        RandomHelper.init(RuntimeEnvironment.application);
    }

    @After
    public final void tearDown() {
        TestHelper.terminate();
    }

}
