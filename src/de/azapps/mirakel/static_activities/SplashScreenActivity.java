package de.azapps.mirakel.static_activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.Window;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.main_activity.MainActivity;

public class SplashScreenActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
	    getActionBar().hide();
		setContentView(R.layout.activity_splash_screen);

	    SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	    if(!preferences.contains("startupAllLists")) {
			Intent intent = new Intent(SplashScreenActivity.this,
					StartActivity.class);
			startActivity(intent);
	    } else {
	    	if(preferences.getBoolean("startupAllLists", false)) {
				Intent intent = new Intent(SplashScreenActivity.this,
						MainActivity.class);
				intent.setAction(MainActivity.SHOW_LISTS);
				startActivity(intent);
	    	} else {
	    		int listId=Integer.parseInt(preferences.getString("startupList", ""+Mirakel.LIST_ALL));
				Intent intent = new Intent(SplashScreenActivity.this,
						MainActivity.class);
				intent.setAction(MainActivity.SHOW_LIST);
				intent.putExtra(MainActivity.EXTRA_ID, listId);
				startActivity(intent);
	    		
	    	}
	    }
	    

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.splash_screen, menu);
		return true;
	}

}
