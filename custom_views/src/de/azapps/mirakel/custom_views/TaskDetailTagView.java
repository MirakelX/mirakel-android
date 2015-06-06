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
package de.azapps.mirakel.custom_views;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import de.azapps.mirakel.adapter.TagDialog;
import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class TaskDetailTagView extends BaseTaskDetailRow {

    public interface NeedFragmentManager {
        abstract public FragmentManager getFragmentManager();
    }

    protected static final String TAG = "TaskDetailTagView";
    private final LinearLayout tagList;
    private final ImageButton editContent;
    private NeedFragmentManager fragmentManager;

    public TaskDetailTagView(final Context ctx) {
        super(ctx);
        inflate(ctx, R.layout.task_tags, this);
        this.tagList = (LinearLayout) findViewById(R.id.tag_list);
        this.editContent = (ImageButton) findViewById(R.id.add_tags);
        this.fragmentManager = null;
        this.editContent.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                TaskDetailTagView.this.task.getTags();
                if (TaskDetailTagView.this.fragmentManager != null
                    && TaskDetailTagView.this.fragmentManager
                    .getFragmentManager() != null) {
                    TagDialog.newDialog(TaskDetailTagView.this.context,
                                        TaskDetailTagView.this.task,
                    new OnTaskChangedListner() {
                        @Override
                        public void onTaskChanged(final Task newTask) {
                            save();
                            TaskDetailTagView.this.tagList.invalidate();
                            updateView();
                        }
                    }).show(
                        TaskDetailTagView.this.fragmentManager
                        .getFragmentManager(), "dialog");
                } else {
                    Log.wtf(TAG, "cannot create dialog");
                    Log.w(TAG, "fragmentmanager is null "
                          + (TaskDetailTagView.this.fragmentManager == null));
                    if (TaskDetailTagView.this.fragmentManager != null) {
                        Log.v(TAG,
                              "return empty fragmentmanager "
                              + (TaskDetailTagView.this.fragmentManager
                                 .getFragmentManager() == null));
                    }
                }
            }
        });
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw,
                                 final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateView();
    }

    @Override
    protected void updateView() {
        if (this.task == null) {
            return;
        }
        TagListView tagView;
        if (this.tagList.getChildCount() < 1) {
            tagView = new TagListView(this.context);
            tagView.setLayoutParams(new LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            tagView.init(this.task);
            this.tagList.addView(tagView);
        } else {
            tagView = (TagListView) this.tagList.getChildAt(0);
            tagView.init(this.task);
        }
    }

    public void setNeedFragmentManager(final NeedFragmentManager fm) {
        this.fragmentManager = fm;
    }

}
