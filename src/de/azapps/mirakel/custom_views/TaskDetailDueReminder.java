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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fourmob.datetimepicker.date.DatePicker;
import com.fourmob.datetimepicker.date.SupportDatePickerDialog;
import com.google.common.base.Optional;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.model.task.TaskVanishedException;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.tools.Log;

import static com.google.common.base.Optional.of;

public class TaskDetailDueReminder extends BaseTaskDetailRow {
    public enum Type {
        Combined, Due, Reminder
    }

    public static final int MIN_DUE_NEXT_TO_REMINDER_SIZE = 800;

    private static final String TAG = "TaskDetailDueReminder";

    public static void setRecurringImage(final ImageButton image, final long id) {
        image.setImageResource(id == -1 ? android.R.drawable.ic_menu_mylocation
                               : android.R.drawable.ic_menu_rotate);
    }

    private static void setupRecurrenceDrawable(final ImageButton recurrence,
            @NonNull final Optional<Recurring> recurring) {
        int id;
        if (!recurring.isPresent() || recurring.get().getId() == -1) {
            id = android.R.drawable.ic_menu_mylocation;
        } else {
            id = android.R.drawable.ic_menu_rotate;
        }
        recurrence.setImageResource(id);
    }

    private final LinearLayout dueWrapper;
    private final LinearLayout mainWrapper;

    protected boolean mIgnoreTimeSet;

    protected ImageButton recurrenceDue;

    protected final ImageButton recurrenceReminder;

    private final LinearLayout reminderWrapper;

    private TextView taskDue;

    private final TextView taskReminder;

    private Type type;

    private void updateTask() {
        Optional<Task> taskOptional = Task.get(task.getId());
        if (taskOptional.isPresent()) {
            task = taskOptional.get();
        } else {
            throw new TaskVanishedException(task.getId());
        }
    }

    public TaskDetailDueReminder(final Context ctx) {
        super(ctx);
        inflate(ctx, R.layout.due_reminder_row, this);
        this.taskReminder = (TextView) findViewById(R.id.task_reminder);
        this.taskReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                TaskDialogHelpers.handleReminder(
                    (ActionBarActivity) TaskDetailDueReminder.this.context,
                    TaskDetailDueReminder.this.task,
                new OnTaskChangedListner() {
                    @Override
                    public void onTaskChanged(final Task newTask) {
                        save();
                        update(TaskDetailDueReminder.this.task);
                        ReminderAlarm
                        .updateAlarms(TaskDetailDueReminder.this.context);
                    }
                });
            }
        });
        this.recurrenceReminder = (ImageButton) findViewById(R.id.reccuring_reminder);
        this.recurrenceReminder.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                TaskDialogHelpers
                .handleRecurrence(
                    (ActionBarActivity) TaskDetailDueReminder.this.context,
                    TaskDetailDueReminder.this.task, false,
                new ExecInterface() {
                    @Override
                    public void exec() {
                        updateTask();
                        TaskDetailDueReminder
                        .setRecurringImage(
                            TaskDetailDueReminder.this.recurrenceReminder,
                            TaskDetailDueReminder.this.task
                            .getRecurringReminderId());
                        setReminder();
                    }
                });
            }
        });
        this.reminderWrapper = (LinearLayout) findViewById(R.id.wrapper_reminder);
        this.dueWrapper = (LinearLayout) findViewById(R.id.wrapper_due);
        this.mainWrapper = (LinearLayout) findViewById(R.id.wrapper_reminder_due);
        this.taskDue = (TextView) findViewById(R.id.task_due);
        this.recurrenceDue = (ImageButton) findViewById(R.id.reccuring_due);
        this.recurrenceDue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                TaskDialogHelpers
                .handleRecurrence(
                    (ActionBarActivity) TaskDetailDueReminder.this.context,
                    TaskDetailDueReminder.this.task, true,
                new ExecInterface() {
                    @Override
                    public void exec() {
                        updateTask();
                        TaskDetailDueReminder
                        .setRecurringImage(
                            TaskDetailDueReminder.this.recurrenceDue,
                            TaskDetailDueReminder.this.task
                            .getRecurrenceId());
                        setDue();
                    }
                });
            }
        });
        this.taskDue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                TaskDetailDueReminder.this.mIgnoreTimeSet = false;
                final Calendar dueLocal = TaskDetailDueReminder.this.task
                                          .getDue().or(new GregorianCalendar());
                final FragmentManager fm = ((ActionBarActivity) TaskDetailDueReminder.this.context)
                                           .getSupportFragmentManager();
                final SupportDatePickerDialog datePickerDialog = SupportDatePickerDialog
                        .newInstance(
                new DatePicker.OnDateSetListener() {
                    @Override
                    public void onDateSet(final DatePicker dp,
                                          final int year, final int month,
                                          final int day) {
                        if (TaskDetailDueReminder.this.mIgnoreTimeSet) {
                            return;
                        }
                        TaskDetailDueReminder.this.task
                        .setDue(of((Calendar) new GregorianCalendar(
                                       year, month, day)));
                        save();
                        setDue();
                    }

                    @Override
                    public void onNoDateSet() {
                        TaskDetailDueReminder.this.task
                        .setDue(Optional.<Calendar>absent());
                        TaskDetailDueReminder.this.task
                        .setRecurrence(-1);
                        save();
                        setDue();
                        setupRecurrenceDrawable(
                            TaskDetailDueReminder.this.recurrenceDue,
                            null);
                    }
                }, dueLocal.get(Calendar.YEAR), dueLocal
                .get(Calendar.MONTH), dueLocal
                .get(Calendar.DAY_OF_MONTH), false,
                MirakelCommonPreferences.isDark(), true);
                datePickerDialog.show(fm, "datepicker");
            }
        });
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw,
                                 final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        handleMultiline(w);
    }

    private void handleMultiline(final int width) {
        if (this.type == null || this.type != Type.Combined) {
            return;
        }
        setLayoutParams(new LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        if (width < TaskDetailDueReminder.MIN_DUE_NEXT_TO_REMINDER_SIZE) {
            this.mainWrapper.setOrientation(LinearLayout.VERTICAL);
            final android.view.ViewGroup.LayoutParams dueParams = this.dueWrapper
                    .getLayoutParams();
            this.dueWrapper.setLayoutParams(new LayoutParams(dueParams.width,
                                            dueParams.height, 1));
            final android.view.ViewGroup.LayoutParams reminderParams = this.reminderWrapper
                    .getLayoutParams();
            this.reminderWrapper.setLayoutParams(new LayoutParams(
                    reminderParams.width, reminderParams.height, 1));
        } else {
            this.mainWrapper.setOrientation(LinearLayout.HORIZONTAL);
            final android.view.ViewGroup.LayoutParams dueParams = this.dueWrapper
                    .getLayoutParams();
            this.dueWrapper.setLayoutParams(new LayoutParams(dueParams.width,
                                            dueParams.height, 0.33f));
            final android.view.ViewGroup.LayoutParams reminderParams = this.reminderWrapper
                    .getLayoutParams();
            this.reminderWrapper.setLayoutParams(new LayoutParams(
                    reminderParams.width, reminderParams.height, 0.66f));
        }
        this.dueWrapper.invalidate();
        this.reminderWrapper.invalidate();
        setLayoutParams(new LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        invalidate();
    }

    protected void setDue() {
        if (!this.task.getDue().isPresent()) {
            this.taskDue.setText(this.context.getString(R.string.no_date));
            this.taskDue.setTextColor(this.context.getResources().getColor(
                                          BaseTaskDetailRow.inactive_color));
        } else {
            this.taskDue.setText(DateTimeHelper.formatDate(this.context,
                                 this.task.getDue()));
            this.taskDue.setTextColor(TaskHelper.getTaskDueColor(this.context, this.task.getDue(),
                                      this.task.isDone()));
        }
    }

    private void setReminder() {
        if (!this.task.getReminder().isPresent()) {
            this.taskReminder.setText(this.context
                                      .getString(R.string.no_reminder));
            this.taskReminder.setTextColor(this.context.getResources()
                                           .getColor(BaseTaskDetailRow.inactive_color));
        } else {
            Calendar reminder = (Calendar) this.task.getReminder().get().clone();
            final Optional<Recurring> recurringReminder = this.task.getRecurringReminder();
            if (recurringReminder.isPresent()) {
                reminder = recurringReminder.get().addRecurring(of(reminder)).get();
                if (new GregorianCalendar().compareTo(reminder) > 0) {
                    reminder.setTimeInMillis(reminder.getTimeInMillis()
                                             + recurringReminder.get().getInterval());
                }
            }
            this.taskReminder.setText(DateTimeHelper.formatReminder(
                                          this.context, reminder));
            this.taskReminder.setTextColor(this.context.getResources()
                                           .getColor(BaseTaskDetailRow.inactive_color));
        }
    }

    public void setType(final Type t) {
        this.type = t;
        switch (t) {
        case Combined:
            this.dueWrapper.setVisibility(View.VISIBLE);
            this.reminderWrapper.setVisibility(View.VISIBLE);
            break;
        case Due:
            this.dueWrapper.setVisibility(View.VISIBLE);
            this.reminderWrapper.setVisibility(View.GONE);
            break;
        case Reminder:
            this.dueWrapper.setVisibility(View.GONE);
            this.reminderWrapper.setVisibility(View.VISIBLE);
            break;
        default:
            Log.d(TaskDetailDueReminder.TAG, "where are the dragons");
            break;
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void updateView() {
        final Drawable reminder_img = this.context.getResources().getDrawable(
                                          android.R.drawable.ic_menu_recent_history);
        reminder_img.setBounds(0, 1, 42, 42);
        final Drawable dueImg = this.context.getResources().getDrawable(
                                    android.R.drawable.ic_menu_today);
        dueImg.setBounds(0, 1, 42, 42);
        final Configuration config = this.context.getResources()
                                     .getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
            && config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            this.taskReminder.setCompoundDrawables(null, null, reminder_img,
                                                   null);
            this.taskDue.setCompoundDrawables(null, null, dueImg, null);
        } else {
            this.taskReminder.setCompoundDrawables(reminder_img, null, null,
                                                   null);
            this.taskDue.setCompoundDrawables(dueImg, null, null, null);
        }
        setRecurringImage(this.recurrenceDue, this.task.getRecurrenceId());
        setRecurringImage(this.recurrenceReminder,
                          this.task.getRecurringReminderId());
        setupRecurrenceDrawable(this.recurrenceDue, this.task.getRecurring());
        setupRecurrenceDrawable(this.recurrenceReminder,
                                this.task.getRecurringReminder());
        setDue();
        setReminder();
    }

}
