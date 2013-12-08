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
package de.azapps.mirakel.helper;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.support.v4.app.FragmentManager;
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

import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.adapter.SubtaskAdapter;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.main_activity.task_fragment.TaskFragmentAdapter;
import de.azapps.mirakel.model.DatabaseHelper;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.sync.SyncAdapter;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_STATE;
import de.azapps.mirakelandroid.R;
import de.azapps.widgets.DateTimeDialog;
import de.azapps.widgets.DateTimeDialog.OnDateTimeSetListner;

public class TaskDialogHelpers {
	protected static final String TAG = "TaskDialogHelpers";
	private static final DialogInterface.OnClickListener dialogDoNothing = new DialogInterface.OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			// Nothing

		}
	};

	public static void handlePriority(final Context ctx, final Task task,
			final Helpers.ExecInterface onSuccess) {

		final String[] t = { "2", "1", "0", "-1", "-2" };
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.task_change_prio_title);
		builder.setSingleChoiceItems(t, 2 - task.getPriority(),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						task.setPriority(2 - which);
						safeSafeTask(ctx, task);
						onSuccess.exec();
					}
				});
		builder.show();
	}

	private static void safeSafeTask(Context context, Task task) {
		try {
			task.save();
		} catch (NoSuchListException e) {
			Toast.makeText(context, R.string.list_vanished, Toast.LENGTH_LONG)
					.show();
		}
	}

	public static void handleReminder(final Activity ctx, final Task task,
			final ExecInterface onSuccess, boolean dark) {
		final Calendar reminder = (task.getReminder() == null ? new GregorianCalendar()
				: task.getReminder());

		final FragmentManager fm = ((MainActivity) ctx)
				.getSupportFragmentManager();
		final DateTimeDialog dtDialog = DateTimeDialog.newInstance(
				new OnDateTimeSetListner() {

					@Override
					public void onDateTimeSet(int year, int month,
							int dayOfMonth, int hourOfDay, int minute) {
						reminder.set(Calendar.YEAR, year);
						reminder.set(Calendar.MONTH, month);
						reminder.set(Calendar.DAY_OF_MONTH, dayOfMonth);
						reminder.set(Calendar.HOUR_OF_DAY, hourOfDay);
						reminder.set(Calendar.MINUTE, minute);
						task.setReminder(reminder);
						safeSafeTask(ctx, task);
						onSuccess.exec();

					}

					@Override
					public void onNoTimeSet() {
						task.setReminder(null);
						((MainActivity) ctx).saveTask(task);
						onSuccess.exec();

					}
				}, reminder.get(Calendar.YEAR), reminder.get(Calendar.MONTH),
				reminder.get(Calendar.DAY_OF_MONTH), reminder
						.get(Calendar.HOUR_OF_DAY), reminder
						.get(Calendar.MINUTE), true, dark);
		dtDialog.show(fm, "datetimedialog");
	}

	@SuppressLint("NewApi")
	public static void handleRecurrence(final Activity activity,
			final Task task, final boolean isDue, final ImageButton image,
			boolean dark) {
		FragmentManager fm = ((MainActivity) activity)
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
					public void OnCustomRecurnceSetIntervall(boolean isDue,
							int intervalYears, int intervalMonths,
							int intervalDays, int intervalHours,
							int intervalMinutes, Calendar startDate,
							Calendar endDate, boolean isExact) {
						Recurring r = Recurring.newRecurring("",
								intervalMinutes, intervalHours, intervalDays,
								intervalMonths, intervalYears, isDue,
								startDate, endDate, true, isExact,
								new SparseBooleanArray());
						setRecurence(task, isDue, r.getId());
					}

					@Override
					public void OnCustomRecurnceSetWeekdays(boolean isDue,
							List<Integer> weekdays, Calendar startDate,
							Calendar endDate, boolean isExact) {
						SparseBooleanArray weekdaysArray = new SparseBooleanArray();
						for (int day : weekdays) {
							weekdaysArray.put(day, true);
						}
						Recurring r = Recurring.newRecurring("", 0, 0, 0, 0, 0,
								isDue, startDate, endDate, true, isExact,
								weekdaysArray);
						setRecurence(task, isDue, r.getId());
					}

					@Override
					public void OnRecurrenceSet(Recurring r) {
						image.setBackground(activity.getResources()
								.getDrawable(android.R.drawable.ic_menu_rotate));
						setRecurence(task, isDue, r.getId());

					}

					@Override
					public void onNoRecurrenceSet() {
						image.setBackground(activity.getResources()
								.getDrawable(
										android.R.drawable.ic_menu_mylocation));
						setRecurence(task, isDue, -1);
					}

				}, r, isDue, dark, isExact);
		rp.show(fm, "reccurence");

	}

	public static void handleDeleteFile(final List<FileMirakel> selectedItems,
			Context ctx, final Task t, final TaskFragmentAdapter adapter) {
		if (selectedItems.size() < 1) {
			return;
		}
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
							public void onClick(DialogInterface dialog,
									int which) {
								for (FileMirakel f : selectedItems) {
									f.destroy();
								}
								adapter.setData(t);

							}
						})
				.setNegativeButton(android.R.string.cancel, dialogDoNothing)
				.show();

	}

	private static String searchString;
	private static boolean done;
	private static boolean content;
	private static boolean reminder;
	private static int listId;
	private static boolean optionEnabled;
	private static boolean newTask;
	private static SubtaskAdapter subtaskAdapter;

	public static void handleSubtask(final Context ctx, final Task task,
			final TaskFragmentAdapter adapter, final boolean asSubtask) {
		final List<Pair<Long, String>> names = Task.getTaskNames();
		CharSequence[] values = new String[names.size()];
		for (int i = 0; i < names.size(); i++) {
			values[i] = names.get(i).second;
		}
		View v = ((MainActivity) ctx).getLayoutInflater().inflate(
				R.layout.select_subtask, null, false);
		final ListView lv = (ListView) v.findViewById(R.id.subtask_listview);
		new Thread(new Runnable() {
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
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				searchString = s.toString();
				updateListView(subtaskAdapter, task, lv);

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// Nothing

			}

			@Override
			public void afterTextChanged(Editable s) {
				// Nothing

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
		final boolean darkTheme = MirakelPreferences.isDark();
		if (asSubtask) {
			v.findViewById(R.id.subtask_header).setVisibility(View.GONE);
			switcher.showNext();
			newTask = false;
		} else {
			subtaskNewtask.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (newTask)
						return;
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
					if (!newTask)
						return;
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
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				done = isChecked;
				updateListView(subtaskAdapter, task, lv);
			}
		});
		final CheckBox reminderBox = (CheckBox) v
				.findViewById(R.id.subtask_reminder);
		reminderBox
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
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
							public void onClick(DialogInterface dialog,
									int which) {
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
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
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
							public void onClick(DialogInterface dialog,
									int which) {
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
													try {
														task.addSubtask(tasks
																.get(i));

													} catch (NoSuchListException e) {
														Log.e(TAG,
																"list did vanish");
													}
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
													try {
														tasks.get(i)
																.addSubtask(
																		task);

													} catch (NoSuchListException e) {
														Log.e(TAG,
																"list did vanish");
													}
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
								if (adapter != null) {
									adapter.setData(task);
								}
								dialog.dismiss();
							}

						})
				.setNegativeButton(android.R.string.cancel, dialogDoNothing)
				.show();

		newTaskEdit.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					newSubtask(v.getText().toString(), task, ctx);
					v.setText(null);
					if (adapter != null) {
						adapter.setData(task);
					}
					dialog.dismiss();
				}
				return false;
			}
		});
		if (!MirakelPreferences.isSubtaskDefaultNew()) {
			subtaskSelectOld.performClick();
		}

	}

	private static Task newSubtask(String name, Task parent, Context ctx) {
		ListMirakel list;
		if (MirakelPreferences.addSubtaskToSameList()) {
			list = parent.getList();
		} else {
			list = MirakelPreferences.subtaskAddToList();
			if (list == null)
				list = parent.getList();
		}
		Task t = Semantic.createTask(name, list,
				MirakelPreferences.useSemanticNewTask(), ctx);
		try {
			parent.addSubtask(t);
		} catch (NoSuchListException e) {
			Log.e(TAG, "list did vanish");
		}

		return t;
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
		query += " AND NOT " + SyncAdapter.SYNC_STATE + "=" + SYNC_STATE.DELETE;
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
						.getWhereQuery(false);
				Log.d(TAG, where);
				if (where != null && !where.trim().equals(""))
					query += " and " + where;
			}
		}
		query += ";";
		Log.d(TAG, query);
		return query;
	}

	public static void handleRemoveSubtask(final List<Task> subtasks,
			Context ctx, final TaskFragmentAdapter adapter, final Task task) {
		if (subtasks.size() == 0)
			return;
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
							public void onClick(DialogInterface dialog,
									int which) {
								for (Task s : subtasks) {
									task.deleteSubtask(s);
								}
								adapter.setData(task);
							}
						})
				.setNegativeButton(android.R.string.cancel, dialogDoNothing)
				.show();

	}

	private static void updateListView(final SubtaskAdapter a, final Task t,
			final ListView lv) {
		new Thread(new Runnable() {
			public void run() {
				a.setData(Task.rawQuery(generateQuery(t)));
				lv.post(new Runnable() {

					@Override
					public void run() {
						a.notifyDataSetChanged();

					}
				});

			}
		}).start();

	}

	private static void setRecurence(final Task task, final boolean isDue,
			int id) {
		if (isDue) {
			Recurring.destroyTemporary(task.getRecurrenceId());
			task.setRecurrence(id);
		} else {
			Recurring.destroyTemporary(task.getRecurringReminderId());
			task.setRecurringReminder(id);
		}
		task.safeSave();
	}
}
