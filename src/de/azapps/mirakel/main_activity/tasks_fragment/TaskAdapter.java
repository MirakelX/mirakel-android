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

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import de.azapps.mirakel.custom_views.BaseTaskDetailRow.OnTaskChangedListner;
import de.azapps.mirakel.custom_views.TaskSummary;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.Log;

public class TaskAdapter extends CursorAdapter {

	private static final String TAG = "TaskAdapter";
	private OnTaskChangedListner taskChange;

	public TaskAdapter(Context context, Cursor c, boolean autoRequery,OnTaskChangedListner taskChange) {
		super(context, c, autoRequery);
		this.taskChange=taskChange;
	}

	//update view here
	@Override
	public void bindView(View v, Context ctx, Cursor c) {
		if(v==null||! (v instanceof TaskSummary)){
			Log.d(TAG,"create new tasksummary");
			v=new TaskSummary(ctx);
		}
		Task t=Task.cursorToTask(c);
		((TaskSummary)v).updatePart(t);
		v.setTag(t.getId());
		
	}

	//create new views
	@Override
	public View newView(Context ctx, Cursor c, ViewGroup parent) {
		TaskSummary summary=new TaskSummary(ctx);
		summary.setOnTaskChangedListner(this.taskChange);
		return summary;
	}

}
