/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists Copyright (c) 2013-2014 Anatolij Zelenin, Georg
 * Semmler. This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or any later version. This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.helper;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Looper;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.calendar.recurrencepicker.RecurrencePickerDialog;
import com.android.calendar.recurrencepicker.RecurrencePickerDialog.OnRecurenceSetListner;

import de.azapps.mirakel.DefenitionsModel.ExecInterfaceWithTask;
import de.azapps.mirakel.DefinitionsHelper.SYNC_STATE;
import de.azapps.mirakel.custom_views.BaseTaskDetailRow.OnTaskChangedListner;
import de.azapps.mirakel.custom_views.TaskDetailDueReminder;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.tools.FileUtils;
import de.azapps.tools.Log;
import de.azapps.widgets.DateTimeDialog;
import de.azapps.widgets.DateTimeDialog.OnDateTimeSetListner;
import de.azapps.mirakel.customviews.R;

public class TaskDialogHelpers {
	protected static AlertDialog							audio_playback_dialog;
	protected static boolean								audio_playback_playing;

	private static AlertDialog		audio_record_alert_dialog;

	protected static String									audio_record_filePath;

	protected static MediaRecorder							audio_record_mRecorder;

	protected static boolean								content;

	private static final DialogInterface.OnClickListener	dialogDoNothing	= null;

	protected static boolean								done;
	protected static int									listId;
	protected static boolean								newTask;
	protected static boolean								optionEnabled;
	protected static boolean								reminder;
	protected static String									searchString;
	protected static SubtaskAdapter							subtaskAdapter;
	protected static final String							TAG				= "TaskDialogHelpers";

	protected static void cancelRecording() {
		audio_record_mRecorder.stop();
		audio_record_mRecorder.release();
		audio_record_mRecorder = null;
		try {
			new File(audio_record_filePath).delete();
		} catch (Exception e) {
			// eat it
		}
	}

	protected static String generateQuery(Task t) {
		String col = Task.allColumns[0];
		for (int i = 1; i < Task.allColumns.length; i++) {
			col += "," + Task.allColumns[i];
		}
		String query = "SELECT " + col + " FROM " + Task.TABLE
				+ " WHERE name LIKE '%" + searchString + "%' AND";
		query += " NOT _id IN (SELECT parent_id from " + Task.SUBTASK_TABLE
				+ " where child_id=" + t.getId() + ") AND ";
		query += "NOT " + DatabaseHelper.ID + "=" + t.getId();
		query += " AND NOT " + DatabaseHelper.SYNC_STATE_FIELD + "=" + SYNC_STATE.DELETE;
		if (optionEnabled) {
			if (!done) {
				query += " and " + Task.DONE + "=0";
			}
			if (content) {
				query += " and " + Task.CONTENT + " is not null and not "
						+ Task.CONTENT + " =''";
			}
			if (reminder) {
				query += " and " + Task.REMINDER + " is not null";
			}
			if (listId > 0) {
				query += " and " + Task.LIST_ID + "=" + listId;
			} else {
				String where = ((SpecialList) ListMirakel.getList(listId))
						.getWhereQueryForTasks(false);
				Log.d(TAG, where);
				if (where != null && !where.trim().equals("")) {
					query += " and " + where;
				}
			}
		}
		query += ";";
		Log.d(TAG, query);
		return query;
	}

	public static void handleAudioRecord(final Context context, final Task task, final ExecInterfaceWithTask onSuccess) {
		try {
			audio_record_mRecorder = new MediaRecorder();
			audio_record_mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			audio_record_mRecorder
			.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			audio_record_filePath = FileUtils.getOutputMediaFile(
					FileUtils.MEDIA_TYPE_AUDIO).getAbsolutePath();
			audio_record_mRecorder.setOutputFile(audio_record_filePath);
			audio_record_mRecorder
			.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
				audio_record_mRecorder.prepare();
			
			audio_record_mRecorder.start();
		} catch (Exception e) {
			Log.e(TAG, "prepare() failed");
			return;
		}
		audio_record_alert_dialog = new AlertDialog.Builder(context)
		.setTitle(R.string.audio_record_title)
		.setMessage(R.string.audio_record_message)
		.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Task mTask = task;
				if (task == null || task.getId() == 0) {
					mTask = Semantic.createTask(
							MirakelCommonPreferences
							.getAudioDefaultTitle(),
							task==null?MirakelModelPreferences.getImportDefaultList(true):task.getList(), true, context);
				}
				audio_record_mRecorder.stop();
				audio_record_mRecorder.release();
				audio_record_mRecorder = null;
				mTask.addFile(context, audio_record_filePath);
				onSuccess.exec(mTask);
			}
		})
		.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				cancelRecording();

			}
		}).setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				cancelRecording();
			}
		}).show();
	}

	public static void handleDeleteFile(final List<FileMirakel> selectedItems, Context ctx, final Task t, final ExecInterfaceWithTask onSuccess) {
		if (selectedItems.size() < 1) return;
		String files = selectedItems.get(0).getName();
		for (int i = 1; i < selectedItems.size(); i++) {
			files += ", " + selectedItems.get(i).getName();
		}
		new AlertDialog.Builder(ctx)
		.setTitle(ctx.getString(R.string.remove_files))
		.setMessage(
				ctx.getString(R.string.remove_files_summary, files,
						t.getName()))
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								for (FileMirakel f : selectedItems) {
									f.destroy();
								}
								if(onSuccess!=null){
									onSuccess.exec(t);
								}
							}
						})
						.setNegativeButton(android.R.string.cancel, dialogDoNothing)
						.show();

	}


	public static void handlePriority(final Context ctx, final Task task, final Helpers.ExecInterface onSuccess) {
		if (task == null) return;
		final String[] t = { "2", "1", "0", "-1", "-2" };
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.task_change_prio_title);
		builder.setSingleChoiceItems(t, 2 - task.getPriority(),
				new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				task.setPriority(2 - which);
				task.safeSave();
				onSuccess.exec();
			}
		});
		builder.show();
	}

	@SuppressWarnings("boxing")
	public static void handleRecurrence(final ActionBarActivity activity, final Task task, final boolean isDue, final ImageButton image) {
		FragmentManager fm = activity
				.getSupportFragmentManager();
		Recurring r = isDue ? task.getRecurring() : task.getRecurringReminder();
		boolean isExact = false;
		if (r != null) {
			isExact = r.isExact();
			Log.d(TAG, "exact: " + isExact);
			if (r.getDerivedFrom() != null) {
				r = Recurring.get(r.getDerivedFrom());
			}
		}
		RecurrencePickerDialog rp = RecurrencePickerDialog.newInstance(
				new OnRecurenceSetListner() {

					@Override
					public void OnCustomRecurnceSetIntervall(boolean isDue, int intervalYears, int intervalMonths, int intervalDays, int intervalHours, int intervalMinutes, Calendar startDate, Calendar endDate, boolean isExact) {
						Recurring r = Recurring.newRecurring("",
								intervalMinutes, intervalHours, intervalDays,
								intervalMonths, intervalYears, isDue,
								startDate, endDate, true, isExact,
								new SparseBooleanArray());
						setRecurence(task, isDue, r.getId(), image);
					}

					@Override
					public void OnCustomRecurnceSetWeekdays(boolean isDue, List<Integer> weekdays, Calendar startDate, Calendar endDate, boolean isExact) {
						SparseBooleanArray weekdaysArray = new SparseBooleanArray();
						for (int day : weekdays) {
							weekdaysArray.put(day, true);
						}
						Recurring r = Recurring.newRecurring("", 0, 0, 0, 0, 0,
								isDue, startDate, endDate, true, isExact,
								weekdaysArray);
						setRecurence(task, isDue, r.getId(), image);
					}

					@Override
					public void onNoRecurrenceSet() {
						setRecurence(task, isDue, -1, image);
					}

					@Override
					public void OnRecurrenceSet(Recurring r) {
						setRecurence(task, isDue, r.getId(), image);

					}

				}, r, isDue, MirakelCommonPreferences.isDark(), isExact);
		rp.show(fm, "reccurence");

	}

	public static void handleReminder(final ActionBarActivity ctx, final Task task, final OnTaskChangedListner onSuccess) {
		final Calendar reminder = task.getReminder() == null ? new GregorianCalendar()
		: task.getReminder();

		final FragmentManager fm =  ctx
				.getSupportFragmentManager();
		final DateTimeDialog dtDialog = DateTimeDialog.newInstance(
				new OnDateTimeSetListner() {

					@Override
					public void onDateTimeSet(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
						reminder.set(Calendar.YEAR, year);
						reminder.set(Calendar.MONTH, month);
						reminder.set(Calendar.DAY_OF_MONTH, dayOfMonth);
						reminder.set(Calendar.HOUR_OF_DAY, hourOfDay);
						reminder.set(Calendar.MINUTE, minute);
						task.setReminder(reminder,true);
						onSuccess.onTaskChanged(task);

					}

					@Override
					public void onNoTimeSet() {
						task.setReminder(null);
						onSuccess.onTaskChanged(task);

					}
				}, reminder.get(Calendar.YEAR), reminder.get(Calendar.MONTH),
				reminder.get(Calendar.DAY_OF_MONTH),
				reminder.get(Calendar.HOUR_OF_DAY),
				reminder.get(Calendar.MINUTE), true,
				MirakelCommonPreferences.isDark());
		dtDialog.show(fm, "datetimedialog");
	}
	public static void handleRemoveSubtask(final List<Task> subtasks, Context ctx, final Task task,final ExecInterfaceWithTask onSuccess) {
		if (subtasks.size() == 0) return;
		String names = subtasks.get(0).getName();
		for (int i = 1; i < subtasks.size(); i++) {
			names += ", " + subtasks.get(i).getName();
		}
		new AlertDialog.Builder(ctx)
		.setTitle(ctx.getString(R.string.remove_subtask))
		.setMessage(
				ctx.getString(R.string.remove_files_summary, names,
						task.getName()))
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								for (Task s : subtasks) {
									task.deleteSubtask(s);
								}
								if(onSuccess!=null){
									onSuccess.exec(task);
								}
							}
						})
						.setNegativeButton(android.R.string.cancel, dialogDoNothing)
						.show();

	}

	public static void handleSubtask(final Context ctx, final Task task, final OnTaskChangedListner taskChanged, final boolean asSubtask) {
		final List<Pair<Long, String>> names = Task.getTaskNames();
		CharSequence[] values = new String[names.size()];
		for (int i = 0; i < names.size(); i++) {
			values[i] = names.get(i).second;
		}
		View v = ((Activity) ctx).getLayoutInflater().inflate(
				R.layout.select_subtask, null, false);
		final ListView lv = (ListView) v.findViewById(R.id.subtask_listview);
		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				subtaskAdapter = new SubtaskAdapter(ctx, 0, Task.all(), task,
						asSubtask);
				lv.post(new Runnable() {

					@Override
					public void run() {
						lv.setAdapter(subtaskAdapter);
					}
				});
			}
		}).start();
		searchString = "";
		done = false;
		content = false;
		reminder = false;
		optionEnabled = false;
		newTask = true;
		listId = SpecialList.firstSpecialSafe(ctx).getId();
		final EditText search = (EditText) v
				.findViewById(R.id.subtask_searchbox);
		search.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				// Nothing

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// Nothing

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				searchString = s.toString();
				updateListView(subtaskAdapter, task, lv);

			}
		});

		Button options = (Button) v.findViewById(R.id.subtasks_options);
		final LinearLayout wrapper = (LinearLayout) v
				.findViewById(R.id.subtask_option_wrapper);
		wrapper.setVisibility(View.GONE);
		options.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (optionEnabled) {
					wrapper.setVisibility(View.GONE);
				} else {
					wrapper.setVisibility(View.VISIBLE);
					InputMethodManager imm = (InputMethodManager) ctx
							.getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
				}
				optionEnabled = !optionEnabled;

			}
		});
		final ViewSwitcher switcher = (ViewSwitcher) v
				.findViewById(R.id.subtask_switcher);
		final Button subtaskNewtask = (Button) v
				.findViewById(R.id.subtask_newtask);
		final Button subtaskSelectOld = (Button) v
				.findViewById(R.id.subtask_select_old);
		final boolean darkTheme = MirakelCommonPreferences.isDark();
		if (asSubtask) {
			v.findViewById(R.id.subtask_header).setVisibility(View.GONE);
			switcher.showNext();
			newTask = false;
		} else {
			subtaskNewtask.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (newTask) return;
					switcher.showPrevious();
					subtaskNewtask.setTextColor(ctx.getResources().getColor(
							darkTheme ? R.color.White : R.color.Black));
					subtaskSelectOld.setTextColor(ctx.getResources().getColor(
							R.color.Grey));
					newTask = true;

				}
			});
			subtaskSelectOld.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!newTask) return;
					switcher.showNext();
					subtaskNewtask.setTextColor(ctx.getResources().getColor(
							R.color.Grey));
					subtaskSelectOld.setTextColor(ctx.getResources().getColor(
							darkTheme ? R.color.White : R.color.Black));
					newTask = false;
				}
			});
		}
		final CheckBox doneBox = (CheckBox) v.findViewById(R.id.subtask_done);
		doneBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				done = isChecked;
				updateListView(subtaskAdapter, task, lv);
			}
		});
		final CheckBox reminderBox = (CheckBox) v
				.findViewById(R.id.subtask_reminder);
		reminderBox
		.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				reminder = isChecked;
				updateListView(subtaskAdapter, task, lv);
			}
		});

		final Button list = (Button) v.findViewById(R.id.subtask_list);
		list.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final List<ListMirakel> lists = ListMirakel.all(true);
				CharSequence[] names = new String[lists.size()];
				for (int i = 0; i < names.length; i++) {
					names[i] = lists.get(i).getName();
				}
				new AlertDialog.Builder(ctx).setSingleChoiceItems(names, -1,
						new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						listId = lists.get(which).getId();
						updateListView(subtaskAdapter, task, lv);
						list.setText(lists.get(which).getName());
						dialog.dismiss();
					}
				}).show();

			}
		});
		final CheckBox contentBox = (CheckBox) v
				.findViewById(R.id.subtask_content);
		contentBox
		.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				content = isChecked;
				updateListView(subtaskAdapter, task, lv);
			}
		});

		final EditText newTaskEdit = (EditText) v
				.findViewById(R.id.subtask_add_task_edit);

		final AlertDialog dialog = new AlertDialog.Builder(ctx)
		.setTitle(ctx.getString(R.string.add_subtask))
		.setView(v)
		.setPositiveButton(R.string.add,
				new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (newTask
						&& newTaskEdit.getText().length() > 0) {
					newSubtask(
							newTaskEdit.getText().toString(),
							task, ctx);
				} else if (!newTask) {
					boolean[] checked = subtaskAdapter
							.getChecked();
					List<Task> tasks = subtaskAdapter.getData();
					for (int i = 0; i < checked.length; i++) {
						if (checked[i]) {
							if (!asSubtask) {
								if (!tasks.get(i)
										.checkIfParent(task)) {
									task.addSubtask(tasks
											.get(i));
								} else {
									Toast.makeText(
											ctx,
											ctx.getString(R.string.no_loop),
											Toast.LENGTH_LONG)
											.show();
									Log.d(TAG,
											"cannot create loop");
								}
							} else {
								if (!task.checkIfParent(tasks
										.get(i))) {
									tasks.get(i).addSubtask(
											task);
								} else {
									Toast.makeText(
											ctx,
											ctx.getString(R.string.no_loop),
											Toast.LENGTH_LONG)
											.show();
									Log.d(TAG,
											"cannot create loop");
								}
							}
						}
					}
				}
				if (taskChanged != null) {
					taskChanged.onTaskChanged(task);
				}
				dialog.dismiss();
			}

		})
		.setNegativeButton(android.R.string.cancel, dialogDoNothing)
		.show();

		newTaskEdit.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					newSubtask(v.getText().toString(), task, ctx);
					v.setText(null);
					if (taskChanged != null) {
						taskChanged.onTaskChanged(task);
					}
					dialog.dismiss();
				}
				return false;
			}
		});
		if (!MirakelCommonPreferences.isSubtaskDefaultNew()) {
			subtaskSelectOld.performClick();
		}

	}

	protected static Task newSubtask(String name, Task parent, Context ctx) {
		ListMirakel list = MirakelModelPreferences.getListForSubtask(parent);
		Task t = Semantic.createTask(name, list,
				MirakelCommonPreferences.useSemanticNewTask(), ctx);
		parent.addSubtask(t);
		return t;
	}

	public static void openFile(Context context, FileMirakel file) {
		String mimetype = FileUtils.getMimeType(file.getPath());
		Intent i2 = new Intent();
		i2.setAction(android.content.Intent.ACTION_VIEW);
		i2.setDataAndType(Uri.fromFile(new File(file.getPath())), mimetype);
		try {
			context.startActivity(i2);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(context,
					context.getString(R.string.file_no_activity),
					Toast.LENGTH_SHORT).show();
		}
	}

	public static void playbackFile(final Activity context, FileMirakel file, boolean loud) {
		final MediaPlayer mPlayer = new MediaPlayer();

		AudioManager am = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		if (!loud) {
			am.setSpeakerphoneOn(false);
			am.setMode(AudioManager.MODE_IN_CALL);
			context.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
		}

		try {
			mPlayer.reset();
			if (!loud) {
				mPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
			}
			mPlayer.setDataSource(file.getPath());
			mPlayer.prepare();
			mPlayer.start();
			mPlayer.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					audio_playback_dialog.dismiss();
				}
			});
			am.setMode(AudioManager.MODE_NORMAL);
			audio_playback_playing = true;
		} catch (IOException e) {
			Log.e(TAG, "prepare() failed");
		}
		audio_playback_dialog = new AlertDialog.Builder(context)
		.setTitle(R.string.audio_playback_title)
		.setPositiveButton(R.string.audio_playback_pause, null)
		.setNegativeButton(R.string.audio_playback_stop,
				new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				mPlayer.release();
			}
		}).setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				mPlayer.release();
				dialog.cancel();
			}
		}).create();
		audio_playback_dialog.setOnShowListener(new OnShowListener() {

			@Override
			public void onShow(DialogInterface dialog) {
				final Button button = ((AlertDialog) dialog)
						.getButton(DialogInterface.BUTTON_POSITIVE);
				button.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						if (audio_playback_playing) {
							button.setText(R.string.audio_playback_play);
							mPlayer.pause();
							audio_playback_playing = false;
						} else {
							button.setText(R.string.audio_playback_pause);
							mPlayer.start();
							audio_playback_playing = true;
						}

					}
				});

			}
		});
		audio_playback_dialog.show();
	}

	protected static void setRecurence(final Task task, final boolean isDue, int id, ImageButton image) {
		if (isDue) {
			Recurring.destroyTemporary(task.getRecurrenceId());
			task.setRecurrence(id);
		} else {
			Recurring.destroyTemporary(task.getRecurringReminderId());
			task.setRecurringReminder(id);
		}
		TaskDetailDueReminder.setRecurringImage(image, id);
		task.safeSave();
		// if (!isDue) {
		// ReminderAlarm.updateAlarms(ctx);
		// }
	}

	public static void stopRecording() {
		if (audio_record_mRecorder != null) {
			try {
				cancelRecording();
				audio_record_alert_dialog.dismiss();
			} catch (Exception e) {
				// eat it
			}
		}
	}

	protected static void updateListView(final SubtaskAdapter a, final Task t, final ListView lv) {
		if (t == null || a == null || lv == null) return;
		new Thread(new Runnable() {
			@Override
			public void run() {
				List<Task> tasks = Task.rawQuery(generateQuery(t));
				if (tasks == null) return;

				a.setData(tasks);
				lv.post(new Runnable() {

					@Override
					public void run() {
						a.notifyDataSetChanged();

					}
				});

			}
		}).start();

	}
}
