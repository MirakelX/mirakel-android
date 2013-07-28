package de.azapps.mirakel.special_lists_settings;

import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakelandroid.R;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

//@SuppressLint("NewApi")
public class SpecialListsSettingsActivity extends ActionBarActivity {
	protected SpecialListSettingsFragment SettingsFragment;
	@SuppressWarnings("unused")
	final static private String TAG="SpecialListsSettingsActivity";
	public static final String SLIST_ID = "de.azapps.mirakel.SpecialListSettings/list_id";
	private SpecialList specialList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("DarkTheme", false))
			setTheme(R.style.AppBaseThemeDARK);
		setContentView(R.layout.activity_special_lists_settings_b);
		SettingsFragment=new SpecialListSettingsFragment();
		specialList=SpecialList.getSpecialList(getIntent().getIntExtra(SLIST_ID, 1));
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

		// Replace whatever is in the fragment_container view with this fragment,
		// and add the transaction to the back stack so the user can navigate back
		transaction.replace(R.id.special_lists_fragment_container, SettingsFragment);
		transaction.addToBackStack(null);
		transaction.commit();
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(specialList.getName());
		SettingsFragment.setSpecialList(specialList);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.special_list_settingsactivity, menu);
		
		return true;
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
	    if (event.getAction() == KeyEvent.ACTION_DOWN) {
	        switch (event.getKeyCode()) {
	            case KeyEvent.KEYCODE_BACK: 
	                 finish();
	                 return true;
	        }
	    }
	    return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:

			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
//			NavUtils.navigateUpFromSameTask(this);
			finish();
			return true;
		case R.id.menu_delete:
			specialList.destroy();
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
