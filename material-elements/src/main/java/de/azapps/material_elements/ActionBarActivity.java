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
package de.azapps.material_elements;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import java.util.Locale;

import de.azapps.material_elements.utils.ThemeManager;

public abstract class ActionBarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        ThemeManager.setTheme(this);
        Locale.setDefault(getLocale());
        super.onCreate(savedInstanceState);
    }

    protected abstract Locale getLocale();

    @Override
    public void setSupportActionBar(final Toolbar toolbar) {
        try {
            super.setSupportActionBar(toolbar);
        } catch (final Throwable ignored) {
            // SEE https://code.google.com/p/android/issues/detail?id=78377
            // WTF SAMSUNG!
        }
    }
}
