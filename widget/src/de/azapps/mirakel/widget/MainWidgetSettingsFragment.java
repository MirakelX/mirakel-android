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
package de.azapps.mirakel.widget;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;

import de.azapps.mirakel.helper.PreferencesWidgetHelper;
import de.azapps.mirakel.settings.custom_views.Settings;
import de.azapps.mirakel.settings.fragments.MirakelPreferencesFragment;
import de.azapps.tools.Log;

@SuppressLint("NewApi")
public class MainWidgetSettingsFragment extends MirakelPreferencesFragment<Settings> {
    private static final String TAG = "MainWidgetSettingsFragment";
    private int widgetId;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_widget);
        Log.e(TAG, "open settings for:" + this.widgetId);
        new PreferencesWidgetHelper(this).setFunctionsWidget(getActivity(),
                this.widgetId);
    }

    public void setup(final int widgetId) {
        this.widgetId = widgetId;
    }

    @NonNull
    @Override
    public Settings getItem() {
        return Settings.WIDGET;
    }


}
