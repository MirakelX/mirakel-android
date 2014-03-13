/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
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
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.tools.Log;

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

	protected boolean clickOnLast = false;

	private boolean loaded = false;
	private boolean isTablet;

	protected List<Header> mTarget;

	public void clickOnLast() {
		this.clickOnLast = true;
	}

	protected abstract OnClickListener getAddOnClickListener();

	public abstract OnClickListener getDelOnClickListener();

	protected abstract Class<?> getDestClass();

	protected abstract Class<?> getDestFragmentClass();

	public List<Header> getHeader() {
		return this.mTarget;
	}

	protected abstract OnClickListener getHelpOnClickListener();

	protected abstract List<Pair<Integer, String>> getItems();

	protected abstract int getSettingsRessource();

	protected abstract int getTitleRessource();

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public void invalidateHeaders() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			super.invalidateHeaders();
		} else if (!this.loaded) {
			getPreferenceScreen().removeAll();
			setup();
		}
		this.loaded = false;
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		return fragmentName.equals(getDestFragmentClass().getCanonicalName());
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
		if (this.clickOnLast) {
			onHeaderClick(this.mTarget.get(this.mTarget.size() - 1),
					this.mTarget.size() - 1);
			this.clickOnLast = false;
		}
		this.mTarget = target;
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		Locale.setDefault(Helpers.getLocal(this));
		super.onConfigurationChanged(newConfig);
		if (this.isTablet != MirakelCommonPreferences.isTablet()) {
			onCreate(null);
		}
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (MirakelCommonPreferences.isDark()) {
			setTheme(R.style.AppBaseThemeDARK);
		}
		Locale.setDefault(Helpers.getLocal(this));
		super.onCreate(savedInstanceState);
		this.loaded = true;
		this.isTablet = MirakelCommonPreferences.isTablet();
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
			if (MirakelCommonPreferences.isTablet()) {
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
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.CENTER_VERTICAL | DefinitionsHelper.GRAVITY_RIGHT));
			invalidateHeaders();

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

	@SuppressLint("NewApi")
	@Override
	public void onHeaderClick(Header header, int position) {
		super.onHeaderClick(header, position);
	}

	@Override
	public boolean onIsMultiPane() {
		return MirakelCommonPreferences.isTablet();
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

	@Override
	public void onResume() {
		super.onResume();
		invalidateHeaders();
		this.loaded = false;
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

	protected abstract void setupSettings();
}
