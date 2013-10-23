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
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import de.azapps.mirakel.helper.WidgetHelper;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MainWidgetService extends RemoteViewsService {

	private static final String TAG = "MainWidgetService";

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		Log.wtf(TAG, "create");
		return new MainWidgetViewsFactory(getApplicationContext(), intent);
	}

}

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class MainWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

	private static final String TAG = "MainWidgetViewsFactory";
	private Context mContext;
	private List<Task> tasks;
	private int widgetId;
	private ListMirakel list;

	public MainWidgetViewsFactory(Context context, Intent intent) {
		if (intent.getIntExtra(MainWidgetProvider.EXTRA_WIDGET_LAYOUT,
				MainWidgetProvider.NORMAL_WIDGET) != MainWidgetProvider.NORMAL_WIDGET) {
			Log.wtf(TAG, "falscher provider");
		}
		mContext = context;
		widgetId = intent.getIntExtra(MainWidgetProvider.EXTRA_WIDGET_ID, 0);
	}

	/**
	 * Define and open the DataSources
	 */
	@Override
	public void onCreate() {
		list = WidgetHelper.getList(mContext, widgetId);
		tasks = list.tasks(WidgetHelper.showDone(mContext, widgetId));
	}

	public void onDestroy() {
	}

	public int getCount() {
		return tasks.size();
	}

	public RemoteViews getViewAt(int position) {
		if (position > tasks.size()) {
			Log.wtf(TAG, "Klick on unkown Task");
			return null;
		}
		Task task = tasks.get(position);
		// Get The Task
		boolean isMinimalistic = WidgetHelper
				.isMinimalistic(mContext, widgetId);
		RemoteViews rv = new RemoteViews(mContext.getPackageName(),
				isMinimalistic ? R.layout.widget_row_minimal
						: R.layout.widget_row);

		// Set the Contents of the Row
		rv = WidgetHelper.configureItem(rv, task, mContext, list.getId(),
				isMinimalistic,widgetId);

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
