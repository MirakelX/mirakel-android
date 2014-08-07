package de.azapps.mirakel.new_ui.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.faizmalkani.floatingactionbutton.FloatingActionButton;
import com.google.common.base.Optional;


import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.new_ui.R;
import de.azapps.mirakel.new_ui.adapter.TaskAdapter;
import de.azapps.mirakel.new_ui.interfaces.OnTaskSelectedListener;

public class TasksFragment extends Fragment implements LoaderManager.LoaderCallbacks {

    private TaskAdapter mAdapter;
    private ListView mListView;
    private View layout;
    private OnTaskSelectedListener mListener;

    private Optional<Long> listId;

    public TasksFragment() {
        // Required empty public constructor
    }


    public static TasksFragment newInstance(long list_id) {
        TasksFragment f = new TasksFragment();
        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putLong("list_id", list_id);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new TaskAdapter(getActivity(), null, 0, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onTaskSelected(((TaskAdapter.ViewHolder) v.getTag()).getTask());
            }
        });
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mListener.onTaskSelected(((TaskAdapter.ViewHolder) view.getTag()).getTask());
            }
        });
        listId = Optional.absent();
        getLoaderManager().initLoader(0, null, this);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnTaskSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
        }
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


    private void update() {
        Bundle args = new Bundle();
        if (listId.isPresent()) {
            args.putLong("list_id", listId.get());
        }
        getLoaderManager().restartLoader(0, args, this);
    }


    @Override
    public Loader onCreateLoader(int i, Bundle bundle) {
        boolean showDone = MirakelCommonPreferences.showDoneMain();
        Bundle arguments = getArguments();
        try {
            listId = Optional.of(arguments.getLong("list_id"));
        } catch (IllegalStateException | NullPointerException e) {
            listId = Optional.absent();
        }
        if (listId.isPresent()) {
            Optional<ListMirakel> list = ListMirakel.get(listId.get());
            if (list.isPresent()) {
                return Task.getCursorLoader(list.get(), showDone);
            }
        }
        return Task.allCursorLoader(showDone);
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
