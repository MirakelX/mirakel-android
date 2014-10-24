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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import java.util.HashMap;
import java.util.Map;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.adapter.SimpleModelAdapter;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.MirakelContentObserver;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.R;
import de.azapps.mirakel.new_ui.interfaces.OnTaskSelectedListener;
import de.azapps.mirakel.new_ui.views.DatesView;
import de.azapps.mirakel.new_ui.views.NoteView;
import de.azapps.mirakel.new_ui.views.ProgressDoneView;
import de.azapps.mirakel.new_ui.views.ProgressView;
import de.azapps.mirakel.new_ui.views.SubtasksView;
import de.azapps.mirakel.new_ui.views.TagsView;
import de.azapps.tools.Log;
import de.azapps.tools.OptionalUtils.Procedure;
import de.azapps.widgets.SupportDateTimeDialog;

import static com.google.common.base.Optional.of;

public class TaskFragment extends DialogFragment {

    private static final String TAG = "TaskFragment";
    public static final String ARGUMENT_TASK = "task";

    private View layout;
    private Task task;
    private ProgressDoneView progressDoneView;
    private ProgressView progressView;

    // TaskName
    private TextView taskName;
    private EditText taskNameEdit;
    private ViewSwitcher taskNameViewSwitcher;


    private NoteView noteView;
    private DatesView datesView;
    private TagsView task_tags;
    private SubtasksView subtasksView;
    private Button addMoreButton;
    private Button doneButton;

    private MirakelContentObserver observer;


    public TaskFragment() {
    }

    public static TaskFragment newInstance(Task task) {
        TaskFragment f = new TaskFragment();
        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_TASK, task);
        f.setArguments(args);
        return f;
    }

    private void updateTask() {
        Optional<Task> taskOptional = Task.get(task.getId());
        if (taskOptional.isPresent()) {
            task = taskOptional.get();
        } // else do nothing
        updateAll();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
        Bundle arguments = getArguments();
        task = arguments.getParcelable(ARGUMENT_TASK);
        Map<Uri, MirakelContentObserver.ObserverCallBack> callBackMap = new HashMap<>();
        callBackMap.put(Task.URI, new MirakelContentObserver.ObserverCallBack() {
            @Override
            public void handleChange() {
                updateTask();
            }

            @Override
            public void handleChange(long id) {
                updateTask();
            }
        });
        observer = new MirakelContentObserver(new Handler(Looper.getMainLooper()), getActivity(),
                                              callBackMap);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (observer != null && getActivity() != null && getActivity().getContentResolver() != null) {
            getActivity().getContentResolver().unregisterContentObserver(observer);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ////////////////////////////////////////
        // Inflate the layout for this fragment
        layout = inflater.inflate(R.layout.fragment_task, container, false);
        progressDoneView = (ProgressDoneView) layout.findViewById(R.id.task_progress_done);
        taskName = (TextView) layout.findViewById(R.id.task_name);
        taskNameEdit = (EditText) layout.findViewById(R.id.task_name_edit);
        taskNameViewSwitcher = (ViewSwitcher) layout.findViewById(R.id.task_name_view_switcher);
        progressView = (ProgressView) layout.findViewById(R.id.task_progress);
        noteView = (NoteView) layout.findViewById(R.id.task_note);
        datesView = (DatesView) layout.findViewById(R.id.task_dates);
        task_tags = (TagsView) layout.findViewById(R.id.task_tags);
        subtasksView = (SubtasksView) layout.findViewById(R.id.task_subtasks);
        addMoreButton = (Button) layout.findViewById(R.id.task_button_add_more);
        doneButton = (Button) layout.findViewById(R.id.task_button_done);
        updateAll();
        if (task.isStub()) {
            taskNameViewSwitcher.showNext();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                                         Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(taskNameEdit, InputMethodManager.SHOW_IMPLICIT);
            taskNameEdit.selectAll();
        }
        return layout;
    }

    private void updateAll() {
        ///////////////////
        // Now the actions
        progressDoneView.setProgress(task.getProgress());
        progressDoneView.setChecked(task.isDone());
        progressDoneView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                task.setDone(isChecked);
                task.save();
            }
        });
        taskName.setText(task.getName());
        taskNameEdit.setText(task.getName());
        taskName.setOnClickListener(onEditName);
        progressView.setProgress(task.getProgress());
        progressView.setOnProgressChangeListener(progressChangedListener);
        noteView.setNote(task.getContent());
        noteView.setOnNoteChangedListener(noteChangedListener);
        datesView.setData(task);
        datesView.setListeners(dueEditListener, listEditListener, reminderEditListener);
        task_tags.setTask(task);
        subtasksView.setSubtasks(task.getSubtasks(), onSubtaskAddListener, onSubtaskClickListener,
                                 onSubtaskDoneListener);
        doneButton.setOnClickListener(onDoneButtonClickListener);
    }

    private Procedure<Integer> progressChangedListener = new
    Procedure<Integer>() {
        @Override
        public void apply(Integer input) {
            task.setProgress(input);
            task.save();
        }
    };

    private View.OnClickListener onEditName = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            taskNameViewSwitcher.showNext();
            taskNameEdit.setText(task.getName());
            taskNameEdit.requestFocus();
            taskNameEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    switch (actionId) {
                    case EditorInfo.IME_ACTION_DONE:
                    case EditorInfo.IME_ACTION_SEND:
                        updateName();
                        return true;
                    }
                    return false;
                }
            });
            taskNameEdit.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
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
            task.setName(taskNameEdit.getText().toString());
            taskName.setText(task.getName());
            task.save();
            taskNameViewSwitcher.showNext();
        }
    };

    private Procedure<String> noteChangedListener = new
    Procedure<String>() {
        @Override
        public void apply(String input) {
            task.setContent(input);
            task.save();
        }
    };
    private final View.OnClickListener dueEditListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SupportDatePickerDialog datePickerDialog = SupportDatePickerDialog.newInstance(new
            DatePicker.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePickerDialog, int year, int month, int day) {
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
        public void onClick(View v) {
            final SimpleModelAdapter<ListMirakel> adapter = new SimpleModelAdapter<>(getActivity(),
                    ListMirakel.getAllCursor(),
                    0, ListMirakel.class);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.task_move_to);
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    task.setList(adapter.getItem(i));
                    task.save();
                }
            });
            builder.show();
        }
    };
    private final View.OnClickListener reminderEditListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SupportDateTimeDialog dateTimeDialog = SupportDateTimeDialog.newInstance(
            new SupportDateTimeDialog.OnDateTimeSetListener() {
                @Override
                public void onDateTimeSet(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
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


    private Procedure<Task> onSubtaskDoneListener = new Procedure<Task>() {
        @Override
        public void apply(Task task) {
            task.toggleDone();
            task.save();
        }
    };
    private View.OnClickListener onSubtaskAddListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            AddSubtaskFragment addSubtaskFragment = AddSubtaskFragment.newInstance(task);
            addSubtaskFragment.show(getFragmentManager(), "subtaskAddDialog");
        }
    };

    private OnTaskSelectedListener onSubtaskClickListener = new OnTaskSelectedListener() {
        @Override
        public void onTaskSelected(Task task) {
            DialogFragment newFragment = TaskFragment.newInstance(task);
            newFragment.show(getFragmentManager(), "dialog");
        }
    };

    private View.OnClickListener onDoneButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
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
    };

}

