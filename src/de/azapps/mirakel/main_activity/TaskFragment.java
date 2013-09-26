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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.Toast;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.main_activity.TaskFragmentAdapter.TYPE;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class TaskFragment extends Fragment {
	private static final String TAG = "TaskActivity";

	public TaskFragmentAdapter adapter;

	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final MainActivity main = (MainActivity) getActivity();
		View view = inflater.inflate(R.layout.task_fragment, container, false);
		ListView listView = (ListView) view.findViewById(R.id.taskFragment);
		final Task task = main.getCurrentTask();
		List<Pair<Integer, Integer>> data = generateData(task);
		Log.d(TAG, data.size() + " entries");
		adapter = new TaskFragmentAdapter(main, R.layout.task_head_line, data,
				main.getCurrentTask());
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				if (adapter.getData().get(position).first == TYPE.FILE) {
					FileMirakel file = FileMirakel.getForTask(task).get(
							adapter.getData().get(position).second);
					String mimetype = Helpers.getMimeType(file.getPath());

					Intent i2 = new Intent();
					i2.setAction(android.content.Intent.ACTION_VIEW);
					i2.setDataAndType(Uri.fromFile(new File(file.getPath())),
							mimetype);
					try {
						main.startActivity(i2);
					} catch (ActivityNotFoundException e) {
						Toast.makeText(main,
								main.getString(R.string.file_no_activity),
								Toast.LENGTH_SHORT).show();
					}
				}

			}
		});
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent,
						View item, int position, final long id) {
					Log.e(TAG, "implement for <3.0");
					return true;
				}
			});
		} else {
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
			if (adapter != null) {
				adapter.resetSelected();
			}
			listView.setHapticFeedbackEnabled(true);
			listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

				@Override
				public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

					return false;
				}

				@Override
				public void onDestroyActionMode(ActionMode mode) {
					adapter.resetSelected();
				}

				@Override
				public boolean onCreateActionMode(ActionMode mode, Menu menu) {
					MenuInflater inflater = mode.getMenuInflater();
					inflater.inflate(R.menu.context_task, menu);
					return true;
				}

				@Override
				public boolean onActionItemClicked(ActionMode mode,
						MenuItem item) {

					switch (item.getItemId()) {
					case R.id.menu_delete:
						List<Pair<Integer, Integer>> selected = adapter
						.getSelected();
						if(adapter.getSelectedCount()>0&&adapter.getSelected().get(0).first==TYPE.FILE){
							List<FileMirakel> files = adapter.getTask().getFiles();
							List<FileMirakel> selectedItems = new ArrayList<FileMirakel>();
							for (Pair<Integer, Integer> p : selected) {
								if (p.first == TYPE.FILE) {
									selectedItems.add(files.get(p.second));
								}
							}
							TaskDialogHelpers.handleDeleteFile(selectedItems, main,
									adapter.getTask(), adapter);
							break;
						}else if(adapter.getSelectedCount()>0&&adapter.getSelected().get(0).first==TYPE.SUBTASK){
							List<Task>subtasks=adapter.getTask().getSubtasks();
							List<Task> selectedItems = new ArrayList<Task>();
							for (Pair<Integer, Integer> p : selected) {
								if (p.first == TYPE.SUBTASK) {
									selectedItems.add(subtasks.get(p.second));
								}
							}
							TaskDialogHelpers.handleRemoveSubtask(selectedItems,main,adapter,adapter.getTask());
						}else{
							Log.e(TAG, "How did you get selected this?");
						}

					default:
						break;
					}
					mode.finish();
					return false;
				}

				@Override
				public void onItemCheckedStateChanged(ActionMode mode,
						int position, long id, boolean checked) {
					Log.d(TAG, "item " + position + " selected");
					if ((adapter.getData().get(position).first == TYPE.FILE && (adapter
							.getSelected().size() == 0 || (adapter
							.getSelected().get(0).first == TYPE.FILE)))
							|| (adapter.getData().get(position).first == TYPE.SUBTASK && (adapter
									.getSelected().size() == 0 || adapter
									.getSelected().get(0).first == TYPE.SUBTASK))) {
						adapter.setSelected(position, checked);
						adapter.notifyDataSetChanged();
					} else if(adapter.getSelectedCount()==0) {
						mode.finish();// No CAB
					}

				}
			});
		}
		Log.d(TAG, "created");
		return view;
	}

	public static List<Pair<Integer, Integer>> generateData(Task task) {
		// TODO implement Subtasks
		List<Pair<Integer, Integer>> data = new ArrayList<Pair<Integer, Integer>>();
		data.add(new Pair<Integer, Integer>(TYPE.HEADER, 0));
		data.add(new Pair<Integer, Integer>(TYPE.DUE, 0));
		data.add(new Pair<Integer, Integer>(TYPE.REMINDER, 0));
		data.add(new Pair<Integer, Integer>(TYPE.CONTENT, 0));
		data.add(new Pair<Integer, Integer>(TYPE.SUBTITLE, 0));
		int subtaskCount = task.getSubtaskCount();
		for (int i = 0; i < subtaskCount; i++) {
			data.add(new Pair<Integer, Integer>(TYPE.SUBTASK, i));
		}
		data.add(new Pair<Integer, Integer>(TYPE.SUBTITLE, 1));
		int fileCount = FileMirakel.getFileCount(task);
		Log.d(TAG, "filecount " + fileCount);
		for (int i = 0; i < fileCount; i++)
			data.add(new Pair<Integer, Integer>(TYPE.FILE, i));
		return data;
	}

	public void update(Task t) {
		if (adapter != null) {
			adapter.setData(generateData(t), t);
			adapter.notifyDataSetChanged();
		}
	}

}
