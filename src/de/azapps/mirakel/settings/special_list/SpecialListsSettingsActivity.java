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
package de.azapps.mirakel.settings.special_list;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakelandroid.R;

@SuppressLint("NewApi")
public class SpecialListsSettingsActivity extends ListSettings {
	@SuppressWarnings("unused")
	private static final String TAG = "SpecialListsActivity";

	private SpecialList newSpecialList() {
		return SpecialList.newSpecialList(
				getString(R.string.special_lists_new), "", true, this);
	}

	@SuppressLint("NewApi")
	@Override
	protected OnClickListener getAddOnClickListener() {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				newSpecialList();
				clickOnLast();
				invalidateHeaders();
			}
		};
	}

	@Override
	protected int getSettingsRessource() {
		return R.xml.settings_special_list;
	}

	@Override
	protected void setupSettings() {
		specialList = SpecialList.getSpecialList(getIntent().getIntExtra("id",
				SpecialList.firstSpecial().getId())
				* -1);
		try {
			new SpecialListSettings(this, specialList).setup();
		} catch (NoSuchListException e) {
			finish();
		}

	}

	@Override
	protected List<Pair<Integer, String>> getItems() {
		List<SpecialList> specialLists = SpecialList.allSpecial(true);
		List<Pair<Integer, String>> items = new ArrayList<Pair<Integer, String>>();
		for (SpecialList list : specialLists) {
			items.add(new Pair<Integer, String>(list.getId(), list.getName()));
		}
		return items;
	}

	@Override
	protected Class<?> getDestClass() {
		return SpecialListsSettingsActivity.class;
	}

	@Override
	protected Class<?> getDestFragmentClass() {
		return SpecialListsSettingsFragment.class;
	}

	@Override
	protected int getTitleRessource() {
		return R.string.special_lists_title;
	}

	private SpecialList specialList;

	public boolean onCreateOptionsMenu(Menu menu) {
		if (Build.VERSION_CODES.ICE_CREAM_SANDWICH > Build.VERSION.SDK_INT) {
			if (getIntent().hasExtra("id")) {
				menu.add(R.string.delete);
			} else {
				menu.add(R.string.add);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			if (item.getTitle().equals(getString(R.string.delete))) {
				if (specialList != null) {
					specialList.destroy();
				}
				finish();
				return true;
			} else if (item.getTitle().equals(getString(R.string.add))) {
				SpecialList s = newSpecialList();
				Intent intent = new Intent(this,
						SpecialListsSettingsActivity.class);
				intent.putExtra("id", s.getId());
				startActivity(intent);
				return true;
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected OnClickListener getHelpOnClickListener() {
		return new OnClickListener() {

			@Override
			public void onClick(View v) {
				Helpers.openHelp(getApplicationContext(), "special-lists");
			}
		};
	}

	@Override
	public OnClickListener getDelOnClickListener() {
		return new OnClickListener() {

			@Override
			public void onClick(View v) {
				specialList.destroy();
				if (!onIsMultiPane())
					finish();
				else {
					try {
						if (getHeader().size() > 0)
							onHeaderClick(getHeader().get(0), 0);
						invalidateHeaders();
					} catch (Exception e) {
						finish();
					}
				}
			}
		};
	}

	public void setSpecialList(SpecialList specialList) {
		this.specialList = specialList;
	}

}
