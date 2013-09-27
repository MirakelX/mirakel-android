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
package de.azapps.mirakel.settings.semantics;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakelandroid.R;

public class SemanticsSettingsActivity extends ActionBarActivity {

	protected SemanticsSettingsFragment settingsFragment;
	@SuppressWarnings("unused")
	final static private String TAG = "SemanticsSettingsActivity";
	public static final String SEMANTIC_ID = "de.azapps.mirakel.SemanticsSettingsActivity/semantic_id";
	private Semantic semantic;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				"DarkTheme", false))
			setTheme(R.style.AppBaseThemeDARK);
		setContentView(R.layout.activity_special_lists_settings_b);
		settingsFragment = new SemanticsSettingsFragment();
		semantic = Semantic.get(getIntent().getIntExtra(SEMANTIC_ID, 1));
		FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();

		// Replace whatever is in the fragment_container view with this
		// fragment,
		// and add the transaction to the back stack so the user can navigate
		// back
		transaction.replace(R.id.special_lists_fragment_container,
				settingsFragment);
		transaction.addToBackStack(null);
		transaction.commit();

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(semantic.getCondition());
		settingsFragment.setSemantic(semantic);
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
			// NavUtils.navigateUpFromSameTask(this);
			finish();
			return true;
		case R.id.menu_delete:
			semantic.destroy();
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
