package de.azapps.mirakel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView.OnEditorActionListener;

public class TaskFragment extends Fragment {
	private View view;
	private static final String TAG = "TaskActivity";
	protected TextView Task_name;
	protected CheckBox Task_done;
	protected TextView Task_prio;
	protected TextView Task_content;
	protected TextView Task_due;

	protected MainActivity main;
	protected NumberPicker picker;
	protected EditText input;

	private boolean mIgnoreTimeSet = false;
	
	public void setActivity(MainActivity activity){
		main=activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.task_fragment, container, false);
		 
		update();
		return view;
	}
	protected void update() {

		// Task Name
		Task_name = (TextView) view.findViewById(R.id.task_name);
		Task_name.setText(main.currentTask.getName());
		Task_name.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ViewSwitcher switcher = (ViewSwitcher) view.findViewById(R.id.switch_name);
				switcher.showNext(); // or switcher.showPrevious();
				EditText txt = (EditText) view.findViewById(R.id.edit_name);
				txt.setText(Task_name.getText());
				txt.requestFocus();

				InputMethodManager imm = (InputMethodManager) main.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(txt, InputMethodManager.SHOW_IMPLICIT);
				txt.setOnEditorActionListener(new OnEditorActionListener() {
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_DONE) {
							EditText txt = (EditText) view.findViewById(R.id.edit_name);
							InputMethodManager imm = (InputMethodManager) main.getSystemService(Context.INPUT_METHOD_SERVICE);
							ViewSwitcher switcher = (ViewSwitcher) view.findViewById(R.id.switch_name);
							main.currentTask.setName(txt.getText().toString());
							main.taskDataSource.saveTask(main.currentTask);
							Task_name.setText(main.currentTask.getName());
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
		Task_done = (CheckBox) view.findViewById(R.id.task_done);
		Task_done.setChecked(main.currentTask.isDone());
		Task_done.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				main.currentTask.setDone(isChecked);
				main.taskDataSource.saveTask(main.currentTask);
			}
		});

		// Task priority
		Task_prio = (TextView) view.findViewById(R.id.task_prio);
		set_prio(Task_prio, main.currentTask);
		Task_prio.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				picker = new NumberPicker(main);
				picker.setMaxValue(4);
				picker.setMinValue(0);
				String[] t = { "-2", "-1", "0", "1", "2" };
				picker.setDisplayedValues(t);
				picker.setWrapSelectorWheel(false);
				picker.setValue(main.currentTask.getPriority() + 2);
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
										main.currentTask.setPriority((picker.getValue() - 2));
										main.taskDataSource.saveTask(main.currentTask);
										TaskActivity.set_prio(Task_prio, main.currentTask);
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
		Task_due = (TextView) view.findViewById(R.id.task_due);
		Drawable due_img = main.getResources().getDrawable(
				android.R.drawable.ic_menu_today);
		due_img.setBounds(0, 0, 60, 60);
		Task_due.setCompoundDrawables(due_img, null, null, null);
		Task_due.setText(main.currentTask.getDue().compareTo(
				new GregorianCalendar(1970, 1, 1)) < 0 ? this
				.getString(R.string.task_no_due) : new SimpleDateFormat(this
				.getString(R.string.dateFormat), Locale.getDefault())
				.format(main.currentTask.getDue().getTime()));

		Task_due.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mIgnoreTimeSet = false;
				GregorianCalendar due = (main.currentTask.getDue().compareTo(
						new GregorianCalendar()) < 0 ? new GregorianCalendar()
						: main.currentTask.getDue());
				DatePickerDialog dialog = new DatePickerDialog(main,
						new OnDateSetListener() {

							@Override
							public void onDateSet(DatePicker view, int year,
									int monthOfYear, int dayOfMonth) {
								if (mIgnoreTimeSet)
									return;

								main.currentTask.setDue(new GregorianCalendar(year,
										monthOfYear, dayOfMonth));
								main.taskDataSource.saveTask(main.currentTask);
								Task_due.setText(new SimpleDateFormat(view
										.getContext().getString(
												R.string.dateFormat), Locale
										.getDefault()).format(main.currentTask.getDue()
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
									main.currentTask.setDue(new GregorianCalendar(0, 1, 1));
									main.taskDataSource.saveTask(main.currentTask);
									Task_due.setText(R.string.task_no_due);
								}
							}
						});
				dialog.show();

			}
		});

		// Task content
		Task_content = (TextView) view.findViewById(R.id.task_content);
		Task_content.setText(main.currentTask.getContent().length() == 0 ? this
				.getString(R.string.task_no_content) : main.currentTask.getContent());
		Drawable content_img = main.getResources()
				.getDrawable(android.R.drawable.ic_menu_edit);
		content_img.setBounds(0, 0, 60, 60);
		Task_content.setCompoundDrawables(content_img, null, null, null);
		Task_content.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ViewSwitcher switcher = (ViewSwitcher) view.findViewById(R.id.switch_content);
				switcher.showNext(); // or switcher.showPrevious();
				EditText txt = (EditText) view.findViewById(R.id.edit_content);
				txt.setText(main.currentTask.getContent());
				txt.requestFocus();

				InputMethodManager imm = (InputMethodManager) main.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(txt, InputMethodManager.SHOW_IMPLICIT);
				Button submit = (Button) view.findViewById(R.id.submit_content);
				submit.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						EditText txt = (EditText) view.findViewById(R.id.edit_content);
						InputMethodManager imm = (InputMethodManager) main.getSystemService(Context.INPUT_METHOD_SERVICE);
						main.currentTask.setContent(txt.getText().toString());
						main.taskDataSource.saveTask(main.currentTask);
						Task_content
								.setText(main.currentTask.getContent().trim().length() == 0 ? getString(R.string.task_no_content)
										: main.currentTask.getContent());
						ViewSwitcher switcher = (ViewSwitcher) view.findViewById(R.id.switch_content);
						switcher.showPrevious();
						imm.hideSoftInputFromWindow(txt.getWindowToken(), 0);

					}
				});
			}
		});
	}

	protected void set_prio(TextView Task_prio, Task task) {
		Task_prio.setText("" + main.currentTask.getPriority());
		Task_prio
				.setBackgroundColor(Mirakel.PRIO_COLOR[main.currentTask.getPriority() + 2]);

	}

}
