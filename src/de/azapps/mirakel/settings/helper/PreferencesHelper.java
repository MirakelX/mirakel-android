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

package de.azapps.mirakel.settings.helper;

import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class PreferencesHelper {

    @Nullable
    public static Integer findPreferenceScreenForPreference( @NonNull final String key,
            final @NonNull PreferenceScreen screen ) {
        final android.widget.Adapter ada = screen.getRootAdapter();
        for ( int i = 0; i < ada.getCount(); i++ ) {
            final String prefKey = ((Preference)ada.getItem(i)).getKey();
            if (key != null && key.equals( prefKey ) ) {
                return i;
            }
            if ( ada.getItem(i) instanceof PreferenceScreen ) {
                final Integer result = findPreferenceScreenForPreference(key, (PreferenceScreen) ada.getItem(i));
                if ( result != null ) {
                    return result;
                }
            }
        }
        return null;
    }
}
