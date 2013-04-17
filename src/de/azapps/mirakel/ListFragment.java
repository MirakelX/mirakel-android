package de.azapps.mirakel;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class ListFragment extends Fragment {
	private static final String TAG = "ListsActivity";
	private ListAdapter adapter;
	protected MainActivity main;
	protected EditText input;
	private View view;
	protected boolean EditName;


	public void setActivity(MainActivity activity) {
		main = activity;
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		main=(MainActivity) getActivity();
		view = inflater.inflate(R.layout.activity_list, container, false);
		// Inflate the layout for this fragment
		EditName=false;
		update();
		return view;
	}

	void update() {

		final List<List_mirakle> values = main.getListDataSource().getAllLists();

		adapter = new ListAdapter(this.getActivity(), R.layout.lists_row,
				values);
		ListView listView = (ListView) view.findViewById(R.id.lists_list);
		listView.setItemsCanFocus(true);
		listView.setAdapter(adapter);
		listView.requestFocus();

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				if(EditName){
					EditName=false;
					return;
				}
				List_mirakle list = values.get((int) id);
				main.setCurrentList(list);
			}
		});
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, final View item,
					int position, final long id) {
				List_mirakle list = values.get((int) id);
				if (list.getId()<=0)
					return false;
				EditName=true;
				((ViewSwitcher)item.findViewById(R.id.switch_listname)).showNext();
				EditText txt = (EditText) item.findViewById(R.id.edit_listname);
				txt.setText(values.get((int) id).getName());
				txt.requestFocus();

				main.getApplicationContext();
				InputMethodManager imm = (InputMethodManager) main.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(txt, InputMethodManager.SHOW_FORCED);
				txt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_DONE) {
							List_mirakle l= values.get((int) id);
							EditText txt = (EditText) item.findViewById(R.id.edit_listname);
							main.getApplicationContext();
							InputMethodManager imm = (InputMethodManager) main.getSystemService(Context.INPUT_METHOD_SERVICE);
							ViewSwitcher switcher = (ViewSwitcher) item.findViewById(R.id.switch_listname);
							l.setName(txt.getText().toString());
							main.getListDataSource().saveList(l);
							((TextView)item.findViewById(R.id.list_row_name)).setText(l.getName());
							switcher.showPrevious();
							imm.hideSoftInputFromWindow(txt.getWindowToken(), 0);
							return true;
						}
						return false;
					}
				});
				return false;
			}

		});
	}

}