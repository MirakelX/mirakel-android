package de.azapps.mirakel.helper;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.main_activity.TaskFragmentAdapter;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class TaskDialogHelpers {
	protected static final String TAG = "TaskDialogHelpers";
	/**
	 * Ugly helper variable
	 */
	private static View numberPicker;

	@SuppressLint("NewApi")
	public static void handlePriority(final Context ctx, final Task task,
			final Helpers.ExecInterface onSuccess) {
		final String[] t = { "-2", "-1", "0", "1", "2" };
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			numberPicker = new NumberPicker(ctx);
			NumberPicker np = (NumberPicker) numberPicker;
			np.setMaxValue(4);
			np.setMinValue(0);
			np.setDisplayedValues(t);
			np.setWrapSelectorWheel(false);
			np.setValue(task.getPriority() + 2);
			np.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		} else {
			numberPicker = ((LayoutInflater) ctx
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.dialog_num_picker_v10, null);
			((Button) numberPicker.findViewById(R.id.dialog_num_pick_plus))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							String val = ((TextView) numberPicker
									.findViewById(R.id.dialog_num_pick_val))
									.getText().toString();
							int i = 0;
							while (!(t[i].contains(val) && t[i].length() == val
									.length())) {
								if (++i >= t.length) {
									Log.wtf(TAG,
											"unknown Value in NumericPicker");
									return;
								}
							}
							((TextView) numberPicker
									.findViewById(R.id.dialog_num_pick_val))
									.setText(t[i + 1 == t.length ? i : i + 1]);
						}
					});
			((Button) numberPicker.findViewById(R.id.dialog_num_pick_minus))
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							String val = ((TextView) numberPicker
									.findViewById(R.id.dialog_num_pick_val))
									.getText().toString();
							int i = 0;
							while (!(t[i].contains(val) && t[i].length() == val
									.length())) {
								if (++i >= t.length) {
									Log.wtf(TAG,
											"unknown Value in NumericPicker");
									return;
								}
							}
							((TextView) numberPicker
									.findViewById(R.id.dialog_num_pick_val))
									.setText(t[i - 1 >= 0 ? i - 1 : i]);
						}
					});
		}
		new AlertDialog.Builder(ctx)
				.setTitle(ctx.getString(R.string.task_change_prio_title))
				.setMessage(ctx.getString(R.string.task_change_prio_cont))
				.setView(numberPicker)
				.setPositiveButton(ctx.getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
									task.setPriority((((NumberPicker) numberPicker)
											.getValue() - 2));
								} else {
									task.setPriority(Integer.parseInt(((TextView) numberPicker
											.findViewById(R.id.dialog_num_pick_val))
											.getText().toString()));
								}
								safeSafeTask(ctx, task);
								onSuccess.exec();
							}

						})
				.setNegativeButton(ctx.getString(android.R.string.cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Do nothing.
							}
						}).show();
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
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// Nothing

							}
						}).show();

	}

	private static String searchString;
	private static boolean done;
	private static boolean content;
	private static boolean reminder;
	private static int listId;
	private static boolean optionEnabled;
	private static boolean newTask; 

	public static void handleSubtask(final Context ctx, final Task task,
			final TaskFragmentAdapter adapter) {
		final List<Pair<Long, String>> names = Task.getTaskNames();
		CharSequence[] values = new String[names.size()];
		for (int i = 0; i < names.size(); i++) {
			values[i] = names.get(i).second;
		}
		View v = ((MainActivity) ctx).getLayoutInflater().inflate(
				R.layout.select_subtask, null, false);
		final ListView lv = (ListView) v.findViewById(R.id.subtask_listview);
		final SubtaskAdapter a = new SubtaskAdapter(ctx, 0, Task.all(), task);
		lv.setAdapter(a);
		searchString = "";
		done = false;
		content = false;
		reminder = false;
		optionEnabled = false;
		newTask=false;
		listId = SpecialList.firstSpecialSafe(ctx).getId();
		EditText search = (EditText) v.findViewById(R.id.subtask_searchbox);
		search.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				searchString = s.toString();
				updateListView(a);

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
					Animation slideUp = AnimationUtils.loadAnimation(ctx,
							R.anim.slide_up);
					wrapper.startAnimation(slideUp);
				} else {
					wrapper.setVisibility(View.VISIBLE);
					Animation slideDown = AnimationUtils.loadAnimation(ctx,
							R.anim.slide_down);
					wrapper.startAnimation(slideDown);
				}
				optionEnabled = !optionEnabled;

			}
		});
		final ViewSwitcher switcher=(ViewSwitcher)v.findViewById(R.id.subtask_switcher);
		Button subtaskNewtask=(Button)v.findViewById(R.id.subtask_newtask);
		subtaskNewtask.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				switcher.showPrevious();
				newTask=true;
				
			}
		});
		
		Button subtaskSelectOld=(Button)v.findViewById(R.id.subtask_select_old);
		subtaskSelectOld.setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
				switcher.showNext();
				newTask=false;
			}
		});
		
		final CheckBox doneBox = (CheckBox) v.findViewById(R.id.subtask_done);
		doneBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				done = isChecked;
				updateListView(a);
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
						updateListView(a);
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
								updateListView(a);
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
						updateListView(a);
					}
				});
		
		final EditText newTaskEdit=(EditText)v.findViewById(R.id.subtask_add_task_edit);
		new AlertDialog.Builder(ctx)
				.setTitle(ctx.getString(R.string.add_subtask))
				.setView(v)
				.setPositiveButton(R.string.add,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if(newTask&&newTaskEdit.getText().length()>0){
									Task t=Task.newTask(newTaskEdit.getText().toString(), task.getList().getId());
									try {
										task.addSubtask(t);
									} catch (NoSuchListException e) {
										Log.e(TAG, "list did vanish");
									}
								}else if(!newTask){
									boolean[] checked = a.getChecked();
									List<Task> tasks = a.getData();
									for (int i = 0; i < checked.length; i++) {
										if (checked[i]
												&& !tasks.get(i)
														.isSubtaskFrom(task)) {
											try {
												task.addSubtask(tasks.get(i));
											} catch (NoSuchListException e) {
												Log.e(TAG, "list did vanish");
											}
										}
									}
								}
								adapter.setData(task);
								dialog.dismiss();
							}

						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// Nothing
								dialog.dismiss();

							}

						})
				.show();

	}

	protected static String generateQuery() {
		String query = "name LIKE '%" + searchString + "%'";
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
						.getWhereQuery();
				Log.d(TAG, where);
				if (where != null && !where.trim().equals(""))
					query += " and " + where;
			}
		}
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
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// Nothing

							}

						}).show();

	}

	private static void updateListView(final SubtaskAdapter a) {
		new Thread(new Runnable() {
			public void run() {
				a.setData(Task.search(generateQuery()));
			}
		}).start();

	}

}
