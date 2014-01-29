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
package de.azapps.mirakel.main_activity.tasks_fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Helpers.ExecInterfaceWithTask;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragment;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentV14;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentV8;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

public class TasksFragment extends android.support.v4.app.Fragment {
	private static final String	TAG				= "TasksFragment";
	private static final int	TASK_RENAME		= 0, TASK_MOVE = 1,
			TASK_DESTROY = 2;
	private TaskAdapter			adapter;
	private boolean				created			= false;
	private boolean				finishLoad;
	private int					ItemCount;
	private int					listId;
	private ListView			listView;
	private boolean				loadMore;
	private ActionMode			mActionMode		= null;
	private MainActivity		main;
	final Handler				mHandler		= new Handler();
	final Runnable				mUpdateResults	= new Runnable() {
		@Override
		public void run() {
			TasksFragment.this.adapter.changeData(
					new ArrayList<Task>(
							TasksFragment.this.values.subList(
									0,
									TasksFragment.this.ItemCount > TasksFragment.this.values
									.size() ? TasksFragment.this.values
											.size()
											: TasksFragment.this.ItemCount)),
											TasksFragment.this.listId);
			TasksFragment.this.adapter.notifyDataSetChanged();
		}
	};
	protected EditText			newTask;
	private boolean				showDone		= true;
	private List<Task>			values;

	View						view;

	public void clearFocus() {
		if (this.newTask != null) {
			this.newTask.postDelayed(new Runnable() {

				@Override
				public void run() {
					Log.d(TAG, "clear focus");
					if (TasksFragment.this.newTask == null) return;
					TasksFragment.this.newTask.setOnFocusChangeListener(null);
					TasksFragment.this.newTask.clearFocus();
					if (getActivity() == null) return;

					InputMethodManager imm = (InputMethodManager) getActivity()
							.getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(TasksFragment.this.newTask.getWindowToken(), 0);
					getActivity().getWindow().setSoftInputMode(
							WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

				}
			}, 10);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void closeActionMode() {
		if (this.mActionMode != null
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			this.mActionMode.finish();
		}
	}

	public void focusNew(final boolean request_focus) {
		if (this.newTask == null) return;
		this.newTask.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(final View v, final boolean hasFocus) {
				if (TasksFragment.this.main.getCurrentPosition() != MainActivity
						.getTasksFragmentPosition()) return;
				TasksFragment.this.newTask.post(new Runnable() {
					@Override
					public void run() {
						Log.d(TAG, "focus new " + hasFocus);
						InputMethodManager imm = (InputMethodManager) getActivity()
								.getSystemService(Context.INPUT_METHOD_SERVICE);
						if (imm == null) {
							Log.w(TAG, "Inputmanager==null");
							return;
						}
						imm.restartInput(TasksFragment.this.newTask);
						if (request_focus && hasFocus) {

							getActivity()
							.getWindow()
							.setSoftInputMode(
									WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
							imm.showSoftInput(TasksFragment.this.newTask,
									InputMethodManager.SHOW_IMPLICIT);
							getActivity()
							.getWindow()
							.setSoftInputMode(
									WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
						} else if (!hasFocus) {
							TasksFragment.this.newTask.requestFocus();
							imm.showSoftInput(TasksFragment.this.newTask,
									InputMethodManager.SHOW_IMPLICIT);
						} else if (!request_focus) {
							clearFocus();
						}
					}
				});
			}
		});
		this.newTask.requestFocus();
	}

	public TaskAdapter getAdapter() {
		return this.adapter;
	}

	public View getFragmentView() {
		return this.view;
	}

	public ListView getListView() {
		return this.listView;
	}

	public boolean isReady() {
		return this.newTask!=null;
	}

	private boolean newTask(String name) {
		this.newTask.setText(null);
		InputMethodManager imm = (InputMethodManager) this.main
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(this.newTask.getWindowToken(), 0);
		this.newTask.clearFocus();
		if (name.equals("")) {
			this.newTask.setOnFocusChangeListener(null);
			imm.showSoftInput(this.newTask, InputMethodManager.HIDE_IMPLICIT_ONLY);
			return true;
		}

		ListMirakel list = this.main.getCurrentList();
		Task task = Semantic.createTask(name, list,
				MirakelPreferences.useSemanticNewTask(), getActivity());

		this.adapter.addToHead(task);
		this.values.add(0, task);
		this.adapter.notifyDataSetChanged();

		this.main.getListFragment().update();
		if (!MirakelPreferences.hideKeyboard()) {
			focusNew(true);
		}
		this.main.updateShare();
		return true;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.finishLoad = false;
		this.loadMore = false;
		this.ItemCount = 0;
		this.main = (MainActivity) getActivity();
		this.showDone = MirakelPreferences.showDoneMain();
		this.listId = this.main.getCurrentList().getId();

		if (MirakelPreferences.isTablet()) {
			this.view = inflater.inflate(R.layout.tasks_fragment_tablet, container,
					false);
			TaskFragment t;
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				t = new TaskFragmentV8();
			} else {
				t = new TaskFragmentV14();
			}
			getChildFragmentManager().beginTransaction()
			.replace(R.id.task_fragment_in_tasks, t).commit();
			this.main.setTaskFragment(t);
		} else {
			this.view = inflater.inflate(R.layout.tasks_fragment_phone, container,
					false);
		}
		// getResources().getString(R.string.action_settings);
		try {
			this.values = this.main.getCurrentList().tasks(this.showDone);
		} catch (NullPointerException e) {
			this.values = null;
		}
		this.adapter = null;
		this.created = true;

		this.listView = (ListView) this.view.findViewById(R.id.tasks_list);
		this.listView.setDescendantFocusability(ListView.FOCUS_AFTER_DESCENDANTS);
		// Events
		this.newTask = (EditText) this.view.findViewById(R.id.tasks_new);
		if (MirakelPreferences.isTablet()) {
			this.newTask.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
		}
		this.newTask.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND
						|| actionId == EditorInfo.IME_NULL && event
						.getAction() == KeyEvent.ACTION_DOWN) {
					newTask(v.getText().toString());
					v.setText(null);
				}
				return false;
			}
		});
		this.newTask.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				ImageButton send = (ImageButton) TasksFragment.this.view
						.findViewById(R.id.btnEnter);
				if (s.length() > 0) {
					send.setVisibility(View.VISIBLE);
				} else {
					send.setVisibility(View.GONE);
				}

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});
		this.ItemCount = 10;// TODO get this from somewhere
		update(true);
		this.listView.setOnScrollListener(new AbsListView.OnScrollListener() {

			@Override
			public void onScroll(AbsListView v, int firstVisibleItem, int visibleItemCount, final int totalItemCount) {
				int lastInScreen = firstVisibleItem + visibleItemCount;
				if (lastInScreen == totalItemCount && !TasksFragment.this.loadMore
						&& TasksFragment.this.finishLoad && TasksFragment.this.adapter != null
						&& TasksFragment.this.values.size() > totalItemCount) {
					TasksFragment.this.ItemCount = totalItemCount + 2;
					update(false);
				}
			}

			@Override
			public void onScrollStateChanged(AbsListView v, int scrollState) {
				// Nothing

			}
		});

		ImageButton btnEnter = (ImageButton) this.view.findViewById(R.id.btnEnter);
		btnEnter.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				newTask(TasksFragment.this.newTask.getText().toString());
				TasksFragment.this.newTask.setText(null);

			}
		});

		updateButtons();
		// Inflate the layout for this fragment
		return this.view;
	}

	@Override
	public void onResume() {
		super.onResume();
		this.showDone = MirakelPreferences.showDoneMain();
	}

	public void setActivity(MainActivity activity) {
		this.main = activity;
	}

	public void setScrollPosition(int pos) {
		if (this.listView == null) return;
		if (this.listView.getCount() > pos) {
			this.listView.setSelectionFromTop(pos, 0);
		} else {
			this.listView.setSelectionFromTop(0, 0);
		}
	}

	public void setTasks(List<Task> tasks) {
		this.values = tasks;
	}

	@SuppressLint("NewApi")
	protected void update(boolean reset) {
		if (!this.created) return;
		if (this.values == null) {
			try {
				this.values = this.main.getCurrentList().tasks(this.showDone);
			} catch (NullPointerException w) {
				this.values = null;
				return;
			}
		}
		if (this.adapter != null && this.finishLoad) {
			this.mHandler.post(this.mUpdateResults);
			if (reset) {
				setScrollPosition(0);
			}
			return;
		}
		if (this.adapter != null) {
			this.adapter.resetSelected();
		}

		// main.showMessageFromSync();

		this.listView = (ListView) this.view.findViewById(R.id.tasks_list);
		AsyncTask<Void, Void, TaskAdapter> asyncTask = new AsyncTask<Void, Void, TaskAdapter>() {
			@Override
			protected TaskAdapter doInBackground(Void... params) {
				TasksFragment.this.adapter = new TaskAdapter(
						TasksFragment.this.main,
						R.layout.task_summary,
						new ArrayList<Task>(TasksFragment.this.values.subList(0,
								TasksFragment.this.ItemCount > TasksFragment.this.values.size() ? TasksFragment.this.values.size()
										: TasksFragment.this.ItemCount)), TasksFragment.this.main.getCurrentList().getId());
				return TasksFragment.this.adapter;
			}

			@Override
			protected void onPostExecute(TaskAdapter a) {
				TasksFragment.this.listView
				.setAdapter(TasksFragment.this.adapter);
				TasksFragment.this.finishLoad = true;
			}
		};

		asyncTask.execute();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			this.listView
			.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View item, int position, final long id) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							getActivity());
					Task task = TasksFragment.this.values.get((int) id);
					builder.setTitle(task.getName());
					List<CharSequence> items = new ArrayList<CharSequence>(
							Arrays.asList(getActivity().getResources()
									.getStringArray(R.array.task_actions_items)));

					builder.setItems(
							items.toArray(new CharSequence[items.size()]),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int item) {
									Task t = TasksFragment.this.values.get((int) id);
									switch (item) {
										case TASK_RENAME:
											TasksFragment.this.main.setCurrentTask(t);
											break;
										case TASK_MOVE:
											TasksFragment.this.main.handleMoveTask(t);
											break;
										case TASK_DESTROY:
											TasksFragment.this.main.handleDestroyTask(t);
											break;
										default:
											break;
									}
								}
							});

					AlertDialog dialog = builder.create();
					dialog.show();
					return true;
				}
			});
		} else {
			this.listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
			if (this.adapter != null) {
				this.adapter.resetSelected();
			}
			this.listView.setHapticFeedbackEnabled(true);
			this.listView
			.setMultiChoiceModeListener(new MultiChoiceModeListener() {

				@Override
				public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
					List<Task> tasks = TasksFragment.this.adapter.getSelected();
					switch (item.getItemId()) {
						case R.id.menu_delete:
							TasksFragment.this.main.handleDestroyTask(tasks);
							break;
						case R.id.menu_move:
							TasksFragment.this.main.handleMoveTask(tasks);
							break;
						case R.id.edit_task:
							TasksFragment.this.main.setCurrentTask(tasks.get(0), true);
							break;
						case R.id.done_task:
							for (Task t : tasks) {
								t.setDone(true);
								try {
									t.save();
								} catch (NoSuchListException e) {
									Log.d(TAG, "list did vanish");
								}
							}
							TasksFragment.this.adapter.notifyDataSetChanged();
							break;
						default:
							break;
					}
					mode.finish();
					return false;
				}

				@Override
				public boolean onCreateActionMode(ActionMode mode, Menu menu) {
					MenuInflater inflater = mode.getMenuInflater();
					inflater.inflate(R.menu.context_tasks, menu);
					TasksFragment.this.mActionMode = mode;
					clearFocus();
					return true;
				}

				@Override
				public void onDestroyActionMode(ActionMode mode) {
					TasksFragment.this.adapter.resetSelected();
				}

				@Override
				public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
					int oldCount = TasksFragment.this.adapter.getSelectedCount();
					TasksFragment.this.adapter.setSelected(position, checked);
					int newCount = TasksFragment.this.adapter.getSelectedCount();
					mode.setTitle(TasksFragment.this.main.getResources().getQuantityString(
							R.plurals.selected_tasks, newCount, newCount));
					if (oldCount < 2 && newCount >= 2
							|| oldCount >= 2 && newCount < 2) {
						mode.invalidate();
					}

				}

				@Override
				public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
					menu.findItem(R.id.edit_task).setVisible(
							TasksFragment.this.adapter.getSelectedCount() <= 1);
					return false;
				}
			});
		}
		this.listView
		.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View item, int position, long id) {
				// TODO Remove Bad Hack
				Task task = TasksFragment.this.values.get((int) id);
				Log.v(TAG, "Switch to Task " + task.getId() + " ("
						+ task.getUUID() + ")");
				TasksFragment.this.main.setCurrentTask(task, true);
			}
		});
	}

	public void updateButtons() {
		// a) Android 2.3 dosen't support speech toText
		// b) The user can switch off the button
		if (this.view == null) return;
		if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.HONEYCOMB
				|| !MirakelPreferences.useBtnSpeak()) {
			this.view.findViewById(R.id.btnSpeak_tasks).setVisibility(View.GONE);
		} else {
			ImageButton btnSpeak = (ImageButton) this.view
					.findViewById(R.id.btnSpeak_tasks);
			// txtText = newTask;
			btnSpeak.setVisibility(View.VISIBLE);
			btnSpeak.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {

					Intent intent = new Intent(
							RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

					intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
							TasksFragment.this.main.getString(R.string.speak_lang_code));

					try {
						getActivity().startActivityForResult(intent,
								MainActivity.RESULT_SPEECH);
						TasksFragment.this.newTask.setText("");
					} catch (ActivityNotFoundException a) {
						Toast t = Toast
								.makeText(
										TasksFragment.this.main,
										"Opps! Your device doesn't support Speech to Text",
										Toast.LENGTH_SHORT);
						t.show();
					}
				}
			});
		}
		if (!MirakelPreferences.useBtnAudioRecord()) {
			this.view.findViewById(R.id.btnAudio_tasks).setVisibility(View.GONE);
		} else {
			ImageButton btnAudio = (ImageButton) this.view
					.findViewById(R.id.btnAudio_tasks);
			btnAudio.setVisibility(View.VISIBLE);
			btnAudio.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO BAHHHH this is ugly!
					final Task task = new Task("");
					task.setList(TasksFragment.this.main.getCurrentList(), true);
					task.setId(0);
					TaskDialogHelpers.handleAudioRecord(TasksFragment.this.main, task,
							new ExecInterfaceWithTask() {
						@Override
						public void exec(Task t) {
							TasksFragment.this.main.setCurrentList(t.getList());
							TasksFragment.this.main.setCurrentTask(t, true);
						}
					});
				}
			});

		}
		if (!MirakelPreferences.useBtnCamera()
				|| !Helpers.isIntentAvailable(this.main,
						MediaStore.ACTION_IMAGE_CAPTURE)) {
			this.view.findViewById(R.id.btnCamera).setVisibility(View.GONE);
		} else {
			ImageButton btnCamera = (ImageButton) this.view
					.findViewById(R.id.btnCamera);
			btnCamera.setVisibility(View.VISIBLE);
			btnCamera.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						Intent cameraIntent = new Intent(
								MediaStore.ACTION_IMAGE_CAPTURE);
						Uri fileUri = FileUtils
								.getOutputMediaFileUri(FileUtils.MEDIA_TYPE_IMAGE);
						if (fileUri == null) return;
						TasksFragment.this.main.setFileUri(fileUri);
						cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
						getActivity().startActivityForResult(cameraIntent,
								MainActivity.RESULT_CAMERA);

					} catch (ActivityNotFoundException a) {
						Toast.makeText(
								TasksFragment.this.main,
								"Opps! Your device doesn't support taking photos",
								Toast.LENGTH_SHORT).show();
					}
				}
			});
		}
	}

	public void updateList() {
		updateList(true);
	}

	public void updateList(final boolean reset) {
		try {
			this.listId = this.main.getCurrentList().getId();
			this.values = this.main.getCurrentList().tasks(this.showDone);
			update(reset);
		} catch (NullPointerException e) {
			this.values = null;
		}
	}

}
