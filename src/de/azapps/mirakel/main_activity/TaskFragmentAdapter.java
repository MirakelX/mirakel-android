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
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.util.Pair;
import android.util.TypedValue;
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
import de.azapps.mirakel.Mirakel.NoSuchListException;
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
	private List<Task> subtasks;
	private List<FileMirakel> files;
	private boolean editContent;
	protected Handler mHandler;
	protected ActionMode mActionMode;
	private static final int SUBTITLE_CONTENT = 0, SUBTITLE_SUBTASKS = 1,
			SUBTITLE_FILES = 2;

	public class TYPE {
		final static int HEADER = 0;
		final static int FILE = 1;
		final static int DUE = 2;
		final static int REMINDER = 3;
		final static int CONTENT = 4;
		final static int SUBTITLE = 5;
		final static int SUBTASK = 6;
	}

	public TaskFragmentAdapter(Context c) {
		// Do not call!!
		super(c, 0, new ArrayList<Pair<Integer, Integer>>());
	}

	public TaskFragmentAdapter(Context context, int textViewResourceId, Task t) {
		super(context, textViewResourceId, generateData(t));
		this.task = t;
		if (task == null)
			task = Task.getDummy(context);
		subtasks = task.getSubtasks();
		files = task.getFiles();
		this.inflater = ((Activity) context).getLayoutInflater();
		this.adapter = this;
		editContent = false;

	}

	@Override
	public int getViewTypeCount() {
		return 7;
	}

	@Override
	public int getItemViewType(int position) {
		return data.get(position).first;
	}

	public void setEditContent(boolean edit) {
		editContent = edit;
		notifyDataSetChanged();
		if (mActionMode != null && edit == false) {
			mActionMode.finish();
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = new View(context);
		if (position >= data.size()) {
			Log.w(TAG, "position > data");
			return row;
		}
		switch (getItemViewType(position)) {
		case TYPE.DUE:
			row = setupDue(parent, convertView);
			break;
		case TYPE.FILE:
			row = setupFile(parent, files.get(data.get(position).second),
					convertView, position);
			break;
		case TYPE.HEADER:
			row = setupHeader(parent, convertView);
			break;
		case TYPE.REMINDER:
			row = setupReminder(parent, convertView);
			break;
		case TYPE.SUBTASK:
			row = setupSubtask(parent, convertView,
					subtasks.get(data.get(position).second), position);
			break;
		case TYPE.SUBTITLE:
			String title = null;
			OnClickListener action = null;
			boolean pencilButton = false;
			switch (data.get(position).second) {
			case SUBTITLE_CONTENT:
				pencilButton = true;
				title = context.getString(R.string.content);
				action = new OnClickListener() {
					@Override
					public void onClick(View v) {
						editContent = !editContent;
						saveContent();
						notifyDataSetChanged();
					}
				};
				break;
			case SUBTITLE_SUBTASKS:
				pencilButton = false;
				title = context.getString(R.string.subtasks);
				action = new OnClickListener() {
					@Override
					public void onClick(View v) {
						TaskDialogHelpers.handleSubtask(context, task, adapter);
					}
				};
				break;
			case SUBTITLE_FILES:
				pencilButton = false;
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
				Log.w(TAG, "unknown subtitle");
				break;
			}
			row = setupSubtitle(parent, title, pencilButton, action,
					convertView);
			break;
		case TYPE.CONTENT:
			row = setupContent(parent, convertView);
			break;

		}

		return row;
	}

	private void saveContent() {
		if (!editContent) {// End Edit, save content
			if (mActionMode != null) {
				mActionMode.finish();
			}
			EditText txt = (EditText) ((MainActivity) context)
					.findViewById(R.id.task_content_edit);
			if (txt != null) {
				((InputMethodManager) context
						.getSystemService(Context.INPUT_METHOD_SERVICE))
						.showSoftInput(txt,
								InputMethodManager.HIDE_IMPLICIT_ONLY);
				if (!txt.getText().toString().trim().equals(task.getContent())) {
					task.setContent(txt.getText().toString().trim());
					try {
						task.save();
					} catch (NoSuchListException e) {
						Log.w(TAG, "List did vanish");
					}
				} else {
					Log.d(TAG, "content equal");
				}
			} else {
				Log.d(TAG, "edit_content not found");
			}
		}
	}

	static class TaskHolder {
		CheckBox taskRowDone;
		LinearLayout taskRowDoneWrapper;
		TextView taskRowName;
		TextView taskRowPriority;
		TextView taskRowDue, taskRowList;
		ImageView taskRowHasContent;
	}

	private View setupSubtask(ViewGroup parent, View convertView, Task task,
			int position) {
		final View row = convertView == null ? inflater.inflate(
				R.layout.tasks_row, parent, false) : convertView;
		final TaskHolder holder;

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

			holder.taskRowDoneWrapper.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Task task = (Task) v.getTag();
					task.toggleDone();
					((MainActivity) context).saveTask(task);
					ReminderAlarm.updateAlarms(context);
					holder.taskRowDone.setChecked(task.isDone());
					updateName(task, row, holder);
				}
			});

			holder.taskRowPriority.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					final Task task = (Task) v.getTag();
					TaskDialogHelpers.handlePriority(context, task,
							new ExecInterface() {

								@Override
								public void exec() {
									((MainActivity) context)
											.updatesForTask(task);
								}
							});

				}
			});

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
		holder.taskRowDone.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Task task = (Task) v.getTag();
				task.toggleDone();
				((MainActivity) context).saveTask(task);
				ReminderAlarm.updateAlarms(context);
				holder.taskRowDone.setChecked(task.isDone());
				updateName(task, row, holder);
			}
		});
		if (task.getContent().length() != 0) {
			holder.taskRowHasContent.setVisibility(View.VISIBLE);
		} else {
			holder.taskRowHasContent.setVisibility(View.INVISIBLE);
		}
		if (task != null && task.getList() != null) {
			holder.taskRowList.setVisibility(View.VISIBLE);
			holder.taskRowList.setText(task.getList().getName());
		} else {
			holder.taskRowList.setVisibility(View.GONE);
		}

		// Name
		holder.taskRowName.setText(task.getName());

		updateName(task, row, holder);

		// Priority
		holder.taskRowPriority.setText("" + task.getPriority());

		GradientDrawable bg = (GradientDrawable) holder.taskRowPriority
				.getBackground();
		bg.setColor(Mirakel.PRIO_COLOR[task.getPriority() + 2]);
		holder.taskRowPriority.setTag(task);

		// Due
		if (task.getDue() != null) {
			holder.taskRowDue.setVisibility(View.VISIBLE);
			holder.taskRowDue
					.setText(Helpers.formatDate(context, task.getDue()));
			holder.taskRowDue.setTextColor(row.getResources().getColor(
					Helpers.getTaskDueColor(task.getDue(), task.isDone())));
		} else {
			holder.taskRowDue.setVisibility(View.GONE);
		}
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (selected.get(position)) {
			row.setBackgroundColor(context.getResources().getColor(
					darkTheme ? R.color.highlighted_text_holo_dark
							: R.color.highlighted_text_holo_light));
		} else if (settings.getBoolean("colorize_tasks", true)) {
			if (settings.getBoolean("colorize_subtasks", true)) {
				int w = row.getWidth() == 0 ? parent.getWidth() : row
						.getWidth();
				Helpers.setListColorBackground(task.getList(), row, darkTheme,
						w);
			} else {
				row.setBackgroundColor(context.getResources().getColor(
						android.R.color.transparent));
			}
		} else {
			row.setBackgroundColor(context.getResources().getColor(
					android.R.color.transparent));
		}
		return row;
	}

	private void updateName(Task task, View row, final TaskHolder holder) {
		if (task.isDone()) {
			holder.taskRowName.setTextColor(row.getResources().getColor(
					R.color.Grey));
		} else {
			holder.taskRowName.setTextColor(row.getResources().getColor(
					darkTheme ? android.R.color.primary_text_dark
							: android.R.color.primary_text_light));
		}
	}

	static class FileHolder {
		ImageView fileImage;
		TextView fileName;
		TextView filePath;
	}

	private View setupFile(ViewGroup parent, final FileMirakel file,
			View convertView, int position) {
		final View row = convertView == null ? inflater.inflate(
				R.layout.files_row, parent, false) : convertView;
		final FileHolder holder;
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
		if (!file.getFile().exists()) {
			holder.filePath.setText(R.string.file_vanished);
		} else {
			holder.filePath.setText(file.getPath());
		}
		markSelected(position, row);

		return row;
	}

	private void markSelected(int position, final View row) {
		if (selected.get(position)) {
			row.setBackgroundColor(context.getResources().getColor(
					darkTheme ? R.color.highlighted_text_holo_dark
							: R.color.highlighted_text_holo_light));
		} else {
			row.setBackgroundColor(context.getResources().getColor(
					android.R.color.transparent));
		}
	}

	static class SubtitleHolder {
		TextView title;
		ImageButton button;
	}

	private View setupSubtitle(ViewGroup parent, String title,
			boolean pencilButton, OnClickListener action, View convertView) {
		if (title == null || action == null)
			return new View(context);
		final View subtitle = convertView == null ? inflater.inflate(
				R.layout.task_subtitle, parent, false) : convertView;

		final SubtitleHolder holder;
		if (convertView == null) {
			holder = new SubtitleHolder();
			holder.title = ((TextView) subtitle
					.findViewById(R.id.task_subtitle));
			holder.button = ((ImageButton) subtitle
					.findViewById(R.id.task_subtitle_button));
			subtitle.setTag(holder);
		} else {
			holder = (SubtitleHolder) subtitle.getTag();
		}
		holder.title.setText(title);
		holder.button.setOnClickListener(action);
		if (pencilButton) {
			if (editContent) {
				holder.button.setImageDrawable(context.getResources()
						.getDrawable(android.R.drawable.ic_menu_save));
			} else {
				holder.button.setImageDrawable(context.getResources()
						.getDrawable(android.R.drawable.ic_menu_edit));
			}
		} else {
			holder.button.setImageDrawable(context.getResources().getDrawable(
					android.R.drawable.ic_menu_add));
		}
		return subtitle;
	}

	static class ContentHolder {
		TextView taskContent;
		EditText taskContentEdit;
		ViewSwitcher taskContentSwitcher;
	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.save, menu);
			return true;
		}

		// Called each time the action mode is shown. Always called after
		// onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false; // Return false if nothing is done
		}

		// Called when the user selects a contextual menu item
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.save:
				editContent = !editContent;
				saveContent();
				notifyDataSetChanged();
				break;
			case R.id.cancel:
				editContent = !editContent;
				notifyDataSetChanged();
				mode.finish();
			}
			return true;
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			if (editContent
					&& Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {// on
																				// 2.x
																				// you
																				// cannot
																				// get
																				// done
																				// button
				editContent = !editContent;
				saveContent();
				notifyDataSetChanged();
			}
			mActionMode = null;
		}

	};
	private String taskEditText;
	protected int cursorPos;

	@SuppressLint("NewApi")
	private View setupContent(ViewGroup parent, View convertView) {
		final View content = convertView == null ? inflater.inflate(
				R.layout.task_content, parent, false) : convertView;
		final ContentHolder holder;
		if (convertView == null) {
			holder = new ContentHolder();
			holder.taskContent = (TextView) content
					.findViewById(R.id.task_content);
			holder.taskContentSwitcher = (ViewSwitcher) content
					.findViewById(R.id.switcher_content);
			holder.taskContentEdit = (EditText) content
					.findViewById(R.id.task_content_edit);
			holder.taskContentEdit
					.setOnFocusChangeListener(new OnFocusChangeListener() {

						@Override
						public void onFocusChange(View v, boolean hasFocus) {
							if (hasFocus) {
								((MainActivity) context)
										.getWindow()
										.setSoftInputMode(
												WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
							} else {
								((MainActivity) context)
										.getWindow()
										.setSoftInputMode(
												WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
							}

						}
					});
			holder.taskContentEdit.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {

				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					if (editContent) {
						cursorPos = holder.taskContentEdit.getSelectionEnd();
					}

				}

				@Override
				public void afterTextChanged(Editable s) {
					taskEditText = holder.taskContentEdit.getText().toString();

				}
			});
			content.setTag(holder);
		} else {
			holder = (ContentHolder) content.getTag();
		}
		while (holder.taskContentSwitcher.getCurrentView().getId() != (editContent ? R.id.task_content_edit
				: R.id.task_content)) {
			holder.taskContentSwitcher.showNext();
		}
		if (editContent) {
			editContent=false;//do not record Textchanges
			holder.taskContentEdit.setText(taskEditText);
			holder.taskContentEdit.setSelection(cursorPos);
			Linkify.addLinks(holder.taskContentEdit, Linkify.WEB_URLS);
			holder.taskContentEdit.requestFocus();
			InputMethodManager imm = ((InputMethodManager) context
					.getSystemService(Context.INPUT_METHOD_SERVICE));
			imm.showSoftInput(holder.taskContentEdit,
					InputMethodManager.SHOW_IMPLICIT);
			holder.taskContentEdit.setSelected(true);
			if (mActionMode == null) {
				mActionMode = ((ActionBarActivity) context)
						.startSupportActionMode(mActionModeCallback);
				View doneButton;
				if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
					int doneButtonId = Resources.getSystem().getIdentifier(
							"action_mode_close_button", "id", "android");
					doneButton = ((ActionBarActivity) context)
							.findViewById(doneButtonId);
					if (doneButton != null) {
						doneButton
								.setOnClickListener(new View.OnClickListener() {

									@Override
									public void onClick(View v) {
										saveContent();
										editContent = false;
										notifyDataSetChanged();
										if (mActionMode != null) {
											mActionMode.finish();
										}
									}
								});
					}
				}
			}
			editContent=true;
		} else {
			// Task content
			holder.taskContent
					.setText(task.getContent().length() == 0 ? context
							.getString(R.string.task_no_content) : task
							.getContent());
			taskEditText = task.getContent();
			cursorPos = taskEditText.length();
			Linkify.addLinks(holder.taskContent, Linkify.WEB_URLS);
			InputMethodManager imm = (InputMethodManager) context
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(
					holder.taskContentEdit.getWindowToken(), 0);
		}
		return content;
	}

	static class ReminderHolder {
		TextView taskReminder;
	}

	private View setupReminder(ViewGroup parent, View convertView) {
		final View reminder = convertView == null ? inflater.inflate(
				R.layout.task_reminder, parent, false) : convertView;
		final ReminderHolder holder;
		if (convertView == null) {
			holder = new ReminderHolder();
			holder.taskReminder = (TextView) reminder
					.findViewById(R.id.task_reminder);
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
												Locale.getDefault())
												.format(task.getReminder()
														.getTime()));
									}
									ReminderAlarm.updateAlarms(context);

								}
							});
				}
			});
			reminder.setTag(holder);
		} else {
			holder = (ReminderHolder) reminder.getTag();
		}
		// Task Reminder
		Drawable reminder_img = context.getResources().getDrawable(
				android.R.drawable.ic_menu_recent_history);
		reminder_img.setBounds(0, 0, 42, 42);
		holder.taskReminder
				.setCompoundDrawables(reminder_img, null, null, null);
		if (task.getReminder() == null) {
			holder.taskReminder
					.setText(context.getString(R.string.no_reminder));
		} else {
			holder.taskReminder.setText(Helpers.formatDate(task.getReminder(),
					context.getString(R.string.humanDateTimeFormat)));
		}
		return reminder;
	}

	static class DueHolder {
		TextView taskDue;
	}

	private View setupDue(ViewGroup parent, View convertView) {
		final View due = convertView == null ? inflater.inflate(
				R.layout.task_due, parent, false) : convertView;
		final DueHolder holder;
		if (convertView == null) {
			holder = new DueHolder();
			holder.taskDue = (TextView) due.findViewById(R.id.task_due);
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
							task.setDue(new GregorianCalendar(year,
									monthOfYear, dayOfMonth));
							((MainActivity) context).saveTask(task);
							holder.taskDue.setText(new SimpleDateFormat(view
									.getContext()
									.getString(R.string.dateFormat), Locale
									.getDefault()).format(task.getDue()
									.getTime()));
						}
					} : null);
					final DatePickerDialog dialog = new DatePickerDialog(
							context, listner, due.get(Calendar.YEAR), due
									.get(Calendar.MONTH), due
									.get(Calendar.DAY_OF_MONTH));
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						dialog.getDatePicker().setCalendarViewShown(false);
						dialog.setButton(DialogInterface.BUTTON_POSITIVE,
								context.getString(android.R.string.ok),
								new DialogInterface.OnClickListener() {

									public void onClick(
											DialogInterface dialog1, int which) {
										if (which == DialogInterface.BUTTON_POSITIVE) {
											if (mIgnoreTimeSet)
												return;
											DatePicker dp = dialog
													.getDatePicker();
											task.setDue(new GregorianCalendar(
													dp.getYear(),
													dp.getMonth(), dp
															.getDayOfMonth()));
											((MainActivity) context)
													.saveTask(task);
											holder.taskDue
													.setText(new SimpleDateFormat(
															context.getString(R.string.dateFormat),
															Locale.getDefault())
															.format(task
																	.getDue()
																	.getTime()));

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
										holder.taskDue
												.setText(R.string.no_date);
									}
								}
							});
					dialog.show();

				}
			});
			due.setTag(holder);
		} else {
			holder = (DueHolder) due.getTag();
		}
		// Task due
		Drawable dueImg = context.getResources().getDrawable(
				android.R.drawable.ic_menu_today);
		dueImg.setBounds(0, 0, 42, 42);
		holder.taskDue.setCompoundDrawables(dueImg, null, null, null);
		if (task.getDue() == null) {
			holder.taskDue.setText(context.getString(R.string.no_date));
		} else {
			holder.taskDue.setText(Helpers.formatDate(context, task.getDue()));
		}
		return due;
	}

	static class HeaderHolder {
		TextView taskName;
		CheckBox taskDone;
		TextView taskPrio;
		ViewSwitcher switcher;
		EditText txt;
	}

	private View setupHeader(ViewGroup parent, View convertView) {
		// Task Name
		final View header = convertView == null ? inflater.inflate(
				R.layout.task_head_line, null, false) : convertView;
		final HeaderHolder holder;
		if (convertView == null) {
			holder = new HeaderHolder();
			holder.taskName = (TextView) header.findViewById(R.id.task_name);
			holder.taskDone = (CheckBox) header.findViewById(R.id.task_done);
			holder.taskPrio = (TextView) header.findViewById(R.id.task_prio);
			holder.switcher = (ViewSwitcher) header
					.findViewById(R.id.switch_name);
			holder.txt = (EditText) header.findViewById(R.id.edit_name);
			holder.taskName.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (((MainActivity) context).isTablet) {
						((EditText) ((MainActivity) context)
								.findViewById(R.id.tasks_new))
								.setOnFocusChangeListener(null);
					}
					holder.switcher.showNext(); // or switcher.showPrevious();
					holder.txt.setText(holder.taskName.getText());
					holder.txt
							.setOnFocusChangeListener(new OnFocusChangeListener() {

								@Override
								public void onFocusChange(View v,
										boolean hasFocus) {
									InputMethodManager imm = (InputMethodManager) context
											.getSystemService(Context.INPUT_METHOD_SERVICE);
									if (hasFocus) {
										imm.showSoftInput(
												holder.txt,
												InputMethodManager.SHOW_IMPLICIT);
									} else {
										imm.showSoftInput(
												holder.txt,
												InputMethodManager.HIDE_IMPLICIT_ONLY);
									}

								}
							});
					holder.txt.requestFocus();
					holder.txt
							.setOnEditorActionListener(new OnEditorActionListener() {
								public boolean onEditorAction(TextView v,
										int actionId, KeyEvent event) {
									if (actionId == EditorInfo.IME_ACTION_DONE) {
										EditText txt = (EditText) header
												.findViewById(R.id.edit_name);
										InputMethodManager imm = (InputMethodManager) context
												.getSystemService(Context.INPUT_METHOD_SERVICE);
										task.setName(txt.getText().toString());
										((MainActivity) context).saveTask(task);
										holder.taskName.setText(task.getName());
										txt.setOnFocusChangeListener(null);
										imm.hideSoftInputFromWindow(
												txt.getWindowToken(), 0);
										holder.switcher.showPrevious();

										return true;
									}
									return false;
								}

							});
				}
			});
			holder.taskPrio.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					TaskDialogHelpers.handlePriority(context, task,
							new ExecInterface() {
								@Override
								public void exec() {
									((MainActivity) context)
											.updatesForTask(task);
									setPrio(holder.taskPrio, task);

								}
							});
				}
			});
			header.setTag(holder);
		} else {
			holder = (HeaderHolder) header.getTag();
			holder.taskDone.setOnCheckedChangeListener(null);
		}

		String tname = task.getName();
		holder.taskName.setText(tname == null ? "" : tname);
		if (((MainActivity) context).isTablet)
			holder.taskName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);

		if (holder.switcher.getCurrentView().getId() == R.id.edit_name
				&& !task.getName().equals(holder.txt.getText().toString())) {
			holder.switcher.showPrevious();
		}

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
		setPrio(holder.taskPrio, task);
		return header;
	}

	protected void setPrio(TextView Task_prio, Task task) {
		Task_prio.setText("" + task.getPriority());

		GradientDrawable bg = (GradientDrawable) Task_prio.getBackground();
		bg.setColor(Mirakel.PRIO_COLOR[task.getPriority() + 2]);

	}

	public void setData(Task t) {
		List<Pair<Integer, Integer>> generateData = generateData(t);
		super.changeData(generateData);
		task = t;
		subtasks = task.getSubtasks();
		files = task.getFiles();
		notifyDataSetInvalidated();
	}

	public List<Pair<Integer, Integer>> getData() {
		return data;
	}

	public Task getTask() {
		return task;
	}

	private static List<Pair<Integer, Integer>> generateData(Task task) {
		List<Pair<Integer, Integer>> data = new ArrayList<Pair<Integer, Integer>>();
		data.add(new Pair<Integer, Integer>(TYPE.HEADER, 0));
		data.add(new Pair<Integer, Integer>(TYPE.DUE, 0));
		data.add(new Pair<Integer, Integer>(TYPE.REMINDER, 0));
		data.add(new Pair<Integer, Integer>(TYPE.SUBTITLE, SUBTITLE_CONTENT));
		data.add(new Pair<Integer, Integer>(TYPE.CONTENT, 0));
		data.add(new Pair<Integer, Integer>(TYPE.SUBTITLE, SUBTITLE_SUBTASKS));
		int subtaskCount = task.getSubtaskCount();
		for (int i = 0; i < subtaskCount; i++) {
			data.add(new Pair<Integer, Integer>(TYPE.SUBTASK, i));
		}
		data.add(new Pair<Integer, Integer>(TYPE.SUBTITLE, SUBTITLE_FILES));
		int fileCount = FileMirakel.getFileCount(task);
		for (int i = 0; i < fileCount; i++)
			data.add(new Pair<Integer, Integer>(TYPE.FILE, i));
		return data;
	}

}
