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

package de.azapps.mirakel.settings.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import de.azapps.changelog.Changelog;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.SettingsActivity;

public class AboutSettingsFragment extends PreferenceFragment {

    protected int debugCounter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings_about);

        final Preference dashclock = findPreference("dashclock");
        final Intent startDashclockIntent = new Intent(
            Intent.ACTION_VIEW,
            Uri.parse("http://play.google.com/store/apps/details?id=de.azapps.mirakel.dashclock"));
        dashclock.setIntent(startDashclockIntent);

        final Preference changelog = findPreference("changelog");
        changelog
        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            @SuppressLint("NewApi")
            public boolean onPreferenceClick(
                final Preference preference) {
                final Changelog cl = new Changelog(
                    getActivity());
                cl.showChangelog(Changelog.NO_VERSION);
                return true;
            }
        });

        final Preference credits = findPreference("credits");
        if (credits != null) {
            credits.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ((PreferenceActivity)getActivity()).startPreferenceFragment(new CreditsFragment(), false);
                    return true;
                }
            });
        }
        final Preference contact = findPreference("contact");
        if (contact != null) {
            contact.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    Helpers.contact(getActivity());
                    return false;
                }
            });
        }

        final Preference version = findPreference("version");
        version.setSummary(DefinitionsHelper.VERSIONS_NAME);
        version.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            private Toast toast;

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                if (++debugCounter > 6) {
                    MirakelCommonPreferences.toogleDebugMenu();
                    debugCounter = 0;
                    if (this.toast != null) {
                        this.toast.cancel();
                    }
                    this.toast = Toast
                                 .makeText(
                                     getActivity(),
                                     getActivity()
                                     .getString(
                                         R.string.change_dev_mode,
                                         getActivity()
                                         .getString(MirakelCommonPreferences
                                                    .isEnabledDebugMenu() ? R.string.enabled
                                                    : R.string.disabled)),
                                     Toast.LENGTH_LONG);
                    this.toast.show();
                    ((SettingsActivity) getActivity())
                    .invalidateHeaders();
                } else if (debugCounter > 3
                           || MirakelCommonPreferences.isEnabledDebugMenu()) {
                    if (this.toast != null) {
                        this.toast.cancel();
                    }
                    this.toast = Toast
                                 .makeText(
                                     getActivity(),
                                     getActivity()
                                     .getResources()
                                     .getQuantityString(
                                         R.plurals.dev_toast,
                                         7 - debugCounter,
                                         7 - debugCounter,
                                         getActivity()
                                         .getString(!MirakelCommonPreferences
                                                    .isEnabledDebugMenu() ? R.string.enable
                                                    : R.string.disable)),
                                     Toast.LENGTH_SHORT);
                    this.toast.show();
                }
                return false;
            }
        });
    }
}
