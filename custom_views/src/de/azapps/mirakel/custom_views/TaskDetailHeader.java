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

import android.content.Context;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewSwitcher;

import com.google.common.base.Optional;

import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class TaskDetailHeader extends BaseTaskDetailRow {

    public interface OnDoneChangedListner {
        public abstract void onDoneChanged(final Task newTask);
    }

    protected static final String TAG = "TaskDetailHeader";
    protected OnDoneChangedListner doneChanged;
    protected ViewSwitcher switcher;
    private CheckBox taskDone;
    protected TextView taskName;
    protected TextView taskPrio;
    protected EditText txt;

    public TaskDetailHeader(final Context ctx) {
        super(ctx);
        inflate(ctx, R.layout.task_head_line, this);
        this.taskName = (TextView) findViewById(R.id.task_name);
        this.taskDone = (CheckBox) findViewById(R.id.task_done);
        this.taskPrio = (TextView) findViewById(R.id.task_prio);
        this.switcher = (ViewSwitcher) findViewById(R.id.switch_name);
        this.txt = (EditText) findViewById(R.id.edit_name);
        final InputMethodManager imm = (InputMethodManager) TaskDetailHeader.this.context
                                       .getSystemService(Context.INPUT_METHOD_SERVICE);
        this.taskName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                clearFocus();
                TaskDetailHeader.this.switcher.showNext(); // or
                // switcher.showPrevious();
                final CharSequence name = TaskDetailHeader.this.taskName
                                          .getText();
                TaskDetailHeader.this.txt.setText(name);
                TaskDetailHeader.this.txt
                .setOnFocusChangeListener(new OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(final View view,
                                              final boolean hasFocus) {
                        if (hasFocus) {
                            imm.showSoftInput(
                                TaskDetailHeader.this.txt,
                                InputMethodManager.SHOW_IMPLICIT);
                        } else {
                            imm.showSoftInput(
                                TaskDetailHeader.this.txt,
                                InputMethodManager.HIDE_IMPLICIT_ONLY);
                        }
                        imm.restartInput(TaskDetailHeader.this.txt);
                    }
                });
                TaskDetailHeader.this.txt.requestFocus();
                TaskDetailHeader.this.txt
                .setOnEditorActionListener(new OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(final TextView view,
                                                  final int actionId, final KeyEvent event) {
                        TaskDetailHeader.this.txt.clearFocus();
                        imm.restartInput(TaskDetailHeader.this.txt);
                        TaskDetailHeader.this.txt
                        .setOnFocusChangeListener(null);
                        if (actionId == EditorInfo.IME_ACTION_DONE
                            && TaskDetailHeader.this.task != null) {
                            TaskDetailHeader.this.task
                            .setName(TaskDetailHeader.this.txt
                                     .getText().toString());
                            save();
                            TaskDetailHeader.this.taskName
                            .setText(TaskDetailHeader.this.task
                                     .getName());
                            TaskDetailHeader.this.txt
                            .setOnFocusChangeListener(null);
                            imm.hideSoftInputFromWindow(
                                TaskDetailHeader.this.txt
                                .getWindowToken(), 0);
                            TaskDetailHeader.this.switcher
                            .showPrevious();
                            return true;
                        }
                        return false;
                    }
                });
                TaskDetailHeader.this.txt.setSelection(name.length());
            }
        });
        this.taskPrio.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                TaskDialogHelpers.handlePriority(TaskDetailHeader.this.context,
                TaskDetailHeader.this.task, new ExecInterface() {
                    @Override
                    public void exec() {
                        TaskHelper.setPrio(
                            TaskDetailHeader.this.taskPrio,
                            TaskDetailHeader.this.task);
                        save();
                    }
                });
            }
        });
    }

    public void setOnDoneChangedListner(final OnDoneChangedListner l) {
        this.doneChanged = l;
    }

    @Override
    protected void updateView() {
        final String tname = this.task.getName();
        this.taskName.setText(tname == null ? "" : tname);
        if (MirakelCommonPreferences.isTablet()) {
            this.taskName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        }
        if (this.switcher.getCurrentView().getId() == R.id.edit_name
            && !this.task.getName().equals(this.txt.getText().toString())) {
            this.switcher.showPrevious();
        }
        // Task done
        this.taskDone.setOnCheckedChangeListener(null);
        this.taskDone.setChecked(this.task.isDone());
        this.taskDone.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView,
                                         final boolean isChecked) {
                Log.d(TAG, "check " + isChecked);
                final Optional<Task> newTask = TaskDetailHeader.this.task
                                               .setDone(isChecked);
                save();
                if (newTask.isPresent()) {
                    // recurring, task changed, plug new task in
                    TaskDetailHeader.this.task = newTask.get();
                }
                if (TaskDetailHeader.this.doneChanged != null) {
                    TaskDetailHeader.this.doneChanged
                    .onDoneChanged(TaskDetailHeader.this.task);
                }
            }
        });
        // Task priority
        TaskHelper.setPrio(this.taskPrio, this.task);
    }
}
