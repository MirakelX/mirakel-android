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

package de.azapps.material_elements.utils;

import android.content.Context;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.util.Log;
import android.view.Menu;

import java.lang.reflect.Method;

public class MenuHelper {
    private final static String TAG = "MenuHelper";

    /**
     * Source: http://stackoverflow.com/questions/18374183/how-to-show-icons-in-overflow-menu-in-actionbar
     */
    public static void showMenuIcons(final Context context, final Menu menu) {
        // It seams that the setOptionalIconsVisible code is very old and does not support RTL layouts.
        // Thus we hide the icons for the few RTL users instead of showing them in an ugly way
        if (!ViewHelper.isRTL(context) && menu instanceof MenuBuilder) {
            try {
                final Method m = menu.getClass().getDeclaredMethod(
                                     "setOptionalIconsVisible", Boolean.TYPE);
                m.setAccessible(true);
                m.invoke(menu, true);
            } catch (final NoSuchMethodException e) {
                Log.e(TAG, "onMenuOpened", e);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
