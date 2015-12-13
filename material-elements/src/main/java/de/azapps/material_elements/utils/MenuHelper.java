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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v7.view.menu.MenuBuilder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

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

    public static void colorizeMenuItems(final Menu menu, final int color) {
        colorizeMenuItems(menu, color, 0);
    }

    public static void colorizeMenuItems(final Menu menu, final int color, final int startIndex) {
        colorizeMenuItems(menu, color, startIndex, menu.size());
    }

    public static void colorizeMenuItems(final Menu menu, final int color, final int startIndex,
                                         final int endIndex) {
        for (int i = startIndex; (i < menu.size()) && (i < endIndex); i++) {
            final MenuItem menuItem = menu.getItem(i);
            final Drawable icon = menuItem.getIcon();
            if (icon != null) {
                icon.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
            }
        }
    }

    public static void setTextColor(final Menu menu, final int color) {
        for (int i = 0; i < menu.size(); i++) {
            final MenuItem mi = menu.getItem(i);
            final String title = mi.getTitle().toString();
            final Spannable newTitle = new SpannableString(title);
            newTitle.setSpan(new ForegroundColorSpan(color), 0, newTitle.length(),
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mi.setTitle(newTitle);
        }
    }
}
