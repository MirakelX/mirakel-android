package de.azapps.mirakel;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;

public class HelpActivity extends Activity {
	
	private static final String TAG="HelpActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		SharedPreferences settings=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor=settings.edit();
		editor.putBoolean("showHelp", false);
		editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.help, menu);
		return true;
	}
	public void contact(View v){
		Log.e(TAG,"Implement Contact");
	}
	public void donate(View v){
		Log.e(TAG,"Implement Donate");
	}
	public void ok(View v){
		Intent intent= new Intent(HelpActivity.this, TasksActivity.class);
		startActivity(intent);
	}

}
