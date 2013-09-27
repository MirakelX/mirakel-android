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

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakelandroid.R;

public class SpecialListsSettings extends ListSettings {

	private static final String TAG = "SpecialListsSettings";
	private static final int requestCode = 0;
	private SpecialList specialList = SpecialList.firstSpecial();
	private List<SpecialList> specialLists;

	private SpecialListSettingsFragment settingsFragment = new SpecialListSettingsFragment();;

	protected void init() {
		specialLists = SpecialList.allSpecial(true);
	}

	protected void updateList() {
		specialLists = SpecialList.allSpecial(true);
	}

	protected Fragment getSettingsFragment() {
		return settingsFragment;
	}

	protected List<String> getListContent() {
		List<String> listContent = new ArrayList<String>();
		for (SpecialList list : specialLists) {
			listContent.add(list.getName());
		}
		return listContent;
	}

	protected OnItemClickListener getOnItemClickListener() {
		return new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				SpecialList sl = specialLists.get(position);
				editSList(sl);
			}
		};
	}

	private void editSList(final SpecialList slist) {
		if (getResources().getBoolean(R.bool.isTablet)) {
			specialList = slist;
			// update();
			if (specialList == null)
				findViewById(R.id.special_lists_fragment_container)
						.setVisibility(View.GONE);
			else {
				findViewById(R.id.special_lists_fragment_container)
						.setVisibility(View.VISIBLE);
				settingsFragment.setSpecialList(slist);
			}
		} else {
			Intent intent = new Intent(this, SpecialListsSettingsActivity.class);
			intent.putExtra(SpecialListsSettingsActivity.SLIST_ID,
					-slist.getId());
			startActivityForResult(intent, requestCode);
		}
	}

	protected void setDefaultItemInFragment() {
		SpecialList specialList = SpecialList.firstSpecial();
		settingsFragment.setSpecialList(specialList);
		if (specialList == null)
			findViewById(R.id.special_lists_fragment_container).setVisibility(
					View.GONE);
		else
			findViewById(R.id.special_lists_fragment_container).setVisibility(
					View.VISIBLE);
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
}
