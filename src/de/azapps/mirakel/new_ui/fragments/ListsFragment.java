package de.azapps.mirakel.new_ui.fragments;



import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.new_ui.adapter.ListAdapter;
import de.azapps.mirakel.new_ui.interfaces.OnListSelectedListener;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class ListsFragment extends ListFragment implements LoaderManager.LoaderCallbacks {

	ListAdapter mAdapter;
	OnListSelectedListener mListener;


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
	public void onListItemClick (ListView l, View v, int position, long id) {
		mListener.onListSelected(((ListAdapter.ViewHolder) v.getTag()).getList());
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnListSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnListSelectedListener");
		}
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
