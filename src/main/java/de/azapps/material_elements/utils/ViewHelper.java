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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

public class ViewHelper {

    public static int dpToPx(final Context ctx, final int dp) {
        return (int) dpToPx(ctx, (float) dp);
    }

    public static float dpToPx(final Context ctx, final float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                                         ctx.getResources().getDisplayMetrics());
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isRTL(final Context ctx) {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) &&
               (ctx.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
    }

    public static void setCompoundDrawable(final Context context, final TextView title,
                                           final Drawable icon) {
        if (ViewHelper.isRTL(context)) {
            title.setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null);
        } else {
            title.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }
    }
}
