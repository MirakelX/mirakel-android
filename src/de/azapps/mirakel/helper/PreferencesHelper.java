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

package de.azapps.mirakel.helper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class PreferencesHelper {
    protected final Activity activity;
    protected final Object ctx;
    protected boolean v4_0;

    public PreferencesHelper(final PreferenceActivity c) {
        this.ctx = c;
        this.v4_0 = false;
        this.activity = c;
    }

    @SuppressLint("NewApi")
    public PreferencesHelper(final PreferenceFragment c) {
        this.ctx = c;
        this.v4_0 = true;
        this.activity = c.getActivity();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    protected Preference findPreference(final String key) {
        if (this.v4_0) {
            return ((PreferenceFragment) this.ctx).findPreference(key);
        }
        return ((PreferenceActivity) this.ctx).findPreference(key);
    }

}
