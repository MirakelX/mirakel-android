package de.azapps.mirakel;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class StartActivity extends Activity {
	private static final String TAG = "StartActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int version = Mirakel.getReadableDatabase().getVersion();
		if (version != Mirakel.DATABASE_VERSION) {
			Log.v(TAG, "SET DB-VERSION " + Mirakel.DATABASE_VERSION);
			Mirakel.getReadableDatabase().setVersion(Mirakel.DATABASE_VERSION);
		}
		setContentView(R.layout.activity_start);
		Button offline_button = (Button) findViewById(R.id.home_offline);
		Button own_server = (Button) findViewById(R.id.home_own_server);
		Button offical_server = (Button) findViewById(R.id.home_official_server);

		offline_button.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.v(TAG, "Click");
				// generateDummies();
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());
				if (settings.getBoolean("showHelp", true)) {
					Intent intent = new Intent(StartActivity.this,
							HelpActivity.class);
					startActivity(intent);
				} else {
					Intent intent = new Intent(StartActivity.this,
							MainActivity.class);
					intent.putExtra("listId", Mirakel.LIST_ALL);
					startActivity(intent);
				}
			}
		});
		own_server.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.v(TAG, "OWN SERVER");
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());
				if (settings.getBoolean("showHelp", true)) {
					Intent intent = new Intent(StartActivity.this,
							HelpActivity.class);
					startActivity(intent);
				} else {
					Intent intent = new Intent(StartActivity.this,
							LoginActivity.class);
					intent.putExtra("own_server", true);
					startActivity(intent);
				}
			}
		});

		offical_server.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.v(TAG, "OFFICAL SERVER");
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());
				if (settings.getBoolean("showHelp", true)) {
					Intent intent = new Intent(StartActivity.this,
							HelpActivity.class);
					startActivity(intent);
				} else {
					Intent intent = new Intent(StartActivity.this,
							LoginActivity.class);
					intent.putExtra("own_server", false);
					startActivity(intent);
				}
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.start, menu);
		return true;
	}

	public void generateDummies() {
		Mirakel.getWritableDatabase().execSQL(
				"INSERT INTO lists (name) VALUES (?),(?)",
				new String[] { "Foo", "Bar" });
		Mirakel.getWritableDatabase()
				.execSQL(
						"INSERT INTO tasks (list_id,name) VALUES (1,'First task'), (1,'Second'), (2,'another')");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			Intent intent = new Intent(StartActivity.this,
					SettingsActivity.class);
			startActivity(intent);
			break;
		}
		return true;
	}

}
