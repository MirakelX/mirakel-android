package de.azapps.mirakel;

import android.os.Bundle;
import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class TasksActivity extends Activity {
	private static final String TAG="TasksActivity";
	private static final String TABLE_NAME="tasks";
	private static final String[] FROM={"_id","name","done","priority"};
	private static final String[] FROM_VIEW={"done","name","priority"};
	private static final int[] TO_VIEW={R.id.tasks_row_done,R.id.tasks_row_name,R.id.tasks_row_priority};
	private int listId;
	private SimpleCursorAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tasks);
		this.listId=this.getIntent().getIntExtra("listId", 0);
		Cursor tasks=updateListCursor();
		adapter=new SimpleCursorAdapter(this, R.layout.tasks_row, tasks, FROM_VIEW, TO_VIEW,0);
		ListView taskList=(ListView) findViewById(R.id.tasks_list);
		taskList.setAdapter(adapter);
		
		
		//Events
		EditText newTask=(EditText) findViewById(R.id.tasks_new);
		newTask.setOnEditorActionListener(new OnEditorActionListener() {
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId == EditorInfo.IME_ACTION_SEND) {
		        	Log.v(TAG,"New Task");
		        	ContentValues values=new ContentValues();
		        	values.put("name", v.getText().toString());
		        	values.put("list_id", getListId());
		        	Mirakel.getWritableDatabase().insert("tasks",null,values);
		        	v.setText(null);
		        	adapter.swapCursor(updateListCursor());
		            return true;
		        }
		        return false;
		    }
		});
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
