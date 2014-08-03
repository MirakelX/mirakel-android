package de.azapps.mirakel.new_ui.fragments;



import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.new_ui.R;
import de.azapps.mirakel.new_ui.adapter.ListAdapter;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class ListsFragment extends ListFragment implements LoaderManager.LoaderCallbacks {

	ListAdapter mAdapter;


    public ListsFragment() {
        // Required empty public constructor
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mAdapter = new ListAdapter(getActivity(), null, 0);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}


	@Override
	public Loader onCreateLoader(int i, Bundle bundle) {
		return ListMirakel.allCursorLoader();
	}

	@Override
	public void onLoadFinished(Loader loader, Object o) {
		mAdapter.swapCursor((Cursor) o);
	}

	@Override
	public void onLoaderReset(Loader loader) {
		mAdapter.swapCursor(null);
	}
}
