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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.azapps.mirakel.settings.custom_views.ChangelogDialog;
import de.azapps.mirakel.DefinitionsHelper;
import de.azapps.mirakel.helper.AnalyticsWrapperBase;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.SettingsActivity;
import de.azapps.mirakel.settings.custom_views.Settings;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelDetailActivity;

public class AboutSettingsFragment extends MirakelPreferencesFragment<Settings> {

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

                ChangelogDialog.show(getActivity(), DefinitionsHelper.APK_NAME);
                return true;
            }
        });

        final Preference credits = findPreference("credits");
        if (credits != null) {
            credits.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (getActivity() instanceof SettingsActivity) {
                        ((SettingsActivity) getActivity()).onItemSelected(Settings.CREDITS);
                    } else {
                        ((GenericModelDetailActivity) getActivity()).setFragment(Settings.CREDITS.getFragment());
                    }
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
        String buildDate;
        try {
            final ApplicationInfo ai = getActivity().getPackageManager().getApplicationInfo(
                                           getActivity().getPackageName(), 0);
            final ZipFile zf = new ZipFile(ai.sourceDir);
            final ZipEntry ze = zf.getEntry("META-INF/MANIFEST.MF");
            buildDate = DateFormat.getDateTimeInstance().format(new Date((ze.getTime())));
            zf.close();
        } catch (final Exception ignored) {
            buildDate = "unknown";
        }
        version.setSummary(getString(R.string.version_string, DefinitionsHelper.VERSIONS_NAME, buildDate));
        version.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            private Toast toast;

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                if (++debugCounter > 6) {
                    MirakelCommonPreferences.toogleDebugMenu();
                    if (MirakelCommonPreferences.isDebug()) {
                        AnalyticsWrapperBase.track(AnalyticsWrapperBase.ACTION.ACTIVATED_DEVELOPMENT_SETTINGS);
                    }
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
                    if (getActivity() instanceof SettingsActivity) {
                        ((SettingsActivity) getActivity()).reloadSettings();
                    }
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

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsWrapperBase.setScreen(this);
    }

    @NonNull
    @Override
    public Settings getItem() {
        return Settings.ABOUT;
    }
}
