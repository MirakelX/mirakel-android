package de.azapps.mirakel.widget;

import java.util.List;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.List_mirakle;
import de.azapps.mirakel.model.ListsDataSource;

public class MainWidgetSettingsFragment  extends PreferenceFragment {

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
		addPreferencesFromResource(R.xml.main_widget_preferences);
		

		// Notifications List
		ListPreference notificationsListPreference = (ListPreference) findPreference("widgetList");
		notificationsListPreference.setEntries(entries);
		notificationsListPreference.setEntryValues(entryValues);

	}

}
