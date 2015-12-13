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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ContextThemeWrapper;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;

import butterknife.InjectView;
import de.azapps.material_elements.utils.IconizedMenu;
import de.azapps.material_elements.utils.MenuHelper;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.mirakelandroid.R;

public class PriorityChangeView extends PriorityView implements View.OnClickListener {
    @InjectView(R.id.priority_button)
    View priorityButton;
    @Nullable
    private OnPriorityChangeListener onPriorityChangeListener;


    public interface OnPriorityChangeListener {
        public void priorityChanged(int priority);
    }

    public PriorityChangeView(Context context) {
        this(context, null);
    }

    public PriorityChangeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PriorityChangeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnClickListener(this);
    }

    protected int getDrawableId(int priority) {
        int drawableID;
        switch (priority) {
        case -1:
        case -2:
            drawableID = R.drawable.ic_priority_low_20dp;
            break;
        case 1:
            drawableID = R.drawable.ic_priority_high_20dp;
            break;
        case 2:
            drawableID = R.drawable.ic_priority_veryhigh_20dp;
            break;
        case 0:
        default:
            drawableID = R.drawable.ic_priority_none_20dp;
            break;

        }
        return drawableID;
    }

    protected int getLayout() {
        return R.layout.view_priority_change;
    }

    public void setOnPriorityChangeListener(@NonNull final OnPriorityChangeListener
                                            onPriorityChangeListener) {
        this.onPriorityChangeListener = onPriorityChangeListener;
        rebuildLayout();
    }

    @Override
    public void onClick(View view) {
        IconizedMenu priorityPopup = new IconizedMenu(new ContextThemeWrapper(getContext(),
                R.style.MirakelBaseTheme), priorityButton);
        priorityPopup.inflate(R.menu.priority_menu);
        priorityPopup.setOnMenuItemClickListener(new IconizedMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final int priority;
                switch (item.getItemId()) {
                case R.id.priority_2:
                    priority = 2;
                    break;
                case R.id.priority_1:
                    priority = 1;
                    break;
                case R.id.priority_0:
                    priority = 0;
                    break;
                case R.id.priority_m1:
                    priority = -1;
                    break;
                default:
                    throw new IllegalArgumentException("Can not handle this priority");
                }
                setPriority(priority);
                if (onPriorityChangeListener != null) {
                    onPriorityChangeListener.priorityChanged(priority);
                }
                return true;
            }
        });
        MenuHelper.colorizeMenuItems(priorityPopup.getMenu(), ThemeManager.getColor(R.attr.colorTextGrey));
        priorityPopup.show();
    }

    protected boolean shouldHideText() {
        return false;
    }

}
