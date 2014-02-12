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
package de.azapps.mirakel.main_activity.tasks_fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import de.azapps.mirakel.adapter.MirakelArrayAdapter;
import de.azapps.mirakel.custom_views.BaseTaskDetailRow.OnTaskChangedListner;
import de.azapps.mirakel.custom_views.TaskSummary;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.model.task.Task;


public class TaskAdapter extends MirakelArrayAdapter<Task> {
	/**
	 * The class, holding the Views of the Row
	 * 
	 * @author az
	 * 
	 */
	static class TaskHolder {
		TaskSummary	summary;
	}


	int listId;

	private OnTaskChangedListner	onTaskChanged;

	protected int touchPosition;

	private final Map<Long, View>	viewsForTasks	= new HashMap<Long, View>();

	public TaskAdapter(Context c) {
		// do not call this, only for error-fixing there
		super(c, 0, new ArrayList<Task>());
	}

	public TaskAdapter(Context context, int layoutResourceId, List<Task> data,
			int listId, OnTaskChangedListner onTaskChanged) {
		super(context, layoutResourceId, data);
		this.listId = listId;
		this.onTaskChanged = onTaskChanged;

	}

	public void changeData(List<Task> tasks, int listID) {
		this.viewsForTasks.clear();
		this.listId = listID;
		super.changeData(tasks);
	}

	public int getListID() {
		return this.listId;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final Task task = position >= this.data.size() ? null : this.data
				.get(position);
		TaskSummary row;
		if (convertView == null) {
			row = new TaskSummary(this.context);
		} else {
			row = (TaskSummary) convertView;
		}

		row.updatePart(task);

		if (this.selected.get(position)) {
			row.setBackgroundColor(Helpers
					.getHighlightedColor(this.context));
		}
		row.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				TaskAdapter.this.touchPosition = position;
				return false;
			}
		});
		row.setOnTaskChangedListner(this.onTaskChanged);
		if(task!=null) {
			this.viewsForTasks.put(task.getId(), row);
		}
		return row;
	}

	public View getViewForTask(Task task) {
		return this.viewsForTasks.get(task.getId());
	}

	public Task lastTouched() {
		if (this.touchPosition < this.data.size())
			return this.data.get(this.touchPosition);
		return null;
	}

	public void setListID(int listId) {
		this.listId = listId;
	}

}
