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
package de.azapps.mirakel.main_activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class TaskAdapter extends ArrayAdapter<Task> {
	private static final String TAG = "TaskAdapter";
	MainActivity main;
	int layoutResourceId, listId;
	List<Task> data = null;
	OnClickListener clickCheckbox;
	OnClickListener clickPrio;
	boolean darkTheme;
	private Map<Long, View> viewsForTasks = new HashMap<Long, View>();
	private List<Boolean> selected;
	private int selecdetCount;

	public View getViewForTask(Task task) {
		return viewsForTasks.get(task.getId());
	}

	public List<Task> getSelected() {
		List<Task> ret = new ArrayList<Task>();
		for (int i = 0; i < data.size(); i++) {
			if (selected.get(i)) {
				ret.add(data.get(i));
			}
		}
		return ret;
	}

	public TaskAdapter(MainActivity context, int layoutResourceId,
			List<Task> data, OnClickListener clickCheckbox,
			OnClickListener click_prio, int listId, boolean darkTheme) {
		super(context, layoutResourceId, data);
		Log.d(TAG, "created");
		this.layoutResourceId = layoutResourceId;
		this.data = data;
		this.main = context;
		this.clickCheckbox = clickCheckbox;
		this.clickPrio = click_prio;
		this.listId = listId;
		this.darkTheme = darkTheme;
		selected = new ArrayList<Boolean>();
		for (int i = 0; i < data.size(); i++) {
			selected.add(false);
		}
		selecdetCount = 0;
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

	void changeData(List<Task> tasks, int listId) {
		viewsForTasks.clear();
		data.clear();
		data.addAll(tasks);
		this.listId = listId;
		while (data.size() > selected.size()) {
			selected.add(false);
		}
		selecdetCount=data.size();
	}

	public void setSelected(int position, boolean selected) {
		this.selected.set(position, selected);
		notifyDataSetChanged();
		selecdetCount += (selected ? 1 : -1);
	}

	public int getSelectedCount() {
		return selecdetCount;
	}

	public void resetSelected() {
		for (int i = 0; i < selected.size(); i++) {
			selected.set(i, false);
		}
		notifyDataSetChanged();
		selecdetCount = 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		TaskHolder holder = null;

		if (row == null) {
			// Initialize the View
			LayoutInflater inflater = ((Activity) main).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new TaskHolder();
			holder.taskRowDone = (CheckBox) row
					.findViewById(R.id.tasks_row_done);
			holder.taskRowDoneWrapper = (LinearLayout) row
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

			row.setTag(holder);
		} else {
			holder = (TaskHolder) row.getTag();
		}
		if (selected.get(position)) {
			row.setBackgroundColor(main.getResources().getColor(
					darkTheme ? R.color.highlighted_text_holo_dark
							: R.color.highlighted_text_holo_light));
		} else {
			row.setBackgroundColor(main.getResources().getColor(
					android.R.color.transparent));
		}
		if (position >= data.size())
			return row;
		Task task = data.get(position);

		// Done
		holder.taskRowDone.setChecked(task.isDone());
		holder.taskRowDone.setOnClickListener(clickCheckbox);
		holder.taskRowDone.setTag(task);
		holder.taskRowDoneWrapper.setOnClickListener(clickCheckbox);
		holder.taskRowDoneWrapper.setTag(task);
		if (task.getContent().length() != 0) {
			holder.taskRowHasContent.setVisibility(View.VISIBLE);
		} else {
			holder.taskRowHasContent.setVisibility(View.INVISIBLE);
		}
		if (listId <= 0 && task != null && task.getList() != null) {
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
		// Log.e("Blubb",holder.taskRowPriority.getBackground().getClass().toString());

		GradientDrawable bg = (GradientDrawable) holder.taskRowPriority
				.getBackground();
		bg.setColor(Mirakel.PRIO_COLOR[task.getPriority() + 2]);
		holder.taskRowPriority.setOnClickListener(clickPrio);
		holder.taskRowPriority.setTag(task);

		// Due
		if (task.getDue() != null) {
			holder.taskRowDue.setVisibility(View.VISIBLE);
			holder.taskRowDue.setText(Helpers.formatDate(task.getDue(),
					main.getString(R.string.dateFormat)));
			holder.taskRowDue.setTextColor(row.getResources().getColor(
					Helpers.getTaskDueColor(task.getDue(), task.isDone())));
		} else {
			holder.taskRowDue.setVisibility(View.GONE);
		}
		viewsForTasks.put(task.getId(), row);
		if (main.getPreferences().getBoolean("colorize_tasks", true)) {
			if (main.getPreferences().getBoolean("colorize_tasks_everywhere",
					false)
					|| main.getCurrentList().isSpecialList()) {
				Helpers.setListColorBackground(task.getList(), row, darkTheme);
			}
		}
		return row;
	}

	/**
	 * The class, holding the Views of the Row
	 * 
	 * @author az
	 * 
	 */
	static class TaskHolder {
		CheckBox taskRowDone;
		LinearLayout taskRowDoneWrapper;
		TextView taskRowName;
		TextView taskRowPriority;
		TextView taskRowDue, taskRowList;
		ImageView taskRowHasContent;
	}

}
