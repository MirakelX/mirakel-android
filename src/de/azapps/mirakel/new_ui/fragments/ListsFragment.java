/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *  Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.new_ui.fragments;


import android.app.Activity;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.azapps.mirakel.adapter.OnItemClickedListener;
import butterknife.ButterKnife;
import butterknife.InjectView;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakelandroid.R;
import de.azapps.mirakel.new_ui.adapter.ListAdapter;

public class ListsFragment extends Fragment implements LoaderManager.LoaderCallbacks {

    private ListAdapter mAdapter;
    private OnItemClickedListener<ListMirakel> mListener;
    @InjectView(R.id.list_lists)
    RecyclerView mListView;


    public ListsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new ListAdapter(getActivity(), null, 0, mListener);
        getLoaderManager().initLoader(0, null, this);
        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }


    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnItemClickedListener<ListMirakel>) activity;
        } catch (final ClassCastException ignored) {
            throw new ClassCastException(activity.toString() + " must implement OnListSelectedListener");
        }
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View layout = inflater.inflate(R.layout.fragment_lists, container, false);
        ButterKnife.inject(this, layout);
        return layout;
    }

    @Override
    public Loader onCreateLoader(final int i, final Bundle bundle) {
        return ListMirakel.allWithSpecialSupportCursorLoader();
    }

    @Override
    public void onLoadFinished(final Loader loader, final Object o) {
        mAdapter.swapCursor((Cursor) o);
    }

    @Override
    public void onLoaderReset(final Loader loader) {
        mAdapter.swapCursor(null);
    }
}
