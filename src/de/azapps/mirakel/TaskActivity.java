package de.azapps.mirakel;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class TaskActivity extends Activity {
	private static final String TAG = "TaskActivity";
	protected long id;
	protected Task task;
	private TasksDataSource datasource;
	protected TextView Task_name;
	protected CheckBox Task_done;
	protected TextView Task_prio;
	protected TextView Task_content;

	protected TaskActivity main;
	protected NumberPicker picker;
	protected EditText input;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		main = this;
		setContentView(R.layout.activity_task);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			id = extras.getLong("id");
		} else {
			id = -1;
		}
		Log.v(TAG, "Taskid " + id);

		Task_name = (TextView) findViewById(R.id.task_name);
		Task_done = (CheckBox) findViewById(R.id.task_done);
		Task_prio = (TextView) findViewById(R.id.task_prio);
		Task_content = (TextView) findViewById(R.id.task_content);
		datasource = new TasksDataSource(this);
		datasource.open();
		task = datasource.getTask(id);
		Task_name.setText(task.getName());
		Task_content.setText(task.getContent().trim().length() == 0 ? this.getString(R.string.task_no_content) : task.getContent());
		Task_done.setChecked(task.isDone());
		set_prio();

		Task_name.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				input = new EditText(main);
				input.setText(task.getName());
				input.setTag(main);
				new AlertDialog.Builder(main)
						.setTitle(
								main.getString(R.string.task_change_name_title))
						.setMessage(
								main.getString(R.string.task_change_name_cont))
						.setView(input)
						.setPositiveButton(main.getString(R.string.OK),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										task.setName(input.getText().toString());
										datasource.saveTask(task);
										Task_name.setText(task.getName());
									}
								})
						.setNegativeButton(main.getString(R.string.Cancel),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										// Do nothing.
									}
								}).show();
			}
		});
		Task_content.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				input = new EditText(main);
				input.setText(task.getContent());
				input.setTag(main);
				input.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
				new AlertDialog.Builder(main)
						.setTitle(
								main.getString(R.string.task_change_content_title))
						.setMessage(
								main.getString(R.string.task_change_content_cont))
						.setView(input)
						.setPositiveButton(main.getString(R.string.OK),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										task.setContent(input.getText()
												.toString());
										datasource.saveTask(task);
										Task_content.setText(task.getContent());
									}
								})
						.setNegativeButton(main.getString(R.string.Cancel),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										// Do nothing.
									}
								}).show();
			}
		});

		Task_done.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				task.setDone(isChecked);
				datasource.saveTask(task);
			}

		});
		Task_prio.setOnClickListener(new OnClickListener() {
			@SuppressLint("NewApi")
			// TODO FIX API-Version
			@Override
			public void onClick(View v) {
				picker = new NumberPicker(main);
				picker.setMaxValue(4);
				picker.setMinValue(0);
				String[] t = { "-2", "-1", "0", "1", "2" };
				picker.setDisplayedValues(t);
				picker.setWrapSelectorWheel(false);
				picker.setValue(task.getPriority() + 2);
				new AlertDialog.Builder(main)
						.setTitle(
								main.getString(R.string.task_change_prio_title))
						.setMessage(
								main.getString(R.string.task_change_prio_cont))
						.setView(picker)
						.setPositiveButton(main.getString(R.string.OK),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										task.setPriority((picker.getValue() - 2));
										datasource.saveTask(task);
										main.set_prio();
									}

								})
						.setNegativeButton(main.getString(R.string.Cancel),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										// Do nothing.
									}
								}).show();

			}
		});

	} // Log.e(TAG,task.getContent().trim().length()+"");

	protected void set_prio() {
		Task_prio.setText("" + task.getPriority());
		Task_prio
				.setBackgroundColor(Mirakel.PRIO_COLOR[task.getPriority() + 2]);

	}
	@Override
	public void onPause() {
		datasource.close();
		super.onPause();
	}

	@Override
	public void onStart() {
		super.onStart();
		datasource.open();
	}

	@Override
	public void onRestart() {
		super.onRestart();
		datasource.open();
	}

	@Override
	public void onResume() {
		super.onResume();
		datasource.open();
	}

	@Override
    public void onStop(){
		datasource.close();
		super.onStop();
	}

	@Override
    public void onDestroy(){
		datasource.close();
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_delete:
			new AlertDialog.Builder(this)
					.setTitle("Delete entry")
					.setMessage("Are you sure you want to delete this entry?")
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									datasource.deleteTask(task);
									finish();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// do nothing
								}
							}).show();
			return true;
		default:
			return super.onOptionsItemSelected(item);

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_task, menu);
		return true;
	}

}
