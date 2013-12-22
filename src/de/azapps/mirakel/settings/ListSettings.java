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

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
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
public abstract class ListSettings extends PreferenceActivity {

	private static final String TAG = "ListSettings";

	protected abstract OnClickListener getAddOnClickListener();

	public abstract OnClickListener getDelOnClickListener();

	protected abstract OnClickListener getHelpOnClickListener();

	protected abstract int getSettingsRessource();

	protected abstract void setupSettings();

	private boolean loaded = false;
	protected boolean		clickOnLast	= false;
	protected List<Header>	mTarget;

	protected abstract List<Pair<Integer, String>> getItems();

	protected abstract Class<?> getDestClass();

	protected abstract Class<?> getDestFragmentClass();

	protected abstract int getTitleRessource();

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (MirakelPreferences.isDark())
			setTheme(R.style.AppBaseThemeDARK);
		super.onCreate(savedInstanceState);
		loaded = true;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (getIntent().hasExtra("id")) {
				addPreferencesFromResource(getSettingsRessource());
				setupSettings();
			} else {
				addPreferencesFromResource(R.xml.settings_headers_v10);
				setup();
			}

		} else {
			ActionBar actionbar = getActionBar();
			actionbar.setTitle(getTitleRessource());
			actionbar.setDisplayHomeAsUpEnabled(true);
			View v;
			ImageButton addList = new ImageButton(this);
			addList.setBackgroundResource(android.R.drawable.ic_menu_add);
			addList.setOnClickListener(getAddOnClickListener());
			if (MirakelPreferences.isTablet()) {
				LinearLayout l = new LinearLayout(this);
				l.setLayoutDirection(LinearLayout.VERTICAL);
				l.addView(addList);
				ImageButton delList = new ImageButton(this);
				delList.setBackgroundResource(android.R.drawable.ic_menu_delete);
				delList.setOnClickListener(getDelOnClickListener());
				l.addView(delList);
				Log.d(TAG, "isTablet");

				v = l;
			} else {
				v = addList;
			}

			actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
					ActionBar.DISPLAY_SHOW_CUSTOM);
			actionbar.setCustomView(v, new ActionBar.LayoutParams(
					ActionBar.LayoutParams.WRAP_CONTENT,
					ActionBar.LayoutParams.WRAP_CONTENT,
					Gravity.CENTER_VERTICAL | Mirakel.GRAVITY_RIGHT));

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			menu.add(R.string.add);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
			default:
				Log.d(TAG, "unknown menuentry");
				break;
		}
		if (item.getTitle() == getString(R.string.add)) {
			getAddOnClickListener().onClick(null);
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("deprecation")
	private void setup() {
		List<Pair<Integer, String>> items = getItems();
		for (Pair<Integer, String> item : items) {
			Preference p = new Preference(this);
			p.setTitle(item.second);
			p.setKey(String.valueOf(item.first));
			final ListSettings that = this;
			p.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent(that, getDestClass());
					intent.putExtra("id", Integer.parseInt(preference.getKey()));
					that.startActivity(intent);
					return false;
				}
			});
			getPreferenceScreen().addPreference(p);
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void onHeaderClick(Header header, int position) {
		super.onHeaderClick(header, position);
	}

	@Override
	public boolean onIsMultiPane() {
		return MirakelPreferences.isTablet();
	}

	@Override
	public void onResume() {
		super.onResume();
		invalidateHeaders();
		loaded = false;
	}

	public List<Header> getHeader() {
		return mTarget;
	}

	@SuppressLint("NewApi")
	@Override
	public void onBuildHeaders(List<Header> target) {
		for (Pair<Integer, String> item : getItems()) {
			Bundle b = new Bundle();
			b.putInt("id", item.first);
			Header header = new Header();
			header.fragment = getDestFragmentClass().getCanonicalName();
			header.title = item.second;
			header.fragmentArguments = b;
			header.extras = b;
			target.add(header);
		}
		if (getItems().size() == 0) {
			Header header = new Header();
			header.title = " ";
			header.fragment = getDestFragmentClass().getCanonicalName();
			target.add(header);
		}
		if (clickOnLast) {
			onHeaderClick(mTarget.get(mTarget.size() - 1), mTarget.size() - 1);
			clickOnLast = false;
		}
		mTarget = target;
	}

	public void clickOnLast() {
		clickOnLast = true;
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public void invalidateHeaders() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			super.invalidateHeaders();
		} else if (!loaded) {
			getPreferenceScreen().removeAll();
			setup();
		}
		loaded = false;
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		// TODO test this if have kitkat
		return true;
	}
}
