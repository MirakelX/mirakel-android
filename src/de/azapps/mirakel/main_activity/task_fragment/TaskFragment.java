/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
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
package de.azapps.mirakel.main_activity.task_fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import de.azapps.mirakel.custom_views.BaseTaskDetailRow.OnTaskChangedListner;
import de.azapps.mirakel.custom_views.TaskDetailContent.OnEditChanged;
import de.azapps.mirakel.custom_views.TaskDetailFilePart.OnFileMarkedListner;
import de.azapps.mirakel.custom_views.TaskDetailView;
import de.azapps.mirakel.custom_views.TaskSummary.OnTaskClickListner;
import de.azapps.mirakel.custom_views.TaskSummary.OnTaskMarkedListner;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Helpers.ExecInterfaceWithTask;
import de.azapps.mirakel.helper.MirakelPreferences;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;

public class TaskFragment extends Fragment {
	private enum ActionbarState {
		CONTENT, FILE, SUBTASK;
	}

	private static final String			TAG					= "TaskActivity";

	private ActionbarState				cabState;
	private TaskDetailView				detailView;
	private ActionMode					mActionMode;
	private final ActionMode.Callback	mActionModeCallback	= new ActionMode.Callback() {

		// Called when the user selects a
		// contextual menu item
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item
					.getItemId()) {
						case R.id.save:
							if (TaskFragment.this.detailView != null) {
								TaskFragment.this.detailView
								.saveContent();
							}
							break;
						case R.id.cancel:
							if (TaskFragment.this.detailView != null) {
								TaskFragment.this.detailView
								.cancelContent();
							}
							break;
						case R.id.menu_delete:
							if (TaskFragment.this.cabState == ActionbarState.FILE) {
								List<FileMirakel> selectedItems = new ArrayList<FileMirakel>();
								for (int i = 0; i < TaskFragment.this.markedFiles
										.size(); i++) {
									selectedItems
									.add(TaskFragment.this.markedFiles
											.valueAt(i));
								}
								TaskDialogHelpers
								.handleDeleteFile(
										selectedItems,
										getActivity(),
										TaskFragment.this.task,
										TaskFragment.this);
							} else {// Subtask

								List<Task> selectedItems = new ArrayList<Task>();

								for (Map.Entry<Long, Task> e : TaskFragment.this.markedSubtasks
										.entrySet()) {
									selectedItems
									.add(e.getValue());
								}
								TaskDialogHelpers
								.handleRemoveSubtask(
										selectedItems,
										getActivity(),
										TaskFragment.this,
										TaskFragment.this.task);
							}
							break;
						case R.id.edit_task:
							if (TaskFragment.this.main != null) {
								TaskFragment.this.main
								.setCurrentTask(TaskFragment.this.markedSubtasks
										.entrySet()
										.iterator()
										.next()
										.getValue());
							}
							break;
						case R.id.done_task:
							for (Map.Entry<Long, Task> e : TaskFragment.this.markedSubtasks
									.entrySet()) {
								if (e.getValue() != null) {
									e.getValue()
									.setDone(
											true);
									e.getValue()
									.safeSave();
								}
							}
							update(TaskFragment.this.task);
							TaskFragment.this.main
							.getTasksFragment()
							.updateList();
							TaskFragment.this.main
							.getListFragment()
							.update();
							break;
						default:
							return false;
			}
			mode.finish();
			return true;
		}

		// Called when the action mode is
		// created; startActionMode() was
		// called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource
			// providing context menu items
			MenuInflater inflater = mode
					.getMenuInflater();
			if (TaskFragment.this.cabState == null)
				return false;
			switch (TaskFragment.this.cabState) {
				case CONTENT:
					inflater.inflate(
							R.menu.save,
							menu);
					break;
				case FILE:
					inflater.inflate(
							R.menu.context_file,
							menu);
					break;
				case SUBTASK:
					inflater.inflate(
							R.menu.context_subtask,
							menu);
					break;
				default:
					Log.d(TAG,
							"where are the dragons");
					return false;

			}
			TaskFragment.this.mActionMode = mode;
			TaskFragment.this.mMenu = menu;
			return true;
		}

		// Called when the user exits the
		// action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			TaskFragment.this.mActionMode = null;
			TaskFragment.this.cabState = null;
			if (TaskFragment.this.detailView != null) {
				TaskFragment.this.detailView
				.unmark();
			}
			TaskFragment.this.markedFiles = new SparseArray<FileMirakel>();
			TaskFragment.this.markedSubtasks = new HashMap<Long, Task>();
		}

		// Called each time the action mode
		// is shown. Always called after
		// onCreateActionMode, but
		// may be called multiple times if
		// the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false; // Return false if
			// nothing is
			// done
		}
	};

	private MainActivity	main;
	SparseArray<FileMirakel>			markedFiles;

	Map<Long, Task>						markedSubtasks;

	private Menu mMenu;

	private Task						task;

	public void closeActionMode() {
		if (this.mActionMode != null) {
			this.mActionMode.finish();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.cabState = null;
		this.markedFiles = new SparseArray<FileMirakel>();
		this.markedSubtasks = new HashMap<Long, Task>();

		Log.w(TAG, "taskfragment");
		this.main = (MainActivity) getActivity();
		View view;
		try {
			view = inflater.inflate(R.layout.task_fragment, container, false);
		} catch (Exception e) {
			Log.i(TAG, Log.getStackTraceString(e));
			return null;
		}
		this.detailView = (TaskDetailView) view.findViewById(R.id.taskFragment);
		this.detailView.setOnTaskChangedListner(new OnTaskChangedListner() {

			@Override
			public void onTaskChanged(Task newTask) {
				TaskFragment.this.main.getTasksFragment().updateList();
				TaskFragment.this.main.getListFragment().update();
			}
		});
		this.detailView.setOnSubtaskClick(new OnTaskClickListner() {

			@Override
			public void onTaskClick(Task t) {
				TaskFragment.this.main.setCurrentTask(t);
			}
		});

		if (MirakelPreferences.useBtnCamera()
				&& Helpers.isIntentAvailable(this.main,
						MediaStore.ACTION_IMAGE_CAPTURE)) {
			this.detailView.setAudioButtonClick(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					TaskDialogHelpers.handleAudioRecord(getActivity(),
							TaskFragment.this.main.getCurrentTask(), new ExecInterfaceWithTask() {
						@Override
						public void exec(Task t) {
							update(t);
						}
					});
				}
			});
			this.detailView.setCameraButtonClick(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					try {
						Intent cameraIntent = new Intent(
								MediaStore.ACTION_IMAGE_CAPTURE);
						Uri fileUri = FileUtils
								.getOutputMediaFileUri(FileUtils.MEDIA_TYPE_IMAGE);
						if (fileUri == null) return;
						TaskFragment.this.main.setFileUri(fileUri);
						cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
						getActivity().startActivityForResult(cameraIntent,
								MainActivity.RESULT_ADD_PICTURE);

					} catch (ActivityNotFoundException a) {
						Toast.makeText(
								TaskFragment.this.main,
								"Opps! Your device doesn't support taking photos",
								Toast.LENGTH_SHORT).show();
					}

				}
			});
		}

		if (this.task != null) {
			update(this.task);
		}

		this.detailView.setOnSubtaskMarked(new OnTaskMarkedListner() {

			@Override
			public void markTask(View v, Task t, boolean marked) {
				if (t == null || TaskFragment.this.cabState != null
						&& TaskFragment.this.cabState != ActionbarState.SUBTASK)
					return;
				if (marked) {
					TaskFragment.this.cabState = ActionbarState.SUBTASK;
					if (TaskFragment.this.mActionMode == null) {
						getActivity().startActionMode(
								TaskFragment.this.mActionModeCallback);
					}
					v.setBackgroundColor(Helpers
							.getHighlightedColor(getActivity()));
					TaskFragment.this.markedSubtasks.put(t.getId(), t);

				} else {
					Log.d(TAG, "not marked");
					v.setBackgroundColor(getActivity().getResources().getColor(
							android.R.color.transparent));
					TaskFragment.this.markedSubtasks.remove(t.getId());
					if (TaskFragment.this.markedSubtasks.isEmpty()
							&& TaskFragment.this.mActionMode != null) {
						TaskFragment.this.mActionMode.finish();
					}
				}
				if (TaskFragment.this.mMenu != null) {
					MenuItem item=TaskFragment.this.mMenu.findItem(R.id.edit_task);
					if(TaskFragment.this.markedSubtasks.size()>1&&item!=null){
						item.setVisible(false);
					}else if(TaskFragment.this.mActionMode!=null&&item!=null){
						item.setVisible(true);
					}
				}
			}
		});

		this.detailView.setOnFileMarked(new OnFileMarkedListner() {

			@Override
			public void markFile(View v, FileMirakel e, boolean marked) {
				if (e == null || TaskFragment.this.cabState != null
						&& TaskFragment.this.cabState != ActionbarState.FILE)
					return;
				if (marked) {
					TaskFragment.this.cabState = ActionbarState.FILE;
					if (TaskFragment.this.mActionMode == null) {
						getActivity().startActionMode(
								TaskFragment.this.mActionModeCallback);
					}
					v.setBackgroundColor(Helpers
							.getHighlightedColor(getActivity()));
					TaskFragment.this.markedFiles.put(e.getId(), e);
				} else {
					v.setBackgroundColor(getActivity().getResources().getColor(
							android.R.color.transparent));
					TaskFragment.this.markedFiles.remove(e.getId());
					if (TaskFragment.this.markedFiles.size() == 0
							&& TaskFragment.this.mActionMode != null) {
						TaskFragment.this.mActionMode.finish();
					}
				}
			}
		});

		this.detailView.setOnContentEdit(new OnEditChanged() {

			@Override
			public void handleCab(boolean startEdit) {
				if (startEdit) {
					TaskFragment.this.cabState = ActionbarState.CONTENT;
					getActivity().startActionMode(
							TaskFragment.this.mActionModeCallback);
				} else if (TaskFragment.this.mActionMode != null) {
					TaskFragment.this.mActionMode.finish();
				}

			}
		});

		return view;
	}

	public void update(Task t) {
		this.task = t;
		if (this.detailView != null) {
			this.detailView.update(t);
		}

	}

	public void updateLayout() {
		if (this.detailView != null) {
			this.detailView.updateLayout();
		}

	}

}
