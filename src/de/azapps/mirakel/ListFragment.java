package de.azapps.mirakel;

import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

public class ListFragment extends Fragment {
	private static final String TAG = "ListsActivity";
	private ListAdapter adapter;
	protected MainActivity main;
	protected EditText input;
	private View view;

	public void setActivity(MainActivity activity) {
		main = activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.activity_list, container, false);
		// Inflate the layout for this fragment

		update();
		return view;
	}

	void update() {
		//TODO does not update the List!
		final List<List_mirakle> values = main.getListDataSource().getAllLists();

		adapter = new ListAdapter(this.getActivity(), R.layout.lists_row,
				values);
		ListView listView = (ListView) view.findViewById(R.id.lists_list);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				List_mirakle list = values.get((int) id);
				main.setCurrentList(list);
			}
		});
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View item,
					int position, final long id) {
				List_mirakle list = values.get((int) id);
				if (list.getId()<=0)
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
										main.getListDataSource().saveList(list);
										update();
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

}