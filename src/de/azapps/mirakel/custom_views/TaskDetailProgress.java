/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.custom_views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.model.task.Task;

public class TaskDetailProgress extends
    TaskDetailSubtitleView<Integer, TaskDetailProgressBar> {

    private TaskDetailProgressBar progressBar;

    public TaskDetailProgress(final Context ctx) {
        super(ctx);
        this.title.setText(R.string.task_fragment_progress);
        this.audioButton.setVisibility(GONE);
        this.cameraButton.setVisibility(GONE);
        this.button.setVisibility(GONE);
    }

    @Override
    TaskDetailProgressBar newElement() {
        this.progressBar = new TaskDetailProgressBar(this.context);
        this.progressBar.setOnTaskChangedListner(new OnTaskChangedListner() {
            @Override
            public void onTaskChanged(final Task newTask) {
                TaskDetailProgress.this.task = newTask;
                save();
            }
        });
        this.progressBar.setTask(this.task);
        this.progressBar.update(this.task);
        return this.progressBar;
    }

    @Override
    protected void updateView() {
        final List<Integer> l = new ArrayList<Integer>();
        l.add(this.task.getProgress());
        if (this.progressBar != null) {
            this.progressBar.setTask(this.task);
        }
        updateSubviews(l);
    }

}
