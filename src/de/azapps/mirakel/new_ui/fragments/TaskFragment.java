package de.azapps.mirakel.new_ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.fourmob.datetimepicker.date.DatePicker;
import com.fourmob.datetimepicker.date.DatePickerDialog;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;


import de.azapps.mirakel.custom_views.BaseTaskDetailRow;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.model.MirakelContentObserver;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.R;
import de.azapps.mirakel.new_ui.adapter.SimpleModelAdapter;
import de.azapps.mirakel.new_ui.interfaces.OnTaskSelectedListener;
import de.azapps.mirakel.new_ui.views.DatesView;
import de.azapps.mirakel.new_ui.views.NoteView;
import de.azapps.mirakel.new_ui.views.ProgressDoneView;
import de.azapps.mirakel.new_ui.views.ProgressView;
import de.azapps.mirakel.new_ui.views.SubtasksView;
import de.azapps.mirakel.new_ui.views.TagsView;
import de.azapps.tools.OptionalUtils;
import de.azapps.widgets.DateTimeDialog;

import static com.google.common.base.Optional.fromNullable;
import static de.azapps.tools.OptionalUtils.Procedure;

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
                task = Task.get(task.getId());
                updateAll();
            }
            @Override
            public void handleChange(long id) {
                task = Task.get(task.getId());
                updateAll();
            }
        });
        observer = new MirakelContentObserver(new Handler(Looper.getMainLooper()), getActivity(),
                                              callBackMap);
    }

    @Override
    public void onDismiss (DialogInterface dialog) {
        if (observer != null) {
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
        updateAll();
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
            DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(new
            DatePicker.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePickerDialog, int year, int month, int day) {
                    task.setDue(new GregorianCalendar(year, month, day));
                    task.save();
                }
                @Override
                public void onNoDateSet() {
                    task.setDue(null);
                    task.save();
                }
            }, fromNullable(task.getDue()), false);
            datePickerDialog.show(getFragmentManager(), "dueDialog");
        }
    };

    private final View.OnClickListener listEditListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final SimpleModelAdapter adapter = new SimpleModelAdapter(getActivity(), ListMirakel.getAllCursor(),
                    0);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.task_move_to);
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    task.setList(new ListMirakel((Cursor) adapter.getItem(i)));
                    task.save();
                }
            });
            builder.show();
        }
    };
    private final View.OnClickListener reminderEditListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            DateTimeDialog dateTimeDialog = DateTimeDialog.newInstance(new
            DateTimeDialog.OnDateTimeSetListener() {
                @Override
                public void onDateTimeSet(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
                    task.setReminder(new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute));
                    task.save();
                }
                @Override
                public void onNoTimeSet() {
                    task.setReminder(null);
                    task.save();
                }
            }, fromNullable(task.getReminder()), false);
            dateTimeDialog.show(getFragmentManager(), "reminderDialog");
        }
    };


    private Procedure<Task> onSubtaskDoneListener = new Procedure<Task>() {
        @Override
        public void apply(Task task) {
            task.setDone(!task.isDone());
            task.save();
        }
    };
    private View.OnClickListener onSubtaskAddListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            TaskDialogHelpers.handleSubtask(getActivity(), task, new BaseTaskDetailRow.OnTaskChangedListner() {
                @Override
                public void onTaskChanged(Task newTask) {
                    task.save();
                }
            }, false);
        }
    };

    private OnTaskSelectedListener onSubtaskClickListener = new OnTaskSelectedListener() {
        @Override
        public void onTaskSelected(Task task) {
            DialogFragment newFragment = TaskFragment.newInstance(task);
            newFragment.show(getFragmentManager(), "dialog");
        }
    };

}

