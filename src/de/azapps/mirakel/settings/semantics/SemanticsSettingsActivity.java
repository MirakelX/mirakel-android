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

import java.util.List;

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
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakelandroid.R;

public class SemanticsSettingsActivity extends PreferenceActivity {
	@SuppressWarnings("unused")
	private static final String TAG = "SpecialListsActivity";
	private ImageButton addSemantic;
	private List<Semantic> semantics = Semantic.all();;
	private List<Header> mTarget;
	private boolean clickOnLast = false;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				"DarkTheme", false))
			setTheme(R.style.AppBaseThemeDARK);
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getActionBar();
		actionBar.setTitle(R.string.settings_semantics_title);
		actionBar.setDisplayHomeAsUpEnabled(true);

		addSemantic = new ImageButton(this);
		addSemantic.setBackgroundResource(android.R.drawable.ic_menu_add);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
				ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setCustomView(addSemantic, new ActionBar.LayoutParams(
				ActionBar.LayoutParams.WRAP_CONTENT,
				ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL
						| Gravity.RIGHT));
		addSemantic.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				semantics.add(Semantic.newSemantic(
						getString(R.string.semantic_new), null, null, null));
				clickOnLast = true;
				invalidateHeaders();

			}
		});

	}

	@Override
	@SuppressLint("NewApi")
	public void onResume() {
		super.onResume();
		semantics = Semantic.all();
		invalidateHeaders();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressLint("NewApi")
	@Override
	public void onBuildHeaders(List<Header> target) {
		for (Semantic s : semantics) {
			Bundle b = new Bundle();
			b.putInt("id", s.getId());
			Header header = new Header();
			header.fragment = SemanticsSettingsFragment.class
					.getCanonicalName();
			header.title = s.getCondition();
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

}