/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *  Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.new_ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import de.azapps.mirakel.custom_views.TagListView;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class TagsView extends LinearLayout {

    private LinearLayout tagList;
    private Task task;

    public TagsView(Context context) {
        this(context, null);
    }

    public TagsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TagsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_tags, this);
        tagList = (LinearLayout) findViewById(R.id.task_tags_wrapper);
    }

    private void rebuildLayout() {
        TagListView tagView;
        if (this.tagList.getChildCount() < 1) {
            tagView = new TagListView(getContext());
            tagView.setLayoutParams(new LinearLayout.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            tagView.init(this.task);
            this.tagList.addView(tagView);
        } else {
            tagView = (TagListView) this.tagList.getChildAt(0);
            tagView.init(this.task);
        }
        invalidate();
        requestLayout();
    }

    public void setTask(Task task) {
        this.task = task;
        rebuildLayout();
    }
}
