package de.azapps.mirakel.helper;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.main_activity.TaskFragmentAdapter;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.model.semantic.Semantic;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

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

	public static void handleReminder(final Activity act, final Task task,
			final ExecInterface onSuccess) {
		final Context ctx = (Context) act;
		Calendar reminder = (task.getReminder() == null ? new GregorianCalendar()
				: task.getReminder());
		// Inflate the root layout
		final RelativeLayout mDateTimeDialogView = (RelativeLayout) act
				.getLayoutInflater().inflate(R.layout.date_time_dialog, null);
		// Grab widget instance
		final DateTimePicker mDateTimePicker = (DateTimePicker) mDateTimeDialogView
				.findViewById(R.id.DateTimePicker);
		mDateTimePicker.setIs24HourView(true);
		mDateTimePicker.updateDate(reminder.get(Calendar.YEAR),
				reminder.get(Calendar.MONTH),
				reminder.get(Calendar.DAY_OF_MONTH));
		mDateTimePicker.updateTime(reminder.get(Calendar.HOUR_OF_DAY),
				reminder.get(Calendar.MINUTE));
		new AlertDialog.Builder(ctx)
				.setTitle(R.string.task_set_reminder)
				.setView(mDateTimeDialogView)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mDateTimePicker.clearFocus();

								task.setReminder(new GregorianCalendar(
										mDateTimePicker.get(Calendar.YEAR),
										mDateTimePicker.get(Calendar.MONTH),
										mDateTimePicker
												.get(Calendar.DAY_OF_MONTH),
										mDateTimePicker
												.get(Calendar.HOUR_OF_DAY),
										mDateTimePicker.get(Calendar.MINUTE)));
								safeSafeTask(ctx, task);
								onSuccess.exec();

							}
						})
				.setNegativeButton(R.string.no_date,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mDateTimePicker.clearFocus();
								task.setReminder(null);
								safeSafeTask(ctx, task);
								onSuccess.exec();

							}
						}).show();
		Spinner recurrence = (Spinner) mDateTimeDialogView
				.findViewById(R.id.add_reccuring);
		TaskDialogHelpers.handleRecurrence(ctx, task, recurrence, false);
	}

	public static void handleRecurrence(final Context context, final Task task,
			Spinner spinner, final boolean isDue) {
		final List<Pair<Integer, String>> recurring = Recurring
				.getForDialog(isDue);
		CharSequence[] items = new String[recurring.size() + 1];
		Recurring r = isDue ? task.getRecurring() : task.getRecurringReminder();

		items[0] = context.getString(R.string.nothing);
		int pos = 0;
		for (int i = 1; i < recurring.size() + 1; i++) {
			items[i] = recurring.get(i - 1).second;
			if (r != null && items[i].equals(r.getLabel())) {
				pos = i;
			}
		}
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
				context, android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(pos);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				int r = pos == 0 ? -1 : recurring.get(pos - 1).first;
				if (isDue) {
					task.setRecurrence(r);
				} else {
					task.setRecurrenceReminder(r);
				}
				try {
					task.save();
				} catch (NoSuchListException e) {
					Log.w(TAG, "List did vanish");
				}
			}

			public void onNothingSelected(AdapterView<?> parent) {
				// Another interface callback
			}

		});

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
			final TaskFragmentAdapter adapter) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(ctx);
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
				subtaskAdapter = new SubtaskAdapter(ctx, 0, Task.all(), task);
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
		EditText search = (EditText) v.findViewById(R.id.subtask_searchbox);
		search.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				searchString = s.toString();
				updateListView(subtaskAdapter, task);

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
		final boolean darkTheme = settings.getBoolean("DarkTheme", false);
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
				;
				newTask = false;
			}
		});

		final CheckBox doneBox = (CheckBox) v.findViewById(R.id.subtask_done);
		doneBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				done = isChecked;
				updateListView(subtaskAdapter, task);
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
						updateListView(subtaskAdapter, task);
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
								updateListView(subtaskAdapter, task);
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
						updateListView(subtaskAdapter, task);
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
											if (!tasks.get(i).checkIfParent(
													task)) {
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
												Log.d(TAG, "cannot create loop");
											}
										}
									}
								}
								adapter.setData(task);
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
					adapter.setData(task);
					dialog.dismiss();
				}
				return false;
			}
		});
		if (!settings.getBoolean("subtaskDefaultNew", true)) {
			subtaskSelectOld.performClick();
		}

	}

	private static Task newSubtask(String name, Task parent, Context ctx) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		try {
			ListMirakel list;
			if (settings.getBoolean("subtaskAddToSameList", true)) {
				list = parent.getList();
			} else {
				list = ListMirakel.getList(settings.getInt("subtaskAddToList",
						-1));
				if (list == null)
					list = parent.getList();
			}
			Task t = Semantic.createTask(name, list,
					settings.getBoolean("semanticNewTask", true));
			try {
				parent.addSubtask(t);
			} catch (NoSuchListException e) {
				Log.e(TAG, "list did vanish");
			}

			return t;
		} catch (Semantic.NoListsException e) {
			Toast.makeText(ctx, R.string.no_lists, Toast.LENGTH_LONG).show();
		}
		return null;
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
		query += "NOT _id=" + t.getId();
		if (optionEnabled) {
			if (done) {
				query += " and done=0";
			}
			if (content) {
				query += " and content is not null";
			}
			if (reminder) {
				query += " and reminder is not null";
			}
			if (listId > 0) {
				query += " and list_id=" + listId;
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

	private static void updateListView(final SubtaskAdapter a, final Task t) {
		new Thread(new Runnable() {
			public void run() {
				a.setData(Task.rawQuery(generateQuery(t)));
			}
		}).start();

	}

}
