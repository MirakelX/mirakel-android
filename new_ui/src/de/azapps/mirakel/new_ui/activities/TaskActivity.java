/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.new_ui.activities;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import de.azapps.mirakel.new_ui.R;

/**
 * Created by az on 06.08.14.
 */
public class TaskActivity extends ActionBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);
        getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(
                R.color.colorPrimary)));
    }
}
