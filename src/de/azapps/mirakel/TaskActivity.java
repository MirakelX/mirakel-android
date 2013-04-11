package de.azapps.mirakel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewSwitcher;

public class TaskActivity extends Activity {
	private static final String TAG = "TaskActivity";
	protected long id;
	protected Task task;
	private TasksDataSource datasource;
	protected TextView Task_name;
	protected CheckBox Task_done;
	protected TextView Task_prio;
	protected TextView Task_content;
	protected TextView Task_due;

	protected TaskActivity main;
	protected NumberPicker picker;
	protected EditText input;

	private boolean mIgnoreTimeSet = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		main = this;
		setContentView(R.layout.activity_task);
		id = getIntent().getLongExtra("id", -1);
		Log.v(TAG, "Taskid " + id);

		// Init
		datasource = new TasksDataSource(this);
		datasource.open();
		task = datasource.getTask(id);
		// Swipe commands
		Map<SwipeListener.Direction, SwipeCommand> commands = new HashMap<SwipeListener.Direction, SwipeCommand>();
		commands.put(SwipeListener.Direction.LEFT, new SwipeCommand() {
			@Override
			public void runCommand(View v, MotionEvent event) {
				finish();
			}
		});
		((LinearLayout) this.findViewById(R.id.task_details))
				.setOnTouchListener(new SwipeListener(true, commands));

		// Task Name
		Task_name = (TextView) findViewById(R.id.task_name);
		Task_name.setText(task.getName());
		Task_name.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.switch_name);
				switcher.showNext(); // or switcher.showPrevious();
				EditText txt = (EditText) findViewById(R.id.edit_name);
				txt.setText(Task_name.getText());
				txt.requestFocus();

				getApplicationContext();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(txt, InputMethodManager.SHOW_IMPLICIT);
				txt.setOnEditorActionListener(new OnEditorActionListener() {
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_DONE) {
							EditText txt = (EditText) findViewById(R.id.edit_name);
							getApplicationContext();
							InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
							ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.switch_name);
							task.setName(txt.getText().toString());
							datasource.saveTask(task);
							Task_name.setText(task.getName());
							switcher.showPrevious();
							imm.hideSoftInputFromWindow(txt.getWindowToken(), 0);
							return true;
						}
						return false;
					}
				});
			}
		});

		// Task done
		Task_done = (CheckBox) findViewById(R.id.task_done);
		Task_done.setChecked(task.isDone());
		Task_done.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				task.setDone(isChecked);
				datasource.saveTask(task);
			}
		});

		// Task priority
		Task_prio = (TextView) findViewById(R.id.task_prio);
		set_prio(Task_prio, task);
		Task_prio.setOnClickListener(new OnClickListener() {
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
										TaskActivity.set_prio(Task_prio, task);
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

		// Task due
		Task_due = (TextView) findViewById(R.id.task_due);
		Drawable due_img = getApplicationContext().getResources().getDrawable(
				android.R.drawable.ic_menu_today);
		due_img.setBounds(0, 0, 60, 60);
		Task_due.setCompoundDrawables(due_img, null, null, null);
		Task_due.setText(task.getDue().compareTo(
				new GregorianCalendar(1970, 1, 1)) < 0 ? this
				.getString(R.string.task_no_due) : new SimpleDateFormat(this
				.getString(R.string.dateFormat), Locale.getDefault())
				.format(task.getDue().getTime()));

		Task_due.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mIgnoreTimeSet = false;
				GregorianCalendar due = (task.getDue().compareTo(
						new GregorianCalendar()) < 0 ? new GregorianCalendar()
						: task.getDue());
				DatePickerDialog dialog = new DatePickerDialog(main,
						new OnDateSetListener() {

							@Override
							public void onDateSet(DatePicker view, int year,
									int monthOfYear, int dayOfMonth) {
								if (mIgnoreTimeSet)
									return;

								task.setDue(new GregorianCalendar(year,
										monthOfYear, dayOfMonth));
								datasource.saveTask(task);
								Task_due.setText(new SimpleDateFormat(view
										.getContext().getString(
												R.string.dateFormat), Locale
										.getDefault()).format(task.getDue()
										.getTime()));

							}
						}, due.get(Calendar.YEAR), due.get(Calendar.MONTH), due
								.get(Calendar.DAY_OF_MONTH));
				dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
						getString(R.string.no_date),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (which == DialogInterface.BUTTON_NEGATIVE) {
									mIgnoreTimeSet = true;
									Log.v(TAG, "cancel");
									task.setDue(new GregorianCalendar(0, 1, 1));
									datasource.saveTask(task);
									Task_due.setText(R.string.task_no_due);
								}
							}
						});
				dialog.show();

			}
		});

		// Task content
		Task_content = (TextView) findViewById(R.id.task_content);
		Task_content.setText(task.getContent().length() == 0 ? this
				.getString(R.string.task_no_content) : task.getContent());
		Drawable content_img = getApplicationContext().getResources()
				.getDrawable(android.R.drawable.ic_menu_edit);
		content_img.setBounds(0, 0, 60, 60);
		Task_content.setCompoundDrawables(content_img, null, null, null);
		Task_content.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.switch_content);
				switcher.showNext(); // or switcher.showPrevious();
				EditText txt = (EditText) findViewById(R.id.edit_content);
				txt.setText(task.getContent());
				txt.requestFocus();

				getApplicationContext();
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(txt, InputMethodManager.SHOW_IMPLICIT);
				Button submit = (Button) findViewById(R.id.submit_content);
				submit.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						EditText txt = (EditText) findViewById(R.id.edit_content);
						getApplicationContext();
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						task.setContent(txt.getText().toString());
						datasource.saveTask(task);
						Task_content
								.setText(task.getContent().trim().length() == 0 ? getString(R.string.task_no_content)
										: task.getContent());
						ViewSwitcher switcher = (ViewSwitcher) findViewById(R.id.switch_content);
						switcher.showPrevious();
						imm.hideSoftInputFromWindow(txt.getWindowToken(), 0);

					}
				});
			}
		});

	} // Log.e(TAG,task.getContent().trim().length()+"");

	protected static void set_prio(TextView Task_prio, Task task) {
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
	public void onStop() {
		datasource.close();
		super.onStop();
	}

	@Override
	public void onDestroy() {
		datasource.close();
		super.onDestroy();
	}
	private List<List_mirakle> lists;
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_delete:
			new AlertDialog.Builder(this)
					.setTitle(this.getString(R.string.task_delete_title))
					.setMessage(this.getString(R.string.task_delete_content))
					.setPositiveButton(this.getString(R.string.Yes),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									datasource.deleteTask(task);
									finish();
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
		case android.R.id.home:
			finish();
			return true;
		case R.id.menu_move:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(
					R.string.dialog_move);
/*
			builder.setPositiveButton(this.getString(R.string.Yes),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

						}
					});
			builder.setNegativeButton(this.getString(R.string.no),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// do nothing
						}
					});*/
			ListsDataSource listds=new ListsDataSource(this);
			listds.open();
			lists=listds.getAllLists();
			listds.close();
			List<CharSequence> items=new ArrayList<CharSequence>();
			final List<Integer> list_ids=new ArrayList<Integer>();
			for(List_mirakle list:lists) {
				if(list.getId()>0){
					items.add(list.getName());
					list_ids.add(list.getId());
				}
			}
			
			builder.setItems(items.toArray(new CharSequence[items.size()]), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					task.setListId(list_ids.get(item));
					datasource.saveTask(task);
				}
			});

			AlertDialog dialog = builder.create();
			dialog.show();
			return true;

		default:
			return super.onOptionsItemSelected(item);

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_task, menu);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		return true;
	}

}
