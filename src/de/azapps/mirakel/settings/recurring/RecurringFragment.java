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
package de.azapps.mirakel.settings.recurring;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.Gravity;
import android.widget.ImageButton;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.settings.ListSettings;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.Log;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class RecurringFragment extends PreferenceFragment {
	private static final String TAG = "RecurringFragment";
	private Recurring recurring;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "foo");
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_recurring);
		ActionBar actionBar = getActivity().getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		Bundle b = getArguments();
		if (b != null) {
			recurring = Recurring.get(getArguments().getInt("id"));
			((RecurringActivity) getActivity()).setReccuring(recurring);
			actionBar.setTitle(recurring.getLabel());
			if (!MirakelPreferences.isTablet()) {
				ImageButton delSemantic = new ImageButton(getActivity());
				delSemantic
						.setBackgroundResource(android.R.drawable.ic_menu_delete);
				actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
						ActionBar.DISPLAY_SHOW_CUSTOM);
				actionBar.setCustomView(delSemantic,
						new ActionBar.LayoutParams(
								ActionBar.LayoutParams.WRAP_CONTENT,
								ActionBar.LayoutParams.WRAP_CONTENT,
										Gravity.CENTER_VERTICAL
												| Mirakel.GRAVITY_RIGHT));
				delSemantic.setOnClickListener(((ListSettings) getActivity())
						.getDelOnClickListener());
			}
			new RecurringSettings(this, recurring).setup();
		} else {
			Log.d(TAG, "bundle null");
		}
	}

}
