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
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.support.design.widget.FloatingActionButton;

import de.azapps.mirakel.helper.AnalyticsWrapperBase;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.generic.IGenericElementInterface;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.adapter.SettingsGroupAdapter;
import de.azapps.mirakel.settings.custom_views.SwipeLinearLayout;
import de.azapps.mirakel.settings.model_settings.generic_list.IDetailFragment;


public abstract class MirakelPreferencesFragment<T extends IGenericElementInterface> extends
    PreferenceFragment implements
    IDetailFragment<T>, View.OnClickListener {


    protected RecyclerView recyclerView;
    protected FloatingActionButton mFab;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getItem().getName());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsWrapperBase.setScreen(this);
    }

    protected void onFABClicked() {
        //nothing
    }

    protected void updateScreen(final PreferenceScreen preferenceScreen) {
        setAdapter(preferenceScreen);
    }

    @Override
    public void onClick(final View v) {
        onFABClicked();
    }

    @Override
    @NonNull
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.generic_list_fragment, null);
        recyclerView = (RecyclerView)rootView.findViewById(R.id.generic_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        setAdapter(getPreferenceScreen());
        mFab = (FloatingActionButton) rootView.findViewById(R.id.fabbutton);
        configureFab(mFab);
        setHasOptionsMenu(hasMenu());
        return rootView;
    }

    protected void setAdapter(final PreferenceScreen preferenceScreen) {
        final SettingsGroupAdapter mAdapter = new SettingsGroupAdapter(preferenceScreen);
        mAdapter.setRemoveListener(getRemoveListener());
        recyclerView.setAdapter(mAdapter);
    }

    protected void configureFab(final FloatingActionButton fab) {
        if (isFabVisible()) {
            fab.setImageResource(R.drawable.ic_delete_24px);
            fab.setOnClickListener(this);
        } else {
            fab.setVisibility(View.GONE);
        }
    }

    @Nullable
    protected SwipeLinearLayout.OnItemRemoveListener getRemoveListener() {
        return null;
    }


    protected boolean isFabVisible() {
        return false;
    }

    protected boolean hasMenu() {
        return false;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.generic_model_menu, menu);
        final MenuItem item = menu.findItem(R.id.action_create_special);
        item.setEnabled(false);
        item.setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.action_delete_special) {
            handleDelete();
            if (!MirakelCommonPreferences.isTablet()) {
                getActivity().finish();
            }
            return true;
        } else {
            return false;
        }
    }

    protected void handleDelete() {
        //nothing
    }



    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        try {
            super.onActivityCreated(savedInstanceState);
        } catch (final RuntimeException ignored) {
            //we must call this
        }
    }
}
