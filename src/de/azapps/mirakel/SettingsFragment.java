/*******************************************************************************
 * Mirakel is an Android App for Managing your ToDo-Lists
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
package de.azapps.mirakel;

import java.util.List;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

public class SettingsFragment extends PreferenceFragment {
	private ListPreference startupListPreference;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Initialize needed Arrays
		ListsDataSource listsDataSource = new ListsDataSource(getActivity());
		List<List_mirakle> lists = listsDataSource.getAllLists();
		CharSequence entryValues[] = new String[lists.size()];
		CharSequence entries[] = new String[lists.size()];
		int i = 0;
		for (List_mirakle list : lists) {
			entryValues[i] = String.valueOf(list.getId());
			entries[i] = list.getName();
			i++;
		}

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		// Notifications List
		ListPreference notificationsListPreference = (ListPreference) findPreference("notificationsList");
		notificationsListPreference.setEntries(entries);
		notificationsListPreference.setEntryValues(entryValues);

		// Startup
		CheckBoxPreference startupAllListPreference = (CheckBoxPreference) findPreference("startupAllLists");
		startupListPreference = (ListPreference) findPreference("startupList");
		if (startupAllListPreference.isChecked()) {
			startupListPreference.setEnabled(false);
		}
		startupAllListPreference
				.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						if((Boolean) newValue) {
							startupListPreference.setEnabled(false);
						} else {
							startupListPreference.setEnabled(true);
						}
						return true;
					}

				});
		startupListPreference.setEntries(entries);
		startupListPreference.setEntryValues(entryValues);
	}

}
