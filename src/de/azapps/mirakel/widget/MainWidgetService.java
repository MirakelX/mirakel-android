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
package de.azapps.mirakel.widget;

import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import de.azapps.mirakel.R;
import de.azapps.mirakel.helper.WidgetHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MainWidgetService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new MainWidgetViewsFactory(getApplicationContext(), intent);
	}

}

class MainWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

	private Context mContext;
	private List<Task> tasks;
	private int listId = 0;
	@SuppressWarnings("unused")
	private int sorting;
	private boolean showDone;

	public MainWidgetViewsFactory(Context context, Intent intent) {
		mContext = context;
		listId = intent.getIntExtra(MainWidgetProvider.EXTRA_LISTID, 0);
		sorting = intent.getIntExtra(MainWidgetProvider.EXTRA_LISTSORT,
				(int) ListMirakel.SORT_BY_OPT);
		showDone = intent.getBooleanExtra(MainWidgetProvider.EXTRA_SHOWDONE,
				false);
	}

	/**
	 * Define and open the DataSources
	 */
	@Override
	public void onCreate() {
		ListMirakel list = ListMirakel.getList(listId);
		tasks = list.tasks(showDone);
	}

	public void onDestroy() {
	}

	public int getCount() {
		return tasks.size();
	}

	public RemoteViews getViewAt(int position) {
		Task task=tasks.get(position);
		// Get The Task
		RemoteViews rv = new RemoteViews(mContext.getPackageName(),
				R.layout.widget_row);

		// Set the Contents of the Row
		rv=WidgetHelper.configureItem(rv, task, mContext, listId);

		// rv.setBoolean(R.id.tasks_row_done, "setChecked", true);
		/*
		 * holder.taskRowDone.setChecked(task.isDone());
		 * holder.taskRowDone.setOnClickListener(clickCheckbox);
		 */

		// Set the Click–Intent
		// We need to do so, because we can not start the Activity directly from
		// the Service

		Bundle extras = new Bundle();
		extras.putInt(MainWidgetProvider.EXTRA_TASKID, (int) task.getId());
		Intent fillInIntent = new Intent(MainWidgetProvider.CLICK_TASK);
		fillInIntent.putExtras(extras);
		rv.setOnClickFillInIntent(R.id.tasks_row, fillInIntent);

		return rv;
	}


	public RemoteViews getLoadingView() {
		// We aren't going to return a default loading view in this sample
		return null;
	}

	public int getViewTypeCount() {
		return 1;
	}

	public int getItemViewType(int position) {
		return 0;
	}

	public long getItemId(int position) {
		return position;
	}

	public boolean hasStableIds() {
		return true;
	}

	public void onDataSetChanged() {
	}
}
