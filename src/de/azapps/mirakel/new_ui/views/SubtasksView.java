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
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class SubtasksView extends LinearLayout {

    private final LayoutInflater layoutInflater;
    @InjectView(R.id.task_subtasks_wrapper)
    LinearLayout subtasksWrapper;
    @InjectView(R.id.view_switcher)
    ViewSwitcher viewSwitcher;
    @InjectView(R.id.task_name_edit)
    EditText taskNameEdit;
    @Nullable
    private SubtaskListener subtaskListener;

    public interface SubtaskListener {
        void onAddSubtask(String taskName);
        void onSubtaskClick(Task subtask);
        void onSubtaskDone(Task subtask);
    }

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
    public void initListeners(final SubtaskListener subtaskAddListener) {
        this.subtaskListener = subtaskAddListener;
    }

    public void setSubtasks(List<Task> subtasks) {
        subtasksWrapper.removeAllViews();
        for (final Task subtask : subtasks) {
            final LinearLayout layout = (LinearLayout) layoutInflater.inflate(R.layout.row_subtask,
                                        subtasksWrapper,
                                        false);
            CheckBox subtaskDone = (CheckBox) layout.findViewById(R.id.subtask_done);
            subtaskDone.setChecked(subtask.isDone());

            TextView subtaskName = (TextView) layout.findViewById(R.id.subtask_name);
            subtaskName.setText(subtask.getName());
            if (subtaskListener != null) {
                subtaskDone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
                        subtaskListener.onSubtaskDone(subtask);
                    }
                });

                subtaskName.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        subtaskListener.onSubtaskClick(subtask);
                    }
                });
            }

            if (subtask.isDone()) {
                subtaskName.setPaintFlags(subtaskName.getPaintFlags() |
                                          Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                subtaskName.setPaintFlags(subtaskName.getPaintFlags() &
                                          ~Paint.STRIKE_THRU_TEXT_FLAG);
            }
            subtasksWrapper.addView(layout);
        }

        // Set up taskNameEdit
        taskNameEdit.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    onCreateSubTask();
                    return true;
                }
                return false;
            }
        });
    }



    @OnClick(R.id.task_subtasks_add)
    void onClick() {
        viewSwitcher.setDisplayedChild(1);
        taskNameEdit.requestFocus();
    }

    void onCreateSubTask() {
        taskNameEdit.clearFocus();
        viewSwitcher.setDisplayedChild(0);
        String taskName = taskNameEdit.getText().toString().trim();
        taskNameEdit.setText("");
        if (!taskName.isEmpty() && subtaskListener != null) {
            subtaskListener.onAddSubtask(taskName);
        }
    }

    @OnEditorAction(R.id.task_name_edit)
    boolean onEditorAction(int actionId) {
        switch (actionId) {
        case EditorInfo.IME_ACTION_DONE:
        case EditorInfo.IME_ACTION_SEND:
            onCreateSubTask();
            return true;
        }
        return false;
    }

}
