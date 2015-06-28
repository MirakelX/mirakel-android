/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2015 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.settings.fragments;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;

import java.util.List;

import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.AnalyticsWrapperBase;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.SettingsActivity;
import de.azapps.mirakel.settings.custom_views.Settings;
import de.azapps.mirakel.settings.custom_views.SwitchCompatPreference;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelDetailActivity;

public class UISettingsFragment extends MirakelPreferencesFragment<Settings> {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings_gui);

        final List<ListMirakel> lists = ListMirakel.all();
        final CharSequence entryValues[] = new String[lists.size()];
        final CharSequence entries[] = new String[lists.size()];
        int i = 0;
        for (final ListMirakel list : lists) {
            final String id = String.valueOf(list.getId());
            final String name = list.getName();
            entryValues[i] = id;
            entries[i] = name;
            i++;
        }

        final ListPreference language = (ListPreference) findPreference("language");
        setLanguageSummary(language, MirakelCommonPreferences.getLanguage());
        language.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference,
                                              final Object newValue) {
                setLanguageSummary(language, newValue.toString());
                MirakelPreferences.getEditor()
                .putString("language", newValue.toString())
                .commit();
                Helpers.restartApp(getActivity());
                return false;
            }
        });

        final ListPreference startupListPreference = (ListPreference) findPreference("startupList");
        startupListPreference.setEntries(entries);
        startupListPreference.setEntryValues(entryValues);
        startupListPreference.setEnabled(true);



        final Preference semantics = findPreference("semanticNewTaskSettings");
        if (semantics != null) {
            semantics.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    if (getActivity() instanceof SettingsActivity) {
                        ((SettingsActivity) getActivity()).onItemSelected(Settings.TASK_TEMPLATES);
                    } else {
                        ((GenericModelDetailActivity) getActivity()).setFragment(Settings.TASK_TEMPLATES.getFragment());
                    }
                    return true;
                }
            });
        }
        final SwitchCompatPreference analytics = (SwitchCompatPreference) findPreference("useAnalytics");
        if (analytics != null) {
            if (DefinitionsHelper.isFdroid()) {
                getPreferenceScreen().removePreference(analytics);
            } else {
                analytics.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean doTrack = (boolean) newValue;
                        if (!doTrack) {
                            AnalyticsWrapperBase.getWrapper().doNotTrack();
                        }
                        return true;
                    }
                });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsWrapperBase.setScreen(this);
    }

    private void setLanguageSummary(final ListPreference language,
                                    final String current) {
        final String[] keys = getResources().getStringArray(
                                  R.array.language_keys);
        language.setSummary(keys[0]);
        for (int j = 0; j < keys.length; j++) {
            if (current.equals(keys[j])) {
                language.setSummary(getResources()
                                    .getStringArray(R.array.language_values)[j]);
                break;
            }
        }
    }

    @NonNull
    @Override
    public Settings getItem() {
        return Settings.UI;
    }
}
