/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
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

package de.azapps.mirakel.settings;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.google.common.base.Optional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.azapps.mirakel.ThemeManager;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.settings.model_settings.generic_list.GenericModelListActivity;


public class SettingsActivity extends GenericModelListActivity<Settings> {

    private static final int DEV_SETTINGS_POSITION = 6;

    public static final int DONATE = 5;
    public static final int FILE_ASTRID = 0, FILE_IMPORT_DB = 1,
                            NEW_ACCOUNT = 2, FILE_ANY_DO = 3, FILE_WUNDERLIST = 4;
    private static final String TAG = "SettingsActivity";

    @NonNull
    private static List<Settings> HEADERS = new LinkedList(Arrays.asList(Settings.UI, Settings.SYNC,
            Settings.TASK, Settings.SPECIAL_LISTS, Settings.NOTIFICATION, Settings.BACKUP, Settings.DEV,
            Settings.ABOUT, Settings.DONATE));
    @NonNull
    private static final Set<Settings> SUBSETTINGS = EnumSet.of(Settings.SYNC, Settings.SPECIAL_LISTS);
    @Nullable
    private SettingsHeaderAdapter headerAdapter;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reloadSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadSettings();
    }

    @NonNull
    @Override
    protected Optional<Fragment> getDetailFragment(final @NonNull Settings settings) {
        if (SUBSETTINGS.contains(settings)) {
            final Intent intent = settings.getIntent(this);
            startActivity(intent);
            return Optional.absent();
        } else {
            return Optional.of(settings.getFragment());
        }
    }

    @Override
    protected boolean hasMenu() {
        return false;
    }

    @Override
    protected boolean isSupport() {
        return false;
    }

    @NonNull
    @Override
    protected Class<? extends GenericModelListActivity> getSelf() {
        return this.getClass();
    }

    @NonNull
    @Override
    protected Settings getDefaultItem() {
        return Settings.UI;
    }

    @Override
    protected void createItem(@NonNull final Context ctx) {
        //nothing
    }

    @Override
    protected String getTextTitle() {
        return getString(R.string.title_settings);
    }

    @Override
    protected void setUpActionBar() {
        final Toolbar bar = (Toolbar) findViewById(R.id.actionbar);
        bar.setBackgroundColor(ThemeManager.getPrimaryThemeColor());
        setSupportActionBar(bar);
    }

    @Nullable
    @Override
    public RecyclerView.Adapter getAdapter() {
        headerAdapter = new SettingsHeaderAdapter(HEADERS, this);
        return headerAdapter;
    }

    public void reloadSettings() {
        if (MirakelCommonPreferences.isEnabledDebugMenu() && !HEADERS.contains(Settings.DEV)) {
            HEADERS.add(DEV_SETTINGS_POSITION, Settings.DEV);
        } else if (!MirakelCommonPreferences.isEnabledDebugMenu() && HEADERS.contains(Settings.DEV)) {
            HEADERS.remove(DEV_SETTINGS_POSITION);
        }
        if (headerAdapter != null) {
            headerAdapter.setData(HEADERS);
        }
    }



}
