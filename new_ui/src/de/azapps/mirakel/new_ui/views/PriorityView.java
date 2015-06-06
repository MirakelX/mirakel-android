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
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.mirakelandroid.R;

public class PriorityView extends LinearLayout {
    @InjectView(R.id.priority_text)
    TextView priorityText;
    private int priority;
    private String[] priorities = {"-2", "-1", "∅", "!", "!!"};

    public PriorityView(Context context) {
        this(context, null);
    }

    public PriorityView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PriorityView(Context context, AttributeSet attrs, int defStyleAttr) {
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

    public void setPriority(int priority) {
        this.priority = priority;
        rebuildLayout();
    }

    protected void rebuildLayout() {
        if (shouldHideText()) {
            priorityText.setVisibility(GONE);
        } else {
            priorityText.setVisibility(VISIBLE);
            priorityText.setText(priorities[priority + 2]);
        }
    }
    protected boolean shouldHideText() {
        if (priority == 0) {
            return true;
        }
        return false;
    }
}
