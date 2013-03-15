package de.azapps.mirakel;

import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.view.View.OnClickListener;

public class TasksActivity extends Activity {
	private static final String TAG="TasksActivity";
	private static final String TABLE_NAME="tasks";
	private static final String[] FROM={"_id","name","done","priority"};
	private int listId;
	private TasksDataSource datasource;
	private TaskAdapter adapter;
	private NumberPicker picker;
	private TasksActivity main;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tasks);
		main=this;
		this.listId=this.getIntent().getIntExtra("listId", 0);
		
		datasource=new TasksDataSource(this);
		datasource.open();
		load_tasks();
		
		//Events
		EditText newTask=(EditText) findViewById(R.id.tasks_new);
		newTask.setOnEditorActionListener(new OnEditorActionListener() {
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId == EditorInfo.IME_ACTION_SEND) {
		        	Log.v(TAG,"New Task");
		        	Task task=datasource.createTask(v.getText().toString(), getListId());
		        	v.setText(null);
		        	adapter.add(task);
		    		adapter.notifyDataSetChanged();
		        	//adapter.swapCursor(updateListCursor());
		            return true;
		        }
		        return false;
		    }
		});
	}

	private void load_tasks() {
		final List<Task> values= datasource.getAllTasks();
		
		adapter=new TaskAdapter(this, R.layout.tasks_row,values, new OnClickListener() {
			@Override
			public void onClick(View v) {
				CheckBox cb=(CheckBox) v;
				Task task=(Task) cb.getTag();
				task.toggleDone();
				datasource.saveTask(task);
			}
		},
		new OnClickListener() {
			
			@Override
			public void onClick(final View v) {
				
				picker = new NumberPicker(main);
				picker.setMaxValue(4);
				picker.setMinValue(0);
				String[] t = { "-2", "-1", "0", "1", "2" };
				picker.setDisplayedValues(t);
				picker.setWrapSelectorWheel(false);
				picker.setValue(((Task)v.getTag()).getPriority() + 2);
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
										Task task=(Task) v.getTag();
										task.setPriority((picker.getValue() - 2));
										datasource.saveTask(task);
										load_tasks();
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
		ListView listView=(ListView) findViewById(R.id.tasks_list);
		listView.setAdapter(adapter);
		
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item, int position, long id) {
				//TODO Remove Bad Hack
				Task t=values.get((int) id);
				Log.v(TAG,"Switch to Task "+t.getId());
				Intent task = new Intent(item.getContext(),TaskActivity.class);				
				task.putExtra("id", t.getId());
				startActivity(task);
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		datasource.open();
		load_tasks();
		
	}
	@Override
	protected void onPause() {
		datasource.close();
		super.onPause();
	}
	
	private Cursor updateListCursor(){
		Cursor tasks;
		switch(listId){
		case Mirakel.LIST_ALL:
			this.setTitle(R.string.tasks_all);
			tasks=Mirakel.getReadableDatabase().query(TABLE_NAME, FROM, null, null, null, null, null);
			break;
		case Mirakel.LIST_DAILY:
			this.setTitle(R.string.tasks_daily);
			tasks=Mirakel.getReadableDatabase().query(TABLE_NAME, FROM, "due<=date('now')", null, null, null, null);
			break;
		case Mirakel.LIST_WEEKLY:
			this.setTitle(R.string.tasks_weekly);
			tasks=Mirakel.getReadableDatabase().query(TABLE_NAME, FROM, "due<=date('now','+7 days')", null, null, null, null);
			break;
		default:
			tasks=Mirakel.getReadableDatabase().query(TABLE_NAME, FROM, "list_id='"+listId+"'", null, null, null, null);
			Log.e(TAG, "Implement show tasks");
		}
		return tasks;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tasks, menu);
		return true;
	}
	@Override
    public void onStart(){
		super.onStart();
		datasource.open();
		load_tasks();
	}
	@Override
    public void onRestart(){
		super.onRestart();
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
	private long getListId(){
		long id=listId;
		if(id<1) id=1;
		return id;
	}

}
