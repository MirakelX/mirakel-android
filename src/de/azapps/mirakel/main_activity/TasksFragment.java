/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.main_activity;

import java.util.GregorianCalendar;
import java.util.List;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
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
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.TaskBase;

public class TasksFragment extends Fragment {
	private static final String TAG = "TasksActivity";
	private TaskAdapter adapter;
	private NumberPicker picker;
	private MainActivity main;
	View view;
	private EditText newTask;
	private boolean created = false;
	private ListView listView;

	public void setActivity(MainActivity activity) {
		main = activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		main = (MainActivity) getActivity();
		view = inflater.inflate(R.layout.activity_tasks, container, false);

		getResources().getString(R.string.action_settings);
		created = true;
		update();

		listView = (ListView) view.findViewById(R.id.tasks_list);
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
		InputMethodManager imm = (InputMethodManager) main
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(newTask.getWindowToken(), 0);
		if (name.equals(""))
			return true;
		long id = main.getCurrentList().getId();
		GregorianCalendar due = null;
		if (id <= 0) {
			if (id == ListMirakel.DAILY) {
				due = new GregorianCalendar();
			} else if (id == ListMirakel.WEEKLY) {
				due = new GregorianCalendar();
				due.add(GregorianCalendar.DAY_OF_MONTH, 7);
			}
			try {
				id = ListMirakel.first().getId();
			} catch (NullPointerException e) {
				id = 0;
				Toast.makeText(main, R.string.no_lists, Toast.LENGTH_LONG)
						.show();
			}
		}
		TaskBase task = main.getTaskDataSource().createTask(name, id);
		task.setDue(due);

		adapter.addToHead(task);
		main.getListFragment().update();
		adapter.notifyDataSetChanged();
		// adapter.swapCursor(updateListCursor());
		return true;
	}

	public void update() {
		if (!created)
			return;
		final List<TaskBase> values = main.getTaskDataSource().getTasks(
				main.getCurrentList(), main.getCurrentList().getSortBy());
		final ListView listView = (ListView) view.findViewById(R.id.tasks_list);
		AsyncTask<Void, Void, TaskAdapter> task = new AsyncTask<Void, Void, TaskAdapter>() {
			@Override
			protected TaskAdapter doInBackground(Void... params) {
				adapter = new TaskAdapter(main, R.layout.tasks_row, values,
						new OnClickListener() {
							@Override
							public void onClick(View v) {
								CheckBox cb = (CheckBox) v;
								TaskBase task = (TaskBase) cb.getTag();
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
								picker.setValue(((TaskBase) v.getTag())
										.getPriority() + 2);
								new AlertDialog.Builder(main)
										.setTitle(
												main.getString(R.string.task_change_prio_title))
										.setMessage(
												main.getString(R.string.task_change_prio_cont))
										.setView(picker)
										.setPositiveButton(
												main.getString(R.string.OK),
												new DialogInterface.OnClickListener() {
													public void onClick(
															DialogInterface dialog,
															int whichButton) {
														TaskBase task = (TaskBase) v
																.getTag();
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
						}, main.getCurrentList().getId());
				return adapter;
			}

			@Override
			protected void onPostExecute(TaskAdapter adapter) {
				listView.setAdapter(adapter);
			}
		};

		task.execute();
		// listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item,
					int position, long id) {
				// TODO Remove Bad Hack
				TaskBase t = values.get((int) id);
				Log.v(TAG, "Switch to Task " + t.getId());
				main.setCurrentTask(t);
			}
		});
	}

	/**
	 * Get the State of the listView
	 * 
	 * @return
	 */
	public Parcelable getState() {
		return listView == null ? null : listView.onSaveInstanceState();
	}

	/**
	 * Set the State of the listView
	 * 
	 * @param state
	 */
	public void setState(Parcelable state) {
		if (listView == null || state == null)
			return;
		listView.onRestoreInstanceState(state);
	}

}
