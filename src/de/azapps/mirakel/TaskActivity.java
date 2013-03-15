package de.azapps.mirakel;


import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class TaskActivity extends Activity {
	private static final String TAG = "TaskActivity";
	private static final int[] ColorList = { Color.parseColor("#006400"),
			Color.GREEN, Color.YELLOW, Color.parseColor("#FF8C00"), Color.RED };
	protected long id;
	protected Task task;
	private TasksDataSource datasource;
	protected TextView Task_name;
	protected CheckBox Task_done;
	protected TextView Task_prio;
	protected EditText Task_content;
	
	
	protected TaskActivity main;
	protected NumberPicker picker;
	protected EditText input;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		main=this;
		setContentView(R.layout.activity_task);
		Bundle extras = getIntent().getExtras(); 
		if(extras !=null) {
		    id = extras.getLong("id");
		}else{
			id=-1;
		}
		Log.e(TAG,"Taskid "+id);
		Task_name=(TextView)findViewById(R.id.task_name);
		Task_done=(CheckBox)findViewById(R.id.task_done);
		Task_prio=(TextView)findViewById(R.id.task_prio);
		Task_content=(EditText)findViewById(R.id.task_content);
		datasource=new TasksDataSource(this);
		datasource.open();
		task=datasource.getTask(id);
		datasource.close();
		Task_name.setText(task.getName());
		//Log.e(TAG,task.getContent().trim().length()+"");
		Task_content.setText(task.getContent().trim().length()==0?this.getString(R.string.task_no_content):task.getContent());
		Task_content.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Task_content.setText((Task_content.getText().toString()==main.getString(R.string.task_no_content)?"":Task_content.getText().toString()));
				
			}
		});
		
		Task_name.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				input = new EditText(main);
				input.setText(task.getName());
				input.setTag(main);
				new AlertDialog.Builder(main)
						.setTitle("Change Title")
						.setMessage("New List-Title")
						.setView(input)
						.setPositiveButton("Ok",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										task.setName(input.getText().toString());
										datasource.open();
										datasource.saveTask(task);
										datasource.close();
										Task_name.setText(task.getName());
									}
								})
						.setNegativeButton("Cancel",
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
				datasource.open();
				datasource.saveTask(task);			
			}
		
		});
		Task_prio.setOnClickListener(new OnClickListener() {
		@SuppressLint("NewApi")
		//TODO FIX API-Version
		@Override
		public void onClick(View v) {
			picker = new NumberPicker(main);
			picker.setMaxValue(4);
			picker.setMinValue(0);
			String[] t = { "-2", "-1", "0", "1", "2" };
			picker.setDisplayedValues(t);
			picker.setWrapSelectorWheel(false);
			picker.setValue(task.getPriority()+2);
			new AlertDialog.Builder(main)
					.setTitle("Change Priority")
					.setMessage("New Task-Priority")
					.setView(picker)
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									 task.setPriority((picker.getValue() - 2));
									 datasource.open();
									 datasource.saveTask(task);
									 datasource.close();
									 main.set_prio();										
								}

							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// Do nothing.
								}
							}).show();

		}
	});

	}

	protected  void set_prio() {
		Task_prio.setText("" + task.getPriority());
		Task_prio.setBackgroundColor(ColorList[task.getPriority() + 2]);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_task, menu);
		return true;
	}

}
