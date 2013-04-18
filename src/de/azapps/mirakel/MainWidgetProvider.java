package de.azapps.mirakel;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

public class MainWidgetProvider extends AppWidgetProvider {
	public static String EXTRA_LISTID = "de.azapps.mirakel.EXTRA_LISTID";
	public static String CLICK_TASK = "de.azapps.mirakel.CLICK_TASK";
	public static String EXTRA_TASKID = "de.azapps.mirakel.EXTRA_TASKID";

	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		final int N = appWidgetIds.length;

		// Perform this loop procedure for each App Widget that belongs to this
		// provider
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];
			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.widget_main);

			// Create an Intent to launch ExampleActivity
			// Intent intent = new Intent(context, MainActivity.class);
			// PendingIntent pendingIntent = PendingIntent.getActivity(context,
			// 0, intent, 0);

			// Get the layout for the App Widget and attach an on-click listener
			// to the button
			// views.setOnClickPendingIntent(R.id.button, pendingIntent);

			// Here we setup the intent which points to the StackViewService
			// which will
			// provide the views for this collection.
			Intent intent = new Intent(context, MainWidgetService.class);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					appWidgetIds[i]);
			// When intents are compared, the extras are ignored, so we need to
			// embed the extras
			// into the data so that the extras will not be ignored.
			int listId = 0;
			ListsDataSource listsDataSource = new ListsDataSource(context);
			intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
			intent.putExtra(EXTRA_LISTID, listId);
			views.setRemoteAdapter(R.id.widget_tasks_list, intent);

			// The empty view is displayed when the collection has no items. It
			// should be a sibling
			// of the collection view.
			views.setEmptyView(R.id.widget_tasks_list, R.id.empty_view);
			views.setTextViewText(R.id.widget_list_name, listsDataSource
					.getList(listId).getName());

			// Here we setup the a pending intent template. Individuals items of
			// a collection
			// cannot setup their own pending intents, instead, the collection
			// as a whole can
			// setup a pending intent template, and the individual items can set
			// a fillInIntent
			// to create unique before on an item to item basis.
			Intent toastIntent = new Intent(context, MainWidgetProvider.class);
			intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
			PendingIntent toastPendingIntent = PendingIntent.getBroadcast(
					context, 0, toastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setPendingIntentTemplate(R.id.widget_tasks_list,
					toastPendingIntent);

			appWidgetManager.updateAppWidget(appWidgetIds[i], views);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(CLICK_TASK)) {
			int taskId = intent.getIntExtra(EXTRA_TASKID, 0);
			Intent startMainIntent = new Intent(context, MainActivity.class);
			startMainIntent.putExtra(MainActivity.EXTRA_TASKID, taskId);
			startMainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(startMainIntent);
		}
		super.onReceive(context, intent);
	}

}
