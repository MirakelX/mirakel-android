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
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.todddavies.components.progressbar.ProgressWheel;

import de.azapps.mirakel.customviews.R;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.helper.ViewHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.tools.Log;

public class TaskSummary extends TaskDetailSubListBase<Task> implements
		android.view.View.OnClickListener {
	public interface OnTaskClickListner {
		public abstract void onTaskClick(final Task t);
	}

	public interface OnTaskMarkedListner {
		abstract public void markTask(final View v, final Task t,
				final boolean markted);
	}

	private static final String TAG = "TaskSummary";
	private boolean marked;
	private OnTaskMarkedListner markedListner;
	private final ProgressWheel taskProgress;
	private final CheckBox taskRowDone;
	private final RelativeLayout taskRowDoneWrapper;
	private final TextView taskRowDue;
	private final ImageView taskRowHasContent;

	private final TextView taskRowList;
	private final TextView taskRowName;
	private final TextView taskRowPriority;

	public TaskSummary(final Context ctx) {
		super(ctx);
		inflate(ctx, R.layout.task_summary, this);
		this.taskRowDone = (CheckBox) findViewById(R.id.tasks_row_done);
		this.taskRowDoneWrapper = (RelativeLayout) findViewById(R.id.tasks_row_done_wrapper);
		this.taskRowName = (TextView) findViewById(R.id.tasks_row_name);
		this.taskRowPriority = (TextView) findViewById(R.id.tasks_row_priority);
		this.taskRowDue = (TextView) findViewById(R.id.tasks_row_due);
		this.taskRowHasContent = (ImageView) findViewById(R.id.tasks_row_has_content);
		this.taskRowList = (TextView) findViewById(R.id.tasks_row_list_name);
		this.taskProgress = (ProgressWheel) findViewById(R.id.tasks_row_progress);

		this.taskRowDoneWrapper.setOnClickListener(this);

		this.taskRowPriority.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (TaskSummary.this.task != null) {
					TaskDialogHelpers.handlePriority(TaskSummary.this.context,
							TaskSummary.this.task, new ExecInterface() {
								@Override
								public void exec() {
									updatePriority();
									save();
								}
							});
				}

			}
		});
	}

	protected void handleMark() {
		if (this.markedListner != null) {
			this.marked = !this.marked;
			this.markedListner.markTask(this, this.task, this.marked);
		}
	}

	@Override
	public void onClick(final View v) {
		final long id = this.task.toggleDone();
		ReminderAlarm.updateAlarms(TaskSummary.this.context);
		save();
		if (id != this.task.getId()) {
			this.task = Task.get(id);
		}
		TaskSummary.this.taskRowDone.setChecked(this.task.isDone());
		updateName();
		updateProgress();
		updateDue();

	}

	public void setOnTaskClick(final OnTaskClickListner l) {
		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (!TaskSummary.this.markedEnabled) {
					l.onTaskClick(TaskSummary.this.task);
				} else {
					handleMark();
				}
			}
		});
	}

	public void setOnTaskMarked(final OnTaskMarkedListner l) {
		this.markedListner = l;
		setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(final View v) {
				handleMark();
				return true;
			}
		});
	}

	public void setShortMark(final boolean shortMark) {
		Log.w(TAG, "enable shortmark " + shortMark);
		this.markedEnabled = shortMark;
	}

	private void updateDue() {
		if (this.task.getDue() != null) {
			this.taskRowDue.setVisibility(View.VISIBLE);
			this.taskRowDue.setText(DateTimeHelper.formatDate(this.context,
					this.task.getDue()));
			this.taskRowDue.setTextColor(this.context.getResources().getColor(
					TaskHelper.getTaskDueColor(this.task.getDue(),
							this.task.isDone())));
		} else {
			this.taskRowDue.setVisibility(View.GONE);
		}
	}

	private void updateName() {
		if (this.task.isDone()) {
			this.taskRowName.setTextColor(this.context.getResources().getColor(
					R.color.Grey));
		} else {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				this.taskRowName
						.setTextColor(this.context
								.getResources()
								.getColor(
										MirakelCommonPreferences.isDark() ? android.R.color.primary_text_dark
												: android.R.color.primary_text_light));
			}
		}
	}

	@Override
	public void updatePart(final Task newValue) {
		this.task = newValue;
		if (this.task == null) {
			this.task = Task.getDummy(this.context,
					ListMirakel.safeFirst(this.context));
		}
		// Done
		this.taskRowDone.setChecked(this.task.isDone());
		this.taskRowDone.setOnClickListener(this);
		if (this.task.getContent().length() != 0) {
			this.taskRowHasContent.setVisibility(View.VISIBLE);
		} else {
			this.taskRowHasContent.setVisibility(View.INVISIBLE);
		}
		if (this.task.getList() != null) {
			this.taskRowList.setVisibility(View.VISIBLE);
			this.taskRowList.setText(this.task.getList().getName());
		} else {
			this.taskRowList.setVisibility(View.GONE);
		}

		// Name
		this.taskRowName.setText(this.task.getName());

		updateName();

		// Priority
		updatePriority();

		// Progress
		updateProgress();

		// Due
		updateDue();
		updateBackground();
	}

	@Override
	protected void onSizeChanged(final int w, final int h, final int oldw,
			final int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		updateBackground();
	}

	public void updateBackground() {
		if (MirakelCommonPreferences.colorizeTasks()) {
			if (MirakelCommonPreferences.colorizeSubTasks()) {
				final int w = getWidth();
				ViewHelper.setListColorBackground(this.task.getList(), this, w);
			} else {
				setBackgroundColor(this.context.getResources().getColor(
						android.R.color.transparent));
			}
		} else {
			setBackgroundColor(this.context.getResources().getColor(
					android.R.color.transparent));
		}
	}

	protected void updatePriority() {
		this.taskRowPriority.setText("" + this.task.getPriority());
		final GradientDrawable bg = (GradientDrawable) this.taskRowPriority
				.getBackground();
		bg.setColor(TaskHelper.getPrioColor(this.task.getPriority()));
	}

	private void updateProgress() {
		this.taskProgress.setProgress((int) (this.task.getProgress() * 3.7));
		if (this.task.getProgress() > 0
				&& (this.task.getProgress() < 100 || !this.task.isDone())) {
			this.taskProgress.setVisibility(VISIBLE);
		} else {
			this.taskProgress.setVisibility(GONE);
		}
	}

}
