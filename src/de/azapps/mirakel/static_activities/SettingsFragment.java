/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists Copyright (c) 2013 Anatolij Zelenin, Georg
 * Semmler. This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or any later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.static_activities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.view.ViewGroup;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.PreferencesHelper;
import de.azapps.mirakel.settings.accounts.AccountSettingsActivity;
import de.azapps.mirakel.settings.special_list.SpecialListsSettingsActivity;
import de.azapps.mirakel.settings.taskfragment.TaskFragmentSettingsFragment;
import de.azapps.mirakelandroid.R;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment extends PreferenceFragment {
	private static final String	TAG	= "SettingsFragment";
	private PreferencesHelper	helper;

	// private MainActivity main;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments().getString("type").equals("gui")) {
			addPreferencesFromResource(R.xml.settings_gui);
		} else if (getArguments().getString("type").equals("tasks")) {
			addPreferencesFromResource(R.xml.settings_tasks);
		} else if (getArguments().getString("type").equals("notification")) {
			addPreferencesFromResource(R.xml.settings_notifications);
		} else if (getArguments().getString("type").equals("backup")) {
			addPreferencesFromResource(R.xml.settings_backup);
		} else if (getArguments().getString("type").equals("accounts")) {
			startActivity(new Intent(getActivity(),
					AccountSettingsActivity.class));
			if (!MirakelPreferences.isTablet()) getActivity().finish();
			else {
				addPreferencesFromResource(R.xml.settings_notifications);
			}
		} else if (getArguments().getString("type").equals("misc")) {
			addPreferencesFromResource(R.xml.settings_misc);
		} else if (getArguments().getString("type").equals("about")) {
			addPreferencesFromResource(R.xml.settings_about);
		} else if (getArguments().getString("type").equals("help")) {
			Helpers.openHelp(getActivity());
			getActivity().finish();
		} else if (getArguments().getString("type").equals("donate")) {
			startActivity(new Intent(getActivity(), DonationsActivity.class));
		} else if (getArguments().getString("type").equals("speciallists")) {
			startActivity(new Intent(getActivity(),
					SpecialListsSettingsActivity.class));
			if (!MirakelPreferences.isTablet()) getActivity().finish();
			else {
				addPreferencesFromResource(R.xml.settings_notifications);
			}
		} else {

			Log.wtf(TAG, "unkown prefernce " + getArguments().getString("type"));
		}

		helper = new PreferencesHelper(this);
		helper.setFunctionsApp();
	}

	@SuppressLint("NewApi")
	@Override
	public void onDestroy() {
		if (helper.actionBarSwitch != null) {
			helper.actionBarSwitch.setVisibility(View.GONE);
		}
		super.onDestroy();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "fragment");
		switch (requestCode) {
			case SettingsActivity.NEW_ACCOUNT:
				Preference server = findPreference("syncServer");
				PreferencesHelper.updateSyncText(null, server,
						findPreference("syncFrequency"), getActivity());
				break;
			default:
				Log.d(TAG, "unkown activity result");
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void showTaskFragmentSettings() {
		int id = ((ViewGroup) getView().getParent()).getId();
		FragmentManager fm = getActivity().getFragmentManager();
		TaskFragmentSettingsFragment settings = new TaskFragmentSettingsFragment();
		fm.beginTransaction().replace(id, settings).commit();
		// TODO maybe fix order...

	}

}
