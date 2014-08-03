package de.azapps.mirakel.new_ui.fragments;



import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.faizmalkani.floatingactionbutton.FloatingActionButton;

import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.R;
import de.azapps.mirakel.new_ui.adapter.TaskAdapter;

public class TasksFragment extends Fragment  implements LoaderManager.LoaderCallbacks {

	TaskAdapter mAdapter;
	ListView mListView;
	View layout;

    public TasksFragment() {
        // Required empty public constructor
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mAdapter = new TaskAdapter(getActivity(), null, 0);
		mListView.setAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
		layout = inflater.inflate(R.layout.fragment_tasks, container, false);
		mListView = (ListView) layout.findViewById(R.id.task_listview);
		initFab();
		return layout;
    }

	public void initFab() {
		FloatingActionButton mFab = (FloatingActionButton) layout.findViewById(R.id.fabbutton);
		mFab.setColor(getResources().getColor(R.color.colorControlHighlight));
		mFab.setDrawable(getResources().getDrawable(android.R.drawable.ic_menu_add));
		mFab.hide(false);
		mFab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickFAB(v);
			}
		});
	}

	private void clickFAB(View v) {
		// TODO
	}


	@Override
	public Loader onCreateLoader(int i, Bundle bundle) {
		return Task.allCursorLoader(true);
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
