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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.main_activity.TaskFragment.TYPE;
import de.azapps.mirakel.model.file.FileMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;
import de.azapps.mirakelandroid.R;

public class TaskFragmentAdapter extends
		MirakelArrayAdapter<Pair<TaskFragment.TYPE, Integer>> {
	private static final String TAG = "TaskFragmentAdapter";
	private Task task;
	private Context ctx;
	private List<Pair<TaskFragment.TYPE, Integer>> data;
	protected boolean mIgnoreTimeSet;
	
	public TaskFragmentAdapter(Context c){
		//Do not call!!
		super(c, 0, new ArrayList<Pair<TaskFragment.TYPE, Integer>>());
	}

	public TaskFragmentAdapter(Context context, int textViewResourceId,
			List<Pair<TaskFragment.TYPE, Integer>> objects, Task t) {
		super(context, textViewResourceId, objects);
		this.task = t;
		if (task == null)
			task = Task.getDummy(ctx);
		this.ctx = context;
		this.data = objects;

	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = new View(ctx);
		if (position >= data.size())
			return row;
		LayoutInflater inflater = ((Activity) ctx).getLayoutInflater();
		switch (data.get(position).first) {
		case DUE:
			row = setupDue(inflater, parent);
			break;
		case FILE:
			row = setupFile(inflater, parent,
					task.getFiles().get(data.get(position).second));
			break;
		case HEADER:
			row = setupHeader(inflater, parent);
			break;
		case REMINDER:
			row = setupReminder(inflater, parent);
			break;
		case SUBTASK:
			// TODO implement Subtasks!!
			break;
		case SUBTITLE:
			String title = null;
			OnClickListener action = null;
			switch (data.get(position).second) {
			case 0:
				title = ctx.getString(R.string.subtasks);
				action = new OnClickListener() {
					@Override
					public void onClick(View v) {
						Log.e(TAG, "do something");

					}
				};
				break;
			case 1:
				title = ctx.getString(R.string.files);
				action = new OnClickListener() {
					@Override
					public void onClick(View v) {
						Helpers.showFileChooser(MainActivity.RESULT_ADD_FILE,
								ctx.getString(R.string.file_select),
								(Activity) ctx);
					}
				};
				break;

			default:
				Log.d(TAG, "unkown subtitle");
				break;
			}
			row = setupSubtitle(inflater, parent, title, action);
			break;
		case CONTENT:
			row = setupContent(inflater, parent);
			break;

		}

		return row;
	}

	private View setupFile(LayoutInflater inflater, ViewGroup parent,
			final FileMirakel file) {

		View row = inflater.inflate(R.layout.files_row, parent, false);
		row.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String mimetype = Helpers.getMimeType(file.getPath());

				Intent i2 = new Intent();
				i2.setAction(android.content.Intent.ACTION_VIEW);
				i2.setDataAndType(Uri.fromFile(new File(file.getPath())),
						mimetype);
				try {
					ctx.startActivity(i2);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(ctx,
							ctx.getString(R.string.file_no_activity),
							Toast.LENGTH_SHORT).show();
				}

			}
		});
		ImageView fileImage = (ImageView) row.findViewById(R.id.file_image);
		TextView fileName = (TextView) row.findViewById(R.id.file_name);
		TextView filePath = (TextView) row.findViewById(R.id.file_path);

		Bitmap preview = file.getPreview();
		if (preview != null) {
			fileImage.setImageBitmap(preview);
			Log.e(TAG, file.getPath());
		} else {
			LayoutParams params = (LayoutParams) fileImage.getLayoutParams();
			params.height = 0;
			fileImage.setLayoutParams(params);
		}
		fileName.setText(file.getName());
		filePath.setText(file.getPath());
		// if (selected.get(position)) {
		// row.setBackgroundColor(context.getResources().getColor(
		// darkTheme ? R.color.highlighted_text_holo_dark
		// : R.color.highlighted_text_holo_light));
		// }else{
		// row.setBackgroundColor(context.getResources().getColor(
		// android.R.color.transparent));
		// }

		return row;
	}

	private View setupSubtitle(LayoutInflater inflater, ViewGroup parent,
			String title, OnClickListener action) {
		if(title==null||action==null)
			return new View(ctx);
		final View subtitle = inflater.inflate(R.layout.task_subtitle, parent,
				false);
		((TextView) subtitle.findViewById(R.id.task_subtitle)).setText(title);
		((ImageButton) subtitle.findViewById(R.id.task_subtitle_button))
				.setOnClickListener(action);
		return subtitle;
	}

	private View setupContent(LayoutInflater inflater, ViewGroup parent) {
		final View content = inflater.inflate(R.layout.task_content, parent,
				false);
		// Task content
		final TextView taskContent = (TextView) content
				.findViewById(R.id.task_content);
		setTaskContent(taskContent);

		taskContent.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				final EditText editTxt = new EditText(ctx);
				editTxt.setText(task.getContent());
				final AlertDialog dialog = new AlertDialog.Builder(ctx)
						.setTitle(R.string.change_content)
						.setView(editTxt)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										task.setContent(editTxt.getText()
												.toString());
										((MainActivity) ctx).saveTask(task);
										setTaskContent(taskContent);

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
		taskContent.setText(task.getContent().length() == 0 ? ctx
				.getString(R.string.task_no_content) : task.getContent());
		Linkify.addLinks(taskContent, Linkify.WEB_URLS);
	}

	private View setupReminder(LayoutInflater inflater, ViewGroup parent) {
		final View reminder = inflater.inflate(R.layout.task_reminder, parent,
				false);
		// Task Reminder
		final TextView taskReminder = (TextView) reminder
				.findViewById(R.id.task_reminder);
		Drawable reminder_img = ctx.getResources().getDrawable(
				android.R.drawable.ic_menu_recent_history);
		reminder_img.setBounds(0, 0, 60, 60);
		taskReminder.setCompoundDrawables(reminder_img, null, null, null);
		if (task.getReminder() == null) {
			taskReminder.setText(ctx.getString(R.string.no_reminder));
		} else {
			taskReminder.setText(Helpers.formatDate(task.getReminder(),
					ctx.getString(R.string.humanDateTimeFormat)));
		}

		taskReminder.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				TaskDialogHelpers.handleReminder((Activity) ctx, task,
						new ExecInterface() {

							@Override
							public void exec() {
								if (task.getReminder() == null) {
									taskReminder.setText(R.string.no_reminder);
								} else {
									taskReminder.setText(new SimpleDateFormat(
											ctx.getString(R.string.humanDateTimeFormat),
											Locale.getDefault()).format(task
											.getReminder().getTime()));
								}
								ReminderAlarm.updateAlarms(ctx);

							}
						});
			}
		});
		return reminder;
	}

	private View setupDue(LayoutInflater inflater, ViewGroup parent) {
		final View due = inflater.inflate(R.layout.task_due, parent, false);
		// Task due
		final TextView taskDue = (TextView) due.findViewById(R.id.task_due);
		Drawable due_img = ctx.getResources().getDrawable(
				android.R.drawable.ic_menu_today);
		due_img.setBounds(0, 0, 60, 60);
		taskDue.setCompoundDrawables(due_img, null, null, null);
		if (task.getDue() == null) {
			taskDue.setText(ctx.getString(R.string.no_date));
		} else {
			taskDue.setText(Helpers.formatDate(task.getDue(),
					ctx.getString(R.string.dateFormat)));
		}

		taskDue.setOnClickListener(new View.OnClickListener() {

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
						((MainActivity) ctx).saveTask(task);
						taskDue.setText(new SimpleDateFormat(view.getContext()
								.getString(R.string.dateFormat), Locale
								.getDefault()).format(task.getDue().getTime()));
					}
				}
						: null);
				final DatePickerDialog dialog = new DatePickerDialog(ctx,
						listner, due.get(Calendar.YEAR), due
								.get(Calendar.MONTH), due
								.get(Calendar.DAY_OF_MONTH));
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					dialog.getDatePicker().setCalendarViewShown(false);
					dialog.setButton(DialogInterface.BUTTON_POSITIVE,
							ctx.getString(android.R.string.ok),
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
										((MainActivity) ctx).saveTask(task);
										taskDue.setText(new SimpleDateFormat(
												ctx.getString(R.string.dateFormat),
												Locale.getDefault())
												.format(task.getDue().getTime()));

									}
								}
							});
				}
				dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
						ctx.getString(R.string.no_date),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog1,
									int which) {
								if (which == DialogInterface.BUTTON_NEGATIVE) {
									mIgnoreTimeSet = true;
									Log.v(TAG, "cancel");
									task.setDue(null);
									((MainActivity) ctx).saveTask(task);
									taskDue.setText(R.string.no_date);
								}
							}
						});
				dialog.show();

			}
		});
		return due;
	}

	private View setupHeader(LayoutInflater inflater, ViewGroup parent) {
		final View header = inflater.inflate(R.layout.task_head_line, parent,
				false);
		// Task Name
		final TextView taskName = (TextView) header
				.findViewById(R.id.task_name);
		String tname = task.getName();
		taskName.setText(tname == null ? "" : tname);
		if (((MainActivity) ctx).isTablet)
			taskName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
		taskName.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((MainActivity) ctx).isTablet) {
					((EditText) ((MainActivity) ctx)
							.findViewById(R.id.tasks_new))
							.setOnFocusChangeListener(null);
				}
				ViewSwitcher switcher = (ViewSwitcher) header
						.findViewById(R.id.switch_name);
				switcher.showNext(); // or switcher.showPrevious();
				EditText txt = (EditText) header.findViewById(R.id.edit_name);
				txt.setText(taskName.getText());
				txt.requestFocus();

				InputMethodManager imm = (InputMethodManager) ctx
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(txt, InputMethodManager.SHOW_IMPLICIT);
				txt.setOnEditorActionListener(new OnEditorActionListener() {
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_DONE) {
							EditText txt = (EditText) header
									.findViewById(R.id.edit_name);
							InputMethodManager imm = (InputMethodManager) ctx
									.getSystemService(Context.INPUT_METHOD_SERVICE);
							task.setName(txt.getText().toString());
							((MainActivity) ctx).saveTask(task);
							taskName.setText(task.getName());
							imm.hideSoftInputFromWindow(txt.getWindowToken(), 0);
							return true;
						}
						return false;
					}

				});
			}
		});

		// Task done
		final CheckBox taskDone = (CheckBox) header
				.findViewById(R.id.task_done);
		taskDone.setChecked(task.isDone());
		taskDone.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				task.setDone(isChecked);
				((MainActivity) ctx).saveTask(task);
				ReminderAlarm.updateAlarms(ctx);
				((MainActivity) ctx).getListFragment().update();
			}
		});

		// Task priority
		final TextView taskPrio = (TextView) header
				.findViewById(R.id.task_prio);
		set_prio(taskPrio, task);
		taskPrio.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				TaskDialogHelpers.handlePriority(ctx, task,
						new ExecInterface() {
							@Override
							public void exec() {
								((MainActivity) ctx).updatesForTask(task);
								set_prio(taskPrio, task);

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

	public void setData(List<Pair<TYPE, Integer>> generateData, Task t) {
		this.data = generateData;
		task = t;
		notifyDataSetInvalidated();
		notifyDataSetChanged();
	}

}
