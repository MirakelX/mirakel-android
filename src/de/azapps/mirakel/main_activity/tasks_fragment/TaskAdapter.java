/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
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
package de.azapps.mirakel.main_activity.tasks_fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.todddavies.components.progressbar.ProgressWheel;

import de.azapps.mirakel.adapter.MirakelArrayAdapter;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.TaskHelper;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class TaskAdapter extends MirakelArrayAdapter<Task> {
	@SuppressWarnings("unused")
	private static final String TAG = "TaskAdapter";
	int listId;
	OnClickListener clickCheckbox;
	OnClickListener clickPrio;
	private Map<Long, View> viewsForTasks = new HashMap<Long, View>();
	protected int touchPosition;

	public View getViewForTask(Task task) {
		return viewsForTasks.get(task.getId());
	}

	public TaskAdapter(Context c) {
		// do not call this, only for error-fixing there
		super(c, 0, (List<Task>) new ArrayList<Task>());
	}

	public TaskAdapter(Context context, int layoutResourceId, List<Task> data,
			OnClickListener clickCheckbox, OnClickListener click_prio,
			int listId) {
		super(context, layoutResourceId, data);
		this.clickCheckbox = clickCheckbox;
		this.clickPrio = click_prio;
		this.listId = listId;

	}

	/**
	 * Add a task to the head of the List
	 * 
	 * @param task
	 */
	void addToHead(Task task) {
		data.add(0, task);
		selected.add(false);
	}

	public void changeData(List<Task> tasks, int listId) {
		viewsForTasks.clear();
		this.listId = listId;
		super.changeData(tasks);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		Task task = position >= data.size() ? null : data.get(position);
		if (task == null) {
			task = Task.getDummy(context, ListMirakel.safeFirst(context));
		}
		View row = setupRow(convertView, parent, context, layoutResourceId,
				task, listId <= 0, darkTheme);
		TaskHolder holder = (TaskHolder) row.getTag();
		holder.taskRowPriority.setOnClickListener(clickPrio);
		holder.taskRowDone.setOnClickListener(clickCheckbox);
		holder.taskRowDoneWrapper.setOnClickListener(clickCheckbox);
		viewsForTasks.put(task.getId(), row);
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (selected.get(position)) {
			row.setBackgroundColor(context.getResources().getColor(
					darkTheme ? R.color.highlighted_text_holo_dark
							: R.color.highlighted_text_holo_light));
		} else if (settings.getBoolean("colorize_tasks", true)) {
			if (settings.getBoolean("colorize_tasks_everywhere", false)
					|| ((MainActivity) context).getCurrentList()
							.isSpecialList()) {
				int w = row.getWidth() == 0 ? parent.getWidth() : row
						.getWidth();
				Helpers.setListColorBackground(task.getList(), row, darkTheme,
						w);
			} else {
				row.setBackgroundColor(context.getResources().getColor(
						android.R.color.transparent));
			}
		} else {
			row.setBackgroundColor(context.getResources().getColor(
					android.R.color.transparent));
		}
		row.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				touchPosition = position;
				return false;
			}
		});
		return row;
	}

	public static View setupRow(View convertView, ViewGroup parent,
			Context context, int layoutResourceId, Task task, boolean showList,
			boolean darkTheme) {
		View row = convertView;
		TaskHolder holder;

		if (row == null) {
			// Initialize the View
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new TaskHolder();
			holder.taskRowDone = (CheckBox) row
					.findViewById(R.id.tasks_row_done);
			holder.taskRowDoneWrapper = (RelativeLayout) row
					.findViewById(R.id.tasks_row_done_wrapper);
			holder.taskRowName = (TextView) row
					.findViewById(R.id.tasks_row_name);
			holder.taskRowPriority = (TextView) row
					.findViewById(R.id.tasks_row_priority);
			holder.taskRowDue = (TextView) row.findViewById(R.id.tasks_row_due);
			holder.taskRowHasContent = (ImageView) row
					.findViewById(R.id.tasks_row_has_content);
			holder.taskRowList = (TextView) row
					.findViewById(R.id.tasks_row_list_name);
			holder.taskRowProgress = (ProgressWheel) row
					.findViewById(R.id.tasks_row_progress);
			row.setTag(holder);
		} else {
			holder = (TaskHolder) row.getTag();
		}
		if (task == null)
			return row;
		// Done
		if (task.getProgress() == 0)
			holder.taskRowProgress.setVisibility(View.GONE);
		else
			holder.taskRowProgress.setVisibility(View.VISIBLE);

		holder.taskRowProgress.setProgress(Math.round((360.0f / 100.0f)
				* (float) task.getProgress()));
		holder.taskRowDone.setChecked(task.isDone());
		holder.taskRowDone.setTag(task);
		holder.taskRowDoneWrapper.setTag(task);
		if (task.getContent().length() != 0 || task.getSubtaskCount() > 0
				|| task.getFiles().size() > 0) {
			holder.taskRowHasContent.setVisibility(View.VISIBLE);
		} else {
			holder.taskRowHasContent.setVisibility(View.INVISIBLE);
		}
		if (showList && task != null && task.getList() != null) {
			holder.taskRowList.setVisibility(View.VISIBLE);
			holder.taskRowList.setText(task.getList().getName());
		} else {
			holder.taskRowList.setVisibility(View.GONE);
		}

		// Name
		holder.taskRowName.setText(task.getName());

		if (task.isDone()) {
			holder.taskRowName.setTextColor(row.getResources().getColor(
					R.color.Grey));
		} else {
			holder.taskRowName.setTextColor(row.getResources().getColor(
					darkTheme ? android.R.color.primary_text_dark
							: android.R.color.primary_text_light));
		}

		// Priority
		holder.taskRowPriority.setText("" + task.getPriority());

		GradientDrawable bg = (GradientDrawable) holder.taskRowPriority
				.getBackground();
		bg.setColor(TaskHelper.getPrioColor(task.getPriority(), context));
		holder.taskRowPriority.setTag(task);

		// Due
		if (task.getDue() != null) {
			holder.taskRowDue.setVisibility(View.VISIBLE);
			holder.taskRowDue.setText(DateTimeHelper.formatDate(context,
					task.getDue()));
			holder.taskRowDue.setTextColor(row.getResources().getColor(
					TaskHelper.getTaskDueColor(task.getDue(), task.isDone())));
		} else {
			holder.taskRowDue.setVisibility(View.GONE);
		}
		return row;
	}

	public Task lastTouched() {
		if (touchPosition < data.size())
			return data.get(touchPosition);
		else
			return null;
	}

	/**
	 * The class, holding the Views of the Row
	 * 
	 * @author az
	 * 
	 */
	static class TaskHolder {
		CheckBox taskRowDone;
		RelativeLayout taskRowDoneWrapper;
		ProgressWheel taskRowProgress;
		TextView taskRowName;
		TextView taskRowPriority;
		TextView taskRowDue, taskRowList;
		ImageView taskRowHasContent;
	}

}
