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

package de.azapps.mirakel.new_ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakelandroid.R;

public class PriorityView extends LinearLayout {
    @InjectView(R.id.priority_icon)
    ImageView priorityIcon;
    private int priority;

    public PriorityView(final Context context) {
        this(context, null);
    }

    public PriorityView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PriorityView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, getLayout(), this);
        ButterKnife.inject(this, this);
        rebuildLayout();
    }
    protected int getLayout() {
        return R.layout.view_priority;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(final int priority) {
        this.priority = priority;
        rebuildLayout();
    }

    protected int getDrawableId(int priority) {
        int drawableID;
        switch (priority) {
        case -1:
        case -2:
            drawableID = R.drawable.ic_priority_low_24dp;
            break;
        case 1:
            drawableID = R.drawable.ic_priority_high_24dp;
            break;
        case 2:
            drawableID = R.drawable.ic_priority_veryhigh_24dp;
            break;
        case 0:
        default:
            drawableID = R.drawable.ic_priority_none_24dp;
            break;

        }
        return drawableID;
    }

    protected void rebuildLayout() {
        if (shouldHideText()) {
            priorityIcon.setVisibility(GONE);
        } else {
            priorityIcon.setVisibility(VISIBLE);
            int drawableID = getDrawableId(priority);
            priorityIcon.setImageDrawable(ThemeManager.getColoredIcon(drawableID
                                          , ThemeManager.getColor(R.attr.colorTextWhite)));

        }
    }
    protected boolean shouldHideText() {
        return priority == 0;
    }
}
