package de.azapps.mirakel.special_lists_settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.azapps.mirakel.R;
import de.azapps.mirakel.R.layout;
import de.azapps.mirakel.R.menu;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.support.v4.app.NavUtils;

public class SpecialListsSettings extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_special_lists_settings);
		// Show the Up button in the action bar.
		setupActionBar();
		
		ListView listView=(ListView) findViewById(R.id.special_lists_list);
		final List<SpecialList> slists=SpecialList.allSpecial(true);
		List<String> listContent=new ArrayList<String>();
		for(SpecialList list : slists) {
			listContent.add(list.getName());
		}
		ArrayAdapter<String> adapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,listContent);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				// TODO Remove Bad Hack
				SpecialList sl = slists.get((int) id);
				editSList(sl);
			}
		});
	}
	
	private void editSList(final SpecialList slist){

		final LinearLayout llayout=(LinearLayout) findViewById(R.id.edit_special_list);
		new AlertDialog.Builder(this)
				.setTitle(slist.getName())
				.setMessage(getString(R.string.list_change_name_cont))
				.setView(llayout)
				.setPositiveButton(getString(R.string.OK),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// List_mirakle list = values.get((int) id);
								//slist.setName(input.getText().toString());
								//slist.save();
							}
						})
				.setNegativeButton(getString(R.string.Cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Do nothing.
							}
						}).show();
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.special_lists_settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
