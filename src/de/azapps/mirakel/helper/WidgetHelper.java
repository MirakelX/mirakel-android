package de.azapps.mirakel.helper;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.task.Task;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

public class WidgetHelper {
	public static RemoteViews configureItem(RemoteViews rv, Task task,Context mContext,int listId){
		Intent openIntent = new Intent(mContext, MainActivity.class);
		openIntent.setAction(MainActivity.SHOW_TASK);
		openIntent.putExtra(MainActivity.EXTRA_ID, task.getId());
		openIntent
				.setData(Uri.parse(openIntent.toUri(Intent.URI_INTENT_SCHEME)));
		openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pOpenIntent = PendingIntent.getActivity(mContext, 0,
				openIntent, 0);
		
		
		rv.setOnClickPendingIntent(R.id.tasks_row, pOpenIntent);
		rv.setOnClickPendingIntent(R.id.tasks_row_name, pOpenIntent);
		
		
		rv.setTextViewText(R.id.tasks_row_name, task.getName());
		if (task.isDone()) {
			rv.setTextColor(R.id.tasks_row_name, mContext.getResources()
					.getColor(R.color.Grey));
		} else {
			rv.setTextColor(R.id.tasks_row_name, mContext.getResources()
					.getColor(R.color.Black));
		}
		rv.setTextViewText(R.id.tasks_row_priority, task.getPriority() + "");
		rv.setTextColor(R.id.tasks_row_priority, mContext.getResources()
				.getColor(R.color.Black));
		rv.setInt(R.id.tasks_row_priority, "setBackgroundColor",
				Mirakel.PRIO_COLOR[task.getPriority() + 2]);

		if (listId <= 0) {
			rv.setViewVisibility(R.id.tasks_row_list_name, View.VISIBLE);
			rv.setTextViewText(R.id.tasks_row_list_name, task.getList()
					.getName());
		} else {
			rv.setViewVisibility(R.id.tasks_row_list_name, View.GONE);
		}

		if (task.getDue() != null) {
			rv.setViewVisibility(R.id.tasks_row_due, View.VISIBLE);
			rv.setTextViewText(
					R.id.tasks_row_due,
					Helpers.formatDate(task.getDue(),
							mContext.getString(R.string.dateFormat)));
			rv.setTextColor(
					R.id.tasks_row_due,
					mContext.getResources().getColor(
							Helpers.getTaskDueColor(task.getDue(),
									task.isDone())));
		} else {
			rv.setViewVisibility(R.id.tasks_row_due, View.GONE);
		}

		if (task.getContent().length() != 0) {
			rv.setViewVisibility(R.id.tasks_row_has_content, View.VISIBLE);
		} else {
			rv.setViewVisibility(R.id.tasks_row_has_content, View.GONE);
		}
		return rv;
	}
}
