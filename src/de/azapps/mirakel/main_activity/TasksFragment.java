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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import de.azapps.mirakel.helper.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import de.azapps.mirakel.R;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;

public class TasksFragment extends Fragment {
	private static final String TAG = "TasksActivity";
	private TaskAdapter adapter;
	private MainActivity main;
	View view;
	private EditText newTask;
	private boolean created = false;
	private boolean finishLoad;
	private boolean loadMore;
	private ListView listView;
	private int ItemCount;
	private List<Task> values;
	private static final int TASK_RENAME = 0, TASK_MOVE = 1, TASK_DESTROY = 2;
	private int listId;

	final Handler mHandler = new Handler();

	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			adapter.changeData(
					new ArrayList<Task>(values.subList(0, ItemCount > values
							.size() ? values.size() : ItemCount)), listId);
			adapter.notifyDataSetChanged();
		}
	};

	public void setActivity(MainActivity activity) {
		main = activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		finishLoad = false;
		loadMore = false;
		ItemCount = 0;
		main = (MainActivity) getActivity();
		listId = main.getCurrentList().getId();
		view = inflater.inflate(R.layout.activity_tasks, container, false);

		getResources().getString(R.string.action_settings);
		try {
			values = main.getCurrentList().tasks();
		} catch (NullPointerException e) {
			values = null;
		}
		if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.HONEYCOMB) {
			view.findViewById(R.id.btnSpeak_tasks).setVisibility(View.GONE);// Android
																			// 2.3
																			// dosen't
																			// support
																			// speech
																			// to
																			// Text
		}
		adapter = null;
		created = true;

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
		ItemCount = 10;// TODO get this from somewhere
		update(true);
		listView.setOnScrollListener(new AbsListView.OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// Nothing

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, final int totalItemCount) {
				int lastInScreen = firstVisibleItem + visibleItemCount;
				if ((lastInScreen == totalItemCount) && !(loadMore)
						&& finishLoad && adapter != null
						&& values.size() > totalItemCount) {
					ItemCount = totalItemCount + 2;
					update(false);
				}
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

	public void focusNew() {
		if(newTask==null) return;
		// Hmm… Something is going wrong here…
		
		newTask.requestFocus();

		InputMethodManager imm = (InputMethodManager) main
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(newTask, InputMethodManager.SHOW_IMPLICIT);
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
			try {
				SpecialList slist = (SpecialList) main.getCurrentList();
				id = slist.getDefaultList().getId();
				if (slist.getDefaultDate() != null) {
					due = new GregorianCalendar();
					due.add(GregorianCalendar.DAY_OF_MONTH,
							slist.getDefaultDate());
				}
			} catch (NullPointerException e) {
				id = 0;
				due = null;
				Toast.makeText(main, R.string.no_lists, Toast.LENGTH_LONG)
						.show();
			}
		}
		Task task = Task.newTask(name, id);
		task.setDue(due);
		task.save();

		adapter.addToHead(task);
		values.add(0, task);
		main.getListFragment().update();
		adapter.notifyDataSetChanged();
		return true;
	}

	public void updateList() {
		updateList(true);
	}

	public void updateList(final boolean reset) {
		try {
			listId = main.getCurrentList().getId();
			values = main.getCurrentList().tasks();
			update(reset);
		} catch (NullPointerException e) {
			values = null;
		}
	}

	public void setTasks(List<Task> tasks) {
		values = tasks;
	}

	protected void update(boolean reset) {
		if (!created)
			return;
		if (values == null) {
			try {
				values = main.getCurrentList().tasks();
			} catch (NullPointerException w) {
				values = null;
				return;
			}
		}
		if (adapter != null && finishLoad) {
			mHandler.post(mUpdateResults);
			if (reset)
				setScrollPosition(0);
			return;
		}
		final ListView listView = (ListView) view.findViewById(R.id.tasks_list);
		AsyncTask<Void, Void, TaskAdapter> task = new AsyncTask<Void, Void, TaskAdapter>() {
			@Override
			protected TaskAdapter doInBackground(Void... params) {
				adapter = new TaskAdapter(main, R.layout.tasks_row,
						new ArrayList<Task>(values.subList(0,
								ItemCount > values.size() ? values.size()
										: ItemCount)), new OnClickListener() {
							@Override
							public void onClick(View v) {
								Task task = (Task) v.getTag();
								task.toggleDone();
								main.saveTask(task);
								ReminderAlarm.updateAlarms(getActivity());
							}
						}, new OnClickListener() {
							@Override
							public void onClick(final View v) {
								final Task task = (Task) v.getTag();
								TaskDialogHelpers.handlePriority(main, task,
										new ExecInterface() {

											@Override
											public void exec() {
												main.updatesForTask(task);
											}
										});

							}
						}, main.getCurrentList().getId());
				return adapter;
			}

			@Override
			protected void onPostExecute(TaskAdapter adapter) {
				listView.setAdapter(adapter);
				finishLoad = true;
			}
		};

		task.execute();
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View item,
					int position, final long id) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				Task task = values.get((int) id);
				builder.setTitle(task.getName());
				List<CharSequence> items = new ArrayList<CharSequence>(Arrays
						.asList(getActivity().getResources().getStringArray(
								R.array.task_actions_items)));

				builder.setItems(items.toArray(new CharSequence[items.size()]),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								Task task = values.get((int) id);
								switch (item) {
								case TASK_RENAME:
									main.setCurrentTask(task);
									break;
								case TASK_MOVE:
									main.handleMoveTask(task);
									break;
								case TASK_DESTROY:
									main.handleDestroyTask(task);
									break;
								}
							}
						});

				AlertDialog dialog = builder.create();
				dialog.show();
				return false;
			}
		});
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

	public void setScrollPosition(int pos) {
		if (listView == null)
			return;
		if (listView.getCount() > pos)
			listView.setSelectionFromTop(pos, 0);
		else
			listView.setSelectionFromTop(0, 0);
	}

}
