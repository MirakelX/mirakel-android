package de.azapps.mirakel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import android.widget.ImageButton;
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
	private Task task;
	
	private boolean mIgnoreTimeSet = false;
	
	public void setActivity(MainActivity activity){
		main=activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.activity_task, container, false);
		 
		update();
		return view;
	}
	protected void update() {

		// Task Name
		task=main.getCurrentTask();
		Task_name = (TextView) view.findViewById(R.id.task_name);
		Task_name.setText(task.getName());
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
							task.setName(txt.getText().toString());
							main.saveTask(task);
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
		Task_done = (CheckBox) view.findViewById(R.id.task_done);
		Task_done.setChecked(task.isDone());
		Task_done.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				task.setDone(isChecked);
				main.saveTask(task);
			}
		});

		// Task priority
		Task_prio = (TextView) view.findViewById(R.id.task_prio);
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
										main.saveTask(task);
										set_prio(Task_prio, task);
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
								main.saveTask(task);
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
									main.saveTask(task);
									Task_due.setText(R.string.task_no_due);
								}
							}
						});
				dialog.show();

			}
		});

		// Task content
		Task_content = (TextView) view.findViewById(R.id.task_content);
		Task_content.setText(task.getContent().length() == 0 ? this
				.getString(R.string.task_no_content) : task.getContent());
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
				txt.setText(task.getContent());
				txt.requestFocus();

				InputMethodManager imm = (InputMethodManager) main.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(txt, InputMethodManager.SHOW_IMPLICIT);
				Button submit = (Button) view.findViewById(R.id.submit_content);
				submit.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						EditText txt = (EditText) view.findViewById(R.id.edit_content);
						InputMethodManager imm = (InputMethodManager) main.getSystemService(Context.INPUT_METHOD_SERVICE);
						task.setContent(txt.getText().toString());
						main.saveTask(task);
						Task_content
								.setText(task.getContent().trim().length() == 0 ? getString(R.string.task_no_content)
										: task.getContent());
						ViewSwitcher switcher = (ViewSwitcher) view.findViewById(R.id.switch_content);
						switcher.showPrevious();
						imm.hideSoftInputFromWindow(txt.getWindowToken(), 0);

					}
				});
			}
		});
		

		//VoiceToText
		ImageButton btnSpeakName = (ImageButton) view.findViewById(R.id.btnSpeak_task_name);
		//txtText = newTask;


		btnSpeakName.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent intent = new Intent(
						RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, main.getString(R.string.speak_lang_code));

				try {
					getActivity().startActivityForResult(intent, MainActivity.RESULT_SPEECH_NAME);
					Task_name.setText("");
				} catch (ActivityNotFoundException a) {
					Toast t = Toast.makeText(main,
							"Opps! Your device doesn't support Speech to Text",
							Toast.LENGTH_SHORT);
					t.show();
				}
			}
		});
		ImageButton btnSpeakContent = (ImageButton) view.findViewById(R.id.btnSpeak_task_content);
		//txtText = newTask;


		btnSpeakContent.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent intent = new Intent(
						RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, main.getString(R.string.speak_lang_code));

				try {
					getActivity().startActivityForResult(intent, MainActivity.RESULT_SPEECH_CONTENT);
					Task_content.setText("");
				} catch (ActivityNotFoundException a) {
					Toast t = Toast.makeText(main,
							"Opps! Your device doesn't support Speech to Text",
							Toast.LENGTH_SHORT);
					t.show();
				}
			}
		});
	}

	protected void set_prio(TextView Task_prio, Task task) {
		Task_prio.setText("" + task.getPriority());
		Task_prio
				.setBackgroundColor(Mirakel.PRIO_COLOR[task.getPriority() + 2]);

	}
	


}
