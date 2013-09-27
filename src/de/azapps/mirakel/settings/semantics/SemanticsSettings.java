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

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakelandroid.R;

public class SemanticsSettings extends ListSettings {
	private static final String TAG = "SemanticSettings";
	private static final int requestCode = 0;
	private Semantic semantic = Semantic.first();
	private List<Semantic> semantics;

	private SemanticsSettingsFragment settingsFragment = new SemanticsSettingsFragment();

	protected void init() {
		updateList();
	}

	protected void updateList() {
		semantics = Semantic.all();
	}

	@Override
	protected List<String> getListContent() {
		List<String> listContent = new ArrayList<String>();
		for (Semantic semantic : semantics) {
			listContent.add(semantic.getCondition());
		}
		return listContent;
	}

	@Override
	protected Fragment getSettingsFragment() {
		return settingsFragment;
	}

	@Override
	protected OnItemClickListener getOnItemClickListener() {
		return new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				Semantic s = semantics.get(position);
				editSemantic(s);
			}
		};
	}

	@Override
	protected void setDefaultItemInFragment() {

		settingsFragment.setSemantic(Semantic.first());
		/*
		 * if (specialList == null)
		 * findViewById(R.id.special_lists_fragment_container).setVisibility(
		 * View.GONE); else
		 * findViewById(R.id.special_lists_fragment_container).setVisibility(
		 * View.VISIBLE);
		 */

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.menu_new_special_list:
			Semantic newSemantic = Semantic.newSemantic("NewSemantic", null,
					null, null);
			editSemantic(newSemantic);
			return true;
		case R.id.menu_delete:
			semantic.destroy();
			// editSList(SpecialList.firstSpecial());
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void editSemantic(final Semantic semantic) {
		if (getResources().getBoolean(R.bool.isTablet)) {
			this.semantic = semantic;
			// update();
			if (semantic == null)
				findViewById(R.id.special_lists_fragment_container)
						.setVisibility(View.GONE);
			else {
				findViewById(R.id.special_lists_fragment_container)
						.setVisibility(View.VISIBLE);
				settingsFragment.setSemantic(semantic);
			}
		} else {
			Intent intent = new Intent(this, SemanticsSettingsActivity.class);
			intent.putExtra(SemanticsSettingsActivity.SEMANTIC_ID,
					semantic.getId());
			startActivityForResult(intent, requestCode);
		}
	}

}
