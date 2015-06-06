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

package de.azapps.material_elements.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.NumberPicker;

import java.lang.reflect.Field;

import de.azapps.material_elements.utils.ThemeManager;


public class MaterialNumberPicker extends NumberPicker {
    private final String TAG = "MaterialNumberPicker";

    public MaterialNumberPicker(final Context context) {
        this(context, null);
    }
    public MaterialNumberPicker(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        try {
            //dirty hack to override the divider
            final Field mSelectionDivider = NumberPicker.class.getDeclaredField("mSelectionDivider");
            mSelectionDivider.setAccessible(true);
            final Drawable divider = (Drawable) mSelectionDivider.get(this);
            divider.setColorFilter(ThemeManager.getPrimaryThemeColor(), PorterDuff.Mode.SRC_IN);
            mSelectionDivider.set(this, divider);
        } catch (final NoSuchFieldException e) {
            Log.wtf(TAG, "mSelectionDivider not found");
        } catch (IllegalAccessException e) {
            Log.wtf(TAG, "mSelectionDivider not accessible");
        }

    }
}
