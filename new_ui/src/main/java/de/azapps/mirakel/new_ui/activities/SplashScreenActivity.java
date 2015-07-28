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
package de.azapps.mirakel.new_ui.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;

public class SplashScreenActivity extends Activity {

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create a meta list if not existent
        SpecialList.firstSpecialSafe();
        final ListMirakel listId = MirakelModelPreferences.getStartupList();
        final Intent intent = new Intent(SplashScreenActivity.this,
                                         MirakelActivity.class);
        intent.setAction(DefinitionsHelper.SHOW_LIST);
        intent.putExtra(DefinitionsHelper.EXTRA_LIST, listId);
        finish();
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent data) {
        finish();
    }
}
