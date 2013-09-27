/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.settings;

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import de.azapps.mirakelandroid.R;

/**
 * This is a generic Activity for showing Lists in the settings (and edit the
 * items of that list)
 * 
 * How to use: Implement the abstract methods and if you need the
 * onOptionsItemSelected()-function
 * 
 * @author az
 * 
 */
public abstract class ListSettings extends ActionBarActivity {
	/**
	 * Returns an array of Strings, which the ListView should display
	 * 
	 * @return
	 */
	protected abstract List<String> getListContent();

	/**
	 * Returns the Fragment with the Settings for one item
	 * 
	 * @return
	 */
	protected abstract Fragment getSettingsFragment();

	/**
	 * Returns an OnItemClickListener with the action which should be done, when
	 * the user clicks on the Item
	 * 
	 * @return
	 */
	protected abstract OnItemClickListener getOnItemClickListener();

	/**
	 * Sets the default Item in the SettingsFragment. This function is only
	 * called if the device is a Tablet.
	 */
	protected abstract void setDefaultItemInFragment();

	/**
	 * This function is in the onCreate called
	 */
	protected void init() {

	}

	/**
	 * Thus function is called when android redraws the ListView
	 */
	protected void updateList() {

	}

	/**
	 * Returns the Layout of the list-activity
	 * 
	 * @return
	 */
	protected int getListLayout() {
		return R.layout.activity_special_lists_settings_a;
	}

	/**
	 * Returns the View of the fragment-container
	 * 
	 * @return
	 */
	protected int getFragmentContainer() {
		return R.id.special_lists_fragment_container;
	}

	/**
	 * Returns the Menu for tablets
	 * 
	 * @return
	 */
	protected int getTabletMenu() {
		return R.menu.special_list_tablet;
	}

	/**
	 * Returns the Menu for Smartphones (of the ListView)
	 * 
	 * @return
	 */
	protected int getDefaultMenu() {
		return R.menu.special_lists_settings;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		init();
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (preferences.getBoolean("DarkTheme", false))
			setTheme(R.style.AppBaseThemeDARK);
		setContentView(getListLayout());
		if (getResources().getBoolean(R.bool.isTablet)) {
			FragmentTransaction transaction = getSupportFragmentManager()
					.beginTransaction();

			// Replace whatever is in the fragment_container view with this
			// fragment,
			// and add the transaction to the back stack so the user can
			// navigate back
			transaction.replace(getFragmentContainer(), getSettingsFragment());
			transaction.addToBackStack(null);
			transaction.commit();
		}
		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		update();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK:
				if (getResources().getBoolean(R.bool.isTablet)) {
					finish();
					return true;
				} else {
					return super.dispatchKeyEvent(event);
				}
			}
		}
		return super.dispatchKeyEvent(event);
	}

	private void update() {
		updateList();
		ListView listView = (ListView) findViewById(R.id.special_lists_list);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, getListContent());
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(getOnItemClickListener());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		if (getResources().getBoolean(R.bool.isTablet))
			getMenuInflater().inflate(getTabletMenu(), menu);
		else
			getMenuInflater().inflate(getDefaultMenu(), menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		update();
	}

}
