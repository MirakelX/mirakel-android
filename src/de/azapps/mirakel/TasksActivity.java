package de.azapps.mirakel;

import java.util.List;

import android.os.Bundle;
import android.os.Message;
import android.app.Activity;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;
import android.view.View.OnClickListener;

public class TasksActivity extends Activity {
	private static final String TAG="TasksActivity";
	private static final String TABLE_NAME="tasks";
	private static final String[] FROM={"_id","name","done","priority"};
	private int listId;
	private TasksDataSource datasource;
	private TaskAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tasks);
		
		this.listId=this.getIntent().getIntExtra("listId", 0);
		
		datasource=new TasksDataSource(this);
		datasource.open();
		final List<Task> values= datasource.getAllTasks();
		
		adapter=new TaskAdapter(this, R.layout.tasks_row,values, new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				CheckBox cb=(CheckBox) v;
				Task task=(Task) cb.getTag();
				task.toggleDone();
				datasource.saveTask(task);
				
			}
		});
		ListView listView=(ListView) findViewById(R.id.tasks_list);
		listView.setAdapter(adapter);
		
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item, int position, long id) {
				//Log.e(TAG,"Implement OnClick");
				//TODO Remove Bad Hack
				Task t=values.get((int) id);
				Log.e(TAG,"Switch to Task "+t.getId());
				Intent task = new Intent(item.getContext(),TaskActivity.class);				
				task.putExtra("id", t.getId());
				startActivity(task);
			}
		});
		
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
	
	@Override
	protected void onResume() {
		datasource.open();
		super.onResume();
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
			tasks=Mirakel.getReadableDatabase().query(TABLE_NAME, FROM, "due<=date('now','+7 days')", null, null, null, null);
			Log.e(TAG, "Implement showltasks");
		}
		return tasks;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tasks, menu);
		return true;
	}
	private long getListId(){
		long id=listId;
		if(id<1) id=1;
		return id;
	}

}
