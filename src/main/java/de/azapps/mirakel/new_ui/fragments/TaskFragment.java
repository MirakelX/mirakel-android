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

package de.azapps.mirakel.new_ui.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.android.calendar.recurrencepicker.RecurrencePickerDialog;
import com.fourmob.datetimepicker.date.DatePicker;
import com.fourmob.datetimepicker.date.SupportDatePickerDialog;
import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import de.azapps.material_elements.utils.IconizedMenu;
import de.azapps.material_elements.utils.MenuHelper;
import de.azapps.material_elements.utils.SoftKeyboard;
import de.azapps.material_elements.utils.ThemeManager;
import de.azapps.material_elements.utils.ViewHelper;
import de.azapps.material_elements.views.Slider;
import de.azapps.mirakel.helper.AnalyticsWrapperBase;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelModelPreferences;
import de.azapps.mirakel.model.MirakelContentObserver;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.tags.Tag;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.views.AddTagView;
import de.azapps.mirakel.new_ui.views.DatesView;
import de.azapps.mirakel.new_ui.views.FileView;
import de.azapps.mirakel.new_ui.views.NoteView;
import de.azapps.mirakel.new_ui.views.PriorityChangeView;
import de.azapps.mirakel.new_ui.views.ProgressDoneView;
import de.azapps.mirakel.new_ui.views.SubtasksView;
import de.azapps.mirakel.new_ui.views.TagsView;
import de.azapps.mirakelandroid.R;
import de.azapps.widgets.OnDateTimeSetListener;
import de.azapps.widgets.SupportDateTimeDialog;

import static de.azapps.tools.OptionalUtils.Procedure;

public class TaskFragment extends DialogFragment implements SoftKeyboard.SoftKeyboardChanged,
    PriorityChangeView.OnPriorityChangeListener, MirakelContentObserver.ObserverCallBack,
    SubtasksView.SubtaskListener, AddTagView.TagChangedListener {

    private static final String TAG = "TaskFragment";
    private static final String TASK = "task";
    private static final String EDIT_TEXT_STATE = "edit_name_state";
    private static final String TASK_NAME_STATE = "task_name_state";
    private static final String EDIT_CURSOR_POS = "edit_cursor_pos";
    private static final String HAS_DIRTY_SUBTASK = "has_subtask";
    private static final String HAS_DIRTY_NOTE = "has_note";
    private static final String HAS_DIRTY_FILE = "has_file";
    private static final String HAS_DIRTY_TAG = "has_tag";
    private static final String FOCUS_TASK_NAME = "focus_task_name";
    private static final String LIST_DIALOG_STATE = "dialog";
    private static final String LIST_DIALOG_SHOWING = "is_showing";
    public  static final int REQUEST_IMAGE_CAPTURE = 324;
    public static final int FILE_SELECT_CODE = 521;
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
    @InjectView(R.id.priority)
    PriorityChangeView priorityChangeView;
    @InjectView(R.id.progress_bar)
    Slider progressSlider;

    @InjectView(R.id.task_note)
    NoteView noteView;
    @InjectView(R.id.task_dates)
    DatesView datesView;
    @InjectView(R.id.task_tags)
    TagsView taskTags;
    @InjectView(R.id.task_subtasks)
    SubtasksView subtasksView;
    @InjectView(R.id.task_files)
    FileView filesView;
    @InjectView(R.id.task_tag_add_view)
    AddTagView tagView;

    @InjectView(R.id.task_button_add_more)
    Button addMoreButton;
    IconizedMenu addMorePopup;

    @InjectView(R.id.tag_wrapper)
    LinearLayout tagWrapper;
    @InjectView(R.id.note_wrapper)
    LinearLayout noteWrapper;
    @InjectView(R.id.subtask_wrapper)
    LinearLayout subtaskWrapper;
    @InjectView(R.id.file_wrapper)
    LinearLayout fileWrapper;
    @InjectView(R.id.progress_text)
    TextView progressText;

    private MirakelContentObserver observer;
    private int hiddenViews = 3;
    private SoftKeyboard keyboard;
    private boolean taskNameInitialized = false;

    @Override
    public void onSoftKeyboardHide() {

    }

    @Override
    public void onSoftKeyboardShow() {

    }

    @Override
    public void handleChange() {
        updateTask();
    }

    @Override
    public void handleChange(final long id) {
        if (id == task.getId()) {
            updateTask();
        }
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
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(TASK, task);
        outState.putParcelable(EDIT_TEXT_STATE, taskNameEdit.onSaveInstanceState());
        outState.putInt(EDIT_CURSOR_POS, taskNameEdit.getSelectionEnd());
        outState.putInt(TASK_NAME_STATE, taskNameViewSwitcher.getCurrentView().getId());
        outState.putBoolean(HAS_DIRTY_SUBTASK, subtaskWrapper.getVisibility() == View.VISIBLE);
        outState.putBoolean(HAS_DIRTY_TAG, tagWrapper.getVisibility() == View.VISIBLE);
        outState.putBoolean(HAS_DIRTY_FILE, fileWrapper.getVisibility() == View.VISIBLE);
        outState.putBoolean(HAS_DIRTY_NOTE, noteWrapper.getVisibility() == View.VISIBLE);
        outState.putBoolean(FOCUS_TASK_NAME, taskNameEdit.hasFocus());
        if (listDialog != null) {
            outState.putParcelable(LIST_DIALOG_STATE, listDialog.onSaveInstanceState());
            outState.putBoolean(LIST_DIALOG_SHOWING, listDialog.isShowing());
        }
    }

    @Override
    public void onViewStateRestored(final Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            if (taskNameEdit != null) {
                taskNameEdit.onRestoreInstanceState(savedInstanceState.getParcelable(EDIT_TEXT_STATE));
            }
            if (taskNameViewSwitcher != null) {
                while (taskNameViewSwitcher.getCurrentView().getId() != savedInstanceState.getInt(
                           TASK_NAME_STATE)) {
                    taskNameViewSwitcher.showNext();
                }
                if (savedInstanceState.getInt(TASK_NAME_STATE) == taskNameEdit.getId() &&
                    savedInstanceState.getBoolean(FOCUS_TASK_NAME)) {
                    //we need to delay this a bit, because otherwise the keyboard is not shown
                    taskNameEdit.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            taskNameEdit.requestFocus();
                            taskNameEdit.setSelection(savedInstanceState.getInt(EDIT_CURSOR_POS));
                        }
                    }, 10L);

                }
            }
            restoreAddMoreItem(noteWrapper, HAS_DIRTY_NOTE, savedInstanceState, R.id.add_note_menu);
            restoreAddMoreItem(fileWrapper, HAS_DIRTY_FILE, savedInstanceState, R.id.add_file_menu);
            restoreAddMoreItem(tagWrapper, HAS_DIRTY_TAG, savedInstanceState, R.id.add_tags_menu);
            restoreAddMoreItem(subtaskWrapper, HAS_DIRTY_SUBTASK, savedInstanceState, R.id.add_subtask_menu);
            filesView.setActivity(getActivity());

            if (savedInstanceState.getParcelable(LIST_DIALOG_STATE) != null) {
                listDialog = createListDialog();
                listDialog.onRestoreInstanceState((Bundle) savedInstanceState.getParcelable(LIST_DIALOG_STATE));
                if (savedInstanceState.getBoolean(LIST_DIALOG_SHOWING, false) && !listDialog.isShowing()) {
                    listDialog.show();
                }
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (filesView != null) {
            filesView.setActivity(activity);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (filesView != null) {
            filesView.setActivity(null);
        }
    }

    void restoreAddMoreItem(final @Nullable View which, final @NonNull String key,
                            final @NonNull Bundle saved, final @IdRes int menuItem) {
        if (which != null && saved.getBoolean(key) &&
            which.getVisibility() != View.VISIBLE) {
            which.setVisibility(View.VISIBLE);
            checkDisableAddButton();
            disableItem(addMorePopup.getMenu(), menuItem);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        setStyle(DialogFragment.STYLE_NO_TITLE, ThemeManager.getDialogTheme());
        Locale.setDefault(Helpers.getLocale(getActivity()));
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            task = savedInstanceState.getParcelable(TASK);
        } else {
            final Bundle arguments = getArguments();
            task = arguments.getParcelable(ARGUMENT_TASK);
        }
        setRetainInstance(true);
        setContentObserver();
    }

    private void setContentObserver() {
        unregisterContentObserver();
        observer = new MirakelContentObserver(new Handler(Looper.getMainLooper()), getActivity(), Task.URI,
                                              this);
    }

    private void unregisterContentObserver() {
        if ((observer != null) && (getActivity() != null) && (getActivity().getContentResolver() != null)) {
            getActivity().getContentResolver().unregisterContentObserver(observer);
        }
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        unregisterContentObserver();
        super.onDismiss(dialog);
        final boolean appliedSemantics = applySemantics();
        if (!appliedSemantics && task.isStub() && task.getName().equals(getString(R.string.task_new))) {
            task.destroy();
        } else {
            task.save();
        }
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.fragment_task, container, false);
        keyboard = new SoftKeyboard((ViewGroup) layout);
        keyboard.setSoftKeyboardCallback(this);
        ButterKnife.inject(this, layout);
        updateAll();
        setupAddMore();

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsWrapperBase.setScreen(this);
    }


    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if ((requestCode == REQUEST_IMAGE_CAPTURE) && (resultCode == Activity.RESULT_OK)) {
            filesView.addPhoto();
        } else if ((requestCode == FILE_SELECT_CODE) && (resultCode == Activity.RESULT_OK)) {
            filesView.addFile(data.getData());
        }
    }

    private void setupAddMore() {
        hiddenViews = 4;
        addMorePopup = new IconizedMenu(getActivity(), addMoreButton);
        addMorePopup.inflate(R.menu.add_more_menu);
        final Menu popupMenu = addMorePopup.getMenu();
        if (task.getContent().isEmpty()) {
            noteWrapper.setVisibility(View.GONE);
        } else {
            checkDisableAddButton();
            disableItem(popupMenu, R.id.add_note_menu);
        }
        if (task.getFiles().isEmpty()) {
            fileWrapper.setVisibility(View.GONE);
        } else {
            checkDisableAddButton();
            disableItem(popupMenu, R.id.add_file_menu);
        }
        if (task.getSubtasks().isEmpty()) {
            subtaskWrapper.setVisibility(View.GONE);
        } else {
            checkDisableAddButton();
            disableItem(popupMenu, R.id.add_subtask_menu);
        }
        if (task.getTags().isEmpty()) {
            tagWrapper.setVisibility(View.GONE);
        } else {
            checkDisableAddButton();
            disableItem(popupMenu, R.id.add_tags_menu);
        }
        MenuHelper.showMenuIcons(getActivity(), popupMenu);
        MenuHelper.colorizeMenuItems(popupMenu, ThemeManager.getColor(R.attr.colorTextGrey));
        addMorePopup.setOnMenuItemClickListener(new IconizedMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                item.setVisible(false);
                switch (item.getItemId()) {
                case R.id.add_note_menu:
                    noteWrapper.setVisibility(View.VISIBLE);
                    noteView.handleEditNote();
                    break;
                case R.id.add_subtask_menu:
                    subtaskWrapper.setVisibility(View.VISIBLE);
                    subtasksView.handleAddSubtask();
                    break;
                case R.id.add_tags_menu:
                    tagWrapper.setVisibility(View.VISIBLE);
                    tagWrapper.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            tagView.clearFocus();
                            tagView.onClick(tagView);
                        }
                    }, 100L);
                    break;
                case R.id.add_file_menu:
                    fileWrapper.setVisibility(View.VISIBLE);
                    filesView.addFile();
                    break;
                }
                checkDisableAddButton();
                return false;
            }
        });
    }

    private static void disableItem(final Menu m, final int id) {
        final MenuItem item = m.findItem(id);
        if (item != null) {
            item.setVisible(false);
        }
    }


    @OnClick(R.id.task_button_add_more)
    void addMore() {
        addMorePopup.show();
    }

    private void checkDisableAddButton() {
        hiddenViews = hiddenViews - 1; // It's easier to see what's happening
        if (hiddenViews < 1) {
            addMoreButton.setTextColor(ThemeManager.getColor(R.attr.colorLightGrey));
            addMoreButton.setEnabled(false);
            ViewHelper.setCompoundDrawable(getActivity(), addMoreButton,
                                           ThemeManager.getColoredIcon(R.drawable.ic_plus_white_18dp,
                                                   ThemeManager.getColor(R.attr.colorLightGrey)));
        }
    }

    @Override
    public void onDestroyView() {
        if ((getDialog() != null) && getRetainInstance()) {
            getDialog().setOnDismissListener(null);
        }
        super.onDestroyView();
        ButterKnife.reset(this);
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

        final Drawable icon = ThemeManager.getColoredIcon(R.drawable.ic_track_changes_white_18dp,
                              ThemeManager.getColor(R.attr.colorTextGrey));
        ViewHelper.setCompoundDrawable(getActivity(), progressText, icon);
        progressSlider.setProgress(task.getProgress());
        progressSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {

                new Thread(new Runnable() {
                    public void run() {
                        if (task.getProgress() == 0 && progress > 0) {
                            AnalyticsWrapperBase.track(AnalyticsWrapperBase.ACTION.SET_PROGRESS);
                        }
                        task.setProgress(progress);
                        task.save();
                    }
                }).run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        noteView.setNote(task.getContent());
        noteView.setOnNoteChangedListener(noteChangedListener);
        datesView.setData(task);
        datesView.setListeners(dueEditListener, listEditListener, reminderEditListener,
                               dueRecurrenceEditListener);
        priorityChangeView.setPriority(task.getPriority());
        priorityChangeView.setOnPriorityChangeListener(this);
        taskTags.setTags(task.getTags());
        taskTags.setTagChangedListener(this);
        subtasksView.initListeners(this);
        subtasksView.setSubtasks(task.getSubtasks());
        filesView.setFiles(task);
        filesView.setActivity(getActivity());
        ViewHelper.setCompoundDrawable(getActivity(), addMoreButton,
                                       ThemeManager.getColoredIcon(R.drawable.ic_plus_white_18dp,
                                               ThemeManager.getColor(R.attr.colorTextGrey)));
    }

    @Override
    public void onTagAdded(final Tag tag) {
        task.addTag(tag);
        AnalyticsWrapperBase.track(AnalyticsWrapperBase.ACTION.ADD_TAG);
    }

    @Override
    public void onTagRemoved(final Tag tag) {
        task.removeTag(tag);
    }


    @Override
    public void priorityChanged(int priority) {
        task.setPriority(priority);
        task.save();
        AnalyticsWrapperBase.track(AnalyticsWrapperBase.ACTION.SET_PRIORITY);
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
        if (task.isStub() && !taskNameInitialized) {
            taskNameInitialized = true;
            taskNameViewSwitcher.showNext();
            taskNameEdit.postDelayed(new Runnable() {
                @Override
                public void run() {
                    taskNameEdit.requestFocus();
                    taskNameEdit.selectAll();
                }
            }, 10L);

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
        taskNameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                task.setName(s.toString());
            }
        });
    }

    private void updateName() {
        taskNameEdit.clearFocus();
        applySemantics();
        taskNameEdit.setText(task.getName());
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
            if (task.getContent().isEmpty() && !input.isEmpty()) {
                AnalyticsWrapperBase.track(AnalyticsWrapperBase.ACTION.ADD_NOTE);
            }
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
                public void onDateSet(final DatePicker datePickerDialog,
                                      final @NonNull Optional<LocalDate> newDate) {
                    if (newDate.isPresent()) {
                        task.setDue(newDate.get().toDateTimeAtStartOfDay());
                    } else {
                        task.setDue(Optional.<DateTime>absent());
                    }
                    task.save();
                    AnalyticsWrapperBase.track(AnalyticsWrapperBase.ACTION.SET_DUE);
                }

            }, task.getDue());
            datePickerDialog.show(getFragmentManager(), "dueDialog");
        }
    };

    private Dialog listDialog;
    private final View.OnClickListener listEditListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            listDialog = createListDialog();
            listDialog.show();
        }
    };

    private Dialog createListDialog() {
        final ArrayAdapter<ListMirakel> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, ListMirakel.all(false));
        return new AlertDialogWrapper.Builder(getActivity()).setTitle(R.string.task_move_to).setAdapter(
        adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, final int i) {
                task.setList(adapter.getItem(i));
                task.save();
                dialogInterface.dismiss();
            }
        }).create();
    }

    private final View.OnClickListener reminderEditListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            final SupportDateTimeDialog dateTimeDialog = SupportDateTimeDialog.newInstance(
            new OnDateTimeSetListener() {
                @Override
                public void onDateTimeSet(final @NonNull Optional<DateTime> newDate) {
                    AnalyticsWrapperBase.track(AnalyticsWrapperBase.ACTION.SET_REMINDER);
                    task.setReminder(newDate);
                    task.save();
                }
            }, task.getReminder());
            dateTimeDialog.show(getFragmentManager(), "reminderDialog");
        }
    };


    private final View.OnClickListener dueRecurrenceEditListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            RecurrencePickerDialog rp = RecurrencePickerDialog.newInstance(new
            RecurrencePickerDialog.OnRecurrenceSetListener() {

                @Override
                public void onRecurrenceSet(@NonNull Optional<Recurring> r) {
                    task.setRecurrence(r);
                    task.save();
                }
            }, task.getRecurrence(), true, false);
            rp.show(getFragmentManager(), "recurrencePickerDue");
        }
    };

    private boolean applySemantics() {
        return Semantic.applySemantics(task, taskNameEdit.getText().toString());
    }

    @OnClick(R.id.task_button_done)
    void doneClick() {
        applySemantics();
        task.save();
        taskNameEdit.clearFocus();
        tagView.clearFocus();
        dismiss();
    }



    @Override
    public void onAddSubtask(String taskName) {
        final ListMirakel list = MirakelModelPreferences
                                 .getListForSubtask(task);
        final Task subtask = Semantic.createTask(taskName, Optional.fromNullable(list),
                             true);
        task.addSubtask(subtask);
        task.save();
        AnalyticsWrapperBase.track(AnalyticsWrapperBase.ACTION.ADD_SUBTASK);
    }

    @Override
    public void onSubtaskClick(Task subtask) {
        final DialogFragment newFragment = TaskFragment.newInstance(subtask);
        newFragment.show(getFragmentManager(), "dialog");

    }

    @Override
    public void onSubtaskDone(Task subtask, boolean done) {
        subtask.setDone(done);
        subtask.save();

    }


}

