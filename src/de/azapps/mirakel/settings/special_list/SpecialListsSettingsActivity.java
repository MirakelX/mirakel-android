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
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;


public class SpecialListsSettingsActivity extends PreferenceActivity {
	@SuppressWarnings("unused")
	private static final String TAG = "SpecialListsActivity";
	
	private ImageButton addList;

	private List<Header> mTarget;
	private List<SpecialList> specialLists;
	

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				"DarkTheme", false))
			setTheme(R.style.AppBaseThemeDARK);
		super.onCreate(savedInstanceState);
		ActionBar actionbar = getActionBar();
		actionbar.setTitle(R.string.special_lists_title);
		actionbar.setDisplayHomeAsUpEnabled(true);
		addList=new ImageButton(this);
		addList.setBackgroundResource(android.R.drawable.ic_menu_add);
		actionbar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
				ActionBar.DISPLAY_SHOW_CUSTOM);
		actionbar.setCustomView(addList,new ActionBar.LayoutParams(
				ActionBar.LayoutParams.WRAP_CONTENT,
				ActionBar.LayoutParams.WRAP_CONTENT,
				Gravity.CENTER_VERTICAL | Gravity.RIGHT));
		specialLists= SpecialList.allSpecial(true);
		addList.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				specialLists.add(SpecialList.newSpecialList("NewList", "",
						false));	
				invalidateHeaders();
				onHeaderClick(mTarget.get(mTarget.size()-1), mTarget.size()-1);
			
			}
		});
	}

	@SuppressLint("NewApi")
	@Override
	public void onHeaderClick(Header header, int position) {
	    super.onHeaderClick(header, position);	 
	}
	@Override
	@SuppressLint("NewApi")
	protected void onRestart() {
		super.onRestart();
		specialLists=null;
		invalidateHeaders();
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onBuildHeaders(List<Header> target) {
		if(specialLists==null){
			specialLists= SpecialList.allSpecial(true);
		}
		for (SpecialList s : specialLists) {
			Bundle b=new Bundle();
			b.putInt("id", s.getId());
			Header header = new Header();
			header.fragment = SpecialListsSettingsFragment.class
					.getCanonicalName();
			header.title = s.getName();
			header.fragmentArguments=b;
			header.extras=b;
			target.add(header);
		}
		mTarget=target;
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
		}
		return super.onOptionsItemSelected(item);
	}
	
	public List<Header> getHeader(){
		return mTarget;
	}

}
