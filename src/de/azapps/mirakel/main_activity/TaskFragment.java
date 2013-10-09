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
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import de.azapps.mirakel.Mirakel.NoSuchListException;
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
		adapter = new TaskFragmentAdapter(main, R.layout.task_head_line,
				main.getCurrentTask());
		listView.setAdapter(adapter);
		listView.setItemsCanFocus(true);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				int type = adapter.getData().get(position).first;
				if (type == TYPE.FILE) {
					FileMirakel file = main.getCurrentTask().getFiles()
							.get(adapter.getData().get(position).second);
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
				} else if (type == TYPE.SUBTASK) {
					Task t = adapter.getTask().getSubtasks()
							.get(adapter.getData().get(position).second);
					main.setGoBackTo(adapter.getTask());
					if (t.getList().getId() != main.getCurrentList().getId()) {
						main.setCurrentList(t.getList(), null, false, false);
					}
					main.setCurrentTask(t, true, false);
				}

			}
		});
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent,
						View item, final int position, final long id) {
					Integer typ = adapter.getData().get(position).first;
					if (typ == TYPE.SUBTASK) {
						AlertDialog.Builder builder = new AlertDialog.Builder(
								getActivity());
						builder.setTitle(adapter.getTask().getSubtasks()
								.get(adapter.getData().get(position).second)
								.getName());

						builder.setItems(
								new String[] { getString(R.string.remove_subtask) },
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										if (which == 0) {
											List<Task> l = new ArrayList<Task>();
											l.add(adapter
													.getTask()
													.getSubtasks()
													.get(adapter.getData().get(
															position).second));
											TaskDialogHelpers
													.handleRemoveSubtask(l,
															main, adapter,
															adapter.getTask());
										}

									}

								});
						builder.create().show();
					} else if (typ == TYPE.FILE) {
						AlertDialog.Builder builder = new AlertDialog.Builder(
								getActivity());
						builder.setTitle(adapter.getTask().getFiles()
								.get(adapter.getData().get(position).second)
								.getName());

						builder.setItems(
								new String[] { getString(R.string.remove_files) },
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										if (which == 0) {
											List<FileMirakel> l = new ArrayList<FileMirakel>();
											l.add(adapter
													.getTask()
													.getFiles()
													.get(adapter.getData().get(
															position).second));
											TaskDialogHelpers.handleDeleteFile(
													l, main, adapter.getTask(),
													adapter);
										}

									}

								});
						builder.create().show();
						return false;
					}
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
					if (adapter.getSelectedCount() > 0) {
						menu.findItem(R.id.edit_task)
								.setVisible(
										adapter.getSelectedCount() == 1
												&& (adapter.getSelected()
														.get(0).first == TYPE.SUBTASK));
						menu.findItem(R.id.done_task)
								.setVisible(
										adapter.getSelected().get(0).first == TYPE.SUBTASK);
					}
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
						if (adapter.getSelectedCount() > 0
								&& adapter.getSelected().get(0).first == TYPE.FILE) {
							List<FileMirakel> files = adapter.getTask()
									.getFiles();
							List<FileMirakel> selectedItems = new ArrayList<FileMirakel>();
							for (Pair<Integer, Integer> p : selected) {
								if (p.first == TYPE.FILE) {
									selectedItems.add(files.get(p.second));
								}
							}
							TaskDialogHelpers.handleDeleteFile(selectedItems,
									main, adapter.getTask(), adapter);
							break;
						} else if (adapter.getSelectedCount() > 0
								&& adapter.getSelected().get(0).first == TYPE.SUBTASK) {
							List<Task> subtasks = adapter.getTask()
									.getSubtasks();
							List<Task> selectedItems = new ArrayList<Task>();
							for (Pair<Integer, Integer> p : selected) {
								if (p.first == TYPE.SUBTASK) {
									selectedItems.add(subtasks.get(p.second));
								}
							}
							TaskDialogHelpers.handleRemoveSubtask(
									selectedItems, main, adapter,
									adapter.getTask());
						} else {
							Log.e(TAG, "How did you get selected this?");
						}
					case R.id.edit_task:
						if (adapter.getSelectedCount() == 1) {
							adapter.setData(adapter.getTask().getSubtasks()
									.get(adapter.getSelected().get(0).second));
						}
						break;
					case R.id.done_task:
						List<Task> subtasks = adapter.getTask().getSubtasks();
						for (Pair<Integer, Integer> s : adapter.getSelected()) {
							Task t = subtasks.get(s.second);
							t.setDone(true);
							try {
								t.save();
							} catch (NoSuchListException e) {
								Log.d(TAG, "list did vanish");
							}
						}
						break;
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
						mode.invalidate();
					} else if (adapter.getSelectedCount() == 0) {
						mode.finish();// No CAB
					}

				}
			});
		}

		if (main.getPreferences().getBoolean("useBtnCamera", true)
				&& Helpers.isIntentAvailable(main,
						MediaStore.ACTION_IMAGE_CAPTURE)) {
			adapter.setcameraButtonClick(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					try {
						Intent cameraIntent = new Intent(
								MediaStore.ACTION_IMAGE_CAPTURE);
						Uri fileUri = Helpers
								.getOutputMediaFileUri(Helpers.MEDIA_TYPE_IMAGE);
						if (fileUri == null)
							return;
						main.setFileUri(fileUri);
						cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
						cameraIntent.putExtra(MainActivity.TASK_ID, main
								.getCurrentTask().getId());
						getActivity().startActivityForResult(cameraIntent,
								MainActivity.RESULT_CAMERA);

					} catch (ActivityNotFoundException a) {
						Toast.makeText(
								main,
								"Opps! Your device doesn't support taking photos",
								Toast.LENGTH_SHORT).show();
					}

				}
			});
		}
		Log.d(TAG, "created");
		return view;
	}

	public void update(Task t) {
		if (adapter != null) {
			adapter.setData(t);
		}
	}

}
