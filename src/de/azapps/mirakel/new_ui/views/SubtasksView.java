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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.R;
import de.azapps.mirakel.new_ui.interfaces.OnTaskSelectedListener;
import de.azapps.tools.OptionalUtils;

public class SubtasksView extends LinearLayout {
    LinearLayout subtasksWrapper;

    private LayoutInflater layoutInflater;
    public SubtasksView(Context context) {
        this(context, null);
    }

    public SubtasksView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SubtasksView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_subtasks, this);
        layoutInflater = LayoutInflater.from(context);
        subtasksWrapper = (LinearLayout) findViewById(R.id.task_subtasks_wrapper);
    }

    public void setSubtasks(List<Task> subtasks, OnClickListener onSubtaskAddListener,
                            final OnTaskSelectedListener onSubtaskClickListener,
                            final OptionalUtils.Procedure<Task> onSubtaskDoneListener) {
        subtasksWrapper.removeAllViews();
        for (final Task subtask : subtasks) {
            LinearLayout v = (LinearLayout) layoutInflater.inflate(R.layout.row_subtask, subtasksWrapper,
                             false);
            CheckBox subtask_done = (CheckBox) v.findViewById(R.id.subtask_done);
            TextView subtask_name = (TextView) v.findViewById(R.id.subtask_name);
            subtask_done.setChecked(subtask.isDone());
            subtask_name.setText(subtask.getName());
            subtask_done.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    onSubtaskDoneListener.apply(subtask);
                }
            });
            subtask_name.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    onSubtaskClickListener.onTaskSelected(subtask);
                }
            });
            subtasksWrapper.addView(v);
        }
        findViewById(R.id.task_subtasks_add).setOnClickListener(onSubtaskAddListener);
    }
}
