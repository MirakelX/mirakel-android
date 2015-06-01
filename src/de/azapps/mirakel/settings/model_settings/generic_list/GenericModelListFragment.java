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

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.adapter.SettingsGroupAdapter;


public class GenericModelListFragment extends PreferenceFragment implements View.OnClickListener {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;


    @Nullable
    private RecyclerView listView;

    private FloatingActionButton fab;

    public void reload() {
        if (listView != null) {
            listView.setLayoutManager(mCallbacks.getLayoutManager(getActivity()));
            listView.setAdapter(mCallbacks.getAdapter(this));

        }
    }

    @Override
    public void onClick(View v) {
        mCallbacks.addItem();
    }


    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {

        @Nullable
        public RecyclerView.Adapter getAdapter(final @NonNull PreferenceFragment caller);

        @NonNull
        public RecyclerView.LayoutManager getLayoutManager(final @NonNull Context ctx);

        public void addItem();

        public boolean hasFab();

    }

    private static final String TAG = "GenericModelListFragment";
    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private  Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        @Nullable
        public RecyclerView.Adapter getAdapter(final @NonNull PreferenceFragment caller) {
            return null;
        }

        @NonNull
        @Override
        public RecyclerView.LayoutManager getLayoutManager(final @NonNull Context ctx) {
            return new LinearLayoutManager(ctx);
        }

        @Override
        public void addItem() {
            //nothing
        }

        @Override
        public boolean hasFab() {
            return true;
        }


    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public GenericModelListFragment() {
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reload();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.generic_list_fragment, container, false);
        listView = (RecyclerView)rootView.findViewById(R.id.generic_list);
        fab = (FloatingActionButton)rootView.findViewById(R.id.fabbutton);
        fab.setOnClickListener(this);
        fab.setVisibility(mCallbacks.hasFab() ? View.VISIBLE : View.GONE);
        if (!(mCallbacks.getAdapter(this) instanceof SettingsGroupAdapter)) {
            listView.addItemDecoration(new DividerItemDecoration(rootView.getContext(), null, false, false));
        }
        return rootView;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if ((savedInstanceState != null)
            && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (listView != null) {
            listView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
        if (fab != null) {
            fab.setVisibility(mCallbacks.hasFab() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }


    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }



    private void setActivatedPosition(final int position) {
        if (position == ListView.INVALID_POSITION) {
            //getListView().setItemChecked(mActivatedPosition, false);
        } else {
            //getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
}
