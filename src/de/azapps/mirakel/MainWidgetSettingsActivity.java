package de.azapps.mirakel;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class MainWidgetSettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Display the fragment as the main content.
		getFragmentManager()
				.beginTransaction()
				.replace(android.R.id.content, new MainWidgetSettingsFragment())
				.commit();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.e("WIDGET", "updated");
		Intent intent = new Intent(this, MainWidgetProvider.class);
		intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		// Use an array and EXTRA_APPWIDGET_IDS instead of
		// AppWidgetManager.EXTRA_APPWIDGET_ID,
		// since it seems the onUpdate() is only fired on that:
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, Mirakel.widgets);
		sendBroadcast(intent);
	}

}
