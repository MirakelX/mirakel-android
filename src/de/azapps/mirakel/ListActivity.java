package de.azapps.mirakel;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

public class ListActivity extends Activity {
	private static final String TAG = "ListsActivity";
	private ListsDataSource datasource;
	private ListAdapter adapter;
	protected ListActivity main;
	protected EditText input;
	private float start_x;
	private float start_y;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list);
		main = this;
		datasource = new ListsDataSource(this);
		datasource.open();
		load_lists();
		ListView listView = (ListView) findViewById(R.id.lists_list);
		listView.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				if (action == MotionEvent.ACTION_DOWN) {
					start_x = event.getRawX();
					start_y = event.getRawY();
				} else if (action == MotionEvent.ACTION_UP
						|| action == MotionEvent.ACTION_CANCEL) {
					float dx = start_x - event.getRawX();
					float dy = start_y - event.getRawY();
					if (dy > 3 * dx && dy > v.getHeight() / 3) {
						Log.v(TAG, "swipe up");
					} else if (dx > 3 * dy && dx > v.getWidth() / 3) {
						Log.v(TAG, "swipe rigth");
						Intent returnIntent = new Intent();
						// returnIntent.putExtra("list_id", t.getId());
						setResult(RESULT_CANCELED, returnIntent);
						finish();
					} else if (dy < -3 * dx && -1 * dy > v.getHeight() / 3) {
						Log.v(TAG, "swipe down");
					} else if (dx < -3 * dy && -1 * dx > v.getWidth() / 3) {
						Log.v(TAG, "swipe left");

					} else {
						Log.v(TAG, "Nothing");
					}
				}
				return false;
			}
		});

	}

	private void load_lists() {
		final List<List_mirakle> values = datasource.getAllLists();

		adapter = new ListAdapter(this, R.layout.lists_row, values);
		ListView listView = (ListView) findViewById(R.id.lists_list);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				// TODO Remove Bad Hack
				List_mirakle t = values.get((int) id);
				Log.v(TAG, "Switch to List " + t.getId());
				Intent returnIntent = new Intent(getApplicationContext(),TasksActivity.class);
				returnIntent.putExtra("listId", t.getId());
				startActivity(returnIntent);
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
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_list, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		datasource.open();
		load_lists();

	}

	@Override
	protected void onPause() {
		datasource.close();
		super.onPause();
	}

	@Override
	public void onStart() {
		super.onStart();
		datasource.open();
		load_lists();
	}

	@Override
	public void onRestart() {
		super.onRestart();
		datasource.open();
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
