package de.azapps.mirakel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

public class ListFragment extends Fragment {
	private static final String TAG = "ListsActivity";
	private ListsDataSource datasource;
	private ListAdapter adapter;
	protected ListActivity main;
	protected EditText input;
	private View view;
	private MainActivity mainActivity;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view=inflater.inflate(R.layout.list_fragment, container, false);
		// Inflate the layout for this fragment

		datasource = new ListsDataSource(this.getActivity());
		datasource.open();
		mainActivity=(MainActivity) getActivity();
		load_lists();
		return view;
	}
	

	private void load_lists() {
		final List<List_mirakle> values = datasource.getAllLists();

		adapter = new ListAdapter(this.getActivity(), R.layout.lists_row,
				values);
		ListView listView = (ListView) view.findViewById(R.id.lists_list);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				// TODO Remove Bad Hack
				List_mirakle list = values.get((int) id);
				mainActivity.tasksFragment.setList(list);
				mainActivity.tasksFragment.update();
				
				
				((MainActivity)getActivity()).mViewPager.setCurrentItem(1);
			}
		});
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View item,
					int position, final long id) {
				List_mirakle list = values.get((int) id);
				if (list.getId() == Mirakel.LIST_ALL
						|| list.getId() == Mirakel.LIST_DAILY
						|| list.getId() == Mirakel.LIST_WEEKLY)
					return false;
				input = new EditText(main);
				input.setText(list.getName());
				input.setTag(main);
				input.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
				new AlertDialog.Builder(main)
						.setTitle(
								main.getString(R.string.list_change_name_title))
						.setMessage(
								main.getString(R.string.list_change_name_cont))
						.setView(input)
						.setPositiveButton(main.getString(R.string.OK),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										List_mirakle list = values
												.get((int) id);
										list.setName(input.getText().toString());
										datasource.saveList(list);
										load_lists();
									}
								})
						.setNegativeButton(main.getString(R.string.Cancel),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										// Do nothing.
									}
								}).show();
				return false;
			}

		});
	}

	@Override
	public void onStart() {
		super.onStart();
		datasource.open();
		load_lists();
	}

	@Override
	public void onStop() {
		datasource.close();
		super.onStop();
	}

	@Override
	public void onDestroy() {
		datasource.close();
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Log.e(TAG, "Implement Settings");
			// TODO implement Settings
			return true;
		case R.id.menu_new_list:
			Log.v(TAG, "New List");
			datasource.createList(this.getString(R.string.list_menu_new_list));
			load_lists();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}