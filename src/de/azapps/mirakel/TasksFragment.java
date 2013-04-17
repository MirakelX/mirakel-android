package de.azapps.mirakel;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class TasksFragment extends Fragment {
	private static final String TAG = "TasksActivity";
	private TaskAdapter adapter;
	private NumberPicker picker;
	private MainActivity main;
	View view;
	private EditText newTask;

	public void setActivity(MainActivity activity) {
		main = activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		main=(MainActivity) getActivity();
		view = inflater.inflate(R.layout.activity_tasks, container, false);

		getResources().getString(R.string.action_settings);
		update();

		// Events
		newTask = (EditText) view.findViewById(R.id.tasks_new);
		newTask.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					newTask(v.getText().toString());
					v.setText(null);
				}
				return false;
			}
		});

		ImageButton btnEnter = (ImageButton) view.findViewById(R.id.btnEnter);
		btnEnter.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				newTask(newTask.getText().toString());
				newTask.setText(null);

			}
		});

		ImageButton btnSpeak = (ImageButton) view
				.findViewById(R.id.btnSpeak_tasks);
		// txtText = newTask;

		btnSpeak.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent intent = new Intent(
						RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
						main.getString(R.string.speak_lang_code));

				try {
					getActivity().startActivityForResult(intent,
							MainActivity.RESULT_SPEECH);
					newTask.setText("");
				} catch (ActivityNotFoundException a) {
					Toast t = Toast.makeText(main,
							"Opps! Your device doesn't support Speech to Text",
							Toast.LENGTH_SHORT);
					t.show();
				}
			}
		});

		// Inflate the layout for this fragment
		return view;
	}

	private boolean newTask(String name) {

		Log.v(TAG, "New Task");
		long id = main.getCurrentList().getId();
		Log.v(TAG, "Create in " + id);
		InputMethodManager imm = (InputMethodManager) main.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(newTask.getWindowToken(), 0);
		if (id <= 0) {
			try {
				id = main.getListDataSource().getFirstList().getId();
			} catch (NullPointerException e) {
				Toast.makeText(main, R.string.no_lists, Toast.LENGTH_LONG)
						.show();
				return false;
			}
		}
		Task task = main.getTaskDataSource().createTask(name, id);
		adapter.addToHead(task);
		
		adapter.notifyDataSetChanged();
		// adapter.swapCursor(updateListCursor());
		return true;
	}

	public void update() {
		Log.v(TAG, "loading...");
		if (main.getCurrentList() == null || view==null)
			return;
		Log.v(TAG, "loading..." + main.getCurrentList().getId());
		// TODO Does not work properly
		final List<Task> values = main.getTaskDataSource().getTasks(
				main.getCurrentList(), main.getCurrentList().getSortBy());
		adapter = new TaskAdapter(main, R.layout.tasks_row, values,
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						CheckBox cb = (CheckBox) v;
						Task task = (Task) cb.getTag();
						task.toggleDone();
						main.saveTask(task);
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
												main.saveTask(task);
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
		ListView listView = (ListView) view.findViewById(R.id.tasks_list);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				// TODO Remove Bad Hack
				Task t = values.get((int) id);
				Log.v(TAG, "Switch to Task " + t.getId());
				main.setCurrentTask(t);
			}
		});
		switch (main.getCurrentList().getId()) {
		case Mirakel.LIST_ALL:
			main.setTitle(this.getString(R.string.list_all));
			break;
		case Mirakel.LIST_DAILY:
			main.setTitle(this.getString(R.string.list_today));
			break;
		case Mirakel.LIST_WEEKLY:
			main.setTitle(this.getString(R.string.list_week));
			break;
		default:
			main.setTitle(main.getCurrentList().getName());
		}
	}

}
