package de.azapps.mirakel.widget.minimal;

import java.util.Random;

import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.widget.MainWidgetProvider;
import de.azapps.mirakel.widget.MainWidgetService;
import de.azapps.mirakel.widget.MainWidgetSettingsActivity;
import de.azapps.mirakelandroid.R;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class MinimalWidgetProvider extends AppWidgetProvider {
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		final int N = appWidgetIds.length;

		// Perform this loop procedure for each App Widget that belongs to this
		// provider
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];
			SharedPreferences preferences = PreferenceManager
					.getDefaultSharedPreferences(context);
			// Get the layout for the App Widget and attach an on-click listener
			// to the button
			Integer listId = MainWidgetProvider.getListId(context, preferences);
			if (listId == null)
				return;
			int listSort = Integer.parseInt(preferences.getString("widgetSort",
					ListMirakel.SORT_BY_OPT + ""));
			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.widget_minimal);
			// Create an Intent to launch SettingsActivity
			Intent settingsIntent = new Intent(context,
					MainWidgetSettingsActivity.class);
			PendingIntent settingsPendingIntent = PendingIntent.getActivity(
					context, 0, settingsIntent, 0);

			views.setOnClickPendingIntent(R.id.widget_preferences,
					settingsPendingIntent);
			Intent intent = new Intent(context, MainWidgetService.class);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			// When intents are compared, the extras are ignored, so we need
			// to
			// embed the extras
			// into the data so that the extras will not be ignored.
			intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
			intent.putExtra(MainWidgetProvider.EXTRA_LISTID, listId);
			intent.putExtra(MainWidgetProvider.EXTRA_LISTSORT, listSort);
			intent.putExtra(MainWidgetProvider.EXTRA_SHOWDONE,
					preferences.getBoolean("widgetDone", false));
			intent.putExtra("Random", new Random().nextInt());
			views.setRemoteAdapter(R.id.widget_tasks_list, intent);
			views.setEmptyView(R.id.widget_tasks_list, R.id.empty_view);
			// views.setOnClickPendingIntent(R.id.button, pendingIntent);
			Intent toastIntent = new Intent(context,
					MinimalWidgetProvider.class);
			intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
			intent.putExtra(MainWidgetProvider.EXTRA_WIDGET_LAYOUT,
					MainWidgetProvider.MINIMAL_WIDGET);
			PendingIntent toastPendingIntent = PendingIntent.getBroadcast(
					context, 1, toastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setPendingIntentTemplate(R.id.widget_tasks_list,
					toastPendingIntent);

			// Tell the AppWidgetManager to perform an update on the current app
			// widget
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}
}
