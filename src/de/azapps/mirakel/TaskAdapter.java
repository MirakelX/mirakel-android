/*******************************************************************************
 * Mirakel is an Android App for Managing your ToDo-Lists
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
package de.azapps.mirakel;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.opengl.Visibility;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class TaskAdapter extends ArrayAdapter<Task> {
	Context context;
	int layoutResourceId, listId;
	List<Task> data = null;
	OnClickListener clickCheckbox;
	OnClickListener clickPrio;
	ListsDataSource listsDataSource;

	public TaskAdapter(Context context, int layoutResourceId, List<Task> data,
			OnClickListener clickCheckbox, OnClickListener click_prio,
			int listId) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.data = data;
		this.context = context;
		this.clickCheckbox = clickCheckbox;
		this.clickPrio = click_prio;
		listsDataSource = new ListsDataSource(context);
		this.listId = listId;
	}

	/**
	 * Add a task to the head of the List
	 * 
	 * @param task
	 */
	void addToHead(Task task) {
		data.add(0, task);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		TaskHolder holder = null;

		if (row == null) {
			// Initialize the View
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new TaskHolder();
			holder.taskRowDone = (CheckBox) row
					.findViewById(R.id.tasks_row_done);
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

		Task task = data.get(position);

		// Done
		holder.taskRowDone.setChecked(task.isDone());
		holder.taskRowDone.setOnClickListener(clickCheckbox);
		holder.taskRowDone.setTag(task);
		if (task.getContent().length() != 0 ) {
			holder.taskRowHasContent.setVisibility(View.VISIBLE);
		} else {
			holder.taskRowHasContent.setVisibility(View.GONE);
		}
		if (listId <= 0) {
			holder.taskRowList.setVisibility(View.VISIBLE);
			holder.taskRowList.setText(listsDataSource.getList(
					(int) task.getListId()).getName());
		} else {
			holder.taskRowList.setVisibility(View.GONE);
		}

		// Name
		holder.taskRowName.setText(task.getName());
		int nameColor;
		if (task.isDone()) {
			nameColor = R.color.Grey;
		} else {
			nameColor = R.color.Black;
		}
		holder.taskRowName.setTextColor(row.getResources().getColor(nameColor));

		// Priority
		holder.taskRowPriority.setText("" + task.getPriority());
		holder.taskRowPriority.setBackgroundColor(Mirakel.PRIO_COLOR[task
				.getPriority() + 2]);
		holder.taskRowPriority.setOnClickListener(clickPrio);
		holder.taskRowPriority.setTag(task);

		// Due
		String due = MirakelHelper.formatDate(task.getDue(),
				context.getString(R.string.dateFormat));
		if (due != "") {
			holder.taskRowDue.setVisibility(View.VISIBLE);
			holder.taskRowDue.setText(due);
			holder.taskRowDue.setTextColor(row.getResources()
					.getColor(
							MirakelHelper.getTaskDueColor(task.getDue(),
									task.isDone())));
		} else {
			holder.taskRowDue.setVisibility(View.GONE);
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
		TextView taskRowName;
		TextView taskRowPriority;
		TextView taskRowDue, taskRowList;
		ImageView taskRowHasContent;
	}

}
