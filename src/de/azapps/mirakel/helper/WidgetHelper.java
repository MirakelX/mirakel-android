package de.azapps.mirakel.helper;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class WidgetHelper {
	public static RemoteViews configureItem(RemoteViews rv, Task task,
			Context context, int listId, boolean isMinimal) {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		Intent openIntent = new Intent(context, MainActivity.class);
		openIntent.setAction(MainActivity.SHOW_TASK);
		openIntent.putExtra(MainActivity.EXTRA_ID, task.getId());
		openIntent
				.setData(Uri.parse(openIntent.toUri(Intent.URI_INTENT_SCHEME)));
		PendingIntent pOpenIntent = PendingIntent.getActivity(context, 0,
				openIntent, 0);

		rv.setOnClickPendingIntent(R.id.tasks_row, pOpenIntent);
		rv.setOnClickPendingIntent(R.id.tasks_row_name, pOpenIntent);
		String text=task.getName();
		if(isMinimal){
			if(task.getDue()!=null){
				text+=context.getString(R.string.due_to,Helpers.formatDate(context, task.getDue()));
			}
			rv.setInt(R.id.tasks_row_priority, "setBackgroundColor", 
					Helpers.getPrioColor(task.getPriority(), context));
		}
		rv.setTextViewText(R.id.tasks_row_name,text);
		if (task.isDone()) {
			rv.setTextColor(R.id.tasks_row_name, context.getResources()
					.getColor(R.color.Grey));
		} else {
			rv.setTextColor(
					R.id.tasks_row_name,
					context.getResources()
							.getColor(
									preferences.getBoolean("darkWidget", false) ? R.color.White
											: R.color.Black));
		}
		if (!isMinimal) {
			rv.setTextViewText(R.id.tasks_row_priority, task.getPriority() + "");
			rv.setTextColor(R.id.tasks_row_priority, context.getResources()
					.getColor(R.color.Black));
			GradientDrawable drawable = (GradientDrawable) context
					.getResources().getDrawable(R.drawable.priority_rectangle);
			drawable.setColor(Helpers.getPrioColor(task.getPriority(), context));
			Bitmap bitmap = Bitmap.createBitmap(40, 40, Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			drawable.draw(canvas);
			rv.setImageViewBitmap(R.id.label_bg, bitmap);

			if (listId <= 0) {
				rv.setViewVisibility(R.id.tasks_row_list_name, View.VISIBLE);
				rv.setTextViewText(R.id.tasks_row_list_name, task.getList()
						.getName());
			} else {
				rv.setViewVisibility(R.id.tasks_row_list_name, View.GONE);
			}
			if (task.getContent().length() != 0 || task.getSubtaskCount() > 0
					|| task.getFiles().size() > 0) {
				rv.setViewVisibility(R.id.tasks_row_has_content, View.VISIBLE);
			} else {
				rv.setViewVisibility(R.id.tasks_row_has_content, View.GONE);
			}

			if (task.getDue() != null) {
				rv.setViewVisibility(R.id.tasks_row_due, View.VISIBLE);
				rv.setTextViewText(R.id.tasks_row_due,
						Helpers.formatDate(context, task.getDue()));
				if (!isMinimal) {
					rv.setTextColor(
							R.id.tasks_row_due,
							context.getResources().getColor(
									Helpers.getTaskDueColor(task.getDue(),
											task.isDone())));
				}

			} else {
				rv.setViewVisibility(R.id.tasks_row_due, View.GONE);
			}
		}
		return rv;
	}
}
