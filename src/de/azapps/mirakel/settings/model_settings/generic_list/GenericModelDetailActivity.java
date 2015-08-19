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

package de.azapps.mirakel.settings.model_settings.generic_list;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.azapps.material_elements.ActionBarActivity;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.generic.IGenericElementInterface;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.SettingsHelper;
import de.azapps.tools.Log;

/**
 * An activity representing a single SpecialList detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link GenericModelListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link de.azapps.mirakel.settings.model_settings.special_list.SpecialListDetailFragment}.
 */
public class GenericModelDetailActivity extends
    ActionBarActivity {

    public static final int NEED_UPDATE = 42;
    public static final int SWITCH_LAYOUT = 43;
    public static final String FRAGMENT = "Fragment";
    public static final String BACK_ACTIVITY = "back";
    private static final String TAG = "GenericModelSettingsDetailActivity";
    public static final String DETAIL_FRAGMENT = "detailFragment";


    protected boolean isSupport;
    @NonNull
    private Class<? extends GenericModelListActivity> backActivity;
    @NonNull
    protected List<Object> backstack = new ArrayList<>();




    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_model_detail);
        setUpActionbar();

        // Show the Up button in the action bar.
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.

            final Class<?> fragmentClass =
                (Class<?>) getIntent().getSerializableExtra(FRAGMENT);
            backActivity = (Class<? extends GenericModelListActivity>) getIntent().getSerializableExtra(
                               BACK_ACTIVITY);
            final Object fragment;
            try {
                fragment = fragmentClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                Log.wtf(TAG, "Cannot create fragment: " + fragmentClass.getName());
                throw new IllegalArgumentException("Cannot create fragment: " + fragmentClass.getName(), e);
            }
            setFragmentIntern(fragment);
        }
    }

    @Override
    protected Locale getLocale() {
        return Helpers.getLocale(this);
    }

    private void setFragmentIntern(final @NonNull Object fragment) {
        final Bundle arguments = new Bundle();
        final IGenericElementInterface item = getIntent().getParcelableExtra(
                GenericModelDetailFragment.ARG_ITEM);
        setTitle(item.getName());
        arguments.putParcelable(GenericModelDetailFragment.ARG_ITEM,
                                item);
        backstack.add(fragment);
        if (fragment instanceof android.app.Fragment) {
            ((android.app.Fragment)fragment).setArguments(arguments);
            getFragmentManager().beginTransaction()
            .replace(R.id.speciallist_detail_container, ((android.app.Fragment) fragment), DETAIL_FRAGMENT)
            .commit();
        } else if (fragment instanceof Fragment) {
            ((Fragment)fragment).setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
            .replace(R.id.speciallist_detail_container, ((Fragment) fragment),
                     DETAIL_FRAGMENT)
            .commit();
            isSupport = true;
        }
    }

    public void setFragment(final @NonNull android.app.Fragment f) {
        setFragmentIntern(f);
    }

    public void setFragment(final @NonNull Fragment f) {
        setFragmentIntern(f);
    }

    @Override
    public void onBackPressed() {
        if (backstack.size() > 1) {
            backstack.remove(backstack.size() - 1);
            setFragmentIntern(backstack.get(backstack.size() - 1));
            backstack.remove(backstack.size() - 1);
        } else {
            super.onBackPressed();
        }
    }

    protected void setUpActionbar() {
        final Toolbar actionBar = (Toolbar) findViewById(R.id.actionbar);
        actionBar.setBackgroundResource(R.color.colorPrimary);
        actionBar.setVisibility(View.VISIBLE);
        setSupportActionBar(actionBar);
        getSupportActionBar().setElevation(getResources().getDimension(R.dimen.actionbar_elevation));
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (MirakelCommonPreferences.isTablet()) {
            setResult(SWITCH_LAYOUT, getIntent());
            finish();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, new Intent(this, backActivity));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent data) {

        if (SettingsHelper.handleActivityResult(requestCode, resultCode, data, this)) {
            finish();
        }
    }
}
