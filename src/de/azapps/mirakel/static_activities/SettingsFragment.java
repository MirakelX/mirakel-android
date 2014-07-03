/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists Copyright (c) 2013-2014 Anatolij Zelenin, Georg
 * Semmler. This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or any later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.static_activities;

import java.util.Locale;

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
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.PreferencesAppHelper;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.accounts.AccountSettingsActivity;
import de.azapps.mirakel.settings.special_list.SpecialListsSettingsActivity;
import de.azapps.mirakel.settings.tags.TagsSettingsActivity;
import de.azapps.mirakel.settings.taskfragment.TaskFragmentSettingsFragment;
import de.azapps.tools.Log;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment extends PreferenceFragment {
    private static final String TAG = "SettingsFragment";
    private PreferencesAppHelper helper;

    public SettingsFragment() {
        super();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode,
                                 final Intent data) {
        Log.d(TAG, "fragment");
        switch (requestCode) {
        case SettingsActivity.NEW_ACCOUNT:
            final Preference server = findPreference("syncServer");
            PreferencesAppHelper.updateSyncText(null, server,
                                                findPreference("syncFrequency"), getActivity());
            break;
        default:
            Log.d(TAG, "unkown activity result");
            break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Locale.setDefault(Helpers.getLocal(getActivity()));
        switch (getArguments().getString("type")) {
        case "gui":
            addPreferencesFromResource(R.xml.settings_gui);
            break;
        case "tasks":
            addPreferencesFromResource(R.xml.settings_tasks);
            break;
        case "notification":
            addPreferencesFromResource(R.xml.settings_notifications);
            break;
        case "backup":
            addPreferencesFromResource(R.xml.settings_backup);
            break;
        case "accounts":
            startActivity(new Intent(getActivity(),
                                     AccountSettingsActivity.class));
            if (!MirakelCommonPreferences.isTablet()) {
                getActivity().finish();
            } else {
                addPreferencesFromResource(R.xml.settings_notifications);
            }
            break;
        case "misc":
            addPreferencesFromResource(R.xml.settings_misc);
            break;
        case "about":
            addPreferencesFromResource(R.xml.settings_about);
            break;
        case "help":
            Helpers.openHelp(getActivity());
            getActivity().finish();
            break;
        case "donate":
            startActivityForResult(new Intent(getActivity(),
                                              DonationsActivity.class), SettingsActivity.DONATE);
            if (!MirakelCommonPreferences.isTablet()) {
                getActivity().finish();
            }
            break;
        case "speciallists":
            startActivity(new Intent(getActivity(),
                                     SpecialListsSettingsActivity.class));
            if (!MirakelCommonPreferences.isTablet()) {
                getActivity().finish();
            } else {
                addPreferencesFromResource(R.xml.settings_notifications);
            }
            break;
        case "tag":
            startActivity(new Intent(getActivity(), TagsSettingsActivity.class));
            if (!MirakelCommonPreferences.isTablet()) {
                getActivity().finish();
            } else {
                addPreferencesFromResource(R.xml.settings_notifications);
            }
            break;
        case "dev":
            addPreferencesFromResource(R.xml.settings_dev);
            break;
        default:
            Log.wtf(TAG, "unkown prefernce " + getArguments().getString("type"));
        }
        this.helper = new PreferencesAppHelper(this);
        this.helper.setFunctionsApp();
    }

    @SuppressLint("NewApi")
    @Override
    public void onDestroy() {
        if (this.helper.actionBarSwitch != null) {
            this.helper.actionBarSwitch.setVisibility(View.GONE);
        }
        super.onDestroy();
    }

    public void showTaskFragmentSettings() {
        final int id = ((ViewGroup) getView().getParent()).getId();
        final FragmentManager fm = getActivity().getFragmentManager();
        final TaskFragmentSettingsFragment settings = new TaskFragmentSettingsFragment();
        fm.beginTransaction().replace(id, settings).commit();
        // TODO maybe fix order...
    }

}
