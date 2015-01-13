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
import android.view.View;
import de.azapps.mirakel.custom_views.TaskSummary.OnTaskClickListener;
import de.azapps.mirakel.custom_views.TaskSummary.OnTaskMarkedListener;
import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.model.task.Task;

public class TaskDetailSubtask extends
    TaskDetailSubtitleView<Task, TaskSummary> implements
    OnTaskClickListener, OnTaskMarkedListener {

    protected static final String TAG = "TaskDetailSubtask";
    private int markCounter;
    private OnTaskClickListener onClick;
    private OnTaskMarkedListener onMarked;

    public TaskDetailSubtask(final Context ctx) {
        super(ctx);
        this.title.setText(R.string.add_subtasks);
        this.button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                TaskDialogHelpers.handleSubtask(TaskDetailSubtask.this.context,
                                                TaskDetailSubtask.this.task,
                new OnTaskChangedListner() {
                    @Override
                    public void onTaskChanged(final Task newTask) {
                        update(newTask);
                        if (TaskDetailSubtask.this.taskChangedListner != null) {
                            TaskDetailSubtask.this.taskChangedListner
                            .onTaskChanged(newTask);
                        }
                    }
                }, false);
            }
        });
        this.cameraButton.setVisibility(GONE);
        this.audioButton.setVisibility(GONE);
        this.button.setImageDrawable(this.context.getResources().getDrawable(
                                         android.R.drawable.ic_menu_add));
    }

    @Override
    public void markTask(final View v, final Task t, final boolean marked) {
        if (this.onMarked != null) {
            markTaskHelper(marked);
            this.onMarked.markTask(v, t, marked);
        }
    }

    private void markTaskHelper(final boolean markted) {
        this.markCounter += markted ? 1 : -1;
        for (final TaskSummary v : this.viewList) {
            v.setShortMark(this.markCounter > 0);
        }
    }

    @Override
    TaskSummary newElement() {
        final TaskSummary t = new TaskSummary(this.context);
        t.setOnTaskClick(this);
        t.setOnTaskMarked(this);
        t.setOnTaskChangedListner(new OnTaskChangedListner() {
            @Override
            public void onTaskChanged(final Task task) {
                if (task != null) {
                    task.save();
                    if (TaskDetailSubtask.this.taskChangedListner != null) {
                        TaskDetailSubtask.this.taskChangedListner
                        .onTaskChanged(task);
                    }
                }
            }
        });
        return t;
    }

    @Override
    public void onTaskClick(final Task t) {
        if (this.onClick != null) {
            this.onClick.onTaskClick(t);
        }
    }

    public void setOnClick(final OnTaskClickListener l) {
        this.onClick = l;
    }

    public void setOnTaskMarked(final OnTaskMarkedListener l) {
        this.onMarked = l;
    }

    @Override
    protected void updateView() {
        updateSubviews(this.task.getSubtasks());
    }

}
