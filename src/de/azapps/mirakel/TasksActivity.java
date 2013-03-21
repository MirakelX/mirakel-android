package de.azapps.mirakel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
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

public class TasksActivity extends Activity {
	private static final String TAG = "TasksActivity";
	// private static final String TABLE_NAME="tasks";
	// private static final String[] FROM={"_id","name","done","priority"};
	private int listId;
	private String taskOrder;
	private TasksDataSource datasource;
	private ListsDataSource datasource_lists;
	private TaskAdapter adapter;
	private NumberPicker picker;
	private TasksActivity main;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tasks);
		main = this;
		this.listId = this.getIntent().getIntExtra("listId", 0);
		Log.v(TAG, "Start list" + listId);

		datasource = new TasksDataSource(this);
		datasource.open();
		datasource_lists = new ListsDataSource(this);
		datasource_lists.open();
		getResources().getString(R.string.action_settings);
		load_tasks();
		ListView listView = (ListView) findViewById(R.id.tasks_list);

		Map<SwipeListener.Direction, SwipeCommand> commands = new HashMap<SwipeListener.Direction, SwipeCommand>();
		commands.put(SwipeListener.Direction.LEFT, new SwipeCommand() {
			@Override
			public void runCommand(View v, MotionEvent event) {
				Intent list = new Intent(v.getContext(), ListActivity.class);
				datasource.close();
				datasource_lists.close();
				startActivityForResult(list, 1);
			}
		});
		listView.setOnTouchListener(new SwipeListener(false, commands));

		// Events
		EditText newTask = (EditText) findViewById(R.id.tasks_new);
		newTask.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					Log.v(TAG, "New Task");
					long id=getListId();
					Log.v(TAG,"Create in " + id);
					if(id<=0) {
						try {
							id=datasource_lists.getFirstList().getId();
						} catch (NullPointerException e) {
							Toast.makeText(getApplicationContext(), R.string.no_lists, Toast.LENGTH_LONG).show();
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
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				Log.v(TAG, "Change List");
				listId = data.getIntExtra("listId", Mirakel.LIST_ALL);
				datasource_lists.open();
				datasource.open();
				load_tasks();
			}
			if (resultCode == RESULT_CANCELED) {
				// Write your code on no result return
			}
		}
	}

	private void load_tasks() {
		Log.v(TAG, "loading..." + listId);
		final List<Task> values = datasource.getTasks(listId, taskOrder);

		adapter = new TaskAdapter(this, R.layout.tasks_row, values,
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						CheckBox cb = (CheckBox) v;
						Task task = (Task) cb.getTag();
						task.toggleDone();
						datasource.saveTask(task);
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
												load_tasks();
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
		ListView listView = (ListView) findViewById(R.id.tasks_list);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				// TODO Remove Bad Hack
				Task t = values.get((int) id);
				Log.v(TAG, "Switch to Task " + t.getId());
				Intent task = new Intent(item.getContext(), TaskActivity.class);
				task.putExtra("id", t.getId());
				startActivity(task);
			}
		});
		switch (listId) {
		case Mirakel.LIST_ALL:
			this.setTitle(this.getString(R.string.list_all));
			break;
		case Mirakel.LIST_DAILY:
			this.setTitle(this.getString(R.string.list_today));
			break;
		case Mirakel.LIST_WEEKLY:
			this.setTitle(this.getString(R.string.list_week));
			break;
		default:
			List_mirakle list = datasource_lists.getList(listId);
			this.setTitle(list.getName());
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		datasource.open();
		datasource_lists.open();
		load_tasks();

	}

	@Override
	protected void onPause() {
		datasource.close();
		datasource_lists.close();
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tasks, menu);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		return true;
	}

	@Override
	public void onStart() {
		super.onStart();
		datasource.open();
		datasource_lists.open();
		load_tasks();
	}

	@Override
	public void onRestart() {
		super.onRestart();
		datasource.open();
		datasource_lists.open();
	}

	@Override
	public void onStop() {
		datasource.close();
		datasource_lists.close();
		super.onStop();
	}

	@Override
	public void onDestroy() {
		datasource.close();
		datasource_lists.close();
		super.onDestroy();
	}

	private long getListId() {
		return listId;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.list_delete:
			if (listId == Mirakel.LIST_ALL || listId == Mirakel.LIST_DAILY
					|| listId == Mirakel.LIST_WEEKLY)
				return true;
			new AlertDialog.Builder(this)
					.setTitle(this.getString(R.string.list_delete_title))
					.setMessage(this.getString(R.string.list_delete_content))
					.setPositiveButton(this.getString(R.string.Yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									datasource_lists
											.deleteList(datasource_lists
													.getList(listId));
									listId = Mirakel.LIST_ALL;
									load_tasks();
								}
							})
					.setNegativeButton(this.getString(R.string.no),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// do nothing
								}
							}).show();
			return true;
		case R.id.task_sorting:
			final CharSequence[] items = getResources().getStringArray(
					R.array.task_sorting_items);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(this.getString(R.string.task_sorting_title));
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					switch (item) {
					case 1:
						taskOrder = Mirakel.ORDER_BY_DUE;
						break;
					case 2:
						taskOrder = Mirakel.ORDER_BY_PRIO;
						break;
					default:
						taskOrder = Mirakel.ORDER_BY_ID;
						break;
					}
					Log.e(TAG, "sorting: " + taskOrder);
					load_tasks();
					Toast.makeText(getApplicationContext(), items[item],
							Toast.LENGTH_SHORT).show();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		case android.R.id.home:
			finish();
			Intent list = new Intent(this.getApplicationContext(),
					ListActivity.class);
			startActivity(list);
			return true;
		default:
			return super.onOptionsItemSelected(item);

		}
	}
}
