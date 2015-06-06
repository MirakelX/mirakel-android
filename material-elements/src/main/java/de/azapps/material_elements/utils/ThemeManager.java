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


import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import de.azapps.material_elements.R;


public class ThemeManager {
    private static Context context;
    private static int themeResId;
    private static int dialogTheme;

    private static final float SHADE_FACTOR = 0.8F;

    public static void init(final @NonNull Context ctx,final int theme,final int dialogTheme) {
        context = ctx;
        themeResId = theme;
        ThemeManager.dialogTheme = dialogTheme;

    }

    public static void setTheme(final @NonNull Activity activity) {
        activity.setTheme(themeResId);
    }

    public static int getPrimaryThemeColor() {
        return getColor(R.attr.colorPrimary);
    }

    public static int getPrimaryDarkThemeColor() {
        return getColor(R.attr.colorPrimaryDark);
    }

    public static int getAccentThemeColor() {
        return getColor(R.attr.colorAccent);
    }

    public static int getColor(int attrId) {
        // The attributes you want retrieved
        final int[] attrs = {attrId};

        // Parse MyCustomStyle, using Context.obtainStyledAttributes()
        final TypedArray ta = context.obtainStyledAttributes(themeResId, attrs);

        // Fetching the colors defined in your style
        final int color = ta.getColor(0, Color.BLACK);

        // OH, and don't forget to recycle the TypedArray
        ta.recycle();
        return color;
    }

    public static int getThemeID() {
        return themeResId;
    }

    public static int getDialogTheme() {
        return dialogTheme;
    }

    public static int getDarkerShade(final int color) {
        return Color.rgb((int) (SHADE_FACTOR * Color.red(color)),
                (int) (SHADE_FACTOR * Color.green(color)),
                (int) (SHADE_FACTOR * Color.blue(color)));
    }

    public static Drawable getColoredIcon(final int drawable, final int color) {
        final Drawable icon = context.getResources().getDrawable(drawable);
        icon.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        return icon;
    }


}
