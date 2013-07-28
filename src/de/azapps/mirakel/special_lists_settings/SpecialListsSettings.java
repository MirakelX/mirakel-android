package de.azapps.mirakel.special_lists_settings;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakelandroid.R;

public class SpecialListsSettings extends FragmentActivity {

	private static final String TAG = "SpecialListsSettings";
	private static final int requestCode = 0;
	private SpecialListSettingsFragment SettingsFragment;
	private SpecialList specialList;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if(preferences.getBoolean("DarkTheme", false))
			setTheme(R.style.AppBaseThemeDARK);
		setContentView(R.layout.activity_special_lists_settings_a);
		if(getResources().getBoolean(R.bool.isTablet)){
			SettingsFragment=new SpecialListSettingsFragment();
			specialList=SpecialList.firstSpecial();
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

			// Replace whatever is in the fragment_container view with this fragment,
			// and add the transaction to the back stack so the user can navigate back
			transaction.replace(R.id.special_lists_fragment_container, SettingsFragment);
			transaction.addToBackStack(null);
			transaction.commit();
			SettingsFragment.setSpecialList(specialList);
			if(specialList==null)
				findViewById(R.id.special_lists_fragment_container).setVisibility(View.GONE);
			else
				findViewById(R.id.special_lists_fragment_container).setVisibility(View.VISIBLE);
		}
		// Show the Up button in the action bar.
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
			getActionBar().setDisplayHomeAsUpEnabled(true);


		update();
	}
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
	    if (event.getAction() == KeyEvent.ACTION_DOWN) {
	        switch (event.getKeyCode()) {
	            case KeyEvent.KEYCODE_BACK:
	            	if(getResources().getBoolean(R.bool.isTablet)){
	            		finish();
	                 	return true;
	            	}else{
	            		return super.dispatchKeyEvent(event);
	            	}
	        }
	    }
	    return super.dispatchKeyEvent(event);
	}

	private void update() {
		ListView listView = (ListView) findViewById(R.id.special_lists_list);
		final List<SpecialList> slists = SpecialList.allSpecial(true);
		List<String> listContent = new ArrayList<String>();
		for (SpecialList list : slists) {
			listContent.add(list.getName());
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, listContent);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				// TODO Remove Bad Hack
				SpecialList sl = slists.get((int) id);
				editSList(sl);
			}
		});
	}

	private void editSList(final SpecialList slist) {
		if(getResources().getBoolean(R.bool.isTablet)){
			specialList=slist;
			update();
			if(specialList==null)
				findViewById(R.id.special_lists_fragment_container).setVisibility(View.GONE);
			else{
				findViewById(R.id.special_lists_fragment_container).setVisibility(View.VISIBLE);
				SettingsFragment.setSpecialList(slist);
			}
		}else{
			Intent intent = new Intent(this, SpecialListsSettingsActivity.class);
			intent.putExtra(SpecialListsSettingsActivity.SLIST_ID, -slist.getId());
			startActivityForResult(intent, requestCode);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		if(getResources().getBoolean(R.bool.isTablet))
			getMenuInflater().inflate(R.menu.special_list_tablet, menu);
		else
			getMenuInflater().inflate(R.menu.special_lists_settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.menu_new_special_list:
			Log.e(TAG, "new SpecialList");
			SpecialList newList = SpecialList.newSpecialList("NewList", "",
					false);
			editSList(newList);
			return true;
		case R.id.menu_delete:
			specialList.destroy();
			editSList(SpecialList.firstSpecial());
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		update();
	}

}
