package de.azapps.mirakel;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.joda.time.LocalDate;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.RemoteViewsService.RemoteViewsFactory;
import android.widget.Toast;

public class MainWidgetService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new MainWidgetViewsFactory(getApplicationContext(), intent);
	}

}

class MainWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

	private Context mContext;
	private int mAppWidgetId;
	private List<Task> tasks;
	private TasksDataSource tasksDatasource;
	private int listId = 0;

	public MainWidgetViewsFactory(Context context, Intent intent) {
		mContext = context;
		mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		listId = intent.getIntExtra(MainWidgetProvider.EXTRA_LISTID, 0);
	}

	public void onCreate() {
		tasksDatasource = new TasksDataSource(mContext);
		tasksDatasource.open();
		tasks = tasksDatasource.getTasks(listId, Mirakel.SORT_BY_OPT);
	}

	public void onDestroy() {
		// tasksDatasource.close();
	}

	public int getCount() {
		return tasks.size();
	}

	public RemoteViews getViewAt(int position) {
		// Get the data for this position from the content provider
		Task task = tasks.get(position);
		// Return a proper item with the proper day and temperature
		RemoteViews rv = new RemoteViews(mContext.getPackageName(),
				R.layout.row);
		rv.setTextViewText(R.id.tasks_row_name, task.getName());
		if (task.isDone()) {
			rv.setTextColor(R.id.tasks_row_name, mContext.getResources()
					.getColor(R.color.Grey));
		} else {
			rv.setTextColor(R.id.tasks_row_name, mContext.getResources()
					.getColor(R.color.Black));
		}
		rv.setTextViewText(R.id.tasks_row_priority, task.getPriority() + "");
		rv.setInt(R.id.tasks_row_priority, "setBackgroundColor",
				Mirakel.PRIO_COLOR[task.getPriority() + 2]);
		rv.setTextViewText(
				R.id.tasks_row_due,
				task.getDue().compareTo(new GregorianCalendar(1970, 1, 1)) < 0 ? ""
						: new SimpleDateFormat(mContext
								.getString(R.string.dateFormat), Locale
								.getDefault()).format(task.getDue().getTime()));

		LocalDate today = new LocalDate();
		LocalDate nextWeek = new LocalDate().plusDays(7);
		LocalDate due = new LocalDate(task.getDue());
		int cmpr = today.compareTo(due);
		if (task.isDone()) {
			rv.setTextColor(R.id.tasks_row_due, mContext.getResources()
					.getColor(R.color.Grey));
		} else if (cmpr > 0) {
			rv.setTextColor(R.id.tasks_row_due, mContext.getResources()
					.getColor(R.color.Red));
		} else if (cmpr == 0) {
			rv.setTextColor(R.id.tasks_row_due, mContext.getResources()
					.getColor(R.color.Orange));
		} else if (nextWeek.compareTo(due) >= 0) {
			rv.setTextColor(R.id.tasks_row_due, mContext.getResources()
					.getColor(R.color.Yellow));
		} else {
			rv.setTextColor(R.id.tasks_row_due, mContext.getResources()
					.getColor(R.color.Green));
		}
		/*
		 * holder.taskRowDone.setChecked(task.isDone());
		 * holder.taskRowDone.setOnClickListener(clickCheckbox);
		 * 
		 * holder.taskRowPriority.setOnClickListener(clickPrio);
		 * holder.taskRowPriority.setTag(task);
		 */

		// rv.setTextViewText(R.id.tasks_row_name, task.getName());

		// Set the click intent so that we can handle it and show a toast
		// message

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
		// tasksDatasource.open();
		// tasks = tasksDatasource.getTasks(listId);
	}
}