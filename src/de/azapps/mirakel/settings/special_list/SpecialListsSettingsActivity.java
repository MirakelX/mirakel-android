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
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SpecialListsSettingsActivity extends PreferenceActivity {
	private static final String TAG = "SpecialListsActivity";

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				"DarkTheme", false))
			setTheme(R.style.AppBaseThemeDARK);
		super.onCreate(savedInstanceState);
		getActionBar().setTitle(R.string.special_lists_title);
	}

	@SuppressLint("NewApi")
	@Override
	public void onBuildHeaders(List<Header> target) {
		List<SpecialList> specialLists = SpecialList.allSpecial();
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
	}

}
