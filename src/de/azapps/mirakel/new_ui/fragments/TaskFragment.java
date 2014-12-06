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

package de.azapps.mirakel.new_ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.fourmob.datetimepicker.date.DatePicker;
import com.fourmob.datetimepicker.date.SupportDatePickerDialog;
import com.google.common.base.Optional;

import java.util.Calendar;
import java.util.GregorianCalendar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.MirakelContentObserver;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.views.DatesView;
import de.azapps.mirakel.new_ui.views.NoteView;
import de.azapps.mirakel.new_ui.views.ProgressDoneView;
import de.azapps.mirakel.new_ui.views.ProgressView;
import de.azapps.mirakel.new_ui.views.SubtasksView;
import de.azapps.mirakel.new_ui.views.TagsView;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.Log;
import de.azapps.widgets.SupportDateTimeDialog;

import static com.google.common.base.Optional.of;
import static de.azapps.tools.OptionalUtils.Procedure;

public class TaskFragment extends DialogFragment {

    private static final String TAG = "TaskFragment";
    public static final String ARGUMENT_TASK = "task";

    private Task task;
    @InjectView(R.id.task_progress_done)
    ProgressDoneView progressDoneView;

    // TaskName
    @InjectView(R.id.task_name)
    TextView taskName;
    @InjectView(R.id.task_name_edit)
    EditText taskNameEdit;
    @InjectView(R.id.task_name_view_switcher)
    ViewSwitcher taskNameViewSwitcher;
    @InjectView(R.id.task_progress)
    ProgressView progressView;

    @InjectView(R.id.task_note)
    NoteView noteView;
    @InjectView(R.id.task_dates)
    DatesView datesView;
    @InjectView(R.id.task_tags)
    TagsView taskTags;
    @InjectView(R.id.task_subtasks)
    SubtasksView subtasksView;
    @InjectView(R.id.task_button_add_more)
    Button addMoreButton;
    @InjectView(R.id.task_button_done)
    Button doneButton;

    private MirakelContentObserver observer;
    private InputMethodManager inputMethodManager;


    public TaskFragment() {
    }

    public static TaskFragment newInstance(final Task task) {
        final TaskFragment taskFragment = new TaskFragment();
        // Supply num input as an argument.
        final Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_TASK, task);
        taskFragment.setArguments(args);
        return taskFragment;
    }

    private void updateTask() {
        final Optional<Task> taskOptional = Task.get(task.getId());
        if (taskOptional.isPresent()) {
            task = taskOptional.get();
        } // else do nothing
        updateAll();

    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
        final Bundle arguments = getArguments();
        task = arguments.getParcelable(ARGUMENT_TASK);
        observer = new MirakelContentObserver(new Handler(Looper.getMainLooper()), getActivity(), Task.URI,
        new MirakelContentObserver.ObserverCallBack() {
            @Override
            public void handleChange() {
                updateTask();
            }

            @Override
            public void handleChange(final long id) {
                updateTask();
            }
        });
        inputMethodManager = (InputMethodManager) getActivity().getSystemService(
                                 Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (observer != null && getActivity() != null && getActivity().getContentResolver() != null) {
            getActivity().getContentResolver().unregisterContentObserver(observer);
        }
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.fragment_task, container, false);
        ButterKnife.inject(this, layout);
        updateAll();

        return layout;
    }

    private void toggleKeyboard() {
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private void updateAll() {
        ///////////////////
        // Now the actions
        progressDoneView.setProgress(task.getProgress());
        progressDoneView.setChecked(task.isDone());
        progressDoneView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                task.setDone(isChecked);
                task.save();
            }
        });
        taskName.setText(task.getName());
        initTaskNameEdit();
        progressView.setProgress(task.getProgress());
        progressView.setOnProgressChangeListener(progressChangedListener);
        noteView.setNote(task.getContent());
        noteView.setOnNoteChangedListener(noteChangedListener);
        datesView.setData(task);
        datesView.setListeners(dueEditListener, listEditListener, reminderEditListener);
        taskTags.setTask(task);
        subtasksView.setSubtasks(task.getSubtasks(), onSubtaskAddListener, onSubtaskClickListener,
                                 onSubtaskDoneListener);
    }

    private final Procedure<Integer> progressChangedListener = new
    Procedure<Integer>() {
        @Override
        public void apply(final Integer input) {
            task.setProgress(input);
            task.save();
        }
    };

    @OnFocusChange(R.id.task_name_edit)
    void taskNameEditFocusChange(boolean hasFocus) {
        if (hasFocus) {
            toggleKeyboard();
        }
    }

    @OnEditorAction(R.id.task_name_edit)
    boolean onEditorAction(int actionId) {
        switch (actionId) {
        case EditorInfo.IME_ACTION_DONE:
        case EditorInfo.IME_ACTION_SEND:
            updateName();
            return true;
        }
        return false;
    }

    private void initTaskNameEdit() {
        taskNameEdit.setText(task.getName());
        // Show Keyboard if stub
        if (task.isStub()) {
            taskNameViewSwitcher.showNext();
            taskNameEdit.selectAll();
            taskNameEdit.requestFocus();
            toggleKeyboard();
        }
        taskNameEdit.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    updateName();
                    return true;
                }
                return false;
            }
        });
    }

    private void updateName() {
        toggleKeyboard();
        taskNameEdit.clearFocus();
        task.setName(taskNameEdit.getText().toString());
        taskName.setText(task.getName());
        task.save();
        taskNameViewSwitcher.showPrevious();
    }

    @OnClick(R.id.task_name)
    void clickTaskName() {
        taskNameViewSwitcher.showNext();
        taskNameEdit.setText(task.getName());
        taskNameEdit.requestFocus();

    }

    private final Procedure<String> noteChangedListener = new
    Procedure<String>() {
        @Override
        public void apply(final String input) {
            task.setContent(input);
            task.save();
        }
    };
    private final View.OnClickListener dueEditListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final SupportDatePickerDialog datePickerDialog = SupportDatePickerDialog.newInstance(new
            DatePicker.OnDateSetListener() {
                @Override
                public void onDateSet(final DatePicker datePickerDialog, final int year, final int month,
                                      final int day) {
                    task.setDue(of((Calendar) new GregorianCalendar(year, month, day)));
                    task.save();
                }

                @Override
                public void onNoDateSet() {
                    task.setDue(Optional.<Calendar>absent());
                    task.save();
                }
            }, task.getDue(), false);
            datePickerDialog.show(getFragmentManager(), "dueDialog");
        }
    };

    private final View.OnClickListener listEditListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            //TODO
            final ArrayAdapter<ListMirakel> adapter = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_list_item_1, ListMirakel.all(false));
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.task_move_to);
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, final int i) {
                    task.setList(adapter.getItem(i));
                    task.save();
                }
            });
            builder.show();
        }
    };
    private final View.OnClickListener reminderEditListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final SupportDateTimeDialog dateTimeDialog = SupportDateTimeDialog.newInstance(
            new SupportDateTimeDialog.OnDateTimeSetListener() {
                @Override
                public void onDateTimeSet(final int year, final int month, final int dayOfMonth,
                                          final int hourOfDay, final int minute) {
                    task.setReminder(of((Calendar) new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute)));
                    task.save();
                }

                @Override
                public void onNoTimeSet() {
                    task.setReminder(Optional.<Calendar>absent());
                    task.save();
                }
            }, task.getReminder(), false);
            dateTimeDialog.show(getFragmentManager(), "reminderDialog");
        }
    };


    private final Procedure<Task> onSubtaskDoneListener = new Procedure<Task>() {
        @Override
        public void apply(final Task task) {
            task.toggleDone();
            task.save();
        }
    };
    private final View.OnClickListener onSubtaskAddListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            final AddSubtaskFragment addSubtaskFragment = AddSubtaskFragment.newInstance(task);
            addSubtaskFragment.show(getFragmentManager(), "subtaskAddDialog");
        }
    };

    private final OnItemClickedListener<Task> onSubtaskClickListener = new
    OnItemClickedListener<Task>() {

        @Override
        public void onItemSelected(final @NonNull Task item) {
            final DialogFragment newFragment = TaskFragment.newInstance(task);
            newFragment.show(getFragmentManager(), "dialog");
        }
    };

    @OnClick(R.id.task_button_done)
    void doneClick() {
        if (task.isStub()) {
            try {
                task.create();
            } catch (final DefinitionsHelper.NoSuchListException e) {
                ErrorReporter.report(ErrorType.TASKS_NO_LIST);
                Log.e(TAG, "NoSuchListException", e);
            }
        }
        dismiss();
    }

}

