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
package de.azapps.mirakel.settings.special_list;

import java.util.List;

import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakelandroid.R;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class SpecialListsSettingsActivity extends PreferenceActivity {
	@SuppressWarnings("unused")
	private static final String TAG = "SpecialListsActivity";

	private ImageButton addList;

	private List<Header> mTarget;
	private List<SpecialList> specialLists = SpecialList.allSpecial(true);
	private SpecialList specialList;
	private boolean clickOnLast = false;
	private boolean loaded;

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				"DarkTheme", false))
			setTheme(R.style.AppBaseThemeDARK);
		super.onCreate(savedInstanceState);
		loaded=true;
		specialLists = SpecialList.allSpecial(true);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (getIntent().hasExtra("id")) {
				addPreferencesFromResource(R.xml.settings_special_list);
				specialList = SpecialList
						.getSpecialList(getIntent().getIntExtra("id",
								SpecialList.firstSpecial().getId())
								* -1);
				new SpecialListPreferences(this, specialList).setup();
			} else {
				addPreferencesFromResource(R.xml.speciallists_headers_v10);
				setup();
			}

		} else {
			ActionBar actionbar = getActionBar();
			actionbar.setTitle(R.string.special_lists_title);
			actionbar.setDisplayHomeAsUpEnabled(true);
			addList = new ImageButton(this);
			addList.setBackgroundResource(android.R.drawable.ic_menu_add);
			actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
					ActionBar.DISPLAY_SHOW_CUSTOM);
			actionbar.setCustomView(addList, new ActionBar.LayoutParams(
					ActionBar.LayoutParams.WRAP_CONTENT,
					ActionBar.LayoutParams.WRAP_CONTENT,
					Gravity.CENTER_VERTICAL | Gravity.RIGHT));
			addList.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					SpecialList.newSpecialList("NewList", "", false);
					specialLists = SpecialList.allSpecial(true);
					clickOnLast = true;
					invalidateHeaders();

				}
			});
		}
	}
	
	
	public boolean onCreateOptionsMenu(Menu menu){
		if(Build.VERSION_CODES.ICE_CREAM_SANDWICH>Build.VERSION.SDK_INT){
			if(getIntent().hasExtra("id")){
				menu.add(R.string.delete);
			}else{
				menu.add(R.string.add);
			}
			return true;
		}
		return false;		
	}

	@SuppressLint("NewApi")
	@Override
	public void onHeaderClick(Header header, int position) {
		super.onHeaderClick(header, position);
	}

	@SuppressLint("NewApi")
	@Override
	public void onBuildHeaders(List<Header> target) {
		for (SpecialList s : specialLists) {
			Bundle b = new Bundle();
			b.putInt("id", s.getId());
			Header header = new Header();
			header.fragment = SpecialListsSettingsFragment.class
					.getCanonicalName();
			header.title = s.getName();
			header.fragmentArguments = b;
			header.extras = b;
			target.add(header);
		}
		if (clickOnLast) {
			onHeaderClick(mTarget.get(mTarget.size() - 1), mTarget.size() - 1);
			clickOnLast = false;
		}
		mTarget = target;
	}

	@Override
	public boolean isMultiPane() {
		return getResources().getBoolean(R.bool.isTablet);
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
			default:
				if(item.getTitle().equals(getString(R.string.delete))){
					if(specialList!=null){
						specialList.destroy();
					}
					finish();
					return true;
				}else if(item.getTitle().equals(getString(R.string.add))){
					SpecialList s= SpecialList.newSpecialList("NewList", "", false);
					Intent intent = new Intent(this,
							SpecialListsSettingsActivity.class);
					intent.putExtra("id",s.getId());
					startActivity(intent);
					return true;
				}
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("deprecation")
	@Override
	@SuppressLint("NewApi")
	public void onResume() {
		super.onResume();
		specialLists = SpecialList.allSpecial(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			invalidateHeaders();
		}else if(!loaded){
			getPreferenceScreen().removeAll();
			setup();
		}
		loaded=false;
	}

	@SuppressWarnings("deprecation")
	private void setup() {
		for (SpecialList s : specialLists) {
			Preference p = new Preference(this);
			p.setTitle(s.getName());
			p.setKey(s.getId() + "");
			final SpecialListsSettingsActivity that = this;
			p.setOnPreferenceClickListener(new OnPreferenceClickListener() {

				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent(that,
							SpecialListsSettingsActivity.class);
					intent.putExtra("id",
							Integer.parseInt(preference.getKey()));
					that.startActivity(intent);
					return false;
				}
			});
			getPreferenceScreen().addPreference(p);
		}
	}


	public List<Header> getHeader() {
		return mTarget;
	}

}
