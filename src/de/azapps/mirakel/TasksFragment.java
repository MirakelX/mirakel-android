package de.azapps.mirakel;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

public class TasksFragment extends Fragment {
	private static final String TAG = "TasksActivity";
	private List_mirakle list;
	private TasksDataSource datasource;
	private ListsDataSource datasource_lists;
	private TaskAdapter adapter;
	private NumberPicker picker;
	private FragmentActivity main;
	private String server_url;
	private String Email;
	private String Password;
	private View view;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		view=inflater.inflate(R.layout.tasks_fragment, container, false);
		main=getActivity();
		datasource = new TasksDataSource(getActivity());
		datasource.open();
		datasource_lists = new ListsDataSource(getActivity());
		datasource_lists.open();
		this.list = datasource_lists.getList(0);
		
		Log.v(TAG, "Start list" + list.getId());
		getResources().getString(R.string.action_settings);
		update();

		// Events
		EditText newTask = (EditText) view.findViewById(R.id.tasks_new);
		newTask.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					Log.v(TAG, "New Task");
					long id = list.getId();
					Log.v(TAG, "Create in " + id);
					if (id <= 0) {
						try {
							id = datasource_lists.getFirstList().getId();
						} catch (NullPointerException e) {
							Toast.makeText(getActivity(),
									R.string.no_lists, Toast.LENGTH_LONG)
									.show();
							return false;
						}
					}
					Task task = datasource.createTask(v.getText().toString(),
							id);
					v.setText(null);
					adapter.add(task);
					adapter.notifyDataSetChanged();
					// adapter.swapCursor(updateListCursor());
					return true;
				}
				return false;
			}
		});
    	
    	
        // Inflate the layout for this fragment
        return view;
    }
    
    

    public void setList(List_mirakle list) {
    	this.list=list;
    }
    
	public void update() {
		Log.v(TAG, "loading...");
		if(list==null) return;
		Log.v(TAG, "loading..." + list.getId());
		final List<Task> values = datasource.getTasks(list, list.getSortBy());
		adapter = new TaskAdapter(getActivity(), R.layout.tasks_row, values,
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						CheckBox cb = (CheckBox) v;
						Task task = (Task) cb.getTag();
						task.toggleDone();
						datasource.saveTask(task);
						update();
					}
				}, new OnClickListener() {
					@Override
					public void onClick(final View v) {

						picker = new NumberPicker(main);
						picker.setMaxValue(4);
						picker.setMinValue(0);
						String[] t = { "-2", "-1", "0", "1", "2" };
						picker.setDisplayedValues(t);
						picker.setWrapSelectorWheel(false);
						picker.setValue(((Task) v.getTag()).getPriority() + 2);
						new AlertDialog.Builder(main)
								.setTitle(
										main.getString(R.string.task_change_prio_title))
								.setMessage(
										main.getString(R.string.task_change_prio_cont))
								.setView(picker)
								.setPositiveButton(main.getString(R.string.OK),
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int whichButton) {
												Task task = (Task) v.getTag();
												task.setPriority((picker
														.getValue() - 2));
												datasource.saveTask(task);
												update();
											}

										})
								.setNegativeButton(
										main.getString(R.string.Cancel),
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int whichButton) {
												// Do nothing.
											}
										}).show();

					}
				});
		ListView listView = (ListView) view.findViewById(R.id.tasks_list);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				// TODO Remove Bad Hack
				Task t = values.get((int) id);
				Log.v(TAG, "Switch to Task " + t.getId());
				((MainActivity) getActivity()).taskFragment.setTask(t);
				((MainActivity) getActivity()).taskFragment.update();
				((MainActivity)getActivity()).mViewPager.setCurrentItem(2);
			}
		});
		switch (list.getId()) {
		case Mirakel.LIST_ALL:
			getActivity().setTitle(this.getString(R.string.list_all));
			break;
		case Mirakel.LIST_DAILY:
			getActivity().setTitle(this.getString(R.string.list_today));
			break;
		case Mirakel.LIST_WEEKLY:
			getActivity().setTitle(this.getString(R.string.list_week));
			break;
		default:
			getActivity().setTitle(list.getName());
		}
	}


}
