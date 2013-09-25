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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.util.Linkify;
import android.util.Pair;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewSwitcher;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakelandroid.R;

public class TaskFragmentAdapter extends
		MirakelArrayAdapter<Pair<Integer, Integer>> {// TYPE,INDEX
	private static final String TAG = "TaskFragmentAdapter";
	private Task task;
	protected boolean mIgnoreTimeSet;
	private LayoutInflater inflater;
	private TaskFragmentAdapter adapter;

	public class TYPE {
		final static int HEADER = 0;
		final static int FILE = 1;
		final static int DUE = 2;
		final static int REMINDER = 3;
		final static int CONTENT = 4;
		final static int SUBTITLE = 5;
		final static int SUBTASK = 6;
		final static int NOTHING = -1;
	}

	public TaskFragmentAdapter(Context c) {
		// Do not call!!
		super(c, 0, new ArrayList<Pair<Integer, Integer>>());
	}

	public TaskFragmentAdapter(Context context, int textViewResourceId,
			List<Pair<Integer, Integer>> objects, Task t) {
		super(context, textViewResourceId, objects);
		this.task = t;
		if (task == null)
			task = Task.getDummy(context);
		this.inflater = ((Activity) context).getLayoutInflater();
		this.adapter=this;

	}

	@Override
	public int getViewTypeCount() {
		return 7;
	}


	@Override
	public int getItemViewType(int position) {
		return data.size() > position ? data.get(position).first : TYPE.NOTHING;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = new View(context);
		if (position >= data.size())
			return row;
		switch (getItemViewType(position)) {
		case TYPE.DUE:
			row = setupDue( parent, convertView);
			break;
		case TYPE.FILE:
			row = setupFile( parent,
					task.getFiles().get(data.get(position).second),
					convertView, position);
			break;
		case TYPE.HEADER:
			row = setupHeader( parent, convertView);
			break;
		case TYPE.REMINDER:
			row = setupReminder( parent, convertView);
			break;
		case TYPE.SUBTASK:
			row = setupSubtask( parent, convertView,task.getSubtasks().get(data.get(position).second));
			break;
		case TYPE.SUBTITLE:
			String title = null;
			OnClickListener action = null;
			switch (data.get(position).second) {
			case 0:
				title = context.getString(R.string.subtasks);
				action = new OnClickListener() {
					@Override
					public void onClick(View v) {
						TaskDialogHelpers.handleSubtask(context, task,adapter);
					}
				};
				break;
			case 1:
				title = context.getString(R.string.files);
				action = new OnClickListener() {
					@Override
					public void onClick(View v) {
						Helpers.showFileChooser(MainActivity.RESULT_ADD_FILE,
								context.getString(R.string.file_select),
								(Activity) context);
					}
				};
				break;

			default:
				Log.d(TAG, "unknown subtitle");
				break;
			}
			row = setupSubtitle( parent, title, action, convertView);
			break;
		case TYPE.CONTENT:
			Log.d(TAG, "load content");
			row = setupContent( parent, convertView);
			break;

		}

		return row;
	}
	
	


	static class TaskHolder {
		CheckBox taskRowDone;
		LinearLayout taskRowDoneWrapper;
		TextView taskRowName;
		TextView taskRowPriority;
		TextView taskRowDue, taskRowList;
		ImageView taskRowHasContent;
	}
	private View setupSubtask(ViewGroup parent, View convertView, Task task) {
//		return TaskAdapter.setupRow( null, parent, context, layoutResourceId, task, true, darkTheme);
		View row = convertView==null?inflater.inflate(R.layout.tasks_row, parent, false):convertView;
		TaskHolder holder;

		if (convertView == null) {
			// Initialize the View
			holder = new TaskHolder();
			holder.taskRowDone = (CheckBox) row
					.findViewById(R.id.tasks_row_done);
			holder.taskRowDoneWrapper = (LinearLayout) row
					.findViewById(R.id.tasks_row_done_wrapper);
			holder.taskRowName = (TextView) row
					.findViewById(R.id.tasks_row_name);
			holder.taskRowPriority = (TextView) row
					.findViewById(R.id.tasks_row_priority);
			holder.taskRowDue = (TextView) row.findViewById(R.id.tasks_row_due);
			holder.taskRowHasContent = (ImageView) row
					.findViewById(R.id.tasks_row_has_content);
			holder.taskRowList = (TextView) row
					.findViewById(R.id.tasks_row_list_name);

			row.setTag(holder);
		} else {
			holder = (TaskHolder) row.getTag();
		}
		if (task == null)
			return row;

		// Done
		holder.taskRowDone.setChecked(task.isDone());
		holder.taskRowDone.setTag(task);
		holder.taskRowDoneWrapper.setTag(task);
		if (task.getContent().length() != 0) {
			holder.taskRowHasContent.setVisibility(View.VISIBLE);
		} else {
			holder.taskRowHasContent.setVisibility(View.INVISIBLE);
		}
		if ( task != null && task.getList() != null) {
			holder.taskRowList.setVisibility(View.VISIBLE);
			holder.taskRowList.setText(task.getList().getName());
		} else {
			holder.taskRowList.setVisibility(View.GONE);
		}

		// Name
		holder.taskRowName.setText(task.getName());

		if (task.isDone()) {
			holder.taskRowName.setTextColor(row.getResources().getColor(
					R.color.Grey));
		} else {
			holder.taskRowName.setTextColor(row.getResources().getColor(
					darkTheme ? android.R.color.primary_text_dark
							: android.R.color.primary_text_light));
		}

		// Priority
		holder.taskRowPriority.setText("" + task.getPriority());
		// Log.e("Blubb",holder.taskRowPriority.getBackground().getClass().toString());

		GradientDrawable bg = (GradientDrawable) holder.taskRowPriority
				.getBackground();
		bg.setColor(Mirakel.PRIO_COLOR[task.getPriority() + 2]);
		holder.taskRowPriority.setTag(task);

		// Due
		if (task.getDue() != null) {
			holder.taskRowDue.setVisibility(View.VISIBLE);
			holder.taskRowDue.setText(Helpers.formatDate(task.getDue(),
					context.getString(R.string.dateFormat)));
			holder.taskRowDue.setTextColor(row.getResources().getColor(
					Helpers.getTaskDueColor(task.getDue(), task.isDone())));
		} else {
			holder.taskRowDue.setVisibility(View.GONE);
		}
		return row;
	}




	static class FileHolder {
		ImageView fileImage;
		TextView fileName;
		TextView filePath;
	}

	private View setupFile(ViewGroup parent,
			final FileMirakel file, View convertView, int position) {
		final View row = convertView == null ? inflater.inflate(
				R.layout.files_row, parent, false) : convertView;
		final FileHolder holder;
		Log.d(TAG, "setup files");
		if (convertView == null) {
			holder = new FileHolder();
			holder.fileImage = (ImageView) row.findViewById(R.id.file_image);
			holder.fileName = (TextView) row.findViewById(R.id.file_name);
			holder.filePath = (TextView) row.findViewById(R.id.file_path);
			row.setTag(holder);
		} else {
			holder = (FileHolder) row.getTag();
		}
		new Thread(new Runnable() {
			public void run() {
				final Bitmap preview = file.getPreview();
				if (preview != null) {
					holder.fileImage.post(new Runnable() {
						@Override
						public void run() {
							holder.fileImage.setImageBitmap(preview);

						}
					});
					Log.e(TAG, file.getPath());
				} else {

					holder.fileImage.post(new Runnable() {
						@Override
						public void run() {
							LayoutParams params = (LayoutParams) holder.fileImage
									.getLayoutParams();
							params.height = 0;
							holder.fileImage.setLayoutParams(params);

						}
					});
				}
			}
		}).start();
		holder.fileName.setText(file.getName());
		holder.filePath.setText(file.getPath());
		if (selected.get(position)) {
			row.setBackgroundColor(context.getResources().getColor(
					darkTheme ? R.color.highlighted_text_holo_dark
							: R.color.highlighted_text_holo_light));
		} else {
			row.setBackgroundColor(context.getResources().getColor(
					android.R.color.transparent));
		}

		return row;
	}

	static class SubtitleHolder {
		TextView title;
		ImageButton button;
	}

	private View setupSubtitle(ViewGroup parent,
			String title, OnClickListener action, View convertView) {
		if (title == null || action == null)
			return new View(context);
		final View subtitle = convertView == null ? inflater.inflate(
				R.layout.task_subtitle, parent, false) : convertView;

		((TextView) subtitle.findViewById(R.id.task_subtitle)).setText(title);
		((ImageButton) subtitle.findViewById(R.id.task_subtitle_button))
				.setOnClickListener(action);
		return subtitle;
	}

	static class ContentHolder {
		TextView taskContent;
	}

	private View setupContent(ViewGroup parent,
			View convertView) {
		final View content = convertView == null ? inflater.inflate(
				R.layout.task_content, parent, false) : convertView;
		final ContentHolder holder;
		if (convertView == null) {
			holder = new ContentHolder();
			holder.taskContent = (TextView) content
					.findViewById(R.id.task_content);
			content.setTag(holder);
		} else {
			holder = (ContentHolder) content.getTag();
		}
		// Task content
		setTaskContent(holder.taskContent);

		holder.taskContent.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				final EditText editTxt = new EditText(context);
				editTxt.setText(task.getContent());
				final AlertDialog dialog = new AlertDialog.Builder(context)
						.setTitle(R.string.change_content)
						.setView(editTxt)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										task.setContent(editTxt.getText()
												.toString());
										((MainActivity) context).saveTask(task);
										setTaskContent(holder.taskContent);

									}
								})
						.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// TODO Auto-generated method stub

									}
								}).create();
				dialog.show();
				editTxt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if (hasFocus) {
							dialog.getWindow()
									.setSoftInputMode(
											WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
						}
					}
				});

			}
		});
		return content;
	}

	private void setTaskContent(TextView taskContent) {
		taskContent.setText(task.getContent().length() == 0 ? context
				.getString(R.string.task_no_content) : task.getContent());
		Linkify.addLinks(taskContent, Linkify.WEB_URLS);
	}

	static class ReminderHolder {
		TextView taskReminder;
	}

	private View setupReminder(ViewGroup parent,
			View convertView) {
		final View reminder = convertView == null ? inflater.inflate(
				R.layout.task_reminder, parent, false) : convertView;
		final ReminderHolder holder;
		if (convertView == null) {
			holder = new ReminderHolder();
			holder.taskReminder = (TextView) reminder
					.findViewById(R.id.task_reminder);
			reminder.setTag(holder);
		} else {
			holder = (ReminderHolder) reminder.getTag();
		}
		// Task Reminder
		Drawable reminder_img = context.getResources().getDrawable(
				android.R.drawable.ic_menu_recent_history);
		reminder_img.setBounds(0, 0, 60, 60);
		holder.taskReminder
				.setCompoundDrawables(reminder_img, null, null, null);
		if (task.getReminder() == null) {
			holder.taskReminder
					.setText(context.getString(R.string.no_reminder));
		} else {
			holder.taskReminder.setText(Helpers.formatDate(task.getReminder(),
					context.getString(R.string.humanDateTimeFormat)));
		}
		holder.taskReminder.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				TaskDialogHelpers.handleReminder((Activity) context, task,
						new ExecInterface() {

							@Override
							public void exec() {
								if (task.getReminder() == null) {
									holder.taskReminder
											.setText(R.string.no_reminder);
								} else {
									holder.taskReminder.setText(new SimpleDateFormat(
											context.getString(R.string.humanDateTimeFormat),
											Locale.getDefault()).format(task
											.getReminder().getTime()));
								}
								ReminderAlarm.updateAlarms(context);

							}
						});
			}
		});
		return reminder;
	}

	static class DueHolder {
		TextView taskDue;
	}

	private View setupDue(ViewGroup parent,
			View convertView) {
		final View due = convertView == null ? inflater.inflate(
				R.layout.task_due, parent, false) : convertView;
		final DueHolder holder;
		if (convertView == null) {
			holder = new DueHolder();
			holder.taskDue = (TextView) due.findViewById(R.id.task_due);
			due.setTag(holder);
		} else {
			holder = (DueHolder) due.getTag();
		}
		// Task due
		Drawable due_img = context.getResources().getDrawable(
				android.R.drawable.ic_menu_today);
		due_img.setBounds(0, 0, 60, 60);
		holder.taskDue.setCompoundDrawables(due_img, null, null, null);
		if (task.getDue() == null) {
			holder.taskDue.setText(context.getString(R.string.no_date));
		} else {
			holder.taskDue.setText(Helpers.formatDate(task.getDue(),
					context.getString(R.string.dateFormat)));
		}

		holder.taskDue.setOnClickListener(new View.OnClickListener() {

			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			@Override
			public void onClick(View v) {
				mIgnoreTimeSet = false;
				Calendar due = (task.getDue() == null ? new GregorianCalendar()
						: task.getDue());
				OnDateSetListener listner = (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ? new OnDateSetListener() {

					@Override
					public void onDateSet(DatePicker view, int year,
							int monthOfYear, int dayOfMonth) {
						task.setDue(new GregorianCalendar(year, monthOfYear,
								dayOfMonth));
						((MainActivity) context).saveTask(task);
						holder.taskDue.setText(new SimpleDateFormat(view
								.getContext().getString(R.string.dateFormat),
								Locale.getDefault()).format(task.getDue()
								.getTime()));
					}
				} : null);
				final DatePickerDialog dialog = new DatePickerDialog(context,
						listner, due.get(Calendar.YEAR), due
								.get(Calendar.MONTH), due
								.get(Calendar.DAY_OF_MONTH));
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					dialog.getDatePicker().setCalendarViewShown(false);
					dialog.setButton(DialogInterface.BUTTON_POSITIVE,
							context.getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog1,
										int which) {
									if (which == DialogInterface.BUTTON_POSITIVE) {
										if (mIgnoreTimeSet)
											return;
										DatePicker dp = dialog.getDatePicker();
										task.setDue(new GregorianCalendar(dp
												.getYear(), dp.getMonth(), dp
												.getDayOfMonth()));
										((MainActivity) context).saveTask(task);
										holder.taskDue.setText(new SimpleDateFormat(
												context.getString(R.string.dateFormat),
												Locale.getDefault())
												.format(task.getDue().getTime()));

									}
								}
							});
				}
				dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
						context.getString(R.string.no_date),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog1,
									int which) {
								if (which == DialogInterface.BUTTON_NEGATIVE) {
									mIgnoreTimeSet = true;
									Log.v(TAG, "cancel");
									task.setDue(null);
									((MainActivity) context).saveTask(task);
									holder.taskDue.setText(R.string.no_date);
								}
							}
						});
				dialog.show();

			}
		});
		return due;
	}

	static class HeaderHolder {
		TextView taskName;
		CheckBox taskDone;
		TextView taskPrio;
	}

	private View setupHeader(ViewGroup parent,
			View convertView) {
		// Task Name
		final View header = convertView == null ? inflater.inflate(
				R.layout.task_head_line, null, false) : convertView;
		final HeaderHolder holder;
		if (convertView == null) {
			holder = new HeaderHolder();
			holder.taskName = (TextView) header.findViewById(R.id.task_name);
			holder.taskDone = (CheckBox) header.findViewById(R.id.task_done);
			holder.taskPrio = (TextView) header.findViewById(R.id.task_prio);
			header.setTag(holder);
		} else {
			holder = (HeaderHolder) header.getTag();
		}

		String tname = task.getName();
		holder.taskName.setText(tname == null ? "" : tname);
		if (((MainActivity) context).isTablet)
			holder.taskName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
		holder.taskName.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((MainActivity) context).isTablet) {
					((EditText) ((MainActivity) context)
							.findViewById(R.id.tasks_new))
							.setOnFocusChangeListener(null);
				}
				ViewSwitcher switcher = (ViewSwitcher) header
						.findViewById(R.id.switch_name);
				switcher.showNext(); // or switcher.showPrevious();
				EditText txt = (EditText) header.findViewById(R.id.edit_name);
				txt.setText(holder.taskName.getText());
				txt.requestFocus();

				InputMethodManager imm = (InputMethodManager) context
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(txt, InputMethodManager.SHOW_IMPLICIT);
				txt.setOnEditorActionListener(new OnEditorActionListener() {
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_DONE) {
							EditText txt = (EditText) header
									.findViewById(R.id.edit_name);
							InputMethodManager imm = (InputMethodManager) context
									.getSystemService(Context.INPUT_METHOD_SERVICE);
							task.setName(txt.getText().toString());
							((MainActivity) context).saveTask(task);
							holder.taskName.setText(task.getName());
							imm.hideSoftInputFromWindow(txt.getWindowToken(), 0);
							return true;
						}
						return false;
					}

				});
			}
		});

		// Task done
		holder.taskDone.setChecked(task.isDone());
		holder.taskDone
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						task.setDone(isChecked);
						((MainActivity) context).saveTask(task);
						ReminderAlarm.updateAlarms(context);
						((MainActivity) context).getListFragment().update();
					}
				});

		// Task priority
		set_prio(holder.taskPrio, task);
		holder.taskPrio.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				TaskDialogHelpers.handlePriority(context, task,
						new ExecInterface() {
							@Override
							public void exec() {
								((MainActivity) context).updatesForTask(task);
								set_prio(holder.taskPrio, task);

							}
						});
			}
		});

		return header;
	}

	protected void set_prio(TextView Task_prio, Task task) {
		Task_prio.setText("" + task.getPriority());

		GradientDrawable bg = (GradientDrawable) Task_prio.getBackground();
		bg.setColor(Mirakel.PRIO_COLOR[task.getPriority() + 2]);

	}

	public void setData(List<Pair<Integer, Integer>> generateData, Task t) {
		super.changeData(generateData);
		task = t;
		notifyDataSetInvalidated();
	}

	public List<Pair<Integer, Integer>> getData() {
		return data;
	}

	public Task getTask() {
		return task;
	}

	public void setData(List<Pair<Integer, Integer>> generateData) {
		setData(generateData, task);
	}

}
