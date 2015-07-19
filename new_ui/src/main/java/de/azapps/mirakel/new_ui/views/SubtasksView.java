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
import android.os.Bundle;
import android.os.Parcelable;
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
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.material_elements.utils.ViewHelper;
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
    @InjectView(R.id.task_subtasks_add)
    TextView subtaskAdd;
    @Nullable
    private SubtaskListener subtaskListener;

    public interface SubtaskListener {
        void onAddSubtask(String taskName);
        void onSubtaskClick(Task subtask);
        void onSubtaskDone(Task subtask, boolean done);
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
        ViewHelper.setCompoundDrawable(context, subtaskAdd,
                                       ThemeManager.getColoredIcon(R.drawable.ic_plus_white_24dp,
                                               ThemeManager.getColor(R.attr.colorLightGrey)));
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
                        subtaskListener.onSubtaskDone(subtask, isChecked);
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
    public void handleAddSubtask() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                viewSwitcher.setDisplayedChild(1);
                taskNameEdit.requestFocusFromTouch();
            }
        }, 100L);
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

    private static final String PARENT_STATE = "parent";
    private static final String EDIT_STATE = "edit_state";
    private static final String EDIT_POS = "edit_pos";
    private static final String IS_EDIT = "is_edit";
    private static final String HAS_FOCUS = "has_focus";

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle out = new Bundle();
        out.putParcelable(PARENT_STATE, super.onSaveInstanceState());
        out.putParcelable(EDIT_STATE, taskNameEdit.onSaveInstanceState());
        out.putInt(EDIT_POS, taskNameEdit.getSelectionEnd());
        out.putInt(IS_EDIT, viewSwitcher.getCurrentView().getId());
        out.putBoolean(HAS_FOCUS, taskNameEdit.hasFocus());
        return out;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !(state instanceof Bundle)) {
            return;
        }
        final Bundle saved = (Bundle) state;
        super.onRestoreInstanceState(saved.getParcelable(PARENT_STATE));
        taskNameEdit.onRestoreInstanceState(saved.getParcelable(EDIT_STATE));
        if (viewSwitcher.getCurrentView().getId() != saved.getInt(IS_EDIT)) {
            viewSwitcher.showNext();
        }
        taskNameEdit.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (saved.getInt(IS_EDIT) == taskNameEdit.getId() && saved.getBoolean(HAS_FOCUS)) {
                    taskNameEdit.requestFocus();
                }
                taskNameEdit.setSelection(saved.getInt(EDIT_POS));
            }
        }, 10L);
    }
}
