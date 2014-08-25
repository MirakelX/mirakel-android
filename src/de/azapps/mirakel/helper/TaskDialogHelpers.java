/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists Copyright (c) 2013-2014 Anatolij Zelenin, Georg
 * Semmler. This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or any later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.helper;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewSwitcher;

import com.android.calendar.recurrencepicker.RecurrencePickerDialog;
import com.android.calendar.recurrencepicker.RecurrencePickerDialog.OnRecurrenceSetListener;
import com.google.common.base.Optional;

import de.azapps.mirakel.DefenitionsModel.ExecInterfaceWithTask;
import de.azapps.mirakel.adapter.SubtaskAdapter;
import de.azapps.mirakel.custom_views.BaseTaskDetailRow.OnTaskChangedListner;
import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.helper.error.ErrorReporter;
import de.azapps.mirakel.helper.error.ErrorType;
import de.azapps.mirakel.model.MirakelInternalContentProvider;
import de.azapps.mirakel.model.ModelBase;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder;
import de.azapps.mirakel.model.query_builder.MirakelQueryBuilder.Operation;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;
import de.azapps.tools.OptionalUtils;
import de.azapps.widgets.SupportDateTimeDialog;
import de.azapps.widgets.SupportDateTimeDialog.OnDateTimeSetListener;
import static com.google.common.base.Optional.of;

public class TaskDialogHelpers {
    protected static AlertDialog audio_playback_dialog;
    protected static boolean audio_playback_playing;

    private static AlertDialog audio_record_alert_dialog;

    protected static String audio_record_filePath;

    protected static MediaRecorder audio_record_mRecorder;

    protected static boolean content;

    private static final DialogInterface.OnClickListener dialogDoNothing = null;

    protected static boolean done;
    protected static long listId;
    protected static boolean newTask;
    protected static boolean optionEnabled;
    protected static boolean reminder;
    protected static String searchString;
    protected static SubtaskAdapter subtaskAdapter;
    protected static final String TAG = "TaskDialogHelpers";

    protected static void cancelRecording() {
        audio_record_mRecorder.stop();
        audio_record_mRecorder.release();
        audio_record_mRecorder = null;
        try {
            new File(audio_record_filePath).delete();
        } catch (final Exception e) {
            // eat it
        }
    }

    protected static Cursor queryForSubtasks(final Task t, final Context ctx) {
        final MirakelQueryBuilder qb = new MirakelQueryBuilder(ctx);
        qb.and(Task.NAME, Operation.LIKE, "%" + searchString + "%");
        qb.and(Task.ID, Operation.NOT_IN,
               new MirakelQueryBuilder(ctx).select("parent_id").and("child_id", Operation.EQ,
                       t), MirakelInternalContentProvider.SUBTASK_URI);
        qb.and(Task.ID, Operation.NOT_EQ, t);
        Task.addBasicFiler(qb);
        if (optionEnabled) {
            if (!done) {
                qb.and(Task.DONE, Operation.EQ, false);
            }
            if (content) {
                qb.and(Task.CONTENT, Operation.NOT_EQ, (String) null);
                qb.and(Task.CONTENT, Operation.NOT_EQ, "");
            }
            if (reminder) {
                qb.and(Task.REMINDER, Operation.NOT_EQ, (String) null);
            }
            if (listId > 0) {
                qb.and(Task.LIST_ID, Operation.EQ, listId);
            } else {
                Optional<SpecialList> specialListOptional = SpecialList.getSpecial(listId);
                OptionalUtils.withOptional(specialListOptional, new OptionalUtils.Procedure<SpecialList>() {
                    @Override
                    public void apply(SpecialList input) {
                        qb.and(input.getWhereQueryForTasks());
                    }
                });
            }
        }
        return qb.select(Task.allColumns).query(Task.URI);
    }

    public static void handleAudioRecord(final Context context,
                                         final Task task, final ExecInterfaceWithTask onSuccess) {
        try {
            audio_record_mRecorder = new MediaRecorder();
            audio_record_mRecorder
            .setAudioSource(MediaRecorder.AudioSource.MIC);
            audio_record_mRecorder
            .setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            audio_record_filePath = FileUtils.getOutputMediaFile(
                                        FileUtils.MEDIA_TYPE_AUDIO).getAbsolutePath();
            audio_record_mRecorder.setOutputFile(audio_record_filePath);
            audio_record_mRecorder
            .setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            audio_record_mRecorder.prepare();
            audio_record_mRecorder.start();
        } catch (final Exception e) {
            Log.e(TAG, "prepare() failed");
            ErrorReporter.report(ErrorType.NO_SPEACH_RECOGNITION);
            return;
        }
        audio_record_alert_dialog = new AlertDialog.Builder(context)
        .setTitle(R.string.audio_record_title)
        .setMessage(R.string.audio_record_message)
        .setPositiveButton(android.R.string.ok,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                Task mTask = task;
                if (task == null || task.getId() == 0) {
                    ListMirakel listMirakel;
                    if (task == null) {
                        listMirakel = MirakelModelPreferences
                                      .getSafeImportDefaultList();
                    } else {
                        listMirakel = task.getList();
                    }
                    mTask = Semantic.createTask(
                                MirakelCommonPreferences
                                .getAudioDefaultTitle(), Optional.fromNullable(listMirakel), true,
                                context);
                }
                audio_record_mRecorder.stop();
                audio_record_mRecorder.release();
                audio_record_mRecorder = null;
                mTask.addFile(context, Uri.fromFile(new File(
                                                        audio_record_filePath)));
                onSuccess.exec(mTask);
            }
        })
        .setNegativeButton(android.R.string.cancel,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                cancelRecording();
            }
        }).setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(final DialogInterface dialog) {
                cancelRecording();
            }
        }).show();
    }

    public static void handleDeleteFile(final List<FileMirakel> selectedItems,
                                        final Context ctx, final Task t,
                                        final ExecInterfaceWithTask onSuccess) {
        if (selectedItems.size() < 1) {
            return;
        }
        String files = "\"" + selectedItems.get(0).getName() + "\"";
        for (int i = 1; i < selectedItems.size(); i++) {
            files += ", \"" + selectedItems.get(i).getName() + "\"";
        }
        new AlertDialog.Builder(ctx)
        .setTitle(ctx.getString(R.string.remove_files))
        .setMessage(
            ctx.getString(R.string.remove_files_summary, files,
                          t.getName()))
        .setPositiveButton(android.R.string.ok,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                for (final FileMirakel f : selectedItems) {
                    f.destroy();
                }
                if (onSuccess != null) {
                    onSuccess.exec(t);
                }
            }
        })
        .setNegativeButton(android.R.string.cancel, dialogDoNothing)
        .show();
    }

    public static void handlePriority(final Context ctx, final Task task,
                                      final Helpers.ExecInterface onSuccess) {
        if (task == null) {
            return;
        }
        final String[] t = { "2", "1", "0", "-1", "-2" };
        final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(R.string.task_change_prio_title);
        builder.setSingleChoiceItems(t, 2 - task.getPriority(),
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                dialog.dismiss();
                task.setPriority(2 - which);
                task.save();
                onSuccess.exec();
            }
        });
        builder.show();
    }

    @SuppressWarnings("boxing")
    public static void handleRecurrence(final ActionBarActivity activity,
                                        final Task task, final boolean isDue, final ExecInterface callback) {
        final FragmentManager fm = activity.getSupportFragmentManager();
        Optional<Recurring> recurringOptional = isDue ? task.getRecurring() : task.getRecurringReminder();
        boolean isExact = false;
        if (recurringOptional.isPresent()) {
            Recurring recurring = recurringOptional.get();
            isExact = recurring.isExact();
            Log.d(TAG, "exact: " + isExact);
            if (recurring.getDerivedFrom().isPresent()) {
                final Optional<Recurring> master = Recurring.get(recurring.getDerivedFrom().get());
                if (master.isPresent()) {
                    recurring = master.get();
                }
            }
        }
        final RecurrencePickerDialog rp = RecurrencePickerDialog.newInstance(
        new OnRecurrenceSetListener() {
            @Override
            public void onCustomRecurrenceSetInterval(
                final boolean isDue, final int intervalYears,
                final int intervalMonths, final int intervalDays,
                final int intervalHours, final int intervalMinutes,
                @NonNull final Optional<Calendar> startDate, @NonNull final Optional<Calendar> endDate,
                final boolean isExact) {
                final Recurring r = Recurring.newRecurring("",
                                    intervalMinutes, intervalHours, intervalDays,
                                    intervalMonths, intervalYears, isDue,
                                    startDate, endDate, true, isExact,
                                    new SparseBooleanArray());
                setRecurence(task, isDue, r.getId(), callback);
            }
            @Override
            public void onCustomRecurrenceSetWeekdays(
                final boolean isDue, @NonNull final List<Integer> weekdays,
                @NonNull final Optional<Calendar> startDate, @NonNull final Optional<Calendar> endDate,
                final boolean isExact) {
                final SparseBooleanArray weekdaysArray = new SparseBooleanArray();
                for (final int day : weekdays) {
                    weekdaysArray.put(day, true);
                }
                final Recurring r = Recurring.newRecurring("", 0, 0, 0,
                                    0, 0, isDue, startDate, endDate, true, isExact,
                                    weekdaysArray);
                setRecurence(task, isDue, r.getId(), callback);
            }
            @Override
            public void onNoRecurrenceSet() {
                setRecurence(task, isDue, -1, callback);
            }
            @Override
            public void onRecurrenceSet(final Recurring r) {
                setRecurence(task, isDue, r.getId(), callback);
            }
        }, recurringOptional, isDue, MirakelCommonPreferences.isDark(), isExact);
        rp.show(fm, "reccurence");
    }

    public static void handleReminder(final ActionBarActivity ctx,
                                      final Task task, final OnTaskChangedListner onSuccess) {
        final Calendar reminder = task.getReminder().or(new GregorianCalendar());
        final FragmentManager fm = ctx.getSupportFragmentManager();
        final SupportDateTimeDialog dtDialog = SupportDateTimeDialog.newInstance(
        new OnDateTimeSetListener() {
            @Override
            public void onDateTimeSet(final int year, final int month,
                                      final int dayOfMonth, final int hourOfDay,
                                      final int minute) {
                reminder.set(Calendar.YEAR, year);
                reminder.set(Calendar.MONTH, month);
                reminder.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                reminder.set(Calendar.HOUR_OF_DAY, hourOfDay);
                reminder.set(Calendar.MINUTE, minute);
                task.setReminder(of(reminder), true);
                onSuccess.onTaskChanged(task);
            }
            @Override
            public void onNoTimeSet() {
                task.setReminder(Optional.<Calendar>absent());
                onSuccess.onTaskChanged(task);
            }
        }, reminder.get(Calendar.YEAR), reminder.get(Calendar.MONTH),
        reminder.get(Calendar.DAY_OF_MONTH), reminder
        .get(Calendar.HOUR_OF_DAY), reminder
        .get(Calendar.MINUTE), true, MirakelCommonPreferences
        .isDark());
        dtDialog.show(fm, "datetimedialog");
    }

    public static void handleRemoveSubtask(final List<Task> subtasks,
                                           final Context ctx, final Task task,
                                           final ExecInterfaceWithTask onSuccess) {
        if (subtasks.size() == 0) {
            return;
        }
        String names = "\"" + subtasks.get(0).getName() + "\"";
        for (int i = 1; i < subtasks.size(); i++) {
            names += ", \"" + subtasks.get(i).getName() + "\"";
        }
        new AlertDialog.Builder(ctx)
        .setTitle(ctx.getString(R.string.remove_subtask))
        .setMessage(
            ctx.getString(R.string.remove_files_summary, names,
                          task.getName()))
        .setPositiveButton(android.R.string.ok,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                for (final Task s : subtasks) {
                    final boolean permanent;
                    Optional<ListMirakel> listMirakelOptional = MirakelModelPreferences.subtaskAddToList();
                    if (listMirakelOptional.isPresent() && s.getList().equals(listMirakelOptional.get())) {
                        permanent = true;
                    } else {
                        permanent = false;
                    }
                    task.deleteSubtask(s, permanent);
                }
                if (onSuccess != null) {
                    onSuccess.exec(task);
                }
            }
        }
                          )
        .setNegativeButton(android.R.string.cancel, dialogDoNothing)
        .show();
    }

    @SuppressLint("NewApi")
    public static void handleSubtask(final Context ctx, final Task task,
                                     final OnTaskChangedListner taskChanged, final boolean asSubtask) {
        final List<Pair<Long, String>> names = Task.getTaskNames();
        final CharSequence[] values = new String[names.size()];
        for (int i = 0; i < names.size(); i++) {
            values[i] = names.get(i).second;
        }
        final View v = ((Activity) ctx).getLayoutInflater().inflate(
                           R.layout.select_subtask, null, false);
        final ListView lv = (ListView) v.findViewById(R.id.subtask_listview);
        final List<Task> tasks = Task.cursorToTaskList(ctx.getContentResolver().query(
                                     MirakelInternalContentProvider.TASK_URI, Task.allColumns,
                                     ModelBase.ID + "=" + task.getId() + " AND " + Task.BASIC_FILTER_DISPLAY_TASKS, null, null, null));
        subtaskAdapter = new SubtaskAdapter(ctx, 0, tasks, task, asSubtask);
        lv.post(new Runnable() {
            @Override
            public void run() {
                lv.setAdapter(subtaskAdapter);
            }
        });
        searchString = "";
        done = false;
        content = false;
        reminder = false;
        optionEnabled = false;
        newTask = true;
        listId = SpecialList.firstSpecialSafe().getId();
        final EditText search = (EditText) v
                                .findViewById(R.id.subtask_searchbox);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Drawable left = ctx.getResources().getDrawable(
                                android.R.drawable.ic_menu_search);
            Drawable right = null;
            left.setBounds(0, 0, 42, 42);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                && ctx.getResources().getConfiguration()
                .getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                right = ctx.getResources().getDrawable(
                            android.R.drawable.ic_menu_search);
                right.setBounds(0, 0, 42, 42);
                left = null;
            }
            search.setCompoundDrawables(left, null, right, null);
        }
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(final Editable s) {
                // Nothing
            }
            @Override
            public void beforeTextChanged(final CharSequence s,
                                          final int start, final int count, final int after) {
                // Nothing
            }
            @Override
            public void onTextChanged(final CharSequence s, final int start,
                                      final int before, final int count) {
                searchString = s.toString();
                updateListView(subtaskAdapter, task, lv, ctx);
            }
        });
        final Button options = (Button) v.findViewById(R.id.subtasks_options);
        final LinearLayout wrapper = (LinearLayout) v
                                     .findViewById(R.id.subtask_option_wrapper);
        wrapper.setVisibility(View.GONE);
        options.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (optionEnabled) {
                    wrapper.setVisibility(View.GONE);
                } else {
                    wrapper.setVisibility(View.VISIBLE);
                    final InputMethodManager imm = (InputMethodManager) ctx
                                                   .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
                }
                optionEnabled = !optionEnabled;
            }
        });
        final ViewSwitcher switcher = (ViewSwitcher) v
                                      .findViewById(R.id.subtask_switcher);
        final Button subtaskNewtask = (Button) v
                                      .findViewById(R.id.subtask_newtask);
        final Button subtaskSelectOld = (Button) v
                                        .findViewById(R.id.subtask_select_old);
        final boolean darkTheme = MirakelCommonPreferences.isDark();
        if (asSubtask) {
            v.findViewById(R.id.subtask_header).setVisibility(View.GONE);
            switcher.showNext();
            newTask = false;
        } else {
            subtaskNewtask.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if (newTask) {
                        return;
                    }
                    switcher.showPrevious();
                    subtaskNewtask.setTextColor(ctx.getResources().getColor(
                                                    darkTheme ? R.color.White : R.color.Black));
                    subtaskSelectOld.setTextColor(ctx.getResources().getColor(
                                                      R.color.Grey));
                    newTask = true;
                }
            });
            subtaskSelectOld.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if (!newTask) {
                        return;
                    }
                    switcher.showNext();
                    subtaskNewtask.setTextColor(ctx.getResources().getColor(
                                                    R.color.Grey));
                    subtaskSelectOld.setTextColor(ctx.getResources().getColor(
                                                      darkTheme ? R.color.White : R.color.Black));
                    if (subtaskAdapter != null) {
                        subtaskAdapter.notifyDataSetChanged();
                    }
                    newTask = false;
                    lv.invalidateViews();
                    updateListView(subtaskAdapter, task, lv, ctx);
                }
            });
        }
        final CheckBox doneBox = (CheckBox) v.findViewById(R.id.subtask_done);
        doneBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView,
                                         final boolean isChecked) {
                done = isChecked;
                updateListView(subtaskAdapter, task, lv, ctx);
            }
        });
        final CheckBox reminderBox = (CheckBox) v
                                     .findViewById(R.id.subtask_reminder);
        reminderBox
        .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(
                final CompoundButton buttonView,
                final boolean isChecked) {
                reminder = isChecked;
                updateListView(subtaskAdapter, task, lv, ctx);
            }
        });
        final Button list = (Button) v.findViewById(R.id.subtask_list);
        list.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                final List<ListMirakel> lists = ListMirakel.all(true);
                final CharSequence[] names = new String[lists.size()];
                for (int i = 0; i < names.length; i++) {
                    names[i] = lists.get(i).getName();
                }
                new AlertDialog.Builder(ctx).setSingleChoiceItems(names, -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                                        final int which) {
                        listId = lists.get(which).getId();
                        updateListView(subtaskAdapter, task, lv, ctx);
                        list.setText(lists.get(which).getName());
                        dialog.dismiss();
                    }
                }).show();
            }
        });
        final CheckBox contentBox = (CheckBox) v
                                    .findViewById(R.id.subtask_content);
        contentBox
        .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(
                final CompoundButton buttonView,
                final boolean isChecked) {
                content = isChecked;
                updateListView(subtaskAdapter, task, lv, ctx);
            }
        });
        final EditText newTaskEdit = (EditText) v
                                     .findViewById(R.id.subtask_add_task_edit);
        final AlertDialog dialog = new AlertDialog.Builder(ctx)
        .setTitle(ctx.getString(R.string.add_subtask))
        .setView(v)
        .setPositiveButton(R.string.add,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                if (newTask
                    && newTaskEdit.getText().length() > 0) {
                    newSubtask(
                        newTaskEdit.getText().toString(),
                        task, ctx);
                } else if (!newTask) {
                    final List<Task> checked = subtaskAdapter
                                               .getSelected();
                    for (final Task t : checked) {
                        if (!asSubtask) {
                            if (!t.hasSubtasksLoop(task)) {
                                task.addSubtask(t);
                            } else {
                                ErrorReporter
                                .report(ErrorType.TASKS_CANNOT_FORM_LOOP);
                            }
                        } else {
                            if (!task.hasSubtasksLoop(t)) {
                                t.addSubtask(task);
                            } else {
                                ErrorReporter
                                .report(ErrorType.TASKS_CANNOT_FORM_LOOP);
                            }
                        }
                    }
                }
                if (taskChanged != null) {
                    taskChanged.onTaskChanged(task);
                }
                ((Activity) ctx)
                .getWindow()
                .setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                dialog.dismiss();
            }
        })
        .setNegativeButton(android.R.string.cancel, dialogDoNothing)
        .show();
        newTaskEdit.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(final TextView v, final int actionId,
                                          final KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    newSubtask(v.getText().toString(), task, ctx);
                    v.setText(null);
                    if (taskChanged != null) {
                        taskChanged.onTaskChanged(task);
                    }
                    dialog.dismiss();
                }
                return false;
            }
        });
    }

    protected static Task newSubtask(final String name, final Task parent,
                                     final Context ctx) {
        final ListMirakel list = MirakelModelPreferences
                                 .getListForSubtask(parent);
        final Task t = Semantic.createTask(name, Optional.fromNullable(list), true, ctx);
        parent.addSubtask(t);
        return t;
    }

    public static void openFile(final Context context, final FileMirakel file) {
        final String mimetype = FileUtils.getMimeType(file.getFileUri());
        final Intent i2 = new Intent();
        i2.setAction(android.content.Intent.ACTION_VIEW);
        i2.setDataAndType(file.getFileUri(), mimetype);
        try {
            context.startActivity(i2);
        } catch (final ActivityNotFoundException e) {
            ErrorReporter.report(ErrorType.FILE_NO_ACTIVITY);
        }
    }

    public static void playbackFile(final Activity context,
                                    final FileMirakel file, final boolean loud) {
        final MediaPlayer mPlayer = new MediaPlayer();
        final AudioManager am = (AudioManager) context
                                .getSystemService(Context.AUDIO_SERVICE);
        if (!loud) {
            am.setSpeakerphoneOn(false);
            am.setMode(AudioManager.MODE_IN_CALL);
            context.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        }
        try {
            mPlayer.reset();
            if (!loud) {
                mPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            }
            mPlayer.setDataSource(file.getFileStream(context).getFD());
            mPlayer.prepare();
            mPlayer.start();
            mPlayer.setOnCompletionListener(new OnCompletionListener() {
                @Override
                public void onCompletion(final MediaPlayer mp) {
                    audio_playback_dialog.dismiss();
                }
            });
            am.setMode(AudioManager.MODE_NORMAL);
            audio_playback_playing = true;
        } catch (final IOException e) {
            Log.e(TAG, "prepare() failed");
        }
        audio_playback_dialog = new AlertDialog.Builder(context)
        .setTitle(R.string.audio_playback_title)
        .setPositiveButton(R.string.audio_playback_pause, null)
        .setNegativeButton(R.string.audio_playback_stop,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog,
                                final int which) {
                mPlayer.release();
            }
        }).setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(final DialogInterface dialog) {
                mPlayer.release();
                dialog.cancel();
            }
        }).create();
        audio_playback_dialog.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                final Button button = ((AlertDialog) dialog)
                                      .getButton(DialogInterface.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        if (audio_playback_playing) {
                            button.setText(R.string.audio_playback_play);
                            mPlayer.pause();
                            audio_playback_playing = false;
                        } else {
                            button.setText(R.string.audio_playback_pause);
                            mPlayer.start();
                            audio_playback_playing = true;
                        }
                    }
                });
            }
        });
        audio_playback_dialog.show();
    }

    protected static void setRecurence(final Task task, final boolean isDue,
                                       final long id, final ExecInterface callback) {
        if (isDue) {
            Recurring.destroyTemporary(task.getRecurrenceId());
            task.setRecurrence(id);
        } else {
            Recurring.destroyTemporary(task.getRecurringReminderId());
            task.setRecurringReminder(id);
        }
        task.save();
        if (callback != null) {
            callback.exec();
        }
    }

    public static void stopRecording() {
        if (audio_record_mRecorder != null) {
            try {
                cancelRecording();
                audio_record_alert_dialog.dismiss();
            } catch (final Exception e) {
                // eat it
            }
        }
    }

    protected static void updateListView(final SubtaskAdapter a, final Task t,
                                         final ListView lv, final Context ctx) {
        if (t == null || a == null || lv == null) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Task> tasks = Task.cursorToTaskList(queryForSubtasks(t, ctx));
                if (tasks == null) {
                    return;
                }
                lv.post(new Runnable() {
                    @Override
                    public void run() {
                        a.changeData(tasks);
                    }
                });
            }
        }).start();
    }

    public static interface OnRecurrenceChange {
        public void handleSingleChange();
        public void handleMultiChange();
    }

    public static void handleChangeRecurringTask(Context context, String title,
            final OnRecurrenceChange onRecurrenceChange) {
        new AlertDialog.Builder(context)
        .setTitle(title)
        .setItems(R.array.recurring_task_select, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                case 0: // Only this recurrence
                    onRecurrenceChange.handleSingleChange();
                    break;
                case 1: // All recurrences
                    onRecurrenceChange.handleMultiChange();
                    break;
                }
            }
        }).show();
    }
}
