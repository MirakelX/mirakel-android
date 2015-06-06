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

package com.mobeta.android.dslv;

import android.content.Context;
import android.view.View;
import android.widget.Checkable;

/**
 * Lightweight ViewGroup that wraps list items obtained from user's ListAdapter.
 * ItemView expects a single child that has a definite height (i.e. the child's
 * layout height is not MATCH_PARENT). The width of ItemView will always match
 * the width of its child (that is, the width MeasureSpec given to ItemView is
 * passed directly to the child, and the ItemView measured width is set to the
 * child's measured width). The height of ItemView can be anything; the
 *
 *
 * The purpose of this class is to optimize slide shuffle animations.
 */
public class DragSortItemViewCheckable extends DragSortItemView implements
    Checkable {

    public DragSortItemViewCheckable(final Context context) {
        super(context);
    }

    @Override
    public boolean isChecked() {
        final View child = getChildAt(0);
        if (child instanceof Checkable) {
            return ((Checkable) child).isChecked();
        } else {
            return false;
        }
    }

    @Override
    public void setChecked(final boolean checked) {
        final View child = getChildAt(0);
        if (child instanceof Checkable) {
            ((Checkable) child).setChecked(checked);
        }
    }

    @Override
    public void toggle() {
        final View child = getChildAt(0);
        if (child instanceof Checkable) {
            ((Checkable) child).toggle();
        }
    }
}
