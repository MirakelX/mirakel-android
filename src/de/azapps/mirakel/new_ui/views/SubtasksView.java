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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.mirakelandroid.R;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.OptionalUtils;

public class SubtasksView extends LinearLayout {

    private final LayoutInflater layoutInflater;
    @InjectView(R.id.task_subtasks_wrapper)
    LinearLayout subtasksWrapper;
    @InjectView(R.id.subtask_done)
    CheckBox subtaskDone;
    @InjectView(R.id.subtask_name)
    TextView subtaskName;
    @InjectView(R.id.task_subtasks_add)
    Button addSubtask;

    public SubtasksView(final Context context) {
        this(context, null);
    }

    public SubtasksView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SubtasksView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.view_subtasks, this);
        layoutInflater = LayoutInflater.from(context);
        ButterKnife.inject(this, this);
    }

    public void setSubtasks(List<Task> subtasks, OnClickListener onSubtaskAddListener,
                            final OnItemClickedListener<Task> onSubtaskClickListener,
                            final OptionalUtils.Procedure<Task> onSubtaskDoneListener) {
        subtasksWrapper.removeAllViews();
        for (final Task subtask : subtasks) {
            final LinearLayout layout = (LinearLayout) layoutInflater.inflate(R.layout.row_subtask,
                                        subtasksWrapper,
                                        false);
            subtaskDone.setChecked(subtask.isDone());
            subtaskName.setText(subtask.getName());
            subtaskDone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
                    onSubtaskDoneListener.apply(subtask);
                }
            });
            subtaskName.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View view) {
                    onSubtaskClickListener.onItemSelected(subtask);
                }
            });
            subtasksWrapper.addView(layout);
        }
        addSubtask.setOnClickListener(onSubtaskAddListener);
    }
}
