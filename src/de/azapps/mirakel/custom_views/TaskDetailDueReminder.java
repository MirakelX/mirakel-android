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

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.FragmentManager;
import android.view.Display;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fourmob.datetimepicker.date.DatePicker;
import com.fourmob.datetimepicker.date.DatePickerDialog;

import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.Log;

public class TaskDetailDueReminder extends BaseTaskDetailRow {
	public enum Type {
		Combined, Due, Reminder;
	}

	public static final int		MIN_DUE_NEXT_TO_REMINDER_SIZE	= 800;

	private static final String	TAG								= "TaskDetailDueReminder";

	public static void setRecurringImage(ImageButton image, int id) {
		image.setImageResource(id == -1 ? android.R.drawable.ic_menu_mylocation
				: android.R.drawable.ic_menu_rotate);
	}
	private static void setupRecurrenceDrawable(ImageButton reccurence, Recurring recurring) {
		int id;
		if (recurring == null || recurring.getId() == -1) {
			id = android.R.drawable.ic_menu_mylocation;
		} else {
			id = android.R.drawable.ic_menu_rotate;
		}
		reccurence.setImageResource(id);
	}
	private final LinearLayout	dueWrapper;
	private final LinearLayout	mainWrapper;

	protected boolean	mIgnoreTimeSet;

	private ImageButton			recurrenceDue;

	private final ImageButton	recurrenceReminder;

	private final LinearLayout	reminderWrapper;

	private TextView			taskDue;

	private final TextView		taskReminder;

	private Type				type;

	public TaskDetailDueReminder(Context ctx) {
		super(ctx);
		inflate(ctx, R.layout.due_reminder_row, this);
		this.taskReminder = (TextView) findViewById(R.id.task_reminder);
		this.taskReminder.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				TaskDialogHelpers.handleReminder((Activity) TaskDetailDueReminder.this.context, TaskDetailDueReminder.this.task,
						new OnTaskChangedListner() {

					@Override
					public void onTaskChanged(Task newTask) {
						save();
						update(TaskDetailDueReminder.this.task);
						ReminderAlarm.updateAlarms(TaskDetailDueReminder.this.context);

					}
				});
			}
		});
		this.recurrenceReminder = (ImageButton) findViewById(R.id.reccuring_reminder);
		this.recurrenceReminder.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				TaskDialogHelpers.handleRecurrence(
						(Activity) TaskDetailDueReminder.this.context,
						TaskDetailDueReminder.this.task, false,
						TaskDetailDueReminder.this.recurrenceReminder);

			}
		});
		this.reminderWrapper = (LinearLayout) findViewById(R.id.wrapper_reminder);
		this.dueWrapper = (LinearLayout) findViewById(R.id.wrapper_due);
		this.mainWrapper = (LinearLayout) findViewById(R.id.wrapper_reminder_due);

		this.taskDue = (TextView) findViewById(R.id.task_due);
		this.recurrenceDue = (ImageButton) findViewById(R.id.reccuring_due);
		this.recurrenceDue.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				TaskDialogHelpers.handleRecurrence(
						(Activity) TaskDetailDueReminder.this.context,
						TaskDetailDueReminder.this.task, true,
						TaskDetailDueReminder.this.recurrenceDue);
			}
		});
		this.taskDue.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				TaskDetailDueReminder.this.mIgnoreTimeSet = false;
				final Calendar dueLocal = TaskDetailDueReminder.this.task
						.getDue() == null ? new GregorianCalendar()
				: TaskDetailDueReminder.this.task.getDue();
						final FragmentManager fm = ((MainActivity) TaskDetailDueReminder.this.context)
								.getSupportFragmentManager();
						final DatePickerDialog datePickerDialog = DatePickerDialog
								.newInstance(
										new DatePicker.OnDateSetListener() {

											@Override
											public void onDateSet(DatePicker dp, int year, int month, int day) {
												if (TaskDetailDueReminder.this.mIgnoreTimeSet)
													return;
												TaskDetailDueReminder.this.task
												.setDue(new GregorianCalendar(
														year, month, day));
												save();
												setDue();

											}

											@Override
											public void onNoDateSet() {
												TaskDetailDueReminder.this.task
												.setDue(null);
												save();
												setDue();

											}
										}, dueLocal.get(Calendar.YEAR), dueLocal
										.get(Calendar.MONTH), dueLocal
										.get(Calendar.DAY_OF_MONTH), false,
										MirakelPreferences.isDark(), true);
						// datePickerDialog.setYearRange(2005, 2036);// must be <
						// 2037
						// TODO fix this(its a int->long problem somewhere;))
						datePickerDialog.show(fm, "datepicker");
			}
		});
	}

	private void handleMultiline() {
		if (this.type == null || this.type != Type.Combined) return;
		Display display = ((Activity) this.context).getWindowManager()
				.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		if (size.x < MIN_DUE_NEXT_TO_REMINDER_SIZE) {
			this.mainWrapper.setOrientation(VERTICAL);
			android.view.ViewGroup.LayoutParams dueParams = this.dueWrapper
					.getLayoutParams();
			this.dueWrapper.setLayoutParams(new LayoutParams(dueParams.width,
					dueParams.height, 1));
			android.view.ViewGroup.LayoutParams reminderParams = this.reminderWrapper
					.getLayoutParams();
			this.reminderWrapper.setLayoutParams(new LayoutParams(
					reminderParams.width, reminderParams.height, 1));
		}else{
			this.mainWrapper.setOrientation(HORIZONTAL);
			android.view.ViewGroup.LayoutParams dueParams = this.dueWrapper
					.getLayoutParams();
			this.dueWrapper.setLayoutParams(new LayoutParams(dueParams.width,
					dueParams.height, 0.33f));
			android.view.ViewGroup.LayoutParams reminderParams = this.reminderWrapper
					.getLayoutParams();
			this.reminderWrapper.setLayoutParams(new LayoutParams(
					reminderParams.width, reminderParams.height, 0.66f));
		}

	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		handleMultiline();
	}

	private void setDue() {
		if (this.task.getDue() == null) {
			this.taskDue.setText(this.context.getString(R.string.no_date));
			this.taskDue.setTextColor(this.context.getResources().getColor(
					inactive_color));
		} else {
			this.taskDue.setText(DateTimeHelper.formatDate(this.context,
					this.task.getDue()));
			this.taskDue.setTextColor(this.context.getResources().getColor(
					TaskHelper.getTaskDueColor(this.task.getDue(),
							this.task.isDone())));
		}
	}



	public void setType(Type t) {
		this.type = t;
		switch (t) {
			case Combined:
				this.dueWrapper.setVisibility(VISIBLE);
				this.reminderWrapper.setVisibility(VISIBLE);
				handleMultiline();
				break;
			case Due:
				this.dueWrapper.setVisibility(VISIBLE);
				this.reminderWrapper.setVisibility(GONE);
				break;
			case Reminder:
				this.dueWrapper.setVisibility(GONE);
				this.reminderWrapper.setVisibility(VISIBLE);
				break;
			default:
				Log.d(TAG, "where are the dragons");
				break;
		}
	}
	@SuppressLint("NewApi")
	@Override
	protected void updateView() {
		Drawable reminder_img = this.context.getResources().getDrawable(
				android.R.drawable.ic_menu_recent_history);
		reminder_img.setBounds(0, 1, 42, 42);
		Drawable dueImg = this.context.getResources().getDrawable(
				android.R.drawable.ic_menu_today);
		dueImg.setBounds(0, 1, 42, 42);
		Configuration config = this.context.getResources().getConfiguration();

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
		if (this.task.getReminder() == null) {
			this.taskReminder.setText(this.context
					.getString(R.string.no_reminder));
			this.taskReminder.setTextColor(this.context.getResources()
					.getColor(inactive_color));
		} else {
			this.taskReminder.setText(DateTimeHelper.formatDate(
					this.task.getReminder(),
					this.context.getString(R.string.humanDateTimeFormat)));
			this.taskReminder.setTextColor(this.context.getResources()
					.getColor(inactive_color));
		}
		handleMultiline();

	}

}
